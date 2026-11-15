package app.betterout.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.ui.semantics.Role
import app.betterout.data.Activity
import app.betterout.data.ChecklistEntry
import app.betterout.domain.ChecklistRules
import app.betterout.ui.gear.ChecklistViewModel
import app.betterout.ui.gear.PackSummary
import app.betterout.ui.theme.AppIcons
import app.betterout.data.Difficulty
import app.betterout.data.Overnight
import app.betterout.data.Trip
import app.betterout.data.TripPace
import app.betterout.data.TripStatus
import app.betterout.data.WaterAvailability
import app.betterout.domain.TripRules
import app.betterout.ui.components.AppCard
import app.betterout.ui.components.ChoiceChips
import app.betterout.ui.components.FieldLabel
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.components.StatusChip
import app.betterout.ui.theme.AppIcons2
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable fun activityLabel(a: Activity) = stringResource(when (a) { Activity.ESCURSIONE -> R.string.attivita_escursione; Activity.TREKKING -> R.string.attivita_trekking; Activity.FERRATA -> R.string.attivita_ferrata; Activity.INVERNALE -> R.string.attivita_invernale; Activity.CORSA -> R.string.attivita_corsa; Activity.ALTRO -> R.string.attivita_altro })
@Composable fun overnightLabel(o: Overnight) = stringResource(when (o) { Overnight.NESSUNO -> R.string.notte_nessuno; Overnight.TENDA -> R.string.notte_tenda; Overnight.RIFUGIO -> R.string.notte_rifugio; Overnight.BIVACCO -> R.string.notte_bivacco })
@Composable fun waterLabel(w: WaterAvailability) = stringResource(when (w) { WaterAvailability.NON_INDICATA -> R.string.acqua_non_indicata; WaterAvailability.FREQUENTE -> R.string.acqua_frequente; WaterAvailability.SCARSA -> R.string.acqua_scarsa; WaterAvailability.ASSENTE -> R.string.acqua_assente })
@Composable fun paceLabel(p: TripPace) = stringResource(when (p) { TripPace.LENTO -> R.string.ritmo_lento; TripPace.MEDIO -> R.string.ritmo_medio; TripPace.VELOCE -> R.string.ritmo_veloce })
@Composable fun difficultyLabel(d: Difficulty) = if (d == Difficulty.NON_INDICATA) stringResource(R.string.difficolta_non_indicata) else d.name

fun millisToDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
fun dateToMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickDateDialog(initial: LocalDate?, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial?.let(::dateToMillis))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onPick(millisToDate(it)) }; onDismiss() }, enabled = state.selectedDateMillis != null) { Text(stringResource(R.string.conferma)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.annulla)) } },
    ) { DatePicker(state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(vm: TripDetailViewModel, packVm: ChecklistViewModel, onPack: () -> Unit, onBack: () -> Unit, onEdit: () -> Unit, onOpenCopy: (String) -> Unit, routeCard: @Composable (Trip) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pack by packVm.state.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var duplicating by rememberSaveable { mutableStateOf(false) }
    val trip = (state as? TripDetailViewModel.State.Ready)?.trip
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.uscita), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons2.Back, contentDescription = stringResource(R.string.indietro)) } },
                actions = {
                    if (trip != null) {
                        IconButton(onClick = onEdit) { Icon(AppIcons2.Edit, contentDescription = stringResource(R.string.modifica)) }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(AppIcons2.More, contentDescription = stringResource(R.string.altre_azioni)) }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                @Composable fun entry(label: String, icon: ImageVector, danger: Boolean = false, action: () -> Unit) = DropdownMenuItem(
                                    text = { Text(label, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
                                    leadingIcon = { Icon(icon, contentDescription = null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
                                    onClick = { menu = false; action() },
                                )
                                entry(stringResource(R.string.duplica), AppIcons2.Copy) { duplicating = true }
                                if (trip.status != TripStatus.COMPLETATA) entry(stringResource(R.string.segna_completata), AppIcons2.Check) { vm.setStatus(TripStatus.COMPLETATA) }
                                if (trip.status != TripStatus.PROGRAMMATA && trip.startDate != null) entry(stringResource(R.string.segna_programmata), AppIcons2.Calendar) { vm.setStatus(TripStatus.PROGRAMMATA) }
                                if (trip.status != TripStatus.BOZZA) entry(stringResource(R.string.riporta_in_bozza), AppIcons2.Edit) { vm.setStatus(TripStatus.BOZZA) }
                                if (trip.status != TripStatus.ANNULLATA) entry(stringResource(R.string.annulla_uscita), AppIcons2.Close) { vm.setStatus(TripStatus.ANNULLATA) }
                                entry(stringResource(R.string.elimina), AppIcons2.Trash, danger = true) { confirmDelete = true }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                TripDetailViewModel.State.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                TripDetailViewModel.State.Missing -> Text(stringResource(R.string.uscita_non_trovata), Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge)
                is TripDetailViewModel.State.Ready -> TripDetailContent(s.trip, pack.entries, onPack, Modifier.fillMaxSize(), routeCard = routeCard)
            }
        }
    }
    if (confirmDelete && trip != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(AppIcons2.Trash, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.elimina_titolo, trip.title), style = MaterialTheme.typography.headlineSmall) },
            text = { Text(stringResource(R.string.elimina_testo_zaino), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(stringResource(R.string.elimina_uscita), { confirmDelete = false; vm.delete(onBack) }, icon = AppIcons2.Trash, danger = true)
                    SecondaryButton(stringResource(R.string.annulla), { confirmDelete = false })
                }
            },
        )
    }
    if (duplicating && trip != null) DuplicateSheet(trip, onDismiss = { duplicating = false }) { title, date, reset -> duplicating = false; vm.duplicate(title, date, reset, onOpenCopy) }
}

