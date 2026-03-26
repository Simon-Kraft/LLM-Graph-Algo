package task1;

import graph.Graph;

/**
 * Fast sequential triangle counting on an undirected graph stored in CSR format.
 *
 * Assumes a Graph class with at least:
 *   int   numVertices
 *   int   numEdges
 *   int[] rowPtr   (length numVertices + 1)
 *   int[] colInd   (length numEdges, sorted within each row)
 *
 * Algorithm: Forward / Schank-Wagner with hash-based (boolean array) set intersection.
 *
 * For every edge (s, t) with s < t, we count how many vertices u < s
 * are already recorded as a forward neighbor of both s and t.
 * We maintain an auxiliary array A[] that stores, for each vertex t,
 * the list of forward neighbors accumulated so far, and a Size[] array
 * tracking the current length of that list.  A boolean marker array
 * (Hash) enables O(1) membership queries during intersection.
 *
 * This is the same core idea as tc_forward_hash in tc.c, translated to Java
 * with a few micro-optimizations for the JVM:
 *   - The smaller list is hashed and the larger list is probed (reduces
 *     the number of Hash set/clear operations).
 *   - Array accesses are hoisted out of inner loops where possible.
 *   - No object allocation inside the hot loop.
 */
public class TCOptClaude {

    public static long tc_fast(Graph graph) {
        final int n = graph.numVertices;
        final int m = graph.numEdges;
        final int[] Ap = graph.rowPtr;
        final int[] Ai = graph.colInd;

        // A[Ap[t] .. Ap[t]+Size[t]-1] stores forward neighbors of t seen so far
        final int[] A = new int[m];
        // Size[v] = number of forward neighbors recorded for v so far
        final int[] Size = new int[n];
        // Boolean marker for O(1) intersection lookups, indexed by vertex id
        final boolean[] Hash = new boolean[n];

        long count = 0;

        for (int s = 0; s < n; s++) {
            final int sBegin = Ap[s];
            final int sEnd   = Ap[s + 1];

            for (int i = sBegin; i < sEnd; i++) {
                final int t = Ai[i];
                if (s >= t) continue; // only process edge once, s < t

                // Intersect forward-neighbor lists of s and t using Hash
                final int sSize = Size[s];
                final int tSize = Size[t];
                final int sBase = Ap[s];
                final int tBase = Ap[t];

                // Hash the smaller list, probe the larger
                if (sSize <= tSize) {
                    // Mark forward neighbors of s
                    for (int j = 0; j < sSize; j++) {
                        Hash[A[sBase + j]] = true;
                    }
                    // Probe with forward neighbors of t
                    for (int j = 0; j < tSize; j++) {
                        if (Hash[A[tBase + j]]) {
                            count++;
                        }
                    }
                    // Clear marks
                    for (int j = 0; j < sSize; j++) {
                        Hash[A[sBase + j]] = false;
                    }
                } else {
                    // Mark forward neighbors of t
                    for (int j = 0; j < tSize; j++) {
                        Hash[A[tBase + j]] = true;
                    }
                    // Probe with forward neighbors of s
                    for (int j = 0; j < sSize; j++) {
                        if (Hash[A[sBase + j]]) {
                            count++;
                        }
                    }
                    // Clear marks
                    for (int j = 0; j < tSize; j++) {
                        Hash[A[tBase + j]] = false;
                    }
                }

                // Record s as a forward neighbor of t
                A[tBase + tSize] = s;
                Size[t] = tSize + 1;
            }
        }

        return count;
    }
}
