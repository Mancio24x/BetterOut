package app.betterout.data

import java.io.InputStream
import java.text.Normalizer

/**
 * Cime e rifugi delle Alpi italiane, francesi e svizzere, inclusi nell'app: si trovano subito e anche senza rete.
 * Il file si genera con `tools/luoghi/genera.py` (dati © OpenStreetMap contributors, ODbL).
 */
class OfflinePlaces(private val open: () -> InputStream) {
    private class Entry(val place: Place, val key: String)

    private val entries: List<Entry> by lazy {
        open().bufferedReader().useLines { lines -> lines.mapNotNull(::entry).toList() }
    }

    /** Legge il file subito; l'app lo chiama in background all'avvio, così la prima ricerca non aspetta. */
    fun warmUp() {
        entries.size
    }

    /** I luoghi il cui nome contiene tutte le parole cercate, prima quelli che iniziano come la ricerca. */
    fun search(query: String, limit: Int = 10): List<Place> {
        val q = normalize(query)
        val words = q.split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        return entries.asSequence()
            .filter { e -> words.all { it in e.key } }
            .sortedWith(compareBy<Entry>({ score(it.key, q, words.first()) }, { it.key.length }))
            .take(limit).map { it.place }.toList()
    }

    private fun score(key: String, q: String, first: String) = when {
        key == q -> 0
        key.startsWith(q) -> 1
        key.split(' ').any { it.startsWith(first) } -> 2
        else -> 3
    }

    companion object {
        /** Minuscole, senza accenti, solo lettere e cifre separate da uno spazio: "Rifugio Città di Fiume" → "rifugio citta di fiume". */
        fun normalize(text: String): String = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

        /** Una riga per luogo: tipo (C cima, R rifugio), latitudine, longitudine, nome, separati da tabulazione. */
        fun parse(lines: Sequence<String>): Sequence<Place> = lines.mapNotNull { entry(it)?.place }

        /**
         * La quinta colonna, se c'è, è il nome già normalizzato: calcolarlo all'apertura per 30 000 luoghi
         * richiedeva secondi sul telefono.
         */
        private fun entry(line: String): Entry? {
            val p = line.split('\t')
            if (p.size != 4 && p.size != 5) return null
            val kind = when (p[0]) { "C" -> PlaceKind.CIMA; "R" -> PlaceKind.RIFUGIO; else -> return null }
            val lat = p[1].toDoubleOrNull() ?: return null
            val lon = p[2].toDoubleOrNull() ?: return null
            return Entry(Place(p[3], lat, lon, kind = kind), p.getOrNull(4) ?: normalize(p[3]))
        }
    }
}
