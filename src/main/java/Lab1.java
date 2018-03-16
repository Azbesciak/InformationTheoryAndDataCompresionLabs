import io.vavr.Tuple2;
import io.vavr.collection.Array;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Lab1 {
    private static final String SEED_WORD = "probability";
    private static final int DEEPTH = 5;
    private static final int GENERATED_STRING_LEN = 1000;

    public static void main(String[] args) {
        List<Character> allChars = Utils.readFileCharacters(Utils.WIKI_TXT);
        io.vavr.collection.HashMap<Character, AtomicInteger> modified = getSingleLettersOccurrences(allChars);
        io.vavr.collection.HashMap<Character, Double> changed = getProbability(modified);
        modified.forEach(System.out::println);
        String randomString = generateRandomString(changed);
        System.out.println(randomString);
        System.out.println("medium len : " + Array.of(randomString.split("\\s+")).map(String::length).average().getOrElse(0.));

        HashMap<Character, CharOrder<Character>> orders = getCharOrderMap(allChars, DEEPTH);
        String resultString = prepareMarkovString(orders);
        String[] split = resultString.split("\\s");
        double avgResLen = Arrays.stream(split).mapToInt(String::length).average().orElse(0);
        System.out.println(resultString);
        System.out.println("medium len : " + avgResLen);
    }

    private static String prepareMarkovString(HashMap<Character, CharOrder<Character>> orders) {
        List<CharOrder> newSequence = new ArrayList<>(GENERATED_STRING_LEN);
        Utils.getCharacterStream(SEED_WORD.chars()).map(orders::get).forEach(newSequence::add);
        for (int i = newSequence.size() - DEEPTH + 1; i < GENERATED_STRING_LEN; i++) {
            CharOrder<Character> next = newSequence.get(i - 1).getNext(getFollowingChars(newSequence, DEEPTH, i));
            newSequence.add(orders.get(next.getSign()));
        }

        return newSequence.stream().map(CharOrder::getSign).map(Object::toString).collect(Collectors.joining());
    }

    private static String generateRandomString(io.vavr.collection.HashMap<Character, Double> changed) {
        Random random = new Random();
        return IntStream.range(0, GENERATED_STRING_LEN)
                .mapToDouble(x -> random.nextDouble())
                .mapToObj(v -> getNextCharacter(changed, v))
                .collect(Collectors.joining());
    }

    @SuppressWarnings("unchecked")
    private static HashMap<Character, CharOrder<Character>> getCharOrderMap(List<Character> allChars, int depth) {
        int totalChars = allChars.size();
        HashMap<Character, CharOrder<Character>> orders = new HashMap<>();
        for (int i = 0; i < totalChars - 1; i++) {
            int followingCharIndex = i + 1;
            orders.computeIfAbsent(allChars.get(i), CharOrder::new)
                    .addOccurrence()
                    .addChars(getFollowingChars(allChars, depth, followingCharIndex));
        }
        CharOrder.normalize(orders);
        return orders;
    }

    private static <T> List<T> getFollowingChars(List<T> allChars, int maxLength, int firstCharIndex) {
        int lastIndex = Math.min(allChars.size(), firstCharIndex + maxLength);
        if (firstCharIndex >= lastIndex)
            return Collections.emptyList();
        return allChars.subList(firstCharIndex, lastIndex);
    }

    private static io.vavr.collection.HashMap<Character, Double> getProbability(io.vavr.collection.HashMap<Character, AtomicInteger> modified) {
        int all = modified.values().sum().intValue();
        return modified.mapValues(AtomicInteger::get).mapValues(a -> a / (double) all);
    }

    private static io.vavr.collection.HashMap<Character, AtomicInteger> getSingleLettersOccurrences(List<Character> allChars) {
        Map<Character, AtomicInteger> occurances = new HashMap<>();
        allChars.forEach(i -> occurances.computeIfAbsent(i, x -> new AtomicInteger(0)).incrementAndGet());
        return io.vavr.collection.HashMap.ofAll(occurances);
    }

    private static String getNextCharacter(io.vavr.collection.HashMap<Character, Double> changed, double v) {
        for (Tuple2<Character, Double> val : changed) {
            if (val._2 >= v) {
                return val._1.toString();
            } else {
                v -= val._2;
            }
        }
        return "";
    }
}
