package com.offsetnull.bt.service

@Suppress("TooManyFunctions")
interface ConnectionCallback {
    fun isWindowShowing(): Boolean
    fun dataIncoming(seq: ByteArray)
    fun processedDataIncoming(seq: CharSequence)
    fun htmlDataIncoming(html: String)
    fun rawDataIncoming(raw: ByteArray)
    fun rawBufferIncoming(incoming: ByteArray)
    fun loadSettings()
    fun displayXMLError(error: String)
    fun displaySaveError(error: String)
    fun displayPluginSaveError(plugin: String, error: String)
    fun executeColorDebug(arg: Int)
    fun invokeDirtyExit()
    fun showMessage(message: String, longtime: Boolean)
    fun showDialog(message: String)
    fun doVisualBell()
    fun setScreenMode(fullscreen: Boolean)
    fun showKeyBoard(
        txt: String,
        popup: Boolean,
        add: Boolean,
        flush: Boolean,
        clear: Boolean,
        close: Boolean,
    )
    fun doDisconnectNotice(display: String)
    fun doLineBreak(i: Int)
    fun reloadButtons(setName: String)
    fun clearAllButtons()
    fun updateMaxVitals(hp: Int, mana: Int, moves: Int)
    fun updateVitals(hp: Int, mana: Int, moves: Int)
    fun updateEnemy(hp: Int)
    fun updateVitals2(hp: Int, mp: Int, maxhp: Int, maxmana: Int, enemy: Int)
    fun luaOmg(stateIndex: Int)
    fun updateTriggerDebugString(str: String)
    fun getPort(): Int
    fun getHost(): String
    fun getDisplay(): String
    fun switchTo(connection: String)
    fun reloadBuffer()
    fun loadWindowSettings()
    fun markWindowsDirty()
    fun markSettingsDirty()
    fun setKeepLast(keep: Boolean)
    fun setOrientation(orientation: Int)
    fun setKeepScreenOn(value: Boolean)
    fun setUseFullscreenEditor(value: Boolean)
    fun setUseSuggestions(value: Boolean)
    fun setCompatibilityMode(value: Boolean)
    fun setRegexWarning(value: Boolean)
}

/** Sealed event hierarchy for future Flow-based replacement of [ConnectionCallback]. */
sealed interface ConnectionEvent {
    data class DataIncoming(val seq: ByteArray) : ConnectionEvent
    data class ProcessedDataIncoming(val seq: CharSequence) : ConnectionEvent
    data class HtmlDataIncoming(val html: String) : ConnectionEvent
    data class RawDataIncoming(val raw: ByteArray) : ConnectionEvent
    data class RawBufferIncoming(val incoming: ByteArray) : ConnectionEvent
    data object LoadSettings : ConnectionEvent
    data class DisplayXMLError(val error: String) : ConnectionEvent
    data class DisplaySaveError(val error: String) : ConnectionEvent
    data class DisplayPluginSaveError(val plugin: String, val error: String) : ConnectionEvent
    data class ExecuteColorDebug(val arg: Int) : ConnectionEvent
    data object InvokeDirtyExit : ConnectionEvent
    data class ShowMessage(val message: String, val longtime: Boolean) : ConnectionEvent
    data class ShowDialog(val message: String) : ConnectionEvent
    data object DoVisualBell : ConnectionEvent
    data class SetScreenMode(val fullscreen: Boolean) : ConnectionEvent
    data class ShowKeyBoard(
        val txt: String,
        val popup: Boolean,
        val add: Boolean,
        val flush: Boolean,
        val clear: Boolean,
        val close: Boolean,
    ) : ConnectionEvent
    data class DoDisconnectNotice(val display: String) : ConnectionEvent
    data class DoLineBreak(val i: Int) : ConnectionEvent
    data class ReloadButtons(val setName: String) : ConnectionEvent
    data object ClearAllButtons : ConnectionEvent
    data class UpdateMaxVitals(val hp: Int, val mana: Int, val moves: Int) : ConnectionEvent
    data class UpdateVitals(val hp: Int, val mana: Int, val moves: Int) : ConnectionEvent
    data class UpdateEnemy(val hp: Int) : ConnectionEvent
    data class UpdateVitals2(
        val hp: Int,
        val mp: Int,
        val maxhp: Int,
        val maxmana: Int,
        val enemy: Int,
    ) : ConnectionEvent
    data class LuaOmg(val stateIndex: Int) : ConnectionEvent
    data class UpdateTriggerDebugString(val str: String) : ConnectionEvent
    data class SwitchTo(val connection: String) : ConnectionEvent
    data object ReloadBuffer : ConnectionEvent
    data object LoadWindowSettings : ConnectionEvent
    data object MarkWindowsDirty : ConnectionEvent
    data object MarkSettingsDirty : ConnectionEvent
    data class SetKeepLast(val keep: Boolean) : ConnectionEvent
    data class SetOrientation(val orientation: Int) : ConnectionEvent
    data class SetKeepScreenOn(val value: Boolean) : ConnectionEvent
    data class SetUseFullscreenEditor(val value: Boolean) : ConnectionEvent
    data class SetUseSuggestions(val value: Boolean) : ConnectionEvent
    data class SetCompatibilityMode(val value: Boolean) : ConnectionEvent
    data class SetRegexWarning(val value: Boolean) : ConnectionEvent
}
