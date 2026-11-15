package app.betterout.ui.gear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.data.ChecklistEntry
import app.betterout.domain.ChecklistRules
import app.betterout.domain.GearKind
import app.betterout.domain.PackTotals
import app.betterout.ui.components.AppCard
import app.betterout.ui.components.ChoiceChips
import app.betterout.ui.components.EmptyState
import app.betterout.ui.components.FieldLabel
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.theme.AppIcons
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons3
import app.betterout.ui.trips.currentLocale

/** Avanzamento e pesi dello zaino: usato nella checklist e nel dettaglio dell'uscita. */
@Composable
fun PackSummary(entries: List<ChecklistEntry>, totals: PackTotals, modifier: Modifier = Modifier) {
    val packed = ChecklistRules.packedCount(entries)
    val done = entries.isNotEmpty() && packed == entries.size
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (done) Icon(AppIcons2.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(if (done) R.string.tutto_pronto else R.string.pronti_di, packed, entries.size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
            Text(stringResource(R.string.nello_zaino, weight(totals.packGrams)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(progress = { if (entries.isEmpty()) 0f else packed.toFloat() / entries.size }, modifier = Modifier.fillMaxWidth().heightIn(min = 10.dp).clip(CircleShape), trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {})
        // Due righe da due: le parole restano intere anche con il testo ingrandito.
        listOf(R.string.peso_base to totals.baseGrams, R.string.peso_consumabili to totals.consumableWithWaterGrams, R.string.peso_indossato to totals.wornGrams, R.string.peso_totale to totals.totalGrams).chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (label, grams) ->
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text(weight(grams), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }
        if (totals.isPartial) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(AppIcons2.Warn, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(pluralStringResource(R.plurals.peso_parziale, totals.linesWithoutWeight, totals.linesWithoutWeight), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(vm: ChecklistViewModel, onBack: () -> Unit, onAdd: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = currentLocale()
    var menu by remember { mutableStateOf(false) }
    var askClear by remember { mutableStateOf(false) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    val entries = state.entries
    val totals = ChecklistRules.totals(entries, state.trip?.waterLiters ?: 0.0)
    val kindNames = GearKind.entries.associateWith { kindLabel(it) }
    val chooser = stringResource(R.string.condividi_con)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.zaino_di, state.trip?.title.orEmpty()), style = MaterialTheme.typography.titleLarge, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons2.Back, contentDescription = stringResource(R.string.indietro)) } },
                actions = {
                    IconButton(onClick = onAdd) { Icon(AppIcons2.Plus, contentDescription = stringResource(R.string.aggiungi_dall_inventario)) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(AppIcons2.More, contentDescription = stringResource(R.string.altre_azioni)) }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.aggiungi_dall_inventario)) }, leadingIcon = { Icon(AppIcons2.Plus, null) }, onClick = { menu = false; onAdd() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.azzera_spunte)) }, leadingIcon = { Icon(AppIcons3.Undo, null) }, enabled = entries.any { it.packed }, onClick = { menu = false; askClear = true })
                            DropdownMenuItem(text = { Text(stringResource(R.string.esporta_csv)) }, leadingIcon = { Icon(AppIcons3.Download, null) }, enabled = entries.isNotEmpty(), onClick = {
                                menu = false
                                shareTextFile(context, "zaino.csv", "text/csv", ChecklistRules.checklistCsv(entries) { kindNames.getValue(it) }, chooser)
                            })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            entries.isEmpty() -> Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
                Spacer(Modifier.weight(1f))
                EmptyState(AppIcons.Backpack, stringResource(R.string.zaino_vuoto_titolo), stringResource(if (state.items.isEmpty()) R.string.zaino_inventario_vuoto else R.string.zaino_vuoto_testo)) {
                    if (state.items.isNotEmpty()) PrimaryButton(stringResource(R.string.aggiungi_dall_inventario), onAdd, icon = AppIcons2.Plus)
                }
                Spacer(Modifier.weight(1.4f))
            }
            else -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
                item { AppCard(Modifier.fillMaxWidth()) { PackSummary(entries, totals, Modifier.padding(16.dp)) } }
                item {
                    val liters = state.trip?.waterLiters ?: 0.0
                    AppCard(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        Row(Modifier.padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons3.Drop, contentDescription = null)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(stringResource(R.string.acqua), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.acqua_dettaglio, formatLiters(liters, locale), weight(totals.waterGrams)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedIconButton(onClick = { vm.changeWater(-0.5) }, modifier = Modifier.size(48.dp), enabled = liters > 0) { Icon(AppIcons2.Minus, contentDescription = stringResource(R.string.meno_acqua)) }
                            Spacer(Modifier.size(6.dp))
                            OutlinedIconButton(onClick = { vm.changeWater(0.5) }, modifier = Modifier.size(48.dp)) { Icon(AppIcons2.Plus, contentDescription = stringResource(R.string.piu_acqua)) }
                        }
                    }
                }
                ChecklistRules.groups(entries).forEach { group ->
                    item(key = "h" + group.category) {
                        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 2.dp)) {
                            Text(group.category, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).semantics { heading() })
                            Text(stringResource(R.string.categoria_riepilogo, group.packed, group.entries.size, weight(group.grams)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(group.entries, key = { it.id }) { e -> EntryRow(e, onToggle = { vm.setPacked(e, it) }, onOpen = { openId = e.id }) }
                }
            }
        }
    }
    entries.firstOrNull { it.id == openId }?.let { e -> EntrySheet(e, onDismiss = { openId = null }, onChange = vm::update, onRemove = { openId = null; vm.remove(e) }) }
    if (askClear) AlertDialog(
        onDismissRequest = { askClear = false }, icon = { Icon(AppIcons3.Undo, contentDescription = null) },
        title = { Text(pluralStringResource(R.plurals.azzera_titolo, ChecklistRules.packedCount(entries), ChecklistRules.packedCount(entries)), style = MaterialTheme.typography.headlineSmall) },
        text = { Text(stringResource(R.string.azzera_testo), style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { PrimaryButton(stringResource(R.string.azzera_spunte), { askClear = false; vm.clearPacked() }); SecondaryButton(stringResource(R.string.annulla), { askClear = false }) } },
    )
}

