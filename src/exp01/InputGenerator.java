
package exp01;

import java.util.Random;

public final class InputGenerator {

    public enum Distribution {
        SORTED,
        REVERSE_SORTED,
        RANDOM,
        NEARLY_SORTED
    }

    private InputGenerator() {
        // Utility class: prevent instantiation.
    }

    public static Integer[] generate(
            int size,
            Distribution distribution,
            long seed) {

        if (size < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }

        Integer[] items = new Integer[size];

        // Start with sorted values.
        for (int i = 0; i < size; i++) {
            items[i] = i;
        }

        Random random = new Random(seed);

        switch (distribution) {

            case SORTED:
                break;

            case REVERSE_SORTED:
                reverse(items);
                break;

            case RANDOM:
                shuffle(items, random);
                break;

            case NEARLY_SORTED:
                // Introduce approximately 1% random swaps.
                int swaps = size < 2 ? 0 : Math.max(1, size / 100);

                for (int i = 0; i < swaps; i++) {
                    int first = random.nextInt(size);
                    int second = random.nextInt(size);

                    swap(items, first, second);
                }
                break;
        }

        return items;
    }

    private static void reverse(Integer[] items) {
        for (int i = 0; i < items.length / 2; i++) {
            swap(items, i, items.length - 1 - i);
        }
    }

    private static void shuffle(Integer[] items, Random random) {
        for (int i = items.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            swap(items, i, j);
        }
    }

    private static void swap(Integer[] items, int i, int j) {
        Integer temp = items[i];
        items[i] = items[j];
        items[j] = temp;
    }
}
