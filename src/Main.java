import java.io.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import graph.Graph;
import task1.*;
import task2.*;
import misc.SystemInfo;

public class Main {

    private static final int EDGE_FACTOR  = 16;
    private static final int LOOP_CNT     = 10;
    private static final int SCALE_MIN    = 6;
    private static final int SCALE_START  = 6;
    private static final int SCALE_END    = 14;

    // Reference scales for efficiency rate computation
    private static final int OPT_REF_SCALE    = 14;
    private static final int TC_REF_SCALE     = 14;
    private static final int DIAM_REF_SCALE   = 14;
    private static final int CLIQUE_REF_SCALE = 8; // restricted graph scale for clique to 9 becomes otherwise runtime gets very large

    static boolean QUIET        = false;
    static boolean runAllScales = false;

    private static int         SCALE         = 0;
    private static boolean     inputSelected = false;
    private static PrintStream outfile       = System.out;
    private static String csvPath            = null;

    // ================================================================
    // Result storage
    // ================================================================

    private static class BenchmarkResult {
        String  type;
        String  name;
        double  timeSec;
        double  memMB;
        long    result;
        boolean correct;
        int     scale;

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

    private static List<BenchmarkResult>              allResults          = new ArrayList<>();
    private static Map<String, List<BenchmarkResult>> currentScaleResults = new LinkedHashMap<>();

    private static void storeResult(BenchmarkResult r) {
        allResults.add(r);
        currentScaleResults.computeIfAbsent(r.type, k -> new ArrayList<>()).add(r);
    }

    // ================================================================
    // TABLE II — Efficiency Rates (Task 2 only)
    // ================================================================

    private static String extractModelName(String name) {
        if (name.endsWith("_claude"))   return "Claude Opus 4.6";
        if (name.endsWith("_gemini"))   return "Gemini 3.1 Pro";
        if (name.endsWith("_gptcodex")) return "GPT-5.3-Codex";
        return name;
    }

    private static void printTableII_EfficiencyRates() {
        outfile.println("\n================================================================");
        outfile.println("TABLE II — Efficiency Rates per Model (Task 2 only)");
        outfile.println("================================================================");

        Map<String, Integer> refScales = new LinkedHashMap<>();
        refScales.put("TC_SYN", TC_REF_SCALE);
        refScales.put("DIAM_SYN",   DIAM_REF_SCALE);
        refScales.put("CLIQUE_SYN", CLIQUE_REF_SCALE);

        List<String> modelOrder = Arrays.asList("Claude Opus 4.6", "Gemini 3.1 Pro", "GPT-5.3-Codex");

        Map<String, Double> totalRates = new LinkedHashMap<>();
        for (String m : modelOrder) totalRates.put(m, 0.0);

        outfile.printf("%-12s\t%-20s\t%10s\t%10s\t%10s%n",
                "Algorithm", "Model", "Rel.Time", "Rel.Mem", "Rate");
        outfile.println("----------------------------------------------------------------------");

        for (Map.Entry<String, Integer> entry : refScales.entrySet()) {
            String type  = entry.getKey();
            int    scale = entry.getValue();

            List<BenchmarkResult> refResults = new ArrayList<>();
            for (BenchmarkResult r : allResults) {
                if (r.type.equals(type) && r.scale == scale) refResults.add(r);
            }
            if (refResults.isEmpty()) continue;

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
                String modelName = extractModelName(r.name);
                if (!r.correct) {
                    outfile.printf("%-12s\t%-20s\t%10s\t%10s\t%10s%n",
                            type, modelName, "-", "-", "0 (not RTU)");
                    totalRates.merge(modelName, 0.0, Double::sum);
                } else {
                    double t    = r.timeSec / minTime;
                    double m    = (r.memMB > 0) ? r.memMB / minMem : 1.0;
                    double rate = 1.0 / (t * m);
                    outfile.printf("%-12s\t%-20s\t%10.4f\t%10.4f\t%10.4f%n",
                            type, modelName, t, m, rate);
                    totalRates.merge(modelName, rate, Double::sum);
                }
            }
        }

        outfile.println("\n--- Total Efficiency Rate per Model ---");
        outfile.printf("%-20s\t%15s%n", "Model", "Efficiency Rate");
        outfile.println("------------------------------------------");
        for (String model : modelOrder) {
            outfile.printf("%-20s\t%15.4f%n", model, totalRates.getOrDefault(model, 0.0));
        }
        outfile.flush();
    }

    // ================================================================
    // TABLE III — Optimization Approach Overview (Task 1, at OPT_REF_SCALE)
    // ================================================================

