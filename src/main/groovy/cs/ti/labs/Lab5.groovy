package cs.ti.labs

class Lab5 {
    static void main(String[] args) {
        def fileString = Utils.getFileString(Utils.WIKI_TXT, 1)
        def codec = new Codec()
//        def bytes = codec.encode(fileString)
        def chars = Splitter.toJava(Splitter.splitWithChars(fileString, 1))
        def total = fileString.length()
        def occurences = chars
                .groupBy { v -> v.get(0) }
                .values()
                .stream()
                .map { v -> v.size() / total.doubleValue() }
                .mapToDouble { v -> v * log2(v as double) }
                .sum()
        println(occurences)
    }

    static double log2(double v) {
        return Math.log(2) / Math.log10(2)
    }
}
