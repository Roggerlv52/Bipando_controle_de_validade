package com.rogger.bp.data.image.datasource

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rogger.bp.data.image.ImageResult
import com.rogger.bp.data.image.UploadResult
import com.rogger.bp.data.model.PostImage
import com.rogger.bp.ui.commun.NetworkUtils
import com.rogger.bp.util.ImagePikerUtil
import com.rogger.bp.util.ImageUtils
import kotlinx.coroutines.tasks.await
import java.io.File

class GlobalImageDataSource {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun fetchGlobalImage(barcode: String): ImageResult {
        if (barcode.isBlank()) {
            return ImageResult.NoImage
        }

        if (!NetworkUtils.isNetworkAvailable()) {
            return ImageResult.NoImage
        }

        return try {
            val barcodeClean = barcode.trim()

            var snapshot = db.collection("imageProdutos")
                .document(barcodeClean)
                .get()
                .await()

            if (!snapshot.exists()) {
                val query = db.collection("imageProdutos")
                    .whereEqualTo("barcode", barcodeClean)
                    .limit(1)
                    .get()
                    .await()

                if (!query.isEmpty) {
                    snapshot = query.documents.first()
                }
            }

            if (snapshot.exists()) {
                val image = snapshot.toObject(PostImage::class.java)

                val resolvedName = if (image?.name?.isNotEmpty() == true) image.name else {
                    snapshot.getString("nomeProduto")
                        ?: snapshot.getString("name")
                        ?: snapshot.getString("productName")
                        ?: ""
                }

                val resolvedUri = if (image?.uri?.isNotEmpty() == true) image.uri else {
                    snapshot.getString("imageUri")
                        ?: snapshot.getString("uri")
                        ?: snapshot.getString("url")
                        ?: snapshot.getString("imageUrl")
                        ?: ""
                }

                if (resolvedUri.isNotEmpty()) {
                    ImageResult.GlobalImage(url = resolvedUri, name = resolvedName)
                } else {
                    ImageResult.NoImage
                }
            } else {
                ImageResult.NoImage
            }
        } catch (e: Exception) {
            ImageResult.Error(e.message ?: "Erro ao buscar imagem global")
        }
    }

    suspend fun createGlobalImageIfAbsent(
        context: Context,
        barcode: String,
        productName: String,
        imageUri: String
    ): UploadResult {
        if (barcode.isBlank() || imageUri.isBlank()) {
            return UploadResult.Error("Barcode ou URI de imagem inválidos")
        }

        if (!NetworkUtils.isNetworkAvailable()) {
            return UploadResult.Error("OFFLINE")
        }

        var tempFile: File? = null
        return try {
            val existing = db.collection("imageProdutos")
                .document(barcode)
                .get()
                .await()

            if (existing.exists()) {
                val image = existing.toObject(PostImage::class.java)
                if (image != null && image.uri.isNotEmpty()) {
                    return UploadResult.Error("ALREADY_EXISTS:${image.uri}")
                }
            }

            val sourceUri: Uri = when {
                imageUri.startsWith("content://") -> Uri.parse(imageUri)
                imageUri.startsWith("file://")    -> Uri.parse(imageUri)
                else                              -> Uri.fromFile(File(imageUri))
            }

            tempFile = ImagePikerUtil.createImageFile(context)
            ImageUtils.processImage(context, sourceUri, tempFile)
            val processedUri = Uri.fromFile(tempFile)

            val imageRef = storage.reference
                .child("imagens_produtos/$barcode.jpg")

            val metadata = com.google.firebase.storage.storageMetadata {
                contentType = "image/jpeg"
            }

            imageRef.putFile(processedUri, metadata).await()

            val downloadUrl = imageRef.downloadUrl.await().toString()

            val postImage = PostImage(
                barcode = barcode,
                name    = productName,
                uri     = downloadUrl
            )

            db.collection("imageProdutos")
                .document(barcode)
                .set(postImage)
                .await()

            UploadResult.Success(downloadUrl)

        } catch (e: Exception) {
            if (e.message?.contains("offline", ignoreCase = true) == true ||
                e.message?.contains("unavailable", ignoreCase = true) == true) {
                return UploadResult.Error("OFFLINE")
            }
            UploadResult.Error(e.message ?: "Erro ao criar imagem global")
        } finally {
            tempFile?.let { ImagePikerUtil.cleanUpTempFiles(it) }
        }
    }
}
