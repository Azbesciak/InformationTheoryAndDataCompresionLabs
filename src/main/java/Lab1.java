import io.vavr.Tuple2;
import io.vavr.collection.Array;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Lab1 {

    public static final String SEED_WORD = "probability";

    public static void main(String[] args) {
        try (Stream<String> lines = Files.lines(Paths.get("./src/main/resources/lab1/norm_wiki_sample.txt"))) {
            List<Character> allChars = readAllChars(lines);
            io.vavr.collection.HashMap<Character, AtomicInteger> modified = getSingleLettersOccurrences(allChars);
            io.vavr.collection.HashMap<Character, Double> changed = getProbability(modified);
            modified.forEach(System.out::println);
            int length = 1000;
            Random random = new Random();
            String s = IntStream.range(0, length)
                    .mapToDouble(x -> random.nextDouble())
                    .mapToObj(v -> getNextCharacter(changed, v))
                    .collect(Collectors.joining());
            System.out.println(s);
            System.out.println("medium len - " + Array.of(s.split("\\s+")).map(String::length).average().getOrElse(0.));

            HashMap<Character, CharOrder> orders = new HashMap<>();
            int depth = 3;
            int totalChars = allChars.size();
            for (int i = 0; i < totalChars - 1; i++) {
                int followingCharIndex = i + 1;
                orders.computeIfAbsent(allChars.get(i), CharOrder::new)
                        .addOccurrence()
                        .addChars(getFollowingChars(allChars, depth, followingCharIndex));
            }
            CharOrder.normalize(orders);
            List<CharOrder> newSequence = new ArrayList<>(length);
            getCharacterStream(SEED_WORD.chars()).map(orders::get).forEach(newSequence::add);
            for (int i = newSequence.size() - depth+1; i < length; i++) {
                CharOrder next = newSequence.get(i - 1).getNext(getFollowingChars(newSequence, depth, i));
                newSequence.add(orders.get(next.getSign()));
            }
            newSequence.forEach(System.out::print);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private static List<Character> readAllChars(Stream<String> lines) {
        return lines.map(String::chars).flatMap(Lab1::getCharacterStream).collect(Collectors.toList());
    }

    private static Stream<Character> getCharacterStream(IntStream s) {
        return s.mapToObj(i -> (char) i);
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
