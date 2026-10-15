package com.rogger.bp.domain.usecase

import com.rogger.bp.domain.model.Product
import com.rogger.bp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class GetProductByUuidUseCase(private val repository: ProductRepository) {
    operator fun invoke(uuid: String): Flow<Product?> {
        return repository.getProductByUuid(uuid)
    }
}
