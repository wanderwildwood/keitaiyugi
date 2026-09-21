package com.swordfish.lemuroid.app.shared.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelRetroGameView

@Composable
fun BaseGameScreen(
    viewModel: BaseGameScreenViewModel,
    gameScreen: @Composable (BaseGameScreenViewModel) -> Unit,
) {
    val gameState =
        viewModel.getGameState()
            .collectAsState(GameViewModelRetroGameView.GameState.Uninitialized)
            .value

    val isGameReady =
        gameState is GameViewModelRetroGameView.GameState.Loaded ||
            gameState is GameViewModelRetroGameView.GameState.Ready

    if (isGameReady) {
        gameScreen(viewModel)
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.wrapContentSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextMMD(text = stringResource(R.string.working))

                val message = if (gameState is GameViewModelRetroGameView.GameState.Loading) gameState.message else null
                if (message != null) {
                    TextMMD(text = message!!, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}
