import java.io.*;
import java.util.Arrays;

import graph.Graph;
import task2.*;

public class Main {

    private static final int DEFAULT_SCALE = 10;
    private static final int EDGE_FACTOR = 16;
    private static final int LOOP_CNT = 10;
    private static final int SCALE_MIN = 6;

    static boolean QUIET = false;
    static boolean PRINT = false;
    static boolean NCUBED = true;

    private static String INFILENAME = null;
    private static int SCALE = 0;
    private static boolean inputSelected = false;
    private static PrintStream outfile = System.out;

    private static void usage() {
        System.out.println("Triangle Counting (Java)\n");
        System.out.println("Usage:\n");
        System.out.println("Either one of these two must be selected:");
        System.out.println(" -f <filename>   [Input Graph in Matrix Market format]");
        System.out.println(" -r SCALE        [Use RMAT graph of size SCALE] (SCALE must be >= " + SCALE_MIN + ")");
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

    // --- Benchmarking infrastructure ---

    @FunctionalInterface
    interface TriangleCounter {
        long count(Graph graph);
    }

    private static void benchmarkTC(TriangleCounter f, Graph originalGraph, Graph graph, String name) {
        long numTriangles = 0;

        double totalTime = System.nanoTime();
        for (int loop = 0; loop < LOOP_CNT; loop++) {
            graph.copyFrom(originalGraph);
            numTriangles = f.count(graph);
        }
        totalTime = System.nanoTime() - totalTime;

        double overTime = System.nanoTime();
        for (int loop = 0; loop < LOOP_CNT; loop++) {
            graph.copyFrom(originalGraph);
        }
        overTime = System.nanoTime() - overTime;

        totalTime -= overTime;
        totalTime /= (double) LOOP_CNT;
        double totalTimeSec = totalTime / 1_000_000_000.0;

        outfile.printf("TC\t%s\t%12d\t%12d\t%-30s\t%9.6f\t%12d%n",
                INFILENAME,
                graph.numVertices, graph.numEdges / 2,
                name, totalTimeSec, numTriangles);
        outfile.flush();
    }

    @FunctionalInterface
    interface BFSAlgorithm {
        void run(Graph graph, int startVertex, int[] level, boolean[] visited);
    }

    private static void benchmarkBFS(BFSAlgorithm f, Graph originalGraph, String name) {
        int n = originalGraph.numVertices;
        boolean[] visited = new boolean[n];
        int[] level = new int[n];

        double totalTime = System.nanoTime();
        for (int loop = 0; loop < LOOP_CNT; loop++) {
            Arrays.fill(visited, false);
            Arrays.fill(level, n + 1);
            for (int i = 0; i < n; i++) {
                if (!visited[i])
                    f.run(originalGraph, i, level, visited);
            }
        }
        totalTime = System.nanoTime() - totalTime;
        totalTime /= (double) LOOP_CNT;
        double totalTimeSec = totalTime / 1_000_000_000.0;

        outfile.printf("BFS\t%s\t%12d\t%12d\t%-30s\t%9.6f%n",
                INFILENAME,
                originalGraph.numVertices, originalGraph.numEdges / 2,
                name, totalTimeSec);
        outfile.flush();
    }

    public static void main(String[] args) {
        parseFlags(args);

        Graph originalGraph;
        Graph graph;

        if (SCALE > 0) {
            originalGraph = Graph.createRMAT(SCALE, EDGE_FACTOR);
            graph = Graph.allocateRMAT(SCALE, EDGE_FACTOR);
        } else {
            if (INFILENAME != null) {
                try {
                    originalGraph = Graph.readMatrixMarketFile(INFILENAME);
                } catch (IOException e) {
                    System.err.println("ERROR: " + e.getMessage());
                    return;
                }
                graph = new Graph(originalGraph.numVertices, originalGraph.numEdges);
            } else {
                System.err.println("ERROR: No input graph selected.");
                return;
            }
        }

        if (!QUIET)
            outfile.printf("Graph has %d vertices and %d undirected edges. Timing loop count %d.%n",
                    originalGraph.numVertices, originalGraph.numEdges / 2, LOOP_CNT);

        if (PRINT)
            originalGraph.print(outfile);

        // // --- Run BFS benchmarks ---
        // benchmarkBFS(BFS::bfsVisited, originalGraph, "bfs_visited");
        // benchmarkBFS(BFS::bfsHybridVisited, originalGraph, "bfs_hybrid_visited");

        // --- Run TC benchmarks ---
        benchmarkTC(TCBaseline::baseline, originalGraph, graph, "tc_baseline");
        benchmarkTC(TCClaude::fast, originalGraph, graph, "tc_claude");
        benchmarkTC(TCGemini::fast, originalGraph, graph, "tc_gemini");
        benchmarkTC(TCGPTCodex::fast, originalGraph, graph, "tc_gptcodex");

        if (outfile != System.out)
            outfile.close();
    }
}
