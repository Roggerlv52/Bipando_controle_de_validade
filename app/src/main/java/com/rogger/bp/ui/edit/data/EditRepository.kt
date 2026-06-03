package com.rogger.bp.ui.edit.data

import com.rogger.bp.data.database.RoomProductCache
import com.rogger.bp.data.model.PostProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 20:03
 */
class EditRepository(
    private val dataSource: PostEditDataSource,
    private val localCache: RoomProductCache
) {

    fun update(produto: PostProduct, callback: EditCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            localCache.updateProduct(produto)
        }

        // 2. Executa a gravação no Firestore em segundo plano
        dataSource.updateProduct(produto, callback)

        // 3. Dispara o callback de sucesso imediatamente para liberar a UI
        callback.onSuccess(produto)
        callback.onComplete()
    }

    fun delete(produto: PostProduct, callback: EditCallback) {
        //dataSource.deleteProduct(produto, callback)
        dataSource.deleteProduct(produto, object : EditCallback {
            override fun onSuccess(p: PostProduct) {
                CoroutineScope(Dispatchers.IO).launch {
                    // Remove do Room imediatamente após o sucesso no Firestore
                    localCache.remove(p.firestoreDocId)
                }
                callback.onSuccess(p)
            }

            override fun onFailure(message: String) {
                callback.onFailure(message)
            }

            override fun onComplete() {
                callback.onComplete()
            }
        })
    }

    fun fetchByDocId(docId: String, callback: EditCallback) {
        dataSource.fetchProductByDocId(docId, callback)
    }
}