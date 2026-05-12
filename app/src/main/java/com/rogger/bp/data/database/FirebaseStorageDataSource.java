package com.rogger.bp.data.database;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

public class FirebaseStorageDataSource {

    private static final String TAG = "FirebaseStorageDS";
    private static final String STORAGE_BUCKET = "gs://stock-230b7.firebasestorage.app";
    private static final String PASTA_PRODUTOS  = "produtos";
    private static final String NOME_ARQUIVO    = "imagem.jpg";

    private final FirebaseStorage  storage;
    private final FirebaseAuth     auth;

    private static volatile FirebaseStorageDataSource INSTANCE;

    public static FirebaseStorageDataSource getInstance() {
        if (INSTANCE == null) {
            synchronized (FirebaseStorageDataSource.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FirebaseStorageDataSource();
                }
            }
        }
        return INSTANCE;
    }

    private FirebaseStorageDataSource() {
        storage = FirebaseStorage.getInstance(STORAGE_BUCKET);
        auth    = FirebaseAuth.getInstance();
    }

    // ======================== OAUTH2 TOKEN ========================

    /**
     * Obtém o OAuth2 Access Token (ID Token) do usuário logado via Firebase Auth.
     *
     * O token retornado é o Firebase ID Token, que pode ser trocado por um
     * OAuth2 Access Token para chamadas autenticadas à API REST do Storage.
     *
     * forceRefresh = true  → sempre busca um token novo (use quando o atual estiver expirado)
     * forceRefresh = false → usa o token em cache se ainda for válido (recomendado no fluxo normal)
     *
     * Uso típico:
     *   obterOAuth2Token(false, new TokenCallback() {
     *       public void onTokenObtido(String token) { ... }
     *       public void onErro(Exception e) { ... }
     *   });
     */
    public void obterFirebaseIdToken(boolean forceRefresh,
                                     @NonNull TokenCallback callback) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onErro(new IllegalStateException("Nenhum usuário autenticado."));
            return;
        }

        user.getIdToken(forceRefresh)
                .addOnSuccessListener(result -> {
                    String token = result.getToken();
                    if (token != null && !token.isEmpty()) {
                        Log.d(TAG, "OAuth2 token obtido com sucesso.");
                        callback.onTokenObtido(token);
                    } else {
                        callback.onErro(new IllegalStateException("Token retornou vazio."));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao obter token: " + e.getMessage());
                    callback.onErro(e);
                });
    }

    /**
     * Obtém o token e já valida se ele está próximo de expirar.
     * Caso expire em menos de 5 minutos, força refresh automático.
     *
     * Use este método quando não quiser gerenciar o forceRefresh manualmente.
     */
    public void obterFirebaseIdTokenSeguro(@NonNull TokenCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onErro(new IllegalStateException("Nenhum usuário autenticado."));
            return;
        }

        // Primeira tentativa sem forçar refresh
        user.getIdToken(false)
                .addOnSuccessListener(result -> {
                    String token = result.getToken();
                    long expiracaoMs = result.getExpirationTimestamp() * 1000L;
                    long agoraMs     = System.currentTimeMillis();
                    long cincoMinMs  = 5 * 60 * 1000L;

                    boolean proximoDeExpirar = (expiracaoMs - agoraMs) < cincoMinMs;

                    if (proximoDeExpirar) {
                        Log.d(TAG, "Token próximo de expirar. Forçando refresh...");
                        // Força novo token
                        obterFirebaseIdToken(true, callback);
                    } else {
                        Log.d(TAG, "Token válido. Expira em " +
                                ((expiracaoMs - agoraMs) / 60000) + " min.");
                        callback.onTokenObtido(token);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao verificar token: " + e.getMessage());
                    callback.onErro(e);
                });
    }

    // ======================== UPLOAD DE IMAGEM ========================

    /**
     * Faz upload de uma imagem (File local) para o Firebase Storage.
     *
     * Caminho no Storage: produtos/{uid}/{produtoId}/imagem.jpg
     *
     * O método:
     *  1. Obtém o token OAuth2 do usuário
     *  2. Verifica autenticação
     *  3. Inicia o upload com progresso
     *  4. Retorna a URL pública de download via callback onSucesso
     *
     * @param produtoId   ID do produto dono da imagem (usado como subpasta)
     * @param arquivoLocal Arquivo de imagem local a ser enviado
     * @param callback    Callbacks de progresso, sucesso e erro
     */
    public void uploadImagem(int produtoId,
                             @NonNull File arquivoLocal,
                             @NonNull UploadCallback callback) {

        // Primeiro valida o token de autenticação
        obterFirebaseIdTokenSeguro(new TokenCallback() {
            @Override
            public void onTokenObtido(@NonNull String token) {
                // Token válido → prossegue com o upload
                Log.d(TAG, "Sessão validada. Iniciando upload do produto " + produtoId);
                executarUpload(produtoId, arquivoLocal, callback, true);
            }

            @Override
            public void onErro(@NonNull Exception e) {
                Log.e(TAG, "Falha na autenticação antes do upload: " + e.getMessage());
                callback.onErro(e);
            }
        });
    }

    /**
     * Executa o upload após validação do token.
     * Separado para poder ser reutilizado internamente.
     */
    private void executarUpload(int produtoId,
                                @NonNull File arquivoLocal,
                                @NonNull UploadCallback callback,
                                boolean isRetry) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onErro(new IllegalStateException("Usuário deslogado durante o upload."));
            return;
        }

        if (!arquivoLocal.exists()) {
            callback.onErro(new IllegalArgumentException(
                    "Arquivo não encontrado: " + arquivoLocal.getAbsolutePath()));
            return;
        }

        String uid     = user.getUid();
        String caminho = PASTA_PRODUTOS + "/" + uid + "/" + produtoId + "/" + NOME_ARQUIVO;

        StorageReference ref = storage.getReference().child(caminho);
        Uri fileUri           = Uri.fromFile(arquivoLocal);

        // Metadados do arquivo
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("produtoId", String.valueOf(produtoId))
                .setCustomMetadata("userId",    uid)
                .build();

        Log.d(TAG, "Iniciando upload para: " + caminho);

        UploadTask uploadTask = ref.putFile(fileUri, metadata);

        uploadTask.addOnProgressListener(snapshot -> {
            double progresso = (100.0 * snapshot.getBytesTransferred())
                    / snapshot.getTotalByteCount();
            int porcentagem = (int) progresso;
            Log.d(TAG, "Upload em progresso: " + porcentagem + "%");
            callback.onProgresso(porcentagem);
        });

        // Upload concluído → busca a URL de download
        uploadTask.addOnSuccessListener(snapshot ->
                ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String urlDownload = uri.toString();
                            Log.d(TAG, "Upload concluído. URL: " + urlDownload);
                            callback.onSucesso(urlDownload);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Upload ok mas falhou ao obter URL: " + e.getMessage());
                            callback.onErro(e);
                        })
        );

        uploadTask.addOnFailureListener(e -> {
            Log.e(TAG, "Falha no upload: " + e.getMessage());

            // Se falhou por falta de autenticação, tenta renovar e repetir o upload uma única vez
            boolean isAuthError = (e instanceof com.google.firebase.storage.StorageException)
                    && ((com.google.firebase.storage.StorageException) e).getErrorCode()
                    == com.google.firebase.storage.StorageException.ERROR_NOT_AUTHENTICATED;

            if (isAuthError && !isRetry) {
                Log.d(TAG, "Erro de autenticação detectado. Tentando renovar sessão e repetir upload...");
                obterFirebaseIdToken(true, new TokenCallback() {
                    @Override
                    public void onTokenObtido(@NonNull String token) {
                        executarUpload(produtoId, arquivoLocal, callback, true);
                    }

                    @Override
                    public void onErro(@NonNull Exception tokenError) {
                        callback.onErro(tokenError);
                    }
                });
            } else {
                callback.onErro(e);
            }
        });
    }

    // ======================== DELETAR IMAGEM ========================
    public void deletarImagem(int produtoId,
                              @NonNull StorageCallback callback) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onErro(new IllegalStateException("Nenhum usuário autenticado."));
            return;
        }

        String uid     = user.getUid();
        String caminho = PASTA_PRODUTOS + "/" + uid + "/" + produtoId + "/" + NOME_ARQUIVO;

        StorageReference ref = storage.getReference().child(caminho);

        Log.d(TAG, "Deletando imagem do Storage: " + caminho);

        ref.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Imagem deletada do Storage: " + caminho);
                    callback.onSucesso();
                })
                .addOnFailureListener(e -> {
                    if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
                        Log.d(TAG, "Imagem já não existia no Storage.");
                        callback.onSucesso();
                    } else {
                        Log.e(TAG, "Erro ao deletar imagem: " + e.getMessage());
                        callback.onErro(e);
                    }
                });
    }

    public void deletarImagemPorUrl(@NonNull String urlDownload,
                                    @Nullable StorageCallback callback) {
        if (urlDownload.isEmpty()) {
            if (callback != null) callback.onSucesso();
            return;
        }

        try {
            StorageReference ref = storage.getReferenceFromUrl(urlDownload);
            Log.d(TAG, "Deletando imagem por URL: " + urlDownload);

            ref.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Imagem deletada por URL com sucesso.");
                        if (callback != null) callback.onSucesso();
                    })
                    .addOnFailureListener(e -> {
                        if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
                            if (callback != null) callback.onSucesso();
                        } else {
                            Log.e(TAG, "Erro ao deletar por URL: " + e.getMessage());
                            if (callback != null) callback.onErro(e);
                        }
                    });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "URL inválida para deletar: " + urlDownload);
            if (callback != null) callback.onErro(e);
        }
    }

    // ======================== CALLBACKS (INTERFACES) ========================

    public interface TokenCallback {
        void onTokenObtido(@NonNull String token);
        void onErro(@NonNull Exception e);
    }
    public interface UploadCallback {
        void onProgresso(int porcentagem);
        void onSucesso(@NonNull String urlDownload);
        void onErro(@NonNull Exception e);

    }
    public interface StorageCallback {
        void onSucesso();
        void onErro(@NonNull Exception e);
    }
}

