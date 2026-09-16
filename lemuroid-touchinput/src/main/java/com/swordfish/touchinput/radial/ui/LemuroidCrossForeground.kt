package com.swordfish.touchinput.radial.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import gg.padkit.ui.DefaultCrossForeground

@Composable
fun LemuroidCrossForeground(
    allowDiagonals: Boolean,
    directionState: State<Offset>,
) {
    DefaultCrossForeground(
        modifier = Modifier.fillMaxSize(),
        directionState = directionState,
        allowDiagonals = allowDiagonals,
        leftDial = {
            LemuroidCrossButton(it, Icons.ChevronLeft)
        },
        rightDial = {
            LemuroidCrossButton(it, Icons.ChevronRight)
        },
        topDial = {
            LemuroidCrossButton(it, Icons.ChevronUp)
        },
        bottomDial = {
            LemuroidCrossButton(it, Icons.ChevronDown)
        },
        foregroundComposite = {
            LemuroidCompositeForeground(it)
        },
    )
}

@Composable
private fun LemuroidCrossButton(
    pressedState: State<Boolean>,
    imageVector: ImageVector,
) {
    LemuroidButtonForeground(
        pressed = pressedState,
        label = { },
        icon = {
            Icon(
                modifier = Modifier.size(maxWidth * 0.5f, maxHeight * 0.5f),
                imageVector = imageVector,
                contentDescription = "",
                tint = LocalLemuroidPadTheme.current.icons(pressedState.value),
            )
        },
    )
}
