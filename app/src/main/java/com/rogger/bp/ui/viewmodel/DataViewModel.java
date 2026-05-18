package com.rogger.bp.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rogger.bp.data.model.PostProduct;

import java.util.List;

public class DataViewModel extends AndroidViewModel {


    public DataViewModel(@NonNull Application application) {
        super(application);

    }


    // Usado por: MainActivity (drawer counter lixeira)
    public LiveData<Integer> getCountDeletados() {
        return getCountDeletados();
    }

    // Usado por: SearchFragment
    public LiveData<List<PostProduct>> buscarPorNome(String query) {
        return buscarPorNome(query);
    }

    // Usado por: SearchFragment
    public LiveData<List<PostProduct>> buscarPorNomeCategoria(String query) {
        return buscarPorNomeCategoria(query);
    }

    // Usado por: SearchFragment
    public LiveData<List<PostProduct>> buscarPorCodigoBarras(String barcode) {
        return buscarPorNome(barcode);
    }
}