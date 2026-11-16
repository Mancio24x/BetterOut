package app.betterout.ui.dive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.betterout.data.Dive
import app.betterout.data.DiveRepository
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
