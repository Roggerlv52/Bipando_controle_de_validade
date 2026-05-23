package com.rogger.bp.ui.edit.data

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rogger.bp.data.model.PostProduct
import java.io.File

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 19:54
 */
class EditDataSource : PostEditDataSource {

    private val TAG = "EditDataSource"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    private fun productsRef(): CollectionReference? {
        val uid = getUserId() ?: return null
        return db.collection("users").document(uid).collection("products")
    }

    // ── 1. Buscar pelo documentId ─────────────────────────────────────────

    override fun fetchProductByDocId(docId: String, callback: EditCallback) {
        val ref = productsRef()
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.document(docId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    callback.onFailure("Produto não encontrado")
                    callback.onComplete()
                    return@addOnSuccessListener
                }

                val data = doc.data ?: run {
                    callback.onFailure("Documento inválido")
                    callback.onComplete()
                    return@addOnSuccessListener
                }

                try {
                    val uidField = data["uid"] as? String ?: ""
                    val uuid = if (uidField.isNotEmpty()) uidField else doc.id

                    val produto = PostProduct(
                        id         = (data["id"]         as? Long)?.toInt() ?: 0,
                        userId     = data["userId"]      as? String ?: "",
                        uuid       = uuid,
                        name       = data["name"]        as? String ?: "",
                        note       = data["note"]        as? String ?: "",
                        barcode    = data["barcode"]     as? String ?: "",
                        categoryId = data["categoryId"] as? String ?: "",
                        timestamp  = data["timestamp"]   as? Long ?: 0L,
                        imageUri   = data["imageUri"]    as? String ?: "",
                        deleted    = data["deleted"]     as? Boolean ?: false,
                        deletedAt  = data["deletedAt"]   as? Long
                    )

                    Log.d(TAG, "Produto carregado: ${produto.name} (docId=$docId)")
                    callback.onSuccess(produto)

                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao mapear produto: ${e.message}")
                    callback.onFailure("Erro ao processar dados do produto")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao buscar produto: ${e.message}")
                callback.onFailure(e.message ?: "Erro ao buscar produto")
            }
            .addOnCompleteListener { callback.onComplete() }
    }

    // ── 2. Actualizar produto — com upload de imagem se necessário ─────────

    override fun updateProduct(produto: PostProduct, callback: EditCallback) {
        val ref = productsRef()
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        val imageUri = produto.imageUri

        // BUGFIX: detecta se imageUri é um caminho local que precisa de upload.
        // Caminhos locais começam com "/" (absoluto) ou "file://".
        // URLs remotas já válidas começam com "http" ou "https".
        val isLocalPath = imageUri.isNotEmpty() &&
                (imageUri.startsWith("/") || imageUri.startsWith("file://"))

        if (isLocalPath) {
            Log.d(TAG, "imageUri é local — fazendo upload antes de salvar: $imageUri")
            uploadImageAndUpdate(ref, produto, imageUri, callback)
        } else {
            Log.d(TAG, "imageUri já é remota — atualizando Firestore diretamente")
            updateFirestoreDoc(ref, produto, imageUri, callback)
        }
    }

    /**
     * Faz upload do arquivo local para o Firebase Storage e,
     * ao obter a URL de download pública, chama [updateFirestoreDoc].
     *
     * Caminho no Storage: user_images/{uid}/{uuid}/product.jpg
     * Isso garante que cada produto tem sua própria imagem isolada
     * e a URL é permanente, independente de reinstalação.
     */
    private fun uploadImageAndUpdate(
        ref: CollectionReference,
        produto: PostProduct,
        localPath: String,
        callback: EditCallback
    ) {
        // Normaliza o caminho para Uri — suporta path absoluto e file://
        val fileUri: Uri = if (localPath.startsWith("file://")) {
            Uri.parse(localPath)
        } else {
            Uri.fromFile(File(localPath))
        }

        // Verifica que o arquivo existe localmente antes de tentar o upload
        val file = File(localPath.removePrefix("file://"))
        if (!file.exists()) {
            Log.e(TAG, "Arquivo local não encontrado: $localPath")
            callback.onFailure("Arquivo de imagem não encontrado no dispositivo")
            callback.onComplete()
            return
        }

        // BUGFIX: caminho deve seguir o mesmo padrão do AddFragment/FireRegisterDataSource:
        //   imagens_produtos/{barcode}/imagem.jpg
        // As regras do Firebase Storage foram configuradas para esse path.
        // Qualquer outro caminho (ex: user_images/) recebe "permission denied".
        //
        // Se o produto não tiver barcode (campo vazio), usa o uuid como fallback
        // para garantir que o upload sempre tenha um caminho válido e único.
        val bucketKey = if (produto.barcode.isNotEmpty()) produto.barcode else produto.uuid
        val storagePath = "imagens_produtos/$bucketKey/imagem.jpg"
        val imageRef = storage.reference.child(storagePath)

        Log.d(TAG, "Iniciando upload para: $storagePath")

        imageRef.putFile(fileUri)
            .addOnSuccessListener {
                // Upload concluído — busca a URL pública de download
                imageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        val remoteUrl = downloadUri.toString()
                        Log.d(TAG, "Upload concluído. URL: $remoteUrl")
                        // Agora salva a URL remota no Firestore
                        updateFirestoreDoc(ref, produto, remoteUrl, callback)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Erro ao obter URL de download: ${e.message}")
                        callback.onFailure("Erro ao obter URL da imagem: ${e.message}")
                        callback.onComplete()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro no upload da imagem: ${e.message}")
                callback.onFailure("Erro ao enviar imagem: ${e.message}")
                callback.onComplete()
            }
    }

    /**
     * Salva os campos do produto no Firestore usando [imageUri] como a
     * URL da imagem (pode ser a URL remota recém-obtida ou a original).
     */
    private fun updateFirestoreDoc(
        ref: CollectionReference,
        produto: PostProduct,
        imageUri: String,
        callback: EditCallback
    ) {
        val updates = mapOf(
            "name"       to produto.name,
            "note"       to produto.note,
            "timestamp"  to produto.timestamp,
            "categoryId" to produto.categoryId,
            "imageUri"   to imageUri,   // ← sempre uma URL remota ou vazia
            "barcode"    to produto.barcode
        )

        ref.document(produto.uuid)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Produto actualizado no Firestore: ${produto.uuid}")
                // Retorna o produto com a URI já corrigida para o presenter
                callback.onSuccess(produto.copy(imageUri = imageUri))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao actualizar Firestore: ${e.message}")
                callback.onFailure(e.message ?: "Erro ao actualizar produto")
            }
            .addOnCompleteListener { callback.onComplete() }
    }

    // ── 3. Soft-delete pelo documentId ────────────────────────────────────

    override fun deleteProduct(produto: PostProduct, callback: EditCallback) {
        val ref = productsRef()
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        ref.document(produto.uuid)
            .update(
                mapOf(
                    "deleted"   to true,
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
