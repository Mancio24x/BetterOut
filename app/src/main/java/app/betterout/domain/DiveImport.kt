package app.betterout.domain

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToInt

/** Un punto del profilo: secondi dall'inizio, profondità in metri, temperatura dell'acqua se c'è. */
data class DiveSample(val seconds: Int, val depthMeters: Double, val tempC: Double? = null)

/** Un'immersione letta da un file, prima di entrare nel logbook. Le misure sono metriche. */
data class ImportedDive(
    val date: LocalDate,
    val startTime: LocalTime? = null,
    val number: Int? = null,
    val siteName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val durationMinutes: Int? = null,
    val maxDepthMeters: Double? = null,
    val averageDepthMeters: Double? = null,
    val waterTempC: Double? = null,
    val visibilityMeters: Double? = null,
    val oxygenPercent: Int = 21,
    val tankLiters: Double? = null,
    val startBar: Int? = null,
    val endBar: Int? = null,
    val buddy: String = "",
    val notes: String = "",
    val samples: List<DiveSample> = emptyList(),
)

enum class DiveFileFormat { FIT, UDDF, SUBSURFACE }

sealed interface DiveImportResult {
    data class Ok(val format: DiveFileFormat, val dives: List<ImportedDive>) : DiveImportResult
    /** [reason]: NON_RICONOSCIUTO, ROVINATO o NESSUNA_IMMERSIONE. */
    data class Error(val reason: DiveImportError) : DiveImportResult
}

enum class DiveImportError { NON_RICONOSCIUTO, ROVINATO, NESSUNA_IMMERSIONE }

object DiveImport {
    /** Riconosce il formato dal contenuto, non dal nome: FIT dalla firma ".FIT", gli XML dall'elemento radice. */
    fun read(bytes: ByteArray, zone: ZoneId = ZoneId.systemDefault()): DiveImportResult {
        val result = try {
            when {
                bytes.size >= 12 && String(bytes, 8, 4, Charsets.US_ASCII) == ".FIT" -> DiveImportResult.Ok(DiveFileFormat.FIT, FitDives.parse(FitReader.read(bytes), zone))
                else -> {
                    val root = Xml.parse(bytes) ?: return DiveImportResult.Error(DiveImportError.NON_RICONOSCIUTO)
                    when (root.localName ?: root.tagName) {
                        "uddf" -> DiveImportResult.Ok(DiveFileFormat.UDDF, Uddf.parse(root))
                        "divelog", "dives" -> DiveImportResult.Ok(DiveFileFormat.SUBSURFACE, Subsurface.parse(root))
                        else -> return DiveImportResult.Error(DiveImportError.NON_RICONOSCIUTO)
                    }
                }
            }
        } catch (e: Exception) {
            // FIT illeggibile, XML malformato o con DTD (rifiutata per sicurezza), valori fuori posto.
            return DiveImportResult.Error(DiveImportError.ROVINATO)
        }
        return if (result.dives.isEmpty()) DiveImportResult.Error(DiveImportError.NESSUNA_IMMERSIONE) else result
    }

    /** Durata in minuti dal profilo, se il file non la dà. */
    internal fun minutesOf(samples: List<DiveSample>): Int? = samples.lastOrNull()?.seconds?.takeIf { it > 0 }?.let { (it / 60.0).roundToInt() }

    internal fun bar(value: Double?): Int? = value?.takeIf { it > 0 }?.roundToInt()
}

// ---- FIT ----

/**
 * Le immersioni di un file FIT di un computer subacqueo. Messaggi usati (profilo FIT pubblico):
 * session 18 (inizio, durata, posizione), record 20 (profondità 92, temperatura 13), dive_summary 268
 * (profondità media 2 e massima 3, numero 10, tempo di fondo 11), dive_gas 259 (ossigeno 1, stato 2),
 * tank_summary 323 (pressione iniziale 1 e finale 2), activity 34 (ora locale 5).
 */
object FitDives {
    private const val FIT_EPOCH = 631_065_600L // 31 dicembre 1989, in secondi Unix
    private const val SEMICIRCLE = 180.0 / 2147483648.0

