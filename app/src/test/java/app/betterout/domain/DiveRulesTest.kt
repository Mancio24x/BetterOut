package app.betterout.domain

import app.betterout.data.AppDatabase
import app.betterout.data.Dive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

class DiveRulesTest {
    private fun dive(number: Int, date: String = "2026-08-01", time: String? = null, minutes: Int? = null, depth: Double? = null) =
        Dive(id = "d$number", number = number, date = LocalDate.parse(date), startTime = time?.let(LocalTime::parse), durationMinutes = minutes, maxDepthMeters = depth, createdAt = 0, updatedAt = 0)

    @Test fun `il numero successivo segue il più alto, la prima immersione è la 1`() {
        assertEquals(1, DiveRules.nextNumber(emptyList()))
        assertEquals(8, DiveRules.nextNumber(listOf(dive(3), dive(7), dive(5))))
    }

    @Test fun `il logbook parte dalla più recente`() {
        val list = listOf(dive(1, "2026-07-01"), dive(3, "2026-08-01", "15:00"), dive(2, "2026-08-01", "09:30"), dive(4, "2026-08-01"))
        assertEquals(listOf(3, 2, 4, 1), DiveRules.sorted(list).map { it.number })
    }

    @Test fun `i totali sommano i minuti noti e tengono la profondità record`() {
        assertEquals(DiveTotals(3, 95, 32.5), DiveRules.totals(listOf(dive(1, minutes = 45, depth = 18.0), dive(2, minutes = 50, depth = 32.5), dive(3))))
        assertEquals(DiveTotals(0, 0, null), DiveRules.totals(emptyList()))
    }

    @Test fun `il consumo in superficie si calcola solo con tutti i dati`() {
        // 12 litri, da 200 a 60 bar, 45 minuti a 15 m di media: 140 × 12 / 45 / 2,5 ≈ 14,9 l/min.
        val full = dive(1, minutes = 45).copy(tankLiters = 12.0, startBar = 200, endBar = 60, averageDepthMeters = 15.0)
        assertEquals(14.93, DiveRules.sacLitersPerMinute(full)!!, 0.01)
        assertEquals(140, DiveRules.usedBar(full))
        assertNull(DiveRules.sacLitersPerMinute(full.copy(tankLiters = null)))
        assertNull(DiveRules.sacLitersPerMinute(full.copy(averageDepthMeters = null)))
        assertNull(DiveRules.usedBar(full.copy(endBar = 210)))
    }

    private fun imported(date: String, time: String? = null, number: Int? = null, depth: Double? = null) =
        ImportedDive(LocalDate.parse(date), time?.let(LocalTime::parse), number, maxDepthMeters = depth)

    @Test fun `un'immersione già nel logbook si riconosce da data e ora, o senza ora dalla profondità`() {
        val log = listOf(dive(1, "2026-08-01", "09:30"), dive(2, "2026-08-02", depth = 18.0))
        assertTrue(DiveRules.isDuplicate(log, imported("2026-08-01", "09:30:40")))
        assertFalse(DiveRules.isDuplicate(log, imported("2026-08-01", "15:00")))
        assertTrue(DiveRules.isDuplicate(log, imported("2026-08-02", depth = 18.0)))
        assertFalse(DiveRules.isDuplicate(log, imported("2026-08-02", depth = 22.0)))
    }

    @Test fun `i numeri del file si tengono se liberi, gli altri seguono dalla più vecchia`() {
        val log = listOf(dive(1), dive(2), dive(5))
        val list = listOf(imported("2026-09-03"), imported("2026-09-01", number = 2), imported("2026-09-02", number = 9))
        assertEquals(listOf(11, 10, 9), DiveRules.importNumbers(log, list))
    }

    @Test fun `la migrazione crea la tabella esattamente come la genera Room`() {
        val generated = File("build/generated/ksp/debug/kotlin/app/betterout/data/AppDatabase_Impl.kt")
        assertTrue("manca il codice generato da Room", generated.exists())
        assertTrue("CREATE_DIVES non coincide con lo schema di Dive", AppDatabase.CREATE_DIVES in generated.readText())
    }
}
