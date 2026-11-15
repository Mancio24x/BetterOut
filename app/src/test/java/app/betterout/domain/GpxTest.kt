package app.betterout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxTest {
    private fun read(xml: String) = GpxReader.read(xml.byteInputStream())
    private fun ok(xml: String) = (read(xml) as GpxResult.Ok).file
    private fun error(xml: String) = (read(xml) as GpxResult.Error).reason

    private val gpx11 = """<?xml version="1.0"?>
        <gpx version="1.1" creator="t" xmlns="http://www.topografix.com/GPX/1/1">
          <metadata><name>File</name></metadata>
          <wpt lat="46.52" lon="12.01"><ele>2100</ele><name>Rifugio</name></wpt>
          <trk><name>Tappa 1</name>
            <trkseg><trkpt lat="46.5" lon="12.0"><ele>2000</ele></trkpt><trkpt lat="46.51" lon="12.0"><ele>2100.5</ele></trkpt></trkseg>
            <trkseg><trkpt lat="46.6" lon="12.0"><ele>1900</ele></trkpt><trkpt lat="46.61" lon="12.0"><ele>1950</ele></trkpt></trkseg>
          </trk>
          <trk><name>Tappa 2</name><trkseg><trkpt lat="46.7" lon="12.1"/><trkpt lat="46.71" lon="12.1"/></trkseg></trk>
        </gpx>"""

    @Test fun `legge tracciati, segmenti separati e punti di interesse di un GPX 1_1`() {
        val file = ok(gpx11)
        assertEquals("1.1", file.version)
        assertEquals(listOf("Tappa 1", "Tappa 2"), file.tracks.map { it.name })
        assertEquals(listOf(2, 2), file.tracks[0].segments.map { it.size })
        assertEquals(GeoPoint(46.51, 12.0, 2100.5), file.tracks[0].segments[0][1])
        assertEquals(listOf(GpxWaypoint(GeoPoint(46.52, 12.01, 2100.0), "Rifugio")), file.waypoints)
        assertEquals(0, file.discardedPoints)
    }

    @Test fun `le quote assenti restano assenti`() {
        assertTrue(ok(gpx11).tracks[1].segments[0].all { it.ele == null })
    }

    @Test fun `legge un GPX 1_0 con itinerario rte`() {
        val file = ok("""<gpx version="1.0" xmlns="http://www.topografix.com/GPX/1/0"><rte><name>Giro</name><rtept lat="45" lon="7"><ele>300</ele></rtept><rtept lat="45.1" lon="7.1"/></rte></gpx>""")
        assertEquals("1.0", file.version)
        assertEquals("Giro", file.tracks.single().name)
        assertEquals(listOf(GeoPoint(45.0, 7.0, 300.0), GeoPoint(45.1, 7.1)), file.tracks.single().segments.single())
    }

    @Test fun `scarta e conta i punti senza coordinate o fuori intervallo`() {
        val file = ok("""<gpx version="1.1"><trk><trkseg>
            <trkpt lat="45" lon="7"/><trkpt lon="7"/><trkpt lat="95" lon="7"/><trkpt lat="45" lon="200"/><trkpt lat="x" lon="7"/><trkpt lat="45.1" lon="7"/>
        </trkseg></trk></gpx>""")
        assertEquals(2, file.tracks.single().segments.single().size)
        assertEquals(4, file.discardedPoints)
    }

    @Test fun `un file senza punti validi è una traccia vuota`() {
        assertEquals(GpxError.VUOTO, error("""<gpx version="1.1"><trk><trkseg><trkpt lat="95" lon="7"/></trkseg></trk></gpx>"""))
        assertEquals(GpxError.VUOTO, error("""<gpx version="1.1"><wpt lat="45" lon="7"/></gpx>"""))
    }

    @Test fun `un file che non è GPX è rifiutato`() {
        assertEquals(GpxError.NON_GPX, error("""<kml><Document/></kml>"""))
        assertEquals(GpxError.NON_GPX, error("solo testo, niente XML"))
        assertEquals(GpxError.NON_GPX, error(""))
    }

    @Test fun `un XML troncato è un errore e non restituisce punti`() {
        assertEquals(GpxError.XML_NON_VALIDO, error("""<gpx version="1.1"><trk><trkseg><trkpt lat="45" lon="7"/>"""))
    }

    @Test fun `i punti duplicati consecutivi restano nei dati`() {
        val file = ok("""<gpx version="1.1"><trk><trkseg><trkpt lat="45" lon="7"/><trkpt lat="45" lon="7"/><trkpt lat="45.1" lon="7"/></trkseg></trk></gpx>""")
        assertEquals(3, file.tracks.single().segments.single().size)
    }

    @Test fun `un segmento vuoto non diventa un segmento`() {
        val file = ok("""<gpx version="1.1"><trk><trkseg/><trkseg><trkpt lat="45" lon="7"/></trkseg></trk><trk><name>Vuoto</name></trk></gpx>""")
        assertEquals(1, file.tracks.size)
        assertEquals(1, file.tracks.single().segments.size)
    }

    @Test fun `ciò che viene scritto si rilegge uguale`() {
        val segments = listOf(listOf(GeoPoint(46.5, 12.0, 2000.0), GeoPoint(46.500001, 0.00001234)), listOf(GeoPoint(-33.9, -70.5, -3.25)))
        val waypoints = listOf(GpxWaypoint(GeoPoint(46.52, 12.01, 2100.0), "Rifugio <A&B>"))
        val file = ok(GpxWriter.write("Giro \"lungo\" & <bello>", segments, waypoints, "Nota"))
        assertEquals("1.1", file.version)
        assertEquals("Giro \"lungo\" & <bello>", file.tracks.single().name)
        assertEquals(segments, file.tracks.single().segments)
        assertEquals(waypoints, file.waypoints)
    }

    @Test fun `il nome del file non diventa nome del tracciato`() {
        assertNull(ok("""<gpx version="1.1"><metadata><name>File</name></metadata><trk><trkseg><trkpt lat="45" lon="7"/></trkseg></trk></gpx>""").tracks.single().name)
    }
}
