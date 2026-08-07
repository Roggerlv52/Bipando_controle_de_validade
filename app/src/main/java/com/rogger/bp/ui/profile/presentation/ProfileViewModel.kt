package com.rogger.bp.ui.profile.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asFlow
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
import kotlinx.coroutines.launch

data class ProfileState(
    val userName: String = "",
    val userEmail: String = "",
    val userPhotoUrl: String = "",
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
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedOut: Boolean = false
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val productDao: ProductDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    init {
        observeProductCount()
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
        // Carrega preferências locais
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

        val themeType = when (themeNumber) {
            2 -> BipandoThemeType.GREEN
            3 -> BipandoThemeType.RED
            4 -> BipandoThemeType.DARK
            else -> BipandoThemeType.CLASSIC
        }

        // Carrega dados iniciais do cache local (SharedPreferences) para feedback imediato
        val userInfo = SharedPreferencesManager.getUserInfo(context)
        _uiState.update {
            it.copy(
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
                soundUri = soundUri
            )
        }

        // Busca dados atualizados do Firebase
        _uiState.update { it.copy(isLoading = true) }
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
                // Sincroniza o cache local com os dados do Firebase
                SharedPreferencesManager.saveUserInfo(context, "", name, photoUrl, email)
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

    fun updateUserName(context: Context, newName: String) {
        if (newName.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }
        profileRepository.updateUserName(newName, object : com.rogger.bp.ui.profile.data.UpdateProfileCallback {
            override fun onSuccess() {
                _uiState.update { it.copy(userName = newName) }
                // Atualiza cache local
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
        SharedPreferencesManager.setLoginState(context, "state", false)
        SharedPreferencesManager.clearUserInfo(context)
        _uiState.update { it.copy(isLoggedOut = true) }
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

    fun uploadProfileImage(context: Context, uri: android.net.Uri) {
        _uiState.update { it.copy(isLoading = true) }
        profileRepository.uploadProfileImage(context, uri, object : com.rogger.bp.ui.profile.data.UploadProfileImageCallback {
            override fun onSuccess(photoUrl: String) {
                _uiState.update { it.copy(userPhotoUrl = photoUrl) }
                // Atualiza cache local
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
}
