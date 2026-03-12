package org.ncmud.mudwammer.service

import java.io.UnsupportedEncodingException

/**
 * Dispatches [ConnectionCommand]s to the appropriate handlers.
 * Extracted from Connection.ConnectionHandler.handleMessage() for testability.
 */
class ConnectionDispatcher(
    private val triggerManager: TriggerManagerHandle? = null,
    private val pump: PumpHandle? = null,
    private val serviceCallbacks: BellCallbacks? = null,
    private val display: DisplayHandle? = null,
    private val lifecycle: LifecycleHandle? = null,
    private val windowManager: WindowManagerHandle? = null,
    private val pluginManager: PluginManagerHandle? = null,
    private val timerManager: TimerManagerHandle? = null,
    private val gmcpHandler: GmcpHandle? = null,
    private val aliasManager: AliasManagerHandle? = null,
    private val encoding: String = "UTF-8",
) {
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun dispatch(command: ConnectionCommand) {
        when (command) {
            // Lifecycle
            is ConnectionCommand.Startup -> lifecycle?.doStartup()
            is ConnectionCommand.Reconnect -> lifecycle?.doReconnect()
            is ConnectionCommand.Connected -> lifecycle?.resetAutoReconnect()
            is ConnectionCommand.Disconnected -> {
                lifecycle?.killNetThreads()
                lifecycle?.doDisconnect(byPeer = false)
            }
            is ConnectionCommand.TerminatedByPeer -> {
                lifecycle?.killNetThreads()
                lifecycle?.doDisconnect(byPeer = true)
            }

            // Network I/O
            is ConnectionCommand.Process -> triggerManager?.dispatch(command.data)
            is ConnectionCommand.SendDataString -> {
                try {
                    val bytes = command.text.toByteArray(charset(encoding))
                    pump?.send(bytes)
                } catch (_: UnsupportedEncodingException) {}
            }
            is ConnectionCommand.SendDataBytes -> pump?.send(command.data)
            is ConnectionCommand.SendGmcpData -> gmcpHandler?.sendData(command.data)
            is ConnectionCommand.StartCompress -> pump?.startCompression(command.trailingData)
            is ConnectionCommand.SendOptionData -> {
                if (command.debugMessage != null) {
                    display?.sendDataToWindow(command.debugMessage)
                }
                pump?.send(command.data)
            }

            // Display
            is ConnectionCommand.ProcessorWarning -> display?.sendDataToWindow(command.text)
            is ConnectionCommand.BellReceived -> {
                if (serviceCallbacks?.vibrateOnBell == true) serviceCallbacks.doVibrateBell()
                if (serviceCallbacks?.notifyOnBell == true) serviceCallbacks.doNotifyBell()
                if (serviceCallbacks?.displayOnBell == true) serviceCallbacks.doDisplayBell()
            }
            is ConnectionCommand.DialogError -> display?.dispatchDialog(command.message)
            is ConnectionCommand.MccpFatalError -> {
                // Handled at DataPumper level now
            }
            is ConnectionCommand.LuaNote -> {
                try {
                    display?.dispatchNoProcess(command.text.toByteArray(charset(encoding)))
                } catch (_: UnsupportedEncodingException) {}
            }
            is ConnectionCommand.LuaError -> display?.dispatchNoProcess(
                command.message.toByteArray(charset(encoding))
            )
            is ConnectionCommand.TriggerLuaError -> display?.dispatchNoProcess(
                command.message.toByteArray(charset(encoding))
            )

            // Window management
            is ConnectionCommand.LineToWindow ->
                windowManager?.lineToWindow(command.target, command.line)
            is ConnectionCommand.DrawWindow -> windowManager?.redrawWindow(command.name)
            is ConnectionCommand.NewWindow -> windowManager?.addWindow(command.token)
            is ConnectionCommand.WindowBuffer ->
                windowManager?.setWindowBuffer(command.name, command.enabled)
            is ConnectionCommand.WindowXCallS ->
                windowManager?.windowXCallS(command.token, command.function, command.data)
            is ConnectionCommand.WindowXCallB ->
                windowManager?.windowXCallB(command.token, command.function, command.data)
            is ConnectionCommand.InvalidateWindowText ->
                windowManager?.invalidateWindowText(command.name)

            // Plugin management
            is ConnectionCommand.AddFunctionCallback ->
                aliasManager?.addFunctionCallback(command.id, command.command, command.callback)
            is ConnectionCommand.GmcpTriggered ->
                gmcpHandler?.handleCallback(command.plugin, command.callback, command.data)
            is ConnectionCommand.CallPlugin ->
                pluginManager?.callPlugin(command.plugin, command.function, command.data)
            is ConnectionCommand.AddLink -> pluginManager?.addLink(command.path)
            is ConnectionCommand.DeletePlugin -> pluginManager?.deletePlugin(command.name)

            // Settings
            is ConnectionCommand.SaveDirtyPlugin ->
                pluginManager?.saveDirtyPlugin(command.name)
            is ConnectionCommand.ExportFile -> pluginManager?.exportSettings(command.path)
            is ConnectionCommand.ImportFile -> pluginManager?.importSettings(command.path)
            is ConnectionCommand.ReloadSettings -> lifecycle?.reloadSettings()
            is ConnectionCommand.ResetSettings -> pluginManager?.resetSettings()
            is ConnectionCommand.SetTriggersDirty -> triggerManager?.setDirty()

            // Echo negotiation
            is ConnectionCommand.DisableLocalEcho -> lifecycle?.setLocalEcho(false)
            is ConnectionCommand.EnableLocalEcho -> lifecycle?.setLocalEcho(true)

            // Timers
            is ConnectionCommand.TimerAction ->
                timerManager?.handleAction(command.name, command.id, command.action)
        }
    }
}

