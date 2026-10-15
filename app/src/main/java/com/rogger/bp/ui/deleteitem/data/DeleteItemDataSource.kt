package com.rogger.bp.ui.deleteitem.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.rogger.bp.data.dao.GroupDao
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.commun.SharedPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DeleteItemDataSource(
    private val context: Context,
    private val groupDao: GroupDao
) : PostDeletedItem {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
    private fun getUserId(): String? = auth.currentUser?.uid

    private fun productsRef(groupId: String = ""): CollectionReference? {
        val uid = getUserId() ?: return null

        return if (groupId.isNotEmpty()) {
            db.collection("groups").document(groupId).collection("products")
        } else {
            db.collection("users").document(uid).collection("products")
        }
    }

    private fun documentToPostProduct(doc: DocumentSnapshot, groupId: String = ""): PostProduct? {
        val data = doc.data ?: return null
        return try {
            val uidField = data["uid"] as? String ?: ""
            val uuid = if (uidField.isNotEmpty()) uidField else doc.id

            PostProduct(
                firestoreDocId = doc.id,
                id         = (data["id"]         as? Long)?.toInt() ?: 0,
                userId     = data["userId"]      as? String ?: "",
                uuid       = uuid,
                name       = data["name"]        as? String ?: return null,
                note       = data["note"]        as? String ?: "",
                barcode    = data["barcode"]     as? String ?: "",
                categoryId = data["categoryId"] as? String ?: "",
                categoryName = data["categoryName"] as? String ?: "",
                timestamp  = data["timestamp"]   as? Long ?: 0L,
                imageUri   = data["imageUri"]    as? String ?: "",
                deleted    = data["deleted"]     as? Boolean ?: false,
                deletedAt  = data["deletedAt"]   as? Long,
                groupId    = groupId
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun restoreItemDeleted(
        item: PostProduct,
        callback: DeleteItemCallback
    ) {
        val ref = productsRef(item.groupId)
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.document(item.firestoreDocId)
            .update("deleted", false, "deletedAt", null)
            .addOnSuccessListener {
                callback.onSuccess()
            }
            .addOnFailureListener { _ ->
                db.collectionGroup("products")
                    .whereEqualTo("uuid", item.uuid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (!snapshot.isEmpty) {
                            val doc = snapshot.documents.first()
                            doc.reference.update("deleted", false, "deletedAt", null)
                                .addOnSuccessListener { 
                                    callback.onSuccess() 
                                }
                                .addOnFailureListener { err ->
                                    callback.onFailure("Erro ao atualizar: ${err.message}")
                                }
                        } else {
                            callback.onFailure("Produto não encontrado no Firestore")
                        }
                    }
                    .addOnFailureListener { err ->
                        callback.onFailure("Erro ao buscar produto: ${err.message}")
                    }
            }
            .addOnCompleteListener { callback.onComplete() }
    }

    override fun deletePermanently(
        product: PostProduct,
        callback: DeleteItemCallback
    ) {
        val ref = productsRef(product.groupId)
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.document(product.firestoreDocId)
            .delete()
            .addOnSuccessListener {
                cleanupProductImage(product)
                callback.onSuccess()
            }
            .addOnFailureListener { _ ->
                db.collectionGroup("products")
                    .whereEqualTo("uuid", product.uuid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (!snapshot.isEmpty) {
                            val doc = snapshot.documents.first()
                            doc.reference.delete()
                                .addOnSuccessListener { 
                                    cleanupProductImage(product)
                                    callback.onSuccess() 
                                }
                                .addOnFailureListener { err ->
                                    callback.onFailure("Erro ao excluir: ${err.message}")
                                }
                        } else {
                            callback.onFailure("Produto não encontrado no Firestore")
                        }
                    }
                    .addOnFailureListener { err ->
                        callback.onFailure("Erro ao buscar produto: ${err.message}")
                    }
            }
            .addOnCompleteListener { callback.onComplete() }
    }

    private fun cleanupProductImage(product: PostProduct) {
        val uid = getUserId() ?: return
        val barcode = product.barcode
        if (barcode.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userProductsQuery = db.collection("users").document(uid).collection("products")
                    .whereEqualTo("barcode", barcode)
                    .get().await()

                val groupProductsQuery = if (product.groupId.isNotEmpty()) {
                    db.collection("groups").document(product.groupId).collection("products")
                        .whereEqualTo("barcode", barcode)
                        .get().await()
                } else null

                val totalRemaining = userProductsQuery.size() + (groupProductsQuery?.size() ?: 0)

                if (totalRemaining == 0) {
                    db.collection("users").document(uid)
                        .collection("productImages").document(barcode)
                        .delete().await()

                    try {
                        val userStoragePath = "produtos/$uid/$barcode.jpg"
                        storage.reference.child(userStoragePath).delete().await()
                    } catch (_: Exception) {}

                    if (product.groupId.isNotEmpty()) {
                        try {
                            val groupStoragePath = "produtos/${product.groupId}/$barcode.jpg"
                            storage.reference.child(groupStoragePath).delete().await()
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun fetchItemDeleted(callback: DeleteItemCallback) {
        val mode = SharedPreferencesManager.getWorkMode(context)
        val activeGroupId = SharedPreferencesManager.getActiveGroupId(context)
        
        val gId = if (mode == 1) activeGroupId else ""

        val ref = productsRef(gId)
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("deleted", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { documentToPostProduct(it, gId) }
                callback.onSuccess(list)
            }
            .addOnFailureListener { e ->
                callback.onFailure(e.message ?: "Erro ao buscar produtos")
            }
            .addOnCompleteListener { callback.onComplete() }
    }
}
