package com.rogger.bp.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.rogger.bp.data.database.FirebaseStorageDataSource;
import com.rogger.bp.data.model.Produto;
import com.rogger.bp.data.repository.ProdutoRepository;

import java.util.List;

public class DataViewModel extends AndroidViewModel {

    private ProdutoRepository repository;
    private LiveData<List<Produto>> produtos;

    public DataViewModel(@NonNull Application application) {
        super(application);
        repository = new ProdutoRepository(application);
        produtos = repository.getProdutosAtivos();
    }

    public LiveData<List<Produto>> getProdutos() {
        return produtos;
    }

    /**
     * ✅ Retorna o estado de carregamento do repositório
     */
    public LiveData<Boolean> getIsLoading() {
        return repository.getIsLoading();
    }

    public LiveData<Produto> getProdutoByIdInMemory(int id) {
        MutableLiveData<Produto> resultado = new MutableLiveData<>();
        produtos.observeForever(lista -> {
            if (lista != null) {
                for (Produto p : lista) {
                    if (p.getId() == id) {
                        resultado.postValue(p);
                        break;
                    }
                }
            }
        });
        return resultado;
    }


    public void insert(Produto p) {
        repository.inserir(p, null);
    }

    public void insert(Produto p, FirebaseStorageDataSource.UploadCallback callback) {
        repository.inserir(p, callback);
    }

    public void update(Produto p) {
        repository.atualizar(p, null, null);
    }

    public void update(Produto p, String urlAntiga, FirebaseStorageDataSource.UploadCallback callback) {
        repository.atualizar(p, urlAntiga, callback);
    }

    public void moverParaLixeira(int id) {
        repository.moverParaLixeira(id);
    }

    public LiveData<List<Produto>> getProdutosDeletados() {
        return repository.getProdutosDeletados();
    }

    public void delete(Produto produto) {
        produto.setDeleted(true);
        produto.setDeletedAt(System.currentTimeMillis());
        update(produto);
    }


    // 🔍 Busca por nome
    public LiveData<List<Produto>> buscarPorNome(String query) {
        return repository.buscarPorNome(query);
    }
    public LiveData<List<Produto>> buscarPorNomeCategoria(String query) {
        return repository.buscarPorNomeCategoria(query);
    }
    // 🔍 Busca por código de barras
    public LiveData<List<Produto>> buscarPorCodigoBarras(String barcode) {
        return repository.buscarPorCodigoBarras(barcode);
    }

    public LiveData<Integer> getCountAtivos() {
        return repository.getCountAtivos();
    }

    public LiveData<Integer> getCountDeletados() {
        return repository.getCountDeletados();
    }
}