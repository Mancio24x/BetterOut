package app.betterout.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.data.ElevationSource
import app.betterout.data.Route
import app.betterout.data.RouteOrigin
import app.betterout.domain.ElevationStats
import app.betterout.domain.ProfilePoint
import app.betterout.ui.components.AppCard
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.gear.shareTextFile
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons3
import app.betterout.ui.trips.currentLocale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RouteDetailScreen(vm: RouteDetailViewModel, onBack: () -> Unit, onEdit: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val route = state.route
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val manualNote = stringResource(R.string.gpx_nota_manuale)
    val chooser = stringResource(R.string.condividi_con)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.percorso), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons2.Back, contentDescription = stringResource(R.string.indietro)) } },
                actions = {
                    if (route != null) Box {
                        IconButton(onClick = { menu = true }) { Icon(AppIcons2.More, contentDescription = stringResource(R.string.altre_azioni)) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.rinomina)) }, leadingIcon = { Icon(AppIcons2.Edit, contentDescription = null) }, onClick = { menu = false; renaming = true })
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.elimina), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(AppIcons2.Trash, contentDescription = null, tint = MaterialTheme.colorScheme.error) }, onClick = { menu = false; confirmDelete = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (route != null) Row(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (route.origin == RouteOrigin.DISEGNATO) SecondaryButton(stringResource(R.string.modifica), onEdit, Modifier.weight(1f), icon = AppIcons2.Edit)
                PrimaryButton(stringResource(R.string.esporta_gpx), {
                    vm.gpx(manualNote) { shareTextFile(context, gpxFileName(route.name), "application/gpx+xml", it, chooser, byteOrderMark = false) }
                }, Modifier.weight(1f), icon = AppIcons3.Download)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                route == null -> Text(stringResource(R.string.percorso_non_trovato), Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge)
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val locale = currentLocale()
                    Text(route.name, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag(stringResource(if (route.origin == RouteOrigin.DISEGNATO) R.string.percorso_disegnato else R.string.percorso_importato), if (route.origin == RouteOrigin.DISEGNATO) AppIcons2.Edit else AppIcons3.Download)
                        if (route.origin == RouteOrigin.DISEGNATO) Tag(stringResource(if (route.hasManualLegs) R.string.con_tratti_manuali else R.string.su_sentiero), if (route.hasManualLegs) AppIcons2.Warn else null)
                        Tag(if (state.tripsUsing == 0) stringResource(R.string.non_usato) else pluralStringResource(R.plurals.usato_in_uscite, state.tripsUsing, state.tripsUsing))
                    }
                    state.geometry?.let { geometry ->
                        val camera = remember { MapCamera() }
                        LaunchedEffect(geometry) { camera.fit(geometry.lines.flatten()) }
                        AppMap(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(26.dp)), lines = remember(geometry) { geometry.mapLines(5.0) }, markers = remember(geometry) { geometry.endMarkers() }, camera = camera, gestures = false)
                    }
                    StatTiles(route.distanceMeters, route.ascentMeters, route.descentMeters, estimatedMinutes(route.distanceMeters, route.ascentMeters, route.descentMeters, route.partialElevation))
                    if (route.hasManualLegs) Notice(null, stringResource(R.string.avviso_tratti_manuali), AppIcons2.Warn)
                    if (route.segmentCount > 1) Notice(null, pluralStringResource(R.plurals.avviso_segmenti, route.segmentCount, route.segmentCount), AppIcons3.Info)
                    if (route.minElevation == null || route.maxElevation == null) {
                        Notice(stringResource(R.string.senza_quote_titolo), stringResource(R.string.senza_quote_testo), AppIcons2.Warn) { ElevationRequest(state, vm::askElevations) }
                    } else {
                        AppCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.profilo_altimetrico), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                                    Text(stringResource(R.string.soglia_metri, ElevationStats.DEFAULT_THRESHOLD_METERS.toInt()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                ElevationProfile(state.profile, route.distanceMeters)
                                Row {
                                    Text(stringResource(R.string.quota_min_max), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(formatMeters(route.minElevation, locale) + " · " + formatMeters(route.maxElevation, locale), style = MaterialTheme.typography.titleSmall)
                                }
                                Text(elevationNote(route) + " " + stringResource(R.string.nota_durata), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (route.partialElevation) {
                                    Text(stringResource(R.string.quote_parziali), style = MaterialTheme.typography.bodySmall)
                                    ElevationRequest(state, vm::askElevations)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (renaming && route != null) {
        var name by rememberSaveable { mutableStateOf(route.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(R.string.rinomina), style = MaterialTheme.typography.headlineSmall) },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, isError = name.isBlank(), label = { Text(stringResource(R.string.nome_percorso)) }) },
            confirmButton = { TextButton(onClick = { renaming = false; vm.rename(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.salva)) } },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.annulla)) } },
        )
    }
    if (confirmDelete && route != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(AppIcons2.Trash, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.elimina_titolo, route.name), style = MaterialTheme.typography.headlineSmall) },
            text = { Text(if (state.tripsUsing == 0) stringResource(R.string.elimina_percorso_libero) else pluralStringResource(R.plurals.elimina_percorso_in_uso, state.tripsUsing, state.tripsUsing), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryButton(stringResource(R.string.elimina_percorso), { confirmDelete = false; vm.delete(onBack) }, icon = AppIcons2.Trash, danger = true)
                    SecondaryButton(stringResource(R.string.annulla), { confirmDelete = false })
                }
            },
        )
    }
}

