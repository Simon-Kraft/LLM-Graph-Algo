package task2;

import graph.Graph;
import java.util.*;

public class CliqueBaseline {

    public static long compute(Graph g) {
        int n = g.numVertices;

        // Build adjacency sets for O(1) edge lookup
        @SuppressWarnings("unchecked")
        Set<Integer>[] adj = new HashSet[n];
        for (int u = 0; u < n; u++) {
            adj[u] = new HashSet<>();
            for (int i = g.rowPtr[u]; i < g.rowPtr[u + 1]; i++) {
                adj[u].add(g.colInd[i]);
            }
        }

        int[] maxClique = {0};
        List<Integer> R = new ArrayList<>();
        List<Integer> P = new ArrayList<>();
        List<Integer> X = new ArrayList<>();

        for (int i = 0; i < n; i++) P.add(i);

        bronKerbosch(adj, R, P, X, maxClique);
        return maxClique[0];
    }

    private static void bronKerbosch(Set<Integer>[] adj,
                                      List<Integer> R,
                                      List<Integer> P,
                                      List<Integer> X,
                                      int[] maxClique) {
        if (P.isEmpty() && X.isEmpty()) {
            if (R.size() > maxClique[0]) maxClique[0] = R.size();
            return;
        }

        if (P.isEmpty()) return;

        // Choose pivot u from P ∪ X with maximum connections to P
        int pivot = -1, maxConn = -1;
        List<Integer> PX = new ArrayList<>(P);
        PX.addAll(X);
        for (int u : PX) {
            int conn = 0;
            for (int v : P) if (adj[u].contains(v)) conn++;
            if (conn > maxConn) { maxConn = conn; pivot = u; }
        }

        // Iterate over P \ N(pivot)
        List<Integer> candidates = new ArrayList<>();
        for (int v : P) {
            if (!adj[pivot].contains(v)) candidates.add(v);
        }

        for (int v : candidates) {
            List<Integer> newR = new ArrayList<>(R); newR.add(v);
            List<Integer> newP = new ArrayList<>();
            List<Integer> newX = new ArrayList<>();
            for (int u : P) if (adj[v].contains(u)) newP.add(u);
            for (int u : X) if (adj[v].contains(u)) newX.add(u);

            bronKerbosch(adj, newR, newP, newX, maxClique);

            P.remove(Integer.valueOf(v));
            X.add(v);
        }
    }
}