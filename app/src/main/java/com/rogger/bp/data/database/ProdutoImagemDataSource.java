package com.rogger.bp.data.database;

import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.rogger.bp.data.model.ProdutoImagem;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * ProdutoImagemDataSource
 *
 * Responsável por TODAS as operações da coleção global "imagens_produtos".
 *
 * Firestore:
 *   imagens_produtos/{codigoBarras}
 *     └── nomeProduto : String
 *     └── imagemUrl   : String
 *
 * Storage:
 *   imagens_produtos/{codigoBarras}/imagem.jpg
 *
 * Regra central:
 *   Só cria um documento/arquivo se o codigoBarras NÃO existe ainda.
 *   Nunca sobrescreve. Nunca atualiza.
 */
public class ProdutoImagemDataSource {

    private static final String TAG = "ProdutoImagemDS";

    // Coleção global — fora do escopo de usuário
    private static final String COL_IMAGENS     = "imagens_produtos";
    private static final String FIELD_NOME      = "nomeProduto";
    private static final String FIELD_IMAGEM    = "imagemUrl";

    // Storage — mesma estrutura da coleção
    private static final String STORAGE_BUCKET  = "gs://stock-230b7.firebasestorage.app";
    private static final String PASTA_IMAGENS   = "imagens_produtos";
    private static final String NOME_ARQUIVO    = "imagem.jpg";

    private final FirebaseFirestore db;
    private final FirebaseStorage   storage;
    private final FirebaseAuth      auth;

    // ======================== SINGLETON ========================

    private static volatile ProdutoImagemDataSource INSTANCE;

