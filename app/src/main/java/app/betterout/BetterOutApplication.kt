package app.betterout

import android.app.Application
import app.betterout.data.AppDatabase
import app.betterout.data.GearRepository
import app.betterout.data.TripRepository

/** Contenitore delle dipendenze: un solo database e i suoi repository per tutta l'app. */
class BetterOutApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val trips: TripRepository by lazy { TripRepository(database) }
    val gear: GearRepository by lazy { GearRepository(database) }
}
