package cs.ts.labs

import cs.ti.labs.Codec
import cs.ti.labs.Lab4
import cs.ti.labs.Utils
import java.io.ByteArrayOutputStream
import java.math.BigInteger


private fun buildDictionary(): MutableMap<List<Byte>, Int> {
    return (Byte.MIN_VALUE..Byte.MAX_VALUE)
            .map { it.toByte().toList() to it }
            .toMap().toMutableMap()
}

fun Byte.toList() = listOf(this)


object Lzw {
    fun compress(uncompressed: ByteArray): MutableList<Int> {
        val dictionary = buildDictionary()
        var w = listOf<Byte>()
        val result = mutableListOf<Int>()
        for (c in uncompressed) {
            val wc = w + c
            w = if (dictionary.contains(wc))
                wc
            else {
                result.add(dictionary[w]!!)
                dictionary[wc] = dictionary.size
                listOf(c)
            }
        }

        if (w.isNotEmpty()) result.add(dictionary[w]!!)
        return result
    }

    fun decompress(compressed: List<Int>): ByteArray {
        val dictionary = buildDictionary()
                .map { it.value to it.key.toByteArray() }
                .toMap().toMutableMap()

        val result = ByteArrayOutputStream()
        val compressedWithoutFirst = compressed.drop(1)
        var w = dictionary[compressed[0]]!!

        result.write(w)
        for (k in compressedWithoutFirst) {
            val entry = when {
                dictionary.containsKey(k) -> dictionary[k]!!
                k == dictionary.size -> w + w[0]
                else -> throw IllegalArgumentException("Bad compressed k: $k")
            }
            result.write(entry)
            dictionary[dictionary.size] = w + entry[0]
            w = entry
        }
        return result.toByteArray()
    }
}

fun main(args: Array<String>) {
    val text = Utils.getFileBytes(Utils.WIKI_TXT, 1)!!
    val compressed = Lzw.compress(text)
    val toBytes = compressed.toBytes()

    Lab4.save(toBytes, "lzw.txt", 6)
    val decodeSaved = toBytes.decodeSaved()
    val decompressed = Lzw.decompress(decodeSaved)
    if (!text.contentEquals(decompressed)) {
        throw IllegalStateException("Invalid after decoding")
    } else {
        println("ok")
    }
}

fun ByteArray.decodeSaved(): List<Int> {
    val res = Codec.splitHeaderAndContent(this)
    val missing = res._2() as Int
    val header = res._3()!!
    val content = res._1()!!
    val maxSize = String(header).toInt()
    val contentBinaryString = Codec.getContentBinaryString(content, missing)
    return contentBinaryString.chunked(maxSize).map { BigInteger(it, 2).toInt() }
}

fun Int.toBytes() = BigInteger(toString()).toByteArray().toList()

fun List<Int>.toBytes(): ByteArray {
    val bytes = map { BigInteger(it.toBytes().toByteArray()).toString(2)!! }
    val maxSize = bytes.map { it.length }.max() ?: 0
    val body = bytes.joinToString("") { it.padStart(maxSize, '0') }
    return Codec.prepareFileBytes(maxSize.toString(), body)
}