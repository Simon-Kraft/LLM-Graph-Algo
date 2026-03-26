package task2;

import java.util.Arrays;

import graph.Graph;

/**
 * Fastest sequential graph diameter using the iFUB (iterative Fringe Upper Bound)
 * algorithm, which combines the Double-Sweep lower bound heuristic with BFS-based
 * upper bound pruning to minimize the number of full BFS traversals.
 *
 * Assumes the Graph class mirrors the C CSR framework:
 *   - graph.numVertices  (int or long)
 *   - graph.numEdges     (int or long)
 *   - graph.rowPtr[]     (long[] of size numVertices + 1)
 *   - graph.colInd[]     (long[] of size numEdges)
 *
 * The graph is assumed to be undirected and connected. If the graph is
 * disconnected, the method returns the diameter of the largest connected
 * component (treating unreachable vertices as having infinite distance).
 */
public class DiameterClaude {

    private static final long INF = Long.MAX_VALUE;

    /**
     * Returns the diameter of the given undirected graph.
     * Uses the iFUB algorithm (Takes et al., 2013) for fast exact diameter.
     */
    public static long fast(Graph graph) {
        final int n = graph.numVertices;
        if (n <= 1) return 0;

        final long[] rowPtr = Arrays.stream(graph.rowPtr).asLongStream().toArray();
        final long[] colInd =Arrays.stream(graph.colInd).asLongStream().toArray();
        // final long[] rowPtr = graph.rowPtr;
        // final long[] colInd = graph.colInd;

        // ---- Step 1: Double-Sweep to get a good lower bound and a "center" vertex ----
        // BFS from vertex 0 (or first vertex in largest component)
        long[] dist0 = new long[n];
        bfs(graph, 0, dist0);

        // Find the farthest vertex from 0
        int u = farthestVertex(dist0, n);

        // BFS from u
        long[] distU = new long[n];
        bfs(graph, u, distU);

        // Find the farthest vertex from u -> gives lower bound on diameter
        int w = farthestVertex(distU, n);
        long lowerBound = distU[w];

        // BFS from w
        long[] distW = new long[n];
        bfs(graph, w, distW);

        // Possibly improve lower bound
        int w2 = farthestVertex(distW, n);
        if (distW[w2] > lowerBound) {
            lowerBound = distW[w2];
        }

        // ---- Step 2: iFUB - iterative pruning from a "center" vertex ----
        // Choose the midpoint of the u-w path as center (vertex minimizing eccentricity estimate)
        // We approximate this by picking the vertex that minimizes max(distU[v], distW[v])
        int center = 0;
        long bestEccEst = INF;
        for (int v = 0; v < n; v++) {
            if (distU[v] == INF || distW[v] == INF) continue;
            long eccEst = Math.max(distU[v], distW[v]);
            if (eccEst < bestEccEst) {
                bestEccEst = eccEst;
                center = v;
            }
        }

        // BFS from center to get exact eccentricity and fringe layers
        long[] distC = new long[n];
        bfs(graph, center, distC);

        long eccCenter = 0;
        for (int v = 0; v < n; v++) {
            if (distC[v] != INF && distC[v] > eccCenter) {
                eccCenter = distC[v];
            }
        }

        // Group vertices by their distance from center (fringe layers)
        // fringe[k] = list of vertices at distance k from center
        int maxDist = (int) eccCenter;
        int[][] fringe = new int[maxDist + 1][];
        int[] fringeCount = new int[maxDist + 1];

        // Count vertices at each distance
        for (int v = 0; v < n; v++) {
            if (distC[v] != INF && distC[v] <= maxDist) {
                fringeCount[(int) distC[v]]++;
            }
        }
        for (int k = 0; k <= maxDist; k++) {
            fringe[k] = new int[fringeCount[k]];
            fringeCount[k] = 0;
        }
        for (int v = 0; v < n; v++) {
            if (distC[v] != INF && distC[v] <= maxDist) {
                int k = (int) distC[v];
                fringe[k][fringeCount[k]++] = v;
            }
        }

        // ---- Step 3: iFUB iteration ----
        // Process fringe layers from outermost inward.
        // For each layer k (from eccCenter down to 0):
        //   - If lowerBound >= 2*k, we can stop (no vertex at distance <= k from
        //     center can have eccentricity > lowerBound).
        //   - Otherwise, BFS from each vertex in fringe[k] and update lowerBound.
        long[] distV = new long[n];
        for (int k = maxDist; k >= 0; k--) {
            if (lowerBound >= 2L * k) {
                break;  // Pruning: no vertex at distance <= k can beat current lower bound
            }
            for (int idx = 0; idx < fringe[k].length; idx++) {
                int v = fringe[k][idx];
                bfs(graph, v, distV);
                long eccV = 0;
                for (int x = 0; x < n; x++) {
                    if (distV[x] != INF && distV[x] > eccV) {
                        eccV = distV[x];
                    }
                }
                if (eccV > lowerBound) {
                    lowerBound = eccV;
                }
                // Re-check pruning condition after each BFS
                if (lowerBound >= 2L * k) {
                    break;
                }
            }
        }

        return lowerBound;
    }

    /**
     * Standard BFS from a source vertex. Sets dist[v] = shortest distance from src,
     * or INF if unreachable. Uses an array-based queue for cache efficiency.
     */
    private static void bfs(Graph graph, int src, long[] dist) {
        final int n = graph.numVertices;
        final long[] rowPtr = Arrays.stream(graph.rowPtr).asLongStream().toArray();
        final long[] colInd =Arrays.stream(graph.colInd).asLongStream().toArray();
        for (int i = 0; i < n; i++) {
            dist[i] = INF;
        }
        dist[src] = 0;

        // Array-based queue (no object overhead, cache-friendly)
        int[] queue = new int[n];
        int head = 0, tail = 0;
        queue[tail++] = src;

        while (head < tail) {
            int v = queue[head++];
            long nextDist = dist[v] + 1;
            long start = rowPtr[v];
            long end = rowPtr[v + 1];
            for (long i = start; i < end; i++) {
                int w = (int) colInd[(int) i];
                if (dist[w] == INF) {
                    dist[w] = nextDist;
                    queue[tail++] = w;
                }
            }
        }
    }

    /**
     * Returns the vertex with the largest finite distance in the dist array.
     */
    private static int farthestVertex(long[] dist, int n) {
        int best = 0;
        long bestDist = -1;
        for (int v = 0; v < n; v++) {
            if (dist[v] != INF && dist[v] > bestDist) {
                bestDist = dist[v];
                best = v;
            }
        }
        return best;
    }
}
