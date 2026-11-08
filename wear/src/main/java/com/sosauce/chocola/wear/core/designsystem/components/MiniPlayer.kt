@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.chocola.wear.core.designsystem.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sosauce.chocola.shared.WatchPlayerState
import com.sosauce.chocola.wear.R
import com.sosauce.chocola.wear.core.domain.player.PlayerActions
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile

@Composable
fun MiniPlayer(
    player: WatchPlayerState,
    onClick: () -> Unit,
    onHandlePlayerActions: (PlayerActions) -> Unit,
    modifier: Modifier = Modifier
) {
    val track = player.track ?: return

    CuteListItem(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
        leadingContent = {
            TrackArtwork(
                icon = R.drawable.music_note,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            IconButton(
                onClick = { onHandlePlayerActions(PlayerActions.PlayOrPause) },
                shapes = IconButtonDefaults.shapes()
            ) {
                AnimatedDrawable(
                    drawable = AnimatedDrawableFile.PLAY,
                    atEnd = player.isPlaying
                )
            }
        }
    ) {
        Text(
            text = track.title,
            maxLines = 1,
            style = MaterialTheme.typography.titleSmallEmphasized,
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
