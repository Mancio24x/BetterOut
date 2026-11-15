package app.betterout.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.betterout.R

private fun fraunces(weight: Int) = Font(
    R.font.fraunces, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("opsz", 72f)),
)

private fun archivo(weight: Int) = Font(
    R.font.archivo, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(100f)),
)

val Fraunces = FontFamily(fraunces(560), fraunces(600))
val Archivo = FontFamily(archivo(400), archivo(500), archivo(600), archivo(700), archivo(800))

// Misure in sp: seguono la dimensione del testo scelta nel sistema.
val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight(560), fontSize = 44.sp, lineHeight = 44.sp, letterSpacing = (-0.02).em),
    headlineLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight(600), fontSize = 31.sp, lineHeight = 34.sp, letterSpacing = (-0.015).em),
    headlineMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight(600), fontSize = 27.sp, lineHeight = 30.sp, letterSpacing = (-0.015).em),
    headlineSmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight(600), fontSize = 23.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontFamily = Archivo, fontWeight = FontWeight(800), fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Archivo, fontWeight = FontWeight(700), fontSize = 16.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = Archivo, fontWeight = FontWeight(700), fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Archivo, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Archivo, fontWeight = FontWeight(400), fontSize = 15.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Archivo, fontWeight = FontWeight(400), fontSize = 13.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Archivo, fontWeight = FontWeight(700), fontSize = 16.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Archivo, fontWeight = FontWeight(600), fontSize = 13.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Archivo, fontWeight = FontWeight(600), fontSize = 12.sp, lineHeight = 15.sp),
)
