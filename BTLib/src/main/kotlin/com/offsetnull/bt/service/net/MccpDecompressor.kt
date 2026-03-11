package com.offsetnull.bt.service.net

import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * Handles MCCP (Mud Client Compression Protocol) zlib decompression.
 * Ported from DataPumper.doDecompress().
 */
class MccpDecompressor {
    private var inflater = Inflater()
    var isCorrupted: Boolean = false
        private set

    data class Result(val data: ByteArray, val remainder: ByteArray?)

    fun decompress(input: ByteArray): Result? {
        inflater.setInput(input, 0, input.size)
        val output = ByteArrayOutputStream()
        val buf = ByteArray(BUFFER_SIZE)

        while (!inflater.needsInput()) {
            val count: Int
            try {
                count = inflater.inflate(buf, 0, buf.size)
            } catch (_: DataFormatException) {
                isCorrupted = true
                inflater = Inflater()
                return null
            }

            if (inflater.finished()) {
                if (count > 0) output.write(buf, 0, count)
                val remaining = inflater.remaining
                val remainder = if (remaining > 0) {
                    val pos = input.size - remaining
                    input.copyOfRange(pos, input.size)
                } else null
                inflater = Inflater()
                val data = output.toByteArray()
                return if (data.isEmpty()) null else Result(data, remainder)
            }

            if (count > 0) output.write(buf, 0, count)
        }

        val data = output.toByteArray()
        return if (data.isEmpty()) null else Result(data, null)
    }

    fun reset() {
        inflater = Inflater()
        isCorrupted = false
    }

    companion object {
        private const val BUFFER_SIZE = 256
    }
}
