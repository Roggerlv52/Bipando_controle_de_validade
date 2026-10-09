package com.rogger.bp.data.dao

import androidx.room.*
import com.rogger.bp.data.model.PostGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun getGroupsFlow(): Flow<List<PostGroup>>

    @Query("SELECT * FROM groups LIMIT 1")
    suspend fun getGroup(): PostGroup?

    @Query("SELECT * FROM groups WHERE groupId = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): PostGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: PostGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<PostGroup>)

    @Query("DELETE FROM groups")
    suspend fun clearGroup()

    @Query("DELETE FROM groups WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: String)
}
