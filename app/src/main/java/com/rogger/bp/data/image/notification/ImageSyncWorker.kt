package com.rogger.bp.data.image.notification

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rogger.bp.data.database.BpDatabase
import com.rogger.bp.ui.commun.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ImageSyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val TAG = "ImageSyncWorker"

    override fun doWork(): Result {
        Log.d(TAG, "Iniciando verificação de uploads de imagens pendentes...")

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.d(TAG, "Usuário não autenticado. Abortando sincronização.")
            return Result.success()
        }

        val database = BpDatabase.Companion.getDatabase(applicationContext)
        val productDao = database.productDao()

        // 1. Busca todos os produtos salvos localmente no Room
        val allProducts = productDao.getAllCachedProducts()
        if (allProducts.isNullOrEmpty()) {
            Log.d(TAG, "Nenhum produto em cache para sincronizar.")
            return Result.success()
        }

        // 2. Filtra produtos ativos que ainda usam caminhos de imagem local
        val pendingProducts = allProducts.filter { product ->
            val uri = product.imageUri
            uri.isNotEmpty() && (uri.startsWith("/") || uri.startsWith("file://")) && !product.deleted
        }

        if (pendingProducts.isEmpty()) {
            Log.d(TAG, "Nenhuma imagem local pendente de upload.")
            return Result.success()
        }

        Log.d(TAG, "Encontrados ${pendingProducts.size} produtos aguardando upload de imagem.")

        val storage = FirebaseStorage.getInstance()
        val db = FirebaseFirestore.getInstance()
        var hasFailures = false

        for (product in pendingProducts) {
            try {
                val cleanPath = product.imageUri.replace("file://", "")
                val file = File(cleanPath)

                if (!file.exists()) {
                    Log.w(TAG, "Arquivo local não existe mais no dispositivo: $cleanPath. Pulando.")
                    continue
                }

                Log.d(TAG, "Iniciando upload de imagem para o produto: ${product.name}")

                // Caminho de armazenamento privado por usuário
                val storagePath = "produtos/$uid/${product.barcode}.jpg"
                val imageRef = storage.reference.child(storagePath)
                val fileUri = Uri.fromFile(file)

                // Executa o upload síncrono na thread do Worker
                Tasks.await(imageRef.putFile(fileUri))

                // Obtém a URL de download pública e permanente
                val downloadUrl = Tasks.await(imageRef.downloadUrl).toString()

                Log.d(TAG, "Upload concluído para ${product.name}. URL: $downloadUrl")

                // 3. Atualiza o banco de dados local (Room) com a URL pública
                val updatedProduct = product.copy(imageUri = downloadUrl)

                CoroutineScope(Dispatchers.IO).launch {
                    productDao.insertProduct(updatedProduct)
                    // 4. Atualiza o documento no Firestore com a URL pública
                    val productRef = db.collection("users")
                        .document(uid)
                        .collection("products")
                        .document(product.firestoreDocId)

                    Tasks.await(productRef.update("imageUri", downloadUrl))
                    Log.d(TAG, "Firestore sincronizado com sucesso para ${product.name}")

                }


            } catch (e: Exception) {
                Log.e(TAG, "Falha ao sincronizar imagem do produto ${product.name}: ${e.message}")
                hasFailures = true
            }
        }

        // Se algum upload falhou (por oscilação de rede), o WorkManager tentará novamente mais tarde
        return if (hasFailures) Result.retry() else Result.success()
    }
}