package com.swordfish.lemuroid.lib

import com.swordfish.lemuroid.lib.library.db.dao.GameSearchDao
import com.swordfish.lemuroid.lib.library.db.dao.Migrations
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.SaveKeys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class SaveKeysTest {
    private lateinit var db: Connection
    private lateinit var dir: File

    @Before
    fun setUp() {
        db = DriverManager.getConnection("jdbc:sqlite::memory:")
        dir =
            File.createTempFile("savekeys", "").also {
                it.delete()
                it.mkdirs()
            }
    }

    @After
    fun tearDown() {
        db.close()
        dir.deleteRecursively()
    }

    // The games table as version 9 had it, with made-up rows.
    private fun createVersion9() {
        db.createStatement().use {
            it.execute(
                """
                CREATE TABLE games (
                  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, fileName TEXT NOT NULL,
                  fileUri TEXT NOT NULL, title TEXT NOT NULL, systemId TEXT NOT NULL, developer TEXT,
                  coverFrontUrl TEXT, lastIndexedAt INTEGER NOT NULL, lastPlayedAt INTEGER,
                  isFavorite INTEGER NOT NULL)
                """,
            )
            it.execute("INSERT INTO games VALUES (1,'Alpha.gb','file:///a/Alpha.gb','Alpha','gb',NULL,NULL,1,NULL,0)")
            it.execute("INSERT INTO games VALUES (2,'Beta.gbc','file:///a/Beta.gbc','Beta','gbc',NULL,NULL,1,5,1)")
            // Two ROMs that already shared a save before the upgrade.
            it.execute("INSERT INTO games VALUES (3,'Twin.gb','file:///x/Twin.gb','Twin','gb',NULL,NULL,1,NULL,0)")
            it.execute("INSERT INTO games VALUES (4,'Twin.gb','file:///y/Twin.gb','Twin','gb',NULL,NULL,1,NULL,0)")
        }
    }

    private fun rows(): List<Game> =
        db.createStatement().use { st ->
            st.executeQuery("SELECT * FROM games ORDER BY id").use { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            Game(
                                id = rs.getInt("id"),
                                fileName = rs.getString("fileName"),
                                fileUri = rs.getString("fileUri"),
                                title = rs.getString("title"),
                                systemId = rs.getString("systemId"),
                                developer = null,
                                coverFrontUrl = null,
                                lastIndexedAt = rs.getLong("lastIndexedAt"),
                                isFavorite = rs.getInt("isFavorite") == 1,
                                saveKey = rs.getString("saveKey"),
                            ),
                        )
                    }
                }
            }
        }

    @Test
    fun migrationKeepsEveryExistingSavePath() {
        createVersion9()
        // Files exactly as version 9 wrote them: the .srm named for the ROM without its
        // extension, states and previews for the whole file name.
        val saves = File(dir, "saves").also { it.mkdirs() }
        val states = File(dir, "states/gambatte").also { it.mkdirs() }
        File(saves, "Alpha.srm").writeText("alpha-battery")
        File(saves, "Beta.srm").writeText("beta-battery")
        File(states, "Alpha.gb.state").writeText("alpha-auto")
        File(states, "Alpha.gb.slot2").writeText("alpha-slot2")
        File(states, "Beta.gbc.slot1").writeText("beta-slot1")

        Migrations.VERSION_9_10_SQL.forEach { sql -> db.createStatement().use { it.execute(sql) } }
        val games = rows()

        assertEquals(listOf("Alpha.gb", "Beta.gbc", "Twin.gb", "Twin.gb"), games.map { it.saveKey })
        assertEquals(4, games.size)
        // Every other column is untouched.
        assertTrue(games[1].isFavorite)
        assertEquals("file:///a/Beta.gbc", games[1].fileUri)

        val alpha = games[0]
        val beta = games[1]
        assertEquals("alpha-battery", File(saves, SaveKeys.srmName(alpha.saveName())).readText())
        assertEquals("beta-battery", File(saves, SaveKeys.srmName(beta.saveName())).readText())
        assertEquals("alpha-auto", File(states, SaveKeys.stateName(alpha.saveName())).readText())
        assertEquals("alpha-slot2", File(states, SaveKeys.slotName(alpha.saveName(), 1)).readText())
        assertEquals("beta-slot1", File(states, SaveKeys.slotName(beta.saveName(), 0)).readText())
    }

    @Test
    fun aGameWithNoKeyFallsBackToItsFileName() {
        val g =
            Game(
                fileName = "Foo.gb",
                fileUri = "u",
                title = "Foo",
                systemId = "gb",
                developer = null,
                coverFrontUrl = null,
                lastIndexedAt = 0,
            )
        assertEquals("Foo.gb", g.saveName())
    }

    @Test
    fun sameNameInTwoFoldersGetsOwnKeys() {
        val taken = mutableListOf<String>()
        val a = SaveKeys.assign("Foo.gb", "file:///one/Foo.gb", taken).also { taken.add(it) }
        val b = SaveKeys.assign("Foo.gb", "file:///two/Foo.gb", taken).also { taken.add(it) }
        assertEquals("Foo.gb", a)
        assertNotEquals(a, b)
        assertTrue(b.startsWith("Foo~") && b.endsWith(".gb"))
        assertNotEquals(SaveKeys.srmName(a), SaveKeys.srmName(b))
    }

    @Test
    fun gbAndGbcOfTheSameNameDoNotShareASrm() {
        val taken = mutableListOf<String>()
        val a = SaveKeys.assign("Foo.gb", "file:///one/Foo.gb", taken).also { taken.add(it) }
        val b = SaveKeys.assign("Foo.gbc", "file:///one/Foo.gbc", taken).also { taken.add(it) }
        assertNotEquals(SaveKeys.srmName(a), SaveKeys.srmName(b))
        assertTrue(b.endsWith(".gbc"))
    }

    @Test
    fun theKeyIsStableAndCaseBlind() {
        val first = SaveKeys.assign("FOO.gb", "file:///two/FOO.gb", listOf("foo.gb"))
        val again = SaveKeys.assign("FOO.gb", "file:///two/FOO.gb", listOf("foo.gb"))
        assertEquals(first, again)
        assertNotEquals("FOO.gb", first)
    }

    @Test
    fun differentNamesKeepTheirFileName() {
        assertEquals("Bar.gb", SaveKeys.assign("Bar.gb", "u", listOf("Foo.gb", "Foo.gbc")))
    }

    @Test
    fun searchQueryTurnsWordsIntoPrefixes() {
        assertEquals("Mar* Kar*", GameSearchDao.buildMatchQuery("Mar Kar"))
        assertEquals("pok* red*", GameSearchDao.buildMatchQuery("  pok,  red "))
    }

    @Test
    fun searchQueryDropsSyntaxCharacters() {
        assertEquals("a* b*", GameSearchDao.buildMatchQuery("\"a\" - (b) : ^ ' ; *"))
        assertEquals("tom* jerry*", GameSearchDao.buildMatchQuery("tom-jerry"))
        assertNull(GameSearchDao.buildMatchQuery("   "))
        assertNull(GameSearchDao.buildMatchQuery("\"()*:-"))
        assertNull(GameSearchDao.buildMatchQuery(""))
    }

    @Test
    fun searchQueryKeepsAccentedLetters() {
        assertEquals("pokémon*", GameSearchDao.buildMatchQuery("pokémon"))
    }

    @Test
    fun onlyGameBoyRomsAreGames() {
        assertTrue(com.swordfish.lemuroid.lib.library.RomFilter.isGameBoyRom("gb", "Foo.gb"))
        assertTrue(com.swordfish.lemuroid.lib.library.RomFilter.isGameBoyRom("gbc", "Foo.GBC"))
        assertTrue(!com.swordfish.lemuroid.lib.library.RomFilter.isGameBoyRom("gb", "notes.txt"))
        assertTrue(!com.swordfish.lemuroid.lib.library.RomFilter.isGameBoyRom("gb", "archive.zip"))
        assertTrue(!com.swordfish.lemuroid.lib.library.RomFilter.isGameBoyRom("gb", "cover.png"))
        assertTrue(!com.swordfish.lemuroid.lib.library.RomFilter.isGameBoyRom("nes", "Foo.gb"))
        assertTrue(!com.swordfish.lemuroid.lib.library.RomFilter.isGameBoyRom(null, "Foo.gb"))
    }
}
