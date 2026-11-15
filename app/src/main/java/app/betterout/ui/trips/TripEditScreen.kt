package app.betterout.ui.trips

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.toggleable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.data.Activity
import app.betterout.data.Difficulty
import app.betterout.data.Overnight
import app.betterout.data.Trip
import app.betterout.data.TripPace
import app.betterout.data.TripStatus
import app.betterout.data.WaterAvailability
import app.betterout.ui.components.ChoiceChips
import app.betterout.ui.components.FieldLabel
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.components.statusLabel
import app.betterout.ui.theme.AppIcons2
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEditScreen(vm: TripEditViewModel, onClose: () -> Unit, onSaved: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var askLeave by remember { mutableStateOf(false) }
    val leave = { if (state.isDirty) askLeave = true else onClose() }
    BackHandler(enabled = state.isDirty) { askLeave = true }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (vm.isNew) R.string.nuova_uscita else R.string.modifica_uscita), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = leave) { Icon(AppIcons2.Close, contentDescription = stringResource(R.string.chiudi)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                PrimaryButton(stringResource(if (vm.isNew) R.string.salva_uscita else R.string.salva_modifiche), { vm.save(onDone = onSaved) }, icon = AppIcons2.Check)
            }
        },
    ) { padding ->
        val draft = state.draft
        if (draft == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else TripForm(draft, state.nameError, state.datesValid, vm.isNew, Modifier.padding(padding), vm::edit)
    }
    if (askLeave) {
        ModalBottomSheet(onDismissRequest = { askLeave = false }, containerColor = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.non_salvate_titolo), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.non_salvate_testo), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PrimaryButton(stringResource(R.string.salva_bozza), { askLeave = false; vm.save(asDraft = true, onDone = onSaved) }, icon = AppIcons2.Check)
                SecondaryButton(stringResource(R.string.esci_senza_salvare), { askLeave = false; onClose() })
                TextButton(onClick = { askLeave = false }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.continua_a_modificare), style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripForm(draft: Trip, nameError: Boolean, datesValid: Boolean, isNew: Boolean, modifier: Modifier, edit: ((Trip) -> Trip) -> Unit) {
    val locale = currentLocale()
    var picking by remember { mutableStateOf<String?>(null) } // "start", "end", "time"
    val shape = RoundedCornerShape(14.dp)
    val caps = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
        FieldLabel(stringResource(R.string.campo_nome), Modifier.padding(top = 0.dp))
        OutlinedTextField(
            draft.title, { v -> edit { it.copy(title = v) } }, Modifier.fillMaxWidth(), singleLine = true, shape = shape, isError = nameError, keyboardOptions = caps,
            supportingText = if (nameError) ({ Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(AppIcons2.Warn, contentDescription = null, modifier = Modifier.size(15.dp)); Text(stringResource(R.string.errore_nome)) } }) else null,
        )
        FieldLabel(stringResource(R.string.campo_localita))
        OutlinedTextField(draft.placeName, { v -> edit { it.copy(placeName = v) } }, Modifier.fillMaxWidth(), singleLine = true, shape = shape, keyboardOptions = caps,
            leadingIcon = { Icon(AppIcons2.Pin, contentDescription = null) }, supportingText = { Text(stringResource(R.string.aiuto_localita)) })

        FieldLabel(stringResource(R.string.campo_attivita))
        ChoiceChips(Activity.entries.map { it to activityLabel(it) }, draft.activity, { v -> edit { it.copy(activity = v) } })

        FieldLabel(stringResource(R.string.campo_date))
        val noDate = draft.startDate == null
        Row(
            Modifier.fillMaxWidth().toggleable(value = noDate, role = Role.Switch) { on ->
                edit { if (on) it.copy(startDate = null, endDate = null, startTime = null, status = if (it.status == TripStatus.PROGRAMMATA) TripStatus.BOZZA else it.status) else it.copy(startDate = LocalDate.now().plusDays(1), status = if (it.status == TripStatus.BOZZA) TripStatus.PROGRAMMATA else it.status) }
            }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.nessuna_data), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.nessuna_data_aiuto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = noDate, onCheckedChange = null, colors = SwitchDefaults.colors(uncheckedTrackColor = MaterialTheme.colorScheme.surface, uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant, uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant))
        }
        if (!noDate) {
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerField(stringResource(R.string.data_inizio), formatDate(draft.startDate!!, locale, true), Modifier.weight(1f)) { picking = "start" }
                PickerField(stringResource(R.string.data_fine), draft.endDate?.let { formatDate(it, locale, true) } ?: stringResource(R.string.scegli), Modifier.weight(1f)) { picking = "end" }
            }
            if (!datesValid) Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(AppIcons2.Warn, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.errore_date), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                PickerField(stringResource(R.string.ora_partenza), draft.startTime?.let(::formatTime) ?: stringResource(R.string.scegli), Modifier.weight(1f)) { picking = "time" }
                if (draft.endDate != null) TextButton(onClick = { edit { it.copy(endDate = null) } }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.togli) + " · " + stringResource(R.string.data_fine)) }
                else Box(Modifier.weight(1f))
            }
            Text(stringResource(R.string.fuso_orario, draft.timeZoneId), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }

        FieldLabel(stringResource(R.string.campo_stato))
        ChoiceChips(TripStatus.entries.map { it to statusLabel(it) }, draft.status, { v -> edit { it.copy(status = v) } })
        if (!noDate && draft.status == TripStatus.PROGRAMMATA) Text(stringResource(R.string.suggerisci_programmata), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

        FieldLabel(stringResource(R.string.campo_pernottamento))
        ChoiceChips(Overnight.entries.map { it to overnightLabel(it) }, draft.overnight, { v -> edit { it.copy(overnight = v) } })
        FieldLabel(stringResource(R.string.campo_acqua))
        ChoiceChips(WaterAvailability.entries.map { it to waterLabel(it) }, draft.water, { v -> edit { it.copy(water = v) } })
        FieldLabel(stringResource(R.string.campo_ritmo))
        ChoiceChips(TripPace.entries.map { it to paceLabel(it) }, draft.pace, { v -> edit { it.copy(pace = v) } })

        FieldLabel(stringResource(R.string.campo_ore))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedIconButton(onClick = { edit { it.copy(walkingHoursPerDay = (it.walkingHoursPerDay - 1).coerceAtLeast(1)) } }, modifier = Modifier.size(48.dp), shape = CircleShape) { Icon(AppIcons2.Minus, contentDescription = stringResource(R.string.meno)) }
            Text(draft.walkingHoursPerDay.toString(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 48.dp))
            OutlinedIconButton(onClick = { edit { it.copy(walkingHoursPerDay = (it.walkingHoursPerDay + 1).coerceAtMost(14)) } }, modifier = Modifier.size(48.dp), shape = CircleShape) { Icon(AppIcons2.Plus, contentDescription = stringResource(R.string.piu)) }
        }

        FieldLabel(stringResource(R.string.campo_difficolta))
        ChoiceChips(Difficulty.entries.map { it to difficultyLabel(it) }, draft.difficulty, { v -> edit { it.copy(difficulty = v) } })

        FieldLabel(stringResource(R.string.campo_descrizione))
        OutlinedTextField(draft.description, { v -> edit { it.copy(description = v) } }, Modifier.fillMaxWidth(), minLines = 2, shape = shape, keyboardOptions = caps)
        FieldLabel(stringResource(R.string.campo_note))
        OutlinedTextField(draft.notes, { v -> edit { it.copy(notes = v) } }, Modifier.fillMaxWidth(), minLines = 2, shape = shape, keyboardOptions = caps)
    }
    when (picking) {
        "start" -> PickDateDialog(draft.startDate, { picking = null }) { d -> edit { it.copy(startDate = d) } }
        "end" -> PickDateDialog(draft.endDate ?: draft.startDate, { picking = null }) { d -> edit { it.copy(endDate = d) } }
        "time" -> {
            val t = rememberTimePickerState(initialHour = draft.startTime?.hour ?: 7, initialMinute = draft.startTime?.minute ?: 30, is24Hour = true)
            AlertDialog(
                onDismissRequest = { picking = null },
                confirmButton = { TextButton(onClick = { edit { it.copy(startTime = LocalTime.of(t.hour, t.minute)) }; picking = null }) { Text(stringResource(R.string.conferma)) } },
                dismissButton = { TextButton(onClick = { picking = null }) { Text(stringResource(R.string.annulla)) } },
                title = { Text(stringResource(R.string.ora_partenza), style = MaterialTheme.typography.titleMedium) },
                text = { TimePicker(t) },
            )
        }
    }
}

@Composable
private fun PickerField(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val description = "$label: $value"
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        SecondaryButton(value, onClick, Modifier.semantics { contentDescription = description })
    }
}
