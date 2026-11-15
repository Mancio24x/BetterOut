package app.betterout.ui.map

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.betterout.data.Route
import app.betterout.data.RouteGeometry
import app.betterout.data.RouteRepository
import app.betterout.data.RoutingService
import app.betterout.domain.GeoPoint
import app.betterout.domain.GpxError
import app.betterout.domain.GpxFile
import app.betterout.domain.GpxReader
import app.betterout.domain.GpxResult
import app.betterout.domain.LegMode
import app.betterout.domain.ProfilePoint
import app.betterout.domain.RouteDraft
import app.betterout.domain.RouteEditing
import app.betterout.domain.RouteMath
import app.betterout.domain.RouteStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import kotlin.coroutines.coroutineContext

/** Archivio dei percorsi e loro tracce sulla mappa. */
class RoutesViewModel(private val repo: RouteRepository) : ViewModel() {
    /** null finché il database non ha risposto. */
    val routes: StateFlow<List<Route>?> = repo.observeAll().map<List<Route>, List<Route>?> { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Tutti i percorsi salvati, semplificati per il disegno. */
    val lines: StateFlow<List<MapLine>> = repo.observeAll().map { list -> list.flatMap { repo.geometry(it.id)?.mapLines(toleranceMeters = 10.0).orEmpty() } }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun assignToTrip(tripId: String, routeId: String?, onDone: () -> Unit = {}) = viewModelScope.launch { repo.setTripRoute(tripId, routeId); onDone() }

    companion object { fun factory(repo: RouteRepository): ViewModelProvider.Factory = viewModelFactory { initializer { RoutesViewModel(repo) } } }
}

class RouteDetailViewModel(private val repo: RouteRepository, private val id: String) : ViewModel() {
    data class State(
        val loading: Boolean = true, val route: Route? = null, val geometry: RouteGeometry? = null, val profile: List<ProfilePoint> = emptyList(),
        val tripsUsing: Int = 0, val askingElevations: Boolean = false, val elevationError: Boolean = false,
    )

    private val busy = MutableStateFlow(false to false)

    val state: StateFlow<State> = combine(
        repo.observe(id).map { route ->
            val geometry = route?.let { repo.geometry(id) }
            Triple(route, geometry, geometry?.let { RouteMath.profile(it.lines) }.orEmpty())
        }.flowOn(Dispatchers.Default),
        repo.observeTripsUsing(id), busy,
    ) { (route, geometry, profile), uses, (asking, failed) -> State(false, route, geometry, profile, uses, asking, failed) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    fun rename(name: String) = viewModelScope.launch { repo.rename(id, name.trim()) }
    fun delete(onDone: () -> Unit) = viewModelScope.launch { repo.delete(id); onDone() }

    /** Chiede le quote al modello del terreno, solo su richiesta esplicita. */
    fun askElevations() {
        if (busy.value.first) return
        busy.value = true to false
        viewModelScope.launch {
            busy.value = try { repo.requestModelElevations(id); false to false } catch (e: IOException) { false to true }
        }
    }

    fun gpx(manualNote: String, onReady: (String) -> Unit) = viewModelScope.launch { repo.gpx(id, manualNote)?.let(onReady) }

    companion object { fun factory(repo: RouteRepository, id: String): ViewModelProvider.Factory = viewModelFactory { initializer { RouteDetailViewModel(repo, id) } } }
}

class GpxImportViewModel(private val app: Application, private val repo: RouteRepository, private val uri: Uri, private val tripId: String?) : ViewModel() {
    sealed interface State {
        /** [fraction] è la parte di file già letta, da 0 a 1. */
        data class Reading(val fileName: String?, val fraction: Float) : State
        /** [reason] null: il file non si è potuto aprire. */
        data class Failed(val fileName: String?, val reason: GpxError?) : State
        data class Preview(val fileName: String, val sizeBytes: Long, val file: GpxFile, val selected: Set<Int>, val saving: Boolean = false) : State {
            val baseName: String get() = fileName.substringBeforeLast('.')
            fun trackName(index: Int): String = file.tracks[index].name ?: if (file.tracks.size == 1) baseName else "$baseName (${index + 1})"
            val chosen get() = file.tracks.filterIndexed { i, _ -> i in selected }
            val stats: RouteStats get() = RouteMath.stats(chosen.flatMap { it.segments })
        }
    }

