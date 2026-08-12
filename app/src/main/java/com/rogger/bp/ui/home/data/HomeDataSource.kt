package com.rogger.bp.ui.home.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.rogger.bp.data.dao.GroupDao
import com.rogger.bp.data.model.PostProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.content.Context

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 13/05/2026
 * Hora: 22:30
 */
class HomeDataSource(
    private val context: Context,
    private val groupDao: GroupDao
) : PostHomeDataSource {

    private val TAG = "HomeDataSource"

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    private fun productsRef(groupId: String? = null): CollectionReference? {
        val uid = getUserId() ?: return null
        
        return if (!groupId.isNullOrEmpty()) {
            // 🚀 Modo Colaborativo: Produtos ficam na coleção do grupo
            db.collection("groups")
                .document(groupId)
                .collection("products")
        } else {
            // Modo Privado: Produtos ficam na coleção do usuário
            db.collection("users")
                .document(uid)
                .collection("products")
        }
    }

    private fun documentToPostProduct(doc: DocumentSnapshot, groupId: String? = null): PostProduct? {
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
                groupId    = groupId ?: (data["groupId"] as? String ?: "")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao mapear produto: ${e.message}")
            null
        }
    }

    override fun fetchProducts(callback: FetchProductsCallback) {
        // Chamada única (sem listener) — geralmente usada em sync inicial
        CoroutineScope(Dispatchers.IO).launch {
            val activeGroup = groupDao.getGroup()
            val ref = productsRef(activeGroup?.groupId)
            
            ref?.get()?.addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { documentToPostProduct(it, activeGroup?.groupId) }
                callback.onSuccess(list)
            }?.addOnFailureListener { e ->
                callback.onFailure(e.message ?: "Erro")
            }?.addOnCompleteListener { callback.onComplete() }
        }
    }

    override fun fetchProductsByCategory(categoryId: String, callback: FetchProductsCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val activeGroup = groupDao.getGroup()
            val ref = productsRef(activeGroup?.groupId)

            ref?.whereEqualTo("categoryId", categoryId)
                ?.get()
                ?.addOnSuccessListener { snapshot ->
                    val list = snapshot.documents.mapNotNull { documentToPostProduct(it, activeGroup?.groupId) }
                    callback.onSuccess(list)
                }
                ?.addOnFailureListener { e ->
                    callback.onFailure(e.message ?: "Erro")
                }
                ?.addOnCompleteListener { callback.onComplete() }
        }
    }

    override fun deleteProduct(product: PostProduct, callback: HomeCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val uid = getUserId() ?: return@launch
            
            // 1. Verificar permissões se for produto de grupo
            if (product.groupId.isNotEmpty() && product.groupId != uid) {
                try {
                    val memberDoc = db.collection("groups").document(product.groupId)
                        .collection("members").document(uid).get().await()
                    
                    if (memberDoc.exists()) {
                        val role = memberDoc.getString("role") ?: "Reader"
                        if (role != "Admin" && role != "Editor") {
                            callback.onFailure("Apenas Administradores ou Editores podem mover para lixeira.")
                            return@launch
                        }
                    } else {
                        // Se não encontrou o documento de membro e não é o Admin (checado acima), nega.
                        callback.onFailure("Você não tem permissão para excluir produtos neste grupo.")
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao verificar permissão de exclusão: ${e.message}")
                    callback.onFailure("Erro de conexão ao verificar permissões.")
                    return@launch
                }
            }

            // 2. Executar o delete (Soft Delete) no local correto
            val ref = productsRef(product.groupId) ?: return@launch
            
            val updateData = mapOf(
                "deleted" to true, 
                "deletedAt" to System.currentTimeMillis()
            )

            // Tenta deletar pelo ID do documento primeiro
            if (product.firestoreDocId.isNotEmpty()) {
                ref.document(product.firestoreDocId)
                    .update(updateData)
                    .addOnSuccessListener { 
                        cleanupProductImageIfNoMore(product)
                        callback.onSuccess(product) 
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Falha ao deletar por DocId, tentando por UUID: ${e.message}")
                        deleteByUuid(ref, product, updateData, callback)
                    }
            } else {
                deleteByUuid(ref, product, updateData, callback)
            }
        }
    }

    private fun cleanupProductImageIfNoMore(product: PostProduct) {
        val uid = getUserId() ?: return
        val barcode = product.barcode
        if (barcode.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Verificar se existem outros produtos ATIVOS com este barcode
                val activeProductsQuery = db.collection("users").document(uid).collection("products")
                    .whereEqualTo("barcode", barcode)
                    .whereEqualTo("deleted", false)
                    .get().await()

                if (activeProductsQuery.isEmpty) {
                    // Se não houver mais produtos ativos, removemos a associação de imagem personalizada
                    // Isso limpa o "banco" conforme solicitado quando o produto é removido da lista principal
                    db.collection("users").document(uid)
                        .collection("productImages").document(barcode)
                        .delete().await()
                    Log.d(TAG, "Limpando metadados de imagem (Soft Delete) para barcode: $barcode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao limpar imagem em soft delete: ${e.message}")
            }
        }
    }

    private fun deleteByUuid(
        ref: CollectionReference, 
        product: PostProduct, 
        updateData: Map<String, Any?>, 
        callback: HomeCallback
    ) {
        ref.whereEqualTo("uid", product.uuid).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    snapshot.documents.first().reference
                        .update(updateData)
                        .addOnSuccessListener { callback.onSuccess(product) }
                        .addOnFailureListener { e -> callback.onFailure(e.message ?: "Erro ao atualizar") }
                } else {
                    callback.onFailure("Produto não encontrado no Firestore")
                }
            }
            .addOnFailureListener { e -> callback.onFailure(e.message ?: "Erro na busca") }
    }

    override fun restoreProduct(product: PostProduct, callback: HomeCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val uid = getUserId() ?: return@launch

            // 1. Verificar permissões se for produto de grupo
            if (product.groupId.isNotEmpty() && product.groupId != uid) {
                try {
                    val memberDoc = db.collection("groups").document(product.groupId)
                        .collection("members").document(uid).get().await()

                    if (memberDoc.exists()) {
                        val role = memberDoc.getString("role") ?: "Reader"
                        if (role != "Admin" && role != "Editor") {
                            callback.onFailure("Apenas Administradores ou Editores podem restaurar produtos.")
                            return@launch
                        }
                    } else {
                        callback.onFailure("Sem permissão para restaurar.")
                        return@launch
                    }
                } catch (e: Exception) {
                    callback.onFailure("Erro ao verificar permissões.")
                    return@launch
                }
            }

            val ref = productsRef(product.groupId) ?: return@launch
            val updateData = mapOf("deleted" to false, "deletedAt" to null)

            if (product.firestoreDocId.isNotEmpty()) {
                ref.document(product.firestoreDocId)
                    .update(updateData)
                    .addOnSuccessListener { callback.onSuccess(product) }
                    .addOnFailureListener { deleteByUuid(ref, product, updateData, callback) }
            } else {
                deleteByUuid(ref, product, updateData, callback)
            }
        }
    }

    override fun addProductsSnapshotListener(groupId: String?, callback: FetchProductsCallback): ListenerRegistration? {
        val ref = productsRef(groupId)
        if (ref == null) {
            callback.onFailure("Utilizador não autenticado")
            return null
        }

        return ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Erro no listener de produtos: ${error.message}")
                callback.onFailure(error.message ?: "Erro no listener")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { documentToPostProduct(it, groupId) }
                Log.d(TAG, "Produtos atualizados via listener (Grupo=$groupId): ${list.size}")
                callback.onSuccess(list)
            }
        }
    }
}