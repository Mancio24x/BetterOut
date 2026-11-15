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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    @Query("SELECT COUNT(*) FROM trips WHERE routeId = :routeId") fun observeRouteUsage(routeId: String): Flow<Int>
    @Query("UPDATE trips SET routeId = NULL WHERE routeId = :routeId") suspend fun detachRoute(routeId: String)
    @Query("UPDATE trips SET routeId = :routeId, updatedAt = :now WHERE id = :id") suspend fun setRoute(id: String, routeId: String?, now: Long)
}

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes ORDER BY updatedAt DESC") fun observeAll(): Flow<List<Route>>
    @Query("SELECT * FROM routes WHERE id = :id") fun observe(id: String): Flow<Route?>
    @Query("SELECT * FROM routes WHERE id = :id") suspend fun get(id: String): Route?
    @Upsert suspend fun upsert(route: Route)
    @Query("DELETE FROM routes WHERE id = :id") suspend fun delete(id: String)
}

@Dao
interface GearDao {
    @Query("SELECT * FROM gear_items") fun observeItems(): Flow<List<GearItem>>
    @Query("SELECT * FROM gear_items") suspend fun items(): List<GearItem>
    @Query("SELECT * FROM gear_items WHERE id = :id") suspend fun item(id: String): GearItem?
    @Upsert suspend fun upsertItem(item: GearItem)
    @Query("DELETE FROM gear_items WHERE id = :id") suspend fun deleteItem(id: String)
    @Query("SELECT * FROM gear_categories ORDER BY sortOrder") fun observeCategories(): Flow<List<GearCategory>>
    @Query("SELECT * FROM gear_categories ORDER BY sortOrder") suspend fun categories(): List<GearCategory>
    @Upsert suspend fun upsertCategories(categories: List<GearCategory>)
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_entries") fun observeAll(): Flow<List<ChecklistEntry>>
    @Query("SELECT * FROM checklist_entries WHERE tripId = :tripId") fun observeForTrip(tripId: String): Flow<List<ChecklistEntry>>
    @Query("SELECT * FROM checklist_entries WHERE tripId = :tripId") suspend fun forTrip(tripId: String): List<ChecklistEntry>
    @Query("SELECT * FROM checklist_entries WHERE gearItemId = :itemId") suspend fun forItem(itemId: String): List<ChecklistEntry>
    @Query("SELECT COUNT(DISTINCT tripId) FROM checklist_entries WHERE gearItemId = :itemId") suspend fun tripsUsing(itemId: String): Int
    @Upsert suspend fun upsert(entries: List<ChecklistEntry>)
    @Query("DELETE FROM checklist_entries WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM checklist_entries WHERE tripId = :tripId") suspend fun deleteForTrip(tripId: String)
    @Query("UPDATE checklist_entries SET packed = 0 WHERE tripId = :tripId") suspend fun clearPacked(tripId: String)
    @Query("UPDATE checklist_entries SET packed = :packed WHERE id = :id") suspend fun setPacked(id: String, packed: Boolean)
    @Query("UPDATE checklist_entries SET gearItemId = NULL WHERE gearItemId = :itemId") suspend fun detachItem(itemId: String)
}

@Database(entities = [Trip::class, GearCategory::class, GearItem::class, ChecklistEntry::class, Route::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun gearDao(): GearDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun routeDao(): RouteDao

    companion object {
        /** Dalla versione 0.3.0 pubblicata i dati non si cancellano più: ogni cambio di schema ha la sua migrazione. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = db.execSQL("ALTER TABLE gear_items ADD COLUMN preset TEXT")
        }

        // Finché non esiste una versione pubblicata lo schema può cambiare senza migrazioni.
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "betterout.db")
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
