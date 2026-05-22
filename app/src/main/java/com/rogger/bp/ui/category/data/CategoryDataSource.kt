package com.rogger.bp.ui.category.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.rogger.bp.data.model.PostCategory

class CategoryDataSource : PostCategoryDataSource {
    private val TAG = "CategoryDataSource"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * UID do usuário autenticado
     */
    private fun getUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Referência:
     * users/{uid}/category
     */
    private fun categoriasRef(): CollectionReference? {
        val uid = getUserId() ?: return null
        return db
            .collection("users")
            .document(uid)
            .collection("category")
    }

    override fun createCategory(
        category: PostCategory,
        callback: CategoryCallback
    ) {

        val ref = categoriasRef()

        val uid = getUserId()

        if (ref == null || uid == null) {

            callback.onFailure("Usuário não autenticado")
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
                    exception.message ?: "Erro ao verificar categoria"
                )

                callback.onComplete()

            }

    }

    override fun updateCategory(category: PostCategory, callback: CategoryCallback) {
        val ref = categoriasRef() ?: run {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        if (category.firestoreId.isBlank()) {
            callback.onFailure("Categoria sem ID Firestore — não é possível atualizar")
            callback.onComplete()
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
                    .addOnSuccessListener { callback.onSuccess(category) }
                    .addOnFailureListener { e -> callback.onFailure(e.message ?: "Erro") }
                    .addOnCompleteListener { callback.onComplete() }
            }
            .addOnFailureListener { e ->
                callback.onFailure(e.message ?: "Erro ao verificar nome")
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
        val ref = categoriasRef() ?: run {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return
        }

        if (category.firestoreId.isBlank()) {
            callback.onFailure("Categoria sem ID Firestore")
            callback.onComplete()
            return
        }

        ref.document(category.firestoreId)
            .delete()
            .addOnSuccessListener { callback.onSuccess(category) }
            .addOnFailureListener { e -> callback.onFailure(e.message ?: "Erro ao deletar") }
            .addOnCompleteListener { callback.onComplete() }
    }

    override fun fetchCategories(
        callback: FetchCategoriesCallback
    ) {

        val ref = categoriasRef()

        if (ref == null) {

            callback.onFailure("Usuário não autenticado")

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

                Log.d(
                    TAG,
                    "Categorias carregadas: ${list.size}"
                )

                callback.onSuccess(list)

            }
            .addOnFailureListener { exception ->

                callback.onFailure(
                    exception.message
                        ?: "Erro ao buscar categorias"
                )

            }
            .addOnCompleteListener {

                callback.onComplete()

            }

    }

    override fun addCategoriesSnapshotListener(callback: FetchCategoriesCallback): ListenerRegistration? {
        val ref = categoriasRef()
        if (ref == null) {
            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return null
        }

        return ref.orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erro no listener de categorias: ${error.message}")
                    callback.onFailure(error.message ?: "Erro no listener de categorias")
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
                            Log.e(
                                TAG,
                                "Erro ao mapear categoria via listener: ${exception.message}"
                            )
                            null
                        }
                    }
                    Log.d(TAG, "Categorias atualizadas via listener: ${list.size}")
                    callback.onSuccess(list)
                }
            }
    }
}