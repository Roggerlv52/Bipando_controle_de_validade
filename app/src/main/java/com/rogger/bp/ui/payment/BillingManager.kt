package com.rogger.bp.ui.payment

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.PendingPurchasesParams
import com.rogger.bp.R

class BillingManager(
    private val context: Context,
    private val activity: Activity,

    private val onPricesLoaded: (
        mensalPrice: String,
        semestralPrice: String,
        mensalTrialText: String?,
        semestralTrialText: String?,
        isEligibleForTrial: Boolean
    ) -> Unit,
    private val onSubscriptionStatusLoaded: (activeProductId: String?) -> Unit = {}
) : PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient

    // IDs dos produtos na Play Console
    val productMensalId = "bipando_premium_mensal"
    val productPlanoMensalId = "plano-mensal"
    val productSemestralId = "bipando_premium_semestral"

    init {
        setupBillingClient()
    }

    private fun setupBillingClient() {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts() // Obrigatório para suportar transações pendentes
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
                    Log.d("Billing", "Conectado com sucesso ao Google Play Billing")
                    querySubscriptionProducts()
                    queryActiveSubscriptions()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d("Billing", "Desconectado do serviço, tentando novamente...")
                startConnection()
            }
        })
    }

    /**
     * Consulta as compras/assinaturas na Play Store para verificar se a conta
     * da Play Store já realizou alguma assinatura deste aplicativo no passado.
     */
    fun checkSubscriptionHistory(onResult: (hasHadPreviousSubscription: Boolean) -> Unit) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                val hasHadPrevious = purchases.any { purchase ->
                    purchase.products.any { id ->
                        id == productMensalId || id == productPlanoMensalId || id == productSemestralId
                    }
                }
                Log.d("Billing", "checkSubscriptionHistory: hasHadPreviousSubscription=$hasHadPrevious (purchases count=${purchases.size})")
                activity.runOnUiThread {
                    onResult(hasHadPrevious)
                }
            } else {
                Log.w("Billing", "checkSubscriptionHistory error: ${result.debugMessage}")
                activity.runOnUiThread {
                    onResult(false)
                }
            }
        }
    }

    // Busca os preços dos produtos e valida a elegibilidade do trial nativamente
    private fun querySubscriptionProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productMensalId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productPlanoMensalId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productSemestralId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryProductDetailsResult ->
            val productList = queryProductDetailsResult.productDetailsList

            if (result.responseCode == BillingClient.BillingResponseCode.OK && productList != null) {
                var mensalPrice = ""
                var semestralPrice = ""
                var rawMensalTrialText: String? = null
                var rawSemestralTrialText: String? = null

                for (productDetails in productList) {
                    val offerDetailsList = productDetails.subscriptionOfferDetails ?: emptyList()

                    val trialOffer = offerDetailsList.firstOrNull { offer ->
                        offer.offerId == "teste-30-dias" ||
                                offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
                    }
                    val selectedOffer = trialOffer ?: offerDetailsList.firstOrNull()

                    val regularPrice = selectedOffer?.pricingPhases
                        ?.pricingPhaseList?.lastOrNull()
                        ?.formattedPrice
                        ?: selectedOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                        ?: ""

                    var detectedTrialText: String? = null
                    if (selectedOffer != null) {
                        for (phase in selectedOffer.pricingPhases.pricingPhaseList) {
                            if (phase.priceAmountMicros == 0L) {
                                val duration = parseBillingPeriod(phase.billingPeriod, context)
                                detectedTrialText = context.getString(R.string.trial_duration_text, duration, regularPrice)
                                break
                            }
                        }
                    }

                    if (productDetails.productId == productMensalId || productDetails.productId == productPlanoMensalId) {
                        if (regularPrice.isNotEmpty()) mensalPrice = regularPrice
                        rawMensalTrialText = detectedTrialText
                    } else if (productDetails.productId == productSemestralId) {
                        if (regularPrice.isNotEmpty()) semestralPrice = regularPrice
                        rawSemestralTrialText = detectedTrialText
                    }
                }

                // ✅ Consulta as compras da Play Store para validar se o usuário é elegível ao Teste Grátis
                checkSubscriptionHistory { hasHadPreviousSubscription ->
                    val isEligible = !hasHadPreviousSubscription
                    val finalMensalTrial = if (isEligible) rawMensalTrialText else null
                    val finalSemestralTrial = if (isEligible) rawSemestralTrialText else null

                    activity.runOnUiThread {
                        onPricesLoaded(mensalPrice, semestralPrice, finalMensalTrial, finalSemestralTrial, isEligible)
                    }
                }
            } else {
                Log.e("Billing", "Falha ao consultar produtos: ${result.debugMessage}")
            }
        }
    }

    /**
     * Consulta as assinaturas ativas do usuário na Play Store.
     * Retorna o productId do plano ativo, ou null se não houver assinatura.
     */
    fun queryActiveSubscriptions() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val activePurchase = purchases.firstOrNull { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                val activeProductId = activePurchase?.products?.firstOrNull { productId ->
                    productId == productMensalId || productId == productPlanoMensalId || productId == productSemestralId
                }

                activity.runOnUiThread {
                    onSubscriptionStatusLoaded(activeProductId)
                }
            } else {
                Log.e("Billing", "Falha ao consultar assinaturas: ${result.debugMessage}")
                activity.runOnUiThread {
                    onSubscriptionStatusLoaded(null)
                }
            }
        }
    }

    /**
     * Inicia o fluxo de compra/assinatura de um plano.
     */
    fun purchaseSubscription(productId: String) {
        val targetIds = if (productId == productMensalId || productId == productPlanoMensalId) {
            listOf(productMensalId, productPlanoMensalId)
        } else {
            listOf(productId)
        }

        val productListParams = targetIds.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productListParams)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryProductDetailsResult ->
            val productList = queryProductDetailsResult.productDetailsList

            if (result.responseCode == BillingClient.BillingResponseCode.OK && !productList.isNullOrEmpty()) {
                val productDetails = productList[0]
                val offerDetailsList = productDetails.subscriptionOfferDetails ?: emptyList()

                // ✅ Consulta o histórico nativo da Play Store para selecionar a oferta apropriada
                checkSubscriptionHistory { hasHadPreviousSubscription ->
                    val isEligible = !hasHadPreviousSubscription

                    val selectedOffer = if (isEligible) {
                        offerDetailsList.firstOrNull { offer ->
                            offer.offerId == "teste-30-dias" ||
                                    offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
                        } ?: offerDetailsList.firstOrNull()
                    } else {
                        // Se o usuário já teve assinatura anterior na Play Store, seleciona o plano regular sem teste grátis
                        offerDetailsList.firstOrNull { offer ->
                            offer.pricingPhases.pricingPhaseList.none { it.priceAmountMicros == 0L }
                        } ?: offerDetailsList.firstOrNull()
                    }

                    val offerToken = selectedOffer?.offerToken ?: ""

                    val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()

                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(listOf(productDetailsParams))
                        .build()

                    activity.runOnUiThread {
                        billingClient.launchBillingFlow(activity, billingFlowParams)
                    }
                }
            } else {
                Log.e("Billing", "Falha ao consultar detalhes do produto para compra: ${result.debugMessage}")
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
            queryActiveSubscriptions()
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d("Billing", "Usuário cancelou o fluxo de compra")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("Billing", "Assinatura confirmada e liberada!")
                }
            }
        }
    }

    private fun parseBillingPeriod(period: String, context: Context): String {
        val regex = """P(\d+)([DWMY])""".toRegex()
        val matchResult = regex.matchEntire(period) ?: return "30 dias"

        val value = matchResult.groupValues[1].toInt()
        val unit = matchResult.groupValues[2]

        return when (unit) {
            "D" -> if (value == 1) "1 dia" else "$value dias"
            "W" -> if (value == 1) "1 semana" else "$value semanas"
            "M" -> if (value == 1) "1 mês" else "$value meses"
            "Y" -> if (value == 1) "1 ano" else "$value anos"
            else -> "30 dias"
        }
    }

    fun destroy() {
        billingClient.endConnection()
    }
}
