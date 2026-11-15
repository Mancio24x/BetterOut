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

object AppIcons2 {
    private fun stroke(vararg paths: String): ImageVector =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            paths.forEach {
                addPath(pathData = addPathNodes(it), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            }
        }.build()

    val Plus by lazy { stroke("M12 5v14M5 12h14") }
    val Minus by lazy { stroke("M5 12h14") }
    val Back by lazy { stroke("M19 12H5M11 6l-6 6 6 6") }
    val Close by lazy { stroke("M6 6l12 12M18 6 6 18") }
    val More by lazy { stroke("M12 5v.01M12 12v.01M12 19v.01") }
    val Edit by lazy { stroke("M4 20h4L19.5 8.5l-4-4L4 16z", "m13.5 6.5 4 4") }
    val Calendar by lazy { stroke("M6 5.5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-11a2 2 0 0 1 2-2z", "M4 10h16M8 3v4M16 3v4") }
    val Check by lazy { stroke("m5 12.5 4.5 4.5L19 7") }
    val Pin by lazy { stroke("M12 21s-6.5-6.2-6.5-11.2a6.5 6.5 0 0 1 13 0C18.5 14.8 12 21 12 21z", "M14.2 9.8a2.2 2.2 0 1 1-4.4 0a2.2 2.2 0 1 1 4.4 0") }
    val Clock by lazy { stroke("M20.5 12a8.5 8.5 0 1 1-17 0a8.5 8.5 0 1 1 17 0", "M12 7.5V12l3 2") }
    val Chevron by lazy { stroke("m9 6 6 6-6 6") }
    val Trash by lazy { stroke("M4 7h16M9 7V4.5h6V7M6.5 7l1 13h9l1-13M10 11v5.5M14 11v5.5") }
    val Copy by lazy { stroke("M10.5 8.5h7a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-7a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z", "M15.5 8.5v-3a2 2 0 0 0-2-2h-7a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2") }
    val Warn by lazy { stroke("M12 4 21.5 20h-19z", "M12 10v4.5M12 17.2v.01") }
    val Filter by lazy { stroke("M4 6h16M7 12h10M10 18h4") }
}

object AppIcons3 {
    private fun stroke(vararg paths: String): ImageVector =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            paths.forEach {
                addPath(pathData = addPathNodes(it), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            }
        }.build()

    val Search by lazy { stroke("M17.5 11a6.5 6.5 0 1 1-13 0a6.5 6.5 0 1 1 13 0", "m20 20-4.2-4.2") }
    val Undo by lazy { stroke("M8 5 4 9l4 4", "M4 9h10a5 5 0 0 1 0 10h-3") }
    val Download by lazy { stroke("M12 4v11M7 10.5l5 5 5-5M5 20h14") }
    val Drop by lazy { stroke("M12 3s6.5 7 6.5 11.5a6.5 6.5 0 0 1-13 0C5.500 10 12 3 12 3z") }
    val Info by lazy { stroke("M20.5 12a8.5 8.5 0 1 1-17 0a8.5 8.5 0 1 1 17 0", "M12 11v5.5M12 7.6v.01") }
}
