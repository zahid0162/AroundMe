package com.zahid.aroundme.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.zahid.aroundme.domain.model.ThemePreference

data class AroundMeExtendedColors(
    val openStatus: Color,
    val openStatusContainer: Color,
    val closedStatus: Color,
    val closedStatusContainer: Color,
    val ratingStar: Color,
    val mapBackdrop: Color,
    val subtleDivider: Color
)

val LocalAroundMeColors = staticCompositionLocalOf {
    AroundMeExtendedColors(
        openStatus = AroundMePalette.OpenGreen,
        openStatusContainer = AroundMePalette.OpenGreenContainer,
        closedStatus = AroundMePalette.ClosedRed,
        closedStatusContainer = AroundMePalette.ClosedRedContainer,
        ratingStar = AroundMePalette.Amber600,
        mapBackdrop = Color(0xFFE6ECEE),
        subtleDivider = AroundMePalette.Neutral200
    )
}

private val LightColorScheme = lightColorScheme(
    primary = AroundMePalette.Teal700,
    onPrimary = Color.White,
    primaryContainer = AroundMePalette.Teal100,
    onPrimaryContainer = AroundMePalette.Teal900,
    secondary = AroundMePalette.Amber600,
    onSecondary = Color.White,
    secondaryContainer = AroundMePalette.Amber100,
    onSecondaryContainer = AroundMePalette.Amber700,
    background = AroundMePalette.Neutral50,
    onBackground = AroundMePalette.Neutral900,
    surface = AroundMePalette.Neutral0,
    onSurface = AroundMePalette.Neutral900,
    surfaceVariant = AroundMePalette.Neutral100,
    onSurfaceVariant = AroundMePalette.Neutral600,
    outline = AroundMePalette.Neutral200
)

private val DarkColorScheme = darkColorScheme(
    primary = AroundMePalette.Teal300,
    onPrimary = AroundMePalette.Teal900,
    primaryContainer = AroundMePalette.Teal900,
    onPrimaryContainer = AroundMePalette.Teal100,
    secondary = AroundMePalette.Amber400,
    onSecondary = AroundMePalette.Neutral950,
    secondaryContainer = Color(0xFF452408),
    onSecondaryContainer = AroundMePalette.Amber100,
    background = AroundMePalette.Neutral950,
    onBackground = AroundMePalette.Neutral50,
    surface = AroundMePalette.Neutral900,
    onSurface = AroundMePalette.Neutral50,
    surfaceVariant = AroundMePalette.Neutral800,
    onSurfaceVariant = AroundMePalette.Neutral400,
    outline = AroundMePalette.Neutral800
)

@Composable
fun AroundMeTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val extendedColors = if (darkTheme) {
        AroundMeExtendedColors(
            openStatus = AroundMePalette.OpenGreenDark,
            openStatusContainer = Color(0xFF0E2F1C),
            closedStatus = AroundMePalette.ClosedRedDark,
            closedStatusContainer = Color(0xFF3A1214),
            ratingStar = AroundMePalette.Amber400,
            mapBackdrop = Color(0xFF131C21),
            subtleDivider = AroundMePalette.Neutral800
        )
    } else {
        AroundMeExtendedColors(
            openStatus = AroundMePalette.OpenGreen,
            openStatusContainer = AroundMePalette.OpenGreenContainer,
            closedStatus = AroundMePalette.ClosedRed,
            closedStatusContainer = AroundMePalette.ClosedRedContainer,
            ratingStar = AroundMePalette.Amber600,
            mapBackdrop = Color(0xFFE6ECEE),
            subtleDivider = AroundMePalette.Neutral200
        )
    }

    CompositionLocalProvider(LocalAroundMeColors provides extendedColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = AroundMeTypography,
            shapes = AroundMeShapes,
            content = content
        )
    }
}
