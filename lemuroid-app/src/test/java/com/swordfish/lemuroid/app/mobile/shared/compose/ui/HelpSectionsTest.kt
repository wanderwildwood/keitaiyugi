package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HelpSectionsTest {
    private val help = """
        <b>&#8226;How do I start?</b><br/>
        Choose a folder in Settings.<br/><br/>
        <b>&#8226;Why &amp; how?</b><br/>
        Because <i>it is so</i>.<br/><br/>
        A closing note without a question.
    """.trimIndent()

    @Test fun questionBecomesHeadingAndAnswerBody() {
        val s = helpSections(help)
        assertEquals(3, s.size)
        assertEquals("How do I start?", s[0].heading)
        assertEquals("Choose a folder in Settings.", s[0].body)
        assertEquals("Why & how?", s[1].heading)
        assertEquals("Because it is so.", s[1].body)
    }

    @Test fun plainParagraphHasNoHeading() {
        val s = helpSections(help)
        assertNull(s[2].heading)
        assertEquals("A closing note without a question.", s[2].body)
    }

    @Test fun noSectionIsEmptyOrBroken() {
        val data = AboutData("Games 1", helpSections(help), "https://example.org/x")
        assertEquals(emptyList<String>(), aboutProblems(data))
    }
}
