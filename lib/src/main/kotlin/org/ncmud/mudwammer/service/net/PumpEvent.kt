package org.ncmud.mudwammer.service.net

/** Events emitted by [DataPumperLoop] to its owner (Connection). */
sealed interface PumpEvent {
    /** Raw bytes received from the server (after decompression if active). */
    data class DataReceived(val data: ByteArray) : PumpEvent
    /** Server closed the connection gracefully (EOF). */
    data object DisconnectedByPeer : PumpEvent
    /** Connection lost due to I/O error. */
    data class Disconnected(val cause: Exception? = null) : PumpEvent
    /** Informational/warning text (for display, not trigger processing). */
    data class Warning(val text: String) : PumpEvent
    /** Fatal MCCP decompression error. */
    data object MccpFatalError : PumpEvent
    /** Error requiring a dialog display. */
    data class DialogError(val message: String) : PumpEvent
}
