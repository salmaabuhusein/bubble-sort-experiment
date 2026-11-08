"""
Reproducible analysis for the Bubble Algorithm Library experiment.

Run from the project root:
    python3 analysis/analysis.py

Required input:
    results/final-results.csv

Generated outputs:
    results/summary-statistics.csv
    results/median-winners.csv
    results/confidence-intervals.csv
    analysis/figures/*.png
"""

from pathlib import Path

import matplotlib.pyplot as plt
import pandas as pd


ROOT = Path(__file__).resolve().parent.parent
INPUT = ROOT / "results" / "final-results.csv"
RESULTS = ROOT / "results"
FIGURES = ROOT / "analysis" / "figures"

GROUPS = ["algorithm", "array_size", "distribution"]
TIME_COLUMN = "average_time_ns_per_sort"

EXPECTED_ALGORITHMS = {
    "BubbleSortUntilNoChange",
    "BubbleSortWhileNeeded",
    "BubbleSortPassPerItem",
}
EXPECTED_SIZES = {100, 500, 1000, 2000}
EXPECTED_DISTRIBUTIONS = {
    "SORTED",
    "REVERSE_SORTED",
    "RANDOM",
    "NEARLY_SORTED",
}
EXPECTED_REPETITIONS = 30

# Two-sided 95% Student's t critical value for df = 29 (n = 30).
# This avoids introducing SciPy as an additional project dependency.
T_CRITICAL_95_DF_29 = 2.045229642132703


def validate_data(data):
    required = set(
        GROUPS
        + [
            "iteration",
            "batch_size",
            "total_batch_time_ns",
            TIME_COLUMN,
        ]
    )
    missing = required.difference(data.columns)
    if missing:
        raise ValueError(f"Missing columns: {sorted(missing)}")

    if data[list(required)].isna().any().any():
        raise ValueError("Required columns contain missing values.")

    if (data[TIME_COLUMN] <= 0).any():
        raise ValueError("Execution times must be positive.")

    if (data["total_batch_time_ns"] <= 0).any():
        raise ValueError("Total batch times must be positive.")

    if not set(data["algorithm"].unique()) == EXPECTED_ALGORITHMS:
        raise ValueError("Unexpected or missing algorithm names.")

    if not set(data["array_size"].unique()) == EXPECTED_SIZES:
        raise ValueError("Unexpected or missing array sizes.")

    if not set(data["distribution"].unique()) == EXPECTED_DISTRIBUTIONS:
        raise ValueError("Unexpected or missing input distributions.")

    expected_batch_sizes = {100: 1000, 500: 50, 1000: 20, 2000: 5}
    for size, expected_batch in expected_batch_sizes.items():
        observed = set(data.loc[data["array_size"] == size, "batch_size"])
        if observed != {expected_batch}:
            raise ValueError(
                f"Unexpected batch size for array size {size}: {observed}"
            )

    expected_average = (
        data["total_batch_time_ns"] / data["batch_size"]
    )
    if not (
        (data[TIME_COLUMN] - expected_average).abs() < 1e-8
    ).all():
        raise ValueError("Average execution times do not match batch totals.")

    counts = data.groupby(GROUPS).size()
    if len(counts) != 48 or not (counts == EXPECTED_REPETITIONS).all():
        raise ValueError(
            "Expected 48 groups with 30 repetitions each; "
            f"found {len(counts)} groups."
        )

    for _, group in data.groupby(GROUPS):
        iterations = sorted(group["iteration"].tolist())
        if iterations != list(range(1, EXPECTED_REPETITIONS + 1)):
            raise ValueError("Each group must contain iterations 1 through 30.")

    return counts


def create_summary(data):
    summary = (
        data.groupby(GROUPS)[TIME_COLUMN]
        .agg(
            n="count",
            mean_ns="mean",
            median_ns="median",
            sd_ns="std",
            min_ns="min",
            max_ns="max",
        )
        .reset_index()
    )
    summary["cv_percent"] = (
        100 * summary["sd_ns"] / summary["mean_ns"]
    )
    return summary


