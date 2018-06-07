package cs.ts.labs

import cs.ti.labs.Codec
import cs.ti.labs.Lab4
import cs.ti.labs.Utils
import cs.ts.labs.HuffmanCodec.compare
import java.io.ByteArrayOutputStream
import java.math.BigInteger

const val FILE_NAME = Utils.WIKI_TXT
const val LAB_NUM = 1

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
    val text = Utils.getFileBytes(FILE_NAME, LAB_NUM)!!
    val compressed = Lzw.compress(text)
    val lzwBytes = compressed.toBytes()
    val huffmanEncoded = HuffmanCodec.encode(lzwBytes)
    Lab4.save(huffmanEncoded, "lzw_$FILE_NAME", 6)
    val huffmanDecoded = HuffmanCodec.decode(huffmanEncoded)
    compare("Invalid after huffman decoding") { lzwBytes.contentEquals(huffmanDecoded) }
    val decodeSaved = lzwBytes.decodeSaved()
    compare("Invalid lzw codes") { compressed == decodeSaved }
    val decompressed = Lzw.decompress(decodeSaved)
    compare("Invalid after decoding") { text.contentEquals(decompressed) }
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