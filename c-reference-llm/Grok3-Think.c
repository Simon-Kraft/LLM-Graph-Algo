#include "types.h"
#include "graph.h"
#include <stdlib.h>
#include <stdio.h>
#include <assert.h>

// Helper function to get the first neighbor of vertex i
static UINT_t firstNeighborIndex(const GRAPH_TYPE *graph, UINT_t i) {
    const UINT_t* Ap = graph->rowPtr;
    const UINT_t* Ai = graph->colInd;
    UINT_t s = Ap[i];
    UINT_t e = Ap[i + 1];
    if (s == e) return graph->numVertices; // No neighbors
    return Ai[s]; // Smallest neighbor
}

// Helper function to get the next neighbor of vertex i greater than j using binary search
static UINT_t nextNeighborIndex(const GRAPH_TYPE *graph, UINT_t i, UINT_t j) {
    const UINT_t* Ap = graph->rowPtr;
    const UINT_t* Ai = graph->colInd;
    UINT_t s = Ap[i];
    UINT_t e = Ap[i + 1];

    // Binary search for the smallest neighbor greater than j
    UINT_t left = s;
    UINT_t right = e;
    while (left < right) {
        UINT_t mid = left + (right - left) / 2;
        if (Ai[mid] <= j) {
            left = mid + 1;
        } else {
            right = mid;
        }
    }
    if (left < e) {
        return Ai[left];
    } else {
        return graph->numVertices; // No more neighbors greater than j
    }
}

// Fastest sequential triangle counting function
UINT_t tc_fast(const GRAPH_TYPE *graph) {
    // Reorder the graph by degree (highest degree first) to optimize intersection sizes
    GRAPH_TYPE* graph2 = reorder_graph_by_degree(graph, REORDER_HIGHEST_DEGREE_FIRST);
    if (!graph2) {
        fprintf(stderr, "Error: Failed to reorder graph\n");
        exit(1);
    }

    const UINT_t n = graph2->numVertices;
    const UINT_t* Ap = graph2->rowPtr;
    const UINT_t* Ai = graph2->colInd;

    UINT_t count = 0;

    // Iterate over all vertices
    for (UINT_t i = 0; i < n; i++) {
        UINT_t s = Ap[i];
        UINT_t e = Ap[i + 1];

        // Consider each neighbor l of vertex i where l < i
        for (UINT_t w = s; w < e; w++) {
            UINT_t l = Ai[w];
            if (l < i) {
                // Find common neighbors j between i and l where j < l
                UINT_t j = firstNeighborIndex(graph2, i);
                UINT_t k = firstNeighborIndex(graph2, l);

                // Merge the neighbor lists of i and l to count triangles
                while (j < l && k < l) {
                    if (j < k) {
                        j = nextNeighborIndex(graph2, i, j);
                    } else if (k < j) {
                        k = nextNeighborIndex(graph2, l, k);
                    } else { // j == k
                        count++; // Found a triangle: j-l-i
                        UINT_t prev_j = j;
                        j = nextNeighborIndex(graph2, i, prev_j);
                        k = nextNeighborIndex(graph2, l, prev_j);
                    }
                }
            }
        }
    }

    // Clean up
    free_graph(graph2);

    return count;
}
