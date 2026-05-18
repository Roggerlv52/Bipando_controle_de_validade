package com.rogger.bp.ui.deleteitem.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.category.data.CategoryCallback

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 17/05/2026
 * Hora: 16:25
 */
class DeleteItemDataSource : PostDeletedItem {
    private val TAG = "DeleteItemDataSource"

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private fun getUserId(): String? = auth.currentUser?.uid

    private fun productsRef(): CollectionReference? {
        val uid = getUserId() ?: return null
        return db.collection("users")
            .document(uid)
            .collection("products")
    }

    override fun restoreItemDeleted(
        item: PostProduct,
        callback: DeleteItemCallback
    ) {
        // ✅ CORREÇÃO: implementado (estava vazio)
        val ref = productsRef()
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("barcode", item.barcode)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                if (doc == null) {
                    callback.onFailure("Produto não encontrado")
                    callback.onComplete()
                    return@addOnSuccessListener
                }
                doc.reference.update("deleted", false)
                    .addOnSuccessListener {
                        Log.d(TAG, "Produto restaurado: ${item.name}")
                        callback.onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Erro ao restaurar: ${e.message}")
                        callback.onFailure(e.message ?: "Erro ao restaurar produto")
                    }
                    .addOnCompleteListener { callback.onComplete() }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao buscar produto para restaurar: ${e.message}")
                callback.onFailure(e.message ?: "Erro ao buscar produto")
                callback.onComplete()
            }
    }

    override fun deletePermanently(
        product: PostProduct,
        callback: DeleteItemCallback
    ) {
        // ✅ CORREÇÃO: implementado (estava vazio)
        val ref = productsRef()
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("barcode", product.barcode)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                if (doc == null) {
                    callback.onFailure("Produto não encontrado")
                    callback.onComplete()
                    return@addOnSuccessListener
                }
                doc.reference.delete()
                    .addOnSuccessListener {
                        Log.d(TAG, "Produto excluído definitivamente: ${product.name}")
                        callback.onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Erro ao excluir: ${e.message}")
                        callback.onFailure(e.message ?: "Erro ao excluir produto")
                    }
                    .addOnCompleteListener { callback.onComplete() }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao buscar produto para excluir: ${e.message}")
                callback.onFailure(e.message ?: "Erro ao buscar produto")
                callback.onComplete()
            }
    }

    override fun fetchItemDeleted(callback: DeleteItemCallback) {
        val ref = productsRef()
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("deleted", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { document ->
                    try {
                        PostProduct(
                            barcode = document.getString("barcode") ?: return@mapNotNull null,
                            id = (document.getLong("id") ?: 0L).toInt(),
                            name = document.getString("name") ?: return@mapNotNull null,
                            userId = document.getString("userId") ?: "",
                            imageUri = document.getString("imageUri") ?: ""
                        )
                    } catch (exception: Exception) {
                        Log.e(TAG, "Erro ao mapear: ${exception.message}")
                        null
                    }
                }
                Log.d(TAG, "Produtos carregados: ${list.size}")
                callback.onSuccess(list)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao buscar produtos: ${e.message}")
                callback.onFailure(e.message ?: "Erro ao buscar produtos")
            }
            .addOnCompleteListener { callback.onComplete() }
    }
}