package app.betterout.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import app.betterout.BetterOutApplication
import app.betterout.R
import app.betterout.ui.gear.AddToChecklistScreen
import app.betterout.ui.gear.ChecklistScreen
import app.betterout.ui.gear.ChecklistViewModel
import app.betterout.ui.gear.GearEditScreen
import app.betterout.ui.gear.GearEditViewModel
import app.betterout.ui.gear.InventoryScreen
import app.betterout.ui.gear.InventoryViewModel
import app.betterout.ui.map.GpxImportScreen
import app.betterout.ui.map.GpxImportViewModel
import app.betterout.ui.map.MapScreen
import app.betterout.ui.map.RouteDetailScreen
import app.betterout.ui.map.RouteDetailViewModel
import app.betterout.ui.map.RouteEditorScreen
import app.betterout.ui.map.RouteEditorViewModel
import app.betterout.ui.map.RoutesViewModel
import app.betterout.ui.map.TripRouteCard
import app.betterout.ui.map.TripRouteScreen
import app.betterout.ui.weather.TripWeatherCard
import app.betterout.ui.weather.WeatherScreen
import app.betterout.ui.weather.WeatherViewModel
import app.betterout.ui.trips.TripDetailScreen
import app.betterout.ui.trips.TripDetailViewModel
import app.betterout.ui.trips.TripEditScreen
import app.betterout.ui.trips.TripEditViewModel
import app.betterout.ui.trips.TripsViewModel
import app.betterout.ui.theme.AppIcons
import app.betterout.ui.theme.AppIcons3
import app.betterout.ui.theme.AppIcons5
import app.betterout.data.GearItem
import app.betterout.domain.WeightUnit
import app.betterout.data.Trip
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.gear.GearPresets
import app.betterout.ui.gear.shareTextFile
import app.betterout.ui.home.HomeScreen
import app.betterout.ui.home.SettingsScreen
import app.betterout.ui.home.TabHeader
import app.betterout.ui.home.TripMap
import app.betterout.ui.home.TripsScreen
import app.betterout.ui.map.gpxFileName
import androidx.compose.foundation.layout.height
import androidx.lifecycle.compose.collectAsStateWithLifecycle

enum class Tab(val route: String, @param:StringRes val label: Int, val icon: ImageVector) {
    Uscite("uscite", R.string.nav_uscite, AppIcons.Mountain),
    Home("home", R.string.nav_home, AppIcons5.Compass),
    Attrezzatura("attrezzatura", R.string.nav_attrezzatura, AppIcons.Backpack),
}

private const val MAPPA = "mappa"

