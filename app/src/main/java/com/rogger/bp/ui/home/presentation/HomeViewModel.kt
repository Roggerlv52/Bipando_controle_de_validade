package com.rogger.bp.ui.home.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.rogger.bp.R
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.data.repository.toData
import com.rogger.bp.data.repository.toDomain
import com.rogger.bp.domain.model.Product
import com.rogger.bp.domain.repository.AuthRepository
import com.rogger.bp.notification.NotificationPrefs
import com.rogger.bp.notification.NotificationUtil
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.category.data.FetchCategoriesCallback
import com.rogger.bp.ui.commun.AnalyticsManager
import com.rogger.bp.ui.commun.SharedPreferencesManager
import com.rogger.bp.ui.groups.data.GroupRepository
import com.rogger.bp.ui.home.data.FetchProductsCallback
import com.rogger.bp.ui.home.data.HomeRepository
import com.rogger.bp.ui.profile.data.FetchProfileCallback
import com.rogger.bp.ui.profile.data.ProfileRepository
import com.rogger.bp.util.ExcelExportHelper
import com.rogger.bp.util.PdfExportHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val isFirstLoad: Boolean = true,
    val isRoomReady: Boolean = false, // NOVO — true quando Room já emitiu pela primeira vez
    val errorMessage: String? = null,
    val categoryFilterName: String? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val userPhoto: String = "",
    val isPremium: Boolean = false,
    val trialEndsAtMillis: Long? = null,
    val trialStartedAtMillis: Long? = null,
    val freeLimit: Int = 100,
    val isTrialActive: Boolean = false,
    val trialDaysRemaining: Int = 0,
    val hasUnlimitedAccess: Boolean = false,
    val activeCount: Int = 0,
    val globalTotalCount: Int = 0,
    val isLimitReached: Boolean = false,
    val categoryCount: Int = 0,
    val deletedCount: Int = 0,
    val yellowWarningDays: Int = 3,
    val groupId: String? = null,
    val currentGroupName: String = "",
    val workMode: Int = 0,
    val userRole: String = "Admin"
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    context: Context,
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
        // Carrega estado inicial síncronamente no init para evitar flicker
        val initialWorkMode = SharedPreferencesManager.getWorkMode(context)
        val initialGroupId =
            if (initialWorkMode == 1) SharedPreferencesManager.getActiveGroupId(context) else ""

        val initialIsPremium = SharedPreferencesManager.isPremium(context)
        val initialTrialEndsAt = SharedPreferencesManager.getTrialEndsAt(context)
        val initialFreeLimit = SharedPreferencesManager.getFreeLimit(context)

        val isTrialActive = com.rogger.bp.domain.policy.AccessPolicy.isTrialActive(initialTrialEndsAt)
        val daysRemaining = com.rogger.bp.domain.policy.AccessPolicy.getTrialDaysRemaining(initialTrialEndsAt)
        val hasUnlimited = com.rogger.bp.domain.policy.AccessPolicy.hasUnlimitedAccess(initialIsPremium, initialTrialEndsAt)

        _uiState.update {
            it.copy(
                workMode = initialWorkMode,
                groupId = initialGroupId,
                isPremium = initialIsPremium,
                trialEndsAtMillis = initialTrialEndsAt,
                freeLimit = initialFreeLimit,
                isTrialActive = isTrialActive,
                trialDaysRemaining = daysRemaining,
                hasUnlimitedAccess = hasUnlimited
            )
        }

        updateUserRole(context, initialGroupId, initialWorkMode)

        observeProductsPipeline()
        observeCategories()
        observeCounters()
        observeGroupChanges()
        startUserDocumentObserver(context)
        fetchConfigPlans(context)

        // Inicia o listener de convites globalmente se houver usuário
        val user = authRepository.getCurrentUser()
        if (user != null) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(500)
                startInvitationListener(context)
            }
        }
    }

    private fun startUserDocumentObserver(context: Context) {
        val user = authRepository.getCurrentUser() ?: return
        authRepository.observeUserDocument(user.uuid)
            .onEach { userDoc ->
                if (!isLoggingOut) {
                    SharedPreferencesManager.setPremiumState(context, userDoc.isPremium)
                    SharedPreferencesManager.setTrialEndsAt(context, userDoc.trialEndsAtMillis)
                    val freeLimit = SharedPreferencesManager.getFreeLimit(context)

                    val isTrialActive = com.rogger.bp.domain.policy.AccessPolicy.isTrialActive(userDoc.trialEndsAtMillis)
                    val daysRemaining = com.rogger.bp.domain.policy.AccessPolicy.getTrialDaysRemaining(userDoc.trialEndsAtMillis)
                    val hasUnlimited = com.rogger.bp.domain.policy.AccessPolicy.hasUnlimitedAccess(userDoc.isPremium, userDoc.trialEndsAtMillis)

                    _uiState.update { state ->
                        val isLimitReached = com.rogger.bp.domain.policy.AccessPolicy.isLimitReached(
                            userDoc.isPremium,
                            userDoc.trialEndsAtMillis,
                            state.globalTotalCount,
                            freeLimit
                        )
                        state.copy(
                            isPremium = userDoc.isPremium,
                            trialEndsAtMillis = userDoc.trialEndsAtMillis,
                            trialStartedAtMillis = userDoc.trialStartedAtMillis,
                            freeLimit = freeLimit,
                            isTrialActive = isTrialActive,
                            trialDaysRemaining = daysRemaining,
                            hasUnlimitedAccess = hasUnlimited,
                            isLimitReached = isLimitReached
                        )
                    }
                }
            }
            .catch { android.util.Log.e("HomeViewModel", "Erro ao observar documento do usuário", it) }
            .launchIn(viewModelScope)
    }

    private fun fetchConfigPlans(context: Context) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("config")
            .document("plans")
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val freeLimit = doc.getLong("freeLimit")?.toInt() ?: 100
                    val promoActive = doc.getBoolean("promoActive") ?: false
                    SharedPreferencesManager.setFreeLimit(context, freeLimit)
                    SharedPreferencesManager.setPromoActive(context, promoActive)
                    _uiState.update { state ->
                        val isLimitReached = com.rogger.bp.domain.policy.AccessPolicy.isLimitReached(
                            state.isPremium,
                            state.trialEndsAtMillis,
                            state.globalTotalCount,
                            freeLimit
                        )
                        state.copy(freeLimit = freeLimit, isLimitReached = isLimitReached)
                    }
                }
            }
    }

    private fun observeGroupChanges() {
        groupRepository.getLocalGroupsFlow().onEach { groups ->
            val activeGroupId = _uiState.value.groupId
            if (activeGroupId != null) {
                val currentGroup = groups.find { it.groupId == activeGroupId }
                if (currentGroup != null) {
                    val displayName = if (currentGroup.isDefault) "" else currentGroup.name
                    _uiState.update { it.copy(currentGroupName = displayName) }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun updateWorkMode(context: Context) {
        viewModelScope.launch {
            val mode = SharedPreferencesManager.getWorkMode(context)
            val activeGroupId = SharedPreferencesManager.getActiveGroupId(context)

            val group = if (mode == 1 && !activeGroupId.isNullOrEmpty()) {
                groupRepository.getGroupById(activeGroupId)
            } else {
                groupRepository.getLocalGroupFlow().firstOrNull()
            }

            val displayName = if (mode == 1 && group != null && !group.isDefault) group.name else ""
            val newGroupId = if (mode == 1) activeGroupId else ""

            val oldGroupId = _uiState.value.groupId
            val oldMode = _uiState.value.workMode

            if (newGroupId != oldGroupId || mode != oldMode) {
                _uiState.update {
                    it.copy(
                        currentGroupName = displayName,
                        workMode = mode,
                        groupId = newGroupId,
                        // Reset loading state for new group/mode
                        isLoading = true,
                        isFirstLoad = true,
                        isRoomReady = false,
                        products = emptyList()
                    )
                }
                updateUserRole(context, newGroupId, mode)
                refreshProducts()
            } else {
                _uiState.update { it.copy(currentGroupName = displayName) }
                updateUserRole(context, newGroupId, mode)
            }

            categoryRepository.fetchAll(object : FetchCategoriesCallback {
                override fun onSuccess(categories: List<PostCategory>) {}
                override fun onFailure(message: String) {}
                override fun onComplete() {}
            }, forceRefresh = true, workMode = mode, groupId = newGroupId)
        }
    }

    private fun updateUserRole(context: Context, groupId: String?, workMode: Int) {
        if (workMode == 0 || groupId.isNullOrEmpty()) {
            _uiState.update { it.copy(userRole = "Admin") }
            return
        }

        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uuid ?: return@launch
            val cached = SharedPreferencesManager.getCachedRole(context, groupId)
            if (cached != null) {
                _uiState.update { it.copy(userRole = cached) }
            } else {
                val members = groupRepository.fetchMembers(groupId).getOrNull() ?: emptyList()
                val role = members.find { it.userId == uid }?.role ?: "Reader"
                SharedPreferencesManager.setCachedRole(context, groupId, role)
                _uiState.update { it.copy(userRole = role) }
            }
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
        val state = _uiState.value
        categoryRepository.fetchAll(object : FetchCategoriesCallback {
            override fun onSuccess(categories: List<PostCategory>) {}
            override fun onFailure(message: String) {}
            override fun onComplete() {}
        }, workMode = state.workMode, groupId = state.groupId)
    }

    private fun observeProductsPipeline() {
        val groupIdFlow = _uiState.map { it.groupId ?: "" }.distinctUntilChanged()

        combine(
            groupIdFlow.flatMapLatest { groupId ->
                homeRepository.getCachedProductsFlow(groupId)
            },
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
            _uiState.update {
                it.copy(
                    products = productList,
                    isRoomReady = true, // Marca que o Room já emitiu pela primeira vez
                    // Paramos o loading se a lista não estiver vazia.
                    isLoading = if (productList.isNotEmpty()) false else it.isLoading,
                    isFirstLoad = if (productList.isNotEmpty()) false else it.isFirstLoad
                )
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
            override fun onSuccess(
                name: String,
                email: String,
                photoUrl: String,
                isPremium: Boolean,
                trialEndsAtMillis: Long?
            ) {
                val freeLimit = SharedPreferencesManager.getFreeLimit(context)
                val isTrialActive = com.rogger.bp.domain.policy.AccessPolicy.isTrialActive(trialEndsAtMillis)
                val daysRemaining = com.rogger.bp.domain.policy.AccessPolicy.getTrialDaysRemaining(trialEndsAtMillis)
                val hasUnlimited = com.rogger.bp.domain.policy.AccessPolicy.hasUnlimitedAccess(isPremium, trialEndsAtMillis)

                _uiState.update { state ->
                    val isLimitReached = com.rogger.bp.domain.policy.AccessPolicy.isLimitReached(
                        isPremium,
                        trialEndsAtMillis,
                        state.globalTotalCount,
                        freeLimit
                    )
                    state.copy(
                        userName = name,
                        userEmail = email,
                        userPhoto = photoUrl,
                        isPremium = isPremium,
                        trialEndsAtMillis = trialEndsAtMillis,
                        freeLimit = freeLimit,
                        isTrialActive = isTrialActive,
                        trialDaysRemaining = daysRemaining,
                        hasUnlimitedAccess = hasUnlimited,
                        isLimitReached = isLimitReached
                    )
                }
                val uid = authRepository.getCurrentUser()?.uuid ?: ""
                SharedPreferencesManager.saveUserInfo(context, uid, name, photoUrl, email)
                SharedPreferencesManager.setPremiumState(context, isPremium)
                SharedPreferencesManager.setTrialEndsAt(context, trialEndsAtMillis)

                startInvitationListener(context)
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
        if (user == null) return

        val currentState = _uiState.value

        // Só mostra o loader centralizado se não tivermos nenhum produto no estado atual (nem cache)
        if (currentState.products.isEmpty()) {
            _uiState.update { it.copy(isLoading = true) }
        }

        fetchCategories()

        homeRepository.fetchAll(object : FetchProductsCallback {
            override fun onSuccess(products: List<PostProduct>) {
                // Ao receber dados do cache ou remoto, paramos o loading principal
                _uiState.update { it.copy(isFirstLoad = false, isLoading = false) }
            }

            override fun onFailure(message: String) {
                if (!message.contains("PERMISSION_DENIED", ignoreCase = true)) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isFirstLoad = false,
                            errorMessage = message
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, isFirstLoad = false)
                    }
                }
            }

            override fun onComplete() {
                _uiState.update { it.copy(isLoading = false, isFirstLoad = false) }
            }
        }, forceRefresh = true, workMode = currentState.workMode, groupId = currentState.groupId)
    }

    private var invitationJob: kotlinx.coroutines.Job? = null

    private fun startInvitationListener(context: Context? = null) {
        if (invitationJob != null) return

        val user = authRepository.getCurrentUser() ?: return

        invitationJob = groupRepository.getInvitationsFlow(user.uuid)
            .onEach { invites ->
                invites.forEach { invitation ->
                    if (!isLoggingOut) {
                        context?.let { ctx ->
                            val appContext = ctx.applicationContext
                            if (!SharedPreferencesManager.isInvitationNotified(appContext, invitation.id)) {
                                NotificationUtil.showInvitation(appContext, invitation.senderName, invitation.groupName)
                                SharedPreferencesManager.setInvitationNotified(appContext, invitation.id, true)
                            }
                        }
                    }
                }
            }
            .catch { e ->
                if (!isLoggingOut) {
                    if (e.message?.contains("PERMISSION_DENIED") == true) {
                        invitationJob?.cancel()
                        invitationJob = null
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadUserInfo(context: Context) {
        val user = authRepository.getCurrentUser()
        user?.let { u ->
            _uiState.update { state ->
                state.copy(
                    userName = u.name,
                    userEmail = u.email,
                    userPhoto = u.photoUri?.toString() ?: ""
                )
            }

            viewModelScope.launch {
                val result =
                    groupRepository.handleUserLogin(u.uuid, u.name, u.photoUri?.toString() ?: "")
                result.onSuccess { group ->
                    val currentMode = SharedPreferencesManager.getWorkMode(context)
                    if (group.isDefault && currentMode == 0) {
                        SharedPreferencesManager.setWorkMode(context, 1)
                        SharedPreferencesManager.setActiveGroupId(context, group.groupId)
                        updateWorkMode(context)
                    }
                }
                groupRepository.syncUserGroup(u.uuid)
            }
        }
    }

    private fun observeCounters() {
        val groupIdFlow = _uiState.map { it.groupId ?: "" }.distinctUntilChanged()

        viewModelScope.launch {
            combine(
                groupIdFlow.flatMapLatest { groupId ->
                    homeRepository.getCachedProductsFlow(groupId)
                },
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

        // Observador para o limite global (Premium/Trial/Free)
        homeRepository.getGlobalTotalProductsCountFlow().onEach { count ->
            _uiState.update { state ->
                val isLimitReached = com.rogger.bp.domain.policy.AccessPolicy.isLimitReached(
                    state.isPremium,
                    state.trialEndsAtMillis,
                    count,
                    state.freeLimit
                )
                state.copy(
                    globalTotalCount = count,
                    isLimitReached = isLimitReached
                )
            }
        }.launchIn(viewModelScope)
    }

    fun logout(context: Context, onLogout: () -> Unit) {
        isLoggingOut = true
        GroupRepository.setLoggingOut(true)
        invitationJob?.cancel()
        invitationJob = null

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                GoogleSignIn.getClient(context, gso).signOut()
            } catch (_: Exception) {}

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
        val groupName = _uiState.value.currentGroupName.ifEmpty { context.getString(R.string.menu_home) }
        PdfExportHelper.exportToPdf(context, _uiState.value.products.map { it.toData() }, groupName)
    }

    fun exportExcel(context: Context) {
        val groupName = _uiState.value.currentGroupName.ifEmpty { context.getString(R.string.menu_home) }
        ExcelExportHelper.exportToExcel(context, _uiState.value.products.map { it.toData() }, groupName)
    }

    fun deleteProducts(products: List<Product>) {
        val currentState = _uiState.value
        if (currentState.userRole == "Reader") {
            _uiState.update { it.copy(errorMessage = "Apenas Administradores ou Editores podem remover itens.") }
            return
        }

        viewModelScope.launch {
            var errorShown = false
            products.forEach { product ->
                homeRepository.delete(
                    product.toData(),
                    object : com.rogger.bp.ui.home.data.HomeCallback {
                        override fun onSuccess(product: PostProduct) {}
                        override fun onFailure(message: String) {
                            if (!errorShown) {
                                _uiState.update { it.copy(errorMessage = message) }
                                errorShown = true
                            }
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
