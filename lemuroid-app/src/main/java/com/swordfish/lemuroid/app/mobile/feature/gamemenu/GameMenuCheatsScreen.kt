package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.alorma.compose.settings.storage.memory.rememberMemoryBooleanSettingState
import com.mudita.mmd.components.text.TextMMD
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.PagedColumn
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import com.swordfish.lemuroid.lib.box.BoxKey
import com.swordfish.lemuroid.lib.box.Cheat
import com.swordfish.lemuroid.lib.box.CheatStore

/**
 * The game's cheats, each with its own switch. A switch is kept for the game from then on; the
 * game takes up the change when the menu closes.
 */
@Composable
fun GameMenuCheatsScreen(
    key: BoxKey,
    cheats: List<Cheat>,
    store: CheatStore,
) {
    val enabled = store.enabledCodes(key)
    // The box often has the same cheat as a Game Genie code and as a GameShark code. Where a
    // name comes up more than once, each says which kind it is.
    val repeated = cheats.groupingBy { it.description }.eachCount().filterValues { it > 1 }.keys
    PagedColumn {
        cheats.forEach { cheat ->
            item {
                LemuroidSettingsSwitch(
                    title = { TextMMD(text = cheat.description) },
                    subtitle =
                        if (cheat.description in repeated) {
                            {
                                TextMMD(
                                    text =
                                        stringResource(
                                            if (cheat.isGameGenie) {
                                                R.string.cheat_kind_game_genie
                                            } else {
                                                R.string.cheat_kind_gameshark
                                            },
                                            cheat.code,
                                        ),
                                )
                            }
                        } else {
                            null
                        },
                    state = rememberMemoryBooleanSettingState(cheat.code in enabled),
                    onCheckedChange = { store.setEnabled(key, cheat, it) },
                )
            }
        }
    }
}
