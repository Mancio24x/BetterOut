package app.betterout.domain

import app.betterout.domain.WeatherRules.Plan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WeatherRulesTest {
    private val today = LocalDate.of(2026, 11, 15)
    private val place = listOf(WeatherPoint(WeatherPointKind.LOCALITA, 46.58, 12.25))

    @Test fun `senza percorso il punto è la località, senza quota`() {
        assertEquals(place, WeatherRules.points(46.58, 12.25, null))
        assertEquals(emptyList<WeatherPoint>(), WeatherRules.points(null, null, null))
    }

    @Test fun `con un percorso i punti sono la partenza e il punto più alto`() {
        val line = listOf(GeoPoint(46.0, 12.0, 1750.0), GeoPoint(46.1, 12.1, 2752.0), GeoPoint(46.2, 12.2, 2100.0))
        assertEquals(
            listOf(WeatherPoint(WeatherPointKind.PARTENZA, 46.0, 12.0, 1750.0), WeatherPoint(WeatherPointKind.QUOTA_MASSIMA, 46.1, 12.1, 2752.0)),
            WeatherRules.points(45.0, 11.0, listOf(line)),
        )
    }

    @Test fun `un punto più alto di meno di cento metri non è distinto dalla partenza`() {
        val line = listOf(GeoPoint(46.0, 12.0, 1750.0), GeoPoint(46.1, 12.1, 1840.0))
        assertEquals(listOf(WeatherPoint(WeatherPointKind.PARTENZA, 46.0, 12.0, 1750.0)), WeatherRules.points(null, null, listOf(line)))
    }

    @Test fun `senza quota alla partenza si chiede solo la partenza`() {
        val line = listOf(GeoPoint(46.0, 12.0), GeoPoint(46.1, 12.1, 2752.0))
        assertEquals(listOf(WeatherPoint(WeatherPointKind.PARTENZA, 46.0, 12.0)), WeatherRules.points(null, null, listOf(line)))
    }

    @Test fun `senza data o senza luogo non si chiede niente`() {
        assertEquals(Plan.NoDate, WeatherRules.plan(null, null, today, place))
        assertEquals(Plan.NoPlace, WeatherRules.plan(today, null, today, emptyList()))
    }

    @Test fun `un'uscita finita non ha previsioni`() {
        assertEquals(Plan.Past, WeatherRules.plan(today.minusDays(3), today.minusDays(1), today, place))
    }

    @Test fun `un'uscita di un giorno chiede anche il giorno prima e quello dopo`() {
        val day = LocalDate.of(2026, 11, 21)
        val plan = WeatherRules.plan(day, null, today, place) as Plan.Ask
        assertEquals(day.minusDays(1), plan.from)
        assertEquals(day.plusDays(1), plan.to)
        assertEquals(listOf(day), plan.tripDays)
        assertEquals(listOf(day.minusDays(1), day.plusDays(1)), plan.nearbyDays)
        assertFalse(plan.truncated)
    }

    @Test fun `un'uscita di oggi non chiede ieri`() {
        val plan = WeatherRules.plan(today, null, today, place) as Plan.Ask
        assertEquals(today, plan.from)
        assertEquals(listOf(today.plusDays(1)), plan.nearbyDays)
    }

    @Test fun `di un'uscita già cominciata si chiedono i giorni che restano`() {
        val plan = WeatherRules.plan(today.minusDays(1), today.plusDays(1), today, place) as Plan.Ask
        assertEquals(listOf(today, today.plusDays(1)), plan.tripDays)
    }

    @Test fun `l'ultimo giorno con previsioni è oggi più quindici`() {
        val last = LocalDate.of(2026, 11, 30)
        val plan = WeatherRules.plan(last, null, today, place) as Plan.Ask
        assertEquals(last, plan.to)
        assertEquals(listOf(last.minusDays(1)), plan.nearbyDays)
    }

    @Test fun `oltre l'orizzonte dice da quando ci saranno le previsioni`() {
        val plan = WeatherRules.plan(LocalDate.of(2026, 12, 5), null, today, place) as Plan.TooEarly
        assertEquals(LocalDate.of(2026, 11, 20), plan.availableFrom)
        // Il 20 novembre il 5 dicembre è proprio l'ultimo giorno previsto.
        assertTrue(WeatherRules.plan(LocalDate.of(2026, 12, 5), null, plan.availableFrom, place) is Plan.Ask)
    }

    @Test fun `un'uscita a cavallo dell'orizzonte è chiesta fin dove si può e lo dichiara`() {
        val plan = WeatherRules.plan(LocalDate.of(2026, 11, 29), LocalDate.of(2026, 12, 2), today, place) as Plan.Ask
        assertEquals(LocalDate.of(2026, 11, 30), plan.lastDay)
        assertEquals(LocalDate.of(2026, 11, 30), plan.to)
        assertTrue(plan.truncated)
    }

    @Test fun `la chiave cambia con i punti e con le date`() {
        val a = WeatherRules.plan(LocalDate.of(2026, 11, 21), null, today, place) as Plan.Ask
        val b = WeatherRules.plan(LocalDate.of(2026, 11, 22), null, today, place) as Plan.Ask
        val c = WeatherRules.plan(LocalDate.of(2026, 11, 21), null, today, listOf(place[0].copy(lat = 46.6))) as Plan.Ask
        assertTrue(a.key != b.key && a.key != c.key)
        assertEquals(a.key, (WeatherRules.plan(LocalDate.of(2026, 11, 21), null, today, place) as Plan.Ask).key)
    }

    @Test fun `un dato è vecchio dopo tre ore`() {
        assertFalse(WeatherRules.isStale(0, 3 * 3_600_000L))
        assertTrue(WeatherRules.isStale(0, 3 * 3_600_000L + 1))
    }

    @Test fun `il clima guarda gli stessi giorni dei cinque anni già in archivio`() {
        val windows = WeatherRules.climateWindows(LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 5), today)
        assertEquals(5, windows.size)
        assertEquals(LocalDate.of(2025, 12, 2) to LocalDate.of(2025, 12, 8), windows.first())
        assertEquals(LocalDate.of(2021, 12, 2) to LocalDate.of(2021, 12, 8), windows.last())
        // Un periodo che l'anno scorso non è ancora in archivio parte dall'anno prima.
        assertEquals(2025, WeatherRules.climateWindows(LocalDate.of(2027, 11, 14), LocalDate.of(2027, 11, 14), today).first().first.year)
    }

    @Test fun `le medie del clima contano solo i giorni del periodo e quelli con almeno un millimetro`() {
        val windows = listOf(LocalDate.of(2025, 12, 2) to LocalDate.of(2025, 12, 4), LocalDate.of(2024, 12, 2) to LocalDate.of(2024, 12, 4))
        val days = listOf(
            ClimateDay("2024-12-02", -2.0, -10.0, 0.0), ClimateDay("2024-12-03", 0.0, -8.0, 0.9), ClimateDay("2024-12-04", 2.0, -6.0, 1.0),
            ClimateDay("2025-06-01", 20.0, 10.0, 30.0),
            ClimateDay("2025-12-02", -4.0, -12.0, 5.0), ClimateDay("2025-12-03", null, null, null), ClimateDay("2025-12-04", 0.0, -4.0, 0.0),
        )
        val climate = WeatherRules.climate("k", 2575.0, days, windows)
        assertEquals(-0.8, climate.typicalMax!!, 1e-9)
        assertEquals(-8.0, climate.typicalMin!!, 1e-9)
        assertEquals(2, climate.wetDays)
        assertEquals(5, climate.countedDays)
        assertEquals(2, climate.years)
        assertEquals("2025-12-02", climate.from)
    }

    @Test fun `senza dati il clima non inventa valori`() {
        val climate = WeatherRules.climate("k", null, emptyList(), listOf(LocalDate.of(2025, 12, 2) to LocalDate.of(2025, 12, 4)))
        assertNull(climate.typicalMin)
        assertNull(climate.typicalMax)
        assertEquals(0, climate.countedDays)
    }

    @Test fun `i codici del tempo hanno un nome, quelli sconosciuti no`() {
        assertEquals(Sky.SERENO, WeatherRules.sky(0))
        assertEquals(Sky.ROVESCI_NEVE, WeatherRules.sky(85))
        assertEquals(Sky.TEMPORALE_GRANDINE, WeatherRules.sky(99))
        assertNull(WeatherRules.sky(42))
        assertNull(WeatherRules.sky(null))
    }

    @Test fun `la direzione del vento cade in uno degli otto settori`() {
        assertEquals(0, WeatherRules.compassSector(0))
        assertEquals(0, WeatherRules.compassSector(350))
        assertEquals(1, WeatherRules.compassSector(45))
        assertEquals(4, WeatherRules.compassSector(180))
        assertEquals(7, WeatherRules.compassSector(315))
        assertEquals(0, WeatherRules.compassSector(360))
    }

    @Test fun `vento della fascia oraria - media del vento e massimo delle raffiche`() {
        val hours = listOf(
            HourWeather("2026-11-21T06:00", windKmh = 90.0, gustsKmh = 99.0),
            HourWeather("2026-11-21T07:00", windKmh = 20.0, gustsKmh = 30.0),
            HourWeather("2026-11-21T11:00", windKmh = 30.0, gustsKmh = 45.0),
            HourWeather("2026-11-21T12:00", windKmh = 5.0),
        )
        assertEquals(25.0 to 45.0, WeatherRules.windBetween(hours, 7, 11))
        assertEquals(5.0 to null, WeatherRules.windBetween(hours, 12, 16))
        assertNull(WeatherRules.windBetween(hours, 17, 20))
    }

    @Test fun `zero termico - minimo e massimo della giornata`() {
        assertEquals(1900.0 to 2400.0, WeatherRules.freezingRange(listOf(HourWeather("2026-11-21T07:00", freezingLevel = 2400.0), HourWeather("2026-11-21T08:00"), HourWeather("2026-11-21T09:00", freezingLevel = 1900.0))))
        assertNull(WeatherRules.freezingRange(listOf(HourWeather("2026-11-21T07:00"))))
    }

    @Test fun `il margine di luce è negativo se si rientra dopo il tramonto`() {
        assertEquals(280L, WeatherRules.daylightMarginMinutes(LocalDateTime.of(2026, 11, 21, 11, 50), "2026-11-21T16:30"))
        assertEquals(-30L, WeatherRules.daylightMarginMinutes(LocalDateTime.of(2026, 11, 21, 17, 0), "2026-11-21T16:30"))
        assertNull(WeatherRules.daylightMarginMinutes(LocalDateTime.of(2026, 11, 21, 17, 0), null))
    }
}
