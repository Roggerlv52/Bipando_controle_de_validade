package com.rogger.bp.ui.login.presentation

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.rogger.bp.R
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

    override fun loginWithGoogle(context: Context,idToken: String,email : String) {

        if (idToken.isBlank()) {
            view?.onUserUnauthenticated(context.getString(
                R.string.toast_msg_error_google_invalid_token)
            )
            return
        }

        view?.showProgress(true)

        repository.loginWithGoogle(context,idToken, email,object : LoginCallback {
            override fun onSuccess(userAuth: UserAuth) {
                view?.onUserAuthenticated(userAuth)
                Log.e("login_google"," ${userAuth.name} \n ${userAuth.email}")
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