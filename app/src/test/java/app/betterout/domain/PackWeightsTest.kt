package app.betterout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackWeightsTest {
    private val lines = listOf(
        WeightLine(980, 1, GearKind.BASE),          // zaino
        WeightLine(160, 1, GearKind.BASE),          // borraccia vuota
        WeightLine(70, 2, GearKind.BASE),           // calze
        WeightLine(50, 3, GearKind.CONSUMABILE),    // barrette
        WeightLine(410, 1, GearKind.INDOSSATO),     // pile
    )

    @Test fun `ogni riga e contata una sola volta e l'acqua e separata dal contenitore`() {
        val t = PackWeights.totals(lines, waterLiters = 1.0)
        assertEquals(1280, t.baseGrams)
        assertEquals(150, t.consumableGrams)
        assertEquals(1000, t.waterGrams)
        assertEquals(1150, t.consumableWithWaterGrams)
        assertEquals(410, t.wornGrams)
        assertEquals(2430, t.packGrams)
        assertEquals(2840, t.totalGrams)
        assertEquals(t.baseGrams + t.consumableGrams + t.waterGrams + t.wornGrams, t.totalGrams)
        assertFalse(t.isPartial)
    }

    @Test fun `le righe senza peso valgono zero e rendono il totale parziale`() {
        val t = PackWeights.totals(lines + WeightLine(null, 2, GearKind.BASE))
        assertEquals(1280, t.baseGrams)
        assertEquals(1, t.linesWithoutWeight)
        assertTrue(t.isPartial)
    }

    @Test fun `zaino vuoto`() {
        val t = PackWeights.totals(emptyList())
        assertEquals(0, t.totalGrams)
    }
}
