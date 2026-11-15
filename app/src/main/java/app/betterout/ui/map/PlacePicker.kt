package app.betterout.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.betterout.R
import app.betterout.domain.GeoPoint
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.theme.AppIcons2

/** Scelta di un punto sulla mappa: si porta il punto sotto il mirino al centro e si conferma con il pulsante. */
@Composable
fun PlacePickerDialog(initial: GeoPoint?, onDismiss: () -> Unit, onPick: (GeoPoint) -> Unit) {
    val camera = remember { MapCamera() }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (initial != null) camera.moveTo(initial) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column {
                Box(Modifier.weight(1f)) {
                    AppMap(Modifier.fillMaxSize(), camera = camera)
                    Icon(AppIcons2.Plus, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(40.dp), tint = MaterialTheme.colorScheme.error)
                    Row(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        MapButton(AppIcons2.Close, stringResource(R.string.chiudi), onDismiss)
                        SearchPill(stringResource(R.string.cerca_zona), Modifier.weight(1f)) { searching = true }
                    }
                }
                PrimaryButton(stringResource(R.string.usa_questo_punto), { camera.center?.let(onPick) }, Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), icon = AppIcons2.Pin)
            }
        }
    }
    if (searching) PlaceSearchSheet({ searching = false }) { place -> searching = false; camera.moveTo(GeoPoint(place.latitude, place.longitude)) }
}
