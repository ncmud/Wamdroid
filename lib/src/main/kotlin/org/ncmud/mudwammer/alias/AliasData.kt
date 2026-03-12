package org.ncmud.mudwammer.alias

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aliases")
data class AliasData(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var connectionId: Long = 0,
    var pre: String = "",
    var post: String = "",
    @get:JvmName("isEnabled") var enabled: Boolean = true,
) {
    fun copy(): AliasData = AliasData(id, connectionId, pre, post, enabled)
}
