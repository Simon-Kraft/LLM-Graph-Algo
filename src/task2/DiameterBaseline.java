package task2;

import graph.Graph;

public class DiameterBaseline {

    /**
     * Naive baseline diameter algorithm.
     * Runs a BFS from every vertex and returns the maximum
     * shortest-path distance found — i.e. the graph diameter.
     * O(V * (V + E)) time, no optimizations.
     */
    public static long compute(Graph g) {
        int n = g.numVertices;
        long diameter = 0;

        int[] dist = new int[n];

        for (int src = 0; src < n; src++) {
            // BFS from src
            java.util.Arrays.fill(dist, -1);
            dist[src] = 0;

            int[] queue = new int[n];
            int head = 0, tail = 0;
            queue[tail++] = src;

            while (head < tail) {
                int u = queue[head++];
                for (int i = g.rowPtr[u]; i < g.rowPtr[u + 1]; i++) {
                    int v = g.colInd[i];
                    if (dist[v] == -1) {
                        dist[v] = dist[u] + 1;
                        queue[tail++] = v;
                        if (dist[v] > diameter) diameter = dist[v];
                    }
                }
            }
        }

        return diameter;
    }
}