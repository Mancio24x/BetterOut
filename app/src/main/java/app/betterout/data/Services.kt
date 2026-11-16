package app.betterout.data

import app.betterout.domain.Climate
import app.betterout.domain.ClimateDay
import app.betterout.domain.DayWeather
import app.betterout.domain.Geo
import app.betterout.domain.GeoPoint
import app.betterout.domain.HourWeather
import app.betterout.domain.PointForecast
import app.betterout.domain.WeatherPoint
import app.betterout.domain.WeatherRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.util.Locale

/** Richieste ai servizi esterni: partono solo su azione dell'utente e portano solo coordinate o il testo cercato. */
object Http {
    private const val USER_AGENT = "BetterOut/0.1 (Android; +https://github.com/Mancio24x/BetterOut)"

    /** Restituisce il corpo della risposta; qualunque problema di rete o risposta diversa da 200 è una [IOException]. */
    suspend fun get(url: String, connectTimeoutMs: Int = 10_000, readTimeoutMs: Int = 20_000): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
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

enum class PlaceKind { CIMA, RIFUGIO, PASSO, LAGO, LOCALITA, VALLE, GHIACCIAIO, CASCATA, BELVEDERE, PARCHEGGIO, IMPIANTO }

/** Un luogo trovato. [kind] e [area] (il comune) ci sono quando la fonte li dà; [elevation] e [timezone] solo da Open-Meteo. */
@Serializable
data class Place(
    val name: String, val latitude: Double, val longitude: Double, val elevation: Double? = null, val admin1: String? = null, val country: String? = null,
    val timezone: String? = null, val kind: PlaceKind? = null, val area: String? = null,
)

/**
 * Ricerca dei luoghi su tre fonti insieme: Nominatim e Photon (dati OpenStreetMap: cime, rifugi, passi, laghi)
 * e Open-Meteo (GeoNames: paesi, con quota e fuso orario). I risultati doppi sono uniti.
 * Una richiesta per fonte, solo quando l'utente preme "Cerca".
 */
object PlaceSearch {
    @Serializable private data class Response(val results: List<Place> = emptyList())

    /** Risultati uniti fin qui; [done] è vero quando hanno risposto tutte le fonti. */
    data class Progress(val places: List<Place>, val done: Boolean)

    /** Una fonte lenta non deve far aspettare: oltre questi tempi si rinuncia a lei e restano le altre. */
    private val searchFetch: suspend (String) -> String = { Http.get(it, connectTimeoutMs = 5_000, readTimeoutMs = 7_000) }

    /** Cime e rifugi delle Alpi inclusi nell'app; impostato all'avvio. */
    @Volatile var offline: OfflinePlaces? = null

    suspend fun search(query: String, language: String = "it", offline: OfflinePlaces? = this.offline, fetch: suspend (String) -> String = searchFetch): List<Place> =
        progressive(query, language, offline, fetch).last().places

    /**
     * Come [search], ma dà i risultati man mano che arrivano: prima quelli inclusi nell'app, poi le fonti online
     * senza aspettare la più lenta. L'ordine è sempre lo stesso, così la lista non si rimescola all'arrivo di una risposta.
     */
    fun progressive(query: String, language: String = "it", offline: OfflinePlaces? = this.offline, fetch: suspend (String) -> String = searchFetch): Flow<Progress> = channelFlow {
        // Un file illeggibile non ferma la ricerca online.
        val local = offline?.let { withContext(Dispatchers.IO) { try { it.search(query) } catch (e: IOException) { emptyList() } } }.orEmpty()
        if (local.isNotEmpty()) send(Progress(merge(local), done = false))
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val sources: List<suspend () -> List<Place>> = listOf(
            { parseNominatim(fetch("https://nominatim.openstreetmap.org/search?q=$q&format=jsonv2&limit=10&addressdetails=1&accept-language=$language")) },
            { parsePhoton(fetch("https://photon.komoot.io/api/?q=$q&limit=10")) },
            { parse(fetch("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=10&language=$language&format=json")) },
        )
        val answers = arrayOfNulls<List<Place>>(sources.size)
        var answered = 0
        val lock = Mutex()
        sources.forEachIndexed { i, source ->
            launch {
                val places = try { source() } catch (e: IOException) { null }
                lock.withLock {
                    answers[i] = places
                    answered++
                    val done = answered == sources.size
                    // Una fonte che non risponde non ferma le altre: è un errore solo se non risponde nessuna e l'app non ha trovato nulla.
                    if (done && local.isEmpty() && answers.all { it == null }) throw IOException("nessuna fonte di ricerca ha risposto")
                    if (places != null || done) send(Progress(merge(local + answers.filterNotNull().flatten()), done))
                }
            }
        }
    }

    /** Open-Meteo. */
    fun parse(body: String): List<Place> = parsing { json.decodeFromString<Response>(body).results }

