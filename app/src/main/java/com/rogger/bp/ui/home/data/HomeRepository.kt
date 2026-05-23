package com.rogger.bp.ui.home.data

import com.google.firebase.firestore.ListenerRegistration
import com.rogger.bp.data.database.RoomProductCache
import com.rogger.bp.data.model.PostProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    fun fetchAll(callback: FetchProductsCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            // 1. Mostrar dados do cache imediatamente (se houver)
            localCache.getAllProductsFlow().firstOrNull()?.let { cachedProducts ->
                if (cachedProducts.isNotEmpty()) {
                    callback.onSuccess(cachedProducts)
                }
            }

            // 2. Listener do Firestore — snapshot contém SOMENTE deleted=false
            productsListenerRegistration =
                remoteDataSource.addProductsSnapshotListener(object : FetchProductsCallback {
                    override fun onSuccess(products: List<PostProduct>) {
                        CoroutineScope(Dispatchers.IO).launch {
                            localCache.replaceAllProducts(products)
                        }
                    }

                    override fun onFailure(message: String) {
                        callback.onFailure(message)
                    }

                    override fun onComplete() {}
                })
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

    fun restore(product: PostProduct, callback: HomeCallback) {
        remoteDataSource.restoreProduct(product, object : HomeCallback {
            override fun onSuccess(p: PostProduct) {
                CoroutineScope(Dispatchers.IO).launch {
                    // Marca deleted=false no Room para restaurar na lista imediatamente
                    val restoredProduct = p.copy(deleted = false, deletedAt = null)
                    localCache.updateProduct(restoredProduct)
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
    }

    suspend fun insertProductIntoCache(product: PostProduct) {
        localCache.insertProduct(product)
    }

    suspend fun updateProductInCache(product: PostProduct) {
        localCache.updateProduct(product)
    }

    suspend fun deleteProductFromCache(product: PostProduct) {
        localCache.deleteProduct(product)
    }

    fun getCachedProductsFlow(): Flow<List<PostProduct>> {
        return localCache.getAllProductsFlow()
    }

    fun getCachedProductsByCategoryFlow(i: String): Flow<List<PostProduct>> {
        return localCache.getProductsByCategoryFlow(i)
    }
}
