package app.betterout.ui.dive

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.betterout.R
import app.betterout.data.DiveGear
import app.betterout.data.DiveGearRepository
import app.betterout.ui.components.FieldLabel
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.home.TabTopBar
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons4
import app.betterout.ui.theme.AppIcons5
import app.betterout.ui.theme.AppIcons6
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Un pezzo tipico dell'attrezzatura sub: chiave salvata nel database, nome e simbolo. */
class DiveGearPreset(val key: String, @param:StringRes val label: Int, val icon: ImageVector)

val DiveGearPresets: List<DiveGearPreset> by lazy {
    listOf(
        DiveGearPreset("maschera", R.string.sub_maschera, AppIcons5.Mask),
        DiveGearPreset("pinne", R.string.sub_pinne, AppIcons6.Fin),
        DiveGearPreset("muta", R.string.sub_muta, AppIcons6.Wetsuit),
        DiveGearPreset("calzari", R.string.sub_calzari, AppIcons5.Boot),
        DiveGearPreset("guanti", R.string.sub_guanti, AppIcons6.Glove),
        DiveGearPreset("cappuccio", R.string.sub_cappuccio, AppIcons6.Hood),
        DiveGearPreset("gav", R.string.sub_gav, AppIcons6.Bcd),
        DiveGearPreset("erogatore", R.string.sub_erogatore, AppIcons6.Regulator),
        DiveGearPreset("octopus", R.string.sub_octopus, AppIcons6.Octopus),
        DiveGearPreset("manometro", R.string.sub_manometro, AppIcons5.Gauge),
        DiveGearPreset("computer", R.string.sub_computer, AppIcons6.Computer),
        DiveGearPreset("bombola", R.string.sub_bombola, AppIcons6.Tank),
        DiveGearPreset("zavorra", R.string.sub_zavorra, AppIcons6.Weight),
        DiveGearPreset("torcia", R.string.sub_torcia, AppIcons5.Torch),
        DiveGearPreset("coltello", R.string.sub_coltello, AppIcons5.Knife),
        DiveGearPreset("boa", R.string.sub_boa, AppIcons6.Buoy),
        DiveGearPreset("bussola", R.string.sub_bussola, AppIcons5.Compass),
        DiveGearPreset("borsa", R.string.sub_borsa, AppIcons6.Bag),
    )
}

private fun presetFor(key: String?) = DiveGearPresets.firstOrNull { it.key == key }

class DiveGearViewModel(private val repo: DiveGearRepository) : ViewModel() {
    /** null mentre si legge il database. */
    val items: StateFlow<List<DiveGear>?> = repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun add(name: String, preset: String?) { if (name.isNotBlank()) viewModelScope.launch { repo.add(name, preset) } }
    fun toggle(item: DiveGear) = viewModelScope.launch { repo.setPacked(item.id, !item.packed) }
    fun save(item: DiveGear) = viewModelScope.launch { repo.save(item) }
    fun delete(item: DiveGear) = viewModelScope.launch { repo.delete(item.id) }
    fun clear() = viewModelScope.launch { repo.clearPacked() }

    companion object {
        fun factory(repo: DiveGearRepository): ViewModelProvider.Factory = viewModelFactory { initializer { DiveGearViewModel(repo) } }
    }
}

