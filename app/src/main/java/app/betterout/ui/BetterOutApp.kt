package app.betterout.ui

import androidx.annotation.StringRes
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import app.betterout.BetterOutApplication
import app.betterout.R
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
                HomeScreen(viewModel(factory = TripsViewModel.factory(app.trips)), onNew = { nav.navigate("uscita/nuova") }, onOpen = { nav.navigate("uscita/$it") }, onSeeAll = { nav.navigate("uscite/elenco") })
            }
            composable("uscite/elenco") {
                TripListScreen(viewModel(factory = TripsViewModel.factory(app.trips)), onBack = { nav.popBackStack() }, onOpen = { nav.navigate("uscita/$it") })
            }
            composable("uscita/nuova") {
                TripEditScreen(viewModel(factory = TripEditViewModel.factory(app.trips, null)), onClose = { nav.popBackStack() }, onSaved = { id -> nav.navigate("uscita/$id") { popUpTo("uscita/nuova") { inclusive = true } } })
            }
            composable("uscita/{id}") { e ->
                val id = e.arguments?.getString("id").orEmpty()
                TripDetailScreen(viewModel(factory = TripDetailViewModel.factory(app.trips, id)), onBack = { nav.popBackStack() }, onEdit = { nav.navigate("uscita/$id/modifica") }, onOpenCopy = { nav.navigate("uscita/$it") })
            }
            composable("uscita/{id}/modifica") { e ->
                val id = e.arguments?.getString("id")
                TripEditScreen(viewModel(factory = TripEditViewModel.factory(app.trips, id)), onClose = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            Tab.entries.drop(1).forEach { tab ->
                composable(tab.route) { Segnaposto(stringResource(tab.label), stringResource(R.string.in_costruzione)) }
            }
        }
    }
}

/** Schermata provvisoria: verrà sostituita dalle schermate vere nelle fasi successive. */
@Composable
private fun Segnaposto(titolo: String, testo: String) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(titolo, style = MaterialTheme.typography.displayLarge)
        Text(testo, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
