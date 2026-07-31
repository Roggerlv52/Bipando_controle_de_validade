package com.rogger.bp.domain.usecase

import com.rogger.bp.domain.model.Product
import com.rogger.bp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class GetProductsUseCase(private val repository: ProductRepository) {
    operator fun invoke(categoryId: String? = null, query: String? = null): Flow<List<Product>> {
        return when {
            !query.isNullOrBlank() -> repository.searchProducts(query)
            categoryId != null -> repository.getProductsByCategory(categoryId)
            else -> repository.getProducts()
        }
    }
}

class SaveProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(product: Product) {
        repository.saveProduct(product)
    }
}
