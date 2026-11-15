package app.betterout.data

import androidx.room.withTransaction
import app.betterout.domain.ChecklistRules
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class GearRepository(private val db: AppDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val gear = db.gearDao()
    private val checklist = db.checklistDao()
    private val trips = db.tripDao()

    fun observeItems(): Flow<List<GearItem>> = gear.observeItems()
    fun observeCategories(): Flow<List<GearCategory>> = gear.observeCategories()
    fun observeChecklist(tripId: String): Flow<List<ChecklistEntry>> = checklist.observeForTrip(tripId)
    fun observeAllEntries(): Flow<List<ChecklistEntry>> = checklist.observeAll()
    suspend fun item(id: String): GearItem? = gear.item(id)
    suspend fun items(): List<GearItem> = gear.items()
    suspend fun categories(): List<GearCategory> = gear.categories()
    suspend fun tripsUsing(itemId: String): Int = checklist.tripsUsing(itemId)

    /** Le sei categorie iniziali, create una sola volta. */
    suspend fun ensureDefaultCategories() {
        if (gear.categories().isEmpty()) gear.upsertCategories(DEFAULT_CATEGORIES.mapIndexed { i, name -> GearCategory("cat-$i", name, i, builtIn = true) })
    }

    suspend fun addCategory(name: String): GearCategory {
        val existing = gear.categories()
        existing.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }?.let { return it }
        return GearCategory(UUID.randomUUID().toString(), name.trim(), (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1).also { gear.upsertCategories(listOf(it)) }
    }

    /** Salva l'oggetto e aggiorna le righe delle uscite in bozza o programmate; lo storico resta com'era. */
    suspend fun saveItem(item: GearItem) = db.withTransaction {
        val saved = item.copy(updatedAt = now())
        gear.upsertItem(saved)
        val category = gear.categories().firstOrNull { it.id == saved.categoryId }
        val linked = checklist.forItem(saved.id)
        if (linked.isNotEmpty()) {
            val statuses = linked.map { it.tripId }.distinct().associateWith { trips.get(it)?.status }
            checklist.upsert(linked.map { e -> statuses[e.tripId]?.let { ChecklistRules.refreshed(e, saved, category, it) } ?: e })
        }
    }

    /** Elimina l'oggetto dall'inventario: le righe di checklist restano con i dati copiati, senza collegamento. */
    suspend fun deleteItem(id: String) = db.withTransaction { checklist.detachItem(id); gear.deleteItem(id) }

    /** Aggiunge alla checklist gli oggetti indicati, saltando quelli già presenti: nessun doppione. */
    suspend fun addToChecklist(tripId: String, itemIds: List<String>) = db.withTransaction {
        val present = checklist.forTrip(tripId).mapNotNull { it.gearItemId }.toSet()
        val categories = gear.categories().associateBy { it.id }
        val entries = itemIds.distinct().filter { it !in present }.mapNotNull { gear.item(it) }
            .map { ChecklistRules.entryFrom(it, categories[it.categoryId], tripId, UUID.randomUUID().toString(), now()) }
        checklist.upsert(entries)
    }

    suspend fun updateEntry(entry: ChecklistEntry) = checklist.upsert(listOf(entry.copy(quantity = entry.quantity.coerceAtLeast(1))))
    suspend fun setPacked(entryId: String, packed: Boolean) = checklist.setPacked(entryId, packed)
    suspend fun removeEntry(entryId: String) = checklist.delete(entryId)
    suspend fun clearPacked(tripId: String) = checklist.clearPacked(tripId)

    companion object {
        val DEFAULT_CATEGORIES = listOf("Trasporto", "Ricambi", "Cucina e acqua", "Strati", "Notte", "Sicurezza")
    }
}
