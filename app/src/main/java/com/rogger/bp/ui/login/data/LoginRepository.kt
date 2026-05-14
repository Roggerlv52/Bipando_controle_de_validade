package com.rogger.bp.ui.login.data

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 22:42
 */
class LoginRepository(private val dataSource : LoginDataSource) {
    fun logon(email : String, name:String,callback : LoginCallback){
        dataSource.login(email,name,callback)
    }
}