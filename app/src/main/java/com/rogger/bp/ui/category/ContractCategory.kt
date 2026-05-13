package com.rogger.bp.ui.category

import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.ui.base.BasePresenter
import com.rogger.bp.ui.base.BaseView

interface ContractCategory {
    interface Presenter : BasePresenter {

        fun create(postCategory: PostCategory)
        fun upload(postCategory: PostCategory)
        fun delete(postCategory: PostCategory)
    }

    interface View : BaseView<Presenter> {
        fun showProgress(enable: Boolean)
        fun onSuccess()
        fun onFailure(message: String)
        fun categoryExists(message: String)
    }
}