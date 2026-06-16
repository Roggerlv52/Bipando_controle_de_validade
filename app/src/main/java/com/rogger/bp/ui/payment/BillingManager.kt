package com.rogger.bp.ui.payment

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.PendingPurchasesParams

class BillingManager(
    private val context: Context,
    private val activity: Activity,
    private val onPricesLoaded: (mensalPrice: String, semestralPrice: String) -> Unit,
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

        // ✅ Correção: O segundo parâmetro agora é "queryProductDetailsResult" (do tipo QueryProductDetailsResult)
        billingClient.queryProductDetailsAsync(params) { result, queryProductDetailsResult ->
            // Extraímos a lista real de produtos do objeto de resultado da v9.0.0
            val productList = queryProductDetailsResult.productDetailsList

            if (result.responseCode == BillingClient.BillingResponseCode.OK && productList != null) {
                var mensalPrice = "R$ 14,99"
                var semestralPrice = "R$ 77,70"

                for (productDetails in productList) {
                    // Chamadas explícitas de getters evitam problemas de conversão sintética de tipos
                    val firstOffer = productDetails.getSubscriptionOfferDetails()?.getOrNull(0)
                    val price = firstOffer?.getPricingPhases()
                        ?.getPricingPhaseList()?.getOrNull(0)
                        ?.getFormattedPrice() ?: ""

                    if (productDetails.productId == productMensalId) {
                        mensalPrice = price
                    } else if (productDetails.productId == productSemestralId) {
                        semestralPrice = price
                    }
                }

                activity.runOnUiThread {
                    onPricesLoaded(mensalPrice, semestralPrice)
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

    fun destroy() {
        billingClient.endConnection()
    }
}