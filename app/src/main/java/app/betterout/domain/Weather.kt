package app.betterout.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

enum class WeatherPointKind { LOCALITA, PARTENZA, QUOTA_MASSIMA }

/** Un punto per cui si chiede il meteo. [elevation] è la quota nota dal percorso; null se la deve stimare il servizio. */
@Serializable
data class WeatherPoint(val kind: WeatherPointKind, val lat: Double, val lon: Double, val elevation: Double? = null)

/** Un giorno di previsione. Ogni valore è null quando il servizio non lo fornisce: non viene mai sostituito. */
@Serializable
data class DayWeather(
    val date: String, val code: Int? = null, val tempMax: Double? = null, val tempMin: Double? = null, val feelsMin: Double? = null,
    val precipitationMm: Double? = null, val precipitationProbability: Int? = null, val snowCm: Double? = null,
    val windKmh: Double? = null, val gustsKmh: Double? = null, val windDirection: Int? = null,
    val sunrise: String? = null, val sunset: String? = null,
)

/** Un'ora di previsione; [time] è l'ora locale della destinazione, "2026-11-21T07:00". */
@Serializable
data class HourWeather(
    val time: String, val code: Int? = null, val temp: Double? = null, val feels: Double? = null,
    val precipitationProbability: Int? = null, val precipitationMm: Double? = null, val snowCm: Double? = null,
    val windKmh: Double? = null, val gustsKmh: Double? = null, val windDirection: Int? = null,
    val freezingLevel: Double? = null, val isDay: Boolean? = null,
)

/** La previsione di un punto. [elevation] è la quota a cui il servizio l'ha calcolata. */
@Serializable
data class PointForecast(val point: WeatherPoint, val elevation: Double? = null, val days: List<DayWeather> = emptyList(), val hours: List<HourWeather> = emptyList()) {
    /** true se la quota non viene dal percorso ma dal modello del terreno del servizio: è una stima. */
    val elevationIsEstimate: Boolean get() = point.elevation == null
    fun day(date: LocalDate): DayWeather? = days.firstOrNull { it.date == date.toString() }
    fun hours(date: LocalDate): List<HourWeather> = hours.filter { it.time.startsWith(date.toString()) }
}

/** Una risposta del servizio salvata con il momento in cui è arrivata. [key] dice per quali punti e giorni vale. */
@Serializable
data class WeatherReport(val key: String, val fetchedAt: Long, val points: List<PointForecast>)

/** Medie degli anni passati nello stesso periodo: non è una previsione. [from] e [to] sono il periodo nell'anno più recente considerato. */
@Serializable
data class Climate(val key: String, val elevation: Double? = null, val from: String, val to: String, val years: Int, val typicalMin: Double? = null, val typicalMax: Double? = null, val wetDays: Int = 0, val countedDays: Int = 0)

@Serializable
data class ClimateDay(val date: String, val tempMax: Double? = null, val tempMin: Double? = null, val precipitationMm: Double? = null)

/** Tipo di tempo, dai codici WMO usati da Open-Meteo. */
enum class Sky { SERENO, POCO_NUVOLOSO, PARZIALMENTE_NUVOLOSO, COPERTO, NEBBIA, PIOVIGGINE, PIOVIGGINE_GELATA, PIOGGIA_DEBOLE, PIOGGIA, PIOGGIA_FORTE, PIOGGIA_GELATA, NEVE_DEBOLE, NEVE, NEVE_FORTE, ROVESCI, ROVESCI_NEVE, TEMPORALE, TEMPORALE_GRANDINE }

object WeatherRules {
    /** Open-Meteo dà previsioni da oggi fino a oggi + 15 (sedici giorni). Provato: oltre risponde 400. */
    const val HORIZON_DAYS = 15L
    const val MAX_AGE_MILLIS = 3 * 60 * 60 * 1000L
    /** Due punti sono "distinti" se li separano almeno questi metri di quota. */
    const val DISTINCT_ELEVATION_METERS = 100.0
    const val WET_DAY_MM = 1.0
    private const val CLIMATE_YEARS = 5
    private const val CLIMATE_MARGIN_DAYS = 3L
    /** L'archivio storico arriva con qualche giorno di ritardo. */
    private const val ARCHIVE_DELAY_DAYS = 7L

