package task2;

import java.util.Arrays;
import graph.Graph;;

/**
 * Fast sequential maximum clique (clique number) for an undirected graph in CSR-like form.
 *
 * Assumptions for integration with your translated framework:
 * - graph.numVertices() returns n
 * - graph.rowPtr() returns int[] of length n+1
 * - graph.colInd() returns int[] adjacency targets (sorted per row, as in your C pipeline)
 *
 * Returns omega(G): size of the largest clique.
 */
public final class CliqueGPTCodex {

    public static long fast(Graph graph) {
        final int n = graph.numVertices;
        if (n <= 1) return n;

        final int[] rowPtr = graph.rowPtr;
        final int[] colInd = graph.colInd;

        // Degree + degeneracy-style ordering (smallest degree first peel, then reverse)
        final int[] degree = new int[n];
        int maxDeg = 0;
        for (int v = 0; v < n; v++) {
            degree[v] = rowPtr[v + 1] - rowPtr[v];
            if (degree[v] > maxDeg) maxDeg = degree[v];
        }

        final int[] order = degeneracyOrder(degree, rowPtr, colInd, maxDeg); // low->high
        reverse(order); // high->low is often better for max-clique branch-and-bound

        // old vertex -> position in order
        final int[] pos = new int[n];
        for (int i = 0; i < n; i++) pos[order[i]] = i;

        // Build bitset adjacency in reordered index space
        final long[][] adjBits = new long[n][];
        final int words = (n + 63) >>> 6;
        for (int i = 0; i < n; i++) adjBits[i] = new long[words];

        for (int oldV = 0; oldV < n; oldV++) {
            final int v = pos[oldV];
            for (int e = rowPtr[oldV]; e < rowPtr[oldV + 1]; e++) {
                int oldW = colInd[e];
                int w = pos[oldW];
                if (w == v) continue;
                adjBits[v][w >>> 6] |= (1L << (w & 63));
            }
        }

        // Initial candidate set: all vertices
        final long[] all = new long[words];
        Arrays.fill(all, -1L);
        if ((n & 63) != 0) all[words - 1] &= ((1L << (n & 63)) - 1L);

        Solver s = new Solver(n, adjBits, words);
        s.expand(all, 0);
        return s.best;
    }

    // ----- Core solver: Tomita-style coloring bound + bitset BnB -----

    private static final class Solver {
        final int n, words;
        final long[][] adj;
        long best = 0;

        // Reused buffers to reduce allocations:
        final int[] verts;      // vertex list extracted from candidate bitset
        final int[] colors;     // greedy color numbers
        final long[] tmpCand;   // temp bitset for intersections
        final int[] stackIdx;   // recursion bookkeeping (optional but useful)

        Solver(int n, long[][] adj, int words) {
            this.n = n;
            this.adj = adj;
            this.words = words;
            this.verts = new int[n];
            this.colors = new int[n];
            this.tmpCand = new long[words];
            this.stackIdx = new int[n + 1];
        }

        void expand(long[] cand, int size) {
            // quick bound
            int candCount = bitCount(cand);
            if (size + candCount <= best) return;
            if (candCount == 0) {
                if (size > best) best = size;
                return;
            }

            // Build ordered vertex list + color bounds
            int m = colorSort(cand, verts, colors);

            // Process in reverse coloring order (largest color first at end)
            for (int i = m - 1; i >= 0; i--) {
                if (size + colors[i] <= best) return;

                int v = verts[i];

                // tmpCand = cand ∩ N(v)
                andInto(tmpCand, cand, adj[v]);

                int newSize = size + 1;
                if (isEmpty(tmpCand)) {
                    if (newSize > best) best = newSize;
                } else {
                    expand(tmpCand, newSize);
                }

                // Remove v from cand
                cand[v >>> 6] &= ~(1L << (v & 63));
            }
        }

