package app.betterout.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Icone a tratto dell'app, le stesse delle bozze grafiche (griglia 24, tratto 2). */
object AppIcons {
    private fun stroke(vararg paths: String): ImageVector =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it), stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Mountain by lazy { stroke("M3 19 9.5 8l4 6.5 2-3L21 19z") }
    val Map by lazy { stroke("M3 6.5 9 4l6 2.5L21 4v13.5L15 20l-6-2.5L3 20z", "M9 4v13.5M15 6.5V20") }
    val Backpack by lazy { stroke("M7 9.5a5 5 0 0 1 10 0V19a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2z", "M9.5 5a2.5 2.5 0 0 1 5 0M9.5 13.5h5V17h-5z") }
    val Settings by lazy { stroke("M4 7h9M19 7h1M4 17h3M13 17h7", "M18.5 7a2.5 2.5 0 1 1-5 0a2.5 2.5 0 1 1 5 0", "M12.5 17a2.5 2.5 0 1 1-5 0a2.5 2.5 0 1 1 5 0") }
}
