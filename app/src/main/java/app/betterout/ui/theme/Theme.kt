package app.betterout.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import app.betterout.data.ThemeMode

@Composable
fun BetterOutTheme(mode: ThemeMode = ThemeMode.SISTEMA, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = when (mode) {
            ThemeMode.SISTEMA -> if (isSystemInDarkTheme()) DarkColors else LightColors
            ThemeMode.CHIARO -> LightColors
            ThemeMode.NERO -> AmoledColors
        },
        typography = AppTypography,
        content = content,
    )
}
