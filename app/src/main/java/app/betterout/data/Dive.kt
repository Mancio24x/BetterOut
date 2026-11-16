package app.betterout.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import app.betterout.domain.DiveRules
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Un'immersione del logbook, compilata a mano. [number] è il numero progressivo del subacqueo.
 * Le misure sono in unità metriche: metri, minuti, gradi Celsius, bar, litri. [oxygenPercent] 21 è aria.
 */
@Entity(tableName = "dives")
data class Dive(
    @PrimaryKey val id: String,
    val number: Int,
    val siteName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val date: LocalDate,
    val startTime: LocalTime? = null,
    val durationMinutes: Int? = null,
    val maxDepthMeters: Double? = null,
    val averageDepthMeters: Double? = null,
    val waterTempC: Double? = null,
    val visibilityMeters: Double? = null,
    val oxygenPercent: Int = 21,
    val tankLiters: Double? = null,
    val startBar: Int? = null,
    val endBar: Int? = null,
    val buddy: String = "",
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface DiveDao {
    @Query("SELECT * FROM dives") fun observeAll(): Flow<List<Dive>>
    @Query("SELECT * FROM dives") suspend fun all(): List<Dive>
    @Query("SELECT * FROM dives WHERE id = :id") fun observe(id: String): Flow<Dive?>
    @Query("SELECT * FROM dives WHERE id = :id") suspend fun get(id: String): Dive?
    @Upsert suspend fun upsert(dive: Dive)
    @Query("DELETE FROM dives WHERE id = :id") suspend fun delete(id: String)
}

class DiveRepository(private val dao: DiveDao, private val now: () -> Long = System::currentTimeMillis) {
    fun observeAll(): Flow<List<Dive>> = dao.observeAll()
    fun observe(id: String): Flow<Dive?> = dao.observe(id)
    suspend fun get(id: String): Dive? = dao.get(id)

    /** Una nuova immersione di oggi, con il numero successivo all'ultimo del logbook. */
    suspend fun blank(today: LocalDate = LocalDate.now()): Dive {
        val t = now()
        return Dive(id = UUID.randomUUID().toString(), number = DiveRules.nextNumber(dao.all()), date = today, createdAt = t, updatedAt = t)
    }

    suspend fun save(dive: Dive) = dao.upsert(dive.copy(updatedAt = now()))
    suspend fun delete(id: String) = dao.delete(id)
}
