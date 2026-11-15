package app.betterout.ui.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.domain.GeoPoint
import app.betterout.domain.LegMode
import app.betterout.ui.components.ChoiceChips
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons3
import app.betterout.ui.trips.currentLocale

/**
 * Editor del percorso. I punti si mettono toccando la mappa e si spostano trascinandoli; le stesse azioni si fanno
 * anche con i pulsanti (elenco dei punti, "Sposta", "Inserisci", "Fine qui", "Elimina").
 */
@Composable
fun RouteEditorScreen(vm: RouteEditorViewModel, onClose: () -> Unit, onSaved: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val camera = remember { MapCamera() }
    var searching by remember { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val draft = state.draft
    val exit = { if (state.dirty) confirmExit = true else onClose() }
    BackHandler(enabled = state.dirty) { confirmExit = true }
    val topOverlay = with(LocalDensity.current) { 200.dp.roundToPx() }
    LaunchedEffect(state.loading) { if (!state.loading) camera.fit(draft.controls, topOverlay) }

    val lines = remember(draft) { draft.legs.mapNotNull { leg -> leg.points?.let { MapLine(it, dashed = leg.mode == LegMode.MANUALE) } } }
    val markers = remember(draft, state.selected) {
        draft.controls.mapIndexed { i, p ->
            MapMarker(p, when { i == state.selected -> MarkerKind.SELEZIONATO; i == 0 -> MarkerKind.PARTENZA; i == draft.controls.lastIndex -> MarkerKind.ARRIVO; else -> MarkerKind.PUNTO })
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            AppMap(Modifier.fillMaxSize(), lines = lines, markers = markers, camera = camera, onTap = vm::tap, onMarkerTap = vm::select, onMarkerDrag = vm::drag)
            Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    MapButton(AppIcons2.Back, stringResource(R.string.indietro), exit)
                    SearchPill(stringResource(R.string.cerca_zona), Modifier.weight(1f)) { searching = true }
                }
                val failed = state.failedLeg
                val selected = state.selected
                when {
                    failed != null -> RoutingError(failed, vm::retry, vm::failedToManual, vm::undo)
                    selected != null -> {
                        PointActions(
                            canInsert = draft.legs.getOrNull(selected)?.points != null, canEnd = selected < draft.controls.lastIndex,
                            onMove = vm::startMove, onInsert = vm::insertAfterSelected, onEnd = vm::endAtSelected, onDelete = vm::deleteSelected,
                        )
                        Hint(stringResource(if (state.moving) R.string.editor_sposta_tocca else R.string.editor_punto_selezionato, selected + 1, draft.controls.size))
                    }
                    state.routing -> Hint(stringResource(R.string.editor_calcolo), busy = true)
                    draft.controls.isEmpty() -> Hint(stringResource(R.string.editor_inizio))
                    draft.controls.size == 1 -> Hint(stringResource(R.string.editor_secondo_punto))
                }
            }
        }
        EditorSheet(state, vm, onSave = { saving = true })
    }

    if (searching) PlaceSearchSheet({ searching = false }) { place -> searching = false; camera.moveTo(GeoPoint(place.latitude, place.longitude)) }
    if (saving) {
        val defaultName = stringResource(R.string.percorso_senza_nome)
        var name by rememberSaveable { mutableStateOf(state.name.ifBlank { defaultName }) }
        AlertDialog(
            onDismissRequest = { saving = false },
            title = { Text(stringResource(R.string.salva_percorso), style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, singleLine = true, isError = name.isBlank(), label = { Text(stringResource(R.string.nome_percorso)) })
                    if (draft.hasManualLegs) Text(stringResource(R.string.avviso_tratti_manuali), style = MaterialTheme.typography.bodySmall)
                    if (state.tripsUsing > 0) Text(pluralStringResource(R.plurals.percorso_condiviso, state.tripsUsing, state.tripsUsing), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { saving = false; vm.save(name, onSaved) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.salva)) } },
            dismissButton = { TextButton(onClick = { saving = false }) { Text(stringResource(R.string.annulla)) } },
        )
    }
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(stringResource(R.string.non_salvate_titolo), style = MaterialTheme.typography.headlineSmall) },
            text = { Text(stringResource(R.string.editor_non_salvato), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(stringResource(R.string.continua_a_modificare), { confirmExit = false })
                    SecondaryButton(stringResource(R.string.esci_senza_salvare), { confirmExit = false; onClose() })
                }
            },
        )
    }
}

