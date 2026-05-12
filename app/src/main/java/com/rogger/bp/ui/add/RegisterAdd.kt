package com.rogger.bp.ui.add

import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.base.BasePresenter
import com.rogger.bp.ui.base.BaseView

interface RegisterAdd {
    interface Presenter : BasePresenter {
        fun create(product: PostProduct)
        fun createImage(image: PostImage)
        fun upload(image: PostImage)
    }

    interface View : BaseView<Presenter> {
        fun goToHome()
        fun openCamera()
        fun showProgress(enable: Boolean)
        fun imageExit(postImage: PostImage)
        fun onFailure(message: String)
        fun onSave()
    }
}