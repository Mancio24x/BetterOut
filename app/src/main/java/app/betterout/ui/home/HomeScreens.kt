package app.betterout.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import app.betterout.data.RouteRepository
import app.betterout.ui.map.RoutePreview

/** Passaggio fra escursioni e immersioni: il tasto sta accanto alle impostazioni in ogni scheda principale. */
data class ModeSwitch(val diving: Boolean, val toggle: () -> Unit)

val LocalModeSwitch = staticCompositionLocalOf<ModeSwitch?> { null }

/** Anteprima del percorso dell'uscita; niente se l'uscita non ha un percorso. Toccandola si apre la mappa. */
@Composable
fun TripMap(routes: RouteRepository, routeId: String?, modifier: Modifier = Modifier, onOpen: (() -> Unit)? = null) =
    RoutePreview(routes, routeId, modifier, onOpen)
