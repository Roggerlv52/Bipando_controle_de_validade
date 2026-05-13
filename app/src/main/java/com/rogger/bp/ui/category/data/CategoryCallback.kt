package com.rogger.bp.ui.category.data

interface CategoryCallback {
    fun onSuccess()
    fun onFailure(message: String)
    fun onComplete()
}