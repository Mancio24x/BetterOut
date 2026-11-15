package app.betterout.domain

import kotlinx.serialization.Serializable

enum class LegMode { SENTIERO, MANUALE }

/**
 * Il tratto fra due punti di controllo consecutivi. [points] è null finché il tratto su sentiero non è stato calcolato
 * (o se il calcolo non è riuscito): un tratto così non viene disegnato né contato, e il percorso non si può salvare.
 */
@Serializable
data class Leg(val mode: LegMode, val points: List<GeoPoint>?)

/** Un percorso disegnato: punti di controllo e, fra ognuno e il successivo, un tratto. */
@Serializable
data class RouteDraft(val controls: List<GeoPoint> = emptyList(), val legs: List<Leg> = emptyList()) {
    init { require(legs.size == maxOf(0, controls.size - 1)) { "tratti e punti non corrispondono" } }

    val unresolved: List<Int> get() = legs.indices.filter { legs[it].points == null }
    val hasManualLegs: Boolean get() = legs.any { it.mode == LegMode.MANUALE }
    val isLoop: Boolean get() = controls.size > 2 && controls.first().sameSpot(controls.last())

    /**
     * Le linee continue formate dai tratti calcolati: una sola se non manca nessun tratto, altrimenti una per ogni pezzo,
     * così un tratto non calcolato non viene scavalcato con una retta. Alla giunzione fra due tratti il punto compare una
     * volta sola, tenendo la versione che ha la quota.
     */
    fun lines(): List<List<GeoPoint>> {
        val all = ArrayList<MutableList<GeoPoint>>()
        var current: MutableList<GeoPoint>? = null
        for (leg in legs) {
            val points = leg.points
            if (points == null) { current = null; continue }
            val line = current
            if (line == null) current = points.toMutableList().also(all::add)
            else {
                if (line.last().ele == null && points.first().ele != null) line[line.lastIndex] = points.first()
                line += points.drop(1)
            }
        }
        return all
    }
}

private fun GeoPoint.sameSpot(other: GeoPoint) = lat == other.lat && lon == other.lon

/** Operazioni dell'editor. Ognuna restituisce un nuovo percorso; i tratti su sentiero da ricalcolare hanno `points == null`. */
object RouteEditing {
    private fun leg(mode: LegMode, from: GeoPoint, to: GeoPoint) = Leg(mode, if (mode == LegMode.MANUALE) listOf(from, to) else null)

    fun add(d: RouteDraft, p: GeoPoint, mode: LegMode): RouteDraft =
        RouteDraft(d.controls + p, if (d.controls.isEmpty()) d.legs else d.legs + leg(mode, d.controls.last(), p))

    /** Sposta un punto: i tratti che lo toccano vanno rifatti, ognuno nella propria modalità. */
    fun move(d: RouteDraft, index: Int, p: GeoPoint): RouteDraft {
        val controls = d.controls.toMutableList().also { it[index] = p }
        val legs = d.legs.mapIndexed { i, l -> if (i == index - 1 || i == index) leg(l.mode, controls[i], controls[i + 1]) else l }
        return RouteDraft(controls, legs)
    }

    /** Elimina un punto. Se era intermedio, i due tratti diventano uno: su sentiero solo se lo erano entrambi. */
    fun delete(d: RouteDraft, index: Int): RouteDraft {
        val controls = d.controls.filterIndexed { i, _ -> i != index }
        val legs = when (index) {
            0 -> d.legs.drop(1)
            d.controls.lastIndex -> d.legs.dropLast(1)
            else -> {
                val mode = if (d.legs[index - 1].mode == LegMode.SENTIERO && d.legs[index].mode == LegMode.SENTIERO) LegMode.SENTIERO else LegMode.MANUALE
                d.legs.take(index - 1) + leg(mode, controls[index - 1], controls[index]) + d.legs.drop(index + 1)
            }
        }
        return RouteDraft(controls, legs)
    }

    /**
     * Inserisce un punto a metà del tratto che parte dal punto [index]. Il tracciato non cambia: il tratto viene solo diviso
     * in due, quindi non serve ricalcolare nulla. Restituisce null se quel tratto non c'è o non è ancora calcolato.
     */
    fun insertAfter(d: RouteDraft, index: Int): RouteDraft? {
        val leg = d.legs.getOrNull(index) ?: return null
        val points = leg.points ?: return null
        val first: List<GeoPoint>
        val second: List<GeoPoint>
        if (points.size > 2) {
            val half = Geo.length(points) / 2
            var travelled = 0.0
            var cut = 1
            while (cut < points.lastIndex - 1 && travelled + Geo.distance(points[cut - 1], points[cut]) < half) { travelled += Geo.distance(points[cut - 1], points[cut]); cut++ }
            first = points.subList(0, cut + 1)
            second = points.subList(cut, points.size)
        } else {
            val a = points.first()
            val b = points.last()
            val middle = GeoPoint((a.lat + b.lat) / 2, (a.lon + b.lon) / 2)
            first = listOf(a, middle)
            second = listOf(middle, b)
        }
        val middle = first.last().copy(ele = null)
        return RouteDraft(
            d.controls.take(index + 1) + middle + d.controls.drop(index + 1),
            d.legs.take(index) + Leg(leg.mode, first) + Leg(leg.mode, second) + d.legs.drop(index + 1),
        )
    }

    /** Fa finire il percorso al punto [index], togliendo quello che segue. */
    fun endAt(d: RouteDraft, index: Int): RouteDraft = RouteDraft(d.controls.take(index + 1), d.legs.take(index))

    fun reverse(d: RouteDraft): RouteDraft = RouteDraft(d.controls.reversed(), d.legs.reversed().map { it.copy(points = it.points?.reversed()) })

    /** Chiude ad anello tornando al primo punto. Senza effetto se i punti sono meno di due o l'anello è già chiuso. */
    fun closeLoop(d: RouteDraft, mode: LegMode): RouteDraft = if (d.controls.size < 2 || d.isLoop) d else add(d, d.controls.first(), mode)

    /**
     * Registra il risultato del routing per un tratto. Il servizio aggancia gli estremi al sentiero più vicino: i punti di
     * controllo si spostano lì, e i tratti manuali confinanti vengono riattaccati, così la linea resta continua.
     */
    fun resolve(d: RouteDraft, legIndex: Int, points: List<GeoPoint>): RouteDraft {
        require(points.size >= 2) { "un tratto ha almeno due punti" }
        val controls = d.controls.toMutableList()
        controls[legIndex] = points.first().copy(ele = null)
        controls[legIndex + 1] = points.last().copy(ele = null)
        val legs = d.legs.mapIndexed { i, l ->
            when {
                i == legIndex -> l.copy(points = points)
                l.mode == LegMode.MANUALE && (i == legIndex - 1 || i == legIndex + 1) -> Leg(LegMode.MANUALE, listOf(controls[i], controls[i + 1]))
                else -> l
            }
        }
        return RouteDraft(controls, legs)
    }

    /** L'utente sceglie di tracciare a mano un tratto: diventa una linea retta dichiarata come non verificata. */
    fun toManual(d: RouteDraft, legIndex: Int): RouteDraft =
        RouteDraft(d.controls, d.legs.mapIndexed { i, l -> if (i == legIndex) leg(LegMode.MANUALE, d.controls[i], d.controls[i + 1]) else l })
}
