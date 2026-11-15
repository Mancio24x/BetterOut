package app.betterout

import android.app.Application
import app.betterout.data.AppDatabase
import app.betterout.data.BRouterService
import app.betterout.data.GearRepository
import app.betterout.data.RouteRepository
import app.betterout.data.RoutingService
import app.betterout.data.TripRepository
import java.io.File

/** Contenitore delle dipendenze: un solo database e i suoi repository per tutta l'app. */
class BetterOutApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val trips: TripRepository by lazy { TripRepository(database) }
    val gear: GearRepository by lazy { GearRepository(database) }
    val routes: RouteRepository by lazy { RouteRepository(database, File(filesDir, "percorsi")) }
    val routing: RoutingService by lazy { BRouterService() }
}
