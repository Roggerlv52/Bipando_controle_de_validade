package com.rogger.bp.ui.login.data

import android.content.Context
import android.util.Log
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

    override fun login(context: Context,idToken: String, email: String, callback: LoginCallback) {

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
                val photoUrl = user.photoUrl?.toString() ?: ""

                val finalEmail = email

                val firestoreData = hashMapOf(
                    "uid" to uid,
                    "name" to userName,
                    "email" to finalEmail,
                    "photoUrl" to photoUrl
                )
                Log.d("AUTH", auth.currentUser?.uid ?: "NULL")
                firestore.collection("users")
                    .document(uid)
                    .set(firestoreData, SetOptions.merge())
                    .addOnSuccessListener {
                        // ── Devolve UserAuth completo com photoUri ────────
                        val userAuth = UserAuth(
                            uuid = uid,
                            name = userName,
                            email = finalEmail,
                            password = "",
                            photoUri = user.photoUrl
                        )
                        callback.onSuccess(userAuth)
                    }
                    .addOnFailureListener { exception ->
                        val userAuth = UserAuth(
                            uuid = uid,
                            name = userName,
                            email = finalEmail,
                            password = "",
                            photoUri = user.photoUrl
                        )
                        callback.onSuccess(userAuth)
                    }
                    .addOnCompleteListener {
                        callback.onComplete()
                    }
            }
    }

}