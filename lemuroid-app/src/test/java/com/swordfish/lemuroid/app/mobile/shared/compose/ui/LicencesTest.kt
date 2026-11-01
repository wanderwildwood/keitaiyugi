package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LicencesTest {
    private val data =
        LicencesData(
            "Handheld Games",
            SOURCE_URL,
            "GPL-3.0-only",
            LICENCE_COMPONENTS,
            LICENCE_RAW.map { LicenceText(it.first, it.first, it.second) },
        )

    @Test
    fun theDataHasNoProblems() {
        // The GPL-2.0 text of the Game Boy core shares the "gpl" family with the app's own GPL-3.0.
        val problems = licencesProblems(data).filterNot { p -> LICENCE_EXTRA_TEXT_IDS.any { p == "text $it is not used by anything" } }
        assertEquals(emptyList<String>(), problems)
        assertTrue(LICENCE_COMPONENTS.isNotEmpty())
    }

    @Test
    fun theSourceLinkIsTheRepo() {
        assertEquals("https://github.com/wanderwildwood/keitaiyugi", SOURCE_URL)
    }

    @Test
    fun theUpstreamCreditsStay() {
        val names = LICENCE_COMPONENTS.map { it.name }
        assertTrue(names.any { it.startsWith("Lemuroid") })
        assertTrue(names.any { it.startsWith("Gambatte") })
        assertTrue(names.any { it.startsWith("LibretroDroid") })
    }

    @Test
    fun theBundledLicenceTextsAreThere() {
        val raw = File("src/main/res/raw")
        listOf("gpl_3_0", "gpl_2_0", "apache_2_0", "mit", "ofl_1_1", "cc_by_sa_4_0").forEach {
            assertTrue(it, licenceParagraphs(File(raw, "$it.txt").readText()).size > 3)
        }
        assertTrue(File(raw, "gpl_3_0.txt").readText().contains("GNU GENERAL PUBLIC LICENSE"))
    }
}
