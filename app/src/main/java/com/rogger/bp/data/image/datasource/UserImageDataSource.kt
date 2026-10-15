package com.rogger.bp.data.image.datasource

import android.content.Context
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rogger.bp.data.image.ImageResult
import com.rogger.bp.data.image.UploadResult
import com.rogger.bp.data.model.UserProductImage
import com.rogger.bp.ui.commun.NetworkUtils
import com.rogger.bp.util.ImagePikerUtil
import com.rogger.bp.util.ImageUtils
import kotlinx.coroutines.tasks.await
import java.io.File

class UserImageDataSource {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private fun currentUid(): String? = auth.currentUser?.uid

    suspend fun fetchUserImage(barcode: String): ImageResult {
        val uid = currentUid()
            ?: return ImageResult.Error("Utilizador não autenticado")

        if (barcode.isBlank()) {
            return ImageResult.Error("Código de barras inválido")
        }

        if (!NetworkUtils.isNetworkAvailable()) {
            return ImageResult.NoImage
        }

        return try {
            val snapshot = db.collection("users")
                .document(uid)
                .collection("productImages")
                .document(barcode)
                .get()
                .await()

            if (snapshot.exists()) {
                val image = snapshot.toObject(UserProductImage::class.java)
                if (image != null && image.customImageUrl.isNotEmpty()) {
                    ImageResult.CustomImage(url = image.customImageUrl)
                } else {
                    ImageResult.NoImage
                }
            } else {
                ImageResult.NoImage
            }

        } catch (e: Exception) {
            ImageResult.Error(e.message ?: "Erro ao buscar imagem personalizada")
        }
    }

    suspend fun saveUserImage(context: Context, barcode: String, imageUri: String): UploadResult {
        val uid = currentUid()
            ?: return UploadResult.Error("Utilizador não autenticado")

        if (barcode.isBlank() || imageUri.isBlank()) {
            return UploadResult.Error("Barcode ou URI de imagem inválidos")
        }

        if (!NetworkUtils.isNetworkAvailable()) {
            return UploadResult.Error("OFFLINE")
        }

        var tempFile: File? = null
        return try {
            val sourceUri: Uri = when {
                imageUri.startsWith("content://") -> Uri.parse(imageUri)
                imageUri.startsWith("file://")    -> Uri.parse(imageUri)
                else                              -> Uri.fromFile(File(imageUri))
            }

            tempFile = ImagePikerUtil.createImageFile(context)
            ImageUtils.processImage(context, sourceUri, tempFile)
            val processedUri = Uri.fromFile(tempFile)

            val storagePath = "produtos/$uid/$barcode.jpg"
            val imageRef = storage.reference.child(storagePath)

            val metadata = com.google.firebase.storage.storageMetadata {
                contentType = "image/jpeg"
            }

            imageRef.putFile(processedUri, metadata).await()

            val downloadUrl = imageRef.downloadUrl.await().toString()

            val userImage = UserProductImage(
                barcode        = barcode,
                customImageUrl = downloadUrl,
                updatedAt      = Timestamp.now()
            )

            db.collection("users")
                .document(uid)
                .collection("productImages")
                .document(barcode)
                .set(userImage)
                .await()

            UploadResult.Success(downloadUrl)

        } catch (e: Exception) {
            if (e.message?.contains("offline", ignoreCase = true) == true ||
                e.message?.contains("unavailable", ignoreCase = true) == true) {
                return UploadResult.Error("OFFLINE")
            }
            UploadResult.Error(e.message ?: "Erro ao salvar imagem personalizada")
        } finally {
            tempFile?.let { ImagePikerUtil.cleanUpTempFiles(it) }
        }
    }

    suspend fun removeUserImage(barcode: String): Boolean {
        val uid = currentUid() ?: return false

        return try {
            val storagePath = "produtos/$uid/$barcode.jpg"
            try {
                storage.reference.child(storagePath).delete().await()
            } catch (_: Exception) {}

            db.collection("users")
                .document(uid)
                .collection("productImages")
                .document(barcode)
                .delete()
                .await()

            true

        } catch (_: Exception) {
            false
        }
    }
}
