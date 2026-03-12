package com.offsetnull.bt.service.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Java-friendly callback for pump events. */
fun interface PumpEventListener {
    fun onEvent(event: PumpEvent)
}

/**
 * Bridges the Java-based [com.offsetnull.bt.service.DataPumper] to the
 * Kotlin coroutine-based [DataPumperLoop]. Hides coroutine API from Java.
 */
class DataPumperBridge(
    socket: SocketIO,
    listener: PumpEventListener,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val loop = DataPumperLoop(socket) { event ->
        listener.onEvent(event)
    }

    fun start() {
        scope.launch {
            loop.run()
        }
    }

    fun shutdown() {
        scope.cancel()
    }
}
