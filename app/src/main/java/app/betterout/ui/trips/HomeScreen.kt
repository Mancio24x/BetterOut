package app.betterout.ui.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.data.Trip
import app.betterout.data.TripStatus
import app.betterout.domain.TripRules
import app.betterout.ui.components.AppCard
import app.betterout.ui.components.EmptyState
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.components.StatusChip
import app.betterout.ui.theme.AppIcons
import app.betterout.ui.theme.AppIcons2
import java.time.LocalDate

@Composable
fun HomeScreen(vm: TripsViewModel, onNew: () -> Unit, onOpen: (String) -> Unit, onSeeAll: () -> Unit) {
    val trips by vm.trips.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    Box(Modifier.fillMaxSize()) {
        val list = trips
        when {
            list == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            list.isEmpty() -> Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                HomeTitle()
                Spacer(Modifier.weight(1f))
                EmptyState(AppIcons.Mountain, stringResource(R.string.home_vuota_titolo), stringResource(R.string.home_vuota_testo)) {
                    PrimaryButton(stringResource(R.string.nuova_uscita), onNew, icon = AppIcons2.Plus)
                }
                Spacer(Modifier.weight(1.4f))
            }
            else -> {
                val next = TripRules.next(list, today)
                val others = TripRules.sorted(list.filter { it.id != next?.id }, today)
                LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp)) {
                    item { HomeTitle() }
                    if (next != null) item { NextTripCard(next, today) { onOpen(next.id) } }
                    if (others.isNotEmpty()) {
                        item {
                            Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.altre_uscite), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).semantics { heading() })
                                TextButton(onClick = onSeeAll) { Text(stringResource(R.string.vedi_tutte, list.size), style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                        items(others.take(4), key = { it.id }) { TripRow(it, today) { onOpen(it.id) } }
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = onNew, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary, shape = RoundedCornerShape(22.dp),
                    icon = { Icon(AppIcons2.Plus, contentDescription = null, modifier = Modifier.size(28.dp)) },
                    text = { Text(stringResource(R.string.nuova_uscita), style = MaterialTheme.typography.titleMedium) },
                )
            }
        }
    }
}

@Composable
private fun HomeTitle() {
    Text(stringResource(R.string.home_titolo), style = MaterialTheme.typography.displayLarge, modifier = Modifier.padding(top = 12.dp, bottom = 16.dp).semantics { heading() })
}

@Composable
private fun NextTripCard(trip: Trip, today: LocalDate, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AppCard(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).clickable(role = Role.Button, onClick = onClick), color = scheme.primary, contentColor = scheme.onPrimary) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (trip.status == TripStatus.PROGRAMMATA) R.string.prossima_uscita else R.string.ultima_bozza), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                tripCountdown(trip, today)?.let {
                    Row(Modifier.clip(CircleShape).background(scheme.onPrimary).padding(horizontal = 11.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(AppIcons2.Calendar, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(15.dp))
                        Text(it, style = MaterialTheme.typography.labelSmall, color = scheme.primary)
                    }
                }
            }
            Text(trip.title, style = MaterialTheme.typography.headlineMedium)
            Text(tripWhen(trip), style = MaterialTheme.typography.bodyMedium)
            Text(trip.placeName.ifBlank { stringResource(R.string.senza_localita) }, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun TripRow(trip: Trip, today: LocalDate, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onClick), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(trip.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    if (TripRules.isOverdue(trip, today)) stringResource(R.string.data_passata) else listOfNotNull(tripWhen(trip), trip.placeName.takeIf { it.isNotBlank() }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(trip.status)
        }
    }
}
