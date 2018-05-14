package cs.ti.labs

import java.math.RoundingMode
import java.util.function.Function

class Lab5 {
    private static final String OUTPUT_FILE_NAME = "huffman.txt"
    private static final boolean withValidation = true

    static def compare(Function<String, byte[]> coder) {
        def fileString = Utils.getFileString(Utils.WIKI_TXT, 1)
        def bytes = coder.apply(fileString)
        def chars = Splitter.toJava(Splitter.splitWithChars(fileString, 1)).get(0)
        def allCharsCount = new BigDecimal(fileString.length())
        def entropy = chars.countBy { it }
                .collect { s, v ->
            new BigDecimal(v).setScale(10, BigDecimal.ROUND_HALF_UP)
                    .divide(allCharsCount, 10, RoundingMode.HALF_UP)
        }
        .collect { -it * log2(it as BigDecimal) }
                .sum()
        println("original: ${entropy / 8}")
        println("encoded: ${entropy / getCodingWordLength(bytes.length, fileString.length())}")
    }

    static def getCodingWordLength(int coded, int original) {
        coded / original.doubleValue() * 8
    }

    static void main(String[] args) {
//        compare(new Codec().&encode)
        compare { huffmanCode(it) }
    }

    private static byte[] huffmanCode(String original) {
        def coder = new HuffmanCoder()
        def codded = coder.encode(original)
        Lab4.save(codded, OUTPUT_FILE_NAME, 5)
        if (withValidation) validate(coder, original)
        codded
    }

    private static void validate(HuffmanCoder coder, CharSequence original) {
        def bytes = Utils.getFileBytes(OUTPUT_FILE_NAME, 5)
        def decoded = coder.decode(bytes)
        if (!decoded.equals(original)) {
            throw new IllegalStateException("Not the same after decoding")
        }
    }

    static double log2(BigDecimal v) {
        Math.log10(v) / Math.log10(2)
    }
}
