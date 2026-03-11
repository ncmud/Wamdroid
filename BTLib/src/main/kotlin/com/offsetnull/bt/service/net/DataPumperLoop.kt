package com.offsetnull.bt.service.net

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Coroutine-based replacement for [com.offsetnull.bt.service.DataPumper].
 *
 * Reads from [socket] in a loop, forwarding events via [onEvent].
 * Outbound data is queued via [send] and written on a separate coroutine.
 *
 * Cancel the coroutine running [run] to shut down.
 */
class DataPumperLoop(
    private val socket: SocketIO,
    private val onEvent: suspend (PumpEvent) -> Unit,
) {
    private val outgoing = Channel<ByteArray>(Channel.BUFFERED)
    private val decompressor = MccpDecompressor()
    private var compressed = false

    /** Queue bytes to be sent to the server. */
    fun send(data: ByteArray) {
        outgoing.trySend(data)
    }

    fun startCompression(trailingData: ByteArray?) {
        compressed = true
        decompressor.reset()
    }

    fun stopCompression() {
        compressed = false
    }

    /** Runs the read and write loops until cancelled or disconnected. */
    suspend fun run() = coroutineScope {
        launch { writeLoop() }
        readLoop()
        outgoing.close()
    }

    private suspend fun readLoop() {
        while (socket.isConnected) {
            val data: ByteArray
            try {
                data = socket.read()
            } catch (e: IOException) {
                onEvent(PumpEvent.Disconnected(e))
                return
            }
            if (data.isEmpty()) {
                onEvent(PumpEvent.DisconnectedByPeer)
                return
            }
            val processed = if (compressed) {
                val result = decompressor.decompress(data)
                if (result == null) {
                    if (decompressor.isCorrupted) {
                        onEvent(PumpEvent.MccpFatalError)
                        return
                    }
                    continue
                }
                result.data
            } else {
                data
            }
            onEvent(PumpEvent.DataReceived(processed))
        }
    }

    private suspend fun writeLoop() {
        for (data in outgoing) {
            try {
                socket.write(data)
            } catch (e: IOException) {
                onEvent(PumpEvent.Disconnected(e))
                return
            }
        }
    }
}