    fun parse(messages: List<FitMessage>, zone: ZoneId): List<ImportedDive> {
        val records = messages.filter { it.number == 20 && it.double(92) != null }
        val sessions = messages.filter { it.number == 18 }
        val summaries = messages.filter { it.number == 268 }
        if (records.isEmpty() && summaries.isEmpty()) return emptyList()

        val startFit = sessions.firstNotNullOfOrNull { it.long(2) } ?: records.firstNotNullOfOrNull { it.long(253) } ?: return emptyList()
        // L'ora locale del file vale più del fuso del telefono: l'immersione può essere stata fatta altrove.
        val activity = messages.firstOrNull { it.number == 34 && it.long(5) != null && it.long(253) != null }
        val offsetSeconds = activity?.let { it.long(5)!! - it.long(253)!! }
        val instant = Instant.ofEpochSecond(startFit + FIT_EPOCH)
        val local = if (offsetSeconds != null && kotlin.math.abs(offsetSeconds) <= 18 * 3600) LocalDateTime.ofInstant(instant, ZoneOffset.ofTotalSeconds(offsetSeconds.toInt())) else LocalDateTime.ofInstant(instant, zone)

        val samples = records.mapNotNull { r ->
            val t = r.long(253) ?: return@mapNotNull null
            DiveSample((t - startFit).toInt().coerceAtLeast(0), r.double(92)!! / 1000.0, r.double(13))
        }.sortedBy { it.seconds }

        // Il riepilogo della sessione (riferimento 18) vale per tutta l'immersione; quelli dei giri per un tratto.
        val summary = summaries.firstOrNull { it.long(0) == 18L } ?: summaries.lastOrNull()
        val session = sessions.firstOrNull()
        val gas = messages.filter { it.number == 259 && it.long(2) != 0L }.minByOrNull { it.long(254) ?: 0 }
        val tank = messages.firstOrNull { it.number == 323 }
        // Alcuni computer scrivono 0, 0 quando la posizione manca.
        val lat = (session?.long(3) ?: records.firstNotNullOfOrNull { it.long(0) })?.takeIf { it != 0L }
        val lon = (session?.long(4) ?: records.firstNotNullOfOrNull { it.long(1) })?.takeIf { it != 0L }

        val seconds = summary?.double(11)?.div(1000) ?: session?.double(7)?.div(1000)
        return listOf(
            ImportedDive(
                date = local.toLocalDate(), startTime = local.toLocalTime().withSecond(0).withNano(0),
                number = summary?.long(10)?.toInt()?.takeIf { it > 0 },
                latitude = if (lat != null && lon != null) lat * SEMICIRCLE else null,
                longitude = if (lat != null && lon != null) lon * SEMICIRCLE else null,
                durationMinutes = seconds?.let { (it / 60.0).roundToInt() } ?: DiveImport.minutesOf(samples),
                maxDepthMeters = summary?.double(3)?.div(1000) ?: samples.maxOfOrNull { it.depthMeters },
                averageDepthMeters = summary?.double(2)?.div(1000),
                waterTempC = samples.mapNotNull { it.tempC }.minOrNull(),
                oxygenPercent = gas?.long(1)?.toInt()?.takeIf { it in 21..100 } ?: 21,
                startBar = DiveImport.bar(tank?.double(1)?.div(100)),
                endBar = DiveImport.bar(tank?.double(2)?.div(100)),
                samples = samples,
            ),
        )
    }
}

// ---- XML comune ----

