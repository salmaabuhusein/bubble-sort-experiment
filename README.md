# Bubble Algorithm Experiment

A Java benchmarking experiment comparing three Bubble Sort implementations under different input distributions and array sizes.

The experiment evaluates execution time to identify which implementation is most suitable for Bubble Inc.

## Project Structure

```text
bubble-algorithm-experiment/
│
├── README.md
├── .gitignore
│
├── src/
│   └── exp01/
│       ├── Sorter.java
│       ├── BubbleSortUntilNoChange.java
│       ├── BubbleSortWhileNeeded.java
│       ├── BubbleSortPassPerItem.java
│       ├── InputGenerator.java
│       ├── InputGeneratorTest.java
│       ├── SmokeTest.java
│       └── BenchmarkRunner.java
│
├── analysis/
│   ├── analysis.py
│   └── figures/
│
├── results/
│   ├── environment.txt
│   ├── final-results.csv
│   ├── summary-statistics.csv
│   ├── median-winners.csv
│   └── confidence-intervals.csv
│
└── out/                  # Compiled Java classes (not committed)
```

## Requirements

### Java
- JDK 25 or compatible.
- Java compiler (`javac`) and runtime (`java`).

### Python
- Python 3.9 or compatible.
- pandas 2.3.3
- matplotlib 3.9.4

Install the Python dependencies if necessary:

```bash
python3 -m pip install pandas matplotlib
```

## 1. Compile the Java source code

Run from the project root:

```bash
javac -d out src/exp01/*.java
```

This compiles the sorting implementations, input generator, tests, and benchmark runner.

## 2. Run correctness tests

### Sorting implementations

```bash
java -cp out exp01.SmokeTest
```

Expected output:

```text
BubbleSortUntilNoChange: PASS
BubbleSortWhileNeeded: PASS
BubbleSortPassPerItem: PASS
All three sorting implementations passed.
```

### Input generator

```bash
java -cp out exp01.InputGeneratorTest
```

The test checks that:
- Identical seeds produce identical arrays.
- Generated arrays contain all expected values.
- No duplicate, missing, or invalid values are present.

## 3. Run the benchmark

From the project root:

```bash
java -cp out exp01.BenchmarkRunner
```

The benchmark evaluates three sorting implementations using the following experimental configuration.

| Parameter | Configuration |
|---|---|
| Algorithms | 3 |
| Array sizes | 100, 500, 1,000, 2,000 |
| Input distributions | 4 |
| Warm-up runs | 5 per algorithm and condition |
| Measured repetitions | 30 per algorithm and condition |
| Random seed | 42 |
| Timing method | `System.nanoTime()` |
| Execution order | Randomized |
| Measurement unit | Nanoseconds per sort |

### Input distributions

- **SORTED:** Elements are already in ascending order.
- **REVERSE_SORTED:** Elements are in descending order.
- **RANDOM:** Elements are randomly shuffled using Fisher–Yates.
- **NEARLY_SORTED:** Starts with sorted elements and applies approximately 1% random swap operations.

### Measurement procedure

- The same generated input is used for all three algorithms and all repetitions within each experimental condition.
- Input copying takes place outside the timed region.
- Sorting operations are measured using `System.nanoTime()`.
- Correctness validation takes place outside the timed region.
- Multiple sorting operations are grouped into batches to obtain sufficiently measurable execution times.
- Algorithm execution order is randomized during warm-up and measurement.
- Experimental condition order is also randomized.

Batch sizes depend on array size:

| Array size | Batch size |
|---:|---:|
| 100 | 1,000 |
| 500 | 50 |
| 1,000 | 20 |
| 2,000 | 5 |

The benchmark produces 1,440 measurements:

3 algorithms × 4 array sizes × 4 distributions × 30 repetitions.

Raw measurements are saved to:

```text
results/final-results.csv
```

**Important:** Running the benchmark again overwrites the existing raw results file.

## 4. Run the statistical analysis

After collecting benchmark measurements, run:

```bash
python3 analysis/analysis.py
```

The analysis script validates the raw dataset and generates the following outputs.

### Statistical results

| File | Description |
|---|---|
| `results/summary-statistics.csv` | Sample size, mean, median, standard deviation, minimum, maximum, and coefficient of variation |
| `results/median-winners.csv` | Fastest algorithm by median execution time for each condition |
| `results/confidence-intervals.csv` | 95% confidence intervals for mean execution time |

Confidence intervals are calculated using the two-sided Student's t-distribution:

\[
\bar{x} \pm t_{0.975,29}\frac{s}{\sqrt{n}}
\]

where \(n=30\).

### Figures

Eight figures are generated in `analysis/figures/`:

- Four median execution-time plots, one for each input distribution.
- Four boxplot figures, one for each array size.

Execution-time axes use logarithmic scales to accommodate the differences in magnitude between algorithms and input conditions.

## 5. Experimental environment

The measurements were collected on the following system:

| Component | Specification |
|---|---|
| Computer | Apple MacBook Pro |
| Processor | Apple M3 Pro |
| Architecture | ARM64 |
| Memory | 18 GB |
| Operating system | macOS 26.6.2 |
| Java | OpenJDK 25.0.2 |
| Python | 3.9.6 |
| pandas | 2.3.3 |
| matplotlib | 3.9.4 |

Additional environment information is recorded in `results/environment.txt`.

## 6. Reproducibility and limitations

The benchmark uses fixed random seeds and randomized execution order to support reproducibility and reduce systematic ordering effects.

The experiment has several limitations:

- Each array-size and distribution condition uses one generated input instance, reused across all repetitions.
- Repeated measurements therefore characterize timing variability for the selected input instances, rather than variability across independently generated inputs.
- JVM compilation, garbage collection, CPU scheduling, and thermal behaviour may affect execution times.
- Results are specific to the tested hardware, Java runtime, implementations, and experimental configuration.

The confidence intervals quantify uncertainty in the mean measured execution time for each tested condition. They do not account for variation across all possible input arrays.

## 7. Repository contents

The repository includes:

- Java source code and correctness tests.
- Benchmark implementation.
- Raw benchmark measurements.
- Python analysis script.
- Generated statistical summaries.
- Confidence intervals.
- Visualization figures.
- Experimental environment documentation.

All reported results can be regenerated from the raw measurements using the analysis script.