fun gpxFileName(routeName: String): String = routeName.replace(Regex("[^\\p{L}\\p{N} _-]"), "_").trim().take(60).ifBlank { "percorso" } + ".gpx"

@Composable
private fun ElevationRequest(state: RouteDetailViewModel.State, onAsk: () -> Unit) {
    if (state.elevationError) Text(stringResource(R.string.quote_errore), style = MaterialTheme.typography.bodyMedium)
    if (state.askingElevations) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularProgressIndicator(Modifier.height(20.dp).padding(end = 0.dp), strokeWidth = 2.dp)
        Text(stringResource(R.string.quote_in_corso), style = MaterialTheme.typography.bodyMedium)
    } else SecondaryButton(stringResource(if (state.elevationError) R.string.riprova else R.string.chiedi_quote), onAsk)
}

@Composable
private fun elevationNote(route: Route): String = when {
    route.elevationSource == ElevationSource.NESSUNA && route.modelElevation -> stringResource(R.string.quote_da_modello)
    route.elevationSource == ElevationSource.GPX -> stringResource(R.string.quote_da_gpx)
    else -> stringResource(R.string.quote_da_routing)
} + if (route.modelElevation && route.elevationSource != ElevationSource.NESSUNA) " " + stringResource(R.string.quote_completate_modello) else ""

/** Profilo altimetrico: quota sull'asse verticale, distanza percorsa su quello orizzontale. */
@Composable
private fun ElevationProfile(profile: List<ProfilePoint>, totalDistance: Double) {
    if (profile.size < 2) return
    val locale = currentLocale()
    val min = profile.minOf { it.elevation }
    val max = profile.maxOf { it.elevation }
    val span = (max - min).coerceAtLeast(1.0)
    val line = MaterialTheme.colorScheme.onSurface
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val description = stringResource(R.string.profilo_descrizione, formatMeters(min, locale), formatMeters(max, locale), formatDistance(totalDistance, locale))
    Column(Modifier.clearAndSetSemantics { contentDescription = description }) {
        Row(Modifier.height(140.dp)) {
            Column(Modifier.padding(end = 8.dp).height(140.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(formatMeters(max, locale), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMeters(min, locale), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Canvas(Modifier.weight(1f).height(140.dp)) {
                val top = 8.dp.toPx()
                val bottom = size.height - 8.dp.toPx()
                fun x(p: ProfilePoint) = (p.distanceMeters / totalDistance.coerceAtLeast(1.0)).toFloat() * size.width
                fun y(p: ProfilePoint) = bottom - ((p.elevation - min) / span).toFloat() * (bottom - top)
                drawLine(grid, Offset(0f, top), Offset(size.width, top))
                drawLine(grid, Offset(0f, bottom), Offset(size.width, bottom))
                val path = Path().apply { moveTo(x(profile.first()), y(profile.first())); profile.drop(1).forEach { lineTo(x(it), y(it)) } }
                val area = Path().apply { addPath(path); lineTo(x(profile.last()), size.height); lineTo(x(profile.first()), size.height); close() }
                drawPath(area, fill)
                drawPath(path, line, style = Stroke(width = 2.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(formatDistance(0.0, locale), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(formatDistance(totalDistance, locale), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
