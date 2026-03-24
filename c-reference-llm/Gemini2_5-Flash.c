UINT_t tc_fast(const GRAPH_TYPE *graph) {
  register UINT_t s, t, x;
  register UINT_t b, e;
  UINT_t count = 0;
  const UINT_t* restrict Ap = graph->rowPtr;
  const UINT_t* restrict Ai = graph->colInd;
  const UINT_t n = graph->numVertices;

  bool* Hash = (bool *)calloc(n, sizeof(bool));
  assert_malloc(Hash);

  UINT_t* Size = (UINT_t *)calloc(n, sizeof(UINT_t));
  assert_malloc(Size);
  
  UINT_t* A = (UINT_t *)calloc(graph->numEdges, sizeof(UINT_t));
  assert_malloc(A);

  for (s = 0; s < n ; s++) {
    b = Ap[s  ];
    e = Ap[s+1];
    for (UINT_t i=b ; i<e ; i++) {
      t  = Ai[i];
      if (s<t) {
        
        UINT_t s1 = Ap[s];
        UINT_t e1 = Ap[s+1];
        UINT_t s2 = Ap[t];
        UINT_t e2 = Ap[t+1];
        
        if ((e1-s1) < (e2-s2)) {
          for (UINT_t i1 = s1; i1 < e1; i1++)
            Hash[Ai[i1]] = true;
          for (UINT_t i2 = s2; i2 < e2; i2++)
            if (Hash[Ai[i2]]) count++;
          for (UINT_t i1 = s1; i1 < e1; i1++)
            Hash[Ai[i1]] = false;
        } else {
          for (UINT_t i2 = s2; i2 < e2; i2++)
            Hash[Ai[i2]] = true;
          for (UINT_t i1 = s1; i1 < e1; i1++)
            if (Hash[Ai[i1]]) count++;
          for (UINT_t i2 = s2; i2 < e2; i2++)
            Hash[Ai[i2]] = false;
        }
        
	      A[Ap[t] + Size[t]] = s;
	      Size[t]++;
      }
    }
  }

  free(A);
  free(Size);
  free(Hash);
  
  return count;
}