    fun parseNominatim(body: String): List<Place> = parsing {
        json.parseToJsonElement(body).jsonArray.mapNotNull { e ->
            val o = e.jsonObject
            val address = o["address"] as? JsonObject
            fun text(from: JsonObject?, vararg keys: String) = keys.firstNotNullOfOrNull { k -> (from?.get(k) as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() } }
            val name = text(o, "name") ?: text(o, "display_name")?.substringBefore(',') ?: return@mapNotNull null
            Place(
                name, o.getValue("lat").jsonPrimitive.double, o.getValue("lon").jsonPrimitive.double, admin1 = text(address, "state", "region"), country = text(address, "country"),
                kind = placeKind(text(o, "category"), text(o, "type")), area = text(address, "municipality", "city", "town", "village", "county")?.takeIf { it != name },
            )
        }
    }

    fun parsePhoton(body: String): List<Place> = parsing {
        json.parseToJsonElement(body).jsonObject.getValue("features").jsonArray.mapNotNull { e ->
            val p = e.jsonObject.getValue("properties").jsonObject
            fun text(vararg keys: String) = keys.firstNotNullOfOrNull { k -> (p[k] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() } }
            val name = text("name") ?: return@mapNotNull null
            val c = e.jsonObject.getValue("geometry").jsonObject.getValue("coordinates").jsonArray
            Place(name, c[1].jsonPrimitive.double, c[0].jsonPrimitive.double, admin1 = text("state"), country = text("country"), kind = placeKind(text("osm_key"), text("osm_value")), area = text("city", "county")?.takeIf { it != name })
        }
    }

    /** Il tipo di luogo dalle etichette OpenStreetMap; null per quelli che a chi cammina non dicono niente. */
    fun placeKind(key: String?, value: String?): PlaceKind? = when {
        value == "peak" || value == "volcano" -> PlaceKind.CIMA
        value == "alpine_hut" || value == "wilderness_hut" -> PlaceKind.RIFUGIO
        key == "mountain_pass" || value == "saddle" || value == "mountain_pass" -> PlaceKind.PASSO
        key == "water" || value == "water" || value == "lake" || value == "reservoir" -> PlaceKind.LAGO
        value == "valley" -> PlaceKind.VALLE
        value == "glacier" -> PlaceKind.GHIACCIAIO
        value == "waterfall" -> PlaceKind.CASCATA
        value == "viewpoint" -> PlaceKind.BELVEDERE
        value == "parking" -> PlaceKind.PARCHEGGIO
        key == "aerialway" -> PlaceKind.IMPIANTO
        key == "place" || key == "boundary" -> PlaceKind.LOCALITA
        else -> null
    }

    /**
     * Unisce i risultati delle fonti: lo stesso luogo (stesso nome entro 500 m, o a meno di 50 m) compare una volta,
     * con i dati di tutte. Prima i luoghi di cui si conosce il tipo, nell'ordine dato dalle fonti.
     */
    fun merge(places: List<Place>, limit: Int = 20): List<Place> {
        val merged = ArrayList<Place>()
        for (p in places) {
            val i = merged.indexOfFirst { m ->
                val d = Geo.distance(GeoPoint(m.latitude, m.longitude), GeoPoint(p.latitude, p.longitude))
                d < 50 || d < 500 && m.name.trim().equals(p.name.trim(), ignoreCase = true)
            }
            if (i < 0) merged += p
            else merged[i] = merged[i].let { m -> m.copy(elevation = m.elevation ?: p.elevation, admin1 = m.admin1 ?: p.admin1, country = m.country ?: p.country, timezone = m.timezone ?: p.timezone, kind = m.kind ?: p.kind, area = m.area ?: p.area) }
        }
        return merged.sortedBy { it.kind == null }.take(limit)
    }
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

/**
 * Previsioni di Open-Meteo: una sola richiesta per tutti i punti, nel fuso della destinazione.
 * I dati hanno licenza CC BY 4.0: chi li mostra cita la fonte.
 */
object WeatherForecast {
    private const val DAILY = "weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_min,precipitation_sum,precipitation_probability_max,snowfall_sum,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,sunrise,sunset"
    private const val HOURLY = "weather_code,temperature_2m,apparent_temperature,precipitation_probability,precipitation,snowfall,wind_speed_10m,wind_gusts_10m,wind_direction_10m,freezing_level_height,is_day"

    suspend fun forecast(ask: WeatherRules.Plan.Ask, fetch: suspend (String) -> String = Http::get): List<PointForecast> =
        parse(fetch(url(ask)), ask.points)

