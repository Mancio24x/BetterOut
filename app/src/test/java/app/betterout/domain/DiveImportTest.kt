package app.betterout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DiveImportTest {
    private val rome = ZoneId.of("Europe/Rome")

    /** Costruisce un file FIT minimo: definizioni e dati, in little o big endian. */
    private class FitBuilder {
        private val body = ByteArrayOutputStream()

        /** [fields]: numero del campo, dimensione, tipo base. */
        fun define(local: Int, global: Int, fields: List<Triple<Int, Int, Int>>, bigEndian: Boolean = false) {
            body.write(0x40 or local); body.write(0); body.write(if (bigEndian) 1 else 0)
            body.write(ByteBuffer.allocate(2).order(if (bigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN).putShort(global.toShort()).array())
            body.write(fields.size)
            fields.forEach { (n, size, type) -> body.write(n); body.write(size); body.write(type) }
        }

        fun data(header: Int, vararg values: Pair<Int, Long>, bigEndian: Boolean = false) {
            body.write(header)
            val order = if (bigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
            values.forEach { (size, v) ->
                val b = ByteBuffer.allocate(size).order(order)
                when (size) { 1 -> b.put(v.toByte()); 2 -> b.putShort(v.toShort()); 4 -> b.putInt(v.toInt()) }
                body.write(b.array())
            }
        }

        fun bytes(): ByteArray {
            val data = body.toByteArray()
            val header = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN).put(14).put(0x20).putShort(2132).putInt(data.size).put(".FIT".toByteArray()).putShort(0).array()
            return header + data + byteArrayOf(0, 0)
        }
    }

    // 2023-08-17 06:51:00 UTC in secondi FIT (dal 31 dicembre 1989).
    private val start = 1692255060L - 631065600L

    private fun fitDive(): ByteArray = FitBuilder().apply {
        // session: start_time, total_elapsed_time, posizione
        define(0, 18, listOf(Triple(2, 4, 0x86), Triple(7, 4, 0x86), Triple(3, 4, 0x85), Triple(4, 4, 0x85)))
        data(0, 4 to start, 4 to 2_700_000, 4 to 552_000_000L, 4 to 147_000_000L)
        // activity: timestamp e ora locale (+2 h)
        define(1, 34, listOf(Triple(253, 4, 0x86), Triple(5, 4, 0x86)))
        data(1, 4 to start, 4 to start + 7200)
        // record: timestamp, profondità (mm), temperatura; il terzo con intestazione compressa (+20 s)
        define(2, 20, listOf(Triple(253, 4, 0x86), Triple(92, 4, 0x86), Triple(13, 1, 0x01)), bigEndian = true)
        data(2, 4 to start, 4 to 0, 1 to 24, bigEndian = true)
        data(2, 4 to start + 60, 4 to 12_500, 1 to 22, bigEndian = true)
        // record senza timestamp, con intestazione compressa: 20 secondi dopo il precedente
        define(3, 20, listOf(Triple(92, 4, 0x86), Triple(13, 1, 0x01)), bigEndian = true)
        data(0x80 or (3 shl 5) or ((start + 80) and 0x1F).toInt(), 4 to 18_250, 1 to 21, bigEndian = true)
        // dive_summary della sessione: riferimento 18, profondità media e massima, numero (tipo locale 0 ridefinito)
        define(0, 268, listOf(Triple(0, 2, 0x84), Triple(2, 4, 0x86), Triple(3, 4, 0x86), Triple(10, 4, 0x86)))
        data(0, 2 to 18, 4 to 11_000, 4 to 18_250, 4 to 42)
        // dive_gas: indice 0, 32% di ossigeno, attivo
        define(1, 259, listOf(Triple(254, 2, 0x84), Triple(1, 1, 0x02), Triple(2, 1, 0x00)))
        data(1, 2 to 0, 1 to 32, 1 to 1)
    }.bytes()

    @Test fun `FIT - riepilogo, ora locale del file, gas, posizione e profilo`() {
        val result = DiveImport.read(fitDive(), rome) as DiveImportResult.Ok
        assertEquals(DiveFileFormat.FIT, result.format)
        val d = result.dives.single()
        assertEquals(LocalDate.of(2023, 8, 17), d.date)
        assertEquals(LocalTime.of(8, 51), d.startTime)
        assertEquals(42, d.number)
        assertEquals(45, d.durationMinutes)
        assertEquals(18.25, d.maxDepthMeters!!, 0.001)
        assertEquals(11.0, d.averageDepthMeters!!, 0.001)
        assertEquals(21.0, d.waterTempC!!, 0.0)
        assertEquals(32, d.oxygenPercent)
        assertEquals(46.27, d.latitude!!, 0.01)
        assertEquals(12.32, d.longitude!!, 0.01)
        assertEquals(listOf(DiveSample(0, 0.0, 24.0), DiveSample(60, 12.5, 22.0), DiveSample(80, 18.25, 21.0)), d.samples)
    }

    @Test fun `FIT rovinato o senza immersione`() {
        val broken = fitDive().copyOf(60)
        assertEquals(DiveImportResult.Error(DiveImportError.ROVINATO), DiveImport.read(broken, rome))
        val walk = FitBuilder().apply { define(0, 18, listOf(Triple(2, 4, 0x86))); data(0, 4 to start) }.bytes()
        assertEquals(DiveImportResult.Error(DiveImportError.NESSUNA_IMMERSIONE), DiveImport.read(walk, rome))
    }

    @Test fun `UDDF - unità del sistema internazionale convertite, sito, gas e compagno collegati`() {
        val xml = """<?xml version="1.0"?>
            <uddf xmlns="http://www.streit.cc/uddf/3.2/" version="3.2.0">
              <diver><buddy id="b1"><personal><firstname>Anna</firstname><lastname>Rossi</lastname></personal></buddy></diver>
              <gasdefinitions><mix id="ean32"><o2>0.32</o2></mix></gasdefinitions>
              <divesite><site id="s1"><name>Secca di Capo Noli</name><geography><latitude>44.20</latitude><longitude>8.42</longitude></geography></site></divesite>
              <profiledata><repetitiongroup><dive id="d1">
                <informationbeforedive><link ref="s1"/><link ref="b1"/><divenumber>7</divenumber><datetime>2024-07-01T10:15:00</datetime></informationbeforedive>
                <tankdata><link ref="ean32"/><tankvolume>0.012</tankvolume><tankpressurebegin>20000000</tankpressurebegin><tankpressureend>6000000</tankpressureend></tankdata>
                <samples><waypoint><divetime>0</divetime><depth>0</depth><temperature>296.15</temperature></waypoint><waypoint><divetime>600</divetime><depth>21.4</depth></waypoint></samples>
                <informationafterdive><greatestdepth>21.4</greatestdepth><averagedepth>12.1</averagedepth><diveduration>2880</diveduration><lowesttemperature>291.45</lowesttemperature><visibility>15</visibility><notes><para>Cernie</para></notes></informationafterdive>
              </dive></repetitiongroup></profiledata>
            </uddf>"""
        val d = (DiveImport.read(xml.toByteArray(), rome) as DiveImportResult.Ok).dives.single()
        assertEquals(ImportedDive(
            date = LocalDate.of(2024, 7, 1), startTime = LocalTime.of(10, 15), number = 7, siteName = "Secca di Capo Noli", latitude = 44.20, longitude = 8.42,
            durationMinutes = 48, maxDepthMeters = 21.4, averageDepthMeters = 12.1, waterTempC = 18.3, visibilityMeters = 15.0, oxygenPercent = 32,
            tankLiters = 12.0, startBar = 200, endBar = 60, buddy = "Anna Rossi", notes = "Cernie",
            samples = listOf(DiveSample(0, 0.0, 23.0), DiveSample(600, 21.4, null)),
        ), d)
    }

    @Test fun `Subsurface - valori con unità, sito e viaggi`() {
        val xml = """<divelog program='subsurface' version='3'>
            <divesites><site uuid=' 4a3c' name='Relitto Haven' gps='44.3700 8.7400'/></divesites>
            <dives><trip date='2024-08-01'>
              <dive number='12' date='2024-08-02' time='09:40:00' duration='52:30 min' divesiteid='4a3c'>
                <buddy>Marco</buddy><notes>Corrente in superficie</notes>
                <cylinder size='15.0 l' workpressure='232.0 bar' start='210.0 bar' end='70.0 bar' o2='28.0%' />
                <divecomputer model='X'><depth max='34.6 m' mean='22.1 m' /><temperature water='17.0 C' />
                  <sample time='0:10 min' depth='3.2 m' temp='20.0 C' /><sample time='1:00:05' depth='0.0 m' />
                </divecomputer>
              </dive>
            </trip></dives></divelog>"""
        val d = (DiveImport.read(xml.toByteArray(), rome) as DiveImportResult.Ok).dives.single()
        assertEquals(12, d.number)
        assertEquals("Relitto Haven", d.siteName)
        assertEquals(44.37, d.latitude!!, 0.0)
        assertEquals(53, d.durationMinutes)
        assertEquals(34.6, d.maxDepthMeters!!, 0.0)
        assertEquals(17.0, d.waterTempC!!, 0.0)
        assertEquals(28, d.oxygenPercent)
        assertEquals(15.0, d.tankLiters!!, 0.0)
        assertEquals(210 to 70, d.startBar to d.endBar)
        assertEquals(listOf(DiveSample(10, 3.2, 20.0), DiveSample(3605, 0.0, null)), d.samples)
    }

    @Test fun `file di altro tipo e XML con entità esterne non vengono letti`() {
        assertEquals(DiveImportResult.Error(DiveImportError.NON_RICONOSCIUTO), DiveImport.read("ciao".toByteArray(), rome))
        assertEquals(DiveImportResult.Error(DiveImportError.NON_RICONOSCIUTO), DiveImport.read("<gpx></gpx>".toByteArray(), rome))
        val xxe = """<?xml version="1.0"?><!DOCTYPE divelog [<!ENTITY x SYSTEM "file:///etc/passwd">]><divelog><dives><dive date='2024-01-01'><notes>&x;</notes></dive></dives></divelog>"""
        assertTrue(DiveImport.read(xxe.toByteArray(), rome) is DiveImportResult.Error)
    }

    @Test fun `durate e numeri di Subsurface`() {
        assertEquals(2730, Subsurface.duration("45:30 min"))
        assertEquals(3723, Subsurface.duration("1:02:03"))
        assertNull(Subsurface.duration(""))
        assertEquals(32.0, Subsurface.number("32.0%")!!, 0.0)
        assertNull(Subsurface.number(null))
    }
}
