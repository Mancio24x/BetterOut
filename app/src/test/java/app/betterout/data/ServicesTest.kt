package app.betterout.data

import app.betterout.domain.GeoPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
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

    @Test fun `Nominatim - nome, tipo, comune e regione`() {
        val body = """[{"name":"Passo di Falzarego","category":"mountain_pass","type":"yes","lat":"46.5187504","lon":"12.0084323","display_name":"Passo di Falzarego, Cortina d'Ampezzo, Belluno, Veneto, Italia","address":{"town":"Cortina d'Ampezzo","state":"Veneto","country":"Italia"}},{"name":"","display_name":"Falzarego, SR48","category":"historic","type":"memorial","lat":"46.519","lon":"12.009"}]"""
        val places = PlaceSearch.parseNominatim(body)
        assertEquals(Place("Passo di Falzarego", 46.5187504, 12.0084323, admin1 = "Veneto", country = "Italia", kind = PlaceKind.PASSO, area = "Cortina d'Ampezzo"), places[0])
        assertEquals("Falzarego", places[1].name)
        assertEquals(null, places[1].kind)
    }

    @Test fun `Photon - longitudine prima di latitudine, luoghi senza nome scartati`() {
        val body = """{"features":[{"geometry":{"coordinates":[12.008133,46.527754],"type":"Point"},"properties":{"name":"Rifugio Lagazuoi","osm_key":"tourism","osm_value":"alpine_hut","state":"Veneto","country":"Italia"}},{"geometry":{"coordinates":[12.0,46.0]},"properties":{"osm_key":"highway","osm_value":"path"}}]}"""
        assertEquals(listOf(Place("Rifugio Lagazuoi", 46.527754, 12.008133, admin1 = "Veneto", country = "Italia", kind = PlaceKind.RIFUGIO)), PlaceSearch.parsePhoton(body))
    }

    @Test fun `lo stesso luogo trovato da più fonti compare una volta, con i dati di tutte`() {
        val merged = PlaceSearch.merge(listOf(
            Place("Falzarego", 46.5190, 12.0090),
            Place("Rifugio Lagazuoi", 46.527754, 12.008133, kind = PlaceKind.RIFUGIO),
            Place("rifugio lagazuoi", 46.5278, 12.0082, elevation = 2752.0, timezone = "Europe/Rome"),
            Place("Misurina", 46.5811, 12.2528, 1752.0),
        ))
        assertEquals(listOf("Rifugio Lagazuoi", "Falzarego", "Misurina"), merged.map { it.name })
        assertEquals(2752.0, merged[0].elevation!!, 0.0)
        assertEquals("Europe/Rome", merged[0].timezone)
    }

    @Test fun `la ricerca usa tre fonti e ne sopporta la mancanza, ma non di tutte`() = runTest {
        val asked = mutableListOf<String>()
        val places = PlaceSearch.search("Rifugio Lagazuoi") { url ->
            asked += url.substringAfter("://").substringBefore("/")
            when {
                "nominatim" in url -> throw IOException("non risponde")
                "photon" in url -> """{"features":[{"geometry":{"coordinates":[12.008133,46.527754]},"properties":{"name":"Rifugio Lagazuoi","osm_key":"tourism","osm_value":"alpine_hut"}}]}"""
                else -> """{"generationtime_ms":0.1}"""
            }
        }
        assertEquals(setOf("nominatim.openstreetmap.org", "photon.komoot.io", "geocoding-api.open-meteo.com"), asked.toSet())
        assertEquals(listOf("Rifugio Lagazuoi"), places.map { it.name })
        var failed = false
        try { PlaceSearch.search("x") { throw IOException("senza rete") } } catch (e: IOException) { failed = true }
        assertTrue(failed)
    }

    @Test fun `i risultati della fonte veloce arrivano senza aspettare la lenta`() = runTest {
        val steps = PlaceSearch.progressive("Lagazuoi") { url ->
            when {
                "nominatim" in url -> { delay(5_000); """[{"name":"Lagazuoi","category":"natural","type":"peak","lat":"46.53","lon":"12.01"}]""" }
                "photon" in url -> """{"features":[{"geometry":{"coordinates":[12.008133,46.527754]},"properties":{"name":"Rifugio Lagazuoi","osm_key":"tourism","osm_value":"alpine_hut"}}]}"""
                else -> throw IOException("non risponde")
            }
        }.toList()
        assertEquals(listOf(listOf("Rifugio Lagazuoi") to false), steps.take(1).map { s -> s.places.map { it.name } to s.done })
        // Alla fine l'ordine è quello delle fonti, come nella ricerca intera.
        assertEquals(listOf("Lagazuoi", "Rifugio Lagazuoi") to true, steps.last().let { s -> s.places.map { it.name } to s.done })
    }

    @Test fun `i tipi di luogo utili a chi cammina sono riconosciuti`() {
        assertEquals(PlaceKind.CIMA, PlaceSearch.placeKind("natural", "peak"))
        assertEquals(PlaceKind.LAGO, PlaceSearch.placeKind("water", "lake"))
        assertEquals(PlaceKind.LOCALITA, PlaceSearch.placeKind("place", "village"))
        assertEquals(PlaceKind.IMPIANTO, PlaceSearch.placeKind("aerialway", "chair_lift"))
        assertEquals(null, PlaceSearch.placeKind("historic", "memorial"))
    }
}
