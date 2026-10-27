package com.rogger.bp.domain.usecase

import com.rogger.bp.domain.model.Product
import com.rogger.bp.domain.repository.ProductRepository

class SaveProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(product: Product) {
        repository.saveProduct(product)
    }
}
