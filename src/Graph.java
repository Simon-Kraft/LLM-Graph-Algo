import java.io.*;
import java.util.*;

public class Graph {

    public int numVertices;
    public int numEdges;
    public int[] rowPtr;
    public int[] colInd;

    public Graph() {}

    public Graph(int numVertices, int numEdges) {
        this.numVertices = numVertices;
        this.numEdges = numEdges;
        allocate();
    }

    public void allocate() {
        rowPtr = new int[numVertices + 1];
        colInd = new int[numEdges];
    }

    public void copyFrom(Graph src) {
        this.numVertices = src.numVertices;
        this.numEdges = src.numEdges;
        System.arraycopy(src.rowPtr, 0, this.rowPtr, 0, src.numVertices + 1);
        System.arraycopy(src.colInd, 0, this.colInd, 0, src.numEdges);
    }

    public static Graph allocateRMAT(int scale, int edgeFactor) {
        Graph g = new Graph();
        g.numVertices = 1 << scale;
        g.numEdges = 2 * g.numVertices * edgeFactor;
        g.allocate();
        return g;
    }

    public static Graph createRMAT(int scale, int edgeFactor) {
        Graph graph = allocateRMAT(scale, edgeFactor);
        Random rand = new Random();

        int[][] edges = new int[graph.numEdges][2]; // [i][0]=src, [i][1]=dst

        for (int e = 0; e < graph.numEdges; e += 2) {
            boolean good = false;
            int src = 0, dst = 0;

            while (!good) {
                src = 0;
                dst = 0;

                for (int level = 0; level < scale; level++) {
                    double r = rand.nextDouble();
                    double a = 0.57, b = 0.19, c = 0.19;

                    if (r < a)
                        continue;
                    else if (r < a + b)
                        dst |= 1 << level;
                    else if (r < a + b + c)
                        src |= 1 << level;
                    else {
                        src |= 1 << level;
                        dst |= 1 << level;
                    }
                }

                good = true;

                // Only keep unique edges
                for (int i = 0; i < e; i++)
                    if (edges[i][0] == src && edges[i][1] == dst) good = false;
                // Do not keep self-loops
                if (src == dst) good = false;
            }

            edges[e][0] = src;
            edges[e][1] = dst;
            edges[e + 1][0] = dst;
            edges[e + 1][1] = src;
        }

        convertEdgesToGraph(edges, graph);
        return graph;
    }

    public static void convertEdgesToGraph(int[][] edges, Graph graph) {
        int n = graph.numVertices;
        int m = graph.numEdges;
        int[] Ap = graph.rowPtr;
        int[] Ai = graph.colInd;

        Arrays.fill(Ap, 0);

        // Count edges per vertex
        for (int i = 0; i < m; i++) {
            int vertex = edges[i][0];
            Ap[vertex + 1]++;
        }

        // Prefix sum
        for (int i = 1; i <= n; i++) {
            Ap[i] += Ap[i - 1];
        }

        // Populate colInd
        int[] currentRow = new int[n];
        for (int i = 0; i < m; i++) {
            int srcVertex = edges[i][0];
            int dstVertex = edges[i][1];
            int index = Ap[srcVertex] + currentRow[srcVertex];
            Ai[index] = dstVertex;
            currentRow[srcVertex]++;
        }

        // Sort column indices within each row
        for (int i = 0; i < n; i++) {
            int s = Ap[i];
            int e = Ap[i + 1];
            Arrays.sort(Ai, s, e);
        }
    }

    public void print(PrintStream out) {
        out.println("Number of Vertices: " + numVertices);
        out.println("Number of Edges: " + numEdges);
        StringBuilder sb = new StringBuilder("RowPtr: ");
        for (int i = 0; i <= numVertices; i++)
            sb.append(rowPtr[i]).append(' ');
        out.println(sb.toString());
        sb = new StringBuilder("ColInd: ");
        for (int i = 0; i < numEdges; i++)
            sb.append(colInd[i]).append(' ');
        out.println(sb.toString());
    }

    public boolean checkEdge(int v, int w) {
        int s = rowPtr[v];
        int e = rowPtr[v + 1];
        for (int i = s; i < e; i++)
            if (colInd[i] == w)
                return true;
        return false;
    }

    // --- Intersection methods ---

    public int intersectSizeMergePath(int v, int w) {
        int vb = rowPtr[v], ve = rowPtr[v + 1];
        int wb = rowPtr[w], we = rowPtr[w + 1];
        int ptrV = vb, ptrW = wb;
        int count = 0;

        while (ptrV < ve && ptrW < we) {
            if (colInd[ptrV] == colInd[ptrW]) {
                count++;
                ptrV++;
                ptrW++;
            } else if (colInd[ptrV] < colInd[ptrW])
                ptrV++;
            else
                ptrW++;
        }
        return count;
    }

