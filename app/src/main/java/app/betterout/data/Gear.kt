package app.betterout.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.betterout.domain.GearFunction
import app.betterout.domain.GearKind

@Entity(tableName = "gear_categories")
data class GearCategory(@PrimaryKey val id: String, val name: String, val sortOrder: Int, val builtIn: Boolean = false)

/** Oggetto dell'inventario, riutilizzabile in ogni uscita. */
@Entity(tableName = "gear_items")
data class GearItem(
    @PrimaryKey val id: String,
    val name: String,
    val weightGrams: Int? = null,
    val weightIsEstimate: Boolean = false,
    val categoryId: String,
    val kind: GearKind = GearKind.BASE,
    val function: GearFunction = GearFunction.ALTRO,
    val volumeLiters: Double? = null,
    val capacityLiters: Double? = null,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Riga della checklist di un'uscita. Conserva una copia dei dati dell'oggetto (nome, peso, categoria, tipo, volume)
 * più il collegamento [gearItemId], che diventa null se l'oggetto viene eliminato dall'inventario.
 */
@Entity(tableName = "checklist_entries", indices = [Index("tripId"), Index("gearItemId")])
data class ChecklistEntry(
    @PrimaryKey val id: String,
    val tripId: String,
    val gearItemId: String?,
    val name: String,
    val unitGrams: Int?,
    val weightIsEstimate: Boolean = false,
    val categoryName: String,
    val categoryOrder: Int = 0,
    val kind: GearKind,
    val function: GearFunction = GearFunction.ALTRO,
    val volumeLiters: Double? = null,
    val quantity: Int = 1,
    val packed: Boolean = false,
    val notes: String = "",
    val createdAt: Long,
)
