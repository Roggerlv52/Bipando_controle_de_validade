package com.rogger.bp.data.database

import com.rogger.bp.data.dao.Cache
import com.rogger.bp.data.dao.ProductDao
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

    override suspend fun remove(key: String) {
        if (key.isNotEmpty()) {
            productDao.removeCachedProduct(key) // Tenta remover um produto específico
        } else {
            productDao.clearAllProducts()
        }
    }

    suspend fun replaceAllProducts(products: List<PostProduct>) {
        productDao.clearAllProducts()
        productDao.putAllProducts(products)
    }

    override suspend fun clear() {
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