    private static void printTableIII_OptimizationOverview() {
        outfile.println("\n================================================================");
        outfile.println("TABLE III — Optimization Approach Overview (RMAT-" + OPT_REF_SCALE + ")");
        outfile.println("================================================================");
        outfile.printf("%-20s\t%10s\t%7s\t%14s\t%12s\t%10s%n",
                "Model", "Compilable", "Correct", "#Triangles", "Runtime(s)", "Mem(MB)");
        outfile.println("-----------------------------------------------------------------------------------------------");

        List<BenchmarkResult> optResults = new ArrayList<>();
        for (BenchmarkResult r : allResults) {
            if (r.type.equals("TC_OPT") && r.scale == OPT_REF_SCALE) optResults.add(r);
        }

        for (BenchmarkResult r : optResults) {
            outfile.printf("%-20s\t%10s\t%7s\t%14d\t%12.6f\t%10.2f%n",
                    extractModelName(r.name),
                    "Yes",
                    r.correct ? "Yes" : "NO",
                    r.result,
                    r.timeSec,
                    r.memMB);
        }
        outfile.flush();
    }

    // ================================================================
    // Usage + flag parsing
    // ================================================================

    private static void usage() {
        System.out.println("LLM Graph Algorithm Benchmark (Java)\n");
        System.out.println("Either one of these must be selected:");
        System.out.println(" -r SCALE   [Run single RMAT graph of given scale (>= " + SCALE_MIN + ")]");
        System.out.println(" -a         [Run all RMAT scales from " + SCALE_START + " to " + SCALE_END + "]");
        System.out.println("Optional:");
        System.out.println(" -o <file>  [Save output to file]");
        System.out.println(" -q         [Quiet mode]");
        System.exit(8);
    }

