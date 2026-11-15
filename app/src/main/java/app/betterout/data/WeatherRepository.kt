package app.betterout.data

import app.betterout.domain.Climate
import app.betterout.domain.WeatherPoint
import app.betterout.domain.WeatherReport
import app.betterout.domain.WeatherRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

/**
 * Meteo delle uscite. Ogni risposta è salvata in un file con il momento in cui è arrivata, così senza rete
 * resta l'ultimo dato con la sua età. Il meteo non tocca mai l'uscita: un errore qui non blocca nient'altro.
 */
class WeatherRepository(private val dir: File, private val now: () -> Long = System::currentTimeMillis, private val fetch: suspend (String) -> String = Http::get) {
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()

    /** L'ultima previsione salvata per questi punti e giorni; null se non c'è o se riguarda altri punti o altre date. */
    suspend fun cached(tripId: String, ask: WeatherRules.Plan.Ask): WeatherReport? =
        read<WeatherReport>(forecastFile(tripId))?.takeIf { it.key == ask.key }

    /** L'ultima previsione salvata per l'uscita, qualunque sia: serve a ricordare che tempo era previsto per un'uscita passata. */
    suspend fun lastSaved(tripId: String): WeatherReport? = read<WeatherReport>(forecastFile(tripId))

    /**
     * Restituisce la previsione, chiedendola al servizio se manca, se ha più di tre ore o se [force].
     * Lancia [java.io.IOException] se la richiesta non riesce: il dato salvato resta dov'è.
     */
    suspend fun forecast(tripId: String, ask: WeatherRules.Plan.Ask, force: Boolean = false): WeatherReport = lock.withLock {
        val saved = cached(tripId, ask)
        if (saved != null && !force && !WeatherRules.isStale(saved.fetchedAt, now())) return@withLock saved
        WeatherReport(ask.key, now(), WeatherForecast.forecast(ask, fetch)).also { write(forecastFile(tripId), json.encodeToString(WeatherReport.serializer(), it)) }
    }

    /** Medie del periodo negli anni passati; chieste una volta sola per punto e date. */
    suspend fun climate(tripId: String, point: WeatherPoint, start: LocalDate, end: LocalDate, today: LocalDate): Climate = lock.withLock {
        val key = "${point.lat},${point.lon},${point.elevation}|$start|$end"
        read<Climate>(climateFile(tripId))?.takeIf { it.key == key }
            ?: ClimateArchive.climate(key, point, WeatherRules.climateWindows(start, end, today), fetch).also { write(climateFile(tripId), json.encodeToString(Climate.serializer(), it)) }
    }

    suspend fun delete(tripId: String) {
        withContext(Dispatchers.IO) { forecastFile(tripId).delete(); climateFile(tripId).delete() }
    }

    private suspend inline fun <reified T> read(file: File): T? = withContext(Dispatchers.IO) {
        // Un file illeggibile vale come nessun dato: verrà riscritto alla prossima risposta.
        if (file.exists()) runCatching { json.decodeFromString<T>(file.readText()) }.getOrNull() else null
    }

    private suspend fun write(file: File, text: String) = withContext(Dispatchers.IO) { dir.mkdirs(); file.writeText(text) }

    private fun forecastFile(tripId: String) = File(dir, "$tripId.json")
    private fun climateFile(tripId: String) = File(dir, "$tripId-clima.json")
}
