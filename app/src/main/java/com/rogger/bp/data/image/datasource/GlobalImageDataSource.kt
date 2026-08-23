package com.rogger.bp.data.image.datasource
import android.net.Uri
import android.util.Log
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
import android.content.Context
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
            return ImageResult.NoImage
        }

        if (!NetworkUtils.isNetworkAvailable()) {
            Log.d(TAG, "Dispositivo offline — ignorando busca de imagem global para barcode=$barcode")
            return ImageResult.NoImage
        }

        return try {
            val barcodeClean = barcode.trim()
            Log.d(TAG, "Buscando imagem global em imageProdutos para barcode='$barcodeClean'")
            
            // Tenta primeiro pelo ID do documento (mais eficiente)
            var snapshot = db.collection("imageProdutos")
                .document(barcodeClean)
                .get()
                .await()

            // Se não encontrar pelo ID, tenta uma query pelo campo 'barcode'
            if (!snapshot.exists()) {
                Log.d(TAG, "Documento não encontrado pelo ID '$barcodeClean', tentando query por campo...")
                val query = db.collection("imageProdutos")
                    .whereEqualTo("barcode", barcodeClean)
                    .limit(1)
                    .get()
                    .await()
                
                if (!query.isEmpty) {
                    snapshot = query.documents.first()
                    Log.d(TAG, "Documento encontrado via query por campo barcode")
                }
            }

            if (snapshot.exists()) {
                Log.d(TAG, "Documento resolvido. Dados: ${snapshot.data}")
                
                val image = snapshot.toObject(PostImage::class.java)
                
                // Fallback agressivo: busca por todos os nomes de campos possíveis
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
                    Log.d(TAG, "Sucesso: Imagem global resolvida. Nome: $resolvedName, URL: $resolvedUri")
                    ImageResult.GlobalImage(url = resolvedUri, name = resolvedName)
                } else {
                    Log.w(TAG, "Aviso: Documento existe mas 'resolvedUri' está vazio para $barcodeClean")
                    ImageResult.NoImage
                }
            } else {
                Log.d(TAG, "Log: Documento com barcode '$barcodeClean' não existe em imageProdutos")
                ImageResult.NoImage
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro crítico ao buscar imagem global: ${e.message}", e)
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
        context: Context,
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

        var tempFile: File? = null
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

            // ── Otimização e Upload para o Storage ────────────────────────
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

            Log.d(TAG, "Iniciando upload da imagem global (otimizada) para: imagens_produtos/$barcode.jpg")

            val metadata = com.google.firebase.storage.storageMetadata {
                contentType = "image/jpeg"
            }

            imageRef.putFile(processedUri, metadata).await()

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

            Log.d(TAG, "Imagem global criada e otimizada com sucesso para barcode=$barcode")
            UploadResult.Success(downloadUrl)

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao criar imagem global: ${e.message}")
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
