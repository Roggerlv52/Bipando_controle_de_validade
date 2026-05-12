package com.rogger.bp.ui.add.presentation

import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.add.RegisterAdd
import com.rogger.bp.ui.add.data.RegisterItemCallback
import com.rogger.bp.ui.add.data.RegisterItemRepository
import com.rogger.bp.ui.add.data.SaveImageCallback

class AddItemPresenter(
    private var view: RegisterAdd.View?,
    private val repository: RegisterItemRepository

) : RegisterAdd.Presenter {

    override fun create(product: PostProduct) {
        view?.showProgress(true)
        repository.create(product, object : RegisterItemCallback {
            override fun onSuccess(image: PostImage?) {
                // Sucesso na criação do item
            }

            override fun onFailure(message: String) {
                view?.onFailure(message)
            }

            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }

    override fun createImage(image: PostImage) {
        if (image.barcode.isEmpty()) {
            view?.onFailure("Código de barras inválido")
            return
        }

        view?.showProgress(true)
        // A lógica de verificação já está no repository/datasource (saveProductImage)
        repository.createImage(image, object : SaveImageCallback {
            override fun onSuccess() {
                // Imagem nova salva com sucesso
            }

            override fun onAlreadyExists(image: PostImage) {
                // Imagem já existe no banco, o datasource já retornou os dados dela
                // Aqui você pode atualizar a UI informando que a imagem foi reaproveitada
            }

            override fun onFailure(message: String) {
                view?.onFailure(message)
            }

            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }

    override fun upload(image: PostImage) {
        view?.showProgress(true)
        repository.uploadImage(image, object : SaveImageCallback {
            override fun onSuccess() {
                // Upload concluído
            }

            override fun onAlreadyExists(image: PostImage) {
                // Caso improvável aqui, mas tratado pelo callback
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
