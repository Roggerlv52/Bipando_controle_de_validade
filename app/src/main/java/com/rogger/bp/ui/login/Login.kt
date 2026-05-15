package com.rogger.bp.ui.login

import com.rogger.bp.data.model.UserAuth
import com.rogger.bp.ui.base.BasePresenter
import com.rogger.bp.ui.base.BaseView

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 22:37
 */
interface Login {
    interface Presenter : BasePresenter {
        fun loginWithGoogle(idToken: String,email : String)
        fun checkSession(): Boolean
    }

    interface View : BaseView<Presenter> {
        fun showProgress(enabled: Boolean)
        fun startGoogleSignIn()
        fun onUserAuthenticated(userAuth: UserAuth)
        fun onUserUnauthenticated(message: String)
    }
}