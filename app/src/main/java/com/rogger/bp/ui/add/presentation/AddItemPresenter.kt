package com.rogger.bp.ui.add.presentation

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.add.RegisterAdd
import com.rogger.bp.ui.add.data.ItemDataSource
import com.rogger.bp.ui.add.data.RegisterItemCallback
import com.rogger.bp.ui.add.data.RegisterItemRepository
import com.rogger.bp.ui.add.data.SaveImageCallback
import java.io.File


/**
 * Presenter do fluxo de cadastro de item.
 *
 * Responsabilidades:
 *  1. Verificar se imagem do barcode já existe (checkOrCreateImage).
 *  2. Fazer upload de imagem capturada pela câmera/galeria (uploadImage).
 *  3. Salvar o produto no banco (saveProduct).
 *
 * A View é notificada em onDestroy() para evitar memory leak.
 */
class AddItemPresenter(
    private var view: RegisterAdd.View?,
    private val repository: RegisterItemRepository
) : RegisterAdd.Presenter {

    // ── 1. Verificar/criar imagem pelo barcode ─────────────────────────────

    override fun checkOrCreateImage(barcode: String) {
        if (barcode.isEmpty()) {
            view?.onFailure("Código de barras inválido")
            return
        }

        view?.showProgress(true)

        val image = PostImage(barcode = barcode)

        repository.createImage(image, object : SaveImageCallback {

            override fun onSuccess(image: PostImage) {
                view?.showProgress(false)
            }

            override fun onAlreadyExists(image: PostImage) {
                view?.showProgress(false)
                view?.imageAlreadyExists(image)
            }
            override fun onFailure(message: String) {
                view?.onFailure(message)
            }
            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }

    // ── 2. Upload de imagem capturada (câmera / galeria) ──────────────────
    override fun uploadImage(image: PostImage) {
        if (image.uri.isEmpty()) {
            view?.onFailure("Nenhuma imagem selecionada")
            return
        }

        view?.showProgress(true)
        repository.uploadImage(image, object : SaveImageCallback {

            override fun onSuccess(image: PostImage) {
                view?.goToHome()
            }
            override fun onAlreadyExists(image: PostImage) {
                view?.imageAlreadyExists(image)
            }
            override fun onFailure(message: String) {
                view?.onFailure(message)
            }
            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }

    override fun saveProduct(product: PostProduct) {
        if (product.name.isBlank()) {
            view?.onFailure("Informe o nome do produto")
            return
        }

        view?.showProgress(true)

        // Upload de imagem local adiado para o momento do "Salvar":
        // só faz upload se o URI for local (file://) e houver barcode.
        val isLocalImage = product.imageUri.startsWith("file://")

        if (isLocalImage && product.barcode.isNotEmpty()) {
            val postImage = PostImage(
                barcode = product.barcode,
                name    = product.name,
                uri     = product.imageUri
            )
            repository.uploadImage(postImage, object : SaveImageCallback {
                override fun onSuccess(uploaded: PostImage) {
                    // Upload OK → persiste o produto com a URI remota devolvida pelo Storage
                    val updatedProduct = product.copy(imageUri = uploaded.uri)
                    repository.create(updatedProduct, createCallback())
                }
                override fun onAlreadyExists(image: PostImage) {
                    // Imagem já existia → usa URI remota existente
                    val updatedProduct = product.copy(imageUri = image.uri)
                    repository.create(updatedProduct, createCallback())
                }
                override fun onFailure(message: String) {
                    view?.onFailure(message)
                    view?.showProgress(false)
                }
                override fun onComplete() { /* gerido pelos ramos acima */ }
            })
        } else {
            // Sem imagem local (remota já existe ou nenhuma imagem)
            repository.create(product, createCallback())
        }
    }

    private fun createCallback() = object : RegisterItemCallback {
        override fun onSuccess(image: PostImage?) { view?.goToHome() }
        override fun onFailure(message: String)   { view?.onFailure(message) }
        override fun onComplete()                 { view?.showProgress(false) }
    }
    override fun onDestroy() {
        view = null
    }
}