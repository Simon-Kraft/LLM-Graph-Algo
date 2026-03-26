import java.io.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import graph.Graph;
import task2.*;

public class Main {

    private static final int EDGE_FACTOR  = 16;
    private static final int LOOP_CNT     = 10;
    private static final int SCALE_MIN    = 6;
    private static final int SCALE_START  = 6;
    private static final int SCALE_END    = 14;

    // Reference scales for Table II / Table III (matching the paper)
    private static final int OPT_REF_SCALE    = 14;  // Optimization approach reference
    private static final int TC_REF_SCALE     = 14;  // Triangle counting reference
    private static final int DIAM_REF_SCALE   = 14;  // Diameter reference
    private static final int CLIQUE_REF_SCALE = 8;  // Clique number reference

    static boolean QUIET         = false;
    static boolean PRINT         = false;
    static boolean NCUBED        = true;
    static boolean runAllScales  = false;

    private static String      INFILENAME    = null;
    private static int         SCALE         = 0;
    private static boolean     inputSelected = false;
    private static PrintStream outfile       = System.out;

    // ================================================================
    // Data structures for cross-scale result accumulation
    // ================================================================

    private static class BenchmarkResult {
        String type;
        String name;
        double timeSec;
        double memMB;
        long   result;
        boolean correct;
        int    scale;

        BenchmarkResult(String type, String name, double timeSec, double memMB,
                        long result, boolean correct, int scale) {
            this.type    = type;
            this.name    = name;
            this.timeSec = timeSec;
            this.memMB   = memMB;
            this.result  = result;
            this.correct = correct;
            this.scale   = scale;
        }
    }

    // All results ever collected, across all scales
    private static List<BenchmarkResult> allResults = new ArrayList<>();

    // Results for the current scale only (for per-scale efficiency output)
    private static Map<String, List<BenchmarkResult>> currentScaleResults = new LinkedHashMap<>();

    private static void storeResult(BenchmarkResult r) {
        allResults.add(r);
        currentScaleResults.computeIfAbsent(r.type, k -> new ArrayList<>()).add(r);
    }

    // ================================================================
    // TABLE I — RTU Percentage per Model (Task 2 only, across all scales)
    //
    // An implementation is RTU if it is correct on EVERY scale it ran on.
    // RTU% = (number of RTU algorithm types) / (total algorithm types) * 100
    // ================================================================

    private static void printTableI_RTU() {
        outfile.println("\n================================================================");
        outfile.println("TABLE I — RTU Percentage per Model (Task 2 only)");
        outfile.println("================================================================");
        outfile.printf("%-35s\t%s%n", "Model", "RTU Percentage");
        outfile.println("--------------------------------------------------");

        // Task 2 algorithm types
        List<String> task2Types = Arrays.asList("TC_SYN", "DIAM", "CLIQUE");

        // Collect all model names that appear in task2 results
        Set<String> models = new LinkedHashSet<>();
        for (BenchmarkResult r : allResults) {
            if (task2Types.contains(r.type)) models.add(r.name);
        }

        for (String model : models) {
            int rtuCount   = 0;
            int totalTypes = 0;

            for (String type : task2Types) {
                // Get all results for this model and type across all scales
                List<BenchmarkResult> modelTypeResults = new ArrayList<>();
                for (BenchmarkResult r : allResults) {
                    if (r.type.equals(type) && r.name.equals(model)) {
                        modelTypeResults.add(r);
                    }
                }
                if (modelTypeResults.isEmpty()) continue;

                totalTypes++;
                // RTU = correct on ALL scales
                boolean allCorrect = true;
                for (BenchmarkResult r : modelTypeResults) {
                    if (!r.correct) { allCorrect = false; break; }
                }
                if (allCorrect) rtuCount++;
            }

            double rtuPct = (totalTypes > 0) ? (rtuCount * 100.0 / totalTypes) : 0.0;
            outfile.printf("%-35s\t%.0f%%%n", model, rtuPct);
        }
        outfile.flush();
    }

    // ================================================================
    // TABLE II — Efficiency Rate per Model (Task 2 only)
    //
    // For each algorithm type, compute rate = 1/(t*m) at the reference
    // scale. Only RTU implementations contribute. Sum rates across types.
    // ================================================================

