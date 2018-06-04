package cs.ti.labs

import groovy.transform.CompileStatic

import java.util.function.BiFunction
import java.util.stream.Collectors

class HuffmanCoder {
    public static final int CODE_SIZE = 4

    Map<Character, String> correspondance(Node n, Map<Character, String> corresp = [:], String prefix = '') {
        if (n.isLeaf()) {
            corresp[n.letter.charAt(0)] = prefix ?: '0'
        } else {
            correspondance(n.left, corresp, prefix + '0')
            correspondance(n.right, corresp, prefix + '1')
        }
        corresp
    }

    Map<Character, String> huffmanCode(String message) {
        def queue = message.toList().countBy { it } // char frequencies
                .collect { String letter, int freq ->   // transformed into tree nodes
            new Node(letter, freq)
        } as TreeSet<Node> // put in a queue that maintains ordering
        queue.sort(true) { n -> -(n as Node).freq }
        while (queue.size() > 1) {
            def (Node nodeLeft, Node nodeRight) = [queue.pollFirst(), queue.pollFirst()]

            queue << new Node(
                    freq: nodeLeft.freq + nodeRight.freq,
                    letter: nodeLeft.letter + nodeRight.letter,
                    left: nodeLeft, right: nodeRight
            )
        }

        correspondance(queue.pollFirst())
    }

    byte[] encode(String content) {
        def code = huffmanCode(content)
        def binaryString = encode(content, code)
        encodeToBytes(binaryString, code)
    }

    static byte[] encodeToBytes(String binaryString, Map<Character, String> code) {
        def coding = code.collectEntries { Character key, String value -> [key, new BigInteger("1$value", 2).intValue()] }
        def header = Codec.prepareHeader(coding as Map<Object, Object>, CODE_SIZE)
        Codec.prepareFileBytes(header, binaryString)
    }

    static String encode(CharSequence msg, Map<Character, String> codeTable) {
        msg.chars().mapToObj{ codeTable[it as char] }.collect(Collectors.joining())
    }

    static String decode(byte[] encoded) {
        decode(encoded, this.&decode)
    }

    static String decode(byte[] encoded, BiFunction<String, Map<String, String>, String> decoder) {
        def t = Codec.splitHeaderAndContent(encoded)
        byte[] content = t._1()
        int lastCharSize = t._2() as int
        byte[] header = t._3()
        def coding = Codec.getRawCoding(header, CODE_SIZE + 1)
        def javaCoding = Codec.toJava(coding).collectEntries { k, v -> [(k.drop(1)): v] }
        def binaryString = Codec.getContentBinaryString(content, lastCharSize)
        decoder.apply(binaryString, javaCoding as Map<String, String>)
    }

    @CompileStatic
    static String decode(String codedMsg, Map<String, String> codeTable) {
        def message = new StringBuilder()
        def key = ""
        codedMsg.toCharArray().each {
            key += it
            def letter = codeTable[key]
            if (letter != null) {
                message.append(letter)
                key = ""
            }
        }
        if (!key.isEmpty()) {
            throw new IllegalStateException("$key left without assignment")
        }
        return message.toString()
    }
}