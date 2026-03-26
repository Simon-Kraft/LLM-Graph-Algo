package task2;

import graph.Graph;

public final class TCGPTCodex {
    public static long fast(Graph graph) {
        final int n = graph.numVertices;
        final int[] rowPtr = graph.rowPtr;   // length n+1
        final int[] colInd = graph.colInd;   // CSR neighbors, each row sorted asc

        // Degree of each vertex
        final int[] degree = new int[n];
        for (int v = 0; v < n; v++) {
            degree[v] = rowPtr[v + 1] - rowPtr[v];
        }

        // Build oriented forward graph:
        // keep edge u->v iff (deg(u) < deg(v)) or (deg(u)==deg(v) && u < v)
        final int[] fCount = new int[n];
        for (int u = 0; u < n; u++) {
            for (int p = rowPtr[u], end = rowPtr[u + 1]; p < end; p++) {
                int v = colInd[p];
                if (degree[u] < degree[v] || (degree[u] == degree[v] && u < v)) {
                    fCount[u]++;
                }
            }
        }

        final int[] fRowPtr = new int[n + 1];
        for (int i = 0; i < n; i++) fRowPtr[i + 1] = fRowPtr[i] + fCount[i];

        final int[] fColInd = new int[fRowPtr[n]];
        final int[] write = fRowPtr.clone();
        for (int u = 0; u < n; u++) {
            for (int p = rowPtr[u], end = rowPtr[u + 1]; p < end; p++) {
                int v = colInd[p];
                if (degree[u] < degree[v] || (degree[u] == degree[v] && u < v)) {
                    fColInd[write[u]++] = v;
                }
            }
        }

        // Triangle counting by intersecting forward neighbor lists
        long triangles = 0L;
        for (int u = 0; u < n; u++) {
            int uStart = fRowPtr[u], uEnd = fRowPtr[u + 1];
            for (int up = uStart; up < uEnd; up++) {
                int v = fColInd[up];

                int i = uStart;         // N+(u)
                int j = fRowPtr[v];     // N+(v)
                int jEnd = fRowPtr[v + 1];

                while (i < uEnd && j < jEnd) {
                    int a = fColInd[i];
                    int b = fColInd[j];
                    if (a == b) {
                        triangles++;
                        i++;
                        j++;
                    } else if (a < b) {
                        i++;
                    } else {
                        j++;
                    }
                }
            }
        }

        return triangles;
    }
}