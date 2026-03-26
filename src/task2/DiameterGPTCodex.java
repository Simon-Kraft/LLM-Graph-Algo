package task2;

import graph.Graph;

public class DiameterGPTCodex {

    public static long fast(Graph graph) {
    final int n = graph.numVertices;      // assumed translated field
    final int[] rowPtr = graph.rowPtr;    // CSR offsets, length n+1
    final int[] colInd = graph.colInd;    // CSR adjacency

    if (n <= 1) return 0L;

    // Reused work arrays (avoid repeated allocations / fills)
    final int[] q = new int[n];
    final int[] dist = new int[n];

    // Stamp arrays to avoid O(n) clearing each BFS/component
    final int[] seenStamp = new int[n];
    final int[] compStamp = new int[n];
    int stamp = 1;
    int cstamp = 1;

    long diameter = 0L;

    // Process each connected component once
    for (int s = 0; s < n; s++) {
        if (compStamp[s] == cstamp) continue; // already assigned by prior component BFS

        // --- BFS #1 from arbitrary node s: find farthest a, and mark component ---
        int a = s;
        int maxD = 0;

        int head = 0, tail = 0;
        q[tail++] = s;
        seenStamp[s] = stamp;
        dist[s] = 0;
        compStamp[s] = cstamp;

        while (head < tail) {
            int v = q[head++];
            int dv = dist[v];
            if (dv > maxD) {
                maxD = dv;
                a = v;
            }

            for (int ei = rowPtr[v]; ei < rowPtr[v + 1]; ei++) {
                int w = colInd[ei];
                if (seenStamp[w] != stamp) {
                    seenStamp[w] = stamp;
                    compStamp[w] = cstamp; // component membership
                    dist[w] = dv + 1;
                    q[tail++] = w;
                }
            }
        }

        // --- BFS #2 from farthest a: eccentricity(a), good diameter estimate / exact on trees ---
        stamp++;
        int bMax = 0;
        head = 0; tail = 0;
        q[tail++] = a;
        seenStamp[a] = stamp;
        dist[a] = 0;

        while (head < tail) {
            int v = q[head++];
            int dv = dist[v];
            if (dv > bMax) bMax = dv;

            for (int ei = rowPtr[v]; ei < rowPtr[v + 1]; ei++) {
                int w = colInd[ei];
                if (seenStamp[w] != stamp) {
                    seenStamp[w] = stamp;
                    dist[w] = dv + 1;
                    q[tail++] = w;
                }
            }
        }

        if (bMax > diameter) diameter = bMax;

        stamp++;
        cstamp++;
    }

    return diameter;
}
}
