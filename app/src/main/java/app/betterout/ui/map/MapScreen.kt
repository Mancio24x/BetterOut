package app.betterout.ui.map

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.data.Route
import app.betterout.domain.GeoPoint
import app.betterout.ui.components.AddFab
import app.betterout.ui.components.ChoiceChips
import app.betterout.ui.components.EmptyState
import app.betterout.ui.theme.AppIcons
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons3
import app.betterout.ui.trips.currentLocale

/** Per il selettore di Android: i file GPX non hanno un tipo riconosciuto ovunque, quindi si accetta qualunque file e lo si controlla leggendolo. */
val GPX_MIME_TYPES = arrayOf("*/*")

/** Scheda Mappa: la mappa con i percorsi salvati, oppure l'archivio in elenco. */
@Composable
fun MapScreen(vm: RoutesViewModel, onOpenRoute: (String) -> Unit, onDraw: () -> Unit, onImport: (Uri) -> Unit) {
    val routes by vm.routes.collectAsStateWithLifecycle()
    val lines by vm.lines.collectAsStateWithLifecycle()
    var showList by rememberSaveable { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    val camera = remember { MapCamera() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) onImport(uri) }

    Box(Modifier.fillMaxSize()) {
        if (!showList) {
            AppMap(Modifier.fillMaxSize(), lines = lines, camera = camera)
            // Alla prima apertura con percorsi salvati la mappa li inquadra.
            var fitted by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(lines) { if (!fitted && lines.isNotEmpty()) { camera.fit(lines.flatMap { it.points }); fitted = true } }
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!showList) SearchPill(stringResource(R.string.cerca_zona)) { searching = true }
            else Text(stringResource(R.string.percorsi), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            ChoiceChips(listOf(false to stringResource(R.string.nav_mappa), true to stringResource(R.string.percorsi_conteggio, routes?.size ?: 0)), showList, { showList = it })
            if (showList) RouteList(routes, onOpenRoute, Modifier.weight(1f))
        }
        AddFab(stringResource(R.string.nuovo_percorso), { creating = true }, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = if (showList) 16.dp else 44.dp))
    }
    if (searching) PlaceSearchSheet({ searching = false }) { place -> searching = false; camera.moveTo(GeoPoint(place.latitude, place.longitude)) }
    if (creating) NewRouteSheet({ creating = false }, onDraw = { creating = false; onDraw() }, onImport = { creating = false; picker.launch(GPX_MIME_TYPES) })
}

@Composable
fun SearchPill(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(modifier.fillMaxWidth().clip(CircleShape).clickable(role = Role.Button, onClick = onClick), shape = CircleShape, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface), shadowElevation = 3.dp) {
        Row(Modifier.heightIn(min = 52.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(AppIcons3.Search, contentDescription = null)
            Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RouteList(routes: List<Route>?, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    when {
        routes == null -> Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        routes.isEmpty() -> Column(modifier) {
            Spacer(Modifier.weight(1f))
            EmptyState(AppIcons.Map, stringResource(R.string.percorsi_vuoto_titolo), stringResource(R.string.percorsi_vuoto_testo))
            Spacer(Modifier.weight(1.6f))
        }
        else -> LazyColumn(modifier, contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(routes, key = { it.id }) { route -> RouteRow(route) { onOpen(route.id) } }
        }
    }
}

@Composable
private fun RouteRow(route: Route, onClick: () -> Unit) {
    val locale = currentLocale()
    Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(role = Role.Button, onClick = onClick), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(route.name, style = MaterialTheme.typography.titleMedium)
                Text(routeSubtitle(route), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatDistance(route.distanceMeters, locale), style = MaterialTheme.typography.titleSmall)
            Icon(AppIcons2.Chevron, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewRouteSheet(onDismiss: () -> Unit, onDraw: () -> Unit, onImport: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.nuovo_percorso), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            OptionRow(AppIcons2.Edit, stringResource(R.string.disegna_titolo), stringResource(R.string.disegna_testo), onDraw)
            OptionRow(AppIcons3.Download, stringResource(R.string.importa_titolo), stringResource(R.string.importa_testo), onImport)
        }
    }
}
