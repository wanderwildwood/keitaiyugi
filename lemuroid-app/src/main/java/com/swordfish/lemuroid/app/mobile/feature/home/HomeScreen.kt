package com.swordfish.lemuroid.app.mobile.feature.home

import android.Manifest
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.Lifecycle
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.cards.CardMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.lazy.LazyRowMMD
import com.mudita.mmd.components.text.TextMMD
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameCard
import com.swordfish.lemuroid.app.utils.android.ComposableLifecycle
import com.swordfish.lemuroid.common.displayDetailsSettingsScreen
import com.swordfish.lemuroid.lib.library.db.entity.Game

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

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                viewModel.updatePermissions(applicationContext)
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

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeViewModel.UIState,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenCoreSelection: () -> Unit,
    onEnableNotificationsClicked: () -> Unit,
    onEnableMicrophoneClicked: () -> Unit,
    onSetDirectoryClicked: () -> Unit,
) {
    // Paged, not scrolled: MMD's list steps four rows to a swipe and stops, and brings
    // the chevron rail with it. Nothing on this panel coasts.
    //
    // Every block is added only when it has something to show. An item that draws nothing
    // still gets the list's 16dp gap, and with no notices and no recent or favourite games
    // five of them stacked an empty band above Discover.
    LazyColumnMMD(
        modifier =
            modifier
                .padding(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.showNoNotificationPermissionCard) {
            item {
                HomeNotification(
                    titleId = R.string.home_notification_title,
                    messageId = R.string.home_notification_message,
                    actionId = R.string.home_notification_action,
                    onAction = onEnableNotificationsClicked,
                )
            }
        }
        if (state.showNoGamesCard) {
            item {
                HomeNotification(
                    titleId = R.string.home_empty_title,
                    messageId = R.string.home_empty_message,
                    actionId = R.string.home_empty_action,
                    onAction = onSetDirectoryClicked,
                    enabled = !state.indexInProgress,
                )
            }
        }
        if (state.showNoMicrophonePermissionCard) {
            item {
                HomeNotification(
                    titleId = R.string.home_microphone_title,
                    messageId = R.string.home_microphone_message,
                    actionId = R.string.home_microphone_action,
                    onAction = onEnableMicrophoneClicked,
                )
            }
        }
        if (state.showDesmumeDeprecatedCard) {
            item {
                HomeNotification(
                    titleId = R.string.home_notification_desmume_deprecated_title,
                    messageId = R.string.home_notification_desmume_deprecated_message,
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
        // MMD's row gives its list weight(1f) above the rail, which fills whatever height it
        // is given -- and inside the home screen's vertical list it is given none, so the
        // cards came out zero high and only the rail was drawn: an empty "Discover" row on
        // every home screen from 0.1.2 to 0.1.3. So the row is told its height: one card
        // (a square picture, a line of title, a line of subtitle) plus the row's padding
        // and MMD's rail beneath.
        LazyRowMMD(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(homeRowHeight(scrolls = rowScrolls(games.size))),
            horizontalArrangement = Arrangement.spacedBy(ROW_SPACING),
            contentPadding = PaddingValues(ROW_PADDING),
        ) {
            items(games.size, key = { games[it].id }) { index ->
                val game = games[index]
                LemuroidGameCard(
                    modifier =
                        Modifier
                            .widthIn(0.dp, CARD_WIDTH)
                            .animateItem(),
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

// MMD's rail under a scrollable row: a 24dp chevron with 8dp above and below. The size is
// internal to MMD, so it is restated here.
private val RAIL_HEIGHT = 24.dp + 8.dp * 2

// Whether a row of this many cards is wider than the screen, which is when MMD draws its
// rail beneath it. A row that fits keeps no room for a rail it does not have.
@Composable
private fun rowScrolls(count: Int): Boolean {
    val screen = LocalConfiguration.current.screenWidthDp.dp
    val cards = CARD_WIDTH * count + ROW_SPACING * (count - 1) + ROW_PADDING * 2
    return cards > screen
}

@Composable
private fun homeRowHeight(scrolls: Boolean): Dp {
    val type = MaterialTheme.typography
    val density = LocalDensity.current

    fun lineOf(style: TextStyle): Dp {
        val line = if (style.lineHeight.isSp) style.lineHeight else style.fontSize * 1.25f
        return with(density) { line.toDp() }
    }
    val rail = if (scrolls) RAIL_HEIGHT else 0.dp
    return ROW_PADDING * 2 + CARD_WIDTH + TEXT_PADDING * 2 +
        lineOf(type.titleSmall) + lineOf(type.labelSmall) + rail
}

@Composable
private fun HomeNotification(
    titleId: Int,
    messageId: Int,
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
                text = stringResource(titleId),
                style = MaterialTheme.typography.titleMedium,
            )
            TextMMD(
                text = stringResource(messageId),
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
