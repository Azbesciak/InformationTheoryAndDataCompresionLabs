import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CharOrder<T> {
    private final T sign;
    private double probability;
    private int occurance;
    private final Map<T, CharOrder<T>> next;

    public CharOrder(T sign) {
        this.sign = sign;
        this.next = new HashMap<>();
        this.probability = -1;
        this.occurance = 0;
    }

    CharOrder addOccurrence() {
        this.occurance++;
        return this;
    }

    public CharOrder getNext(List<CharOrder<T>> chars) {
        if (!chars.isEmpty()) {
            CharOrder charOrder = next.get(chars.get(0).sign);
            if (charOrder == null) {
                throw new IllegalStateException("Expected letter was not found");
            }
            return charOrder.getNext(getAllWithoutFirstCharacter(chars));
        }
        return getRandomFrom(next);
    }

    public static <T> CharOrder getRandomFrom(Map<T, CharOrder<T>> chars) {
        double val = new Random().nextDouble();
        for (Map.Entry<T, CharOrder<T>> e : chars.entrySet()) {
            if (val > e.getValue().probability) {
                val -= e.getValue().probability;
            } else {
                return e.getValue();
            }
        }
        throw new IllegalStateException("Could not specify letter");
    }

    public void addChars(List<T> chars) {
       addChars(next, chars);
    }

    public static <T> void addChars(Map<T, CharOrder<T>> all, List<T> chars) {
        if (!chars.isEmpty()) {
            all.computeIfAbsent(chars.get(0), CharOrder::new)
                    .addOccurrence()
                    .addChars(getAllWithoutFirstCharacter(chars));
        }
    }

    public static <T> void normalize(Map<T, CharOrder<T>> all) {
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

    public T getSign() {
        return sign;
    }

    @Override
    public String toString() {
        return sign.toString();
    }
}
