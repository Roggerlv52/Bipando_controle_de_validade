package com.rogger.bp.ui.login.presentation

import com.rogger.bp.data.model.UserAuth
import com.rogger.bp.ui.login.Login
import com.rogger.bp.ui.login.data.LoginCallback
import com.rogger.bp.ui.login.data.LoginRepository

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 23:24
 */
class LoginPresenter(
    private var view: Login.View?,
    private var repository: LoginRepository
) : Login.Presenter {
    override fun login(email: String, name: String) {
        repository.logon(email, name, object : LoginCallback {
            override fun onSuccess(userAuth: UserAuth) {
                view?.onUserAuthenticated()
            }

            override fun onFailure(message: String) {
                view?.onUserUnauthenticated(message)
            }
            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }
    override fun onDestroy() {
        view = null
    }
}