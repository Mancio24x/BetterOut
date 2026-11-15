package app.betterout.ui.map

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.data.RouteRepository
import app.betterout.data.Trip
import app.betterout.domain.Pace
import app.betterout.ui.components.AppCard
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.theme.AppIcons
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons3
import app.betterout.ui.trips.paceLabel
import kotlinx.coroutines.flow.flowOf

/** Riquadro del percorso nel dettaglio di un'uscita: statistiche con il ritmo dell'uscita, oppure come aggiungerlo. */
@Composable
fun TripRouteCard(trip: Trip, routes: RouteRepository, onOpenRoute: (String) -> Unit, onChoose: () -> Unit, onRemove: () -> Unit) {
    val route by remember(trip.routeId) { trip.routeId?.let(routes::observe) ?: flowOf(null) }.collectAsStateWithLifecycle(null)
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.percorso), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            val r = route
            if (r == null) {
                Text(stringResource(R.string.uscita_senza_percorso), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SecondaryButton(stringResource(R.string.aggiungi_percorso), onChoose, icon = AppIcons.Map)
            } else {
                Text(r.name, style = MaterialTheme.typography.titleLarge)
                StatTiles(r.distanceMeters, r.ascentMeters, r.descentMeters, estimatedMinutes(r.distanceMeters, r.ascentMeters, r.descentMeters, r.partialElevation, Pace.valueOf(trip.pace.name)))
                Text(
                    if (r.ascentMeters == null || r.partialElevation) stringResource(R.string.uscita_percorso_senza_quote) else stringResource(R.string.uscita_durata_nota, paceLabel(trip.pace).lowercase()),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (r.hasManualLegs) Text(stringResource(R.string.avviso_tratti_manuali), style = MaterialTheme.typography.bodySmall)
                SecondaryButton(stringResource(R.string.apri_percorso), { onOpenRoute(r.id) }, icon = AppIcons.Map)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onChoose) { Text(stringResource(R.string.cambia_percorso)) }
                    TextButton(onClick = onRemove) { Text(stringResource(R.string.togli_percorso)) }
                }
            }
        }
    }
}

/** Scelta del percorso di un'uscita: disegnarlo, importarlo o prenderlo dall'archivio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripRouteScreen(vm: RoutesViewModel, onBack: () -> Unit, onDraw: () -> Unit, onImport: (Uri) -> Unit, onPick: (String) -> Unit) {
    val routes by vm.routes.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) onImport(uri) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.percorso_uscita), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons2.Back, contentDescription = stringResource(R.string.indietro)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.scelta_percorso_titolo), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.scelta_percorso_testo), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OptionRow(AppIcons2.Edit, stringResource(R.string.disegna_titolo), stringResource(R.string.disegna_testo), onDraw)
            OptionRow(AppIcons3.Download, stringResource(R.string.importa_titolo), stringResource(R.string.importa_testo), { picker.launch(GPX_MIME_TYPES) })
            Text(stringResource(R.string.scegli_archivio), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
            if (routes?.isEmpty() == true) Text(stringResource(R.string.percorsi_vuoto_titolo), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else RouteList(routes, onPick, Modifier.weight(1f))
        }
    }
}
