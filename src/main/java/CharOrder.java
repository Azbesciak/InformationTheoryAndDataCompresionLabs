import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CharOrder {
    private final Character sign;
    private double probability;
    private int occurance;
    private final Map<Character, CharOrder> next;


    public CharOrder(Character sign) {
        this.sign = sign;
        this.next = new HashMap<>();
        this.probability = -1;
        this.occurance = 0;
    }

    CharOrder addOccurrence() {
        this.occurance++;
        return this;
    }

    public CharOrder getNext(List<CharOrder> chars) {
        if (!chars.isEmpty()) {
            CharOrder charOrder = next.get(chars.get(0).sign);
            if (charOrder == null) {
                throw new IllegalStateException("Expected letter was not found");
            }
            return charOrder.getNext(getAllWithoutFirstCharacter(chars));
        }
        return getRandomFrom(next);
    }

    public static CharOrder getRandomFrom(Map<Character, CharOrder> chars) {
        double val = new Random().nextDouble();
        for (Map.Entry<Character, CharOrder> e : chars.entrySet()) {
            if (val > e.getValue().probability) {
                val -= e.getValue().probability;
            } else {
                return e.getValue();
            }
        }
        throw new IllegalStateException("Could not specify letter");
    }

    public void addChars(List<Character> chars) {
       addChars(next, chars);
    }

    public static void addChars(Map<Character, CharOrder> all, List<Character> chars) {
        if (!chars.isEmpty()) {
            all.computeIfAbsent(chars.get(0), CharOrder::new)
                    .addOccurrence()
                    .addChars(getAllWithoutFirstCharacter(chars));
        }
    }

    public static void normalize(Map<Character, CharOrder> all) {
        if (all.isEmpty())
            return;
        int sum = all.entrySet().stream().map(Map.Entry::getValue).mapToInt(t -> t.occurance).sum();
        all.forEach((k, v) -> v.normalize(sum));
    }

    private static <T> List<T> getAllWithoutFirstCharacter(List<T> chars) {
        return chars.subList(1, chars.size());
    }

    public void normalize(int total) {
        this.probability = occurance / (double)total;
        normalize(next);
    }

    public Character getSign() {
        return sign;
    }

    @Override
    public String toString() {
        return sign.toString();
    }
}
