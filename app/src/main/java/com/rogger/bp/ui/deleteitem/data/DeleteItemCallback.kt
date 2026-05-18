package com.rogger.bp.ui.deleteitem.data

import com.rogger.bp.data.model.PostProduct

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 18/05/2026
 * Hora: 09:36
 */
interface DeleteItemCallback {
 fun onSuccess(items: List<PostProduct>? = null)
 fun onFailure(message: String)
 fun onComplete()
}