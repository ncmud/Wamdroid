package com.offsetnull.bt.service

import android.content.Context

@Suppress("TooManyFunctions")
interface ConnectionPluginCallback {
    fun setTriggersDirty()
    fun getWindowByName(name: String): WindowToken?
    fun isWindowShowing(): Boolean
    fun attatchWindowSettingsChangedListener(w: WindowToken)
    fun getStatusBarHeight(): Int
    fun getTitleBarHeight(): Int
    fun buildTriggerSystem()
    fun getDisplayName(): String
    fun getHostName(): String
    fun getPort(): Int
    fun getContext(): Context
    fun getSettingsListener(): SettingsChangedListener
    fun callPlugin(plugin: String, function: String, data: String)
    fun pluginSupports(plugin: String, function: String): Boolean
}

/** Sealed event hierarchy for future Flow-based replacement of [ConnectionPluginCallback]. */
sealed interface PluginEvent {
    data object SetTriggersDirty : PluginEvent
    data object BuildTriggerSystem : PluginEvent
    data class CallPlugin(
        val plugin: String,
        val function: String,
        val data: String,
    ) : PluginEvent
}
