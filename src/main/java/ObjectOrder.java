import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ObjectOrder<T> {
    private final T sign;
    private double probability;
    private int occurrence;
    private final Map<T, ObjectOrder<T>> next;

    public ObjectOrder(T sign) {
        this.sign = sign;
        this.next = new HashMap<>();
        this.probability = -1;
        this.occurrence = 0;
    }

    ObjectOrder<T> addOccurrence() {
        this.occurrence++;
        return this;
    }

    public ObjectOrder<T> getNext(List<ObjectOrder<T>> chars) {
        if (!chars.isEmpty()) {
            ObjectOrder<T> objectOrder = next.get(chars.get(0).sign);
            if (objectOrder == null) {
                throw new IllegalStateException("Expected letter was not found");
            }
            return objectOrder.getNext(getAllWithoutFirstCharacter(chars));
        }
        return getRandomFrom(next);
    }

    public static <T> ObjectOrder<T> getRandomFrom(Map<T, ObjectOrder<T>> chars) {
        double val = new Random().nextDouble();
        for (Map.Entry<T, ObjectOrder<T>> e : chars.entrySet()) {
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

    public static <T> void addChars(Map<T, ObjectOrder<T>> all, List<T> chars) {
        if (!chars.isEmpty()) {
            all.computeIfAbsent(chars.get(0), ObjectOrder::new)
                    .addOccurrence()
                    .addChars(getAllWithoutFirstCharacter(chars));
        }
    }

    public static <T> void normalize(Map<T, ObjectOrder<T>> all) {
        if (all.isEmpty())
            return;
        int sum = all.entrySet().stream().map(Map.Entry::getValue).mapToInt(t -> t.occurrence).sum();
        all.forEach((k, v) -> v.normalize(sum));
    }

    private static <T> List<T> getAllWithoutFirstCharacter(List<T> chars) {
        return chars.subList(1, chars.size());
    }

    public void normalize(int total) {
        this.probability = occurrence / (double)total;
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
