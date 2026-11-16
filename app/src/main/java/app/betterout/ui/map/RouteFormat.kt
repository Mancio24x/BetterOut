package app.betterout.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.betterout.R
import app.betterout.data.Route
import app.betterout.data.RouteGeometry
import app.betterout.data.RouteOrigin
import app.betterout.domain.DurationEstimator
import app.betterout.domain.Geo
import app.betterout.domain.GeoPoint
import app.betterout.domain.LegMode
import app.betterout.domain.Pace
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.trips.currentLocale
import java.util.Locale

fun formatDistance(meters: Double, locale: Locale): String = when {
    meters < 1000 -> String.format(locale, "%.0f m", meters)
    meters < 100_000 -> String.format(locale, "%.1f km", meters / 1000)
    else -> String.format(locale, "%.0f km", meters / 1000)
}

fun formatMeters(meters: Double, locale: Locale): String = String.format(locale, "%.0f m", meters)

fun formatDuration(minutes: Int): String = if (minutes < 60) "$minutes min" else String.format(Locale.ROOT, "%d h %02d", minutes / 60, minutes % 60)

/**
 * Durata stimata di cammino. È null se le quote mancano del tutto o in parte ([partial]): con un dislivello incompleto
 * la stima sarebbe troppo bassa, e una stima ingannevole è peggio di nessuna stima.
 */
fun estimatedMinutes(distanceMeters: Double, ascent: Double?, descent: Double?, partial: Boolean, pace: Pace = Pace.MEDIO): Int? =
    if (ascent != null && descent != null && !partial) DurationEstimator.minutes(distanceMeters, ascent, descent, pace) else null

/** Le linee da disegnare. Con [toleranceMeters] > 0 sono semplificate: vale solo per il disegno. */
fun RouteGeometry.mapLines(toleranceMeters: Double = 0.0): List<MapLine> {
    fun shown(points: List<GeoPoint>) = if (toleranceMeters > 0) Geo.simplify(points, toleranceMeters) else points
    return draft?.legs?.mapNotNull { leg -> leg.points?.let { MapLine(shown(it), dashed = leg.mode == LegMode.MANUALE) } } ?: segments.map { MapLine(shown(it)) }
}

fun RouteGeometry.endMarkers(): List<MapMarker> {
    val all = lines.filter { it.isNotEmpty() }
    if (all.isEmpty()) return emptyList()
    return listOf(MapMarker(all.first().first(), MarkerKind.PARTENZA), MapMarker(all.last().last(), MarkerKind.ARRIVO))
}

@Composable
fun routeSubtitle(route: Route): String {
    val locale = currentLocale()
    return listOfNotNull(
        stringResource(if (route.origin == RouteOrigin.DISEGNATO) R.string.percorso_disegnato else R.string.percorso_importato),
        formatDistance(route.distanceMeters, locale),
        route.ascentMeters?.let { stringResource(R.string.salita_di, formatMeters(it, locale)) } ?: stringResource(R.string.quote_assenti),
        stringResource(R.string.con_tratti_manuali).takeIf { route.hasManualLegs },
    ).joinToString(" · ")
}

/** Le quattro statistiche. Un valore che manca è mostrato come trattino e letto come "non disponibile". */
@Composable
fun StatTiles(distanceMeters: Double, ascent: Double?, descent: Double?, minutes: Int?, modifier: Modifier = Modifier) {
    val locale = currentLocale()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(formatDistance(distanceMeters, locale), stringResource(R.string.stat_distanza), Modifier.weight(1f))
        StatTile(ascent?.let { formatMeters(it, locale) }, stringResource(R.string.stat_salita), Modifier.weight(1f))
        StatTile(descent?.let { formatMeters(it, locale) }, stringResource(R.string.stat_discesa), Modifier.weight(1f))
        StatTile(minutes?.let(::formatDuration), stringResource(R.string.stat_stimata), Modifier.weight(1f))
    }
}

@Composable
fun StatTile(value: String?, label: String, modifier: Modifier = Modifier) {
    val spoken = label + ": " + (value ?: stringResource(R.string.non_disponibile))
    Surface(modifier.clearAndSetSemantics { contentDescription = spoken }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(value ?: "—", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, lineHeight = 24.sp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Etichetta con bordo: sempre testo, con icona facoltativa. */
@Composable
fun Tag(text: String, icon: ImageVector? = null) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.background, border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Riquadro di avviso o informazione: icona e testo, mai solo colore. */
@Composable
fun Notice(title: String?, text: String, icon: ImageVector, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (title != null) Text(title, style = MaterialTheme.typography.titleSmall)
                Text(text, style = MaterialTheme.typography.bodyMedium)
                action?.invoke()
            }
        }
    }
}

/** Riga di scelta a tutta larghezza: icona, titolo, spiegazione. */
@Composable
fun OptionRow(icon: ImageVector, title: String, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(role = Role.Button, onClick = onClick), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Row(Modifier.heightIn(min = 72.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null) } }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(AppIcons2.Chevron, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

/** Pulsante tondo sopra la mappa. */
@Composable
fun MapButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(
        modifier.size(52.dp).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        shape = CircleShape, color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.3f)), shadowElevation = 3.dp,
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f)) }
    }
}
