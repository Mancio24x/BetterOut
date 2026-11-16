package app.betterout.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.data.AppSettings
import app.betterout.data.ThemeMode
import app.betterout.domain.WeightUnit
import app.betterout.ui.components.AppCard
import app.betterout.ui.components.ChoiceChips
import app.betterout.ui.map.GPX_MIME_TYPES
import app.betterout.ui.map.OptionRow
import app.betterout.ui.theme.AppIcons
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/** [onImportGpx] e [onRoutes] null nascondono la sezione dei percorsi, che nella modalità immersioni non serve. */
fun SettingsScreen(settings: AppSettings, onBack: () -> Unit, onImportGpx: ((Uri) -> Unit)?, onRoutes: (() -> Unit)?) {
    val theme by settings.theme.collectAsStateWithLifecycle()
    val unit by settings.weightUnit.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) onImportGpx?.invoke(uri) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_impostazioni), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons2.Back, contentDescription = stringResource(R.string.indietro)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LocalModeSwitch.current?.let { mode ->
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.impostazioni_modalita), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                        ChoiceChips(
                            listOf(false to stringResource(R.string.modalita_escursioni), true to stringResource(R.string.modalita_immersioni)),
                            mode.diving, { diving -> if (diving != mode.diving) mode.toggle() },
                        )
                    }
                }
            }
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.impostazioni_tema), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    ChoiceChips(
                        listOf(ThemeMode.SISTEMA to stringResource(R.string.tema_sistema), ThemeMode.CHIARO to stringResource(R.string.tema_chiaro), ThemeMode.NERO to stringResource(R.string.tema_nero)),
                        theme, settings::setTheme,
                    )
                    Text(stringResource(R.string.tema_nota), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.unita_peso), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    ChoiceChips(listOf(WeightUnit.G to stringResource(R.string.unita_grammi), WeightUnit.KG to stringResource(R.string.unita_chilogrammi)), unit, settings::setWeightUnit)
                }
            }
            if (onImportGpx != null && onRoutes != null) {
                Text(stringResource(R.string.impostazioni_dati), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
                OptionRow(AppIcons3.Download, stringResource(R.string.importa_titolo), stringResource(R.string.importa_testo), { picker.launch(GPX_MIME_TYPES) })
                OptionRow(AppIcons.Map, stringResource(R.string.impostazioni_percorsi), stringResource(R.string.impostazioni_percorsi_testo), onRoutes)
            }
            Text(stringResource(R.string.impostazioni_info), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
            Text(stringResource(R.string.impostazioni_versione, version), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.impostazioni_fonti), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
