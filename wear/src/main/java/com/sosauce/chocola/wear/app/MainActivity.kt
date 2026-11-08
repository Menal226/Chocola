package com.sosauce.chocola.wear.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import com.sosauce.chocola.wear.core.designsystem.theme.ChocolaTheme
import com.sosauce.chocola.wear.core.domain.player.PlayerActions
import com.sosauce.chocola.wear.core.presentation.navigation.Navigation
import com.sosauce.chocola.wear.core.presentation.player.MusicViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = koinViewModel<MusicViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            LifecycleResumeEffect(Unit) {
                viewModel.handlePlayerActions(PlayerActions.Sync)
                onPauseOrDispose { }
            }

            ChocolaTheme(
                appearance = state.appearance,
                artImageBitmap = state.artwork
            ) {
                AppScaffold {
                    Navigation(
                        state = state,
                        onHandlePlayerActions = viewModel::handlePlayerActions,
                        onLoadArtwork = viewModel::artwork
                    )
                }
            }
        }
    }
}
