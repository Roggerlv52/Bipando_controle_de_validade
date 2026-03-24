package com.rogger.bipando.data.database;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.rogger.bipando.data.model.Categoria;
import com.rogger.bipando.data.model.Produto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FirebaseDataSource
 *
 * Responsável por TODAS as operações com o Firestore.
 * Estrutura das coleções no Firestore:
 *
 *   users/{uid}/produtos/{firestoreId}   → documentos de Produto
 *   users/{uid}/categorias/{firestoreId} → documentos de Categoria
 *
 * Cada operação:
 *  - salvar(Produto)   → cria ou sobrescreve o documento
 *  - atualizar(Produto)→ atualiza campos parcialmente (merge)
 *  - deletar(Produto)  → marca deleted=true (soft delete) ou remove
 *  - buscarProdutos()  → listener em tempo real, retorna via callback
 *  - buscarCategorias()→ listener em tempo real, retorna via callback
 */
public class FirebaseDataSource {

    private static final String TAG = "FirebaseDataSource";

    // ======================== COLLECTIONS ========================
    private static final String COL_USERS      = "users";
    private static final String COL_PRODUTOS   = "produtos";
    private static final String COL_CATEGORIAS = "categorias";

    // ======================== FIELDS ========================
    private static final String FIELD_ID           = "id";
    private static final String FIELD_NOME         = "nome";
    private static final String FIELD_CODIGO       = "codigoBarras";
    private static final String FIELD_CATEGORIA_ID = "categoriaId";
    private static final String FIELD_TIMESTAMP    = "timestamp";
    private static final String FIELD_ANOTACOES    = "anotacoes";
    private static final String FIELD_IMAGEM       = "imagem";
    private static final String FIELD_DELETED      = "deleted";
    private static final String FIELD_DELETED_AT   = "deletedAt";
    private static final String FIELD_USER_ID      = "userId";
    private static final String FIELD_FIRESTORE_ID = "firestoreId";

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final String uid;

