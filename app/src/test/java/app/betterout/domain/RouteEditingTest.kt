package app.betterout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteEditingTest {
    private val a = GeoPoint(46.0, 12.0)
    private val b = GeoPoint(46.01, 12.0)
    private val c = GeoPoint(46.02, 12.0)
    private fun manual(vararg p: GeoPoint) = p.fold(RouteDraft()) { d, point -> RouteEditing.add(d, point, LegMode.MANUALE) }

    @Test fun `il primo punto non crea tratti`() {
        val d = RouteEditing.add(RouteDraft(), a, LegMode.SENTIERO)
        assertEquals(listOf(a), d.controls)
        assertTrue(d.legs.isEmpty())
    }

    @Test fun `un tratto su sentiero resta da calcolare e non è mai una linea retta`() {
        val d = RouteEditing.add(RouteEditing.add(RouteDraft(), a, LegMode.SENTIERO), b, LegMode.SENTIERO)
        assertNull(d.legs.single().points)
        assertEquals(listOf(0), d.unresolved)
        assertTrue(d.lines().flatten().isEmpty())
    }

    @Test fun `un tratto manuale è una retta dichiarata`() {
        val d = manual(a, b)
        assertEquals(Leg(LegMode.MANUALE, listOf(a, b)), d.legs.single())
        assertTrue(d.hasManualLegs)
        assertTrue(d.unresolved.isEmpty())
    }

    @Test fun `il risultato del routing aggancia i punti di controllo e i tratti manuali vicini`() {
        var d = RouteEditing.add(manual(a, b), c, LegMode.SENTIERO)
        val snappedB = GeoPoint(46.0101, 12.0001, 1500.0)
        val snappedC = GeoPoint(46.0199, 12.0002, 1600.0)
        d = RouteEditing.resolve(d, 1, listOf(snappedB, GeoPoint(46.015, 12.001, 1550.0), snappedC))
        assertEquals(listOf(a, snappedB.copy(ele = null), snappedC.copy(ele = null)), d.controls)
        assertEquals(listOf(a, snappedB.copy(ele = null)), d.legs[0].points)
        assertTrue(d.unresolved.isEmpty())
        assertEquals(4, d.lines().flatten().size)
    }

    @Test fun `spostare un punto fa rifare solo i tratti che lo toccano`() {
        val d0 = RouteEditing.resolve(RouteEditing.add(manual(a, b), c, LegMode.SENTIERO), 1, listOf(b, c))
        val moved = GeoPoint(46.03, 12.0)
        val d = RouteEditing.move(d0, 2, moved)
        assertEquals(d0.legs[0], d.legs[0])
        assertEquals(Leg(LegMode.SENTIERO, null), d.legs[1])
        val first = RouteEditing.move(d0, 0, moved)
        assertEquals(Leg(LegMode.MANUALE, listOf(moved, b)), first.legs[0])
        assertEquals(d0.legs[1], first.legs[1])
    }

    @Test fun `eliminare un punto intermedio unisce i due tratti`() {
        val d = RouteEditing.delete(manual(a, b, c), 1)
        assertEquals(listOf(a, c), d.controls)
        assertEquals(Leg(LegMode.MANUALE, listOf(a, c)), d.legs.single())
    }

    @Test fun `eliminare primo o ultimo punto toglie solo il tratto vicino`() {
        val d = manual(a, b, c)
        assertEquals(listOf(Leg(LegMode.MANUALE, listOf(b, c))), RouteEditing.delete(d, 0).legs)
        assertEquals(listOf(Leg(LegMode.MANUALE, listOf(a, b))), RouteEditing.delete(d, 2).legs)
        assertEquals(RouteDraft(), RouteEditing.delete(manual(a), 0))
    }

    @Test fun `unire un tratto su sentiero e uno manuale dà un tratto manuale`() {
        val d = RouteEditing.resolve(RouteEditing.add(manual(a, b), c, LegMode.SENTIERO), 1, listOf(b, c))
        assertEquals(LegMode.MANUALE, RouteEditing.delete(d, 1).legs.single().mode)
    }

    @Test fun `inserire un punto divide il tratto senza cambiare il tracciato`() {
        val path = listOf(a, GeoPoint(46.002, 12.0, 10.0), GeoPoint(46.005, 12.0, 20.0), GeoPoint(46.008, 12.0, 30.0), b)
        val d0 = RouteEditing.resolve(RouteEditing.add(RouteEditing.add(RouteDraft(), a, LegMode.SENTIERO), b, LegMode.SENTIERO), 0, path)
        val d = RouteEditing.insertAfter(d0, 0)!!
        assertEquals(3, d.controls.size)
        assertEquals(GeoPoint(46.005, 12.0), d.controls[1])
        assertEquals(d0.lines().flatten(), d.lines().flatten())
        assertTrue(d.legs.all { it.mode == LegMode.SENTIERO })
    }

    @Test fun `inserire su un tratto manuale usa il punto medio`() {
        val d = RouteEditing.insertAfter(manual(a, c), 0)!!
        assertEquals(b.lat, d.controls[1].lat, 1e-9)
        assertEquals(2, d.legs.size)
    }

    @Test fun `non si inserisce dopo l'ultimo punto né su un tratto non calcolato`() {
        assertNull(RouteEditing.insertAfter(manual(a, b), 1))
        assertNull(RouteEditing.insertAfter(RouteEditing.add(manual(a), b, LegMode.SENTIERO), 0))
    }

    @Test fun `fine qui toglie ciò che segue`() {
        assertEquals(manual(a, b), RouteEditing.endAt(manual(a, b, c), 1))
    }

    @Test fun `invertire due volte riporta al percorso di partenza`() {
        val d = manual(a, b, c)
        val reversed = RouteEditing.reverse(d)
        assertEquals(listOf(c, b, a), reversed.controls)
        assertEquals(listOf(c, b), reversed.legs[0].points)
        assertEquals(d, RouteEditing.reverse(reversed))
    }

    @Test fun `chiudere ad anello torna al primo punto una sola volta`() {
        val d = RouteEditing.closeLoop(manual(a, b, c), LegMode.MANUALE)
        assertEquals(a, d.controls.last())
        assertTrue(d.isLoop)
        assertEquals(d, RouteEditing.closeLoop(d, LegMode.MANUALE))
        assertFalse(manual(a, b).isLoop)
        assertEquals(manual(a), RouteEditing.closeLoop(manual(a), LegMode.MANUALE))
    }

    @Test fun `un tratto manuale fra due tratti su sentiero non toglie le quote alle giunzioni`() {
        val d = RouteDraft(listOf(a, b, c, a), listOf(Leg(LegMode.SENTIERO, listOf(a.copy(ele = 1.0), b.copy(ele = 2.0))), Leg(LegMode.MANUALE, listOf(b, c)), Leg(LegMode.SENTIERO, listOf(c.copy(ele = 3.0), a.copy(ele = 4.0)))))
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0), d.lines().single().map { it.ele })
    }

    @Test fun `un tratto non calcolato divide il percorso e non viene scavalcato`() {
        val d = RouteDraft(listOf(a, b, c, a), listOf(Leg(LegMode.MANUALE, listOf(a, b)), Leg(LegMode.SENTIERO, null), Leg(LegMode.MANUALE, listOf(c, a))))
        assertEquals(listOf(listOf(a, b), listOf(c, a)), d.lines())
        assertEquals(Geo.distance(a, b) + Geo.distance(c, a), RouteMath.stats(d.lines()).distanceMeters, 0.001)
    }

    @Test fun `un tratto non riuscito può diventare manuale solo per scelta`() {
        val d = RouteEditing.add(manual(a), b, LegMode.SENTIERO)
        assertEquals(Leg(LegMode.MANUALE, listOf(a, b)), RouteEditing.toManual(d, 0).legs.single())
    }
}
