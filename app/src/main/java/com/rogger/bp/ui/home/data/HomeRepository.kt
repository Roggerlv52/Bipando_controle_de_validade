package com.rogger.bp.ui.home.data

import com.google.firebase.firestore.ListenerRegistration
import com.rogger.bp.data.database.RoomProductCache
import com.rogger.bp.data.model.PostProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 13/05/2026
 * Hora: 22:33
 */
class HomeRepository(
    private val remoteDataSource: PostHomeDataSource,
    private val localCache: RoomProductCache
) {

    private var productsListenerRegistration: ListenerRegistration? = null
    private var syncJob: Job? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private var currentTargetGroupId: String? = null
    private var currentWorkMode: Int = -1

    fun isSyncing(): Boolean = productsListenerRegistration != null

    fun fetchAll(callback: FetchProductsCallback, forceRefresh: Boolean = false, workMode: Int = 0, groupId: String? = null) {
        repositoryScope.launch {
            val targetGroupId = if (workMode == 1) groupId else null
            
            // Se o modo é GRUPO mas não temos groupId ainda, não inicia fetch individual
            if (workMode == 1 && targetGroupId == null) {
                android.util.Log.w("HomeRepository", "FetchAll: Modo grupo ativo mas groupId ainda não disponível")
                callback.onComplete()
                return@launch
            }

            // Se já estamos sincronizando exatamente o que foi pedido, não reinicia
            if (!forceRefresh && productsListenerRegistration != null && 
                currentTargetGroupId == targetGroupId && currentWorkMode == workMode) {
                callback.onComplete()
                return@launch
            }

            syncJob?.cancel()
            syncJob = repositoryScope.launch {
                currentTargetGroupId = targetGroupId
                currentWorkMode = workMode

                // 1. Mostrar dados do cache local imediatamente
                localCache.getAllProductsFlow(targetGroupId ?: "").firstOrNull()?.let { cached ->
                    if (cached.isNotEmpty()) callback.onSuccess(cached)
                }

                stopListeningForProducts()
                
                productsListenerRegistration = remoteDataSource.addProductsSnapshotListener(
                    targetGroupId,
                    object : FetchProductsCallback {
                        override fun onSuccess(products: List<PostProduct>) {
                            repositoryScope.launch {
                                localCache.replaceAllProductsByGroup(targetGroupId ?: "", products)
                                callback.onComplete()
                            }
                        }

                        override fun onFailure(message: String) {
                            callback.onFailure(message)
                        }

                        override fun onComplete() {}
                    }
                )
            }
        }
    }

    fun delete(product: PostProduct, callback: HomeCallback) {
        remoteDataSource.deleteProduct(product, object : HomeCallback {
            override fun onSuccess(p: PostProduct) {
                CoroutineScope(Dispatchers.IO).launch {

                    val deletedProduct =
                        p.copy(deleted = true, deletedAt = System.currentTimeMillis())
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
    }

    fun stopListeningForProducts() {
        productsListenerRegistration?.remove()
        productsListenerRegistration = null
        syncJob?.cancel()
    }

    suspend fun clearLocalCache() {
        localCache.clear()
    }

    fun getCachedProductsFlow(groupId: String = ""): Flow<List<PostProduct>> {
        return localCache.getAllProductsFlow(groupId)
    }
}
