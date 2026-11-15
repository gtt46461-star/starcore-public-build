package com.ethk3.novaonepro.strategyv2;

import java.util.LinkedHashMap;
import java.util.Map;

/** One dataset snapshot + split + label view for a generation (docs/05 5.6 step 1). Labels may be shuffled within
 * H_TRAIN/H_SELECT for the null-evolution control; H_HOLDOUT labels are always real. */
public final class EvalContextR27 implements AutoCloseable {
    public final ReplayDatasetR27 data; public final FeatureCacheR27 cache; public final SplitR27 split; public final int[][] labels;
    private static final Map<EvalContextR27,Boolean> REGISTERED_R31=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<EvalContextR27,Boolean>());
    private final Map<String, double[]> scoreMemo = new LinkedHashMap<String, double[]>(64, 0.75f, true);
    private final java.util.IdentityHashMap<double[],Integer> scoreReferencesR31=new java.util.IdentityHashMap<>();
    private final long scoreByteCapacityR31;
    private long scoreMemoBytesR31,scoreBytesDroppedR31,scoreComputationsR31,scoreHitsR31;
    private volatile boolean closedR31;
    private final Map<String, FailureProfileR30> failureMemo = new LinkedHashMap<String, FailureProfileR30>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, FailureProfileR30> e) { return size() > 96; }
    };

    public EvalContextR27(FeatureCacheR27 cache, SplitR27 split, int[][] labels) {
        this.cache = cache; this.data = cache.data; this.split = split; this.labels = labels == null ? cache.data.draws : labels;
        this.scoreByteCapacityR31=Math.max(128,Math.min(8L<<20,((Number)cache.memoryBudgetR31().snapshot().get("maxHeapBytes")).longValue()/64));
        REGISTERED_R31.put(this,Boolean.TRUE);
    }

    public double[] scores(PolicySpecR27 s) {
        String k = data.datasetHash + "|" + s.ast.toString() + "@" + s.position+(s.ast.modelPositions!=0?"|MODEL_SOURCE_"+cache.modelSourceVersion():""); // R30-B03-06: a new dataset never hits old scores
        synchronized (scoreMemo) { if(closedR31)throw new IllegalStateException("R31_EVAL_CONTEXT_CLOSED");double[] v = scoreMemo.get(k); if (v != null) {scoreHitsR31++;return v;} }
        double[] v = PolicyAstR27.eval(s.ast, cache, s.position); // outside the lock: parallel replay threads
        synchronized (scoreMemo) {
            scoreComputationsR31++;double[] raced=scoreMemo.get(k);if(raced!=null)return raced;
            if(!closedR31){scoreMemo.put(k,v);Integer prior=scoreReferencesR31.get(v);scoreReferencesR31.put(v,prior==null?1:prior+1);scoreMemoBytesR31+=128+(prior==null?16L+8L*v.length:0);trimScoresR31(scoreByteCapacityR31);}
        }
        return v;
    }
    PolicyAstR27.ScopedScoresR31 borrowScoresR31(PolicySpecR27 spec){
        String key=data.datasetHash+"|"+spec.ast.toString()+"@"+spec.position+(spec.ast.modelPositions!=0?"|MODEL_SOURCE_"+cache.modelSourceVersion():"");
        synchronized(scoreMemo){if(closedR31)throw new IllegalStateException("R31_EVAL_CONTEXT_CLOSED");double[] cached=scoreMemo.get(key);if(cached!=null){scoreHitsR31++;return PolicyAstR27.ScopedScoresR31.legacy(cached);}}
        return PolicyAstR27.evalScopedR31(spec.ast,cache,spec.position);
    }
    private void trimScoresR31(long target){
        java.util.Iterator<Map.Entry<String,double[]>> entries=scoreMemo.entrySet().iterator();
        while((scoreMemoBytesR31>target||scoreMemo.size()>96)&&entries.hasNext()){
            double[] array=entries.next().getValue();entries.remove();int refs=scoreReferencesR31.get(array);long removed=128;
            if(refs==1){scoreReferencesR31.remove(array);removed+=16L+8L*array.length;}else scoreReferencesR31.put(array,refs-1);
            scoreMemoBytesR31-=removed;scoreBytesDroppedR31+=removed;
        }
    }
    public Map<String,Object> scoreMemorySnapshotR31(){synchronized(scoreMemo){Map<String,Object> out=new LinkedHashMap<>();out.put("estimatedRetainedScoreBytes",scoreMemoBytesR31);out.put("byteCapacity",scoreByteCapacityR31);out.put("memoEntries",scoreMemo.size());out.put("distinctPrimitiveBackings",scoreReferencesR31.size());out.put("estimatedReferenceBytesDropped",scoreBytesDroppedR31);out.put("computations",scoreComputationsR31);out.put("hits",scoreHitsR31);out.put("actualHeapReleasedBytes",null);out.put("lifecycle","DROP_MEMO_REFERENCE_ONLY; RETURNED_ARRAYS_MAY_STILL_BE_IN_USE");return out;}}
    private String scoreKeyR31(PolicySpecR27 spec){return data.datasetHash+"|"+spec.ast.toString()+"@"+spec.position+(spec.ast.modelPositions!=0?"|MODEL_SOURCE_"+cache.modelSourceVersion():"");}
    public boolean holdsScoresR31(PolicySpecR27 spec){synchronized(scoreMemo){return scoreMemo.containsKey(scoreKeyR31(spec));}}
    /** R31-B07-C03: releases a retired policy's memoized score backing and failure profile; returns the released bytes. */
    public long releaseRetiredR31(PolicySpecR27 spec){
        long released=0;
        synchronized(scoreMemo){double[] array=scoreMemo.remove(scoreKeyR31(spec));if(array!=null){int refs=scoreReferencesR31.get(array);released=128;if(refs==1){scoreReferencesR31.remove(array);released+=16L+8L*array.length;}else scoreReferencesR31.put(array,refs-1);scoreMemoBytesR31-=released;scoreBytesDroppedR31+=released;}}
        synchronized(failureMemo){failureMemo.remove(spec.policyHash());}
        return released;
    }
    public void trimUnusedScoresR31(){synchronized(scoreMemo){trimScoresR31(0);}synchronized(failureMemo){failureMemo.clear();}}
    static int trimRegisteredScoresR31(MemoryBudgetR31 memory){java.util.List<EvalContextR27> contexts;synchronized(REGISTERED_R31){contexts=new java.util.ArrayList<>(REGISTERED_R31.keySet());}int trimmed=0;for(EvalContextR27 context:contexts)if(context.cache.memoryBudgetR31()==memory){context.trimUnusedScoresR31();trimmed++;}return trimmed;}
    @Override public void close(){synchronized(scoreMemo){closedR31=true;trimScoresR31(0);}synchronized(failureMemo){failureMemo.clear();}REGISTERED_R31.remove(this);}
    public double temperature(PolicySpecR27 s) { try(PolicyAstR27.ScopedScoresR31 scores=borrowScoresR31(s)){return ReplayEvaluatorR27.fitTemperatureScopedR31(s,scores,labels,split.trainFrom,split.trainTo);} }

    public FailureProfileR30 failureProfile(PolicySpecR27 s) {
        String key = s.policyHash(); // this context owns one immutable dataset, split and label view
        synchronized (failureMemo) { FailureProfileR30 p = failureMemo.get(key); if (p != null) return p; }
        FailureProfileR30 p = FailureProfileR30.measure(s,this); // no replay while holding the cache monitor
        synchronized (failureMemo) { if(!closedR31)failureMemo.put(key,p); }
        return p;
    }

    /** Pass through H_TRAIN and H_SELECT only (candidates never touch the sealed block). */
    public ReplayEvaluatorR27.Trace selectTrace(PolicySpecR27 s, double tau) {
        return ReplayEvaluatorR27.evaluateTrace(s, this, SplitR27.SELECT, tau, null);
    }
    /** Pass that also records H_HOLDOUT — only the generation's champion and its challengers may call this, through the ledger. */
    public ReplayEvaluatorR27.Trace holdoutTrace(PolicySpecR27 s, double tau) {
        return holdoutTrace(s, tau, null);
    }
    public ReplayEvaluatorR27.Trace holdoutTrace(PolicySpecR27 s, double tau, HoldoutLedgerR27 ledger) {
        return ReplayEvaluatorR27.evaluateTrace(s, this, SplitR27.CONFIRM, tau, ledger);
    }

    /** H_TRAIN mean log-loss at the policy's position with temperature tau (fusion weight fitting). */
    public double trainLogLoss(PolicySpecR27 s, double tau) {
        try(PolicyAstR27.ScopedScoresR31 sc=borrowScoresR31(s)){
        double[] z = new double[10], p = new double[10]; double loss = 0; int n = 0;
        for (int t = split.trainFrom; t < split.trainTo; t++) {
            if (!ReplayEvaluatorR27.standardizeScopedR31(sc, t, z)) continue;
            ReplayEvaluatorR27.softmax(z, tau, null, p);
            loss -= Math.log(Math.max(1e-12, p[labels[t][s.position - 1] - 1])); n++;
        }
        return n == 0 ? Double.POSITIVE_INFINITY : loss / n;
        }
    }

    /** Pearson correlation of standardized score rows of a and b at position tp over the last 500 H_SELECT rows. */
    public double correlation(PolicySpecR27 a, PolicySpecR27 b, int tp) {
        try(PolicyAstR27.ScopedScoresR31 x=PolicyAstR27.evalScopedR31(a.ast,cache,tp);PolicyAstR27.ScopedScoresR31 y=PolicyAstR27.evalScopedR31(b.ast,cache,tp)){
        double[] za = new double[10], zb = new double[10];
        double sxy = 0, sxx = 0, syy = 0; int n = 0;
        for (int t = Math.max(split.selectFrom, split.selectTo - 500); t < split.selectTo; t++) {
            if (!ReplayEvaluatorR27.standardizeScopedR31(x, t, za) || !ReplayEvaluatorR27.standardizeScopedR31(y, t, zb)) continue;
            for (int j = 0; j < 10; j++) { sxy += za[j] * zb[j]; sxx += za[j] * za[j]; syy += zb[j] * zb[j]; } n++;
        }
        return n == 0 || sxx == 0 || syy == 0 ? 0 : sxy / Math.sqrt(sxx * syy);
        }
    }
}
