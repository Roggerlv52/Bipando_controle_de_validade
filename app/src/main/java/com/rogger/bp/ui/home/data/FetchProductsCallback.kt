package com.rogger.bp.ui.home.data

import com.rogger.bp.data.model.PostProduct

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 19:17
 */

/**
 * Callback dedicado à busca de lista de produtos do Firestore.
 * Espelha [FetchCategoriesCallback] do módulo category.
 */
interface FetchProductsCallback {
 fun onSuccess(products: List<PostProduct>)
 fun onFailure(message: String)
 fun onComplete()
}