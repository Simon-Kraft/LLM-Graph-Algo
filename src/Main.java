import java.io.*;
import java.util.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import graph.Graph;
import task2.*;

public class Main {

    private static final int EDGE_FACTOR = 16;
    private static final int LOOP_CNT = 10;
    private static final int SCALE_MIN = 6;

    // Default scale range for full experiment run
    private static final int SCALE_START = 6;
    private static final int SCALE_END = 12;

    static boolean QUIET = false;
    static boolean PRINT = false;
    static boolean NCUBED = true;

    private static String INFILENAME = null;
    private static int SCALE = 0;
    private static boolean inputSelected = false;
    private static boolean runAllScales = false;
    private static PrintStream outfile = System.out;

    // ----------------------------------------------------------------
    // Result storage for efficiency rate computation
    // ----------------------------------------------------------------

    private static class BenchmarkResult {
        String type;
        String name;
        double timeSec;
        double memMB;
        long result;
        boolean correct;

        BenchmarkResult(String type, String name, double timeSec, double memMB, long result, boolean correct) {
            this.type = type;
            this.name = name;
            this.timeSec = timeSec;
            this.memMB = memMB;
            this.result = result;
            this.correct = correct;
        }
    }

    // Results grouped by algorithm type for the current scale
    private static Map<String, List<BenchmarkResult>> resultsPerType = new LinkedHashMap<>();

    private static void storeResult(BenchmarkResult r) {
        resultsPerType.computeIfAbsent(r.type, k -> new ArrayList<>()).add(r);
    }

    // ----------------------------------------------------------------
    // Print efficiency rates for all collected results
    // ----------------------------------------------------------------

    private static void printEfficiencyRates() {
        outfile.println("\n--- Efficiency Rates ---");
        outfile.printf("%-10s\t%-35s\t%8s\t%8s\t%10s%n",
                "TYPE", "NAME", "Rel.Time", "Rel.Mem", "Rate");
        outfile.println("-----------------------------------------------------------------------------------------------");

        // Accumulate total rate per model across all algorithm types
        Map<String, Double> modelTotalRates = new LinkedHashMap<>();

        for (Map.Entry<String, List<BenchmarkResult>> entry : resultsPerType.entrySet()) {
            String type = entry.getKey();
            List<BenchmarkResult> results = entry.getValue();

            // Find min time and min memory among RTU (correct) results only
            double minTime = Double.MAX_VALUE;
            double minMem  = Double.MAX_VALUE;
            for (BenchmarkResult r : results) {
                if (r.correct) {
                    if (r.timeSec > 0 && r.timeSec < minTime) minTime = r.timeSec;
                    if (r.memMB  > 0 && r.memMB  < minMem)  minMem  = r.memMB;
                }
            }
            // Avoid division by zero
            if (minTime == Double.MAX_VALUE) minTime = 1.0;
            if (minMem  == Double.MAX_VALUE || minMem <= 0) minMem = 1.0;

            for (BenchmarkResult r : results) {
                if (!r.correct) {
                    outfile.printf("%-10s\t%-35s\t%8s\t%8s\t%10s%n",
                            type, r.name, "-", "-", "0 (not RTU)");
                    modelTotalRates.merge(r.name, 0.0, Double::sum);
                } else {
                    double t    = r.timeSec / minTime;
                    double m    = (r.memMB > 0) ? r.memMB / minMem : 1.0;
                    double rate = 1.0 / (t * m);
                    outfile.printf("%-10s\t%-35s\t%8.4f\t%8.4f\t%10.4f%n",
                            type, r.name, t, m, rate);
                    modelTotalRates.merge(r.name, rate, Double::sum);
                }
            }
        }

        // Print total efficiency rate summary
        outfile.println("\n--- Total Efficiency Rate per Model ---");
        outfile.printf("%-35s\t%10s%n", "Model", "Total Rate");
        outfile.println("--------------------------------------------------");
        for (Map.Entry<String, Double> e : modelTotalRates.entrySet()) {
            outfile.printf("%-35s\t%10.4f%n", e.getKey(), e.getValue());
        }
        outfile.flush();
    }

    // ----------------------------------------------------------------
    // Usage
    // ----------------------------------------------------------------

    private static void usage() {
        System.out.println("LLM Graph Algorithm Benchmark (Java)\n");
        System.out.println("Usage:\n");
        System.out.println("Either one of these must be selected:");
        System.out.println(" -f <filename>   [Input Graph in Matrix Market format]");
        System.out.println(" -r SCALE        [Use RMAT graph of size SCALE] (SCALE must be >= " + SCALE_MIN + ")");
        System.out.println(" -a              [Run all RMAT scales from " + SCALE_START + " to " + SCALE_END + "]");
        System.out.println("Optional arguments:");
        System.out.println(" -o <filename>   [Output File]");
        System.out.println(" -d              [Display/Print Input Graph]");
        System.out.println(" -q              [Turn on Quiet mode]");
        System.out.println(" -x              [Do not run N^3 algorithms]");
        System.exit(8);
    }

