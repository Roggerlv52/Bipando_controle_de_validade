package com.rogger.bipando.ui.deleteitem;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.rogger.bipando.data.model.Produto;
import com.rogger.bipando.data.repository.ProdutoRepository;
import com.rogger.bipando.ui.viewmodel.DataViewModel;

import java.util.List;

public class ItemDeletedViewModel extends AndroidViewModel {

    private final ProdutoRepository repository;
    private final LiveData<List<Produto>> produtosDeletados;

    public ItemDeletedViewModel(@NonNull Application application) {
        super(application);
        repository = new ProdutoRepository(application);
        produtosDeletados = repository.getProdutosDeletados();
    }

    public LiveData<List<Produto>> getProdutosDeletados() {
        return produtosDeletados;
    }

    public void restaurar(int id) {
        repository.restaurar(id);
    }

    public void remover(int id) {
        repository.excluirDefinitivoPorId(id);
    }
}

