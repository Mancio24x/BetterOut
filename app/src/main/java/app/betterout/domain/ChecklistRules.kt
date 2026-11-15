package app.betterout.domain

import app.betterout.data.ChecklistEntry
import app.betterout.data.GearCategory
import app.betterout.data.GearItem
import app.betterout.data.TripStatus

data class ChecklistGroup(val category: String, val entries: List<ChecklistEntry>) {
    val packed: Int get() = entries.count { it.packed }
    val grams: Long get() = entries.sumOf { (it.unitGrams ?: 0).toLong() * it.quantity }
}

data class VolumeEstimate(val liters: Double, val entriesWithoutVolume: Int, val capacityLiters: Double?)

object ChecklistRules {
    fun totals(entries: List<ChecklistEntry>, waterLiters: Double): PackTotals =
        PackWeights.totals(entries.map { WeightLine(it.unitGrams, it.quantity, it.kind) }, waterLiters)

    /** Righe raggruppate per categoria, nell'ordine delle categorie e poi per nome. */
    fun groups(entries: List<ChecklistEntry>): List<ChecklistGroup> =
        entries.groupBy { it.categoryName }.map { (name, list) -> ChecklistGroup(name, list.sortedBy { it.name.lowercase() }) }
            .sortedWith(compareBy<ChecklistGroup> { g -> g.entries.minOf { it.categoryOrder } }.thenBy { it.category.lowercase() })

    fun packedCount(entries: List<ChecklistEntry>): Int = entries.count { it.packed }

    /** Nuova riga a partire da un oggetto dell'inventario: i dati sono copiati, il collegamento resta. */
    fun entryFrom(item: GearItem, category: GearCategory?, tripId: String, id: String, now: Long, quantity: Int = 1): ChecklistEntry = ChecklistEntry(
        id = id, tripId = tripId, gearItemId = item.id, name = item.name, unitGrams = item.weightGrams, weightIsEstimate = item.weightIsEstimate,
        categoryName = category?.name.orEmpty(), categoryOrder = category?.sortOrder ?: Int.MAX_VALUE, kind = item.kind, function = item.function,
        volumeLiters = item.volumeLiters, quantity = quantity.coerceAtLeast(1), createdAt = now,
    )

    /**
     * Modifica di un oggetto dell'inventario: le righe delle uscite in bozza o programmate si aggiornano,
     * quelle delle uscite completate o annullate restano com'erano (storico).
     * Tipo, quantità, nota e spunta sono scelte dell'uscita e non vengono toccate.
     */
    fun refreshed(entry: ChecklistEntry, item: GearItem, category: GearCategory?, tripStatus: TripStatus): ChecklistEntry =
        if (tripStatus == TripStatus.COMPLETATA || tripStatus == TripStatus.ANNULLATA) entry
        else entry.copy(
            name = item.name, unitGrams = item.weightGrams, weightIsEstimate = item.weightIsEstimate, function = item.function, volumeLiters = item.volumeLiters,
            categoryName = category?.name ?: entry.categoryName, categoryOrder = category?.sortOrder ?: entry.categoryOrder,
        )

    /** Copia delle righe per un'uscita duplicata: nuovi identificativi, spunte azzerate se richiesto. */
    fun copiesFor(entries: List<ChecklistEntry>, newTripId: String, resetPacked: Boolean, now: Long, newId: () -> String): List<ChecklistEntry> =
        entries.map { it.copy(id = newId(), tripId = newTripId, packed = if (resetPacked) false else it.packed, createdAt = now) }

    /** Volume delle righe non indossate rispetto alla capacità dello zaino presente in checklist. */
    fun volume(entries: List<ChecklistEntry>, capacityOf: (ChecklistEntry) -> Double?): VolumeEstimate {
        val carried = entries.filter { it.kind != GearKind.INDOSSATO && it.function != GearFunction.ZAINO }
        return VolumeEstimate(
            liters = carried.sumOf { (it.volumeLiters ?: 0.0) * it.quantity },
            entriesWithoutVolume = carried.count { it.volumeLiters == null },
            capacityLiters = entries.filter { it.function == GearFunction.ZAINO }.firstNotNullOfOrNull(capacityOf),
        )
    }

    private fun csv(value: String): String = if (value.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    /** CSV con separatore punto e virgola (quello che i fogli di calcolo italiani aprono senza passaggi). */
    fun checklistCsv(entries: List<ChecklistEntry>, kindLabel: (GearKind) -> String): String = buildString {
        append("Categoria;Oggetto;Quantità;Peso unitario (g);Peso totale (g);Tipo;Pronto;Note\r\n")
        groups(entries).forEach { g ->
            g.entries.forEach { e ->
                append(listOf(csv(e.categoryName), csv(e.name), e.quantity.toString(), e.unitGrams?.toString().orEmpty(), e.unitGrams?.let { (it.toLong() * e.quantity).toString() }.orEmpty(), csv(kindLabel(e.kind)), if (e.packed) "sì" else "no", csv(e.notes)).joinToString(";"))
                append("\r\n")
            }
        }
    }

    fun inventoryCsv(items: List<GearItem>, categoryName: (String) -> String, kindLabel: (GearKind) -> String): String = buildString {
        append("Categoria;Oggetto;Peso (g);Peso stimato;Tipo;Volume (L);Capacità (L);Note\r\n")
        items.sortedWith(compareBy({ categoryName(it.categoryId).lowercase() }, { it.name.lowercase() })).forEach { i ->
            append(listOf(csv(categoryName(i.categoryId)), csv(i.name), i.weightGrams?.toString().orEmpty(), if (i.weightIsEstimate) "sì" else "no", csv(kindLabel(i.kind)), i.volumeLiters?.toString().orEmpty(), i.capacityLiters?.toString().orEmpty(), csv(i.notes)).joinToString(";"))
            append("\r\n")
        }
    }
}
