package cs.ti.labs

import groovy.transform.Canonical
import groovy.transform.CompileStatic

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
        def coding = code.collectEntries { Character key, String value -> [key, new BigInteger("1$value", 2).intValue()] }
        def header = Codec.prepareHeader(coding as Map<Object, Object>, CODE_SIZE)
        def bytes = Codec.prepareFileBytes(header, binaryString)
        bytes
    }

    static String encode(CharSequence msg, Map<Character, String> codeTable) {
        msg.chars().mapToObj{ codeTable[it as char] }.collect(Collectors.joining())
    }

    static String decode(byte[] encoded) {
        def t = Codec.splitHeaderAndContent(encoded)
        byte[] content = t._1()
        int lastCharSize = t._2() as int
        byte[] header = t._3()
        def coding = Codec.getRawCoding(header, CODE_SIZE + 1)
        def javaCoding = Codec.toJava(coding).collectEntries { k, v -> [(k.drop(1)): v] }
        def binaryString = Codec.getContentBinaryString(content, lastCharSize)
        decode(binaryString, javaCoding as Map<String, String>)
    }

    @CompileStatic
    static String decode(String codedMsg, Map<String, String> codeTable) {
        StringBuilder message = new StringBuilder()
        def codes = codeTable.collect { k, v -> new Code(code: k, letter: v, codeSize: k.size()) }.sort { it.code.size() }
        while (!codedMsg.isEmpty()) {
            def node = codes.find { codedMsg.startsWith(it.code) }
            if (node == null) {
                new IllegalStateException("$codedMsg not found in coding: $codeTable")
            }
            message.append(node.letter)
            codedMsg = codedMsg.substring(node.codeSize)
        }
        message.toString()
    }

    @Canonical
    @CompileStatic
    static class Code {
        String letter
        String code
        int codeSize
    }
}