    private static void printTableII_EfficiencyRates() {
        outfile.println("\n================================================================");
        outfile.println("TABLE II — Efficiency Rates per Model (Task 2 only)");
        outfile.println("================================================================");

        // Map: algorithmType -> reference scale
        Map<String, Integer> refScales = new LinkedHashMap<>();
        refScales.put("TC_SYN", TC_REF_SCALE);
        refScales.put("DIAM",   DIAM_REF_SCALE);
        refScales.put("CLIQUE", CLIQUE_REF_SCALE);

        // Collect model names
        Set<String> models = new LinkedHashSet<>();
        for (BenchmarkResult r : allResults) {
            if (refScales.containsKey(r.type)) models.add(r.name);
        }

        // Print detailed breakdown first
        outfile.printf("%-10s\t%-35s\t%8s\t%8s\t%10s%n",
                "TYPE", "NAME", "Rel.Time", "Rel.Mem", "Rate");
        outfile.println("-----------------------------------------------------------------------------------------------");

        Map<String, Double> totalRates = new LinkedHashMap<>();
        for (String m : models) totalRates.put(m, 0.0);

        for (Map.Entry<String, Integer> entry : refScales.entrySet()) {
            String type  = entry.getKey();
            int    scale = entry.getValue();

            // Get all results for this type at the reference scale
            List<BenchmarkResult> refResults = new ArrayList<>();
            for (BenchmarkResult r : allResults) {
                if (r.type.equals(type) && r.scale == scale) refResults.add(r);
            }
            if (refResults.isEmpty()) continue;

            // Find min time and min memory among RTU results
            double minTime = Double.MAX_VALUE;
            double minMem  = Double.MAX_VALUE;
            for (BenchmarkResult r : refResults) {
                if (r.correct) {
                    if (r.timeSec > 0 && r.timeSec < minTime) minTime = r.timeSec;
                    if (r.memMB   > 0 && r.memMB   < minMem)  minMem  = r.memMB;
                }
            }
            if (minTime == Double.MAX_VALUE) minTime = 1.0;
            if (minMem  == Double.MAX_VALUE || minMem <= 0) minMem = 1.0;

            for (BenchmarkResult r : refResults) {
                if (!r.correct) {
                    outfile.printf("%-10s\t%-35s\t%8s\t%8s\t%10s%n",
                            type + "-" + scale, r.name, "-", "-", "0 (not RTU)");
                    totalRates.merge(r.name, 0.0, Double::sum);
                } else {
                    double t    = r.timeSec / minTime;
                    double m    = (r.memMB > 0) ? r.memMB / minMem : 1.0;
                    double rate = 1.0 / (t * m);
                    outfile.printf("%-10s\t%-35s\t%8.4f\t%8.4f\t%10.4f%n",
                            type + "-" + scale, r.name, t, m, rate);
                    totalRates.merge(r.name, rate, Double::sum);
                }
            }
        }

        // Print total efficiency rate summary
        outfile.println("\n--- Total Efficiency Rate per Model ---");
        outfile.printf("%-35s\t%10s%n", "Model", "Total Rate");
        outfile.println("--------------------------------------------------");
        for (Map.Entry<String, Double> e : totalRates.entrySet()) {
            outfile.printf("%-35s\t%10.4f%n", e.getKey(), e.getValue());
        }
        outfile.flush();
    }

    // ================================================================
    // TABLE III — Optimization Approach Overview (Task 1 only, at OPT_REF_SCALE)
    //
    // Shows compilable, correct, # triangles, runtime, memory per model.
    // ================================================================

