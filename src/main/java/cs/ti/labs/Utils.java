package cs.ti.labs;

import io.vavr.Tuple2;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Utils {
    public static final String SEED_WORD = "probability";

    public static final String HAMLET_TXT = "norm_hamlet.txt";
    public static final String ROMEO_TXT = "norm_romeo_and_juliet.txt";
    public static final String WIKI_TXT = "norm_wiki_sample.txt";

    public static List<Character> readFileCharacters(String fileName, int labNum) {
        return parseFile(fileName, labNum, Utils::readAllChars);
    }

    public static String getFileString(String fileName, int labNum) {
        return parseFile(fileName, labNum, s -> s.collect(Collectors.joining(" ")));
    }

    public static String getImage(String fileName, int labNum) throws IOException {
        byte[] bytes = getFileBytes(fileName, labNum);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] getFileBytes(String fileName, int labNum) throws IOException {
        Path path = Paths.get(getLabDir(labNum) + fileName);
        return Files.readAllBytes(path);
    }

    public static List<String> getFilesInDirectory(int labNum) {
        File file = new File(getLabDir(labNum));
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                return Arrays.stream(files)
                        .filter(File::isFile)
                        .map(File::getName)
                        .collect(Collectors.toList());
            }
        }
        System.err.println("no files found in lab " + labNum);
        return Collections.emptyList();
    }

    public static String getLabDir(int labNum) {
        return "./src/main/resources/lab" + labNum + "/";
    }

    private static <T> T parseFile(String fileName, int labNum, Function<Stream<String>, T> mapper) {
        try (Stream<String> lines = Files.lines(Paths.get(getLabDir(labNum) + fileName))) {
            return mapper.apply(lines);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Character> readAllChars(Stream<String> lines) {
        return lines.map(String::chars).flatMap(Utils::getCharacterStream).collect(Collectors.toList());
    }

    public static Stream<Character> getCharacterStream(IntStream s) {
        return s.mapToObj(i -> (char) i);
    }

    static <T> List<T> getFollowingObjects(List<T> objects, int maxLength, int firstCharIndex) {
        int lastIndex = Math.min(objects.size(), firstCharIndex + maxLength);
        if (firstCharIndex >= lastIndex)
            return Collections.emptyList();
        return objects.subList(firstCharIndex, lastIndex);
    }

    @NotNull
    public static <T> String prepareMarkovString(
            Map<T, ObjectOrder<T>> orders, int elements, int depth, String delimiter,
            Supplier<List<ObjectOrder<T>>> seedProvider) {
        List<ObjectOrder<T>> newSequence = seedProvider.get();
        for (int i = Math.max(1, newSequence.size() - depth + 1); i < elements; i++) {
            ObjectOrder<T> next = newSequence.get(Math.max(i - 1, 0))
                    .getNext(getFollowingObjects(newSequence, depth, i));
            newSequence.add(orders.get(next.getSign()));
        }
        return newSequence.stream().map(ObjectOrder::getSign).map(Object::toString).collect(Collectors.joining(delimiter));
    }

    public static <T> String generateRandomizedSequence(io.vavr.collection.HashMap<T, Double> changed, int elements) {
        Random random = new Random();
        return IntStream.range(0, elements)
                .mapToDouble(x -> random.nextDouble())
                .mapToObj(v -> getNext(changed, v))
                .collect(Collectors.joining());
    }

    public static <T> Map<T, ObjectOrder<T>> getObjectsOrderMap(List<T> objects, int depth) {
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
