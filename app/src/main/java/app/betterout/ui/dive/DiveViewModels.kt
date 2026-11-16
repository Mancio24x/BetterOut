package app.betterout.ui.dive

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.betterout.data.Dive
import app.betterout.data.DiveRepository
import app.betterout.domain.DiveFileFormat
import app.betterout.domain.DiveImport
import app.betterout.domain.DiveImportError
import app.betterout.domain.DiveImportResult
import app.betterout.domain.DiveRules
import app.betterout.domain.DiveSample
import app.betterout.domain.ImportedDive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DiveLogViewModel(repo: DiveRepository) : ViewModel() {
    /** null mentre si legge il database. */
    val dives: StateFlow<List<Dive>?> = repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        fun factory(repo: DiveRepository): ViewModelProvider.Factory = viewModelFactory { initializer { DiveLogViewModel(repo) } }
    }
}

class DiveDetailViewModel(private val repo: DiveRepository, val id: String) : ViewModel() {
    val dive: StateFlow<Dive?> = repo.observe(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _profile = MutableStateFlow<List<DiveSample>>(emptyList())
    /** Vuoto per le immersioni scritte a mano. */
    val profile: StateFlow<List<DiveSample>> = _profile.asStateFlow()

    init {
        viewModelScope.launch { _profile.value = repo.profile(id) }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch { repo.delete(id); onDone() }
    }

    companion object {
        fun factory(repo: DiveRepository, id: String): ViewModelProvider.Factory = viewModelFactory { initializer { DiveDetailViewModel(repo, id) } }
    }
}

class DiveEditViewModel(private val repo: DiveRepository, private val id: String?) : ViewModel() {
    data class State(val original: Dive? = null, val draft: Dive? = null, val invalid: Set<String> = emptySet()) {
        val isDirty: Boolean get() = draft != null && draft != original
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    val isNew: Boolean get() = id == null

    init {
        viewModelScope.launch {
            val dive = id?.let { repo.get(it) } ?: repo.blank()
            _state.value = State(original = dive, draft = dive)
        }
    }

    fun edit(change: (Dive) -> Dive) = _state.update { s -> s.draft?.let { s.copy(draft = change(it)) } ?: s }

    /** I campi con un testo che non è un numero (o un'ora) valido: finché ce n'è uno non si salva. */
    fun setInvalid(field: String, invalid: Boolean) = _state.update { it.copy(invalid = if (invalid) it.invalid + field else it.invalid - field) }

    fun save(onDone: (String) -> Unit) {
        val s = _state.value
        val draft = s.draft ?: return
        if (s.invalid.isNotEmpty()) return
        val cleaned = draft.copy(siteName = draft.siteName.trim(), buddy = draft.buddy.trim(), notes = draft.notes.trim())
        viewModelScope.launch { repo.save(cleaned); onDone(cleaned.id) }
    }

    companion object {
        fun factory(repo: DiveRepository, id: String?): ViewModelProvider.Factory = viewModelFactory { initializer { DiveEditViewModel(repo, id) } }
    }
}

class DiveImportViewModel(private val app: Application, private val repo: DiveRepository, private val uri: Uri) : ViewModel() {
    sealed interface State {
        data object Reading : State
        /** [reason] null: il file non si è potuto aprire o è troppo grande. */
        data class Failed(val fileName: String?, val reason: DiveImportError?) : State
        data class Preview(val fileName: String?, val format: DiveFileFormat, val dives: List<ImportedDive>, val duplicates: Set<Int>, val selected: Set<Int>, val saving: Boolean = false) : State
    }

    private val _state = MutableStateFlow<State>(State.Reading)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.value = read() }
    }

    private suspend fun read(): State {
        var name: String? = null
        val bytes = try {
            withContext(Dispatchers.IO) {
                app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) name = c.getString(0) }
                (app.contentResolver.openInputStream(uri) ?: throw IOException("file non apribile")).use { input ->
                    // Un logbook anche di migliaia di immersioni sta in pochi MB: oltre questo limite non è un file da leggere.
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        if (out.size() > MAX_BYTES) throw IOException("file troppo grande")
                    }
                    out.toByteArray()
                }
            }
        } catch (e: IOException) {
            return State.Failed(name, null)
        } catch (e: SecurityException) {
            return State.Failed(name, null)
        }
        return when (val result = withContext(Dispatchers.Default) { DiveImport.read(bytes) }) {
            is DiveImportResult.Error -> State.Failed(name, result.reason)
            is DiveImportResult.Ok -> {
                val existing = repo.all()
                val dives = result.dives.sortedWith(compareByDescending<ImportedDive> { it.date }.thenByDescending { it.startTime })
                val duplicates = dives.indices.filter { DiveRules.isDuplicate(existing, dives[it]) }.toSet()
                State.Preview(name, result.format, dives, duplicates, selected = dives.indices.toSet() - duplicates)
            }
        }
    }

    fun toggle(index: Int) = _state.update { s ->
        if (s is State.Preview && !s.saving) s.copy(selected = if (index in s.selected) s.selected - index else s.selected + index) else s
    }

    fun import(onDone: (Int) -> Unit) {
        val s = _state.value as? State.Preview ?: return
        if (s.saving || s.selected.isEmpty()) return
        _state.value = s.copy(saving = true)
        viewModelScope.launch { onDone(repo.import(s.dives.filterIndexed { i, _ -> i in s.selected })) }
    }

    companion object {
        private const val MAX_BYTES = 64 * 1024 * 1024

        fun factory(app: Application, repo: DiveRepository, uri: Uri): ViewModelProvider.Factory = viewModelFactory { initializer { DiveImportViewModel(app, repo, uri) } }
    }
}
