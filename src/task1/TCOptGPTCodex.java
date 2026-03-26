package task1;

import graph.Graph;

public final class TCOptGPTCodex {

    // Assumed Graph API (as implied by your prompt):
    // graph.numVertices : int
    // graph.rowPtr      : int[]   length n+1
    // graph.colInd      : int[]   length m
    // CSR rows are sorted ascending (true in your C pipeline via qsort in convert_edges_to_graph).

    public static long tc_fast(Graph graph) {
        final int n = graph.numVertices;
        final int[] rowPtr = graph.rowPtr;
        final int[] colInd = graph.colInd;

        // Degree-order relabeling (highest degree first) usually speeds up forward counting.
        // We count on the reordered graph.
        final int[] degree = new int[n];
        for (int v = 0; v < n; v++) degree[v] = rowPtr[v + 1] - rowPtr[v];

        // Sort vertices by degree descending, tie by vertex id ascending.
        final Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> {
            int da = degree[a], db = degree[b];
            if (da != db) return Integer.compare(db, da); // high degree first
            return Integer.compare(a, b);
        });

        final int[] newId = new int[n];
        for (int i = 0; i < n; i++) newId[order[i]] = i;

        // Build reordered CSR
        final int[] newDeg = new int[n];
        for (int oldV = 0; oldV < n; oldV++) newDeg[newId[oldV]] = degree[oldV];

        final int[] rp = new int[n + 1];
        for (int i = 0; i < n; i++) rp[i + 1] = rp[i] + newDeg[i];

        final int m = rp[n];
        final int[] ci = new int[m];
        final int[] fill = new int[n];

        for (int oldV = 0; oldV < n; oldV++) {
            int nv = newId[oldV];
            int base = rp[nv];
            int f = fill[nv];
            for (int p = rowPtr[oldV], e = rowPtr[oldV + 1]; p < e; p++) {
                ci[base + f++] = newId[colInd[p]];
            }
            fill[nv] = f;
        }

        // Sort each row after relabeling.
        for (int v = 0; v < n; v++) {
            java.util.Arrays.sort(ci, rp[v], rp[v + 1]);
        }

        // Forward triangle counting with per-vertex compact "forward lists" and timestamp-mark array.
        // This is the fastest practical sequential variant from your C family.
        final int[] fSize = new int[n];
        final int[] fAdj = new int[m]; // compact forward adjacency storage by CSR offsets

        final int[] mark = new int[n];
        int epoch = 1;
        long triangles = 0L;

        for (int s = 0; s < n; s++) {
            final int sb = rp[s], se = rp[s + 1];

            for (int p = sb; p < se; p++) {
                final int t = ci[p];
                if (s < t) { // oriented edge
                    // mark smaller forward-list
                    final int sOff = rp[s], sLen = fSize[s];
                    final int tOff = rp[t], tLen = fSize[t];

                    if (sLen <= tLen) {
                        final int targetEpoch = epoch++;
                        if (epoch == Integer.MAX_VALUE) {
                            java.util.Arrays.fill(mark, 0);
                            epoch = 1;
                        }

                        for (int i = 0; i < sLen; i++) mark[fAdj[sOff + i]] = targetEpoch;
                        for (int i = 0; i < tLen; i++) {
                            if (mark[fAdj[tOff + i]] == targetEpoch) triangles++;
                        }
                    } else {
                        final int targetEpoch = epoch++;
                        if (epoch == Integer.MAX_VALUE) {
                            java.util.Arrays.fill(mark, 0);
                            epoch = 1;
                        }

                        for (int i = 0; i < tLen; i++) mark[fAdj[tOff + i]] = targetEpoch;
                        for (int i = 0; i < sLen; i++) {
                            if (mark[fAdj[sOff + i]] == targetEpoch) triangles++;
                        }
                    }

                    // add s to forward list of t
                    fAdj[rp[t] + fSize[t]] = s;
                    fSize[t]++;
                }
            }
        }

        return triangles;
    }

    private TCOptGPTCodex() {}
}
