package app.betterout.domain

enum class GearKind { BASE, INDOSSATO, CONSUMABILE }

/** Una riga di checklist ai fini del peso: peso unitario in grammi (null se non noto), quantità, tipo. */
data class WeightLine(val unitGrams: Int?, val quantity: Int, val kind: GearKind)

data class PackTotals(
    val baseGrams: Long,
    val consumableGrams: Long,
    val waterGrams: Long,
    val wornGrams: Long,
    val linesWithoutWeight: Int,
) {
    /** Consumabili compresa l'acqua. */
    val consumableWithWaterGrams: Long get() = consumableGrams + waterGrams
    /** Peso nello zaino = base + consumabili (acqua compresa). */
    val packGrams: Long get() = baseGrams + consumableWithWaterGrams
    /** Peso complessivo = zaino + indossato. */
    val totalGrams: Long get() = packGrams + wornGrams
    val isPartial: Boolean get() = linesWithoutWeight > 0
}

object PackWeights {
    const val GRAMS_PER_LITER_OF_WATER = 1000

    /**
     * Ogni riga ha un solo tipo, quindi nessun peso è contato due volte. L'acqua è una voce a parte:
     * il contenitore è una riga con il proprio peso a vuoto.
     */
    fun totals(lines: List<WeightLine>, waterLiters: Double = 0.0): PackTotals {
        require(waterLiters >= 0) { "litri negativi" }
        fun sum(kind: GearKind) = lines.filter { it.kind == kind }.sumOf { (it.unitGrams ?: 0).toLong() * it.quantity.coerceAtLeast(0) }
        return PackTotals(
            baseGrams = sum(GearKind.BASE),
            consumableGrams = sum(GearKind.CONSUMABILE),
            waterGrams = Math.round(waterLiters * GRAMS_PER_LITER_OF_WATER),
            wornGrams = sum(GearKind.INDOSSATO),
            linesWithoutWeight = lines.count { it.unitGrams == null && it.quantity > 0 },
        )
    }
}

enum class WeightUnit { G, HG, KG }

object WeightFormat {
    /**
     * Sotto l'unità scelta si mostrano sempre i grammi (sotto un chilo con kg, sotto un etto con hg).
     * La virgola decimale segue la lingua indicata.
     */
    fun format(grams: Long, unit: WeightUnit = WeightUnit.KG, locale: java.util.Locale = java.util.Locale.ITALIAN): String = when {
        unit == WeightUnit.KG && grams >= 1000 -> String.format(locale, "%.2f kg", grams / 1000.0)
        unit == WeightUnit.HG && grams >= 100 -> String.format(locale, "%.1f hg", grams / 100.0)
        else -> String.format(locale, "%d g", grams)
    }
}