    private static void parseFlags(String[] args) {
        if (args.length < 1) usage();

        int i = 0;
        while (i < args.length) {
            if (!args[i].startsWith("-")) {
                System.err.println("Wrong Argument: " + args[i]);
                usage();
            }

            switch (args[i].charAt(1)) {
                case 'f':
                    if (i + 1 >= args.length) usage();
                    if (!QUIET) System.out.println("Input Graph: " + args[i + 1]);
                    INFILENAME = args[i + 1];
                    inputSelected = true;
                    i += 2;
                    break;

                case 'o':
                    if (i + 1 >= args.length) usage();
                    if (!QUIET) System.out.println("Output file: " + args[i + 1]);
                    try {
                        outfile = new PrintStream(new FileOutputStream(args[i + 1], true));
                    } catch (FileNotFoundException e) {
                        System.err.println("Cannot open output file: " + args[i + 1]);
                        usage();
                    }
                    i += 2;
                    break;

                case 'r':
                    if (i + 1 >= args.length) usage();
                    SCALE = Integer.parseInt(args[i + 1]);
                    if (!QUIET) System.out.println("RMAT Scale: " + SCALE);
                    INFILENAME = "RMAT";
                    if (SCALE >= SCALE_MIN) inputSelected = true;
                    i += 2;
                    break;

                case 'a':
                    runAllScales = true;
                    inputSelected = true;
                    INFILENAME = "RMAT";
                    i++;
                    break;

                case 'q':
                    QUIET = true;
                    i++;
                    break;

                case 'd':
                    PRINT = true;
                    i++;
                    break;

                case 'x':
                    NCUBED = false;
                    i++;
                    break;

                default:
                    System.err.println("Wrong Argument: " + args[i]);
                    usage();
            }
        }

        if (!inputSelected) usage();
    }

    // ----------------------------------------------------------------
    // Memory measurement helpers
    // ----------------------------------------------------------------

    private static long getUsedMemoryBytes() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private static void forceGC() {
        System.gc();
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
    }

    // ----------------------------------------------------------------
    // Benchmarking infrastructure
    // ----------------------------------------------------------------

    @FunctionalInterface
    interface GraphAlgorithm {
        long compute(Graph graph);
    }

    // Full benchmark with correctness check
    private static void benchmark(GraphAlgorithm f, Graph originalGraph, Graph graph,
                                   String type, String name, long expectedResult) {
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
        for (int loop = 0; loop < LOOP_CNT; loop++) {
            graph.copyFrom(originalGraph);
        }
        overTime = System.nanoTime() - overTime;

        totalTime -= overTime;
        totalTime /= (double) LOOP_CNT;
        double totalTimeSec = totalTime / 1_000_000_000.0;

        // Correctness: pass -1 to skip check (used for baseline itself)
        boolean correct = (expectedResult < 0) || (result == expectedResult);
        String correctMark = correct ? "OK" : "WRONG";

        outfile.printf("%-8s\t%s\t%12d\t%12d\t%-35s\t%12.6f\t%12d\t%10.2f\t%s%n",
                type, INFILENAME,
                graph.numVertices, graph.numEdges / 2,
                name, totalTimeSec, result, peakMemMB, correctMark);
        outfile.flush();

        storeResult(new BenchmarkResult(type, name, totalTimeSec, peakMemMB, result, correct));
    }

    // Overload without correctness check (baseline calls)
    private static void benchmark(GraphAlgorithm f, Graph originalGraph, Graph graph,
                                   String type, String name) {
        benchmark(f, originalGraph, graph, type, name, -1L);
    }

    // ----------------------------------------------------------------
    // Run all experiments for a single graph scale
    // ----------------------------------------------------------------

