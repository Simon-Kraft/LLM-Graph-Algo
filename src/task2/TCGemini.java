package task2;

import graph.Graph;

public class TCGemini {
    public static long fast(Graph graph) {
        long triangleCount = 0;
        
        // Assuming standard CSR field names translated from the C struct.
        // Adjust 'numVertices', 'rowPtrs', and 'colIdxs' to match your exact Java Graph API.
        int numVertices = graph.numVertices; 
        int[] rowPtrs = graph.rowPtr;       // Also commonly called row_offsets
        int[] colIdxs = graph.colInd;       // Also commonly called edges or column_indices

        for (int u = 0; u < numVertices; u++) {
            int uStart = rowPtrs[u];
            int uEnd = rowPtrs[u + 1];

            // Iterate over all neighbors v of vertex u
            for (int e_u = uStart; e_u < uEnd; e_u++) {
                int v = colIdxs[e_u];
                
                // To ensure we only count each triangle exactly once and skip redundant 
                // checks, we enforce an artificial directed DAG order: u < v < w.
                // We only process edge (u, v) if u < v.
                if (v <= u) {
                    continue;
                }

                // Because adjacency lists are sorted, all neighbors of u after index 'e_u' 
                // are strictly greater than v. We start our pointer for u here.
                int ptrU = e_u + 1;
                
                int ptrV = rowPtrs[v];
                int vEnd = rowPtrs[v + 1];

                // Two-pointer intersection to find common neighbors w
                while (ptrU < uEnd && ptrV < vEnd) {
                    int w_u = colIdxs[ptrU];
                    int w_v = colIdxs[ptrV];

                    if (w_u == w_v) {
                        // We found a common neighbor w! 
                        // Because ptrU started after v, we implicitly know w > v.
                        triangleCount++;
                        ptrU++;
                        ptrV++;
                    } else if (w_u < w_v) {
                        ptrU++;
                    } else {
                        ptrV++;
                    }
                }
            }
        }
        
        return triangleCount;
    }
}