package app.betterout.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Un pezzo dell'attrezzatura sub, separata da quella delle escursioni. [preset] è il modello da cui è nato
 * (dà il simbolo), null per un pezzo scritto a mano. [packed] è la spunta della checklist: pronto per l'immersione.
 */
@Entity(tableName = "dive_gear")
data class DiveGear(
    @PrimaryKey val id: String,
    val name: String,
    val preset: String? = null,
    val notes: String = "",
    val packed: Boolean = false,
    val createdAt: Long,
)

@Dao
interface DiveGearDao {
    @Query("SELECT * FROM dive_gear ORDER BY createdAt") fun observeAll(): Flow<List<DiveGear>>
    @Upsert suspend fun upsert(item: DiveGear)
    @Query("DELETE FROM dive_gear WHERE id = :id") suspend fun delete(id: String)
    @Query("UPDATE dive_gear SET packed = :packed WHERE id = :id") suspend fun setPacked(id: String, packed: Boolean)
    @Query("UPDATE dive_gear SET packed = 0") suspend fun clearPacked()
}

class DiveGearRepository(private val dao: DiveGearDao, private val now: () -> Long = System::currentTimeMillis) {
    fun observeAll(): Flow<List<DiveGear>> = dao.observeAll()

    suspend fun add(name: String, preset: String?) = dao.upsert(DiveGear(UUID.randomUUID().toString(), name.trim(), preset, createdAt = now()))
    suspend fun save(item: DiveGear) = dao.upsert(item.copy(name = item.name.trim(), notes = item.notes.trim()))
    suspend fun delete(id: String) = dao.delete(id)
    suspend fun setPacked(id: String, packed: Boolean) = dao.setPacked(id, packed)

    /** Toglie tutte le spunte: il kit si prepara di nuovo per la prossima immersione. I pezzi restano. */
    suspend fun clearPacked() = dao.clearPacked()
}
