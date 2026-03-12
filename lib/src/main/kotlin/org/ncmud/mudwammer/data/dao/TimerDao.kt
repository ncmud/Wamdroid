package org.ncmud.mudwammer.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.ncmud.mudwammer.timer.TimerData
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {
    @Query("SELECT * FROM timers WHERE connectionId = :connectionId")
    fun getByConnection(connectionId: Long): Flow<List<TimerData>>

    @Insert fun insert(timer: TimerData): Long
    @Update fun update(timer: TimerData)
    @Delete fun delete(timer: TimerData)
    @Query("DELETE FROM timers WHERE connectionId = :connectionId")
    fun deleteByConnection(connectionId: Long)
}
