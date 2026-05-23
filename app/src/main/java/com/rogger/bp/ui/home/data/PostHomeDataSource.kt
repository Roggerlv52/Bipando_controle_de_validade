package com.rogger.bp.ui.home.data

import com.google.firebase.firestore.ListenerRegistration
import com.rogger.bp.data.model.PostProduct

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 13/05/2026
 * Hora: 22:35
 */
interface PostHomeDataSource {
    fun fetchProducts(callback: FetchProductsCallback)
    fun fetchProductsByCategory(categoryId: String, callback: FetchProductsCallback)
    fun deleteProduct(product: PostProduct, callback: HomeCallback)
    fun restoreProduct(product: PostProduct, callback: HomeCallback)

    fun addProductsSnapshotListener(callback: FetchProductsCallback): ListenerRegistration?
}