def create_confidence_intervals(summary):
    ci = summary[GROUPS + ["n", "mean_ns", "sd_ns", "median_ns"]].copy()
    ci["se_ns"] = ci["sd_ns"] / (ci["n"] ** 0.5)
    ci["t_critical_95"] = T_CRITICAL_95_DF_29
    margin = ci["t_critical_95"] * ci["se_ns"]
    ci["ci95_lower_mean_ns"] = ci["mean_ns"] - margin
    ci["ci95_upper_mean_ns"] = ci["mean_ns"] + margin
    return ci


def create_winners(summary):
    winners = summary.loc[
        summary.groupby(["array_size", "distribution"])["median_ns"].idxmin(),
        ["array_size", "distribution", "algorithm", "median_ns"],
    ].sort_values(["array_size", "distribution"])
    return winners


def create_figures(data, summary):
    FIGURES.mkdir(parents=True, exist_ok=True)

    for distribution in sorted(data["distribution"].unique()):
        subset = summary[summary["distribution"] == distribution]
        fig, ax = plt.subplots(figsize=(8, 5))

        for algorithm in sorted(subset["algorithm"].unique()):
            series = subset[
                subset["algorithm"] == algorithm
            ].sort_values("array_size")
            ax.plot(
                series["array_size"],
                series["median_ns"],
                marker="o",
                label=algorithm,
            )

        ax.set_title(f"Median execution time — {distribution}")
        ax.set_xlabel("Array size (elements)")
        ax.set_ylabel("Median execution time (ns per sort)")
        ax.set_yscale("log")
        ax.grid(True, which="both", alpha=0.3)
        ax.legend()
        fig.tight_layout()
        fig.savefig(
            FIGURES / f"median-time-{distribution.lower()}.png",
            dpi=180,
        )
        plt.close(fig)

    for size in sorted(data["array_size"].unique()):
        subset = data[data["array_size"] == size]
        labels = []
        values = []

        for distribution in sorted(subset["distribution"].unique()):
            for algorithm in sorted(subset["algorithm"].unique()):
                group = subset[
                    (subset["distribution"] == distribution)
                    & (subset["algorithm"] == algorithm)
                ]
                labels.append(
                    f"{distribution}\n{algorithm.replace('BubbleSort', '')}"
                )
                values.append(group[TIME_COLUMN].to_numpy())

        fig, ax = plt.subplots(figsize=(11, 5))
        ax.boxplot(values, tick_labels=labels, showfliers=True)
        ax.set_title(f"Measurement distributions — array size {size}")
        ax.set_ylabel("Execution time (ns per sort)")
        ax.set_yscale("log")
        ax.tick_params(axis="x", labelrotation=75, labelsize=7)
        ax.grid(True, axis="y", which="both", alpha=0.3)
        fig.tight_layout()
        fig.savefig(FIGURES / f"boxplots-size-{size}.png", dpi=180)
        plt.close(fig)


def main():
    if not INPUT.exists():
        raise FileNotFoundError(f"Missing input CSV: {INPUT}")

    RESULTS.mkdir(parents=True, exist_ok=True)

    data = pd.read_csv(INPUT)
    counts = validate_data(data)

    summary = create_summary(data)
    summary.to_csv(RESULTS / "summary-statistics.csv", index=False)

    winners = create_winners(summary)
    winners.to_csv(RESULTS / "median-winners.csv", index=False)

    confidence_intervals = create_confidence_intervals(summary)
    confidence_intervals.to_csv(
        RESULTS / "confidence-intervals.csv",
        index=False,
    )

    create_figures(data, summary)

    print(f"Validated {len(data)} measurements across {len(counts)} conditions.")
    print(f"Summary: {RESULTS / 'summary-statistics.csv'}")
    print(f"Median winners: {RESULTS / 'median-winners.csv'}")
    print(f"Confidence intervals: {RESULTS / 'confidence-intervals.csv'}")
    print(f"Figures saved in: {FIGURES}")
    print()
    print(winners.to_string(index=False))


if __name__ == "__main__":
    main()