    sealed interface Plan {
        /** Manca la data dell'uscita. */
        data object NoDate : Plan
        /** Mancano sia la località sia un percorso. */
        data object NoPlace : Plan
        /** L'uscita è finita: non c'è niente da prevedere. */
        data object Past : Plan
        /** Tutta l'uscita è oltre l'orizzonte; le previsioni ci saranno da [availableFrom]. */
        data class TooEarly(val start: LocalDate, val end: LocalDate, val availableFrom: LocalDate, val points: List<WeatherPoint>) : Plan
        /** Si può chiedere da [from] a [to]; i giorni dell'uscita coperti vanno da [firstDay] a [lastDay], quelli dopo [lastDay] non ancora se [truncated]. */
        data class Ask(val points: List<WeatherPoint>, val from: LocalDate, val to: LocalDate, val firstDay: LocalDate, val lastDay: LocalDate, val truncated: Boolean) : Plan {
            val key: String get() = points.joinToString(";") { "${it.kind}:${it.lat},${it.lon},${it.elevation}" } + "|$from|$to"
            val tripDays: List<LocalDate> get() = generateSequence(firstDay) { it.plusDays(1) }.takeWhile { !it.isAfter(lastDay) }.toList()
            /** I giorni chiesti attorno all'uscita, che non ne fanno parte. */
            val nearbyDays: List<LocalDate> get() = listOf(from, to).filter { it.isBefore(firstDay) || it.isAfter(lastDay) }.distinct()
        }
    }

    /**
     * I punti rappresentativi. Con un percorso: la partenza e, se ha le quote ed è distinto, il punto più alto.
     * Senza percorso: la località dell'uscita. La quota massima si chiede solo se anche la partenza ha la quota,
     * perché la richiesta porta le quote di tutti i punti o di nessuno.
     */
    fun points(latitude: Double?, longitude: Double?, routeLines: List<List<GeoPoint>>?): List<WeatherPoint> {
        val start = routeLines?.firstOrNull { it.isNotEmpty() }?.first()
        if (start == null) return if (latitude != null && longitude != null) listOf(WeatherPoint(WeatherPointKind.LOCALITA, latitude, longitude)) else emptyList()
        val departure = WeatherPoint(WeatherPointKind.PARTENZA, start.lat, start.lon, start.ele)
        val top = routeLines.flatten().filter { it.ele != null }.maxByOrNull { it.ele!! }
        return if (start.ele != null && top?.ele != null && top.ele - start.ele >= DISTINCT_ELEVATION_METERS) listOf(departure, WeatherPoint(WeatherPointKind.QUOTA_MASSIMA, top.lat, top.lon, top.ele)) else listOf(departure)
    }

    fun plan(start: LocalDate?, end: LocalDate?, today: LocalDate, points: List<WeatherPoint>): Plan {
        if (start == null) return Plan.NoDate
        if (points.isEmpty()) return Plan.NoPlace
        val last = end?.takeIf { it.isAfter(start) } ?: start
        val horizon = today.plusDays(HORIZON_DAYS)
        if (last.isBefore(today)) return Plan.Past
        if (start.isAfter(horizon)) return Plan.TooEarly(start, last, start.minusDays(HORIZON_DAYS), points)
        val firstDay = maxOf(start, today)
        val lastDay = minOf(last, horizon)
        return Plan.Ask(points, from = maxOf(today, firstDay.minusDays(1)), to = minOf(horizon, lastDay.plusDays(1)), firstDay = firstDay, lastDay = lastDay, truncated = last.isAfter(horizon))
    }

    fun isStale(fetchedAt: Long, now: Long): Boolean = now - fetchedAt > MAX_AGE_MILLIS

