package app.betterout.ui.map

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import app.betterout.domain.Geo
import app.betterout.domain.GpxError
import app.betterout.domain.GpxTrack
import app.betterout.domain.RouteMath
import app.betterout.ui.components.AppCard
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons3
import app.betterout.ui.trips.currentLocale
import java.util.Locale

/**
 * Importazione di un file GPX: lettura, anteprima con avvisi, conferma. Nulla è salvato prima della conferma.
 * [onDone] riceve i percorsi creati e se uno è stato associato all'uscita.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpxImportScreen(vm: GpxImportViewModel, onBack: () -> Unit, onAnotherFile: (Uri) -> Unit, onDone: (List<String>, Boolean) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) onAnotherFile(uri) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.importa_gpx), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = { vm.cancel(); onBack() }) { Icon(AppIcons2.Back, contentDescription = stringResource(R.string.indietro)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            val s = state
            Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (s) {
                    is GpxImportViewModel.State.Reading -> SecondaryButton(stringResource(R.string.annulla), { vm.cancel(); onBack() })
                    is GpxImportViewModel.State.Failed -> PrimaryButton(stringResource(R.string.scegli_altro_file), { picker.launch(GPX_MIME_TYPES) })
                    is GpxImportViewModel.State.Preview -> {
                        val count = s.selected.size
                        if (vm.forTrip && count == 1) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton(stringResource(R.string.solo_archivio), { vm.save(link = false) { onDone(it, false) } }, Modifier.weight(1f))
                            PrimaryButton(stringResource(R.string.associa_uscita), { vm.save(link = true) { onDone(it, true) } }, Modifier.weight(1f), enabled = !s.saving)
                        } else PrimaryButton(
                            if (count <= 1) stringResource(R.string.salva_in_archivio) else pluralStringResource(R.plurals.importa_percorsi, count, count),
                            { vm.save(link = false) { onDone(it, false) } }, enabled = count > 0 && !s.saving,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (val s = state) {
                is GpxImportViewModel.State.Reading -> {
                    FileCard(s.fileName ?: stringResource(R.string.file_scelto), null)
                    val percent = (s.fraction * 100).toInt().coerceIn(0, 100)
                    Text(stringResource(R.string.lettura_in_corso, percent), style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(progress = { s.fraction.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                }
                is GpxImportViewModel.State.Failed -> {
                    FileCard(s.fileName ?: stringResource(R.string.file_scelto), null)
                    val (title, text) = when (s.reason) {
                        GpxError.NON_GPX -> R.string.gpx_errore_non_gpx_titolo to R.string.gpx_errore_non_gpx_testo
                        GpxError.XML_NON_VALIDO -> R.string.gpx_errore_xml_titolo to R.string.gpx_errore_xml_testo
                        GpxError.VUOTO -> R.string.gpx_errore_vuoto_titolo to R.string.gpx_errore_vuoto_testo
                        null -> R.string.gpx_errore_apertura_titolo to R.string.gpx_errore_apertura_testo
                    }
                    Notice(stringResource(title), stringResource(text) + " " + stringResource(R.string.gpx_nulla_salvato), AppIcons2.Warn)
                }
                is GpxImportViewModel.State.Preview -> Preview(s, vm::toggle)
            }
        }
    }
}

@Composable
private fun Preview(s: GpxImportViewModel.State.Preview, onToggle: (Int) -> Unit) {
    val locale = currentLocale()
    val tracks = s.file.tracks
    val segments = tracks.sumOf { it.segments.size }
    FileCard(s.fileName, listOfNotNull(s.file.version?.let { "GPX $it" }, formatSize(s.sizeBytes, locale)).joinToString(" · "))
    if (segments > tracks.size) Notice(
        pluralStringResource(R.plurals.conteggio_tracciati, tracks.size, tracks.size) + ", " + pluralStringResource(R.plurals.conteggio_segmenti, segments, segments),
        stringResource(R.string.gpx_segmenti_separati), AppIcons3.Info,
    )
    if (s.file.discardedPoints > 0) Notice(null, pluralStringResource(R.plurals.gpx_punti_scartati, s.file.discardedPoints, s.file.discardedPoints), AppIcons2.Warn)
    if (tracks.size > 1) {
        Row {
            Text(stringResource(R.string.cosa_importare), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).semantics { heading() })
            Text(stringResource(R.string.scelti_di, s.selected.size, tracks.size), style = MaterialTheme.typography.titleSmall)
        }
        tracks.forEachIndexed { i, track ->
            Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).toggleable(value = i in s.selected, role = Role.Checkbox) { onToggle(i) }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = i in s.selected, onCheckedChange = null, modifier = Modifier.padding(horizontal = 10.dp))
                    Column {
                        Text(s.trackName(i), style = MaterialTheme.typography.titleMedium)
                        Text(trackSubtitle(track, locale), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    val stats = s.stats
    if (s.selected.isNotEmpty()) {
        if (stats.elevation == null || stats.partialElevation) Notice(
            stringResource(if (stats.elevation == null) R.string.senza_quote_titolo else R.string.quote_parziali_titolo), stringResource(R.string.gpx_senza_quote_testo), AppIcons2.Warn,
        )
        val lines = remember(s.selected) { s.chosen.flatMap { t -> t.segments.map { MapLine(Geo.simplify(it, 10.0)) } } }
        val camera = remember { MapCamera() }
        LaunchedEffect(lines) { camera.fit(lines.flatMap { it.points }) }
        AppMap(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(26.dp)), lines = lines, camera = camera, gestures = false)
        StatTiles(stats.distanceMeters, stats.elevation?.ascent, stats.elevation?.descent, estimatedMinutes(stats.distanceMeters, stats.elevation?.ascent, stats.elevation?.descent, stats.partialElevation))
        Text(
            listOfNotNull(
                pluralStringResource(R.plurals.conteggio_punti, stats.pointCount, stats.pointCount),
                s.file.waypoints.size.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.conteggio_punti_interesse, it, it) },
                stringResource(R.string.stima_nota_breve).takeIf { stats.elevation != null && !stats.partialElevation },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else Text(stringResource(R.string.gpx_nessuno_scelto), style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun trackSubtitle(track: GpxTrack, locale: Locale): String {
    val stats = RouteMath.stats(track.segments)
    return listOf(
        pluralStringResource(R.plurals.conteggio_segmenti, track.segments.size, track.segments.size),
        formatDistance(stats.distanceMeters, locale),
        stats.elevation?.let { stringResource(R.string.salita_di, formatMeters(it.ascent, locale)) } ?: stringResource(R.string.quote_assenti),
    ).joinToString(" · ")
}

@Composable
private fun FileCard(name: String, details: String?) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Box(Modifier.padding(10.dp)) { Icon(AppIcons3.File, contentDescription = null) } }
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium)
                if (details != null) Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun formatSize(bytes: Long, locale: Locale): String = if (bytes >= 1_000_000) String.format(locale, "%.1f MB", bytes / 1_000_000.0) else String.format(locale, "%.0f kB", (bytes / 1000.0).coerceAtLeast(1.0))
