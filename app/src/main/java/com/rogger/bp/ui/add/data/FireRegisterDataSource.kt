package com.rogger.bp.ui.add.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rogger.bp.data.image.ImageResult
import com.rogger.bp.data.image.UploadResult
import com.rogger.bp.data.image.datasource.GlobalImageDataSource
import com.rogger.bp.data.image.datasource.UserImageDataSource
import com.rogger.bp.data.image.repository.ImageResolutionRepository
import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FireRegisterDataSource : ItemDataSource {

    private val TAG = "FireRegisterDataSource"

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

        db.collection("users")
            .document(uid)
            .collection("products")
            .document()
            .set(
                hashMapOf(
                    "uid"        to produto.uuid,
                    "userId"     to uid,
                    "id"         to produto.id,
                    "imageUri"   to produto.imageUri,
                    "name"       to produto.name,
                    "note"       to produto.note,
                    "barcode"    to produto.barcode,
                    "categoryId" to produto.categoryId,
                    "deleted"    to produto.deleted,
                    "timestamp"  to produto.timestamp,
                )
            )
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener { e -> callback.onFailure(e.message.toString()) }
            .addOnCompleteListener { callback.onComplete() }
    }

    // ── 2. Verificar/criar imagem (chamado ao escanear barcode) ────────────

    /**
     * Verifica se já existe imagem (personalizada ou global) para o barcode.
     *
     * Se existir → [SaveImageCallback.onAlreadyExists]
     * Se não existir E não há URI → [SaveImageCallback.onComplete] (UI pede upload)
     * Se não existir E há URI → [SaveImageCallback.onSuccess] após upload global
     */
    override fun saveProductImage(image: PostImage, callback: SaveImageCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = imageRepository.resolveImage(image.barcode)

            when (result) {
                is ImageResult.CustomImage -> {
                    Log.d(TAG, "Imagem personalizada encontrada: ${result.url}")
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
                    Log.d(TAG, "Imagem global encontrada: ${result.url}")
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
                        Log.d(TAG, "Sem imagem e sem URI — aguardando upload do utilizador")
                        CoroutineScope(Dispatchers.Main).launch {
                            callback.onComplete()
                        }
                    }
                }

                is ImageResult.Error -> {
                    Log.e(TAG, "Erro ao verificar imagem: ${result.message}")
                    CoroutineScope(Dispatchers.Main).launch {
                        callback.onFailure(result.message)
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

        CoroutineScope(Dispatchers.IO).launch {
            // Verifica se já existe imagem global
            val globalExists = imageRepository.globalImageExists(image.barcode)

            if (!globalExists) {
                // ── Caso A: sem imagem global → cria global ────────────────
                Log.d(TAG, "Criando imagem global para barcode=${image.barcode}")
                uploadGlobalImageInternal(image, callback)
            } else {
                // ── Caso B: imagem global existe → salva imagem personalizada
                Log.d(TAG, "Imagem global já existe → salvando imagem personalizada para barcode=${image.barcode}")
                saveUserImageInternal(image, callback)
            }
        }
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    private suspend fun uploadGlobalImageInternal(image: PostImage, callback: SaveImageCallback) {
        val uploadResult = imageRepository.uploadGlobalImage(
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
                    Log.w(TAG, "Race condition: imagem global criada por outro utilizador, usando URL existente")
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
