package com.swordfish.lemuroid.app.mobile.feature.games

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.paging.compose.collectAsLazyPagingItems
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidEmptyView
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameListRow
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.ListJump
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.ListJumpBox
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.PagedColumn
import com.swordfish.lemuroid.lib.library.db.entity.Game

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GamesScreen(
    modifier: Modifier = Modifier,
    viewModel: GamesViewModel,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onGameFavoriteToggle: (Game, Boolean) -> Unit,
) {
    val games = viewModel.games.collectAsLazyPagingItems()

    if (games.itemCount == 0) {
        LemuroidEmptyView()
        return
    }

    // Sorted by title, so the jump down the edge is letters; the titles come whole from the
    // database, since the paged list holds only the screens near the one shown.
    val titles by viewModel.titles.collectAsState(initial = emptyList())
    val state = rememberLazyListState()
    val jump =
        remember(titles, games.itemCount) {
            if (titles.size == games.itemCount) ListJump.Letters(titles) else ListJump.Pages
        }

    // MMD's list rather than a plain LazyColumn. It brings the paging and the
    // scroll step Mudita tuned for this panel; its scrollbar is turned off, as it is in
    // every one of these apps. A smooth fling is the worst thing to ask of this screen:
    // scrolling here is display-bound, not CPU-bound, so the only lever that helps is
    // producing fewer frames -- which is why e-ink readers page instead of scrolling.
    ListJumpBox(state, jump, modifier) {
        PagedColumn(Modifier.fillMaxSize(), state = state) {
            items(games.itemCount, key = { games[it]?.id ?: it }) { index ->
                val game = games[index] ?: return@items

                LemuroidGameListRow(
                    modifier = Modifier.animateItem(),
                    game = game,
                    onClick = { onGameClick(game) },
                    onLongClick = { onGameLongClick(game) },
                    onFavoriteToggle = { isFavorite -> onGameFavoriteToggle(game, isFavorite) },
                )
            }
        }
    }
}
