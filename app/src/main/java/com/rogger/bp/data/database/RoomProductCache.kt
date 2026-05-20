package com.rogger.bp.data.database

import com.rogger.bp.data.dao.Cache
import com.rogger.bp.data.dao.ProductDao
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct
import kotlinx.coroutines.flow.Flow

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 19/05/2026
 * Hora: 09:50
 */

class RoomProductCache(private val productDao: ProductDao) : Cache<List<PostProduct>> {

    override fun isCached(key: String): Boolean {
        // Para uma lista, podemos verificar se há algum item no cache
        return productDao.isAnyProductCached()
    }

    override fun get(key: String): List<PostProduct>? {
        // Retorna todos os produtos em cache
        return productDao.getAllCachedProducts()
    }

    override fun put(key: String, data: List<PostProduct>) {
        // Insere/atualiza todos os produtos na lista
        productDao.putAllProducts(data)
    }

    override fun remove(key: String) {
        // Para remover um item específico, a key precisaria ser o firestoreDocId
        // ou, se a key for para a lista toda, chamar clear()
        // Aqui, vamos assumir que 'remove' para a lista significa limpar tudo se a key for genérica
        // ou remover um item específico se a key for um firestoreDocId.
        // Para simplificar, se a key não for um docId, vamos limpar tudo.
        // Uma implementação mais robusta poderia ter um método remove(item: PostProduct)
        if (key.isNotEmpty()) {
            productDao.removeCachedProduct(key) // Tenta remover um produto específico
        } else {
            productDao.clearAllProducts()
        }
    }

    override fun clear() {
        productDao.clearAllProducts()
    }
    fun putAllProducts(categories: List<PostProduct>) {
        productDao.putAllProducts(categories)
    }

    fun getAllProductsFlow(): Flow<List<PostProduct>> {
        return productDao.getAllProducts()
    }

    fun getProductsByCategoryFlow(categoryId: Int): Flow<List<PostProduct>> {
        return productDao.getProductsByCategory(categoryId)
    }

    suspend fun insertProduct(product: PostProduct) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: PostProduct) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: PostProduct) {
        productDao.deleteProduct(product)
    }
}
