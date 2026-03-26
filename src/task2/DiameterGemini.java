package task2;

import graph.Graph;

public class DiameterGemini {

    public static long fast(Graph graph) {
        if (graph == null || graph.numVertices == 0) {
            return 0;
        }

        int n = graph.numVertices;
        int[] rowPtr = graph.rowPtr;
        int[] colInd = graph.colInd;

        // Allocate auxiliary arrays EXACTLY once to avoid Garbage Collection overhead
        int[] queue = new int[n];
        int[] visited = new int[n]; 
        
        long maxDiameter = 0;

        // Run BFS from every single vertex
        for (int i = 0; i < n; i++) {
            // By using a generation counter, we clear the visited array in O(1) time
            // since default int array elements are 0, we start currentGen at 1.
            int currentGen = i + 1; 
            
            int head = 0;
            int tail = 0;
            
            // Enqueue start vertex
            queue[tail++] = i;
            visited[i] = currentGen;
            
            // Track depth without needing a level[] array
            int currentLevelNodes = 1;
            int nextLevelNodes = 0;
            long depth = 0;
            
            while (head < tail) {
                int u = queue[head++];
                currentLevelNodes--;
                
                // CSR format: get neighbors of u
                int startEdge = rowPtr[u];
                int endEdge = rowPtr[u + 1];
                
                for (int e = startEdge; e < endEdge; e++) {
                    int v = colInd[e];
                    
                    if (visited[v] != currentGen) {
                        visited[v] = currentGen;
                        queue[tail++] = v;
                        nextLevelNodes++;
                    }
                }
                
                // If we finished processing all nodes on the current BFS level
                if (currentLevelNodes == 0) {
                    if (nextLevelNodes > 0) {
                        depth++;
                        currentLevelNodes = nextLevelNodes;
                        nextLevelNodes = 0;
                    }
                }
            }
            
            if (depth > maxDiameter) {
                maxDiameter = depth;
            }
        }
        
        return maxDiameter;
    }
}