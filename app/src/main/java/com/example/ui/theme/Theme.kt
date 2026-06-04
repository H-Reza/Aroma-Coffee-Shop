package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SleekGold,
    secondary = SleekClay,
    tertiary = SleekBorder,
    background = DeepEspresso,
    surface = DarkAsh,
    onPrimary = DeepEspresso,
    onSecondary = SleekLinen,
    onTertiary = SleekLinen,
    onBackground = SleekLinen,
    onSurface = SleekLinen,
    surfaceVariant = CharcoalAccent,
    onSurfaceVariant = LightMutedText
)

private val LightColorScheme = lightColorScheme(
    primary = SleekEspresso,
    secondary = SleekClay,
    tertiary = SleekGold,
    background = SleekLinen,
    surface = SleekWhite,
    onPrimary = SleekWhite,
    onSecondary = SleekWhite,
    onTertiary = SleekEspresso,
    onBackground = SleekCharcoal,
    onSurface = SleekEspresso,
    surfaceVariant = SleekWhite,
    onSurfaceVariant = SleekClay
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We disable dynamic color to preserve the specialty coffee branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
