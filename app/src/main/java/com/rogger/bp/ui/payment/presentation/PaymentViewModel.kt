package com.rogger.bp.ui.payment.presentation

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rogger.bp.ui.commun.SharedPreferencesManager
import com.rogger.bp.ui.payment.BillingManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PaymentState(
    val selectedPlanId: String? = null,
    val activePlanId: String? = null,
    val mensalPrice: String = "",
    val semestralPrice: String = "",
    val mensalTrialText: String? = null,
    val semestralTrialText: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class PaymentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentState())
    val uiState: StateFlow<PaymentState> = _uiState.asStateFlow()

    private var billingManager: BillingManager? = null

    fun initBilling(activity: Activity) {
        if (billingManager != null) return

        billingManager = BillingManager(
            context = activity,
            activity = activity,
            onPricesLoaded = { mensalPrice, semestralPrice, mensalTrial, semestralTrial ->
                _uiState.update { 
                    it.copy(
                        mensalPrice = mensalPrice,
                        semestralPrice = semestralPrice,
                        mensalTrialText = mensalTrial,
                        semestralTrialText = semestralTrial
                    ) 
                }
            },
            onSubscriptionStatusLoaded = { activeProductId ->
                _uiState.update { it.copy(activePlanId = activeProductId) }
                val premiumAtivo = activeProductId != null
                SharedPreferencesManager.setPremiumState(activity, premiumAtivo)
                
                // Pré-selecionar plano ativo se nada estiver selecionado
                if (_uiState.value.selectedPlanId == null && activeProductId != null) {
                    onPlanSelect(activeProductId)
                }
            }
        )
    }

    fun onPlanSelect(planId: String) {
        _uiState.update { it.copy(selectedPlanId = planId) }
    }

    fun purchaseSelectedPlan(activity: Activity) {
        val planId = _uiState.value.selectedPlanId ?: return
        billingManager?.purchaseSubscription(planId)
    }

    override fun onCleared() {
        super.onCleared()
        billingManager?.destroy()
    }
}
