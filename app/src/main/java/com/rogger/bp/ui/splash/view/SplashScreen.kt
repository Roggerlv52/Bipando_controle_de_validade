package com.rogger.bp.ui.splash.view

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.rogger.bp.R
import com.rogger.bp.ui.splash.presentation.SplashState
import com.rogger.bp.ui.splash.presentation.SplashViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    // Oculta completamente a StatusBar e a NavigationBar (Immersive Fullscreen) na Splash
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            // Restaura as barras do sistema ao navegar para fora da Splash Screen
            val window = (context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Monitora o estado da Splash Screen e navega automaticamente assim que estiver pronto
    LaunchedEffect(state) {
        when (val currentState = state) {
            is SplashState.NavigateToHome -> {
                onNavigateToHome()
            }
            is SplashState.NavigateToLogin -> {
                onNavigateToLogin()
            }
            is SplashState.Error -> {
                // Caso ocorra erro ou timeout, aguarda 3 segundos adicionais antes de navegar
                delay(3000L)
                if (currentState.fallbackRoute == "Home") {
                    onNavigateToHome()
                } else {
                    onNavigateToLogin()
                }
            }
            SplashState.Loading -> {
                // Permanece exibindo a Splash Screen
            }
        }
    }

    // Única SplashScreen do aplicativo (código Compose 100% Imersivo)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF24333D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo centralizado no meio da tela
            Box(
                modifier = Modifier.wrapContentSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_bp_logo_small),
                    contentDescription = stringResource(R.string.cd_logo_bipando),
                    modifier = Modifier.size(280.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
