package app.betterout.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.data.ChecklistEntry
import app.betterout.data.Route
import app.betterout.data.RouteRepository
import app.betterout.data.Trip
import app.betterout.data.TripStatus
import app.betterout.data.WeatherRepository
import app.betterout.domain.ChecklistRules
import app.betterout.domain.DayWeather
import app.betterout.domain.Pace
import app.betterout.domain.TripRules
import app.betterout.ui.components.EmptyState
import app.betterout.ui.components.StatusChip
import app.betterout.ui.gear.weight
import app.betterout.ui.map.estimatedMinutes
import app.betterout.ui.map.formatDistance
import app.betterout.ui.map.formatDuration
import app.betterout.ui.map.formatMeters
import app.betterout.ui.theme.AppIcons
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons4
import app.betterout.ui.theme.AppIcons5
import app.betterout.ui.trips.TripsViewModel
import app.betterout.ui.trips.currentLocale
import app.betterout.ui.trips.tripCountdown
import app.betterout.ui.trips.tripWhen
import app.betterout.ui.weather.formatTemp
import app.betterout.ui.weather.skyIcon
import app.betterout.ui.weather.skyLabel
import java.time.LocalDate

// ---- Barra in alto delle schede principali ----

/** Il passaggio fra escursioni e immersioni, accanto alle impostazioni: tonalità tenue del colore principale, ben visibile ma non urlato. */
@Composable
fun ModeSwitchButton() {
    val mode = LocalModeSwitch.current ?: return
    FilledTonalIconButton(
        onClick = mode.toggle,
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
    ) {
        Icon(if (mode.diving) AppIcons.Mountain else AppIcons5.Wave, contentDescription = stringResource(if (mode.diving) R.string.passa_escursioni else R.string.passa_immersioni))
    }
}

/**
 * Barra Material 3 delle schede principali: titolo grande che, scorrendo, si comprime in una barra sottile.
 * Gli spazi di sistema in alto li gestisce già la cornice dell'app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabTopBar(title: String, onSettings: () -> Unit, scroll: TopAppBarScrollBehavior, actions: @Composable RowScope.() -> Unit = {}) {
    LargeTopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        actions = {
            actions()
            ModeSwitchButton()
            IconButton(onClick = onSettings) { Icon(AppIcons5.Cog, contentDescription = stringResource(R.string.nav_impostazioni)) }
        },
        scrollBehavior = scroll,
        windowInsets = WindowInsets(0.dp),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

// ---- Home ----

/**
 * La Home: la prossima uscita (programmata più vicina, altrimenti l'ultima bozza) in una scheda piena,
 * l'anteprima del percorso, le azioni rapide e le altre uscite. Vuota, invita a crearne una.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboard(
    vm: TripsViewModel, routes: List<Route>, routeRepo: RouteRepository, weather: WeatherRepository,
    onNew: () -> Unit, onOpen: (String) -> Unit, onPack: (String) -> Unit, onWeather: (String) -> Unit, onRoute: (String) -> Unit,
    onAllTrips: () -> Unit, onMap: () -> Unit, onSettings: () -> Unit,
) {
    val trips by vm.trips.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val list = trips
    val next = remember(list) { list?.let { TripRules.next(it, today) } }
    val others = remember(list, next) { list?.let { TripRules.sorted(it, today).filter { t -> t.id != next?.id && t.status != TripStatus.ANNULLATA }.take(3) }.orEmpty() }
    val byId = remember(routes) { routes.associateBy { it.id } }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val expandedFab by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 40 } }
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            // Il titolo è il nome dell'app: "prossima uscita" o "bozza" lo dice già la scheda.
            TabTopBar(stringResource(R.string.app_name), onSettings, scroll) {
                IconButton(onClick = onMap) { Icon(AppIcons.Map, contentDescription = stringResource(R.string.home_mappa_percorsi)) }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.nuova_uscita)) }, icon = { Icon(AppIcons2.Plus, contentDescription = null) },
                onClick = onNew, expanded = expandedFab,
                containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary,
            )
        },
    ) { padding ->
        when {
            list == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            next == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(AppIcons.Mountain, stringResource(R.string.home_vuota_titolo), stringResource(R.string.home_vuota))
            }
            else -> LazyColumn(Modifier.padding(padding), state = listState, contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item(key = "prossima") {
                    NextTripCard(next, next.routeId?.let(byId::get), entries.filter { it.tripId == next.id }, weather, today) { onOpen(next.id) }
                }
                if (next.routeId != null) item(key = "anteprima") {
                    TripMap(routeRepo, next.routeId, Modifier.height(200.dp)) { onRoute(next.routeId) }
                }
                item(key = "azioni") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickAction(AppIcons.Backpack, stringResource(R.string.zaino), Modifier.weight(1f)) { onPack(next.id) }
                        QuickAction(AppIcons4.Cloud, stringResource(R.string.meteo), Modifier.weight(1f)) { onWeather(next.id) }
                        QuickAction(AppIcons2.Edit, stringResource(R.string.dettagli), Modifier.weight(1f)) { onOpen(next.id) }
                    }
                }
                if (others.isNotEmpty()) {
                    item(key = "altre") {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.altre_uscite), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
                            TextButton(onClick = onAllTrips) { Text(stringResource(R.string.vedi_tutte, list.size)) }
                        }
                    }
                    items(others, key = { it.id }) { trip -> TripRow(trip, trip.routeId?.let(byId::get), null, Modifier.animateItem()) { onOpen(trip.id) } }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick, modifier = modifier.heightIn(min = 52.dp), contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary),
        elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = 1.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(label, modifier = Modifier.padding(start = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** La prossima uscita in una scheda piena del colore principale: quando, dove, meteo, numeri del percorso e zaino. */
