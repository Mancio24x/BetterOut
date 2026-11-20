package app.betterout.domain

import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.xml.parsers.SAXParserFactory

/**
 * Un tracciato (`trk`) o un itinerario (`rte`) del file. I segmenti restano separati.
 * [start] e [end] sono il primo e l'ultimo orario dei punti, se il file li registra.
 */
data class GpxTrack(val name: String?, val segments: List<List<GeoPoint>>, val start: Instant? = null, val end: Instant? = null)

@kotlinx.serialization.Serializable
data class GpxWaypoint(val point: GeoPoint, val name: String?)

data class GpxFile(val version: String?, val tracks: List<GpxTrack>, val waypoints: List<GpxWaypoint>, val discardedPoints: Int)

enum class GpxError { NON_GPX, XML_NON_VALIDO, VUOTO }

sealed interface GpxResult {
    data class Ok(val file: GpxFile) : GpxResult
    data class Error(val reason: GpxError) : GpxResult
}

/** Lettura a flusso di GPX 1.0 e 1.1: il file non viene mai caricato per intero come albero. */
object GpxReader {
    private class NotGpx : SAXException()

    fun read(input: InputStream): GpxResult {
        val handler = Handler()
        try {
            val factory = SAXParserFactory.newInstance()
            // Un GPX non ha bisogno di DOCTYPE: rifiutarlo evita entità esterne. Non tutti i parser conoscono l'opzione.
            runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            factory.newSAXParser().parse(input, handler)
        } catch (e: NotGpx) {
            return GpxResult.Error(GpxError.NON_GPX)
        } catch (e: SAXException) {
            return GpxResult.Error(if (handler.sawRoot) GpxError.XML_NON_VALIDO else GpxError.NON_GPX)
        }
        val tracks = handler.tracks.mapNotNull { t -> t.segments.filter { it.isNotEmpty() }.takeIf { it.isNotEmpty() }?.let { GpxTrack(t.name, it, t.start, t.end) } }
        if (tracks.isEmpty()) return GpxResult.Error(GpxError.VUOTO)
        return GpxResult.Ok(GpxFile(handler.version, tracks, handler.waypoints, handler.discarded))
    }

    private class MutableTrack(var name: String? = null, val segments: MutableList<MutableList<GeoPoint>> = mutableListOf(), var start: Instant? = null, var end: Instant? = null)

    /** Orario ISO 8601 del GPX; senza fuso è inteso in UTC, come vuole lo standard. */
    internal fun parseTime(s: String): Instant? =
        runCatching { OffsetDateTime.parse(s).toInstant() }.recoverCatching { LocalDateTime.parse(s).toInstant(ZoneOffset.UTC) }.getOrNull()

    private class Handler : DefaultHandler() {
        var sawRoot = false
        var version: String? = null
        var discarded = 0
        val tracks = mutableListOf<MutableTrack>()
        val waypoints = mutableListOf<GpxWaypoint>()

        private var track: MutableTrack? = null
        private var pointKind: String? = null
        private var point: GeoPoint? = null
        private var pointName: String? = null
        private var pointTime: Instant? = null
        private val text = StringBuilder()

        override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes) {
            val name = qName.substringAfter(':')
            if (!sawRoot) {
                if (name != "gpx") throw NotGpx()
                sawRoot = true
                version = attributes.getValue("version")
                return
            }
            text.setLength(0)
            when (name) {
                "trk" -> track = MutableTrack().also(tracks::add)
                "rte" -> track = MutableTrack(segments = mutableListOf(mutableListOf())).also(tracks::add)
                "trkseg" -> track?.segments?.add(mutableListOf())
                "trkpt", "rtept", "wpt" -> {
                    val lat = attributes.getValue("lat")?.trim()?.toDoubleOrNull()
                    val lon = attributes.getValue("lon")?.trim()?.toDoubleOrNull()
                    pointKind = name
                    pointName = null
                    pointTime = null
                    point = if (lat != null && lon != null && Geo.isValid(lat, lon)) GeoPoint(lat, lon) else null
                    if (point == null) discarded++
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            text.appendRange(ch, start, start + length)
        }

        override fun endElement(uri: String?, localName: String?, qName: String) {
            when (val name = qName.substringAfter(':')) {
                "ele" -> point = point?.copy(ele = text.toString().trim().toDoubleOrNull()?.takeIf { it.isFinite() })
                "name" -> text.toString().trim().takeIf { it.isNotEmpty() }.let { if (pointKind != null) pointName = it else if (track != null && track?.name == null) track?.name = it }
                "time" -> if (pointKind != null) pointTime = parseTime(text.toString().trim())
                "trk", "rte" -> track = null
                "trkpt", "rtept", "wpt" -> {
                    val p = point
                    if (p != null) {
                        if (name == "wpt") waypoints += GpxWaypoint(p, pointName) else track?.let { t ->
                            t.segments.lastOrNull()?.add(p)
                            pointTime?.let { time ->
                                val (start, end) = t.start to t.end
                                if (start == null || time < start) t.start = time
                                if (end == null || time > end) t.end = time
                            }
                        }
                    }
                    pointKind = null
                    point = null
                }
            }
        }
    }
}

object GpxWriter {
    /** Scrive un GPX 1.1 con un tracciato; ogni lista di [segments] diventa un `trkseg` separato. */
    fun write(name: String, segments: List<List<GeoPoint>>, waypoints: List<GpxWaypoint> = emptyList(), description: String? = null): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"BetterOut\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        for (w in waypoints) {
            append("  <wpt ").append(coordinates(w.point)).append('>')
            w.point.ele?.let { append("<ele>").append(number(it)).append("</ele>") }
            w.name?.let { append("<name>").append(escape(it)).append("</name>") }
            append("</wpt>\n")
        }
        append("  <trk>\n    <name>").append(escape(name)).append("</name>\n")
        description?.let { append("    <desc>").append(escape(it)).append("</desc>\n") }
        for (segment in segments) {
            append("    <trkseg>\n")
            for (p in segment) {
                append("      <trkpt ").append(coordinates(p))
                if (p.ele != null) append("><ele>").append(number(p.ele)).append("</ele></trkpt>\n") else append("/>\n")
            }
            append("    </trkseg>\n")
        }
        append("  </trk>\n</gpx>\n")
    }

    private fun coordinates(p: GeoPoint) = "lat=\"${number(p.lat)}\" lon=\"${number(p.lon)}\""

    /** Numero senza notazione esponenziale e senza perdere cifre. */
    private fun number(value: Double): String = BigDecimal.valueOf(value).toPlainString()

    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
