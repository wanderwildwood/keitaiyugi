package com.swordfish.lemuroid.app.mobile.feature.gamemenu.states

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink

@Composable
fun GameMenuStatesScreen(
    viewModel: GameMenuStatesViewModel,
    onStateClicked: (Int) -> Unit,
) {
    val state = viewModel.uiStates.collectAsState(initial = GameMenuStatesViewModel.State())

    // Paged, not scrolled: MMD's list steps four rows to a swipe and stops, and brings
    // the chevron rail with it. Nothing on this panel coasts.
    LazyColumnMMD {
        state.value.entries.forEachIndexed { index, entry ->
            item {
                LemuroidSettingsMenuLink(
                    title = { TextMMD(text = entry.title) },
                    subtitle = { TextMMD(text = entry.description) },
                    enabled = entry.enabled,
                    icon = {
                        if (entry.preview != null) {
                            Image(
                                modifier = Modifier.size(48.dp),
                                bitmap = entry.preview.asImageBitmap(),
                                contentScale = ContentScale.Crop,
                                contentDescription = null,
                            )
                        }
                    },
                    onClick = { onStateClicked(index) },
                )
            }
        }
    }
}
