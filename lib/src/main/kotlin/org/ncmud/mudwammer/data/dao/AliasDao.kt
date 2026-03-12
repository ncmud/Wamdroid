package org.ncmud.mudwammer.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.ncmud.mudwammer.alias.AliasData
import kotlinx.coroutines.flow.Flow

@Dao
interface AliasDao {
    @Query("SELECT * FROM aliases WHERE connectionId = :connectionId")
    fun getByConnection(connectionId: Long): Flow<List<AliasData>>

    @Insert fun insert(alias: AliasData): Long
    @Update fun update(alias: AliasData)
    @Delete fun delete(alias: AliasData)
    @Query("DELETE FROM aliases WHERE connectionId = :connectionId")
    fun deleteByConnection(connectionId: Long)
}
