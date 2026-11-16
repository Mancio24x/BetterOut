package app.betterout.domain

import app.betterout.data.Trip
import app.betterout.data.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TripRulesTest {
    private val today = LocalDate.of(2026, 11, 15)
    private fun trip(id: String, status: TripStatus, date: LocalDate? = null, end: LocalDate? = null, updated: Long = 0) =
        Trip(id = id, title = id, timeZoneId = "Europe/Rome", status = status, startDate = date, endDate = end, startTime = date?.let { LocalTime.of(7, 30) }, createdAt = 0, updatedAt = updated)

    @Test fun `la prossima uscita e la programmata futura piu vicina`() {
        val trips = listOf(
            trip("dicembre", TripStatus.PROGRAMMATA, LocalDate.of(2026, 12, 5)),
            trip("novembre", TripStatus.PROGRAMMATA, LocalDate.of(2026, 11, 21)),
            trip("passata", TripStatus.PROGRAMMATA, LocalDate.of(2026, 11, 2)),
            trip("completata", TripStatus.COMPLETATA, LocalDate.of(2026, 11, 16)),
            trip("bozza", TripStatus.BOZZA, updated = 99),
        )
        assertEquals("novembre", TripRules.next(trips, today)?.id)
    }

    @Test fun `un'uscita di oggi e ancora la prossima`() {
        assertEquals("oggi", TripRules.next(listOf(trip("oggi", TripStatus.PROGRAMMATA, today)), today)?.id)
    }

    @Test fun `senza programmate vale la bozza piu recente e senza nulla non c'e prossima uscita`() {
        val trips = listOf(trip("vecchia", TripStatus.BOZZA, updated = 1), trip("recente", TripStatus.BOZZA, updated = 2), trip("annullata", TripStatus.ANNULLATA, LocalDate.of(2026, 12, 1)))
        assertEquals("recente", TripRules.next(trips, today)?.id)
        assertNull(TripRules.next(listOf(trip("fatta", TripStatus.COMPLETATA, LocalDate.of(2026, 11, 1))), today))
    }

    @Test fun `giorni alla partenza`() {
        assertEquals(6L, TripRules.daysUntil(trip("a", TripStatus.PROGRAMMATA, LocalDate.of(2026, 11, 21)), today))
        assertEquals(-13L, TripRules.daysUntil(trip("a", TripStatus.COMPLETATA, LocalDate.of(2026, 11, 2)), today))
        assertNull(TripRules.daysUntil(trip("a", TripStatus.BOZZA), today))
    }

    @Test fun `la copia e indipendente e in bozza`() {
        val original = trip("orig", TripStatus.PROGRAMMATA, LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 23)).copy(notes = "note", routeId = "r1")
        val before = original.copy()
        val copy = TripRules.duplicate(original, "nuova", "Copia", LocalDate.of(2027, 6, 1), now = 500)
        assertEquals(before, original)
        assertNotEquals(original.id, copy.id)
        assertEquals(TripStatus.BOZZA, copy.status)
        assertEquals(LocalDate.of(2027, 6, 1), copy.startDate)
        assertEquals(LocalDate.of(2027, 6, 3), copy.endDate)
        assertEquals("note", copy.notes)
        assertEquals("r1", copy.routeId)
        assertEquals(500L, copy.createdAt)
    }

    @Test fun `la copia senza data perde data e ora`() {
        val copy = TripRules.duplicate(trip("orig", TripStatus.PROGRAMMATA, LocalDate.of(2026, 11, 21)), "n", "Copia", null, 1)
        assertNull(copy.startDate); assertNull(copy.endDate); assertNull(copy.startTime)
    }

    @Test fun `programmata con data passata e da segnalare`() {
        assertTrue(TripRules.isOverdue(trip("a", TripStatus.PROGRAMMATA, LocalDate.of(2026, 11, 2)), today))
        assertFalse(TripRules.isOverdue(trip("a", TripStatus.PROGRAMMATA, today), today))
        assertFalse(TripRules.isOverdue(trip("a", TripStatus.COMPLETATA, LocalDate.of(2026, 11, 2)), today))
    }

    @Test fun `ordine dell'elenco e validita delle date`() {
        val trips = listOf(trip("passata", TripStatus.COMPLETATA, LocalDate.of(2026, 11, 2)), trip("senza", TripStatus.BOZZA), trip("futura", TripStatus.PROGRAMMATA, LocalDate.of(2026, 12, 5)), trip("vicina", TripStatus.PROGRAMMATA, LocalDate.of(2026, 11, 21)))
        assertEquals(listOf("vicina", "futura", "senza", "passata"), TripRules.sorted(trips, today).map { it.id })
        assertTrue(TripRules.datesAreValid(null, null))
        assertTrue(TripRules.datesAreValid(today, today))
        assertFalse(TripRules.datesAreValid(today, today.minusDays(1)))
        assertFalse(TripRules.datesAreValid(null, today))
    }

    @Test fun `le uscite si dividono per stato, senza sezioni vuote`() {
        val today = LocalDate.of(2026, 11, 16)
        val list = listOf(
            trip("a", TripStatus.BOZZA), trip("b", TripStatus.PROGRAMMATA, today.plusDays(3)),
            trip("c", TripStatus.COMPLETATA, today.minusDays(9)), trip("d", TripStatus.PROGRAMMATA, today.plusDays(1)),
        )
        val sections = TripRules.sections(list, today)
        assertEquals(listOf(TripRules.Section.IN_PROGRAMMA, TripRules.Section.BOZZE, TripRules.Section.FATTE), sections.map { it.first })
        assertEquals(listOf("d", "b"), sections[0].second.map { it.id })
    }
}
