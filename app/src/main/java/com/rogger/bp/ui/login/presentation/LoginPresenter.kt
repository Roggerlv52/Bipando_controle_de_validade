package com.rogger.bp.ui.login.presentation

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

    // ── 1. Autenticar com Google ──────────────────────────────────────────

    override fun loginWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            view?.onUserUnauthenticated("Token inválido. Tente novamente.")
            return
        }

        view?.showProgress(true)

        repository.loginWithGoogle(idToken, object : LoginCallback {

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

    // ── 2. Verificar sessão activa ────────────────────────────────────────

    override fun checkSession(): Boolean {
        return auth.currentUser != null
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onDestroy() {
        view = null
    }
}