package com.rogger.bp.ui.login.data

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 22:41
 */
interface LoginDataSource {
 fun login(idTokesn : String, email : String, callback: LoginCallback)
}