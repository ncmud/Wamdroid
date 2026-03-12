package org.ncmud.mudwammer.service.net

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.zip.Deflater

class DataPumperLoopTest {

    private fun compress(input: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setInput(input)
        deflater.finish()
        val output = ByteArray(input.size + 64)
        val len = deflater.deflate(output)
        deflater.end()
        return output.copyOf(len)
    }

    /** Collects events emitted by [DataPumperLoop]. */
    private class Collector {
        val received = mutableListOf<ByteArray>()
        val events = mutableListOf<PumpEvent>()
    }

    private fun fakeSocket(vararg chunks: ByteArray): FakeSocketIO =
        FakeSocketIO(chunks.toMutableList())

    @Test
    fun `delivers incoming data to onData callback`() = runTest {
        val data = "Hello from MUD\n".toByteArray()
        val socket = fakeSocket(data)
        val collector = Collector()
        val loop = DataPumperLoop(socket) { event ->
            when (event) {
                is PumpEvent.DataReceived -> collector.received.add(event.data)
                else -> collector.events.add(event)
            }
        }

        val job = launch { loop.run() }
        advanceUntilIdle()
        job.cancel()

        assertEquals(1, collector.received.size)
        assertArrayEquals(data, collector.received[0])
    }

    @Test
    fun `emits Disconnected on EOF`() = runTest {
        val socket = fakeSocket() // no data, returns EOF immediately
        val collector = Collector()
        val loop = DataPumperLoop(socket) { event ->
            when (event) {
                is PumpEvent.DataReceived -> collector.received.add(event.data)
                else -> collector.events.add(event)
            }
        }

        val job = launch { loop.run() }
        advanceUntilIdle()
        job.cancel()

        assertTrue(collector.events.any { it is PumpEvent.DisconnectedByPeer })
    }

    @Test
    fun `emits Disconnected on IOException`() = runTest {
        val socket = ErrorSocketIO()
        val collector = Collector()
        val loop = DataPumperLoop(socket) { event ->
            when (event) {
                is PumpEvent.DataReceived -> collector.received.add(event.data)
                else -> collector.events.add(event)
            }
        }

        val job = launch { loop.run() }
        advanceUntilIdle()
        job.cancel()

        assertTrue(collector.events.any { it is PumpEvent.Disconnected })
    }

    @Test
    fun `write sends data through socket`() = runTest {
        val socket = fakeSocket()
        val loop = DataPumperLoop(socket) { }

        val job = launch { loop.run() }
        loop.send("test\n".toByteArray())
        advanceUntilIdle()
        job.cancel()

        assertEquals(1, socket.written.size)
        assertArrayEquals("test\n".toByteArray(), socket.written[0])
    }

    @Test
    fun `startCompression enables decompression of subsequent data`() = runTest {
        val original = "compressed data from server".toByteArray()
        val compressed = compress(original)
        val socket = fakeSocket(compressed)
        val collector = Collector()
        val loop = DataPumperLoop(socket) { event ->
            when (event) {
                is PumpEvent.DataReceived -> collector.received.add(event.data)
                else -> collector.events.add(event)
            }
        }

        loop.startCompression(null)

        val job = launch { loop.run() }
        advanceUntilIdle()
        job.cancel()

        assertEquals(1, collector.received.size)
        assertArrayEquals(original, collector.received[0])
    }
}

/** Fake that yields pre-loaded chunks then signals EOF. */
private class FakeSocketIO(
    private val chunks: MutableList<ByteArray> = mutableListOf(),
) : SocketIO {
    val written = mutableListOf<ByteArray>()
    private var open = true

    override suspend fun read(): ByteArray {
        if (chunks.isEmpty()) {
            open = false
            return ByteArray(0)
        }
        return chunks.removeFirst()
    }

    override suspend fun write(data: ByteArray) {
        written.add(data.copyOf())
    }

    override fun close() { open = false }
    override val isConnected: Boolean get() = open
}

/** Fake that throws IOException on first read. */
private class ErrorSocketIO : SocketIO {
    override suspend fun read(): ByteArray = throw IOException("connection reset")
    override suspend fun write(data: ByteArray) = throw IOException("broken pipe")
    override fun close() {}
    override val isConnected: Boolean = true
}
