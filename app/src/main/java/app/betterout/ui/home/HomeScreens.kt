package app.betterout.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.betterout.R
import app.betterout.data.RouteRepository
import app.betterout.ui.map.RoutePreview
import app.betterout.ui.theme.AppIcons5

/** Passaggio fra escursioni e immersioni: il tasto sta accanto alle impostazioni in ogni scheda principale. */
data class ModeSwitch(val diving: Boolean, val toggle: () -> Unit)

val LocalModeSwitch = staticCompositionLocalOf<ModeSwitch?> { null }

/** Titolo di una scheda principale, con le azioni in alto a destra (le impostazioni sono sempre l'ultima). */
@Composable
fun TabHeader(title: String, onSettings: () -> Unit, extra: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f).semantics { heading() })
        extra()
        ModeSwitchButton()
        IconButton(onClick = onSettings) { Icon(AppIcons5.Cog, contentDescription = stringResource(R.string.nav_impostazioni)) }
    }
}

/** Pulsante tondo grande in basso a destra. */
@Composable
fun RoundFab(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier.size(72.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        shape = CircleShape, color = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary, shadowElevation = 6.dp,
    ) { Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, modifier = Modifier.size(36.dp)) } }
}

/** Anteprima del percorso dell'uscita; niente se l'uscita non ha un percorso. Toccandola si apre la mappa. */
@Composable
fun TripMap(routes: RouteRepository, routeId: String?, modifier: Modifier = Modifier, onOpen: (() -> Unit)? = null) =
    RoutePreview(routes, routeId, modifier, onOpen)
