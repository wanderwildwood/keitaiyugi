package com.swordfish.lemuroid.app.mobile.feature.home

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.cards.CardMMD
import com.mudita.mmd.components.lazy.LazyRowMMD
import com.mudita.mmd.components.text.TextMMD
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameCard
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.PagedColumn
import com.swordfish.lemuroid.app.utils.android.ComposableLifecycle
import com.swordfish.lemuroid.common.displayDetailsSettingsScreen
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenCoreSelection: () -> Unit,
) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext

    // The folder the games are read from, once one has been chosen: with it, an empty library is
    // "nothing found in that folder", not a request to pick one.
    var folderName by remember { mutableStateOf(chosenFolderName(applicationContext)) }

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                viewModel.updatePermissions(applicationContext)
                folderName = chosenFolderName(applicationContext)
            }
            else -> { }
        }
    }

    val permissionsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (!isGranted) {
                context.displayDetailsSettingsScreen()
            }
        }

    val state = viewModel.getViewStates().collectAsState(HomeViewModel.UIState())
    HomeScreen(
        modifier,
        state.value,
        folderName,
        onGameClick,
        onGameLongClick,
        onOpenCoreSelection,
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return@HomeScreen
            }

            permissionsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        { permissionsLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        { viewModel.changeLocalStorageFolder(context) },
    ) // TODO COMPOSE We need to understand what's going to happen here.
}

@Suppress("DEPRECATION")
private fun chosenFolderName(context: Context): String? {
    val uri =
        SharedPreferencesHelper.getLegacySharedPreferences(context)
            .getString(context.getString(com.swordfish.lemuroid.lib.R.string.pref_key_extenral_folder), null)
    if (uri.isNullOrEmpty()) return null
    return runCatching { DocumentFile.fromTreeUri(context, Uri.parse(uri))?.name }.getOrNull()
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeViewModel.UIState,
    folderName: String?,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenCoreSelection: () -> Unit,
    onEnableNotificationsClicked: () -> Unit,
    onEnableMicrophoneClicked: () -> Unit,
    onSetDirectoryClicked: () -> Unit,
) {
    // Paged, not scrolled: MMD's list steps four rows to a swipe and stops, with no
    // scrollbar beside it. Nothing on this panel coasts.
    //
    // Every block is added only when it has something to show. An item that draws nothing
    // still gets the list's 16dp gap, and with no notices and no recent or favourite games
    // five of them stacked an empty band above Discover.
    PagedColumn(
        modifier =
            modifier
                .padding(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.showNoNotificationPermissionCard) {
            item {
                HomeNotification(
                    title = stringResource(R.string.home_notification_title),
                    message = stringResource(R.string.home_notification_message),
                    actionId = R.string.home_notification_action,
                    onAction = onEnableNotificationsClicked,
                )
            }
        }
        if (state.showNoGamesCard) {
            item {
                if (folderName == null) {
                    HomeNotification(
                        title = stringResource(R.string.home_empty_title),
                        message = stringResource(R.string.home_empty_message),
                        actionId = R.string.home_empty_action,
                        onAction = onSetDirectoryClicked,
                        enabled = !state.indexInProgress,
                    )
                } else if (state.indexInProgress) {
                    HomeNotification(
                        title = stringResource(R.string.home_scanning_title, folderName),
                        message = stringResource(R.string.home_scanning_message),
                        actionId = R.string.home_empty_action,
                        onAction = onSetDirectoryClicked,
                        enabled = false,
                    )
                } else {
                    HomeNotification(
                        title = stringResource(R.string.home_none_found_title, folderName),
                        message = stringResource(R.string.home_none_found_message),
                        actionId = R.string.home_empty_action,
                        onAction = onSetDirectoryClicked,
                    )
                }
            }
        }
        if (state.showNoMicrophonePermissionCard) {
            item {
                HomeNotification(
                    title = stringResource(R.string.home_microphone_title),
                    message = stringResource(R.string.home_microphone_message),
                    actionId = R.string.home_microphone_action,
                    onAction = onEnableMicrophoneClicked,
                )
            }
        }
        if (state.showDesmumeDeprecatedCard) {
            item {
                HomeNotification(
                    title = stringResource(R.string.home_notification_desmume_deprecated_title),
                    message = stringResource(R.string.home_notification_desmume_deprecated_message),
                    actionId = R.string.home_notification_desmume_deprecated_action,
                    onAction = onOpenCoreSelection,
                )
            }
        }
        if (state.recentGames.isNotEmpty()) {
            item {
                HomeRow(
                    stringResource(id = R.string.recent),
                    state.recentGames,
                    onGameClicked,
                    onGameLongClick,
                )
            }
        }
        if (state.favoritesGames.isNotEmpty()) {
            item {
                HomeRow(
                    stringResource(id = R.string.favorites),
                    state.favoritesGames,
                    onGameClicked,
                    onGameLongClick,
                )
            }
        }
        if (state.discoveryGames.isNotEmpty()) {
            item {
                HomeRow(
                    stringResource(id = R.string.discover),
                    state.discoveryGames,
                    onGameClicked,
                    onGameLongClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeRow(
    title: String,
    games: List<Game>,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
) {
    if (games.isEmpty()) {
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        TextMMD(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        )
        // MMD's row gives its list weight(1f), which fills whatever height it is given --
        // and inside the home screen's vertical list it is given none, so the cards came
        // out zero high: an empty "Discover" row on every home screen from 0.1.2 to 0.1.3.
        // So the row is told its height: one card (a square picture, a line of title, a
        // line of subtitle) plus the row's padding. No rail: a swipe moves it.
        LazyRowMMD(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(homeRowHeight()),
            horizontalArrangement = Arrangement.spacedBy(ROW_SPACING),
            contentPadding = PaddingValues(ROW_PADDING),
            isScrollbarVisible = false,
        ) {
            items(games.size, key = { games[it].id }) { index ->
                val game = games[index]
                LemuroidGameCard(
                    modifier = Modifier.widthIn(0.dp, CARD_WIDTH),
                    game = game,
                    onClick = { onGameClicked(game) },
                    onLongClick = { onGameLongClick(game) },
                )
            }
        }
    }
}

private val CARD_WIDTH = 144.dp
private val ROW_PADDING = 16.dp
private val ROW_SPACING = 16.dp
private val TEXT_PADDING = 8.dp

@Composable
private fun homeRowHeight(): Dp {
    val type = MaterialTheme.typography
    val density = LocalDensity.current

    fun lineOf(style: TextStyle): Dp {
        val line = if (style.lineHeight.isSp) style.lineHeight else style.fontSize * 1.25f
        return with(density) { line.toDp() }
    }
    return ROW_PADDING * 2 + CARD_WIDTH + TEXT_PADDING * 2 +
        lineOf(type.titleSmall) + lineOf(type.labelSmall)
}

@Composable
private fun HomeNotification(
    title: String,
    message: String,
    actionId: Int,
    enabled: Boolean = true,
    onAction: () -> Unit = { },
) {
    CardMMD(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextMMD(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            TextMMD(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButtonMMD(
                modifier = Modifier.align(Alignment.End),
                onClick = onAction,
                enabled = enabled,
            ) {
                TextMMD(stringResource(id = actionId))
            }
        }
    }
}
