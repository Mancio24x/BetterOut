package app.betterout.domain

import app.betterout.data.Dive
import java.time.temporal.ChronoUnit

/** Totali del logbook: quante immersioni, quanto tempo sott'acqua, la profondità più grande. */
data class DiveTotals(val count: Int, val minutes: Int, val deepestMeters: Double?)

object DiveRules {
    /** Il numero dopo il più alto già usato; la prima immersione è la 1. */
    fun nextNumber(dives: List<Dive>): Int = (dives.maxOfOrNull { it.number } ?: 0) + 1

    /** Dalla più recente: data, ora (senza ora in fondo alla giornata), poi numero. */
    fun sorted(dives: List<Dive>): List<Dive> =
        dives.sortedWith(compareByDescending<Dive> { it.date }.thenByDescending { it.startTime }.thenByDescending { it.number })

    fun totals(dives: List<Dive>) = DiveTotals(dives.size, dives.sumOf { it.durationMinutes ?: 0 }, dives.mapNotNull { it.maxDepthMeters }.maxOrNull())

    /** Bar consumati, se si conoscono inizio e fine e la fine non supera l'inizio. */
    fun usedBar(dive: Dive): Int? {
        val start = dive.startBar ?: return null
        val end = dive.endBar ?: return null
        return (start - end).takeIf { it >= 0 }
    }

    /**
     * Consumo in superficie (SAC), in litri al minuto: gas usato riportato alla pressione di superficie,
     * diviso per la durata e per la pressione assoluta alla profondità media (1 bar ogni 10 m, più 1).
     * Senza bombola, bar, durata o profondità media non si calcola.
     */
    fun sacLitersPerMinute(dive: Dive): Double? {
        val bar = usedBar(dive) ?: return null
        val liters = dive.tankLiters?.takeIf { it > 0 } ?: return null
        val minutes = dive.durationMinutes?.takeIf { it > 0 } ?: return null
        val depth = dive.averageDepthMeters?.takeIf { it >= 0 } ?: return null
        return bar * liters / minutes / (depth / 10.0 + 1.0)
    }

    /**
     * Un'immersione letta da file è già nel logbook se ha la stessa data e la stessa ora d'ingresso (al minuto);
     * senza ora, se ha la stessa data e la stessa profondità massima.
     */
    fun isDuplicate(existing: List<Dive>, imported: ImportedDive): Boolean = existing.any { d ->
        d.date == imported.date && when {
            d.startTime != null && imported.startTime != null -> d.startTime.truncatedTo(ChronoUnit.MINUTES) == imported.startTime.truncatedTo(ChronoUnit.MINUTES)
            else -> d.maxDepthMeters != null && d.maxDepthMeters == imported.maxDepthMeters
        }
    }

    /**
     * I numeri da dare alle immersioni importate, nell'ordine dato: quello del file se c'è e non è già usato,
     * altrimenti il successivo al più alto. Le immersioni sono numerate dalla più vecchia.
     */
    fun importNumbers(existing: List<Dive>, imported: List<ImportedDive>): List<Int> {
        val used = existing.map { it.number }.toMutableSet()
        val numbers = arrayOfNulls<Int>(imported.size)
        val order = imported.indices.sortedWith(compareBy<Int> { imported[it].date }.thenBy { imported[it].startTime })
        for (i in order) imported[i].number?.takeIf { it > 0 && it !in used }?.let { numbers[i] = it; used += it }
        for (i in order) if (numbers[i] == null) ((used.maxOrNull() ?: 0) + 1).let { numbers[i] = it; used += it }
        return numbers.map { it!! }
    }
}
