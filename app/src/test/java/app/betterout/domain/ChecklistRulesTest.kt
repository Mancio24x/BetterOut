package app.betterout.domain

import app.betterout.data.ChecklistEntry
import app.betterout.data.GearCategory
import app.betterout.data.GearItem
import app.betterout.data.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ChecklistRulesTest {
    private val strati = GearCategory("c1", "Strati", 3)
    private val giacca = GearItem("g1", "Giacca hardshell", 380, false, "c1", GearKind.BASE, GearFunction.GUSCIO_PIOGGIA, 1.2, null, "", 0, 0)
    private fun entry(id: String, name: String, grams: Int?, qty: Int = 1, kind: GearKind = GearKind.BASE, packed: Boolean = false, cat: String = "Strati", order: Int = 3, volume: Double? = null, function: GearFunction = GearFunction.ALTRO) =
        ChecklistEntry(id, "t1", null, name, grams, false, cat, order, kind, function, volume, qty, packed, "", 0)

    @Test fun `la riga copia i dati dell'oggetto e ne conserva il collegamento`() {
        val e = ChecklistRules.entryFrom(giacca, strati, "t1", "e1", 10)
        assertEquals("g1", e.gearItemId); assertEquals("Giacca hardshell", e.name); assertEquals(380, e.unitGrams); assertEquals("Strati", e.categoryName); assertFalse(e.packed)
    }

    @Test fun `la modifica dell'inventario aggiorna le uscite aperte e non lo storico`() {
        val e = ChecklistRules.entryFrom(giacca, strati, "t1", "e1", 10).copy(quantity = 2, packed = true, notes = "mia nota", kind = GearKind.INDOSSATO)
        val changed = giacca.copy(name = "Giacca nuova", weightGrams = 410)
        for (open in listOf(TripStatus.BOZZA, TripStatus.PROGRAMMATA)) {
            val r = ChecklistRules.refreshed(e, changed, strati, open)
            assertEquals("Giacca nuova", r.name); assertEquals(410, r.unitGrams)
            assertEquals(2, r.quantity); assertTrue(r.packed); assertEquals("mia nota", r.notes); assertEquals(GearKind.INDOSSATO, r.kind)
        }
        for (closed in listOf(TripStatus.COMPLETATA, TripStatus.ANNULLATA)) assertEquals(e, ChecklistRules.refreshed(e, changed, strati, closed))
    }

    @Test fun `le copie per la duplicazione sono indipendenti e con spunte azzerate`() {
        val originals = listOf(entry("a", "Pile", 410, packed = true), entry("b", "Calze", 70, 2))
        var n = 0
        val copies = ChecklistRules.copiesFor(originals, "t2", resetPacked = true, now = 5) { "n${n++}" }
        assertEquals(listOf("t2", "t2"), copies.map { it.tripId })
        assertTrue(copies.none { it.packed })
        assertNotEquals(originals.map { it.id }, copies.map { it.id })
        assertTrue(originals[0].packed)
        assertEquals(2, copies[1].quantity)
        assertTrue(ChecklistRules.copiesFor(originals, "t3", resetPacked = false, now = 5) { "x${n++}" }[0].packed)
    }

    @Test fun `gruppi per categoria con subtotali e spunte`() {
        val entries = listOf(entry("a", "Pile", 410, packed = true), entry("b", "Calze", 70, 2), entry("c", "Borraccia", 160, cat = "Cucina e acqua", order = 2, packed = true), entry("d", "Guanti", null))
        val groups = ChecklistRules.groups(entries)
        assertEquals(listOf("Cucina e acqua", "Strati"), groups.map { it.category })
        assertEquals(listOf("Calze", "Guanti", "Pile"), groups[1].entries.map { it.name })
        assertEquals(550L, groups[1].grams); assertEquals(1, groups[1].packed)
        assertEquals(2, ChecklistRules.packedCount(entries))
        val t = ChecklistRules.totals(entries, 1.0)
        assertEquals(710L, t.baseGrams); assertEquals(1000L, t.waterGrams); assertEquals(1710L, t.packGrams); assertTrue(t.isPartial)
    }

    @Test fun `volume rispetto allo zaino con dati mancanti in evidenza`() {
        val entries = listOf(entry("z", "Zaino", 980, function = GearFunction.ZAINO, volume = 30.0), entry("a", "Piumino", 310, volume = 3.0, qty = 2), entry("b", "Frontale", 86), entry("c", "Pile", 410, kind = GearKind.INDOSSATO, volume = 2.0))
        val v = ChecklistRules.volume(entries) { 30.0 }
        assertEquals(6.0, v.liters, 0.001); assertEquals(1, v.entriesWithoutVolume); assertEquals(30.0, v.capacityLiters!!, 0.001)
        assertNull(ChecklistRules.volume(entries.drop(1)) { 30.0 }.capacityLiters)
    }

    @Test fun `csv con campi protetti`() {
        val csv = ChecklistRules.checklistCsv(listOf(entry("a", "Tazza; grande \"blu\"", 90, 2, packed = true))) { "Base" }
        val lines = csv.trim().split("\r\n")
        assertEquals("Categoria;Oggetto;Quantità;Peso unitario (g);Peso totale (g);Tipo;Pronto;Note", lines[0])
        assertEquals("Strati;\"Tazza; grande \"\"blu\"\"\";2;90;180;Base;sì;", lines[1])
    }

    @Test fun `formato del peso`() {
        assertEquals("380 g", WeightFormat.format(380)); assertEquals("7,45 kg", WeightFormat.format(7450)); assertEquals("1,00 kg", WeightFormat.format(1000))
        assertEquals("74,5 hg", WeightFormat.format(7450, WeightUnit.HG)); assertEquals("86 g", WeightFormat.format(86, WeightUnit.HG)); assertEquals("7450 g", WeightFormat.format(7450, WeightUnit.G))
        assertEquals("7.45 kg", WeightFormat.format(7450, WeightUnit.KG, Locale.ENGLISH))
    }
}
