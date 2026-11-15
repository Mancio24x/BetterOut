package app.betterout.ui.trips

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.data.TripStatus
import app.betterout.domain.TripRules
import app.betterout.ui.components.EmptyState
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.components.statusLabel
import app.betterout.ui.theme.AppIcons2
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(vm: TripsViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val trips by vm.trips.collectAsStateWithLifecycle()
    var filterName by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = filterName?.let(TripStatus::valueOf)
    val today = LocalDate.now()
    val all = trips.orEmpty()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tutte_le_uscite), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons2.Back, contentDescription = stringResource(R.string.indietro)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val options: List<Pair<TripStatus?, String>> = listOf(null to stringResource(R.string.filtro_tutte)) + TripStatus.entries.map { it to statusLabel(it) }
                options.forEach { (status, label) ->
                    val count = if (status == null) all.size else all.count { it.status == status }
                    FilterChip(
                        selected = filter == status, onClick = { filterName = status?.name },
                        label = { Text(stringResource(R.string.filtro_conteggio, label, count), style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = if (filter == status) ({ Icon(AppIcons2.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }) else null,
                        shape = CircleShape, modifier = Modifier.heightIn(min = 40.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface, selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary, selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
            val shown = TripRules.sorted(all.filter { filter == null || it.status == filter }, today)
            if (trips != null && shown.isEmpty()) {
                Spacer(Modifier.weight(1f))
                EmptyState(AppIcons2.Filter, stringResource(R.string.filtro_vuoto_titolo), stringResource(R.string.filtro_vuoto_testo)) {
                    if (filter != null) SecondaryButton(stringResource(R.string.mostra_tutte), { filterName = null })
                }
                Spacer(Modifier.weight(1.4f))
            } else {
                LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp)) {
                    items(shown, key = { it.id }) { TripRow(it, today) { onOpen(it.id) } }
                }
            }
        }
    }
}
