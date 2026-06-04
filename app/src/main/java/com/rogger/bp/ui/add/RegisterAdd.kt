package com.rogger.bp.ui.add

import android.content.Context
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.base.BasePresenter
import com.rogger.bp.ui.base.BaseView


interface RegisterAdd {
    interface Presenter : BasePresenter {

        fun checkOrCreateImage(barcode: String)
        fun uploadImage(image: PostImage)
        fun saveProduct(product: PostProduct,context: Context)
        fun fetchCategories()
    }

    interface View : BaseView<Presenter> {
        fun showProgress(enable: Boolean)
        fun onFailure(message: String)
        fun imageAlreadyExists(postImage: PostImage)
        fun onImageNotFound()
        fun openCamera()
        fun openGallery()
        fun goToHome()
        fun showCategories(categories: List<PostCategory>)
    }
}