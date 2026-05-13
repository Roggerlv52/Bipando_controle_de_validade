package com.rogger.bp.ui.category.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
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
                        id = (existing.getLong("id") ?: 0L).toInt(),
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
                    "id" to category.id,
                    "name" to category.name.trim(),
                    "userId" to uid

                )

                docRef.set(data)
                    .addOnSuccessListener {

                        Log.d(TAG, "Categoria criada: ${category.name}")

                        callback.onSuccess(
                            category.copy(userId = uid)
                        )

                    }
                    .addOnFailureListener { exception ->

                        Log.e(TAG, "Erro: ${exception.message}")

                        callback.onFailure(
                            exception.message ?: "Erro ao criar categoria"
                        )

                    }
                    .addOnCompleteListener {

                        callback.onComplete()

                    }

            }
            .addOnFailureListener { exception ->

                callback.onFailure(
                    exception.message ?: "Erro ao verificar categoria"
                )

                callback.onComplete()

            }

    }

    override fun updateCategory(
        category: PostCategory,
        callback: CategoryCallback
    ) {

        val ref = categoriasRef()

        if (ref == null) {

            callback.onFailure("Usuário não autenticado")
            callback.onComplete()
            return

        }

        ref.whereEqualTo("name", category.name.trim())
            .get()
            .addOnSuccessListener { snapshot ->

                val conflict = snapshot.documents.any { document ->

                    (document.getLong("id") ?: 0L).toInt() != category.id

                }

                if (conflict) {

                    val existing = snapshot.documents.first()

                    val existingCategory = PostCategory(
                        id = (existing.getLong("id") ?: 0L).toInt(),
                        name = existing.getString("name") ?: ""
                    )

                    callback.onAlreadyExists(existingCategory)

                    callback.onComplete()

                    return@addOnSuccessListener
                }

                ref.whereEqualTo("id", category.id)
                    .get()
                    .addOnSuccessListener { idSnapshot ->

                        if (idSnapshot.isEmpty) {

                            callback.onFailure("Categoria não encontrada")

                            callback.onComplete()

                            return@addOnSuccessListener
                        }

                        val docRef = idSnapshot.documents.first().reference

                        docRef.update(
                            "name",
                            category.name.trim()
                        )
                            .addOnSuccessListener {

                                Log.d(
                                    TAG,
                                    "Categoria atualizada: ${category.name}"
                                )

                                callback.onSuccess(category)

                            }
                            .addOnFailureListener { exception ->

                                callback.onFailure(
                                    exception.message
                                        ?: "Erro ao atualizar categoria"
                                )

                            }
                            .addOnCompleteListener {

                                callback.onComplete()

                            }

                    }
                    .addOnFailureListener { exception ->

                        callback.onFailure(
                            exception.message
                                ?: "Erro ao buscar categoria"
                        )

                        callback.onComplete()

                    }

            }
            .addOnFailureListener { exception ->

                callback.onFailure(
                    exception.message ?: "Erro ao verificar categoria"
                )

                callback.onComplete()

            }

    }

    override fun deleteCategory(
        category: PostCategory,
        callback: CategoryCallback
    ) {

        val ref = categoriasRef()

        if (ref == null) {

            callback.onFailure("Usuário não autenticado")

            callback.onComplete()

            return
        }

        ref.whereEqualTo("id", category.id)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {

                    callback.onSuccess(category)

                    callback.onComplete()

                    return@addOnSuccessListener
                }

                val docRef = snapshot.documents.first().reference

                docRef.delete()
                    .addOnSuccessListener {

                        Log.d(
                            TAG,
                            "Categoria deletada: ${category.id}"
                        )

                        callback.onSuccess(category)

                    }
                    .addOnFailureListener { exception ->

                        callback.onFailure(
                            exception.message
                                ?: "Erro ao deletar categoria"
                        )

                    }
                    .addOnCompleteListener {

                        callback.onComplete()

                    }

            }
            .addOnFailureListener { exception ->

                callback.onFailure(
                    exception.message
                        ?: "Erro ao buscar categoria"
                )

                callback.onComplete()

            }

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

                            id = (document.getLong("id") ?: 0L).toInt(),

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

}