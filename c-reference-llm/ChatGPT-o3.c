/* tc_fast.c  – fast sequential triangle counter
 * Add this file to the build and declare the prototype in tc.h.
 */
#include "types.h"
#include "graph.h"
#include "tc.h"

/* Empirically tuned – feel free to move the threshold after profiling on
 * your own datasets. 16 k directed edges ≈ 8 k undirected edges.          */
#define SMALL_EDGE_THRESHOLD 16384UL

UINT_t tc_fast (const GRAPH_TYPE *graph)
{
    const UINT_t m = graph->numEdges;

    /* Very small graphs: the simple forward‑hash with degree re‑ordering
       has lower constant factors than the BFS hybrid. */
    if (m < SMALL_EDGE_THRESHOLD)
        return tc_forward_hash_degreeOrder (graph);

    /* Everything else: Bader hybrid is usually the overall winner. */
    return tc_bader_hybrid (graph);
}
