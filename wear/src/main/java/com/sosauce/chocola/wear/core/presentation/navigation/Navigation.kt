package com.sosauce.chocola.wear.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.sosauce.chocola.wear.core.domain.player.PlayerActions
import com.sosauce.chocola.wear.core.presentation.player.MusicState
import com.sosauce.chocola.wear.feature.folders.FolderDetailsScreen
import com.sosauce.chocola.wear.feature.folders.FoldersScreen
import com.sosauce.chocola.wear.feature.nowplaying.NowPlayingScreen

@Composable
fun Navigation(
    state: MusicState,
    onHandlePlayerActions: (PlayerActions) -> Unit
) {
    val backStack = rememberNavBackStack(Screen.Folders)
    val navigateToNowPlaying = {
        if (backStack.lastOrNull() !is Screen.NowPlaying) backStack.add(Screen.NowPlaying)
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Screen.Folders> {
                FoldersScreen(
                    state = state,
                    onNavigateToFolder = { backStack.add(Screen.FolderDetails(it)) },
                    onNavigateToNowPlaying = navigateToNowPlaying,
                    onHandlePlayerActions = onHandlePlayerActions
                )
            }
            entry<Screen.FolderDetails> { screen ->
                FolderDetailsScreen(
                    folder = state.folders.find { it.path == screen.path },
                    player = state.player,
                    onHandlePlayerActions = onHandlePlayerActions,
                    onNavigateToNowPlaying = navigateToNowPlaying
                )
            }
            entry<Screen.NowPlaying> {
                NowPlayingScreen(
                    state = state,
                    onHandlePlayerActions = onHandlePlayerActions
                )
            }
        }
    )
}
