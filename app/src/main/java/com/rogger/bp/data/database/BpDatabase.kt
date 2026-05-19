package com.rogger.bp.data.database

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 19/05/2026
 * Hora: 13:02
 */
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rogger.bp.data.dao.CategoryDao
import com.rogger.bp.data.dao.ProductDao
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct

@Database(entities = [PostProduct::class, PostCategory::class], version = 1, exportSchema = false)
abstract class BpDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: BpDatabase? = null

        fun getDatabase(context: Context): BpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BpDatabase::class.java,
                    "bp_database"
                )
                    .fallbackToDestructiveMigration() // Estratégia de migração simples para desenvolvimento
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}