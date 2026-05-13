package com.rogger.bp.ui.category.data

import com.rogger.bp.data.model.PostCategory

interface FetchCategoriesCallback {
    fun onSuccess(categories: List<PostCategory>)
    fun onFailure(message: String)
    fun onComplete()
}