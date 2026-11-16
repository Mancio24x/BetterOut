package app.betterout.ui.dive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseNumberTest {
    @Test fun `virgola e punto valgono entrambi, il campo vuoto è nessun valore`() {
        assertEquals(18.5, parseNumber("18,5", decimals = true, allowNegative = false).getOrThrow())
        assertEquals(18.5, parseNumber(" 18.5 ", decimals = true, allowNegative = false).getOrThrow())
        assertEquals(null, parseNumber("", decimals = true, allowNegative = false).getOrThrow())
    }

    @Test fun `testo, decimali dove servono interi e negativi non ammessi sono errori`() {
        assertTrue(parseNumber("abc", decimals = true, allowNegative = false).isFailure)
        assertTrue(parseNumber("45,5", decimals = false, allowNegative = false).isFailure)
        assertTrue(parseNumber("-3", decimals = true, allowNegative = false).isFailure)
        assertEquals(-2.0, parseNumber("-2", decimals = true, allowNegative = true).getOrThrow())
    }
}
