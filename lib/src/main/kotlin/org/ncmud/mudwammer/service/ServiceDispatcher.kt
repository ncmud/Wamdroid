package org.ncmud.mudwammer.service

class ServiceDispatcher(
    private val connectionFactory: (String, String, Int) -> ConnectionHandle = { _, _, _ ->
        throw IllegalStateException("No connection factory configured")
    },
) {
    val connections = mutableMapOf<String, ConnectionHandle>()
    var activeConnection: String? = null
        private set

    fun dispatch(command: ServiceCommand) {
        when (command) {
            is ServiceCommand.NewConnection -> {
                if (command.display !in connections) {
                    val conn = connectionFactory(command.display, command.host, command.port)
                    connections[command.display] = conn
                    activeConnection = command.display
                    conn.initWindows()
                }
            }
            is ServiceCommand.SwitchConnection -> {
                activeConnection = command.display
            }
            is ServiceCommand.Startup -> {
                val conn = connections[activeConnection] ?: return
                conn.startup()
            }
            is ServiceCommand.ReloadSettings -> {
                val conn = connections[activeConnection] ?: return
                conn.reloadSettings()
            }
        }
    }
}

/** Minimal interface that ServiceDispatcher needs from a Connection. */
interface ConnectionHandle {
    fun startup()
    fun reloadSettings()
    fun initWindows()
}