        /**
         * Greedy sequential coloring on candidate set -> upper bound.
         * Fills outVerts[0..m), outColors[0..m), returns m.
         */
        int colorSort(long[] cand, int[] outVerts, int[] outColors) {
            // U = cand copy
            long[] U = cand.clone();
            int m = 0, color = 0;

            // Working independent set extraction per color
            long[] Q = new long[words];

            while (!isEmpty(U)) {
                color++;
                System.arraycopy(U, 0, Q, 0, words);

                while (!isEmpty(Q)) {
                    int v = nextSetBit(Q);
                    outVerts[m] = v;
                    outColors[m] = color;
                    m++;

                    // remove v from U
                    U[v >>> 6] &= ~(1L << (v & 63));

                    // Q = Q \ ({v} U N(v))
                    Q[v >>> 6] &= ~(1L << (v & 63));
                    andNotInPlace(Q, adj[v]);
                }
            }
            return m;
        }

        static void andInto(long[] dst, long[] a, long[] b) {
            for (int i = 0; i < dst.length; i++) dst[i] = a[i] & b[i];
        }

        static void andNotInPlace(long[] a, long[] b) {
            for (int i = 0; i < a.length; i++) a[i] &= ~b[i];
        }

        static boolean isEmpty(long[] bits) {
            for (long w : bits) if (w != 0L) return false;
            return true;
        }

        static int bitCount(long[] bits) {
            int c = 0;
            for (long w : bits) c += Long.bitCount(w);
            return c;
        }

        static int nextSetBit(long[] bits) {
            for (int i = 0; i < bits.length; i++) {
                long w = bits[i];
                if (w != 0L) return (i << 6) + Long.numberOfTrailingZeros(w);
            }
            return -1;
        }
    }

    // ----- Degeneracy ordering (bucket queue O(n+m) expected for sparse graphs) -----

    private static int[] degeneracyOrder(int[] degreeInit, int[] rowPtr, int[] colInd, int maxDeg) {
        final int n = degreeInit.length;
        final int[] deg = degreeInit.clone();

        // bucket[d] is head of linked list of vertices currently degree d
        final int[] head = new int[maxDeg + 1];
        Arrays.fill(head, -1);
        final int[] next = new int[n];
        final int[] prev = new int[n];
        Arrays.fill(next, -1);
        Arrays.fill(prev, -1);

        // insert all vertices
        for (int v = 0; v < n; v++) {
            int d = deg[v];
            next[v] = head[d];
            if (head[d] != -1) prev[head[d]] = v;
            head[d] = v;
        }

        final boolean[] removed = new boolean[n];
        final int[] order = new int[n];
        int idx = 0;
        int curMin = 0;

        for (int it = 0; it < n; it++) {
            while (curMin <= maxDeg && head[curMin] == -1) curMin++;
            int v = head[curMin];
            // pop v
            head[curMin] = next[v];
            if (head[curMin] != -1) prev[head[curMin]] = -1;
            next[v] = prev[v] = -1;

            removed[v] = true;
            order[idx++] = v;

            for (int e = rowPtr[v]; e < rowPtr[v + 1]; e++) {
                int w = colInd[e];
                if (removed[w]) continue;
                int dOld = deg[w];

                // remove w from bucket dOld
                int pw = prev[w], nw = next[w];
                if (pw != -1) next[pw] = nw;
                else head[dOld] = nw;
                if (nw != -1) prev[nw] = pw;

                // reinsert at dOld-1
                int dNew = dOld - 1;
                deg[w] = dNew;
                prev[w] = -1;
                next[w] = head[dNew];
                if (head[dNew] != -1) prev[head[dNew]] = w;
                head[dNew] = w;

                if (dNew < curMin) curMin = dNew;
            }
        }
        return order;
    }

    private static void reverse(int[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            int t = a[i]; a[i] = a[j]; a[j] = t;
        }
    }

    // Prevent instantiation
    private CliqueGPTCodex() {}
}
