package com.swordfish.lemuroid.app.mobile.feature.settings.general

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.main.MainRoute
import com.swordfish.lemuroid.app.mobile.feature.main.navigateToRoute
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import com.swordfish.lemuroid.app.utils.android.settings.booleanPreferenceState
import com.swordfish.lemuroid.app.utils.android.settings.indexPreferenceState
import com.swordfish.lemuroid.app.utils.android.stringListResource

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    navController: NavController,
) {
    val state =
        viewModel.uiState
            .collectAsState(SettingsViewModel.State())
            .value

    val scanInProgress =
        viewModel.directoryScanInProgress
            .collectAsState(false)
            .value

    val indexingInProgress =
        viewModel.indexingInProgress
            .collectAsState(false)
            .value

    LemuroidSettingsPage(modifier = modifier) {
        RomsSettings(
            state = state,
            onChangeFolder = { viewModel.changeLocalStorageFolder() },
            indexingInProgress = indexingInProgress,
            scanInProgress = scanInProgress,
        )
        GeneralSettings()
        InputSettings(navController = navController)
        MiscSettings(
            indexingInProgress = indexingInProgress,
            isSaveSyncSupported = state.isSaveSyncSupported,
            navController = navController,
        )
    }
}

@Composable
private fun MiscSettings(
    indexingInProgress: Boolean,
    isSaveSyncSupported: Boolean,
    navController: NavController,
) {
    LemuroidCardSettingsGroup(
        title = { Text(text = stringResource(id = R.string.settings_category_misc)) },
    ) {
        if (isSaveSyncSupported) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.settings_title_save_sync)) },
                subtitle = {
                    Text(text = stringResource(id = R.string.settings_description_save_sync))
                },
                onClick = { navController.navigateToRoute(MainRoute.SETTINGS_SAVE_SYNC) },
            )
        }
        // No core chooser: there is one core for one system, so the screen could only
        // ever show a single row with nothing to choose between. No BIOS screen either
        // -- a Game Boy needs no BIOS file and gambatte asks for none, so it could only
        // ever report that nothing is missing.
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_advanced_settings)) },
            subtitle = {
                Text(text = stringResource(id = R.string.settings_description_advanced_settings))
            },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_ADVANCED) },
        )
    }
}

@Composable
private fun InputSettings(navController: NavController) {
    LemuroidCardSettingsGroup(
        title = { Text(text = stringResource(id = R.string.settings_category_input)) },
    ) {
        LemuroidSettingsList(
            state =
                indexPreferenceState(
                    R.string.pref_key_haptic_feedback_mode,
                    "press",
                    stringListResource(R.array.pref_key_haptic_feedback_mode_values),
                ),
            title = {
                Text(text = stringResource(id = R.string.settings_title_enable_touch_feedback))
            },
            items = stringListResource(R.array.pref_key_haptic_feedback_mode_display_names),
        )
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_gamepad_settings)) },
            subtitle = {
                Text(text = stringResource(id = R.string.settings_description_gamepad_settings))
            },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_INPUT_DEVICES) },
        )
    }
}

@Composable
private fun GeneralSettings() {
    LemuroidCardSettingsGroup(
        title = { Text(text = stringResource(id = R.string.settings_category_general)) },
    ) {
        LemuroidSettingsSwitch(
            state = booleanPreferenceState(R.string.pref_key_autosave, true),
            title = { Text(text = stringResource(id = R.string.settings_title_enable_autosave)) },
            subtitle = { Text(text = stringResource(id = R.string.settings_description_enable_autosave)) },
        )
        // Immersive mode is gone. It colours the letterbox from the game's own visuals,
        // which on four greys is a grey wash -- more ink held on the panel, more
        // ghosting, and nothing gained. HD mode and its quality slider are gone for the
        // same kind of reason: they are post-processing upscalers, and this app draws a
        // 160x144 picture at exactly 3x onto a screen with no colour. There is nothing
        // for them to improve and a cost to running them.
        LemuroidSettingsList(
            state =
                indexPreferenceState(
                    R.string.pref_key_shader_filter,
                    "auto",
                    stringListResource(R.array.pref_key_shader_filter_values).toList(),
                ),
            title = { Text(text = stringResource(id = R.string.display_filter)) },
            items = stringListResource(R.array.pref_key_shader_filter_display_names),
        )
    }
}

@Composable
private fun RomsSettings(
    state: SettingsViewModel.State,
    onChangeFolder: () -> Unit,
    indexingInProgress: Boolean,
    scanInProgress: Boolean,
) {
    val context = LocalContext.current

    val currentDirectory = state.currentDirectory
    val emptyDirectory = stringResource(R.string.none)

    val currentDirectoryName =
        remember(state.currentDirectory) {
            runCatching {
                DocumentFile.fromTreeUri(context, Uri.parse(currentDirectory))?.name
            }.getOrNull() ?: emptyDirectory
        }

    LemuroidCardSettingsGroup(title = { Text(text = stringResource(id = R.string.roms)) }) {
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.directory)) },
            subtitle = { Text(text = currentDirectoryName) },
            onClick = { onChangeFolder() },
            enabled = !indexingInProgress,
        )
        if (scanInProgress) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.stop)) },
                onClick = { LibraryIndexScheduler.cancelLibrarySync(context) },
            )
        } else {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.rescan)) },
                onClick = { LibraryIndexScheduler.scheduleLibrarySync(context) },
                enabled = !indexingInProgress,
            )
        }
    }
}
