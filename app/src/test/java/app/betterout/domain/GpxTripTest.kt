package app.betterout.domain

import app.betterout.data.Trip
import app.betterout.data.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class GpxTripTest {
    private val rome = ZoneId.of("Europe/Rome")
    private val now = Instant.parse("2024-07-01T00:00:00Z").toEpochMilli()
    private val points = listOf(listOf(GeoPoint(46.5, 12.0, 2000.0), GeoPoint(46.6, 12.1)))
    private val recorded = GpxTrack("Giro", points, Instant.parse("2024-06-01T05:12:33Z"), Instant.parse("2024-06-01T11:00:00Z"))

    @Test fun `un tracciato registrato diventa un'uscita completata con data, ora, posizione e percorso`() {
        val trip = GpxTrip.create("u1", "Giro", recorded, "p1", rome, now)
        assertEquals("p1", trip.routeId)
        assertEquals(TripStatus.COMPLETATA, trip.status)
        assertEquals(LocalDate.of(2024, 6, 1), trip.startDate)
        assertNull(trip.endDate)
        assertEquals(LocalTime.of(7, 12), trip.startTime)
        assertEquals("Europe/Rome", trip.timeZoneId)
        assertEquals(46.5, trip.latitude)
        assertEquals(12.0, trip.longitude)
    }

    @Test fun `su più giorni c'è anche la data di fine`() {
        val trip = GpxTrip.create("u1", "Giro", recorded.copy(end = Instant.parse("2024-06-03T10:00:00Z")), "p1", rome, now)
        assertEquals(LocalDate.of(2024, 6, 3), trip.endDate)
    }

    @Test fun `senza orari l'uscita resta in bozza e senza date`() {
        val trip = GpxTrip.create("u1", "Giro", GpxTrack("Giro", points), "p1", rome, now)
        assertEquals(TripStatus.BOZZA, trip.status)
        assertNull(trip.startDate)
        assertNull(trip.startTime)
        assertEquals(46.5, trip.latitude)
    }

    @Test fun `in un'uscita esistente non sovrascrive ciò che è già scritto`() {
        val existing = Trip(
            id = "u1", title = "Mia", timeZoneId = "Europe/Rome", startDate = LocalDate.of(2024, 5, 30), startTime = LocalTime.of(6, 0),
            latitude = 1.0, longitude = 2.0, status = TripStatus.PROGRAMMATA, createdAt = 0, updatedAt = 0,
        )
        val trip = GpxTrip.fill(existing, recorded, "p1")
        assertEquals(existing.copy(routeId = "p1"), trip)
    }

    @Test fun `in un'uscita esistente riempie i campi vuoti`() {
        val existing = Trip(id = "u1", title = "Mia", timeZoneId = "Europe/Rome", createdAt = 0, updatedAt = 0)
        val trip = GpxTrip.fill(existing, recorded, "p1")
        assertEquals(LocalDate.of(2024, 6, 1), trip.startDate)
        assertEquals(LocalTime.of(7, 12), trip.startTime)
        assertEquals(46.5, trip.latitude)
        assertEquals("Mia", trip.title)
        assertEquals(TripStatus.BOZZA, trip.status)
    }

    @Test fun `l'ora non viene presa se la data scritta è un'altra`() {
        val existing = Trip(id = "u1", title = "Mia", timeZoneId = "Europe/Rome", startDate = LocalDate.of(2024, 5, 30), createdAt = 0, updatedAt = 0)
        assertNull(GpxTrip.fill(existing, recorded, "p1").startTime)
    }
}