internal object Xml {
    /** L'elemento radice, o null se non è XML. Entità esterne e DTD sono disattivate. */
    fun parse(bytes: ByteArray): Element? {
        val head = String(bytes, 0, minOf(bytes.size, 512), Charsets.UTF_8).trimStart('\uFEFF', ' ', '\n', '\r', '\t')
        if (!head.startsWith("<")) return null
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.isExpandEntityReferences = false
        listOf(
            "http://apache.org/xml/features/disallow-doctype-decl" to true,
            "http://xml.org/sax/features/external-general-entities" to false,
            "http://xml.org/sax/features/external-parameter-entities" to false,
        ).forEach { (feature, on) -> runCatching { factory.setFeature(feature, on) } }
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes)).documentElement
    }

    fun Element.children(name: String): List<Element> {
        val out = ArrayList<Element>()
        val nodes = childNodes
        for (i in 0 until nodes.length) (nodes.item(i) as? Element)?.let { if ((it.localName ?: it.tagName) == name) out += it }
        return out
    }

    fun Element.child(name: String): Element? = children(name).firstOrNull()

    /** Il primo discendente con quel nome, a qualunque profondità. */
    fun Element.find(name: String): Element? {
        val nodes = getElementsByTagNameNS("*", name)
        return (if (nodes.length > 0) nodes.item(0) else getElementsByTagName(name).item(0)) as? Element
    }

    fun Element.findAll(name: String): List<Element> {
        val nodes = getElementsByTagNameNS("*", name).takeIf { it.length > 0 } ?: getElementsByTagName(name)
        return (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    fun Element.text(name: String): String? = find(name)?.textContent?.trim()?.ifEmpty { null }

    fun Element.number(name: String): Double? = text(name)?.toDoubleOrNull()
}

// ---- UDDF ----

/** Universal Dive Data Format. Unità del sistema internazionale: metri, kelvin, pascal, metri cubi, secondi. */
object Uddf {
    fun parse(root: Element): List<ImportedDive> = with(Xml) {
        val sites = root.findAll("site").associateBy { it.getAttribute("id") }
        val mixes = root.findAll("mix").associateBy { it.getAttribute("id") }
        val buddies = root.findAll("buddy").associateBy { it.getAttribute("id") }
        root.findAll("dive").mapNotNull { dive ->
            val before = dive.child("informationbeforedive")
            val after = dive.child("informationafterdive")
            val start = before?.text("datetime")?.let(::dateTime) ?: return@mapNotNull null
            val site = before.children("link").firstNotNullOfOrNull { sites[it.getAttribute("ref")] }
            val buddy = before.children("link").mapNotNull { buddies[it.getAttribute("ref")] }
                .map { b -> listOfNotNull(b.text("firstname"), b.text("lastname")).joinToString(" ") }.filter { it.isNotBlank() }.joinToString(", ")
            val tank = dive.find("tankdata")
            val mix = tank?.children("link")?.firstNotNullOfOrNull { mixes[it.getAttribute("ref")] }
            val samples = dive.findAll("waypoint").mapNotNull { w ->
                val t = w.number("divetime") ?: return@mapNotNull null
                val d = w.number("depth") ?: return@mapNotNull null
                DiveSample(t.roundToInt(), d, w.number("temperature")?.let(::kelvin))
            }
            ImportedDive(
                date = start.toLocalDate(), startTime = start.toLocalTime().withSecond(0).withNano(0),
                number = before.text("divenumber")?.toIntOrNull(),
                siteName = site?.text("name").orEmpty(),
                latitude = site?.find("geography")?.number("latitude"),
                longitude = site?.find("geography")?.number("longitude"),
                durationMinutes = after?.number("diveduration")?.let { (it / 60.0).roundToInt() } ?: DiveImport.minutesOf(samples),
                maxDepthMeters = after?.number("greatestdepth") ?: samples.maxOfOrNull { it.depthMeters },
                averageDepthMeters = after?.number("averagedepth"),
                waterTempC = after?.number("lowesttemperature")?.let(::kelvin) ?: samples.mapNotNull { it.tempC }.minOrNull(),
                visibilityMeters = after?.number("visibility"),
                oxygenPercent = mix?.number("o2")?.let { (it * 100).roundToInt() }?.takeIf { it in 21..100 } ?: 21,
                tankLiters = tank?.number("tankvolume")?.let { it * 1000 },
                startBar = DiveImport.bar(tank?.number("tankpressurebegin")?.div(100_000)),
                endBar = DiveImport.bar(tank?.number("tankpressureend")?.div(100_000)),
                buddy = buddy,
                notes = after?.child("notes")?.findAll("para")?.joinToString("\n") { it.textContent.trim() }.orEmpty(),
                samples = samples,
            )
        }
    }

    /** Da kelvin a gradi Celsius, al decimo: 277,45 K diventa 4,3 °C e non 4,300000000000011. */
    private fun kelvin(k: Double) = Math.round((k - 273.15) * 10) / 10.0

    private fun dateTime(text: String): LocalDateTime? = try {
        LocalDateTime.parse(text.take(19))
    } catch (e: DateTimeParseException) {
        runCatching { LocalDate.parse(text.take(10)).atStartOfDay() }.getOrNull()
    }
}

// ---- Subsurface ----

/** Il logbook XML di Subsurface: valori con l'unità scritta ("18.5 m", "45:30 min", "200.0 bar", "32.0%"). */
object Subsurface {
    fun parse(root: Element): List<ImportedDive> = with(Xml) {
        val sites = root.findAll("site").associateBy { it.getAttribute("uuid").trim() }
        root.findAll("dive").mapNotNull { dive ->
            val date = runCatching { LocalDate.parse(dive.getAttribute("date")) }.getOrNull() ?: return@mapNotNull null
            val time = runCatching { LocalTime.parse(dive.getAttribute("time")) }.getOrNull()
            val site = sites[dive.getAttribute("divesiteid").trim()]
            // Il formato più vecchio (radice <dives>) mette luogo, coordinate, profondità e campioni direttamente nell'immersione.
            val gps = (site?.getAttribute("gps") ?: dive.child("gps")?.textContent)?.trim()?.split(Regex("\\s+"))?.mapNotNull { it.toDoubleOrNull() }
            val cylinder = dive.child("cylinder")
            val computer = dive.child("divecomputer")
            val samples = (computer ?: dive).children("sample").mapNotNull { s ->
                val t = duration(s.getAttribute("time")) ?: return@mapNotNull null
                val d = number(s.getAttribute("depth")) ?: return@mapNotNull null
                DiveSample(t, d, number(s.getAttribute("temp")))
            }
            val depth = computer?.child("depth") ?: dive.child("depth")
            val temperature = computer?.child("temperature") ?: dive.child("temperature")
            ImportedDive(
                date = date, startTime = time?.withSecond(0),
                number = dive.getAttribute("number").toIntOrNull(),
                siteName = (site?.getAttribute("name") ?: dive.child("location")?.textContent)?.trim().orEmpty(),
                latitude = gps?.getOrNull(0), longitude = gps?.getOrNull(1),
                durationMinutes = duration(dive.getAttribute("duration"))?.let { (it / 60.0).roundToInt() } ?: DiveImport.minutesOf(samples),
                maxDepthMeters = number(depth?.getAttribute("max")) ?: samples.maxOfOrNull { it.depthMeters },
                averageDepthMeters = number(depth?.getAttribute("mean")),
                waterTempC = number(temperature?.getAttribute("water")) ?: samples.mapNotNull { it.tempC }.minOrNull(),
                visibilityMeters = null,
                oxygenPercent = number(cylinder?.getAttribute("o2"))?.roundToInt()?.takeIf { it in 21..100 } ?: 21,
                tankLiters = number(cylinder?.getAttribute("size")),
                startBar = DiveImport.bar(number(cylinder?.getAttribute("start"))),
                endBar = DiveImport.bar(number(cylinder?.getAttribute("end"))),
                buddy = dive.child("buddy")?.textContent?.trim().orEmpty(),
                notes = dive.child("notes")?.textContent?.trim().orEmpty(),
                samples = samples,
            )
        }
    }

    /** Il numero all'inizio di "18.5 m" o "32.0%"; null per un attributo vuoto. */
    internal fun number(text: String?): Double? = text?.trim()?.let { Regex("^-?[0-9]+(\\.[0-9]+)?").find(it)?.value?.toDoubleOrNull() }

    /** "45:30 min" → 2730 secondi; "1:02:03" → ore, minuti e secondi. */
    internal fun duration(text: String?): Int? {
        val parts = text?.trim()?.substringBefore(' ')?.split(':')?.map { it.toIntOrNull() ?: return null } ?: return null
        return when (parts.size) { 1 -> parts[0] * 60; 2 -> parts[0] * 60 + parts[1]; 3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]; else -> null }
    }
}
