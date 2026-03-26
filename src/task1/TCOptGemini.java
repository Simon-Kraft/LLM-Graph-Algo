package task1;

import graph.Graph;

public class TCOptGemini {

    /**
     * Finds the total number of triangles in the graph sequentially.
     * * @param graph The input graph in CSR format with sorted adjacency lists.
     * @return The exact count of triangles.
     */
    public static long tc_fast(Graph graph) {
        long triangles = 0;
        
        // Cache local references for performance (avoids repeated object dereferencing overhead)
        int[] rowPtr = graph.rowPtr;
        int[] colInd = graph.colInd;
        int numVertices = graph.numVertices;

        for (int u = 0; u < numVertices; u++) {
            int startU = rowPtr[u];
            int endU = rowPtr[u + 1];

            // Iterate through neighbors v of u
            for (int i = startU; i < endU; i++) {
                int v = colInd[i];
                
                // Enforce u < v to evaluate each edge exactly once 
                if (v > u) {
                    
                    // We need to find common neighbors w such that u < v < w.
                    // Because colInd is sorted, all neighbors of u starting from index 'i + 1' 
                    // are guaranteed to be > v. We start ptrU directly at i + 1.
                    int ptrU = i + 1;
                    
                    int ptrV = rowPtr[v];
                    int endV = rowPtr[v + 1];

                    // Merge-path intersection
                    while (ptrU < endU && ptrV < endV) {
                        int wU = colInd[ptrU];
                        int wV = colInd[ptrV];
                        
                        if (wU == wV) {
                            triangles++;
                            ptrU++;
                            ptrV++;
                        } else if (wU < wV) {
                            ptrU++;
                        } else {
                            // If wV is significantly behind, this efficiently advances it
                            // implicitly handling the fact we only care about elements > v
                            ptrV++;
                        }
                    }
                }
            }
        }
        
        // No division by 3 or 6 is needed because u < v < w guarantees 
        // every triangle is discovered in exactly one permutation.
        return triangles;
    }
}
