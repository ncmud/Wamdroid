package com.offsetnull.bt.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.offsetnull.bt.trigger.TriggerData
import kotlinx.coroutines.flow.Flow

@Dao
interface TriggerDao {
    @Query("SELECT * FROM triggers WHERE connectionId = :connectionId")
    fun getByConnection(connectionId: Long): Flow<List<TriggerData>>

    @Insert fun insert(trigger: TriggerData): Long
    @Update fun update(trigger: TriggerData)
    @Delete fun delete(trigger: TriggerData)
    @Query("DELETE FROM triggers WHERE connectionId = :connectionId")
    fun deleteByConnection(connectionId: Long)
}