@Composable
fun BetterOutApp() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val app = LocalContext.current.applicationContext as BetterOutApplication
    val route = entry?.destination?.route
    val showTabs = route == MAPPA || Tab.entries.any { it.route == route }
    val scope = rememberCoroutineScope()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showTabs) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = entry?.destination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        // Le schermate di secondo livello gestiscono da sole barre e margini di sistema.
        NavHost(nav, startDestination = Tab.Home.route, modifier = if (showTabs) Modifier.padding(padding) else Modifier) {
            val settings = { nav.navigate("impostazioni") }
            val inKg = { app.settings.weightUnit.value == WeightUnit.KG }
            val setUnit = { kg: Boolean -> app.settings.setWeightUnit(if (kg) WeightUnit.KG else WeightUnit.G) }
            // Il percorso di un'uscita si può aggiungere dal suo dettaglio o dalla Home: si torna da dove si era partiti.
            val backToTrip = { if (!nav.popBackStack("uscita/{id}", inclusive = false)) nav.popBackStack(Tab.Home.route, inclusive = false) }
            // Mappa, percorso e meteo di un'uscita: gli stessi pezzi in Home, nel dettaglio e nelle pillole aperte.
            val tripDetails: @Composable (Trip, Boolean) -> Unit = { trip, withActions ->
                TripMap(app.routes, trip.routeId, Modifier.height(260.dp))
                if (withActions) TripRouteCard(trip, app.routes, onOpenRoute = { nav.navigate("percorso/$it") }, onChoose = { nav.navigate("uscita/${trip.id}/percorso") }, onRemove = { scope.launch { app.routes.setTripRoute(trip.id, null) } })
                TripWeatherCard(viewModel(key = "meteo-${trip.id}", factory = WeatherViewModel.factory(app.trips, app.routes, app.weather, trip.id)), onOpen = { nav.navigate("uscita/${trip.id}/meteo") })
            }
            composable(Tab.Home.route) {
                HomeScreen(viewModel(factory = TripsViewModel.factory(app.trips, app.gear)), onNew = { nav.navigate("uscita/nuova") }, onOpen = { nav.navigate("uscita/$it") }, onPack = { nav.navigate("uscita/$it/zaino") }, onMap = { nav.navigate(MAPPA) }, onSettings = settings) { trip -> tripDetails(trip, true) }
            }
            composable(Tab.Uscite.route) {
                val routes by viewModel<RoutesViewModel>(factory = RoutesViewModel.factory(app.routes)).routes.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val manualNote = stringResource(R.string.gpx_nota_manuale)
                val chooser = stringResource(R.string.condividi_con)
                TripsScreen(viewModel(factory = TripsViewModel.factory(app.trips, app.gear)), routes.orEmpty(), app.weather, onSettings = settings) { trip ->
                    tripDetails(trip, false)
                    val route = routes?.firstOrNull { it.id == trip.routeId }
                    if (route != null) SecondaryButton(stringResource(R.string.esporta_gpx), { scope.launch { app.routes.gpx(route.id, manualNote)?.let { shareTextFile(context, gpxFileName(route.name), "application/gpx+xml", it, chooser, byteOrderMark = false) } } }, icon = AppIcons3.Download)
                    SecondaryButton(stringResource(R.string.pillola_zaino), { nav.navigate("uscita/${trip.id}/zaino") }, icon = AppIcons.Backpack)
                    PrimaryButton(stringResource(R.string.apri_uscita), { nav.navigate("uscita/${trip.id}") })
                }
            }
            composable("uscita/nuova") {
                TripEditScreen(viewModel(factory = TripEditViewModel.factory(app.trips, null)), onClose = { nav.popBackStack() }, onSaved = { nav.popBackStack(Tab.Home.route, inclusive = false) })
            }
            composable("uscita/{id}") { e ->
                val id = e.arguments?.getString("id").orEmpty()
                TripDetailScreen(viewModel(factory = TripDetailViewModel.factory(app.trips, id)), viewModel(factory = ChecklistViewModel.factory(app.gear, app.trips, id)), onPack = { nav.navigate("uscita/$id/zaino") }, onBack = { nav.popBackStack() }, onEdit = { nav.navigate("uscita/$id/modifica") }, onOpenCopy = { nav.navigate("uscita/$it") }) { trip ->
                    tripDetails(trip, true)
                }
            }
            composable("uscita/{id}/meteo") { e ->
                val id = e.arguments?.getString("id").orEmpty()
                WeatherScreen(viewModel(factory = WeatherViewModel.factory(app.trips, app.routes, app.weather, id)), onBack = { nav.popBackStack() })
            }
            composable("uscita/{id}/percorso") { e ->
                val id = e.arguments?.getString("id").orEmpty()
                val vm: RoutesViewModel = viewModel(factory = RoutesViewModel.factory(app.routes))
                TripRouteScreen(vm, onBack = { nav.popBackStack() }, onDraw = { nav.navigate("editor?uscita=$id") }, onImport = { nav.navigate("gpx?uri=${Uri.encode(it.toString())}&uscita=$id") }, onPick = { vm.assignToTrip(id, it) { nav.popBackStack() } })
            }
            composable("uscita/{id}/modifica") { e ->
                val id = e.arguments?.getString("id")
                TripEditScreen(viewModel(factory = TripEditViewModel.factory(app.trips, id)), onClose = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(Tab.Attrezzatura.route) {
                InventoryScreen(viewModel(factory = InventoryViewModel.factory(app.gear)), onNew = { nav.navigate("oggetto/nuovo") }, onOpen = { nav.navigate("oggetto/$it") }, onPreset = { nav.navigate("oggetto/nuovo?modello=$it") }) { TabHeader(stringResource(R.string.nav_attrezzatura), settings) }
            }
            composable("oggetto/nuovo?modello={modello}", arguments = listOf(optional("modello"))) { e ->
                // Un oggetto scelto dall'elenco proposto parte con nome, uso e tipo già impostati.
                val preset = e.arguments?.getString("modello")?.toIntOrNull()?.let(GearPresets::getOrNull)
                val template = preset?.let { GearItem(id = "", name = stringResource(it.nameRes), categoryId = "", kind = it.kind, function = it.function, preset = it.key, createdAt = 0, updatedAt = 0) }
                GearEditScreen(viewModel(factory = GearEditViewModel.factory(app.gear, null, template, inKg())), onClose = { nav.popBackStack() }, onUnit = setUnit)
            }
            composable("oggetto/{id}") { e -> GearEditScreen(viewModel(factory = GearEditViewModel.factory(app.gear, e.arguments?.getString("id"), kg = inKg())), onClose = { nav.popBackStack() }, onUnit = setUnit) }
            composable("uscita/{id}/zaino") { e ->
                val id = e.arguments?.getString("id").orEmpty()
                ChecklistScreen(viewModel(factory = ChecklistViewModel.factory(app.gear, app.trips, id)), onBack = { nav.popBackStack() }, onAdd = { nav.navigate("uscita/$id/zaino/aggiungi") })
            }
            composable("uscita/{id}/zaino/aggiungi") { e ->
                val id = e.arguments?.getString("id").orEmpty()
                AddToChecklistScreen(viewModel(factory = ChecklistViewModel.factory(app.gear, app.trips, id)), onClose = { nav.popBackStack() })
            }
            composable(MAPPA) {
                MapScreen(viewModel(factory = RoutesViewModel.factory(app.routes)), onOpenRoute = { nav.navigate("percorso/$it") }, onDraw = { nav.navigate("editor") }, onImport = { nav.navigate("gpx?uri=${Uri.encode(it.toString())}") })
            }
            composable("percorso/{id}") { e ->
                val id = e.arguments?.getString("id").orEmpty()
                RouteDetailScreen(viewModel(factory = RouteDetailViewModel.factory(app.routes, id)), onBack = { nav.popBackStack() }, onEdit = { nav.navigate("editor?id=$id") })
            }
            // L'editor e l'importazione possono partire da un'uscita ("uscita"): in quel caso il percorso le viene associato.
            composable("editor?id={id}&uscita={uscita}", arguments = listOf(optional("id"), optional("uscita"))) { e ->
                val id = e.arguments?.getString("id")
                val tripId = e.arguments?.getString("uscita")
                RouteEditorScreen(viewModel(factory = RouteEditorViewModel.factory(app.routes, app.routing, id, tripId)), onClose = { nav.popBackStack() }, onSaved = { saved ->
                    when {
                        tripId != null -> backToTrip()
                        id != null -> nav.popBackStack()
                        else -> nav.navigate("percorso/$saved") { popUpTo(MAPPA) }
                    }
                })
            }
            composable("gpx?uri={uri}&uscita={uscita}", arguments = listOf(optional("uri"), optional("uscita"))) { e ->
                val uri = e.arguments?.getString("uri").orEmpty().toUri()
                val tripId = e.arguments?.getString("uscita")
                GpxImportScreen(
                    viewModel(factory = GpxImportViewModel.factory(app, app.routes, uri, tripId)), onBack = { nav.popBackStack() },
                    onAnotherFile = { nav.popBackStack(); nav.navigate("gpx?uri=${Uri.encode(it.toString())}" + if (tripId != null) "&uscita=$tripId" else "") },
                    onDone = { ids, linked ->
                        when {
                            linked -> backToTrip()
                            tripId == null && ids.size == 1 -> nav.navigate("percorso/${ids.single()}") { popUpTo(MAPPA) }
                            else -> nav.popBackStack()
                        }
                    },
                )
            }
            composable("impostazioni") {
                SettingsScreen(app.settings, onBack = { nav.popBackStack() }, onImportGpx = { nav.navigate("gpx?uri=${Uri.encode(it.toString())}") }, onRoutes = { nav.navigate(MAPPA) })
            }
        }
    }
}

private fun optional(name: String) = navArgument(name) { type = NavType.StringType; nullable = true }
