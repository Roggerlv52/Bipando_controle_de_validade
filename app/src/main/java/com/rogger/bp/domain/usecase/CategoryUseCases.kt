package com.rogger.bp.domain.usecase

import com.rogger.bp.domain.model.Category
import com.rogger.bp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class GetCategoriesUseCase(private val repository: ProductRepository) {
    operator fun invoke(): Flow<List<Category>> = repository.getCategories()
}

class SaveCategoryUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(category: Category) {
        repository.saveCategory(category)
    }
}

class DeleteCategoryUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(category: Category) {
        repository.deleteCategory(category)
    }
}
