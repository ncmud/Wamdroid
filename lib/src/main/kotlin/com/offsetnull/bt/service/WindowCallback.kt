package com.offsetnull.bt.service

@Suppress("TooManyFunctions")
interface WindowCallback {
    fun isWindowShowing(): Boolean
    fun rawDataIncoming(raw: ByteArray)
    fun resetWithRawDataIncoming(raw: ByteArray)
    fun redraw()
    fun getName(): String
    fun shutdown()
    fun xcallS(function: String, str: String)
    fun xcallB(function: String, raw: ByteArray)
    fun clearText()
    fun updateSetting(key: String, value: String)
    fun setEncoding(value: String)
}

/** Sealed event hierarchy for future Flow-based replacement of [WindowCallback]. */
sealed interface WindowEvent {
    data class RawDataIncoming(val raw: ByteArray) : WindowEvent
    data class ResetWithRawDataIncoming(val raw: ByteArray) : WindowEvent
    data object Redraw : WindowEvent
    data object Shutdown : WindowEvent
    data class XcallS(val function: String, val str: String) : WindowEvent
    data class XcallB(val function: String, val raw: ByteArray) : WindowEvent
    data object ClearText : WindowEvent
    data class UpdateSetting(val key: String, val value: String) : WindowEvent
    data class SetEncoding(val value: String) : WindowEvent
}
