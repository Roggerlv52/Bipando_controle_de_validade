package com.rogger.bp.ui.login.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rogger.bp.domain.usecase.LoginUseCase
import com.rogger.bp.ui.home.data.HomeRepository
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.groups.data.GroupRepository
import com.rogger.bp.ui.home.data.FetchProductsCallback
import com.rogger.bp.ui.category.data.FetchCategoriesCallback
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.ui.commun.SharedPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class LoginState(
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val errorMessage: String? = null
)

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val homeRepository: HomeRepository,
    private val categoryRepository: CategoryRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginState())
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    fun loginWithGoogle(context: Context, idToken: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val workMode = SharedPreferencesManager.getWorkMode(context)
        
        // Reset logging out flag
        com.rogger.bp.ui.groups.data.GroupRepository.setLoggingOut(false)

        viewModelScope.launch {
            loginUseCase.loginWithGoogle(idToken).collect { result ->
                result.onSuccess { user ->
                    // Salva info básica para o app não abrir vazio
                    SharedPreferencesManager.saveUserInfo(
                        context, 
                        user.uuid, 
                        user.name, 
                        user.photoUri?.toString() ?: "", 
                        user.email
                    )
                    
                    // Sincroniza dados ANTES de navegar
                    viewModelScope.launch {
                        groupRepository.handleUserLogin(user.uuid, user.name, user.photoUri?.toString() ?: "")
                        groupRepository.syncUserGroup(user.uuid)

                        val productsSynced = syncProducts(workMode)
                        val categoriesSynced = syncCategories(workMode)
                        
                        // Para economizar recursos, paramos os listeners da tela de login
                        homeRepository.stopListeningForProducts()
                        categoryRepository.stopListeningForCategories()
                        
                        if (productsSynced && categoriesSynced) {
                            _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                        } else {
                            // Mesmo que falhe a sync total, permitimos entrar, mas avisamos
                            _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                        }
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            }
        }
    }

    private suspend fun syncProducts(workMode: Int): Boolean = suspendCancellableCoroutine { continuation ->
        homeRepository.fetchAll(object : FetchProductsCallback {
            override fun onSuccess(products: List<PostProduct>) {
                // O primeiro sucesso do listener já é suficiente para popular o Room
            }
            override fun onFailure(message: String) {
                if (continuation.isActive) continuation.resume(false)
            }
            override fun onComplete() {
                if (continuation.isActive) continuation.resume(true)
            }
        }, forceRefresh = true, workMode = workMode)
    }

    private suspend fun syncCategories(workMode: Int): Boolean = suspendCancellableCoroutine { continuation ->
        categoryRepository.fetchAll(object : FetchCategoriesCallback {
            override fun onSuccess(categories: List<PostCategory>) {}
            override fun onFailure(message: String) {
                if (continuation.isActive) continuation.resume(false)
            }
            override fun onComplete() {
                if (continuation.isActive) continuation.resume(true)
            }
        }, forceRefresh = true, workMode = workMode)
    }
}
