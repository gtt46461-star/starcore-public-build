package com.ethk3.novaonepro.strategyv2;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** One dataset-owned bounded LRU. It is never shared between real and control experiments. */
public final class ScoreHistoryCacheSU1 {
    private final int capacity;
    private final LinkedHashMap<String,double[][]> values=new LinkedHashMap<>(64,.75f,true);
    private final Map<Integer,SparseConditionalCountsSU1> transitions=new java.util.TreeMap<>();
    private final Map<Integer,SparseConditionalCountsSU1.Estimate> audit=new java.util.TreeMap<>();
    private int[][] dataset;
    private String sourceRoot;
    private RelationTensorAdapterSU1.Factory graphFactory;
    private int auditTarget=-1;
    public ScoreHistoryCacheSU1(int capacity){this(capacity,(String)null);}
    public ScoreHistoryCacheSU1(int capacity,String sourceRoot){if(capacity<1||capacity>4096)throw new IllegalArgumentException("SU1_CACHE_CAP");this.capacity=capacity;this.sourceRoot=sourceRoot==null?null:HashingSU1.sha(sourceRoot);}
    public ScoreHistoryCacheSU1(int capacity,EventFrameSU1 frame){this(capacity,frame.sourceVersionHash);graphFactory=new RelationTensorAdapterSU1.Factory(frame);}
    private void bind(int[][] draws){
        if(dataset==null){
            dataset=draws;
            if(graphFactory!=null&&!graphFactory.sameNumbers(draws))throw new IllegalArgumentException("SU1_GRAPH_FRAME_DATASET_MISMATCH");
        }else if(dataset!=draws)throw new IllegalArgumentException("SU1_CACHE_DATASET_IDENTITY");
        if(sourceRoot==null){StringBuilder b=new StringBuilder("SU1_DATASET_NUMBERS_V1|");for(int[] row:draws)for(int n:row)b.append(n).append(',');sourceRoot=HashingSU1.text(b.toString());}
    }
    public synchronized RelationTensorAdapterSU1.Factory graphFactory(int[][] draws){bind(draws);if(graphFactory==null)graphFactory=new RelationTensorAdapterSU1.Factory(draws);return graphFactory;}
    public synchronized double[][] transition(int[][] draws,int target,int offset,BudgetTokenSU1 budget){
        bind(draws);
        SparseConditionalCountsSU1 estimator=transitions.get(offset);
        if(estimator==null){estimator=new SparseConditionalCountsSU1(draws,RelationSpecSU1.xt(offset),sourceRoot);transitions.put(offset,estimator);}
        SparseConditionalCountsSU1.Estimate estimate=estimator.at(target,budget);
        if(target>auditTarget){audit.clear();auditTarget=target;}
        if(target==auditTarget)audit.put(offset,estimate);
        return ProgramTreeSU1.Experts.znorm(estimate.probabilities());
    }
    public java.util.List<SparseConditionalCountsSU1.Estimate> relationAudit(){return new java.util.ArrayList<>(audit.values());}
    public double[][] score(String program,int target,Supplier<double[][]> compute) {
        String key=target+"|"+program;double[][] v=values.get(key);
        if(v==null){v=compute.get();values.put(key,v);while(values.size()>capacity){String first=values.keySet().iterator().next();values.remove(first);}}
        return v;
    }
    public int size(){return values.size();}
    public void clear(){values.clear();transitions.clear();audit.clear();dataset=null;graphFactory=null;auditTarget=-1;}
}
