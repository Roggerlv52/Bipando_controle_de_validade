package com.rogger.bp.data.database;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

/**
 * FirebaseStorageDataSource
 *
 * Responsável por:
 *  1. Obter o OAuth2 Access Token do usuário autenticado via Firebase Auth
 *  2. Fazer upload de imagens para o Firebase Storage
 *  3. Deletar imagens do Firebase Storage
 *  4. Gerar a URL pública de download após o upload
 *
 * Estrutura de pastas no Storage:
 *   produtos/{uid}/{produtoId}/imagem.jpg
 *
 * Todos os métodos são assíncronos e retornam resultado via callbacks.
 *
 * DEPENDÊNCIA: adicionar no build.gradle.kts:
 *   implementation("com.google.firebase:firebase-storage:21.0.1")
 */
public class FirebaseStorageDataSource {

    private static final String TAG = "FirebaseStorageDS";

    // Bucket definido no google-services.json
    private static final String STORAGE_BUCKET = "gs://stock-230b7.firebasestorage.app";

    // Pasta raiz das imagens de produtos
    private static final String PASTA_PRODUTOS  = "produtos";
    private static final String NOME_ARQUIVO    = "imagem.jpg";

    private final FirebaseStorage  storage;
    private final FirebaseAuth     auth;

    // ======================== SINGLETON ========================

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
                executarUpload(produtoId, arquivoLocal, callback, false);
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

        // Progresso do upload
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

        // Falha no upload
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

    /**
     * Deleta a imagem de um produto no Firebase Storage.
     *
     * Útil ao excluir um produto permanentemente ou ao trocar a imagem.
     * Se a imagem não existir no Storage, o callback onSucesso é chamado
     * normalmente (operação idempotente).
     *
     * @param produtoId ID do produto cuja imagem será deletada
     * @param callback  Callbacks de sucesso e erro
     */
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
                    // Arquivo não existe = tudo certo (já foi deletado antes)
                    if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
                        Log.d(TAG, "Imagem já não existia no Storage.");
                        callback.onSucesso();
                    } else {
                        Log.e(TAG, "Erro ao deletar imagem: " + e.getMessage());
                        callback.onErro(e);
                    }
                });
    }

    /**
     * Deleta imagem usando a URL completa de download (alternativa ao ID).
     * Útil quando se tem a URL mas não o produtoId.
     *
     * @param urlDownload URL completa retornada no momento do upload
     * @param callback    Callbacks de sucesso e erro
     */
    public void deletarImagemPorUrl(@NonNull String urlDownload,
                                    @NonNull StorageCallback callback) {

        if (urlDownload.isEmpty()) {
            callback.onSucesso(); // Nada para deletar
            return;
        }

        try {
            StorageReference ref = storage.getReferenceFromUrl(urlDownload);
            Log.d(TAG, "Deletando imagem por URL: " + urlDownload);

            ref.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Imagem deletada por URL com sucesso.");
                        callback.onSucesso();
                    })
                    .addOnFailureListener(e -> {
                        if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
                            callback.onSucesso();
                        } else {
                            Log.e(TAG, "Erro ao deletar por URL: " + e.getMessage());
                            callback.onErro(e);
                        }
                    });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "URL inválida para deletar: " + urlDownload);
            callback.onErro(e);
        }
    }

    // ======================== CALLBACKS (INTERFACES) ========================

    /**
     * Callback para obtenção do OAuth2 token.
     */
    public interface TokenCallback {
        /** Chamado quando o token foi obtido com sucesso */
        void onTokenObtido(@NonNull String token);

        /** Chamado quando houve falha ao obter o token */
        void onErro(@NonNull Exception e);
    }

    /**
     * Callback para upload de imagem com progresso.
     */
    public interface UploadCallback {
        /**
         * Chamado durante o upload com a porcentagem de conclusão (0–100).
         * Ideal para atualizar uma ProgressBar.
         */
        void onProgresso(int porcentagem);

        /**
         * Chamado quando o upload foi concluído com sucesso.
         * @param urlDownload URL pública de download da imagem no Storage
         */
        void onSucesso(@NonNull String urlDownload);

        /** Chamado quando o upload falhou */
        void onErro(@NonNull Exception e);

    }

    /**
     * Callback simples para operações sem retorno de dado (delete, etc).
     */
    public interface StorageCallback {
        void onSucesso();
        void onErro(@NonNull Exception e);
    }
}
