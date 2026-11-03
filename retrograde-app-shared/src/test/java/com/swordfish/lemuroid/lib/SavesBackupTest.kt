package com.swordfish.lemuroid.lib

import com.swordfish.lemuroid.lib.saves.SavesBackup
import com.swordfish.lemuroid.lib.saves.SavesBackup.BackupException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

class SavesBackupTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun folders(root: File) =
        SavesBackup.Folders(File(root, "saves"), File(root, "states"), File(root, "state-previews")).also {
            it.saves.mkdirs()
            it.states.mkdirs()
            it.previews.mkdirs()
        }

    private val srm = Random(1).nextBytes(8192)
    private val state = Random(2).nextBytes(20_000)

    /** A phone with two games saved: a battery save, an auto-save state with its notes, a slot picture. */
    private fun phone(): SavesBackup.Folders {
        val f = folders(tmp.newFolder())
        File(f.saves, "Lighthouse Quest.srm").writeBytes(srm)
        File(f.states, "gambatte").mkdirs()
        File(f.states, "gambatte/Lighthouse Quest.gb.state").writeBytes(state)
        File(f.states, "gambatte/Lighthouse Quest.gb.state.metadata").writeText("""{"diskIndex":0}""")
        File(f.previews, "Tides.gb.slot1.jpg").writeBytes(byteArrayOf(1, 2, 3))
        // Left over from a write that was cut short: not a save, not backed up.
        File(f.saves, "Lighthouse Quest.srm.bak").writeBytes(byteArrayOf(9))
        return f
    }

    private fun backupOf(f: SavesBackup.Folders) = ByteArrayOutputStream().also { SavesBackup.write(it, f) }.toByteArray()

    private fun refused(
        kind: BackupException.Kind,
        staging: File,
        block: () -> Unit,
    ) {
        try {
            block()
            fail("expected $kind")
        } catch (e: BackupException) {
            assertEquals(kind, e.kind)
        }
        assertFalse("staging left behind", staging.exists() && staging.walkTopDown().any { it.isFile })
    }

    @Test
    fun savesComeBackOnAnotherPhone() {
        val zip = backupOf(phone())
        val other = folders(tmp.newFolder())
        File(other.saves, "Other Game.srm").writeBytes(byteArrayOf(5, 5))
        val staging = tmp.newFolder("staging")
        val staged = SavesBackup.stage(ByteArrayInputStream(zip), staging)
        assertEquals(4, staged.size)
        assertEquals(4, SavesBackup.apply(staging, other))
        assertArrayEquals(srm, File(other.saves, "Lighthouse Quest.srm").readBytes())
        assertArrayEquals(state, File(other.states, "gambatte/Lighthouse Quest.gb.state").readBytes())
        assertTrue(File(other.previews, "Tides.gb.slot1.jpg").exists())
        // A game the backup does not have keeps its save.
        assertArrayEquals(byteArrayOf(5, 5), File(other.saves, "Other Game.srm").readBytes())
        assertFalse(File(other.saves, "Lighthouse Quest.srm.bak").exists())
        assertFalse(staging.exists())
    }

    @Test
    fun aRestoredSaveIsNotUndoneByAStaleBackupFileBesideIt() {
        val zip = backupOf(phone())
        val other = folders(tmp.newFolder())
        File(other.saves, "Lighthouse Quest.srm").writeBytes(byteArrayOf(0))
        File(other.saves, "Lighthouse Quest.srm.bak").writeBytes(byteArrayOf(0))
        val staging = tmp.newFolder("staging")
        SavesBackup.stage(ByteArrayInputStream(zip), staging)
        SavesBackup.apply(staging, other)
        assertArrayEquals(srm, File(other.saves, "Lighthouse Quest.srm").readBytes())
        assertFalse(File(other.saves, "Lighthouse Quest.srm.bak").exists())
    }

    @Test
    fun aCutOffFileChangesNothing() {
        val zip = backupOf(phone())
        val staging = tmp.newFolder("staging")
        refused(BackupException.Kind.DAMAGED, staging) {
            SavesBackup.stage(ByteArrayInputStream(zip.copyOf(zip.size / 2)), staging)
        }
    }

    @Test
    fun aChangedByteIsCaught() {
        val zip = backupOf(phone())
        // Inside the stored state, which is random and so stored at full length.
        val i = zip.indexOfSlice(state.copyOfRange(5000, 5016))
        assertTrue(i > 0)
        zip[i] = (zip[i].toInt() xor 0x5a).toByte()
        val staging = tmp.newFolder("staging")
        refused(BackupException.Kind.DAMAGED, staging) { SavesBackup.stage(ByteArrayInputStream(zip), staging) }
    }

    @Test
    fun anotherZipIsNotABackup() {
        val other =
            ByteArrayOutputStream().also { out ->
                ZipOutputStream(out).use { z ->
                    z.putNextEntry(ZipEntry("Lighthouse Quest.gb"))
                    z.write(srm)
                    z.closeEntry()
                }
            }.toByteArray()
        val staging = tmp.newFolder("staging")
        refused(BackupException.Kind.NOT_A_BACKUP, staging) { SavesBackup.stage(ByteArrayInputStream(other), staging) }
        refused(BackupException.Kind.NOT_A_BACKUP, staging) {
            SavesBackup.stage(ByteArrayInputStream("not a zip".toByteArray()), staging)
        }
    }

    @Test
    fun aNewerBackupIsRefused() {
        val zip = zipWith("format=keitaiyugi-saves\nversion=2\n")
        val staging = tmp.newFolder("staging")
        refused(BackupException.Kind.NEWER, staging) { SavesBackup.stage(ByteArrayInputStream(zip), staging) }
    }

    @Test
    fun anEntryOutsideTheSaveFoldersIsRefused() {
        val zip = zipWith("format=keitaiyugi-saves\nversion=1\nfile=2\tsaves/../../evil\n", "saves/../../evil" to byteArrayOf(1, 2))
        val staging = tmp.newFolder("staging")
        refused(BackupException.Kind.DAMAGED, staging) { SavesBackup.stage(ByteArrayInputStream(zip), staging) }
        assertFalse(File(staging.parentFile, "evil").exists())
        assertFalse(SavesBackup.safe("roms/game.gb"))
        assertFalse(SavesBackup.safe("/saves/a.srm"))
        assertTrue(SavesBackup.safe("states/gambatte/a.gb.state"))
    }

    @Test
    fun anEntryTheListDoesNotNameIsRefused() {
        val zip = zipWith("format=keitaiyugi-saves\nversion=1\n", "saves/extra.srm" to byteArrayOf(1))
        val staging = tmp.newFolder("staging")
        refused(BackupException.Kind.DAMAGED, staging) { SavesBackup.stage(ByteArrayInputStream(zip), staging) }
    }

    private fun zipWith(
        manifest: String,
        vararg entries: Pair<String, ByteArray>,
    ): ByteArray =
        ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { z ->
                z.putNextEntry(ZipEntry(SavesBackup.MANIFEST))
                z.write(manifest.toByteArray())
                z.closeEntry()
                for ((name, bytes) in entries) {
                    z.putNextEntry(ZipEntry(name))
                    z.write(bytes)
                    z.closeEntry()
                }
            }
        }.toByteArray()

    private fun ByteArray.indexOfSlice(slice: ByteArray): Int =
        (0..size - slice.size).firstOrNull { i -> slice.indices.all { this[i + it] == slice[it] } } ?: -1
}