    /**
     * Gli stessi giorni dell'uscita (con tre giorni di margine per parte) negli ultimi cinque anni già in archivio.
     * Ogni coppia è un intervallo di date, dal più recente.
     */
    fun climateWindows(start: LocalDate, end: LocalDate, today: LocalDate): List<Pair<LocalDate, LocalDate>> {
        val from = start.minusDays(CLIMATE_MARGIN_DAYS)
        val to = end.plusDays(CLIMATE_MARGIN_DAYS)
        return generateSequence(1L) { it + 1 }.map { from.minusYears(it) to to.minusYears(it) }
            .filter { it.second.isBefore(today.minusDays(ARCHIVE_DELAY_DAYS)) }.take(CLIMATE_YEARS).toList()
    }

    /** Medie delle minime e delle massime e giorni con almeno 1 mm, sui soli giorni dentro le [windows]. */
    fun climate(key: String, elevation: Double?, days: List<ClimateDay>, windows: List<Pair<LocalDate, LocalDate>>): Climate {
        val inside = days.filter { d -> LocalDate.parse(d.date).let { date -> windows.any { !date.isBefore(it.first) && !date.isAfter(it.second) } } }
        val rain = inside.mapNotNull { it.precipitationMm }
        return Climate(
            key = key, elevation = elevation, from = windows.first().first.toString(), to = windows.first().second.toString(), years = windows.size,
            typicalMin = inside.mapNotNull { it.tempMin }.takeIf { it.isNotEmpty() }?.average(),
            typicalMax = inside.mapNotNull { it.tempMax }.takeIf { it.isNotEmpty() }?.average(),
            wetDays = rain.count { it >= WET_DAY_MM }, countedDays = rain.size,
        )
    }

    fun sky(code: Int?): Sky? = when (code) {
        0 -> Sky.SERENO; 1 -> Sky.POCO_NUVOLOSO; 2 -> Sky.PARZIALMENTE_NUVOLOSO; 3 -> Sky.COPERTO
        45, 48 -> Sky.NEBBIA
        51, 53, 55 -> Sky.PIOVIGGINE; 56, 57 -> Sky.PIOVIGGINE_GELATA
        61 -> Sky.PIOGGIA_DEBOLE; 63 -> Sky.PIOGGIA; 65 -> Sky.PIOGGIA_FORTE; 66, 67 -> Sky.PIOGGIA_GELATA
        71, 77 -> Sky.NEVE_DEBOLE; 73 -> Sky.NEVE; 75 -> Sky.NEVE_FORTE
        80, 81, 82 -> Sky.ROVESCI; 85, 86 -> Sky.ROVESCI_NEVE
        95 -> Sky.TEMPORALE; 96, 99 -> Sky.TEMPORALE_GRANDINE
        else -> null
    }

    /** Settore della rosa dei venti da cui soffia: 0 = nord, 1 = nord-est, … 7 = nord-ovest. */
    fun compassSector(degrees: Int): Int = Math.floorMod(Math.round(degrees / 45.0).toInt(), 8)

    /** Vento medio e raffica massima fra le ore [fromHour] e [toHour] comprese; null se mancano i dati. */
    fun windBetween(hours: List<HourWeather>, fromHour: Int, toHour: Int): Pair<Double, Double?>? {
        val slot = hours.filter { LocalDateTime.parse(it.time).hour in fromHour..toHour }
        val wind = slot.mapNotNull { it.windKmh }.takeIf { it.isNotEmpty() } ?: return null
        return wind.average() to slot.mapNotNull { it.gustsKmh }.maxOrNull()
    }

    /** Quota minima e massima dello zero termico nella giornata. */
    fun freezingRange(hours: List<HourWeather>): Pair<Double, Double>? {
        val levels = hours.mapNotNull { it.freezingLevel }
        return if (levels.isEmpty()) null else levels.min() to levels.max()
    }

    /** Minuti fra il rientro stimato e il tramonto: negativi se si rientra col buio. */
    fun daylightMarginMinutes(returnAt: LocalDateTime, sunset: String?): Long? =
        sunset?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }?.let { ChronoUnit.MINUTES.between(returnAt, it) }
}
