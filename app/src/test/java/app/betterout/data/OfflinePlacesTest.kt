package app.betterout.data

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.io.IOException

class OfflinePlacesTest {
    private fun places(vararg lines: String) = OfflinePlaces { lines.joinToString("\n").byteInputStream() }

    private val sample = places(
        "R\t46.52773\t12.00813\tRifugio Lagazuoi",
        "C\t46.53689\t12.01886\tLagazuoi di Mezzo",
        "C\t45.83271\t6.86517\tMont Blanc / Monte Bianco",
        "R\t46.00000\t7.00000\tCabane du Vélan",
        "riga rotta",
    )

    @Test fun `le righe diventano luoghi con tipo, quelle rotte sono scartate`() {
        assertEquals(
            listOf(Place("Rifugio Lagazuoi", 46.52773, 12.00813, kind = PlaceKind.RIFUGIO), Place("Mont Blanc", 45.8, 6.8, kind = PlaceKind.CIMA)),
            OfflinePlaces.parse(sequenceOf("R\t46.52773\t12.00813\tRifugio Lagazuoi", "C\t45.8\t6.8\tMont Blanc", "X\t1\t2\tAltro", "C\tno\t6.8\tNome")).toList(),
        )
    }

    @Test fun `prima il nome che inizia come la ricerca, poi quello che la contiene come parola`() {
        assertEquals(listOf("Lagazuoi di Mezzo", "Rifugio Lagazuoi"), sample.search("lagazuoi").map { it.name })
    }

    @Test fun `la ricerca ignora maiuscole, accenti e punteggiatura e vuole tutte le parole`() {
        assertEquals(listOf("Cabane du Vélan"), sample.search("cabane velan").map { it.name })
        assertEquals(listOf("Mont Blanc / Monte Bianco"), sample.search("MONTE BIANCO").map { it.name })
        assertEquals(emptyList<Place>(), sample.search("rifugio bianco"))
        assertEquals(emptyList<Place>(), sample.search("  "))
    }

    @Test fun `i luoghi dell'app arrivano subito e bastano anche senza rete`() = runTest {
        val steps = PlaceSearch.progressive("lagazuoi", fetch = { throw IOException("senza rete") }, offline = sample).toList()
        assertEquals(listOf("Lagazuoi di Mezzo", "Rifugio Lagazuoi"), steps.first().places.map { it.name })
        assertEquals(false, steps.first().done)
        assertEquals(true, steps.last().done)
        assertEquals(steps.first().places, steps.last().places)
    }

    @Test fun `il file incluso nell'app si legge e contiene le cime e i rifugi principali`() {
        val file = File("src/main/assets/luoghi-alpi.tsv")
        val all = OfflinePlaces { file.inputStream() }
        assertEquals(PlaceKind.CIMA, all.search("Matterhorn").first { it.name == "Matterhorn" }.kind)
        assertEquals(PlaceKind.RIFUGIO, all.search("Rifugio Lagazuoi").first().kind)
        assertEquals(PlaceKind.CIMA, all.search("Gran Paradiso").first { it.name == "Gran Paradiso" }.kind)
    }
}
