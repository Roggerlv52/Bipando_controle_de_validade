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

    }

    override fun deletePermanently(
        product: PostProduct,
        callback: DeleteItemCallback
    ) {

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
                val list = snapshot.documents.mapNotNull {document ->
                    try {

                        PostProduct(
                            barcode = document.getString("barcode")?: return@mapNotNull null,
                            id = (document.getLong("id") ?: 0L).toInt(),
                            name = document.getString("name")
                                ?: return@mapNotNull null,
                            userId = document.getString("userId") ?: "",
                            imageUri = document.getString("imageUri")?:""


                        )

                    } catch (exception: Exception) {

                        Log.e(
                            TAG,
                            "Erro ao mapear: ${exception.message}"
                        )

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