package com.rogger.bp.data.repository;


import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.rogger.bp.data.dao.ProdutoDao;
import com.rogger.bp.data.database.BpdDatabase;
import com.rogger.bp.data.database.FirebaseDataSource;
import com.rogger.bp.data.database.FirebaseStorageDataSource;
import com.rogger.bp.data.database.LocalCache;
import com.rogger.bp.data.model.Produto;

import java.io.File;
import java.util.List;

/**
 * ProdutoRepository
 *
 * Orquestrador entre Room (local), Firestore (nuvem) e Storage (imagens).
 */
public class ProdutoRepository {

    private static final String TAG = "ProdutoRepository";

    private final ProdutoDao                produtoDao;
    private final FirebaseDataSource        firebaseDataSource;
    private final FirebaseStorageDataSource storageDataSource;
    private final LocalCache                localCache;
    private final String                    userId;

    // LiveData observada pela UI — vem do Room
    private final LiveData<List<Produto>> produtosAtivos;
    private final LiveData<List<Produto>> produtosDeletados;

    // ✅ Estado de carregamento
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public ProdutoRepository(Application application) {
        BpdDatabase db = BpdDatabase.getDatabase(application);
        produtoDao         = db.produtoDao();
        firebaseDataSource = FirebaseDataSource.getInstance();
        storageDataSource  = FirebaseStorageDataSource.getInstance();
        localCache         = LocalCache.getInstance();
        userId             = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        produtosAtivos    = produtoDao.listarProdutosAtivos(userId);
        produtosDeletados = produtoDao.listarProdutosDeletados(userId);

        // Sincroniza dados do Firestore para o Room na primeira abertura
        sincronizarDoFirestore();
    }

    // ======================== LEITURA ========================

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /** LiveData principal da UI — observa o Room local */
    public LiveData<List<Produto>> getProdutosAtivos() {
        return produtosAtivos;
    }

    public LiveData<List<Produto>> getProdutosDeletados() {
        return produtosDeletados;
    }

