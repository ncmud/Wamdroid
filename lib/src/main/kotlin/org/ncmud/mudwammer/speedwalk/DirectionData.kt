package org.ncmud.mudwammer.speedwalk

data class DirectionData @JvmOverloads constructor(
    var direction: String = "",
    var command: String = "",
    var reverse: String = "",
) {
    fun copy(): DirectionData = DirectionData(direction, command, reverse)
}
