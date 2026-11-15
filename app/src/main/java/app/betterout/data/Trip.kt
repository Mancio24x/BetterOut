package app.betterout.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

enum class TripStatus { BOZZA, PROGRAMMATA, COMPLETATA, ANNULLATA }
enum class Activity { ESCURSIONE, TREKKING, FERRATA, INVERNALE, CORSA, ALTRO }
enum class Overnight { NESSUNO, TENDA, RIFUGIO, BIVACCO }
enum class WaterAvailability { NON_INDICATA, FREQUENTE, SCARSA, ASSENTE }
enum class Difficulty { NON_INDICATA, T, E, EE, EEA }
enum class TripPace { LENTO, MEDIO, VELOCE }

/** Un'uscita. Date e ora sono locali alla destinazione; [timeZoneId] dice quale fuso. */
@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val notes: String = "",
    val placeName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val timeZoneId: String,
    val activity: Activity = Activity.ESCURSIONE,
    val status: TripStatus = TripStatus.BOZZA,
    val routeId: String? = null,
    val overnight: Overnight = Overnight.NESSUNO,
    val water: WaterAvailability = WaterAvailability.NON_INDICATA,
    val pace: TripPace = TripPace.MEDIO,
    val walkingHoursPerDay: Int = 6,
    val difficulty: Difficulty = Difficulty.NON_INDICATA,
    val waterLiters: Double = 0.0,
    val createdAt: Long,
    val updatedAt: Long,
)
