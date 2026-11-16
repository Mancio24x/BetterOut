package app.betterout.ui.dive

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.betterout.BetterOutApplication
import app.betterout.R
import app.betterout.data.Dive
import app.betterout.domain.DiveRules
import app.betterout.domain.GeoPoint
import app.betterout.ui.components.AddFab
import app.betterout.ui.components.AppCard
import app.betterout.ui.components.ChoiceChips
import app.betterout.ui.components.EmptyState
import app.betterout.ui.components.FieldLabel
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.home.SettingsScreen
import app.betterout.ui.home.TabHeader
import app.betterout.ui.map.PlacePickerDialog
import app.betterout.ui.map.StatTile
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons3
import app.betterout.ui.theme.AppIcons5
import app.betterout.ui.trips.PickDateDialog
import app.betterout.ui.trips.currentLocale
import app.betterout.ui.trips.formatDate
import app.betterout.ui.trips.formatTime
import java.time.LocalTime
import java.time.format.DateTimeParseException
import java.util.Locale

/** La modalità immersioni: logbook, scheda dell'immersione e impostazioni, con il tema blu. */
@Composable
fun DiveApp(app: BetterOutApplication) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "logbook") {
        val settings = { nav.navigate("impostazioni") }
        composable("logbook") {
            DiveLogScreen(
                viewModel(factory = DiveLogViewModel.factory(app.dives)), onNew = { nav.navigate("immersione/nuova") }, onOpen = { nav.navigate("immersione/$it") },
                onImport = { nav.navigate("importa?uri=${Uri.encode(it.toString())}") }, onSettings = settings,
            )
        }
        composable("importa?uri={uri}", listOf(navArgument("uri") { type = NavType.StringType })) { e ->
            val uri = Uri.parse(e.arguments?.getString("uri").orEmpty())
            DiveImportScreen(viewModel(factory = DiveImportViewModel.factory(app, app.dives, uri)), onClose = { nav.popBackStack() }) { nav.popBackStack("logbook", inclusive = false) }
        }
        composable("immersione/nuova") {
            DiveEditScreen(viewModel(factory = DiveEditViewModel.factory(app.dives, null)), onClose = { nav.popBackStack() }) { id ->
                nav.navigate("immersione/$id") { popUpTo("logbook") }
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

// ---- Formati ----

internal fun decimal(value: Double, locale: Locale): String =
    if (value % 1.0 == 0.0) String.format(locale, "%.0f", value) else String.format(locale, "%.1f", value)

internal fun meters(value: Double, locale: Locale) = decimal(value, locale) + " m"

/** "−2 °C": segno meno tipografico e unità sempre scritta. */
internal fun celsius(value: Double, locale: Locale) = decimal(value, locale).replace("-", "−") + " °C"

@Composable
internal fun minutes(value: Int): String = stringResource(R.string.imm_minuti, value)

@Composable
private fun hours(totalMinutes: Int): String = stringResource(R.string.imm_ore_minuti, totalMinutes / 60, totalMinutes % 60)

@Composable
internal fun gasName(oxygenPercent: Int): String = if (oxygenPercent == 21) stringResource(R.string.imm_aria) else stringResource(R.string.imm_ean, oxygenPercent)

// ---- Logbook ----

@Composable
fun DiveLogScreen(vm: DiveLogViewModel, onNew: () -> Unit, onOpen: (String) -> Unit, onImport: (Uri) -> Unit, onSettings: () -> Unit) {
    val list by vm.dives.collectAsStateWithLifecycle()
    // FIT, UDDF e XML non hanno un tipo MIME comune a tutti i telefoni: si sceglie fra tutti i file e si riconosce dal contenuto.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) onImport(uri) }
    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            TabHeader(stringResource(R.string.nav_immersioni), onSettings) {
                IconButton(onClick = { picker.launch(arrayOf("*/*")) }) { Icon(AppIcons3.Download, contentDescription = stringResource(R.string.imm_importa)) }
            }
            val dives = list
            when {
                dives == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                dives.isEmpty() -> EmptyState(AppIcons5.Mask, stringResource(R.string.logbook_vuoto_titolo), stringResource(R.string.logbook_vuoto_testo), Modifier.padding(top = 48.dp)) {
                    SecondaryButton(stringResource(R.string.imm_importa), { picker.launch(arrayOf("*/*")) }, icon = AppIcons3.Download)
                }
                else -> LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 104.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { TotalsCard(dives) }
                    items(DiveRules.sorted(dives), key = { it.id }) { DiveRow(it) { onOpen(it.id) } }
                }
            }
        }
        AddFab(stringResource(R.string.nuova_immersione), onNew, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp))
    }
}