    private static int binarySearch(int[] list, int start, int end, int target) {
        int s = start, e = end;
        while (s < e) {
            int mid = s + (e - s) / 2;
            if (list[mid] == target)
                return mid;
            if (list[mid] < target)
                s = mid + 1;
            else
                e = mid;
        }
        return -1;
    }

    public int intersectSizeBinarySearch(int v, int w) {
        int vb = rowPtr[v], ve = rowPtr[v + 1];
        int wb = rowPtr[w], we = rowPtr[w + 1];
        int count = 0;

        int sizeV = ve - vb;
        int sizeW = we - wb;

        if (sizeV <= sizeW) {
            for (int i = vb; i < ve; i++)
                if (binarySearch(colInd, wb, we, colInd[i]) >= 0) count++;
        } else {
            for (int i = wb; i < we; i++)
                if (binarySearch(colInd, vb, ve, colInd[i]) >= 0) count++;
        }
        return count;
    }

    private static int binarySearchPartition(int[] list, int start, int end, int target) {
        int s = start, e = end;
        while (s < e) {
            int mid = s + (e - s) / 2;
            if (list[mid] == target)
                return mid;
            if (list[mid] < target)
                s = mid + 1;
            else
                e = mid;
        }
        return s;
    }

    public static int searchListsWithPartitioning(int[] list1, int s1, int e1, int[] list2, int s2, int e2) {
        if (s1 > e1 || s2 > e2)
            return 0;

        int mid1 = s1 + (e1 - s1) / 2;
        int loc2 = binarySearchPartition(list2, s2, e2, list1[mid1]);

        int s11 = s1, e11 = mid1 - 1;
        int s21 = s2, e21 = loc2;
        int s12 = mid1 + 1, e12 = e1;
        int s22 = loc2, e22 = e2;

        int count = 0;
        if (list1[mid1] == list2[loc2]) {
            count++;
            e21--;
            s22++;
        }
        count += searchListsWithPartitioning(list1, s11, e11, list2, s21, e21);
        count += searchListsWithPartitioning(list1, s12, e12, list2, s22, e22);
        return count;
    }

    public int intersectSizeHash(boolean[] hash, int v, int w) {
        int vb = rowPtr[v], ve = rowPtr[v + 1];
        int wb = rowPtr[w], we = rowPtr[w + 1];
        int s1, e1, s2, e2;

        if ((ve - vb) < (we - wb)) {
            s1 = vb; e1 = ve; s2 = wb; e2 = we;
        } else {
            s1 = wb; e1 = we; s2 = vb; e2 = ve;
        }

        int count = 0;
        for (int i = s1; i < e1; i++)
            hash[colInd[i]] = true;
        for (int i = s2; i < e2; i++)
            if (hash[colInd[i]]) count++;
        for (int i = s1; i < e1; i++)
            hash[colInd[i]] = false;

        return count;
    }

    public int intersectSizeMergePathForward(int v, int w, int[] A, int[] size) {
        int vb = rowPtr[v], ve = vb + size[v];
        int wb = rowPtr[w], we = wb + size[w];
        int ptrV = vb, ptrW = wb;
        int count = 0;

        while (ptrV < ve && ptrW < we) {
            if (A[ptrV] == A[ptrW]) {
                count++;
                ptrV++;
                ptrW++;
            } else if (A[ptrV] < A[ptrW])
                ptrV++;
            else
                ptrW++;
        }
        return count;
    }

    public int intersectSizeHashForward(boolean[] hash, int v, int w, int[] A, int[] size) {
        int vb = rowPtr[v], ve = vb + size[v];
        int wb = rowPtr[w], we = wb + size[w];
        int s1, e1, s2, e2;

        if (size[v] < size[w]) {
            s1 = vb; e1 = ve; s2 = wb; e2 = we;
        } else {
            s1 = wb; e1 = we; s2 = vb; e2 = ve;
        }

        int count = 0;
        for (int i = s1; i < e1; i++)
            hash[A[i]] = true;
        for (int i = s2; i < e2; i++)
            if (hash[A[i]]) count++;
        for (int i = s1; i < e1; i++)
            hash[A[i]] = false;

        return count;
    }

