package com.rogger.bp.ui.profile.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import com.rogger.bp.notification.NotificationPrefs
import com.rogger.bp.notification.NotificationScheduler
import com.rogger.bp.ui.commun.SharedPreferencesManager
import com.rogger.bp.ui.theme.BipandoThemeType
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val userName: String = "",
    val userPhotoUrl: String = "",
    val themeType: BipandoThemeType = BipandoThemeType.CLASSIC,
    val isBeepEnabled: Boolean = false,
    val isNotificationEnabled: Boolean = false,
    val notificationDays: Int = 7,
    val notificationTime: String = "08:00",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedOut: Boolean = false
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    fun loadProfile(context: Context) {
        val userInfo = SharedPreferencesManager.getUserInfo(context)
        val themeNumber = SharedPreferencesManager.getThemeNumber(context, "chave")
        val beepState = SharedPreferencesManager.getBeepState(context, "beep")
        val notifEnabled = NotificationPrefs.getAlert(context)
        val notifDays = NotificationPrefs.getDays(context)
        val hour = NotificationPrefs.getHour(context)
        val minute = NotificationPrefs.getMinute(context)

        val themeType = when (themeNumber) {
            2 -> BipandoThemeType.GREEN
            3 -> BipandoThemeType.RED
            4 -> BipandoThemeType.DARK
            else -> BipandoThemeType.CLASSIC
        }

        _uiState.update {
            it.copy(
                userName = userInfo.getOrNull(1) ?: "",
                userPhotoUrl = userInfo.getOrNull(2) ?: "",
                themeType = themeType,
                isBeepEnabled = beepState,
                isNotificationEnabled = notifEnabled,
                notificationDays = notifDays,
                notificationTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            )
        }
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

    fun logout(context: Context) {
        SharedPreferencesManager.setLoginState(context, "state", false)
        SharedPreferencesManager.clearUserInfo(context)
        _uiState.update { it.copy(isLoggedOut = true) }
    }
}
