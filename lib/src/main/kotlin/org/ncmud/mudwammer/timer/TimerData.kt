package org.ncmud.mudwammer.timer

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.ncmud.mudwammer.responder.TriggerResponder

@Entity(tableName = "timers")
data class TimerData(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var connectionId: Long = 0,
    var name: String = "",
    var ordinal: Int = 0,
    var seconds: Int = 30,
    @get:JvmName("isRepeat") var repeat: Boolean = true,
) {
    @Ignore
    @get:JvmName("isPlaying")
    var playing: Boolean = false

    @Ignore var startTime: Long = 0L
    @Ignore var remainingTime: Int = 0

    @Ignore var responders: MutableList<TriggerResponder> = mutableListOf()

    fun reset() { /* no-op, preserved for API compat */ }

    fun copy(): TimerData {
        val tmp = TimerData(
            id = id, connectionId = connectionId, name = name,
            ordinal = ordinal, seconds = seconds, repeat = repeat
        )
        tmp.playing = playing
        tmp.remainingTime = remainingTime
        for (r in responders) { tmp.responders.add(r.copy()) }
        return tmp
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TimerData) return false
        if (name != other.name) return false
        if (ordinal != other.ordinal) return false
        if (seconds != other.seconds) return false
        if (repeat != other.repeat) return false
        if (playing != other.playing) return false
        if (responders.size != other.responders.size) return false
        for (i in responders.indices) {
            if (responders[i] != other.responders[i]) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + ordinal
        result = 31 * result + seconds
        result = 31 * result + repeat.hashCode()
        return result
    }
}
