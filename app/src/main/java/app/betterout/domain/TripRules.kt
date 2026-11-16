package app.betterout.domain

import app.betterout.data.Trip
import app.betterout.data.TripStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object TripRules {
    /**
     * Prossima uscita: quella programmata con la data di oggi o futura più vicina;
     * se non ce n'è, la bozza modificata più di recente.
     */
    fun next(trips: List<Trip>, today: LocalDate): Trip? =
        trips.filter { it.status == TripStatus.PROGRAMMATA && it.startDate != null && !it.startDate.isBefore(today) }
            .minWithOrNull(compareBy<Trip> { it.startDate }.thenBy { it.startTime }.thenBy { it.title })
            ?: trips.filter { it.status == TripStatus.BOZZA }.maxByOrNull { it.updatedAt }

    /** Ordine dell'elenco: prima le uscite con data (dalla più vicina in avanti, poi le passate), poi quelle senza data. */
    fun sorted(trips: List<Trip>, today: LocalDate): List<Trip> {
        val (dated, undated) = trips.partition { it.startDate != null }
        val (future, past) = dated.partition { !it.startDate!!.isBefore(today) }
        return future.sortedBy { it.startDate } + undated.sortedByDescending { it.updatedAt } + past.sortedByDescending { it.startDate }
    }

    /** Giorni da oggi alla partenza: 0 oggi, negativo se passata, null senza data. */
    /** Le sezioni dell'elenco delle uscite, nell'ordine in cui si mostrano. */
    enum class Section { IN_PROGRAMMA, BOZZE, FATTE, ANNULLATE }

    /** Le uscite divise per sezione, ognuna nell'ordine di [sorted]; le sezioni vuote non compaiono. */
    fun sections(trips: List<Trip>, today: LocalDate): List<Pair<Section, List<Trip>>> {
        val ordered = sorted(trips, today)
        return Section.entries.map { section ->
            section to ordered.filter {
                when (section) {
                    Section.IN_PROGRAMMA -> it.status == TripStatus.PROGRAMMATA
                    Section.BOZZE -> it.status == TripStatus.BOZZA
                    Section.FATTE -> it.status == TripStatus.COMPLETATA
                    Section.ANNULLATE -> it.status == TripStatus.ANNULLATA
                }
            }
        }.filter { it.second.isNotEmpty() }
    }

    fun daysUntil(trip: Trip, today: LocalDate): Long? = trip.startDate?.let { ChronoUnit.DAYS.between(today, it) }

    /** Numero di giorni dell'uscita (1 se manca la data di fine). */
    fun lengthInDays(trip: Trip): Int {
        val start = trip.startDate ?: return 1
        val end = trip.endDate ?: return 1
        return (ChronoUnit.DAYS.between(start, end).toInt() + 1).coerceAtLeast(1)
    }

    /** Programmata ma con data già passata: da segnalare, senza cambiare lo stato da soli. */
    fun isOverdue(trip: Trip, today: LocalDate): Boolean =
        trip.status == TripStatus.PROGRAMMATA && (trip.endDate ?: trip.startDate)?.isBefore(today) == true

    /**
     * Copia indipendente: nuovo identificativo, stato bozza, nuova data (o nessuna) mantenendo la durata in giorni.
     * Il percorso resta collegato per riferimento; tutto il resto è copiato per valore.
     */
    fun duplicate(original: Trip, newId: String, newTitle: String, newDate: LocalDate?, now: Long): Trip {
        val days = lengthInDays(original)
        return original.copy(
            id = newId,
            title = newTitle,
            status = TripStatus.BOZZA,
            startDate = newDate,
            endDate = newDate?.plusDays((days - 1).toLong())?.takeIf { days > 1 },
            startTime = if (newDate == null) null else original.startTime,
            createdAt = now,
            updatedAt = now,
        )
    }

    /** Una data di fine precedente all'inizio non è valida. */
    fun datesAreValid(start: LocalDate?, end: LocalDate?): Boolean = start == null && end == null || start != null && (end == null || !end.isBefore(start))
}
