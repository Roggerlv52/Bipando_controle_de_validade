package com.rogger.bp.ui.deleteitem.data

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 17/05/2026
 * Hora: 16:25
 */
class DeleteItemDataSource(
    private val context: Context,
    private val groupDao: GroupDao
) : PostDeletedItem {
    private val TAG = "DeleteItemDataSource"

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
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao mapear produto: ${e.message}")
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

        Log.d(TAG, "Restaurando item: ${item.name} (ID: ${item.firestoreDocId}, Group: ${item.groupId})")

        ref.document(item.firestoreDocId)
            .update("deleted", false, "deletedAt", null)
            .addOnSuccessListener {
                Log.d(TAG, "Produto restaurado com sucesso: ${item.name}")
                callback.onSuccess()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Erro ao restaurar via firestoreDocId: ${e.message}. Tentando via UUID.")
                
                // Fallback query by UUID em todas as coleções de produtos (usando collectionGroup para ser mais robusto)
                db.collectionGroup("products")
                    .whereEqualTo("uuid", item.uuid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (!snapshot.isEmpty) {
                            val doc = snapshot.documents.first()
                            Log.d(TAG, "Produto encontrado via UUID em: ${doc.reference.path}")
                            doc.reference.update("deleted", false, "deletedAt", null)
                                .addOnSuccessListener { 
                                    Log.d(TAG, "Produto restaurado com sucesso via fallback UUID")
                                    callback.onSuccess() 
                                }
                                .addOnFailureListener { err ->
                                    Log.e(TAG, "Erro ao atualizar documento encontrado via UUID: ${err.message}")
                                    callback.onFailure("Erro ao atualizar: ${err.message}")
                                }
                        } else {
                            Log.e(TAG, "Produto não encontrado nem via firestoreDocId nem via UUID: ${item.uuid}")
                            callback.onFailure("Produto não encontrado no Firestore")
                        }
                    }
                    .addOnFailureListener { err ->
                        Log.e(TAG, "Erro na consulta de fallback: ${err.message}")
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

        Log.d(TAG, "Excluindo permanentemente: ${product.name} (ID: ${product.firestoreDocId}, Group: ${product.groupId})")

        ref.document(product.firestoreDocId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Produto excluído definitivamente: ${product.name}")
                cleanupProductImage(product)
                callback.onSuccess()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Erro ao excluir via firestoreDocId: ${e.message}. Tentando via UUID.")
                
                db.collectionGroup("products")
                    .whereEqualTo("uuid", product.uuid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (!snapshot.isEmpty) {
                            val doc = snapshot.documents.first()
                            Log.d(TAG, "Produto para exclusão encontrado via UUID em: ${doc.reference.path}")
                            doc.reference.delete()
                                .addOnSuccessListener { 
                                    Log.d(TAG, "Produto excluído definitivamente via fallback UUID")
                                    cleanupProductImage(product)
                                    callback.onSuccess() 
                                }
                                .addOnFailureListener { err ->
                                    Log.e(TAG, "Erro ao excluir documento encontrado via UUID: ${err.message}")
                                    callback.onFailure("Erro ao excluir: ${err.message}")
                                }
                        } else {
                            Log.e(TAG, "Produto não encontrado para exclusão: ${product.uuid}")
                            callback.onFailure("Produto não encontrado no Firestore")
                        }
                    }
                    .addOnFailureListener { err ->
                        Log.e(TAG, "Erro na consulta de fallback para exclusão: ${err.message}")
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
                // 1. Verificar se ainda existe algum produto com este barcode para este usuário
                // Procuramos em products (usuário) e em todos os grupos onde ele é o dono (individual ou admin)
                val userProductsQuery = db.collection("users").document(uid).collection("products")
                    .whereEqualTo("barcode", barcode)
                    .get().await()

                if (userProductsQuery.isEmpty) {
                    // 2. Se não houver mais produtos com esse barcode, removemos a entrada em productImages
                    Log.d(TAG, "Limpando metadados de imagem para barcode: $barcode")
                    db.collection("users").document(uid)
                        .collection("productImages").document(barcode)
                        .delete().await()

                    // 3. Opcional: Remover arquivo físico do Storage se for imagem privada
                    try {
                        val storagePath = "produtos/$uid/$barcode.jpg"
                        storage.reference.child(storagePath).delete().await()
                        Log.d(TAG, "Arquivo no Storage removido: $storagePath")
                    } catch (e: Exception) {
                        // Ignora se o arquivo não existir
                    }
                } else {
                    Log.d(TAG, "Imagem mantida: ainda existem ${userProductsQuery.size()} produtos com o barcode $barcode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao limpar imagem do produto: ${e.message}")
            }
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