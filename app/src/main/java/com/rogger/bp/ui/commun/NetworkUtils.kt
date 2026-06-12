package com.rogger.bp.ui.commun

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.FirebaseApp

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 04/06/2026
 * Hora: 11:14
 */
object NetworkUtils {
    fun isNetworkAvailable(): Boolean {
        return try {
            val context = FirebaseApp.getInstance().applicationContext
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false

            // 👉 NET_CAPABILITY_VALIDATED garante que o Android testou e confirmou tráfego real de internet
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            false // Fallback seguro: assume offline em caso de erro de inicialização
        }
    }
}