package com.rogger.bp.ui.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.data.repository.toData
import com.rogger.bp.data.repository.toDomain
import com.rogger.bp.domain.model.Product
import com.rogger.bp.domain.repository.AuthRepository
import com.rogger.bp.ui.commun.SharedPreferencesManager
import com.rogger.bp.ui.home.data.FetchProductsCallback
import com.rogger.bp.ui.home.data.HomeRepository
import com.rogger.bp.util.ExcelExportHelper
import com.rogger.bp.util.PdfExportHelper
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.category.data.FetchCategoriesCallback
import com.rogger.bp.notification.NotificationPrefs
import com.rogger.bp.ui.profile.data.FetchProfileCallback
import com.rogger.bp.ui.profile.data.ProfileRepository
import com.rogger.bp.ui.profile.data.UpdateProfileCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val isFirstLoad: Boolean = true, // Novo flag para controlar a primeira carga absoluta
    val errorMessage: String? = null,
    val categoryFilterName: String? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val userPhoto: String = "",
    val isPremium: Boolean = false,
    val activeCount: Int = 0,
    val categoryCount: Int = 0,
    val deletedCount: Int = 0,
    val yellowWarningDays: Int = 3
)

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val authRepository: AuthRepository,
    private val categoryRepository: CategoryRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<PostCategory>>(emptyList())
    val categories: StateFlow<List<PostCategory>> = _categories.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _categoryId = MutableStateFlow<String?>(null)

    init {
        loadUserInfo()
        observeProductsPipeline()
        observeCategories()
        observeCounters()
    }

    private fun observeCategories() {
        viewModelScope.launch {
            categoryRepository.getCachedCategoriesFlow().collect {
                _categories.value = it
            }
        }
    }

    fun fetchCategories() {
        categoryRepository.fetchAll(object : FetchCategoriesCallback {
            override fun onSuccess(categories: List<PostCategory>) {}
            override fun onFailure(message: String) {}
            override fun onComplete() {}
        })
    }

    private fun observeProductsPipeline() {
        combine(
            homeRepository.getCachedProductsFlow(),
            _searchQuery,
            _categoryId
        ) { products, query, categoryId ->
            products.filter { !it.deleted }
                .filter { if (categoryId != null) it.categoryId == categoryId else true }
                .filter { 
                    query.isEmpty() || 
                    it.name.contains(query, ignoreCase = true) || 
                    it.barcode.contains(query) ||
                    it.categoryName.contains(query, ignoreCase = true)
                }
                .map { it.toDomain() }
        }.onEach { productList ->
            _uiState.update { it.copy(products = productList) }
        }.launchIn(viewModelScope)
    }

    fun syncAndFetchProducts(context: Context) {
        // Carrega dados do perfil do Firestore
        profileRepository.getUserProfile(object : FetchProfileCallback {
            override fun onSuccess(name: String, email: String, photoUrl: String, isPremium: Boolean) {
                _uiState.update { 
                    it.copy(
                        userName = name,
                        userEmail = email,
                        userPhoto = photoUrl,
                        isPremium = isPremium
                    )
                }
                SharedPreferencesManager.saveUserInfo(context, "", name, photoUrl, email)
                SharedPreferencesManager.setPremiumState(context, isPremium)
            }
            override fun onFailure(message: String) {}
            override fun onComplete() {}
        })

        // Se já tivermos produtos, marcamos que não é mais a primeira carga
        if (_uiState.value.products.isNotEmpty()) {
            _uiState.update { it.copy(isFirstLoad = false) }
        }

        // Só mostra loading se a lista estiver vazia E ainda não estiver sincronizando
        if (_uiState.value.products.isEmpty() && !homeRepository.isSyncing()) {
            _uiState.update { it.copy(isLoading = true) }
        }
        
        _uiState.update { 
            it.copy(yellowWarningDays = NotificationPrefs.getDays(context)) 
        }
        
        homeRepository.fetchAll(object : FetchProductsCallback {
            override fun onSuccess(products: List<PostProduct>) {
                // Quando o repositório retorna o cache inicial ou dados do servidor
                if (products.isNotEmpty()) {
                    _uiState.update { it.copy(isFirstLoad = false) }
                }
            }

            override fun onFailure(message: String) {
                _uiState.update { it.copy(isLoading = false, isFirstLoad = false, errorMessage = message) }
            }

            override fun onComplete() {
                _uiState.update { it.copy(isLoading = false, isFirstLoad = false) }
            }
        })
    }

    private fun loadUserInfo() {
        val user = authRepository.getCurrentUser()
        user?.let {
            _uiState.update { state ->
                state.copy(
                    userName = it.name,
                    userEmail = it.email,
                    userPhoto = it.photoUri?.toString() ?: ""
                )
            }
        }
    }

    private fun observeCounters() {
        viewModelScope.launch {
            combine(
                homeRepository.getCachedProductsFlow(),
                categoryRepository.getCachedCategoriesFlow()
            ) { products, categories ->
                _uiState.update { state ->
                    state.copy(
                        activeCount = products.count { !it.deleted },
                        deletedCount = products.count { it.deleted },
                        categoryCount = categories.size
                    )
                }
            }.collect {}
        }
    }

    fun logout(context: Context, onLogout: () -> Unit) {
        homeRepository.stopListeningForProducts()
        authRepository.logout()
        SharedPreferencesManager.setLoginState(context, "state", false)
        SharedPreferencesManager.clearUserInfo(context)
        onLogout()
    }

    fun exportPdf(context: Context) {
        PdfExportHelper.exportToPdf(context, _uiState.value.products.map { it.toData() })
    }

    fun exportExcel(context: Context) {
        ExcelExportHelper.exportToExcel(context, _uiState.value.products.map { it.toData() })
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            homeRepository.delete(product.toData(), object : com.rogger.bp.ui.home.data.HomeCallback {
                override fun onSuccess(product: PostProduct) {}
                override fun onFailure(message: String) {
                    _uiState.update { it.copy(errorMessage = message) }
                }
                override fun onComplete() {}
            })
        }
    }

    fun deleteProducts(products: List<Product>) {
        viewModelScope.launch {
            products.forEach { product ->
                homeRepository.delete(product.toData(), object : com.rogger.bp.ui.home.data.HomeCallback {
                    override fun onSuccess(product: PostProduct) {}
                    override fun onFailure(message: String) {
                        _uiState.update { it.copy(errorMessage = message) }
                    }
                    override fun onComplete() {}
                })
            }
        }
    }

    fun fetchProducts(categoryId: String? = null, categoryName: String? = null) {
        _categoryId.value = categoryId
        _uiState.update { it.copy(categoryFilterName = categoryName) }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        _uiState.update { it.copy(searchQuery = newQuery) }
    }

    fun updateUserName(context: Context, newName: String) {
        if (newName.isBlank()) return

        profileRepository.updateUserName(newName, object : UpdateProfileCallback {
            override fun onSuccess() {
                _uiState.update { it.copy(userName = newName) }
                val userInfo = SharedPreferencesManager.getUserInfo(context)
                SharedPreferencesManager.saveUserInfo(
                    context,
                    userInfo.getOrNull(0) ?: "",
                    newName,
                    userInfo.getOrNull(2) ?: "",
                    userInfo.getOrNull(3) ?: ""
                )
            }
            override fun onFailure(message: String) {
                _uiState.update { it.copy(errorMessage = message) }
            }
            override fun onComplete() {}
        })
    }

    fun uploadProfileImage(context: Context, imageUri: android.net.Uri) {
        _uiState.update { it.copy(isLoading = true) }
        profileRepository.uploadProfileImage(imageUri, object : com.rogger.bp.ui.profile.data.UploadProfileImageCallback {
            override fun onSuccess(photoUrl: String) {
                _uiState.update { it.copy(userPhoto = photoUrl) }
                val userInfo = SharedPreferencesManager.getUserInfo(context)
                SharedPreferencesManager.saveUserInfo(
                    context,
                    userInfo.getOrNull(0) ?: "",
                    userInfo.getOrNull(1) ?: "",
                    photoUrl,
                    userInfo.getOrNull(3) ?: ""
                )
            }
            override fun onFailure(message: String) {
                _uiState.update { it.copy(errorMessage = message) }
            }
            override fun onComplete() {
                _uiState.update { it.copy(isLoading = false) }
            }
        })
    }

    fun toggleSearch(active: Boolean) {
        _uiState.update { 
            it.copy(
                isSearchActive = active,
                searchQuery = if (!active) "" else it.searchQuery
            ) 
        }
        if (!active) {
            _searchQuery.value = ""
        }
    }

    override fun onCleared() {
        super.onCleared()
        homeRepository.stopListeningForProducts()
    }
}
