package com.rogger.bp.ui.login.data

import android.content.Context

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 22:42
 */
class LoginRepository(private val dataSource: LoginDataSource) {

    fun loginWithGoogle(context: Context,idToken: String, email: String, callback: LoginCallback) {
        dataSource.login(context,idToken, email, callback = callback)
    }

}