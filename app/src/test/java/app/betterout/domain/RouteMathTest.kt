package app.betterout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMathTest {
    @Test fun `un grado di latitudine misura circa 111,2 km`() {
        assertEquals(111_195.0, Geo.distance(GeoPoint(45.0, 7.0), GeoPoint(46.0, 7.0)), 10.0)
    }

    @Test fun `la distanza non attraversa il vuoto fra due segmenti`() {
        val a = listOf(GeoPoint(45.0, 7.0), GeoPoint(45.01, 7.0))
        val b = listOf(GeoPoint(46.0, 7.0), GeoPoint(46.01, 7.0))
        val stats = RouteMath.stats(listOf(a, b))
        assertEquals(Geo.length(a) + Geo.length(b), stats.distanceMeters, 0.001)
        assertTrue(stats.distanceMeters < 3000)
    }

    @Test fun `il salto di quota fra due segmenti non conta nel dislivello`() {
        val a = listOf(GeoPoint(45.0, 7.0, 1000.0), GeoPoint(45.01, 7.0, 1100.0))
        val b = listOf(GeoPoint(46.0, 7.0, 2000.0), GeoPoint(46.01, 7.0, 1950.0))
        val totals = RouteMath.stats(listOf(a, b)).elevation!!
        assertEquals(100.0, totals.ascent, 0.0)
        assertEquals(50.0, totals.descent, 0.0)
    }

    @Test fun `senza quote il dislivello manca`() {
        val stats = RouteMath.stats(listOf(listOf(GeoPoint(45.0, 7.0), GeoPoint(45.01, 7.0))))
        assertNull(stats.elevation)
        assertFalse(stats.partialElevation)
    }

    @Test fun `quote su una parte dei punti sono dichiarate parziali`() {
        assertTrue(RouteMath.stats(listOf(listOf(GeoPoint(45.0, 7.0, 100.0), GeoPoint(45.01, 7.0)))).partialElevation)
    }

    @Test fun `i punti duplicati non cambiano la distanza`() {
        val p = GeoPoint(45.0, 7.0)
        val q = GeoPoint(45.01, 7.0)
        assertEquals(Geo.length(listOf(p, q)), Geo.length(listOf(p, p, q, q)), 0.0)
    }

    @Test fun `la semplificazione tiene estremi e curve e toglie i punti allineati`() {
        val line = listOf(GeoPoint(45.0, 7.0), GeoPoint(45.001, 7.0), GeoPoint(45.002, 7.0), GeoPoint(45.002, 7.01), GeoPoint(45.002, 7.02))
        assertEquals(listOf(line[0], line[2], line[4]), Geo.simplify(line, 5.0))
    }

    @Test fun `il profilo segue la distanza percorsa e salta i punti senza quota`() {
        val profile = RouteMath.profile(listOf(listOf(GeoPoint(45.0, 7.0, 100.0), GeoPoint(45.01, 7.0), GeoPoint(45.02, 7.0, 300.0))))
        assertEquals(listOf(100.0, 300.0), profile.map { it.elevation })
        assertEquals(0.0, profile[0].distanceMeters, 0.0)
        assertEquals(2224.0, profile[1].distanceMeters, 2.0)
    }

    @Test fun `il profilo ridotto conserva la quota massima e la minima`() {
        val points = (0 until 5000).map { GeoPoint(45.0 + it * 0.0001, 7.0, if (it == 2500) 3000.0 else if (it == 4000) 50.0 else 1000.0) }
        val profile = RouteMath.profile(listOf(points), maxSamples = 100)
        assertTrue(profile.size <= 200)
        assertEquals(3000.0, profile.maxOf { it.elevation }, 0.0)
        assertEquals(50.0, profile.minOf { it.elevation }, 0.0)
        assertEquals(profile.sortedBy { it.distanceMeters }, profile)
    }
}
