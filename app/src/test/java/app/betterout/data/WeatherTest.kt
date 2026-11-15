package app.betterout.data

import app.betterout.domain.WeatherPoint
import app.betterout.domain.WeatherPointKind
import app.betterout.domain.WeatherRules
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.LocalDate

class WeatherTest {
    @get:Rule val folder = TemporaryFolder()

    private val today = LocalDate.of(2026, 11, 15)
    private val start = WeatherPoint(WeatherPointKind.PARTENZA, 46.5811, 12.2528, 1752.0)
    private val top = WeatherPoint(WeatherPointKind.QUOTA_MASSIMA, 46.62, 12.3, 2752.0)
    private fun ask(points: List<WeatherPoint> = listOf(start, top), day: LocalDate = LocalDate.of(2026, 11, 17)) = WeatherRules.plan(day, null, today, points) as WeatherRules.Plan.Ask

    private fun answer(elevation: Double, tempMax: String = "5.8") = """{"latitude":46.56,"longitude":12.25,"timezone":"Europe/Rome","elevation":$elevation,
        "hourly":{"time":["2026-11-17T00:00","2026-11-17T01:00"],"temperature_2m":[0.5,null],"apparent_temperature":[-3.5,-3.7],"precipitation_probability":[0,12],"precipitation":[0.00,0.10],"snowfall":[0.00,0.07],"weather_code":[3,85],"wind_speed_10m":[8.0,8.2],"wind_gusts_10m":[15.5,16.2],"wind_direction_10m":[18,295],"freezing_level_height":[2130.00,2120.00],"is_day":[0,1]},
        "daily":{"time":["2026-11-17"],"weather_code":[3],"temperature_2m_max":[$tempMax],"temperature_2m_min":[0.2],"apparent_temperature_min":[-3.9],"precipitation_sum":[0.00],"precipitation_probability_max":[12],"snowfall_sum":[0.00],"wind_speed_10m_max":[8.8],"wind_gusts_10m_max":[31.3],"wind_direction_10m_dominant":[320],"sunrise":["2026-11-17T07:13"],"sunset":["2026-11-17T16:38"]}}"""

    @Test fun `una sola richiesta per tutti i punti, con le loro quote e il fuso della destinazione`() {
        val url = WeatherForecast.url(ask())
        assertTrue(url, url.contains("latitude=46.581100,46.620000&longitude=12.252800,12.300000&elevation=1752,2752"))
        assertTrue(url, url.contains("timezone=auto&start_date=2026-11-16&end_date=2026-11-18"))
    }

    @Test fun `senza quota nota la quota non viene mandata`() {
        val url = WeatherForecast.url(ask(listOf(WeatherPoint(WeatherPointKind.LOCALITA, 46.5811, 12.2528))))
        assertTrue(url, !url.contains("elevation="))
    }

    @Test fun `la risposta con più punti è letta nell'ordine della richiesta`() {
        val forecasts = WeatherForecast.parse("[${answer(1752.0)},${answer(2752.0, tempMax = "-1.5")}]", listOf(start, top))
        assertEquals(listOf(start, top), forecasts.map { it.point })
        assertEquals(2752.0, forecasts[1].elevation!!, 0.0)
        assertEquals(-1.5, forecasts[1].days.single().tempMax!!, 0.0)
        val day = forecasts[0].day(LocalDate.of(2026, 11, 17))!!
        assertEquals(3, day.code)
        assertEquals(12, day.precipitationProbability)
        assertEquals(320, day.windDirection)
        assertEquals("2026-11-17T16:38", day.sunset)
        val hours = forecasts[0].hours(LocalDate.of(2026, 11, 17))
        assertEquals(2, hours.size)
        assertEquals(85, hours[1].code)
        assertEquals(true, hours[1].isDay)
        assertEquals(2120.0, hours[1].freezingLevel!!, 0.0)
    }