    private static void parseFlags(String[] args) {
        if (args.length < 1) usage();
        int i = 0;
        while (i < args.length) {
            if (!args[i].startsWith("-")) { System.err.println("Unknown argument: " + args[i]); usage(); }
            switch (args[i].charAt(1)) {
                case 'o':
                    if (i + 1 >= args.length) usage();
                    try {
                        String outPath = args[i + 1];
                        outfile = new PrintStream(new FileOutputStream(outPath, true));
                        // Automatically derive CSV path from txt path
                        csvPath = outPath.endsWith(".txt")
                                ? outPath.substring(0, outPath.length() - 4) + ".csv"
                                : outPath + ".csv";
                    } catch (FileNotFoundException e) {
                        System.err.println("Cannot open: " + args[i + 1]); usage();
                    }
                    i += 2; break;
                case 'r':
                    if (i + 1 >= args.length) usage();
                    SCALE = Integer.parseInt(args[i + 1]);
                    if (SCALE >= SCALE_MIN) inputSelected = true;
                    i += 2; break;
                case 'a':
                    runAllScales = true; inputSelected = true; i++; break;
                case 'q':
                    QUIET = true; i++; break;
                default:
                    System.err.println("Unknown argument: " + args[i]); usage();
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
        System.gc();
        System.gc();
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

        outfile.printf("%-8s\tRMAT-%-2d\t%12d\t%12d\t%-35s\t%12.6f\t%12d\t%10.2f\t%s%n",
                type, scale,
                graph.numVertices, graph.numEdges / 2,
                name, totalTimeSec, result, peakMemMB, correctMark);
        outfile.flush();

        storeResult(new BenchmarkResult(type, name, totalTimeSec, peakMemMB, result, correct, scale));
    }

    // Overload without correctness check — used for baselines
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
        // TASK 1: OPTIMIZATION APPROACH
        // Prompt: given all TC code (bfs.c, graph.c, main.c, queue.c, tc.c)
        // ============================================================
        if (!QUIET) outfile.println("--- Task 1: Optimization Approach ---");

        benchmark(TCBaseline::baseline, originalGraph, graph, "TC_OPT", "opt_baseline", scale);
        long baselineTCOpt = currentScaleResults.get("TC_OPT").get(0).result;
        allResults.remove(allResults.size() - 1);
        currentScaleResults.get("TC_OPT").clear();

        benchmark(TCOptClaude::tc_fast,   originalGraph, graph, "TC_OPT", "opt_claude",   baselineTCOpt, scale);
        benchmark(TCOptGemini::tc_fast,   originalGraph, graph, "TC_OPT", "opt_gemini",   baselineTCOpt, scale);
        benchmark(TCOptGPTCodex::tc_fast, originalGraph, graph, "TC_OPT", "opt_gptcodex", baselineTCOpt, scale);

        // ============================================================
        // TASK 2A: ALGORITHM-SYNTHESIS — Triangle Counting
        // Prompt: given infrastructure only (bfs.c, graph.c, main.c, queue.c)
        // ============================================================
        if (!QUIET) outfile.println("--- Task 2a: Algorithm-Synthesis - Triangle Counting ---");

        benchmark(TCBaseline::baseline, originalGraph, graph, "TC_SYN", "syn_tc_baseline", scale);
        long baselineTC = currentScaleResults.get("TC_SYN").get(0).result;
        allResults.remove(allResults.size() - 1);
        currentScaleResults.get("TC_SYN").clear();

        benchmark(TCClaude::fast,   originalGraph, graph, "TC_SYN", "syn_tc_claude",   baselineTC, scale);
        benchmark(TCGemini::fast,   originalGraph, graph, "TC_SYN", "syn_tc_gemini",   baselineTC, scale);
        benchmark(TCGPTCodex::fast, originalGraph, graph, "TC_SYN", "syn_tc_gptcodex", baselineTC, scale);

        // ============================================================
        // TASK 2B: ALGORITHM-SYNTHESIS — Diameter
        // ============================================================
        if (!QUIET) outfile.println("--- Task 2b: Algorithm-Synthesis - Diameter ---");

        benchmark(DiameterBaseline::compute, originalGraph, graph, "DIAM_SYN", "syn_diam_baseline", scale);
        long baselineDiam = currentScaleResults.get("DIAM_SYN").get(0).result;
        allResults.remove(allResults.size() - 1);
        currentScaleResults.get("DIAM_SYN").clear();

        benchmark(DiameterClaude::fast,   originalGraph, graph, "DIAM_SYN", "syn_diam_claude",   baselineDiam, scale);
        benchmark(DiameterGemini::fast,   originalGraph, graph, "DIAM_SYN", "syn_diam_gemini",   baselineDiam, scale);
        benchmark(DiameterGPTCodex::fast, originalGraph, graph, "DIAM_SYN", "syn_diam_gptcodex", baselineDiam, scale);

        // ============================================================
        // TASK 2C: ALGORITHM-SYNTHESIS — Clique Number
        // Only runs up to CLIQUE_REF_SCALE — NP-hard, hangs on large graphs
        // ============================================================
        if (!QUIET) outfile.println("--- Task 2c: Algorithm-Synthesis - Clique Number ---");

        if (scale <= CLIQUE_REF_SCALE) {
            benchmark(CliqueBaseline::compute, originalGraph, graph, "CLIQUE_SYN", "syn_clique_baseline", scale);
            long baselineClique = currentScaleResults.get("CLIQUE_SYN").get(0).result;
            allResults.remove(allResults.size() - 1);
            currentScaleResults.get("CLIQUE_SYN").clear();

            benchmark(CliqueClaude::fast,   originalGraph, graph, "CLIQUE_SYN", "syn_clique_claude",   baselineClique, scale);
            benchmark(CliqueGemini::fast,   originalGraph, graph, "CLIQUE_SYN", "syn_clique_gemini",   baselineClique, scale);
            benchmark(CliqueGPTCodex::fast, originalGraph, graph, "CLIQUE_SYN", "syn_clique_gptcodex", baselineClique, scale);
        }
    }

    // ================================================================
    // CSV Export — saves all raw results for Python plotting
    // ================================================================

    private static void saveCSV(String filepath) {
        try (PrintWriter csv = new PrintWriter(new FileWriter(filepath))) {

            // --------------------------------------------------------
            // Sheet 1: Raw results — one row per benchmark run
            // --------------------------------------------------------
            csv.println("# RAW_RESULTS");
            csv.println("type,graph,scale,vertices,edges,name,model,algorithm,task,time_sec,mem_mb,result,correct");

            for (BenchmarkResult r : allResults) {
                String model     = extractModelName(r.name);
                String algorithm = extractAlgorithm(r.name, r.type);
                String task      = r.type.startsWith("TC_OPT") ? "Task1_Optimization" : "Task2_Synthesis";

                csv.printf("%s,RMAT-%d,%d,%d,%d,%s,%s,%s,%s,%.9f,%.4f,%d,%s%n",
                        r.type, r.scale, r.scale,
                        scaleToVertices(r.scale), scaleToEdges(r.scale),
                        r.name, model, algorithm, task,
                        r.timeSec, r.memMB, r.result,
                        r.correct ? "true" : "false");
            }

            // --------------------------------------------------------
            // Sheet 2: Efficiency rates — for bar charts
            // --------------------------------------------------------
            csv.println("\n# EFFICIENCY_RATES");
            csv.println("model,algorithm,scale,rel_time,rel_mem,rate,rtu");

            Map<String, Integer> refScales = new LinkedHashMap<>();
            refScales.put("TC_SYN", TC_REF_SCALE);
            refScales.put("DIAM_SYN",   DIAM_REF_SCALE);
            refScales.put("CLIQUE_SYN", CLIQUE_REF_SCALE);

            for (Map.Entry<String, Integer> entry : refScales.entrySet()) {
                String type  = entry.getKey();
                int    scale = entry.getValue();

                List<BenchmarkResult> refResults = new ArrayList<>();
                for (BenchmarkResult r : allResults) {
                    if (r.type.equals(type) && r.scale == scale) refResults.add(r);
                }
                if (refResults.isEmpty()) continue;

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
                    String modelName = extractModelName(r.name);
                    String alg       = extractAlgorithm(r.name, r.type);
                    if (!r.correct) {
                        csv.printf("%s,%s,%d,-,-,0,false%n", modelName, alg, scale);
                    } else {
                        double t    = r.timeSec / minTime;
                        double m    = (r.memMB > 0) ? r.memMB / minMem : 1.0;
                        double rate = 1.0 / (t * m);
                        csv.printf("%s,%s,%d,%.4f,%.4f,%.4f,true%n",
                                modelName, alg, scale, t, m, rate);
                    }
                }
            }

            // --------------------------------------------------------
            // Sheet 3: Speedup over baseline per scale — for scaling plots
            // --------------------------------------------------------
            csv.println("\n# SPEEDUP_OVER_BASELINE");
            csv.println("type,algorithm,model,scale,vertices,edges,time_sec,mem_mb,speedup_over_baseline,correct");

            // Group by type and scale, find baseline time
            Map<String, Double> baselineTimes = new HashMap<>();
            for (BenchmarkResult r : allResults) {
                if (r.name.contains("baseline")) {
                    baselineTimes.put(r.type + "_" + r.scale, r.timeSec);
                }
            }

            for (BenchmarkResult r : allResults) {
                if (r.name.contains("baseline")) continue;
                String key          = r.type + "_" + r.scale;
                double baselineTime = baselineTimes.getOrDefault(key, r.timeSec);
                double speedup      = baselineTime / r.timeSec;
                String model        = extractModelName(r.name);
                String alg          = extractAlgorithm(r.name, r.type);

                csv.printf("%s,%s,%s,%d,%d,%d,%.9f,%.4f,%.4f,%s%n",
                        r.type, alg, model, r.scale,
                        scaleToVertices(r.scale), scaleToEdges(r.scale),
                        r.timeSec, r.memMB, speedup,
                        r.correct ? "true" : "false");
            }

            // --------------------------------------------------------
            // Sheet 4: Task 1 optimization overview
            // --------------------------------------------------------
            csv.println("\n# OPTIMIZATION_OVERVIEW");
            csv.println("model,scale,vertices,edges,triangles,time_sec,mem_mb,correct");

            for (BenchmarkResult r : allResults) {
                if (!r.type.equals("TC_OPT")) continue;
                csv.printf("%s,%d,%d,%d,%d,%.9f,%.4f,%s%n",
                        extractModelName(r.name), r.scale,
                        scaleToVertices(r.scale), scaleToEdges(r.scale),
                        r.result, r.timeSec, r.memMB,
                        r.correct ? "true" : "false");
            }

        } catch (IOException e) {
            System.err.println("ERROR writing CSV: " + e.getMessage());
        }
    }

    // Helper: extract algorithm name from benchmark name and type
    private static String extractAlgorithm(String name, String type) {
        if (type.equals("TC_OPT") || type.equals("TC_SYN")) return "Triangle Counting";
        if (type.equals("DIAM"))   return "Diameter";
        if (type.equals("CLIQUE")) return "Clique Number";
        return type;
    }

    // Helper: approximate vertex count from RMAT scale
    private static int scaleToVertices(int scale) {
        return (int) Math.pow(2, scale);
    }

    // Helper: approximate edge count from RMAT scale
    private static int scaleToEdges(int scale) {
        return (int) Math.pow(2, scale) * EDGE_FACTOR;
    }

    // ================================================================
    // Main
    // ================================================================

    public static void main(String[] args) {
        parseFlags(args);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (!QUIET) {
            SystemInfo.print(outfile);
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
        } else {
            runExperiments(SCALE);
        }

        outfile.println("\n\n################################################################");
        outfile.println("# SUMMARY TABLES FOR REPORT");
        outfile.println("################################################################");

        printTableII_EfficiencyRates();
        printTableIII_OptimizationOverview();

        // save the results in a csv
        if (csvPath != null) saveCSV(csvPath);

        if (outfile != System.out) outfile.close();
    }
}