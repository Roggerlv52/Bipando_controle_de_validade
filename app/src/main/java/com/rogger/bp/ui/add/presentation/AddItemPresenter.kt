package com.rogger.bp.ui.add.presentation

import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.add.RegisterAdd
import com.rogger.bp.ui.add.data.RegisterItemCallback
import com.rogger.bp.ui.add.data.RegisterItemRepository
import com.rogger.bp.ui.add.data.SaveImageCallback


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

            override fun onSuccess() {
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

            override fun onSuccess() {
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
        repository.create(product, object : RegisterItemCallback {

            override fun onSuccess(image:PostImage?) {
                view?.goToHome()
            }
            override fun onFailure(message: String) {
                view?.onFailure(message)
            }
            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }
    override fun onDestroy() {
        view = null
    }
}