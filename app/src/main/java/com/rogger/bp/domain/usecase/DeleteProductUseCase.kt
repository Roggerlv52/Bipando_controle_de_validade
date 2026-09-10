package com.rogger.bp.domain.usecase

import com.rogger.bp.domain.model.Product
import com.rogger.bp.domain.repository.ProductRepository

class DeleteProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(product: Product) {
        repository.deleteProduct(product)
    }
}
