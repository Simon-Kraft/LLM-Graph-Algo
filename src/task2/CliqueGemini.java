package task2;

import java.util.Arrays;
import graph.Graph;

public class CliqueGemini {

    public static long fast(Graph graph) {
        if (graph == null || graph.numVertices == 0) {
            return 0;
        }
        
        int n = graph.numVertices;
        int maxDegree = 0;
        
        // Find the maximum degree to bound our recursion depth
        for (int i = 0; i < n; i++) {
            int deg = graph.rowPtr[i + 1] - graph.rowPtr[i];
            if (deg > maxDegree) {
                maxDegree = deg;
            }
        }
        
        // The maximum clique size cannot exceed the max degree + 1
        long[] maxCliqueSize = new long[]{1};
        
        // Pre-allocate depth buffers to avoid GC overhead during deep recursion
        // P_levels[depth] holds the candidate vertices at a given recursion depth.
        int[][] P_levels = new int[maxDegree + 2][maxDegree + 1];
        
        // Initial candidates array
        int[] P_0 = new int[n];
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        
        // Sort initial vertices by degree descending to find larger cliques faster
        Arrays.sort(order, (a, b) -> {
            int degA = graph.rowPtr[a + 1] - graph.rowPtr[a];
            int degB = graph.rowPtr[b + 1] - graph.rowPtr[b];
            return Integer.compare(degB, degA);
        });
        
        for (int i = 0; i < n; i++) {
            P_0[i] = order[i];
        }
        
        // Fast O(1) adjacency lookup board, cleared manually on the fly
        boolean[] isNeighbor = new boolean[n];
        
        // Begin Branch and Bound search
        expand(graph, 0, P_0, n, P_levels, maxCliqueSize, isNeighbor);
        
        return maxCliqueSize[0];
    }

    private static void expand(Graph graph, int currentCliqueSize, int[] P_current, int pSize, 
                               int[][] P_levels, long[] maxCliqueSize, boolean[] isNeighbor) {
        // Pruning: if current clique + remaining candidates cannot beat the max, return
        if (currentCliqueSize + pSize <= maxCliqueSize[0]) {
            return;
        }
        
        // If no more candidates, we have reached a maximal clique in this branch
        if (pSize == 0) {
            if (currentCliqueSize > maxCliqueSize[0]) {
                maxCliqueSize[0] = currentCliqueSize;
            }
            return;
        }

        // Buffer for the next depth's candidates
        int[] P_next = P_levels[currentCliqueSize + 1];

        for (int i = 0; i < pSize; i++) {
            // Further pruning during iteration
            if (currentCliqueSize + pSize - i <= maxCliqueSize[0]) {
                return; 
            }

            int v = P_current[i];
            int pNextSize = 0;

            // Mark neighbors of v for O(1) intersection testing
            int start = graph.rowPtr[v];
            int end = graph.rowPtr[v + 1];
            for (int j = start; j < end; j++) {
                isNeighbor[graph.colInd[j]] = true;
            }

            // Intersect P_current with Neighbors(v)
            for (int j = i + 1; j < pSize; j++) {
                if (isNeighbor[P_current[j]]) {
                    P_next[pNextSize++] = P_current[j];
                }
            }

            // Clean up boolean array (faster than Arrays.fill for sparse neighbors)
            for (int j = start; j < end; j++) {
                isNeighbor[graph.colInd[j]] = false;
            }

            // Recurse into deeper levels
            expand(graph, currentCliqueSize + 1, P_next, pNextSize, P_levels, maxCliqueSize, isNeighbor);
        }
    }
}
