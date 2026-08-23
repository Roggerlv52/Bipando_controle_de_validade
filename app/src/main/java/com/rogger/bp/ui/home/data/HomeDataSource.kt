package com.rogger.bp.ui.home.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.rogger.bp.data.dao.GroupDao
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.commun.SharedPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeDataSource(
    private val context: Context,
    private val groupDao: GroupDao
) : PostHomeDataSource {

    private val TAG = "HomeDataSource"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    /**
     * PROBLEMA 7 — Corrigido: Prioriza sempre o grupo, usando user apenas como fallback de leitura.
     */
    private fun productsRef(groupId: String? = null): CollectionReference? {
        val uid = getUserId() ?: return null
        val activeGroupId = SharedPreferencesManager.getActiveGroupId(context)
        val targetGroupId = if (!groupId.isNullOrEmpty()) groupId else activeGroupId

        return if (!targetGroupId.isNullOrEmpty()) {
            db.collection("groups").document(targetGroupId).collection("products")
        } else {
            // Fallback temporário apenas para leitura durante migração
            db.collection("users").document(uid).collection("products")
        }
    }

    override fun fetchProducts(callback: FetchProductsCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val ref = productsRef()
            ref?.get()?.addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { documentToPostProduct(it) }
                callback.onSuccess(list)
            }?.addOnFailureListener { e ->
                callback.onFailure(e.message ?: "Erro ao buscar produtos")
            }?.addOnCompleteListener { callback.onComplete() }
        }
    }

    override fun fetchProductsByCategory(categoryId: String, callback: FetchProductsCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val ref = productsRef()
            ref?.whereEqualTo("categoryId", categoryId)
                ?.get()
                ?.addOnSuccessListener { snapshot ->
                    val list = snapshot.documents.mapNotNull { documentToPostProduct(it) }
                    callback.onSuccess(list)
                }
                ?.addOnFailureListener { e ->
                    callback.onFailure(e.message ?: "Erro ao filtrar produtos")
                }
                ?.addOnCompleteListener { callback.onComplete() }
        }
    }

    override fun deleteProduct(product: PostProduct, callback: HomeCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val uid = getUserId() ?: return@launch
            
            if (product.groupId.isNotEmpty()) {
                val hasPermission = checkUserPermission(uid, product.groupId)
                if (!hasPermission) {
                    callback.onFailure("Apenas Administradores ou Editores podem mover para lixeira.")
                    return@launch
                }
            }

            val ref = productsRef(product.groupId) ?: return@launch
            val updateData = mapOf("deleted" to true, "deletedAt" to System.currentTimeMillis())

            if (product.firestoreDocId.isNotEmpty()) {
                ref.document(product.firestoreDocId).update(updateData)
                    .addOnSuccessListener { 
                        cleanupProductImageIfNoMore(product)
                        callback.onSuccess(product) 
                    }
                    .addOnFailureListener { deleteByUuid(ref, product, updateData, callback) }
            } else {
                deleteByUuid(ref, product, updateData, callback)
            }
        }
    }

    override fun restoreProduct(product: PostProduct, callback: HomeCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val uid = getUserId() ?: return@launch

            if (product.groupId.isNotEmpty()) {
                val hasPermission = checkUserPermission(uid, product.groupId)
                if (!hasPermission) {
                    callback.onFailure("Apenas Administradores ou Editores podem restaurar produtos.")
                    return@launch
                }
            }

            val ref = productsRef(product.groupId) ?: return@launch
            val updateData = mapOf("deleted" to false, "deletedAt" to null)

            if (product.firestoreDocId.isNotEmpty()) {
                ref.document(product.firestoreDocId).update(updateData)
                    .addOnSuccessListener { callback.onSuccess(product) }
                    .addOnFailureListener { deleteByUuid(ref, product, updateData, callback) }
            } else {
                deleteByUuid(ref, product, updateData, callback)
            }
        }
    }

    private suspend fun checkUserPermission(uid: String, groupId: String): Boolean {
        val cachedRole = SharedPreferencesManager.getCachedRole(context, groupId)
        if (cachedRole != null) {
            return cachedRole == "Admin" || cachedRole == "Editor"
        }

        return try {
            val memberDoc = db.collection("groups").document(groupId)
                .collection("members").document(uid).get().await()
            
            val role = if (memberDoc.exists()) memberDoc.getString("role") ?: "Reader" else "Reader"
            SharedPreferencesManager.setCachedRole(context, groupId, role)
            role == "Admin" || role == "Editor"
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar permissões: ${e.message}")
            false
        }
    }

    private fun cleanupProductImageIfNoMore(product: PostProduct) {
        val uid = getUserId() ?: return
        val barcode = product.barcode
        if (barcode.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ref = productsRef(product.groupId) ?: return@launch
                val activeProductsQuery = ref.whereEqualTo("barcode", barcode)
                    .whereEqualTo("deleted", false)
                    .limit(1)
                    .get().await()

                if (activeProductsQuery.isEmpty) {
                    val imgRef = if (product.groupId.isNotEmpty()) {
                        db.collection("groups").document(product.groupId).collection("productImages")
                    } else {
                        db.collection("users").document(uid).collection("productImages")
                    }
                    imgRef.document(barcode).delete().await()
                    Log.d(TAG, "Limpando imagem para barcode: $barcode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao limpar imagem: ${e.message}")
            }
        }
    }

    private fun deleteByUuid(ref: CollectionReference, product: PostProduct, updateData: Map<String, Any?>, callback: HomeCallback) {
        ref.whereEqualTo("uid", product.uuid).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    snapshot.documents.first().reference.update(updateData)
                        .addOnSuccessListener { callback.onSuccess(product) }
                        .addOnFailureListener { callback.onFailure(it.message ?: "Erro ao atualizar") }
                } else {
                    callback.onFailure("Produto não encontrado")
                }
            }
            .addOnFailureListener { callback.onFailure(it.message ?: "Erro na busca") }
    }

    override fun addProductsSnapshotListener(groupId: String?, callback: FetchProductsCallback): ListenerRegistration? {
        val ref = productsRef(groupId)
        if (ref == null) {
            callback.onFailure("Utilizador não autenticado")
            return null
        }

        return ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                callback.onFailure(error.message ?: "Erro no listener")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { documentToPostProduct(it) }
                callback.onSuccess(list)
            }
        }
    }

    private fun documentToPostProduct(doc: DocumentSnapshot): PostProduct? {
        return try {
            val data = doc.data ?: return null
            val name = data["name"] as? String ?: return null
            PostProduct(
                firestoreDocId = doc.id,
                id = (data["id"] as? Long)?.toInt() ?: 0,
                userId = data["userId"] as? String ?: "",
                uuid = data["uid"] as? String ?: doc.id,
                name = name,
                note = data["note"] as? String ?: "",
                barcode = data["barcode"] as? String ?: "",
                categoryId = data["categoryId"] as? String ?: "",
                categoryName = data["categoryName"] as? String ?: "",
                timestamp = data["timestamp"] as? Long ?: 0L,
                imageUri = data["imageUri"] as? String ?: "",
                deleted = data["deleted"] as? Boolean ?: false,
                deletedAt = data["deletedAt"] as? Long,
                groupId = data["groupId"] as? String ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}
