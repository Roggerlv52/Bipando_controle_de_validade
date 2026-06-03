package com.rogger.bp.ui.deleteitem.data

import com.rogger.bp.data.database.RoomProductCache
import com.rogger.bp.data.image.datasource.UserImageDataSource
import com.rogger.bp.data.model.PostProduct
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
    fun fetchItemDeleted(callback: DeleteItemCallback) {
        dataSource.fetchItemDeleted(callback)
    }

    fun restore(item: PostProduct, callback: DeleteItemCallback) {
        dataSource.restoreItemDeleted(item, callback)
    }

    fun deletePermanently(item: PostProduct, callback: DeleteItemCallback) {
        dataSource.deletePermanently(item, object : DeleteItemCallback {
            override fun onSuccess(items: List<PostProduct>?) {

                CoroutineScope(Dispatchers.IO).launch {
                    // Remove permanentemente o registro do Room local
                    localCache.remove(item.firestoreDocId)
                    UserImageDataSource().removeUserImage(item.barcode)
                }
                callback.onSuccess(items)
            }

            override fun onFailure(message: String) {
                callback.onFailure(message)
            }

            override fun onComplete() {
                callback.onComplete()
            }
        })
    }
}