import java.util.Arrays;

public class BFS {

    /**
     * Standard BFS from startVertex, writing distances into level[].
     * Allocates its own visited array.
     */
    public static void bfs(Graph graph, int startVertex, int[] level) {
        boolean[] visited = new boolean[graph.numVertices];
        Queue queue = new Queue(graph.numVertices);

        visited[startVertex] = true;
        queue.enqueue(startVertex);
        level[startVertex] = 0;

        while (!queue.isEmpty()) {
            int v = queue.dequeue();
            for (int i = graph.rowPtr[v]; i < graph.rowPtr[v + 1]; i++) {
                int w = graph.colInd[i];
                if (!visited[w]) {
                    visited[w] = true;
                    queue.enqueue(w);
                    level[w] = level[v] + 1;
                }
            }
        }
    }

    /**
     * BFS from startVertex using a caller-provided visited array.
     */
    public static void bfsVisited(Graph graph, int startVertex, int[] level, boolean[] visited) {
        Queue queue = new Queue(graph.numVertices);

        visited[startVertex] = true;
        queue.enqueue(startVertex);
        level[startVertex] = 0;

        while (!queue.isEmpty()) {
            int v = queue.dequeue();
            for (int i = graph.rowPtr[v]; i < graph.rowPtr[v + 1]; i++) {
                int w = graph.colInd[i];
                if (!visited[w]) {
                    visited[w] = true;
                    queue.enqueue(w);
                    level[w] = level[v] + 1;
                }
            }
        }
    }

    /**
     * BFS that marks horizontal edges (same-level edges).
     */
    public static void bfsMarkHorizontalEdges(Graph graph, int startVertex, int[] level,
                                               Queue queue, boolean[] visited, boolean[] horiz) {
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
                    horiz[i] = false;
                    visited[w] = true;
                    queue.enqueue(w);
                    level[w] = level[v] + 1;
                } else {
                    horiz[i] = (level[w] == 0) || (level[w] == level[v]);
                }
            }
        }
    }

    // --- Beamer-style direction-optimizing BFS ---

    private static final double ALPHA = 14.0;
    private static final double BETA = 24.0;

    private static int topDownStep(int[] frontier, int[] next, boolean[] visited,
                                    Graph graph, int frontierSize, int[] level) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int nextSize = 0;

        for (int i = 0; i < frontierSize; i++) {
            int v = frontier[i];
            for (int j = Ap[v]; j < Ap[v + 1]; j++) {
                int w = Ai[j];
                if (!visited[w]) {
                    visited[w] = true;
                    level[w] = level[v] + 1;
                    next[nextSize++] = w;
                }
            }
        }
        return nextSize;
    }

    private static int bottomUpStep(int[] frontier, int[] next, boolean[] visited,
                                     Graph graph, int frontierSize, int[] level) {
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;
        int nextSize = 0;

        for (int i = 0; i < frontierSize; i++) {
            int v = frontier[i];
            if (!visited[v]) {
                for (int j = Ap[v]; j < Ap[v + 1]; j++) {
                    int w = Ai[j];
                    if (w == frontier[j]) {
                        visited[v] = true;
                        level[v] = level[w] + 1;
                        next[nextSize++] = v;
                        break;
                    }
                }
            }
        }
        return nextSize;
    }

    /**
     * Hybrid (direction-optimizing) BFS from startVertex.
     */
    public static void bfsHybridVisited(Graph graph, int startVertex, int[] level, boolean[] visited) {
        int n = graph.numVertices;
        int[] Ap = graph.rowPtr;

        int[] frontier = new int[n];
        int[] next = new int[n];

        int frontierSize = 0;

        frontier[frontierSize++] = startVertex;
        level[startVertex] = 0;

        while (frontierSize > 0) {
            int numEdgesFrontier = 0;
            for (int i = 0; i < frontierSize; i++) {
                int v = frontier[i];
                numEdgesFrontier += Ap[v + 1] - Ap[v];
            }

            int numEdgesUnexplored = 0;
            for (int v = 0; v < n; v++) {
                if (!visited[v]) {
                    numEdgesUnexplored += Ap[v + 1] - Ap[v];
                }
            }

            int nextSize;
            if (numEdgesFrontier > numEdgesUnexplored / ALPHA) {
                nextSize = bottomUpStep(frontier, next, visited, graph, frontierSize, level);
            } else {
                nextSize = topDownStep(frontier, next, visited, graph, frontierSize, level);
            }

            // Swap frontier and next
            int[] temp = frontier;
            frontier = next;
            next = temp;
            frontierSize = nextSize;

            if (frontierSize <= n / BETA) {
                int nextSize2 = topDownStep(frontier, next, visited, graph, frontierSize, level);
                // Note: matches C code structure (result not swapped back in same iteration)
            }
        }
    }
}
