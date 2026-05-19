package com.rogger.bp.ui.category.data

import com.google.firebase.firestore.ListenerRegistration
import com.rogger.bp.data.model.PostCategory

interface PostCategoryDataSource {
    fun createCategory(category: PostCategory, callback: CategoryCallback)
    fun updateCategory(category: PostCategory, callback: CategoryCallback)
    fun deleteCategory(category: PostCategory, callback: CategoryCallback)
    fun fetchCategories(callback: FetchCategoriesCallback)

    fun addCategoriesSnapshotListener(callback: FetchCategoriesCallback): ListenerRegistration?
}