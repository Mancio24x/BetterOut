package app.betterout.ui.gear

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import app.betterout.ui.components.ChoiceChips
import app.betterout.ui.components.EmptyState
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.theme.AppIcons3
import app.betterout.ui.home.TabTopBar
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.data.GearCategory
import app.betterout.data.GearItem
import app.betterout.domain.GearFunction
import app.betterout.ui.theme.AppIcons
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons5
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import app.betterout.domain.GearKind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import app.betterout.ui.trips.currentLocale

/** Filtra per nome o categoria, senza distinguere maiuscole. */
fun filterItems(items: List<GearItem>, categories: List<GearCategory>, query: String, categoryId: String?): List<GearItem> {
    val names = categories.associate { it.id to it.name }
    val q = query.trim().lowercase()
    return items.filter { (categoryId == null || it.categoryId == categoryId) && (q.isEmpty() || it.name.lowercase().contains(q) || names[it.categoryId].orEmpty().lowercase().contains(q)) }
}

/** Un oggetto proposto: simbolo, nome e uso già impostati; peso e volume li mette l'utente. */
class GearPreset(val key: String, val nameRes: Int, val icon: ImageVector, val function: GearFunction = GearFunction.ALTRO, val kind: GearKind = GearKind.BASE)

val GearPresets = listOf(
    GearPreset("ZAINO", R.string.modello_zaino, AppIcons.Backpack, GearFunction.ZAINO),
    GearPreset("SCARPE", R.string.modello_scarpe, AppIcons5.Boot, kind = GearKind.INDOSSATO),
    GearPreset("BORRACCIA", R.string.modello_borraccia, AppIcons5.Bottle, GearFunction.CONTENITORE_ACQUA),
    GearPreset("MAGLIETTA", R.string.modello_maglietta, AppIcons5.Shirt, GearFunction.MAGLIETTA, GearKind.INDOSSATO),
    GearPreset("CAMBI", R.string.modello_cambi, AppIcons5.Layers),
    GearPreset("CALZE", R.string.modello_calze, AppIcons5.Sock, GearFunction.CALZE),
    GearPreset("TORCIA", R.string.modello_torcia, AppIcons5.Torch, GearFunction.FRONTALE),
    GearPreset("CIBO", R.string.modello_cibo, AppIcons5.Food, kind = GearKind.CONSUMABILE),
    GearPreset("FORNELLO", R.string.modello_fornello, AppIcons5.Flame, GearFunction.FORNELLO),
    GearPreset("CAPPELLO", R.string.modello_cappello, AppIcons5.Hat, GearFunction.BERRETTO),
    GearPreset("OCCHIALI", R.string.modello_occhiali, AppIcons5.Glasses, GearFunction.OCCHIALI_SOLE),
    GearPreset("COLTELLO", R.string.modello_coltello, AppIcons5.Knife),
    GearPreset("GIACCA", R.string.modello_giacca, AppIcons5.Jacket, GearFunction.GUSCIO_PIOGGIA),
    GearPreset("PRIMO_SOCCORSO", R.string.modello_primo_soccorso, AppIcons5.FirstAid, GearFunction.PRIMO_SOCCORSO),
    GearPreset("SACCO_A_PELO", R.string.modello_sacco_a_pelo, AppIcons5.SleepingBag, GearFunction.SACCO_LETTO),
    GearPreset("TENDA", R.string.modello_tenda, AppIcons5.Tent, GearFunction.RIPARO),
    GearPreset("POWERBANK", R.string.modello_powerbank, AppIcons5.Battery, GearFunction.POWERBANK),
)

/**
 * L'oggetto proposto a cui corrisponde un oggetto dell'inventario: quello da cui è nato; per gli oggetti creati prima
 * che il collegamento fosse salvato, quello il cui nome compare nel nome dell'oggetto, o con lo stesso uso.
 */
