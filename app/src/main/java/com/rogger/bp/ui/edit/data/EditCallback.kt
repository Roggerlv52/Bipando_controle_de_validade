package com.rogger.bp.ui.edit.data

import com.rogger.bp.data.model.PostProduct

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 19:52
 */
interface EditCallback {
    fun onSuccess(postProduct: PostProduct)
    fun onFailure(message: String)
    fun onComplete()
}