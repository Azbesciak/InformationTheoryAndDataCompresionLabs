package cs.ti.labs

class HuffmanCoder {
    public static final int CODE_SIZE = 4

    Map correspondance(Node n, Map corresp = [:], String prefix = '') {
        if (n.isLeaf()) {
            corresp[n.letter] = prefix ?: '0'
        } else {
            correspondance(n.left, corresp, prefix + '0')
            correspondance(n.right, corresp, prefix + '1')
        }
        corresp
    }

    Map huffmanCode(String message) {
        def queue = message.toList().countBy { it } // char frequencies
                .collect { String letter, int freq ->   // transformed into tree nodes
            new Node(letter, freq)
        } as TreeSet // put in a queue that maintains ordering
        queue.sort(true) {n -> -n.freq}
        while (queue.size() > 1) {
            def (nodeLeft, nodeRight) = [queue.pollFirst(), queue.pollFirst()]

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
        def coding = code.collectEntries { key, value -> [key.charAt(0), new BigInteger("1$value", 2).intValue()] }
        def header = Codec.prepareHeader(coding as Map<Character, Integer>, CODE_SIZE)
        def bytes = Codec.prepareFileBytes(header, binaryString)
        bytes
    }

    String encode(CharSequence msg, Map codeTable) {
        msg.collect { codeTable[it] }.join()
    }

    String decode(byte[] encoded) {
        def t = Codec.splitHeaderAndContent(encoded)
        byte[] content = t._1()
        int lastCharSize = t._2()
        byte[] header = t._3()
        def coding = Codec.getRawCoding(header, CODE_SIZE + 1)
        def javaCoding = Codec.toJava(coding).collectEntries {k, v -> [(k.drop(1)) : v]}
        def binaryString = Codec.getContentBinaryString(content, lastCharSize)
        decode(binaryString, javaCoding)
    }

    String decode(String codedMsg, Map codeTable) {
        StringBuilder message = new StringBuilder()
        while (!codedMsg.isEmpty()) {
            def pair = codeTable.find { k, v -> codedMsg.startsWith(k) }
            if (pair != null) {
                message.append(pair.value)
                codedMsg = codedMsg.drop(pair.key.size())
            } else if (!codedMsg.isEmpty()) {
                throw new IllegalStateException("$codedMsg not found in coding: $codeTable")
            }
        }
        message.toString()
    }

}