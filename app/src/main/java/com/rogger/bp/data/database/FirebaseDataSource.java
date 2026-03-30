package com.rogger.bp.data.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private static final String FIELD_DELETED_AT   = "deletedAt";
    private static final String FIELD_USER_ID      = "userId";
    private static final String FIELD_FIRESTORE_ID = "firestoreId";

    private final FirebaseFirestore db;

    // Listeners ativos (para remover ao destruir)
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
        db  = FirebaseFirestore.getInstance();
    }

    /** Obtém o UID do usuário logado dinamicamente */
    private String getUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "";
    }

    // ======================== REFERÊNCIAS ========================

    /** Coleção de produtos do usuário atual */
    private com.google.firebase.firestore.CollectionReference produtosRef() {
        return db.collection(COL_USERS).document(getUid()).collection(COL_PRODUTOS);
    }

    /** Coleção de categorias do usuário atual */
    private com.google.firebase.firestore.CollectionReference categoriasRef() {
        return db.collection(COL_USERS).document(getUid()).collection(COL_CATEGORIAS);
    }

    // ======================== PRODUTOS — ESCRITA ========================

    /**
     * Salva (cria ou sobrescreve) um produto no Firestore.
     * Usa o Room id como parte do documento para garantir idempotência.
     * O firestoreId gerado é devolvido via callback.
     */
    public void salvarProduto(@NonNull Produto produto,
                              @Nullable FirestoreCallback<String> callback) {

        // Documento com ID determinístico baseado no uid do Room
        String docId = getUid() + "_produto_" + produto.getId();
        Map<String, Object> data = produtoParaMap(produto, docId);

        produtosRef().document(docId)
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

    /**
     * Atualiza campos específicos de um produto (merge parcial).
     * Não sobrescreve campos que não foram passados.
     */
    public void atualizarProduto(@NonNull Produto produto,
                                 @Nullable FirestoreCallback<Void> callback) {

        String docId = getUid() + "_produto_" + produto.getId();
        Map<String, Object> data = produtoParaMap(produto, docId);

        produtosRef().document(docId)
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

    /**
     * Soft delete: marca deleted=true e registra o timestamp de exclusão.
     * O documento permanece no Firestore (útil para sync entre dispositivos).
     */
    public void moverProdutoParaLixeira(int produtoId,
                                        @Nullable FirestoreCallback<Void> callback) {

        String docId = getUid() + "_produto_" + produtoId;
        Map<String, Object> update = new HashMap<>();
        update.put(FIELD_DELETED,    true);
        update.put(FIELD_DELETED_AT, System.currentTimeMillis());

        produtosRef().document(docId)
                .update(update)
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(null); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    /**
     * Restaura produto da lixeira (deleted=false).
     */
    public void restaurarProduto(int produtoId,
                                 @Nullable FirestoreCallback<Void> callback) {

        String docId = getUid() + "_produto_" + produtoId;
        Map<String, Object> update = new HashMap<>();
        update.put(FIELD_DELETED,    false);
        update.put(FIELD_DELETED_AT, null);

        produtosRef().document(docId)
                .update(update)
                .addOnSuccessListener(aVoid -> { if (callback != null) callback.onSuccess(null); })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    /**
     * Exclusão permanente do documento no Firestore.
     */
    public void excluirProdutoPermanente(int produtoId,
                                         @Nullable FirestoreCallback<Void> callback) {

        String docId = getUid() + "_produto_" + produtoId;

        produtosRef().document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Produto excluído: " + docId);
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> { if (callback != null) callback.onFailure(e); });
    }

    // ======================== PRODUTOS — LEITURA ========================

    /**
     * Busca única (one-shot) de todos os produtos ativos do usuário.
     * Ideal para sincronização inicial ou refresh manual.
     */
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
                    Log.d(TAG, "Produtos ativos buscados: " + lista.size());
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar produtos: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    /**
     * Listener em tempo real para produtos ativos.
     * Chama o callback sempre que houver mudança no Firestore.
     * Guarda a referência do listener para poder cancelar com stopProdutosListener().
     */
    public void ouvirProdutosAtivos(@NonNull FirestoreCallback<List<Produto>> callback) {
        // Remove listener anterior se existir
        stopProdutosListener();

        produtosListener = produtosRef()
                .whereEqualTo(FIELD_DELETED, false)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onFailure(error);
                        return;
                    }
                    if (snapshot != null) {
                        List<Produto> lista = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Produto p = mapParaProduto(doc.getData());
                            if (p != null) lista.add(p);
                        }
                        callback.onSuccess(lista);
                    }
                });
    }

    /**
     * Busca produtos da lixeira (deleted=true).
     */
    public void buscarProdutosDeletados(@NonNull FirestoreCallback<List<Produto>> callback) {
        produtosRef()
                .whereEqualTo(FIELD_DELETED, true)
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

    public void stopProdutosListener() {
        if (produtosListener != null) {
            produtosListener.remove();
            produtosListener = null;
        }
    }

    // ======================== CATEGORIAS — ESCRITA ========================

    /**
     * Salva (cria ou sobrescreve) uma categoria no Firestore.
     */
    public void salvarCategoria(@NonNull Categoria categoria,
                                @NonNull FirestoreCallback<String> callback) {

        String docId = getUid() + "_categoria_" + categoria.getId();
        Map<String, Object> data = categoriaParaMap(categoria, docId);

        categoriasRef().document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Categoria salva: " + docId);
                    callback.onSuccess(docId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao salvar categoria: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    /**
     * Atualiza o nome de uma categoria existente.
     */
    public void atualizarCategoria(@NonNull Categoria categoria,
                                   @NonNull FirestoreCallback<Void> callback) {

        String docId = getUid() + "_categoria_" + categoria.getId();
        Map<String, Object> data = categoriaParaMap(categoria, docId);

        categoriasRef().document(docId)
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Categoria atualizada: " + docId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao atualizar categoria: " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    /**
     * Exclui permanentemente uma categoria do Firestore.
     */
    public void excluirCategoria(int categoriaId,
                                 @NonNull FirestoreCallback<Void> callback) {

        String docId = getUid() + "_categoria_" + categoriaId;

        categoriasRef().document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Categoria excluída: " + docId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ======================== CATEGORIAS — LEITURA ========================

    /**
     * Busca única de todas as categorias do usuário.
     */
    public void buscarCategorias(@NonNull FirestoreCallback<List<Categoria>> callback) {
        categoriasRef()
                .orderBy(FIELD_NOME, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Categoria> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Categoria c = mapParaCategoria(doc.getData());
                        if (c != null) lista.add(c);
                    }
                    Log.d(TAG, "Categorias buscadas: " + lista.size());
                    callback.onSuccess(lista);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Listener em tempo real para categorias.
     */
    public void ouvirCategorias(@NonNull FirestoreCallback<List<Categoria>> callback) {
        stopCategoriasListener();

        categoriasListener = categoriasRef()
                .orderBy(FIELD_NOME, Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) { callback.onFailure(error); return; }
                    if (snapshot != null) {
                        List<Categoria> lista = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Categoria c = mapParaCategoria(doc.getData());
                            if (c != null) lista.add(c);
                        }
                        callback.onSuccess(lista);
                    }
                });
    }

    public void stopCategoriasListener() {
        if (categoriasListener != null) {
            categoriasListener.remove();
            categoriasListener = null;
        }
    }

    /**
     * Para todos os listeners ativos. Chamar no onDestroy da Activity principal.
     */
    public void stopAllListeners() {
        stopProdutosListener();
        stopCategoriasListener();
    }

    // ======================== MAPEAMENTOS ========================

    /** Converte Produto em Map para salvar no Firestore */
    private Map<String, Object> produtoParaMap(@NonNull Produto p, String docId) {
        Map<String, Object> m = new HashMap<>();
        m.put(FIELD_FIRESTORE_ID, docId);
        m.put(FIELD_ID,           p.getId());
        m.put(FIELD_NOME,         p.getNome());
        m.put(FIELD_CODIGO,       p.getCodigoBarras());
        m.put(FIELD_CATEGORIA_ID, p.getCategoryId());
        m.put(FIELD_TIMESTAMP,    p.getTimestamp());
        m.put(FIELD_ANOTACOES,    p.getAnotacoes());
        m.put(FIELD_IMAGEM,       p.getImagem());
        m.put(FIELD_DELETED,      p.isDeleted());
        m.put(FIELD_DELETED_AT,   p.getDeletedAt());
        m.put(FIELD_USER_ID,      getUid());
        return m;
    }

    /** Converte Map do Firestore em Produto */
    private Produto mapParaProduto(Map<String, Object> data) {
        if (data == null) return null;
        Produto p = new Produto();
        p.setId(    toInt(data.get(FIELD_ID)));
        p.setNome(  (String) data.get(FIELD_NOME));
        p.setCodigoBarras((String) data.get(FIELD_CODIGO));
        p.setCategoryId(toInt(data.get(FIELD_CATEGORIA_ID)));
        p.setTimestamp(  toLong(data.get(FIELD_TIMESTAMP)));
        p.setAnotacoes(  (String) data.get(FIELD_ANOTACOES));
        p.setImagem(     (String) data.get(FIELD_IMAGEM));
        p.setDeleted(    toBool(data.get(FIELD_DELETED)));
        p.setUserId(     (String) data.get(FIELD_USER_ID));

        Object deletedAt = data.get(FIELD_DELETED_AT);
        if (deletedAt != null) p.setDeletedAt(toLong(deletedAt));
        return p;
    }

    /** Converte Categoria em Map para salvar no Firestore */
    private Map<String, Object> categoriaParaMap(@NonNull Categoria c, String docId) {
        Map<String, Object> m = new HashMap<>();
        m.put(FIELD_FIRESTORE_ID, docId);
        m.put(FIELD_ID,           c.getId());
        m.put(FIELD_NOME,         c.getNome());
        m.put(FIELD_USER_ID,      getUid());
        return m;
    }

    /** Converte Map do Firestore em Categoria */
    private Categoria mapParaCategoria(Map<String, Object> data) {
        if (data == null) return null;
        Categoria c = new Categoria();
        c.setId(  toInt(data.get(FIELD_ID)));
        c.setNome((String) data.get(FIELD_NOME));
        return c;
    }

    // ======================== HELPERS DE CONVERSÃO ========================

    private int toInt(Object o) {
        if (o instanceof Long)    return ((Long) o).intValue();
        if (o instanceof Integer) return (Integer) o;
        return 0;
    }

    private long toLong(Object o) {
        if (o instanceof Long)    return (Long) o;
        if (o instanceof Integer) return ((Integer) o).longValue();
        return 0L;
    }

    private boolean toBool(Object o) {
        if (o instanceof Boolean) return (Boolean) o;
        return false;
    }

    // ======================== CALLBACK INTERFACE ========================

    /**
     * Interface genérica de callback para operações assíncronas do Firestore.
     */
    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(@NonNull Exception e);
    }
}