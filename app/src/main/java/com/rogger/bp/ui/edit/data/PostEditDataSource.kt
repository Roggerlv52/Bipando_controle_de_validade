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
    /**
     * Busca o produto pelo [docId] — o documentId do Firestore.
     *
     * Usando o docId em vez do campo "uid" porque:
     *  - O docId é SEMPRE único e nunca vazio (gerado pelo Firestore).
     *  - O campo "uid" pode estar vazio em produtos criados antes da
     *    implementação do uuid (ou se uuid não foi gerado no momento do add).
     *  - O [HomeDataSource] expõe o docId via [PostProduct.uuid] com fallback.
     */
    fun fetchProductByDocId(docId: String, callback: EditCallback)
}