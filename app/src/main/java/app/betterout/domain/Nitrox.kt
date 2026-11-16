package app.betterout.domain

import kotlin.math.floor

/**
 * Calcoli per le miscele nitrox, in acqua di mare, con l'approssimazione usata nei corsi: 10 m d'acqua = 1 bar,
 * più 1 bar di atmosfera in superficie. Sono calcoli indicativi per pianificare e controllare: non sostituiscono
 * la formazione né il computer subacqueo.
 */
object Nitrox {
    /** Pressione parziale massima dell'ossigeno consigliata per la parte di fondo dell'immersione. */
    const val PPO2_LAVORO = 1.4

    /** Limite di emergenza, da non superare mai. */
    const val PPO2_LIMITE = 1.6

    /** Pressione assoluta in bar alla profondità [depthMeters]. */
    fun ambientBar(depthMeters: Double): Double = depthMeters / 10.0 + 1.0

    /** Profondità massima operativa in metri per una miscela con [oxygenPercent] di ossigeno e il limite [ppO2]. */
    fun mod(oxygenPercent: Int, ppO2: Double = PPO2_LAVORO): Double {
        require(oxygenPercent in 1..100) { "ossigeno fuori intervallo" }
        return (ppO2 / (oxygenPercent / 100.0) - 1.0) * 10.0
    }

    /**
     * La miscela più ricca di ossigeno usabile fino a [depthMeters] senza superare [ppO2], arrotondata per difetto
     * al punto percentuale (arrotondare per eccesso supererebbe il limite). Mai sotto l'aria, mai oltre il 100%.
     */
    fun bestMix(depthMeters: Double, ppO2: Double = PPO2_LAVORO): Int {
        require(depthMeters >= 0) { "profondità negativa" }
        return floor(ppO2 / ambientBar(depthMeters) * 100.0 + 1e-9).toInt().coerceIn(21, 100)
    }

    /** Profondità equivalente in aria: a quale profondità respirare aria darebbe la stessa pressione parziale d'azoto. */
    fun ead(depthMeters: Double, oxygenPercent: Int): Double {
        val nitrogen = 1.0 - oxygenPercent / 100.0
        return ((depthMeters + 10.0) * nitrogen / 0.79 - 10.0).coerceAtLeast(0.0)
    }

    /** Pressione parziale dell'ossigeno respirando la miscela a [depthMeters]. */
    fun ppO2(depthMeters: Double, oxygenPercent: Int): Double = oxygenPercent / 100.0 * ambientBar(depthMeters)
}
