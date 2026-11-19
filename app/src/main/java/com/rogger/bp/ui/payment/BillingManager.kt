package com.rogger.bp.ui.payment

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.rogger.bp.R
import com.rogger.bp.ui.commun.SharedPreferencesManager

object PlanKeys {
    const val MENSAL = "MENSAL"
    const val SEMESTRAL = "SEMESTRAL"
    const val ANUAL = "ANUAL"
}

object BasePlanIds {
    const val MENSAL = "bipando-premium-mensal"
    // ATENÇÃO: O ID cadastrado no Play Console possui erro de digitação ("bipamdo" com "m")
    // e é imutável no console. Mantemos a string exata para correta correspondência.
    const val SEMESTRAL = "bipamdo-premium-semestral"
    const val ANUAL = "bipando-premium-anual"
}

object ProductIds {
    const val PRODUCT_BIPANDO_PREMIUM = "bipando_premium"

    // Constantes mantidas APENAS para reconhecer assinantes existentes de produtos antigos
    const val LEGACY_MENSAL_1 = "bipando_premium_mensal"
    const val LEGACY_MENSAL_2 = "plano-mensal"
    const val LEGACY_SEMESTRAL = "bipando_premium_semestral"
}

data class PlanInfo(
    val basePlanId: String,
    val price: String = "",
    val trialText: String? = null,
    val isAvailable: Boolean = false
)

