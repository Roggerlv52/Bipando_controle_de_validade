package com.rogger.bp.ui.home.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.rogger.bp.data.model.PostProduct

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 13/05/2026
 * Hora: 22:30
 */
class HomeDataSource : PostHomeDataSource {

    private val TAG = "HomeDataSource"

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    private fun productsRef(): CollectionReference? {
        val uid = getUserId() ?: return null
        return db.collection("users")
            .document(uid)
            .collection("products")
    }

    /**
     * Converte um [DocumentSnapshot] para [PostProduct].
     *
     * ── Chave "uuid" (campo uid no Firestore) ────────────────────────────
     * O [FireRegisterDataSource] grava `"uid" to produto.uuid`.
     * Se o produto foi criado antes de ter uuid gerado, o campo
     * pode estar vazio no Firestore.
     *
     * Estratégia de fallback garantida:
     *   1. Usa `data["uid"]` se não for vazio.
     *   2. Caso contrário, usa `doc.id` — o documentId do Firestore,
     *      que é sempre único e nunca vazio.
     *
     * O [EditDataSource] busca pelo documentId directamente,
     * eliminando a dependência do campo "uid".
     */
    private fun documentToPostProduct(doc: DocumentSnapshot): PostProduct? {
        val data = doc.data ?: return null
        return try {
            val uidField = data["uid"] as? String ?: ""
            // fallback: usa o docId quando uid está vazio
            val uuid = if (uidField.isNotEmpty()) uidField else doc.id

            PostProduct(
                firestoreDocId = doc.id, // Adicionado para o Room
                id         = (data["id"]         as? Long)?.toInt() ?: 0,
                userId     = data["userId"]      as? String ?: "",
                uuid       = uuid,
                name       = data["name"]        as? String ?: return null,
                note       = data["note"]        as? String ?: "",
                barcode    = data["barcode"]     as? String ?: "",
                categoryId = data["categoryId"] as? String ?: "",
                timestamp  = data["timestamp"]   as? Long ?: 0L,
                imageUri   = data["imageUri"]    as? String ?: "",
                deleted    = data["deleted"]     as? Boolean ?: false,
                deletedAt  = data["deletedAt"]   as? Long
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
                val list = snapshot.documents.mapNotNull { documentToPostProduct(it) }
                Log.d(TAG, "Produtos carregados: ${list.size}")
                callback.onSuccess(list)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao buscar produtos: ${e.message}")
                callback.onFailure(e.message ?: "Erro ao buscar produtos")
            }
            .addOnCompleteListener { callback.onComplete() }
    }

    // ── Buscar por categoria ──────────────────────────────────────────────

    override fun fetchProductsByCategory(categoryId: String, callback: FetchProductsCallback) {
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
                val list = snapshot.documents.mapNotNull { documentToPostProduct(it) }
                Log.d(TAG, "Produtos da categoria $categoryId: ${list.size}")
                callback.onSuccess(list)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao buscar por categoria: ${e.message}")
                callback.onFailure(e.message ?: "Erro ao buscar produtos por categoria")
            }
            .addOnCompleteListener { callback.onComplete() }
    }

    // ── Soft-delete ───────────────────────────────────────────────────────

    override fun deleteProduct(product: PostProduct, callback: HomeCallback) {
        val ref = productsRef()
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        // Busca pelo docId (uuid) — garantido pelo fallback acima
        ref.document(product.firestoreDocId) // Usar firestoreDocId
            .update(mapOf("deleted" to true, "deletedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                Log.d(TAG, "Produto eliminado (soft): ${product.uuid}")
                callback.onSuccess(product)
            }
            .addOnFailureListener { e ->
                // fallback: tenta via whereEqualTo("uid")
                deleteByQuery(product, callback)
            }
    }

    private fun deleteByQuery(product: PostProduct, callback: HomeCallback) {
        productsRef()?.whereEqualTo("uid", product.uuid)?.get()
            ?.addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    callback.onFailure("Produto não encontrado")
                    callback.onComplete()
                    return@addOnSuccessListener
                }
                snapshot.documents.first().reference
                    .update(mapOf("deleted" to true, "deletedAt" to System.currentTimeMillis()))
                    .addOnSuccessListener { callback.onSuccess(product) }
                    .addOnFailureListener { e -> callback.onFailure(e.message ?: "Erro ao eliminar") }
                    .addOnCompleteListener { callback.onComplete() }
            }
    }

    // ── Restaurar ─────────────────────────────────────────────────────────

    override fun restoreProduct(product: PostProduct, callback: HomeCallback) {
        val ref = productsRef()
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.document(product.firestoreDocId) // Usar firestoreDocId
            .update(mapOf("deleted" to false, "deletedAt" to null))
            .addOnSuccessListener {
                Log.d(TAG, "Produto restaurado: ${product.uuid}")
                callback.onSuccess(product)
            }
            .addOnFailureListener { _ ->
                restoreByQuery(product, callback)
            }
    }

    private fun restoreByQuery(product: PostProduct, callback: HomeCallback) {
        productsRef()?.whereEqualTo("uid", product.uuid)?.get()
            ?.addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    callback.onFailure("Produto não encontrado")
                    callback.onComplete()
                    return@addOnSuccessListener
                }
                snapshot.documents.first().reference
                    .update(mapOf("deleted" to false, "deletedAt" to null))
                    .addOnSuccessListener { callback.onSuccess(product) }
                    .addOnFailureListener { e -> callback.onFailure(e.message ?: "Erro ao restaurar") }
                    .addOnCompleteListener { callback.onComplete() }
            }
    }

    override fun addProductsSnapshotListener(callback: FetchProductsCallback): ListenerRegistration? {
        val ref = productsRef()
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return null
        }

        return ref.whereEqualTo("deleted", false)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erro no listener de produtos: ${error.message}")
                    callback.onFailure(error.message ?: "Erro no listener de produtos")
                    callback.onComplete()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { documentToPostProduct(it) }
                    Log.d(TAG, "Produtos atualizados via listener: ${list.size}")
                    callback.onSuccess(list)
                }
            }
    }
}