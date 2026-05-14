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

    private fun getUsername(): String? {
        val user = auth.currentUser ?: return null
        val display = user.displayName
        if (!display.isNullOrEmpty()) {
            return display.trim().lowercase().replace(" ", "_")
        }
        return user.email?.substringBefore("@")?.lowercase()
    }

    /**
     * Referência: users/{username}/produtos
     */
    private fun produtosRef(): CollectionReference? {
        val username = getUsername() ?: return null
        return db.collection("users")
            .document(username)
            .collection("produtos")
    }

    // ── 1. Actualizar produto ──────────────────────────────────────────────

    override fun updateProduct(produto: PostProduct, callback: EditCallback) {

        val ref = produtosRef()

        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        // Localiza o documento pelo id local (Room)
        ref.whereEqualTo("id", produto.id)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {
                    callback.onFailure("Produto não encontrado no Firestore")
                    callback.onComplete()
                    return@addOnSuccessListener
                }

                val docRef = snapshot.documents.first().reference

                val updates = mapOf(
                    "name"        to produto.name,
                    "note"   to (produto.note ?: ""),
                    "timestamp"   to produto.timestamp,
                    "categoryId" to produto.categoryId,
                    "image"      to (produto.uri ?: ""),
                    "barcode" to (produto.barcode ?: "")
                )

                docRef.update(updates)
                    .addOnSuccessListener {
                        Log.d(TAG, "Produto actualizado: ${produto.id}")
                        callback.onSuccess(produto)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Erro ao actualizar: ${exception.message}")
                        callback.onFailure(exception.message ?: "Erro ao actualizar produto")
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

    // ── 2. Soft-delete ────────────────────────────────────────────────────

    override fun deleteProduct(produto: PostProduct, callback: EditCallback) {

        val ref = produtosRef()

        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("id", produto.id)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {
                    // Já não existe — considera sucesso silencioso
                    callback.onSuccess(produto)
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
                        Log.d(TAG, "Produto movido para lixeira: ${produto.id}")
                        callback.onSuccess(produto)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Erro ao eliminar: ${exception.message}")
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

    // ── 3. Buscar produto por id local ────────────────────────────────────

    override fun fetchProduct(productId: Int, callback: EditCallback) {

        val ref = produtosRef()

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

                val doc  = snapshot.documents.first()
                val data = doc.data ?: run {
                    callback.onFailure("Documento inválido")
                    callback.onComplete()
                    return@addOnSuccessListener
                }

                try {
                    val produto = PostProduct().apply {
                        id           = (data["id"] as? Long)?.toInt() ?: productId
                        userId       = data["userId"] as? String ?: ""
                        nome         = data["nome"] as? String ?: ""
                        codigoBarras = data["codigoBarras"] as? String ?: ""
                        categoryId   = (data["categoriaId"] as? Long)?.toInt() ?: 0
                        timestamp    = data["timestamp"] as? Long ?: 0L
                        note    = data["anotacoes"] as? String ?: ""
                        imagem       = data["imagem"] as? String ?: ""
                        isDeleted    = data["deleted"] as? Boolean ?: false
                        deletedAt    = data["deletedAt"] as? Long
                    }

                    Log.d(TAG, "Produto carregado: ${produto.nome}")
                    callback.onSuccess(produto)

                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao mapear produto: ${e.message}")
                    callback.onFailure("Erro ao processar dados do produto")
                }
            }
            .addOnFailureListener { exception ->
                callback.onFailure(exception.message ?: "Erro ao buscar produto")
            }
            .addOnCompleteListener {
                callback.onComplete()
            }
    }
}