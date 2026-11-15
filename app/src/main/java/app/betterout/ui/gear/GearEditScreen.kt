package app.betterout.ui.gear

import android.app.SearchManager
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.domain.GearFunction
import app.betterout.domain.GearKind
import app.betterout.ui.components.ChoiceChips
import app.betterout.ui.components.FieldLabel
import app.betterout.ui.components.PrimaryButton
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.theme.AppIcons2

@Composable
private fun ErrorText(text: String) = Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(AppIcons2.Warn, contentDescription = null, modifier = Modifier.size(15.dp)); Text(text) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GearEditScreen(vm: GearEditViewModel, onClose: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var askLeave by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf<String?>(null) }
    var functionMenu by remember { mutableStateOf(false) }
    val leave = { if (state.isDirty) askLeave = true else onClose() }
    BackHandler(enabled = state.isDirty) { askLeave = true }
    val shape = RoundedCornerShape(14.dp)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (vm.isNew) R.string.nuovo_oggetto else R.string.modifica_oggetto), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = leave) { Icon(AppIcons2.Close, contentDescription = stringResource(R.string.chiudi)) } },
                actions = { if (!vm.isNew) IconButton(onClick = { askDelete = true }) { Icon(AppIcons2.Trash, contentDescription = stringResource(R.string.elimina), tint = MaterialTheme.colorScheme.error) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = { Box(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp)) { PrimaryButton(stringResource(R.string.salva_oggetto), { vm.save(onClose) }, icon = AppIcons2.Check) } },
    ) { padding ->
        val item = state.item
        if (item == null) { Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            FieldLabel(stringResource(R.string.campo_nome_oggetto), Modifier.padding(top = 0.dp))
            OutlinedTextField(item.name, { v -> vm.edit { it.copy(name = v) } }, Modifier.fillMaxWidth(), singleLine = true, shape = shape, isError = state.nameError,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), supportingText = if (state.nameError) ({ ErrorText(stringResource(R.string.errore_nome_oggetto)) }) else null)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(R.string.campo_peso))
                    OutlinedTextField(state.weight, vm::setWeight, Modifier.fillMaxWidth(), singleLine = true, shape = shape, isError = state.weightError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = if (state.weightError) ({ ErrorText(stringResource(R.string.errore_numero)) }) else null)
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(R.string.campo_volume))
                    OutlinedTextField(state.volume, vm::setVolume, Modifier.fillMaxWidth(), singleLine = true, shape = shape, isError = state.volumeError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = if (state.volumeError) ({ ErrorText(stringResource(R.string.errore_numero)) }) else null)
                }
            }
            Row(Modifier.fillMaxWidth().toggleable(value = item.weightIsEstimate, role = Role.Switch) { on -> vm.edit { it.copy(weightIsEstimate = on) } }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.peso_e_stima), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.peso_e_stima_aiuto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = item.weightIsEstimate, onCheckedChange = null)
            }
            val query = stringResource(R.string.cerca_peso_query, item.name.trim())
            val noBrowser = stringResource(R.string.nessun_browser)
            TextButton(enabled = item.name.isNotBlank(), onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)) }.onFailure { Toast.makeText(context, noBrowser, Toast.LENGTH_LONG).show() }
            }) { Text(stringResource(R.string.cerca_peso_online), style = MaterialTheme.typography.labelLarge) }
            Text(stringResource(R.string.cerca_peso_aiuto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            FieldLabel(stringResource(R.string.campo_categoria))
            ChoiceChips(state.categories.map { it.id to it.name } + ("+" to stringResource(R.string.nuova_categoria)), item.categoryId, { v -> if (v == "+") newCategory = "" else vm.edit { it.copy(categoryId = v) } })
            FieldLabel(stringResource(R.string.campo_tipo))
            ChoiceChips(GearKind.entries.map { it to kindLabel(it) }, item.kind, { v -> vm.edit { it.copy(kind = v) } })
            FieldLabel(stringResource(R.string.campo_uso))
            Box {
                SecondaryButton(functionLabel(item.function), { functionMenu = true })
                DropdownMenu(functionMenu, { functionMenu = false }) {
                    GearFunction.entries.forEach { f -> DropdownMenuItem(text = { Text(functionLabel(f)) }, onClick = { functionMenu = false; vm.edit { it.copy(function = f) } }) }
                }
            }
            Text(stringResource(R.string.uso_aiuto), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            if (item.function == GearFunction.ZAINO) {
                FieldLabel(stringResource(R.string.campo_capacita))
                OutlinedTextField(state.capacity, vm::setCapacity, Modifier.fillMaxWidth(), singleLine = true, shape = shape, isError = state.capacityError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = if (state.capacityError) ({ ErrorText(stringResource(R.string.errore_numero)) }) else null)
            }
            FieldLabel(stringResource(R.string.campo_note))
            OutlinedTextField(item.notes, { v -> vm.edit { it.copy(notes = v) } }, Modifier.fillMaxWidth(), minLines = 2, shape = shape, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
        }
    }
    newCategory?.let { name ->
        AlertDialog(
            onDismissRequest = { newCategory = null }, title = { Text(stringResource(R.string.nuova_categoria), style = MaterialTheme.typography.headlineSmall) },
            text = { OutlinedTextField(name, { newCategory = it }, singleLine = true, label = { Text(stringResource(R.string.nome_categoria)) }, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)) },
            confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { vm.addCategory(name); newCategory = null }) { Text(stringResource(R.string.aggiungi)) } },
            dismissButton = { TextButton(onClick = { newCategory = null }) { Text(stringResource(R.string.annulla)) } },
        )
    }
    if (askDelete) state.item?.let { item ->
        AlertDialog(
            onDismissRequest = { askDelete = false }, icon = { Icon(AppIcons2.Trash, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.elimina_oggetto_titolo, item.name), style = MaterialTheme.typography.headlineSmall) },
            text = { Text(if (state.tripsUsing == 0) stringResource(R.string.elimina_oggetto_libero) else pluralStringResource(R.plurals.elimina_oggetto_in_uso, state.tripsUsing, state.tripsUsing), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { PrimaryButton(stringResource(R.string.elimina_dall_inventario), { askDelete = false; vm.delete(onClose) }, icon = AppIcons2.Trash, danger = true); SecondaryButton(stringResource(R.string.annulla), { askDelete = false }) } },
        )
    }
    if (askLeave) ModalBottomSheet(onDismissRequest = { askLeave = false }, containerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.non_salvate_titolo), style = MaterialTheme.typography.headlineSmall)
            PrimaryButton(stringResource(R.string.salva), { askLeave = false; vm.save(onClose) }, icon = AppIcons2.Check)
            SecondaryButton(stringResource(R.string.esci_senza_salvare), { askLeave = false; onClose() })
            TextButton(onClick = { askLeave = false }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.continua_a_modificare), style = MaterialTheme.typography.labelLarge) }
        }
    }
}
