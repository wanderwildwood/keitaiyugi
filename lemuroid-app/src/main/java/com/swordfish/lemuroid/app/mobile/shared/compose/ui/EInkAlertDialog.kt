package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A titled dialog with a press at the bottom, in [EInkDialog]'s panel.
 *
 * Upstream asked six questions through Material's `AlertDialog`, which animates in, sizes
 * itself to its buttons, and takes its container from `surfaceContainerHigh`. That last one
 * is the reason the FAQ dialog once came up as text floating over the game list: MMD does not
 * set that role, and an unset role paints transparent. See [monochrome].
 *
 * Material's centred icon above the title goes with it, and a button in the slots is a
 * full-width [OutlinedButtonMMD] rather than a strip of small text in the corner — which is
 * how every other app on this phone asks.
 *
 * [modifier] lands on the content rather than the panel, because the two gamepad-binding
 * screens hang a focus requester and a key handler off it and need them inside the dialog.
 */
@Composable
fun EInkAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (ColumnScope.() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    EInkDialog(onDismiss = onDismissRequest) {
        Column(modifier = modifier) {
            if (title != null) {
                CompositionLocalProvider(
                    LocalTextStyle provides
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                ) { title() }
                Spacer(Modifier.height(12.dp))
            }
            if (text != null) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodySmall,
                ) { text() }
                Spacer(Modifier.height(16.dp))
            }
            confirmButton()
            if (dismissButton != null) {
                Spacer(Modifier.height(8.dp))
                dismissButton()
            }
        }
    }
}
