package app.betterout.ui.dive

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.betterout.BetterOutApplication
import app.betterout.R
import app.betterout.domain.DiveRules
import app.betterout.domain.DiveSite
import app.betterout.domain.Nitrox
import app.betterout.ui.components.AppCard
import app.betterout.ui.components.ChoiceChips
import app.betterout.ui.components.EmptyState
import app.betterout.ui.components.NavMotion
import app.betterout.ui.home.SettingsScreen
import app.betterout.ui.home.TabTopBar
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons5
import app.betterout.ui.trips.currentLocale
import app.betterout.ui.trips.formatDate
import kotlin.math.roundToInt

private enum class DiveTab(val route: String, @param:StringRes val label: Int, val icon: ImageVector) {
    Logbook("logbook", R.string.nav_logbook, AppIcons5.Mask),
    Siti("siti", R.string.nav_siti, AppIcons2.Pin),
    Strumenti("strumenti", R.string.nav_strumenti, AppIcons5.Gauge),
}

/** La modalità immersioni: logbook, siti e strumenti nella barra in basso, con il tema blu. */
@Composable
fun DiveApp(app: BetterOutApplication) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val showTabs = DiveTab.entries.any { it.route == entry?.destination?.route }
    val motion = remember { NavMotion(DiveTab.entries.map { it.route }.toSet()) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showTabs) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                DiveTab.entries.forEach { tab ->
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
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer, selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        // Le schermate di secondo livello gestiscono da sole barre e margini di sistema.
        NavHost(
            nav, startDestination = DiveTab.Logbook.route, modifier = if (showTabs) Modifier.padding(padding) else Modifier,
            enterTransition = motion.enter, exitTransition = motion.exit, popEnterTransition = motion.popEnter, popExitTransition = motion.popExit,
        ) {
            val settings = { nav.navigate("impostazioni") }
            composable(DiveTab.Logbook.route) {
                DiveLogScreen(
                    viewModel(factory = DiveLogViewModel.factory(app.dives)), onNew = { nav.navigate("immersione/nuova") }, onOpen = { nav.navigate("immersione/$it") },
                    onImport = { nav.navigate("importa?uri=${Uri.encode(it.toString())}") }, onSettings = settings,
                )
            }
            composable(DiveTab.Siti.route) {
                SitesScreen(viewModel(factory = DiveLogViewModel.factory(app.dives)), onOpen = { nav.navigate("sito/${Uri.encode(it)}") }, onSettings = settings)
            }
            composable(DiveTab.Strumenti.route) { NitroxScreen(onSettings = settings) }
            composable("sito/{nome}", listOf(navArgument("nome") { type = NavType.StringType })) { e ->
                SiteScreen(viewModel(factory = DiveLogViewModel.factory(app.dives)), e.arguments?.getString("nome").orEmpty(), onBack = { nav.popBackStack() }, onOpen = { nav.navigate("immersione/$it") })
            }
            composable("importa?uri={uri}", listOf(navArgument("uri") { type = NavType.StringType })) { e ->
                val uri = Uri.parse(e.arguments?.getString("uri").orEmpty())
                DiveImportScreen(viewModel(factory = DiveImportViewModel.factory(app, app.dives, uri)), onClose = { nav.popBackStack() }) { nav.popBackStack(DiveTab.Logbook.route, inclusive = false) }
            }
            composable("immersione/nuova") {
                DiveEditScreen(viewModel(factory = DiveEditViewModel.factory(app.dives, null)), onClose = { nav.popBackStack() }) { id ->
                    nav.navigate("immersione/$id") { popUpTo(DiveTab.Logbook.route) }
                }
            }
            composable("immersione/{id}", listOf(navArgument("id") { type = NavType.StringType })) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                DiveDetailScreen(viewModel(factory = DiveDetailViewModel.factory(app.dives, id)), onBack = { nav.popBackStack() }, onEdit = { nav.navigate("immersione/$id/modifica") })
            }
            composable("immersione/{id}/modifica", listOf(navArgument("id") { type = NavType.StringType })) { e ->
                val id = e.arguments?.getString("id").orEmpty()
                DiveEditScreen(viewModel(factory = DiveEditViewModel.factory(app.dives, id)), onClose = { nav.popBackStack() }) { nav.popBackStack() }
            }
            composable("impostazioni") { SettingsScreen(app.settings, onBack = { nav.popBackStack() }, onImportGpx = null, onRoutes = null) }
        }
    }
}

// ---- Siti ----

