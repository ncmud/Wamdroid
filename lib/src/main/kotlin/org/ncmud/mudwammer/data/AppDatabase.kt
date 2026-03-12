package org.ncmud.mudwammer.data

import androidx.room.Database
import androidx.room.RoomDatabase
import org.ncmud.mudwammer.alias.AliasData
import org.ncmud.mudwammer.button.SlickButtonData
import org.ncmud.mudwammer.data.dao.AliasDao
import org.ncmud.mudwammer.data.dao.ButtonDao
import org.ncmud.mudwammer.data.dao.MudConnectionDao
import org.ncmud.mudwammer.data.dao.TimerDao
import org.ncmud.mudwammer.data.dao.TriggerDao
import org.ncmud.mudwammer.launcher.MudConnection
import org.ncmud.mudwammer.timer.TimerData
import org.ncmud.mudwammer.trigger.TriggerData

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
