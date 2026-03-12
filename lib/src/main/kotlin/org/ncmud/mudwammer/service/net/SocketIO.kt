package org.ncmud.mudwammer.service.net

/**
 * Abstraction over TCP socket I/O so that [DataPumperLoop] can be tested
 * without real network connections.
 */
interface SocketIO {
    /** Reads available bytes. Returns empty array if nothing available. Throws on error. */
    suspend fun read(): ByteArray

    /** Writes bytes to the socket. Throws on error. */
    suspend fun write(data: ByteArray)

    /** Closes the connection. Safe to call multiple times. */
    fun close()

    /** True if the connection is open. */
    val isConnected: Boolean
}
