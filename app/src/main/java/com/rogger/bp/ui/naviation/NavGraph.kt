package com.rogger.bp.ui.naviation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.auth.FirebaseAuth
import com.rogger.bp.data.repository.AuthRepositoryImpl
import com.rogger.bp.domain.usecase.LoginUseCase
import com.rogger.bp.ui.login.presentation.LoginViewModel
import com.rogger.bp.ui.login.view.LoginScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Composable
fun BipandoNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            val authRepository = AuthRepositoryImpl(FirebaseAuth.getInstance())
            val loginUseCase = LoginUseCase(authRepository)
            
            val viewModel: LoginViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return LoginViewModel(loginUseCase) as T
                    }
                }
            )

            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.REGISTER) {
            // Placeholder para a próxima etapa
        }

        composable(Routes.HOME) {
            // Placeholder - aqui será a integração com a Home legada ou nova
        }
    }
}
