package app.betterout.data

import app.betterout.domain.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** Richieste ai servizi esterni: partono solo su azione dell'utente e portano solo coordinate o il testo cercato. */
object Http {
    private const val USER_AGENT = "BetterOut/0.1 (Android; +https://github.com/Mancio24x/BetterOut)"

    /** Restituisce il corpo della risposta; qualunque problema di rete o risposta diversa da 200 è una [IOException]. */
    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (connection.responseCode != 200) throw IOException("HTTP ${connection.responseCode}")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

private val json = Json { ignoreUnknownKeys = true }
private fun coordinate(value: Double) = String.format(Locale.ROOT, "%.6f", value)
private fun <T> parsing(block: () -> T): T = try { block() } catch (e: RuntimeException) { throw IOException("risposta non leggibile", e) }

/** Calcola un tratto lungo i sentieri. Lancia [IOException] se non ci riesce: decidere cosa fare spetta a chi chiama. */
fun interface RoutingService {
    suspend fun route(from: GeoPoint, to: GeoPoint): List<GeoPoint>
}

/** Server pubblico di BRouter, profilo escursionistico di montagna. Una richiesta per tratto. */
class BRouterService(private val fetch: suspend (String) -> String = Http::get) : RoutingService {
    override suspend fun route(from: GeoPoint, to: GeoPoint): List<GeoPoint> {
        val lonlats = listOf(from, to).joinToString("|") { coordinate(it.lon) + "," + coordinate(it.lat) }
        return parse(fetch("https://brouter.de/brouter?lonlats=$lonlats&profile=hiking-mountain&alternativeidx=0&format=geojson"))
    }

    companion object {
        fun parse(body: String): List<GeoPoint> = parsing {
            val coordinates = json.parseToJsonElement(body).jsonObject.getValue("features").jsonArray.first().jsonObject.getValue("geometry").jsonObject.getValue("coordinates").jsonArray
            coordinates.map { c -> c.jsonArray.let { GeoPoint(it[1].jsonPrimitive.double, it[0].jsonPrimitive.double, it.getOrNull(2)?.jsonPrimitive?.double) } }
                .also { if (it.size < 2) throw IOException("tratto vuoto") }
        }
    }
}

@Serializable
data class Place(val name: String, val latitude: Double, val longitude: Double, val elevation: Double? = null, val admin1: String? = null, val country: String? = null, val timezone: String? = null)

/** Ricerca località di Open-Meteo (GeoNames). */
object PlaceSearch {
    @Serializable private data class Response(val results: List<Place> = emptyList())

    suspend fun search(query: String, language: String = "it", fetch: suspend (String) -> String = Http::get): List<Place> {
        val body = fetch("https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(query.trim(), "UTF-8")}&count=10&language=$language&format=json")
        return parse(body)
    }

    fun parse(body: String): List<Place> = parsing { json.decodeFromString<Response>(body).results }
}

/** Quote dal modello del terreno Copernicus GLO-90 tramite Open-Meteo: al più 100 punti per richiesta. */
object TerrainElevation {
    const val MAX_POINTS_PER_REQUEST = 100

    suspend fun elevations(points: List<GeoPoint>, fetch: suspend (String) -> String = Http::get): List<Double> = points.chunked(MAX_POINTS_PER_REQUEST).flatMap { chunk ->
        val body = fetch("https://api.open-meteo.com/v1/elevation?latitude=${chunk.joinToString(",") { coordinate(it.lat) }}&longitude=${chunk.joinToString(",") { coordinate(it.lon) }}")
        parse(body).also { if (it.size != chunk.size) throw IOException("quote mancanti nella risposta") }
    }

    fun parse(body: String): List<Double> = parsing {
        val array: JsonArray = (json.parseToJsonElement(body) as JsonObject).getValue("elevation").jsonArray
        array.map { it.jsonPrimitive.double }
    }
}
