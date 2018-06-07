package cs.ts.labs

import cs.ti.labs.HuffmanCoder
import cs.ti.labs.Lab4
import cs.ti.labs.Utils
import java.io.ByteArrayOutputStream
import java.util.*

sealed class HuffmanTree(val freq: Int) : Comparable<HuffmanTree> {
    override fun compareTo(other: HuffmanTree) = freq - other.freq
}

class HuffmanLeaf(freq: Int, val value: Char) : HuffmanTree(freq)

class HuffmanNode(val left: HuffmanTree, val right: HuffmanTree) : HuffmanTree(left.freq + right.freq)

fun Byte.asChar() =
        if (toInt() < 0)
            (256 + toInt()).toChar()
        else
            toChar()

fun Char.asByte() = if (toInt() > Byte.MAX_VALUE) (toInt()-256).toByte() else toByte()


fun ByteArray.toListOfChars() = map { it.asChar() }.toList()

object HuffmanCodec {
    private fun buildTree(charFreqs: Map<Byte, Int>): HuffmanTree {
        val trees = PriorityQueue<HuffmanTree>()
        charFreqs.mapKeys { it.key.asChar() }
                .toList().sortedByDescending { it.second }
                .forEach { trees.offer(HuffmanLeaf(it.second, it.first)) }

        assert(trees.size > 0)
        while (trees.size > 1) {
            val a = trees.poll()
            val b = trees.poll()
            trees.offer(HuffmanNode(a, b))
        }

        return trees.poll()
    }

    private fun toMap(tree: HuffmanTree, codding: MutableMap<Char, String>, prefix: String = "") {
        when (tree) {
            is HuffmanLeaf -> codding[tree.value] = prefix
            is HuffmanNode -> {
                toMap(tree.left, codding, prefix + '0')
                toMap(tree.right, codding, prefix + '1')
            }
        }
    }

    private fun encode(msg: CharSequence, codeTable: Map<Char, String>) =
            msg.map { codeTable[it] }.joinToString("")

    private fun decode(codedMsg: String, codeTable: Map<String, Char>): ByteArray {
        val message = ByteArrayOutputStream()
        var key = ""
        codedMsg.toCharArray().forEach {
            key += it
            val letter = codeTable[key]
            if (letter != null) {
                val byteArrayOf = byteArrayOf(letter.asByte())
                message.write(byteArrayOf)
                key = ""
            }
        }
        if (key.isNotEmpty()) {
            throw IllegalStateException("$key left without assignment")
        }
        return message.toByteArray()
    }

    fun encode(toEncode: ByteArray): ByteArray {
        val freqs = toEncode.groupBy { it }.mapValues { it.value.size }

        val tree = buildTree(freqs)
        val codding = mutableMapOf<Char, String>()
        toMap(tree, codding)
        val encoded = encode(toEncode.toListOfChars().joinToString(""), codding)
        return HuffmanCoder.encodeToBytes(encoded, codding)
    }

    fun decode(bytes: ByteArray): ByteArray = HuffmanCoder.decode(bytes, ::decode)

    @JvmStatic
    fun main(args: Array<String>) {
        val test = Utils.getFileBytes(Utils.WIKI_TXT, 1)
        val bytes = encode(test)
        Lab4.save(bytes, "huffmankt.txt", 5)
        val decoded = decode(bytes)
        compare("Different") { decoded.contentEquals(test) }
    }

    fun compare(message: String, isEqual: () -> Boolean) {
        if (!isEqual()) {
            throw IllegalStateException(message)
        } else {
            println("ok")
        }
    }
}

