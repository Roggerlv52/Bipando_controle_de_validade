package com.rogger.bp.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.rogger.bp.data.model.Categoria;
import com.rogger.bp.data.model.CategoriaWithCount;
import com.rogger.bp.data.repository.CategoriaRepository;

import java.util.ArrayList;
import java.util.List;

public class CategoriaViewModel extends AndroidViewModel {

    private CategoriaRepository repository;
    private LiveData<List<CategoriaWithCount>> categoriasComContagem;

    private final LiveData<List<Categoria>> categorias;

    public CategoriaViewModel(@NonNull Application application) {
        super(application);
        repository = new CategoriaRepository(application);
        categoriasComContagem = repository.getCategoriasComContagem();

        categorias = Transformations.map(categoriasComContagem, input -> {
            List<Categoria> list = new ArrayList<>();
            for (CategoriaWithCount item : input) {
                Categoria c = item.categoria;
                c.setCount(item.count);
                list.add(c);
            }
            return list;
        });
    }

    /**
     * ✅ Retorna categorias com contagem (para o AdapterCategory)
     */
    public LiveData<List<CategoriaWithCount>> getCategoriesWithCount() {
        return categoriasComContagem;
    }

    /**
     * ✅ Retorna sempre o mesmo LiveData — estável entre chamadas
     */
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