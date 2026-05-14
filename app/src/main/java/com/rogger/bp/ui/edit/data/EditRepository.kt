package com.rogger.bp.ui.edit.data

import com.rogger.bp.data.model.PostProduct

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 20:03
 */
class EditRepository(private val dataSource: PostEditDataSource) {

    /**
     * Actualiza os campos do [produto] no Firestore.
     */
    fun update(produto: PostProduct, callback: EditCallback) {
        dataSource.updateProduct(produto, callback)
    }

    /**
     * Soft-delete: move [produto] para a lixeira (deleted=true).
     */
    fun delete(produto: PostProduct, callback: EditCallback) {
        dataSource.deleteProduct(produto, callback)
    }

    /**
     * Busca um produto pelo [productId] (id local Room) no Firestore.
     */
    fun fetch(productId: Int, callback: EditCallback) {
        dataSource.fetchProduct(productId, callback)
    }
}