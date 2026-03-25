public class TCGPTCodex {

    /**
     * Fast sequential triangle counting on CSR graph.
     *
     * Strategy:
     * 1) Orient each undirected edge (u,v) from lower-rank -> higher-rank,
     *    where rank is (degree, vertex id): lower degree first, tie by lower id.
     * 2) Build a forward adjacency list F[u] containing only out-neighbors.
     * 3) For every oriented edge (u,v), count |F[u] ∩ F[v]| with two pointers.
     *
     * Each triangle is counted exactly once.
     */
    public static long fast(Graph graph) {
        final int n = graph.numVertices;
        final int[] Ap = graph.rowPtr;
        final int[] Ai = graph.colInd;

        // Degree of each vertex
        final int[] deg = new int[n];
        for (int u = 0; u < n; u++) {
            deg[u] = Ap[u + 1] - Ap[u];
        }

        // First pass: count forward out-degree after orientation
        final int[] fDeg = new int[n];
        for (int u = 0; u < n; u++) {
            int du = deg[u];
            for (int p = Ap[u]; p < Ap[u + 1]; p++) {
                int v = Ai[p];
                int dv = deg[v];

                // orient u -> v if u has smaller (deg, id)
                if (du < dv || (du == dv && u < v)) {
                    fDeg[u]++;
                }
            }
        }

        // Prefix sums for forward CSR
        final int[] Fp = new int[n + 1];
        for (int u = 0; u < n; u++) {
            Fp[u + 1] = Fp[u] + fDeg[u];
        }

        // Build forward adjacency
        final int[] Fa = new int[Fp[n]];
        final int[] write = new int[n];
        System.arraycopy(Fp, 0, write, 0, n);

        for (int u = 0; u < n; u++) {
            int du = deg[u];
            for (int p = Ap[u]; p < Ap[u + 1]; p++) {
                int v = Ai[p];
                int dv = deg[v];
                if (du < dv || (du == dv && u < v)) {
                    Fa[write[u]++] = v;
                }
            }
        }

        // Count triangles by intersecting forward lists
        long triangles = 0L;
        for (int u = 0; u < n; u++) {
            int uStart = Fp[u];
            int uEnd = Fp[u + 1];

            for (int p = uStart; p < uEnd; p++) {
                int v = Fa[p];

                int i = uStart;        // F[u]
                int iEnd = uEnd;
                int j = Fp[v];         // F[v]
                int jEnd = Fp[v + 1];

                // Two-pointer intersection
                while (i < iEnd && j < jEnd) {
                    int a = Fa[i];
                    int b = Fa[j];
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