package com.swordfish.lemuroid.lib

import com.swordfish.lemuroid.lib.box.BoxNames
import com.swordfish.lemuroid.lib.box.SmbBoxShare
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

class BoxLimitsTest {
    @Test
    fun `names that reach outside their folder are not plain`() {
        assertTrue(BoxNames.isPlainName("Tetris (World).gb"))
        assertFalse(BoxNames.isPlainName(""))
        assertFalse(BoxNames.isPlainName("."))
        assertFalse(BoxNames.isPlainName(".."))
        assertFalse(BoxNames.isPlainName("../x.srm"))
        assertFalse(BoxNames.isPlainName("a\\b.gb"))
        assertFalse(BoxNames.isPlainName("a\u0000.gb"))
    }

    @Test
    fun `a read within the limit comes back whole`() {
        val bytes = ByteArray(100_000) { it.toByte() }
        assertArrayEquals(bytes, SmbBoxShare.readCapped(ByteArrayInputStream(bytes), 100_000))
    }

    @Test(expected = IOException::class)
    fun `a read past the limit is refused`() {
        SmbBoxShare.readCapped(ByteArrayInputStream(ByteArray(100_001)), 100_000)
    }
}
