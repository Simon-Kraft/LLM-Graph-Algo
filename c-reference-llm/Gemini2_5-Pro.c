#include "types.h" // Assuming this contains GRAPH_TYPE, UINT_t, bool, etc.
#include "graph.h" // Assuming this declares reorder_graph_by_degree, free_graph, 
                   // and enum reorderDegree_t (for REORDER_HIGHEST_DEGREE_FIRST)
#include "tc.h"    // Assuming this declares intersectSizeHash_forward
#include <stdlib.h> // For calloc, free

// If assert_malloc is not in a common header, its definition would be needed.
// For example:
// #define assert_malloc(ptr) if (!(ptr)) { fprintf(stderr, "malloc failed\n"); exit(1); }


/**
 * @brief Computes the number of triangles in a graph using a fast sequential algorithm.
 *
 * This function implements the "forward" algorithm with degree ordering and hash-based intersection.
 * 1. The graph is reordered such that vertices with higher degrees appear earlier.
 * 2. It then iterates through edges (s,t) in the reordered graph (where s < t according to the new order).
 * 3. For each such edge, it counts common neighbors 'r' that appear before 's' in the new order (r < s < t).
 * This is done by intersecting lists of "backward" neighbors of s and t.
 * Each triangle is counted exactly once this way.
 *
 * @param graph Pointer to the input graph structure. The graph is treated as read-only.
 * @return The total number of triangles in the graph.
 */
UINT_t tc_fast(const GRAPH_TYPE *graph) {
    UINT_t triangle_count = 0;

    // 1. Reorder the graph by highest degree first.
    // This creates a new graph structure `graph_reordered`.
    // reorder_graph_by_degree is assumed to be available from "graph.c" / "graph.h".
    // REORDER_HIGHEST_DEGREE_FIRST is assumed to be an enum member from "graph.h".
    GRAPH_TYPE *graph_reordered = reorder_graph_by_degree(graph, REORDER_HIGHEST_DEGREE_FIRST);
    if (!graph_reordered) {
        // Handle error from reorder_graph_by_degree if necessary, though it uses assert_malloc
        return 0; // Or some error code
    }

    const UINT_t* restrict Ap_reordered = graph_reordered->rowPtr;
    const UINT_t* restrict Ai_reordered = graph_reordered->colInd;
    const UINT_t n_reordered = graph_reordered->numVertices;
    const UINT_t m_reordered = graph_reordered->numEdges;

    // 2. Allocate auxiliary data structures for the forward algorithm.
    
    // Hash array for performing intersections.
    // Corrected allocation: size n_reordered (number of vertices).
    bool* Hash = (bool *)calloc(n_reordered, sizeof(bool));
    assert_malloc(Hash); // Ensure assert_malloc is defined and available

    // Size[v] stores the count of "backward" neighbors of v found so far.
    // (i.e., neighbors u of v such that u < v in the reordered graph).
    UINT_t* Size = (UINT_t *)calloc(n_reordered, sizeof(UINT_t));
    assert_malloc(Size);
  
    // A[] stores the lists of "backward" neighbors.
    // It's structured like Ai (column indices), using Ap_reordered for offsets.
    // Total space needed is for all backward edges, at most m_reordered.
    UINT_t* A = (UINT_t *)calloc(m_reordered, sizeof(UINT_t));
    assert_malloc(A);

    // 3. Main loop of the "forward" algorithm on the reordered graph.
    for (UINT_t s = 0; s < n_reordered; s++) {
        UINT_t s_adj_start = Ap_reordered[s];
        UINT_t s_adj_end = Ap_reordered[s+1];
        
        // Iterate over neighbors 't' of 's' in the reordered graph.
        for (UINT_t i = s_adj_start; i < s_adj_end; i++) {
            UINT_t t = Ai_reordered[i];

            // Process edges (s,t) where s < t in the reordered graph.
            // This ensures each edge is considered in one direction.
            if (s < t) {
                // Count common "backward" neighbors of s and t.
                // intersectSizeHash_forward is assumed available from "tc.c" / "tc.h".
                // It intersects A_list_for_s = {r | (r,s) is edge, r < s}
                // with A_list_for_t = {r' | (r',t) is edge, r' < t}.
                // Common elements 'r' form triangles (r,s,t).
                triangle_count += intersectSizeHash_forward(graph_reordered, Hash, s, t, A, Size);
                
                // Add 's' to 't's list of backward neighbors.
                // The list for 't' in 'A' is A[Ap_reordered[t] ... Ap_reordered[t] + Degree[t] - 1].
                // Size[t] tracks the current number of elements in this list for 't'.
                if (Size[t] < (Ap_reordered[t+1] - Ap_reordered[t])) {
                    A[Ap_reordered[t] + Size[t]] = s;
                    Size[t]++;
                } else {
                    // This should not happen if degrees are consistent with Ap pointers.
                    // It indicates an issue or an unexpected graph property if Size[t]
                    // tries to exceed the allocated degree space for vertex t.
                }
            }
        }
    }

    // 4. Free auxiliary data structures.
    free(A);
    free(Size);
    free(Hash);

    // 5. Free the reordered graph structure.
    // free_graph is assumed to be available from "graph.c" / "graph.h".
    free_graph(graph_reordered);
  
    return triangle_count;
}
