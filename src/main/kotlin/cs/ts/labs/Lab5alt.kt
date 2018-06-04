package cs.ts.labs

import cs.ti.labs.HuffmanCoder
import cs.ti.labs.Lab4
import cs.ti.labs.Utils
import java.util.*

abstract class HuffmanTree(val freq: Int) : Comparable<HuffmanTree> {
    override fun compareTo(other: HuffmanTree) = freq - other.freq
}

class HuffmanLeaf(freq: Int, val value: Char) : HuffmanTree(freq)

class HuffmanNode(val left: HuffmanTree, val right: HuffmanTree) : HuffmanTree(left.freq + right.freq)

fun buildTree(charFreqs: IntArray): HuffmanTree {
    val trees = PriorityQueue<HuffmanTree>()

    charFreqs.forEachIndexed { index, freq ->
        if (freq > 0) trees.offer(HuffmanLeaf(freq, index.toChar()))
    }

    assert(trees.size > 0)
    while (trees.size > 1) {
        val a = trees.poll()
        val b = trees.poll()
        trees.offer(HuffmanNode(a, b))
    }

    return trees.poll()
}

fun toMap(tree: HuffmanTree, codding: MutableMap<Char, String>, prefix: String = "") {
    when (tree) {
        is HuffmanLeaf -> codding[tree.value] = prefix
        is HuffmanNode -> {
            toMap(tree.left, codding, prefix + '0')
            toMap(tree.right, codding, prefix + '1')
        }
    }
}

fun encode(msg: CharSequence, codeTable: Map<Char, String>) =
        msg.map { codeTable[it] }.joinToString("")

fun decode(codedMsg: String, codeTable: Map<String, String>): String {
    val message = StringBuilder()
    var key = ""
    codedMsg.toCharArray().forEach {
        key += it
        val letter = codeTable[key]
        if (letter != null) {
            message.append(letter)
            key = ""
        }
    }
    if (key.isNotEmpty()) {
        throw IllegalStateException("$key left without assignment")
    }
    return message.toString()
}

fun main(args: Array<String>) {
    val test = Utils.getFileString(Utils.WIKI_TXT, 1)

    val maxIndex = test.max()!!.toInt() + 1
    val freqs = IntArray(maxIndex) //256 enough for latin ASCII table, but dynamic size is more fun
    test.forEach { freqs[it.toInt()] += 1 }

    val tree = buildTree(freqs)
    val codding = mutableMapOf<Char, String>()
    toMap(tree, codding)
    val encoded = encode(test, codding)
    val bytes = HuffmanCoder.encodeToBytes(encoded, codding)
    Lab4.save(bytes, "huffmankt.txt", 5)
    val decoded = HuffmanCoder.decode(bytes, ::decode)
    if (decoded != test) {
        throw IllegalStateException("DIFFERENT")
    } else {
        println("OK")
    }
}