    private static void printTableIII_OptimizationOverview() {
        outfile.println("\n================================================================");
        outfile.println("TABLE III — Optimization Approach Overview (RMAT-" + OPT_REF_SCALE + ")");
        outfile.println("================================================================");
        outfile.printf("%-35s\t%10s\t%7s\t%14s\t%12s\t%10s%n",
                "Model", "Compilable", "Correct", "#Triangles", "Runtime(s)", "Mem(MB)");
        outfile.println("-----------------------------------------------------------------------------------------------");

        List<BenchmarkResult> optResults = new ArrayList<>();
        for (BenchmarkResult r : allResults) {
            if (r.type.equals("TC_OPT") && r.scale == OPT_REF_SCALE) optResults.add(r);
        }

        if (optResults.isEmpty()) {
            outfile.println("  (no Task 1 results yet — add task1 implementations and uncomment in runExperiments)");
        } else {
            for (BenchmarkResult r : optResults) {
                outfile.printf("%-35s\t%10s\t%7s\t%14d\t%12.6f\t%10.2f%n",
                        r.name,
                        "Yes",
                        r.correct ? "Yes" : "NO",
                        r.result,
                        r.timeSec,
                        r.memMB);
            }
        }
        outfile.flush();
    }

    // ================================================================
    // Usage + flag parsing
    // ================================================================

    private static void usage() {
        System.out.println("LLM Graph Algorithm Benchmark (Java)\n");
        System.out.println("Either one of these must be selected:");
        System.out.println(" -r SCALE   [Use RMAT graph of size SCALE (>= " + SCALE_MIN + ")]");
        System.out.println(" -a         [Run all RMAT scales from " + SCALE_START + " to " + SCALE_END + "]");
        System.out.println(" -f <file>  [Read graph from Matrix Market file]");
        System.out.println("Optional:");
        System.out.println(" -o <file>  [Save output to file]");
        System.out.println(" -q         [Quiet mode]");
        System.out.println(" -d         [Print graph]");
        System.out.println(" -x         [Skip N^3 algorithms]");
        System.exit(8);
    }

    private static void parseFlags(String[] args) {
        if (args.length < 1) usage();
        int i = 0;
        while (i < args.length) {
            if (!args[i].startsWith("-")) { System.err.println("Wrong: " + args[i]); usage(); }
            switch (args[i].charAt(1)) {
                case 'f':
                    if (i + 1 >= args.length) usage();
                    INFILENAME = args[i + 1]; inputSelected = true; i += 2; break;
                case 'o':
                    if (i + 1 >= args.length) usage();
                    try { outfile = new PrintStream(new FileOutputStream(args[i + 1], true)); }
                    catch (FileNotFoundException e) { System.err.println("Cannot open: " + args[i+1]); usage(); }
                    i += 2; break;
                case 'r':
                    if (i + 1 >= args.length) usage();
                    SCALE = Integer.parseInt(args[i + 1]);
                    INFILENAME = "RMAT";
                    if (SCALE >= SCALE_MIN) inputSelected = true;
                    i += 2; break;
                case 'a':
                    runAllScales = true; inputSelected = true; INFILENAME = "RMAT"; i++; break;
                case 'q': QUIET = true; i++; break;
                case 'd': PRINT = true; i++; break;
                case 'x': NCUBED = false; i++; break;
                default: System.err.println("Wrong: " + args[i]); usage();
            }
        }
        if (!inputSelected) usage();
    }

    // ================================================================
    // Memory helpers
    // ================================================================

    private static long getUsedMemoryBytes() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private static void forceGC() {
        System.gc(); System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
    }

    // ================================================================
    // Benchmarking
    // ================================================================

    @FunctionalInterface
    interface GraphAlgorithm { long compute(Graph graph); }

    private static void benchmark(GraphAlgorithm f, Graph originalGraph, Graph graph,
                                  String type, String name, long expectedResult, int scale) {
        long result = 0;

        forceGC();
        long memBefore = getUsedMemoryBytes();

        double totalTime = System.nanoTime();
        for (int loop = 0; loop < LOOP_CNT; loop++) {
            graph.copyFrom(originalGraph);
            result = f.compute(graph);
        }
        totalTime = System.nanoTime() - totalTime;

        long memAfter = getUsedMemoryBytes();
        double peakMemMB = (memAfter - memBefore) / (1024.0 * 1024.0);

        double overTime = System.nanoTime();
        for (int loop = 0; loop < LOOP_CNT; loop++) { graph.copyFrom(originalGraph); }
        overTime = System.nanoTime() - overTime;

        totalTime -= overTime;
        totalTime /= (double) LOOP_CNT;
        double totalTimeSec = totalTime / 1_000_000_000.0;

        boolean correct    = (expectedResult < 0) || (result == expectedResult);
        String correctMark = correct ? "OK" : "WRONG";

        outfile.printf("%-8s\t%s\t%12d\t%12d\t%-35s\t%12.6f\t%12d\t%10.2f\t%s%n",
                type, "RMAT-" + scale,
                graph.numVertices, graph.numEdges / 2,
                name, totalTimeSec, result, peakMemMB, correctMark);
        outfile.flush();

        storeResult(new BenchmarkResult(type, name, totalTimeSec, peakMemMB, result, correct, scale));
    }

