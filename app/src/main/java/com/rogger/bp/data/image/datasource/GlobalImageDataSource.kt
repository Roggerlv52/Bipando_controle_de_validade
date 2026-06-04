package com.rogger.bp.data.image.datasource
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rogger.bp.data.image.ImageResult
import com.rogger.bp.data.image.UploadResult
import com.rogger.bp.data.model.PostImage
import com.rogger.bp.ui.commun.NetworkUtils
import kotlinx.coroutines.tasks.await
import java.io.File
/*
 * Desenvolvido por Roger de Oliveira
 * Data: 28/05/2026
 * Hora: 20:16
 */
class GlobalImageDataSource {

    private val TAG = "GlobalImageDataSource"
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // ── 1. Buscar imagem global ───────────────────────────────────────────

    /**
     * Busca a imagem global para o [barcode] informado.
     *
     * Retorna:
     *  - [ImageResult.GlobalImage] se existir
     *  - [ImageResult.NoImage] se não existir
     *  - [ImageResult.Error] em caso de falha de rede
     */
    suspend fun fetchGlobalImage(barcode: String): ImageResult {
        if (barcode.isBlank()) {
            return ImageResult.Error("Código de barras inválido")
        }

        // 👉 Se estiver offline, retorna NoImage direto para evitar a exceção do Firebase
        if (!NetworkUtils.isNetworkAvailable()) {
            Log.d(TAG, "Dispositivo offline — ignorando busca de imagem global para barcode=$barcode")
            return ImageResult.NoImage
        }

        return try {
            val snapshot = db.collection("imageProdutos")
                .document(barcode)
                .get()
                .await()

            if (snapshot.exists()) {
                val image = snapshot.toObject(PostImage::class.java)
                if (image != null && image.uri.isNotEmpty()) {
                    Log.d(TAG, "Imagem global encontrada para barcode=$barcode")
                    ImageResult.GlobalImage(url = image.uri, name = image.name)
                } else {
                    Log.w(TAG, "Documento existe mas uri está vazia para barcode=$barcode")
                    ImageResult.NoImage
                }
            } else {
                Log.d(TAG, "Nenhuma imagem global para barcode=$barcode")
                ImageResult.NoImage
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar imagem global: ${e.message}")
            ImageResult.Error(e.message ?: "Erro ao buscar imagem global")
        }
    }

    // ── 2. Criar imagem global (SOMENTE se não existir) ───────────────────

    /**
     * Faz upload e cria a imagem global para o [barcode].
     *
     * IMPORTANTE: Verifica ANTES se já existe. Se existir, retorna
     * [UploadResult.Error] com código especial "ALREADY_EXISTS".
     * Quem chama deve tratar esse caso e usar a URL existente em vez de
     * tentar sobrescrever.
     *
     * O Presenter e o Repository NUNCA devem chamar este método se já
     * souberem que a imagem global existe.
     */
    suspend fun createGlobalImageIfAbsent(
        barcode: String,
        productName: String,
        imageUri: String
    ): UploadResult {
        if (barcode.isBlank() || imageUri.isBlank()) {
            return UploadResult.Error("Barcode ou URI de imagem inválidos")
        }

        // 👉 Se detectar offline no início, retorna OFFLINE imediatamente
        if (!NetworkUtils.isNetworkAvailable()) {
            return UploadResult.Error("OFFLINE")
        }

        return try {
            // ── Verificação de existência (guarda de segurança) ──────────
            val existing = db.collection("imageProdutos")
                .document(barcode)
                .get()
                .await()

            if (existing.exists()) {
                val image = existing.toObject(PostImage::class.java)
                if (image != null && image.uri.isNotEmpty()) {
                    Log.w(TAG, "Imagem global já existe para barcode=$barcode — não sobrescrevendo")
                    return UploadResult.Error("ALREADY_EXISTS:${image.uri}")
                }
            }

            // ── Upload para o Storage ─────────────────────────────────────
            val fileUri: Uri = when {
                imageUri.startsWith("content://") -> Uri.parse(imageUri)
                imageUri.startsWith("file://")    -> Uri.parse(imageUri)
                else                              -> Uri.fromFile(File(imageUri))
            }

            val imageRef = storage.reference
                .child("imagens_produtos/$barcode.jpg")

            Log.d(TAG, "Iniciando upload da imagem global para: imagens_produtos/$barcode.jpg")

            imageRef.putFile(fileUri).await()

            val downloadUrl = imageRef.downloadUrl.await().toString()

            // ── Salvar metadata no Firestore (CREATE — nunca UPDATE) ─────
            val postImage = PostImage(
                barcode = barcode,
                name    = productName,
                uri     = downloadUrl
            )

            db.collection("imageProdutos")
                .document(barcode)
                .set(postImage)
                .await()

            Log.d(TAG, "Imagem global criada com sucesso para barcode=$barcode")
            UploadResult.Success(downloadUrl)

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao criar imagem global: ${e.message}")
            // 👉 Se falhar devido à falta de rede durante a execução, retorna OFFLINE de forma amigável
            if (e.message?.contains("offline", ignoreCase = true) == true ||
                e.message?.contains("unavailable", ignoreCase = true) == true) {
                return UploadResult.Error("OFFLINE")
            }
            UploadResult.Error(e.message ?: "Erro ao criar imagem global")
        }
    }
}