    private static void runExperiments(int scale) {
        INFILENAME = "RMAT-" + scale;

        // Clear results for this scale
        resultsPerType.clear();

        Graph originalGraph = Graph.createRMAT(scale, EDGE_FACTOR);
        Graph graph         = Graph.allocateRMAT(scale, EDGE_FACTOR);

        if (!QUIET)
            outfile.printf("%nRMAT Scale: %d | Vertices: %d | Edges: %d%n",
                    scale, originalGraph.numVertices, originalGraph.numEdges / 2);

        // ============================================================
        // TASK 1: OPTIMIZATION APPROACH
        // LLMs given all existing TC code and asked for fastest routine
        // ============================================================
        if (!QUIET) outfile.println("--- Task 1: Optimization Approach ---");

        // Uncomment as you add task1 implementations:
        // benchmark(TCBaseline::baseline,       originalGraph, graph, "TC_OPT", "opt_baseline");
        // long baselineTCOpt = resultsPerType.get("TC_OPT").get(0).result;
        // benchmark(task1.TCOptClaude::fast,    originalGraph, graph, "TC_OPT", "opt_claude",   baselineTCOpt);
        // benchmark(task1.TCOptGemini::fast,    originalGraph, graph, "TC_OPT", "opt_gemini",   baselineTCOpt);
        // benchmark(task1.TCOptGPTCodex::fast,  originalGraph, graph, "TC_OPT", "opt_gptcodex", baselineTCOpt);

        // ============================================================
        // TASK 2A: ALGORITHM-SYNTHESIS - Triangle Counting
        // LLMs given only graph infrastructure, no TC code
        // ============================================================
        if (!QUIET) outfile.println("--- Task 2a: Algorithm-Synthesis - Triangle Counting ---");

        benchmark(TCBaseline::baseline, originalGraph, graph, "TC_SYN", "syn_tc_baseline");
        long baselineTC = resultsPerType.get("TC_SYN").get(0).result;

        // Remove the baseline from results so it doesn't affect efficiency rates
        resultsPerType.get("TC_SYN").clear();

        benchmark(TCClaude::fast,   originalGraph, graph, "TC_SYN", "syn_tc_claude",   baselineTC);
        benchmark(TCGemini::fast,   originalGraph, graph, "TC_SYN", "syn_tc_gemini",   baselineTC);
        benchmark(TCGPTCodex::fast, originalGraph, graph, "TC_SYN", "syn_tc_gptcodex", baselineTC);

        // ============================================================
        // TASK 2B: ALGORITHM-SYNTHESIS - Diameter
        // ============================================================
        if (!QUIET) outfile.println("--- Task 2b: Algorithm-Synthesis - Diameter ---");

        // Uncomment as you add diameter implementations:
        // benchmark(DiameterBaseline::compute, originalGraph, graph, "DIAM", "syn_diam_baseline");
        // long baselineDiam = resultsPerType.get("DIAM").get(0).result;
        // benchmark(DiameterClaude::fast,      originalGraph, graph, "DIAM", "syn_diam_claude",   baselineDiam);
        // benchmark(DiameterGemini::fast,      originalGraph, graph, "DIAM", "syn_diam_gemini",   baselineDiam);
        // benchmark(DiameterGPTCodex::fast,    originalGraph, graph, "DIAM", "syn_diam_gptcodex", baselineDiam);

        // ============================================================
        // TASK 2C: ALGORITHM-SYNTHESIS - Clique Number
        // ============================================================
        if (!QUIET) outfile.println("--- Task 2c: Algorithm-Synthesis - Clique Number ---");

        // Uncomment as you add clique implementations:
        // benchmark(CliqueBaseline::compute, originalGraph, graph, "CLIQUE", "syn_clique_baseline");
        // long baselineClique = resultsPerType.get("CLIQUE").get(0).result;
        // benchmark(CliqueClaude::fast,      originalGraph, graph, "CLIQUE", "syn_clique_claude",   baselineClique);
        // benchmark(CliqueGemini::fast,      originalGraph, graph, "CLIQUE", "syn_clique_gemini",   baselineClique);
        // benchmark(CliqueGPTCodex::fast,    originalGraph, graph, "CLIQUE", "syn_clique_gptcodex", baselineClique);

        // Print efficiency rates for this scale
        printEfficiencyRates();
    }

    // ----------------------------------------------------------------
    // Main
    // ----------------------------------------------------------------

    public static void main(String[] args) {
        parseFlags(args);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (!QUIET) {
            outfile.println("# LLM Graph Algorithm Benchmark");
            outfile.println("# Run: " + timestamp);
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
            try {
                Graph originalGraph = Graph.readMatrixMarketFile(INFILENAME);
                Graph graph = new Graph(originalGraph.numVertices, originalGraph.numEdges);

                if (!QUIET)
                    outfile.printf("Graph has %d vertices and %d undirected edges.%n",
                            originalGraph.numVertices, originalGraph.numEdges / 2);

                benchmark(TCBaseline::baseline, originalGraph, graph, "TC_SYN", "syn_tc_baseline");
                long baselineTC = resultsPerType.get("TC_SYN").get(0).result;
                benchmark(TCClaude::fast,   originalGraph, graph, "TC_SYN", "syn_tc_claude",   baselineTC);
                benchmark(TCGemini::fast,   originalGraph, graph, "TC_SYN", "syn_tc_gemini",   baselineTC);
                benchmark(TCGPTCodex::fast, originalGraph, graph, "TC_SYN", "syn_tc_gptcodex", baselineTC);

                printEfficiencyRates();

            } catch (IOException e) {
                System.err.println("ERROR: " + e.getMessage());
                return;
            }
        }

        if (outfile != System.out)
            outfile.close();
    }
}