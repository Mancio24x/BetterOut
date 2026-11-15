package app.betterout.domain

import kotlinx.serialization.Serializable
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Un punto in gradi decimali WGS84; [ele] è la quota in metri, null se non nota. */
@Serializable
data class GeoPoint(val lat: Double, val lon: Double, val ele: Double? = null)

object Geo {
    private const val EARTH_RADIUS_METERS = 6_371_008.8

    fun isValid(lat: Double, lon: Double): Boolean = lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0

    /** Distanza sulla sfera (haversine), in metri. */
    fun distance(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun length(points: List<GeoPoint>): Double = points.zipWithNext { a, b -> distance(a, b) }.sum()

    /**
     * Semplificazione di Douglas–Peucker, solo per il disegno: statistiche ed esportazione usano sempre i punti originali.
     * Tiene i punti che si discostano più di [toleranceMeters] dalla linea semplificata.
     */
    fun simplify(points: List<GeoPoint>, toleranceMeters: Double): List<GeoPoint> {
        if (points.size < 3) return points
        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.lastIndex] = true
        val stack = ArrayDeque(listOf(0 to points.lastIndex))
        while (stack.isNotEmpty()) {
            val (first, last) = stack.removeLast()
            var worst = -1
            var worstDistance = toleranceMeters
            for (i in first + 1 until last) {
                val d = distanceToSegment(points[i], points[first], points[last])
                if (d > worstDistance) { worst = i; worstDistance = d }
            }
            if (worst >= 0) { keep[worst] = true; stack.addLast(first to worst); stack.addLast(worst to last) }
        }
        return points.filterIndexed { i, _ -> keep[i] }
    }

    /** Distanza di [p] dal tratto a–b su un piano locale: basta per decidere cosa disegnare. */
    private fun distanceToSegment(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val metersPerDegree = Math.toRadians(1.0) * EARTH_RADIUS_METERS
        val kx = metersPerDegree * cos(Math.toRadians(a.lat))
        val bx = (b.lon - a.lon) * kx
        val by = (b.lat - a.lat) * metersPerDegree
        val px = (p.lon - a.lon) * kx
        val py = (p.lat - a.lat) * metersPerDegree
        val lengthSquared = bx * bx + by * by
        val t = if (lengthSquared == 0.0) 0.0 else ((px * bx + py * by) / lengthSquared).coerceIn(0.0, 1.0)
        return sqrt((px - t * bx).pow(2) + (py - t * by).pow(2))
    }
}