    private val _state = MutableStateFlow<State>(State.Reading(null, 0f))
    val state: StateFlow<State> = _state.asStateFlow()
    val forTrip: Boolean get() = tripId != null

    /** Copia privata del file scelto: è quella che viene letta e, al salvataggio, conservata senza modifiche. */
    private val copy = File(app.cacheDir, "importazione-${System.nanoTime()}.gpx")
    private val reading: Job = viewModelScope.launch { read() }

    private suspend fun read() {
        var name: String? = null
        _state.value = try {
            withContext(Dispatchers.IO) {
                app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) name = c.getString(0) }
                _state.value = State.Reading(name, 0f)
                (app.contentResolver.openInputStream(uri) ?: throw IOException("file non apribile")).use { input -> copy.outputStream().use { input.copyTo(it) } }
                val total = copy.length().coerceAtLeast(1)
                val context = coroutineContext
                val counted = object : FilterInputStream(copy.inputStream().buffered()) {
                    var done = 0L
                    override fun read(): Int = super.read().also { if (it >= 0) done++ }
                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        context.ensureActive()
                        return super.read(b, off, len).also { if (it > 0) { done += it; _state.value = State.Reading(name, done.toFloat() / total) } }
                    }
                }
                when (val result = counted.use { GpxReader.read(it) }) {
                    is GpxResult.Error -> State.Failed(name, result.reason)
                    is GpxResult.Ok -> State.Preview(name ?: "tracciato.gpx", total, result.file, result.file.tracks.indices.toSet())
                }
            }
        } catch (e: IOException) {
            State.Failed(name, null)
        } catch (e: SecurityException) {
            State.Failed(name, null)
        }
    }

    fun cancel() = reading.cancel()

    fun toggle(index: Int) = _state.update { s -> if (s is State.Preview && !s.saving) s.copy(selected = if (index in s.selected) s.selected - index else s.selected + index) else s }

    /** Salva i tracciati scelti nell'archivio; con [link] associa all'uscita il percorso, se è uno solo. */
    fun save(link: Boolean, onDone: (List<String>) -> Unit) {
        val s = _state.value as? State.Preview ?: return
        if (s.saving || s.selected.isEmpty()) return
        _state.value = s.copy(saving = true)
        viewModelScope.launch {
            val ids = s.selected.sorted().map { i -> repo.saveImported(s.trackName(i), s.file.tracks[i], s.file.waypoints, s.fileName, copy) }
            if (link && tripId != null && ids.size == 1) repo.setTripRoute(tripId, ids.single())
            onDone(ids)
        }
    }

    override fun onCleared() { copy.delete() }

    companion object {
        fun factory(app: Application, repo: RouteRepository, uri: Uri, tripId: String?): ViewModelProvider.Factory = viewModelFactory { initializer { GpxImportViewModel(app, repo, uri, tripId) } }
    }
}

