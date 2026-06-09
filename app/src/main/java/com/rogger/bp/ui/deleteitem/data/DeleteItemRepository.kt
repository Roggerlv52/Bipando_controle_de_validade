package com.rogger.bp.ui.deleteitem.data

import com.rogger.bp.data.database.RoomProductCache
import com.rogger.bp.data.image.datasource.UserImageDataSource
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.commun.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 17/05/2026
 * Hora: 16:29
 */

class DeleteItemRepository(
    private val dataSource: PostDeletedItem,
    private val localCache: RoomProductCache
) {
    fun fetchItemDeleted(callback: DeleteItemCallback){
        dataSource.fetchItemDeleted(callback)
    }

    fun restore(item: PostProduct, callback: DeleteItemCallback) {
        val isOnline = NetworkUtils.isNetworkAvailable()

        if (isOnline) {
            // ── CENÁRIO ONLINE ──
            dataSource.restoreItemDeleted(item, object : DeleteItemCallback {
                override fun onSuccess(items: List<PostProduct>?) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val restoredProduct = item.copy(deleted = false, deletedAt = null)
                        localCache.updateProduct(restoredProduct)
                    }
                    callback.onSuccess(items)
                }
                override fun onFailure(message: String) = callback.onFailure(message)
                override fun onComplete() = callback.onComplete()
            })
        } else {
            // ── CENÁRIO OFFLINE ──
            CoroutineScope(Dispatchers.IO).launch {
                val restoredProduct = item.copy(deleted = false, deletedAt = null)
                localCache.updateProduct(restoredProduct)
            }
            dataSource.restoreItemDeleted(item, callback)
            // Retorna o sucesso imediatamente para disparar o toast simples na UI
            callback.onSuccess(null)
            callback.onComplete()
        }
    }

    fun deletePermanently(item: PostProduct, callback: DeleteItemCallback) {
        val isOnline = NetworkUtils.isNetworkAvailable()

        if (isOnline) {
            // ── CENÁRIO ONLINE ──
            dataSource.deletePermanently(item, object : DeleteItemCallback {
                override fun onSuccess(items: List<PostProduct>?) {
                    CoroutineScope(Dispatchers.IO).launch {
                        localCache.remove(item.firestoreDocId)
                    }
                    callback.onSuccess(items)
                }
                override fun onFailure(message: String) = callback.onFailure(message)
                override fun onComplete() = callback.onComplete()
            })
        } else {
            // ── CENÁRIO OFFLINE ──
            CoroutineScope(Dispatchers.IO).launch {
                localCache.remove(item.firestoreDocId)
            }
            dataSource.deletePermanently(item, callback)
            // Retorna o sucesso imediatamente para disparar o toast simples na UI
            callback.onSuccess(null)
            callback.onComplete()
        }
    }
}