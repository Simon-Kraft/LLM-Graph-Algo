package task2;

import graph.Graph;

public class TCClaude {

    /**
     * Fastest sequential triangle counting using degree-ordered forward hashing.
     *
     * Algorithm:
     * 1. Orient every edge u→v so that (degree[u] < degree[v]) or
     *    (degree[u] == degree[v] && u < v). This produces a DAG.
     * 2. For each vertex u, for each forward neighbor v of u,
     *    count |forwardNeighbors(u) ∩ forwardNeighbors(v)| using a hash/marker array.
     *
     * The hash-based intersection gives O(min(deg,deg)) per edge and avoids
     * the log factor of binary search or the branch mispredictions of merge-path
     * on irregular graphs.
     */
    public static long fast(Graph graph) {
        final int n = graph.numVertices;
        final int m = graph.numEdges;
        final int[] Ap = graph.rowPtr;
        final int[] Ai = graph.colInd;

        // Step 1: Compute degree for each vertex
        final int[] degree = new int[n];
        for (int i = 0; i < n; i++) {
            degree[i] = Ap[i + 1] - Ap[i];
        }

        // Step 2: Build the forward (oriented) adjacency structure.
        // An edge u-v is oriented u→v iff (degree[u] < degree[v]) ||
        // (degree[u] == degree[v] && u < v).
        // We store this in compact CSR: fwdPtr / fwdInd.

        // Count forward degree for each vertex
        final int[] fwdDeg = new int[n];
        for (int u = 0; u < n; u++) {
            for (int j = Ap[u]; j < Ap[u + 1]; j++) {
                int v = Ai[j];
                if (degree[u] < degree[v] || (degree[u] == degree[v] && u < v)) {
                    fwdDeg[u]++;
                }
            }
        }

        // Build prefix sum for forward rowPtr
        final int[] fwdPtr = new int[n + 1];
        fwdPtr[0] = 0;
        for (int i = 0; i < n; i++) {
            fwdPtr[i + 1] = fwdPtr[i] + fwdDeg[i];
        }

        final int fwdM = fwdPtr[n];
        final int[] fwdInd = new int[fwdM];

        // Populate forward adjacency lists
        final int[] pos = new int[n]; // current insertion position per vertex
        for (int u = 0; u < n; u++) {
            for (int j = Ap[u]; j < Ap[u + 1]; j++) {
                int v = Ai[j];
                if (degree[u] < degree[v] || (degree[u] == degree[v] && u < v)) {
                    fwdInd[fwdPtr[u] + pos[u]] = v;
                    pos[u]++;
                }
            }
        }

        // Step 3: Count triangles using hash-based intersection.
        // For each u, mark all forward neighbors of u in a boolean array,
        // then for each forward neighbor v of u, scan forward neighbors of v
        // and count hits.
        final boolean[] mark = new boolean[n];
        long count = 0;

        for (int u = 0; u < n; u++) {
            final int uStart = fwdPtr[u];
            final int uEnd = fwdPtr[u + 1];

            // Mark all forward neighbors of u
            for (int j = uStart; j < uEnd; j++) {
                mark[fwdInd[j]] = true;
            }

            // For each forward neighbor v of u, check v's forward neighbors
            for (int j = uStart; j < uEnd; j++) {
                int v = fwdInd[j];
                final int vStart = fwdPtr[v];
                final int vEnd = fwdPtr[v + 1];
                for (int k = vStart; k < vEnd; k++) {
                    if (mark[fwdInd[k]]) {
                        count++;
                    }
                }
            }

            // Unmark
            for (int j = uStart; j < uEnd; j++) {
                mark[fwdInd[j]] = false;
            }
        }

        return count;
    }
}