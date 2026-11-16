package app.betterout.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.betterout.R
import app.betterout.data.RouteGeometry
import app.betterout.data.RouteRepository
import app.betterout.domain.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter
import java.io.File
import kotlin.coroutines.resume

private const val ROUTE = 0xFFB9541A.toInt()
private const val LIGHT = 0xFFFFFCF5.toInt()
private const val DARK = 0xFF2A2620.toInt()
private const val PLAIN = 0xFFEFE7D8.toInt()

/**
 * Anteprima del percorso di un'uscita: un'immagine della mappa con il percorso sopra, generata una volta e salvata.
 * Al posto della mappa viva nelle pagine che scorrono, che è pesante da creare e carica le tessere mentre si scorre.
 * L'altezza la decide [modifier]; lo spazio è riservato subito, così la pagina non salta quando l'immagine arriva.
 * Niente se l'uscita non ha un percorso.
 */
@Composable
fun RoutePreview(routes: RouteRepository, routeId: String?, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val geometry by produceState(initialValue = null as RouteGeometry?, routeId) { value = routeId?.let { routes.geometry(it) } }
    val g = geometry ?: return
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var size by remember { mutableStateOf(IntSize.Zero) }
    val image by produceState(initialValue = null as ImageBitmap?, g, size) {
        if (size.width > 0 && size.height > 0) value = previewBitmap(context, g, size, density)?.asImageBitmap()
    }
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceVariant).onSizeChanged { size = it }
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.apri_mappa), onClick = onClick) else Modifier),
    ) {
        image?.let { Image(it, contentDescription = stringResource(R.string.mappa_descrizione), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Surface(Modifier.align(Alignment.BottomStart).padding(8.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
            Text(stringResource(R.string.mappa_attribuzione), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

/** L'immagine salvata se c'è, altrimenti la genera: sfondo dalla mappa (o neutro senza rete) e percorso disegnato sopra. */
private suspend fun previewBitmap(context: Context, g: RouteGeometry, size: IntSize, density: Float): Bitmap? {
    val lines = g.mapLines(5.0).filter { it.points.size >= 2 }
    if (lines.isEmpty()) return null
    val key = (lines.hashCode().toLong() * 31 + size.width * 7919 + size.height).toString(16)
    val file = File(context.cacheDir, "anteprime/$key.png")
    withContext(Dispatchers.IO) { file.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) } }?.let { return it }

    val all = lines.flatMap { it.points }
    val style = styleJson(File(context.cacheDir, "stile-mappa.json"))
    val drawn = (if (style != null) snapshot(context, style, all, size, density) else null)
        ?.let { (bitmap, project) -> drawRoute(bitmap.copy(Bitmap.Config.ARGB_8888, true), lines, density, project) }
        ?: plainPreview(lines, size, density)
    // Senza sfondo l'immagine non si salva: la prossima volta, con la rete, arriva quella con la mappa.
    if (style != null) withContext(Dispatchers.IO) { file.parentFile?.mkdirs(); file.outputStream().use { drawn.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    return drawn
}

/** Lo sfondo dalla mappa, inquadrato sul percorso con un margine; null se non si riesce a generarlo. */
private suspend fun snapshot(context: Context, style: String, points: List<GeoPoint>, size: IntSize, density: Float): Pair<Bitmap, (GeoPoint) -> PointF>? =
    suspendCancellableCoroutine { cont ->
        MapLibre.getInstance(context)
        val minLat = points.minOf { it.lat }; val maxLat = points.maxOf { it.lat }
        val minLon = points.minOf { it.lon }; val maxLon = points.maxOf { it.lon }
        // Margine del 18% per lato (almeno ~300 m), perché partenza e arrivo non finiscano sul bordo.
        val padLat = maxOf((maxLat - minLat) * 0.18, 0.003); val padLon = maxOf((maxLon - minLon) * 0.18, 0.004)
        val bounds = LatLngBounds.Builder().include(LatLng(minLat - padLat, minLon - padLon)).include(LatLng(maxLat + padLat, maxLon + padLon)).build()
        val options = MapSnapshotter.Options((size.width / density).toInt(), (size.height / density).toInt())
            .withStyleBuilder(Style.Builder().fromJson(style))
            .withRegion(bounds)
            .withPixelRatio(density)
            // L'attribuzione la scrive l'anteprima sopra l'immagine, leggibile: quella di MapLibre sarebbe doppia.
            .withLogo(false)
        val snapshotter = MapSnapshotter(context, options)
        cont.invokeOnCancellation { snapshotter.cancel() }
        snapshotter.start({ shot ->
            if (cont.isActive) cont.resume(shot.bitmap to { p: GeoPoint -> shot.pixelForLatLng(LatLng(p.lat, p.lon)) })
        }, { _ -> if (cont.isActive) cont.resume(null) })
    }

/** Senza rete: il percorso su uno sfondo neutro, in proporzione (le longitudini ridotte con la latitudine). */
private fun plainPreview(lines: List<MapLine>, size: IntSize, density: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(PLAIN)
    val all = lines.flatMap { it.points }
    val minLat = all.minOf { it.lat }; val maxLat = all.maxOf { it.lat }
    val minLon = all.minOf { it.lon }; val maxLon = all.maxOf { it.lon }
    val k = kotlin.math.cos(Math.toRadians((minLat + maxLat) / 2))
    val w = ((maxLon - minLon) * k).coerceAtLeast(1e-6); val h = (maxLat - minLat).coerceAtLeast(1e-6)
    val margin = 28 * density
    val scale = minOf((size.width - 2 * margin) / w, (size.height - 2 * margin) / h)
    val ox = (size.width - w * scale) / 2; val oy = (size.height - h * scale) / 2
    return drawRoute(bitmap, lines, density) { p -> PointF((ox + (p.lon - minLon) * k * scale).toFloat(), (size.height - oy - (p.lat - minLat) * scale).toFloat()) }
}

/** Percorso con bordo chiaro (tratti manuali tratteggiati), partenza chiara e arrivo scuro: come sulla mappa viva. */
private fun drawRoute(bitmap: Bitmap, lines: List<MapLine>, density: Float, project: (GeoPoint) -> PointF): Bitmap {
    val canvas = Canvas(bitmap)
    fun path(points: List<GeoPoint>) = Path().apply { points.forEachIndexed { i, p -> project(p).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } } }
    val stroke = { color: Int, width: Float -> Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = width * density; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND } }
    lines.forEach { canvas.drawPath(path(it.points), stroke(LIGHT, 8f)) }
    lines.forEach { line ->
        canvas.drawPath(path(line.points), stroke(ROUTE, 4.5f).apply { if (line.dashed) pathEffect = DashPathEffect(floatArrayOf(1f * density, 9f * density), 0f) })
    }
    fun dot(p: GeoPoint, fill: Int, ring: Int, ringWidth: Float) {
        val c = project(p)
        canvas.drawCircle(c.x, c.y, 9f * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill })
        canvas.drawCircle(c.x, c.y, 9f * density, stroke(ring, ringWidth))
    }
    dot(lines.first().points.first(), LIGHT, DARK, 3.5f)
    dot(lines.last().points.last(), DARK, LIGHT, 3f)
    return bitmap
}
