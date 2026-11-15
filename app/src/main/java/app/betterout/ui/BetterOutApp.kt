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
import app.betterout.ui.trips.HomeScreen
import app.betterout.ui.trips.TripDetailScreen
import app.betterout.ui.trips.TripDetailViewModel
import app.betterout.ui.trips.TripEditScreen
import app.betterout.ui.trips.TripEditViewModel
import app.betterout.ui.trips.TripListScreen
import app.betterout.ui.trips.TripsViewModel
import app.betterout.ui.theme.AppIcons

enum class Tab(val route: String, @param:StringRes val label: Int, val icon: ImageVector) {
    Uscite("uscite", R.string.nav_uscite, AppIcons.Mountain),
    Mappa("mappa", R.string.nav_mappa, AppIcons.Map),
    Attrezzatura("attrezzatura", R.string.nav_attrezzatura, AppIcons.Backpack),
    Impostazioni("impostazioni", R.string.nav_impostazioni, AppIcons.Settings),
}

@Composable
fun BetterOutApp() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val app = LocalContext.current.applicationContext as BetterOutApplication
    val route = entry?.destination?.route
    val showTabs = Tab.entries.any { it.route == route }
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
        NavHost(nav, startDestination = Tab.Uscite.route, modifier = if (showTabs) Modifier.padding(padding) else Modifier) {
            composable(Tab.Uscite.route) {
                HomeScreen(viewModel(factory = TripsViewModel.factory(app.trips, app.gear)), onNew = { nav.navigate("uscita/nuova") }, onOpen = { nav.navigate("uscita/$it") }, onSeeAll = { nav.navigate("uscite/elenco") })
            }
            composable("uscite/elenco") {
                TripListScreen(viewModel(factory = TripsViewModel.factory(app.trips, app.gear)), onBack = { nav.popBackStack() }, onOpen = { nav.navigate("uscita/$it") })
            }
            composable("uscita/nuova") {
                TripEditScreen(viewModel(factory = TripEditViewModel.factory(app.trips, null)), onClose = { nav.popBackStack() }, onSaved = { id -> nav.navigate("uscita/$id") { popUpTo("uscita/nuova") { inclusive = true } } })
            }
            composable("uscita/{id}") { e ->
                val id = e.arguments?.getString("id").orEmpty()
                TripDetailScreen(viewModel(factory = TripDetailViewModel.factory(app.trips, id)), viewModel(factory = ChecklistViewModel.factory(app.gear, app.trips, id)), onPack = { nav.navigate("uscita/$id/zaino") }, onBack = { nav.popBackStack() }, onEdit = { nav.navigate("uscita/$id/modifica") }, onOpenCopy = { nav.navigate("uscita/$it") }) { trip ->
                    TripRouteCard(trip, app.routes, onOpenRoute = { nav.navigate("percorso/$it") }, onChoose = { nav.navigate("uscita/$id/percorso") }, onRemove = { scope.launch { app.routes.setTripRoute(id, null) } })
                }
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
                InventoryScreen(viewModel(factory = InventoryViewModel.factory(app.gear)), onNew = { nav.navigate("oggetto/nuovo") }, onOpen = { nav.navigate("oggetto/$it") })
            }
            composable("oggetto/nuovo") { GearEditScreen(viewModel(factory = GearEditViewModel.factory(app.gear, null)), onClose = { nav.popBackStack() }) }
            composable("oggetto/{id}") { e -> GearEditScreen(viewModel(factory = GearEditViewModel.factory(app.gear, e.arguments?.getString("id"))), onClose = { nav.popBackStack() }) }
            composable("uscita/{id}/zaino") { e ->
                val id = e.arguments?.getString("id").orEmpty()
                ChecklistScreen(viewModel(factory = ChecklistViewModel.factory(app.gear, app.trips, id)), onBack = { nav.popBackStack() }, onAdd = { nav.navigate("uscita/$id/zaino/aggiungi") })
            }
            composable("uscita/{id}/zaino/aggiungi") { e ->
                val id = e.arguments?.getString("id").orEmpty()
                AddToChecklistScreen(viewModel(factory = ChecklistViewModel.factory(app.gear, app.trips, id)), onClose = { nav.popBackStack() })
            }
            composable(Tab.Mappa.route) {
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
                        tripId != null -> nav.popBackStack("uscita/{id}", inclusive = false)
                        id != null -> nav.popBackStack()
                        else -> nav.navigate("percorso/$saved") { popUpTo(Tab.Mappa.route) }
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
                            linked -> nav.popBackStack("uscita/{id}", inclusive = false)
                            tripId == null && ids.size == 1 -> nav.navigate("percorso/${ids.single()}") { popUpTo(Tab.Mappa.route) }
                            else -> nav.popBackStack()
                        }
                    },
                )
            }
            composable(Tab.Impostazioni.route) { Segnaposto(stringResource(Tab.Impostazioni.label), stringResource(R.string.in_costruzione)) }
        }
    }
}

private fun optional(name: String) = navArgument(name) { type = NavType.StringType; nullable = true }

/** Schermata provvisoria: verrà sostituita dalle schermate vere nelle fasi successive. */
@Composable
private fun Segnaposto(titolo: String, testo: String) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(titolo, style = MaterialTheme.typography.displayLarge)
        Text(testo, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
