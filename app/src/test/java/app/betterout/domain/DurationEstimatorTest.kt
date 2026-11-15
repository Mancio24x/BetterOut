package app.betterout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationEstimatorTest {
    @Test fun `caso del riferimento - 14 km con 900 m di salita e discesa`() {
        // to = 3,5 h; tv = 3 + 1,8 = 4,8 h; durata = 4,8 + 1,75 = 6,55 h = 393 minuti
        assertEquals(393, DurationEstimator.minutes(14_000.0, 900.0, 900.0))
    }

    @Test fun `in piano conta solo la distanza`() {
        assertEquals(120, DurationEstimator.minutes(8_000.0, 0.0, 0.0))
    }

    @Test fun `quando prevale la distanza il tempo verticale vale la meta`() {
        // to = 5 h; tv = 1 h; durata = 5,5 h
        assertEquals(330, DurationEstimator.minutes(20_000.0, 300.0, 0.0))
    }

    @Test fun `il ritmo scala la durata`() {
        assertEquals(150, DurationEstimator.minutes(8_000.0, 0.0, 0.0, Pace.LENTO))
        assertEquals(96, DurationEstimator.minutes(8_000.0, 0.0, 0.0, Pace.VELOCE))
    }

    @Test fun `giorni e notti`() {
        assertEquals(2, DurationEstimator.days(393, 6.0))
        assertEquals(1, DurationEstimator.days(360, 6.0))
        assertEquals(1, DurationEstimator.days(0, 6.0))
        assertEquals(1, DurationEstimator.nights(2))
        assertEquals(0, DurationEstimator.nights(1))
    }
}