/** Scheda piena nel colore principale, come la prossima uscita: quante immersioni, quanto tempo sott'acqua, il record. */
@Composable
private fun TotalsCard(dives: List<Dive>) {
    val totals = DiveRules.totals(dives)
    val locale = currentLocale()
    AppCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(pluralStringResource(R.plurals.immersioni_conteggio, totals.count, totals.count), style = MaterialTheme.typography.headlineMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TotalValue(hours(totals.minutes), stringResource(R.string.imm_tempo_sottacqua))
                totals.deepestMeters?.let { TotalValue(meters(it, locale), stringResource(R.string.imm_record)) }
            }
        }
    }
}

@Composable
private fun TotalValue(value: String, label: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DiveRow(dive: Dive, onClick: () -> Unit) {
    val locale = currentLocale()
    Surface(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("#${dive.number}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(dive.siteName.ifBlank { stringResource(R.string.sito_vuoto) }, style = MaterialTheme.typography.titleMedium)
                val facts = listOfNotNull(formatDate(dive.date, locale, withYear = true), dive.maxDepthMeters?.let { meters(it, locale) }, dive.durationMinutes?.let { minutes(it) })
                Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---- Dettaglio ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiveDetailScreen(vm: DiveDetailViewModel, onBack: () -> Unit, onEdit: () -> Unit) {
    val dive by vm.dive.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    var askDelete by remember { mutableStateOf(false) }
    val locale = currentLocale()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(dive?.let { stringResource(R.string.immersione_n, it.number) }.orEmpty(), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons2.Back, contentDescription = stringResource(R.string.indietro)) } },
                actions = {
                    if (dive != null) {
                        IconButton(onClick = onEdit) { Icon(AppIcons2.Edit, contentDescription = stringResource(R.string.modifica)) }
                        IconButton(onClick = { askDelete = true }) { Icon(AppIcons2.Trash, contentDescription = stringResource(R.string.elimina)) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val d = dive ?: return@Scaffold Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(d.siteName.ifBlank { stringResource(R.string.sito_vuoto) }, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Text(listOfNotNull(formatDate(d.date, locale, withYear = true), d.startTime?.let(::formatTime)).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(d.maxDepthMeters?.let { meters(it, locale) }, stringResource(R.string.imm_stat_profondita), Modifier.weight(1f))
                StatTile(d.durationMinutes?.let { minutes(it) }, stringResource(R.string.imm_stat_durata), Modifier.weight(1f))
                StatTile(d.waterTempC?.let { celsius(it, locale) }, stringResource(R.string.imm_stat_acqua), Modifier.weight(1f))
            }
            if (profile.size >= 2) DiveProfileCard(profile)
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(stringResource(R.string.dettagli), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp).semantics { heading() })
                    val sac = DiveRules.sacLitersPerMinute(d)
                    listOf(
                        R.string.imm_gas to gasName(d.oxygenPercent),
                        R.string.imm_prof_media to d.averageDepthMeters?.let { meters(it, locale) },
                        R.string.imm_visibilita to d.visibilityMeters?.let { meters(it, locale) },
                        R.string.imm_bombola to d.tankLiters?.let { stringResource(R.string.imm_litri, decimal(it, locale)) },
                        R.string.imm_bar_inizio to d.startBar?.let { stringResource(R.string.imm_bar, it) },
                        R.string.imm_bar_fine to d.endBar?.let { stringResource(R.string.imm_bar, it) },
                        R.string.imm_consumo to sac?.let { stringResource(R.string.imm_consumo_valore, String.format(locale, "%.1f", it)) },
                        R.string.imm_compagno to d.buddy.ifBlank { null },
                    ).forEach { (label, value) ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(value ?: "—", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            if (DiveRules.sacLitersPerMinute(d) == null && d.startBar != null) {
                Text(stringResource(R.string.imm_consumo_mancano), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (d.notes.isNotBlank()) AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.campo_note), style = MaterialTheme.typography.titleMedium)
                    Text(d.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    val d = dive
    if (askDelete && d != null) AlertDialog(
        onDismissRequest = { askDelete = false },
        icon = { Icon(AppIcons2.Trash, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.imm_elimina_titolo, d.number)) },
        text = { Text(stringResource(R.string.imm_elimina_testo)) },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(stringResource(R.string.elimina), { askDelete = false; vm.delete(onBack) }, danger = true)
                SecondaryButton(stringResource(R.string.annulla), { askDelete = false })
            }
        },
    )
}

// ---- Nuova e modifica ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiveEditScreen(vm: DiveEditViewModel, onClose: () -> Unit, onSaved: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var askLeave by remember { mutableStateOf(false) }
    val leave = { if (state.isDirty) askLeave = true else onClose() }
    BackHandler(enabled = state.isDirty) { askLeave = true }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (vm.isNew) R.string.nuova_immersione else R.string.modifica_immersione), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = leave) { Icon(AppIcons2.Close, contentDescription = stringResource(R.string.chiudi)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                PrimaryButton(stringResource(if (vm.isNew) R.string.salva_immersione else R.string.salva_modifiche), { vm.save(onSaved) }, icon = AppIcons2.Check, enabled = state.invalid.isEmpty())
            }
        },
    ) { padding ->
        val draft = state.draft
        if (draft == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else DiveForm(draft, Modifier.padding(padding), vm::edit, vm::setInvalid)
    }
    if (askLeave) {
        ModalBottomSheet(onDismissRequest = { askLeave = false }, containerColor = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.non_salvate_titolo), style = MaterialTheme.typography.headlineSmall)
                if (state.invalid.isEmpty()) PrimaryButton(stringResource(R.string.salva_immersione), { askLeave = false; vm.save(onSaved) }, icon = AppIcons2.Check)
                SecondaryButton(stringResource(R.string.esci_senza_salvare), { askLeave = false; onClose() })
                TextButton(onClick = { askLeave = false }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.continua_a_modificare), style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

@Composable
private fun DiveForm(draft: Dive, modifier: Modifier, edit: ((Dive) -> Dive) -> Unit, setInvalid: (String, Boolean) -> Unit) {
    val locale = currentLocale()
    val shape = RoundedCornerShape(14.dp)
    var pickingDate by remember { mutableStateOf(false) }
    var pickingPoint by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberField(stringResource(R.string.imm_numero), draft.number.toDouble(), "", "numero", setInvalid, decimals = false, required = true) { v -> edit { it.copy(number = v?.toInt() ?: it.number) } }
        FieldLabel(stringResource(R.string.campo_sito))
        OutlinedTextField(draft.siteName, { v -> edit { it.copy(siteName = v) } }, Modifier.fillMaxWidth(), singleLine = true, shape = shape, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
        val lat = draft.latitude
        val lon = draft.longitude
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (lat != null && lon != null) stringResource(R.string.coordinate, String.format(locale, "%.4f, %.4f", lat, lon)) else stringResource(R.string.senza_coordinate),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
            )
            if (lat != null) TextButton(onClick = { edit { it.copy(latitude = null, longitude = null) } }) { Text(stringResource(R.string.togli_coordinate)) }
        }
        SecondaryButton(stringResource(R.string.scegli_sulla_mappa), { pickingPoint = true })

        FieldLabel(stringResource(R.string.imm_data))
        SecondaryButton(formatDate(draft.date, locale, withYear = true), { pickingDate = true })
        TimeField(draft.startTime, setInvalid) { t -> edit { it.copy(startTime = t) } }

        NumberField(stringResource(R.string.imm_durata), draft.durationMinutes?.toDouble(), "min", "durata", setInvalid, decimals = false) { v -> edit { it.copy(durationMinutes = v?.toInt()) } }
        NumberField(stringResource(R.string.imm_prof_max), draft.maxDepthMeters, "m", "prof_max", setInvalid) { v -> edit { it.copy(maxDepthMeters = v) } }
        NumberField(stringResource(R.string.imm_prof_media), draft.averageDepthMeters, "m", "prof_media", setInvalid) { v -> edit { it.copy(averageDepthMeters = v) } }
        NumberField(stringResource(R.string.imm_temp), draft.waterTempC, "°C", "temp", setInvalid, allowNegative = true) { v -> edit { it.copy(waterTempC = v) } }
        NumberField(stringResource(R.string.imm_visibilita), draft.visibilityMeters, "m", "visibilita", setInvalid) { v -> edit { it.copy(visibilityMeters = v) } }

        FieldLabel(stringResource(R.string.imm_gas))
        ChoiceChips(listOf(21 to stringResource(R.string.imm_aria), 32 to stringResource(R.string.imm_ean, 32), 36 to stringResource(R.string.imm_ean, 36)), draft.oxygenPercent, { o -> edit { it.copy(oxygenPercent = o) } })
        NumberField(stringResource(R.string.imm_ossigeno), draft.oxygenPercent.toDouble(), "%", "ossigeno", setInvalid, decimals = false, required = true, range = 21.0..100.0) { v -> edit { it.copy(oxygenPercent = v?.toInt() ?: it.oxygenPercent) } }
        NumberField(stringResource(R.string.imm_bombola), draft.tankLiters, "l", "bombola", setInvalid) { v -> edit { it.copy(tankLiters = v) } }
        NumberField(stringResource(R.string.imm_bar_inizio), draft.startBar?.toDouble(), "bar", "bar_inizio", setInvalid, decimals = false) { v -> edit { it.copy(startBar = v?.toInt()) } }
        NumberField(stringResource(R.string.imm_bar_fine), draft.endBar?.toDouble(), "bar", "bar_fine", setInvalid, decimals = false) { v -> edit { it.copy(endBar = v?.toInt()) } }

        FieldLabel(stringResource(R.string.imm_compagno))
        OutlinedTextField(draft.buddy, { v -> edit { it.copy(buddy = v) } }, Modifier.fillMaxWidth(), singleLine = true, shape = shape, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
        FieldLabel(stringResource(R.string.campo_note))
        OutlinedTextField(draft.notes, { v -> edit { it.copy(notes = v) } }, Modifier.fillMaxWidth(), minLines = 3, shape = shape, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
    }
    if (pickingDate) PickDateDialog(draft.date, { pickingDate = false }) { d -> edit { it.copy(date = d) } }
    if (pickingPoint) PlacePickerDialog(draft.latitude?.let { la -> draft.longitude?.let { GeoPoint(la, it) } }, { pickingPoint = false }) { p ->
        pickingPoint = false
        edit { it.copy(latitude = p.lat, longitude = p.lon) }
    }
}

/** Legge "18,5" o "18.5"; null per un campo vuoto. Un testo che non è un numero valido resta scritto e segnalato. */
internal fun parseNumber(text: String, decimals: Boolean, allowNegative: Boolean): Result<Double?> {
    val t = text.trim().replace(',', '.')
    if (t.isEmpty()) return Result.success(null)
    val v = t.toDoubleOrNull() ?: return Result.failure(NumberFormatException(t))
    if (!decimals && v % 1.0 != 0.0) return Result.failure(NumberFormatException(t))
    if (!allowNegative && v < 0) return Result.failure(NumberFormatException(t))
    return Result.success(v)
}

@Composable
private fun NumberField(
    label: String, value: Double?, unit: String, key: String, setInvalid: (String, Boolean) -> Unit,
    decimals: Boolean = true, allowNegative: Boolean = false, required: Boolean = false, range: ClosedFloatingPointRange<Double>? = null,
    onChange: (Double?) -> Unit,
) {
    val locale = currentLocale()
    var text by remember(key) { mutableStateOf(value?.let { decimal(it, locale) }.orEmpty()) }
    // Un valore cambiato da fuori (per esempio dalle pillole del gas) aggiorna il testo.
    val parsed = parseNumber(text, decimals, allowNegative)
    if (parsed.isSuccess && value != parsed.getOrNull() && value != null) text = decimal(value, locale)
    val error = parsed.isFailure || (required && parsed.getOrNull() == null) || (range != null && parsed.getOrNull()?.let { it !in range } == true)
    FieldLabel(label)
    OutlinedTextField(
        text,
        { t ->
            text = t
            val p = parseNumber(t, decimals, allowNegative)
            val bad = p.isFailure || (required && p.getOrNull() == null) || (range != null && p.getOrNull()?.let { it !in range } == true)
            setInvalid(key, bad)
            if (!bad) onChange(p.getOrNull())
        },
        Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp), isError = error,
        suffix = { Text(unit) },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimals) KeyboardType.Decimal else KeyboardType.Number),
        supportingText = if (error) ({ Text(stringResource(if (range != null) R.string.imm_fuori_intervallo else R.string.imm_valore_non_valido)) }) else null,
    )
}

@Composable
private fun TimeField(value: LocalTime?, setInvalid: (String, Boolean) -> Unit, onChange: (LocalTime?) -> Unit) {
    var text by remember { mutableStateOf(value?.let(::formatTime).orEmpty()) }
    fun parse(t: String): Result<LocalTime?> = if (t.isBlank()) Result.success(null) else try {
        Result.success(LocalTime.parse(t.trim().replace('.', ':').let { if (it.length == 4 && it[1] == ':') "0$it" else it }))
    } catch (e: DateTimeParseException) { Result.failure(e) }
    val error = parse(text).isFailure
    FieldLabel(stringResource(R.string.imm_ora))
    OutlinedTextField(
        text,
        { t -> text = t; val p = parse(t); setInvalid("ora", p.isFailure); if (p.isSuccess) onChange(p.getOrNull()) },
        Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp), isError = error, placeholder = { Text("09:30") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        supportingText = if (error) ({ Text(stringResource(R.string.imm_ora_non_valida)) }) else null,
    )
}
