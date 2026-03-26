package task2;

import graph.Graph;

/**
 * Fast sequential maximum clique finder for CSR-format graphs.
 *
 * Uses a Bron-Kerbosch style branch-and-bound with:
 *   - Degree-based vertex ordering (highest degree first) for initial ordering
 *   - Greedy coloring upper bound for pruning
 *   - Pivot selection (vertex in P∪X with most connections into P)
 *   - Bitset-accelerated set intersections for small-to-moderate graphs
 *   - Array-based candidate sets for large graphs
 *
 * Assumes the Graph class from the translated C framework:
 *   int numVertices, int numEdges, int[] rowPtr, int[] colInd
 * with an undirected graph stored symmetrically in CSR.
 */
public class CliqueClaude {

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Returns the clique number (size of the largest clique) of the
     * given undirected graph in CSR format.
     */
    public static long fast(Graph graph) {
        final int n = graph.numVertices;
        if (n == 0) return 0;

        final int[] Ap = graph.rowPtr;
        final int[] Ai = graph.colInd;

        // Build adjacency bitsets (for graphs up to ~64k vertices this is
        // very cache-friendly; for larger graphs we fall back to sorted-
        // array intersection via the CSR directly).
        final boolean useBitsets = ((long) n * n / 64) <= 200_000_000L; // ~200 MB cap

        long[][] adjBits = null;
        if (useBitsets) {
            final int words = (n + 63) >>> 6;
            adjBits = new long[n][words];
            for (int u = 0; u < n; u++) {
                for (int p = Ap[u]; p < Ap[u + 1]; p++) {
                    adjBits[u][Ai[p] >>> 6] |= 1L << (Ai[p] & 63);
                }
            }
        }

        // Degeneracy ordering for initial candidate list.
        // Process vertices in reverse degeneracy order (highest core number
        // vertices first) so the search space is smallest at the leaves.
        int[] order = degeneracyOrder(n, Ap, Ai);

        // Reverse: we want to start with the vertex that has the largest
        // core number (it appears last in the degeneracy ordering).
        // We'll iterate from the end of `order` backwards.

        // Build a rank array: rank[v] = position of v in degeneracy order
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) rank[order[i]] = i;

        int[] maxClique = new int[]{1}; // current best

        // For each vertex v (processed in reverse degeneracy order),
        // build the candidate set P = neighbors of v with higher rank,
        // then search for a clique in G[P] that includes v.
        int[] cand = new int[n];
        for (int idx = n - 1; idx >= 0; idx--) {
            int v = order[idx];
            int pSize = 0;
            for (int p = Ap[v]; p < Ap[v + 1]; p++) {
                int w = Ai[p];
                if (rank[w] > rank[v]) {
                    cand[pSize++] = w;
                }
            }
            // Upper bound: even if all candidates form a clique with v,
            // can we beat the current best?
            if (pSize + 1 <= maxClique[0]) continue;

            // Greedy coloring bound on the subgraph induced by cand
            int colorBound = greedyColorBound(cand, pSize, Ap, Ai, n, adjBits);
            if (colorBound + 1 <= maxClique[0]) continue;

            // Branch-and-bound search
            if (useBitsets) {
                bbSearchBitset(cand, pSize, 1, maxClique, Ap, Ai, adjBits, n);
            } else {
                bbSearchCSR(cand, pSize, 1, maxClique, Ap, Ai, n);
            }
        }

