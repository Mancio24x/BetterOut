package app.betterout.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import app.betterout.data.ThemeMode

@Composable
fun BetterOutTheme(mode: ThemeMode = ThemeMode.SISTEMA, diving: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = when (mode) {
            ThemeMode.SISTEMA -> if (isSystemInDarkTheme()) (if (diving) DiveDarkColors else DarkColors) else (if (diving) DiveLightColors else LightColors)
            ThemeMode.CHIARO -> if (diving) DiveLightColors else LightColors
            ThemeMode.NERO -> if (diving) DiveAmoledColors else AmoledColors
        },
        typography = AppTypography,
        content = content,
    )
}
