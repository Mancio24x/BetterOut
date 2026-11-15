package app.betterout.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.betterout.data.Trip
import app.betterout.data.TripRepository
import app.betterout.data.TripStatus
import app.betterout.domain.TripRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class TripsViewModel(repo: TripRepository) : ViewModel() {
    /** null finché il database non ha risposto: distingue il caricamento dall'elenco vuoto. */
    val trips: StateFlow<List<Trip>?> = repo.observeAll().map<List<Trip>, List<Trip>?> { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        fun factory(repo: TripRepository): ViewModelProvider.Factory = viewModelFactory { initializer { TripsViewModel(repo) } }
    }
}

class TripDetailViewModel(private val repo: TripRepository, private val id: String) : ViewModel() {
    sealed interface State { data object Loading : State; data object Missing : State; data class Ready(val trip: Trip) : State }

    val state: StateFlow<State> = repo.observe(id).map { if (it == null) State.Missing else State.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State.Loading)

    fun setStatus(status: TripStatus) = viewModelScope.launch { repo.setStatus(id, status) }
    fun delete(onDone: () -> Unit) = viewModelScope.launch { repo.delete(id); onDone() }
    fun duplicate(title: String, date: LocalDate?, onDone: (String) -> Unit) = viewModelScope.launch { repo.duplicate(id, title, date)?.let(onDone) }

    companion object {
        fun factory(repo: TripRepository, id: String): ViewModelProvider.Factory = viewModelFactory { initializer { TripDetailViewModel(repo, id) } }
    }
}

class TripEditViewModel(private val repo: TripRepository, private val id: String?) : ViewModel() {
    data class State(val loading: Boolean = true, val original: Trip? = null, val draft: Trip? = null, val nameError: Boolean = false) {
        val isDirty: Boolean get() = draft != null && draft != original
        val datesValid: Boolean get() = draft == null || TripRules.datesAreValid(draft.startDate, draft.endDate)
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    val isNew: Boolean get() = id == null

    init {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val trip = id?.let { repo.get(it) } ?: Trip(id = UUID.randomUUID().toString(), title = "", timeZoneId = ZoneId.systemDefault().id, createdAt = now, updatedAt = now)
            _state.value = State(loading = false, original = trip, draft = trip)
        }
    }

    fun edit(change: (Trip) -> Trip) = _state.update { s -> s.draft?.let { s.copy(draft = change(it), nameError = false) } ?: s }

    /** Salva. Con [asDraft] lo stato diventa bozza; un'uscita nuova con una data viene proposta come programmata. */
    fun save(asDraft: Boolean = false, onDone: (String) -> Unit) {
        val s = _state.value
        val draft = s.draft ?: return
        if (draft.title.isBlank()) { _state.update { it.copy(nameError = true) }; return }
        if (!s.datesValid) return
        val cleaned = draft.copy(title = draft.title.trim(), placeName = draft.placeName.trim(), status = if (asDraft) TripStatus.BOZZA else draft.status)
        viewModelScope.launch { repo.save(cleaned); onDone(cleaned.id) }
    }

    companion object {
        fun factory(repo: TripRepository, id: String?): ViewModelProvider.Factory = viewModelFactory { initializer { TripEditViewModel(repo, id) } }
    }
}
