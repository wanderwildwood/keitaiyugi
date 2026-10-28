package com.swordfish.touchinput.radial.layouts

import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import gg.padkit.anchors.Anchor
import gg.padkit.ids.Id
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.max

class GBFaceButtonsTest {
    private val a = KeyEvent.KEYCODE_BUTTON_A
    private val b = KeyEvent.KEYCODE_BUTTON_B
    private val anchors = spacedFaceButtons(a, b) + bothFaceButtons(a, b)

    // The same rule padkit applies to a finger: the anchor with the smallest distance from its
    // edge wins, the first in the list on a tie.
    private fun pressedAt(point: Offset): Set<Int> {
        fun distance(anchor: Anchor<Id.Key>) = max(0f, (point - anchor.position).getDistance() - anchor.size)
        val closest = anchors.minByOrNull { distance(it) }!!
        return closest.buttons.map { it.value }.toSet()
    }

    private val towardA = Offset(0.866f, -0.5f)

    @Test
    fun centre_presses_both() {
        assertEquals(setOf(a, b), pressedAt(Offset.Zero))
    }

    @Test
    fun anywhere_in_the_gap_presses_both() {
        for (t in listOf(-0.5f, -0.3f, 0.3f, 0.5f)) {
            assertEquals("t=$t", setOf(a, b), pressedAt(towardA * t))
        }
    }

    @Test
    fun a_button_centre_presses_only_that_button() {
        assertEquals(setOf(a), pressedAt(towardA))
        assertEquals(setOf(b), pressedAt(-towardA))
    }

    @Test
    fun just_beside_a_button_still_presses_only_it() {
        assertEquals(setOf(a), pressedAt(towardA * 0.7f))
        assertEquals(setOf(b), pressedAt(towardA * -0.7f))
    }

    @Test
    fun the_zone_stays_clear_of_both_buttons() {
        // zone edge to each button's edge leaves room, so the two never overlap
        assertEquals(true, BOTH_ZONE_SIZE + FACE_BUTTON_SIZE < 1f)
    }
}
