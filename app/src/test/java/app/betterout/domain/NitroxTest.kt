package app.betterout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** Valori di confronto: le tabelle dei corsi nitrox (acqua di mare, 10 m = 1 bar). */
class NitroxTest {
    @Test fun `profondità massima operativa delle miscele comuni`() {
        assertEquals(33.75, Nitrox.mod(32), 0.01)        // EAN32 a 1,4: 33,8 m
        assertEquals(28.89, Nitrox.mod(36), 0.01)        // EAN36 a 1,4: 28,9 m
        assertEquals(40.0, Nitrox.mod(32, 1.6), 0.01)    // EAN32 al limite 1,6: 40 m
        assertEquals(56.67, Nitrox.mod(21), 0.01)        // aria a 1,4: 56,7 m
        assertEquals(30.63, Nitrox.mod(32, 1.3), 0.01)   // EAN32 a 1,3: 30,6 m
        assertEquals(27.5, Nitrox.mod(32, 1.2), 0.01)    // EAN32 a 1,2: 27,5 m
    }

    @Test fun `miscela migliore arrotondata per difetto, mai sotto l'aria`() {
        assertEquals(35, Nitrox.bestMix(30.0))           // 1,4 / 4 bar = 35%
        assertEquals(31, Nitrox.bestMix(35.0))           // 1,4 / 4,5 = 31,1%
        assertEquals(40, Nitrox.bestMix(25.0, 1.4))      // 1,4 / 3,5 = 40%
        assertEquals(21, Nitrox.bestMix(80.0))           // troppo profondo per il nitrox: resta l'aria
        assertEquals(100, Nitrox.bestMix(0.0, 1.6))
        assertEquals(32, Nitrox.bestMix(30.0, 1.3))      // 1,3 / 4 = 32,5%
    }

    @Test fun `profondità equivalente in aria e pressione parziale`() {
        assertEquals(24.43, Nitrox.ead(30.0, 32), 0.01)  // EAN32 a 30 m equivale all’aria a circa 24,4 m
        assertEquals(30.0, Nitrox.ead(30.0, 21), 0.01)
        assertEquals(1.28, Nitrox.ppO2(30.0, 32), 0.001)
    }
}
