package com.rogger.bp.ui.login

import com.rogger.bp.ui.base.BasePresenter
import com.rogger.bp.ui.base.BaseView

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 22:37
 */
interface Login {
    interface Presenter : BasePresenter {
        fun login(email: String, name: String)
    }

    interface View : BaseView<Presenter> {
        fun showProgress(enabled: Boolean)
        fun onUserAuthenticated()
        fun onUserUnauthenticated(message: String)
    }
}