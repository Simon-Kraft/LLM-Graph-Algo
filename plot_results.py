"""
plot_results.py — Plotting script for LLM Graph Algorithm Benchmark results.
Usage: python3 plot_results.py results/experiment_20260326_1409.csv
"""

import sys
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np

# ================================================================
# Configuration
# ================================================================

CSV_FILE  = sys.argv[1] if len(sys.argv) > 1 else "results/experiment.csv"
OUT_DIR   = "results/plots"

# Colors per model — consistent across all plots
MODEL_COLORS = {
    "Claude Opus 4.6": "#d4501a",   # orange-red
    "Gemini 3.1 Pro":  "#1a73e8",   # Google blue
    "GPT-5.3-Codex":   "#10a37f",   # OpenAI green
}

MODEL_MARKERS = {
    "Claude Opus 4.6": "o",
    "Gemini 3.1 Pro":  "s",
    "GPT-5.3-Codex":   "^",
}

import os
os.makedirs(OUT_DIR, exist_ok=True)

# ================================================================
# Load CSV — handles multi-section format with # headers
# ================================================================

def load_section(filepath, section_name):
    """Extract a named section from the multi-section CSV file."""
    lines = []
    inside = False
    with open(filepath, "r") as f:
        for line in f:
            line = line.rstrip("\n")
            if line.strip() == f"# {section_name}":
                inside = True
                continue
            if inside:
                if line.startswith("# ") and line.strip() != f"# {section_name}":
                    break  # hit next section
                if line.strip() == "":
                    continue
                lines.append(line)
    from io import StringIO
    return pd.read_csv(StringIO("\n".join(lines)))

# Load all four sections
raw        = load_section(CSV_FILE, "RAW_RESULTS")
efficiency = load_section(CSV_FILE, "EFFICIENCY_RATES")
speedup    = load_section(CSV_FILE, "SPEEDUP_OVER_BASELINE")
opt        = load_section(CSV_FILE, "OPTIMIZATION_OVERVIEW")

print(f"Loaded {len(raw)} raw results, {len(efficiency)} efficiency rows, "
      f"{len(speedup)} speedup rows, {len(opt)} opt rows.")

# ================================================================
# PLOT 1 — Runtime scaling: Task 2 algorithms across RMAT scales
# One subplot per algorithm, lines per model
# ================================================================

fig, axes = plt.subplots(1, 3, figsize=(15, 5))
fig.suptitle("Runtime Scaling — Task 2 Algorithm-Synthesis", fontsize=14, fontweight="bold")

algorithms = ["Triangle Counting", "Diameter", "Clique Number"]
types      = ["TC_SYN", "DIAM_SYN", "CLIQUE_SYN"]

for ax, alg, typ in zip(axes, algorithms, types):
    df = raw[(raw["type"] == typ) & (raw["correct"] == True)].copy()

    for model in MODEL_COLORS:
        mdf = df[df["model"] == model].sort_values("scale")
        if mdf.empty:
            continue
        ax.plot(mdf["scale"], mdf["time_sec"],
                label=model,
                color=MODEL_COLORS[model],
                marker=MODEL_MARKERS[model],
                linewidth=2, markersize=6)

    ax.set_title(alg, fontsize=12)
    ax.set_xlabel("RMAT Scale")
    ax.set_ylabel("Runtime (s)")
    ax.set_yscale("log")
    ax.xaxis.set_major_locator(ticker.MaxNLocator(integer=True))
    ax.grid(True, alpha=0.3)
    ax.legend(fontsize=8)

plt.tight_layout()
plt.savefig(f"{OUT_DIR}/plot1_runtime_scaling.pdf", bbox_inches="tight")
plt.savefig(f"{OUT_DIR}/plot1_runtime_scaling.png", dpi=150, bbox_inches="tight")
print("Saved plot1_runtime_scaling")

# ================================================================
# PLOT 2 — Speedup over baseline across scales (Task 2)
# ================================================================

fig, axes = plt.subplots(1, 3, figsize=(15, 5))
fig.suptitle("Speedup over Brute-Force Baseline — Task 2", fontsize=14, fontweight="bold")

for ax, alg, typ in zip(axes, algorithms, types):
    df = speedup[(speedup["type"] == typ) & (speedup["correct"] == True)].copy()

    for model in MODEL_COLORS:
        mdf = df[df["model"] == model].sort_values("scale")
        if mdf.empty:
            continue
        ax.plot(mdf["scale"], mdf["speedup_over_baseline"],
                label=model,
                color=MODEL_COLORS[model],
                marker=MODEL_MARKERS[model],
                linewidth=2, markersize=6)

    ax.axhline(y=1.0, color="black", linestyle="--", linewidth=1, alpha=0.5, label="Baseline")
    ax.set_title(alg, fontsize=12)
    ax.set_xlabel("RMAT Scale")
    ax.set_ylabel("Speedup (×)")
    ax.xaxis.set_major_locator(ticker.MaxNLocator(integer=True))
    ax.grid(True, alpha=0.3)
    ax.legend(fontsize=8)

