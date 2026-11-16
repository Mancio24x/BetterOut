package app.betterout.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.data.Route
import app.betterout.data.RouteRepository
import app.betterout.data.Trip
import app.betterout.data.TripStatus
import app.betterout.data.WeatherRepository
import app.betterout.domain.TripRules
import app.betterout.ui.components.StatusChip
import app.betterout.ui.map.AppMap
import app.betterout.ui.map.RoutePreview
import app.betterout.ui.map.MapCamera
import app.betterout.ui.map.StatTile
import app.betterout.ui.map.endMarkers
import app.betterout.ui.map.formatDistance
import app.betterout.ui.map.formatMeters
import app.betterout.ui.map.mapLines
import app.betterout.ui.theme.AppIcons
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons5
import app.betterout.ui.trips.TripDetailContent
import app.betterout.ui.trips.TripsViewModel
import app.betterout.ui.trips.currentLocale
import app.betterout.ui.trips.tripWhen
import app.betterout.ui.weather.formatTemp
import java.time.LocalDate

/** Passaggio fra escursioni e immersioni: il tasto sta accanto alle impostazioni in ogni scheda principale. */
data class ModeSwitch(val diving: Boolean, val toggle: () -> Unit)

val LocalModeSwitch = staticCompositionLocalOf<ModeSwitch?> { null }

/** Titolo di una scheda principale, con le azioni in alto a destra (le impostazioni sono sempre l'ultima). */
@Composable
fun TabHeader(title: String, onSettings: () -> Unit, extra: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).semantics { heading() })
        extra()
        // Il passaggio fra le modalità ha uno sfondo pieno: deve vedersi subito, non confondersi con le altre icone.
        LocalModeSwitch.current?.let { mode ->
            FilledTonalIconButton(onClick = mode.toggle) {
                Icon(if (mode.diving) AppIcons.Mountain else AppIcons5.Wave, contentDescription = stringResource(if (mode.diving) R.string.passa_escursioni else R.string.passa_immersioni))
            }
        }
        IconButton(onClick = onSettings) { Icon(AppIcons5.Cog, contentDescription = stringResource(R.string.nav_impostazioni)) }
    }
}

/** Pulsante tondo grande in basso a destra. */
@Composable
fun RoundFab(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier.size(72.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        shape = CircleShape, color = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary, shadowElevation = 6.dp,
    ) { Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, modifier = Modifier.size(36.dp)) } }
}

/** Anteprima del percorso dell'uscita; niente se l'uscita non ha un percorso. Toccandola si apre la mappa. */
@Composable
fun TripMap(routes: RouteRepository, routeId: String?, modifier: Modifier = Modifier, onOpen: (() -> Unit)? = null) =
    RoutePreview(routes, routeId, modifier, onOpen)

/**
 * Home: il prossimo itinerario in grande, con mappa, percorso, meteo e zaino; vuota finché non ce n'è uno.
 * [details] disegna mappa, percorso e meteo dell'uscita.
 */
@Composable
fun HomeScreen(vm: TripsViewModel, onNew: () -> Unit, onOpen: (String) -> Unit, onPack: (String) -> Unit, onMap: () -> Unit, onSettings: () -> Unit, details: @Composable (Trip) -> Unit) {
    val trips by vm.trips.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val list = trips
    val next = list?.let { TripRules.next(it, LocalDate.now()) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TabHeader(stringResource(if (next == null) R.string.home_itinerario else if (next.status == TripStatus.PROGRAMMATA) R.string.prossima_uscita else R.string.ultima_bozza), onSettings) {
                if (next != null) IconButton(onClick = { onOpen(next.id) }) { Icon(AppIcons2.Edit, contentDescription = stringResource(R.string.apri_uscita)) }
                IconButton(onClick = onMap) { Icon(AppIcons.Map, contentDescription = stringResource(R.string.home_mappa_percorsi)) }
            }
            when {
                list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                next == null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.home_vuota), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> TripDetailContent(next, entries.filter { it.tripId == next.id }, { onPack(next.id) }, Modifier.fillMaxSize(), bottomSpace = 104.dp, routeCard = details)
            }
        }
        RoundFab(AppIcons2.Plus, stringResource(R.string.nuova_uscita), onNew, Modifier.align(Alignment.BottomEnd).padding(20.dp))
    }
}

/** "Le tue uscite": tutte le uscite una dopo l'altra, a pillole. Toccandone una si apre con mappa, meteo, zaino e azioni. */
@Composable
fun TripsScreen(vm: TripsViewModel, routes: List<Route>, weather: WeatherRepository, onSettings: () -> Unit, expanded: @Composable (Trip) -> Unit) {
    val trips by vm.trips.collectAsStateWithLifecycle()
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    val today = LocalDate.now()
    val byId = remember(routes) { routes.associateBy { it.id } }
    // Le temperature si leggono tutte insieme, una volta: lette scheda per scheda arriverebbero durante lo scorrimento
    // e farebbero "saltare" le pillole.
    val temperatures by produceState(emptyMap<String, String>(), trips) {
        value = trips.orEmpty().mapNotNull { trip ->
            val d = trip.startDate?.let { day -> weather.lastSaved(trip.id)?.points?.firstOrNull()?.day(day) } ?: return@mapNotNull null
            if (d.tempMin != null && d.tempMax != null) trip.id to formatTemp(d.tempMin) + " / " + formatTemp(d.tempMax) else null
        }.toMap()
    }
    val sorted = remember(trips) { trips?.let { TripRules.sorted(it, today) } }
    Column(Modifier.fillMaxSize()) {
        TabHeader(stringResource(R.string.le_tue_uscite), onSettings)
        val list = sorted
        when {
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            list.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.uscite_vuote), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { trip ->
                    Box(Modifier.animateItem()) {
                        TripPill(trip, trip.routeId?.let(byId::get), temperatures[trip.id], open = openId == trip.id, onToggle = { openId = if (openId == trip.id) null else trip.id }) { expanded(trip) }
                    }
                }
            }
        }
    }
}

@Composable
/** [temperature]: l'ultima previsione salvata per il primo giorno dell'uscita, alla partenza. */
private fun TripPill(trip: Trip, route: Route?, temperature: String?, open: Boolean, onToggle: () -> Unit, expanded: @Composable () -> Unit) {
    val locale = currentLocale()
    val state = stringResource(if (open) R.string.pillola_aperta else R.string.pillola_chiusa)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Column {
            Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onToggle).semantics { stateDescription = state }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(trip.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(tripWhen(trip, withYear = true), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusChip(trip.status)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(route?.maxElevation?.let { formatMeters(it, locale) }, stringResource(R.string.stat_quota_max), Modifier.weight(1f))
                    StatTile(temperature, stringResource(R.string.stat_temperatura), Modifier.weight(1f))
                    StatTile(route?.let { formatDistance(it.distanceMeters, locale) }, stringResource(R.string.stat_distanza), Modifier.weight(1f))
                }
            }
            AnimatedVisibility(open) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { expanded() }
            }
        }
    }
}
