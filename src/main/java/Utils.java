import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Utils {
    public static final String HAMLET_TXT = "norm_hamlet.txt";
    public static final String ROMEO_TXT = "norm_romeo_and_juliet.txt";
    public static final String WIKI_TXT = "norm_wiki_sample.txt";

    public static List<Character> readFileCharacters(String fileName) {
        try (Stream<String> lines = Files.lines(Paths.get("./src/main/resources/lab1/" + fileName))) {
            return readAllChars(lines);
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
}
