package app.betterout.domain

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

enum class Pace(val factor: Double) { LENTO(1.25), MEDIO(1.0), VELOCE(0.8) }

/** Velocità di riferimento del metodo escursionistico (DIN 33466), modificabili dalle impostazioni. */
data class PaceSettings(
    val flatKmPerHour: Double = 4.0,
    val ascentMetersPerHour: Double = 300.0,
    val descentMetersPerHour: Double = 500.0,
)

object DurationEstimator {
    /**
     * Durata di cammino senza soste, in minuti.
     * to = distanza / velocità in piano; tv = salita / velocità in salita + discesa / velocità in discesa;
     * durata = max(to, tv) + min(to, tv) / 2, moltiplicata per il fattore del ritmo.
     */
    fun minutes(distanceMeters: Double, ascentMeters: Double, descentMeters: Double, pace: Pace = Pace.MEDIO, settings: PaceSettings = PaceSettings()): Int {
        require(distanceMeters >= 0 && ascentMeters >= 0 && descentMeters >= 0) { "valori negativi" }
        val horizontal = distanceMeters / 1000.0 / settings.flatKmPerHour
        val vertical = ascentMeters / settings.ascentMetersPerHour + descentMeters / settings.descentMetersPerHour
        val hours = (max(horizontal, vertical) + min(horizontal, vertical) / 2.0) * pace.factor
        return Math.round(hours * 60.0).toInt()
    }

    /** Giorni di cammino: durata divisa per le ore al giorno, arrotondata per eccesso (almeno 1). */
    fun days(totalMinutes: Int, walkingHoursPerDay: Double): Int {
        require(walkingHoursPerDay > 0) { "ore al giorno non valide" }
        return max(1, ceil(totalMinutes / 60.0 / walkingHoursPerDay).toInt())
    }

    fun nights(days: Int): Int = max(0, days - 1)
}
