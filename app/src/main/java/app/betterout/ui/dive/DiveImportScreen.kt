package app.betterout.ui.dive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.domain.DiveFileFormat
import app.betterout.domain.DiveImportError
import app.betterout.domain.DiveSample
import app.betterout.domain.ImportedDive
import app.betterout.ui.components.AppCard
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.trips.currentLocale
import app.betterout.ui.trips.formatDate
import app.betterout.ui.trips.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiveImportScreen(vm: DiveImportViewModel, onClose: () -> Unit, onDone: (Int) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.imm_importa), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(AppIcons2.Close, contentDescription = stringResource(R.string.chiudi)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            val s = state
            if (s is DiveImportViewModel.State.Preview) Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                PrimaryButton(
                    pluralStringResource(R.plurals.imm_importa_n, s.selected.size, s.selected.size), { vm.import(onDone) },
                    icon = AppIcons2.Check, enabled = s.selected.isNotEmpty() && !s.saving,
                )
            }
        },
    ) { padding ->
        when (val s = state) {
            DiveImportViewModel.State.Reading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.imm_lettura), style = MaterialTheme.typography.bodyMedium)
                }
            }
            is DiveImportViewModel.State.Failed -> Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                s.fileName?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                Text(
                    stringResource(
                        when (s.reason) {
                            null -> R.string.imm_errore_apertura
                            DiveImportError.NON_RICONOSCIUTO -> R.string.imm_errore_formato
                            DiveImportError.ROVINATO -> R.string.imm_errore_rovinato
                            DiveImportError.NESSUNA_IMMERSIONE -> R.string.imm_errore_vuoto
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(stringResource(R.string.imm_formati), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SecondaryButton(stringResource(R.string.chiudi), onClose)
            }
            is DiveImportViewModel.State.Preview -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
                item {
                    val format = when (s.format) { DiveFileFormat.FIT -> "FIT"; DiveFileFormat.UDDF -> "UDDF"; DiveFileFormat.SUBSURFACE -> "Subsurface" }
                    Text(listOfNotNull(s.fileName, format).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(pluralStringResource(R.plurals.imm_trovate, s.dives.size, s.dives.size), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 8.dp).semantics { heading() })
                    if (s.duplicates.isNotEmpty()) Text(
                        pluralStringResource(R.plurals.imm_gia_presenti, s.duplicates.size, s.duplicates.size),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                itemsIndexed(s.dives) { i, d ->
                    ImportRow(d, duplicate = i in s.duplicates, checked = i in s.selected) { vm.toggle(i) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun ImportRow(d: ImportedDive, duplicate: Boolean, checked: Boolean, onToggle: () -> Unit) {
    val locale = currentLocale()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).toggleable(checked, role = Role.Checkbox) { onToggle() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked, onCheckedChange = null)
        Column(Modifier.weight(1f)) {
            Text(d.siteName.ifBlank { listOfNotNull(formatDate(d.date, locale, withYear = true), d.startTime?.let(::formatTime)).joinToString(" · ") }, style = MaterialTheme.typography.titleMedium)
            val facts = listOfNotNull(
                if (d.siteName.isNotBlank()) formatDate(d.date, locale, withYear = true) else null,
                d.maxDepthMeters?.let { meters(it, locale) }, d.durationMinutes?.let { minutes(it) },
                if (d.oxygenPercent != 21) gasName(d.oxygenPercent) else null,
                if (duplicate) stringResource(R.string.imm_gia_nel_logbook) else null,
            )
            Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Profilo dell'immersione: profondità nel tempo, con la superficie in alto. */
@Composable
internal fun DiveProfileCard(samples: List<DiveSample>) {
    val locale = currentLocale()
    val maxDepth = samples.maxOf { it.depthMeters }.coerceAtLeast(1.0)
    val totalSeconds = samples.last().seconds.coerceAtLeast(1)
    val line = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val description = stringResource(R.string.imm_profilo_letto, meters(maxDepth, locale), minutes((totalSeconds + 30) / 60))
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.imm_profilo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.height(160.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("0 m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(meters(maxDepth, locale), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Canvas(Modifier.weight(1f).height(160.dp).semantics { contentDescription = description }) {
                    fun point(s: DiveSample) = Offset(size.width * s.seconds / totalSeconds, (size.height * (s.depthMeters / maxDepth)).toFloat())
                    listOf(0f, 0.5f, 1f).forEach { f -> drawLine(grid, Offset(0f, size.height * f), Offset(size.width, size.height * f), strokeWidth = 1.dp.toPx()) }
                    // Al più un punto per pixel: un profilo di migliaia di campioni resta leggero da disegnare.
                    val step = (samples.size / size.width.toInt().coerceAtLeast(1)).coerceAtLeast(1)
                    val points = samples.filterIndexed { i, _ -> i % step == 0 || i == samples.lastIndex }.map(::point)
                    val path = Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) } }
                    val area = Path().apply { addPath(path); lineTo(points.last().x, 0f); lineTo(points.first().x, 0f); close() }
                    drawPath(area, fill)
                    drawPath(path, line, style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round))
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 40.dp)) {
                Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(minutes((totalSeconds + 30) / 60), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
