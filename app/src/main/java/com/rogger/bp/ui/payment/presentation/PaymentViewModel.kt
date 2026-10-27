package com.rogger.bp.ui.payment.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.rogger.bp.ui.commun.SharedPreferencesManager
import com.rogger.bp.ui.payment.BillingManager
import com.rogger.bp.ui.payment.PlanKeys
import com.rogger.bp.ui.profile.data.ProfileRepository
import com.rogger.bp.ui.profile.data.UpdateProfileCallback
import kotlinx.coroutines.flow.*

data class PaymentState(
    val selectedPlanId: String? = null,
    val activePlanId: String? = null,
    val mensalPrice: String = "",
    val semestralPrice: String = "",
    val anualPrice: String = "",
    val mensalTrialText: String? = null,
    val semestralTrialText: String? = null,
    val anualTrialText: String? = null,
    val mensalAvailable: Boolean = false,
    val semestralAvailable: Boolean = false,
    val anualAvailable: Boolean = false,
    val isEligibleForTrial: Boolean = true,
    val isPremium: Boolean = false,
    val trialEndsAtMillis: Long? = null,
    val freeLimit: Int = 100,
    val isLimitReached: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class PaymentViewModel(
    private val profileRepository: ProfileRepository,
    private val productDao: com.rogger.bp.data.dao.ProductDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentState())
    val uiState: StateFlow<PaymentState> = _uiState.asStateFlow()

    private var billingManager: BillingManager? = null
    private var activePurchaseToken: String? = null

    init {
        observeProductLimit()
    }

    private fun observeProductLimit() {
        productDao.getGlobalTotalProductsCountLiveData().asFlow()
            .onEach { count ->
                _uiState.update { state ->
                    val isPremiumNow = state.activePlanId != null || state.isPremium
                    state.copy(
                        isLimitReached = com.rogger.bp.domain.policy.AccessPolicy.isLimitReached(
                            isPremium = isPremiumNow,
                            trialEndsAtMillis = state.trialEndsAtMillis,
                            count = count ?: 0,
                            freeLimit = state.freeLimit
                        )
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun initBilling(activity: Activity) {
        if (billingManager != null) return

        val initialPremium = SharedPreferencesManager.isPremium(activity)
        val trialEndsAt = SharedPreferencesManager.getTrialEndsAt(activity)
        val freeLimit = SharedPreferencesManager.getFreeLimit(activity)
        _uiState.update { it.copy(isPremium = initialPremium, trialEndsAtMillis = trialEndsAt, freeLimit = freeLimit) }

        billingManager = BillingManager(
            context = activity,
            activity = activity,
            onPricesLoaded = { mensalInfo, semestralInfo, anualInfo, isEligibleForTrial ->
                _uiState.update { state ->
                    val currentSelected = state.selectedPlanId
                    val defaultSelection = currentSelected ?: when {
                        mensalInfo.isAvailable -> PlanKeys.MENSAL
                        semestralInfo.isAvailable -> PlanKeys.SEMESTRAL
                        anualInfo.isAvailable -> PlanKeys.ANUAL
                        else -> null
                    }

                    state.copy(
                        mensalPrice = mensalInfo.price,
                        semestralPrice = semestralInfo.price,
                        anualPrice = anualInfo.price,
                        mensalTrialText = mensalInfo.trialText,
                        semestralTrialText = semestralInfo.trialText,
                        anualTrialText = anualInfo.trialText,
                        mensalAvailable = mensalInfo.isAvailable,
                        semestralAvailable = semestralInfo.isAvailable,
                        anualAvailable = anualInfo.isAvailable,
                        isEligibleForTrial = isEligibleForTrial,
                        selectedPlanId = defaultSelection,
                        isLoading = false
                    )
                }
            },
            onSubscriptionStatusLoaded = { activePlanKey, purchaseToken ->
                activePurchaseToken = purchaseToken
                val isPremiumActive = activePlanKey != null

                _uiState.update { state ->
                    state.copy(
                        activePlanId = activePlanKey,
                        isPremium = isPremiumActive,
                        selectedPlanId = state.selectedPlanId ?: activePlanKey
                    )
                }

                // 1. Salva localmente para feedback imediato
                SharedPreferencesManager.setPremiumState(activity, isPremiumActive)

                // 2. Sincroniza com o Firebase
                profileRepository.updatePremiumStatus(isPremiumActive, object : UpdateProfileCallback {
                    override fun onSuccess() {}
                    override fun onFailure(message: String) {
                        _uiState.update { it.copy(errorMessage = message) }
                    }
                    override fun onComplete() {}
                })
            },
            onError = { message ->
                _uiState.update { it.copy(errorMessage = message, isLoading = false) }
            }
        )
    }

    fun onPlanSelect(planIdOrKey: String) {
        val key = when (planIdOrKey) {
            "bipando_premium_mensal", "plano-mensal", PlanKeys.MENSAL -> PlanKeys.MENSAL
            "bipando_premium_semestral", PlanKeys.SEMESTRAL -> PlanKeys.SEMESTRAL
            "bipando_premium_anual", PlanKeys.ANUAL -> PlanKeys.ANUAL
            else -> planIdOrKey
        }
        _uiState.update { it.copy(selectedPlanId = key) }
    }

    fun purchaseSelectedPlan(activity: Activity) {
        val planKey = _uiState.value.selectedPlanId ?: return
        billingManager?.purchaseSubscription(
            planKey = planKey,
            currentActivePurchaseToken = activePurchaseToken,
            currentActivePlanKey = _uiState.value.activePlanId
        )
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        billingManager?.destroy()
    }
}
