package com.swordfish.lemuroid.lib

import com.swordfish.lemuroid.lib.storage.local.DocumentFileParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipGameEntryTest {
    /** A zip holding one stored file named [name], as a ROM zip holds its game. */
    private fun zipOf(name: String): ByteArray {
        val data = ByteArray(32_000) { (it * 7).toByte() }
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            val entry = ZipEntry(name)
            entry.method = ZipEntry.STORED
            entry.size = data.size.toLong()
            entry.compressedSize = data.size.toLong()
            entry.crc = CRC32().apply { update(data) }.value
            zip.putNextEntry(entry)
            zip.write(data)
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    private fun gameEntryName(name: String): String? {
        val bytes = zipOf(name)
        return ZipInputStream(ByteArrayInputStream(bytes)).use {
            DocumentFileParser.findGameEntry(it, bytes.size.toLong())?.name
        }
    }

    @Test
    fun aGameWithAPlainNameIsFound() {
        assertEquals("Tetris (World).gb", gameEntryName("Tetris (World).gb"))
    }

    @Test
    fun anEntryWhoseNameLeavesItsFolderIsNotAGame() {
        assertNull(gameEntryName("../../shared_prefs/x.xml"))
        assertNull(gameEntryName("Tetris/Tetris (World).gb"))
        assertNull(gameEntryName("..\\files\\x.gb"))
    }
}
