package com.swordfish.lemuroid.lib

import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.SaveKeyMigration
import com.swordfish.lemuroid.lib.saves.SaveKeys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class SaveKeyMigrationTest {
    private lateinit var root: File
    private lateinit var saves: File
    private lateinit var states: File
    private lateinit var previews: File

    @Before
    fun setUp() {
        root = File.createTempFile("savekeymig", "").also { it.delete(); it.mkdirs() }
        saves = File(root, "saves").also { it.mkdirs() }
        states = File(root, "states/gambatte").also { it.mkdirs() }.parentFile
        previews = File(root, "previews/gambatte").also { it.mkdirs() }.parentFile
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun game(
        id: Int,
        name: String,
        uri: String,
    ) = Game(id = id, fileName = name, fileUri = uri, title = name, systemId = "gb", developer = null, coverFrontUrl = null, lastIndexedAt = 0, saveKey = name)

    private fun run(games: List<Game>) = SaveKeyMigration.split(games, saves, states, previews)

    @Test
    fun twoGamesSharingAFileNameEndUpWithACopyEach() {
        File(saves, "Twin.srm").writeText("shared-battery")
        File(states, "gambatte/Twin.gb.state").writeText("shared-auto")
        File(states, "gambatte/Twin.gb.slot1").writeText("shared-slot1")
        File(states, "gambatte/Twin.gb.slot1.metadata").writeText("{}")
        File(previews, "gambatte/Twin.gb.slot1.jpg").writeText("pic")

        val first = game(3, "Twin.gb", "file:///a/Twin.gb")
        val second = game(4, "Twin.gb", "file:///b/Twin.gb")
        val changes = run(listOf(second, first))

        // Only the later game moves to a key of its own; the first keeps what it had.
        assertEquals(setOf(4), changes.keys)
        val key = changes.getValue(4)
        assertTrue(key.startsWith("Twin~") && key.endsWith(".gb"))

        // The first game's files are exactly as they were.
        assertEquals("shared-battery", File(saves, "Twin.srm").readText())
        assertEquals("shared-auto", File(states, "gambatte/Twin.gb.state").readText())
        // The second has a copy of each.
        assertEquals("shared-battery", File(saves, SaveKeys.srmName(key)).readText())
        assertEquals("shared-auto", File(states, "gambatte/" + SaveKeys.stateName(key)).readText())
        assertEquals("shared-slot1", File(states, "gambatte/" + SaveKeys.slotName(key, 0)).readText())
        assertEquals("{}", File(states, "gambatte/" + SaveKeys.slotName(key, 0) + ".metadata").readText())
        assertEquals("pic", File(previews, "gambatte/" + SaveKeys.previewName(key, 0)).readText())
    }

    @Test
    fun gbAndGbcOfOneNameStopSharingTheBatterySave() {
        File(saves, "Foo.srm").writeText("battery")
        val a = game(1, "Foo.gb", "file:///x/Foo.gb")
        val b = game(2, "Foo.gbc", "file:///x/Foo.gbc")
        val changes = run(listOf(a, b))
        val key = changes.getValue(2)
        assertTrue(key.endsWith(".gbc"))
        assertEquals("battery", File(saves, SaveKeys.srmName(key)).readText())
        assertEquals("battery", File(saves, "Foo.srm").readText())
    }

    @Test
    fun afterwardsNothingIsLeftToSplit() {
        File(saves, "Twin.srm").writeText("x")
        val games = listOf(game(1, "Twin.gb", "file:///a/Twin.gb"), game(2, "Twin.gb", "file:///b/Twin.gb"))
        val changes = run(games)
        val updated = games.map { if (it.id in changes) it.copy(saveKey = changes.getValue(it.id)) else it }
        assertTrue(run(updated).isEmpty())
    }

    @Test
    fun gamesWithDistinctNamesAreLeftAlone() {
        File(saves, "Alpha.srm").writeText("a")
        val changes = run(listOf(game(1, "Alpha.gb", "u1"), game(2, "Beta.gb", "u2")))
        assertTrue(changes.isEmpty())
        assertEquals(listOf("Alpha.srm"), saves.list()!!.toList())
    }

    @Test
    fun aFileAlreadyUnderTheNewKeyIsNeverReplaced() {
        val second = game(2, "Twin.gb", "file:///b/Twin.gb")
        val key = SaveKeys.assign(second.fileName, second.fileUri, listOf("Twin.gb"))
        File(saves, "Twin.srm").writeText("old")
        File(saves, SaveKeys.srmName(key)).writeText("already-here")
        run(listOf(game(1, "Twin.gb", "file:///a/Twin.gb"), second))
        assertEquals("already-here", File(saves, SaveKeys.srmName(key)).readText())
        assertFalse(File(saves, "Twin.srm").readText() != "old")
    }
}
