package com.rogger.bp.ui.login.data

import com.rogger.bp.data.model.UserAuth

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 22:39
 */
interface LoginCallback {
    fun onSuccess(userAuth: UserAuth) // caso de sucesso
    fun onFailure(message : String) // para messagem de erro do cervidor
    fun onComplete()
}