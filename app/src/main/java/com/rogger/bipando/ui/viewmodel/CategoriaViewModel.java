package com.rogger.bipando.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rogger.bipando.data.model.Categoria;
import com.rogger.bipando.data.repository.CategoriaRepository;

import java.util.List;

public class CategoriaViewModel extends AndroidViewModel {

    private CategoriaRepository repository;
    private LiveData<List<Categoria>> categorias;

    public CategoriaViewModel(@NonNull Application application) {
        super(application);
        repository = new CategoriaRepository(application);
        categorias = repository.getCategorias();
    }

    public LiveData<List<Categoria>> getCategories() {
        return categorias;
    }

    public void insert(Categoria c) {
        repository.inserir(c);
    }

    public void update(Categoria c) {
        repository.atualizar(c);
    }

    public void remover(Categoria c) {
        repository.remover(c);
    }
}
