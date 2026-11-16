package app.betterout.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
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
        shapes = AppShapes,
        content = content,
    )
}

/** Le forme Material 3 con i raggi del design system: campi 14, riquadri 16, schede 26, fogli e dialoghi 28. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
