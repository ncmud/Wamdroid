package org.ncmud.mudwammer.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.ncmud.mudwammer.launcher.MudConnection
import kotlinx.coroutines.flow.Flow

@Dao
interface MudConnectionDao {
    @Query("SELECT * FROM connections")
    fun getAll(): Flow<List<MudConnection>>

    @Query("SELECT * FROM connections WHERE id = :id")
    fun getById(id: Long): MudConnection?

    @Insert fun insert(connection: MudConnection): Long
    @Update fun update(connection: MudConnection)
    @Delete fun delete(connection: MudConnection)
}