class RouteEditorViewModel(private val repo: RouteRepository, private val routing: RoutingService, private val id: String?, private val tripId: String?) : ViewModel() {
    data class State(
        val loading: Boolean = true, val name: String = "", val draft: RouteDraft = RouteDraft(), val mode: LegMode = LegMode.SENTIERO,
        val selected: Int? = null, val moving: Boolean = false, val routing: Boolean = false,
        val canUndo: Boolean = false, val canRedo: Boolean = false, val dirty: Boolean = false, val tripsUsing: Int = 0,
    ) {
        /** Il tratto su sentiero che non si è riusciti a calcolare: finché c'è, decide l'utente cosa farne. */
        val failedLeg: Int? get() = if (routing) null else draft.unresolved.firstOrNull()
        val canSave: Boolean get() = !routing && draft.legs.isNotEmpty() && draft.unresolved.isEmpty()
        val locked: Boolean get() = loading || routing || failedLeg != null
        val stats: RouteStats = RouteMath.stats(draft.lines())
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    val isNew: Boolean get() = id == null

    private val undoStack = ArrayDeque<RouteDraft>()
    private val redoStack = ArrayDeque<RouteDraft>()
    private var saved = RouteDraft()

    init {
        viewModelScope.launch {
            val route = id?.let { repo.get(it) }
            saved = id?.let { repo.geometry(it)?.draft } ?: RouteDraft()
            _state.value = State(loading = false, name = route?.name.orEmpty(), draft = saved)
        }
        if (id != null) viewModelScope.launch { repo.observeTripsUsing(id).collect { n -> _state.update { it.copy(tripsUsing = n) } } }
    }

    fun setMode(mode: LegMode) = _state.update { it.copy(mode = mode) }
    fun select(index: Int?) = _state.update { if (it.locked) it else it.copy(selected = if (index == it.selected) null else index, moving = false) }
    fun startMove() = _state.update { it.copy(moving = it.selected != null) }

    /** Tocco sulla mappa: sposta il punto se si è scelto "Sposta", toglie la selezione se c'è, altrimenti aggiunge un punto. */
    fun tap(p: GeoPoint) {
        val s = _state.value
        if (s.locked) return
        when {
            s.moving && s.selected != null -> change(RouteEditing.move(s.draft, s.selected, p), s.selected)
            s.selected != null -> select(null)
            else -> change(RouteEditing.add(s.draft, p, s.mode), null)
        }
    }

    fun drag(index: Int, p: GeoPoint) = edit { change(RouteEditing.move(it.draft, index, p), index) }
    fun insertAfterSelected() = edit { s -> s.selected?.let { i -> RouteEditing.insertAfter(s.draft, i)?.let { change(it, i + 1) } } }
    fun endAtSelected() = edit { s -> s.selected?.let { change(RouteEditing.endAt(s.draft, it), null) } }
    fun deleteSelected() = edit { s -> s.selected?.let { change(RouteEditing.delete(s.draft, it), null) } }
    fun reverse() = edit { change(RouteEditing.reverse(it.draft), null) }
    fun closeLoop() = edit { change(RouteEditing.closeLoop(it.draft, it.mode), null) }

    fun undo() {
        if (_state.value.routing || undoStack.isEmpty()) return
        redoStack.addLast(_state.value.draft)
        show(undoStack.removeLast(), null)
    }

    fun redo() {
        if (_state.value.locked || redoStack.isEmpty()) return
        undoStack.addLast(_state.value.draft)
        show(redoStack.removeLast(), null)
        resolve()
    }

    /** Dopo un errore di routing: riprova lo stesso tratto. */
    fun retry() = resolve()

    /** Dopo un errore di routing: l'utente sceglie di tracciare a mano quel tratto. */
    fun failedToManual() {
        val s = _state.value
        val leg = s.failedLeg ?: return
        show(RouteEditing.toManual(s.draft, leg), s.selected)
        resolve()
    }

    fun save(name: String, onDone: (String) -> Unit) {
        val s = _state.value
        if (!s.canSave || name.isBlank()) return
        viewModelScope.launch {
            val routeId = repo.saveDrawn(id, name.trim(), s.draft)
            if (tripId != null) repo.setTripRoute(tripId, routeId)
            onDone(routeId)
        }
    }

    private inline fun edit(action: (State) -> Unit) { val s = _state.value; if (!s.locked) action(s) }

    private fun change(next: RouteDraft, selected: Int?) {
        val current = _state.value.draft
        if (next == current) return
        undoStack.addLast(current)
        redoStack.clear()
        show(next, selected)
        resolve()
    }

    private fun show(draft: RouteDraft, selected: Int?) = _state.update {
        it.copy(draft = draft, selected = selected, moving = false, canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty(), dirty = draft != saved)
    }

    /** Calcola uno alla volta i tratti su sentiero mancanti. Se uno non riesce si ferma: non lo sostituisce con una retta. */
    private fun resolve() {
        if (_state.value.routing || _state.value.draft.unresolved.isEmpty()) return
        _state.update { it.copy(routing = true) }
        viewModelScope.launch {
            try {
                while (true) {
                    val draft = _state.value.draft
                    val leg = draft.unresolved.firstOrNull() ?: break
                    val points = routing.route(draft.controls[leg], draft.controls[leg + 1])
                    _state.update { it.copy(draft = RouteEditing.resolve(it.draft, leg, points)) }
                }
            } catch (e: IOException) {
                // Il tratto resta non calcolato: la schermata mostra l'errore e le tre scelte.
            }
            _state.update { it.copy(routing = false, dirty = it.draft != saved) }
        }
    }

    companion object {
        fun factory(repo: RouteRepository, routing: RoutingService, id: String?, tripId: String?): ViewModelProvider.Factory = viewModelFactory { initializer { RouteEditorViewModel(repo, routing, id, tripId) } }
    }
}
