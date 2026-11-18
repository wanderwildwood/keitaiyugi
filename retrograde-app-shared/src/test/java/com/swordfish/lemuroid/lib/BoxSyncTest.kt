package com.swordfish.lemuroid.lib

import com.swordfish.lemuroid.lib.box.BoxEntry
import com.swordfish.lemuroid.lib.box.BoxFileTooLargeException
import com.swordfish.lemuroid.lib.box.BoxKey
import com.swordfish.lemuroid.lib.box.BoxNames
import com.swordfish.lemuroid.lib.box.BoxShare
import com.swordfish.lemuroid.lib.box.BoxSync
import com.swordfish.lemuroid.lib.box.ChtFile
import com.swordfish.lemuroid.lib.box.SaveAction
import com.swordfish.lemuroid.lib.box.SaveMerge
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class BoxSyncTest {
    /** A box held in memory. Every write moves its clock on, as a real one's would. */
    private class FakeShare : BoxShare {
        val files = HashMap<String, Pair<ByteArray, Long>>()
        var clock = 1_000_000L
        var reads = 0

        /** Files that read as larger than the share lets through. */
        val tooLarge = HashSet<String>()

        fun put(
            path: String,
            data: String,
            modified: Long = ++clock,
        ) {
            files[path] = data.toByteArray() to modified
        }

        fun text(path: String) = files[path]?.first?.toString(Charsets.UTF_8)

        override fun list(folder: String): List<BoxEntry> =
            files.filterKeys { it.substringBeforeLast('/') == folder }
                .map { (path, file) ->
                    BoxEntry(path.substringAfterLast('/'), false, file.first.size.toLong(), file.second)
                }

        override fun read(path: String): ByteArray {
            reads++
            if (path in tooLarge) throw BoxFileTooLargeException(path)
            return files.getValue(path).first
        }

        override fun download(
            path: String,
            to: File,
        ) = to.writeBytes(read(path))

        override fun write(
            path: String,
            data: ByteArray,
        ) {
            files[path] = data to ++clock
        }

        override fun move(
            from: String,
            to: String,
        ) {
            files[to] = files.remove(from)!!
        }

        override fun close() = Unit
    }

    private lateinit var dir: File
    private lateinit var share: FakeShare
    private lateinit var sync: BoxSync
    private var time = 1_700_000_000_000L

    private val roms get() = File(dir, "roms/box")
    private val saves get() = File(dir, "saves")
    private val states get() = File(dir, "states")
    private val cheats get() = File(dir, "cheats")
    private val replaced get() = File(dir, "replaced")

    @Before
    fun setUp() {
        dir =
            File.createTempFile("boxsync", "").also {
                it.delete()
                it.mkdirs()
            }
        share = FakeShare()
        sync = BoxSync(File(dir, "state"), roms, saves, states, cheats, replaced) { time }
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    /** A game as the library lists one copied from the box: the zip's address, the ROM's name. */
    private fun boxGame(
        system: String,
        zip: String,
        id: Int = 1,
    ): Game {
        val file = File(File(roms, system), zip)
        val uri = "file://" + file.path.replace(" ", "%20")
        return Game(
            id = id,
            fileName = BoxNames.stem(zip) + ".$system",
            fileUri = uri,
            title = zip,
            systemId = system,
            developer = null,
            coverFrontUrl = null,
            lastIndexedAt = 0,
        )
    }

    private fun localSave(
        name: String,
        data: String,
        modified: Long,
    ) {
        saves.mkdirs()
        File(saves, name).apply {
            writeText(data)
            setLastModified(modified)
        }
    }

    // ---- which way a save goes ----

    @Test
    fun aSaveOnOneSideOnlyIsCopiedToTheOther() {
        val side = SaveMerge.Side("a", 5)
        assertEquals(SaveAction.PULL, SaveMerge.decide(null, side, null))
        assertEquals(SaveAction.PUSH, SaveMerge.decide(side, null, null))
        assertEquals(SaveAction.NOTHING, SaveMerge.decide(null, null, null))
    }

    @Test
    fun theSideThatWasNotPlayedTakesTheOthersSaveWhateverTheDates() {
        val old = SaveMerge.Side("agreed", 900)
        val played = SaveMerge.Side("new", 100)
        assertEquals(SaveAction.PULL, SaveMerge.decide(old, played, "agreed"))
        assertEquals(SaveAction.PUSH, SaveMerge.decide(played, old, "agreed"))
    }

    @Test
    fun whenBothWerePlayedTheNewerFileWins() {
        val a = SaveMerge.Side("a", 100)
        val b = SaveMerge.Side("b", 200)
        assertEquals(SaveAction.PULL, SaveMerge.decide(a, b, "agreed"))
        assertEquals(SaveAction.PUSH, SaveMerge.decide(b, a, "agreed"))
        assertEquals(SaveAction.PULL, SaveMerge.decide(a, b, null))
        assertEquals(SaveAction.NOTHING, SaveMerge.decide(a, a.copy(modified = 999), null))
    }

    // ---- games ----

    @Test
    fun gamesAreCopiedOnceAndOnlyGames() {
        share.put("roms/gb/Tetris (World).zip", "tetris")
        share.put("roms/gbc/Wario Land 3 (World).zip", "wario")
        share.put("roms/gb/gamelist.xml", "<x/>")
        share.put("roms/gb/_info.txt", "x")

        val first = sync.syncGames(share, emptyList())
        assertEquals(BoxSync.GamesResult(copied = 2, onBox = 2), first)
        assertEquals("tetris", File(roms, "gb/Tetris (World).zip").readText())
        assertEquals("wario", File(roms, "gbc/Wario Land 3 (World).zip").readText())
        assertFalse(File(roms, "gb/gamelist.xml").exists())

        assertEquals(0, sync.syncGames(share, emptyList()).copied)

        share.put("roms/gb/Kirby's Dream Land (USA).zip", "kirby")
        assertEquals(1, sync.syncGames(share, emptyList()).copied)
    }

    @Test
    fun aFileTooLargeOnTheBoxIsPassedOverAndTheSyncGoesOn() {
        share.put("roms/gb/Huge (World).zip", "huge")
        share.tooLarge += "roms/gb/Huge (World).zip"
        share.put("roms/gb/Tetris (World).zip", "tetris")
        share.put("${BoxNames.CHEATS_FOLDER}/Tetris (World) (Game Genie).cht", "huge cheats")
        share.tooLarge += "${BoxNames.CHEATS_FOLDER}/Tetris (World) (Game Genie).cht"
        share.put("${BoxNames.CHEATS_FOLDER}/Tetris (World).cht", "cheats")

        assertEquals(1, sync.syncGames(share, emptyList()).copied)
        assertEquals("tetris", File(roms, "gb/Tetris (World).zip").readText())
        assertEquals(listOf("Tetris (World).zip"), File(roms, "gb").list()!!.toList())
        assertEquals(1, sync.syncCheats(share))
        assertEquals(listOf("Tetris (World).cht"), cheats.list()!!.toList())

        val game = boxGame("gb", "Tetris (World).zip")
        share.put("saves/gb/Tetris (World).srm", "box save")
        assertEquals(1, sync.syncSaves(share, listOf(game)).pulled)
    }

    @Test
    fun aGameReplacedOnTheBoxIsCopiedAgain() {
        share.put("roms/gb/Tetris (World).zip", "tetris")
        sync.syncGames(share, emptyList())
        share.put("roms/gb/Tetris (World).zip", "TETRIS")
        assertEquals(1, sync.syncGames(share, emptyList()).copied)
        assertEquals("TETRIS", File(roms, "gb/Tetris (World).zip").readText())
    }

    @Test
    fun twoBoxGamesOfOneNameAreNotBothTakenSoTheyNeverShareASave() {
        share.put("roms/gb/Tetris (World).zip", "zipped")
        share.put("roms/gb/Tetris (World).gb", "plain")

        assertEquals(BoxSync.GamesResult(copied = 1, onBox = 2, sameName = 1), sync.syncGames(share, emptyList()))
        assertEquals(listOf("Tetris (World).gb"), File(roms, "gb").list()!!.toList())
        assertEquals(BoxKey("gb", "Tetris (World)"), sync.keyFor(boxGame("gb", "Tetris (World).gb")))
        // A copy of the other one, made before, keeps its save on the phone and sends nothing.
        assertNull(sync.keyFor(boxGame("gb", "Tetris (World).zip", id = 2)))

        assertEquals(0, sync.syncGames(share, emptyList()).copied)
    }

    @Test
    fun aGameThePhoneAlreadyHasIsPairedNotCopied() {
        share.put("roms/gb/Tetris (World).zip", "tetris")
        val own =
            Game(
                id = 7,
                fileName = "Tetris (World).gb",
                fileUri =
                    "content://com.android.externalstorage.documents/tree/primary%3AGames/document/" +
                        "primary%3AGames%2Ftetris%20(world).zip",
                title = "Tetris",
                systemId = "gb",
                developer = null,
                coverFrontUrl = null,
                lastIndexedAt = 0,
            )
        assertEquals(0, sync.syncGames(share, listOf(own)).copied)
        assertFalse(File(roms, "gb/Tetris (World).zip").exists())
        assertEquals(BoxKey("gb", "Tetris (World)"), sync.keyFor(own))

        // The same name on the other system is another game.
        assertNull(sync.keyFor(own.copy(id = 8, fileUri = "content://x/other.gbc", systemId = "gbc")))
    }

    @Test
    fun anOwnGameOfTheNameOfABoxCopyIsLeftUnpaired() {
        share.put("roms/gb/Tetris (World).zip", "tetris")
        sync.syncGames(share, emptyList())
        val copy = boxGame("gb", "Tetris (World).zip")
        val own =
            Game(
                id = 7,
                fileName = "Tetris (World).gb",
                fileUri = "content://com.android.externalstorage.documents/tree/primary%3AGames/document/primary%3ATetris%20(World).gb",
                title = "Tetris",
                systemId = "gb",
                developer = null,
                coverFrontUrl = null,
                lastIndexedAt = 0,
            )
        sync.syncGames(share, listOf(copy, own))
        assertEquals(BoxKey("gb", "Tetris (World)"), sync.keyFor(copy))
        assertNull(sync.keyFor(own))
    }

    @Test
    fun aCopiedGameIsKnownByItsFolderAndFileName() {
        assertEquals(BoxKey("gbc", "Wario Land 3 (World)"), sync.keyFor(boxGame("gbc", "Wario Land 3 (World).zip")))
    }

    // ---- saves ----

    @Test
    fun theBoxsSaveComesDownAndTheNextSyncReadsNothing() {
        val game = boxGame("gb", "Tetris (World).zip")
        share.put("saves/gb/Tetris (World).srm", "box save")

        assertEquals(SaveAction.PULL, sync.syncSave(share, game))
        assertEquals("box save", File(saves, "Tetris (World).srm").readText())

        val readsBefore = share.reads
        assertEquals(SaveAction.NOTHING, sync.syncSave(share, game))
        assertEquals(readsBefore, share.reads)
    }

    @Test
    fun playingOnThePhoneSendsTheSaveAndSetsTheBoxsAutoStateAside() {
        val game = boxGame("gb", "Tetris (World).zip")
        share.put("saves/gb/Tetris (World).srm", "box save")
        share.put("saves/gb/Tetris (World).state.auto", "state")
        share.put("saves/gb/Tetris (World).state.auto.png", "png")
        share.put("saves/gb/Other Game.state.auto", "other")
        sync.syncSave(share, game)

        localSave("Tetris (World).srm", "phone save", 5)
        assertEquals(SaveAction.PUSH, sync.syncSave(share, game))

        assertEquals("phone save", share.text("saves/gb/Tetris (World).srm"))
        assertTrue(share.files.keys.toString(), share.files.keys.any { it.startsWith("saves-sync-backup/") })
        val folder = share.files.keys.first { it.startsWith("saves-sync-backup/") }.substringBeforeLast('/')
        assertTrue(folder.endsWith("-phone/gb"))
        assertEquals("box save", share.text("$folder/Tetris (World).srm"))
        assertEquals("state", share.text("$folder/Tetris (World).state.auto"))
        assertEquals("png", share.text("$folder/Tetris (World).state.auto.png"))
        assertEquals("other", share.text("saves/gb/Other Game.state.auto"))

        assertEquals(SaveAction.NOTHING, sync.syncSave(share, game))
    }

    @Test
    fun theSaveReplacedOnThePhoneIsKept() {
        val game = boxGame("gb", "Tetris (World).zip")
        localSave("Tetris (World).srm", "old phone save", 5)
        share.put("saves/gb/Tetris (World).srm", "newer box save", modified = 10)

        assertEquals(SaveAction.PULL, sync.syncSave(share, game))
        assertEquals("newer box save", File(saves, "Tetris (World).srm").readText())
        val kept = replaced.walkTopDown().filter { it.isFile }.toList()
        assertEquals(1, kept.size)
        assertEquals("old phone save", kept.single().readText())
    }

    @Test
    fun aSaveThatComesInSetsThePhonesAutoStateAsideAndLeavesTheSlots() {
        val game = boxGame("gb", "Tetris (World).zip")
        localSave("Tetris (World).srm", "old phone save", 5)
        val core = File(states, "gambatte").apply { mkdirs() }
        File(core, "Tetris (World).gb.state").writeText("auto")
        File(core, "Tetris (World).gb.state.metadata").writeText("meta")
        File(core, "Tetris (World).gb.slot1").writeText("slot")
        File(core, "Other.gb.state").writeText("other")
        share.put("saves/gb/Tetris (World).srm", "newer box save", modified = 10)

        assertEquals(SaveAction.PULL, sync.syncSave(share, game))

        assertFalse(File(core, "Tetris (World).gb.state").exists())
        assertFalse(File(core, "Tetris (World).gb.state.metadata").exists())
        assertEquals("slot", File(core, "Tetris (World).gb.slot1").readText())
        assertEquals("other", File(core, "Other.gb.state").readText())
        val kept = replaced.walkTopDown().filter { it.isFile }.map { it.name to it.readText() }.toMap()
        assertEquals(
            mapOf(
                "Tetris (World).srm" to "old phone save",
                "Tetris (World).gb.state" to "auto",
                "Tetris (World).gb.state.metadata" to "meta",
            ),
            kept,
        )
    }

    @Test
    fun closingAGameWithoutSavingSendsNothing() {
        val game = boxGame("gb", "Tetris (World).zip")
        share.put("saves/gb/Tetris (World).srm", "box save")
        sync.syncSave(share, game)
        // The app writes the save again on the way out: same content, new date.
        File(saves, "Tetris (World).srm").setLastModified(System.currentTimeMillis() + 60_000)

        assertEquals(SaveAction.NOTHING, sync.syncSave(share, game))
        assertEquals("box save", share.text("saves/gb/Tetris (World).srm"))
    }

    @Test
    fun aGameWithItsOwnSaveKeyStillUsesTheBoxsName() {
        val game = boxGame("gbc", "Tetris (World).zip").copy(saveKey = "Tetris (World)~1a2b3c4d.gbc")
        share.put("saves/gbc/Tetris (World).srm", "color save")
        sync.syncSave(share, game)
        assertEquals("color save", File(saves, "Tetris (World)~1a2b3c4d.srm").readText())
    }

    @Test
    fun aGameTheBoxDoesNotHaveIsLeftAlone() {
        val own = boxGame("gb", "x.zip").copy(fileUri = "content://x/Homebrew.gb", fileName = "Homebrew.gb")
        localSave("Homebrew.srm", "mine", 5)
        assertEquals(BoxSync.SavesResult(0, 0), sync.syncSaves(share, listOf(own)))
        assertTrue(share.files.isEmpty())
    }

    @Test
    fun allSavesGoTheirOwnWays() {
        val tetris = boxGame("gb", "Tetris (World).zip", 1)
        val wario = boxGame("gbc", "Wario Land 3 (World).zip", 2)
        share.put("saves/gb/Tetris (World).srm", "box")
        localSave("Wario Land 3 (World).srm", "phone", 5)

        assertEquals(BoxSync.SavesResult(pulled = 1, pushed = 1), sync.syncSaves(share, listOf(tetris, wario)))
        assertEquals("phone", share.text("saves/gbc/Wario Land 3 (World).srm"))
        assertArrayEquals("box".toByteArray(), File(saves, "Tetris (World).srm").readBytes())
    }

    // ---- cheats ----

    @Test
    fun cheatFilesComeDownForGamesTheBoxHas() {
        share.put("roms/gb/Aerostar (USA, Europe).zip", "rom")
        share.put("cheats/cht/Gambatte/Aerostar (USA, Europe).cht", "a")
        share.put("cheats/cht/Gambatte/Aerostar (USA, Europe) (Game Genie).cht", "b")
        share.put("cheats/cht/Gambatte/Aerostar (USA, Europe) (GameShark).cht", "c")
        share.put("cheats/cht/Gambatte/Some Other Game (USA).cht", "d")
        sync.syncGames(share, emptyList())

        assertEquals(3, sync.syncCheats(share))
        assertEquals(3, cheats.listFiles()!!.size)
        assertEquals(0, sync.syncCheats(share))
    }

    @Test
    fun cheatFilesAreReadAndUnusableCodesLeftOut() {
        val text =
            """
            cheats = 4

            cheat0_desc = "Infinite Lives"
            cheat0_code = "0109E8FF"
            cheat0_enable = false

            cheat1_desc = "Invincibility"
            cheat1_code = "03A-ADD-A21+F06-F1E-6E9"
            cheat1_enable = false
            cheat2_desc = "Start On Level ??"
            cheat2_code = "01??E8FF"
            cheat3_code = "010AE8FF+0105E9FF"
            """.trimIndent()
        val cheats = ChtFile.parse(text)
        assertEquals(listOf("Infinite Lives", "Invincibility", "010AE8FF+0105E9FF"), cheats.map { it.description })
        assertEquals(listOf(false, true, false), cheats.map { it.isGameGenie })
        assertEquals("03A-ADD-A21+F06-F1E-6E9", cheats[1].code)
    }

    @Test
    fun cheatFileNamesGiveTheGame() {
        assertEquals("Aerostar (USA, Europe)", BoxNames.cheatFileStem("Aerostar (USA, Europe) (Game Genie).cht"))
        assertEquals("Aerostar (USA, Europe)", BoxNames.cheatFileStem("Aerostar (USA, Europe) (GameShark).cht"))
        assertEquals("Aerostar (USA, Europe)", BoxNames.cheatFileStem("Aerostar (USA, Europe).cht"))
        assertNull(BoxNames.cheatFileStem("gamelist.xml"))
    }
}
