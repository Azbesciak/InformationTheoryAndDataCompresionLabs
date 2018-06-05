package cs.ts.labs

import cs.ti.labs.Utils
import java.io.ByteArrayOutputStream


private fun buildDictionary(): MutableMap<List<Byte>, Int> {
    return (Byte.MIN_VALUE..Byte.MAX_VALUE)
            .mapIndexed { i, c -> c.toByte().toArray() to i }
            .toMap().toMutableMap()
}

fun Byte.toArray() = listOf(this)


object Lzw {
    fun compress(uncompressed: ByteArray): MutableList<Int> {
        // Build the dictionary.
        val dictionary = buildDictionary()
        var w = listOf<Byte>()
        val result = mutableListOf<Int>()
        for (c in uncompressed) {
            val wc = w + c
            w = if (dictionary.contains(wc))
                wc
            else {
                result.add(dictionary[w]!!)
                dictionary[wc] = dictionary.size + 1
                c.toArray()
            }
        }

        if (w.isNotEmpty()) result.add(dictionary[w]!!)
        return result
    }

    fun decompress(compressed: MutableList<Int>): ByteArray {
        val dictionary = buildDictionary()
                .map { it.value to it.key }
                .toMap().toMutableMap()

        val result = ByteArrayOutputStream()
        var w = compressed.removeAt(0).toByte().toArray()
        result.write(w.toByteArray())
        for (k in compressed) {
            val entry = when {
                dictionary.containsKey(k) -> dictionary[k]!!
                k == dictionary.size -> w + w[0]
                else -> throw IllegalArgumentException("Bad compressed k: $k")
            }
            result.write(entry.toByteArray())
            dictionary[dictionary.size] = w + entry[0]
            w = entry
        }
        return result.toByteArray()
    }
}

fun main(args: Array<String>) {
    val text = Utils.getFileBytes("lena.bmp", 6)!!
    val compressed = Lzw.compress(text)

    val decompressed = Lzw.decompress(compressed)
    if (!text.contentEquals(decompressed)) {
        throw IllegalStateException("Invalid after decoding")
    } else {
        println("ok")
    }
}