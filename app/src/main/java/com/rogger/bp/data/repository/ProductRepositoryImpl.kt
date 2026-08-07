package com.rogger.bp.data.repository

import androidx.lifecycle.asFlow
import com.rogger.bp.data.database.BpDatabase
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.domain.model.Category
import com.rogger.bp.domain.model.Product
import com.rogger.bp.domain.repository.ProductRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rogger.bp.data.image.UploadResult
import com.rogger.bp.data.image.repository.ImageResolutionRepository
import com.rogger.bp.ui.commun.NetworkUtils
import kotlinx.coroutines.Dispatchers
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProductRepositoryImpl(
    private val context: Context,
    private val database: BpDatabase,
    private val imageResolutionRepository: ImageResolutionRepository? = null
) : ProductRepository {

    override fun getProducts(): Flow<List<Product>> {
        return database.productDao().getAllProducts().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getDeletedProducts(): Flow<List<Product>> {
        return database.productDao().getDeletedProductsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getProductByUuid(uuid: String): Flow<Product?> {
        return database.productDao().getProductByUuidFlow(uuid).map { it?.toDomain() }
    }

    override fun getProductsByCategory(categoryId: String): Flow<List<Product>> {
        return database.productDao().getProductsByCategory(categoryId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun searchProducts(query: String): Flow<List<Product>> {
        return database.productDao().searchAllFields(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getCategories(): Flow<List<Category>> {
        return database.categoryDao().getAllCategories().map { list ->
            list.map { Category(id = it.firestoreId, name = it.name) }
        }
    }

    override suspend fun saveCategory(category: Category) {
        val postCategory = com.rogger.bp.data.model.PostCategory(
            firestoreId = category.id,
            name = category.name
        )
        database.categoryDao().insertCategory(postCategory)
    }

    override suspend fun deleteCategory(category: Category) {
        database.categoryDao().removeCategory(category.id)
    }

    override suspend fun saveProduct(product: Product) = withContext(Dispatchers.IO) {
        var finalImageUri = product.imageUri

        // 0. Processamento de imagem (Upload para Firebase Storage se for local)
        val isLocalPath = finalImageUri.isNotEmpty() &&
                (finalImageUri.startsWith("/") || finalImageUri.startsWith("file://") || finalImageUri.startsWith("content://"))

        if (isLocalPath && NetworkUtils.isNetworkAvailable() && imageResolutionRepository != null) {
            try {
                val globalExists = imageResolutionRepository.globalImageExists(product.barcode)
                val result = if (!globalExists) {
                    imageResolutionRepository.uploadGlobalImage(
                        context = context,
                        barcode = product.barcode,
                        productName = product.name,
                        imageUri = finalImageUri
                    )
                } else {
                    imageResolutionRepository.saveUserImage(
                        context = context,
                        barcode = product.barcode,
                        imageUri = finalImageUri
                    )
                }

                if (result is UploadResult.Success) {
                    finalImageUri = result.url
                } else if (result is UploadResult.Error && result.message.startsWith("ALREADY_EXISTS:")) {
                    finalImageUri = result.message.removePrefix("ALREADY_EXISTS:")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductRepo", "Erro ao fazer upload da imagem: ${e.message}")
            }
        }

        val original = database.productDao().getProductByDocId(product.uuid)
        val postProduct = if (original != null) {
            original.copy(
                name = product.name,
                barcode = product.barcode,
                categoryId = product.categoryId,
                categoryName = product.categoryName,
                imageUri = finalImageUri,
                timestamp = product.timestamp,
                note = product.note,
                deleted = false,
                deletedAt = null
            )
        } else {
            product.toData().copy(
                imageUri = finalImageUri,
                deleted = false,
                deletedAt = null
            )
        }
        
        // 1. Atualiza Local (Room)
        database.productDao().insertProduct(postProduct)
        
        // 2. Sincroniza Remoto (Firestore)
        syncProductToFirestore(postProduct)
    }

    override suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        val original = database.productDao().getProductByDocId(product.uuid)
        if (original != null) {
            val updated = original.copy(
                deleted = true,
                deletedAt = System.currentTimeMillis()
            )
            // 1. Atualiza Local (Room)
            database.productDao().updateProduct(updated)
            // 2. Sincroniza Remoto (Firestore)
            syncProductToFirestore(updated)
        } else {
            // Caso não exista no Room, deleta/marca como deletado no Firestore de qualquer forma
            val updated = product.toData().copy(deleted = true, deletedAt = System.currentTimeMillis())
            database.productDao().insertProduct(updated)
            syncProductToFirestore(updated)
        }
    }

    private fun syncProductToFirestore(product: PostProduct) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val updates = hashMapOf(
                "name" to product.name,
                "barcode" to product.barcode,
                "categoryId" to product.categoryId,
                "categoryName" to product.categoryName,
                "imageUri" to product.imageUri,
                "timestamp" to product.timestamp,
                "note" to product.note,
                "deleted" to product.deleted,
                "deletedAt" to product.deletedAt,
                "userId" to product.userId
            )
            
            // Usar o documentId correto (firestoreDocId)
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("products").document(product.firestoreDocId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener {
                    android.util.Log.d("ProductRepo", "Firestore sync success: ${product.name}")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("ProductRepo", "Firestore sync failed: ${e.message}")
                }
        } catch (e: Exception) {
            android.util.Log.e("ProductRepo", "Firestore sync exception: ${e.message}")
        }
    }

    override suspend fun restoreProduct(product: Product) = withContext(Dispatchers.IO) {
        val original = database.productDao().getProductByDocId(product.uuid)
        if (original != null) {
            val restored = original.copy(deleted = false, deletedAt = null)
            database.productDao().updateProduct(restored)
            syncProductToFirestore(restored)
        }
    }

    override suspend fun permanentDeleteProduct(product: Product) = withContext(Dispatchers.IO) {
        database.productDao().removeProduct(product.uuid)
        
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("products").document(product.uuid)
                .delete()
        } catch (e: Exception) {}
    }

    override suspend fun getTotalProductsCount(): Flow<Int> {
        return database.productDao().getTotalProductsCountLiveData().asFlow()
    }
}

fun PostProduct.toDomain(): Product = Product(
    uuid = if (this.firestoreDocId.isNotEmpty()) this.firestoreDocId else this.uuid,
    name = this.name,
    barcode = this.barcode,
    categoryId = this.categoryId,
    categoryName = this.categoryName,
    imageUri = this.imageUri,
    timestamp = this.timestamp,
    note = this.note,
    deleted = this.deleted
)

fun Product.toData(): PostProduct = PostProduct(
    firestoreDocId = this.uuid,
    uuid = this.uuid,
    name = this.name,
    barcode = this.barcode,
    categoryId = this.categoryId,
    categoryName = this.categoryName,
    imageUri = this.imageUri,
    timestamp = this.timestamp,
    note = this.note,
    deleted = this.deleted
)
