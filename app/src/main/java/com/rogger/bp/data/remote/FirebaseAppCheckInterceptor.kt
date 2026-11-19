package com.rogger.bp.data.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.appcheck.FirebaseAppCheck
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor do OkHttp que injeta o cabeçalho X-Firebase-AppCheck em requisições
 * destinadas ao Firebase Storage (ex.: firebasestorage.googleapis.com).
 *
 * Isso garante que o Coil 3 envie o token de App Check válido ao baixar imagens
 * do Firebase Storage, permitindo a verificação com o App Check Enforcement ativado.
 */
class FirebaseAppCheckInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host

        // Verifica se a requisição se destina ao Firebase Storage
        if (host.contains("firebasestorage.googleapis.com") || host.contains("storage.googleapis.com")) {
            try {
                // Obtém o token atual do App Check de forma síncrona na thread do OkHttp
                val appCheckTokenTask = FirebaseAppCheck.getInstance().getAppCheckToken(false)
                val appCheckTokenResult = Tasks.await(appCheckTokenTask)
                val token = appCheckTokenResult.token

                if (token.isNotEmpty()) {
                    val newRequest = request.newBuilder()
                        .header("X-Firebase-AppCheck", token)
                        .build()
                    return chain.proceed(newRequest)
                }
            } catch (_: Exception) {
                // Em caso de erro ao obter o token (ex.: offline), prossegue com a requisição original
            }
        }

        return chain.proceed(request)
    }
}
