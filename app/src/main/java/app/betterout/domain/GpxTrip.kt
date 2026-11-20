package app.betterout.domain

import app.betterout.data.Trip
import app.betterout.data.TripStatus
import java.time.Instant
import java.time.ZoneId

/** L'uscita che corrisponde a un tracciato importato: date e ora dagli orari del file, posizione dal punto di partenza. */
object GpxTrip {
    /** Nuova uscita col percorso [routeId]; completata se il tracciato è registrato e già finito, altrimenti bozza. */
    fun create(id: String, title: String, track: GpxTrack, routeId: String, zone: ZoneId, now: Long): Trip {
        val empty = Trip(id = id, title = title, timeZoneId = zone.id, createdAt = now, updatedAt = now)
        val end = track.end
        val status = if (end != null && end.isBefore(Instant.ofEpochMilli(now))) TripStatus.COMPLETATA else TripStatus.BOZZA
        return fill(empty, track, routeId).copy(status = status)
    }

    /** Collega il percorso e riempie solo date, ora e posizione ancora vuote: ciò che è già scritto resta. */
    fun fill(trip: Trip, track: GpxTrack, routeId: String): Trip {
        val zone = runCatching { ZoneId.of(trip.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        val start = track.start?.atZone(zone)
        val endDate = track.end?.atZone(zone)?.toLocalDate()
        val first = track.segments.firstOrNull()?.firstOrNull()
        val hasPlace = trip.latitude != null && trip.longitude != null
        return trip.copy(
            routeId = routeId,
            startDate = trip.startDate ?: start?.toLocalDate(),
            endDate = if (trip.startDate == null && start != null) endDate?.takeIf { it.isAfter(start.toLocalDate()) } else trip.endDate,
            startTime = trip.startTime ?: if (trip.startDate == null || trip.startDate == start?.toLocalDate()) start?.toLocalTime()?.withSecond(0)?.withNano(0) else null,
            latitude = if (hasPlace) trip.latitude else first?.lat,
            longitude = if (hasPlace) trip.longitude else first?.lon,
        )
    }
}