    @Test fun `un valore che il servizio non dà resta mancante`() {
        val forecast = WeatherForecast.parse(answer(1752.0), listOf(start)).single()
        assertNull(forecast.hours[1].temp)
        assertEquals(0.5, forecast.hours[0].temp!!, 0.0)
    }

    @Test fun `una risposta che non è una previsione è un errore`() {
        assertThrows(IOException::class.java) { WeatherForecast.parse("""{"reason":"Parameter 'start_date' is out of allowed range","error":true}""", listOf(start)) }
        assertThrows(IOException::class.java) { WeatherForecast.parse("<html>", listOf(start)) }
        assertThrows(IOException::class.java) { WeatherForecast.parse(answer(1752.0), listOf(start, top)) }
    }

    @Test fun `la previsione è salvata e non viene richiesta prima di tre ore`() = runTest {
        var now = 1_000L
        var calls = 0
        val repo = WeatherRepository(folder.root, { now }) { calls++; answer(1752.0) }
        val first = repo.forecast("u1", ask(listOf(start)))
        assertEquals(1_000L, first.fetchedAt)
        now += WeatherRules.MAX_AGE_MILLIS
        assertEquals(first, repo.forecast("u1", ask(listOf(start))))
        assertEquals(1, calls)
        now += 1
        assertEquals(now, repo.forecast("u1", ask(listOf(start))).fetchedAt)
        assertEquals(2, calls)
        repo.forecast("u1", ask(listOf(start)), force = true)
        assertEquals(3, calls)
    }

    @Test fun `se la richiesta non riesce resta l'ultimo dato salvato`() = runTest {
        var online = true
        val repo = WeatherRepository(folder.root, { 0L }) { if (online) answer(1752.0) else throw IOException("senza rete") }
        val saved = repo.forecast("u1", ask(listOf(start)))
        online = false
        var failed = false
        try { repo.forecast("u1", ask(listOf(start)), force = true) } catch (e: IOException) { failed = true }
        assertTrue(failed)
        assertEquals(saved, WeatherRepository(folder.root).cached("u1", ask(listOf(start))))
    }

    @Test fun `un dato salvato per altre date o altri punti non viene mostrato`() = runTest {
        val repo = WeatherRepository(folder.root, { 0L }) { answer(1752.0) }
        repo.forecast("u1", ask(listOf(start)))
        assertNull(repo.cached("u1", ask(listOf(start), LocalDate.of(2026, 11, 20))))
        assertNull(repo.cached("u1", ask(listOf(start.copy(lat = 46.0)))))
        assertNull(repo.cached("u2", ask(listOf(start))))
        repo.delete("u1")
        assertNull(repo.cached("u1", ask(listOf(start))))
    }

    @Test fun `il clima è chiesto all'archivio una volta sola, per i cinque anni passati`() = runTest {
        val asked = mutableListOf<String>()
        val repo = WeatherRepository(folder.root, { 0L }) { url ->
            asked += url
            """{"elevation":2575.0,"daily":{"time":["2025-12-02","2025-12-03","2025-07-01"],"temperature_2m_max":[-2.0,-4.0,20.0],"temperature_2m_min":[-9.0,-11.0,9.0],"precipitation_sum":[0.0,3.2,12.0]}}"""
        }
        val day = LocalDate.of(2026, 12, 5)
        val climate = repo.climate("u1", top, day, day, today)
        assertTrue(asked.single(), asked.single().startsWith("https://archive-api.open-meteo.com/v1/archive?latitude=46.620000&longitude=12.300000&elevation=2752&timezone=auto&start_date=2021-12-02&end_date=2025-12-08"))
        assertEquals(-3.0, climate.typicalMax!!, 1e-9)
        assertEquals(-10.0, climate.typicalMin!!, 1e-9)
        assertEquals(1, climate.wetDays)
        assertEquals(2, climate.countedDays)
        assertEquals(2575.0, climate.elevation!!, 0.0)
        assertEquals(climate, repo.climate("u1", top, day, day, today))
        assertEquals(1, asked.size)
    }
}
