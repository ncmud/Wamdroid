package com.offsetnull.bt.service

/**
 * Sealed hierarchy representing all messages dispatched through Connection's handler.
 * Maps 1:1 to the MESSAGE_* constants in Connection.java.
 */
sealed interface ConnectionCommand {
    // Lifecycle
    data object Startup : ConnectionCommand
    data object Reconnect : ConnectionCommand
    data object Connected : ConnectionCommand
    data object Disconnected : ConnectionCommand
    data class TerminatedByPeer(val unit: Unit = Unit) : ConnectionCommand

    // Network I/O
    data class Process(val data: ByteArray) : ConnectionCommand
    data class SendDataString(val text: String) : ConnectionCommand
    data class SendDataBytes(val data: ByteArray) : ConnectionCommand
    data class SendGmcpData(val data: String) : ConnectionCommand
    data class StartCompress(val trailingData: ByteArray?) : ConnectionCommand
    data class SendOptionData(val data: ByteArray, val debugMessage: String?) : ConnectionCommand

    // Display
    data class ProcessorWarning(val text: String) : ConnectionCommand
    data class BellReceived(val unit: Unit = Unit) : ConnectionCommand
    data class DialogError(val message: String) : ConnectionCommand
    data object MccpFatalError : ConnectionCommand
    data class LuaNote(val text: String) : ConnectionCommand
    data class LuaError(val message: String) : ConnectionCommand
    data class TriggerLuaError(val message: String) : ConnectionCommand

    // Window management
    data class LineToWindow(val target: String, val line: Any) : ConnectionCommand
    data class DrawWindow(val name: String) : ConnectionCommand
    data class NewWindow(val token: Any) : ConnectionCommand
    data class WindowBuffer(val name: String, val enabled: Boolean) : ConnectionCommand
    data class WindowXCallS(
        val token: String,
        val function: String,
        val data: Any,
    ) : ConnectionCommand
    data class WindowXCallB(
        val token: String,
        val function: String,
        val data: ByteArray,
    ) : ConnectionCommand
    data class InvalidateWindowText(val name: String) : ConnectionCommand

    // Plugin management
    data class AddFunctionCallback(
        val id: String,
        val command: String,
        val callback: String,
    ) : ConnectionCommand
    data class GmcpTriggered(
        val plugin: String,
        val callback: String,
        val data: Any,
    ) : ConnectionCommand
    data class CallPlugin(
        val plugin: String,
        val function: String,
        val data: String,
    ) : ConnectionCommand
    data class AddLink(val path: String) : ConnectionCommand
    data class DeletePlugin(val name: String) : ConnectionCommand

    // Echo negotiation
    data object DisableLocalEcho : ConnectionCommand
    data object EnableLocalEcho : ConnectionCommand

    // Settings
    data class SaveDirtyPlugin(val name: String) : ConnectionCommand
    data class ExportFile(val path: String) : ConnectionCommand
    data class ImportFile(val path: String) : ConnectionCommand
    data object ReloadSettings : ConnectionCommand
    data object ResetSettings : ConnectionCommand
    data object SetTriggersDirty : ConnectionCommand

    // Timers
    data class TimerAction(
        val name: String,
        val id: Int,
        val action: TimerActionType,
    ) : ConnectionCommand

    enum class TimerActionType { START, STOP, PAUSE, RESET, INFO }
}
