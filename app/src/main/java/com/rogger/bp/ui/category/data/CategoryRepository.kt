package com.rogger.bp.ui.category.data

import com.rogger.bp.data.model.PostCategory

class CategoryRepository(private val dataSource: PostCategoryDataSource) {
    fun create(product: PostCategory, callback: CategoryCallback) {
        dataSource.createdCategory(product, callback)
    }
    fun delete(post: PostCategory, callback: CategoryCallback) {
        dataSource.delete(post, callback)
    }
    fun upload(post: PostCategory, callback: CategoryCallback) {
        dataSource.upload(post, callback)
    }
}