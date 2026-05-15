package com.rogger.bp.ui.login.presentation

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
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
    private val repository: LoginRepository
) : Login.Presenter {

    private val auth = FirebaseAuth.getInstance()

    override fun loginWithGoogle(idToken: String,email : String) {

        if (idToken.isBlank()) {
            view?.onUserUnauthenticated("Token inválido. Tente novamente.")
            return
        }

        view?.showProgress(true)

        repository.loginWithGoogle(idToken, email,object : LoginCallback {
            override fun onSuccess(userAuth: UserAuth) {
                view?.onUserAuthenticated(userAuth)
            }

            override fun onFailure(message: String) {
                view?.onUserUnauthenticated(message)
            }

            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }

    override fun checkSession(): Boolean {
        var b = false
        if (auth.currentUser != null) {
            b = true
        }
        return b
    }

    override fun onDestroy() {
        view = null
    }
}