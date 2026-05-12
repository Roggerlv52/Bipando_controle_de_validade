package com.rogger.bp.data.repository;

import android.app.Application;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.rogger.bp.data.database.FirebaseDataSource;
import com.rogger.bp.data.model.UserModel;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private final FirebaseDataSource firebaseDataSource;
    private String userId;

    public UserRepository(Application app) {
        firebaseDataSource = FirebaseDataSource.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
    }

    public void inserir(UserModel userModel) {
        userModel.setUid(userId);
        firebaseDataSource.salvarUsuario(userModel, new FirebaseDataSource.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String docId) {
                Log.d(TAG, "Categoria salva no Firestore: " + docId);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Falha ao salvar categoria no Firestore: " + e.getMessage());
            }
        });
    }
}
