package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.utils.games.GameUtils
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun LemuroidGameTexts(
    modifier: Modifier = Modifier,
    game: Game,
) {
    val context = LocalContext.current
    val subtitle =
        remember(game.id) {
            GameUtils.getGameSubtitle(context, game)
        }

    LemuroidTexts(modifier, game.title, subtitle)
}

@Composable
fun LemuroidTexts(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
) {
    Column(
        modifier = modifier.padding(8.dp),
    ) {
        TextMMD(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextMMD(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** One line above a list of games: the menu behind a game is a long press, and nothing shows it. */
@Composable
fun LongPressHint(modifier: Modifier = Modifier) {
    TextMMD(
        text = stringResource(R.string.long_press_hint),
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
