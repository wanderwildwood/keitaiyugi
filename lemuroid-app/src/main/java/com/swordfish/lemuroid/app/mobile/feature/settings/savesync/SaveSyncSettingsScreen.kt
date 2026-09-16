package com.swordfish.lemuroid.app.mobile.feature.settings.savesync

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mudita.mmd.components.text.TextMMD
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.savesync.SaveSyncWork
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsListMultiSelect
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import com.swordfish.lemuroid.app.utils.android.settings.booleanPreferenceState
import com.swordfish.lemuroid.app.utils.android.settings.stringsSetPreferenceState

@Composable
fun SaveSyncSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SaveSyncSettingsViewModel,
) {
    val context = LocalContext.current

    val saveSyncState =
        viewModel.uiState
            .collectAsState()
            .value

    val isSyncInProgress =
        viewModel.saveSyncInProgress
            .collectAsState(true)
            .value

    LemuroidSettingsPage(modifier = modifier.fillMaxSize()) {
        LemuroidCardSettingsGroup {
            LemuroidSettingsMenuLink(
                title = {
                    TextMMD(
                        text =
                            stringResource(
                                id = R.string.settings_save_sync_configure,
                                saveSyncState.provider,
                            ),
                    )
                },
                subtitle = { TextMMD(text = saveSyncState.configInfo) },
                enabled = !isSyncInProgress,
                onClick = { context.startActivity(Intent(context, saveSyncState.settingsActivity)) },
            )
            LemuroidSettingsSwitch(
                state = booleanPreferenceState(R.string.pref_key_save_sync_enable, default = false),
                title = { TextMMD(text = stringResource(id = R.string.settings_save_sync_include_saves)) },
                subtitle = {
                    TextMMD(
                        text =
                            stringResource(
                                id = R.string.settings_save_sync_include_saves_description,
                                saveSyncState.savesSpace,
                            ),
                    )
                },
                enabled = saveSyncState.isConfigured && !isSyncInProgress,
            )
            LemuroidSettingsListMultiSelect(
                state =
                    stringsSetPreferenceState(
                        stringResource(R.string.pref_key_save_sync_cores),
                        emptySet(),
                    ),
                title = { TextMMD(text = stringResource(id = R.string.settings_save_sync_include_states)) },
                subtitle = { TextMMD(text = stringResource(id = R.string.settings_save_sync_include_states_description)) },
                entryValues = saveSyncState.coreNames,
                entries = saveSyncState.coreVisibleNames,
                enabled = saveSyncState.isConfigured && !isSyncInProgress,
                confirmButton = stringResource(id = R.string.ok),
            )
            LemuroidSettingsSwitch(
                state = booleanPreferenceState(R.string.pref_key_save_sync_auto, default = false),
                title = { TextMMD(text = stringResource(id = R.string.settings_save_sync_enable_auto)) },
                subtitle = { TextMMD(text = stringResource(id = R.string.settings_save_sync_enable_auto_description)) },
                enabled = saveSyncState.isConfigured && !isSyncInProgress,
            )
            LemuroidSettingsMenuLink(
                title = { TextMMD(text = stringResource(id = R.string.settings_save_sync_refresh)) },
                subtitle = {
                    TextMMD(
                        text =
                            stringResource(
                                id = R.string.settings_save_sync_refresh_description,
                                saveSyncState.lastSyncInfo,
                            ),
                    )
                },
                enabled = saveSyncState.isConfigured && !isSyncInProgress,
                onClick = { SaveSyncWork.enqueueManualWork(context) },
            )
        }
    }
}
