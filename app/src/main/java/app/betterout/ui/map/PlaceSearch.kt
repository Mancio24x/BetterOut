package app.betterout.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.betterout.R
import app.betterout.data.Place
import app.betterout.data.PlaceKind
import app.betterout.data.PlaceSearch
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.theme.AppIcons3
import app.betterout.ui.trips.currentLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

internal sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data object Failed : SearchState
    /** [more] è vero finché qualche fonte deve ancora rispondere. */
    data class Results(val places: List<Place>, val more: Boolean) : SearchState
}

/** Una ricerca alla volta: una nuova annulla la precedente. I risultati arrivano man mano che le fonti rispondono. */
internal class PlaceSearcher(private val scope: CoroutineScope) {
    var state by mutableStateOf<SearchState>(SearchState.Idle)
        private set
    private var job: Job? = null

    fun search(query: String) {
        if (query.isBlank()) return
        job?.cancel()
        state = SearchState.Loading
        job = scope.launch {
            try { PlaceSearch.progressive(query).collect { state = SearchState.Results(it.places, more = !it.done) } }
            catch (e: IOException) { state = SearchState.Failed }
        }
    }

    fun clear() {
        job?.cancel()
        state = SearchState.Idle
    }
}

@Composable
internal fun rememberPlaceSearcher(): PlaceSearcher {
    val scope = rememberCoroutineScope()
    return remember { PlaceSearcher(scope) }
}

/** Risultati, attesa ed errore di una ricerca; [hint] è il testo mostrato prima di cercare (nessuno se null). */
@Composable
internal fun PlaceResults(state: SearchState, hint: String?, onRetry: () -> Unit, onPick: (Place) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        when (state) {
            SearchState.Idle -> if (hint != null) Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            SearchState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp))
            SearchState.Failed -> {
                Text(stringResource(R.string.cerca_errore), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 12.dp))
                SecondaryButton(stringResource(R.string.riprova), onRetry)
            }
            is SearchState.Results -> {
                if (state.places.isEmpty() && !state.more) Text(stringResource(R.string.cerca_nessun_risultato), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
                state.places.forEach { place ->
                    Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(place) }.heightIn(min = 56.dp).padding(vertical = 8.dp)) {
                        Text(place.name, style = MaterialTheme.typography.titleMedium)
                        Text(placeSubtitle(place), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (state.more) Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.cerca_altri_in_arrivo), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(stringResource(R.string.cerca_fonte), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun placeSubtitle(place: Place): String {
    val locale = currentLocale()
    val kind = place.kind?.let {
        stringResource(
            when (it) {
                PlaceKind.CIMA -> R.string.luogo_cima; PlaceKind.RIFUGIO -> R.string.luogo_rifugio; PlaceKind.PASSO -> R.string.luogo_passo; PlaceKind.LAGO -> R.string.luogo_lago
                PlaceKind.LOCALITA -> R.string.luogo_localita; PlaceKind.VALLE -> R.string.luogo_valle; PlaceKind.GHIACCIAIO -> R.string.luogo_ghiacciaio; PlaceKind.CASCATA -> R.string.luogo_cascata
                PlaceKind.BELVEDERE -> R.string.luogo_belvedere; PlaceKind.PARCHEGGIO -> R.string.luogo_parcheggio; PlaceKind.IMPIANTO -> R.string.luogo_impianto
            },
        )
    }
    return listOfNotNull(kind, place.elevation?.let { formatMeters(it, locale) }, place.area, place.admin1, place.country).distinct().joinToString(" · ")
}

/** Ricerca di una località. La richiesta parte solo quando l'utente la chiede, non a ogni lettera. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSearchSheet(onDismiss: () -> Unit, onPick: (Place) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val searcher = rememberPlaceSearcher()
    fun search() = searcher.search(query)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding()) {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, shape = CircleShape,
                placeholder = { Text(stringResource(R.string.cerca_localita)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { search() }),
                trailingIcon = { IconButton(onClick = ::search) { Icon(AppIcons3.Search, contentDescription = stringResource(R.string.cerca)) } },
            )
            Column(Modifier.heightIn(min = 120.dp).verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
                PlaceResults(searcher.state, stringResource(R.string.cerca_aiuto), ::search, onPick)
            }
        }
    }
}
