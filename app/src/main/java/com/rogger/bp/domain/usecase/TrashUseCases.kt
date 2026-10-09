package com.rogger.bp.domain.usecase

import com.rogger.bp.domain.model.Product
import com.rogger.bp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class GetDeletedProductsUseCase(private val repository: ProductRepository) {
    operator fun invoke(): Flow<List<Product>> = repository.getDeletedProducts()
}

class RestoreProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(product: Product) = repository.restoreProduct(product)
}

class PermanentDeleteProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(product: Product) = repository.permanentDeleteProduct(product)
}
