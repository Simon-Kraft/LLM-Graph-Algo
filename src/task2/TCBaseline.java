package task2;

import graph.Graph;

public class TCBaseline {

    // Baseline brute-force (ground truth for correctness checking)
    public static long baseline(Graph graph) {
        long count = 0;
        for (int u = 0; u < graph.numVertices; u++) {
            for (int i = graph.rowPtr[u]; i < graph.rowPtr[u + 1]; i++) {
                int v = graph.colInd[i];
                if (v > u) {
                    for (int j = graph.rowPtr[u]; j < graph.rowPtr[u + 1]; j++) {
                        int w = graph.colInd[j];
                        if (w > v && graph.checkEdge(v, w)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }
}