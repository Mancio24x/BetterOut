package app.betterout.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import app.betterout.domain.DiveRules
import app.betterout.domain.DiveSample
import app.betterout.domain.ImportedDive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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

/** Il logbook: le immersioni nel database, i profili (profondità nel tempo) in file, uno per immersione. */
class DiveRepository(private val dao: DiveDao, private val dir: File, private val now: () -> Long = System::currentTimeMillis) {
    fun observeAll(): Flow<List<Dive>> = dao.observeAll()
    fun observe(id: String): Flow<Dive?> = dao.observe(id)
    suspend fun get(id: String): Dive? = dao.get(id)
    suspend fun all(): List<Dive> = dao.all()

    /** Una nuova immersione di oggi, con il numero successivo all'ultimo del logbook. */
    suspend fun blank(today: LocalDate = LocalDate.now()): Dive {
        val t = now()
        return Dive(id = UUID.randomUUID().toString(), number = DiveRules.nextNumber(dao.all()), date = today, createdAt = t, updatedAt = t)
    }

    suspend fun save(dive: Dive) = dao.upsert(dive.copy(updatedAt = now()))

    suspend fun delete(id: String) {
        dao.delete(id)
        withContext(Dispatchers.IO) { profileFile(id).delete() }
    }

    /** Il profilo salvato, vuoto se l'immersione è stata scritta a mano. */
    suspend fun profile(id: String): List<DiveSample> = withContext(Dispatchers.IO) {
        val f = profileFile(id)
        if (!f.exists()) emptyList() else f.readLines().mapNotNull { line ->
            val p = line.split(',')
            val s = p.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val d = p.getOrNull(1)?.toDoubleOrNull() ?: return@mapNotNull null
            DiveSample(s, d, p.getOrNull(2)?.toDoubleOrNull())
        }
    }

    /** Aggiunge al logbook le immersioni lette da un file, con numeri e profili; restituisce quante. */
    suspend fun import(dives: List<ImportedDive>): Int {
        val numbers = DiveRules.importNumbers(dao.all(), dives)
        dives.forEachIndexed { i, d ->
            val t = now()
            val dive = Dive(
                id = UUID.randomUUID().toString(), number = numbers[i], siteName = d.siteName, latitude = d.latitude, longitude = d.longitude,
                date = d.date, startTime = d.startTime, durationMinutes = d.durationMinutes, maxDepthMeters = d.maxDepthMeters,
                averageDepthMeters = d.averageDepthMeters, waterTempC = d.waterTempC, visibilityMeters = d.visibilityMeters,
                oxygenPercent = d.oxygenPercent, tankLiters = d.tankLiters, startBar = d.startBar, endBar = d.endBar,
                buddy = d.buddy, notes = d.notes, createdAt = t, updatedAt = t,
            )
            // Prima il file, poi la riga: un'immersione nel logbook non resta mai senza il suo profilo.
            if (d.samples.isNotEmpty()) withContext(Dispatchers.IO) {
                dir.mkdirs()
                profileFile(dive.id).writeText(d.samples.joinToString("\n") { s -> listOfNotNull(s.seconds.toString(), s.depthMeters.toString(), s.tempC?.toString()).joinToString(",") })
            }
            dao.upsert(dive)
        }
        return dives.size
    }

    private fun profileFile(id: String) = File(dir, "$id.csv")
}
