package com.rogger.bp.ui.add.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rogger.bp.data.image.ImageResult
import com.rogger.bp.data.image.UploadResult
import com.rogger.bp.data.image.datasource.GlobalImageDataSource
import com.rogger.bp.data.image.datasource.UserImageDataSource
import com.rogger.bp.data.image.repository.ImageResolutionRepository
import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.commun.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FireRegisterDataSource(private val context: Context) : ItemDataSource {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── Repositório central de imagens ────────────────────────────────────
    private val imageRepository = ImageResolutionRepository(
        userImageDataSource   = UserImageDataSource(),
        globalImageDataSource = GlobalImageDataSource()
    )

    // ── 1. Criar produto no Firestore ─────────────────────────────────────

    override fun createItem(produto: PostProduct, callback: RegisterItemCallback) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            callback.onFailure("Utilizador não autenticado")
            callback.onComplete()
            return
        }

        // 👉 Determina o local de gravação baseado no groupId
        /*
        Melhoria futura:
        Melhora onde grava os dados se ouver membro no grupo grava em db.collection("groups")
        se nao em db.collection("users")
        Caso de usos: Se usuario adicionar membro dados produtos são migardo para grupo se o membro for add no grupo pessoal.
         */
        val docRef =
            if (produto.groupId.isNotEmpty()) {
            db.collection("groups").document(
                produto.groupId)
                .collection("products")
        } else {
            db.collection("users").document(
                uid)
                .collection("products")
        }

        val docId = if (produto.firestoreDocId.isNotEmpty()) produto.firestoreDocId else docRef.document().id
        val finalUuid = if (produto.uuid.isNotEmpty()) produto.uuid else docId

        val data = hashMapOf(
            "uid"        to finalUuid,
            "userId"     to uid,
            "id"         to produto.id,
            "imageUri"   to produto.imageUri,
            "name"       to produto.name,
            "note"       to produto.note,
            "barcode"    to produto.barcode,
            "categoryId" to produto.categoryId,
            "categoryName" to produto.categoryName,
            "deleted"    to produto.deleted,
            "timestamp"  to produto.timestamp,
        )

        // Se for produto de grupo, salva o groupId no documento
        if (produto.groupId.isNotEmpty()) {
            data["groupId"] = produto.groupId
        }

        docRef.document(docId)
            .set(data)
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener { e -> callback.onFailure(e.message.toString()) }
            .addOnCompleteListener { callback.onComplete() }
    }

    // ── 2. Verificar/criar imagem (chamado ao escanear barcode) ────────────
    override fun saveProductImage(image: PostImage, callback: SaveImageCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = imageRepository.resolveImage(image.barcode)

            when (result) {
                is ImageResult.CustomImage -> {
                    val existing = PostImage(
                        barcode = image.barcode,
                        uri     = result.url,
                        name    = image.name
                    )
                    CoroutineScope(Dispatchers.Main).launch {
                        callback.onAlreadyExists(existing)
                    }
                }

                is ImageResult.GlobalImage -> {
                    val existing = PostImage(
                        barcode = image.barcode,
                        uri     = result.url,
                        name    = result.name.ifEmpty { image.name }
                    )
                    CoroutineScope(Dispatchers.Main).launch {
                        callback.onAlreadyExists(existing)
                    }
                }

                is ImageResult.NoImage -> {
                    if (image.uri.isNotEmpty()) {
                        // Há URI local — tenta criar a imagem global
                        uploadGlobalImageInternal(image, callback)
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            callback.onFailure("Nenhuma imagem associada")
                            callback.onComplete()
                        }
                    }
                }

                is ImageResult.Error -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        callback.onFailure(result.message)
                        callback.onComplete()
                    }
                }
            }
        }
    }

    // ── 3. Upload de imagem — lógica de separação global/personalizada ─────

    /**
     * Ponto central de upload.
     *
     * Decide automaticamente se deve criar imagem global ou personalizada:
     *  - Imagem global NÃO existe → cria imagem global (1ª vez para este barcode)
     *  - Imagem global JÁ existe  → salva como imagem personalizada (privada)
     *
     * GARANTIA: a imagem global NUNCA é sobrescrita após a criação.
     */
    override fun uploadImage(image: PostImage, callback: SaveImageCallback) {
        if (image.uri.isEmpty()) {
            callback.onFailure("Nenhuma imagem selecionada")
            return
        }

        // 👉 Se estiver offline, pula o upload e conclui usando a URI local da imagem
        if (!NetworkUtils.isNetworkAvailable()) {
            callback.onSuccess(image)
            callback.onComplete()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            // Verifica se já existe imagem global
            val globalExists = imageRepository.globalImageExists(image.barcode)

            if (!globalExists) {
                // ── Caso A: sem imagem global → cria global ────────────────
                uploadGlobalImageInternal(image, callback)
            } else {
                // ── Caso B: imagem global existe → salva imagem personalizada
                saveUserImageInternal(image, callback)
            }
        }
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    private suspend fun uploadGlobalImageInternal(image: PostImage, callback: SaveImageCallback) {
        val uploadResult = imageRepository.uploadGlobalImage(
            context     = context,
            barcode     = image.barcode,
            productName = image.name,
            imageUri    = image.uri
        )

        CoroutineScope(Dispatchers.Main).launch {
            when {
                uploadResult is UploadResult.Success -> {
                    val saved = PostImage(
                        barcode = image.barcode,
                        name    = image.name,
                        uri     = uploadResult.url
                    )
                    callback.onSuccess(saved)
                    callback.onComplete()
                }

                uploadResult is UploadResult.Error &&
                        uploadResult.message.startsWith("ALREADY_EXISTS:") -> {
                    // Race condition — outro utilizador criou a global entretanto
                    val existingUrl = uploadResult.message.removePrefix("ALREADY_EXISTS:")
                    val existing = PostImage(
                        barcode = image.barcode,
                        name    = image.name,
                        uri     = existingUrl
                    )
                    callback.onAlreadyExists(existing)
                    callback.onComplete()
                }

                uploadResult is UploadResult.Error -> {
                    callback.onFailure(uploadResult.message)
                    callback.onComplete()
                }
            }
        }
    }

    private suspend fun saveUserImageInternal(image: PostImage, callback: SaveImageCallback) {
        val uploadResult = imageRepository.saveUserImage(
            context  = context,
            barcode  = image.barcode,
            imageUri = image.uri
        )

        CoroutineScope(Dispatchers.Main).launch {
            when (uploadResult) {
                is UploadResult.Success -> {
                    val saved = PostImage(
                        barcode = image.barcode,
                        name    = image.name,
                        uri     = uploadResult.url
                    )
                    callback.onSuccess(saved)
                    callback.onComplete()
                }
                is UploadResult.Error -> {
                    callback.onFailure(uploadResult.message)
                    callback.onComplete()
                }
                else -> { /* Progress não usado aqui */ }
            }
        }
    }
}
