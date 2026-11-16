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
    val Redo by lazy { stroke("m16 5 4 4-4 4", "M20 9H10a5 5 0 0 0 0 10h3") }
    val Flag by lazy { stroke("M6 21V4h11l-2.5 4 2.5 4H6") }
    val Move by lazy { stroke("M12 3v18M3 12h18", "m9 6 3-3 3 3M9 18l3 3 3-3M6 9l-3 3 3 3M18 9l3 3-3 3") }
    val File by lazy { stroke("M7 3h7l4 4v14H7z", "M14 3v4h4") }
}

/** Icone del meteo. */
object AppIcons4 {
    private fun stroke(vararg paths: String): ImageVector =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            paths.forEach {
                addPath(pathData = addPathNodes(it), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            }
        }.build()

    private const val CLOUD = "M7 18.5a4 4 0 0 1-.6-7.95A5.5 5.5 0 0 1 17 9.6a4.5 4.5 0 0 1 .5 8.9z"
    private const val HIGH_CLOUD = "M7 14.5a4 4 0 0 1-.6-7.95A5.5 5.5 0 0 1 17 5.6a4.5 4.5 0 0 1 .5 8.9z"

    val Sun by lazy { stroke("M16 12a4 4 0 1 1-8 0a4 4 0 1 1 8 0", "M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6 7 7M17 17l1.4 1.4M5.6 18.4 7 17M17 7l1.4-1.4") }
    val Moon by lazy { stroke("M20 14.5A8 8 0 1 1 9.500 4a6.5 6.5 0 0 0 10.5 10.5z") }
    val Cloud by lazy { stroke(CLOUD) }
    val CloudSun by lazy { stroke("M7 19.500a3.500 3.500 0 0 1-.5-6.960A5 5 0 0 1 16 12.300a3.600 3.600 0 0 1 .4 7.200z", "M13 7.500a3.500 3.500 0 0 1 6 3.500", "M16 2.500V4M21.500 8H20M20 4l-1 1") }
    val Rain by lazy { stroke(HIGH_CLOUD, "M8.500 18v2.500M12 18v2.500M15.500 18v2.500") }
    val Snow by lazy { stroke(HIGH_CLOUD, "M8.500 18.500v.01M12 21v.01M15.500 18.500v.01") }
    val Storm by lazy { stroke(HIGH_CLOUD, "m12.500 16.500-2 3h3L12 22") }
    val Fog by lazy { stroke("M4 9h16M4 13h16M7 17h10") }
    val Wind by lazy { stroke("M4 9h10a2.500 2.500 0 1 0-2.500-2.500", "M4 13h14a2.500 2.500 0 1 1-2.500 2.500", "M4 17h6") }
    val Refresh by lazy { stroke("M19.500 12a7.500 7.500 0 1 1-2.200-5.300", "M18 3v4h-4") }
}

/** Icone della barra, delle impostazioni e dell'attrezzatura. */
object AppIcons5 {
    private fun stroke(vararg paths: String): ImageVector =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            paths.forEach {
                addPath(pathData = addPathNodes(it), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            }
        }.build()

    val Compass by lazy { stroke("M20.500 12a8.500 8.500 0 1 1-17 0a8.500 8.500 0 1 1 17 0", "m15.500 8.500-2 5-5 2 2-5z") }
    val Cog by lazy { stroke("M15 12a3 3 0 1 1-6 0a3 3 0 1 1 6 0", "M18.500 12a6.500 6.500 0 1 1-13 0a6.500 6.500 0 1 1 13 0", "M12 2.500v3M12 18.500v3M2.500 12h3M18.500 12h3M5.300 5.300l2.100 2.100M16.600 16.600l2.100 2.100M5.300 18.700l2.100-2.100M16.600 7.400l2.100-2.100") }
    val Box by lazy { stroke("M4 8l8-4 8 4v8l-8 4-8-4z", "M4 8l8 4 8-4M12 12v8") }
    val Boot by lazy { stroke("M6 4h5v7l7 3a3 3 0 0 1 2 2.800V19H4V6a2 2 0 0 1 2-2z", "M4 16h16") }
    val Bottle by lazy { stroke("M9.500 3h5v3h-5z", "M8.500 6h7l1.500 3v10a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2V9z", "M7 13h10") }
    val Shirt by lazy { stroke("M8 4 3 7l2 4 2.500-1V20h9V10L19 11l2-4-5-3a4 4 0 0 1-8 0z") }
    val Layers by lazy { stroke("M12 4 3 8.500l9 4.500 9-4.500z", "m3 12.500 9 4.500 9-4.500", "m3 16.500 9 4.500 9-4.500") }
    val Sock by lazy { stroke("M9 3h7v11.500a4 4 0 0 1-1.500 3.100l-3 2.400a3.300 3.300 0 0 1-4.300-5L9 13z", "M9 7h7") }
    val Torch by lazy { stroke("M7 3h10v3l-2 3v11a1 1 0 0 1-1 1h-4a1 1 0 0 1-1-1V9L7 6z", "M12 12v3") }
    val Food by lazy { stroke("M7 3v18M4.500 3v5a2.500 2.500 0 0 0 5 0V3", "M17 3c-2 2-3 5-3 9h3v9M17 3v9") }
    val Flame by lazy { stroke("M12 3c1 4 5 5.500 5 10.500a5 5 0 0 1-10 0c0-2 1-3.500 2-4.500 0 2 1 3 2 3 0-3-1-5 1-9z") }
    val Hat by lazy { stroke("M3 16.500h18", "M6.500 16.500V13a5.500 5.500 0 0 1 11 0v3.500") }
    val Glasses by lazy { stroke("M10 14.500a3.500 3.500 0 1 1-7 0a3.500 3.500 0 1 1 7 0", "M21 14.500a3.500 3.500 0 1 1-7 0a3.500 3.500 0 1 1 7 0", "M10 14.500c.600-1 3.400-1 4 0", "M3 14 5 7M21 14l-2-7") }
    val Knife by lazy { stroke("m4 20 10.500-10.500", "M14.500 9.500 19 5c1.500 3 .500 7-3 9.500z") }
    val Jacket by lazy { stroke("M9 3 4 6v14h5V9M15 3l5 3v14h-5V9M9 3a3 3 0 0 0 6 0M12 6v14") }
    val FirstAid by lazy { stroke("M5 7h14a1 1 0 0 1 1 1v11a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V8a1 1 0 0 1 1-1z", "M9 7V4h6v3", "M12 11v5M9.500 13.500h5") }
    val SleepingBag by lazy { stroke("M8 3h8a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z", "M5 9h14M12 9v12") }
    val Tent by lazy { stroke("M12 4 3 20h18z", "m12 12-3 8M12 12l3 8") }
    val Wave by lazy { stroke("M3 9.500c2-2 4-2 6 0s4 2 6 0 4-2 6 0", "M3 15.500c2-2 4-2 6 0s4 2 6 0 4-2 6 0") }
    val Mask by lazy { stroke("M5 8h14a2 2 0 0 1 2 2v3a3 3 0 0 1-3 3h-2.500L12 14l-3.500 2H6a3 3 0 0 1-3-3v-3a2 2 0 0 1 2-2z", "M12 8V5") }
    val Gauge by lazy { stroke("M20.500 13a8.500 8.500 0 1 0-17 0", "M12 13l4-4", "M5 18h14") }
    val Battery by lazy { stroke("M7 3h10a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z", "m13 8-3 4.500h4L11 17") }
}
