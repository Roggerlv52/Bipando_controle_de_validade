package com.rogger.bp.ui.deleteitem.data

import com.rogger.bp.data.model.PostProduct

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 17/05/2026
 * Hora: 16:29
 */
class DeleteItemRepository(private val dataSource: PostDeletedItem) {
    fun fetchItemDeleted(callback: DeleteItemCallback){
        dataSource.fetchItemDeleted(callback)
    }

    fun restore(item: PostProduct, callback: DeleteItemCallback) {
        dataSource.restoreItemDeleted(item, callback)
    }

    fun deletePermanently(item: PostProduct, callback: DeleteItemCallback) {
        dataSource.deletePermanently(item, callback)
    }
}