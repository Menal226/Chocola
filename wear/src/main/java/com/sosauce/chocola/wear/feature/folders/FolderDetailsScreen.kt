@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.chocola.wear.feature.folders

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.sosauce.chocola.shared.WatchFolder
import com.sosauce.chocola.shared.WatchPlayerState
import com.sosauce.chocola.wear.R
import com.sosauce.chocola.wear.core.designsystem.components.CuteListItem
import com.sosauce.chocola.wear.core.designsystem.components.TrackArtwork
import com.sosauce.chocola.wear.core.designsystem.components.listItemTransformation
import com.sosauce.chocola.wear.core.domain.player.PlayerActions

@Composable
fun FolderDetailsScreen(
    folder: WatchFolder?,
    player: WatchPlayerState,
    onHandlePlayerActions: (PlayerActions) -> Unit,
    onLoadArtwork: suspend (String) -> ImageBitmap?,
    onNavigateToNowPlaying: () -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    if (folder == null) {
        ScreenScaffold {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        }
        return
    }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        ) {
            item {
                FolderHeader(
                    folder = folder,
                    onShuffle = {
                        onHandlePlayerActions(PlayerActions.PlayFolder(folder.path, shuffle = true))
                        onNavigateToNowPlaying()
                    },
                    onPlay = {
                        onHandlePlayerActions(PlayerActions.PlayFolder(folder.path))
                        onNavigateToNowPlaying()
                    },
                    modifier = Modifier.listItemTransformation(this, transformationSpec)
                )
            }

            items(folder.tracks) { track ->
                val isActive = player.track?.mediaId == track.mediaId
                val background by animateColorAsState(
                    if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent
                )
                val artwork by produceState<ImageBitmap?>(null, track.mediaId) {
                    value = onLoadArtwork(track.mediaId)
                }

                CuteListItem(
                    modifier = Modifier.listItemTransformation(this, transformationSpec),
                    backgroundColor = background,
                    onClick = {
                        onHandlePlayerActions(PlayerActions.PlayFolder(folder.path, track.mediaId))
                        onNavigateToNowPlaying()
                    },
                    leadingContent = {
                        TrackArtwork(
                            icon = R.drawable.music_note,
                            artwork = artwork,
                            tint = if (isActive) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                ) {
                    Text(
                        text = track.title,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = if (isActive) MaterialTheme.colorScheme.primary else Color.Unspecified,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = track.artist,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmallEmphasized.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderHeader(
    folder: WatchFolder,
    onShuffle: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = folder.name,
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLargeEmphasized,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .basicMarquee()
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            FilledTonalButton(
                onClick = onShuffle,
                shapes = ButtonDefaults.shapes(),
                contentPadding = ButtonDefaults.SmallContentPadding,
                modifier = Modifier
                    .weight(.6f)
                    .height(44.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = stringResource(R.string.shuffle)
                )
            }
            Button(
                onClick = onPlay,
                shapes = ButtonDefaults.shapes(),
                contentPadding = ButtonDefaults.SmallContentPadding,
                modifier = Modifier
                    .weight(1.5f)
                    .height(44.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.widget_play),
                    contentDescription = null
                )
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.play))
            }
        }
    }
}
