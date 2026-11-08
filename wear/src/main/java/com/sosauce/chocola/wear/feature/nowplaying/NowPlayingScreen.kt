@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.chocola.wear.feature.nowplaying

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.material3.ScreenScaffold
import com.sosauce.chocola.shared.WatchPlayerState
import com.sosauce.chocola.wear.R
import com.sosauce.chocola.wear.core.designsystem.components.CompactNoXFound
import com.sosauce.chocola.wear.core.domain.player.PlayerActions
import com.sosauce.chocola.wear.core.presentation.player.MusicState
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private const val SEEK_STEP_PIXELS = 48f
private const val SEEK_STEP_MS = 5000L

@Composable
fun NowPlayingScreen(
    state: MusicState,
    onHandlePlayerActions: (PlayerActions) -> Unit
) {
    val player = state.player
    val track = player.track
    var rotaryDelta by remember { mutableFloatStateOf(0f) }
    var seekPosition by remember { mutableStateOf<Long?>(null) }
    val playbackPosition = rememberPosition(player)
    val position = seekPosition ?: playbackPosition
    val progress by animateFloatAsState(
        if (player.duration > 0) position.toFloat() / player.duration else 0f
    )

    LaunchedEffect(player) { seekPosition = null }

    LaunchedEffect(seekPosition) {
        val target = seekPosition ?: return@LaunchedEffect
        delay(400)
        onHandlePlayerActions(PlayerActions.SeekTo(target))
        delay(2000)
        seekPosition = null
    }

    ScreenScaffold(
        timeText = {},
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .onRotaryScrollEvent { event ->
                if (track == null || player.duration <= 0) return@onRotaryScrollEvent false
                rotaryDelta += event.verticalScrollPixels
                val steps = (rotaryDelta / SEEK_STEP_PIXELS).toInt()
                if (steps != 0) {
                    rotaryDelta -= steps * SEEK_STEP_PIXELS
                    seekPosition = (position + steps * SEEK_STEP_MS).coerceIn(0L, player.duration)
                }
                true
            }
            .requestFocusOnHierarchyActive()
            .focusable()
    ) {
        if (track == null) {
            CompactNoXFound(
                headlineText = R.string.now_playing,
                bodyText = R.string.watch_nothing_playing,
                icon = R.drawable.music_note
            )
            return@ScreenScaffold
        }

        PlaybackRing(
            progress = progress,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-12).dp)
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${position.formatTime()} / ${player.duration.formatTime()}",
                style = MaterialTheme.typography.labelLargeEmphasized.copy(
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(Modifier.height(6.dp))
            AnimatedContent(
                targetState = track.title,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { title ->
                Text(
                    text = title,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmallEmphasized.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee()
                )
            }
            Text(
                text = track.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMediumEmphasized.copy(
                    color = MaterialTheme.colorScheme.secondary
                )
            )
            Spacer(Modifier.height(12.dp))
            ActionButtonsRow(
                isPlaying = player.isPlaying,
                onHandlePlayerActions = onHandlePlayerActions
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    isPlaying: Boolean,
    onHandlePlayerActions: (PlayerActions) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledIconButton(
            onClick = { onHandlePlayerActions(PlayerActions.SeekToPreviousMusic) },
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = contentColorFor(MaterialTheme.colorScheme.tertiary)
            ),
            modifier = Modifier.size(width = 46.dp, height = 52.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = stringResource(R.string.previous_song)
            )
        }
        FilledIconButton(
            onClick = { onHandlePlayerActions(PlayerActions.PlayOrPause) },
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = contentColorFor(MaterialTheme.colorScheme.primary)
            ),
            modifier = Modifier.size(width = 72.dp, height = 52.dp)
        ) {
            AnimatedDrawable(
                drawable = AnimatedDrawableFile.PLAY,
                atEnd = isPlaying
            )
        }
        FilledIconButton(
            onClick = { onHandlePlayerActions(PlayerActions.SeekToNextMusic) },
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = contentColorFor(MaterialTheme.colorScheme.tertiary)
            ),
            modifier = Modifier.size(width = 46.dp, height = 52.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = stringResource(R.string.next_song)
            )
        }
    }
}

@Composable
private fun PlaybackRing(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.secondaryContainer

    Canvas(modifier = modifier) {
        val strokeWidth = 5.dp.toPx()
        val thumbRadius = 6.dp.toPx()
        val inset = thumbRadius
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        val sweep = 360f * progress.coerceIn(0f, 1f)

        drawArc(
            color = inactiveColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(strokeWidth)
        )
        drawArc(
            color = activeColor,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )

        val angle = Math.toRadians((sweep - 90f).toDouble())
        val radius = arcSize.width / 2
        drawCircle(
            color = activeColor,
            radius = thumbRadius,
            center = Offset(
                x = center.x + radius * cos(angle).toFloat(),
                y = center.y + radius * sin(angle).toFloat()
            )
        )
    }
}

@Composable
private fun rememberPosition(player: WatchPlayerState): Long {
    var position by remember { mutableLongStateOf(player.position) }

    LaunchedEffect(player) {
        while (true) {
            val elapsed = if (player.isPlaying) System.currentTimeMillis() - player.updatedAt else 0L
            position = (player.position + elapsed.coerceAtLeast(0L)).coerceIn(0L, player.duration.coerceAtLeast(0L))
            if (!player.isPlaying) break
            delay(500)
        }
    }

    return position
}

private fun Long.formatTime(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}