@Composable
private fun Hint(text: String, busy: Boolean = false) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.inverseOnSurface)
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PointActions(canInsert: Boolean, canEnd: Boolean, onMove: () -> Unit, onInsert: () -> Unit, onEnd: () -> Unit, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp, border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface)) {
        Row {
            PointAction(AppIcons3.Move, stringResource(R.string.editor_sposta), onMove)
            PointAction(AppIcons2.Plus, stringResource(R.string.editor_inserisci), onInsert, enabled = canInsert)
            PointAction(AppIcons3.Flag, stringResource(R.string.editor_fine_qui), onEnd, enabled = canEnd)
            PointAction(AppIcons2.Trash, stringResource(R.string.elimina), onDelete, danger = true)
        }
    }
}

@Composable
private fun PointAction(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true, danger: Boolean = false) {
    val color = (if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface).copy(alpha = if (enabled) 1f else 0.38f)
    Column(
        Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick).widthIn(min = 76.dp).heightIn(min = 56.dp).padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = color, textAlign = TextAlign.Center)
    }
}

/** Routing non riuscito: l'errore è detto e la scelta resta all'utente. Nessuna linea viene tracciata al suo posto. */
@Composable
private fun RoutingError(leg: Int, onRetry: () -> Unit, onManual: () -> Unit, onUndo: () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp, border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons2.Warn, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.routing_errore_titolo), style = MaterialTheme.typography.titleMedium)
            }
            Text(stringResource(R.string.routing_errore_testo, leg + 1, leg + 2), style = MaterialTheme.typography.bodyMedium)
            PrimaryButton(stringResource(R.string.riprova), onRetry)
            SecondaryButton(stringResource(R.string.routing_a_mano), onManual)
            SecondaryButton(stringResource(R.string.routing_annulla_modifica), onUndo)
        }
    }
}

@Composable
private fun EditorSheet(state: RouteEditorViewModel.State, vm: RouteEditorViewModel, onSave: () -> Unit) {
    val draft = state.draft
    val stats = state.stats
    val locale = currentLocale()
    Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 8.dp) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChips(listOf(LegMode.SENTIERO to stringResource(R.string.modo_sentieri), LegMode.MANUALE to stringResource(R.string.modo_manuale)), state.mode, vm::setMode, Modifier.weight(1f))
                MapButton(AppIcons3.Undo, stringResource(R.string.annulla_azione), vm::undo, enabled = state.canUndo && !state.routing)
                MapButton(AppIcons3.Redo, stringResource(R.string.ripristina_azione), vm::redo, enabled = state.canRedo && !state.locked)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(formatDistance(stats.distanceMeters, locale), stringResource(R.string.stat_distanza), Modifier.weight(1f))
                StatTile(stats.elevation?.let { formatMeters(it.ascent, locale) }, stringResource(R.string.stat_salita), Modifier.weight(1f))
                StatTile(estimatedMinutes(stats.distanceMeters, stats.elevation?.ascent, stats.elevation?.descent, stats.partialElevation)?.let(::formatDuration), stringResource(R.string.stat_stimata), Modifier.weight(1f))
            }
            if (draft.hasManualLegs) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons2.Warn, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.editor_legenda_manuale), style = MaterialTheme.typography.bodySmall)
            }
            if (draft.controls.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.editor_punti), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                draft.controls.indices.forEach { i ->
                    val label = stringResource(R.string.editor_punto_n, i + 1, draft.controls.size)
                    FilterChip(
                        selected = i == state.selected, onClick = { vm.select(i) }, enabled = !state.locked, shape = CircleShape,
                        label = { Text((i + 1).toString(), style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = if (i == state.selected) ({ Icon(AppIcons2.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }) else null,
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = label },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary, selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(stringResource(R.string.editor_inverti), vm::reverse, Modifier.weight(1f))
                SecondaryButton(stringResource(R.string.editor_anello), vm::closeLoop, Modifier.weight(1f))
                PrimaryButton(stringResource(R.string.salva), onSave, Modifier.weight(1f), enabled = state.canSave && state.dirty)
            }
        }
    }
}
