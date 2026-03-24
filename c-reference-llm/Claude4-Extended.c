UINT_t tc_fast(const GRAPH_TYPE *graph) {
    const UINT_t n = graph->numVertices;
    const UINT_t m = graph->numEdges;
    
    // For very small graphs, use Tim Davis's optimized algorithm directly
    if (n < 100) {
        const UINT_t* restrict Ap = graph->rowPtr;
        const UINT_t* restrict Ai = graph->colInd;
        
        bool* restrict Mark = (bool*)calloc(n, sizeof(bool));
        assert_malloc(Mark);
        
        UINT_t ntri = 0;
        for (UINT_t j = 0; j < n; j++) {
            const UINT_t j_start = Ap[j];
            const UINT_t j_end = Ap[j + 1];
            
            // Mark all neighbors of j
            for (UINT_t p = j_start; p < j_end; p++)
                Mark[Ai[p]] = true;
            
            // Count triangles through common neighbors
            for (UINT_t p = j_start; p < j_end; p++) {
                const UINT_t k = Ai[p];
                const UINT_t k_start = Ap[k];
                const UINT_t k_end = Ap[k + 1];
                
                for (UINT_t pa = k_start; pa < k_end; pa++)
                    ntri += Mark[Ai[pa]];
            }
            
            // Clear marks for next iteration
            for (UINT_t p = j_start; p < j_end; p++)
                Mark[Ai[p]] = false;
        }
        
        free(Mark);
        return ntri / 6;
    }
    
    // For larger graphs, use forward hash algorithm with degree ordering
    // Reorder vertices by degree (highest first) for better cache locality
    GRAPH_TYPE *ordered_graph = reorder_graph_by_degree(graph, REORDER_HIGHEST_DEGREE_FIRST);
    
    const UINT_t* restrict Ap = ordered_graph->rowPtr;
    const UINT_t* restrict Ai = ordered_graph->colInd;

    // Allocate data structures for forward algorithm
    bool* restrict Hash = (bool*)calloc(n, sizeof(bool));
    assert_malloc(Hash);

    UINT_t* restrict Size = (UINT_t*)calloc(n, sizeof(UINT_t));
    assert_malloc(Size);
    
    UINT_t* restrict A = (UINT_t*)calloc(m, sizeof(UINT_t));
    assert_malloc(A);

    UINT_t count = 0;

    // Forward algorithm: process vertices in order
    for (UINT_t s = 0; s < n; s++) {
        const UINT_t s_start = Ap[s];
        const UINT_t s_end = Ap[s + 1];
        
        // Process each neighbor of s
        for (UINT_t i = s_start; i < s_end; i++) {
            const UINT_t t = Ai[i];
            
            // Only process edge (s,t) where s < t to avoid double counting
            if (s < t) {
                // Compute intersection of forward neighbors using hash table
                const UINT_t s_size = Size[s];
                const UINT_t t_size = Size[t];
                
                // Optimize for empty lists
                if (s_size > 0 && t_size > 0) {
                    // Hash the smaller list for better performance
                    if (s_size <= t_size) {
                        // Hash s's forward neighbors
                        const UINT_t s_base = Ap[s];
                        for (UINT_t j = 0; j < s_size; j++)
                            Hash[A[s_base + j]] = true;
                        
                        // Check t's forward neighbors against hash
                        const UINT_t t_base = Ap[t];
                        for (UINT_t j = 0; j < t_size; j++) {
                            count += Hash[A[t_base + j]];
                        }
                        
                        // Clear hash table
                        for (UINT_t j = 0; j < s_size; j++)
                            Hash[A[s_base + j]] = false;
                    } else {
                        // Hash t's forward neighbors
                        const UINT_t t_base = Ap[t];
                        for (UINT_t j = 0; j < t_size; j++)
                            Hash[A[t_base + j]] = true;
                        
                        // Check s's forward neighbors against hash
                        const UINT_t s_base = Ap[s];
                        for (UINT_t j = 0; j < s_size; j++) {
                            count += Hash[A[s_base + j]];
                        }
                        
                        // Clear hash table
                        for (UINT_t j = 0; j < t_size; j++)
                            Hash[A[t_base + j]] = false;
                    }
                }
                
                // Add s to t's forward adjacency list
                A[Ap[t] + Size[t]] = s;
                Size[t]++;
            }
        }
    }

    // Cleanup
    free(A);
    free(Size);
    free(Hash);
    free_graph(ordered_graph);
    
    return count;
}
