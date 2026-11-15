package app.betterout.data

import androidx.room.withTransaction
import app.betterout.domain.GeoPoint
import app.betterout.domain.GpxTrack
import app.betterout.domain.GpxWaypoint
import app.betterout.domain.GpxWriter
import app.betterout.domain.RouteDraft
import app.betterout.domain.RouteMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class RouteRepository(private val db: AppDatabase, private val dir: File, private val now: () -> Long = System::currentTimeMillis) {
    private val dao = db.routeDao()
    private val trips = db.tripDao()
    private val json = Json { ignoreUnknownKeys = true }

    fun observeAll(): Flow<List<Route>> = dao.observeAll()
    fun observe(id: String): Flow<Route?> = dao.observe(id)
    suspend fun get(id: String): Route? = dao.get(id)
    fun observeTripsUsing(id: String): Flow<Int> = trips.observeRouteUsage(id)

    suspend fun geometry(id: String): RouteGeometry? = withContext(Dispatchers.IO) {
        geometryFile(id).takeIf { it.exists() }?.let { json.decodeFromString<RouteGeometry>(it.readText()) }
    }

    /** Salva un percorso disegnato; con [id] aggiorna quello esistente. */
    suspend fun saveDrawn(id: String?, name: String, draft: RouteDraft): String {
        require(draft.unresolved.isEmpty()) { "ci sono tratti non calcolati" }
        val existing = id?.let { dao.get(it) }
        val base = existing ?: Route(id = UUID.randomUUID().toString(), name = name, origin = RouteOrigin.DISEGNATO, distanceMeters = 0.0, createdAt = now(), updatedAt = now())
        store(base.copy(name = name, elevationSource = ElevationSource.ROUTING, hasManualLegs = draft.hasManualLegs), RouteGeometry(draft = draft))
        return base.id
    }

    /** Salva un tracciato letto da un file GPX; [original] è il file così com'era, che viene copiato senza modifiche. */
    suspend fun saveImported(name: String, track: GpxTrack, waypoints: List<GpxWaypoint>, fileName: String?, original: File?): String {
        val route = Route(id = UUID.randomUUID().toString(), name = name, origin = RouteOrigin.IMPORTATO, distanceMeters = 0.0, elevationSource = ElevationSource.GPX, segmentCount = track.segments.size, sourceFileName = fileName, createdAt = now(), updatedAt = now())
        withContext(Dispatchers.IO) { original?.copyTo(originalFile(route.id), overwrite = true) }
        store(route, RouteGeometry(segments = track.segments, waypoints = waypoints))
        return route.id
    }

    /**
     * Chiede al modello del terreno le quote dei punti che non ne hanno (al più [maxPoints], presi a intervalli regolari)
     * e le salva dichiarandole come tali. Le quote già presenti non vengono toccate.
     */
    suspend fun requestModelElevations(id: String, maxPoints: Int = 300, fetch: suspend (List<GeoPoint>) -> List<Double> = { TerrainElevation.elevations(it) }) {
        val route = dao.get(id) ?: return
        val geometry = geometry(id) ?: return
        val missing = ArrayList<GeoPoint>()
        geometry.mapPoints { p -> p.also { if (it.ele == null) missing += it } }
        if (missing.isEmpty()) return
        val step = maxOf(1, (missing.size + maxPoints - 1) / maxPoints)
        val asked = missing.indices.filter { it % step == 0 || it == missing.lastIndex }
        val elevations = asked.zip(fetch(asked.map { missing[it] })).toMap()
        var n = 0
        store(route.copy(modelElevation = true), geometry.mapPoints { p -> if (p.ele == null) p.copy(ele = elevations[n++]) else p })
    }

    suspend fun rename(id: String, name: String) {
        dao.get(id)?.let { dao.upsert(it.copy(name = name, updatedAt = now())) }
    }

    /** Elimina il percorso. Le uscite che lo usavano restano, senza percorso. */
    suspend fun delete(id: String) {
        db.withTransaction { trips.detachRoute(id); dao.delete(id) }
        withContext(Dispatchers.IO) { geometryFile(id).delete(); originalFile(id).delete() }
    }

    suspend fun setTripRoute(tripId: String, routeId: String?) = trips.setRoute(tripId, routeId, now())

    /** Il GPX da esportare, scritto dai punti salvati (mai semplificati). */
    suspend fun gpx(id: String, manualNote: String): String? {
        val route = dao.get(id) ?: return null
        val geometry = geometry(id) ?: return null
        return withContext(Dispatchers.Default) { GpxWriter.write(route.name, geometry.lines, geometry.waypoints, manualNote.takeIf { route.hasManualLegs }) }
    }

    private suspend fun store(route: Route, geometry: RouteGeometry) {
        val stats = withContext(Dispatchers.Default) { RouteMath.stats(geometry.lines) }
        withContext(Dispatchers.IO) { dir.mkdirs(); geometryFile(route.id).writeText(json.encodeToString(RouteGeometry.serializer(), geometry)) }
        dao.upsert(
            route.copy(
                distanceMeters = stats.distanceMeters, ascentMeters = stats.elevation?.ascent, descentMeters = stats.elevation?.descent,
                minElevation = stats.elevation?.min, maxElevation = stats.elevation?.max, partialElevation = stats.partialElevation,
                elevationSource = if (stats.elevation == null) ElevationSource.NESSUNA else route.elevationSource,
                updatedAt = now(),
            ),
        )
    }

    private fun geometryFile(id: String) = File(dir, "$id.json")
    private fun originalFile(id: String) = File(dir, "$id.gpx")
}

private fun RouteGeometry.mapPoints(f: (GeoPoint) -> GeoPoint): RouteGeometry =
    copy(segments = segments.map { it.map(f) }, draft = draft?.let { d -> d.copy(legs = d.legs.map { l -> l.copy(points = l.points?.map(f)) }) })