    public int intersectSizeHashSkipForward(boolean[] hash, int v, int w, int[] A, int[] size) {
        int s1, e1, s2, e2;

        if (size[v] < size[w]) {
            if (size[v] == 0) return 0;
            s1 = rowPtr[v]; e1 = s1 + size[v];
            s2 = rowPtr[w]; e2 = s2 + size[w];
        } else {
            if (size[w] == 0) return 0;
            s1 = rowPtr[w]; e1 = s1 + size[w];
            s2 = rowPtr[v]; e2 = s2 + size[v];
        }

        int count = 0;
        for (int i = s1; i < e1; i++)
            hash[A[i]] = true;
        for (int i = s2; i < e2; i++)
            if (hash[A[i]]) count++;
        for (int i = s1; i < e1; i++)
            hash[A[i]] = false;

        return count;
    }

    // --- Degree-based reordering ---

    public static final int REORDER_HIGHEST_DEGREE_FIRST = 0;
    public static final int REORDER_LOWEST_DEGREE_FIRST = 1;

    public Graph reorderByDegree(int reorderDegree) {
        int n = numVertices;
        int m = numEdges;

        int[][] perm = new int[n][2]; // [i][0]=degree, [i][1]=index
        for (int i = 0; i < n; i++) {
            perm[i][0] = rowPtr[i + 1] - rowPtr[i];
            perm[i][1] = i;
        }

        if (reorderDegree == REORDER_HIGHEST_DEGREE_FIRST) {
            Arrays.sort(perm, (a, b) -> {
                if (a[0] != b[0]) return Integer.compare(b[0], a[0]);
                return Integer.compare(a[1], b[1]);
            });
        } else {
            Arrays.sort(perm, (a, b) -> {
                if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
                return Integer.compare(a[1], b[1]);
            });
        }

        Graph graph2 = new Graph();
        graph2.numVertices = n;
        graph2.numEdges = m;
        graph2.allocate();

        graph2.rowPtr[0] = 0;
        for (int i = 1; i <= n; i++)
            graph2.rowPtr[i] = graph2.rowPtr[i - 1] + perm[i - 1][0];

        int[] reverse = new int[n];
        for (int i = 0; i < n; i++)
            reverse[perm[i][1]] = i;

        for (int s = 0; s < n; s++) {
            int ps = perm[s][1];
            int b = rowPtr[ps];
            int e = rowPtr[ps + 1];
            int d = 0;
            for (int i = b; i < e; i++) {
                graph2.colInd[graph2.rowPtr[s] + d] = reverse[colInd[i]];
                d++;
            }
        }

        return graph2;
    }

    // --- Matrix Market file reader ---

    public static Graph readMatrixMarketFile(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;

        // Skip header/comment lines
        do {
            line = reader.readLine();
        } while (line != null && line.startsWith("%"));

        if (line == null) {
            reader.close();
            throw new IOException("Invalid Matrix Market file");
        }

        String[] parts = line.trim().split("\\s+");
        int numRows = Integer.parseInt(parts[0]);
        int numCols = Integer.parseInt(parts[1]);
        int numEntries = Integer.parseInt(parts[2]);

        if (numRows != numCols) {
            reader.close();
            throw new IOException("Matrix Market input file is not square");
        }

        int edgeCount = 0;
        int[][] edges = new int[2 * numEntries][2];

        for (int i = 0; i < numEntries; i++) {
            line = reader.readLine();
            if (line == null) {
                reader.close();
                throw new IOException("Invalid Matrix Market file: unexpected end");
            }
            parts = line.trim().split("\\s+");
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);

            edges[edgeCount][0] = row - 1;
            edges[edgeCount][1] = col - 1;
            edgeCount++;
            edges[edgeCount][0] = col - 1;
            edges[edgeCount][1] = row - 1;
            edgeCount++;
        }
        reader.close();

        // Sort edges
        Arrays.sort(edges, 0, edgeCount, (a, b) -> {
            if (a[0] != b[0]) return Integer.compare(a[0], b[0]);
            return Integer.compare(a[1], b[1]);
        });

        // Remove duplicates
        int[][] edgesNoDup = new int[edgeCount][2];
        edgesNoDup[0][0] = edges[0][0];
        edgesNoDup[0][1] = edges[0][1];
        int edgeCountNoDup = 1;
        for (int i = 1; i < edgeCount; i++) {
            if (edges[i][0] != edges[i - 1][0] || edges[i][1] != edges[i - 1][1]) {
                edgesNoDup[edgeCountNoDup][0] = edges[i][0];
                edgesNoDup[edgeCountNoDup][1] = edges[i][1];
                edgeCountNoDup++;
            }
        }

        Graph graph = new Graph();
        graph.numVertices = numRows;
        graph.numEdges = edgeCountNoDup;
        graph.allocate();

        int[][] finalEdges = Arrays.copyOf(edgesNoDup, edgeCountNoDup);
        convertEdgesToGraph(finalEdges, graph);

        return graph;
    }
}
