package com.rogger.bp.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.rogger.bp.data.dao.CategoriaDao;
import com.rogger.bp.data.dao.ProdutoDao;
import com.rogger.bp.data.model.Categoria;
import com.rogger.bp.data.model.Produto;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Produto.class, Categoria.class}, version = 3, exportSchema = false)
public abstract class BpdDatabase extends RoomDatabase {
    public static final Executor databaseWriteExecutor = Executors.newSingleThreadExecutor();

    private static volatile BpdDatabase INSTANCE;

    public abstract ProdutoDao produtoDao();
    public abstract CategoriaDao categoriaDao();

    // 🔑 Migration: adiciona coluna userId nas duas tabelas
    static final Migration MIGRATION_1_2 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE produtos ADD COLUMN userId TEXT NOT NULL DEFAULT ''"
            );
            database.execSQL(
                    "ALTER TABLE categorias ADD COLUMN userId TEXT NOT NULL DEFAULT ''"
            );
        }
    };

    public static BpdDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (BpdDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    BpdDatabase.class,
                                    "bpd_data_base"
                            )
                            .addMigrations(MIGRATION_1_2) // 🔑 migration obrigatória
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}