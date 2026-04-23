package com.rogger.bp.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rogger.bp.data.model.Categoria;
import com.rogger.bp.data.model.CategoriaWithCount;

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
    List<Categoria> listarCategoriasSync(String userId);

    /**
     * ✅ Query otimizada que já traz a contagem de produtos ativos por categoria.
     * Faz um LEFT JOIN para garantir que categorias sem produtos também apareçam com count 0.
     */
    @Query("SELECT c.*, COUNT(p.id) as count " +
            "FROM categorias c " +
            "LEFT JOIN produtos p ON c.id = p.categoryId AND p.deleted = 0 " +
            "WHERE c.userId = :userId " +
            "GROUP BY c.id " +
            "ORDER BY c.nome ASC")
    LiveData<List<CategoriaWithCount>> listarCategoriasComContagem(String userId);

    @Query("SELECT COUNT(*) FROM categorias WHERE userId = :userId")
    LiveData<Integer> getCountCategorias(String userId);
}
