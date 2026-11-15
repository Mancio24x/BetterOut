package app.betterout.ui.map

import android.annotation.SuppressLint
import android.graphics.PointF
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.betterout.R
import app.betterout.data.Http
import app.betterout.domain.GeoPoint
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.eq
import org.maplibre.android.style.expressions.Expression.exponential
import org.maplibre.android.style.expressions.Expression.interpolate
import org.maplibre.android.style.expressions.Expression.zoom
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.match
import org.maplibre.android.style.expressions.Expression.stop
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineDasharray
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.hypot

/** Tratto pieno = su sentiero o da file; tratteggiato = manuale, non verificato su sentiero. */
data class MapLine(val points: List<GeoPoint>, val dashed: Boolean = false)

enum class MarkerKind { PARTENZA, ARRIVO, PUNTO, SELEZIONATO }

data class MapMarker(val point: GeoPoint, val kind: MarkerKind)

/** Permette alla schermata di spostare la vista della mappa. */
class MapCamera {
    internal var map: MapLibreMap? = null
    private var pending: ((MapLibreMap) -> Unit)? = null

    val center: GeoPoint? get() = map?.cameraPosition?.target?.let { GeoPoint(it.latitude, it.longitude) }

    fun moveTo(point: GeoPoint, zoom: Double = 13.0) = withMap { it.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(point.lat, point.lon), zoom)) }

    /** Inquadra tutti i punti dati, lasciando in alto [topPadding] pixel liberi per ciò che copre la mappa. Senza punti non fa nulla. */
    fun fit(points: List<GeoPoint>, topPadding: Int = FIT_PADDING) {
        if (points.isEmpty()) return
        val south = points.minOf { it.lat }
        val north = points.maxOf { it.lat }
        val west = points.minOf { it.lon }
        val east = points.maxOf { it.lon }
        withMap {
            if (south == north && west == east) it.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(south, west), 14.0))
            else it.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.from(north, east, south, west), FIT_PADDING, topPadding, FIT_PADDING, FIT_PADDING))
        }
    }

    private fun withMap(action: (MapLibreMap) -> Unit) { map?.let(action) ?: run { pending = action } }

    internal fun attach(m: MapLibreMap) { map = m; pending?.invoke(m); pending = null }
}

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val EMPTY_STYLE = """{"version":8,"sources":{},"layers":[{"id":"fondo","type":"background","paint":{"background-color":"#EFE7D8"}}]}"""
private const val ROUTE_COLOR = "#B9541A"
private const val LIGHT = "#FFFCF5"
private const val DARK = "#2A2620"
private const val HIT_RADIUS_DP = 28
private const val FIT_PADDING = 96

