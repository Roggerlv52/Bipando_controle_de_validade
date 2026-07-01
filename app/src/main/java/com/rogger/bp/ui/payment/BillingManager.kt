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

    // ✅ Correção: agora envia o texto de trial SEPARADO por plano (Mensal/Semestral),
    // em vez de um único texto genérico que era exibido independente do plano selecionado.
    private val onPricesLoaded: (
        mensalPrice: String,
        semestralPrice: String,
        mensalTrialText: String?,
        semestralTrialText: String?
    ) -> Unit,
    private val onSubscriptionStatusLoaded: (activeProductId: String?) -> Unit = {}
) : PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient

    // IDs dos produtos na Play Console
    val productMensalId = "bipando_premium_mensal"
    val productSemestralId = "bipando_premium_semestral"

    init {
        setupBillingClient()
    }

    private fun setupBillingClient() {
        // ✅ Correção: Criação do parâmetro obrigatório exigido na v9.0.0
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts() // Obrigatório para suportar transações pendentes
            .build()

        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(pendingPurchasesParams) // Passa o parâmetro obrigatório
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

    // Busca os preços dos produtos de forma dinâmica
    private fun querySubscriptionProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productMensalId)
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
                var mensalPrice = "R$ 14,99"
                var semestralPrice = "R$ 77,70"
                var mensalTrialText: String? = null    // ✅ Trial específico do plano Mensal
                var semestralTrialText: String? = null // ✅ Trial específico do plano Semestral

                for (productDetails in productList) {
                    val firstOffer = productDetails.getSubscriptionOfferDetails()?.getOrNull(0)
                    val pricingPhases = firstOffer?.getPricingPhases()?.getPricingPhaseList()

                    val regularPrice = firstOffer?.getPricingPhases()
                        ?.getPricingPhaseList()?.getOrNull(0)
                        ?.getFormattedPrice() ?: ""

                    // ✅ DETEÇÃO DINÂMICA DE TESTE GRATUITO (FREE TRIAL)
                    // Antes só era verificado para o Mensal; agora verifica qualquer plano,
                    // já que o Google Play permite configurar trial em qualquer oferta.
                    var detectedTrialText: String? = null
                    if (pricingPhases != null) {
                        for (phase in pricingPhases) {
                            if (phase.getPriceAmountMicros() == 0L) {
                                // Detetou fase gratuita! Lê a duração (ex: P1M ou P30D)
                                val duration = parseBillingPeriod(phase.getBillingPeriod(), context)
                                detectedTrialText = context.getString(R.string.trial_duration_text, duration, regularPrice)
                                break
                            }
                        }
                    }

                    if (productDetails.productId == productMensalId) {
                        mensalPrice = regularPrice
                        mensalTrialText = detectedTrialText
                    } else if (productDetails.productId == productSemestralId) {
                        semestralPrice = regularPrice
                        semestralTrialText = detectedTrialText
                    }
                }

                activity.runOnUiThread {
                    // ✅ Envia os dados completos e o estado da promoção, por plano
                    onPricesLoaded(mensalPrice, semestralPrice, mensalTrialText, semestralTrialText)
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

                // Identifica qual plano está ativo (mensal ou semestral)
                val activeProductId = activePurchase?.products?.firstOrNull { productId ->
                    productId == productMensalId || productId == productSemestralId
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
     * O Google Play cuida automaticamente de upgrades/downgrades entre planos.
     */
    fun purchaseSubscription(productId: String) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        // ✅ Correção: O segundo parâmetro agora é "queryProductDetailsResult" (do tipo QueryProductDetailsResult)
        billingClient.queryProductDetailsAsync(params) { result, queryProductDetailsResult ->
            // Extraímos a lista real de produtos
            val productList = queryProductDetailsResult.productDetailsList

            if (result.responseCode == BillingClient.BillingResponseCode.OK && productList != null && productList.isNotEmpty()) {
                val productDetails = productList[0] // O indexador [0] agora funciona corretamente

                val offerToken = productDetails.getSubscriptionOfferDetails()
                    ?.getOrNull(0)
                    ?.getOfferToken() ?: ""

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
            // Atualiza o status após uma compra/mudança de plano
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