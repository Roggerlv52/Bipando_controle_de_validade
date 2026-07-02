package com.rogger.bp.ui.category

import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.ui.base.BasePresenter
import com.rogger.bp.ui.base.BaseView


interface ContractCategory {
    interface Presenter : BasePresenter {
        fun create(category: PostCategory)
        fun update(category: PostCategory)
        fun delete(category: PostCategory)
        fun deleteMultiple(categories: List<PostCategory>)
        fun fetchCategories()
        fun getCountCategorias()
    }

    interface View : BaseView<Presenter> {
        fun showProgress(enable: Boolean)
        // fun showCategories(categories: List<PostCategory>) // Removido, a View observará o Flow diretamente
        fun showEmpty(isEmpty: Boolean)
        fun onSuccess(message: String)
        fun onError(message: String)
        fun onCategoryExists(category: PostCategory)
        fun onCategoryCreated(category: PostCategory)
        fun onCategoryUpdated(category: PostCategory)
        fun onCategoryDeleted(category: PostCategory)
    }
}