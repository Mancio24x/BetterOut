package app.betterout.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Direzione "Rifugio": valori documentati in docs/03-design-system.md
val LightColors = lightColorScheme(
    primary = Color(0xFF3F5F3A),
    onPrimary = Color(0xFFFFFCF5),
    primaryContainer = Color(0xFFDCE6CF),
    onPrimaryContainer = Color(0xFF22381E),
    secondary = Color(0xFFB9741A),
    onSecondary = Color(0xFFFFFCF5),
    secondaryContainer = Color(0xFFF0C46B),
    onSecondaryContainer = Color(0xFF2A2620),
    background = Color(0xFFF3ECDF),
    onBackground = Color(0xFF2A2620),
    surface = Color(0xFFFFFCF5),
    onSurface = Color(0xFF2A2620),
    surfaceVariant = Color(0xFFEFE7D8),
    onSurfaceVariant = Color(0xFF6A6155),
    surfaceContainer = Color(0xFFFFFCF5),
    outline = Color(0xFF6A6155),
    outlineVariant = Color(0x242A2620),
    error = Color(0xFFA3271B),
    onError = Color(0xFFFFFCF5),
    errorContainer = Color(0xFFF4D3CC),
    onErrorContainer = Color(0xFF5E130B),
)

/** Nero pieno per schermi AMOLED: i pixel dello sfondo restano spenti. */
val AmoledColors by lazy {
    DarkColors.copy(
        background = Color(0xFF000000), surface = Color(0xFF111110), surfaceContainer = Color(0xFF111110), surfaceVariant = Color(0xFF1E1D1A),
    )
}

val DarkColors = darkColorScheme(
    primary = Color(0xFFA9CB98),
    onPrimary = Color(0xFF172413),
    primaryContainer = Color(0xFF3A4A33),
    onPrimaryContainer = Color(0xFFE3F2D9),
    secondary = Color(0xFFE3A84A),
    onSecondary = Color(0xFF191713),
    secondaryContainer = Color(0xFFF0C46B),
    onSecondaryContainer = Color(0xFF2A2620),
    background = Color(0xFF191713),
    onBackground = Color(0xFFF1EADC),
    surface = Color(0xFF25221C),
    onSurface = Color(0xFFF1EADC),
    surfaceVariant = Color(0xFF322E26),
    onSurfaceVariant = Color(0xFFB6AC9B),
    surfaceContainer = Color(0xFF25221C),
    outline = Color(0xFFB6AC9B),
    outlineVariant = Color(0x26F1EADC),
    error = Color(0xFFFF9C8F),
    onError = Color(0xFF2B0905),
    errorContainer = Color(0xFF4F1D16),
    onErrorContainer = Color(0xFFFFD9D2),
)

// Modalità immersioni: blu del mare, stessi ruoli e contrasti dei temi delle escursioni.
val DiveLightColors = lightColorScheme(
    primary = Color(0xFF0B5A86),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFE5F3),
    onPrimaryContainer = Color(0xFF06324D),
    secondary = Color(0xFF0F7FA8),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBFE6F5),
    onSecondaryContainer = Color(0xFF0E2230),
    background = Color(0xFFE8F1F6),
    onBackground = Color(0xFF0E2230),
    surface = Color(0xFFFAFDFF),
    onSurface = Color(0xFF0E2230),
    surfaceVariant = Color(0xFFDCE9F1),
    onSurfaceVariant = Color(0xFF465D6D),
    surfaceContainer = Color(0xFFFAFDFF),
    outline = Color(0xFF465D6D),
    outlineVariant = Color(0x240E2230),
    error = Color(0xFFA3271B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF4D3CC),
    onErrorContainer = Color(0xFF5E130B),
)

val DiveDarkColors = darkColorScheme(
    primary = Color(0xFF7CC4EA),
    onPrimary = Color(0xFF032538),
    primaryContainer = Color(0xFF134560),
    onPrimaryContainer = Color(0xFFD3ECFA),
    secondary = Color(0xFF4FC0E8),
    onSecondary = Color(0xFF032538),
    secondaryContainer = Color(0xFF1B5A78),
    onSecondaryContainer = Color(0xFFE3F0F8),
    background = Color(0xFF07131C),
    onBackground = Color(0xFFE3F0F8),
    surface = Color(0xFF0F212D),
    onSurface = Color(0xFFE3F0F8),
    surfaceVariant = Color(0xFF1A3141),
    onSurfaceVariant = Color(0xFF9DB7C7),
    surfaceContainer = Color(0xFF0F212D),
    outline = Color(0xFF9DB7C7),
    outlineVariant = Color(0x26E3F0F8),
    error = Color(0xFFFF9C8F),
    onError = Color(0xFF2B0905),
    errorContainer = Color(0xFF4F1D16),
    onErrorContainer = Color(0xFFFFD9D2),
)

val DiveAmoledColors by lazy {
    DiveDarkColors.copy(background = Color(0xFF000000), surface = Color(0xFF0A1820), surfaceContainer = Color(0xFF0A1820), surfaceVariant = Color(0xFF142A38))
}
