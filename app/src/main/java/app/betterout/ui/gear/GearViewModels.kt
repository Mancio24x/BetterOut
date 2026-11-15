package app.betterout.ui.gear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.betterout.data.ChecklistEntry
import app.betterout.data.GearCategory
import app.betterout.data.GearItem
import app.betterout.data.GearRepository
import app.betterout.data.Trip
import app.betterout.data.TripRepository
import app.betterout.domain.GearFunction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class InventoryState(val loading: Boolean = true, val items: List<GearItem> = emptyList(), val categories: List<GearCategory> = emptyList())

class InventoryViewModel(private val repo: GearRepository) : ViewModel() {
    val state: StateFlow<InventoryState> = combine(repo.observeItems(), repo.observeCategories()) { i, c -> InventoryState(false, i, c) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryState())

    init { viewModelScope.launch { repo.ensureDefaultCategories() } }

    companion object { fun factory(repo: GearRepository): ViewModelProvider.Factory = viewModelFactory { initializer { InventoryViewModel(repo) } } }
}

class GearEditViewModel(private val repo: GearRepository, private val id: String?, private val template: GearItem? = null, private val startInKg: Boolean = false) : ViewModel() {
    /** I numeri sono tenuti come testo finché non si salva, così un valore non valido resta visibile e segnalato. */
    data class State(
        val loading: Boolean = true, val item: GearItem? = null, val original: State.Snapshot? = null, val categories: List<GearCategory> = emptyList(),
        val weight: String = "", val volume: String = "", val capacity: String = "", val nameError: Boolean = false, val tripsUsing: Int = 0,
        /** Il peso scritto nel campo è in chilogrammi (con decimali) invece che in grammi. */
        val kg: Boolean = false,
    ) {
        data class Snapshot(val item: GearItem, val weight: String, val volume: String, val capacity: String)
        val snapshot: Snapshot? get() = item?.let { Snapshot(it, weight, volume, capacity) }
        val isDirty: Boolean get() = snapshot != original
        val weightError: Boolean get() = weight.isNotBlank() && grams(weight, kg) == null
        val volumeError: Boolean get() = volume.isNotBlank() && parseDecimal(volume) == null
        val capacityError: Boolean get() = capacity.isNotBlank() && parseDecimal(capacity) == null
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    val isNew: Boolean get() = id == null

    init {
        viewModelScope.launch {
            repo.ensureDefaultCategories()
            val categories = repo.categories()
            val now = System.currentTimeMillis()
            val item = id?.let { repo.item(it) } ?: (template ?: GearItem(id = "", name = "", categoryId = "", createdAt = now, updatedAt = now)).copy(id = UUID.randomUUID().toString(), categoryId = categories.first().id, createdAt = now, updatedAt = now)
            val s = State(false, item, null, categories, weightText(item.weightGrams, startInKg), item.volumeLiters?.let(::plain).orEmpty(), item.capacityLiters?.let(::plain).orEmpty(), tripsUsing = id?.let { repo.tripsUsing(it) } ?: 0, kg = startInKg)
            _state.value = s.copy(original = s.snapshot)
        }
    }

    fun edit(change: (GearItem) -> GearItem) = _state.update { s -> s.item?.let { s.copy(item = change(it), nameError = false) } ?: s }
    fun setWeight(v: String) = _state.update { it.copy(weight = v) }
    /** Cambia l'unità del campo del peso, convertendo quello che c'è scritto. */
    fun setUnit(kg: Boolean) = _state.update { s ->
        if (s.kg == kg) s else s.copy(
            kg = kg, weight = grams(s.weight, s.kg)?.let { weightText(it, kg) } ?: s.weight,
            original = s.original?.let { o -> o.copy(weight = grams(o.weight, s.kg)?.let { weightText(it, kg) } ?: o.weight) },
        )
    }

    fun setVolume(v: String) = _state.update { it.copy(volume = v) }
    fun setCapacity(v: String) = _state.update { it.copy(capacity = v) }

    fun addCategory(name: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        val category = repo.addCategory(name)
        _state.update { it.copy(categories = repo.categories(), item = it.item?.copy(categoryId = category.id)) }
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val item = s.item ?: return
        if (item.name.isBlank()) { _state.update { it.copy(nameError = true) }; return }
        if (s.weightError || s.volumeError || s.capacityError) return
        val cleaned = item.copy(
            name = item.name.trim(), weightGrams = grams(s.weight, s.kg), volumeLiters = parseDecimal(s.volume),
            capacityLiters = if (item.function == GearFunction.ZAINO) parseDecimal(s.capacity) else null,
            weightIsEstimate = item.weightIsEstimate && s.weight.isNotBlank(),
        )
        viewModelScope.launch { repo.saveItem(cleaned); onDone() }
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch { id?.let { repo.deleteItem(it) }; onDone() }

    companion object {
        fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 && it.isFinite() }
        /** Grammi dal testo del campo: interi se in grammi, con decimali se in chilogrammi. Null se vuoto o non valido. */
        fun grams(text: String, kg: Boolean): Int? =
            if (kg) parseDecimal(text)?.let { Math.round(it * 1000).toInt() } else text.trim().toIntOrNull()?.takeIf { it >= 0 }
        fun weightText(grams: Int?, kg: Boolean): String = when {
            grams == null -> ""
            kg -> java.math.BigDecimal(grams).movePointLeft(3).stripTrailingZeros().toPlainString().replace('.', ',')
            else -> grams.toString()
        }
        private fun plain(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString().replace('.', ',')
        /** [template] dà nome, uso e tipo di partenza a un oggetto nuovo scelto dall'elenco proposto. */
        fun factory(repo: GearRepository, id: String?, template: GearItem? = null, kg: Boolean = false): ViewModelProvider.Factory = viewModelFactory { initializer { GearEditViewModel(repo, id, template, kg) } }
    }
}

data class ChecklistState(val loading: Boolean = true, val trip: Trip? = null, val entries: List<ChecklistEntry> = emptyList(), val items: List<GearItem> = emptyList(), val categories: List<GearCategory> = emptyList())

class ChecklistViewModel(private val gear: GearRepository, private val trips: TripRepository, private val tripId: String) : ViewModel() {
    val state: StateFlow<ChecklistState> = combine(trips.observe(tripId), gear.observeChecklist(tripId), gear.observeItems(), gear.observeCategories()) { t, e, i, c -> ChecklistState(false, t, e, i, c) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChecklistState())

    fun setPacked(entry: ChecklistEntry, packed: Boolean) = viewModelScope.launch { gear.setPacked(entry.id, packed) }
    fun update(entry: ChecklistEntry) = viewModelScope.launch { gear.updateEntry(entry) }
    fun remove(entry: ChecklistEntry) = viewModelScope.launch { gear.removeEntry(entry.id) }
    fun clearPacked() = viewModelScope.launch { gear.clearPacked(tripId) }
    fun add(itemIds: List<String>, onDone: () -> Unit) = viewModelScope.launch { gear.addToChecklist(tripId, itemIds); onDone() }
    fun changeWater(deltaLiters: Double) = viewModelScope.launch {
        trips.get(tripId)?.let { trips.save(it.copy(waterLiters = (it.waterLiters + deltaLiters).coerceIn(0.0, 20.0))) }
    }

    companion object {
        fun factory(gear: GearRepository, trips: TripRepository, tripId: String): ViewModelProvider.Factory = viewModelFactory { initializer { ChecklistViewModel(gear, trips, tripId) } }
    }
}
