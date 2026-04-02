package task2;

import graph.Graph;

public class TCGemini {

    /**
     * Finds the number of triangles in a graph using an O(V) marker array.
     * Assumes the graph is undirected and its adjacency lists (colInd) are sorted in ascending order.
     * * @param graph The input graph in CSR format.
     * @return The total number of triangles.
     */
    public static long fast(Graph graph) {
        long triangles = 0;
        int[] rowPtr = graph.rowPtr;
        int[] colInd = graph.colInd;
        int numVertices = graph.numVertices;

        // Uses O(V) memory. It acts as a fast lookup table.
        int[] mark = new int[numVertices];
        
        // Initialize with -1 (an invalid vertex ID)
        for (int i = 0; i < numVertices; i++) {
            mark[i] = -1; 
        }

        for (int u = 0; u < numVertices; u++) {
            int startU = rowPtr[u];
            int endU = rowPtr[u + 1];

            // 1. Mark all neighbors of 'u' with 'u' itself.
            // By marking with 'u' instead of 'true', we NEVER have to reset or clear 
            // the array between iterations, saving O(V) operations per vertex!
            for (int i = startU; i < endU; i++) {
                int w = colInd[i];
                mark[w] = u; 
            }

            // 2. Iterate over all neighbors 'v' of 'u'
            for (int i = startU; i < endU; i++) {
                int v = colInd[i];
                
                // Enforce u < v to avoid double counting
                if (v <= u) continue;

                int startV = rowPtr[v];
                int endV = rowPtr[v + 1];

                // 3. For each neighbor 'w' of 'v', check if it is also a neighbor of 'u'
                for (int j = startV; j < endV; j++) {
                    int w = colInd[j];
                    
                    // Enforce v < w so we only count triangles where u < v < w
                    if (w <= v) continue;
                    
                    // O(1) Instant Lookup Check!
                    // If mark[w] == u, then 'w' is connected to both 'u' and 'v'
                    if (mark[w] == u) {
                        triangles++;
                    }
                }
            }
        }
        
        return triangles;
    }
}