/** I siti del logbook, dal più recente: quante immersioni, la più profonda, l'ultima volta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitesScreen(vm: DiveLogViewModel, onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val dives by vm.dives.collectAsStateWithLifecycle()
    val sites = remember(dives) { dives?.let(DiveRules::sites) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = { TabTopBar(stringResource(R.string.nav_siti), onSettings, scroll) },
    ) { padding ->
        val list = sites
        when {
            list == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            list.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(AppIcons2.Pin, stringResource(R.string.siti_vuoti_titolo), stringResource(R.string.siti_vuoti_testo))
            }
            else -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.name.lowercase() }) { site -> SiteRow(site, Modifier.animateItem()) { onOpen(site.name) } }
            }
        }
    }
}

@Composable
private fun SiteRow(site: DiveSite, modifier: Modifier, onClick: () -> Unit) {
    val locale = currentLocale()
    Surface(modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(role = Role.Button, onClick = onClick), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(Modifier.heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(site.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val facts = listOfNotNull(
                    pluralStringResource(R.plurals.immersioni_conteggio, site.count, site.count),
                    site.deepestMeters?.let { stringResource(R.string.sito_max, meters(it, locale)) },
                    stringResource(R.string.sito_ultima, formatDate(site.last.date, locale, withYear = true)),
                )
                Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (site.latitude != null) Icon(AppIcons2.Pin, contentDescription = stringResource(R.string.sito_con_posizione), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Le immersioni di un sito, dalla più recente. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteScreen(vm: DiveLogViewModel, name: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val dives by vm.dives.collectAsStateWithLifecycle()
    val site = remember(dives, name) { dives?.let { DiveRules.sites(it).firstOrNull { s -> s.name.equals(name.trim(), ignoreCase = true) } } }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val locale = currentLocale()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(site?.name ?: name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons2.Back, contentDescription = stringResource(R.string.indietro)) } },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
    ) { padding ->
        val s = site
        if (s == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "riepilogo") {
                val facts = listOfNotNull(
                    pluralStringResource(R.plurals.immersioni_conteggio, s.count, s.count),
                    s.deepestMeters?.let { stringResource(R.string.sito_max, meters(it, locale)) },
                    if (s.latitude != null && s.longitude != null) String.format(locale, "%.4f, %.4f", s.latitude, s.longitude) else null,
                )
                Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(s.dives, key = { it.id }) { DiveRow(it) { onOpen(it.id) } }
        }
    }
}

// ---- Strumenti: nitrox ----

/**
 * Calcolo nitrox: profondità massima operativa di una miscela, miscela migliore per una profondità,
 * profondità equivalente in aria e pressione parziale. Con l'avvertenza che è un aiuto, non un sostituto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NitroxScreen(onSettings: () -> Unit) {
    val locale = currentLocale()
    var oxygen by rememberSaveable { mutableIntStateOf(32) }
    var limit by rememberSaveable { mutableDoubleStateOf(Nitrox.PPO2_LAVORO) }
    var depth by rememberSaveable { mutableFloatStateOf(30f) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    fun one(v: Double) = String.format(locale, "%.1f", v)
    fun pp(v: Double) = String.format(locale, "%.2f", v)
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = { TabTopBar(stringResource(R.string.nitrox_titolo), onSettings, scroll) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.nitrox_avvertenza), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.nitrox_limite), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    ChoiceChips(
                        listOf(1.2 to one(1.2), 1.3 to one(1.3), Nitrox.PPO2_LAVORO to stringResource(R.string.nitrox_lavoro), Nitrox.PPO2_LIMITE to stringResource(R.string.nitrox_emergenza)),
                        limit, { limit = it },
                    )
                }
            }

            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.nitrox_miscela), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    ChoiceChips(listOf(21, 28, 32, 36, 40).map { it to gasName(it) }, oxygen, { oxygen = it })
                    Text(stringResource(R.string.nitrox_ossigeno, oxygen), style = MaterialTheme.typography.bodyMedium)
                    Slider(value = oxygen.toFloat(), onValueChange = { oxygen = it.roundToInt() }, valueRange = 21f..60f)
                    ResultLine(stringResource(R.string.nitrox_mod), meters(Nitrox.mod(oxygen, limit), locale), big = true)
                    // Per confronto il limite massimo (1,6); scelto quello, il valore di fondo (1,4).
                    val other = if (limit < Nitrox.PPO2_LIMITE) Nitrox.PPO2_LIMITE else Nitrox.PPO2_LAVORO
                    ResultLine(stringResource(R.string.nitrox_mod_altro, one(other)), meters(Nitrox.mod(oxygen, other), locale))
                }
            }

            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.nitrox_profondita), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    Text(meters(depth.roundToInt().toDouble(), locale), style = MaterialTheme.typography.bodyMedium)
                    Slider(value = depth, onValueChange = { depth = it }, valueRange = 5f..60f)
                    val d = depth.roundToInt().toDouble()
                    ResultLine(stringResource(R.string.nitrox_migliore), gasName(Nitrox.bestMix(d, limit)), big = true)
                    ResultLine(stringResource(R.string.nitrox_pp, gasName(oxygen)), pp(Nitrox.ppO2(d, oxygen)) + " bar", warn = Nitrox.ppO2(d, oxygen) > limit + 1e-9)
                    ResultLine(stringResource(R.string.nitrox_ead, gasName(oxygen)), meters((Nitrox.ead(d, oxygen) * 10).roundToInt() / 10.0, locale))
                }
            }
        }
    }
}

@Composable
private fun ResultLine(label: String, value: String, big: Boolean = false, warn: Boolean = false) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            value + if (warn) " · " + stringResource(R.string.nitrox_oltre) else "",
            style = if (big) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            color = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}
