package com.rogger.bp.ui.login.data

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 22:42
 */
class LoginRepository(private val dataSource: LoginDataSource) {

    fun loginWithGoogle(idToken: String, email: String, callback: LoginCallback) {
        // idToken é passado como 'email' para respeitar a interface existente
        dataSource.login(idToken, email, callback = callback)
    }

}