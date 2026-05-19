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

    /**
     * Busca todos os produtos activos do utilizador, priorizando o cache local
     * e mantendo-o sincronizado com o Firestore via listener.
     */
    fun fetchAll(callback: FetchProductsCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            // 1. Tentar carregar do cache local imediatamente
            localCache.getAllProductsFlow().firstOrNull()?.let { cachedProducts ->
                if (cachedProducts.isNotEmpty()) {
                    callback.onSuccess(cachedProducts)
                }
            }

            // 2. Configurar listener do Firestore para atualizações em tempo real
            productsListenerRegistration = remoteDataSource.addProductsSnapshotListener(object : FetchProductsCallback {
                override fun onSuccess(products: List<PostProduct>) {
                    // Atualizar o cache local com os dados mais recentes do Firestore
                    CoroutineScope(Dispatchers.IO).launch {
                        localCache.putAllProducts(products)
                        // A UI já estará observando o Flow do Room, então não precisa chamar callback.onSuccess aqui novamente
                    }
                }

                override fun onFailure(message: String) {
                    callback.onFailure(message)
                }

                override fun onComplete() {
                    // onComplete não é chamado para listeners contínuos
                }
            })
        }
    }

    /**
     * Busca produtos filtrados por [categoryId], priorizando o cache local
     * e mantendo-o sincronizado com o Firestore via listener.
     * NOTA: Para esta função, o listener do Firestore precisaria ser adaptado
     * para filtrar por categoria, ou o filtro seria feito no lado do cliente após
     * o carregamento completo. Por simplicidade, vamos focar no fetchAll para o listener.
     * A implementação abaixo ainda usa o método original do remoteDataSource.
     */
    fun fetchByCategory(categoryId: Int, callback: FetchProductsCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            // Tentar carregar do cache local imediatamente
            localCache.getProductsByCategoryFlow(categoryId).firstOrNull()?.let { cachedProducts ->
                if (cachedProducts.isNotEmpty()) {
                    callback.onSuccess(cachedProducts)
                }
            }
            // Fallback para o remoteDataSource se o cache estiver vazio ou para garantir a atualização inicial
            remoteDataSource.fetchProductsByCategory(categoryId, callback)
        }
    }

    /**
     * Soft-delete: marca [product] como eliminado no Firestore e atualiza o cache.
     */
    fun delete(product: PostProduct, callback: HomeCallback) {
        remoteDataSource.deleteProduct(product, object : HomeCallback {
            override fun onSuccess(p: PostProduct) {
                CoroutineScope(Dispatchers.IO).launch {
                    // Atualiza o cache local após a operação remota
                    localCache.updateProduct(p) // O listener do Firestore também faria isso, mas é bom ter consistência imediata
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

    /**
     * Restaura um produto previamente eliminado no Firestore e atualiza o cache.
     */
    fun restore(product: PostProduct, callback: HomeCallback) {
        remoteDataSource.restoreProduct(product, object : HomeCallback {
            override fun onSuccess(p: PostProduct) {
                CoroutineScope(Dispatchers.IO).launch {
                    // Atualiza o cache local após a operação remota
                    localCache.updateProduct(p)
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

    // Métodos para operações de cache direto, se necessário
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
}