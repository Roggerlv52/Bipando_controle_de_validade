package com.rogger.bp.ui.edit.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.rogger.bp.data.image.UploadResult
import com.rogger.bp.data.image.datasource.GlobalImageDataSource
import com.rogger.bp.data.image.datasource.UserImageDataSource
import com.rogger.bp.data.image.repository.ImageResolutionRepository
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.commun.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Context

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 19:54
 */
class EditDataSource(private val context: Context) : PostEditDataSource {

    private val TAG = "EditDataSource"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val imageRepository = ImageResolutionRepository(
        userImageDataSource = UserImageDataSource(),
        globalImageDataSource = GlobalImageDataSource()
    )

    private fun getUserId(): String? = auth.currentUser?.uid

    private fun productsRef(groupId: String = ""): CollectionReference? {
        val uid = getUserId() ?: return null
        return if (groupId.isNotEmpty()) {
            db.collection("groups").document(groupId).collection("products")
        } else {
            db.collection("users").document(uid).collection("products")
        }
    }

    // ── 1. Buscar pelo documentId ─────────────────────────────────────────

    override fun fetchProductByDocId(docId: String, callback: EditCallback) {
        // Como o docId é único globalmente ou sabemos o contexto, tentamos buscar no usuário primeiro
        // Mas o ideal seria passar o groupId. Por agora, vamos buscar em ambos ou usar o que temos.
        // NOTE: Na edição, o ViewModel carrega o produto que já tem o groupId.
        // Mas aqui fetchProductByDocId é chamado ANTES de ter o objeto produto completo.
        
        // Tentativa 1: Coleção de usuários
        val userRef = productsRef("")
        if (userRef == null) {
            callback.onFailure("Utilizador não autenticado")
            callback.onComplete()
            return
        }

        userRef.document(docId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                handleDocSuccess(doc, "", callback)
            } else {
                // Tentativa 2: Buscar em grupos (se o usuário tiver um grupo ativo)
                // Isso é uma limitação se não passarmos o groupId.
                // Mas geralmente o docId no Bipando contém o UUID que buscamos.
                // Vamos tentar via collectionGroup se necessário, ou assumir que o docId é o UUID.
                
                db.collectionGroup("products").whereEqualTo("uuid", docId).get()
                    .addOnSuccessListener { query ->
                        if (!query.isEmpty) {
                            val d = query.documents.first()
                            val gId = d.reference.parent.parent?.id ?: ""
                            handleDocSuccess(d, gId, callback)
                        } else {
                            callback.onFailure("Produto não encontrado")
                            callback.onComplete()
                        }
                    }
            }
        }.addOnFailureListener { e ->
            callback.onFailure(e.message ?: "Erro ao buscar")
            callback.onComplete()
        }
    }

    private fun handleDocSuccess(doc: com.google.firebase.firestore.DocumentSnapshot, groupId: String, callback: EditCallback) {
        val data = doc.data ?: run {
            callback.onFailure("Documento inválido")
            return
        }
        try {
            val uidField = data["uid"] as? String ?: ""
            val uuid = if (uidField.isNotEmpty()) uidField else doc.id

            val produto = PostProduct(
                firestoreDocId = doc.id,
                id = (data["id"] as? Long)?.toInt() ?: 0,
                userId = data["userId"] as? String ?: "",
                uuid = uuid,
                name = data["name"] as? String ?: "",
                note = data["note"] as? String ?: "",
                barcode = data["barcode"] as? String ?: "",
                categoryId = data["categoryId"] as? String ?: "",
                categoryName = data["categoryName"] as? String ?: "",
                timestamp = data["timestamp"] as? Long ?: 0L,
                imageUri = data["imageUri"] as? String ?: "",
                deleted = data["deleted"] as? Boolean ?: false,
                deletedAt = data["deletedAt"] as? Long,
                groupId = groupId
            )
            callback.onSuccess(produto)
        } catch (e: Exception) {
            callback.onFailure("Erro ao mapear produto")
        }
    }

    // ── 2. Atualizar produto ──────────────────────────────────────────────

    override fun updateProduct(produto: PostProduct, callback: EditCallback) {
        val ref = productsRef(produto.groupId)
        if (ref == null) {
            callback.onFailure("Utilizador não autenticado")
            callback.onComplete()
            return
        }
        // ... rest of the code remains similar but using the new ref

        val imageUri = produto.imageUri
        val isLocalPath = imageUri.isNotEmpty() &&
                (imageUri.startsWith("/") || imageUri.startsWith("file://") || imageUri.startsWith("content://"))

        if (isLocalPath) {
            Log.d(TAG,"Estado: "+NetworkUtils.isNetworkAvailable())
            if (!NetworkUtils.isNetworkAvailable()) {
                updateFirestoreDoc(ref, produto, imageUri, callback)
                return
            }
            handleImageUploadForEdit(ref, produto, imageUri, callback)
        } else {
            Log.d(TAG, "imageUri já é remota — atualizando Firestore diretamente")
            updateFirestoreDoc(ref, produto, imageUri, callback)
        }
    }

    /**
     * Resolve o upload de imagem no contexto de edição.
     *
     * Lógica:
     *  1. Verifica se já existe imagem GLOBAL para o barcode.
     *  2. Se NÃO existe → cria imagem global (este utilizador está a definir a imagem canónica).
     *  3. Se JÁ existe → salva imagem PERSONALIZADA do utilizador (não altera a global).
     *
     * Em ambos os casos, o produto do utilizador é atualizado com a URL resultante.
     */
    private fun handleImageUploadForEdit(
        ref: CollectionReference,
        produto: PostProduct,
        localPath: String,
        callback: EditCallback
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val globalExists = imageRepository.globalImageExists(produto.barcode)

            if (!globalExists) {
                // ── Sem imagem global: cria a imagem global ────────────────
                Log.d(TAG, "Sem imagem global — criando para barcode=${produto.barcode}")
                val result = imageRepository.uploadGlobalImage(
                    context = context,
                    barcode = produto.barcode,
                    productName = produto.name,
                    imageUri = localPath
                )
                handleUploadResult(ref, produto, result, callback)

            } else {
                // ── Imagem global existe: salva personalizada (privada) ────
                Log.d(
                    TAG,
                    "Imagem global existe — salvando personalizada para barcode=${produto.barcode}"
                )
                val result = imageRepository.saveUserImage(
                    context = context,
                    barcode = produto.barcode,
                    imageUri = localPath
                )
                handleUploadResult(ref, produto, result, callback)
            }
        }
    }

    private fun handleUploadResult(
        ref: CollectionReference,
        produto: PostProduct,
        result: UploadResult,
        callback: EditCallback
    ) {
        when {
            result is UploadResult.Success -> {
                Log.d(TAG, "Upload concluído. URL: ${result.url}")
                CoroutineScope(Dispatchers.Main).launch {
                    updateFirestoreDoc(ref, produto, result.url, callback)
                }
            }

            result is UploadResult.Error &&
                    result.message.startsWith("ALREADY_EXISTS:") -> {
                val existingUrl = result.message.removePrefix("ALREADY_EXISTS:")
                Log.w(TAG, "Race condition no upload — usando URL existente: $existingUrl")
                CoroutineScope(Dispatchers.Main).launch {
                    updateFirestoreDoc(ref, produto, existingUrl, callback)
                }
            }

            // 👉 NOVO: Trata o resultado "OFFLINE" e atualiza o Firestore localmente usando a URI local
            result is UploadResult.Error && result.message == "OFFLINE" -> {
                Log.d(
                    TAG,
                    "Falha no upload (Dispositivo Offline) — salvando localmente com URI local"
                )
                CoroutineScope(Dispatchers.Main).launch {
                    updateFirestoreDoc(ref, produto, produto.imageUri, callback)
                }
            }

            result is UploadResult.Error -> {
                Log.e(TAG, "Erro no upload: ${result.message}")
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onFailure(result.message)
                    callback.onComplete()
                }
            }
        }
    }

    private fun updateFirestoreDoc(
        ref: CollectionReference,
        produto: PostProduct,
        imageUri: String,
        callback: EditCallback
    ) {
        val updates = mapOf(
            "name" to produto.name,
            "note" to produto.note,
            "timestamp" to produto.timestamp,
            "categoryId" to produto.categoryId,
            "categoryName" to produto.categoryName,
            "imageUri" to imageUri,
            "barcode" to produto.barcode
        )

        ref.document(produto.uuid)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Produto atualizado: ${produto.uuid}")
                callback.onSuccess(produto.copy(imageUri = imageUri))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao atualizar Firestore: ${e.message}")
                callback.onFailure(e.message ?: "Erro ao atualizar produto")
            }
            .addOnCompleteListener { callback.onComplete() }
    }

    // ── 3. Soft-delete ────────────────────────────────────────────────────

    override fun deleteProduct(produto: PostProduct, callback: EditCallback) {
        val ref = productsRef(produto.groupId)
        if (ref == null) {
            callback.onFailure("Utilizador não autenticado")
            callback.onComplete()
            return
        }

        ref.document(produto.firestoreDocId)
            .update(
                mapOf(
                    "deleted" to true,
                    "deletedAt" to System.currentTimeMillis()
                )
            )
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
}
