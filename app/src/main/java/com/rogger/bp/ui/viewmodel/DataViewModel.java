package com.rogger.bp.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
        repository.inserir(p);
    }

    public void update(Produto p) {
        repository.atualizar(p);
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

}