    // Overload: no correctness check (for baselines)
    private static void benchmark(GraphAlgorithm f, Graph originalGraph, Graph graph,
                                  String type, String name, int scale) {
        benchmark(f, originalGraph, graph, type, name, -1L, scale);
    }

    // ================================================================
    // Run experiments for one scale
    // ================================================================

    private static void runExperiments(int scale) {
        currentScaleResults.clear();

        Graph originalGraph = Graph.createRMAT(scale, EDGE_FACTOR);
        Graph graph         = Graph.allocateRMAT(scale, EDGE_FACTOR);

        if (!QUIET)
            outfile.printf("%nRMAT Scale: %d | Vertices: %d | Edges: %d%n",
                    scale, originalGraph.numVertices, originalGraph.numEdges / 2);

        // ============================================================
        // TASK 1: OPTIMIZATION APPROACH (Triangle Counting only)
        // LLMs given all existing TC code — attach bfs.c, graph.c,
        // main.c, queue.c, tc.c
        // ============================================================
        if (!QUIET) outfile.println("--- Task 1: Optimization Approach ---");

        // Step 1: get ground truth
        benchmark(TCBaseline::baseline, originalGraph, graph, "TC_OPT", "opt_baseline", scale);
        long baselineTCOpt = currentScaleResults.get("TC_OPT").get(0).result;
        // Remove baseline from allResults so it doesn't appear in Table III
        allResults.remove(allResults.size() - 1);
        currentScaleResults.get("TC_OPT").clear();

        // Step 2: LLM implementations (uncomment as you add them)
        benchmark(task1.TCOptClaude::tc_fast,   originalGraph, graph, "TC_OPT", "opt_claude",    baselineTCOpt, scale);
        benchmark(task1.TCOptGemini::tc_fast,   originalGraph, graph, "TC_OPT", "opt_gemini",    baselineTCOpt, scale);
        benchmark(task1.TCOptGPTCodex::tc_fast, originalGraph, graph, "TC_OPT", "opt_gptcodex",  baselineTCOpt, scale);

        // ============================================================
        // TASK 2A: ALGORITHM-SYNTHESIS — Triangle Counting
        // LLMs given only graph infrastructure — attach bfs.c, graph.c,
        // main.c, queue.c
        // ============================================================
        if (!QUIET) outfile.println("--- Task 2a: Algorithm-Synthesis - Triangle Counting ---");

        benchmark(TCBaseline::baseline, originalGraph, graph, "TC_SYN", "syn_tc_baseline", scale);
        long baselineTC = currentScaleResults.get("TC_SYN").get(0).result;
        // Remove baseline — ground truth only, not in efficiency computations
        allResults.remove(allResults.size() - 1);
        currentScaleResults.get("TC_SYN").clear();

        benchmark(TCClaude::fast,   originalGraph, graph, "TC_SYN", "syn_tc_claude",   baselineTC, scale);
        benchmark(TCGemini::fast,   originalGraph, graph, "TC_SYN", "syn_tc_gemini",   baselineTC, scale);
        benchmark(TCGPTCodex::fast, originalGraph, graph, "TC_SYN", "syn_tc_gptcodex", baselineTC, scale);

        // ============================================================
        // TASK 2B: ALGORITHM-SYNTHESIS — Diameter
        // ============================================================
        if (!QUIET) outfile.println("--- Task 2b: Algorithm-Synthesis - Diameter ---");

        benchmark(DiameterBaseline::compute, originalGraph, graph, "DIAM", "syn_diam_baseline", scale);
        long baselineDiam = currentScaleResults.get("DIAM").get(0).result;
        allResults.remove(allResults.size() - 1);
        currentScaleResults.get("DIAM").clear();
        benchmark(DiameterClaude::fast,   originalGraph, graph, "DIAM", "syn_diam_claude",   baselineDiam, scale);
        benchmark(DiameterGemini::fast,   originalGraph, graph, "DIAM", "syn_diam_gemini",   baselineDiam, scale);
        benchmark(DiameterGPTCodex::fast, originalGraph, graph, "DIAM", "syn_diam_gptcodex", baselineDiam, scale);

        // ============================================================
        // TASK 2C: ALGORITHM-SYNTHESIS — Clique Number
        // Only run up to RMAT-12 — clique is NP-hard and will hang
        // on larger graphs
        // ============================================================
        if (!QUIET) outfile.println("--- Task 2c: Algorithm-Synthesis - Clique Number ---");

        if (scale <= CLIQUE_REF_SCALE) {
            benchmark(CliqueBaseline::compute, originalGraph, graph, "CLIQUE", "syn_clique_baseline", scale);
            long baselineClique = currentScaleResults.get("CLIQUE").get(0).result;
            allResults.remove(allResults.size() - 1);
            currentScaleResults.get("CLIQUE").clear();
            benchmark(CliqueClaude::fast,   originalGraph, graph, "CLIQUE", "syn_clique_claude",   baselineClique, scale);
            benchmark(CliqueGemini::fast,   originalGraph, graph, "CLIQUE", "syn_clique_gemini",   baselineClique, scale);
            benchmark(CliqueGPTCodex::fast, originalGraph, graph, "CLIQUE", "syn_clique_gptcodex", baselineClique, scale);
        }
    }