class BillingManager(
    private val context: Context,
    private val activity: Activity,
    private val onPricesLoaded: (
        mensalInfo: PlanInfo,
        semestralInfo: PlanInfo,
        anualInfo: PlanInfo,
        isEligibleForTrial: Boolean
    ) -> Unit,
    private val onSubscriptionStatusLoaded: (activePlanKey: String?, activePurchaseToken: String?) -> Unit = { _, _ -> },
    private val onError: (message: String) -> Unit = {}
) : PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient

    init {
        setupBillingClient()
    }

    private fun setupBillingClient() {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()

        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(pendingPurchasesParams)
            .build()

        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySubscriptionProducts()
                    queryActiveSubscriptions()
                } else {
                    activity.runOnUiThread {
                        onError("Erro ao conectar à Play Store (Código: ${result.responseCode})")
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                startConnection()
            }
        })
    }

    /**
     * Consulta o histórico de compras/assinaturas na Play Store para verificar se a conta
     * da Play Store já realizou alguma assinatura deste aplicativo no passado.
     * Aceita tanto o novo ID (bipando_premium) quanto os antigos.
     */
    fun checkSubscriptionHistory(onResult: (hasHadPreviousSubscription: Boolean) -> Unit) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                val hasHadPrevious = purchases.any { purchase ->
                    purchase.products.any { id ->
                        id == ProductIds.PRODUCT_BIPANDO_PREMIUM ||
                                id == ProductIds.LEGACY_MENSAL_1 ||
                                id == ProductIds.LEGACY_MENSAL_2 ||
                                id == ProductIds.LEGACY_SEMESTRAL
                    }
                }
                activity.runOnUiThread {
                    onResult(hasHadPrevious)
                }
            } else {
                activity.runOnUiThread {
                    onResult(false)
                }
            }
        }
    }

    /**
     * Consulta o produto de assinatura "bipando_premium" (SUBS) na Play Store,
     * agrupa as ofertas por basePlanId e devolve as informações de cada plano.
     */
    private fun querySubscriptionProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(ProductIds.PRODUCT_BIPANDO_PREMIUM)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryProductDetailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                activity.runOnUiThread {
                    onError("Erro ao consultar planos de assinatura (Código: ${result.responseCode})")
                    onPricesLoaded(
                        PlanInfo(BasePlanIds.MENSAL),
                        PlanInfo(BasePlanIds.SEMESTRAL),
                        PlanInfo(BasePlanIds.ANUAL),
                        false
                    )
                }
                return@queryProductDetailsAsync
            }

            val productDetails = queryProductDetailsResult.productDetailsList.firstOrNull {
                it.productId == ProductIds.PRODUCT_BIPANDO_PREMIUM
            }

            val offerDetailsList = productDetails?.subscriptionOfferDetails ?: emptyList()

            // Agrupa ofertas por basePlanId
            val mensalOffers = offerDetailsList.filter {
                it.basePlanId == BasePlanIds.MENSAL || it.basePlanId.endsWith("mensal")
            }
            val semestralOffers = offerDetailsList.filter {
                // ATENÇÃO: ID tem "bipamdo" com "m" por erro de digitação cadastrado e imutável no Play Console.
                it.basePlanId == BasePlanIds.SEMESTRAL || it.basePlanId.endsWith("semestral")
            }
            val anualOffers = offerDetailsList.filter {
                it.basePlanId == BasePlanIds.ANUAL || it.basePlanId.endsWith("anual")
            }

            checkSubscriptionHistory { hasHadPreviousSubscription ->
                val isEligible = !hasHadPreviousSubscription

                val mensalInfo = buildPlanInfo(BasePlanIds.MENSAL, mensalOffers, isEligible)
                val semestralInfo = buildPlanInfo(BasePlanIds.SEMESTRAL, semestralOffers, isEligible)
                val anualInfo = buildPlanInfo(BasePlanIds.ANUAL, anualOffers, isEligible)

                activity.runOnUiThread {
                    onPricesLoaded(mensalInfo, semestralInfo, anualInfo, isEligible)
                }
            }
        }
    }

    private fun buildPlanInfo(
        basePlanId: String,
        offers: List<ProductDetails.SubscriptionOfferDetails>,
        isEligibleForTrial: Boolean
    ): PlanInfo {
        if (offers.isEmpty()) {
            return PlanInfo(basePlanId = basePlanId, isAvailable = false)
        }

        // a) Oferta de teste grátis (pricingPhase com priceAmountMicros == 0), se existir
        val trialOffer = offers.firstOrNull { offer ->
            offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
        }
        // b) Oferta base (sem fase gratuita)
        val baseOffer = offers.firstOrNull { offer ->
            offer.pricingPhases.pricingPhaseList.none { it.priceAmountMicros == 0L }
        } ?: offers.firstOrNull()

        val selectedOffer = if (isEligibleForTrial) (trialOffer ?: baseOffer) else baseOffer

        // Preço exibido = formattedPrice da última pricingPhase da oferta escolhida
        val regularPrice = selectedOffer?.pricingPhases
            ?.pricingPhaseList?.lastOrNull()
            ?.formattedPrice
            ?: baseOffer?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
            ?: ""

        // Texto do teste grátis apenas para planos que tenham fase gratuita e usuário elegível
        var trialText: String? = null
        if (isEligibleForTrial && selectedOffer != null) {
            for (phase in selectedOffer.pricingPhases.pricingPhaseList) {
                if (phase.priceAmountMicros == 0L) {
                    val duration = parseBillingPeriod(phase.billingPeriod, context)
                    trialText = context.getString(R.string.trial_duration_text, duration, regularPrice)
                    break
                }
            }
        }

        return PlanInfo(
            basePlanId = basePlanId,
            price = regularPrice,
            trialText = trialText,
            isAvailable = true
        )
    }

    /**
     * Consulta as assinaturas ativas na Play Store.
     * Para produtos antigos, o plano ativo vem do productId.
     * Para bipando_premium, utiliza o registro salvo localmente no SharedPreferencesManager.
     */
    fun queryActiveSubscriptions() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                val activePurchase = purchases.firstOrNull { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                if (activePurchase != null) {
                    var activePlanKey: String? = null
                    val activePurchaseToken = activePurchase.purchaseToken

                    for (productId in activePurchase.products) {
                        when (productId) {
                            ProductIds.LEGACY_MENSAL_1, ProductIds.LEGACY_MENSAL_2 -> {
                                activePlanKey = PlanKeys.MENSAL
                            }
                            ProductIds.LEGACY_SEMESTRAL -> {
                                activePlanKey = PlanKeys.SEMESTRAL
                            }
                            ProductIds.PRODUCT_BIPANDO_PREMIUM -> {
                                // Para o produto novo, a API de cliente Android da Billing Library não informa o basePlanId
                                // no objeto Purchase. Recuperamos a chave do plano salva localmente via SharedPreferencesManager.
                                //
                                // TODO: Em produção com servidor backend, o basePlanId da assinatura "bipando_premium"
                                // deve ser validado via Google Play Developer API (subscriptionsv2).
                                activePlanKey = SharedPreferencesManager.getActivePlanKey(context) ?: PlanKeys.MENSAL
                            }
                        }
                        if (activePlanKey != null) break
                    }

                    activity.runOnUiThread {
                        onSubscriptionStatusLoaded(activePlanKey, activePurchaseToken)
                    }
                } else {
                    activity.runOnUiThread {
                        onSubscriptionStatusLoaded(null, null)
                    }
                }
            } else {
                activity.runOnUiThread {
                    onError("Erro ao consultar assinaturas ativas (Código: ${result.responseCode})")
                    onSubscriptionStatusLoaded(null, null)
                }
            }
        }
    }

    /**
     * Inicia a compra/assinatura do plano indicado (MENSAL, SEMESTRAL, ANUAL).
     */
    fun purchaseSubscription(
        planKey: String,
        currentActivePurchaseToken: String? = null,
        currentActivePlanKey: String? = null
    ) {
        val targetBasePlanId = when (planKey) {
            PlanKeys.MENSAL -> BasePlanIds.MENSAL
            PlanKeys.SEMESTRAL -> BasePlanIds.SEMESTRAL
            PlanKeys.ANUAL -> BasePlanIds.ANUAL
            else -> BasePlanIds.MENSAL
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(ProductIds.PRODUCT_BIPANDO_PREMIUM)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryProductDetailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                activity.runOnUiThread {
                    onError("Erro ao consultar detalhes do produto (Código: ${result.responseCode})")
                }
                return@queryProductDetailsAsync
            }

            val productDetails = queryProductDetailsResult.productDetailsList?.firstOrNull()
            if (productDetails == null) {
                activity.runOnUiThread {
                    onError("Produto de assinatura não encontrado na Play Store.")
                }
                return@queryProductDetailsAsync
            }

            val offerDetailsList = productDetails.subscriptionOfferDetails ?: emptyList()
            val planOffers = offerDetailsList.filter { offer ->
                offer.basePlanId == targetBasePlanId || offer.basePlanId.endsWith(planKey.lowercase())
            }

            if (planOffers.isEmpty()) {
                activity.runOnUiThread {
                    onError("O plano selecionado não está disponível para esta conta ou região.")
                }
                return@queryProductDetailsAsync
            }

            checkSubscriptionHistory { hasHadPreviousSubscription ->
                val isEligible = !hasHadPreviousSubscription

                val selectedOffer = if (isEligible) {
                    planOffers.firstOrNull { offer ->
                        offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
                    } ?: planOffers.firstOrNull()
                } else {
                    planOffers.firstOrNull { offer ->
                        offer.pricingPhases.pricingPhaseList.none { it.priceAmountMicros == 0L }
                    } ?: planOffers.firstOrNull()
                }

                val offerToken = selectedOffer?.offerToken
                if (offerToken.isNullOrEmpty()) {
                    activity.runOnUiThread {
                        onError("Oferta do plano não encontrada.")
                    }
                    return@checkSubscriptionHistory
                }

                // Salva preventivamente a chave do plano que está sendo comprado
                SharedPreferencesManager.setActivePlanKey(context, planKey)

                val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)

                val billingFlowParamsBuilder = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))

                // Troca de plano: se já existe assinatura ativa e o plano é diferente
                if (!currentActivePurchaseToken.isNullOrEmpty() && currentActivePlanKey != null && currentActivePlanKey != planKey) {
                    val currentLevel = getPlanLevel(currentActivePlanKey)
                    val newLevel = getPlanLevel(planKey)

                    // Escolha do ReplacementMode:
                    // - CHARGE_PRORATED_PRICE (Upgrade): Cobra a diferença proporcional imediatamente e aplica o novo plano agora.
                    // - DEFERRED (Downgrade): Aplica a mudança diferida no próximo vencimento para não cancelar prematuramente o plano mais longo.
                    val replacementMode = if (newLevel > currentLevel) {
                        BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE
                    } else {
                        BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED
                    }

                    val subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                        .setOldPurchaseToken(currentActivePurchaseToken)
                        .setSubscriptionReplacementMode(replacementMode)
                        .build()

                    billingFlowParamsBuilder.setSubscriptionUpdateParams(subscriptionUpdateParams)
                }

                activity.runOnUiThread {
                    val flowResult = billingClient.launchBillingFlow(activity, billingFlowParamsBuilder.build())
                    if (flowResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        onError("Erro ao iniciar janela de pagamento (Código: ${flowResult.responseCode})")
                    }
                }
            }
        }
    }

    private fun getPlanLevel(planKey: String): Int {
        return when (planKey) {
            PlanKeys.MENSAL -> 1
            PlanKeys.SEMESTRAL -> 2
            PlanKeys.ANUAL -> 3
            else -> 1
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
            queryActiveSubscriptions()
        } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            activity.runOnUiThread {
                onError("Erro na compra: ${billingResult.debugMessage} (Código: ${billingResult.responseCode})")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(params) { acknowledgeResult ->
                if (acknowledgeResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    activity.runOnUiThread {
                        onError("Erro ao confirmar compra: ${acknowledgeResult.debugMessage}")
                    }
                }
            }
        }
    }

    private fun parseBillingPeriod(period: String, context: Context): String {
        val regex = """P(\d+)([DWMY])""".toRegex()
        val matchResult = regex.matchEntire(period)
            ?: return context.resources.getQuantityString(R.plurals.duration_days, 30, 30)

        val value = matchResult.groupValues[1].toIntOrNull() ?: 30
        val unit = matchResult.groupValues[2]

        return when (unit) {
            "D" -> context.resources.getQuantityString(R.plurals.duration_days, value, value)
            "W" -> context.resources.getQuantityString(R.plurals.duration_weeks, value, value)
            "M" -> context.resources.getQuantityString(R.plurals.duration_months, value, value)
            "Y" -> context.resources.getQuantityString(R.plurals.duration_years, value, value)
            else -> context.resources.getQuantityString(R.plurals.duration_days, value, value)
        }
    }

    fun destroy() {
        billingClient.endConnection()
    }
}