@Composable
fun TripDetailContent(trip: Trip, entries: List<ChecklistEntry>, onPack: () -> Unit, modifier: Modifier = Modifier, bottomSpace: androidx.compose.ui.unit.Dp = 24.dp, routeCard: @Composable (Trip) -> Unit) {
    val today = LocalDate.now()
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = bottomSpace), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(trip.status)
            tripCountdown(trip, today)?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Text(trip.title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        IconLine(AppIcons2.Clock, tripWhen(trip, withYear = true))
        IconLine(AppIcons2.Pin, trip.placeName.ifBlank { stringResource(R.string.senza_localita) })
        if (TripRules.isOverdue(trip, today)) IconLine(AppIcons2.Warn, stringResource(R.string.data_passata))
        if (trip.description.isNotBlank()) Text(trip.description, style = MaterialTheme.typography.bodyLarge)
        routeCard(trip)
        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.zaino), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                if (entries.isEmpty()) Text(stringResource(R.string.zaino_vuoto_testo), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else PackSummary(entries, ChecklistRules.totals(entries, trip.waterLiters))
                PrimaryButton(stringResource(if (entries.isEmpty()) R.string.prepara_zaino else R.string.apri_zaino), onPack, icon = AppIcons.Backpack)
            }
        }
        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(stringResource(R.string.dettagli), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp).semantics { heading() })
                val rows = buildList {
                    add(stringResource(R.string.campo_attivita) to activityLabel(trip.activity))
                    if (trip.startDate != null) { val d = TripRules.lengthInDays(trip); add(stringResource(R.string.campo_date) to pluralStringResource(R.plurals.durata_giorni, d, d)) }
                    add(stringResource(R.string.campo_pernottamento) to overnightLabel(trip.overnight))
                    add(stringResource(R.string.campo_acqua) to waterLabel(trip.water))
                    add(stringResource(R.string.campo_ritmo) to paceLabel(trip.pace))
                    add(stringResource(R.string.campo_ore) to trip.walkingHoursPerDay.toString())
                    add(stringResource(R.string.campo_difficolta) to difficultyLabel(trip.difficulty))
                }
                rows.forEach { (k, v) ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(k, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(v, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        if (trip.notes.isNotBlank()) AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.campo_note), style = MaterialTheme.typography.titleMedium)
                Text(trip.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(stringResource(R.string.fuso_orario, trip.timeZoneId), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IconLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DuplicateSheet(trip: Trip, onDismiss: () -> Unit, onConfirm: (String, LocalDate?, Boolean) -> Unit) {
    val suffix = stringResource(R.string.duplica_suffisso, trip.title)
    var title by rememberSaveable { mutableStateOf(suffix) }
    var dateText by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf(false) }
    var reset by rememberSaveable { mutableStateOf(true) }
    val date = dateText?.let(LocalDate::parse)
    val locale = currentLocale()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.duplica_titolo), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.duplica_testo), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            FieldLabel(stringResource(R.string.duplica_nome))
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), singleLine = true, isError = title.isBlank())
            FieldLabel(stringResource(R.string.duplica_data))
            ChoiceChips(
                listOf(false to stringResource(R.string.senza_data), true to (date?.let { formatDate(it, locale, true) } ?: stringResource(R.string.scegli_data))),
                selected = date != null, onSelect = { if (it) picking = true else dateText = null },
            )
            Row(Modifier.fillMaxWidth().padding(top = 12.dp).toggleable(value = reset, role = Role.Switch) { reset = it }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.duplica_azzera), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.duplica_azzera_aiuto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = reset, onCheckedChange = null)
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(stringResource(R.string.annulla), onDismiss, Modifier.weight(1f))
                PrimaryButton(stringResource(R.string.duplica), { onConfirm(title.trim(), date, reset) }, Modifier.weight(1f), icon = AppIcons2.Copy, enabled = title.isNotBlank())
            }
        }
    }
    if (picking) PickDateDialog(date, { picking = false }) { dateText = it.toString() }
}
