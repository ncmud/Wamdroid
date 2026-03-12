package org.ncmud.mudwammer.service.net

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.zip.Deflater

class MccpDecompressorTest {

    private fun compress(input: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setInput(input)
        deflater.finish()
        val output = ByteArray(input.size + 64)
        val len = deflater.deflate(output)
        deflater.end()
        return output.copyOf(len)
    }

    @Test
    fun `decompresses valid zlib data`() {
        val decompressor = MccpDecompressor()
        val original = "Hello, MUD World!".toByteArray()
        val compressed = compress(original)

        val result = decompressor.decompress(compressed)

        assertArrayEquals(original, result?.data)
        assertNull(result?.remainder)
    }

    @Test
    fun `returns null on corrupt data and sets error flag`() {
        val decompressor = MccpDecompressor()
        val garbage = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val result = decompressor.decompress(garbage)

        assertNull(result)
        assertTrue(decompressor.isCorrupted)
    }

    @Test
    fun `starts clean`() {
        val decompressor = MccpDecompressor()
        assertFalse(decompressor.isCorrupted)
    }
}
