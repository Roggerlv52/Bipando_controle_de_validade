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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = true, // Inicia como true para evitar flicker do EmptyState
    val errorMessage: String? = null,
    val categoryFilterName: String? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val userPhoto: String = "",
    val activeCount: Int = 0,
    val categoryCount: Int = 0,
    val deletedCount: Int = 0,
    val yellowWarningDays: Int = 3
)

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val authRepository: AuthRepository,
    private val categoryRepository: CategoryRepository
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
        // Só mostra loading se a lista estiver vazia (primeira carga)
        if (_uiState.value.products.isEmpty()) {
            _uiState.update { it.copy(isLoading = true) }
        }
        
        _uiState.update { 
            it.copy(yellowWarningDays = NotificationPrefs.getDays(context)) 
        }
        
        homeRepository.fetchAll(object : FetchProductsCallback {
            override fun onSuccess(products: List<PostProduct>) {
                // Pipeline reativo cuida da atualização
            }

            override fun onFailure(message: String) {
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            }

            override fun onComplete() {
                _uiState.update { it.copy(isLoading = false) }
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
