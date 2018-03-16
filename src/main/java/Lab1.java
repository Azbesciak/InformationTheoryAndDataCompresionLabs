import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.Traversable;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Lab1 {
    private static final String SEED_WORD = "probability";
    private static final int DEEPTH = 5;
    private static final int GENERATED_STRING_LEN = 1000;

    public static void main(String[] args) {
        List<Character> allChars = Utils.readFileCharacters(Utils.WIKI_TXT);
        io.vavr.collection.HashMap<Character, Integer> modified = getSingleObjectOccurrences(allChars);
        io.vavr.collection.HashMap<Character, Double> changed = getProbability(modified);
        modified.forEach(System.out::println);
        String randomString = generateRandomizedSequence(changed, GENERATED_STRING_LEN);
        System.out.println(randomString);
        System.out.println("medium len : " + getAvgLength(randomString));

        Map<Character, ObjectOrder<Character>> orders = getObjectsOrderMap(allChars, DEEPTH);
        String resultString = prepareMarkovString(orders, GENERATED_STRING_LEN, DEEPTH, () -> {
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

    public static <T> String prepareMarkovString(
            Map<T, ObjectOrder<T>> orders, int elements, int depth, Supplier<List<ObjectOrder<T>>> seedProvider) {
        List<ObjectOrder<T>> newSequence = seedProvider.get();
        for (int i = newSequence.size() - depth + 1; i < elements; i++) {
            ObjectOrder<T> next = newSequence.get(i - 1).getNext(getFollowingObjects(newSequence, depth, i));
            newSequence.add(orders.get(next.getSign()));
        }

        return newSequence.stream().map(ObjectOrder::getSign).map(Object::toString).collect(Collectors.joining());
    }

    public static <T> String generateRandomizedSequence(io.vavr.collection.HashMap<T, Double> changed, int elements) {
        Random random = new Random();
        return IntStream.range(0, elements)
                .mapToDouble(x -> random.nextDouble())
                .mapToObj(v -> getNext(changed, v))
                .collect(Collectors.joining());
    }

    private static <T> Map<T, ObjectOrder<T>> getObjectsOrderMap(List<T> objects, int depth) {
        int totalChars = objects.size();
        Map<T, ObjectOrder<T>> orders = new HashMap<>();
        for (int i = 0; i < totalChars - 1; i++) {
            int followingCharIndex = i + 1;
            orders.computeIfAbsent(objects.get(i), ObjectOrder::new)
                    .addOccurrence()
                    .addChars(getFollowingObjects(objects, depth, followingCharIndex));
        }
        ObjectOrder.normalize(orders);
        return orders;
    }

    private static <T> List<T> getFollowingObjects(List<T> objects, int maxLength, int firstCharIndex) {
        int lastIndex = Math.min(objects.size(), firstCharIndex + maxLength);
        if (firstCharIndex >= lastIndex)
            return Collections.emptyList();
        return objects.subList(firstCharIndex, lastIndex);
    }

    private static <T> io.vavr.collection.HashMap<T, Double> getProbability(io.vavr.collection.HashMap<T, Integer> modified) {
        int all = modified.values().sum().intValue();
        return modified.mapValues(a -> a / (double) all);
    }

    private static <T> io.vavr.collection.HashMap<T, Integer> getSingleObjectOccurrences(List<T> allObjects) {
        Map<T, Integer> occurrence = Array.ofAll(allObjects).groupBy(c -> c).mapValues(Traversable::size).toJavaMap();
        return io.vavr.collection.HashMap.ofAll(occurrence);
    }

    private static <T> String getNext(io.vavr.collection.HashMap<T, Double> changed, double v) {
        for (Tuple2<T, Double> val : changed) {
            if (val._2 >= v) {
                return val._1.toString();
            } else {
                v -= val._2;
            }
        }
        return "";
    }
}
