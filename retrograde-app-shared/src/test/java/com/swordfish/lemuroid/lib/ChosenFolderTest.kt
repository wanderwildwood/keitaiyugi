package com.swordfish.lemuroid.lib

import com.swordfish.lemuroid.lib.storage.local.ChosenFolder
import org.junit.Assert.assertEquals
import org.junit.Test

class ChosenFolderTest {
    @Test
    fun aFolderIsNamedByItsLastPart() {
        assertEquals("Game Boy", ChosenFolder.nameFromDocumentId("primary:Games/Game Boy"))
        assertEquals("Games", ChosenFolder.nameFromDocumentId("primary:Games"))
        assertEquals("Games", ChosenFolder.nameFromDocumentId("primary:Games/"))
    }

    @Test
    fun theTopOfACardIsNamedByTheCard() {
        assertEquals("1234-5678", ChosenFolder.nameFromDocumentId("1234-5678:"))
    }

    @Test
    fun anIdWithNoVolumeIsKept() {
        assertEquals("roms", ChosenFolder.nameFromDocumentId("roms"))
    }
}
