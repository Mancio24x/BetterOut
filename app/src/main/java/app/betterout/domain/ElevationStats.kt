package app.betterout.domain

data class ElevationTotals(val ascent: Double, val descent: Double, val min: Double, val max: Double)

object ElevationStats {
    const val DEFAULT_THRESHOLD_METERS = 3.0

    /**
     * Dislivello con isteresi. Si tiene una quota di riferimento; quando la quota corrente se ne discosta
     * di almeno [threshold], l'intera differenza va in salita o in discesa e il riferimento si sposta.
     * Così il rumore sotto soglia non conta, ma le salite graduali vengono sommate per intero.
     * Restituisce null se non c'è nessuna quota: il dato manca e non va inventato.
     */
    fun totals(elevations: List<Double?>, threshold: Double = DEFAULT_THRESHOLD_METERS): ElevationTotals? {
        val values = elevations.filterNotNull().filter { it.isFinite() }
        if (values.isEmpty()) return null
        var reference = values.first()
        var ascent = 0.0
        var descent = 0.0
        for (e in values) {
            val delta = e - reference
            if (delta >= threshold) { ascent += delta; reference = e }
            else if (-delta >= threshold) { descent += -delta; reference = e }
        }
        return ElevationTotals(ascent, descent, values.min(), values.max())
    }

    /** Somma i totali di segmenti separati senza contare il salto di quota fra un segmento e l'altro. */
    fun totalsBySegment(segments: List<List<Double?>>, threshold: Double = DEFAULT_THRESHOLD_METERS): ElevationTotals? {
        val parts = segments.mapNotNull { totals(it, threshold) }
        if (parts.isEmpty()) return null
        return ElevationTotals(parts.sumOf { it.ascent }, parts.sumOf { it.descent }, parts.minOf { it.min }, parts.maxOf { it.max })
    }
}
