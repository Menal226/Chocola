@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.chocola.wear.feature.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.sosauce.chocola.wear.R
import com.sosauce.chocola.wear.core.designsystem.components.CompactNoXFound
import com.sosauce.chocola.wear.core.designsystem.components.CuteListItem
import com.sosauce.chocola.wear.core.designsystem.components.MiniPlayer
import com.sosauce.chocola.wear.core.designsystem.components.TrackArtwork
import com.sosauce.chocola.wear.core.designsystem.components.listItemTransformation
import com.sosauce.chocola.wear.core.domain.player.PlayerActions
import com.sosauce.chocola.wear.core.presentation.player.MusicState

@Composable
fun FoldersScreen(
    state: MusicState,
    onNavigateToFolder: (String) -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onHandlePlayerActions: (PlayerActions) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val layoutDirection = LocalLayoutDirection.current

    when {
        !state.isPhoneConnected -> ScreenScaffold {
            CompactNoXFound(
                headlineText = R.string.watch_phone_disconnected,
                bodyText = R.string.watch_phone_disconnected_desc,
                icon = R.drawable.warning
            )
        }

        state.isLoading -> ScreenScaffold {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        }

        state.folders.isEmpty() -> ScreenScaffold {
            CompactNoXFound(
                headlineText = R.string.no_music_title,
                bodyText = R.string.no_music_tip,
                icon = R.drawable.music_note
            )
        }

        else -> ScreenScaffold(scrollState = listState) { contentPadding ->
            TransformingLazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    top = contentPadding.calculateTopPadding() + 28.dp,
                    end = contentPadding.calculateEndPadding(layoutDirection),
                    bottom = contentPadding.calculateBottomPadding()
                ),
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            ) {
                if (state.player.track != null) {
                    item {
                        MiniPlayer(
                            player = state.player,
                            onClick = onNavigateToNowPlaying,
                            onHandlePlayerActions = onHandlePlayerActions,
                            modifier = Modifier.listItemTransformation(this, transformationSpec)
                        )
                    }
                }

                items(
                    items = state.folders,
                    key = { it.path }
                ) { folder ->
                    CuteListItem(
                        modifier = Modifier.listItemTransformation(this, transformationSpec),
                        onClick = { onNavigateToFolder(folder.path) },
                        leadingContent = {
                            TrackArtwork(
                                icon = R.drawable.folder_rounded,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Text(
                            text = folder.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMediumEmphasized
                        )
                        Text(
                            text = pluralStringResource(R.plurals.tracks_plural, folder.tracks.size, folder.tracks.size),
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}
