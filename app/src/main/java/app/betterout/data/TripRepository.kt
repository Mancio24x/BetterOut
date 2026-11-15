package app.betterout.data

import app.betterout.domain.TripRules
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

class TripRepository(private val dao: TripDao, private val now: () -> Long = System::currentTimeMillis) {
    fun observeAll(): Flow<List<Trip>> = dao.observeAll()
    fun observe(id: String): Flow<Trip?> = dao.observe(id)
    suspend fun get(id: String): Trip? = dao.get(id)

    suspend fun save(trip: Trip) = dao.upsert(trip.copy(updatedAt = now()))

    suspend fun setStatus(id: String, status: TripStatus) {
        dao.get(id)?.let { dao.upsert(it.copy(status = status, updatedAt = now())) }
    }

    /** Crea una copia indipendente in bozza e ne restituisce l'identificativo. L'originale non viene toccato. */
    suspend fun duplicate(id: String, newTitle: String, newDate: LocalDate?): String? {
        val original = dao.get(id) ?: return null
        val copy = TripRules.duplicate(original, UUID.randomUUID().toString(), newTitle, newDate, now())
        dao.upsert(copy)
        return copy.id
    }

    suspend fun delete(id: String) = dao.delete(id)
}
