package com.offsetnull.bt.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.offsetnull.bt.button.SlickButtonData
import kotlinx.coroutines.flow.Flow

@Dao
interface ButtonDao {
    @Query("SELECT * FROM buttons WHERE connectionId = :connectionId")
    fun getByConnection(connectionId: Long): Flow<List<SlickButtonData>>

    @Query("SELECT * FROM buttons WHERE connectionId = :connectionId AND buttonSetName = :setName")
    fun getByConnectionAndSet(connectionId: Long, setName: String): Flow<List<SlickButtonData>>

    @Insert fun insert(button: SlickButtonData): Long
    @Update fun update(button: SlickButtonData)
    @Delete fun delete(button: SlickButtonData)
    @Query("DELETE FROM buttons WHERE connectionId = :connectionId")
    fun deleteByConnection(connectionId: Long)
}
