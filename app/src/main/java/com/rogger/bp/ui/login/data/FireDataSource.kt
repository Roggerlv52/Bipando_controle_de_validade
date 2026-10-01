package com.rogger.bp.ui.login.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rogger.bp.R
import com.rogger.bp.data.model.UserAuth

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 22:39
 */
class FireDataSource : LoginDataSource {

    private val TAG = "FireDataSource"
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun login(context: Context, idToken: String, email: String, callback: LoginCallback) {

        if (idToken.isBlank()) {
            callback.onFailure(context.getString(
                R.string.toast_msg_error_google_invalid_token_2
            ))
            callback.onComplete()
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    val msg = task.exception?.message ?: context.getString(
                        R.string.toast_msg_error_google_authentication_failed)
                    callback.onFailure(msg)
                    callback.onComplete()
                    return@addOnCompleteListener
                }

                val user = auth.currentUser
                if (user == null) {
                    callback.onFailure(context.getString(
                        R.string.toast_msg_error_google_user_not_found
                    ))
                    callback.onComplete()
                    return@addOnCompleteListener
                }

                val uid = user.uid
                val userName = user.displayName ?: ""
                val googlePhotoUrl = user.photoUrl?.toString() ?: ""
                val finalEmail = if (email.isNotEmpty()) email else user.email ?: ""

                firestore.collection("users").document(uid).get()
                    .addOnSuccessListener { document ->
                        val firestoreData = hashMapOf<String, Any>(
                            "uid" to uid,
                            "name" to userName,
                            "email" to finalEmail
                        )

                        if (!document.exists()) {
                            firestoreData["isPremium"] = false
                            firestoreData["shareCode"] = uid.take(8).uppercase()
                        }

                        val existingPhoto = document.getString("photoUrl")
                        if (existingPhoto.isNullOrEmpty()) {
                            firestoreData["photoUrl"] = googlePhotoUrl
                        }

                        firestore.collection("users")
                            .document(uid)
                            .set(firestoreData, SetOptions.merge())
                            .addOnSuccessListener {
                                val userAuth = UserAuth(
                                    uuid     = uid,
                                    name     = userName,
                                    email    = finalEmail,
                                    password = "",
                                    photoUri = if (!existingPhoto.isNullOrEmpty())
                                        android.net.Uri.parse(existingPhoto)
                                    else user.photoUrl
                                )
                                callback.onSuccess(userAuth)
                                callback.onComplete()
                            }
                            .addOnFailureListener { e ->
                                callback.onFailure("Erro ao salvar dados do usuário: ${e.message}")
                                callback.onComplete()
                            }
                    }
                    .addOnFailureListener { e ->
                        callback.onFailure("Erro ao verificar usuário existente: ${e.message}")
                        callback.onComplete()
                    }
            }
    }
}
