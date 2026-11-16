package app.betterout.ui.dive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharkBiteTest {
    @Test fun `le mascelle partono aperte, a metà sono chiuse, alla fine di nuovo aperte`() {
        assertEquals(0f, jawClosure(0f), 0f)
        assertEquals(1f, jawClosure(0.5f), 0f)
        assertEquals(0f, jawClosure(1f), 0.0001f)
        assertTrue(jawClosure(0.2f) in 0f..1f && jawClosure(0.8f) in 0f..1f)
    }
}