    /**
     * Força uma re-sincronização do Firestore para o Room.
     */
    public void sincronizarDoFirestore() {
        List<Produto> cached = localCache.getProdutosAtivos();
        if (cached != null) {
            Log.d(TAG, "Cache válido, pulando busca no Firestore.");
            return;
        }

        Log.d(TAG, "Cache expirado/vazio, buscando no Firestore...");
        isLoading.postValue(true); // ✅ Inicia carregamento

        firebaseDataSource.buscarProdutosAtivos(new FirebaseDataSource.FirestoreCallback<List<Produto>>() {
            @Override
            public void onSuccess(List<Produto> produtos) {
                Log.d(TAG, "Sincronizado " + produtos.size() + " produtos do Firestore");
                localCache.setProdutosAtivos(produtos);
                BpdDatabase.databaseWriteExecutor.execute(() -> {
                    for (Produto p : produtos) {
                        p.setUserId(userId);
                        produtoDao.insert(p);
                    }
                    isLoading.postValue(false); // ✅ Finaliza carregamento
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Erro ao sincronizar produtos: " + e.getMessage());
                isLoading.postValue(false); // ✅ Finaliza carregamento em caso de erro
            }
        });
    }

    // ======================== INSERIR ========================

    public void inserir(Produto produto,
                        FirebaseStorageDataSource.UploadCallback callback) {
        produto.setUserId(userId);
        isLoading.postValue(true); // ✅ Inicia carregamento para upload/save

        BpdDatabase.databaseWriteExecutor.execute(() -> {
            long newId = produtoDao.insert(produto);
            produto.setId((int) newId);
            localCache.invalidarProdutos();

            String caminhoImagemLocal = produto.getImagem();
            boolean temImagem = caminhoImagemLocal != null && !caminhoImagemLocal.isEmpty();

            if (temImagem && callback != null) {
                File arquivoLocal = new File(caminhoImagemLocal);
                storageDataSource.uploadImagem(produto.getId(), arquivoLocal,
                        new FirebaseStorageDataSource.UploadCallback() {
                            @Override
                            public void onProgresso(int porcentagem) {
                                callback.onProgresso(porcentagem);
                            }

                            @Override
                            public void onSucesso(String urlDownload) {
                                produto.setImagem(urlDownload);
                                BpdDatabase.databaseWriteExecutor.execute(() -> {
                                    produtoDao.update(produto);
                                    sincronizarProdutoNoFirestore(produto);
                                    isLoading.postValue(false); // ✅ Finaliza
                                });
                                callback.onSucesso(urlDownload);
                            }

                            @Override
                            public void onErro(Exception e) {
                                Log.e(TAG, "Falha no upload da imagem: " + e.getMessage());
                                sincronizarProdutoNoFirestore(produto);
                                isLoading.postValue(false); // ✅ Finaliza
                                callback.onErro(e);
                            }
                        });
            } else {
                sincronizarProdutoNoFirestore(produto);
                isLoading.postValue(false); // ✅ Finaliza
            }
        });
    }

    public void inserir(Produto produto) {
        inserir(produto, null);
    }

    // ======================== ATUALIZAR ========================

    public void atualizar(Produto produto,
                          String urlImagemAntiga,
                          FirebaseStorageDataSource.UploadCallback callback) {
        isLoading.postValue(true); // ✅ Inicia carregamento

        BpdDatabase.databaseWriteExecutor.execute(() -> {
            produtoDao.update(produto);
            localCache.invalidarProdutos();

            String caminhoImagem = produto.getImagem();
            boolean ehUrlStorage = caminhoImagem != null && caminhoImagem.startsWith("https://");
            boolean temImagemLocal = caminhoImagem != null
                    && !caminhoImagem.isEmpty()
                    && !ehUrlStorage;

            if (temImagemLocal && callback != null) {
                if (urlImagemAntiga != null && !urlImagemAntiga.isEmpty()) {
                    storageDataSource.deletarImagemPorUrl(urlImagemAntiga,
                            new FirebaseStorageDataSource.StorageCallback() {
                                @Override public void onSucesso() {
                                    Log.d(TAG, "Imagem antiga deletada com sucesso.");
                                }
                                @Override public void onErro(@NonNull Exception e) {
                                    Log.w(TAG, "Falha ao deletar imagem antiga (não crítico): " + e.getMessage());
                                }
                            });
                }

                File arquivoLocal = new File(caminhoImagem);
                storageDataSource.uploadImagem(produto.getId(), arquivoLocal,
                        new FirebaseStorageDataSource.UploadCallback() {
                            @Override
                            public void onProgresso(int porcentagem) {
                                callback.onProgresso(porcentagem);
                            }

                            @Override
                            public void onSucesso(String urlDownload) {
                                produto.setImagem(urlDownload);
                                BpdDatabase.databaseWriteExecutor.execute(() -> {
                                    produtoDao.update(produto);
                                    sincronizarAtualizacaoNoFirestore(produto);
                                    isLoading.postValue(false); // ✅ Finaliza
                                });
                                callback.onSucesso(urlDownload);
                            }

                            @Override
                            public void onErro(Exception e) {
                                Log.e(TAG, "Falha no upload ao atualizar: " + e.getMessage());
                                sincronizarAtualizacaoNoFirestore(produto);
                                isLoading.postValue(false); // ✅ Finaliza
                                callback.onErro(e);
                            }
                        });
            } else {
                sincronizarAtualizacaoNoFirestore(produto);
                isLoading.postValue(false); // ✅ Finaliza
            }
        });
    }

    public void atualizar(Produto produto) {
        atualizar(produto, null, null);
    }

    // ======================== LIXEIRA ========================

    public void moverParaLixeira(int id) {
        BpdDatabase.databaseWriteExecutor.execute(() -> {
            produtoDao.moverParaLixeira(id, System.currentTimeMillis());
            localCache.invalidarProdutos();
            firebaseDataSource.moverProdutoParaLixeira(id, null);
        });
    }

    /**
     * ✅ Restaura um produto da lixeira
     */
    public void restaurar(int id) {
        BpdDatabase.databaseWriteExecutor.execute(() -> {
            produtoDao.restaurarProduto(id);
            localCache.invalidarProdutos();
            firebaseDataSource.restaurarProduto(id, null);
        });
    }

    /**
     * ✅ Exclui permanentemente um produto (Room, Firestore e Storage)
     */
    public void excluirDefinitivoPorId(int id) {
        BpdDatabase.databaseWriteExecutor.execute(() -> {
            // 1. Busca o produto antes de deletar para pegar a URL da imagem
            Produto produto = produtoDao.buscarPorIdSync(id);

            // 2. Remove do Room e invalida cache
            produtoDao.removerPorId(id);
            localCache.invalidarProdutos();

            // 3. Remove do Firestore
            firebaseDataSource.excluirProdutoPermanente(id, null);

            // 4. Remove a imagem do Firebase Storage (se houver URL)
            if (produto != null) {
                String urlImagem = produto.getImagem();
                if (urlImagem != null && !urlImagem.isEmpty() && urlImagem.startsWith("https://")) {
                    Log.d(TAG, "Deletando imagem do Storage para produto id=" + id);
                    storageDataSource.deletarImagemPorUrl(urlImagem, new FirebaseStorageDataSource.StorageCallback() {
                        @Override
                        public void onSucesso() {
                            Log.d(TAG, "Imagem do produto " + id + " deletada do Storage.");
                        }
                        @Override
                        public void onErro(@NonNull Exception e) {
                            Log.w(TAG, "Falha ao deletar imagem do Storage (produto " + id + "): " + e.getMessage());
                        }
                    });
                } else {
                    // Imagem local (caminho de arquivo) — apaga do disco
                    if (urlImagem != null && !urlImagem.isEmpty()) {
                        File arquivoLocal = new File(urlImagem);
                        if (arquivoLocal.exists()) {
                            arquivoLocal.delete();
                            Log.d(TAG, "Imagem local deletada: " + urlImagem);
                        }
                    }
                }
            }
        });
    }

    // ======================== MÉTODOS PRIVADOS SYNC ========================

    private void sincronizarProdutoNoFirestore(Produto produto) {
        firebaseDataSource.salvarProduto(produto, null);
    }

    private void sincronizarAtualizacaoNoFirestore(Produto produto) {
        firebaseDataSource.atualizarProduto(produto, null);
    }
}
