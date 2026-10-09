package com.rogger.bp.ui.commun

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsManager {
    private const val TAG = "AnalyticsManager"
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        if (firebaseAnalytics == null) {
            Log.d(TAG, "Inicializando Firebase Analytics")
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        }
    }

    fun logEvent(name: String, params: Map<String, Any?> = emptyMap()) {
        Log.d(TAG, "Enviando evento: $name | Params: $params")
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putFloat(key, value)
                is Boolean -> bundle.putBoolean(key, value)
            }
        }
        firebaseAnalytics?.logEvent(name, bundle)
    }

    fun logProductAdded(source: String) {
        logEvent("product_added", mapOf("source" to source))
    }

    fun logProductEdited() {
        logEvent("product_edited")
    }

    fun logProductDeleted() {
        logEvent("product_deleted")
    }

    fun logBarcodeScanned(source: String) {
        logEvent("barcode_scanned", mapOf("source" to source))
    }

    fun logProductSearch(source: String) {
        logEvent("product_search", mapOf("source" to source))
    }

    fun logVoiceSearch() {
        logEvent("voice_search", mapOf("source" to "microphone"))
    }

    fun logGroupCreated(source: String = "app") {
        logEvent("group_created", mapOf("source" to source))
    }

    fun logGroupJoined() {
        logEvent("group_joined")
    }

    fun logNotificationOpened() {
        logEvent("notification_opened")
    }
}
