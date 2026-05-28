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

 /**
  * Busca a imagem personalizada do utilizador para o [barcode].
  *
  * Retorna:
  *  - [ImageResult.CustomImage] se existir
  *  - [ImageResult.NoImage] se não existir
  *  - [ImageResult.Error] se não autenticado ou falha de rede
  */
 suspend fun fetchUserImage(barcode: String): ImageResult {
  val uid = currentUid()
   ?: return ImageResult.Error("Utilizador não autenticado")

  if (barcode.isBlank()) {
   return ImageResult.Error("Código de barras inválido")
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

 /**
  * Faz upload e salva a imagem personalizada do utilizador.
  *
  * Fluxo:
  *  1. Upload para Storage: /produtos/{uid}/{barcode}.jpg
  *  2. Obtém URL pública de download.
  *  3. Salva metadata em: users/{uid}/productImages/{barcode}
  *
  * NUNCA altera imageProdutos/{barcode} (imagem global).
  *
  * @param barcode  código de barras do produto
  * @param imageUri URI local da imagem (content://, file:// ou path absoluto)
  * @return [UploadResult.Success] com a URL pública, ou [UploadResult.Error]
  */
 suspend fun saveUserImage(barcode: String, imageUri: String): UploadResult {
  val uid = currentUid()
   ?: return UploadResult.Error("Utilizador não autenticado")

  if (barcode.isBlank() || imageUri.isBlank()) {
   return UploadResult.Error("Barcode ou URI de imagem inválidos")
  }

  return try {
   val fileUri: Uri = when {
    imageUri.startsWith("content://") -> Uri.parse(imageUri)
    imageUri.startsWith("file://")    -> Uri.parse(imageUri)
    else                              -> Uri.fromFile(File(imageUri))
   }

   // Caminho privado por utilizador — outros utilizadores não têm acesso
   val storagePath = "produtos/$uid/$barcode.jpg"
   val imageRef = storage.reference.child(storagePath)

   Log.d(TAG, "Upload imagem personalizada para: $storagePath")

   imageRef.putFile(fileUri).await()

   val downloadUrl = imageRef.downloadUrl.await().toString()

   // Salva metadata — set() aqui é intencional (cria ou substitui a imagem do utilizador)
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
   UploadResult.Error(e.message ?: "Erro ao salvar imagem personalizada")
  }
 }

 // ── 3. Remover imagem personalizada (opcional) ───────────────────────

 /**
  * Remove a imagem personalizada do utilizador para o [barcode].
  * Após remoção, o fallback para a imagem global volta a funcionar.
  *
  * @return true se removido com sucesso, false caso contrário
  */
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
