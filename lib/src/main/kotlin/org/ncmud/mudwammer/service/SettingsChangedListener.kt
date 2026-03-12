package org.ncmud.mudwammer.service

interface SettingsChangedListener {
    fun updateSetting(key: String, value: String)
}

/** Sealed event hierarchy for future Flow-based replacement of [SettingsChangedListener]. */
sealed interface SettingsEvent {
    data class SettingChanged(val key: String, val value: String) : SettingsEvent
}
