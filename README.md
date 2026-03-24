# LLM Graph Algorithm Benchmarking

A Java benchmarking project for evaluating LLM-generated graph algorithms, replicating and extending the experiments from *"Evaluating Efficiency and Novelty of LLM-Generated Code for Graph Analysis"* (Barati Nia et al., 2025).

## Project Overview

This project evaluates how well three large language models (ChatGPT, Claude, and Gemini) can generate efficient **Java** implementations of graph algorithms when given C-language infrastructure as context. The three algorithms tested are:

- Triangle Counting
- Diameter Finding
- Clique Number

Results are compared against the original paper's findings using the same RMAT synthetic graph benchmark suite (scales 6–18).

## Folder Structure

```
LLM-Graph-Algo/
├── src/                  # Java source files
│   ├── Graph.java        # CSR graph data structure
│   ├── BFS.java          # BFS implementations
│   ├── Queue.java        # Queue data structure
│   ├── Main.java         # Benchmarking driver
│   └── TC.java           # Algorithm placeholder / implementations
├── bin/                  # Compiled .class files (auto-generated)
├── lib/                  # External dependencies (if any)
├── c-reference/          # Original C infrastructure (Bader et al.)
├── c-reference-llm/      # LLM-generated C solutions from the paper
├── results/              # Benchmark timing output files
├── compile.sh            # Compilation script
└── run.sh                # Run script
```

## Getting Started

### Prerequisites

- Java 21 (LTS) — [Download here](https://adoptium.net)
- VSCode with the [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)

### Compiling

```bash
./compile.sh
```

This compiles all `.java` files from `src/` into `bin/`.

### Running

```bash
./run.sh -r <scale>
```

Where `<scale>` is the RMAT graph scale (6–18). For example:

```bash
./run.sh -r 6     # 64 vertices, 1024 edges
./run.sh -r 10    # 1024 vertices, 16384 edges
./run.sh -r 18    # 262144 vertices, 4194304 edges
```

### Expected Graph Sizes

| Scale | Vertices | Edges | Expected Triangles |
|-------|----------|-------|--------------------|
| RMAT-6 | 64 | 1,024 | 9,100 |
| RMAT-8 | 256 | 4,096 | 39,602 |
| RMAT-10 | 1,024 | 16,384 | 187,855 |
| RMAT-12 | 4,096 | 65,536 | 896,224 |
| RMAT-18 | 262,144 | 4,194,304 | 101,930,789 |

## Reference Repositories

- [Bader-Research/triangle-counting](https://github.com/Bader-Research/triangle-counting) — Original C infrastructure
- [Bader-Research/LLM-triangle-counting](https://github.com/Bader-Research/LLM-triangle-counting) — LLM-generated C solutions from the paper

## Reference Paper

Atieh Barati Nia, Mohammad Dindoost, David A. Bader,
*"Evaluating Efficiency and Novelty of LLM-Generated Code for Graph Analysis"*,
IEEE HPEC 2025.
[arXiv:2507.06463](https://doi.org/10.48550/arXiv.2507.06463)

## Acknowledgements

This project was completed as a term project for CPSC482/682 Data Structures II.
The Java infrastructure was translated from the original C benchmark code by Bader et al.
using Claude (Anthropic) as a translation assistant.
