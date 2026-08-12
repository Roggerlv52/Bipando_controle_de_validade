package com.rogger.bp.ui.category.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.ui.commun.SharedPreferencesManager
import android.content.Context
import com.rogger.bp.data.dao.GroupDao
import kotlinx.coroutines.runBlocking

class CategoryDataSource(
    private val context: Context,
    private val groupDao: GroupDao
) : PostCategoryDataSource {
    private val TAG = "CategoryDataSource"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? {
        return auth.currentUser?.uid
    }

    private fun categoriasRef(groupId: String? = null): CollectionReference? {
        val uid = getUserId() ?: return null
        
        return if (!groupId.isNullOrEmpty()) {
            db.collection("groups")
                .document(groupId)
                .collection("category")
        } else {
            db.collection("users")
                .document(uid)
                .collection("category")
        }
    }

    override fun createCategory(
        category: PostCategory,
        callback: CategoryCallback
    ) {
        val mode = SharedPreferencesManager.getWorkMode(context)
        val group = runBlocking { groupDao.getGroup() }
        val targetGroupId = if (mode == 1) group?.groupId else null
        val ref = categoriasRef(targetGroupId)

        val uid = getUserId()

        if (ref == null || uid == null) {

            callback.onFailure("Unauthenticated user")
            callback.onComplete()
            return

        }

        ref.whereEqualTo("name", category.name.trim())
            .get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.isEmpty) {

                    val existing = snapshot.documents.first()

                    val existingCategory = PostCategory(
                        name = existing.getString("name") ?: "",
                        userId = existing.getString("userId") ?: uid
                    )

                    callback.onAlreadyExists(existingCategory)
                    callback.onComplete()

                    return@addOnSuccessListener
                }

                val docRef = ref.document()

                val data = hashMapOf(

                    "firestoreId" to docRef.id,
                    "name" to category.name.trim(),
                    "userId" to uid

                )
                docRef.set(data)
                    .addOnSuccessListener { callback.onSuccess(category.copy(userId = uid)) }
                    .addOnFailureListener { e -> callback.onFailure(e.message ?: "Erro ao criar categoria") }
                    .addOnCompleteListener { callback.onComplete() }

            }
            .addOnFailureListener { exception ->

                callback.onFailure(
                    exception.message ?: "Error verifying category"
                )

                callback.onComplete()

            }

    }

    override fun updateCategory(category: PostCategory, callback: CategoryCallback) {
        val mode = SharedPreferencesManager.getWorkMode(context)
        val group = runBlocking { groupDao.getGroup() }
        val targetGroupId = if (mode == 1) group?.groupId else null
        val ref = categoriasRef(targetGroupId) ?: run {
            callback.onFailure("Unauthenticated user")
            //callback.onComplete()
            return
        }

        if (category.firestoreId.isBlank()) {
            callback.onFailure("Category without Firestore ID — cannot update")
            //callback.onComplete()
            return
        }

        // 1. Verificar conflito de nome (excluindo o próprio documento)
        ref.whereEqualTo("name", category.name.trim())
            .get()
            .addOnSuccessListener { snapshot ->
                val conflict = snapshot.documents.any { it.id != category.firestoreId }
                if (conflict) {
                    callback.onAlreadyExists(snapshot.documents.first().toCategory())
                    callback.onComplete()
                    return@addOnSuccessListener
                }
                // 2. Atualizar diretamente pelo documentId
                ref.document(category.firestoreId)
                    .update("name", category.name.trim())
                    .addOnSuccessListener {
                        // 3. Atualizar o categoryName em todos os produtos vinculados no Firestore
                        val uid = getUserId() ?: return@addOnSuccessListener
                        
                        // Busca produtos no local correto (User ou Grupo)
                        val productsRef = if (!targetGroupId.isNullOrEmpty()) {
                            db.collection("groups").document(targetGroupId).collection("products")
                        } else {
                            db.collection("users").document(uid).collection("products")
                        }

                        productsRef.whereEqualTo("categoryId", category.firestoreId)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val batch = db.batch()
                                for (doc in snapshot.documents) {
                                    batch.update(doc.reference, "categoryName", category.name.trim())
                                }
                                batch.commit()
                            }
                        
                        callback.onSuccess(category) 
                    }
                    .addOnFailureListener { e -> callback.onFailure(e.message ?: "Error") }
                    .addOnCompleteListener { callback.onComplete() }
            }
            .addOnFailureListener { e ->
                callback.onFailure(e.message ?: "Error verifying name")
                callback.onComplete()
            }
    }

    fun DocumentSnapshot.toCategory(): PostCategory {
        return PostCategory(
            firestoreId = id,
            name = getString("name") ?: "",
            userId = getString("userId") ?: ""
        )
    }

    override fun deleteCategory(category: PostCategory, callback: CategoryCallback) {
        val mode = SharedPreferencesManager.getWorkMode(context)
        val group = runBlocking { groupDao.getGroup() }
        val targetGroupId = if (mode == 1) group?.groupId else null
        val ref = categoriasRef(targetGroupId) ?: run {
            callback.onFailure("Unauthenticated user")
            callback.onComplete()
            return
        }

        if (category.firestoreId.isBlank()) {
            callback.onFailure("Category without Firestore ID")
            callback.onComplete()
            return
        }

        val uid = getUserId() ?: return

        // 1. Deletar a categoria
        ref.document(category.firestoreId)
            .delete()
            .addOnSuccessListener {
                // 2. Limpar a referência da categoria em todos os produtos vinculados no Firestore
                // Busca produtos no local correto (User ou Grupo)
                val productsRef = if (!targetGroupId.isNullOrEmpty()) {
                    db.collection("groups").document(targetGroupId).collection("products")
                } else {
                    db.collection("users").document(uid).collection("products")
                }

                productsRef.whereEqualTo("categoryId", category.firestoreId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val batch = db.batch()
                        for (doc in snapshot.documents) {
                            batch.update(doc.reference, "categoryId", "", "categoryName", "")
                        }
                        batch.commit()
                            .addOnSuccessListener { callback.onSuccess(category) }
                            .addOnFailureListener { e -> callback.onFailure("Category deleted, but failed to update products: ${e.message}") }
                    }
                    .addOnFailureListener { e ->
                        // Mesmo que falte os produtos, a categoria foi deletada
                        callback.onSuccess(category)
                    }
            }
            .addOnFailureListener { e -> callback.onFailure(e.message ?: "Error deleting") }
            .addOnCompleteListener { callback.onComplete() }
    }

    override fun fetchCategories(
        callback: FetchCategoriesCallback
    ) {
        val mode = SharedPreferencesManager.getWorkMode(context)
        val group = runBlocking { groupDao.getGroup() }
        val targetGroupId = if (mode == 1) group?.groupId else null
        val ref = categoriasRef(targetGroupId)

        if (ref == null) {

            callback.onFailure("Unauthenticated user")

            callback.onComplete()

            return
        }

        ref.orderBy("name")
            .get()
            .addOnSuccessListener { snapshot ->

                val list = snapshot.documents.mapNotNull { document ->

                    try {

                        PostCategory(
                            firestoreId = document.id, // Capturar o documentId do Firestore
                            name = document.getString("name")
                                ?: return@mapNotNull null,
                            userId = document.getString("userId") ?: ""
                        )

                    } catch (exception: Exception) {

                        Log.e(
                            TAG,
                            "Erro ao mapear: ${exception.message}"
                        )

                        null
                    }

                }

                callback.onSuccess(list)

            }
            .addOnFailureListener { exception ->

                callback.onFailure(
                    exception.message
                        ?: "Error retrieving categories"
                )

            }
            .addOnCompleteListener {

                callback.onComplete()

            }

    }

    override fun addCategoriesSnapshotListener(groupId: String?, callback: FetchCategoriesCallback): ListenerRegistration? {
        val ref = categoriasRef(groupId)
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return null
        }

        return ref.orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    callback.onFailure(error.message ?: "Error in the category listener")
                    callback.onComplete()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { document ->
                        try {
                            PostCategory(
                                firestoreId = document.id, // Capturar o documentId do Firestore
                                name = document.getString("name") ?: return@mapNotNull null,
                                userId = document.getString("userId") ?: ""
                            )
                        } catch (exception: Exception) {
                            null
                        }
                    }

                    callback.onSuccess(list)
                }
            }
    }
}