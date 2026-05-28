package com.rogger.bp.ui.login.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.rogger.bp.MainActivity
import com.rogger.bp.R
import com.rogger.bp.data.model.UserAuth
import com.rogger.bp.databinding.ActivityLoginBinding
import com.rogger.bp.ui.base.BaseActivity
import com.rogger.bp.ui.commun.DependencyInjector
import com.rogger.bp.ui.commun.SharedPreferencesManager
import com.rogger.bp.ui.login.Login
import com.rogger.bp.ui.login.presentation.LoginPresenter

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 22:48
 */
class LoginActivity : BaseActivity(), Login.View {

    companion object {
        private const val TAG = "LoginActivity"
        private const val SLIDESHOW_DELAY = 2000L
    }

    override lateinit var presenter: Login.Presenter
    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            handleGoogleResult(result)
        }

    private var currentIndex = 0
    private val imageArray = intArrayOf(
       // R.drawable.bp_logo,
        R.drawable.picture_2,
        R.drawable.picture_3,
        R.drawable.picture_4,
        R.drawable.picture_1
    )
    private val handler = Handler(Looper.getMainLooper())
    private val slideshowRunnable = object : Runnable {
        override fun run() {
            showNextImage()
            handler.postDelayed(this, SLIDESHOW_DELAY)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.navigationBarColor = Color.TRANSPARENT

        presenter = LoginPresenter(this, DependencyInjector.loginRepository())

        // ✅ Correção: Ambos SharedPreferences e Firebase devem confirmar a sessão
        val isLoggedPref = SharedPreferencesManager.getLoginState(this, "state")
        val isLoggedFirebase = presenter.checkSession()

        if (isLoggedPref && isLoggedFirebase) {
            openMainActivity()
            return
        } else if (!isLoggedFirebase && isLoggedPref) {
            SharedPreferencesManager.setLoginState(this, "state", false)
            SharedPreferencesManager.clearUserInfo(this)
        }

        // ── Google Sign-In client ─────────────────────────────────────────
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ── Click no botão ────────────────────────────────────────────────
        binding.loginWithGmail.setOnClickListener {
            startGoogleSignIn()
        }

        startSlideshow()
    }

    override fun onDestroy() {
        handler.removeCallbacks(slideshowRunnable)
        if (::presenter.isInitialized) presenter.onDestroy()
        super.onDestroy()
    }

    override fun startGoogleSignIn() {
        showProgress(true)
        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun handleGoogleResult(result: ActivityResult) {
        try {
            val account = GoogleSignIn
                .getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)

            val idToken = account.idToken
            if (idToken == null) {
                showProgress(false)
                onUserUnauthenticated("Não foi possível obter o token do Google")
                return
            }

            Log.d(TAG, "idToken obtido — autenticando no Firebase")
            presenter.loginWithGoogle(idToken, account.email.toString())
        } catch (e: ApiException) {
            showProgress(false)
            onUserUnauthenticated("Falha no login com Google (código ${e.statusCode})")
        }
    }

    private fun startSlideshow() {
        handler.postDelayed(slideshowRunnable, SLIDESHOW_DELAY)
    }

    private fun showNextImage() {
        binding.imgActivityLogin.animate()
            .alpha(0f).setDuration(500)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (currentIndex >= imageArray.size) currentIndex = 0
                    binding.imgActivityLogin.setImageResource(imageArray[currentIndex++])
                    binding.imgActivityLogin.animate().alpha(1f).setDuration(500).setListener(null)
                }
            })
    }

    override fun showProgress(enabled: Boolean) {
        binding.progressbarLogin.visibility = if (enabled) View.VISIBLE else View.INVISIBLE
        binding.loginWithGmail.isEnabled = !enabled
    }

    override fun onUserAuthenticated(userAuth: UserAuth) {
        SharedPreferencesManager.saveUserInfo(
            this,
            userAuth.uuid,
            userAuth.name,
            userAuth.photoUri?.toString() ?: "",   // foto do Google — nunca null aqui
            userAuth.email
        )
        SharedPreferencesManager.setLoginState(this, "state", true)
        openMainActivity()
    }

    override fun onUserUnauthenticated(message: String) {
        showProgress(false)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        SharedPreferencesManager.clearUserInfo(this)
    }

    private fun openMainActivity() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }
}
