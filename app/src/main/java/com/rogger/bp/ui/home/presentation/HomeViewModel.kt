package com.rogger.bp.ui.home.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.rogger.bp.R
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
import com.rogger.bp.ui.groups.data.GroupRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val isFirstLoad: Boolean = true,
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
    val yellowWarningDays: Int = 3,
    val groupId: String? = null,
    val currentGroupName: String = "",
    val workMode: Int = 0
)

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val authRepository: AuthRepository,
    private val categoryRepository: CategoryRepository,
    private val profileRepository: ProfileRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private var isLoggingOut = false

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
        observeGroupChanges()
    }

    private fun observeGroupChanges() {
        // ... (removed empty observeInvitations placeholder)
        groupRepository.getLocalGroupFlow().onEach { group ->
            val newGroupId = group?.groupId
            val oldGroupId = _uiState.value.groupId
            
            if (newGroupId != oldGroupId) {
                _uiState.update { it.copy(groupId = newGroupId) }
                // Quando o grupo muda, forçamos o reinício do listener do Firestore
                refreshProducts()
            }
        }.launchIn(viewModelScope)
    }

    fun updateWorkMode(context: android.content.Context) {
        viewModelScope.launch {
            val group = groupRepository.getLocalGroupFlow().firstOrNull()
            val mode = SharedPreferencesManager.getWorkMode(context)
            val displayName = if (mode == 1 && group != null && group.name != "Meu Grupo") group.name else ""
            
            _uiState.update { it.copy(currentGroupName = displayName, workMode = mode) }
            refreshProducts()
            
            categoryRepository.fetchAll(object : FetchCategoriesCallback {
                override fun onSuccess(categories: List<PostCategory>) {}
                override fun onFailure(message: String) {}
                override fun onComplete() {}
            }, forceRefresh = true, workMode = mode)
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            categoryRepository.getCachedCategoriesFlow().collect {
                _categories.value = it
            }
        }
    }

    fun fetchCategories() {
        val workMode = _uiState.value.workMode
        categoryRepository.fetchAll(object : FetchCategoriesCallback {
            override fun onSuccess(categories: List<PostCategory>) {}
            override fun onFailure(message: String) {}
            override fun onComplete() {}
        }, workMode = workMode)
    }

    private fun observeProductsPipeline() {
        combine(
            homeRepository.getCachedProductsFlow(),
            _searchQuery,
            _categoryId
        ) { products, query, categoryId ->
            val normalizedQuery = query.normalizeForSearch()
            
            products.filter { !it.deleted }
                .filter { if (categoryId != null) it.categoryId == categoryId else true }
                .filter { 
                    normalizedQuery.isEmpty() || 
                    it.name.normalizeForSearch().contains(normalizedQuery) || 
                    it.barcode.lowercase().contains(normalizedQuery) ||
                    it.categoryName.normalizeForSearch().contains(normalizedQuery)
                }
                .map { it.toDomain() }
        }.onEach { productList ->
            _uiState.update { it.copy(products = productList) }
            // Se já temos produtos no cache (ex: vieram do login), não precisamos mostrar o spinner de "primeira carga"
            if (productList.isNotEmpty()) {
                _uiState.update { it.copy(isFirstLoad = false) }
            }
        }.launchIn(viewModelScope)
    }

    private fun String.normalizeForSearch(): String {
        return java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
    }

    fun syncAndFetchProducts(context: Context) {
        val workMode = SharedPreferencesManager.getWorkMode(context)
        _uiState.update { it.copy(workMode = workMode) }
        performSync(context)
    }

    private fun performSync(context: Context) {
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
                val uid = authRepository.getCurrentUser()?.uuid ?: ""
                SharedPreferencesManager.saveUserInfo(context, uid, name, photoUrl, email)
                SharedPreferencesManager.setPremiumState(context, isPremium)
            }
            override fun onFailure(message: String) {}
            override fun onComplete() {}
        })

        startInvitationListener(context)
        
        _uiState.update { 
            it.copy(yellowWarningDays = NotificationPrefs.getDays(context)) 
        }
        
        refreshProducts()
    }

    private fun refreshProducts() {
        if (isLoggingOut) return

        val user = authRepository.getCurrentUser()
        if (user == null) {
            Log.d("HomeViewModel", "refreshProducts: skipping because user is null (probably logging out)")
            return
        }

        val currentState = _uiState.value
        
        if (currentState.products.isEmpty() && !homeRepository.isSyncing()) {
            _uiState.update { it.copy(isLoading = true) }
        }

        // Além dos produtos, força a atualização das categorias no modo correto
        fetchCategories()

        homeRepository.fetchAll(object : FetchProductsCallback {
            override fun onSuccess(products: List<PostProduct>) {
                _uiState.update { it.copy(isFirstLoad = false) }
            }
            override fun onFailure(message: String) {
                _uiState.update { it.copy(isLoading = false, isFirstLoad = false, errorMessage = message) }
            }
            override fun onComplete() {
                _uiState.update { it.copy(isLoading = false, isFirstLoad = false) }
            }
        }, forceRefresh = true, workMode = currentState.workMode)
    }

    private var invitationJob: kotlinx.coroutines.Job? = null
    
    private fun startInvitationListener(context: Context) {
        if (invitationJob != null) return
        
        val user = authRepository.getCurrentUser() ?: return
        invitationJob = groupRepository.getInvitationsFlow(user.uuid).onEach { invites ->
            invites.forEach { invitation ->
                com.rogger.bp.notification.NotificationUtil.showInvitation(
                    context,
                    invitation.senderName,
                    invitation.groupName
                )
            }
        }.launchIn(viewModelScope)
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
            // Sincronização prioritária do grupo para usuários membros
            viewModelScope.launch {
                groupRepository.syncUserGroup(it.uuid)
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
        isLoggingOut = true
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Google Sign Out
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                GoogleSignIn.getClient(context, gso).signOut()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Erro ao fazer logout do Google: ${e.message}")
            }

            homeRepository.stopListeningForProducts()
            homeRepository.clearLocalCache()
            categoryRepository.clearLocalCache()
            groupRepository.clearLocalCache()
            authRepository.logout()
            SharedPreferencesManager.setLoginState(context, "state", false)
            SharedPreferencesManager.clearUserInfo(context)
            onLogout()
        }
    }

    fun exportPdf(context: Context) {
        PdfExportHelper.exportToPdf(context, _uiState.value.products.map { it.toData() })
    }

    fun exportExcel(context: Context) {
        ExcelExportHelper.exportToExcel(context, _uiState.value.products.map { it.toData() })
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
