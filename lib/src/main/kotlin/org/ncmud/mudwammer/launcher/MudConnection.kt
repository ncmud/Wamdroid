package org.ncmud.mudwammer.launcher

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class MudConnection(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    @get:JvmName("getDisplayName") @set:JvmName("setDisplayName")
    var displayName: String = "",
    @get:JvmName("getHostName") @set:JvmName("setHostName")
    var hostName: String = "",
    @get:JvmName("getPortString") @set:JvmName("setPortString")
    var portString: String = "",
    var lastPlayed: String = "never",
    @get:JvmName("isConnected") var connected: Boolean = false,
) {
    fun copy(): MudConnection = MudConnection(
        id, displayName, hostName, portString, lastPlayed, connected,
    )
}