@Composable
private fun NextTripCard(trip: Trip, route: Route?, entries: List<ChecklistEntry>, weather: WeatherRepository, today: LocalDate, onClick: () -> Unit) {
    val locale = currentLocale()
    val on = MaterialTheme.colorScheme.onPrimary
    val day by produceState(initialValue = null as DayWeather?, trip.id, trip.startDate) {
        value = trip.startDate?.let { d -> weather.lastSaved(trip.id)?.points?.firstOrNull()?.day(d) }
    }
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).clickable(role = Role.Button, onClickLabel = stringResource(R.string.apri_uscita), onClick = onClick),
        shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primary, contentColor = on,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (trip.status == TripStatus.PROGRAMMATA) R.string.prossima_uscita else R.string.ultima_bozza), style = MaterialTheme.typography.labelLarge, color = on.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
                tripCountdown(trip, today)?.let { countdown ->
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(AppIcons2.Calendar, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(countdown, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            Text(trip.title, style = MaterialTheme.typography.headlineLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tripWhen(trip), style = MaterialTheme.typography.bodyMedium, color = on.copy(alpha = 0.85f))
                if (trip.placeName.isNotBlank()) Text(trip.placeName, style = MaterialTheme.typography.bodyMedium, color = on.copy(alpha = 0.85f))
            }
            day?.let { d ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = on.copy(alpha = 0.12f)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(skyIcon(d.code), contentDescription = null, modifier = Modifier.size(30.dp))
                        Column(Modifier.weight(1f)) {
                            Text(skyLabel(d.code), style = MaterialTheme.typography.titleSmall)
                            d.windKmh?.let { Text(stringResource(R.string.meteo_vento_breve, String.format(locale, "%.0f km/h", it)), style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.85f)) }
                        }
                        if (d.tempMin != null && d.tempMax != null) Text(formatTemp(d.tempMin) + " / " + formatTemp(d.tempMax), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
            if (route != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroStat(formatDistance(route.distanceMeters, locale), stringResource(R.string.stat_distanza), Modifier.weight(1f))
                HeroStat(route.ascentMeters?.let { formatMeters(it, locale) }, stringResource(R.string.stat_salita), Modifier.weight(1f))
                HeroStat(
                    estimatedMinutes(route.distanceMeters, route.ascentMeters, route.descentMeters, route.partialElevation, Pace.valueOf(trip.pace.name))?.let(::formatDuration),
                    stringResource(R.string.stat_stimata), Modifier.weight(1f),
                )
            }
            if (entries.isNotEmpty()) {
                val packed = ChecklistRules.packedCount(entries)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.zaino) + ": " + stringResource(R.string.pronti_di, packed, entries.size), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(weight(ChecklistRules.totals(entries, trip.waterLiters).packGrams), style = MaterialTheme.typography.titleMedium)
                    }
                    LinearProgressIndicator(
                        progress = { packed.toFloat() / entries.size }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.secondaryContainer, trackColor = on.copy(alpha = 0.2f), drawStopIndicator = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStat(value: String?, label: String, modifier: Modifier) {
    val on = MaterialTheme.colorScheme.onPrimary
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = on.copy(alpha = 0.12f)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).semantics(mergeDescendants = true) {}) {
            Text(value ?: "—", style = MaterialTheme.typography.headlineSmall.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize * 0.9f), maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.85f))
        }
    }
}

