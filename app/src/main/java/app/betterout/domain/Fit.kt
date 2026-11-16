package app.betterout.domain

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Un messaggio di un file FIT: numero globale e campi per numero. I valori "non validi" del protocollo non compaiono. */
data class FitMessage(val number: Int, val fields: Map<Int, Any>) {
    fun long(field: Int): Long? = (fields[field] as? Number)?.toLong()
    fun double(field: Int): Double? = (fields[field] as? Number)?.toDouble()
}

class FitFormatException(message: String) : Exception(message)

/**
 * Lettore del formato binario FIT (Flexible and Interoperable Data Transfer), scritto dalla specifica pubblica.
 * Restituisce tutti i messaggi di dati; i campi degli sviluppatori sono saltati, il controllo CRC no.
 * Più file FIT concatenati sono letti uno dopo l'altro.
 */
object FitReader {
    private class Definition(val number: Int, val order: ByteOrder, val fields: List<Triple<Int, Int, Int>>, val developerBytes: Int)

    fun read(bytes: ByteArray): List<FitMessage> {
        val out = ArrayList<FitMessage>()
        var start = 0
        while (start < bytes.size) {
            if (bytes.size - start < 12) break
            val headerSize = bytes[start].toInt() and 0xFF
            if (headerSize < 12 || start + headerSize > bytes.size) throw FitFormatException("intestazione FIT non valida")
            val signature = String(bytes, start + 8, 4, Charsets.US_ASCII)
            if (signature != ".FIT") throw FitFormatException("non è un file FIT")
            val dataSize = ByteBuffer.wrap(bytes, start + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
            val end = start + headerSize + dataSize.toInt()
            if (end > bytes.size) throw FitFormatException("file FIT troncato")
            readRecords(bytes, start + headerSize, end, out)
            start = end + 2 // CRC del file
        }
        if (out.isEmpty()) throw FitFormatException("file FIT senza dati")
        return out
    }

    private fun readRecords(bytes: ByteArray, from: Int, to: Int, out: MutableList<FitMessage>) {
        val definitions = arrayOfNulls<Definition>(16)
        var lastTimestamp = 0L
        var p = from
        while (p < to) {
            val header = bytes[p++].toInt() and 0xFF
            when {
                header and 0x80 != 0 -> {
                    // Intestazione compressa: messaggio di dati con 5 bit di scarto sull'ultimo timestamp.
                    val def = definitions[(header shr 5) and 0x03] ?: throw FitFormatException("messaggio senza definizione")
                    val offset = (header and 0x1F).toLong()
                    val base = lastTimestamp and 0x1FL.inv()
                    lastTimestamp = if (offset >= (lastTimestamp and 0x1F)) base + offset else base + offset + 0x20
                    val fields = HashMap<Int, Any>()
                    p = readData(bytes, p, def, fields)
                    fields[253] = lastTimestamp
                    out += FitMessage(def.number, fields)
                }
                header and 0x40 != 0 -> {
                    val developer = header and 0x20 != 0
                    p++ // riservato
                    val order = if (bytes[p++].toInt() == 1) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
                    val number = ByteBuffer.wrap(bytes, p, 2).order(order).short.toInt() and 0xFFFF
                    p += 2
                    val count = bytes[p++].toInt() and 0xFF
                    val fields = (0 until count).map { i -> Triple(bytes[p + i * 3].toInt() and 0xFF, bytes[p + i * 3 + 1].toInt() and 0xFF, bytes[p + i * 3 + 2].toInt() and 0xFF) }
                    p += count * 3
                    var devBytes = 0
                    if (developer) {
                        val devCount = bytes[p++].toInt() and 0xFF
                        repeat(devCount) { i -> devBytes += bytes[p + i * 3 + 1].toInt() and 0xFF }
                        p += devCount * 3
                    }
                    definitions[header and 0x0F] = Definition(number, order, fields, devBytes)
                }
                else -> {
                    val def = definitions[header and 0x0F] ?: throw FitFormatException("messaggio senza definizione")
                    val fields = HashMap<Int, Any>()
                    p = readData(bytes, p, def, fields)
                    (fields[253] as? Long)?.let { lastTimestamp = it }
                    out += FitMessage(def.number, fields)
                }
            }
            if (p > to) throw FitFormatException("messaggio oltre la fine del file")
        }
    }

    private fun readData(bytes: ByteArray, at: Int, def: Definition, into: MutableMap<Int, Any>): Int {
        var p = at
        for ((field, size, baseType) in def.fields) {
            if (p + size > bytes.size) throw FitFormatException("file FIT troncato")
            value(ByteBuffer.wrap(bytes, p, size).order(def.order), size, baseType)?.let { into[field] = it }
            p += size
        }
        return p + def.developerBytes
    }

    /** Il primo elemento del campo, oppure null se vale "non valido" o il tipo non serve. */
    private fun value(b: ByteBuffer, size: Int, baseType: Int): Any? = when (baseType and 0x1F) {
        0x00, 0x02, 0x0A, 0x0D -> (b.get().toInt() and 0xFF).takeIf { if (baseType and 0x1F == 0x0A) it != 0 else it != 0xFF }?.toLong()
        0x01 -> b.get().toInt().takeIf { it != 0x7F }?.toLong()
        0x03 -> if (size >= 2) b.short.toInt().takeIf { it != 0x7FFF }?.toLong() else null
        0x04, 0x0B -> if (size >= 2) (b.short.toInt() and 0xFFFF).takeIf { if (baseType and 0x1F == 0x0B) it != 0 else it != 0xFFFF }?.toLong() else null
        0x05 -> if (size >= 4) b.int.takeIf { it != 0x7FFFFFFF }?.toLong() else null
        0x06, 0x0C -> if (size >= 4) (b.int.toLong() and 0xFFFFFFFFL).takeIf { if (baseType and 0x1F == 0x0C) it != 0L else it != 0xFFFFFFFFL } else null
        0x08 -> if (size >= 4) b.float.takeIf { !it.isNaN() && java.lang.Float.floatToRawIntBits(it) != -1 }?.toDouble() else null
        0x09 -> if (size >= 8) b.double.takeIf { !it.isNaN() } else null
        0x07 -> String(ByteArray(size).also { b.get(it) }, Charsets.UTF_8).substringBefore('\u0000').ifEmpty { null }
        else -> null
    }
}
