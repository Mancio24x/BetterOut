package app.betterout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ElevationStatsTest {
    @Test fun `senza quote il dato manca`() {
        assertNull(ElevationStats.totals(listOf(null, null)))
        assertNull(ElevationStats.totals(emptyList()))
    }

    @Test fun `il rumore sotto soglia non conta`() {
        val t = ElevationStats.totals(listOf(100.0, 101.0, 99.5, 101.5, 100.0, 102.0, 100.5))!!
        assertEquals(0.0, t.ascent, 0.001)
        assertEquals(0.0, t.descent, 0.001)
    }

    @Test fun `una salita graduale a passi di un metro e contata per intero`() {
        val climb = (0..30).map { 1000.0 + it }
        val t = ElevationStats.totals(climb)!!
        assertEquals(30.0, t.ascent, 0.001)
        assertEquals(0.0, t.descent, 0.001)
        assertEquals(1000.0, t.min, 0.001)
        assertEquals(1030.0, t.max, 0.001)
    }

    @Test fun `salita e discesa`() {
        val t = ElevationStats.totals(listOf(100.0, 150.0, 120.0, 200.0, 90.0))!!
        assertEquals(130.0, t.ascent, 0.001)
        assertEquals(140.0, t.descent, 0.001)
    }

    @Test fun `i punti senza quota vengono saltati`() {
        val t = ElevationStats.totals(listOf(100.0, null, 110.0, null))!!
        assertEquals(10.0, t.ascent, 0.001)
    }

    @Test fun `il salto fra segmenti separati non entra nel dislivello`() {
        val t = ElevationStats.totalsBySegment(listOf(listOf(100.0, 150.0), listOf(900.0, 950.0)))!!
        assertEquals(100.0, t.ascent, 0.001)
        assertEquals(0.0, t.descent, 0.001)
        assertEquals(950.0, t.max, 0.001)
    }

    @Test fun `con soglia piu alta contano solo le variazioni grandi`() {
        val t = ElevationStats.totals(listOf(100.0, 105.0, 100.0, 120.0), threshold = 10.0)!!
        assertEquals(20.0, t.ascent, 0.001)
        assertEquals(0.0, t.descent, 0.001)
    }
}
