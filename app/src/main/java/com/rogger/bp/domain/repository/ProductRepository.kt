package com.rogger.bp.domain.repository

import com.rogger.bp.domain.model.Product
import com.rogger.bp.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(): Flow<List<Product>>
    fun getDeletedProducts(): Flow<List<Product>>
    fun getProductByUuid(uuid: String): Flow<Product?>
    fun getProductsByCategory(categoryId: String): Flow<List<Product>>
    fun searchProducts(query: String): Flow<List<Product>>
    fun getCategories(): Flow<List<Category>>
    suspend fun saveCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun saveProduct(product: Product)
    suspend fun deleteProduct(product: Product)
    suspend fun restoreProduct(product: Product)
    suspend fun permanentDeleteProduct(product: Product)
    suspend fun getTotalProductsCount(): Flow<Int>
}
