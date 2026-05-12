package com.rogger.bp.data.database;

import static com.rogger.bp.data.database.FirestoreSchema.Collections.CATEGORIAS;
import static com.rogger.bp.data.database.FirestoreSchema.Collections.PRODUTOS;
import static com.rogger.bp.data.database.FirestoreSchema.Collections.USERS;
import static com.rogger.bp.data.database.FirestoreSchema.Produto.ANOTACOES;
import static com.rogger.bp.data.database.FirestoreSchema.Produto.CATEGORIA_ID;
import static com.rogger.bp.data.database.FirestoreSchema.Produto.CODIGO;
import static com.rogger.bp.data.database.FirestoreSchema.Produto.DELETED;
import static com.rogger.bp.data.database.FirestoreSchema.Produto.DELETED_AT;
import static com.rogger.bp.data.database.FirestoreSchema.Produto.ID;
import static com.rogger.bp.data.database.FirestoreSchema.Produto.IMAGEM;
import static com.rogger.bp.data.database.FirestoreSchema.Produto.TIMESTAMP;
import static com.rogger.bp.data.database.FirestoreSchema.User.EMAIL;
import static com.rogger.bp.data.database.FirestoreSchema.User.NOME;
import static com.rogger.bp.data.database.FirestoreSchema.User.UID;
import static com.rogger.bp.data.database.FirestoreSchema.User.USERNAME;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.rogger.bp.data.model.Categoria;
import com.rogger.bp.data.model.Produto;
import com.rogger.bp.data.model.UserModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseDataSource {

    private static final String TAG = "FirebaseDataSource";
    private static volatile FirebaseDataSource INSTANCE;

    public static FirebaseDataSource getInstance() {
        if (INSTANCE == null) {
            synchronized (FirebaseDataSource.class) {
                if (INSTANCE == null) INSTANCE = new FirebaseDataSource();
            }
        }
        return INSTANCE;
    }

    private final FirebaseFirestore db;

    private FirebaseDataSource() {
        db = FirebaseFirestore.getInstance();
    }

    private String getUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Deriva o username a partir do displayName ou do email.
     * Ex: "Rogger Oliveira" → "rogger_oliveira" | "rogger@email.com" → "rogger"
     */
    private String getUsername() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;

        String display = user.getDisplayName();
        if (display != null && !display.isEmpty()) {
            return display.trim().toLowerCase().replace(" ", "_");
        }
        String email = user.getEmail();
        if (email != null && email.contains("@")) {
            return email.split("@")[0].toLowerCase();
        }
        return null;
    }

    private DocumentReference userDocRef() {
        String username = getUsername();
        if (username == null || username.isEmpty()) {
            Log.e(TAG, "Username indisponível para montar referência");
            return null;
        }
        return db.collection(USERS).document(username);
    }

    private CollectionReference produtosRef() {
        DocumentReference doc = userDocRef();
        return doc != null ? doc.collection(PRODUTOS) : null;
    }

    private CollectionReference categoriasRef() {
        DocumentReference doc = userDocRef();
        return doc != null ? doc.collection(CATEGORIAS) : null;
    }

    // =======================================================================
    //  USUÁRIO
    // =======================================================================

    public void salvarUsuario(@NonNull UserModel user, @Nullable FirestoreCallback<String> callback) {
        String username = getUsername();
        if (username == null) {
            if (callback != null) callback.onFailure(new Exception("Username não disponível"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put(UID, user.getUid());
        data.put(NOME, user.getNome());
        data.put(EMAIL, user.getEmail());
        data.put(USERNAME, username);

        db.collection(USERS).document(username).set(data)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Usuário salvo: " + username);
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao salvar usuário: " + e.getMessage());
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void buscarUsuario(@NonNull FirestoreCallback<UserModel> callback) {
        DocumentReference doc = userDocRef();
        if (doc == null) {
            callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        doc.get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        callback.onFailure(new Exception("Perfil não encontrado"));
                        return;
                    }
                    UserModel user = new UserModel(
                            (String) snapshot.get(UID),
                            (String) snapshot.get(NOME),
                            (String) snapshot.get(EMAIL),
                            (String) snapshot.get(USERNAME)
                    );
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar usuário: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    // =======================================================================
    //  PRODUTOS — ESCRITA
    // =======================================================================

    public void salvarProduto(@NonNull Produto produto,
                              @Nullable FirestoreCallback<String> callback) {
        CollectionReference ref = produtosRef();
        if (ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        String docId = "produto_" + produto.getId();
        ref.document(docId).set(produtoParaMap(produto))
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Produto salvo: " + docId);
                    if (callback != null) callback.onSuccess(docId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao salvar produto: " + e.getMessage());
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void atualizarProduto(@NonNull Produto produto,
                                 @Nullable FirestoreCallback<Void> callback) {
        CollectionReference ref = produtosRef();
        if (ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        String docId = "produto_" + produto.getId();
        ref.document(docId).update(produtoParaMap(produto))
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Produto atualizado: " + docId);
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao atualizar produto: " + e.getMessage());
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void moverProdutoParaLixeira(int produtoId,
                                        @Nullable FirestoreCallback<Void> callback) {
        CollectionReference ref = produtosRef();
        if (ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put(DELETED, true);
        update.put(DELETED_AT, System.currentTimeMillis());

        ref.document("produto_" + produtoId).update(update)
                .addOnSuccessListener(v -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void restaurarProduto(int produtoId,
                                 @Nullable FirestoreCallback<Void> callback) {
        CollectionReference ref = produtosRef();
        if (ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put(DELETED, false);
        update.put(DELETED_AT, null);

        ref.document("produto_" + produtoId).update(update)
                .addOnSuccessListener(v -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void excluirProdutoPermanente(int produtoId,
                                         @Nullable FirestoreCallback<Void> callback) {
        CollectionReference ref = produtosRef();
        if (ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        ref.document("produto_" + produtoId).delete()
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Produto excluído: produto_" + produtoId);
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // =======================================================================
    //  PRODUTOS — LEITURA
    // =======================================================================

    public void buscarProdutosAtivos(@NonNull FirestoreCallback<List<Produto>> callback) {
        CollectionReference ref = produtosRef();
        if (ref == null) {
            callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        ref.whereEqualTo(DELETED, false)
                .orderBy(TIMESTAMP, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Produto> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Produto p = mapParaProduto(doc.getData());
                        if (p != null) lista.add(p);
                    }
                    Log.d(TAG, "Produtos ativos: " + lista.size());
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar produtos: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    // =======================================================================
    //  CATEGORIAS — ESCRITA
    // =======================================================================

    public void salvarCategoria(@NonNull Categoria categoria,
                                @Nullable FirestoreCallback<String> callback) {
        CollectionReference ref = categoriasRef();
        if (ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        String docId = "categoria_" + categoria.getId();
        ref.document(docId).set(categoriaParaMap(categoria))
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Categoria salva: " + docId);
                    if (callback != null) callback.onSuccess(docId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao salvar categoria: " + e.getMessage());
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void atualizarCategoria(@NonNull Categoria categoria,
                                   @Nullable FirestoreCallback<Void> callback) {
        CollectionReference ref = categoriasRef();
        if (ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        String docId = "categoria_" + categoria.getId();
        ref.document(docId).update(categoriaParaMap(categoria))
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Categoria atualizada: " + docId);
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao atualizar categoria: " + e.getMessage());
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void excluirCategoria(int categoriaId,
                                 @Nullable FirestoreCallback<Void> callback) {
        CollectionReference ref = categoriasRef();
        if (ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        ref.document("categoria_" + categoriaId).delete()
                .addOnSuccessListener(v -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    // =======================================================================
    //  CATEGORIAS — LEITURA
    // =======================================================================

    public void buscarCategorias(@NonNull FirestoreCallback<List<Categoria>> callback) {
        CollectionReference ref = categoriasRef();
        if (ref == null) {
            callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        ref.get()
                .addOnSuccessListener(snapshot -> {
                    List<Categoria> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Categoria c = mapParaCategoria(doc.getData());
                        if (c != null) lista.add(c);
                    }
                    Log.d(TAG, "Categorias buscadas: " + lista.size());
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar categorias: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    // =======================================================================
    //  CONVERSORES  (Objeto ↔ Map)
    // =======================================================================

    private Map<String, Object> produtoParaMap(@NonNull Produto p) {
        Map<String, Object> map = new HashMap<>();
        map.put(ID, p.getId());
        map.put(NOME, p.getNome());
        map.put(CODIGO, p.getCodigoBarras());
        map.put(CATEGORIA_ID, p.getCategoryId());
        map.put(TIMESTAMP, p.getTimestamp());
        map.put(ANOTACOES, p.getAnotacoes());
        map.put(IMAGEM, p.getImagem());
        map.put(DELETED, p.isDeleted());
        map.put(DELETED_AT, p.getDeletedAt());
        return map;
    }

    private Produto mapParaProduto(@Nullable Map<String, Object> map) {
        if (map == null) return null;
        try {
            Produto p = new Produto();
            p.setId(((Long) map.get(ID)).intValue());
            p.setNome((String) map.get(NOME));
            p.setCodigoBarras((String) map.get(CODIGO));
            p.setCategoryId(((Long) map.get(CATEGORIA_ID)).intValue());
            p.setTimestamp((Long) map.get(TIMESTAMP));
            p.setAnotacoes((String) map.get(ANOTACOES));
            p.setImagem((String) map.get(IMAGEM));
            p.setDeleted(Boolean.TRUE.equals(map.get(DELETED)));
            p.setDeletedAt((Long) map.get(DELETED_AT));
            return p;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao converter map -> Produto: " + e.getMessage());
            return null;
        }
    }

    private Map<String, Object> categoriaParaMap(@NonNull Categoria c) {
        Map<String, Object> map = new HashMap<>();
        map.put(ID, c.getId());
        map.put(NOME, c.getNome());
        return map;
    }

    private Categoria mapParaCategoria(@Nullable Map<String, Object> map) {
        if (map == null) return null;
        try {
            Categoria c = new Categoria();
            c.setId(((Long) map.get(ID)).intValue());
            c.setNome((String) map.get(NOME));
            return c;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao converter map -> Categoria: " + e.getMessage());
            return null;
        }
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
}
