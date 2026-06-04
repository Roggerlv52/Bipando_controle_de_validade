package com.rogger.bp.data.image.datasource

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 28/05/2026
 * Hora: 20:17
 */
import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rogger.bp.data.image.ImageResult
import com.rogger.bp.data.image.UploadResult
import com.rogger.bp.data.model.UserProductImage
import com.rogger.bp.ui.commun.NetworkUtils
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * DataSource responsável pela imagem PERSONALIZADA do utilizador.
 *
 * REGRAS:
 *  - Apenas o próprio utilizador pode ler/escrever.
 *  - Pode alterar quantas vezes quiser (não afeta a imagem global).
 *  - NÃO toca em imageProdutos/{barcode} (imagem global).
 *
 * Firestore: users/{uid}/productImages/{barcode}
 * Storage:   /produtos/{uid}/{barcode}.jpg
 */
class UserImageDataSource {

    private val TAG = "UserImageDataSource"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private fun currentUid(): String? = auth.currentUser?.uid

    // ── 1. Buscar imagem personalizada ───────────────────────────────────
    suspend fun fetchUserImage(barcode: String): ImageResult {
        val uid = currentUid()
            ?: return ImageResult.Error("Utilizador não autenticado")

        if (barcode.isBlank()) {
            return ImageResult.Error("Código de barras inválido")
        }

        // 👉 Se estiver offline, ignora a busca remota
        if (!NetworkUtils.isNetworkAvailable()) {
            Log.d(TAG, "Dispositivo offline — ignorando busca de imagem personalizada")
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
                    Log.d(TAG, "Imagem personalizada encontrada uid=$uid barcode=$barcode")
                    ImageResult.CustomImage(url = image.customImageUrl)
                } else {
                    ImageResult.NoImage
                }
            } else {
                Log.d(TAG, "Sem imagem personalizada uid=$uid barcode=$barcode")
                ImageResult.NoImage
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar imagem personalizada: ${e.message}")
            ImageResult.Error(e.message ?: "Erro ao buscar imagem personalizada")
        }
    }

    // ── 2. Salvar imagem personalizada ───────────────────────────────────

    suspend fun saveUserImage(barcode: String, imageUri: String): UploadResult {
        val uid = currentUid()
            ?: return UploadResult.Error("Utilizador não autenticado")

        if (barcode.isBlank() || imageUri.isBlank()) {
            return UploadResult.Error("Barcode ou URI de imagem inválidos")
        }

        // 👉 Bloqueia início se estiver offline
        if (!NetworkUtils.isNetworkAvailable()) { // ← NOT invertido
            return UploadResult.Error("OFFLINE")
        }
        Log.d(TAG,"Estado: "+NetworkUtils.isNetworkAvailable())
        return try {
            val fileUri: Uri = when {
                imageUri.startsWith("content://") -> Uri.parse(imageUri)
                imageUri.startsWith("file://")    -> Uri.parse(imageUri)
                else                              -> Uri.fromFile(File(imageUri))
            }

            val storagePath = "produtos/$uid/$barcode.jpg"
            val imageRef = storage.reference.child(storagePath)

            Log.d(TAG, "Upload imagem personalizada para: $storagePath")

            imageRef.putFile(fileUri).await()

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

            Log.d(TAG, "Imagem personalizada salva: uid=$uid barcode=$barcode url=$downloadUrl")
            UploadResult.Success(downloadUrl)

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar imagem personalizada: ${e.message}")
            // 👉 Trata queda de conexão durante o processo
            if (e.message?.contains("offline", ignoreCase = true) == true ||
                e.message?.contains("unavailable", ignoreCase = true) == true) {
                return UploadResult.Error("OFFLINE")
            }
            UploadResult.Error(e.message ?: "Erro ao salvar imagem personalizada")
        }
    }

    // ── 3. Remover imagem personalizada (opcional) ───────────────────────
    suspend fun removeUserImage(barcode: String): Boolean {
        val uid = currentUid() ?: return false

        return try {
            // Remove do Storage
            val storagePath = "produtos/$uid/$barcode.jpg"
            try {
                storage.reference.child(storagePath).delete().await()
            } catch (storageEx: Exception) {
                // Arquivo pode não existir no Storage — não é erro crítico
                Log.w(TAG, "Arquivo não encontrado no Storage (ignorando): $storagePath")
            }

            // Remove do Firestore
            db.collection("users")
                .document(uid)
                .collection("productImages")
                .document(barcode)
                .delete()
                .await()

            Log.d(TAG, "Imagem personalizada removida: uid=$uid barcode=$barcode")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao remover imagem personalizada: ${e.message}")
            false
        }
    }
}
