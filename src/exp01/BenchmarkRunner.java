package exp01;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BenchmarkRunner {

    private static final int[] ARRAY_SIZES = {100, 500, 1000, 2000};

    private static final int WARMUP_RUNS = 5;
    private static final int MEASURED_RUNS = 30;
    private static final long SEED = 42L;

    private static final Path OUTPUT_FILE =
            Path.of("results", "final-results.csv");

    private static class Algorithm {
        final String name;
        final Sorter<Integer> sorter;

        Algorithm(String name, Sorter<Integer> sorter) {
            this.name = name;
            this.sorter = sorter;
        }
    }

    private static class Condition {
        final int arraySize;
        final InputGenerator.Distribution distribution;

        Condition(int arraySize, InputGenerator.Distribution distribution) {
            this.arraySize = arraySize;
            this.distribution = distribution;
        }
    }

    public static void main(String[] args) throws IOException {

        List<Algorithm> algorithms = List.of(
                new Algorithm("BubbleSortUntilNoChange",
                        new BubbleSortUntilNoChange<>()),
                new Algorithm("BubbleSortWhileNeeded",
                        new BubbleSortWhileNeeded<>()),
                new Algorithm("BubbleSortPassPerItem",
                        new BubbleSortPassPerItem<>())
        );

        Files.createDirectories(OUTPUT_FILE.getParent());

        // Separate deterministic randomization stream for execution order.
        Random orderRandom = new Random(SEED + 1);

        // Randomize the order of the 16 experimental conditions to reduce
        // confounding between condition and temporal changes in the machine.
        List<Condition> conditions = new ArrayList<>();
        for (int size : ARRAY_SIZES) {
            for (InputGenerator.Distribution distribution
                    : InputGenerator.Distribution.values()) {
                conditions.add(new Condition(size, distribution));
            }
        }
        Collections.shuffle(conditions, orderRandom);

        try (BufferedWriter writer = Files.newBufferedWriter(OUTPUT_FILE)) {

            writer.write(
                    "algorithm,array_size,distribution,iteration," +
                    "batch_size,total_batch_time_ns,average_time_ns_per_sort");
            writer.newLine();

            int conditionNumber = 0;

            for (Condition condition : conditions) {
                conditionNumber++;

                int size = condition.arraySize;
                InputGenerator.Distribution distribution =
                        condition.distribution;
                int batchSize = getBatchSize(size);

                System.out.println(
                        "Condition " + conditionNumber + "/"
                        + conditions.size()
                        + ": size=" + size
                        + ", distribution=" + distribution
                        + ", batch_size=" + batchSize);

                // The same deterministic input is used for all algorithms
                // and repetitions within this condition. This isolates the
                // algorithm comparison from differences in input instances.
                Integer[] originalInput =
                        InputGenerator.generate(size, distribution, SEED);

                // Warm up all algorithms before collecting measurements.
                // Warm-up algorithm order is randomized for each condition.
                List<Algorithm> warmupOrder = new ArrayList<>(algorithms);
                Collections.shuffle(warmupOrder, orderRandom);

                for (Algorithm algorithm : warmupOrder) {
                    for (int run = 0; run < WARMUP_RUNS; run++) {
                        for (int i = 0; i < batchSize; i++) {
                            Integer[] input = Arrays.copyOf(
                                    originalInput, originalInput.length);
                            algorithm.sorter.sort(input);
                            validateSorted(input);
                        }
                    }
                }

                // Collect 30 measurements per algorithm and condition.
                // Algorithm order is randomized independently in each
                // iteration to reduce systematic order effects.
                for (int iteration = 1;
                     iteration <= MEASURED_RUNS;
                     iteration++) {

                    List<Algorithm> executionOrder =
                            new ArrayList<>(algorithms);
                    Collections.shuffle(executionOrder, orderRandom);

                    for (Algorithm algorithm : executionOrder) {

                        // Create independent copies outside the timed region.
                        Integer[][] batch = new Integer[batchSize][];
                        for (int i = 0; i < batchSize; i++) {
                            batch[i] = Arrays.copyOf(
                                    originalInput, originalInput.length);
                        }

                        long start = System.nanoTime();

                        for (Integer[] input : batch) {
                            algorithm.sorter.sort(input);
                        }

                        long end = System.nanoTime();
                        long totalTime = end - start;

                        // Correctness validation is deliberately excluded
                        // from the measured interval.
                        for (Integer[] input : batch) {
                            validateSorted(input);
                        }

                        double averageTime =
                                (double) totalTime / batchSize;

                        writer.write(
                                algorithm.name + ","
                                + size + ","
                                + distribution + ","
                                + iteration + ","
                                + batchSize + ","
                                + totalTime + ","
                                + averageTime);
                        writer.newLine();
                    }
                }

                writer.flush();
            }
        }

        System.out.println();
        System.out.println("Benchmark completed.");
        System.out.println("Results saved to: " + OUTPUT_FILE);
    }

    private static int getBatchSize(int size) {
        if (size <= 100) {
            return 1000;
        }
        if (size <= 500) {
            return 50;
        }
        if (size <= 1000) {
            return 20;
        }
        return 5;
    }

    private static void validateSorted(Integer[] items) {
        for (int i = 1; i < items.length; i++) {
            if (items[i - 1] > items[i]) {
                throw new IllegalStateException(
                        "Sorting algorithm produced an incorrectly sorted array.");
            }
        }
    }
}
