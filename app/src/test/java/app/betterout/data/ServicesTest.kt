package app.betterout.data

import app.betterout.domain.GeoPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ServicesTest {
    @Test fun `la risposta di BRouter diventa una linea con le quote`() {
        val body = """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{"track-length":"923"},"geometry":{"type":"LineString","coordinates":[[12.2528,46.5811,1752.5],[12.2531,46.5815,1760],[12.2540,46.5820]]}}]}"""
        assertEquals(listOf(GeoPoint(46.5811, 12.2528, 1752.5), GeoPoint(46.5815, 12.2531, 1760.0), GeoPoint(46.5820, 12.2540)), BRouterService.parse(body))
    }

    @Test fun `una risposta di BRouter che non è un tracciato è un errore, non una linea`() {
        assertThrows(IOException::class.java) { BRouterService.parse("from-position not mapped in existing datafile") }
        assertThrows(IOException::class.java) { BRouterService.parse("""{"features":[]}""") }
        assertThrows(IOException::class.java) { BRouterService.parse("""{"features":[{"geometry":{"coordinates":[[12.0,46.0,1.0]]}}]}""") }
    }

    @Test fun `la richiesta a BRouter mette longitudine prima di latitudine`() = runTest {
        var asked = ""
        BRouterService { url -> asked = url; """{"features":[{"geometry":{"coordinates":[[12.0,46.0],[12.1,46.1]]}}]}""" }.route(GeoPoint(46.0, 12.0), GeoPoint(46.1, 12.1))
        assertTrue(asked, asked.contains("lonlats=12.000000,46.000000|12.100000,46.100000"))
        assertTrue(asked, asked.contains("profile=hiking-mountain"))
    }

    @Test fun `la ricerca legge i risultati e sopporta campi mancanti`() {
        val body = """{"results":[{"id":1,"name":"Misurina","latitude":46.5811,"longitude":12.2528,"elevation":1752.0,"admin1":"Veneto","country":"Italia","timezone":"Europe/Rome"},{"name":"Altrove","latitude":1.0,"longitude":2.0}],"generationtime_ms":0.5}"""
        val places = PlaceSearch.parse(body)
        assertEquals(Place("Misurina", 46.5811, 12.2528, 1752.0, "Veneto", "Italia", "Europe/Rome"), places[0])
        assertEquals(Place("Altrove", 1.0, 2.0), places[1])
    }

    @Test fun `una ricerca senza risultati è un elenco vuoto`() {
        assertEquals(emptyList<Place>(), PlaceSearch.parse("""{"generationtime_ms":0.3}"""))
    }

    @Test fun `le quote del terreno sono chieste a blocchi di cento punti`() = runTest {
        val points = (0 until 250).map { GeoPoint(46.0 + it * 0.001, 12.0) }
        val sizes = mutableListOf<Int>()
        val elevations = TerrainElevation.elevations(points) { url ->
            val n = url.substringAfter("latitude=").substringBefore("&").split(",").size
            sizes += n
            """{"elevation":[${(0 until n).joinToString(",") { "1000.0" }}]}"""
        }
        assertEquals(listOf(100, 100, 50), sizes)
        assertEquals(250, elevations.size)
    }

    @Test fun `quote in numero diverso dai punti chiesti sono un errore`() = runTest {
        var failed = false
        try { TerrainElevation.elevations(listOf(GeoPoint(46.0, 12.0), GeoPoint(46.1, 12.0))) { """{"elevation":[1000.0]}""" } } catch (e: IOException) { failed = true }
        assertTrue(failed)
    }
}
