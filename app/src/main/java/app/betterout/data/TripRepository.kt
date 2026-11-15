package app.betterout.data

import androidx.room.withTransaction
import app.betterout.domain.ChecklistRules
import app.betterout.domain.TripRules
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

class TripRepository(private val db: AppDatabase, private val dao: TripDao = db.tripDao(), private val checklist: ChecklistDao = db.checklistDao(), private val now: () -> Long = System::currentTimeMillis) {
    fun observeAll(): Flow<List<Trip>> = dao.observeAll()
    fun observe(id: String): Flow<Trip?> = dao.observe(id)
    suspend fun get(id: String): Trip? = dao.get(id)

    suspend fun save(trip: Trip) = dao.upsert(trip.copy(updatedAt = now()))

    suspend fun setStatus(id: String, status: TripStatus) {
        dao.get(id)?.let { dao.upsert(it.copy(status = status, updatedAt = now())) }
    }

    /**
     * Crea una copia indipendente in bozza, con la propria checklist, e ne restituisce l'identificativo.
     * L'originale e le sue spunte non vengono toccati.
     */
    suspend fun duplicate(id: String, newTitle: String, newDate: LocalDate?, resetPacked: Boolean = true): String? = db.withTransaction {
        val original = dao.get(id) ?: return@withTransaction null
        val copy = TripRules.duplicate(original, UUID.randomUUID().toString(), newTitle, newDate, now())
        dao.upsert(copy)
        checklist.upsert(ChecklistRules.copiesFor(checklist.forTrip(id), copy.id, resetPacked, now()) { UUID.randomUUID().toString() })
        copy.id
    }

    /** Elimina l'uscita e la sua checklist. Percorsi e inventario restano. */
    suspend fun delete(id: String) = db.withTransaction { checklist.deleteForTrip(id); dao.delete(id) }
}
