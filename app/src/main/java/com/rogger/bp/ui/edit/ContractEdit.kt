package com.rogger.bp.ui.edit

import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.base.BasePresenter
import com.rogger.bp.ui.base.BaseView

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 19:50
 */
interface ContractEdit {
    interface Presenter : BasePresenter {
        fun loadProduct(productId: Int)
        fun saveProduct(produto: PostProduct)
        fun deleteProduct(produto: PostProduct)
        fun fetchCategories()
    }

    interface View : BaseView<Presenter> {
        fun showProgress(enable: Boolean)
        fun bindProduct(produto: PostProduct)
        fun showCategories(categories: List<PostCategory>)
        fun onSuccess(message: String)
        fun onError(message: String)
        fun navigateBack()
    }
}