plt.tight_layout()
plt.savefig(f"{OUT_DIR}/plot2_speedup.pdf", bbox_inches="tight")
plt.savefig(f"{OUT_DIR}/plot2_speedup.png", dpi=150, bbox_inches="tight")
print("Saved plot2_speedup")

# ================================================================
# PLOT 3 — Efficiency rate bar chart (Table II visualized)
# ================================================================

# Compute total efficiency rate per model
eff = efficiency.copy()
eff["rate"] = pd.to_numeric(eff["rate"], errors="coerce").fillna(0)
total_rates = eff.groupby("model")["rate"].sum().reindex(list(MODEL_COLORS.keys()))

fig, ax = plt.subplots(figsize=(7, 5))
bars = ax.bar(total_rates.index,
              total_rates.values,
              color=[MODEL_COLORS[m] for m in total_rates.index],
              edgecolor="black", linewidth=0.8, width=0.5)

# Add value labels on top of bars
for bar, val in zip(bars, total_rates.values):
    ax.text(bar.get_x() + bar.get_width() / 2, bar.get_height() + 0.01,
            f"{val:.4f}", ha="center", va="bottom", fontsize=10)

ax.set_title("Total Efficiency Rate per Model (Task 2)", fontsize=13, fontweight="bold")
ax.set_ylabel("Efficiency Rate (sum of 1/t·m)")
ax.set_ylim(0, total_rates.max() * 1.2)
ax.grid(True, axis="y", alpha=0.3)
ax.set_xticklabels(total_rates.index, rotation=10, ha="right")

plt.tight_layout()
plt.savefig(f"{OUT_DIR}/plot3_efficiency_rates.pdf", bbox_inches="tight")
plt.savefig(f"{OUT_DIR}/plot3_efficiency_rates.png", dpi=150, bbox_inches="tight")
print("Saved plot3_efficiency_rates")

# ================================================================
# PLOT 4 — Task 1 Optimization: runtime vs scale
# ================================================================

fig, ax = plt.subplots(figsize=(8, 5))
fig.suptitle("Runtime Scaling — Task 1 Optimization Approach", fontsize=14, fontweight="bold")

for model in MODEL_COLORS:
    mdf = opt[opt["model"] == model].sort_values("scale")
    if mdf.empty:
        continue
    ax.plot(mdf["scale"], mdf["time_sec"],
            label=model,
            color=MODEL_COLORS[model],
            marker=MODEL_MARKERS[model],
            linewidth=2, markersize=6)

ax.set_xlabel("RMAT Scale")
ax.set_ylabel("Runtime (s)")
ax.set_yscale("log")
ax.xaxis.set_major_locator(ticker.MaxNLocator(integer=True))
ax.grid(True, alpha=0.3)
ax.legend()

plt.tight_layout()
plt.savefig(f"{OUT_DIR}/plot4_task1_optimization.pdf", bbox_inches="tight")
plt.savefig(f"{OUT_DIR}/plot4_task1_optimization.png", dpi=150, bbox_inches="tight")
print("Saved plot4_task1_optimization")

# ================================================================
# PLOT 5 — Memory usage at reference scale (scatter: time vs mem)
# ================================================================

fig, axes = plt.subplots(1, 3, figsize=(15, 5))
fig.suptitle("Time vs Memory Trade-off at Reference Scale — Task 2", fontsize=14, fontweight="bold")

ref_scales = {"TC_SYN": 14, "DIAM_SYN": 14, "CLIQUE_SYN": 8}

for ax, alg, typ in zip(axes, algorithms, types):
    ref_scale = ref_scales[typ]
    df = raw[(raw["type"] == typ) & (raw["scale"] == ref_scale) & (raw["correct"] == True)].copy()

    for model in MODEL_COLORS:
        mdf = df[df["model"] == model]
        if mdf.empty:
            continue
        ax.scatter(mdf["time_sec"], mdf["mem_mb"],
                   label=model,
                   color=MODEL_COLORS[model],
                   marker=MODEL_MARKERS[model],
                   s=120, zorder=5)
        # Annotate each point with model short name
        short = model.split()[0]  # "Claude", "Gemini", "GPT-5.3-Codex"
        ax.annotate(short,
                    (mdf["time_sec"].values[0], mdf["mem_mb"].values[0]),
                    textcoords="offset points", xytext=(6, 4), fontsize=8)

    ax.set_title(f"{alg} (RMAT-{ref_scale})", fontsize=11)
    ax.set_xlabel("Runtime (s)")
    ax.set_ylabel("Memory (MB)")
    ax.grid(True, alpha=0.3)
    ax.legend(fontsize=8)

plt.tight_layout()
plt.savefig(f"{OUT_DIR}/plot5_time_memory_tradeoff.pdf", bbox_inches="tight")
plt.savefig(f"{OUT_DIR}/plot5_time_memory_tradeoff.png", dpi=150, bbox_inches="tight")
print("Saved plot5_time_memory_tradeoff")

print(f"\nAll plots saved to {OUT_DIR}/")
print("Run: open results/plots/  to view them on Mac")