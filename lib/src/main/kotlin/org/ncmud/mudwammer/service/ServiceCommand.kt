package org.ncmud.mudwammer.service

sealed interface ServiceCommand {
    data class NewConnection(val display: String, val host: String, val port: Int) : ServiceCommand
    data class SwitchConnection(val display: String) : ServiceCommand
    data object Startup : ServiceCommand
    data object ReloadSettings : ServiceCommand
}
