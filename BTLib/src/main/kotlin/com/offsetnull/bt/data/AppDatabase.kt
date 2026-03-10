package com.offsetnull.bt.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.offsetnull.bt.alias.AliasData
import com.offsetnull.bt.button.SlickButtonData
import com.offsetnull.bt.data.dao.AliasDao
import com.offsetnull.bt.data.dao.ButtonDao
import com.offsetnull.bt.data.dao.MudConnectionDao
import com.offsetnull.bt.data.dao.TimerDao
import com.offsetnull.bt.data.dao.TriggerDao
import com.offsetnull.bt.launcher.MudConnection
import com.offsetnull.bt.timer.TimerData
import com.offsetnull.bt.trigger.TriggerData

@Database(
    entities = [
        AliasData::class,
        TriggerData::class,
        TimerData::class,
        SlickButtonData::class,
        MudConnection::class,
    ],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aliasDao(): AliasDao
    abstract fun triggerDao(): TriggerDao
    abstract fun timerDao(): TimerDao
    abstract fun buttonDao(): ButtonDao
    abstract fun connectionDao(): MudConnectionDao
}
