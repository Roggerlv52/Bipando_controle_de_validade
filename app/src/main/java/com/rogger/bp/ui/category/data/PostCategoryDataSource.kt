package com.rogger.bp.ui.category.data

import com.rogger.bp.data.model.PostCategory

interface PostCategoryDataSource {
    fun createdCategory(postCategory: PostCategory,callback: CategoryCallback)
    fun upload(category: PostCategory,callback: CategoryCallback)
    fun delete(category: PostCategory,callback: CategoryCallback)
}