fun presetIndexFor(item: GearItem, presetNames: List<String>): Int? =
    GearPresets.indexOfFirst { it.key == item.preset }.takeIf { it >= 0 }
        ?: presetNames.indexOfFirst { it.equals(item.name.trim(), ignoreCase = true) }.takeIf { it >= 0 }
        ?: presetNames.indexOfFirst { item.name.contains(it, ignoreCase = true) }.takeIf { it >= 0 }
        ?: GearPresets.indexOfFirst { it.function != GearFunction.ALTRO && it.function == item.function }.takeIf { it >= 0 }

/** Attrezzatura: prima gli oggetti già compilati, poi quelli proposti ancora da aggiungere. Ogni riga ha simbolo e nome. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun InventoryScreen(vm: InventoryViewModel, onNew: () -> Unit, onOpen: (String) -> Unit, onPreset: (Int) -> Unit, onSettings: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val names = GearPresets.map { stringResource(it.nameRes) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // Dopo un'aggiunta l'elenco torna in cima, dove compare l'oggetto nuovo.
    val listState = rememberLazyListState()
    val expandedFab by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 40 } }
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = { TabTopBar(stringResource(R.string.nav_attrezzatura), onSettings, scroll) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.nuovo_oggetto)) }, icon = { Icon(AppIcons2.Plus, contentDescription = null) },
                onClick = onNew, expanded = expandedFab,
                containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary,
            )
        },
    ) { padding ->
        if (state.loading) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else {
            val items = remember(state.items) { state.items.sortedBy { it.name.lowercase() } }
            val used = remember(items, names) { items.mapNotNull { presetIndexFor(it, names) }.toSet() }
            LaunchedEffect(items.size) { listState.scrollToItem(0) }
            LazyColumn(Modifier.padding(padding), state = listState, contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 104.dp)) {
                if (items.isNotEmpty()) {
                    item(key = "tuoi") {
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)) {
                            Text(stringResource(R.string.attrezzatura_tua), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).semantics { heading() })
                            Text(stringResource(R.string.inventario_riepilogo, pluralStringResource(R.plurals.conteggio_oggetti, items.size, items.size), weight(items.sumOf { (it.weightGrams ?: 0).toLong() })), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(items, key = { it.id }) { item ->
                        Box(Modifier.animateItem()) {
                            GearRow(presetIndexFor(item, names)?.let { GearPresets[it].icon } ?: AppIcons5.Box, item.name, gearSubtitle(item), item.weightGrams?.let { weight(it.toLong()) } ?: "—") { onOpen(item.id) }
                        }
                    }
                }
                val missing = GearPresets.indices.filter { it !in used }
                if (missing.isNotEmpty()) {
                    item(key = "proposti") { Text(stringResource(R.string.attrezzatura_da_aggiungere), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp, bottom = 2.dp).semantics { heading() }) }
                    items(missing, key = { "m$it" }) { i -> Box(Modifier.animateItem()) { GearRow(GearPresets[i].icon, names[i], stringResource(R.string.attrezzatura_tocca), null) { onPreset(i) } } }
                }
            }
        }
    }
}

@Composable
fun gearSubtitle(item: GearItem): String {
    val locale = currentLocale()
    return listOfNotNull(
        kindLabel(item.kind),
        functionLabel(item.function).takeIf { item.function != GearFunction.ALTRO },
        item.capacityLiters?.let { stringResource(R.string.capacita_litri, formatLiters(it, locale)) },
        stringResource(R.string.peso_stimato).takeIf { item.weightIsEstimate && item.weightGrams != null },
        stringResource(R.string.peso_mancante).takeIf { item.weightGrams == null },
    ).joinToString(" · ")
}

@Composable
private fun GearRow(icon: ImageVector, name: String, subtitle: String, weight: String?, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(22.dp)).clickable(role = Role.Button, onClick = onClick), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(Modifier.heightIn(min = 68.dp).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp)) } }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (weight != null) Text(weight, style = MaterialTheme.typography.titleSmall)
            Icon(if (weight != null) AppIcons2.Chevron else AppIcons2.Plus, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
