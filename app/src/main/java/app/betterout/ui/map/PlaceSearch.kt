package app.betterout.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import app.betterout.data.PlaceSearch
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.theme.AppIcons3
import app.betterout.ui.trips.currentLocale
import kotlinx.coroutines.launch
import java.io.IOException

private sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data object Failed : SearchState
    data class Results(val places: List<Place>) : SearchState
}

@Composable
fun placeSubtitle(place: Place): String {
    val locale = currentLocale()
    return listOfNotNull(place.elevation?.let { formatMeters(it, locale) }, place.admin1, place.country).joinToString(" · ")
}

/** Ricerca di una località. La richiesta parte solo quando l'utente la chiede, non a ogni lettera. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSearchSheet(onDismiss: () -> Unit, onPick: (Place) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var state by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    val scope = rememberCoroutineScope()
    fun search() {
        if (query.isBlank()) return
        state = SearchState.Loading
        scope.launch { state = try { SearchState.Results(PlaceSearch.search(query)) } catch (e: IOException) { SearchState.Failed } }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding()) {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, shape = CircleShape,
                placeholder = { Text(stringResource(R.string.cerca_localita)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { search() }),
                trailingIcon = { IconButton(onClick = ::search) { Icon(AppIcons3.Search, contentDescription = stringResource(R.string.cerca)) } },
            )
            Column(Modifier.heightIn(min = 120.dp).verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
                when (val s = state) {
                    SearchState.Idle -> Text(stringResource(R.string.cerca_aiuto), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    SearchState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp))
                    SearchState.Failed -> {
                        Text(stringResource(R.string.cerca_errore), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 12.dp))
                        SecondaryButton(stringResource(R.string.riprova), ::search)
                    }
                    is SearchState.Results -> {
                        if (s.places.isEmpty()) Text(stringResource(R.string.cerca_nessun_risultato), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
                        s.places.forEach { place ->
                            Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(place) }.heightIn(min = 56.dp).padding(vertical = 8.dp)) {
                                Text(place.name, style = MaterialTheme.typography.titleMedium)
                                Text(placeSubtitle(place), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
                Text(stringResource(R.string.cerca_fonte), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}
