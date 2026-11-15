package app.betterout.ui

import app.betterout.ui.gear.GearEditViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GearWeightTest {
    @Test fun `il peso si scrive in grammi interi o in chilogrammi con la virgola`() {
        assertEquals(1200, GearEditViewModel.grams("1200", kg = false))
        assertEquals(1200, GearEditViewModel.grams("1,2", kg = true))
        assertEquals(86, GearEditViewModel.grams("0.086", kg = true))
        assertNull(GearEditViewModel.grams("1,2", kg = false))
        assertNull(GearEditViewModel.grams("", kg = true))
        assertNull(GearEditViewModel.grams("-1", kg = false))
    }

    @Test fun `cambiando unità il numero scritto viene convertito senza zeri inutili`() {
        assertEquals("1,2", GearEditViewModel.weightText(1200, kg = true))
        assertEquals("0,086", GearEditViewModel.weightText(86, kg = true))
        assertEquals("2", GearEditViewModel.weightText(2000, kg = true))
        assertEquals("1200", GearEditViewModel.weightText(1200, kg = false))
        assertEquals("", GearEditViewModel.weightText(null, kg = true))
    }
}
