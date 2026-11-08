@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.chocola.wear.core.designsystem.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.dynamicColorScheme
import com.kmpalette.rememberDominantColorState
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicMaterialThemeState
import com.sosauce.chocola.shared.WatchAppearance
import com.sosauce.chocola.wear.R
import androidx.wear.compose.material3.MaterialTheme as WearMaterialTheme
import androidx.wear.compose.material3.MotionScheme as WearMotionScheme
import androidx.wear.compose.material3.Typography as WearTypography

@Composable
fun ChocolaTheme(
    appearance: WatchAppearance,
    artImageBitmap: ImageBitmap? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemPrimary = remember { dynamicColorScheme(context)?.primary ?: ColorScheme().primary }
    val seedColor by animateColorAsState(
        rememberSeedColor(
            defaultColor = appearance.seedColor?.let(::Color) ?: systemPrimary,
            useMaterialArt = appearance.useArtTheme,
            imageBitmap = artImageBitmap
        )
    )
    val paletteStyle = remember(appearance.paletteStyle) {
        PaletteStyle.entries.find { it.name == appearance.paletteStyle } ?: PaletteStyle.Fidelity
    }

    val state = rememberDynamicMaterialThemeState(
        seedColor = seedColor,
        isDark = true,
        isAmoled = appearance.isAmoled,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = paletteStyle
    )

    DynamicMaterialExpressiveTheme(
        state = state,
        motionScheme = MotionScheme.expressive(),
        typography = if (appearance.useSystemFont) Typography() else GoogleSansFlexTypography
    ) {
        val colorScheme = MaterialTheme.colorScheme.toWearColorScheme()
        WearMaterialTheme(
            colorScheme = colorScheme,
            typography = if (appearance.useSystemFont) WearTypography() else GoogleSansFlexWearTypography,
            motionScheme = WearMotionScheme.expressive()
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                content = content
            )
        }
    }
}

@Composable
private fun rememberSeedColor(
    defaultColor: Color,
    useMaterialArt: Boolean,
    imageBitmap: ImageBitmap?
): Color {
    if (!useMaterialArt) return defaultColor
    if (imageBitmap == null) return defaultColor

    var seedColor by remember { mutableStateOf(defaultColor) }
    val dominantColorState = rememberDominantColorState()

    LaunchedEffect(imageBitmap) {
        dominantColorState.updateFrom(imageBitmap)
        seedColor = dominantColorState.color
    }

    return seedColor
}

private fun androidx.compose.material3.ColorScheme.toWearColorScheme() = ColorScheme(
    primary = primary,
    primaryDim = primary,
    primaryContainer = primaryContainer,
    onPrimary = onPrimary,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    secondaryDim = secondary,
    secondaryContainer = secondaryContainer,
    onSecondary = onSecondary,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    tertiaryDim = tertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiary = onTertiary,
    onTertiaryContainer = onTertiaryContainer,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    onSurface = onSurface,
    onSurfaceVariant = onSurfaceVariant,
    outline = outline,
    outlineVariant = outlineVariant,
    background = background,
    onBackground = onBackground,
    error = error,
    errorDim = error,
    errorContainer = errorContainer,
    onError = onError,
    onErrorContainer = onErrorContainer
)

val googleSansFlexFontFamily = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("ROND", 100f),
            FontVariation.weight(750),
            FontVariation.width(110f)
        )
    )
)

private fun TextStyle.sansFlex() = copy(fontFamily = googleSansFlexFontFamily)

val GoogleSansFlexTypography = Typography().run {
    copy(
        displayLarge = displayLarge.sansFlex(),
        displayMedium = displayMedium.sansFlex(),
        displaySmall = displaySmall.sansFlex(),
        headlineLarge = headlineLarge.sansFlex(),
        headlineMedium = headlineMedium.sansFlex(),
        headlineSmall = headlineSmall.sansFlex(),
        titleLarge = titleLarge.sansFlex(),
        titleMedium = titleMedium.sansFlex(),
        titleSmall = titleSmall.sansFlex(),
        bodyLarge = bodyLarge.sansFlex(),
        bodyMedium = bodyMedium.sansFlex(),
        bodySmall = bodySmall.sansFlex(),
        labelLarge = labelLarge.sansFlex(),
        labelMedium = labelMedium.sansFlex(),
        labelSmall = labelSmall.sansFlex(),
        displayLargeEmphasized = displayLargeEmphasized.sansFlex(),
        displayMediumEmphasized = displayMediumEmphasized.sansFlex(),
        displaySmallEmphasized = displaySmallEmphasized.sansFlex(),
        headlineLargeEmphasized = headlineLargeEmphasized.sansFlex(),
        headlineMediumEmphasized = headlineMediumEmphasized.sansFlex(),
        headlineSmallEmphasized = headlineSmallEmphasized.sansFlex(),
        titleLargeEmphasized = titleLargeEmphasized.sansFlex(),
        titleMediumEmphasized = titleMediumEmphasized.sansFlex(),
        titleSmallEmphasized = titleSmallEmphasized.sansFlex(),
        bodyLargeEmphasized = bodyLargeEmphasized.sansFlex(),
        bodyMediumEmphasized = bodyMediumEmphasized.sansFlex(),
        bodySmallEmphasized = bodySmallEmphasized.sansFlex(),
        labelLargeEmphasized = labelLargeEmphasized.sansFlex(),
        labelMediumEmphasized = labelMediumEmphasized.sansFlex(),
        labelSmallEmphasized = labelSmallEmphasized.sansFlex()
    )
}

val GoogleSansFlexWearTypography = WearTypography().run {
    copy(
        displayLarge = displayLarge.sansFlex(),
        displayMedium = displayMedium.sansFlex(),
        displaySmall = displaySmall.sansFlex(),
        titleLarge = titleLarge.sansFlex(),
        titleMedium = titleMedium.sansFlex(),
        titleSmall = titleSmall.sansFlex(),
        labelLarge = labelLarge.sansFlex(),
        labelMedium = labelMedium.sansFlex(),
        labelSmall = labelSmall.sansFlex(),
        bodyLarge = bodyLarge.sansFlex(),
        bodyMedium = bodyMedium.sansFlex(),
        bodySmall = bodySmall.sansFlex(),
        bodyExtraSmall = bodyExtraSmall.sansFlex(),
        numeralExtraLarge = numeralExtraLarge.sansFlex(),
        numeralLarge = numeralLarge.sansFlex(),
        numeralMedium = numeralMedium.sansFlex(),
        numeralSmall = numeralSmall.sansFlex(),
        numeralExtraSmall = numeralExtraSmall.sansFlex()
    )
}