    // ================================================================
    // Main
    // ================================================================

    public static void main(String[] args) {
        parseFlags(args);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (!QUIET) {
            outfile.println("# LLM Graph Algorithm Benchmark");
            outfile.println("# Run: " + timestamp);
            outfile.println("# Scales: " + SCALE_START + " to " + SCALE_END);
            outfile.println("# Reference scales — TC/Diam: RMAT-" + TC_REF_SCALE
                    + ", Clique: RMAT-" + CLIQUE_REF_SCALE
                    + ", Opt: RMAT-" + OPT_REF_SCALE);
            outfile.println("# Format: TYPE  GRAPH  VERTICES  EDGES  NAME  TIME(s)  RESULT  MEM(MB)  CORRECT");
            outfile.println("# -----------------------------------------------------------------------");
        }

        if (runAllScales) {
            for (int scale = SCALE_START; scale <= SCALE_END; scale++) {
                runExperiments(scale);
            }
        } else if (SCALE > 0) {
            runExperiments(SCALE);
        } else {
            // File input — run single scale equivalent
            try {
                Graph originalGraph = Graph.readMatrixMarketFile(INFILENAME);
                Graph graph = new Graph(originalGraph.numVertices, originalGraph.numEdges);
                if (!QUIET)
                    outfile.printf("Graph has %d vertices and %d undirected edges.%n",
                            originalGraph.numVertices, originalGraph.numEdges / 2);
                int scale = 0; // unknown scale for file input
                benchmark(TCBaseline::baseline, originalGraph, graph, "TC_SYN", "syn_tc_baseline", scale);
                long baselineTC = currentScaleResults.get("TC_SYN").get(0).result;
                allResults.remove(allResults.size() - 1);
                currentScaleResults.get("TC_SYN").clear();
                benchmark(TCClaude::fast,   originalGraph, graph, "TC_SYN", "syn_tc_claude",   baselineTC, scale);
                benchmark(TCGemini::fast,   originalGraph, graph, "TC_SYN", "syn_tc_gemini",   baselineTC, scale);
                benchmark(TCGPTCodex::fast, originalGraph, graph, "TC_SYN", "syn_tc_gptcodex", baselineTC, scale);
            } catch (IOException e) {
                System.err.println("ERROR: " + e.getMessage()); return;
            }
        }

        // ============================================================
        // Print summary tables for the report
        // ============================================================
        outfile.println("\n\n################################################################");
        outfile.println("# SUMMARY TABLES FOR REPORT");
        outfile.println("################################################################");

        printTableI_RTU();
        printTableII_EfficiencyRates();
        printTableIII_OptimizationOverview();

        if (outfile != System.out) outfile.close();
    }
}