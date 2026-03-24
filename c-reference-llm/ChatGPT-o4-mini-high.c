
#include "types.h"
#include "graph.h"
#include <stdlib.h>
#include <assert.h>

// Fast sequential triangle counting via forward merge-path
UINT_t tc_fast(const GRAPH_TYPE *graph) {
    const UINT_t *Ap = graph->rowPtr;
    const UINT_t *Ai = graph->colInd;
    UINT_t n = graph->numVertices;
    UINT_t m = graph->numEdges;

    // Workspace: for each vertex v, Size[v] = number of smaller neighbors seen so far
    UINT_t *Size = (UINT_t*) calloc(n, sizeof(UINT_t));
    assert(Size != NULL);

    // A will store for each vertex v the list of its smaller neighbors,
    // packed at positions Ap[v] ... Ap[v]+Size[v]-1
    UINT_t *A = (UINT_t*) calloc(m, sizeof(UINT_t));
    assert(A != NULL);

    UINT_t count = 0;
    // For each directed edge u -> v with u < v:
    for (UINT_t u = 0; u < n; u++) {
        for (UINT_t ei = Ap[u]; ei < Ap[u+1]; ei++) {
            UINT_t v = Ai[ei];
            if (u < v) {
                // Count how many w < u with edges (w,u) and (w,v):
                count += intersectSizeMergePath_forward(graph, u, v, A, Size);
                // Record u as a “smaller neighbor” of v for future merges
                A[ Ap[v] + Size[v] ] = u;
                Size[v]++;
            }
        }
    }

    free(A);
    free(Size);
    return count;
}
