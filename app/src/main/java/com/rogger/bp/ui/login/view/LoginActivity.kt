package com.rogger.bp.ui.login.view

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import com.rogger.bp.MainActivity
import com.rogger.bp.databinding.ActivityLoginBinding
import com.rogger.bp.ui.base.BaseActivity
import com.rogger.bp.ui.commun.DependencyInjector
import com.rogger.bp.ui.login.Login
import com.rogger.bp.ui.login.presentation.LoginPresenter

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 22:48
 */
class LoginActivity : BaseActivity(), Login.View {
    override lateinit var presenter: Login.Presenter
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = LoginPresenter(this,DependencyInjector.loginRepository())

    }

    override fun showProgress(enabled: Boolean) {

    }

    override fun onUserAuthenticated() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun onUserUnauthenticated(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}