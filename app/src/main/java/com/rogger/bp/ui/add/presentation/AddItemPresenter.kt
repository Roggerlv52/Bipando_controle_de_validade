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
        view?.showProgress(true)
        repository.createImage(image, object : SaveImageCallback {
            override fun onSuccess() {

            }

            override fun onAlreadyExists(image: PostImage) {

            }

            override fun onFailure(message: String) {

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

            }

            override fun onAlreadyExists(image: PostImage) {
            }

            override fun onFailure(message: String) {

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