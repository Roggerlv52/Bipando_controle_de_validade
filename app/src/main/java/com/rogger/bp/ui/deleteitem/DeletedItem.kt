package com.rogger.bp.ui.deleteitem

import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.base.BasePresenter
import com.rogger.bp.ui.base.BaseView

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 17/05/2026
 * Hora: 16:27
 */
interface DeletedItem {
    interface View : BaseView<Presenter> {
        fun showDeletedItems(items: List<PostProduct>?)
        fun showEmptyState()
        fun showSuccessMessage(message: String)
        fun showErrorMessage(message: String)
        fun showLoading(show: Boolean)
    }

    interface Presenter : BasePresenter {
        fun loadDeletedItems()
        fun restoreItem(item: PostProduct)
        fun deletePermanently(item: PostProduct)
    }
}