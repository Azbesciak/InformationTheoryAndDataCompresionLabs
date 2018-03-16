package cs.ti.labs;

import io.vavr.collection.Array;
import io.vavr.collection.Traversable;

import java.util.*;

import static cs.ti.labs.Utils.SEED_WORD;

public class Lab1 {

    private static final int DEPTH = 5;
    private static final int GENERATED_STRING_LEN = 1000;

    public static void main(String[] args) {
        List<Character> allChars = Utils.readFileCharacters(Utils.WIKI_TXT);
        io.vavr.collection.HashMap<Character, Integer> modified = getSingleObjectOccurrences(allChars);
        modified.forEach(System.out::println);
        io.vavr.collection.HashMap<Character, Double> changed = getProbability(modified);
        String randomString = Utils.generateRandomizedSequence(changed, GENERATED_STRING_LEN);
        System.out.println(randomString);
        System.out.println("medium len : " + getAvgLength(randomString));

        Map<Character, ObjectOrder<Character>> orders = Utils.getObjectsOrderMap(allChars, DEPTH);
        String resultString = Utils.prepareMarkovString(orders, GENERATED_STRING_LEN, DEPTH, "", () -> {
            List<ObjectOrder<Character>> newSequence = new ArrayList<>(GENERATED_STRING_LEN);
            Utils.getCharacterStream(SEED_WORD.chars()).map(orders::get).forEach(newSequence::add);
            return newSequence;
        });
        double avgResLen = getAvgLength(resultString);
        System.out.println(resultString);
        System.out.println("medium len : " + avgResLen);
    }

    private static Double getAvgLength(String stringToMeasure) {
        return Array.of(stringToMeasure.split("\\s+"))
                .map(String::length)
                .average().getOrElse(0.);
    }

    private static <T> io.vavr.collection.HashMap<T, Double> getProbability(io.vavr.collection.HashMap<T, Integer> modified) {
        int all = modified.values().sum().intValue();
        return modified.mapValues(a -> a / (double) all);
    }

    private static <T> io.vavr.collection.HashMap<T, Integer> getSingleObjectOccurrences(List<T> allObjects) {
        Map<T, Integer> occurrence = Array.ofAll(allObjects).groupBy(c -> c).mapValues(Traversable::size).toJavaMap();
        return io.vavr.collection.HashMap.ofAll(occurrence);
    }

}
