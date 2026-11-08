package exp01;

import java.util.Arrays;

/** Basic correctness check; this is not part of the performance benchmark. */
public final class SmokeTest {
    private SmokeTest() { }

    public static void main(String[] args) {
        Integer[] input = { 5, -2, 9, 0, 5, 1 };
        Integer[] expected = { -2, 0, 1, 5, 5, 9 };

        check("BubbleSortUntilNoChange", new BubbleSortUntilNoChange<>(), input, expected);
        check("BubbleSortWhileNeeded", new BubbleSortWhileNeeded<>(), input, expected);
        check("BubbleSortPassPerItem", new BubbleSortPassPerItem<>(), input, expected);

        System.out.println("All three sorting implementations passed.");
    }

    private static void check(String name, Sorter<Integer> sorter,
                              Integer[] input, Integer[] expected) {
        Integer[] actual = Arrays.copyOf(input, input.length);
        sorter.sort(actual);
        if (!Arrays.equals(actual, expected)) {
            throw new AssertionError(name + " failed: " + Arrays.toString(actual));
        }
        System.out.println(name + ": PASS");
    }
}
