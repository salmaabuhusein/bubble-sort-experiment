
package exp01;

import java.util.Arrays;

public class InputGeneratorTest {

    public static void main(String[] args) {

        int size = 1000;
        long seed = 42L;

        for (InputGenerator.Distribution distribution :
                InputGenerator.Distribution.values()) {

            Integer[] first = InputGenerator.generate(
                    size, distribution, seed);

            Integer[] second = InputGenerator.generate(
                    size, distribution, seed);

            // Check that the same seed produces identical arrays.
            if (!Arrays.equals(first, second)) {
                throw new AssertionError(
                        distribution + ": results are not reproducible.");
            }

            // Check that the array contains every expected value.
            boolean[] seen = new boolean[size];

            for (Integer value : first) {
                if (value == null || value < 0 || value >= size) {
                    throw new AssertionError(
                            distribution + ": invalid array value.");
                }

                if (seen[value]) {
                    throw new AssertionError(
                            distribution + ": duplicate value.");
                }

                seen[value] = true;
            }

            for (boolean present : seen) {
                if (!present) {
                    throw new AssertionError(
                            distribution + ": missing value.");
                }
            }

            System.out.println(distribution + ": PASS");
        }

        System.out.println("All input generator tests passed.");
    }
}
