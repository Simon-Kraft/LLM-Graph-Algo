#include <stdlib.h>
#include <string.h>
#include "types.h"

typedef struct {
    UINT_t vertex;
    UINT_t degree;
} vertex_degree_t;

static int compare_vd_descending(const void *a, const void *b) {
    vertex_degree_t *x = (vertex_degree_t*)a;
    vertex_degree_t *y = (vertex_degree_t*)b;
    if (x->degree > y->degree) return -1;
    if (x->degree < y->degree) return 1;
    if (x->vertex < y->vertex) return -1;
    if (x->vertex > y->vertex) return 1;
    return 0;
}

static int compare_uint_ascending(const void *a, const void *b) {
    UINT_t x = *(const UINT_t*)a;
    UINT_t y = *(const UINT_t*)b;
    if (x < y) return -1;
    if (x > y) return 1;
    return 0;
}

UINT_t tc_fast(const GRAPH_TYPE *graph) {
    const UINT_t n = graph->numVertices;
    const UINT_t m = graph->numEdges;

    vertex_degree_t *vd = (vertex_degree_t*)malloc(n * sizeof(vertex_degree_t));
    if (vd == NULL) return 0;
    for (UINT_t i = 0; i < n; i++) {
        vd[i].vertex = i;
        vd[i].degree = graph->rowPtr[i+1] - graph->rowPtr[i];
    }
    qsort(vd, n, sizeof(vertex_degree_t), compare_vd_descending);

    UINT_t *P = (UINT_t*)malloc(n * sizeof(UINT_t));
    if (P == NULL) { free(vd); return 0; }
    for (UINT_t i = 0; i < n; i++) {
        P[i] = vd[i].vertex;
    }

    UINT_t *rank = (UINT_t*)malloc(n * sizeof(UINT_t));
    if (rank == NULL) { free(vd); free(P); return 0; }
    for (UINT_t i = 0; i < n; i++) {
        rank[P[i]] = i;
    }
    free(vd);

    UINT_t *new_rowPtr = (UINT_t*)calloc(n+1, sizeof(UINT_t));
    if (new_rowPtr == NULL) { free(P); free(rank); return 0; }
    for (UINT_t i = 0; i < n; i++) {
        new_rowPtr[i+1] = new_rowPtr[i] + (graph->rowPtr[P[i]+1] - graph->rowPtr[P[i]]);
    }

    UINT_t *new_colInd = (UINT_t*)malloc(new_rowPtr[n] * sizeof(UINT_t));
    if (new_colInd == NULL) { free(P); free(rank); free(new_rowPtr); return 0; }

    UINT_t *current_pos = (UINT_t*)malloc(n * sizeof(UINT_t));
    if (current_pos == NULL) { free(P); free(rank); free(new_rowPtr); free(new_colInd); return 0; }
    for (UINT_t i = 0; i < n; i++) {
        current_pos[i] = new_rowPtr[i];
    }

    for (UINT_t i = 0; i < n; i++) {
        UINT_t orig_vertex = P[i];
        UINT_t start = graph->rowPtr[orig_vertex];
        UINT_t end = graph->rowPtr[orig_vertex+1];
        for (UINT_t j = start; j < end; j++) {
            UINT_t orig_neighbor = graph->colInd[j];
            UINT_t new_neighbor = rank[orig_neighbor];
            new_colInd[current_pos[i]++] = new_neighbor;
        }
    }
    free(P);
    free(rank);

    for (UINT_t i = 0; i < n; i++) {
        UINT_t start = new_rowPtr[i];
        UINT_t end = new_rowPtr[i+1];
        qsort(&new_colInd[start], end - start, sizeof(UINT_t), compare_uint_ascending);
    }

    bool *bitmap = (bool*)calloc(n, sizeof(bool));
    if (bitmap == NULL) {
        free(new_rowPtr);
        free(new_colInd);
        free(current_pos);
        return 0;
    }

    UINT_t count = 0;
    for (UINT_t u = 0; u < n; u++) {
        UINT_t u_start = new_rowPtr[u];
        UINT_t u_end = new_rowPtr[u+1];
        for (UINT_t j = u_start; j < u_end; j++) {
            UINT_t w = new_colInd[j];
            if (w > u) {
                bitmap[w] = true;
            }
        }

        for (UINT_t j = u_start; j < u_end; j++) {
            UINT_t v = new_colInd[j];
            if (v > u) {
                UINT_t v_start = new_rowPtr[v];
                UINT_t v_end = new_rowPtr[v+1];
                for (UINT_t k = v_start; k < v_end; k++) {
                    UINT_t w = new_colInd[k];
                    if (w > u && bitmap[w]) {
                        count++;
                    }
                }
            }
        }

        for (UINT_t j = u_start; j < u_end; j++) {
            UINT_t w = new_colInd[j];
            if (w > u) {
                bitmap[w] = false;
            }
        }
    }

    free(new_rowPtr);
    free(new_colInd);
    free(current_pos);
    free(bitmap);

    return count;
}