@Composable
private fun EntryRow(e: ChecklistEntry, onToggle: (Boolean) -> Unit, onOpen: () -> Unit) {
    val label = stringResource(if (e.packed) R.string.pronto_si else R.string.pronto_no, e.name)
    Surface(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).heightIn(min = 60.dp).toggleable(value = e.packed, role = Role.Checkbox, onValueChange = onToggle).semantics { contentDescription = label }.padding(start = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = e.packed, onCheckedChange = null, modifier = Modifier.padding(horizontal = 10.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = MaterialTheme.typography.titleMedium, textDecoration = if (e.packed) TextDecoration.LineThrough else null, color = if (e.packed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    val details = listOfNotNull(
                        kindLabel(e.kind).takeIf { e.kind != GearKind.BASE }, stringResource(R.string.peso_stimato).takeIf { e.weightIsEstimate && e.unitGrams != null },
                        stringResource(R.string.peso_mancante).takeIf { e.unitGrams == null }, stringResource(R.string.non_piu_in_inventario).takeIf { e.gearItemId == null }, e.notes.takeIf { it.isNotBlank() },
                    ).joinToString(" · ")
                    if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(e.unitGrams?.let { weight(it.toLong() * e.quantity) } ?: "—", style = MaterialTheme.typography.titleSmall)
                    if (e.quantity > 1) Text(stringResource(R.string.quantita_x, e.quantity), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onOpen, modifier = Modifier.size(48.dp)) { Icon(AppIcons2.More, contentDescription = stringResource(R.string.dettagli_di, e.name)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntrySheet(e: ChecklistEntry, onDismiss: () -> Unit, onChange: (ChecklistEntry) -> Unit, onRemove: () -> Unit) {
    var notes by remember(e.id) { mutableStateOf(e.notes) }
    ModalBottomSheet(onDismissRequest = { if (notes != e.notes) onChange(e.copy(notes = notes.trim())); onDismiss() }, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding()) {
            Text(e.name, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.riga_sottotitolo, e.unitGrams?.let { weight(it.toLong()) } ?: "—", e.categoryName), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FieldLabel(stringResource(R.string.quantita))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.in_tutto, e.unitGrams?.let { weight(it.toLong() * e.quantity) } ?: "—"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                OutlinedIconButton(onClick = { onChange(e.copy(quantity = e.quantity - 1)) }, enabled = e.quantity > 1, modifier = Modifier.size(48.dp)) { Icon(AppIcons2.Minus, contentDescription = stringResource(R.string.meno)) }
                Text(e.quantity.toString(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 48.dp))
                OutlinedIconButton(onClick = { onChange(e.copy(quantity = e.quantity + 1)) }, modifier = Modifier.size(48.dp)) { Icon(AppIcons2.Plus, contentDescription = stringResource(R.string.piu)) }
            }
            FieldLabel(stringResource(R.string.tipo_per_uscita))
            ChoiceChips(GearKind.entries.map { it to kindLabel(it) }, e.kind, { onChange(e.copy(kind = it, notes = notes.trim())) })
            FieldLabel(stringResource(R.string.nota_per_uscita))
            OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(stringResource(R.string.togli_dallo_zaino), onRemove, Modifier.weight(1f), icon = AppIcons2.Trash)
                PrimaryButton(stringResource(R.string.fatto), { if (notes != e.notes) onChange(e.copy(notes = notes.trim())); onDismiss() }, Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToChecklistScreen(vm: ChecklistViewModel, onClose: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    val present = state.entries.mapNotNull { it.gearItemId }.toSet()
    val grams = state.items.filter { it.id in selected }.sumOf { (it.weightGrams ?: 0).toLong() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.aggiungi_allo_zaino), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(AppIcons2.Close, contentDescription = stringResource(R.string.chiudi)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                PrimaryButton(if (selected.isEmpty()) stringResource(R.string.scegli_oggetti) else stringResource(R.string.aggiungi_n, selected.size, weight(grams)), { vm.add(selected, onClose) }, icon = AppIcons2.Plus, enabled = selected.isNotEmpty())
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 20.dp)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, shape = CircleShape, placeholder = { Text(stringResource(R.string.inventario_cerca)) }, leadingIcon = { Icon(AppIcons3.Search, contentDescription = null) })
            val shown = filterItems(state.items, state.categories, query, categoryId).sortedBy { it.name.lowercase() }
            if (!state.loading && state.items.isEmpty()) Text(stringResource(R.string.zaino_inventario_vuoto), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 24.dp))
            else if (shown.isEmpty() && !state.loading) Text(stringResource(R.string.inventario_nessun_risultato), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 24.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(shown, key = { it.id }) { item ->
                    val already = item.id in present
                    val on = already || item.id in selected
                    Surface(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                        Row(Modifier.heightIn(min = 60.dp).toggleable(value = on, enabled = !already, role = Role.Checkbox) { selected = if (it) selected + item.id else selected - item.id }.padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = on, onCheckedChange = null, enabled = !already, modifier = Modifier.padding(horizontal = 10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text(if (already) stringResource(R.string.gia_nello_zaino) else state.categories.firstOrNull { it.id == item.categoryId }?.name.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(item.weightGrams?.let { weight(it.toLong()) } ?: "—", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(end = 10.dp))
                        }
                    }
                }
            }
        }
    }
}