/**
 * La mappa dell'app: sfondo OpenFreeMap, percorsi e punti. L'attribuzione è sempre visibile; se lo sfondo non si carica
 * viene detto, e percorsi e punti restano comunque disegnati.
 *
 * I gesti sui punti (tocco, trascinamento) sono una scorciatoia: ogni schermata che li usa offre le stesse azioni con pulsanti.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun AppMap(
    modifier: Modifier = Modifier,
    lines: List<MapLine> = emptyList(),
    markers: List<MapMarker> = emptyList(),
    camera: MapCamera = remember { MapCamera() },
    gestures: Boolean = true,
    onTap: ((GeoPoint) -> Unit)? = null,
    onMarkerTap: ((Int) -> Unit)? = null,
    onMarkerDrag: ((Int, GeoPoint) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current.density
    val description = stringResource(R.string.mappa_descrizione)
    var style by remember { mutableStateOf<Style?>(null) }
    var backgroundMissing by remember { mutableStateOf(false) }
    var readyMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var dragged by remember { mutableStateOf<Pair<Int, GeoPoint>?>(null) }
    val currentMarkers by rememberUpdatedState(markers)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnMarkerTap by rememberUpdatedState(onMarkerTap)
    val currentOnMarkerDrag by rememberUpdatedState(onMarkerDrag)

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(mapView) {
        val hitRadius = HIT_RADIUS_DP * density
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        fun markerAt(map: MapLibreMap, x: Float, y: Float): Int = currentMarkers.indices
            .map { it to map.projection.toScreenLocation(LatLng(currentMarkers[it].point.lat, currentMarkers[it].point.lon)) }
            .filter { (_, p) -> hypot(p.x - x, p.y - y) <= hitRadius }
            .minByOrNull { (_, p) -> hypot(p.x - x, p.y - y) }?.first ?: -1

        mapView.getMapAsync { map ->
            map.cameraPosition = CameraPosition.Builder().target(LatLng(45.9, 10.9)).zoom(5.5).build()
            camera.attach(map)
            map.uiSettings.isLogoEnabled = false
            map.uiSettings.isAttributionEnabled = false
            map.uiSettings.isRotateGesturesEnabled = false
            map.uiSettings.isTiltGesturesEnabled = false
            if (!gestures) map.uiSettings.setAllGesturesEnabled(false)
            readyMap = map
            map.addOnMapClickListener { latLng ->
                val screen = map.projection.toScreenLocation(latLng)
                val hit = if (currentOnMarkerTap != null) markerAt(map, screen.x, screen.y) else -1
                if (hit >= 0) currentOnMarkerTap?.invoke(hit) else currentOnTap?.invoke(GeoPoint(latLng.latitude, latLng.longitude))
                true
            }
            // Trascinamento di un punto: finché il dito è su un punto, la mappa non si muove.
            var index = -1
            var start = PointF()
            var moved = false
            mapView.setOnTouchListener { _, e ->
                if (currentOnMarkerDrag == null) return@setOnTouchListener false
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { index = markerAt(map, e.x, e.y); start = PointF(e.x, e.y); moved = false; index >= 0 }
                    MotionEvent.ACTION_MOVE -> if (index >= 0) {
                        if (moved || hypot(e.x - start.x, e.y - start.y) > slop) {
                            moved = true
                            val at = map.projection.fromScreenLocation(PointF(e.x, e.y))
                            dragged = index to GeoPoint(at.latitude, at.longitude)
                        }
                        true
                    } else false
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (index >= 0) {
                        val target = dragged
                        dragged = null
                        if (e.actionMasked == MotionEvent.ACTION_UP) {
                            if (moved && target != null) currentOnMarkerDrag?.invoke(target.first, target.second) else currentOnMarkerTap?.invoke(index)
                        }
                        index = -1
                        true
                    } else false
                    else -> index >= 0
                }
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            camera.map = null
            readyMap = null
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // Lo stile lo scarica l'app, non la libreria: così si sa subito se manca. Senza rete e senza copia locale si mette
    // un fondo vuoto e lo si dice; percorsi e punti si vedono lo stesso.
    LaunchedEffect(readyMap, attempt) {
        val map = readyMap ?: return@LaunchedEffect
        val json = styleJson(File(context.cacheDir, "stile-mappa.json"))
        backgroundMissing = json == null
        style = null
        map.setStyle(Style.Builder().fromJson(json ?: EMPTY_STYLE)) { if (json != null) highlightTrails(it); addRouteLayers(it); style = it }
    }
    LaunchedEffect(style, lines) {
        style?.getSourceAs<GeoJsonSource>("percorsi")?.setGeoJson(
            FeatureCollection.fromFeatures(lines.filter { it.points.size >= 2 }.map { line ->
                Feature.fromGeometry(LineString.fromLngLats(line.points.map { Point.fromLngLat(it.lon, it.lat) })).apply { addBooleanProperty("manuale", line.dashed) }
            }),
        )
    }
    LaunchedEffect(style, markers, dragged) {
        style?.getSourceAs<GeoJsonSource>("punti")?.setGeoJson(
            FeatureCollection.fromFeatures(markers.mapIndexed { i, m ->
                val p = dragged?.takeIf { it.first == i }?.second ?: m.point
                Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat)).apply { addStringProperty("tipo", m.kind.name) }
            }),
        )
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize(), update = { it.contentDescription = description })
        // Mappa di sola anteprima: i tocchi non le arrivano, così la pagina sotto continua a scorrere.
        if (!gestures) Box(Modifier.matchParentSize().pointerInput(Unit) {})
        Column(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
            if (backgroundMissing) MapNote(stringResource(R.string.mappa_sfondo_assente), Modifier.widthIn(max = 200.dp).minimumInteractiveComponentSize().clickable(role = Role.Button) { attempt++ })
            MapNote(stringResource(R.string.mappa_attribuzione))
        }
    }
}

@Composable
private fun MapNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 4.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Lo stile di OpenFreeMap: dalla rete, tenendone una copia; senza rete la copia; null se non c'è né l'una né l'altra. */
private suspend fun styleJson(copy: File): String? = withContext(Dispatchers.IO) {
    try { Http.get(STYLE_URL).also(copy::writeText) } catch (e: IOException) { copy.takeIf { it.exists() }?.readText() }
}

/** Lo stile pubblico disegna i sentieri bianchi e sottili, solo da vicino: qui diventano scuri e compaiono prima. */
private fun highlightTrails(style: Style) {
    (style.getLayer("road_path_pedestrian") as? LineLayer)?.apply {
        minZoom = 12f
        setProperties(lineColor("#7A4A2A"), lineWidth(interpolate(exponential(1.2f), zoom(), stop(12, 1f), stop(16, 2.2f), stop(20, 8f))))
    }
}

private fun addRouteLayers(style: Style) {
    style.addSource(GeoJsonSource("percorsi"))
    style.addSource(GeoJsonSource("punti"))
    val round = arrayOf(lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND))
    style.addLayer(LineLayer("percorsi-bordo", "percorsi").withProperties(lineColor(LIGHT), lineWidth(8f), *round))
    style.addLayer(LineLayer("percorsi-pieni", "percorsi").withFilter(eq(get("manuale"), literal(false))).withProperties(lineColor(ROUTE_COLOR), lineWidth(4.5f), *round))
    style.addLayer(LineLayer("percorsi-manuali", "percorsi").withFilter(eq(get("manuale"), literal(true))).withProperties(lineColor(ROUTE_COLOR), lineWidth(4.5f), lineDasharray(arrayOf(0.1f, 2f)), *round))
    // Forme diverse oltre al colore: partenza chiara con bordo spesso, arrivo scuro, intermedio vuoto, selezionato pieno e più grande.
    fun byKind(start: Any, end: Any, selected: Any, other: Any) = match(
        get("tipo"), literal(other), stop(MarkerKind.PARTENZA.name, literal(start)), stop(MarkerKind.ARRIVO.name, literal(end)), stop(MarkerKind.SELEZIONATO.name, literal(selected)),
    )
    style.addLayer(
        CircleLayer("punti", "punti").withProperties(
            circleRadius(byKind(9f, 9f, 11f, 6.5f)),
            circleColor(byKind(LIGHT, DARK, ROUTE_COLOR, LIGHT)),
            circleStrokeColor(byKind(DARK, LIGHT, LIGHT, ROUTE_COLOR)),
            circleStrokeWidth(byKind(3.5f, 3f, 3f, 3f)),
        ),
    )
}
