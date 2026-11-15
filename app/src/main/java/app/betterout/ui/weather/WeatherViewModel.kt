package app.betterout.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.betterout.data.Route
import app.betterout.data.RouteRepository
import app.betterout.data.Trip
import app.betterout.data.TripRepository
import app.betterout.data.WeatherRepository
import app.betterout.domain.Climate
import app.betterout.domain.WeatherReport
import app.betterout.domain.WeatherRules
import app.betterout.domain.WeatherRules.Plan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

/**
 * Meteo di un'uscita. Le previsioni sono chieste all'apertura se il dato salvato manca o ha più di tre ore,
 * e quando l'utente lo chiede. Un errore lascia in vista l'ultimo dato salvato e non tocca l'uscita.
 */
class WeatherViewModel(private val trips: TripRepository, private val routes: RouteRepository, private val weather: WeatherRepository, private val tripId: String) : ViewModel() {
    /** [plan] è null finché l'uscita non è stata letta. [report] è l'ultima previsione valida per i punti e i giorni di [plan]. */
    data class State(
        val trip: Trip? = null, val route: Route? = null, val plan: Plan? = null,
        val report: WeatherReport? = null, val loading: Boolean = false, val failed: Boolean = false,
        val climate: Climate? = null, val climateFailed: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var job: Job? = null

    init {
        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            trips.observe(tripId).flatMapLatest { trip -> (trip?.routeId?.let(routes::observe) ?: flowOf(null)).map { trip to it } }.collect { (trip, route) ->
                if (trip == null) { job?.cancel(); _state.value = State(); return@collect }
                val lines = route?.let { routes.geometry(it.id) }?.lines
                val plan = WeatherRules.plan(trip.startDate, trip.endDate, today(trip), WeatherRules.points(trip.latitude, trip.longitude, lines))
                // Il meteo si richiede solo se cambiano i punti o i giorni, non a ogni modifica dell'uscita.
                if (plan == _state.value.plan) _state.update { it.copy(trip = trip, route = route) }
                else { _state.value = State(trip, route, plan); load(force = false) }
            }
        }
    }

    fun refresh() = load(force = true)

    private fun load(force: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            val trip = _state.value.trip ?: return@launch
            when (val plan = _state.value.plan) {
                is Plan.Ask -> {
                    _state.update { it.copy(report = weather.cached(tripId, plan), loading = true, failed = false) }
                    try {
                        val report = weather.forecast(tripId, plan, force)
                        _state.update { it.copy(report = report, loading = false) }
                    } catch (e: IOException) {
                        _state.update { it.copy(loading = false, failed = true) }
                    }
                }
                is Plan.TooEarly -> {
                    _state.update { it.copy(loading = true, climateFailed = false) }
                    try {
                        // Il clima è chiesto per il punto più alto, quello dove fa più freddo.
                        val climate = weather.climate(tripId, plan.points.last(), plan.start, plan.end, today(trip))
                        _state.update { it.copy(climate = climate, loading = false) }
                    } catch (e: IOException) {
                        _state.update { it.copy(loading = false, climateFailed = true) }
                    }
                }
                else -> Unit
            }
        }
    }

    /** "Oggi" nel fuso della destinazione, lo stesso in cui il servizio dà i giorni. */
    private fun today(trip: Trip): LocalDate = LocalDate.now(runCatching { ZoneId.of(trip.timeZoneId) }.getOrDefault(ZoneId.systemDefault()))

    companion object {
        fun factory(trips: TripRepository, routes: RouteRepository, weather: WeatherRepository, tripId: String): ViewModelProvider.Factory =
            viewModelFactory { initializer { WeatherViewModel(trips, routes, weather, tripId) } }
    }
}
