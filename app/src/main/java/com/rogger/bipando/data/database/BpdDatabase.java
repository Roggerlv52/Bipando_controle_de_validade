package com.rogger.bipando.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.rogger.bipando.data.dao.CategoriaDao;
import com.rogger.bipando.data.dao.ProdutoDao;
import com.rogger.bipando.data.model.Categoria;
import com.rogger.bipando.data.model.Produto;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Database(entities = {Produto.class, Categoria.class}, version = 1, exportSchema = false)
public abstract class BpdDatabase extends RoomDatabase {
    public static final Executor databaseWriteExecutor = Executors.newSingleThreadExecutor();

    private static volatile BpdDatabase INSTANCE;
    public abstract ProdutoDao produtoDao();
    public abstract CategoriaDao categoriaDao();

    public static BpdDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (BpdDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            BpdDatabase.class,
                            "bpd_data_base"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }

}
