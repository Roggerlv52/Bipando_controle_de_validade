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
                // ✅ CORREÇÃO: lista vazia mostra o estado vazio em vez de RecyclerView em branco
                if (items.isNullOrEmpty()) {
                    view?.showEmptyState()
                } else {
                    view?.showDeletedItems(items)
                }
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
                loadDeletedItems() // ✅ CORREÇÃO: recarrega a lista após restaurar
            }

            override fun onFailure(message: String) {
                view?.showErrorMessage("Erro ao restaurar produto: $message")
                view?.showLoading(false)
            }

            override fun onComplete() {
                // loading encerrado após recarregar a lista
                view?.showLoading(false)
            }
        })
    }

    override fun deletePermanently(item: PostProduct) {
        view?.showLoading(true)
        repository.deletePermanently(item, object : DeleteItemCallback {
            override fun onSuccess(items: List<PostProduct>?) {
                view?.showSuccessMessage("Produto excluído definitivamente")
                loadDeletedItems() // ✅ CORREÇÃO: recarrega a lista após excluir
            }

            override fun onFailure(message: String) {
                view?.showErrorMessage("Erro ao excluir produto: $message")
                view?.showLoading(false)
            }

            override fun onComplete() {
                // loading encerrado após recarregar a lista
            }
        })
    }

    override fun onDestroy() {
        view = null
    }
}