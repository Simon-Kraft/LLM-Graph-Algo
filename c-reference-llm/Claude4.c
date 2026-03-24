UINT_t tc_fast(const GRAPH_TYPE *graph) {
    const UINT_t n = graph->numVertices;
    const UINT_t m = graph->numEdges;
    const UINT_t* restrict Ap = graph->rowPtr;
    const UINT_t* restrict Ai = graph->colInd;
    
    // For very small graphs, use simple wedge algorithm
    if (n < 100) {
        return tc_wedge_DO(graph);
    }
    
    // Use hash-based approach for medium to large graphs
    UINT_t count = 0;
    
    // Allocate hash table - size it to number of vertices for optimal performance
    bool* restrict Hash = (bool*)calloc(n, sizeof(bool));
    if (!Hash) return 0; // Handle allocation failure
    
    // Process each vertex in order
    for (UINT_t v = 0; v < n; v++) {
        const UINT_t v_start = Ap[v];
        const UINT_t v_end = Ap[v + 1];
        const UINT_t v_degree = v_end - v_start;
        
        // Skip isolated vertices
        if (v_degree == 0) continue;
        
        // Mark all neighbors of v in hash table
        for (UINT_t i = v_start; i < v_end; i++) {
            Hash[Ai[i]] = true;
        }
        
        // For each neighbor w of v where v < w (direction optimization)
        for (UINT_t i = v_start; i < v_end; i++) {
            const UINT_t w = Ai[i];
            if (v >= w) continue; // Only process edges in one direction
            
            const UINT_t w_start = Ap[w];
            const UINT_t w_end = Ap[w + 1];
            const UINT_t w_degree = w_end - w_start;
            
            // For each neighbor x of w, check if x is also neighbor of v
            // Use the smaller degree list for better cache performance
            if (v_degree <= w_degree) {
                // Iterate through v's neighbors and check if they're in w's adjacency list
                for (UINT_t j = v_start; j < v_end; j++) {
                    const UINT_t x = Ai[j];
                    if (x > w) { // Ensure v < w < x for proper triangle orientation
                        // Check if x is neighbor of w using hash lookup
                        if (Hash[x]) {
                            // Verify x is actually in w's neighbor list (Hash[x] was set by v's neighbors)
                            // We need to check if w-x edge exists
                            bool found = false;
                            for (UINT_t k = w_start; k < w_end; k++) {
                                if (Ai[k] == x) {
                                    found = true;
                                    break;
                                }
                                if (Ai[k] > x) break; // Early termination since list is sorted
                            }
                            if (found) count++;
                        }
                    }
                }
            } else {
                // Iterate through w's neighbors and check if they're neighbors of v
                for (UINT_t j = w_start; j < w_end; j++) {
                    const UINT_t x = Ai[j];
                    if (x > w && Hash[x]) { // x is neighbor of both v and w, and w < x
                        count++;
                    }
                }
            }
        }
        
        // Clear hash table for next iteration
        for (UINT_t i = v_start; i < v_end; i++) {
            Hash[Ai[i]] = false;
        }
    }
    
    free(Hash);
    return count;
}
