package app.betterout.ui.dive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

private val Jaw = Color(0xFF41525F)
private val JawShadow = Color(0xFF2C3943)
private val Gum = Color(0xFF9E2B3A)
private val Tooth = Color(0xFFF7F4EC)
private val ToothEdge = Color(0xFFD9D2C3)

/** Quanto sono chiuse le mascelle (0 aperte, 1 chiuse) a un punto [t] dell'animazione, da 0 a 1. */
internal fun jawClosure(t: Float): Float = when {
    t < 0.42f -> (t / 0.42f).let { it * it } // si avvicinano accelerando
    t < 0.58f -> 1f // il morso
    else -> 1f - ((t - 0.58f) / 0.42f).let { 1f - (1f - it) * (1f - it) } // si riaprono rallentando
}

/**
 * Il morso dello squalo che porta nella modalità immersioni: due mascelle con i denti si chiudono sullo schermo
 * e si riaprono. [progress] va da 0 a 1; mentre è in corso lo schermo sotto non riceve tocchi.
 */
@Composable
fun SharkBite(progress: Float, modifier: Modifier = Modifier) {
    Canvas(
        modifier.fillMaxSize().pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } },
    ) {
        val closure = jawClosure(progress)
        if (closure <= 0f) return@Canvas
        val toothWidth = 44.dp.toPx()
        val toothHeight = 54.dp.toPx()
        val gum = 14.dp.toPx()
        // Chiuse, le punte dei denti si incrociano un po' oltre la metà dello schermo.
        val reach = (size.height / 2 + toothHeight * 0.35f) * closure
        // Mentre morde, lo squalo scuote la testa.
        val shake = if (progress in 0.42f..0.58f) (sin((progress - 0.42f) / 0.16f * 4 * PI) * 6.dp.toPx()).toFloat() else 0f
        jaw(top = true, reach, toothWidth, toothHeight, gum, shake)
        jaw(top = false, reach, toothWidth, toothHeight, gum, -shake)
    }
}

/** Una mascella: osso scuro, gengiva e una fila di denti triangolari rivolti verso l'altra mascella. */
private fun DrawScope.jaw(top: Boolean, reach: Float, toothWidth: Float, toothHeight: Float, gum: Float, shake: Float) {
    val base = (reach - toothHeight).coerceAtLeast(0f) // dove finisce la gengiva e partono i denti
    fun y(v: Float) = if (top) v else size.height - v
    val boneTop = if (top) 0f else y(base)
    drawRect(Jaw, Offset(0f, boneTop), Size(size.width, base))
    drawRect(JawShadow, Offset(0f, if (top) (base - gum * 2).coerceAtLeast(0f) else y(base)), Size(size.width, minOf(gum * 2, base)))
    drawRect(Gum, Offset(0f, if (top) (base - gum).coerceAtLeast(0f) else y(base)), Size(size.width, minOf(gum, base)))
    // I denti di sotto sono sfalsati di mezzo dente: chiudendo si incastrano con quelli di sopra.
    val offset = (if (top) 0f else toothWidth / 2) + shake
    val count = ceil(size.width / toothWidth).toInt() + 2
    for (i in -1 until count) {
        val left = i * toothWidth + offset
        val tooth = Path().apply {
            moveTo(left, y(base))
            lineTo(left + toothWidth / 2, y(base + toothHeight))
            lineTo(left + toothWidth, y(base))
            close()
        }
        drawPath(tooth, Tooth)
        val edge = Path().apply {
            moveTo(left + toothWidth / 2, y(base + toothHeight))
            lineTo(left + toothWidth, y(base))
            lineTo(left + toothWidth * 0.8f, y(base))
            close()
        }
        drawPath(edge, ToothEdge)
    }
}