// ---- Le tue uscite ----

/** Una riga d'uscita: nome, quando, numeri del percorso e temperatura prevista; a destra lo stato. */
@Composable
fun TripRow(trip: Trip, route: Route?, temperature: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val locale = currentLocale()
    Surface(modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(role = Role.Button, onClick = onClick), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(Modifier.heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(trip.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val facts = listOfNotNull(
                    tripWhen(trip), route?.let { formatDistance(it.distanceMeters, locale) }, route?.ascentMeters?.let { "↑ " + formatMeters(it, locale) }, temperature,
                )
                Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusChip(trip.status)
        }
    }
}

/** "Le tue uscite": a sezioni (in programma, bozze, fatte, annullate); un tocco apre l'uscita. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(vm: TripsViewModel, routes: List<Route>, weather: WeatherRepository, onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val trips by vm.trips.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val byId = remember(routes) { routes.associateBy { it.id } }
    val sections = remember(trips) { trips?.let { TripRules.sections(it, today) } }
    // Le temperature si leggono tutte insieme, una volta: lette riga per riga arriverebbero durante lo scorrimento.
    val temperatures by produceState(emptyMap<String, String>(), trips) {
        value = trips.orEmpty().mapNotNull { trip ->
            val d = trip.startDate?.let { day -> weather.lastSaved(trip.id)?.points?.firstOrNull()?.day(day) } ?: return@mapNotNull null
            if (d.tempMin != null && d.tempMax != null) trip.id to formatTemp(d.tempMin) + " / " + formatTemp(d.tempMax) else null
        }.toMap()
    }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = { TabTopBar(stringResource(R.string.le_tue_uscite), onSettings, scroll) },
    ) { padding ->
        val s = sections
        when {
            s == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            s.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(AppIcons.Mountain, stringResource(R.string.uscite_vuote_titolo), stringResource(R.string.uscite_vuote))
            }
            else -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                s.forEach { (section, list) ->
                    item(key = "sezione-$section") {
                        Text(
                            stringResource(when (section) {
                                TripRules.Section.IN_PROGRAMMA -> R.string.sezione_in_programma
                                TripRules.Section.BOZZE -> R.string.sezione_bozze
                                TripRules.Section.FATTE -> R.string.sezione_fatte
                                TripRules.Section.ANNULLATE -> R.string.sezione_annullate
                            }) + " · " + list.size,
                            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp).animateItem().semantics { heading() },
                        )
                    }
                    items(list, key = { it.id }) { trip -> TripRow(trip, trip.routeId?.let(byId::get), temperatures[trip.id], Modifier.animateItem()) { onOpen(trip.id) } }
                }
            }
        }
    }
}
