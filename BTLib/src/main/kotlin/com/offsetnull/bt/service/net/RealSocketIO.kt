package com.offsetnull.bt.service.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

@Suppress("MagicNumber")
class RealSocketIO(private val host: String, private val port: Int) : SocketIO {
    private var socket: Socket? = null
    private var reader: BufferedInputStream? = null
    private var writer: BufferedOutputStream? = null

    fun connect(timeoutMs: Int = 14000) {
        val s = Socket()
        s.keepAlive = true
        s.soTimeout = 0
        s.connect(InetSocketAddress(host, port), timeoutMs)
        s.sendBufferSize = 1024
        socket = s
        reader = BufferedInputStream(s.getInputStream())
        writer = BufferedOutputStream(s.getOutputStream())
    }

    override suspend fun read(): ByteArray = withContext(Dispatchers.IO) {
        val r = reader ?: throw IOException("not connected")
        val available = r.available()
        if (available > 0) {
            val buf = ByteArray(available)
            val bytesRead = r.read(buf, 0, available)
            if (bytesRead == -1) return@withContext ByteArray(0)
            if (bytesRead < available) buf.copyOf(bytesRead) else buf
        } else {
            // Blocking read for a single byte to detect EOF
            val b = r.read()
            if (b == -1) {
                ByteArray(0) // EOF
            } else {
                // Got one byte, check if more available now
                val more = r.available()
                if (more > 0) {
                    val buf = ByteArray(more + 1)
                    buf[0] = b.toByte()
                    r.read(buf, 1, more)
                    buf
                } else {
                    byteArrayOf(b.toByte())
                }
            }
        }
    }

    override suspend fun write(data: ByteArray) = withContext(Dispatchers.IO) {
        val w = writer ?: throw IOException("not connected")
        w.write(data)
        w.flush()
    }

    override fun close() {
        runCatching { reader?.close() }
        runCatching { writer?.close() }
        runCatching { socket?.close() }
        socket = null
    }

    override val isConnected: Boolean
        get() = socket?.isConnected == true && socket?.isClosed == false
}
