package task2;

import java.util.Arrays;
import graph.Graph;

public class TCGemini {

    /**
     * Finds the number of triangles in an undirected graph in CSR format.
     * Uses a Degree-Ordered Directed Acyclic Graph (DAG) construction
     * to bound the time complexity to O(E^(1.5)) and eliminate redundant checks.
     *
     * @param graph The input graph in CSR format
     * @return The total number of triangles
     */
    public static long fast(Graph graph) {
        int n = graph.numVertices;
        int[] rowPtr = graph.rowPtr;
        int[] colInd = graph.colInd;

        // 1. Allocate DAG row pointers
        int[] dagRowPtr = new int[n + 1];
        
        // 2. Compute out-degrees for the DAG
        // Edge u -> v exists in DAG if degree(u) < degree(v) or (degree(u) == degree(v) && u < v)
        for (int u = 0; u < n; u++) {
            int degU = rowPtr[u + 1] - rowPtr[u];
            int startU = rowPtr[u];
            int endU = rowPtr[u + 1];
            int count = 0;
            
            for (int i = startU; i < endU; i++) {
                int v = colInd[i];
                int degV = rowPtr[v + 1] - rowPtr[v];
                
                if (degU < degV || (degU == degV && u < v)) {
                    count++;
                }
            }
            dagRowPtr[u + 1] = count;
        }
        
        // 3. Prefix sums to set up DAG row pointers
        for (int u = 0; u < n; u++) {
            dagRowPtr[u + 1] += dagRowPtr[u];
        }
        
        // 4. Populate DAG column indices
        // Total edges in DAG will be exactly half of the original undirected edges
        int[] dagColInd = new int[dagRowPtr[n]];
        int[] tempPos = new int[n];
        System.arraycopy(dagRowPtr, 0, tempPos, 0, n);
        
        for (int u = 0; u < n; u++) {
            int degU = rowPtr[u + 1] - rowPtr[u];
            int startU = rowPtr[u];
            int endU = rowPtr[u + 1];
            
            for (int i = startU; i < endU; i++) {
                int v = colInd[i];
                int degV = rowPtr[v + 1] - rowPtr[v];
                
                if (degU < degV || (degU == degV && u < v)) {
                    dagColInd[tempPos[u]++] = v;
                }
            }
        }
        
        // 5. Count Triangles using the DAG
        long triangleCount = 0;
        
        // Marker array to enable O(1) intersection checks (faster than two-pointer scans)
        int[] marker = new int[n];
        Arrays.fill(marker, -1);
        
        for (int u = 0; u < n; u++) {
            int startU = dagRowPtr[u];
            int endU = dagRowPtr[u + 1];
            
            // Mark all outgoing neighbors of u in the DAG
            for (int i = startU; i < endU; i++) {
                marker[dagColInd[i]] = u;
            }
            
            // For each neighbor v of u
            for (int i = startU; i < endU; i++) {
                int v = dagColInd[i];
                int startV = dagRowPtr[v];
                int endV = dagRowPtr[v + 1];
                
                // For each neighbor w of v
                // If w is marked as a neighbor of u, we found the triangle: u -> v -> w <- u
                for (int j = startV; j < endV; j++) {
                    int w = dagColInd[j];
                    if (marker[w] == u) {
                        triangleCount++;
                    }
                }
            }
        }
        
        return triangleCount;
    }
}