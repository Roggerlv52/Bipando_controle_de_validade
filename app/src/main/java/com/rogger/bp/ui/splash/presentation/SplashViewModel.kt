package com.rogger.bp.ui.splash.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rogger.bp.BuildConfig
import com.rogger.bp.data.database.BpDatabase
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.category.data.FetchCategoriesCallback
import com.rogger.bp.ui.commun.DependencyInjector
import com.rogger.bp.ui.commun.SharedPreferencesManager
import com.rogger.bp.ui.home.data.FetchProductsCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

// Estados da Splash Screen
sealed interface SplashState {
    object Loading : SplashState
    object NavigateToHome : SplashState
    object NavigateToLogin : SplashState
    data class Error(val fallbackRoute: String) : SplashState
}

class SplashViewModel(
    context: Context
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow<SplashState>(SplashState.Loading)
    val uiState: StateFlow<SplashState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val groupRepository = DependencyInjector.registerGroupRepository(appContext)
    private val categoryRepository = DependencyInjector.registerCategoryRepository(appContext)
    private val homeRepository = DependencyInjector.registerHomeRepository(appContext)

    init {
        startInitialization()
    }

    private fun startInitialization() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            var isUserLoggedIn = false
            var hasError = false

            try {
                // Limite de tempo máximo para toda a inicialização e pré-carregamento dos dados
                withTimeout(9000L.milliseconds) {
                    coroutineScope {
                        // 1. Inicializa Firebase App Check em background
                        val appCheckJob = async(Dispatchers.IO) {
                            try {
                                val firebaseAppCheck = FirebaseAppCheck.getInstance()
                                if (BuildConfig.DEBUG) {
                                    firebaseAppCheck.installAppCheckProviderFactory(
                                        DebugAppCheckProviderFactory.getInstance()
                                    )
                                } else {
                                    firebaseAppCheck.installAppCheckProviderFactory(
                                        PlayIntegrityAppCheckProviderFactory.getInstance()
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("SplashViewModel", "Erro ao inicializar Firebase App Check", e)
                            }
                        }

                        // 2. Inicializa e verifica o banco de dados Room em background
                        val roomJob = async(Dispatchers.IO) {
                            try {
                                val database = BpDatabase.getDatabase(appContext)
                                database.productDao().getGlobalTotalProductsCount()
                            } catch (e: Exception) {
                                Log.e("SplashViewModel", "Erro ao inicializar banco de dados Room", e)
                            }
                        }

                        // 3. Pré-carregamento completo dos dados (Perfil, Grupos, Categorias e Produtos) antes de abrir a Home
                        val dataPreloadJob = async(Dispatchers.IO) {
                            try {
                                val currentUser = auth.currentUser
                                if (currentUser != null) {
                                    isUserLoggedIn = true

                                    // A. Carrega e salva as informações básicas do perfil
                                    try {
                                        val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                                        if (userDoc.exists()) {
                                            val name = userDoc.getString("name") ?: currentUser.displayName ?: ""
                                            val email = userDoc.getString("email") ?: currentUser.email ?: ""
                                            val photoUrl = userDoc.getString("photoUrl") ?: currentUser.photoUrl?.toString() ?: ""
                                            val isPremium = userDoc.getBoolean("isPremium") ?: false

                                            SharedPreferencesManager.saveUserInfo(appContext, currentUser.uid, name, photoUrl, email)
                                            SharedPreferencesManager.setPremiumState(appContext, isPremium)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SplashViewModel", "Erro ao buscar perfil do Firestore", e)
                                    }

                                    // B. Sincroniza grupos e modo de trabalho do usuário
                                    var effectiveWorkMode = SharedPreferencesManager.getWorkMode(appContext)
                                    var effectiveGroupId: String? = SharedPreferencesManager.getActiveGroupId(appContext)

                                    try {
                                        val groupResult = groupRepository.handleUserLogin(
                                            currentUser.uid,
                                            currentUser.displayName ?: "",
                                            currentUser.photoUrl?.toString() ?: ""
                                        )
                                        groupRepository.syncUserGroup(currentUser.uid)

                                        groupResult.onSuccess { group ->
                                            if (group.isDefault && effectiveWorkMode == 0) {
                                                SharedPreferencesManager.setWorkMode(appContext, 1)
                                                SharedPreferencesManager.setActiveGroupId(appContext, group.groupId)
                                                effectiveWorkMode = 1
                                                effectiveGroupId = group.groupId
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SplashViewModel", "Erro ao sincronizar grupos do usuário", e)
                                    }

                                    // C. Pré-carrega Categorias para o Room em background
                                    try {
                                        suspendCancellableCoroutine<Unit> { continuation ->
                                            categoryRepository.fetchAll(object : FetchCategoriesCallback {
                                                override fun onSuccess(categories: List<PostCategory>) {}
                                                override fun onFailure(message: String) {
                                                    if (continuation.isActive) continuation.resume(Unit)
                                                }
                                                override fun onComplete() {
                                                    if (continuation.isActive) continuation.resume(Unit)
                                                }
                                            }, forceRefresh = true, workMode = effectiveWorkMode, groupId = effectiveGroupId)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SplashViewModel", "Erro ao pré-carregar categorias", e)
                                    }

                                    // D. Pré-carrega Produtos para o Room em background antes da navegação para a Home
                                    try {
                                        suspendCancellableCoroutine<Unit> { continuation ->
                                            homeRepository.fetchAll(object : FetchProductsCallback {
                                                override fun onSuccess(products: List<PostProduct>) {}
                                                override fun onFailure(message: String) {
                                                    if (continuation.isActive) continuation.resume(Unit)
                                                }
                                                override fun onComplete() {
                                                    if (continuation.isActive) continuation.resume(Unit)
                                                }
                                            }, forceRefresh = true, workMode = effectiveWorkMode, groupId = effectiveGroupId)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SplashViewModel", "Erro ao pré-carregar produtos", e)
                                    }

                                    // E. Leitura inicial garantida do cache do Room para assegurar emissão antes de navegar para a Home
                                    try {
                                        val produtosLocais = homeRepository
                                            .getCachedProductsFlow(effectiveGroupId ?: "")
                                            .firstOrNull()
                                        Log.d("SplashViewModel", "Pré-carregamento concluído. Produtos locais no Room: ${produtosLocais?.size ?: 0}")
                                    } catch (e: Exception) {
                                        Log.e("SplashViewModel", "Erro ao verificar produtos locais no Room", e)
                                    }
                                } else {
                                    isUserLoggedIn = false
                                }
                            } catch (e: Exception) {
                                Log.e("SplashViewModel", "Erro ao pré-carregar dados na Splash Screen", e)
                            }
                        }

                        // Executa inicializações de sistema e o pré-carregamento dos dados em paralelo
                        awaitAll(appCheckJob, roomJob, dataPreloadJob)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.w("SplashViewModel", "Timeout atingido na Splash Screen")
                hasError = true
            } catch (e: Exception) {
                Log.e("SplashViewModel", "Erro geral na inicialização da Splash Screen", e)
                hasError = true
            }

            // Garante o tempo mínimo de exibição da Splash Screen de 1500ms
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 1500L) {
                delay((1500L - elapsedTime).milliseconds)
            }

            // Determina o próximo estado de navegação com base no resultado e login do usuário
            if (hasError) {
                val fallback = if (auth.currentUser != null) "Home" else "Login"
                _uiState.value = SplashState.Error(fallback)
            } else {
                if (isUserLoggedIn) {
                    _uiState.value = SplashState.NavigateToHome
                } else {
                    _uiState.value = SplashState.NavigateToLogin
                }
            }
        }
    }
}