    private ListenerRegistration produtosListener;
    private ListenerRegistration categoriasListener;

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
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
    }

    // ======================== REFERENCES ========================
    private CollectionReference produtosRef() {
        return db.collection(COL_USERS).document(uid).collection(COL_PRODUTOS);
    }

    private CollectionReference categoriasRef() {
        return db.collection(COL_USERS).document(uid).collection(COL_CATEGORIAS);
    }

    private StorageReference storageProdutoRef(String fileName) {
        return storage.getReference()
                .child("users")
                .child(uid)
                .child("produtos")
                .child(fileName);
    }

    // ======================== UPLOAD IMAGE ========================
    private void uploadImagem(byte[] imagemBytes, String fileName,
                              FirestoreCallback<String> callback) {

        StorageReference ref = storageProdutoRef(fileName);

        ref.putBytes(imagemBytes)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(uri ->
                                callback.onSuccess(uri.toString())
                        )
                )
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== PRODUTOS ========================

    public void salvarProduto(@NonNull Produto produto, byte[] imagemBytes, @NonNull FirestoreCallback<String> callback) {

        String docId = uid + "_produto_" + produto.getId();

        if (imagemBytes != null) {

            String fileName = docId + "_" + System.currentTimeMillis() + ".jpg";

            uploadImagem(imagemBytes, fileName, new FirestoreCallback<String>() {
                @Override
                public void onSuccess(String url) {
                    produto.setImagem(url);
                    salvarProdutoFirestore(produto, docId, callback);
                }

                @Override
                public void onFailure(@NonNull Exception e) {
                    callback.onFailure(e);
                }
            });

        } else {
            salvarProdutoFirestore(produto, docId, callback);
        }
    }

    private void salvarProdutoFirestore(Produto produto, String docId,
                                        FirestoreCallback<String> callback) {

        Map<String, Object> data = produtoParaMap(produto, docId);

        produtosRef().document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> callback.onSuccess(docId))
                .addOnFailureListener(callback::onFailure);
    }

    public void atualizarProduto(@NonNull Produto produto,
                                 byte[] imagemBytes,
                                 @NonNull FirestoreCallback<Void> callback) {

        String docId = uid + "_produto_" + produto.getId();

        if (imagemBytes != null) {

            String fileName = docId + "_" + System.currentTimeMillis() + ".jpg";

            uploadImagem(imagemBytes, fileName, new FirestoreCallback<String>() {
                @Override
                public void onSuccess(String url) {
                    produto.setImagem(url);
                    atualizarProdutoFirestore(produto, docId, callback);
                }

                @Override
                public void onFailure(@NonNull Exception e) {
                    callback.onFailure(e);
                }
            });

        } else {
            atualizarProdutoFirestore(produto, docId, callback);
        }
    }

    private void atualizarProdutoFirestore(Produto produto, String docId,
                                           FirestoreCallback<Void> callback) {

        Map<String, Object> data = produtoParaMap(produto, docId);

        produtosRef().document(docId)
                .update(data)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void excluirProdutoPermanente(int produtoId,
                                         @NonNull FirestoreCallback<Void> callback) {

        String docId = uid + "_produto_" + produtoId;

        produtosRef().document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void buscarProdutosAtivos(@NonNull FirestoreCallback<List<Produto>> callback) {

        produtosRef()
                .whereEqualTo(FIELD_DELETED, false)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Produto> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Produto p = mapParaProduto(doc.getData());
                        if (p != null) lista.add(p);
                    }
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void ouvirProdutosAtivos(@NonNull FirestoreCallback<List<Produto>> callback) {

        stopProdutosListener();

        produtosListener = produtosRef()
                .whereEqualTo(FIELD_DELETED, false)
                .orderBy(FIELD_TIMESTAMP)
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        callback.onFailure(error);
                        return;
                    }

                    List<Produto> lista = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Produto p = mapParaProduto(doc.getData());
                        if (p != null) lista.add(p);
                    }

                    callback.onSuccess(lista);
                });
    }

    public void stopProdutosListener() {
        if (produtosListener != null) {
            produtosListener.remove();
            produtosListener = null;
        }
    }

    // ======================== CATEGORIAS ========================

    public void salvarCategoria(@NonNull Categoria categoria,
                                @NonNull FirestoreCallback<String> callback) {

        String docId = uid + "_categoria_" + categoria.getId();

        Map<String, Object> data = categoriaParaMap(categoria, docId);

        categoriasRef().document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> callback.onSuccess(docId))
                .addOnFailureListener(callback::onFailure);
    }

    public void buscarCategorias(@NonNull FirestoreCallback<List<Categoria>> callback) {

        categoriasRef()
                .orderBy(FIELD_NOME)
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<Categoria> lista = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Categoria c = mapParaCategoria(doc.getData());
                        if (c != null) lista.add(c);
                    }

                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== MAP ========================

    private Map<String, Object> produtoParaMap(Produto p, String docId) {
        Map<String, Object> m = new HashMap<>();
        m.put(FIELD_FIRESTORE_ID, docId);
        m.put(FIELD_ID, p.getId());
        m.put(FIELD_NOME, p.getNome());
        m.put(FIELD_CODIGO, p.getCodigoBarras());
        m.put(FIELD_CATEGORIA_ID, p.getCategoryId());
        m.put(FIELD_TIMESTAMP, p.getTimestamp());
        m.put(FIELD_ANOTACOES, p.getAnotacoes());
        m.put(FIELD_IMAGEM, p.getImagem());
        m.put(FIELD_DELETED, p.isDeleted());
        m.put(FIELD_DELETED_AT, p.getDeletedAt());
        m.put(FIELD_USER_ID, uid);
        return m;
    }

    private Produto mapParaProduto(Map<String, Object> data) {
        if (data == null) return null;

        Produto p = new Produto();
        p.setId(((Long) data.get(FIELD_ID)).intValue());
        p.setNome((String) data.get(FIELD_NOME));
        p.setCodigoBarras((String) data.get(FIELD_CODIGO));
        p.setCategoryId(((Long) data.get(FIELD_CATEGORIA_ID)).intValue());
        p.setTimestamp((Long) data.get(FIELD_TIMESTAMP));
        p.setAnotacoes((String) data.get(FIELD_ANOTACOES));
        p.setImagem((String) data.get(FIELD_IMAGEM));
        p.setDeleted((Boolean) data.get(FIELD_DELETED));
        p.setUserId((String) data.get(FIELD_USER_ID));

        Object deletedAt = data.get(FIELD_DELETED_AT);
        if (deletedAt != null) p.setDeletedAt((Long) deletedAt);

        return p;
    }

    private Map<String, Object> categoriaParaMap(Categoria c, String docId) {
        Map<String, Object> m = new HashMap<>();
        m.put(FIELD_FIRESTORE_ID, docId);
        m.put(FIELD_ID, c.getId());
        m.put(FIELD_NOME, c.getNome());
        m.put(FIELD_USER_ID, uid);
        return m;
    }
    public void moverProdutoParaLixeira(int produtoId,
                                        @NonNull FirestoreCallback<Void> callback) {

        String docId = uid + "_produto_" + produtoId;
        Map<String, Object> update = new HashMap<>();
        update.put(FIELD_DELETED,    true);
        update.put(FIELD_DELETED_AT, System.currentTimeMillis());

        produtosRef().document(docId)
                .update(update)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
    public void restaurarProduto(int produtoId,
                                 @NonNull FirestoreCallback<Void> callback) {

        String docId = uid + "_produto_" + produtoId;
        Map<String, Object> update = new HashMap<>();
        update.put(FIELD_DELETED,    false);
        update.put(FIELD_DELETED_AT, null);

        produtosRef().document(docId)
                .update(update)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }


    private Categoria mapParaCategoria(Map<String, Object> data) {
        if (data == null) return null;

        Categoria c = new Categoria();
        c.setId(((Long) data.get(FIELD_ID)).intValue());
        c.setNome((String) data.get(FIELD_NOME));
        return c;
    }

    // ======================== CALLBACK ========================

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(@NonNull Exception e);
    }
}