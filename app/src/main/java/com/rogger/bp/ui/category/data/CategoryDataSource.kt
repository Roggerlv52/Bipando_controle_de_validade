package com.rogger.bp.ui.category.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.rogger.bp.data.dao.GroupDao
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.ui.commun.SharedPreferencesManager
import kotlinx.coroutines.tasks.await

class CategoryDataSource(
    private val context: Context,
    private val groupDao: GroupDao
) : PostCategoryDataSource {
    private val TAG = "CategoryDataSource"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    /**
     * PROBLEMA 7 — Corrigido: Prioriza sempre o grupo, usando user apenas como fallback de leitura.
     */
    private fun categoriasRef(groupId: String? = null): CollectionReference? {
        val uid = getUserId() ?: return null
        val activeGroupId = SharedPreferencesManager.getActiveGroupId(context)
        val targetGroupId = if (!groupId.isNullOrEmpty()) groupId else activeGroupId

        return if (!targetGroupId.isNullOrEmpty()) {
            db.collection("groups").document(targetGroupId).collection("category")
        } else {
            // Fallback temporário apenas para leitura durante migração
            db.collection("users").document(uid).collection("category")
        }
    }

    override fun createCategory(category: PostCategory, callback: CategoryCallback) {
        val uid = getUserId() ?: run {
            callback.onFailure("Unauthenticated user")
            callback.onComplete()
            return
        }

        val ref = categoriasRef() ?: run {
            callback.onFailure("Target collection not found")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("name", category.name.trim())
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val existing = snapshot.documents.first().toCategory()
                    if (existing != null) {
                        callback.onAlreadyExists(existing)
                    } else {
                        callback.onFailure("Error mapping existing category")
                    }
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
                    .addOnSuccessListener { callback.onSuccess(category.copy(firestoreId = docRef.id, userId = uid)) }
                    .addOnFailureListener { e -> callback.onFailure(e.message ?: "Erro ao criar categoria") }
                    .addOnCompleteListener { callback.onComplete() }
            }
            .addOnFailureListener { exception ->
                callback.onFailure(exception.message ?: "Error verifying category")
                callback.onComplete()
            }
    }

    override fun updateCategory(category: PostCategory, callback: CategoryCallback) {
        val ref = categoriasRef() ?: run {
            callback.onFailure("Unauthenticated user")
            callback.onComplete()
            return
        }

        if (category.firestoreId.isBlank()) {
            callback.onFailure("Category without Firestore ID — cannot update")
            callback.onComplete()
            return
        }

        ref.whereEqualTo("name", category.name.trim())
            .get()
            .addOnSuccessListener { snapshot ->
                val conflict = snapshot.documents.any { it.id != category.firestoreId }
                if (conflict) {
                    val existing = snapshot.documents.first().toCategory()
                    if (existing != null) callback.onAlreadyExists(existing)
                    callback.onComplete()
                    return@addOnSuccessListener
                }

                ref.document(category.firestoreId)
                    .update("name", category.name.trim())
                    .addOnSuccessListener {
                        updateCategoryNameInProducts(category)
                        callback.onSuccess(category) 
                    }
                    .addOnFailureListener { e -> callback.onFailure(e.message ?: "Error updating category") }
                    .addOnCompleteListener { callback.onComplete() }
            }
            .addOnFailureListener { e ->
                callback.onFailure(e.message ?: "Error verifying name")
                callback.onComplete()
            }
    }

    private fun updateCategoryNameInProducts(category: PostCategory) {
        val activeGroupId = SharedPreferencesManager.getActiveGroupId(context)
        val uid = getUserId() ?: return
        
        val productsRef = if (activeGroupId.isNotEmpty()) {
            db.collection("groups").document(activeGroupId).collection("products")
        } else {
            db.collection("users").document(uid).collection("products")
        }

        productsRef.whereEqualTo("categoryId", category.firestoreId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) return@addOnSuccessListener
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.update(doc.reference, "categoryName", category.name.trim())
                }
                batch.commit()
            }
    }

    override fun deleteCategory(category: PostCategory, callback: CategoryCallback) {
        val ref = categoriasRef() ?: run {
            callback.onFailure("Unauthenticated user")
            callback.onComplete()
            return
        }

        if (category.firestoreId.isBlank()) {
            callback.onFailure("Category without Firestore ID")
            callback.onComplete()
            return
        }

        ref.document(category.firestoreId)
            .delete()
            .addOnSuccessListener {
                clearCategoryInProducts(category.firestoreId)
                callback.onSuccess(category)
            }
            .addOnFailureListener { e -> callback.onFailure(e.message ?: "Error deleting category") }
            .addOnCompleteListener { callback.onComplete() }
    }

    private fun clearCategoryInProducts(categoryId: String) {
        val activeGroupId = SharedPreferencesManager.getActiveGroupId(context)
        val uid = getUserId() ?: return
        
        val productsRef = if (activeGroupId.isNotEmpty()) {
            db.collection("groups").document(activeGroupId).collection("products")
        } else {
            db.collection("users").document(uid).collection("products")
        }

        productsRef.whereEqualTo("categoryId", categoryId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) return@addOnSuccessListener
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.update(doc.reference, "categoryId", "", "categoryName", "")
                }
                batch.commit()
            }
    }

    override fun fetchCategories(callback: FetchCategoriesCallback) {
        val ref = categoriasRef() ?: run {
            callback.onFailure("Unauthenticated user")
            callback.onComplete()
            return
        }

        ref.orderBy("name")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { it.toCategory() }
                callback.onSuccess(list)
            }
            .addOnFailureListener { exception ->
                callback.onFailure(exception.message ?: "Error retrieving categories")
            }
            .addOnCompleteListener { callback.onComplete() }
    }

    override fun addCategoriesSnapshotListener(groupId: String?, callback: FetchCategoriesCallback): ListenerRegistration? {
        val ref = categoriasRef(groupId) ?: run {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return null
        }

        return ref.orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    callback.onFailure(error.message ?: "Error in the category listener")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toCategory() }
                    callback.onSuccess(list)
                }
            }
    }

    private fun DocumentSnapshot.toCategory(): PostCategory? {
        return try {
            val name = getString("name") ?: return null
            PostCategory(
                firestoreId = id,
                name = name,
                userId = getString("userId") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}
