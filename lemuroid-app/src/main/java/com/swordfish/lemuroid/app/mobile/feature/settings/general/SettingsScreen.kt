package com.swordfish.lemuroid.app.mobile.feature.settings.general

import android.net.Uri
import com.swordfish.lemuroid.app.shared.Notice
import com.swordfish.lemuroid.lib.storage.local.ChosenFolder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import com.mudita.mmd.components.text.TextMMD
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.main.MainRoute
import com.swordfish.lemuroid.app.mobile.feature.main.navigateToRoute
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.DarkMode
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.rememberArmedFlag
import com.swordfish.lemuroid.app.shared.FilesCreateDocument
import com.swordfish.lemuroid.app.shared.FilesOpenDocument
import com.swordfish.lemuroid.app.shared.box.BoxSyncRunner
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import com.swordfish.lemuroid.app.utils.android.settings.booleanPreferenceState
import com.swordfish.lemuroid.app.utils.android.settings.indexPreferenceState
import com.swordfish.lemuroid.app.utils.android.stringListResource
import com.alorma.compose.settings.storage.base.SettingValueState
import com.swordfish.lemuroid.lib.box.BoxLink
import com.swordfish.lemuroid.lib.saves.SavesBackup
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    boxLink: BoxLink,
    boxSyncRunner: BoxSyncRunner,
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
        item {
            RomsSettings(
                state = state,
                onChangeFolder = { viewModel.changeLocalStorageFolder() },
                indexingInProgress = indexingInProgress,
                scanInProgress = scanInProgress,
            )
        }
        item { BoxSettings(link = boxLink, runner = boxSyncRunner) }
        item { GeneralSettings() }
        item { InputSettings(navController = navController) }
        item { SavesSettings() }
        item {
            MiscSettings(
                indexingInProgress = indexingInProgress,
                isSaveSyncSupported = state.isSaveSyncSupported,
                navController = navController,
            )
        }
    }
}

/**
 * The saves to and from a file the player picks. Restore is armed: the first press says what a
 * second will do. It stays armed until the next other touch, Back or leaving the screen.
 */
@Composable
private fun SavesSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var armed by rememberArmedFlag()
    var busy by remember { mutableStateOf(false) }
    val save =
        rememberLauncherForActivityResult(FilesCreateDocument("application/zip")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            busy = true
            scope.launch {
                val ok = SavesBackupFiles.save(context, uri)
                busy = false
                // Said along the foot until the next press, not in a toast that goes by itself.
                Notice.say(context.getString(if (ok) R.string.saves_backup_saved else R.string.saves_backup_not_saved))
            }
        }
    val restore =
        rememberLauncherForActivityResult(FilesOpenDocument(writable = false)) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            busy = true
            scope.launch {
                val message =
                    when (val r = SavesBackupFiles.restore(context, uri)) {
                        is SavesBackupFiles.Result.Done ->
                            context.resources.getQuantityString(R.plurals.saves_restore_done, r.files, r.files)
                        is SavesBackupFiles.Result.Refused ->
                            context.getString(
                                when (r.kind) {
                                    SavesBackup.BackupException.Kind.NOT_A_BACKUP -> R.string.saves_restore_not_ours
                                    SavesBackup.BackupException.Kind.NEWER -> R.string.saves_restore_newer
                                    SavesBackup.BackupException.Kind.DAMAGED -> R.string.saves_restore_damaged
                                    SavesBackup.BackupException.Kind.TOO_BIG -> R.string.saves_restore_damaged
                                },
                            )
                        SavesBackupFiles.Result.Unreadable -> context.getString(R.string.saves_restore_unreadable)
                        SavesBackupFiles.Result.StoppedPartway -> context.getString(R.string.saves_restore_partway)
                    }
                busy = false
                Notice.say(message)
            }
        }
    LemuroidCardSettingsGroup(
        title = { TextMMD(text = stringResource(id = R.string.settings_category_saves)) },
    ) {
        LemuroidSettingsMenuLink(
            title = { TextMMD(text = stringResource(id = R.string.settings_title_save_saves)) },
            subtitle = { TextMMD(text = stringResource(id = R.string.settings_description_save_saves)) },
            enabled = !busy,
            onClick = {
                runCatching { save.launch("handheld-games-saves-${java.time.LocalDate.now()}.zip") }
            },
        )
        LemuroidSettingsMenuLink(
            title = { TextMMD(text = stringResource(id = R.string.settings_title_restore_saves)) },
            subtitle = {
                TextMMD(
                    text =
                        stringResource(
                            if (armed) {
                                R.string.settings_restore_saves_armed
                            } else {
                                R.string.settings_description_restore_saves
                            },
                        ),
                )
            },
            enabled = !busy,
            onClick = {
                if (!armed) {
                    armed = true
                } else {
                    armed = false
                    runCatching {
                        restore.launch(
                            arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed"),
                        )
                    }
                }
            },
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
        title = { TextMMD(text = stringResource(id = R.string.settings_category_misc)) },
    ) {
        if (isSaveSyncSupported) {
            LemuroidSettingsMenuLink(
                title = { TextMMD(text = stringResource(id = R.string.settings_title_save_sync)) },
                subtitle = {
                    TextMMD(text = stringResource(id = R.string.settings_description_save_sync))
                },
                onClick = { navController.navigateToRoute(MainRoute.SETTINGS_SAVE_SYNC) },
            )
        }
        // No core chooser: there is one core for one system, so the screen could only
        // ever show a single row with nothing to choose between. No BIOS screen either
        // -- a Game Boy needs no BIOS file and gambatte asks for none, so it could only
        // ever report that nothing is missing.
        LemuroidSettingsMenuLink(
            title = { TextMMD(text = stringResource(id = R.string.settings_title_advanced_settings)) },
            subtitle = {
                TextMMD(text = stringResource(id = R.string.settings_description_advanced_settings))
            },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_ADVANCED) },
        )
    }
}

