package com.rogger.bp.data.dao

import androidx.room.*
import com.rogger.bp.data.model.PostGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups LIMIT 1")
    fun getGroupFlow(): Flow<PostGroup?>

    @Query("SELECT * FROM groups LIMIT 1")
    suspend fun getGroup(): PostGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: PostGroup)

    @Query("DELETE FROM groups")
    suspend fun clearGroup()
}
