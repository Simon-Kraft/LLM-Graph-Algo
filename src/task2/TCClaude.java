package task2;

import graph.Graph;

public class TCClaude {

    /**
     * Fastest sequential triangle counting using forward algorithm
     * with degree-based ordering and hash-based intersection.
     *
     * Corresponds to tc_forward_hash_degreeOrder from the C framework.
     *
     * The graph must be undirected and stored in CSR format with
     * fields: numVertices, numEdges, rowPtr[], colInd[].
     */
    public static long fast(Graph graph) {
        final int n = graph.numVertices;
        final int m = graph.numEdges;
        final int[] Ap = graph.rowPtr;
        final int[] Ai = graph.colInd;

        // Step 1: Compute degree for each vertex
        int[] degree = new int[n];
        for (int i = 0; i < n; i++) {
            degree[i] = Ap[i + 1] - Ap[i];
        }

        // Step 2: Build a "forward" adjacency structure (directed acyclic graph).
        // For each undirected edge (u, v), orient it u -> v where
        // degree[u] < degree[v], or (degree[u] == degree[v] && u < v).
        // Count forward-edges per vertex.
        int[] fwdDeg = new int[n];
        for (int u = 0; u < n; u++) {
            for (int j = Ap[u]; j < Ap[u + 1]; j++) {
                int v = Ai[j];
                if (degree[u] < degree[v] || (degree[u] == degree[v] && u < v)) {
                    fwdDeg[u]++;
                }
            }
        }

        // Build forward CSR: fwdPtr and fwdInd
        int[] fwdPtr = new int[n + 1];
        fwdPtr[0] = 0;
        for (int i = 0; i < n; i++) {
            fwdPtr[i + 1] = fwdPtr[i] + fwdDeg[i];
        }
        int totalFwd = fwdPtr[n];
        int[] fwdInd = new int[totalFwd];
        int[] offset = new int[n]; // current insertion offset per vertex
        for (int u = 0; u < n; u++) {
            for (int j = Ap[u]; j < Ap[u + 1]; j++) {
                int v = Ai[j];
                if (degree[u] < degree[v] || (degree[u] == degree[v] && u < v)) {
                    fwdInd[fwdPtr[u] + offset[u]] = v;
                    offset[u]++;
                }
            }
        }

        // Step 3: Count triangles using hash-based intersection on forward neighbors.
        // For each vertex u, mark u's forward neighbors in a hash (boolean array),
        // then for each forward neighbor v of u, scan v's forward neighbors w
        // and check if w is also a forward neighbor of u.
        long count = 0;
        boolean[] mark = new boolean[n];

        for (int u = 0; u < n; u++) {
            int uStart = fwdPtr[u];
            int uEnd = fwdPtr[u + 1];
            if (uStart == uEnd) continue;

            // Mark all forward neighbors of u
            for (int j = uStart; j < uEnd; j++) {
                mark[fwdInd[j]] = true;
            }

            // For each forward neighbor v of u
            for (int j = uStart; j < uEnd; j++) {
                int v = fwdInd[j];
                // Check v's forward neighbors against u's marks
                for (int k = fwdPtr[v]; k < fwdPtr[v + 1]; k++) {
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