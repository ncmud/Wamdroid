package com.offsetnull.bt.service

import java.io.UnsupportedEncodingException

internal class TriggerManagerAdapter(private val conn: Connection) : TriggerManagerHandle {
    override fun dispatch(data: ByteArray) {
        try {
            conn.mTriggerManager.dispatch(data)
        } catch (_: UnsupportedEncodingException) {}
    }

    override fun setDirty() {
        conn.mTriggerManager.setDirty()
    }
}

internal class PumpAdapter(private val conn: Connection) : PumpHandle {
    override fun send(data: ByteArray) {
        conn.mPump?.sendData(data)
    }

    override fun startCompression(trailingData: ByteArray?) {
        conn.mPump?.startCompression(trailingData)
    }
}

internal class BellCallbacksAdapter(private val conn: Connection) : BellCallbacks {
    override val vibrateOnBell: Boolean get() = conn.mSettings.isVibrateOnBell
    override val notifyOnBell: Boolean get() = conn.mSettings.isNotifyOnBell
    override val displayOnBell: Boolean get() = conn.mSettings.isDisplayOnBell

    override fun doVibrateBell() {
        conn.mService.doVibrateBell()
    }

    override fun doNotifyBell() {
        conn.mService.doNotifyBell(conn.mDisplay, conn.mHost, conn.mPort)
    }

    override fun doDisplayBell() {
        conn.mService.doDisplayBell()
    }
}

internal class DisplayAdapter(private val conn: Connection) : DisplayHandle {
    override fun sendDataToWindow(text: String) {
        conn.sendDataToWindow(text)
    }

    override fun dispatchNoProcess(data: ByteArray) {
        conn.dispatchNoProcess(data)
    }

    override fun dispatchDialog(message: String) {
        conn.dispatchDialog(message)
    }
}

internal class LifecycleAdapter(private val conn: Connection) : LifecycleHandle {
    override fun killNetThreads() {
        conn.killNetThreads(true)
    }

    override fun doDisconnect(byPeer: Boolean) {
        conn.doDisconnect(byPeer)
        conn.mIsConnected = false
    }

    override fun doReconnect() {
        conn.doReconnect()
    }

    override fun doStartup() {
        conn.startup()
    }

    override fun resetAutoReconnect() {
        conn.mAutoReconnectAttempt = 0
    }

    override fun reloadSettings() {
        conn.reloadSettings()
    }

    override fun setLocalEcho(enabled: Boolean) {
        conn.mSettings.isLocalEcho = enabled
    }
}

internal class WindowManagerAdapter(private val conn: Connection) : WindowManagerHandle {
    override fun lineToWindow(target: String, line: Any) {
        conn.lineToWindow(target, line)
    }

    override fun redrawWindow(name: String) {
        conn.redrawWindow(name)
    }

    override fun addWindow(token: Any) {
        conn.mWindowManager.windows.add(token as WindowToken)
    }

    override fun setWindowBuffer(name: String, enabled: Boolean) {
        for (tok in conn.mWindowManager.windows) {
            if (tok.name == name) {
                tok.setBufferText(enabled)
            }
        }
    }

    override fun windowXCallS(token: String, function: String, data: Any) {
        conn.windowXCallS(token, function, data)
    }

    override fun windowXCallB(token: String, function: String, data: ByteArray) {
        conn.windowXCallB(token, function, data)
    }

    override fun invalidateWindowText(name: String) {
        conn.doInvalidateWindowText(name)
    }
}

@Suppress("UNCHECKED_CAST")
internal class PluginManagerAdapter(private val conn: Connection) : PluginManagerHandle {
    override fun callPlugin(plugin: String, function: String, data: String) {
        conn.doCallPlugin(plugin, function, data)
    }

    override fun addLink(path: String) {
        conn.doAddLink(path)
    }

    override fun deletePlugin(name: String) {
        conn.doDeletePlugin(name)
    }

    override fun saveDirtyPlugin(name: String) {
        conn.saveDirtyPlugin(name)
    }

    override fun exportSettings(path: String) {
        conn.exportSettings(path)
    }

    override fun importSettings(path: String) {
        conn.mService.markWindowsDirty()
        conn.importSettings(path, true, false)
    }

    override fun resetSettings() {
        conn.doResetSettings()
    }
}

internal class TimerManagerAdapter(private val conn: Connection) : TimerManagerHandle {
    override fun handleAction(name: String, id: Int, action: ConnectionCommand.TimerActionType) {
        val timerAction = when (action) {
            ConnectionCommand.TimerActionType.START -> TimerManager.TimerAction.PLAY
            ConnectionCommand.TimerActionType.STOP -> TimerManager.TimerAction.STOP
            ConnectionCommand.TimerActionType.PAUSE -> TimerManager.TimerAction.PAUSE
            ConnectionCommand.TimerActionType.RESET -> TimerManager.TimerAction.RESET
            ConnectionCommand.TimerActionType.INFO -> TimerManager.TimerAction.INFO
        }
        conn.mTimerManager.handleAction(name, id, timerAction)
    }
}

@Suppress("UNCHECKED_CAST")
internal class GmcpAdapter(private val conn: Connection) : GmcpHandle {
    override fun sendData(data: String) {
        conn.mGMCPHandler.sendData(data)
    }

    override fun handleCallback(plugin: String, callback: String, data: Any) {
        conn.mGMCPHandler.handleCallback(
            plugin, callback, data as java.util.HashMap<String, Any>
        )
    }
}

internal class AliasManagerAdapter(private val conn: Connection) : AliasManagerHandle {
    override fun addFunctionCallback(id: String, command: String, callback: String) {
        conn.addFunctionCallbackImpl(id, command, callback)
    }
}
