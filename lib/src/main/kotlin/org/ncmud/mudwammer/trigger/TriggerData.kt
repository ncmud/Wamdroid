package org.ncmud.mudwammer.trigger

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.ncmud.mudwammer.responder.TriggerResponder
import java.util.regex.Matcher
import java.util.regex.Pattern

@Entity(tableName = "triggers")
class TriggerData(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var connectionId: Long = 0,
    var name: String = "",
    pattern: String = "",
    @get:JvmName("isInterpretAsRegex") var interpretAsRegex: Boolean = false,
    @get:JvmName("isFireOnce") var fireOnce: Boolean = false,
    @get:JvmName("isHidden") var hidden: Boolean = false,
    @get:JvmName("isEnabled") var enabled: Boolean = true,
    @get:JvmName("isSave") var save: Boolean = true,
    var sequence: Int = DEFAULT_SEQUENCE,
    @get:JvmName("isKeepEvaluating") var keepEvaluating: Boolean = DEFAULT_KEEPEVAL,
    var group: String = DEFAULT_GROUP,
) {
    var pattern: String = pattern
        set(value) { field = value; buildData() }

    @Ignore
    @get:JvmName("isFired")
    var fired: Boolean = false

    @Ignore var responders: MutableList<TriggerResponder> = mutableListOf()

    @Ignore private var p: Pattern? = null
    @Ignore private var m: Matcher? = null

    init { buildData() }

    private fun buildData() {
        p = if (interpretAsRegex) Pattern.compile(pattern) else Pattern.compile("\\Q$pattern\\E")
        m = p!!.matcher("")
    }

    val matcher: Matcher? get() = m
    val compiledPattern: Pattern? get() = p

    fun copy(): TriggerData {
        val tmp = TriggerData(
            name = name, pattern = pattern, interpretAsRegex = interpretAsRegex,
            fireOnce = fireOnce, hidden = hidden, enabled = enabled, save = save,
            sequence = sequence, keepEvaluating = keepEvaluating, group = group
        )
        for (r in responders) { tmp.responders.add(r.copy()) }
        return tmp
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TriggerData) return false
        return name == other.name && pattern == other.pattern
            && interpretAsRegex == other.interpretAsRegex && fireOnce == other.fireOnce
            && hidden == other.hidden && enabled == other.enabled
            && sequence == other.sequence && group == other.group
            && keepEvaluating == other.keepEvaluating
            && responders == other.responders
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + pattern.hashCode()
        result = 31 * result + sequence
        return result
    }

    companion object {
        const val DEFAULT_SEQUENCE = 10
        const val DEFAULT_GROUP = ""
        const val DEFAULT_KEEPEVAL = true
    }
}
