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

    public CategoriaViewModel(@NonNull Application application) {
        super(application);
        repository = new CategoriaRepository(application);
        categoriasComContagem = repository.getCategoriasComContagem();
    }

    /**
     * ✅ Retorna categorias com contagem (para o AdapterCategory)
     */
    public LiveData<List<CategoriaWithCount>> getCategoriesWithCount() {
        return categoriasComContagem;
    }

    /**
     * ✅ Mantém compatibilidade com código que espera apenas List<Categoria>
     */
    public LiveData<List<Categoria>> getCategories() {
        return Transformations.map(categoriasComContagem, input -> {
            List<Categoria> list = new ArrayList<>();
            for (CategoriaWithCount item : input) {
                Categoria c = item.categoria;
                c.setCount(item.count); // Mantém o count no objeto categoria
                list.add(c);
            }
            return list;
        });
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
