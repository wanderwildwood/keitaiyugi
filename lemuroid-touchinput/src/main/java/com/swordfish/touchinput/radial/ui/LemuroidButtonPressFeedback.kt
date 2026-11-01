package com.swordfish.touchinput.radial.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The button's picture while it is held. A plain state change: it appears when pressed and goes
 * when released, with no fade and no progress ring, which only cost the panel redraws.
 * [animationDurationMillis] stays in the signature for the callers and is not used.
 */
@Composable
fun LemuroidButtonPressFeedback(
    pressed: Boolean,
    @Suppress("UNUSED_PARAMETER") animationDurationMillis: Int,
    icon: Int,
) {
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (pressed) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                LemuroidControlBackground()
                LemuroidButtonForeground(
                    pressed = remember { mutableStateOf(false) },
                    icon = icon,
                )
            }
        }
    }
}
