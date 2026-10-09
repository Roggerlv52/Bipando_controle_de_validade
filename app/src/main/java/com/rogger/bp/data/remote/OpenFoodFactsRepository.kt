package com.rogger.bp.data.remote

import android.util.Log
import com.rogger.bp.data.model.PostImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class OpenFoodFactsRepository {
    private val client = OkHttpClient()

    suspend fun fetchProduct(barcode: String): PostImage? = withContext(Dispatchers.IO) {
        val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Bipando - Android - Version 2.0.1")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                
                if (json.optInt("status") == 1) {
                    val productJson = json.optJSONObject("product") ?: return@withContext null
                    
                    // Prioridade para product_name_pt ou product_name_en ou generic_name
                    val name = productJson.optString("product_name_pt")
                        .ifEmpty { productJson.optString("product_name") }
                        .ifEmpty { productJson.optString("generic_name") }

                    val imageUrl = productJson.optString("image_url")
                        .ifEmpty { productJson.optString("image_front_url") }
                    
                    if (name.isNotEmpty()) {
                        return@withContext PostImage(
                            barcode = barcode,
                            name = name,
                            uri = imageUrl
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OpenFoodFacts", "Error fetching product $barcode: ${e.message}")
        }
        null
    }
}
