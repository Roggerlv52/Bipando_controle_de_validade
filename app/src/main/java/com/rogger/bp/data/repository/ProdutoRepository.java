package com.rogger.bp.data.repository;


import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

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
 *
 * Estratégia:
 *  - Leitura: Room é a fonte de verdade para a UI (via LiveData).
 *             Firestore sincroniza em segundo plano e atualiza o Room.
 *  - Escrita: Room primeiro (UI reage instantaneamente),
 *             depois Firestore + Storage em paralelo.
 *  - Imagens: Upload para Storage ao inserir/atualizar com imagem.
 *             Delete do Storage ao excluir permanentemente.
 *  - Cache:   LocalCache evita buscas desnecessárias no Firestore
 *             ao navegar entre telas. TTL = 5 minutos.
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

    /** LiveData principal da UI — observa o Room local */
    public LiveData<List<Produto>> getProdutosAtivos() {
        return produtosAtivos;
    }

    public LiveData<List<Produto>> getProdutosDeletados() {
        return produtosDeletados;
    }

    /**
     * Força uma re-sincronização do Firestore para o Room.
     * Útil para pull-to-refresh ou ao retornar para a tela principal.
     */
    public void sincronizarDoFirestore() {
        List<Produto> cached = localCache.getProdutosAtivos();
        if (cached != null) {
            Log.d(TAG, "Cache válido, pulando busca no Firestore. " +
                    "Expira em " + (localCache.getProdutosTtlRestante() / 1000) + "s");
            return;
        }

        Log.d(TAG, "Cache expirado/vazio, buscando no Firestore...");
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
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Erro ao sincronizar produtos: " + e.getMessage());
            }
        });
    }

    // ======================== INSERIR ========================

    /**
     * Insere produto no Room imediatamente e salva no Firestore em paralelo.
     * Se o produto tiver imagem local, faz upload para o Storage
     * e atualiza a URL no Room e no Firestore após o upload.
     *
     * @param produto    Produto a ser inserido
     * @param callback   Callback de progresso e resultado do upload de imagem.
     *                   Pode ser null se o produto não tiver imagem.
     */
    public void inserir(Produto produto,
                        FirebaseStorageDataSource.UploadCallback callback) {
        produto.setUserId(userId);

        BpdDatabase.databaseWriteExecutor.execute(() -> {
            long newId = produtoDao.insert(produto);
            produto.setId((int) newId);
            localCache.invalidarProdutos();

            String caminhoImagemLocal = produto.getImagem();
            Log.d("ProdutoRepository","Caminho da imagem->"+caminhoImagemLocal);
            boolean temImagem = caminhoImagemLocal != null && !caminhoImagemLocal.isEmpty();

            if (temImagem && callback != null) {
                // Tem imagem: faz upload para o Storage
                File arquivoLocal = new File(caminhoImagemLocal);
                storageDataSource.uploadImagem(produto.getId(), arquivoLocal,
                        new FirebaseStorageDataSource.UploadCallback() {
                            @Override
                            public void onProgresso(int porcentagem) {
                                callback.onProgresso(porcentagem);
                            }

                            @Override
                            public void onSucesso(String urlDownload) {
                                // Atualiza produto com a URL do Storage
                                produto.setImagem(urlDownload);

                                // Persiste URL atualizada no Room
                                BpdDatabase.databaseWriteExecutor.execute(() ->
                                        produtoDao.update(produto)
                                );

                                // Salva no Firestore com a URL do Storage
                                sincronizarProdutoNoFirestore(produto);

                                callback.onSucesso(urlDownload);
                            }

                            @Override
                            public void onErro(Exception e) {
                                Log.e(TAG, "Falha no upload da imagem: " + e.getMessage());
                                // Produto continua salvo localmente com caminho local
                                sincronizarProdutoNoFirestore(produto);
                                callback.onErro(e);
                            }
                        });
            } else {
                // Sem imagem: salva direto no Firestore
                sincronizarProdutoNoFirestore(produto);
            }
        });
    }

    /**
     * Versão simplificada do inserir para produtos sem imagem.
     */
    public void inserir(Produto produto) {
        inserir(produto, null);
    }

    // ======================== ATUALIZAR ========================

    /**
     * Atualiza produto no Room e no Firestore.
     * Se houver nova imagem local, faz upload para o Storage,
     * deleta a imagem antiga e atualiza a URL.
     *
     * @param produto         Produto com dados atualizados
     * @param urlImagemAntiga URL da imagem anterior no Storage (para deletar). Null se não havia.
     * @param callback        Callback de progresso e resultado. Null se não há nova imagem.
     */
    public void atualizar(Produto produto,
                          String urlImagemAntiga,
                          FirebaseStorageDataSource.UploadCallback callback) {

        BpdDatabase.databaseWriteExecutor.execute(() -> {
            produtoDao.update(produto);
            localCache.invalidarProdutos();

            String caminhoImagem = produto.getImagem();
            boolean ehUrlStorage = caminhoImagem != null && caminhoImagem.startsWith("https://");
            boolean temImagemLocal = caminhoImagem != null
                    && !caminhoImagem.isEmpty()
                    && !ehUrlStorage;

            if (temImagemLocal && callback != null) {
                // Deleta imagem antiga do Storage (se existia)
                if (urlImagemAntiga != null && !urlImagemAntiga.isEmpty()) {
                    storageDataSource.deletarImagemPorUrl(urlImagemAntiga,
                            new FirebaseStorageDataSource.StorageCallback() {
                                @Override public void onSucesso() {
                                    Log.d(TAG, "Imagem antiga deletada do Storage.");
                                }
                                @Override public void onErro(Exception e) {
                                    Log.w(TAG, "Falha ao deletar imagem antiga: " + e.getMessage());
                                }
                            });
                }

                // Upload da nova imagem
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
                                BpdDatabase.databaseWriteExecutor.execute(() ->
                                        produtoDao.update(produto)
                                );
                                sincronizarAtualizacaoNoFirestore(produto);
                                callback.onSucesso(urlDownload);
                            }

                            @Override
                            public void onErro(Exception e) {
                                Log.e(TAG, "Falha no upload ao atualizar: " + e.getMessage());
                                sincronizarAtualizacaoNoFirestore(produto);
                                callback.onErro(e);
                            }
                        });
            } else {
                // Sem nova imagem: atualiza direto no Firestore
                sincronizarAtualizacaoNoFirestore(produto);
            }
        });
    }

    /**
     * Versão simplificada do atualizar sem troca de imagem.
     */
    public void atualizar(Produto produto) {
        atualizar(produto, null, null);
    }

    // ======================== LIXEIRA ========================

    public void moverParaLixeira(int id) {
        BpdDatabase.databaseWriteExecutor.execute(() -> {
            produtoDao.moverParaLixeira(id, System.currentTimeMillis());
            localCache.invalidarProdutos();

            firebaseDataSource.moverProdutoParaLixeira(id,
                    new FirebaseDataSource.FirestoreCallback<Void>() {
                        @Override public void onSuccess(Void r) {
                            Log.d(TAG, "Lixeira sync OK: " + id);
                        }
                        @Override public void onFailure(Exception e) {
                            Log.e(TAG, "Lixeira sync fail: " + e.getMessage());
                        }
                    });
        });
    }

    public void restaurar(int id) {
        BpdDatabase.databaseWriteExecutor.execute(() -> {
            produtoDao.restaurarProduto(id);
            localCache.invalidarProdutos();

            firebaseDataSource.restaurarProduto(id,
                    new FirebaseDataSource.FirestoreCallback<Void>() {
                        @Override public void onSuccess(Void r) {
                            Log.d(TAG, "Restaurar sync OK: " + id);
                        }
                        @Override public void onFailure(Exception e) {
                            Log.e(TAG, "Restaurar sync fail: " + e.getMessage());
                        }
                    });
        });
    }

    // ======================== EXCLUIR ========================

    /**
     * Exclui produto permanentemente do Room, Firestore e Storage (imagem).
     *
     * @param id              ID do produto
     * @param urlImagemStorage URL da imagem no Storage (para deletar). Null se não tinha imagem.
     */
    public void excluirDefinitivoPorId(int id, String urlImagemStorage) {
        BpdDatabase.databaseWriteExecutor.execute(() -> {
            produtoDao.removerPorId(id);
            localCache.invalidarProdutos();

            // Deleta do Firestore
            firebaseDataSource.excluirProdutoPermanente(id,
                    new FirebaseDataSource.FirestoreCallback<Void>() {
                        @Override public void onSuccess(Void r) {
                            Log.d(TAG, "Exclusão Firestore OK: " + id);
                        }
                        @Override public void onFailure(Exception e) {
                            Log.e(TAG, "Exclusão Firestore fail: " + e.getMessage());
                        }
                    });

            // Deleta imagem do Storage (se existia)
            if (urlImagemStorage != null && !urlImagemStorage.isEmpty()) {
                storageDataSource.deletarImagemPorUrl(urlImagemStorage,
                        new FirebaseStorageDataSource.StorageCallback() {
                            @Override public void onSucesso() {
                                Log.d(TAG, "Imagem deletada do Storage: produto " + id);
                            }
                            @Override public void onErro(Exception e) {
                                Log.e(TAG, "Falha ao deletar imagem do Storage: " + e.getMessage());
                            }
                        });
            } else {
                // Tenta deletar pelo ID mesmo sem URL conhecida
                storageDataSource.deletarImagem(id,
                        new FirebaseStorageDataSource.StorageCallback() {
                            @Override public void onSucesso() {
                                Log.d(TAG, "Imagem deletada do Storage pelo ID: " + id);
                            }
                            @Override public void onErro(Exception e) {
                                Log.d(TAG, "Sem imagem no Storage para produto " + id);
                            }
                        });
            }
        });
    }

    /**
     * Versão simplificada do excluir sem URL de imagem conhecida.
     */
    public void excluirDefinitivoPorId(int id) {
        excluirDefinitivoPorId(id, null);
    }

    // ======================== HELPERS PRIVADOS ========================

    private void sincronizarProdutoNoFirestore(Produto produto) {
        firebaseDataSource.salvarProduto(produto, new FirebaseDataSource.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String docId) {
                Log.d(TAG, "Produto sincronizado no Firestore: " + docId);
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Falha ao salvar no Firestore: " + e.getMessage());
            }
        });
    }

    private void sincronizarAtualizacaoNoFirestore(Produto produto) {
        firebaseDataSource.atualizarProduto(produto, new FirebaseDataSource.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Produto atualizado no Firestore: " + produto.getId());
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Falha ao atualizar no Firestore: " + e.getMessage());
            }
        });
    }
}