@Composable
private fun InputSettings(navController: NavController) {
    LemuroidCardSettingsGroup(
        title = { TextMMD(text = stringResource(id = R.string.settings_category_input)) },
    ) {
        LemuroidSettingsList(
            state =
                indexPreferenceState(
                    R.string.pref_key_haptic_feedback_mode,
                    "press",
                    stringListResource(R.array.pref_key_haptic_feedback_mode_values),
                ),
            title = {
                TextMMD(text = stringResource(id = R.string.settings_title_enable_touch_feedback))
            },
            items = stringListResource(R.array.pref_key_haptic_feedback_mode_display_names),
        )
        LemuroidSettingsMenuLink(
            title = { TextMMD(text = stringResource(id = R.string.settings_title_gamepad_settings)) },
            subtitle = {
                TextMMD(text = stringResource(id = R.string.settings_description_gamepad_settings))
            },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_INPUT_DEVICES) },
        )
    }
}

@Composable
private fun GeneralSettings() {
    LemuroidCardSettingsGroup(
        title = { TextMMD(text = stringResource(id = R.string.settings_category_general)) },
    ) {
        LemuroidSettingsSwitch(
            state = booleanPreferenceState(R.string.pref_key_autosave, true),
            title = { TextMMD(text = stringResource(id = R.string.settings_title_enable_autosave)) },
            subtitle = { TextMMD(text = stringResource(id = R.string.settings_description_enable_autosave)) },
        )
        LemuroidSettingsSwitch(
            state = booleanPreferenceState(R.string.pref_key_turn_with_phone, true),
            title = { TextMMD(text = stringResource(id = R.string.settings_title_turn_with_phone)) },
            subtitle = { TextMMD(text = stringResource(id = R.string.settings_description_turn_with_phone)) },
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
            title = { TextMMD(text = stringResource(id = R.string.display_filter)) },
            items = stringListResource(R.array.pref_key_shader_filter_display_names),
        )
        val context = LocalContext.current
        LemuroidSettingsSwitch(
            // Dark mode is held by DarkMode, not by a preference key.
            state =
                object : SettingValueState<Boolean> {
                    override var value: Boolean
                        get() = DarkMode.on
                        set(value) = DarkMode.set(context, value)

                    override fun reset() = DarkMode.set(context, false)
                },
            title = { TextMMD(text = stringResource(id = R.string.settings_dark)) },
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

    val goneDirectory = stringResource(R.string.settings_directory_gone)
    val currentDirectoryName =
        remember(state.currentDirectory) {
            val tree = runCatching { Uri.parse(currentDirectory) }.getOrNull()
            when {
                currentDirectory.isNullOrEmpty() || tree == null -> emptyDirectory
                // A folder the phone can no longer read says so, by the name it had.
                !ChosenFolder.isReadable(context, tree) ->
                    String.format(goneDirectory, ChosenFolder.nameFromTree(tree) ?: currentDirectory)
                else ->
                    runCatching { DocumentFile.fromTreeUri(context, tree)?.name }.getOrNull() ?: emptyDirectory
            }
        }

    LemuroidCardSettingsGroup(title = { TextMMD(text = stringResource(id = R.string.roms)) }) {
        LemuroidSettingsMenuLink(
            title = { TextMMD(text = stringResource(id = R.string.directory)) },
            subtitle = {
                TextMMD(
                    text =
                        if (indexingInProgress) {
                            stringResource(R.string.settings_directory_wait)
                        } else {
                            currentDirectoryName
                        },
                )
            },
            onClick = { onChangeFolder() },
            enabled = !indexingInProgress,
        )
        if (scanInProgress) {
            LemuroidSettingsMenuLink(
                title = { TextMMD(text = stringResource(id = R.string.stop)) },
                onClick = { LibraryIndexScheduler.cancelLibrarySync(context) },
            )
        } else {
            LemuroidSettingsMenuLink(
                title = { TextMMD(text = stringResource(id = R.string.rescan)) },
                onClick = { LibraryIndexScheduler.scheduleLibrarySync(context) },
                enabled = !indexingInProgress,
            )
        }
    }
}