        return maxClique[0];
    }

    // ---------------------------------------------------------------
    // Degeneracy (core) ordering
    // ---------------------------------------------------------------

    private static int[] degeneracyOrder(int n, int[] Ap, int[] Ai) {
        int[] deg = new int[n];
        for (int i = 0; i < n; i++) deg[i] = Ap[i + 1] - Ap[i];

        int maxDeg = 0;
        for (int i = 0; i < n; i++) if (deg[i] > maxDeg) maxDeg = deg[i];

        // Bucket sort
        int[] bin = new int[maxDeg + 1];
        for (int i = 0; i < n; i++) bin[deg[i]]++;
        int start = 0;
        for (int d = 0; d <= maxDeg; d++) {
            int num = bin[d];
            bin[d] = start;
            start += num;
        }

        int[] pos = new int[n];
        int[] vert = new int[n];
        for (int v = 0; v < n; v++) {
            pos[v] = bin[deg[v]];
            vert[pos[v]] = v;
            bin[deg[v]]++;
        }
        // Restore bin
        for (int d = maxDeg; d > 0; d--) bin[d] = bin[d - 1];
        bin[0] = 0;

        boolean[] removed = new boolean[n];
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            int v = vert[i];
            order[i] = v;
            removed[v] = true;
            for (int p = Ap[v]; p < Ap[v + 1]; p++) {
                int w = Ai[p];
                if (removed[w]) continue;
                int dw = deg[w];
                if (dw > 0) {
                    // Swap w with the first vertex in its bucket
                    int pw = pos[w];
                    int bw = bin[dw];
                    int u = vert[bw];
                    // Swap positions of w and u
                    vert[pw] = u;
                    vert[bw] = w;
                    pos[u] = pw;
                    pos[w] = bw;
                    bin[dw]++;
                    deg[w]--;
                }
            }
        }
        return order;
    }

    // ---------------------------------------------------------------
    // Greedy coloring upper bound
    // ---------------------------------------------------------------

    private static int greedyColorBound(int[] cand, int size,
                                         int[] Ap, int[] Ai, int n,
                                         long[][] adjBits) {
        if (size == 0) return 0;
        int[] color = new int[n]; // sparse: only cand vertices used
        int maxColor = 0;
        // color array re-initialized per call via the 'used' approach
        boolean[] used = new boolean[size + 1]; // colors 1..size
        for (int i = 0; i < size; i++) {
            int v = cand[i];
            // Mark colors of already-colored neighbors
            for (int j = 0; j < i; j++) {
                int w = cand[j];
                if (adjacent(v, w, Ap, Ai, adjBits)) {
                    if (color[w] > 0 && color[w] <= size) {
                        used[color[w]] = true;
                    }
                }
            }
            // Find smallest available color
            int c = 1;
            while (used[c]) c++;
            color[v] = c;
            if (c > maxColor) maxColor = c;
            // Reset used
            for (int j = 0; j < i; j++) {
                int w = cand[j];
                if (color[w] > 0 && color[w] <= size) {
                    used[color[w]] = false;
                }
            }
        }
        // Clear color entries
        for (int i = 0; i < size; i++) color[cand[i]] = 0;
        return maxColor;
    }

    // ---------------------------------------------------------------
    // Adjacency test
    // ---------------------------------------------------------------

    private static boolean adjacent(int u, int v, int[] Ap, int[] Ai,
                                     long[][] adjBits) {
        if (adjBits != null) {
            return (adjBits[u][v >>> 6] & (1L << (v & 63))) != 0;
        }
        // Binary search in CSR
        return binarySearchCSR(Ai, Ap[u], Ap[u + 1] - 1, v);
    }

    private static boolean binarySearchCSR(int[] Ai, int lo, int hi, int target) {
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (Ai[mid] == target) return true;
            if (Ai[mid] < target) lo = mid + 1;
            else hi = mid - 1;
        }
        return false;
    }

    // ---------------------------------------------------------------
    // Branch-and-bound (bitset variant)
    // ---------------------------------------------------------------

    private static void bbSearchBitset(int[] P, int pSize, int cliqueSize,
                                        int[] maxClique,
                                        int[] Ap, int[] Ai,
                                        long[][] adjBits, int n) {
        if (pSize == 0) {
            if (cliqueSize > maxClique[0]) maxClique[0] = cliqueSize;
            return;
        }

        // Pruning: even if we could add all of P, can we beat the best?
        if (cliqueSize + pSize <= maxClique[0]) return;

        // Greedy coloring bound on P
        int colorBound = greedyColorBoundInline(P, pSize, adjBits);
        if (cliqueSize + colorBound <= maxClique[0]) return;

        // Pivot: choose u in P that maximizes |P ∩ N(u)|
        int pivot = P[0];
        int pivotConn = 0;
        for (int i = 0; i < pSize; i++) {
            int u = P[i];
            int conn = 0;
            for (int j = 0; j < pSize; j++) {
                if (i != j && (adjBits[u][P[j] >>> 6] & (1L << (P[j] & 63))) != 0) {
                    conn++;
                }
            }
            if (conn > pivotConn) {
                pivotConn = conn;
                pivot = u;
            }
        }

        // Branch on vertices in P that are NOT neighbors of pivot
        int[] newP = new int[pSize];
        for (int i = 0; i < pSize; i++) {
            int v = P[i];
            // Skip neighbors of pivot (they'll be reached through pivot)
            if (v != pivot && (adjBits[pivot][v >>> 6] & (1L << (v & 63))) != 0) {
                continue;
            }
            // Build P' = P ∩ N(v)
            int npSize = 0;
            for (int j = 0; j < pSize; j++) {
                int w = P[j];
                if (w != v && (adjBits[v][w >>> 6] & (1L << (w & 63))) != 0) {
                    newP[npSize++] = w;
                }
            }
            bbSearchBitset(newP, npSize, cliqueSize + 1, maxClique, Ap, Ai, adjBits, n);

            // Remove v from P for subsequent iterations (swap with last)
            // Not needed since we iterate by index; the recursive call
            // uses newP which is a filtered copy.
        }
    }

    /**
     * Fast inline greedy coloring for the bitset path.
     */
    private static int greedyColorBoundInline(int[] P, int pSize, long[][] adjBits) {
        int[] color = new int[pSize];
        int maxColor = 0;
        boolean[] used = new boolean[pSize + 1];
        for (int i = 0; i < pSize; i++) {
            int v = P[i];
            for (int j = 0; j < i; j++) {
                int w = P[j];
                if ((adjBits[v][w >>> 6] & (1L << (w & 63))) != 0) {
                    if (color[j] > 0) used[color[j]] = true;
                }
            }
            int c = 1;
            while (used[c]) c++;
            color[i] = c;
            if (c > maxColor) maxColor = c;
            for (int j = 0; j < i; j++) {
                int w = P[j];
                if ((adjBits[v][w >>> 6] & (1L << (w & 63))) != 0) {
                    if (color[j] > 0) used[color[j]] = false;
                }
            }
        }
        return maxColor;
    }

    // ---------------------------------------------------------------
    // Branch-and-bound (CSR / binary-search variant for large graphs)
    // ---------------------------------------------------------------

    private static void bbSearchCSR(int[] P, int pSize, int cliqueSize,
                                     int[] maxClique,
                                     int[] Ap, int[] Ai, int n) {
        if (pSize == 0) {
            if (cliqueSize > maxClique[0]) maxClique[0] = cliqueSize;
            return;
        }
        if (cliqueSize + pSize <= maxClique[0]) return;

        // Pivot selection
        int pivot = P[0];
        int pivotConn = 0;
        for (int i = 0; i < pSize; i++) {
            int u = P[i];
            int conn = 0;
            for (int j = 0; j < pSize; j++) {
                if (i != j && binarySearchCSR(Ai, Ap[u], Ap[u + 1] - 1, P[j])) {
                    conn++;
                }
            }
            if (conn > pivotConn) {
                pivotConn = conn;
                pivot = u;
            }
        }

        int[] newP = new int[pSize];
        for (int i = 0; i < pSize; i++) {
            int v = P[i];
            if (v != pivot && binarySearchCSR(Ai, Ap[pivot], Ap[pivot + 1] - 1, v)) {
                continue;
            }
            int npSize = 0;
            for (int j = 0; j < pSize; j++) {
                int w = P[j];
                if (w != v && binarySearchCSR(Ai, Ap[v], Ap[v + 1] - 1, w)) {
                    newP[npSize++] = w;
                }
            }
            bbSearchCSR(newP, npSize, cliqueSize + 1, maxClique, Ap, Ai, n);
        }
    }
}
