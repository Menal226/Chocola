package com.sosauce.chocola.wear.core.designsystem.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sv.lib.squircleshape.CornerSmoothing
import sv.lib.squircleshape.SquircleShape

@Composable
fun TrackArtwork(
    icon: Int,
    modifier: Modifier = Modifier,
    artwork: ImageBitmap? = null,
    size: Dp = 40.dp,
    tint: Color = LocalContentColor.current
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(SquircleShape(smoothing = CornerSmoothing.Full))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(artwork) { bitmap ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(size / 2)
                    )
                }
            }
        }
    }
}
