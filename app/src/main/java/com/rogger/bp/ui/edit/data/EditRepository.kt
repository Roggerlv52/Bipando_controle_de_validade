package com.rogger.bp.ui.edit.data

import com.rogger.bp.data.database.RoomProductCache
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.commun.NetworkUtils
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
        val isOnline = NetworkUtils.isNetworkAvailable()

        if (isOnline) {
            // ── CENÁRIO ONLINE ──────────────────────────────────────────────
            // Espera o upload e a gravação remota terminarem. Só grava no Room
            // após receber a URL pública definitiva vinda do Firestore.
            dataSource.updateProduct(produto, object : EditCallback {
                override fun onSuccess(p: PostProduct) {
                    CoroutineScope(Dispatchers.IO).launch {
                        localCache.updateProduct(p) // Atualiza o Room com a URL final https://
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
        } else {
            // ── CENÁRIO OFFLINE ─────────────────────────────────────────────
            // Salva instantaneamente usando a URI local para manter o funcionamento offline.
            CoroutineScope(Dispatchers.IO).launch {
                localCache.updateProduct(produto)
            }
            dataSource.updateProduct(produto, callback)
            callback.onSuccess(produto)
            callback.onComplete()
        }
    }

    fun delete(produto: PostProduct, callback: EditCallback) {
        val isOnline = NetworkUtils.isNetworkAvailable()

        if (isOnline) {
            // ── CENÁRIO ONLINE ──────────────────────────────────────────────
            dataSource.deleteProduct(produto, object : EditCallback {
                override fun onSuccess(p: PostProduct) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val deletedProduct = p.copy(deleted = true, deletedAt = System.currentTimeMillis())
                        localCache.updateProduct(deletedProduct)
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
        } else {
            // ── CENÁRIO OFFLINE ─────────────────────────────────────────────
            CoroutineScope(Dispatchers.IO).launch {
                // 👉 CORREÇÃO: Trocado 'p.copy' por 'produto.copy' para compilar com sucesso
                val deletedProduct = produto.copy(deleted = true, deletedAt = System.currentTimeMillis())
                localCache.updateProduct(deletedProduct)
            }
            dataSource.deleteProduct(produto, callback)
            callback.onSuccess(produto)
            callback.onComplete()
        }
    }
    fun fetchByDocId(docId: String, callback: EditCallback) {
        dataSource.fetchProductByDocId(docId, callback)
    }
}