    /** Le quote si mandano solo se le hanno tutti i punti; altrimenti le stima il servizio dal suo modello del terreno. */
    fun url(ask: WeatherRules.Plan.Ask): String {
        val elevation = if (ask.points.all { it.elevation != null }) "&elevation=" + ask.points.joinToString(",") { String.format(Locale.ROOT, "%.0f", it.elevation) } else ""
        return "https://api.open-meteo.com/v1/forecast?latitude=${ask.points.joinToString(",") { coordinate(it.lat) }}&longitude=${ask.points.joinToString(",") { coordinate(it.lon) }}$elevation" +
            "&timezone=auto&start_date=${ask.from}&end_date=${ask.to}&daily=$DAILY&hourly=$HOURLY"
    }

    /** Con un punto la risposta è un oggetto, con più punti un elenco nello stesso ordine della richiesta. */
    fun parse(body: String, points: List<WeatherPoint>): List<PointForecast> = parsing {
        val root = json.parseToJsonElement(body)
        val answers = if (root is JsonArray) root.map { it.jsonObject } else listOf(root.jsonObject)
        if (answers.size != points.size) throw IOException("previsioni mancanti nella risposta")
        points.zip(answers).map { (point, o) ->
            val daily = Columns(o.getValue("daily").jsonObject)
            val hourly = Columns(o.getValue("hourly").jsonObject)
            PointForecast(
                point = point, elevation = o["elevation"]?.number(),
                days = daily.times.indices.map { i ->
                    DayWeather(
                        date = daily.times[i], code = daily.int("weather_code", i), tempMax = daily.double("temperature_2m_max", i), tempMin = daily.double("temperature_2m_min", i),
                        feelsMin = daily.double("apparent_temperature_min", i), precipitationMm = daily.double("precipitation_sum", i), precipitationProbability = daily.int("precipitation_probability_max", i),
                        snowCm = daily.double("snowfall_sum", i), windKmh = daily.double("wind_speed_10m_max", i), gustsKmh = daily.double("wind_gusts_10m_max", i),
                        windDirection = daily.int("wind_direction_10m_dominant", i), sunrise = daily.text("sunrise", i), sunset = daily.text("sunset", i),
                    )
                },
                hours = hourly.times.indices.map { i ->
                    HourWeather(
                        time = hourly.times[i], code = hourly.int("weather_code", i), temp = hourly.double("temperature_2m", i), feels = hourly.double("apparent_temperature", i),
                        precipitationProbability = hourly.int("precipitation_probability", i), precipitationMm = hourly.double("precipitation", i), snowCm = hourly.double("snowfall", i),
                        windKmh = hourly.double("wind_speed_10m", i), gustsKmh = hourly.double("wind_gusts_10m", i), windDirection = hourly.int("wind_direction_10m", i),
                        freezingLevel = hourly.double("freezing_level_height", i), isDay = hourly.int("is_day", i)?.let { it == 1 },
                    )
                },
            )
        }
    }
}

/** Archivio storico di Open-Meteo (rianalisi): serve solo per le medie del periodo, che non sono una previsione. */
object ClimateArchive {
    suspend fun climate(key: String, point: WeatherPoint, windows: List<Pair<LocalDate, LocalDate>>, fetch: suspend (String) -> String = Http::get): Climate {
        val elevation = point.elevation?.let { "&elevation=" + String.format(Locale.ROOT, "%.0f", it) }.orEmpty()
        val body = fetch(
            "https://archive-api.open-meteo.com/v1/archive?latitude=${coordinate(point.lat)}&longitude=${coordinate(point.lon)}$elevation" +
                "&timezone=auto&start_date=${windows.minOf { it.first }}&end_date=${windows.maxOf { it.second }}&daily=temperature_2m_max,temperature_2m_min,precipitation_sum",
        )
        val (usedElevation, days) = parse(body)
        return WeatherRules.climate(key, usedElevation, days, windows)
    }

    fun parse(body: String): Pair<Double?, List<ClimateDay>> = parsing {
        val o = json.parseToJsonElement(body).jsonObject
        val daily = Columns(o.getValue("daily").jsonObject)
        o["elevation"]?.number() to daily.times.indices.map { i -> ClimateDay(daily.times[i], daily.double("temperature_2m_max", i), daily.double("temperature_2m_min", i), daily.double("precipitation_sum", i)) }
    }
}

private fun JsonElement.number(): Double? = if (this is JsonNull) null else jsonPrimitive.doubleOrNull

/** Le serie di Open-Meteo: un elenco di istanti e, per ogni grandezza, un elenco parallelo dove un valore può mancare. */
private class Columns(private val o: JsonObject) {
    val times: List<String> = o.getValue("time").jsonArray.map { it.jsonPrimitive.content }
    private fun cell(name: String, i: Int): JsonElement? = (o[name] as? JsonArray)?.getOrNull(i)?.takeIf { it !is JsonNull }
    fun double(name: String, i: Int): Double? = cell(name, i)?.jsonPrimitive?.doubleOrNull
    fun int(name: String, i: Int): Int? = double(name, i)?.let { Math.round(it).toInt() }
    fun text(name: String, i: Int): String? = cell(name, i)?.jsonPrimitive?.content
}
