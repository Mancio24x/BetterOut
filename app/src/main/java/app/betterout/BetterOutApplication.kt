package app.betterout

import android.app.Application
import app.betterout.data.AppDatabase
import app.betterout.data.AppSettings
import app.betterout.data.BRouterService
import app.betterout.data.DiveGearRepository
import app.betterout.data.DiveRepository
import app.betterout.data.GearRepository
import app.betterout.data.OfflinePlaces
import app.betterout.data.PlaceSearch
import app.betterout.data.RouteRepository
import app.betterout.data.RoutingService
import app.betterout.data.TripRepository
import app.betterout.data.WeatherRepository
import java.io.File

/** Contenitore delle dipendenze: un solo database e i suoi repository per tutta l'app. */
class BetterOutApplication : Application() {
    val settings: AppSettings by lazy { AppSettings(this) }
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val trips: TripRepository by lazy { TripRepository(database, onDeleted = weather::delete) }
    val gear: GearRepository by lazy { GearRepository(database) }
    val routes: RouteRepository by lazy { RouteRepository(database, File(filesDir, "percorsi")) }
    val routing: RoutingService by lazy { BRouterService() }
    val weather: WeatherRepository by lazy { WeatherRepository(File(filesDir, "meteo")) }
    val diveGear: DiveGearRepository by lazy { DiveGearRepository(database.diveGearDao()) }
    val dives: DiveRepository by lazy { DiveRepository(database.diveDao(), File(filesDir, "immersioni")) }

    override fun onCreate() {
        super.onCreate()
        val offline = OfflinePlaces { assets.open("luoghi-alpi.tsv") }
        PlaceSearch.offline = offline
        // Il file si prepara subito in background: la prima ricerca trova i luoghi già pronti.
        Thread({ runCatching { offline.warmUp() } }, "luoghi-offline").apply { priority = Thread.MIN_PRIORITY }.start()
    }
}
