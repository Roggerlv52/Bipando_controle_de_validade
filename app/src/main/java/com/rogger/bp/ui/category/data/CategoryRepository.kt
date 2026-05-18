package com.rogger.bp.ui.category.data

import androidx.lifecycle.LiveData
import com.rogger.bp.data.database.FirebaseDataSource
import com.rogger.bp.data.model.PostCategory

class CategoryRepository(private val dataSource: PostCategoryDataSource) {
    private val firebaseDataSource = FirebaseDataSource.getInstance()
    fun create(category: PostCategory, callback: CategoryCallback) {
        dataSource.createCategory(category, callback)
    }

    fun update(category: PostCategory, callback: CategoryCallback) {
        dataSource.updateCategory(category, callback)
    }

    fun delete(category: PostCategory, callback: CategoryCallback) {
        dataSource.deleteCategory(category, callback)
    }

    fun fetchAll(callback: FetchCategoriesCallback) {
        dataSource.fetchCategories(callback)
    }

    fun getCountCategorias() {
    }
}