/**
 * L'attrezzatura sub: il kit con le spunte della checklist (pronto per l'immersione) e, sotto, i pezzi tipici
 * da aggiungere con un tocco, ognuno con il suo simbolo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiveGearScreen(vm: DiveGearViewModel, onSettings: () -> Unit) {
    val items by vm.items.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<DiveGear?>(null) }
    var addingOther by rememberSaveable { mutableStateOf(false) }
    var askClear by rememberSaveable { mutableStateOf(false) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val kit = items
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TabTopBar(stringResource(R.string.nav_attrezzatura), onSettings, scroll) {
                if (kit.orEmpty().any { it.packed }) IconButton(onClick = { askClear = true }) { Icon(AppIcons4.Refresh, contentDescription = stringResource(R.string.sub_azzera)) }
            }
        },
    ) { padding ->
        if (kit == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else {
            val used = remember(kit) { kit.mapNotNull { it.preset }.toSet() }
            val missing = remember(used) { DiveGearPresets.filter { it.key !in used } }
            // Un pezzo aggiunto compare in cima, nel kit: l'elenco ci torna, invece di restare agganciato al pannello.
            var lastSize by remember { mutableIntStateOf(kit.size) }
            LaunchedEffect(kit.size) { if (kit.size > lastSize) listState.animateScrollToItem(0); lastSize = kit.size }
            LazyColumn(Modifier.padding(padding), state = listState, contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (kit.isNotEmpty()) {
                    item(key = "kit") {
                        val packed = kit.count { it.packed }
                        Column(Modifier.padding(top = 4.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.sub_kit), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).semantics { heading() })
                                Text(
                                    if (packed == kit.size) stringResource(R.string.sub_tutto_pronto) else stringResource(R.string.pronti_di, packed, kit.size),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            LinearProgressIndicator(progress = { packed.toFloat() / kit.size }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), drawStopIndicator = {})
                        }
                    }
                    items(kit, key = { it.id }) { item -> KitRow(item, Modifier.animateItem(), onToggle = { vm.toggle(item) }, onEdit = { editing = item }) }
                }
                item(key = "da-aggiungere") {
                    AddPanel(missing, Modifier.padding(top = 10.dp).animateItem(), onAdd = { preset, name -> vm.add(name, preset.key) }, onOther = { addingOther = true })
                }
            }
        }
    }
    editing?.let { item -> EditSheet(item, onDismiss = { editing = null }, onSave = { vm.save(it); editing = null }, onDelete = { vm.delete(item); editing = null }) }
    if (addingOther) OtherDialog(onDismiss = { addingOther = false }) { vm.add(it, null); addingOther = false }
    if (askClear) AlertDialog(
        onDismissRequest = { askClear = false },
        title = { Text(stringResource(R.string.sub_azzera_titolo)) },
        text = { Text(stringResource(R.string.sub_azzera_testo)) },
        confirmButton = { TextButton(onClick = { vm.clear(); askClear = false }) { Text(stringResource(R.string.sub_azzera)) } },
        dismissButton = { TextButton(onClick = { askClear = false }) { Text(stringResource(R.string.annulla)) } },
    )
}

@Composable
private fun IconBox(icon: ImageVector) {
    Surface(Modifier.size(48.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp)) }
    }
}

/** Un pezzo del kit: tutta la riga spunta e toglie la spunta; la matita apre nome e note. */
@Composable
private fun KitRow(item: DiveGear, modifier: Modifier, onToggle: () -> Unit, onEdit: () -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(Modifier.heightIn(min = 72.dp).padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).toggleable(item.packed, role = Role.Checkbox) { onToggle() }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBox(presetFor(item.preset)?.icon ?: AppIcons5.Box)
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (item.notes.isNotBlank()) Text(item.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Checkbox(item.packed, onCheckedChange = null)
            }
            IconButton(onClick = onEdit) { Icon(AppIcons2.Edit, contentDescription = stringResource(R.string.modifica), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** Il pannello dei pezzi da aggiungere: una griglia di simboli con il nome; un tocco mette il pezzo nel kit. */
@Composable
private fun AddPanel(missing: List<DiveGearPreset>, modifier: Modifier, onAdd: (DiveGearPreset, String) -> Unit, onOther: () -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.attrezzatura_da_aggiungere), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            if (missing.isEmpty()) Text(stringResource(R.string.sub_kit_completo), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
                missing.forEach { preset ->
                    val name = stringResource(preset.label)
                    AddTile(preset.icon, name, Modifier.weight(1f)) { onAdd(preset, name) }
                }
                AddTile(AppIcons2.Plus, stringResource(R.string.sub_altro), Modifier.weight(1f), onClick = onOther)
                // Riempie l'ultima riga: le caselle restano tutte della stessa larghezza.
                repeat((3 - (missing.size + 1) % 3) % 3) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AddTile(icon: ImageVector, name: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier.heightIn(min = 96.dp).clip(RoundedCornerShape(18.dp)).clickable(role = Role.Button, onClickLabel = stringResource(R.string.sub_aggiungi), onClick = onClick),
        shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Text(name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSheet(item: DiveGear, onDismiss: () -> Unit, onSave: (DiveGear) -> Unit, onDelete: () -> Unit) {
    var name by rememberSaveable(item.id) { mutableStateOf(item.name) }
    var notes by rememberSaveable(item.id) { mutableStateOf(item.notes) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldLabel(stringResource(R.string.sub_nome))
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp), isError = name.isBlank(), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            FieldLabel(stringResource(R.string.campo_note))
            OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(14.dp), placeholder = { Text(stringResource(R.string.sub_note_esempio)) }, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            PrimaryButton(stringResource(R.string.salva_modifiche), { onSave(item.copy(name = name, notes = notes)) }, icon = AppIcons2.Check, enabled = name.isNotBlank())
            SecondaryButton(stringResource(R.string.sub_togli), onDelete, icon = AppIcons2.Trash)
        }
    }
}

@Composable
private fun OtherDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sub_altro)) },
        text = { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.sub_nome)) }, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)) },
        confirmButton = { TextButton(onClick = { onAdd(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.sub_aggiungi)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.annulla)) } },
    )
}
