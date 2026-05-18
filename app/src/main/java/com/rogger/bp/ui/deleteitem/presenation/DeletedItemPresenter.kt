package com.rogger.bp.ui.deleteitem.presenation

import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.deleteitem.DeletedItem
import com.rogger.bp.ui.deleteitem.data.DeleteItemCallback
import com.rogger.bp.ui.deleteitem.data.DeleteItemRepository

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 17/05/2026
 * Hora: 16:32
 */
class DeletedItemPresenter(
    private var view: DeletedItem.View?,
    private val repository: DeleteItemRepository
) : DeletedItem.Presenter {

    override fun loadDeletedItems() {
        view?.showLoading(true)
        repository.fetchItemDeleted(object : DeleteItemCallback {

            override fun onSuccess(items: List<PostProduct>?) {
                view?.showDeletedItems(items)
            }
            override fun onFailure(message: String) {
                view?.showErrorMessage(message)
                view?.showEmptyState()
            }
            override fun onComplete() {
                view?.showLoading(false)
            }
        })
    }

    override fun restoreItem(item: PostProduct) {
        view?.showLoading(true)
        repository.restore(item, object : DeleteItemCallback {
            override fun onSuccess(items: List<PostProduct>?) {
                view?.showSuccessMessage("Produto restaurado com sucesso")
            }

            override fun onFailure(message: String) {
                view?.showErrorMessage("Erro ao restaurar produto: $message")
            }

            override fun onComplete() {
                view?.showLoading(false)
            }
        })
    }

    override fun deletePermanently(item: PostProduct) {
        view?.showLoading(true)
        repository.deletePermanently(item, object : DeleteItemCallback {
            override fun onSuccess(items: List<PostProduct>?) {
                view?.showSuccessMessage("Produto excluído definitivamente")
            }

            override fun onFailure(message: String) {
                view?.showErrorMessage("Erro ao excluir produto: $message")
            }

            override fun onComplete() {
                view?.showLoading(false)
            }
        })
    }

    override fun onDestroy() {
        view = null
    }
}