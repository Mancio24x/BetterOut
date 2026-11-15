package app.betterout.domain

data class RouteStats(val distanceMeters: Double, val elevation: ElevationTotals?, val pointCount: Int, val pointsWithElevation: Int) {
    /** Vero se solo una parte dei punti ha la quota: dislivello e profilo sono incompleti. */
    val partialElevation: Boolean get() = pointsWithElevation in 1 until pointCount
}

/** Un campione del profilo altimetrico: distanza dall'inizio e quota, in metri. */
data class ProfilePoint(val distanceMeters: Double, val elevation: Double)

object RouteMath {
    /**
     * Statistiche di un percorso fatto di segmenti separati. La distanza è la somma delle distanze fra punti consecutivi
     * dentro ogni segmento: il vuoto fra un segmento e l'altro non conta, né in distanza né in dislivello.
     */
    fun stats(segments: List<List<GeoPoint>>, threshold: Double = ElevationStats.DEFAULT_THRESHOLD_METERS): RouteStats = RouteStats(
        distanceMeters = segments.sumOf(Geo::length),
        elevation = ElevationStats.totalsBySegment(segments.map { s -> s.map { it.ele } }, threshold),
        pointCount = segments.sumOf { it.size },
        pointsWithElevation = segments.sumOf { s -> s.count { it.ele != null } },
    )

    /** Profilo altimetrico ridotto ad al più [maxSamples] punti circa, per il disegno. I punti senza quota sono saltati. */
    fun profile(segments: List<List<GeoPoint>>, maxSamples: Int = 240): List<ProfilePoint> {
        val all = ArrayList<ProfilePoint>()
        var travelled = 0.0
        for (segment in segments) {
            segment.forEachIndexed { i, p ->
                if (i > 0) travelled += Geo.distance(segment[i - 1], p)
                if (p.ele != null) all += ProfilePoint(travelled, p.ele)
            }
        }
        if (all.size <= maxSamples) return all
        val step = all.size.toDouble() / maxSamples
        // Di ogni gruppo si tengono minimo e massimo, così cime e selle non spariscono dal disegno.
        return (0 until maxSamples).flatMap { n ->
            val group = all.subList((n * step).toInt(), minOf(all.size, ((n + 1) * step).toInt().coerceAtLeast((n * step).toInt() + 1)))
            listOf(group.minBy { it.elevation }, group.maxBy { it.elevation }).distinct().sortedBy { it.distanceMeters }
        }
    }
}
