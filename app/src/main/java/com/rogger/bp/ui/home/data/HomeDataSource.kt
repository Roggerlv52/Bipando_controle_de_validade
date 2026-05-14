package com.rogger.bp.ui.home.data

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.data.model.UserAuth

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 13/05/2026
 * Hora: 22:30
 */
class HomeDataSource : PostHomeDataSource {

    private val TAG = "HomeDataSource"

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun getUserId(): String? = auth.currentUser?.uid

    /**
     * Referência: users/{uid}/products
     */
    private fun productsRef(): CollectionReference? {
        val uid = getUserId() ?: return null
        return db.collection("users")
            .document(uid)
            .collection("products")
    }

    /**
     * Converte um [Map] do Firestore para [PostProduct].
     * Retorna null se campos obrigatórios estiverem ausentes.
     */
    private fun documentToPostProduct(data: Map<String, Any?>): PostProduct? {
        return try {
            PostProduct(
                uuid      = data["uid"] as? String ?: return null,
                uri       = Uri.parse(data["imageUri"] as? String ?: ""),
                name      = data["name"] as? String ?: return null,
                note      = data["note"] as? String ?: "",
                barcode   = data["barcode"] as? String ?: "",
                categoryId = (data["categoryId"] as? Long)?.toInt() ?: 0,
                deleted   = data["deleted"] as? Boolean ?: false,
                timestamp = data["timestamp"] as? Long ?: 0L,
                publisher = UserAuth(
                    uuid     = data["uid"] as? String ?: "",
                    name     = "",
                    email    = "",
                    password = "",
                    photoUri = null
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao mapear produto: ${e.message}")
            null
        }
    }

    // ── Buscar todos os produtos activos ──────────────────────────────────

    override fun fetchProducts(callback: FetchProductsCallback) {

        val ref = productsRef()

        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("deleted", false)
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { snapshot ->

                val list = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { documentToPostProduct(it) }
                }

                Log.d(TAG, "Produtos carregados: ${list.size}")
                callback.onSuccess(list)
            }
            .addOnFailureListener { exception ->

                Log.e(TAG, "Erro ao buscar produtos: ${exception.message}")
                callback.onFailure(exception.message ?: "Erro ao buscar produtos")
            }
            .addOnCompleteListener {
                callback.onComplete()
            }
    }

    // ── Buscar produtos por categoria ─────────────────────────────────────

    override fun fetchProductsByCategory(
        categoryId: Int,
        callback: FetchProductsCallback
    ) {

        val ref = productsRef()

        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("deleted", false)
            .whereEqualTo("categoryId", categoryId)
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { snapshot ->

                val list = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { documentToPostProduct(it) }
                }

                Log.d(TAG, "Produtos da categoria $categoryId: ${list.size}")
                callback.onSuccess(list)
            }
            .addOnFailureListener { exception ->

                Log.e(TAG, "Erro ao buscar por categoria: ${exception.message}")
                callback.onFailure(exception.message ?: "Erro ao buscar produtos por categoria")
            }
            .addOnCompleteListener {
                callback.onComplete()
            }
    }

    // ── Soft-delete de produto ─────────────────────────────────────────────

    override fun deleteProduct(
        product: PostProduct,
        callback: HomeCallback
    ) {

        val ref = productsRef()

        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("uid", product.uuid)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {
                    callback.onFailure("Produto não encontrado")
                    callback.onComplete()
                    return@addOnSuccessListener
                }

                val docRef = snapshot.documents.first().reference

                docRef.update(
                    mapOf(
                        "deleted"   to true,
                        "deletedAt" to System.currentTimeMillis()
                    )
                )
                    .addOnSuccessListener {
                        Log.d(TAG, "Produto eliminado (soft): ${product.uuid}")
                        callback.onSuccess(product)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Erro ao eliminar produto: ${exception.message}")
                        callback.onFailure(exception.message ?: "Erro ao eliminar produto")
                    }
                    .addOnCompleteListener {
                        callback.onComplete()
                    }
            }
            .addOnFailureListener { exception ->

                callback.onFailure(exception.message ?: "Erro ao buscar produto")
                callback.onComplete()
            }
    }

    // ── Restaurar produto eliminado ────────────────────────────────────────

    override fun restoreProduct(
        product: PostProduct,
        callback: HomeCallback
    ) {

        val ref = productsRef()

        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("uid", product.uuid)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {
                    callback.onFailure("Produto não encontrado")
                    callback.onComplete()
                    return@addOnSuccessListener
                }

                val docRef = snapshot.documents.first().reference

                docRef.update(
                    mapOf(
                        "deleted"   to false,
                        "deletedAt" to null
                    )
                )
                    .addOnSuccessListener {
                        Log.d(TAG, "Produto restaurado: ${product.uuid}")
                        callback.onSuccess(product)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Erro ao restaurar produto: ${exception.message}")
                        callback.onFailure(exception.message ?: "Erro ao restaurar produto")
                    }
                    .addOnCompleteListener {
                        callback.onComplete()
                    }
            }
            .addOnFailureListener { exception ->

                callback.onFailure(exception.message ?: "Erro ao buscar produto")
                callback.onComplete()
            }
    }
}