package com.rogger.bp.data.database;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.rogger.bp.data.model.Categoria;
import com.rogger.bp.data.model.Produto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseDataSource {

    private static final String TAG = "FirebaseDataSource";

    // Coleções raiz
    private static final String COL_USERS      = "users";
    private static final String COL_PRODUTOS   = "produtos";
    private static final String COL_CATEGORIAS = "categorias";
    // Campos dos documentos
    private static final String FIELD_ID           = "id";
    private static final String FIELD_NOME         = "nome";
    private static final String FIELD_CODIGO       = "codigoBarras";
    private static final String FIELD_CATEGORIA_ID = "categoriaId";
    private static final String FIELD_TIMESTAMP    = "timestamp";
    private static final String FIELD_ANOTACOES    = "anotacoes";
    private static final String FIELD_IMAGEM       = "imagem";
    private static final String FIELD_DELETED      = "deleted";
    private static final String DELETED_TIMESTAMP  = "delete_date";
    private static final String FIELD_USER_ID      = "userId";
    private static final String FIELD_FIRESTORE_ID = "firestoreId";

    private final FirebaseFirestore db;
    // ======================== SINGLETON ========================
    private static volatile FirebaseDataSource INSTANCE;

    public static FirebaseDataSource getInstance() {
        if (INSTANCE == null) {
            synchronized (FirebaseDataSource.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FirebaseDataSource();
                }
            }
        }
        return INSTANCE;
    }

    private FirebaseDataSource() {
        db  = FirebaseFirestore.getInstance();
    }

    /** Obtém o UID do usuário logado dinamicamente */
    private String getUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // ======================== REFERÊNCIAS ========================

    /** Coleção de produtos do usuário atual */
    private CollectionReference produtosRef() {
        String uid = getUid();
        if (uid == null || uid.isEmpty()) {
            Log.e(TAG, "Tentativa de acessar produtosRef sem UID válido");
            return null;
        }
        return db.collection(COL_USERS).document(uid).collection(COL_PRODUTOS);
    }

    /** Coleção de categorias do usuário atual */
    private CollectionReference categoriasRef() {
        String uid = getUid();
        if (uid == null || uid.isEmpty()) {
            Log.e(TAG, "Tentativa de acessar categoriasRef sem UID válido");
            return null;
        }
        return db.collection(COL_USERS).document(uid).collection(COL_CATEGORIAS);
    }

    // ======================== PRODUTOS — ESCRITA ========================

    public void salvarProduto(@NonNull Produto produto,
                              @Nullable FirestoreCallback<String> callback) {
        String uid = getUid();
        CollectionReference ref = produtosRef();
        if (uid == null || ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        String docId = uid + "_produto_" + produto.getId();
        Map<String, Object> data = produtoParaMap(produto, docId);

        ref.document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
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
        String uid = getUid();
        CollectionReference ref = produtosRef();
        if (uid == null || ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        String docId = uid + "_produto_" + produto.getId();
        Map<String, Object> data = produtoParaMap(produto, docId);
        ref.document(docId)
                .update(data)
                .addOnSuccessListener(aVoid -> {
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
        String uid = getUid();
        CollectionReference ref = produtosRef();
        if (uid == null || ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }
        String docId = uid + "_produto_" + produtoId;
        Map<String, Object> update = new HashMap<>();
        update.put(FIELD_DELETED,    true);
        update.put(DELETED_TIMESTAMP, System.currentTimeMillis());

        ref.document(docId)
                .update(update)
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(null); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    public void restaurarProduto(int produtoId,
                                 @Nullable FirestoreCallback<Void> callback) {
        String uid = getUid();
        CollectionReference ref = produtosRef();
        if (uid == null || ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        String docId = uid + "_produto_" + produtoId;
        Map<String, Object> update = new HashMap<>();
        update.put(FIELD_DELETED,    false);
        update.put(DELETED_TIMESTAMP, null);

        ref.document(docId)
                .update(update)
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(null); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    public void excluirProdutoPermanente(int produtoId,
                                         @Nullable FirestoreCallback<Void> callback) {
        String uid = getUid();
        CollectionReference ref = produtosRef();
        if (uid == null || ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        String docId = uid + "_produto_" + produtoId;
        ref.document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Produto excluído: " + docId);
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    // ======================== PRODUTOS — LEITURA ========================

    public void buscarProdutosAtivos(@NonNull FirestoreCallback<List<Produto>> callback) {
        CollectionReference ref = produtosRef();
        if (ref == null) {
            callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        ref.whereEqualTo(FIELD_DELETED, false)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Produto> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Produto p = mapParaProduto(doc.getData());
                        if (p != null) lista.add(p);
                    }
                    Log.d(TAG, "Produtos ativos buscados: " + lista.size());
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar produtos: " + e.getMessage());
                    callback.onFailure(e);
                });
    }
    // ======================== CATEGORIAS — ESCRITA ========================

    public void salvarCategoria(@NonNull Categoria categoria,
                                @Nullable FirestoreCallback<String> callback) {
        String uid = getUid();
        CollectionReference ref = categoriasRef();
        if (uid == null || ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        String docId = uid + "_categoria_" + categoria.getId();
        Map<String, Object> data = categoriaParaMap(categoria, docId);

        ref.document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Categoria salva: " + docId);
                    if (callback != null) callback.onSuccess(docId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao salvar categoria: " + e.getMessage());
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void excluirCategoria(int categoriaId,
                                 @Nullable FirestoreCallback<Void> callback) {
        String uid = getUid();
        CollectionReference ref = categoriasRef();
        if (uid == null || ref == null) {
            if (callback != null) callback.onFailure(new Exception("Usuário não autenticado"));
            return;
        }

        String docId = uid + "_categoria_" + categoriaId;
        ref.document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(null); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    // ======================== CATEGORIAS — LEITURA ========================

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
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar categorias: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    // ======================== AUXILIARES ========================

    private Map<String, Object> produtoParaMap(Produto p, String firestoreId) {
        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_ID,           p.getId());
        map.put(FIELD_NOME,         p.getNome());
        map.put(FIELD_CODIGO,       p.getCodigoBarras());
        map.put(FIELD_CATEGORIA_ID, p.getCategoryId());
        map.put(FIELD_TIMESTAMP,    p.getTimestamp());
        map.put(FIELD_ANOTACOES,    p.getAnotacoes());
        map.put(FIELD_IMAGEM,       p.getImagem());
        map.put(FIELD_DELETED,      p.isDeleted());
        map.put(DELETED_TIMESTAMP,   p.getDeletedAt());
        map.put(FIELD_USER_ID,      getUid());
        map.put(FIELD_FIRESTORE_ID, firestoreId);
        return map;
    }

    private Produto mapParaProduto(Map<String, Object> map) {
        if (map == null) return null;
        try {
            Produto p = new Produto();
            p.setId(((Long) map.get(FIELD_ID)).intValue());
            p.setNome((String) map.get(FIELD_NOME));
            p.setCodigoBarras((String) map.get(FIELD_CODIGO));
            p.setCategoryId(((Long) map.get(FIELD_CATEGORIA_ID)).intValue());
            p.setTimestamp((Long) map.get(FIELD_TIMESTAMP));
            p.setAnotacoes((String) map.get(FIELD_ANOTACOES));
            p.setImagem((String) map.get(FIELD_IMAGEM));
            p.setDeleted((Boolean) map.get(FIELD_DELETED));
            p.setDeletedAt((Long) map.get(DELETED_TIMESTAMP));
            //p.set((String) map.get(FIELD_FIRESTORE_ID));
            return p;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao converter map para Produto: " + e.getMessage());
            return null;
        }
    }

    private Map<String, Object> categoriaParaMap(Categoria c, String firestoreId) {
        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_ID,           c.getId());
        map.put(FIELD_NOME,         c.getNome());
        map.put(FIELD_USER_ID,      getUid());
        map.put(FIELD_FIRESTORE_ID, firestoreId);
        return map;
    }

    private Categoria mapParaCategoria(Map<String, Object> map) {
        if (map == null) return null;
        try {
            Categoria c = new Categoria();
            c.setId(((Long) map.get(FIELD_ID)).intValue());
            c.setNome((String) map.get(FIELD_NOME));
            return c;
        } catch (Exception e) {
            Log.e(TAG, "Erro ao converter map para Categoria: " + e.getMessage());
            return null;
        }
    }

    public void atualizarCategoria(Categoria categoria, FirestoreCallback<Void> firestoreCallback) {
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
}
