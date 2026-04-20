package com.rogger.bp.data.repository;


import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.rogger.bp.data.dao.CategoriaDao;
import com.rogger.bp.data.dao.ProdutoDao;
import com.rogger.bp.data.database.BpdDatabase;
import com.rogger.bp.data.database.FirebaseDataSource;
import com.rogger.bp.data.database.FirebaseStorageDataSource;
import com.rogger.bp.data.database.LocalCache;
import com.rogger.bp.data.database.ProdutoImagemDataSource;
import com.rogger.bp.data.model.Categoria;
import com.rogger.bp.data.model.Produto;
import com.rogger.bp.data.model.ProdutoImagem;

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
    private final CategoriaDao              categoriaDao;
    private final FirebaseDataSource        firebaseDataSource;
    private final FirebaseStorageDataSource storageDataSource;
    private final ProdutoImagemDataSource   produtoImagemDataSource;
    private final LocalCache                localCache;
    private final String                    userId;

    private final LiveData<List<Produto>> produtosAtivos;
    private final LiveData<List<Produto>> produtosDeletados;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public ProdutoRepository(Application application) {
        BpdDatabase db      = BpdDatabase.getDatabase(application);
        produtoDao          = db.produtoDao();
        categoriaDao        = db.categoriaDao();
        firebaseDataSource  = FirebaseDataSource.getInstance();
        storageDataSource   = FirebaseStorageDataSource.getInstance();
        produtoImagemDataSource = ProdutoImagemDataSource.getInstance();
        localCache          = LocalCache.getInstance();
        userId              = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        produtosAtivos    = produtoDao.listarProdutosAtivos(userId);
        produtosDeletados = produtoDao.listarProdutosDeletados(userId);

        sincronizarDoFirestore();
    }

    // ======================== LEITURA ========================

    public LiveData<Boolean> getIsLoading()             { return isLoading; }
    public LiveData<List<Produto>> getProdutosAtivos()  { return produtosAtivos; }
    public LiveData<List<Produto>> getProdutosDeletados(){ return produtosDeletados; }

    public LiveData<List<Produto>> buscarPorNome(String query) {
        return produtoDao.buscarPorNome(userId, query);
    }

    public LiveData<List<Produto>> buscarPorCodigoBarras(String barcode) {
        return produtoDao.buscarPorCodigoBarras(userId, barcode);
    }

    public void sincronizarDoFirestore() {
        if (localCache.getProdutosAtivos() != null) {
            Log.d(TAG, "Cache válido, pulando busca no Firestore.");
            return;
        }

        Log.d(TAG, "Cache expirado/vazio, buscando no Firestore...");
        isLoading.postValue(true);

        firebaseDataSource.buscarProdutosAtivos(new FirebaseDataSource.FirestoreCallback<List<Produto>>() {
            @Override
            public void onSuccess(List<Produto> produtos) {
                Log.d(TAG, "Sincronizado " + produtos.size() + " produtos do Firestore");
                localCache.setProdutosAtivos(produtos);

                BpdDatabase.databaseWriteExecutor.execute(() -> {
                    preencherNomesCategorias(produtos);
                    for (Produto p : produtos) {
                        p.setUserId(userId);
                        produtoDao.insert(p);
                    }
                    isLoading.postValue(false);
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Erro ao sincronizar: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    /**
     * ✅ Busca nomes das categorias locais e preenche nos produtos em memória
     */
    private void preencherNomesCategorias(List<Produto> produtos) {
        List<Categoria> categorias = categoriaDao.listarCategoriasSync(userId);
        java.util.Map<Integer, String> nomeMap = new java.util.HashMap<>();
        if (categorias != null) {
            for (Categoria c : categorias) {
                nomeMap.put(c.getId(), c.getNome());
            }
        }
        for (Produto p : produtos) {
            String nome = nomeMap.get(p.getCategoryId());
            p.setNomeCategoria(nome != null ? nome : "");
        }
    }

    // ======================== INSERIR ========================

    public void inserir(Produto produto,
                        FirebaseStorageDataSource.UploadCallback callback) {
        produto.setUserId(userId);
        isLoading.postValue(true);

        BpdDatabase.databaseWriteExecutor.execute(() -> {
            // ✅ Preenche o nome da categoria antes de salvar
            List<Produto> listaTemp = new java.util.ArrayList<>();
            listaTemp.add(produto);
            preencherNomesCategorias(listaTemp);

            long newId = produtoDao.insert(produto);
            produto.setId((int) newId);
            localCache.invalidarProdutos();

            String caminhoLocal = produto.getImagem();
            boolean temImagemLocal = caminhoLocal != null
                    && !caminhoLocal.isEmpty()
                    && !caminhoLocal.startsWith("https://");

            String barcode = produto.getCodigoBarras();
            boolean temBarcode = barcode != null && !barcode.isEmpty();

            if (!temImagemLocal) {
                sincronizarProdutoNoFirestore(produto);
                isLoading.postValue(false);
                return;
            }

            File arquivoLocal = new File(caminhoLocal);
            if (temBarcode) {
                inserirComImagemGlobal(produto, barcode, arquivoLocal, callback);
            } else {
                inserirComImagemLegada(produto, arquivoLocal, callback);
            }
        });
    }

    public void inserir(Produto produto) {
        inserir(produto, null);
    }

    private void inserirComImagemGlobal(Produto produto,
                                        String barcode,
                                        File arquivoLocal,
                                        FirebaseStorageDataSource.UploadCallback callback) {

        produtoImagemDataSource.buscarPorCodigoBarras(barcode,
                new ProdutoImagemDataSource.BuscaCallback() {

                    @Override
                    public void onEncontrado(ProdutoImagem produtoImagem) {
                        Log.d(TAG, "Imagem global encontrada para: " + barcode);
                        produto.setImagem(produtoImagem.getImagemUrl());

                        BpdDatabase.databaseWriteExecutor.execute(() -> {
                            produtoDao.update(produto);
                            sincronizarProdutoNoFirestore(produto);
                            isLoading.postValue(false);
                        });

                        if (callback != null) {
                            callback.onSucesso(produtoImagem.getImagemUrl());
                        }
                        arquivoLocal.delete();
                    }

                    @Override
                    public void onNaoEncontrado() {
                        Log.d(TAG, "Barcode novo. Fazendo upload global: " + barcode);
                        uploadGlobalEGravar(produto, barcode, arquivoLocal, callback);
                    }

                    @Override
                    public void onErro(Exception e) {
                        Log.e(TAG, "Erro ao consultar imagem global, prosseguindo com upload: "
                                + e.getMessage());
                        uploadGlobalEGravar(produto, barcode, arquivoLocal, callback);
                    }
                });
    }

    private void uploadGlobalEGravar(Produto produto,
                                     String barcode,
                                     File arquivoLocal,
                                     FirebaseStorageDataSource.UploadCallback callback) {

        String nomeProduto = produto.getNome() != null ? produto.getNome() : "";

        produtoImagemDataSource.salvarNovaImagem(barcode, nomeProduto, arquivoLocal,
                new ProdutoImagemDataSource.SalvarCallback() {

                    @Override
                    public void onProgresso(int porcentagem) {
                        if (callback != null) callback.onProgresso(porcentagem);
                    }

                    @Override
                    public void onSucesso(ProdutoImagem produtoImagem) {
                        String urlDownload = produtoImagem.getImagemUrl();
                        produto.setImagem(urlDownload);
                        Log.d(TAG, "Upload global concluído: " + urlDownload);

                        BpdDatabase.databaseWriteExecutor.execute(() -> {
                            produtoDao.update(produto);
                            sincronizarProdutoNoFirestore(produto);
                            isLoading.postValue(false);
                        });

                        if (callback != null) callback.onSucesso(urlDownload);
                    }

                    @Override
                    public void onJaExiste(@NonNull ProdutoImagem produtoImagemExistente) {

                    }

                    @Override
                    public void onErro(Exception e) {
                        Log.e(TAG, "Falha no upload global: " + e.getMessage());
                        BpdDatabase.databaseWriteExecutor.execute(() -> {
                            sincronizarProdutoNoFirestore(produto);
                            isLoading.postValue(false);
                        });
                        if (callback != null) callback.onErro(e);
                    }
                });
    }

    private void inserirComImagemLegada(Produto produto,
                                        File arquivoLocal,
                                        FirebaseStorageDataSource.UploadCallback callback) {
        storageDataSource.uploadImagem(produto.getId(), arquivoLocal,
                new FirebaseStorageDataSource.UploadCallback() {
                    @Override
                    public void onProgresso(int porcentagem) {
                        if (callback != null) callback.onProgresso(porcentagem);
                    }

                    @Override
                    public void onSucesso(String urlDownload) {
                        produto.setImagem(urlDownload);
                        BpdDatabase.databaseWriteExecutor.execute(() -> {
                            produtoDao.update(produto);
                            sincronizarProdutoNoFirestore(produto);
                            isLoading.postValue(false);
                        });
                        if (callback != null) callback.onSucesso(urlDownload);
                    }

                    @Override
                    public void onErro(Exception e) {
                        Log.e(TAG, "Falha no upload legado: " + e.getMessage());
                        BpdDatabase.databaseWriteExecutor.execute(() -> {
                            sincronizarProdutoNoFirestore(produto);
                            isLoading.postValue(false);
                        });
                        if (callback != null) callback.onErro(e);
                    }
                });
    }

    // ======================== ATUALIZAR ========================

    public void atualizar(Produto produto,
                          String urlImagemAntiga,
                          FirebaseStorageDataSource.UploadCallback callback) {
        isLoading.postValue(true);

        BpdDatabase.databaseWriteExecutor.execute(() -> {
            // ✅ Preenche o nome da categoria antes de atualizar
            List<Produto> listaTemp = new java.util.ArrayList<>();
            listaTemp.add(produto);
            preencherNomesCategorias(listaTemp);

            produtoDao.update(produto);
            localCache.invalidarProdutos();

            String caminhoImagem = produto.getImagem();
            boolean ehUrlStorage = caminhoImagem != null && caminhoImagem.startsWith("https://");
            boolean temImagemLocal = caminhoImagem != null
                    && !caminhoImagem.isEmpty()
                    && !ehUrlStorage;

            if (temImagemLocal && callback != null) {
                if (urlImagemAntiga != null && !urlImagemAntiga.isEmpty()) {
                    storageDataSource.deletarImagemPorUrl(urlImagemAntiga, null);
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
                                    isLoading.postValue(false);
                                });
                                callback.onSucesso(urlDownload);
                            }

                            @Override
                            public void onErro(Exception e) {
                                Log.e(TAG, "Falha no upload ao atualizar: " + e.getMessage());
                                sincronizarAtualizacaoNoFirestore(produto);
                                isLoading.postValue(false);
                                callback.onErro(e);
                            }
                        });
            } else {
                sincronizarAtualizacaoNoFirestore(produto);
                isLoading.postValue(false);
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

    public void restaurar(int id) {
        BpdDatabase.databaseWriteExecutor.execute(() -> {
            produtoDao.restaurarProduto(id);
            localCache.invalidarProdutos();
            //firebaseDataSource.restaurarProdutoDaLixeira(id, null);
        });
    }

    public void excluirDefinitivoPorId(int id) {
        BpdDatabase.databaseWriteExecutor.execute(() -> {
            produtoDao.removerPorId(id);
            localCache.invalidarProdutos();
            //firebaseDataSource.excluirProduto(id, null);
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
