package com.rogger.bp.ui.payment

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*

class BillingManager(
    private val context: Context,
    private val activity: Activity,
    private val onPricesLoaded: (mensalPrice: String, semestralPrice: String) -> Unit // 👉 Callback para atualizar a UI
) : PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient

    // 👉 Nossos dois IDs criados na Play Console
    private val productMensalId = "bipando_premium_mensal"
    private val productSemestralId = "bipando_premium_semestral"

    init {
        setupBillingClient()
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("Billing", "Conectado com sucesso ao Google Play Billing")
                    querySubscriptionProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d("Billing", "Desconectado do serviço, tentando novamente...")
                startConnection()
            }
        })
    }

    // 👉 Método corrigido para buscar ASSINATURAS (SUBS) de forma dinâmica
    private fun querySubscriptionProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productMensalId)
                        .setProductType(BillingClient.ProductType.SUBS) // Tipo SUBS para Assinaturas
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productSemestralId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && productList != null) {
                var mensalPrice = "R$ 14,99"
                var semestralPrice = "R$ 77,70"

                for (productDetails in productList) {
                    // Extrai o preço formatado localizado do Google Play (ex: R$ 14,99 ou € 14,99)
                    val price = productDetails.subscriptionOfferDetails?.getOrNull(0)
                        ?.pricingPhases?.pricingPhaseList?.getOrNull(0)?.formattedPrice ?: ""

                    if (productDetails.productId == productMensalId) {
                        mensalPrice = price
                    } else if (productDetails.productId == productSemestralId) {
                        semestralPrice = price
                    }
                }

                // Devolve os preços localizados para o fragmento via UI thread
                activity.runOnUiThread {
                    onPricesLoaded(mensalPrice, semestralPrice)
                }
            } else {
                Log.e("Billing", "Falha ao consultar produtos: ${result.debugMessage}")
            }
        }
    }

    // Iniciar fluxo de compra de uma assinatura
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

        billingClient.queryProductDetailsAsync(params) { result, productList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && !productList.isNullOrEmpty()) {
                val productDetails = productList[0]
                val offerToken = productDetails.subscriptionOfferDetails?.getOrNull(0)?.offerToken ?: ""

                val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                    .build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
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