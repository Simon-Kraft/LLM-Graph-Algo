package graph;
import java.util.Arrays;

/**
 * Triangle Counting algorithms — Java translation of tc.c
 *
 * The graph is stored in CSR format with undirected edges (each edge stored twice).
 * Neighbor lists are sorted.
 */
public class TC {

    private static final int BADER_RECURSIVE_BASE = 100000;

    // ---------------------------------------------------------------
    // Davis (SuiteSparse:GraphBLAS)
    // ---------------------------------------------------------------
    public static long davis(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;

        boolean[] mark = new boolean[n];
        long ntri = 0;

        for (int j = 0; j < n; j++) {
            for (int p = Ap[j]; p < Ap[j + 1]; p++)
                mark[Ai[p]] = true;

            for (int p = Ap[j]; p < Ap[j + 1]; p++) {
                int k = Ai[p];
                for (int pa = Ap[k]; pa < Ap[k + 1]; pa++)
                    if (mark[Ai[pa]]) ntri++;
            }

            for (int p = Ap[j]; p < Ap[j + 1]; p++)
                mark[Ai[p]] = false;
        }
        return ntri / 6;
    }

    // ---------------------------------------------------------------
    // Wedge
    // ---------------------------------------------------------------
    public static long wedge(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        long count = 0;

        for (int i = 0; i < n; i++) {
            int s = Ap[i], e = Ap[i + 1];
            for (int j = s; j < e; j++) {
                int neighbor1 = Ai[j];
                for (int k = s; k < e; k++) {
                    int neighbor2 = Ai[k];
                    if (neighbor1 != neighbor2) {
                        for (int l = Ap[neighbor1]; l < Ap[neighbor1 + 1]; l++) {
                            if (Ai[l] == neighbor2) {
                                count++;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return count / 6;
    }

    // ---------------------------------------------------------------
    // Wedge — Direction Oriented
    // ---------------------------------------------------------------
    public static long wedgeDO(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        long count = 0;

        for (int i = 0; i < n; i++) {
            int s = Ap[i], e = Ap[i + 1];
            for (int j = s; j < e; j++) {
                int neighbor1 = Ai[j];
                if (neighbor1 > i) {
                    for (int k = s; k < e; k++) {
                        int neighbor2 = Ai[k];
                        if (neighbor1 != neighbor2 && neighbor2 > neighbor1) {
                            for (int l = Ap[neighbor1]; l < Ap[neighbor1 + 1]; l++) {
                                if (Ai[l] == neighbor2) {
                                    count++;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    // ---------------------------------------------------------------
    // Triples — O(n^3)
    // ---------------------------------------------------------------
    public static long triples(Graph graph) {
        int n = graph.numVertices;
        long count = 0;

        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                for (int k = 0; k < n; k++)
                    if (graph.checkEdge(i, j) && graph.checkEdge(j, k) && graph.checkEdge(k, i))
                        count++;

        return count / 6;
    }

    // ---------------------------------------------------------------
    // Triples — Direction Oriented
    // ---------------------------------------------------------------
    public static long triplesDO(Graph graph) {
        int n = graph.numVertices;
        long count = 0;

        for (int i = 0; i < n; i++)
            for (int j = i; j < n; j++)
                for (int k = j; k < n; k++)
                    if (graph.checkEdge(i, j) && graph.checkEdge(j, k) && graph.checkEdge(k, i))
                        count++;

        return count;
    }

    // ---------------------------------------------------------------
    // Intersect — Merge Path
    // ---------------------------------------------------------------
    public static long intersectMergePath(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        long count = 0;

        for (int v = 0; v < n; v++)
            for (int i = Ap[v]; i < Ap[v + 1]; i++)
                count += graph.intersectSizeMergePath(v, Ai[i]);

        return count / 6;
    }

    // ---------------------------------------------------------------
    // Intersect — Merge Path, Direction Oriented
    // ---------------------------------------------------------------
    public static long intersectMergePathDO(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        long count = 0;

        for (int v = 0; v < n; v++)
            for (int i = Ap[v]; i < Ap[v + 1]; i++) {
                int w = Ai[i];
                if (v < w)
                    count += graph.intersectSizeMergePath(v, w);
            }

        return count / 3;
    }

    // ---------------------------------------------------------------
    // Intersect — Binary Search
    // ---------------------------------------------------------------
    public static long intersectBinarySearch(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        long count = 0;

        for (int v = 0; v < n; v++)
            for (int i = Ap[v]; i < Ap[v + 1]; i++)
                count += graph.intersectSizeBinarySearch(v, Ai[i]);

        return count / 6;
    }

    // ---------------------------------------------------------------
    // Intersect — Binary Search, Direction Oriented
    // ---------------------------------------------------------------
    public static long intersectBinarySearchDO(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        long count = 0;

        for (int v = 0; v < n; v++)
            for (int i = Ap[v]; i < Ap[v + 1]; i++) {
                int w = Ai[i];
                if (v < w)
                    count += graph.intersectSizeBinarySearch(v, w);
            }

        return count / 3;
    }

    // ---------------------------------------------------------------
    // Intersect — Partition (recursive binary search)
    // ---------------------------------------------------------------
    public static long intersectPartition(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        long count = 0;

        for (int v = 0; v < n; v++)
            for (int i = Ap[v]; i < Ap[v + 1]; i++) {
                int w = Ai[i];
                count += Graph.searchListsWithPartitioning(Ai, Ap[v], Ap[v + 1] - 1, Ai, Ap[w], Ap[w + 1] - 1);
            }

        return count / 6;
    }

    // ---------------------------------------------------------------
    // Intersect — Partition, Direction Oriented
    // ---------------------------------------------------------------
    public static long intersectPartitionDO(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        long count = 0;

        for (int v = 0; v < n; v++)
            for (int i = Ap[v]; i < Ap[v + 1]; i++) {
                int w = Ai[i];
                if (v < w)
                    count += Graph.searchListsWithPartitioning(Ai, Ap[v], Ap[v + 1] - 1, Ai, Ap[w], Ap[w + 1] - 1);
            }

        return count / 3;
    }

    // ---------------------------------------------------------------
    // Intersect — Hash
    // ---------------------------------------------------------------
    public static long intersectHash(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        boolean[] hash = new boolean[n];
        long count = 0;

        for (int v = 0; v < n; v++)
            for (int i = Ap[v]; i < Ap[v + 1]; i++)
                count += graph.intersectSizeHash(hash, v, Ai[i]);

        return count / 6;
    }

    // ---------------------------------------------------------------
    // Intersect — Hash, Direction Oriented
    // ---------------------------------------------------------------
    public static long intersectHashDO(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        boolean[] hash = new boolean[n];
        long count = 0;

        for (int v = 0; v < n; v++)
            for (int i = Ap[v]; i < Ap[v + 1]; i++) {
                int w = Ai[i];
                if (v < w)
                    count += graph.intersectSizeHash(hash, v, w);
            }

        return count / 3;
    }

    // ---------------------------------------------------------------
    // Low (HPEC 2017)
    // ---------------------------------------------------------------
    public static long low(Graph graph) {
        int[] IA = graph.rowPtr;
        int[] JA = graph.colInd;
        int N = graph.numVertices;
        long delta = 0;

        for (int i = 1; i < N - 1; i++) {
            int currRowX = IA[i];
            int currRowA = IA[i + 1];
            int numNnzCurrRowX = currRowA - currRowX;
            int xColBeginIdx = currRowX;
            int xColEndIdx = xColBeginIdx;
            int rowBoundIdx = xColBeginIdx + numNnzCurrRowX;
            int colXMax = i - 1;

            while (xColEndIdx < rowBoundIdx && JA[xColEndIdx] < colXMax)
                xColEndIdx++;
            if (xColEndIdx >= rowBoundIdx || JA[xColEndIdx] > colXMax)
                xColEndIdx--;

            int yColBeginIdx = xColEndIdx + 1;
            int yColEndIdx = rowBoundIdx - 1;
            int numNnzY = (yColEndIdx - yColBeginIdx) + 1;
            int numNnzX = (xColEndIdx - xColBeginIdx) + 1;

            int yColFirst = i + 1;
            int xColFirst = 0;
            int yColIdx = yColBeginIdx;

            for (int j = 0; j < numNnzY; j++, yColIdx++) {
                int rowIndexA = JA[yColIdx] - yColFirst;
                int xColIdx = xColBeginIdx;
                int numNnzA = IA[currRowA + rowIndexA + 1] - IA[currRowA + rowIndexA];
                int aColIdx = IA[currRowA + rowIndexA];
                int aColMaxIdx = aColIdx + numNnzA;

                for (int k = 0; k < numNnzX && aColIdx < aColMaxIdx && JA[aColIdx] <= colXMax; k++) {
                    int rowIndexX = JA[xColIdx] - xColFirst;
                    while (aColIdx < aColMaxIdx && JA[aColIdx] < JA[xColIdx])
                        aColIdx++;
                    if (aColIdx < aColMaxIdx && JA[aColIdx] == rowIndexX)
                        delta++;
                    xColIdx++;
                }
            }
        }
        return delta;
    }

    // ---------------------------------------------------------------
    // Forward (Schank & Wagner 2005) — Merge Path
    // ---------------------------------------------------------------
    public static long forward(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        int m = graph.numEdges;
        long count = 0;

        int[] size = new int[n];
        int[] A = new int[m];

        for (int s = 0; s < n; s++) {
            for (int i = Ap[s]; i < Ap[s + 1]; i++) {
                int t = Ai[i];
                if (s < t) {
                    count += graph.intersectSizeMergePathForward(s, t, A, size);
                    A[Ap[t] + size[t]] = s;
                    size[t]++;
                }
            }
        }
        return count;
    }

    // ---------------------------------------------------------------
    // Forward — Hash
    // ---------------------------------------------------------------
    private static long forwardHashConfigSize(Graph graph, int hashSize) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        int m = graph.numEdges;
        long count = 0;

        boolean[] hash = new boolean[hashSize == 0 ? m : hashSize];
        int[] size = new int[n];
        int[] A = new int[m];

        for (int s = 0; s < n; s++) {
            for (int i = Ap[s]; i < Ap[s + 1]; i++) {
                int t = Ai[i];
                if (s < t) {
                    count += graph.intersectSizeHashForward(hash, s, t, A, size);
                    A[Ap[t] + size[t]] = s;
                    size[t]++;
                }
            }
        }
        return count;
    }

    public static long forwardHash(Graph graph) {
        return forwardHashConfigSize(graph, 0);
    }

    // ---------------------------------------------------------------
    // Forward — Hash Skip
    // ---------------------------------------------------------------
    private static long forwardHashSkipConfigSize(Graph graph, int hashSize) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        int m = graph.numEdges;
        long count = 0;

        boolean[] hash = new boolean[hashSize == 0 ? m : hashSize];
        int[] size = new int[n];
        int[] A = new int[m];

        for (int s = 0; s < n; s++) {
            for (int i = Ap[s]; i < Ap[s + 1]; i++) {
                int t = Ai[i];
                if (s < t) {
                    count += graph.intersectSizeHashSkipForward(hash, s, t, A, size);
                    A[Ap[t] + size[t]] = s;
                    size[t]++;
                }
            }
        }
        return count;
    }

    public static long forwardHashSkip(Graph graph) {
        return forwardHashSkipConfigSize(graph, 0);
    }

    // ---------------------------------------------------------------
    // Forward — Hash, Degree Order (highest first)
    // ---------------------------------------------------------------
    public static long forwardHashDegreeOrder(Graph graph) {
        Graph graph2 = graph.reorderByDegree(Graph.REORDER_HIGHEST_DEGREE_FIRST);
        return forwardHash(graph2);
    }

    // ---------------------------------------------------------------
    // Forward — Hash, Degree Order Reverse (lowest first)
    // ---------------------------------------------------------------
    public static long forwardHashDegreeOrderReverse(Graph graph) {
        Graph graph2 = graph.reorderByDegree(Graph.REORDER_LOWEST_DEGREE_FIRST);
        return forwardHash(graph2);
    }

    // ---------------------------------------------------------------
    // Compact Forward (Schank 2007 / Latapy 2006)
    // ---------------------------------------------------------------
    private static int firstNeighborIndex(Graph graph, int i) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        int index = n - 1;
        for (int w = Ap[i]; w < Ap[i + 1]; w++)
            if (Ai[w] < index) index = Ai[w];
        return index;
    }

    private static int nextNeighborIndex(Graph graph, int i, int j) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        int index = n - 1;
        for (int w = Ap[i]; w < Ap[i + 1]; w++)
            if (Ai[w] > j && Ai[w] < index) index = Ai[w];
        return index;
    }

    public static long compactForward(Graph graph) {
        Graph graph2 = graph.reorderByDegree(Graph.REORDER_HIGHEST_DEGREE_FIRST);
        int[] Ap = graph2.rowPtr;
        int[] Ai = graph2.colInd;
        int n = graph2.numVertices;
        long count = 0;

        for (int i = 0; i < n; i++) {
            for (int w = Ap[i]; w < Ap[i + 1]; w++) {
                int l = Ai[w];
                if (l < i) {
                    int jj = firstNeighborIndex(graph2, i);
                    int kk = firstNeighborIndex(graph2, l);
                    while (jj < l && kk < l) {
                        if (jj < kk)
                            jj = nextNeighborIndex(graph2, i, jj);
                        else if (kk < jj)
                            kk = nextNeighborIndex(graph2, l, kk);
                        else {
                            count++;
                            jj = nextNeighborIndex(graph2, i, jj);
                            kk = nextNeighborIndex(graph2, l, kk);
                        }
                    }
                }
            }
        }
        return count;
    }

    // ---------------------------------------------------------------
    // Helper: BFS for Bader (level starts at 1, 0 = unvisited)
    // ---------------------------------------------------------------
    private static void bfsBader(Graph graph, int startVertex, int[] level, Queue queue, boolean[] visited) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;

        visited[startVertex] = true;
        queue.enqueue(startVertex);
        level[startVertex] = 1;

        while (!queue.isEmpty()) {
            int v = queue.dequeue();
            for (int i = Ap[v]; i < Ap[v + 1]; i++) {
                int w = Ai[i];
                if (!visited[w]) {
                    visited[w] = true;
                    queue.enqueue(w);
                    level[w] = level[v] + 1;
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Helper: Bader's merge-path intersection with level split
    // ---------------------------------------------------------------
    private static long[] baderIntersectSizeMergePath(Graph graph, int[] level, int v, int w) {
        int[] Ai = graph.colInd;
        int vb = graph.rowPtr[v], ve = graph.rowPtr[v + 1];
        int wb = graph.rowPtr[w], we = graph.rowPtr[w + 1];
        int levelV = level[v];
        long c1 = 0, c2 = 0;

        int ptrV = vb, ptrW = wb;
        while (ptrV < ve && ptrW < we) {
            if (Ai[ptrV] == Ai[ptrW]) {
                if (levelV == level[Ai[ptrV]]) c2++;
                else c1++;
                ptrV++;
                ptrW++;
            } else if (Ai[ptrV] < Ai[ptrW])
                ptrV++;
            else
                ptrW++;
        }
        return new long[]{c1, c2};
    }

    // ---------------------------------------------------------------
    // Helper: Bader2's merge-path intersection (single counter)
    // ---------------------------------------------------------------
    private static long bader2IntersectSizeMergePath(Graph graph, int[] level, int v, int w) {
        int[] Ai = graph.colInd;
        int vb = graph.rowPtr[v], ve = graph.rowPtr[v + 1];
        int wb = graph.rowPtr[w], we = graph.rowPtr[w + 1];
        int levelV = level[v];
        long count = 0;

        int ptrV = vb, ptrW = wb;
        while (ptrV < ve && ptrW < we) {
            int vlist = Ai[ptrV], wlist = Ai[ptrW];
            if (vlist == wlist) {
                if (levelV != level[vlist])
                    count++;
                else if (vlist < v && vlist < w)
                    count++;
                ptrV++;
                ptrW++;
            } else if (vlist < wlist)
                ptrV++;
            else
                ptrW++;
        }
        return count;
    }

    // ---------------------------------------------------------------
    // Compute k (fraction of horizontal edges)
    // ---------------------------------------------------------------
    public static double baderComputeK(Graph graph) {
        int n = graph.numVertices;
        int[] level = new int[n];
        int NO_LEVEL = n;
        Arrays.fill(level, NO_LEVEL);

        for (int i = 0; i < n; i++)
            if (level[i] == NO_LEVEL)
                BFS.bfs(graph, i, level);

        int k = 0;
        for (int v = 0; v < n; v++) {
            int l = level[v];
            for (int j = graph.rowPtr[v]; j < graph.rowPtr[v + 1]; j++) {
                int w = graph.colInd[j];
                if (v < w && level[w] == l)
                    k++;
            }
        }

        return 2.0 * (double) k / (double) graph.numEdges;
    }

    // ---------------------------------------------------------------
    // Bader
    // ---------------------------------------------------------------
    public static long bader(Graph graph) {
        int n = graph.numVertices;
        int[] level = new int[n];
        int NO_LEVEL = n;
        Arrays.fill(level, NO_LEVEL);

        for (int i = 0; i < n; i++)
            if (level[i] == NO_LEVEL)
                BFS.bfs(graph, i, level);

        long c1 = 0, c2 = 0;
        for (int v = 0; v < n; v++) {
            int l = level[v];
            for (int j = graph.rowPtr[v]; j < graph.rowPtr[v + 1]; j++) {
                int w = graph.colInd[j];
                if (v < w && level[w] == l) {
                    long[] r = baderIntersectSizeMergePath(graph, level, v, w);
                    c1 += r[0];
                    c2 += r[1];
                }
            }
        }
        return c1 + c2 / 3;
    }

    // ---------------------------------------------------------------
    // Bader2 (single counter)
    // ---------------------------------------------------------------
    public static long bader2(Graph graph) {
        int n = graph.numVertices;
        int[] level = new int[n];
        int NO_LEVEL = n;
        Arrays.fill(level, NO_LEVEL);

        for (int i = 0; i < n; i++)
            if (level[i] == NO_LEVEL)
                BFS.bfs(graph, i, level);

        long count = 0;
        for (int v = 0; v < n; v++) {
            int l = level[v];
            for (int j = graph.rowPtr[v]; j < graph.rowPtr[v + 1]; j++) {
                int w = graph.colInd[j];
                if (v < w && level[w] == l)
                    count += bader2IntersectSizeMergePath(graph, level, v, w);
            }
        }
        return count;
    }

    // ---------------------------------------------------------------
    // Bader3 (hash-based, BFS level)
    // ---------------------------------------------------------------
    public static long bader3(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;

        int[] level = new int[n];
        boolean[] visited = new boolean[n];
        boolean[] hash = new boolean[n];
        Queue queue = new Queue(n);

        long c1 = 0, c2 = 0;
        for (int v = 0; v < n; v++) {
            if (level[v] == 0)
                bfsBader(graph, v, level, queue, visited);

            int s = Ap[v], e = Ap[v + 1];
            int l = level[v];

            for (int p = s; p < e; p++)
                hash[Ai[p]] = true;

            for (int j = s; j < e; j++) {
                int w = Ai[j];
                if (v < w && level[w] == l) {
                    for (int k = Ap[w]; k < Ap[w + 1]; k++) {
                        int x = Ai[k];
                        if (hash[x]) {
                            if (level[x] != l) c1++;
                            else c2++;
                        }
                    }
                }
            }

            for (int p = s; p < e; p++)
                hash[Ai[p]] = false;
        }
        return c1 + c2 / 3;
    }

    // ---------------------------------------------------------------
    // Bader4 (hash + horizontal edge marking)
    // ---------------------------------------------------------------
    public static long bader4(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        int m = graph.numEdges;

        int[] level = new int[n];
        boolean[] visited = new boolean[n];
        boolean[] hash = new boolean[n];
        boolean[] horiz = new boolean[m];
        Queue queue = new Queue(n);

        long c1 = 0, c2 = 0;
        for (int v = 0; v < n; v++) {
            if (level[v] == 0)
                BFS.bfsMarkHorizontalEdges(graph, v, level, queue, visited, horiz);

            int s = Ap[v], e = Ap[v + 1];
            int l = level[v];

            for (int p = s; p < e; p++)
                hash[Ai[p]] = true;

            for (int j = s; j < e; j++) {
                if (horiz[j]) {
                    int w = Ai[j];
                    if (v < w) {
                        for (int k = Ap[w]; k < Ap[w + 1]; k++) {
                            int x = Ai[k];
                            if (hash[x]) {
                                if (level[x] != l) c1++;
                                else c2++;
                            }
                        }
                    }
                }
            }

            for (int p = s; p < e; p++)
                hash[Ai[p]] = false;
        }
        return c1 + c2 / 3;
    }

    // ---------------------------------------------------------------
    // Bader4 — Degree Order
    // ---------------------------------------------------------------
    public static long bader4DegreeOrder(Graph graph) {
        Graph graph2 = graph.reorderByDegree(Graph.REORDER_HIGHEST_DEGREE_FIRST);
        return bader4(graph2);
    }

    // ---------------------------------------------------------------
    // Bader5 (single counter with v < w < x)
    // ---------------------------------------------------------------
    public static long bader5(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        int m = graph.numEdges;

        int[] level = new int[n];
        boolean[] visited = new boolean[n];
        boolean[] hash = new boolean[n];
        boolean[] horiz = new boolean[m];
        Queue queue = new Queue(n);

        long count = 0;
        for (int v = 0; v < n; v++) {
            if (level[v] == 0)
                BFS.bfsMarkHorizontalEdges(graph, v, level, queue, visited, horiz);

            int s = Ap[v], e = Ap[v + 1];
            int l = level[v];

            for (int j = s; j < e; j++)
                hash[Ai[j]] = true;

            for (int j = s; j < e; j++) {
                if (horiz[j]) {
                    int w = Ai[j];
                    if (v < w) {
                        for (int k = Ap[w]; k < Ap[w + 1]; k++) {
                            int x = Ai[k];
                            if (hash[x]) {
                                if (l != level[x] || (l == level[x] && w < x))
                                    count++;
                            }
                        }
                    }
                }
            }

            for (int j = s; j < e; j++)
                hash[Ai[j]] = false;
        }
        return count;
    }

    // ---------------------------------------------------------------
    // Bader Forward Hash (partition into horizontal + non-horizontal)
    // ---------------------------------------------------------------
    public static long baderForwardHash(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        int m = graph.numEdges;

        int[] level = new int[n];
        boolean[] visited = new boolean[n];
        boolean[] hash = new boolean[n];
        boolean[] horiz = new boolean[m];
        Queue queue = new Queue(n);

        for (int v = 0; v < n; v++)
            if (level[v] == 0)
                BFS.bfsMarkHorizontalEdges(graph, v, level, queue, visited, horiz);

        // Build graph0 (horizontal edges) and graph1 (non-horizontal edges)
        Graph graph0 = new Graph(n, m);
        Graph graph1 = new Graph(n, m);

        int edgeCountG0 = 0, edgeCountG1 = 0;
        graph0.rowPtr[0] = 0;
        graph1.rowPtr[0] = 0;

        for (int v = 0; v < n; v++) {
            int lv = level[v];
            for (int j = Ap[v]; j < Ap[v + 1]; j++) {
                int w = Ai[j];
                if (lv == level[w]) {
                    graph0.colInd[edgeCountG0++] = w;
                } else {
                    graph1.colInd[edgeCountG1++] = w;
                }
            }
            graph0.rowPtr[v + 1] = edgeCountG0;
            graph1.rowPtr[v + 1] = edgeCountG1;
        }
        graph0.numEdges = edgeCountG0;
        graph1.numEdges = edgeCountG1;

        long count = forwardHashConfigSize(graph0, m);

        // Cross-count: horizontal edge (v,w) with non-horizontal neighbors
        for (int v = 0; v < n; v++) {
            int s1 = graph1.rowPtr[v], e1 = graph1.rowPtr[v + 1];
            if (s1 < e1) {
                for (int j = s1; j < e1; j++)
                    hash[graph1.colInd[j]] = true;

                int s0 = graph0.rowPtr[v], e0 = graph0.rowPtr[v + 1];
                for (int j = s0; j < e0; j++) {
                    int w = graph0.colInd[j];
                    if (v < w) {
                        for (int k = graph1.rowPtr[w]; k < graph1.rowPtr[w + 1]; k++)
                            if (hash[graph1.colInd[k]])
                                count++;
                    }
                }

                for (int j = s1; j < e1; j++)
                    hash[graph1.colInd[j]] = false;
            }
        }
        return count;
    }

    // ---------------------------------------------------------------
    // Bader Forward Hash — Degree Order
    // ---------------------------------------------------------------
    public static long baderForwardHashDegreeOrder(Graph graph) {
        Graph graph2 = graph.reorderByDegree(Graph.REORDER_HIGHEST_DEGREE_FIRST);
        return baderForwardHash(graph2);
    }

    // ---------------------------------------------------------------
    // Bader Recursive
    // ---------------------------------------------------------------
    public static long baderRecursive(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        int m = graph.numEdges;

        int[] level = new int[n];
        boolean[] visited = new boolean[n];
        boolean[] hash = new boolean[n];
        boolean[] hash2 = new boolean[n];
        boolean[] horiz = new boolean[m];
        Queue queue = new Queue(n);

        for (int v = 0; v < n; v++)
            if (level[v] == 0)
                BFS.bfsMarkHorizontalEdges(graph, v, level, queue, visited, horiz);

        // Build graph0 (horizontal) and graph1 (non-horizontal)
        Graph graph0 = new Graph(n, m);
        Graph graph1 = new Graph(n, m);

        int edgeCountG0 = 0, edgeCountG1 = 0;
        graph0.rowPtr[0] = 0;
        graph1.rowPtr[0] = 0;

        for (int v = 0; v < n; v++) {
            int lv = level[v];
            for (int j = Ap[v]; j < Ap[v + 1]; j++) {
                int w = Ai[j];
                if (lv == level[w]) {
                    graph0.colInd[edgeCountG0++] = w;
                    hash2[v] = true;
                    hash2[w] = true;
                } else {
                    graph1.colInd[edgeCountG1++] = w;
                }
            }
            graph0.rowPtr[v + 1] = edgeCountG0;
            graph1.rowPtr[v + 1] = edgeCountG1;
        }
        graph0.numEdges = edgeCountG0;
        graph1.numEdges = edgeCountG1;

        // Cross-count
        long count = 0;
        for (int v = 0; v < n; v++) {
            int s1 = graph1.rowPtr[v], e1 = graph1.rowPtr[v + 1];
            if (s1 < e1) {
                for (int j = s1; j < e1; j++)
                    hash[graph1.colInd[j]] = true;

                int s0 = graph0.rowPtr[v], e0 = graph0.rowPtr[v + 1];
                for (int j = s0; j < e0; j++) {
                    int w = graph0.colInd[j];
                    if (v < w) {
                        for (int k = graph1.rowPtr[w]; k < graph1.rowPtr[w + 1]; k++)
                            if (hash[graph1.colInd[k]])
                                count++;
                    }
                }

                for (int j = s1; j < e1; j++)
                    hash[graph1.colInd[j]] = false;
            }
        }

        // Recurse or base case on horizontal graph
        if (edgeCountG0 < BADER_RECURSIVE_BASE) {
            count += forwardHashConfigSize(graph0, m);
        } else {
            // Compact graph0 to only vertices that have horizontal edges
            int[] vlist = new int[n];
            int vn = 0;
            for (int v = 0; v < n; v++)
                if (hash2[v])
                    vlist[v] = vn++;

            Graph graphR0 = new Graph();
            graphR0.numVertices = vn;
            graphR0.numEdges = edgeCountG0;
            graphR0.allocate();

            for (int e = 0; e < edgeCountG0; e++)
                graphR0.colInd[e] = vlist[graph0.colInd[e]];

            for (int v = 0; v < n; v++)
                if (hash2[v])
                    graphR0.rowPtr[vlist[v]] = graph0.rowPtr[v];
            graphR0.rowPtr[vn] = edgeCountG0;

            count += baderRecursive(graphR0);
        }

        return count;
    }

    // ---------------------------------------------------------------
    // Bader Level (helper for baderHybrid)
    // ---------------------------------------------------------------
    private static long baderLevel(Graph graph, int[] level) {
        int n = graph.numVertices;
        long c1 = 0, c2 = 0;

        for (int v = 0; v < n; v++) {
            int l = level[v];
            for (int j = graph.rowPtr[v]; j < graph.rowPtr[v + 1]; j++) {
                int w = graph.colInd[j];
                if (v < w && level[w] == l) {
                    long[] r = baderIntersectSizeMergePath(graph, level, v, w);
                    c1 += r[0];
                    c2 += r[1];
                }
            }
        }
        return c1 + c2 / 3;
    }

    // ---------------------------------------------------------------
    // Bader Hybrid
    // ---------------------------------------------------------------
    public static long baderHybrid(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        int m = graph.numEdges;

        int[] level = new int[n];
        boolean[] visited = new boolean[n];
        boolean[] horiz = new boolean[m];
        Queue queue = new Queue(n);

        for (int v = 0; v < n; v++)
            if (level[v] == 0)
                BFS.bfsMarkHorizontalEdges(graph, v, level, queue, visited, horiz);

        int k = 0;
        for (int v = 0; v < n; v++) {
            int l = level[v];
            for (int j = Ap[v]; j < Ap[v + 1]; j++) {
                int w = Ai[j];
                if (v < w && level[w] == l)
                    k++;
            }
        }

        double pk = 2.0 * (double) k / (double) m;
        if (m < BADER_RECURSIVE_BASE || pk > 0.7)
            return forwardHash(graph);
        else
            return baderLevel(graph, level);
    }

    // ---------------------------------------------------------------
    // Bader New BFS (integrated TC + BFS)
    // ---------------------------------------------------------------
    public static long baderNewBfs(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;

        int[] level = new int[n];
        boolean[] visited = new boolean[n];
        boolean[] hash = new boolean[n];
        Queue queue = new Queue(n);

        long c1 = 0, c2 = 0;
        for (int x = 0; x < n; x++) {
            if (!visited[x]) {
                visited[x] = true;
                queue.enqueue(x);
                level[x] = 1;

                while (!queue.isEmpty()) {
                    int v = queue.dequeue();
                    int lv = level[v];
                    int s = Ap[v], e = Ap[v + 1];
                    int dv = e - s;

                    for (int p = s; p < e; p++)
                        hash[Ai[p]] = true;

                    for (int j = s; j < e; j++) {
                        int w = Ai[j];
                        if (!visited[w]) {
                            visited[w] = true;
                            queue.enqueue(w);
                            level[w] = lv + 1;
                        } else {
                            int sw = Ap[w], ew = Ap[w + 1];
                            int dw = ew - sw;

                            if ((dv > dw || (dv == dw && v < w)) && level[w] == lv) {
                                for (int k = sw; k < ew; k++) {
                                    int y = Ai[k];
                                    if (hash[y]) {
                                        if (level[y] != lv) c1++;
                                        else c2++;
                                    }
                                }
                            }
                        }
                    }

                    for (int p = s; p < e; p++)
                        hash[Ai[p]] = false;
                }
            }
        }
        return c1 + c2 / 3;
    }

    // ---------------------------------------------------------------
    // Treelist (Itai & Rodeh 1978)
    // ---------------------------------------------------------------
    private static void bfsTreelist(Graph graph, boolean[] E, int[] parent) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        boolean[] visited = new boolean[n];

        for (int v = 0; v < n; v++) {
            if (!visited[v]) {
                Queue queue = new Queue(n);
                visited[v] = true;
                queue.enqueue(v);

                while (!queue.isEmpty()) {
                    int curr = queue.dequeue();
                    // Note: C code uses Ap[v] here (bug in original), faithfully replicated
                    int s = Ap[v], e = Ap[v + 1];
                    for (int i = s; i < e; i++) {
                        if (E[i]) {
                            int adj = Ai[i];
                            if (!visited[adj]) {
                                visited[adj] = true;
                                parent[adj] = curr;
                                queue.enqueue(adj);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean andE(boolean[] E, int m) {
        for (int i = 0; i < m; i++)
            if (E[i]) return true;
        return false;
    }

    private static boolean checkEdgeTreelist(Graph graph, boolean[] E, int v, int w) {
        int n = graph.numVertices;
        if (v == n || w == n) return false;
        for (int i = graph.rowPtr[v]; i < graph.rowPtr[v + 1]; i++)
            if (E[i] && graph.colInd[i] == w)
                return true;
        return false;
    }

    private static void removeTreelist(Graph graph, boolean[] E, int[] parent) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;

        for (int v = 0; v < n; v++) {
            for (int i = Ap[v]; i < Ap[v + 1]; i++) {
                if (E[i]) {
                    int w = Ai[i];
                    if (parent[w] == v) {
                        E[i] = false;
                        for (int j = Ap[w]; j < Ap[w + 1]; j++) {
                            if (Ai[j] == v) {
                                E[j] = false;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public static long treelist(Graph graph) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        int m = graph.numEdges;
        long count = 0;

        boolean[] E = new boolean[m];
        Arrays.fill(E, true);
        int[] parent = new int[n];

        while (andE(E, m)) {
            Arrays.fill(parent, n);
            bfsTreelist(graph, E, parent);

            for (int u = 0; u < n; u++) {
                for (int j = Ap[u]; j < Ap[u + 1]; j++) {
                    if (E[j]) {
                        int v = Ai[j];
                        if (parent[u] != v) {
                            if (checkEdgeTreelist(graph, E, parent[u], v))
                                count++;
                            else if (checkEdgeTreelist(graph, E, parent[v], u))
                                count++;
                        }
                    }
                }
            }
            removeTreelist(graph, E, parent);
        }
        return count / 2;
    }

    // ---------------------------------------------------------------
    // Treelist2 (Itai & Rodeh, removes tree edges from graph)
    // ---------------------------------------------------------------
    private static void bfsTreelist2(Graph graph, int[] parent) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        boolean[] visited = new boolean[n];

        for (int v = 0; v < n; v++) {
            if (!visited[v]) {
                Queue queue = new Queue(n);
                visited[v] = true;
                queue.enqueue(v);

                while (!queue.isEmpty()) {
                    int curr = queue.dequeue();
                    // Note: C code uses Ap[v] here, faithfully replicated
                    int s = Ap[v], e = Ap[v + 1];
                    for (int i = s; i < e; i++) {
                        int adj = Ai[i];
                        if (!visited[adj]) {
                            visited[adj] = true;
                            parent[adj] = curr;
                            queue.enqueue(adj);
                        }
                    }
                }
            }
        }
    }

    private static void removeTreelist2(Graph graph, int[] parent) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int n = graph.numVertices;
        int m = graph.numEdges;

        boolean[] E = new boolean[m];
        Arrays.fill(E, true);

        for (int v = 0; v < n; v++) {
            for (int i = Ap[v]; i < Ap[v + 1]; i++) {
                int w = Ai[i];
                if (parent[w] == v) {
                    E[i] = false;
                    for (int j = Ap[w]; j < Ap[w + 1]; j++) {
                        if (Ai[j] == v) {
                            E[j] = false;
                            break;
                        }
                    }
                }
            }
        }

        // Compact the graph
        int numEdgesNew = 0;
        for (int i = 0; i < m; i++)
            if (E[i])
                Ai[numEdgesNew++] = Ai[i];

        int[] degree = new int[n];
        // Recount: we need original E and Ap to compute per-vertex degree
        // Re-derive from the new packed Ai + original Ap
        int idx = 0;
        for (int v = 0; v < n; v++) {
            int s = Ap[v], e = Ap[v + 1];
            for (int i = s; i < e; i++)
                if (E[i])
                    degree[v]++;
        }

        Ap[0] = 0;
        for (int i = 1; i <= n; i++)
            Ap[i] = Ap[i - 1] + degree[i - 1];

        graph.numEdges = numEdgesNew;
    }

    public static long treelist2(Graph graph) {
        int n = graph.numVertices;
        int m = graph.numEdges;
        long count = 0;

        // Work on a copy so we don't destroy the input
        Graph graph2 = new Graph(n, m);
        graph2.copyFrom(graph);

        int[] parent = new int[n];

        int edges = m;
        while (edges > 0) {
            Arrays.fill(parent, n);
            bfsTreelist2(graph2, parent);

            for (int u = 0; u < n; u++) {
                for (int j = graph2.rowPtr[u]; j < graph2.rowPtr[u + 1]; j++) {
                    int v = graph2.colInd[j];
                    if (parent[u] != v) {
                        if (parent[u] < n && graph2.checkEdge(parent[u], v))
                            count++;
                        else if (parent[v] < n && graph2.checkEdge(parent[v], u))
                            count++;
                    }
                }
            }

            removeTreelist2(graph2, parent);
            edges = graph2.numEdges;
        }
        return count / 2;
    }

    // ---------------------------------------------------------------
    // Placeholder for optimized algorithm
    // ---------------------------------------------------------------
    public static long fast(Graph graph) {
        // TODO: replace with your optimized implementation
        return forwardHash(graph);
    }
}
