package com.rogger.bipando.data.dao;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rogger.bipando.data.model.Categoria;

import java.util.List;

@Dao
public interface CategoriaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long inserirCategoria(Categoria categoria);

    @Update
    void atualizarCategoria(Categoria categoria);


    @Delete
    void removerCategoria(Categoria categoria);

    @Query("SELECT * FROM categorias WHERE userId = :userId ORDER BY nome ASC")
    LiveData<List<Categoria>> listarCategorias(String userId);
}

