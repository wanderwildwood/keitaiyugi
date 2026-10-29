package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alorma.compose.settings.storage.memory.rememberMemoryBooleanSettingState
import com.alorma.compose.settings.storage.memory.rememberMemoryIntSettingState
import com.mudita.mmd.components.text.TextMMD
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.tilt.TiltConfigurationMenuEntry
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.Icons
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.PagedColumn
import com.swordfish.lemuroid.app.shared.GameMenuContract
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidArmedSettingsLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import kotlin.reflect.KFunction1

@Composable
fun GameMenuHomeScreen(
    navController: NavController,
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
    onResult: KFunction1<Intent.() -> Unit, Unit>,
) {
    // Paged, not scrolled: MMD's list steps four rows to a swipe and stops, with no
    // scrollbar beside it. Nothing on this panel coasts.
    PagedColumn {
        item {
            if (gameMenuRequest.coreConfig.statesSupported) {
                LemuroidSettingsMenuLink(
                    title = { TextMMD(text = stringResource(id = R.string.game_menu_save)) },
                    icon = {
                        MenuIcon(Icons.Save, stringResource(id = R.string.game_menu_save))
                    },
                    onClick = { navController.navigateToRoute(GameMenuRoute.SAVE) },
                )

                LemuroidSettingsMenuLink(
                    title = { TextMMD(text = stringResource(id = R.string.game_menu_load)) },
                    icon = {
                        MenuIcon(Icons.FolderOpen, stringResource(id = R.string.game_menu_load))
                    },
                    onClick = { navController.navigateToRoute(GameMenuRoute.LOAD) },
                )
            }
        }
        item {
            LemuroidSettingsMenuLink(
                title = { TextMMD(text = stringResource(id = R.string.game_menu_quit)) },
                icon = {
                    MenuIcon(Icons.Logout, stringResource(id = R.string.game_menu_quit))
                },
                onClick = {
                    onResult { putExtra(GameMenuContract.RESULT_QUIT, true) }
                },
            )
        }
        item {
            LemuroidArmedSettingsLink(
                title = stringResource(id = R.string.game_menu_restart),
                armedTitle = stringResource(id = R.string.game_menu_restart_armed),
                icon = {
                    MenuIcon(Icons.RestartAlt, stringResource(id = R.string.game_menu_restart))
                },
                onConfirmed = {
                    onResult { putExtra(GameMenuContract.RESULT_RESET, true) }
                },
            )
        }
        item {
            LemuroidSettingsSwitch(
                title = { TextMMD(text = stringResource(id = R.string.game_menu_mute_audio)) },
                icon = {
                    MenuIcon(Icons.VolumeOff, stringResource(id = R.string.game_menu_mute_audio))
                },
                state = rememberMemoryBooleanSettingState(!gameMenuRequest.audioEnabled),
                onCheckedChange = {
                    onResult { putExtra(GameMenuContract.RESULT_ENABLE_AUDIO, !it) }
                },
            )
        }
        item {
            if (gameMenuRequest.fastForwardSupported) {
                LemuroidSettingsSwitch(
                    title = { TextMMD(text = stringResource(id = R.string.game_menu_fast_forward)) },
                    icon = {
                        MenuIcon(Icons.FastForward, stringResource(id = R.string.game_menu_fast_forward))
                    },
                    state = rememberMemoryBooleanSettingState(gameMenuRequest.fastForwardEnabled),
                    onCheckedChange = {
                        onResult { putExtra(GameMenuContract.RESULT_ENABLE_FAST_FORWARD, it) }
                    },
                )
            }
        }
        item {
            if (gameMenuRequest.numDisks > 1) {
                LemuroidSettingsList(
                    title = { TextMMD(text = stringResource(id = R.string.game_menu_change_disk_button)) },
                    items =
                        (1..gameMenuRequest.numDisks).map {
                            stringResource(
                                R.string.game_menu_change_disk_disk,
                                it,
                            )
                        },
                    useSelectedValueAsSubtitle = false,
                    icon = {
                        MenuIcon(Icons.Album, stringResource(id = R.string.game_menu_change_disk_button))
                    },
                    state = rememberMemoryIntSettingState(gameMenuRequest.currentDisk),
                    onItemSelected = { index, _ ->
                        onResult { putExtra(GameMenuContract.RESULT_CHANGE_DISK, index) }
                    },
                )
            }
        }
        item {
            LemuroidSettingsMenuLink(
                title = { TextMMD(text = stringResource(id = R.string.game_menu_edit_touch_controls)) },
                icon = {
                    MenuIcon(Icons.Gamepad, stringResource(id = R.string.game_menu_edit_touch_controls))
                },
                onClick = {
                    onResult { putExtra(GameMenuContract.RESULT_EDIT_TOUCH_CONTROLS, true) }
                },
            )
        }
        item {
            if (gameMenuRequest.advancedCoreOptions.isNotEmpty() || gameMenuRequest.coreOptions.isNotEmpty()) {
                LemuroidSettingsMenuLink(
                    title = { TextMMD(text = stringResource(id = R.string.game_menu_settings)) },
                    icon = {
                        MenuIcon(Icons.Settings, stringResource(id = R.string.game_menu_settings))
                    },
                    onClick = { navController.navigateToRoute(GameMenuRoute.OPTIONS) },
                )
            }
        }
        item {
            if (gameMenuRequest.allTiltConfigurations.isNotEmpty()) {
                val tiltConfigurationEntries =
                    gameMenuRequest.allTiltConfigurations
                        .map { TiltConfigurationMenuEntry.fromTiltConfiguration(it) }

                val selectedIndex =
                    gameMenuRequest.allTiltConfigurations
                        .indexOf(gameMenuRequest.currentTiltConfiguration)

                LemuroidSettingsList(
                    title = { TextMMD(text = stringResource(id = R.string.game_menu_tilt_sensor)) },
                    items = tiltConfigurationEntries.map { stringResource(it.descriptionId) },
                    useSelectedValueAsSubtitle = false,
                    icon = {
                        MenuIcon(Icons.Sensors, stringResource(id = R.string.game_menu_tilt_sensor))
                    },
                    state = rememberMemoryIntSettingState(selectedIndex),
                    onItemSelected = { index, _ ->
                        onResult {
                            putExtra(
                                GameMenuContract.RESULT_CHANGE_TILT_CONFIG,
                                tiltConfigurationEntries[index].configuration,
                            )
                        }
                    },
                )
            }
        }
    }
}

/**
 * A menu row's icon: the outlined Material Symbols glyph at 22 dp, centred in the 24 dp every
 * row gives its icon so the words line up, the same drawing and size the other apps use.
 */
@Composable
private fun MenuIcon(
    icon: ImageVector,
    description: String,
) {
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Icon(imageVector = icon, contentDescription = description, modifier = Modifier.size(22.dp))
    }
}
