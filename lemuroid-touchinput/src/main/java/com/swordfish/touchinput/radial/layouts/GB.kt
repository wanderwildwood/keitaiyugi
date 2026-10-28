package com.swordfish.touchinput.radial.layouts

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.swordfish.touchinput.radial.controls.LemuroidControlCross
import com.swordfish.touchinput.radial.controls.LemuroidControlFaceButtons
import com.swordfish.touchinput.radial.layouts.shared.ComposeTouchLayouts
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonMenu
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonMenuPlaceholder
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonSelect
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonStart
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import com.swordfish.touchinput.radial.ui.LemuroidButtonForeground
import gg.padkit.PadKitScope
import gg.padkit.anchors.Anchor
import gg.padkit.ids.Id
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PadKitScope.GBLeft(
    modifier: Modifier = Modifier,
    settings: TouchControllerSettingsManager.Settings,
) {
    BaseLayoutLeft(
        settings = settings,
        modifier = modifier,
        primaryDial = { LemuroidControlCross(id = Id.DiscreteDirection(ComposeTouchLayouts.MOTION_SOURCE_DPAD)) },
        secondaryDials = {
            SecondaryButtonSelect()
            SecondaryButtonMenuPlaceholder(settings)
        },
    )
}

@Composable
fun PadKitScope.GBRight(
    modifier: Modifier = Modifier,
    settings: TouchControllerSettingsManager.Settings,
) {
    val spaced = remember { spacedFaceButtons(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B) }
    val both = remember { bothFaceButtons(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B) }
    BaseLayoutRight(
        settings = settings,
        modifier = modifier,
        primaryDial = {
            LemuroidControlFaceButtons(
                primaryAnchors = spaced,
                compositeAnchors = both,
                foregroundComposite = {},
                idsForegrounds =
                    persistentMapOf<Id.Key, @Composable (State<Boolean>) -> Unit>(
                        Id.Key(KeyEvent.KEYCODE_BUTTON_A) to { LemuroidButtonForeground(pressed = it, label = "A") },
                        Id.Key(KeyEvent.KEYCODE_BUTTON_B) to { LemuroidButtonForeground(pressed = it, label = "B") },
                    ),
            )
        },
        secondaryDials = {
            SecondaryButtonStart()
            SecondaryButtonMenu(settings)
        },
    )
}

/**
 * A and B on the same slant as before (A up and to the right, B down and to the left), but
 * smaller within the dial so a clear gap of about a quarter of the dial lies between them: at
 * half the dial each they touched, and a thumb meant for one pressed the other.
 */
internal fun spacedFaceButtons(
    first: Int,
    second: Int,
): PersistentList<Anchor<Id.Key>> {
    val angle = Math.toRadians(-30.0)
    val toFirst = Offset(cos(angle).toFloat(), sin(angle).toFloat())
    return persistentListOf(
        Anchor(toFirst, persistentSetOf(Id.Key(first)), FACE_BUTTON_SIZE),
        Anchor(-toFirst, persistentSetOf(Id.Key(second)), FACE_BUTTON_SIZE),
    )
}

/**
 * One thumb in the gap between A and B presses both: a zone in the middle of the dial that
 * reaches nearly to both buttons, leaving the last stretch beside each to that button alone.
 * The buttons themselves light up; the zone draws nothing, so the dial looks as it did.
 */
internal fun bothFaceButtons(
    first: Int,
    second: Int,
): PersistentList<Anchor<Id.Key>> =
    persistentListOf(
        Anchor(Offset.Zero, persistentSetOf(Id.Key(first), Id.Key(second)), BOTH_ZONE_SIZE),
    )

/** Each button's width as a share of the dial (it was a half, which left no gap at all). */
internal const val FACE_BUTTON_SIZE = 0.36f

/** Radius of the press-both zone; the gap between the buttons' edges is 0.64 each side of centre. */
internal const val BOTH_ZONE_SIZE = 0.55f