    public static ProdutoImagemDataSource getInstance() {
        if (INSTANCE == null) {
            synchronized (ProdutoImagemDataSource.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ProdutoImagemDataSource();
                }
            }
        }
        return INSTANCE;
    }

    private ProdutoImagemDataSource() {
        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance(STORAGE_BUCKET);
        auth    = FirebaseAuth.getInstance();
    }

    // ======================== LEITURA ========================

    /**
     * Busca o documento de imagem pelo código de barras.
     *
     * Retorna via callback:
     *   onEncontrado(ProdutoImagem) → barcode existe, retorna nome + URL
     *   onNaoEncontrado()           → barcode não existe, usuário deve cadastrar
     *   onErro(Exception)           → falha de rede ou autenticação
     *
     * @param codigoBarras Código de barras escaneado
     * @param callback     Resultado da consulta
     */
    public void buscarPorCodigoBarras(@NonNull String codigoBarras,
                                      @NonNull BuscaCallback callback) {

        if (codigoBarras.isEmpty()) {
            callback.onNaoEncontrado();
            return;
        }

        db.collection(COL_IMAGENS)
                .document(codigoBarras)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        ProdutoImagem imagem = mapParaProdutoImagem(codigoBarras, snapshot);
                        Log.d(TAG, "Imagem encontrada para barcode: " + codigoBarras);
                        callback.onEncontrado(imagem);
                    } else {
                        Log.d(TAG, "Barcode não cadastrado ainda: " + codigoBarras);
                        callback.onNaoEncontrado();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar imagem: " + e.getMessage());
                    callback.onErro(e);
                });
    }

    // ======================== ESCRITA ========================

    /**
     * Salva uma nova imagem global para o código de barras.
     *
     * Fluxo:
     *   1. Verifica se o documento já existe (segurança dupla além da Security Rule)
     *   2. Se não existe: faz upload da imagem no Storage
     *   3. Após upload: grava o documento no Firestore com nome + URL
     *   4. Se já existe: retorna onJaExiste() sem fazer nada
     *
     * @param codigoBarras  Código de barras do produto
     * @param nomeProduto   Nome digitado pelo usuário no AddFragment
     * @param arquivoLocal  Arquivo de imagem capturado/selecionado
     * @param callback      Resultado da operação
     */
    public void salvarNovaImagem(@NonNull String codigoBarras,
                                 @NonNull String nomeProduto,
                                 @NonNull File arquivoLocal,
                                 @NonNull SalvarCallback callback) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onErro(new IllegalStateException("Nenhum usuário autenticado."));
            return;
        }

        if (!arquivoLocal.exists()) {
            callback.onErro(new IllegalArgumentException(
                    "Arquivo de imagem não encontrado: " + arquivoLocal.getAbsolutePath()));
            return;
        }

        DocumentReference docRef = db.collection(COL_IMAGENS).document(codigoBarras);

        // Verificação dupla antes de qualquer upload
        docRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                // Já foi cadastrado por outro usuário enquanto este estava preenchendo
                Log.d(TAG, "Barcode já existe, retornando dados existentes: " + codigoBarras);
                ProdutoImagem existente = mapParaProdutoImagem(codigoBarras, snapshot);
                callback.onJaExiste(existente);
            } else {
                // Não existe — prossegue com upload
                executarUploadEGravar(codigoBarras, nomeProduto, arquivoLocal, docRef, callback);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Erro ao verificar existência antes de salvar: " + e.getMessage());
            callback.onErro(e);
        });
    }

    /**
     * Executa o upload no Storage e depois grava no Firestore.
     * Chamado apenas após confirmação de que o barcode não existe.
     */
    private void executarUploadEGravar(@NonNull String codigoBarras,
                                       @NonNull String nomeProduto,
                                       @NonNull File arquivoLocal,
                                       @NonNull DocumentReference docRef,
                                       @NonNull SalvarCallback callback) {

        String caminho = PASTA_IMAGENS + "/" + codigoBarras + "/" + NOME_ARQUIVO;
        StorageReference ref = storage.getReference().child(caminho);
        Uri fileUri = Uri.fromFile(arquivoLocal);

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("codigoBarras", codigoBarras)
                .build();

        Log.d(TAG, "Iniciando upload global para: " + caminho);

        UploadTask uploadTask = ref.putFile(fileUri, metadata);

        uploadTask.addOnProgressListener(snapshot -> {
            double progresso = (100.0 * snapshot.getBytesTransferred())
                    / snapshot.getTotalByteCount();
            callback.onProgresso((int) progresso);
        });

        uploadTask.addOnSuccessListener(snapshot ->
                ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String urlDownload = uri.toString();
                            Log.d(TAG, "Upload concluído. Gravando no Firestore...");
                            gravarNoFirestore(codigoBarras, nomeProduto, urlDownload, docRef, callback);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Upload ok mas falhou ao obter URL: " + e.getMessage());
                            callback.onErro(e);
                        })
        );

        uploadTask.addOnFailureListener(e -> {
            Log.e(TAG, "Falha no upload global: " + e.getMessage());
            callback.onErro(e);
        });
    }

    /**
     * Grava o documento no Firestore após o upload ser concluído.
     * Usa o codigoBarras como ID do documento — garante unicidade.
     */
    private void gravarNoFirestore(@NonNull String codigoBarras,
                                   @NonNull String nomeProduto,
                                   @NonNull String imagemUrl,
                                   @NonNull DocumentReference docRef,
                                   @NonNull SalvarCallback callback) {

        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_NOME,   nomeProduto);
        data.put(FIELD_IMAGEM, imagemUrl);

        // set() com ID já definido (codigoBarras) — idempotente por design
        docRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    ProdutoImagem nova = new ProdutoImagem(codigoBarras, nomeProduto, imagemUrl);
                    Log.d(TAG, "Documento gravado no Firestore: " + codigoBarras);
                    callback.onSucesso(nova);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Falha ao gravar no Firestore: " + e.getMessage());
                    callback.onErro(e);
                });
    }

    // ======================== MAPEAMENTO ========================

    private ProdutoImagem mapParaProdutoImagem(@NonNull String codigoBarras,
                                               @NonNull DocumentSnapshot snapshot) {
        String nome = snapshot.getString(FIELD_NOME);
        String url  = snapshot.getString(FIELD_IMAGEM);
        return new ProdutoImagem(codigoBarras, nome, url);
    }

    // ======================== CALLBACKS ========================

    /**
     * Callback para busca por código de barras.
     */
    public interface BuscaCallback {
        /** Barcode encontrado — retorna nome + URL prontos para uso */
        void onEncontrado(@NonNull ProdutoImagem produtoImagem);

        /** Barcode não existe ainda — usuário deve capturar imagem */
        void onNaoEncontrado();

        /** Falha de rede ou autenticação */
        void onErro(@NonNull Exception e);
    }

    /**
     * Callback para salvar nova imagem global.
     */
    public interface SalvarCallback {
        /** Progresso do upload (0–100) */
        void onProgresso(int porcentagem);

        /** Upload e gravação concluídos com sucesso */
        void onSucesso(@NonNull ProdutoImagem produtoImagem);

        /**
         * Barcode já existia (cadastrado por outro usuário entre a consulta e o save).
         * O AddFragment deve usar os dados retornados ao invés de fazer novo upload.
         */
        void onJaExiste(@NonNull ProdutoImagem produtoImagemExistente);

        /** Falha no processo */
        void onErro(@NonNull Exception e);
    }
}
