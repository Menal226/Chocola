package com.sosauce.chocola.wear.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sosauce.nekobites.components.NoXFound

@Composable
fun CompactNoXFound(
    @StringRes headlineText: Int,
    @StringRes bodyText: Int,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(density.density * 0.55f, density.fontScale)
        ) {
            NoXFound(
                headlineText = headlineText,
                bodyText = bodyText,
                icon = icon
            )
        }
    }
}
