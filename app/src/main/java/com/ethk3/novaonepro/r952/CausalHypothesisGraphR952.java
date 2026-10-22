package com.ethk3.novaonepro.r952;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CausalHypothesisGraphR952 {private final List<CausalEdgeR952> edges=new ArrayList<CausalEdgeR952>();public void add(CausalEdgeR952 e){if(e==null)throw new IllegalArgumentException("R952_CAUSAL_EDGE");edges.add(e);}public List<CausalEdgeR952> edges(){return Collections.unmodifiableList(edges);}}
