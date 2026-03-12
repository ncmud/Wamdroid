package org.ncmud.mudwammer.service

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message

/**
 * Backward-compatible Handler shim for callers that still send messages via
 * Handler (Plugin, Processor, DataPumper, responders).
 *
 * Converts incoming [Message] objects to [ConnectionCommand] and forwards them
 * through [ConnectionEventLoop]. Special cases (sendToServer, GMCP retry) are
 * handled via callbacks to Connection.
 *
 * This is transitional — once Plugin/Processor/DataPumper are migrated to send
 * ConnectionCommand directly, this class can be removed.
 */
class ConnectionHandlerShim(
    private val eventLoop: ConnectionEventLoop,
    private val sendToServerBytes: BytesSink,
    private val sendToServerString: StringSink,
    private val isPumpConnected: PumpCheck,
) : Handler(Looper.getMainLooper()) {

    /** Java-friendly functional interface for sending bytes. */
    fun interface BytesSink {
        fun send(data: ByteArray)
    }

    /** Java-friendly functional interface for sending strings. */
    fun interface StringSink {
        fun send(data: String)
    }

    /** Java-friendly functional interface for checking pump connectivity. */
    fun interface PumpCheck {
        fun isConnected(): Boolean
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun handleMessage(msg: Message) {
        when (msg.what) {
            Connection.MESSAGE_TERMINATED_BY_PEER ->
                eventLoop.send(ConnectionCommand.TerminatedByPeer())

            Connection.MESSAGE_TIMERSTOP ->
                eventLoop.send(ConnectionCommand.TimerAction(
                    msg.obj as String, msg.arg2, ConnectionCommand.TimerActionType.STOP))

            Connection.MESSAGE_TIMERSTART ->
                eventLoop.send(ConnectionCommand.TimerAction(
                    msg.obj as String, msg.arg2, ConnectionCommand.TimerActionType.START))

            Connection.MESSAGE_TIMERRESET ->
                eventLoop.send(ConnectionCommand.TimerAction(
                    msg.obj as String, msg.arg2, ConnectionCommand.TimerActionType.RESET))

            Connection.MESSAGE_TIMERINFO ->
                eventLoop.send(ConnectionCommand.TimerAction(
                    msg.obj as String, msg.arg2, ConnectionCommand.TimerActionType.INFO))

            Connection.MESSAGE_TIMERPAUSE ->
                eventLoop.send(ConnectionCommand.TimerAction(
                    msg.obj as String, msg.arg2, ConnectionCommand.TimerActionType.PAUSE))

            Connection.MESSAGE_CALLPLUGIN -> {
                val data = msg.data
                eventLoop.send(ConnectionCommand.CallPlugin(
                    data.getString("PLUGIN") ?: "",
                    data.getString("FUNCTION") ?: "",
                    data.getString("DATA") ?: ""))
            }

            Connection.MESSAGE_SETTRIGGERSDIRTY ->
                eventLoop.send(ConnectionCommand.SetTriggersDirty)

            Connection.MESSAGE_RELOADSETTINGS ->
                eventLoop.send(ConnectionCommand.ReloadSettings)

            Connection.MESSAGE_TRIGGER_LUA_ERROR ->
                eventLoop.send(ConnectionCommand.TriggerLuaError(msg.obj as String))

            Connection.MESSAGE_RECONNECT ->
                eventLoop.send(ConnectionCommand.Reconnect)

            Connection.MESSAGE_CONNECTED ->
                eventLoop.send(ConnectionCommand.Connected)

            Connection.MESSAGE_DELETEPLUGIN ->
                eventLoop.send(ConnectionCommand.DeletePlugin(msg.obj as String))

            Connection.MESSAGE_ADDLINK ->
                eventLoop.send(ConnectionCommand.AddLink(msg.obj as String))

            Connection.MESSAGE_DORESETSETTINGS ->
                eventLoop.send(ConnectionCommand.ResetSettings)

            Connection.MESSAGE_PLUGINLUAERROR ->
                eventLoop.send(ConnectionCommand.LuaError(msg.obj as String))

            Connection.MESSAGE_EXPORTFILE ->
                eventLoop.send(ConnectionCommand.ExportFile(msg.obj as String))

            Connection.MESSAGE_IMPORTFILE ->
                eventLoop.send(ConnectionCommand.ImportFile(msg.obj as String))

            Connection.MESSAGE_SAVESETTINGS ->
                eventLoop.send(ConnectionCommand.SaveDirtyPlugin(msg.obj as String))

            Connection.MESSAGE_GMCPTRIGGERED -> {
                val data = msg.data
                eventLoop.send(ConnectionCommand.GmcpTriggered(
                    data.getString("TARGET") ?: "",
                    data.getString("CALLBACK") ?: "",
                    msg.obj ?: ""))
            }

            Connection.MESSAGE_INVALIDATEWINDOWTEXT ->
                eventLoop.send(ConnectionCommand.InvalidateWindowText(msg.obj as String))

            Connection.MESSAGE_WINDOWXCALLS -> {
                val o = msg.obj ?: ""
                val data = msg.data
                eventLoop.send(ConnectionCommand.WindowXCallS(
                    data.getString("TOKEN") ?: "",
                    data.getString("FUNCTION") ?: "",
                    o))
            }

            Connection.MESSAGE_WINDOWXCALLB -> {
                val data = msg.data
                eventLoop.send(ConnectionCommand.WindowXCallB(
                    data.getString("TOKEN") ?: "",
                    data.getString("FUNCTION") ?: "",
                    msg.obj as ByteArray))
            }

            Connection.MESSAGE_ADDFUNCTIONCALLBACK -> {
                val data = msg.data
                eventLoop.send(ConnectionCommand.AddFunctionCallback(
                    data.getString("ID") ?: "",
                    data.getString("COMMAND") ?: "",
                    data.getString("CALLBACK") ?: ""))
            }

            Connection.MESSAGE_WINDOWBUFFER ->
                eventLoop.send(ConnectionCommand.WindowBuffer(
                    msg.obj as String, msg.arg1 != 0))

            Connection.MESSAGE_NEWWINDOW ->
                eventLoop.send(ConnectionCommand.NewWindow(msg.obj))

            Connection.MESSAGE_DRAWINDOW ->
                eventLoop.send(ConnectionCommand.DrawWindow(msg.obj as String))

            Connection.MESSAGE_LUANOTE -> {
                val str = msg.obj as? String
                if (str != null) {
                    eventLoop.send(ConnectionCommand.LuaNote(str))
                }
            }

            Connection.MESSAGE_LINETOWINDOW -> {
                val data = msg.data
                eventLoop.send(ConnectionCommand.LineToWindow(
                    data.getString("TARGET") ?: "",
                    msg.obj))
            }

            // Special cases: these go through alias processing, not the dispatcher.
            Connection.MESSAGE_SENDDATA_STRING -> {
                val str = msg.obj as? String ?: return
                sendToServerString.send(str)
            }

            Connection.MESSAGE_SENDDATA_BYTES -> {
                val bytes = msg.obj as? ByteArray ?: return
                sendToServerBytes.send(bytes)
            }

            Connection.MESSAGE_SENDGMCPDATA -> {
                if (isPumpConnected.isConnected()) {
                    eventLoop.send(ConnectionCommand.SendGmcpData(msg.obj as String))
                } else {
                    eventLoop.sendDelayed(
                        ConnectionCommand.SendGmcpData(msg.obj as String),
                        GMCP_RETRY_DELAY_MS,
                    )
                }
            }

            Connection.MESSAGE_STARTUP ->
                eventLoop.send(ConnectionCommand.Startup)

            Connection.MESSAGE_STARTCOMPRESS ->
                eventLoop.send(ConnectionCommand.StartCompress(msg.obj as? ByteArray))

            Connection.MESSAGE_SENDOPTIONDATA -> {
                val b = msg.data
                eventLoop.send(ConnectionCommand.SendOptionData(
                    b.getByteArray("THE_DATA") ?: ByteArray(0),
                    b.getString("DEBUG_MESSAGE")))
            }

            Connection.MESSAGE_PROCESSORWARNING ->
                eventLoop.send(ConnectionCommand.ProcessorWarning(msg.obj as String))

            Connection.MESSAGE_BELLINC ->
                eventLoop.send(ConnectionCommand.BellReceived())

            Connection.MESSAGE_DODIALOG ->
                eventLoop.send(ConnectionCommand.DialogError(msg.obj as String))

            Connection.MESSAGE_PROCESS ->
                eventLoop.send(ConnectionCommand.Process(msg.obj as ByteArray))

            Connection.MESSAGE_DISCONNECTED ->
                eventLoop.send(ConnectionCommand.Disconnected)

            Connection.MESSAGE_DISABLE_LOCAL_ECHO ->
                eventLoop.send(ConnectionCommand.DisableLocalEcho)

            Connection.MESSAGE_ENABLE_LOCAL_ECHO ->
                eventLoop.send(ConnectionCommand.EnableLocalEcho)
        }
    }

    companion object {
        private const val GMCP_RETRY_DELAY_MS = 500L
    }
}
