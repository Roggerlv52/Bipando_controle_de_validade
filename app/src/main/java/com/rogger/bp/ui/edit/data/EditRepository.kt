package com.rogger.bp.ui.edit.data

import com.rogger.bp.data.model.PostProduct

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 20:03
 */
class EditRepository(private val dataSource: PostEditDataSource) {

    fun update(produto: PostProduct, callback: EditCallback) {
        dataSource.updateProduct(produto, callback)
    }

    fun delete(produto: PostProduct, callback: EditCallback) {
        dataSource.deleteProduct(produto, callback)
    }

    fun fetch(productId: Int, callback: EditCallback) {
        dataSource.fetchProduct(productId, callback)
    }
}