# LLM Graph Algorithm Benchmarking

A Java benchmarking project for evaluating LLM-generated graph algorithms, replicating and extending the experiments from *"Evaluating Efficiency and Novelty of LLM-Generated Code for Graph Analysis"* (Barati Nia et al., 2025).

## Project Overview

This project evaluates how well three large language models (OpenAI GPT-5.3-Codex, Anthropic Claude Opus 4.6, and Google Gemini 3.1 Pro) can generate efficient **Java** implementations of graph algorithms when given C-language infrastructure as context. This is the key novelty compared to the original paper: LLMs are prompted to generate Java code while all provided context files are written in C.

Two experimental approaches are evaluated:

**Task 1 — Optimization Approach:** Each LLM is given all existing triangle counting implementations and asked to generate the fastest possible routine.

**Task 2 — Algorithm-Synthesis Approach:** Each LLM is given only the graph infrastructure (no algorithm code) and asked to implement three algorithms from scratch: Triangle Counting, Diameter Finding, and Clique Number.

Results are compared against the original paper's findings using the same RMAT synthetic graph benchmark suite (scales 6–14).

## Folder Structure

```
LLM-Graph-Algo/
├── src/
│   ├── Main.java               # Benchmarking driver and entry point
│   ├── graph/                  # Core graph infrastructure (package graph)
│   │   ├── Graph.java          # CSR graph data structure + RMAT generator
│   │   ├── BFS.java            # BFS implementations
│   │   └── Queue.java          # Queue data structure
│   ├── task1/                  # Task 1: Optimization approach
│   │   ├── TCOptClaude.java    # Claude's optimized triangle counting
│   │   ├── TCOptGemini.java    # Gemini's optimized triangle counting
│   │   └── TCOptGPTCodex.java  # ChatGPT's optimized triangle counting
│   └── task2/                  # Task 2: Algorithm-synthesis approach
│       ├── TCBaseline.java     # Brute-force baseline (ground truth)
│       ├── TCClaude.java       # Claude's triangle counting
│       ├── TCGemini.java       # Gemini's triangle counting
│       ├── TCGPTCodex.java     # ChatGPT's triangle counting
│       ├── DiameterBaseline.java
│       ├── DiameterClaude.java
│       ├── DiameterGemini.java
│       ├── DiameterGPTCodex.java
│       ├── CliqueBaseline.java
│       ├── CliqueClaude.java
│       ├── CliqueGemini.java
│       └── CliqueGPTCodex.java
├── bin/                        # Compiled .class files (auto-generated, gitignored)
├── lib/                        # External dependencies
├── c-reference/                # Original C infrastructure (Bader et al.)
├── c-reference-llm/            # LLM-generated C solutions from the paper
├── results/                    # Benchmark output files (see Results section)
├── report/                     # LaTeX report
│   ├── report.tex
│   ├── preamble.tex
│   └── references.bib
├── compile.sh                  # Compilation script
└── run.sh                      # Run script
```

## Getting Started

### Prerequisites

- Java 21 LTS — [Download here](https://adoptium.net)
- VSCode with the [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)

### Compiling

```bash
./compile.sh
```

This recursively finds and compiles all `.java` files from `src/` into `bin/`.

### Running — All Commands

**Single scale:**
```bash
./run.sh -r <scale>
```

**All scales at once (RMAT 6–14):**
```bash
./run.sh -a
```

**Save results to a timestamped file:**
```bash
./run.sh -a -o results/experiment_$(date +%Y%m%d_%H%M).txt
```

**Single scale, save to file:**
```bash
./run.sh -r 10 -o results/test.txt
```

**Quiet mode (data rows only, no section headers):**
```bash
./run.sh -a -q -o results/experiment.txt
```

**All flags:**

| Flag | Description |
|------|-------------|
| `-r SCALE` | Run single RMAT graph of given scale (must be ≥ 6) |
| `-a` | Run all scales from 6 to 14 |
| `-o <file>` | Save output to file (appends if file exists) |
| `-q` | Quiet mode — suppress section headers |

### RMAT Graph Sizes

| Scale | Vertices | Edges |
|-------|----------|-------|
| RMAT-6 | 64 | 1,024 |
| RMAT-7 | 128 | 2,048 |
| RMAT-8 | 256 | 4,096 |
| RMAT-9 | 512 | 8,192 |
| RMAT-10 | 1,024 | 16,384 |
| RMAT-11 | 2,048 | 32,768 |
| RMAT-12 | 4,096 | 65,536 |
| RMAT-13 | 8,192 | 131,072 |
| RMAT-14 | 16,384 | 262,144 |

> **Note:** Triangle counts vary per run since RMAT uses a random seed. Set a fixed seed in `Graph.java` for reproducible results.

## Output Format

Each result row is tab-separated with the following columns:

```
TYPE    GRAPH     VERTICES  EDGES   NAME               TIME(s)     RESULT   MEM(MB)
TC_SYN  RMAT-10   1024      16384   syn_tc_claude       0.002119    184188   0.80
```

| Column | Description |
|--------|-------------|
| `TYPE` | `TC_OPT` = Task 1, `TC_SYN` = Task 2 triangle counting, `DIAM_SYN` = Task 2 diameter, `CLIQUE_SYN` = Task 2 clique number |
| `GRAPH` | Graph identifier |
| `VERTICES` | Number of vertices |
| `EDGES` | Number of undirected edges |
| `NAME` | Algorithm identifier |
| `TIME(s)` | Average runtime over 10 runs in seconds |
| `RESULT` | Algorithm output (triangle count, diameter, or clique number) |
| `MEM(MB)` | Approximate heap memory allocated during execution |

## Results

Results are saved in the `results/` folder. Each experiment run produces a timestamped file:

```
results/
├── experiment_20260325_1637.txt    # Full run across all scales
├── experiment_20260325_1700.txt    # Run after adding new implementations
└── test_r10.txt                    # Quick single-scale sanity check
```

### Running a Full Experiment

```bash
mkdir -p results
./run.sh -a -o results/experiment_$(date +%Y%m%d_%H%M).txt
```

## LLMs Evaluated

| LLM | Model | Provider |
|-----|-------|----------|
| ChatGPT | GPT Codex (via GitHub Copilot) | OpenAI |
| Claude | Claude Opus 4.6 | Anthropic |
| Gemini | Gemini 3.1 Pro | Google |

## Reference Repositories

- [Bader-Research/triangle-counting](https://github.com/Bader-Research/triangle-counting) — Original C infrastructure
- [Bader-Research/LLM-triangle-counting](https://github.com/Bader-Research/LLM-triangle-counting) — LLM-generated C solutions from the paper

## Reference Paper

Atieh Barati Nia, Mohammad Dindoost, David A. Bader,
*"Evaluating Efficiency and Novelty of LLM-Generated Code for Graph Analysis"*,
IEEE HPEC 2025.
[arXiv:2507.06463](https://doi.org/10.48550/arXiv.2507.06463)

## Acknowledgements

This project was completed as a term project for CPSC482/682 Data Structures II at the University of Northern British Columbia. The Java infrastructure was translated from the original C benchmark code by Bader et al. with the help of Claude Opus 4.6 (Anthropic) as a translation assistant.