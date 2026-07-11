package com.bakersoftware.plannextday.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.bakersoftware.plannextday.data.ThemeChoice

private val LightScheme = lightColorScheme(
    primary = Indigo,
    onPrimary = Cream,
    secondary = Mint,
    tertiary = Coral,
    background = Cream,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Slate,
    outline = Slate
)

private val DarkScheme = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Ink,
    secondary = MintLight,
    tertiary = CoralLight,
    background = Night,
    onBackground = Cream,
    surface = SurfaceDark,
    onSurface = Cream,
    surfaceVariant = NightSoft,
    onSurfaceVariant = Mist,
    outline = Mist
)

@Composable
fun PlanNextDaySimpleTheme(
    themeChoice: ThemeChoice = ThemeChoice.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeChoice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = Typography,
        content = content
    )
}
