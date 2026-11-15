package app.betterout.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter fun dateToString(d: LocalDate?): String? = d?.toString()
    @TypeConverter fun stringToDate(s: String?): LocalDate? = s?.let(LocalDate::parse)
    @TypeConverter fun timeToString(t: LocalTime?): String? = t?.toString()
    @TypeConverter fun stringToTime(s: String?): LocalTime? = s?.let(LocalTime::parse)
}

@Dao
interface TripDao {
    @Query("SELECT * FROM trips") fun observeAll(): Flow<List<Trip>>
    @Query("SELECT * FROM trips WHERE id = :id") fun observe(id: String): Flow<Trip?>
    @Query("SELECT * FROM trips WHERE id = :id") suspend fun get(id: String): Trip?
    @Upsert suspend fun upsert(trip: Trip)
    @Query("DELETE FROM trips WHERE id = :id") suspend fun delete(id: String)
}

@Database(entities = [Trip::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object {
        // Finché non esiste una versione pubblicata lo schema può cambiare senza migrazioni.
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "betterout.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
