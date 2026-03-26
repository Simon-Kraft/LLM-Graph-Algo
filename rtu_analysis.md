# RTU Analysis — Task 2 (Algorithm-Synthesis Approach)

## Overview

Following Barati Nia et al. (2025), a generated implementation is classified as
**Ready-To-Use (RTU)** if and only if it satisfies all three criteria:

1. **Compilability** — compiles successfully without manual modifications
2. **Correctness** — produces correct output on all test scales
3. **Timeliness** — runs within an acceptable runtime threshold

If any criterion fails on even a single test scale, the implementation is **not RTU**.

---

## Compilability Notes

The following manual modifications were required before any testing could begin.
Any modification disqualifies an implementation from RTU status.

| Model | Algorithm | File | Modification Required |
|-------|-----------|-----------|----------------------|
| GPT-5.3-Codex | Triangle Counting | [TCGPTCodex.java](src/task2/TCGPTCodex.java) | Used getter methods to access CSR fields (`getRowPtr()`, `getColInd()`) that do not exist. Changed fields manually to direct access (`graph.rowPtr`, `graph.colInd`). |
| Gemini 3.1 Pro | Triangle Counting | [TCGemini.java](src/task2/TCGemini.java) | Used variable names (`graph.rowPtrs`, `graph.colIdxs`) that do not exist in the `Graph` class. Manually corrected to match actual field names (`graph.rowPtr`, `graph.colInd`). |
| Claude Opus 4.6 | Diameter | [DiameterClaude.java](src/task2/DiameterClaude.java) | Used implicit `int`-to-`long` conversion in return statement that produced a compile error. Manually added explicit cast for type conversion. |
| GPT-5.3-Codex | Clique Number | [CliqueGPTCodex.java](src/task2/CliqueGPTCodex.java) | Again used non-existent getter methods. However, this time explaining the assumed Java graph implementation. Again, manually changed to direct field access. |

**Note:** All four implementations required manual modification and are therefore not RTU.

---

## Correctness Results

### Triangle Counting

All three LLM implementations produced correct triangle counts across all tested
scales (RMAT-6 through RMAT-14). Results match the brute-force baseline on every
scale.

| Model | Correct on all scales |
|-------|----------------------|
| Claude Opus 4.6 | ✅ Yes |
| Gemini 3.1 Pro | ✅ Yes |
| GPT-5.3-Codex | ✅ Yes |

### Diameter Finding

All three LLM implementations produced correct diameter values across all tested
scales (RMAT-6 through RMAT-14). Results match the brute-force baseline on every
scale.

| Model | Correct on all scales |
|-------|----------------------|
| Claude Opus 4.6 | ✅ Yes |
| Gemini 3.1 Pro | ✅ Yes |
| GPT-5.3-Codex | ✅ Yes |

### Clique Number 

Claude Opus 4.6 produced incorrect clique numbers on all tested scales.
GPT-5.3-Codex produced incorrect results on scales 7 and 8.
Only Gemini 3.1 Pro produced correct results on all tested scales.

| Model | RMAT-6 | RMAT-7 | RMAT-8 | Correct on all scales |
|-------|--------|--------|--------|-----------------------|
| Claude Opus 4.6 | ❌ (8 vs 24) | ❌ (5 vs 23) | ❌ (6 vs 25) | ❌ No |
| Gemini 3.1 Pro | ✅ (24) | ✅ (23) | ✅ (25) | ✅ Yes |
| GPT-5.3-Codex | ✅ (24) | ❌ (22 vs 23) | ❌ (24 vs 25) | ❌ No |

---

## RTU Status per Model per Algorithm

Combining compilability and correctness:

| Model | Triangle Counting | Diameter | Clique Number |
|-------|------------------|----------|---------------|
| Claude Opus 4.6 | ✅ | ❌ (compilability) | ❌ (correctness) |
| Gemini 3.1 Pro | ❌ (compilability) | ✅ | ✅ |
| GPT-5.3-Codex | ❌ (compilability) | ✅ | ❌ (compilability + correctness) |

---

## RTU Percentage Computation

RTU% = (number of RTU algorithms) / (total algorithms tested) × 100

With 3 algorithms tested (Triangle Counting, Diameter, Clique Number):

### Claude Opus 4.6
- RTU algorithms: **0** (Triangle Counting ✅, Diameter ❌, Clique ❌)
- RTU% = 1/3 × 100 = **33%**

### Gemini 3.1 Pro
- RTU algorithms: **2** (Triangle Counting ❌, Diameter ✅, Clique ✅)
- RTU% = 2/3 × 100 = **67%**

### GPT-5.3-Codex
- RTU algorithms: **1** (Triangle Counting ❌, Diameter ✅, Clique ❌)
- RTU% = 1/3 × 100 = **33%**

### Summary Table

| Model | RTU Algorithms | Total Algorithms | RTU Percentage |
|-------|---------------|-----------------|----------------|
| Anthropic Claude Opus 4.6 | 1 | 3 | **33%** |
| Google Gemini 3.1 Pro | 2 | 3 | **67%** |
| OpenAI GPT-5.3-Codex | 1 | 3 | **33%** |

---
