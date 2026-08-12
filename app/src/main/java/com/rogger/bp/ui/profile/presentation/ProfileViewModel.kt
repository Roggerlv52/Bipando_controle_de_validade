package com.rogger.bp.ui.profile.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asFlow
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.rogger.bp.R
import com.rogger.bp.data.dao.ProductDao
import com.rogger.bp.notification.NotificationPrefs
import com.rogger.bp.notification.NotificationScheduler
import com.rogger.bp.ui.commun.SharedPreferencesManager
import com.rogger.bp.ui.profile.data.FetchProfileCallback
import com.rogger.bp.ui.profile.data.ProfileRepository
import com.rogger.bp.ui.theme.BipandoThemeType
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class ProfileState(
    val userName: String = "",
    val userEmail: String = "",
    val userPhotoUrl: String = "",
    val userUid: String = "",
    val groupName: String = "",
    val groupInviteCode: String = "",
    val hasCustomGroupName: Boolean = false,
    val totalProductsCount: Int = 0,
    val deletedProductsCount: Int = 0,
    val isPremium: Boolean = false,
    val themeType: BipandoThemeType = BipandoThemeType.CLASSIC,
    val isBeepEnabled: Boolean = false,
    val isNotificationEnabled: Boolean = false,
    val notificationDays: Int = 7,
    val notificationTime: String = "08:00",
    val soundType: Int = 2,
    val soundName: String = "Padrão",
    val soundUri: String = "",
    val datePickerType: Int = 0, // 0 = Calendário, 1 = Spinner
    val workMode: Int = 0, // 0 = Individual, 1 = Grupo
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedOut: Boolean = false,
    val syncSuccess: Boolean = false
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val productDao: ProductDao,
    private val groupRepository: com.rogger.bp.ui.groups.data.GroupRepository,
    private val homeRepository: com.rogger.bp.ui.home.data.HomeRepository,
    private val categoryRepository: com.rogger.bp.ui.category.data.CategoryRepository,
    private val authRepository: com.rogger.bp.domain.repository.AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    init {
        observeProductCount()
        observeGroup()
    }

    private fun observeGroup() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        groupRepository.getLocalGroupFlow().onEach { group ->
            if (group != null) {
                val shortCode = group.shareCode.take(8).uppercase()
                val isCollaborative = group.adminId != currentUser?.uid || (group.name != "Meu Grupo" && group.name.isNotEmpty())
                _uiState.update { 
                    it.copy(
                        groupName = group.name,
                        groupInviteCode = shortCode,
                        hasCustomGroupName = isCollaborative
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        groupName = "",
                        groupInviteCode = "",
                        hasCustomGroupName = false
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun observeProductCount() {
        viewModelScope.launch {
            productDao.getTotalProductsCountLiveData().asFlow().collect { count ->
                _uiState.update { it.copy(totalProductsCount = count ?: 0) }
            }
        }
        viewModelScope.launch {
            productDao.getDeletedProductsCountLiveData(true).asFlow().collect { count ->
                _uiState.update { it.copy(deletedProductsCount = count ?: 0) }
            }
        }
    }

    fun loadProfile(context: Context) {
        val isPremium = SharedPreferencesManager.isPremium(context)
        val themeNumber = SharedPreferencesManager.getThemeNumber(context, "chave")
        val beepState = SharedPreferencesManager.getBeepState(context, "beep")
        val notifEnabled = NotificationPrefs.getAlert(context)
        val notifDays = NotificationPrefs.getDays(context)
        val hour = NotificationPrefs.getHour(context)
        val minute = NotificationPrefs.getMinute(context)
        val soundType = NotificationPrefs.getSoundType(context)
        val soundName = NotificationPrefs.getSoundName(context)
        val soundUri = NotificationPrefs.getSoundUri(context)
        val datePickerType = SharedPreferencesManager.getDatePickerType(context)
        val workMode = SharedPreferencesManager.getWorkMode(context)

        val themeType = when (themeNumber) {
            2 -> BipandoThemeType.GREEN
            3 -> BipandoThemeType.RED
            4 -> BipandoThemeType.DARK
            else -> BipandoThemeType.CLASSIC
        }

        val userInfo = SharedPreferencesManager.getUserInfo(context)
        val userId = userInfo.getOrNull(0) ?: ""
        
        _uiState.update {
            it.copy(
                userUid = userId,
                userName = userInfo.getOrNull(1) ?: "",
                userEmail = userInfo.getOrNull(3) ?: "",
                userPhotoUrl = userInfo.getOrNull(2) ?: "",
                isPremium = isPremium,
                themeType = themeType,
                isBeepEnabled = beepState,
                isNotificationEnabled = notifEnabled,
                notificationDays = notifDays,
                notificationTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                soundType = soundType,
                soundName = soundName,
                soundUri = soundUri,
                datePickerType = datePickerType,
                workMode = workMode
            )
        }

        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            groupRepository.syncUserGroup(userId)
        }

        profileRepository.getUserProfile(object : FetchProfileCallback {
            override fun onSuccess(name: String, email: String, photoUrl: String, isPremium: Boolean) {
                _uiState.update { 
                    it.copy(
                        userName = name,
                        userEmail = email,
                        userPhotoUrl = photoUrl,
                        isPremium = isPremium
                    )
                }
                SharedPreferencesManager.saveUserInfo(context, userId, name, photoUrl, email)
                SharedPreferencesManager.setPremiumState(context, isPremium)
            }
            override fun onFailure(message: String) {
                _uiState.update { it.copy(errorMessage = message) }
            }
            override fun onComplete() {
                _uiState.update { it.copy(isLoading = false) }
            }
        })
    }

    fun onThemeChange(context: Context, themeType: BipandoThemeType) {
        val themeNumber = when (themeType) {
            BipandoThemeType.CLASSIC -> 1
            BipandoThemeType.GREEN -> 2
            BipandoThemeType.RED -> 3
            BipandoThemeType.DARK -> 4
        }
        SharedPreferencesManager.updateThemeNumber(context, "chave", themeNumber)
        _uiState.update { it.copy(themeType = themeType) }
    }

    fun onBeepToggle(context: Context, enabled: Boolean) {
        SharedPreferencesManager.sharedBeepState(context, "beep", enabled)
        _uiState.update { it.copy(isBeepEnabled = enabled) }
    }

    fun onDatePickerTypeChange(context: Context, type: Int) {
        SharedPreferencesManager.setDatePickerType(context, type)
        _uiState.update { it.copy(datePickerType = type) }
    }

    fun onWorkModeChange(context: Context, mode: Int) {
        SharedPreferencesManager.setWorkMode(context, mode)
        _uiState.update { it.copy(workMode = mode) }
    }

    fun syncProductsToGroup() {
        val userId = _uiState.value.userUid
        
        _uiState.update { it.copy(isLoading = true, syncSuccess = false) }
        
        viewModelScope.launch {
            val group = groupRepository.getLocalGroupFlow().firstOrNull()
            val groupId = group?.groupId
            
            Log.d("ProfileViewModel", "syncProductsToGroup: userId=$userId, groupId=$groupId")

            if (userId.isEmpty() || groupId == null) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Dados insuficientes para sincronizar (ID ou Grupo não encontrados)"
                    ) 
                }
                return@launch
            }

            val result = groupRepository.syncUserToGroup(userId, groupId)
            if (result.isSuccess) {
                android.util.Log.d("ProfileViewModel", "Sync success!")
                _uiState.update { it.copy(syncSuccess = true) }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                android.util.Log.e("ProfileViewModel", "Sync failed: $error")
                _uiState.update { it.copy(errorMessage = error) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun resetSyncFlag() = _uiState.update { it.copy(syncSuccess = false) }

    fun onNotificationToggle(context: Context, enabled: Boolean) {
        NotificationPrefs.onAlert(context, enabled)
        if (enabled) {
            NotificationScheduler.start(context)
        } else {
            NotificationScheduler.stop(context)
        }
        _uiState.update { it.copy(isNotificationEnabled = enabled) }
    }

    fun onNotificationDaysChange(context: Context, days: Int) {
        NotificationPrefs.saveDays(context, days)
        if (uiState.value.isNotificationEnabled) {
            NotificationScheduler.start(context)
        }
        _uiState.update { it.copy(notificationDays = days) }
    }

    fun onNotificationTimeChange(context: Context, hour: Int, minute: Int) {
        NotificationPrefs.saveTime(context, hour, minute)
        if (uiState.value.isNotificationEnabled) {
            NotificationScheduler.start(context)
        }
        _uiState.update { 
            it.copy(notificationTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
        }
    }

    fun onSoundTypeChange(context: Context, type: Int, uri: String, name: String) {
        NotificationPrefs.saveSoundType(context, type)
        if (type == 3 || type == 2) {
            NotificationPrefs.saveSoundUri(context, uri, name)
        }
        _uiState.update { it.copy(soundType = type, soundName = name, soundUri = uri) }
    }

    fun logout(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Google Sign Out
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                GoogleSignIn.getClient(context, gso).signOut()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Erro ao fazer logout do Google: ${e.message}")
            }

            homeRepository.stopListeningForProducts()
            homeRepository.clearLocalCache()
            categoryRepository.clearLocalCache()
            groupRepository.clearLocalCache()
            authRepository.logout()
            SharedPreferencesManager.setLoginState(context, "state", false)
            SharedPreferencesManager.clearUserInfo(context)
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }

    fun deleteAccount(context: Context) {
        _uiState.update { it.copy(isLoading = true) }
        profileRepository.deleteUserAccount(object : com.rogger.bp.ui.profile.data.DeleteAccountCallback {
            override fun onSuccess() {
                logout(context)
            }

            override fun onFailure(message: String) {
                _uiState.update { it.copy(errorMessage = message, isLoading = false) }
            }

            override fun onComplete() {
                _uiState.update { it.copy(isLoading = false) }
            }
        })
    }

    fun createGroup(name: String) {
        if (name.isBlank()) return
        val userUid = _uiState.value.userUid
        if (userUid.isEmpty()) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val shortCode = userUid.take(8).uppercase()
            val newGroup = com.rogger.bp.data.model.PostGroup(
                groupId = userUid,
                name = name,
                adminId = userUid,
                shareCode = shortCode,
                createdAt = System.currentTimeMillis()
            )
            val adminMember = com.rogger.bp.data.model.PostMember(
                userId = userUid,
                name = user?.displayName ?: "",
                email = user?.email ?: "",
                photoUrl = user?.photoUrl?.toString() ?: "",
                role = "Admin"
            )
            val result = groupRepository.createGroup(newGroup, adminMember)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun leaveGroup() {
        val userId = _uiState.value.userUid

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val group = groupRepository.getLocalGroupFlow().firstOrNull()
            val groupId = group?.groupId ?: return@launch

            val result = groupRepository.leaveGroup(userId, groupId)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
