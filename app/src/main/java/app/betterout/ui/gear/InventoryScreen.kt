package app.betterout.ui.gear

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import app.betterout.ui.components.AddFab
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import app.betterout.ui.components.ChoiceChips
import app.betterout.ui.components.EmptyState
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.theme.AppIcons
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons3
import app.betterout.ui.trips.currentLocale

/** Filtra per nome o categoria, senza distinguere maiuscole. */
fun filterItems(items: List<GearItem>, categories: List<GearCategory>, query: String, categoryId: String?): List<GearItem> {
    val names = categories.associate { it.id to it.name }
    val q = query.trim().lowercase()
    return items.filter { (categoryId == null || it.categoryId == categoryId) && (q.isEmpty() || it.name.lowercase().contains(q) || names[it.categoryId].orEmpty().lowercase().contains(q)) }
}

@Composable
fun InventoryScreen(vm: InventoryViewModel, onNew: () -> Unit, onOpen: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(Modifier.padding(top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.nav_attrezzatura), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).semantics { heading() })
                if (state.items.isNotEmpty()) Text(stringResource(R.string.inventario_riepilogo, pluralStringResource(R.plurals.conteggio_oggetti, state.items.size, state.items.size), weight(state.items.sumOf { (it.weightGrams ?: 0).toLong() })), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.items.isEmpty() -> {
                    Spacer(Modifier.weight(1f))
                    EmptyState(AppIcons.Backpack, stringResource(R.string.inventario_vuoto_titolo), stringResource(R.string.inventario_vuoto_testo)) { PrimaryButton(stringResource(R.string.nuovo_oggetto), onNew, icon = AppIcons2.Plus) }
                    Spacer(Modifier.weight(1.4f))
                }
                else -> {
                    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, shape = CircleShape, placeholder = { Text(stringResource(R.string.inventario_cerca)) }, leadingIcon = { Icon(AppIcons3.Search, contentDescription = null) })
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
                        ChoiceChips(listOf<Pair<String?, String>>(null to stringResource(R.string.filtro_tutte)) + state.categories.filter { c -> state.items.any { it.categoryId == c.id } }.map { it.id to it.name }, categoryId, { categoryId = it })
                    }
                    val shown = filterItems(state.items, state.categories, query, categoryId)
                    if (shown.isEmpty()) Text(stringResource(R.string.inventario_nessun_risultato), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp))
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                        state.categories.forEach { category ->
                            val inCategory = shown.filter { it.categoryId == category.id }.sortedBy { it.name.lowercase() }
                            if (inCategory.isNotEmpty()) {
                                item(key = "h" + category.id) {
                                    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp)) {
                                        Text(category.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).semantics { heading() })
                                        Text(pluralStringResource(R.plurals.conteggio_oggetti, inCategory.size, inCategory.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                items(inCategory, key = { it.id }) { GearRow(it) { onOpen(it.id) } }
                            }
                        }
                    }
                }
            }
        }
        if (state.items.isNotEmpty()) AddFab(stringResource(R.string.nuovo_oggetto), onNew, Modifier.align(Alignment.BottomEnd).padding(16.dp))
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
private fun GearRow(item: GearItem, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(18.dp)).clickable(role = Role.Button, onClick = onClick), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(Modifier.heightIn(min = 60.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(gearSubtitle(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(item.weightGrams?.let { weight(it.toLong()) } ?: "—", style = MaterialTheme.typography.titleSmall)
            Icon(AppIcons2.Chevron, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
