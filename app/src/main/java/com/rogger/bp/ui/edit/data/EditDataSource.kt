package com.rogger.bp.ui.edit.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.rogger.bp.data.model.PostProduct

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 19:54
 */
class EditDataSource : PostEditDataSource {

    private val TAG = "EditDataSource"
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    /**
     * Referência: users/{uid}/products
     * Igual ao HomeDataSource — mesmo utilizador, mesma colecção.
     */
    private fun productsRef(): CollectionReference? {
        val uid = getUserId() ?: return null
        return db.collection("users")
            .document(uid)
            .collection("products")
    }

    // ── 1. Actualizar produto ──────────────────────────────────────────────

    override fun updateProduct(produto: PostProduct, callback: EditCallback) {
        val ref = productsRef()
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("uid", produto.uuid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    callback.onFailure("Produto não encontrado no Firestore")
                    callback.onComplete()
                    return@addOnSuccessListener
                }

                val docRef = snapshot.documents.first().reference

                // Chaves consistentes com HomeDataSource e FireRegisterDataSource
                val updates = mapOf(
                    "name"       to produto.name,
                    "note"       to produto.note,
                    "timestamp"  to produto.timestamp,
                    "categoryId" to produto.categoryId,
                    "imageUri"   to produto.imageUri,
                    "barcode"    to produto.barcode
                )

                docRef.update(updates)
                    .addOnSuccessListener {
                        Log.d(TAG, "Produto actualizado: ${produto.uuid}")
                        callback.onSuccess(produto)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Erro ao actualizar: ${e.message}")
                        callback.onFailure(e.message ?: "Erro ao actualizar produto")
                    }
                    .addOnCompleteListener { callback.onComplete() }
            }
            .addOnFailureListener { e ->
                callback.onFailure(e.message ?: "Erro ao buscar produto")
                callback.onComplete()
            }
    }

    // ── 2. Soft-delete ────────────────────────────────────────────────────

    override fun deleteProduct(produto: PostProduct, callback: EditCallback) {
        val ref = productsRef()
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("uid", produto.uuid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    callback.onSuccess(produto)
                    callback.onComplete()
                    return@addOnSuccessListener
                }

                snapshot.documents.first().reference
                    .update(mapOf(
                        "deleted"   to true,
                        "deletedAt" to System.currentTimeMillis()
                    ))
                    .addOnSuccessListener {
                        Log.d(TAG, "Produto movido para lixeira: ${produto.uuid}")
                        callback.onSuccess(produto)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Erro ao eliminar: ${e.message}")
                        callback.onFailure(e.message ?: "Erro ao eliminar produto")
                    }
                    .addOnCompleteListener { callback.onComplete() }
            }
            .addOnFailureListener { e ->
                callback.onFailure(e.message ?: "Erro ao buscar produto")
                callback.onComplete()
            }
    }

    // ── 3. Buscar produto por ID (Legado/Local) ───────────────────────────

    override fun fetchProduct(productId: Int, callback: EditCallback) {
        val ref = productsRef()
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("id", productId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    callback.onFailure("Produto não encontrado")
                    callback.onComplete()
                    return@addOnSuccessListener
                }
                processSnapshot(snapshot, callback, productId)
            }
            .addOnFailureListener { e ->
                callback.onFailure(e.message ?: "Erro ao buscar produto")
            }
            .addOnCompleteListener { callback.onComplete() }
    }

    // ── 4. Buscar produto por UUID (Firestore) ────────────────────────────

    override fun fetchProductByUuid(uuid: String, callback: EditCallback) {
        val ref = productsRef()
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("uid", uuid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    callback.onFailure("Produto não encontrado")
                    callback.onComplete()
                    return@addOnSuccessListener
                }
                processSnapshot(snapshot, callback, 0)
            }
            .addOnFailureListener { e ->
                callback.onFailure(e.message ?: "Erro ao buscar produto")
            }
            .addOnCompleteListener { callback.onComplete() }
    }

    private fun processSnapshot(
        snapshot: com.google.firebase.firestore.QuerySnapshot,
        callback: EditCallback,
        defaultId: Int
    ) {
        val data = snapshot.documents.first().data ?: run {
            callback.onFailure("Documento inválido")
            callback.onComplete()
            return
        }

        try {
            val produto = PostProduct(
                id         = (data["id"]         as? Long)?.toInt() ?: defaultId,
                userId     = data["userId"]      as? String ?: "",
                uuid       = data["uid"]         as? String ?: "",
                name       = data["name"]        as? String ?: "",
                note       = data["note"]        as? String ?: "",
                barcode    = data["barcode"]     as? String ?: "",
                categoryId = (data["categoryId"] as? Long)?.toInt() ?: 0,
                timestamp  = data["timestamp"]   as? Long ?: 0L,
                imageUri   = data["imageUri"]    as? String ?: "",
                deleted    = data["deleted"]     as? Boolean ?: false,
                deletedAt  = data["deletedAt"]   as? Long
            )

            Log.d(TAG, "Produto carregado: ${produto.name}")
            callback.onSuccess(produto)

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao mapear produto: ${e.message}")
            callback.onFailure("Erro ao processar dados do produto")
        }
    }
}