// ── Handle interfaces for testability ──────────────────────────────────

interface TriggerManagerHandle {
    fun dispatch(data: ByteArray)
    fun setDirty() {}
}

interface PumpHandle {
    fun send(data: ByteArray)
    fun startCompression(trailingData: ByteArray?)
}

interface BellCallbacks {
    val vibrateOnBell: Boolean
    val notifyOnBell: Boolean
    val displayOnBell: Boolean
    fun doVibrateBell()
    fun doNotifyBell()
    fun doDisplayBell()
}

interface DisplayHandle {
    fun sendDataToWindow(text: String)
    fun dispatchNoProcess(data: ByteArray)
    fun dispatchDialog(message: String)
}

interface LifecycleHandle {
    fun killNetThreads()
    fun doDisconnect(byPeer: Boolean)
    fun doReconnect()
    fun doStartup()
    fun resetAutoReconnect()
    fun reloadSettings()
    fun setLocalEcho(enabled: Boolean) {}
}

interface WindowManagerHandle {
    fun lineToWindow(target: String, line: Any)
    fun redrawWindow(name: String)
    fun addWindow(token: Any)
    fun setWindowBuffer(name: String, enabled: Boolean)
    fun windowXCallS(token: String, function: String, data: Any)
    fun windowXCallB(token: String, function: String, data: ByteArray)
    fun invalidateWindowText(name: String)
}

interface PluginManagerHandle {
    fun callPlugin(plugin: String, function: String, data: String)
    fun addLink(path: String)
    fun deletePlugin(name: String)
    fun saveDirtyPlugin(name: String)
    fun exportSettings(path: String)
    fun importSettings(path: String)
    fun resetSettings()
}

interface TimerManagerHandle {
    fun handleAction(name: String, id: Int, action: ConnectionCommand.TimerActionType)
}

interface GmcpHandle {
    fun sendData(data: String)
    fun handleCallback(plugin: String, callback: String, data: Any)
}

interface AliasManagerHandle {
    fun addFunctionCallback(id: String, command: String, callback: String)
}
