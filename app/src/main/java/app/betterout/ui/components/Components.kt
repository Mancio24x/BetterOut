package app.betterout.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.betterout.R
import app.betterout.data.TripStatus
import app.betterout.ui.theme.AppIcons2

@Composable
fun AppCard(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.surface, contentColor: Color = MaterialTheme.colorScheme.onSurface, content: @Composable () -> Unit) {
    Surface(modifier = modifier, shape = RoundedCornerShape(26.dp), color = color, contentColor = contentColor, shadowElevation = 3.dp, content = content)
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, danger: Boolean = false) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp), shape = CircleShape,
        colors = if (danger) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError) else ButtonDefaults.buttonColors(),
    ) {
        if (icon != null) { Icon(icon, contentDescription = null); Box(Modifier.size(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    OutlinedButton(
        onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 56.dp), shape = CircleShape,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        if (icon != null) { Icon(icon, contentDescription = null); Box(Modifier.size(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun statusLabel(status: TripStatus): String = stringResource(
    when (status) {
        TripStatus.BOZZA -> R.string.stato_bozza
        TripStatus.PROGRAMMATA -> R.string.stato_programmata
        TripStatus.COMPLETATA -> R.string.stato_completata
        TripStatus.ANNULLATA -> R.string.stato_annullata
    },
)

/** Stato dell'uscita: sempre icona e parola, più una forma diversa per ogni stato (mai solo colore). */
@Composable
fun StatusChip(status: TripStatus, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = CircleShape
    val icon = when (status) {
        TripStatus.BOZZA -> AppIcons2.Edit
        TripStatus.PROGRAMMATA -> AppIcons2.Calendar
        TripStatus.COMPLETATA -> AppIcons2.Check
        TripStatus.ANNULLATA -> AppIcons2.Close
    }
    val base = when (status) {
        TripStatus.BOZZA -> modifier.drawBehind {
            drawRoundRect(scheme.onSurfaceVariant, cornerRadius = CornerRadius(size.height / 2), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
        }
        TripStatus.COMPLETATA -> modifier.clip(shape).background(scheme.primary)
        else -> modifier.clip(shape).background(scheme.surface).border(1.5.dp, if (status == TripStatus.ANNULLATA) scheme.onSurfaceVariant else scheme.onSurface, shape)
    }
    val color = when (status) {
        TripStatus.COMPLETATA -> scheme.onPrimary
        TripStatus.PROGRAMMATA -> scheme.onSurface
        else -> scheme.onSurfaceVariant
    }
    Row(base.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Text(statusLabel(status), style = MaterialTheme.typography.labelSmall, color = color, textDecoration = if (status == TripStatus.ANNULLATA) TextDecoration.LineThrough else null)
    }
}

/** Scelta singola a pillole: l'opzione attiva ha anche il segno di spunta. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected, onClick = { onSelect(value) }, label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = if (value == selected) ({ Icon(AppIcons2.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }) else null,
                shape = CircleShape, modifier = Modifier.heightIn(min = 40.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface, labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary, selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = value == selected, borderColor = MaterialTheme.colorScheme.outline, borderWidth = 1.5.dp),
            )
        }
    }
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelMedium, modifier = modifier.padding(top = 16.dp, bottom = 6.dp))
}

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(112.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(52.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        action()
    }
}

/** Pulsante "+" in primo piano: un normale pulsante con testo, così è sempre letto dai lettori di schermo. */
@Composable
fun AddFab(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick, modifier = modifier.heightIn(min = 60.dp), shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
    ) {
        Icon(AppIcons2.Plus, contentDescription = null, modifier = Modifier.size(28.dp))
        Box(Modifier.size(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
