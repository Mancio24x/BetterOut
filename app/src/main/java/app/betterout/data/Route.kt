package app.betterout.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.betterout.domain.GeoPoint
import app.betterout.domain.GpxWaypoint
import app.betterout.domain.RouteDraft
import kotlinx.serialization.Serializable

enum class RouteOrigin { DISEGNATO, IMPORTATO }

/** Da dove vengono le quote dei punti. Le quote chieste al modello del terreno sono segnalate a parte ([Route.modelElevation]). */
enum class ElevationSource { NESSUNA, GPX, ROUTING }

/**
 * Un percorso dell'archivio. Qui stanno solo nome, origine e statistiche già calcolate: i punti sono in un file a parte
 * ([RouteGeometry]), perché un tracciato lungo non entra in una riga del database.
 * Salita, discesa e quote sono null quando nessun punto ha la quota.
 */
@Entity(tableName = "routes")
data class Route(
    @PrimaryKey val id: String,
    val name: String,
    val origin: RouteOrigin,
    val distanceMeters: Double,
    val ascentMeters: Double? = null,
    val descentMeters: Double? = null,
    val minElevation: Double? = null,
    val maxElevation: Double? = null,
    val elevationSource: ElevationSource = ElevationSource.NESSUNA,
    val partialElevation: Boolean = false,
    val modelElevation: Boolean = false,
    val hasManualLegs: Boolean = false,
    val segmentCount: Int = 1,
    val sourceFileName: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** I punti di un percorso: i segmenti di un file importato oppure il disegno fatto nell'editor. */
@Serializable
data class RouteGeometry(val segments: List<List<GeoPoint>> = emptyList(), val draft: RouteDraft? = null, val waypoints: List<GpxWaypoint> = emptyList()) {
    /** Le linee su cui si calcolano le statistiche: i tratti contigui di un disegno formano una linea sola. */
    val lines: List<List<GeoPoint>> get() = draft?.lines() ?: segments
}
