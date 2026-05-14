package com.rogger.bp.ui.edit.data

import com.rogger.bp.data.model.PostProduct

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 19:53
 */
interface PostEditDataSource {
    fun updateProduct(produto: PostProduct, callback: EditCallback)
    fun deleteProduct(produto: PostProduct, callback: EditCallback)
    fun fetchProduct(productId: Int, callback: EditCallback)
}