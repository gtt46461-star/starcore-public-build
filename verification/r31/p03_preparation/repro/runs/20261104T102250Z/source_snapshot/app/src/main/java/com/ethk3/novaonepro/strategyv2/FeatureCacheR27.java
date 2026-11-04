package com.ethk3.novaonepro.strategyv2;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.lang.ref.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/** R27-014 shared point-in-time feature cache. A leaf value at row t is computed only from rows &lt; t
 * (ReplayLeakageTestR27 / H01 checks this for every leaf kind). Values are flattened [t*10 + (number-1)].
 * Lag-based leaves are NaN when the lag window crosses a missing-issue gap; the evaluator records that
 * row as SKIPPED_GAP instead of inventing continuity. One cache serves every candidate of a dataset. */
public final class FeatureCacheR27 implements AutoCloseable {
    public static final int[] WINDOWS = {8, 32, 128, 512, 2048, 5000};
    public static final int[] HALF_LIVES = {8, 32, 128};
    public final ReplayDatasetR27 data;
    private final int capacity;
    private final Map<String, double[]> cache;
    private long computed, hits;
    private static final Map<FeatureCacheR27,Boolean> REGISTERED_R31=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<FeatureCacheR27,Boolean>());
    private final MemoryBudgetR31 memory;
    private final long rawByteCapacity;
    private long rawBytes,rawMapBytesDropped,sharedReads,sharedBuilds;
    private volatile boolean closedR31;
    private SharedFeatureStoreR31 sharedR31;
    private String featureAbiR31="UNBOUND_INSTANCE:"+CACHE_IDS_R31.incrementAndGet();
    private static final java.util.concurrent.atomic.AtomicLong CACHE_IDS_R31=new java.util.concurrent.atomic.AtomicLong();
    private static final ReferenceQueue<double[]> RAW_QUEUE_R31=new ReferenceQueue<>();
    private static final Map<WeakReference<double[]>,MemoryBudgetR31.Lease> RAW_LEASES_R31=new LinkedHashMap<>();
    private static final Runnable RAW_RECLAIMER_R31=FeatureCacheR27::reapReleasedArraysR31;
    private static final class SharedHolderR31 {static final SharedFeatureStoreR31 STORE=new SharedFeatureStoreR31(MemoryBudgetR31.processBudget(),Math.max(272,Math.min(32L<<20,Runtime.getRuntime().maxMemory()/16)));}
    private static final class BuildLockR31 {final ReentrantLock lock=new ReentrantLock();int users;}
    private final Map<String,BuildLockR31> buildLocksR31=new LinkedHashMap<>();
    public static void reapReleasedArraysR31(){synchronized(RAW_LEASES_R31){Reference<? extends double[]> ref;while((ref=RAW_QUEUE_R31.poll())!=null){MemoryBudgetR31.Lease lease=RAW_LEASES_R31.remove(ref);if(lease!=null)lease.close();}}}
    private static void trackRawR31(double[] array,MemoryBudgetR31.Lease lease){synchronized(RAW_LEASES_R31){RAW_LEASES_R31.put(new WeakReference<>(array,RAW_QUEUE_R31),lease);}}
    private static long arrayBytesR31(double[] array){return 128L+8L*array.length;}
    private void clearRawR31(){rawMapBytesDropped+=rawBytes;cache.clear();rawBytes=0;}
    private void trimRawR31(){java.util.Iterator<Map.Entry<String,double[]>> it=cache.entrySet().iterator();while((cache.size()>capacity||rawBytes>rawByteCapacity)&&it.hasNext()){double[] value=it.next().getValue();it.remove();long bytes=arrayBytesR31(value);rawBytes-=bytes;rawMapBytesDropped+=bytes;}}
    public void bindSharedFeaturesR31(String operatorAbiHash){bindSharedFeaturesR31(sharedR31,operatorAbiHash);}
    public static SharedFeatureStoreR31 processSharedFeatureStoreR31(){return SharedHolderR31.STORE;}
    public synchronized void bindSharedFeaturesR31(SharedFeatureStoreR31 shared,String operatorAbiHash){if(operatorAbiHash==null||operatorAbiHash.isEmpty())throw new IllegalArgumentException("R31_FEATURE_ABI");if(!operatorAbiHash.equals(featureAbiR31))clearRawR31();this.sharedR31=java.util.Objects.requireNonNull(shared);featureAbiR31=operatorAbiHash;}
    public synchronized Map<String,Object> memorySnapshotR31(){reapReleasedArraysR31();Map<String,Object> out=new LinkedHashMap<>();out.put("rawCacheBytes",rawBytes);out.put("rawCacheCapacityBytes",rawByteCapacity);out.put("rawMapBytesDropped",rawMapBytesDropped);out.put("rawDropEvidence","MAP_REFERENCE_REMOVED_NOT_ACTUAL_HEAP_FREE");out.put("sharedReads",sharedReads);out.put("sharedBuilds",sharedBuilds);out.put("featureAbi",featureAbiR31);out.put("overrideSharedDisabled",!overrides.isEmpty());synchronized(RAW_LEASES_R31){out.put("processRawArrayLeasesAwaitingGc",RAW_LEASES_R31.size());}out.put("sharedStore",sharedR31.snapshot());out.put("uncoveredOwners",java.util.Arrays.asList("AST_RETURNED_SCORE_ARRAYS_AND_SCORE_MEMO","MUTABLE_FAMILY_MODEL_STATE","LEAF_INTERNAL_TEMPORARY_SCRATCH"));return out;}
    public Map<String,Object> trimUnusedR31(){synchronized(this){clearRawR31();}Map<String,Object> result=sharedR31.evictTo(0);reapReleasedArraysR31();return result;}
    public static Map<String,Object> trimRegisteredCachesR31(){java.util.List<FeatureCacheR27> caches;synchronized(REGISTERED_R31){caches=new java.util.ArrayList<>(REGISTERED_R31.keySet());}int trimmed=0;for(FeatureCacheR27 cache:caches){cache.trimUnusedR31();trimmed++;}SharedHolderR31.STORE.evictTo(0);reapReleasedArraysR31();Map<String,Object> out=new LinkedHashMap<>();out.put("registeredCachesTrimmed",trimmed);out.put("actualHeapReleasedBytes",null);out.put("evidence","UNPINNED_CACHE_REFERENCES_REMOVED; RAW_ARRAY_LEASES_WAIT_FOR_OBSERVED_GC");return out;}
    @Override public synchronized void close(){closedR31=true;clearRawR31();streams.clear();modelHistoryHashes.clear();overrides.clear();reapReleasedArraysR31();}
    private volatile ModelSurfaceProviderR30.Source modelSurfaces=ModelSurfaceProviderR30.NONE;
    private volatile long modelSourceVersion;
    private final Map<Integer,String> modelHistoryHashes=new LinkedHashMap<Integer,String>(128,.75f,true){protected boolean removeEldestEntry(Map.Entry<Integer,String> e){return size()>128;}};
    public void bindModelSurfaces(ModelSurfaceProviderR30.Source source){synchronized(this){modelSurfaces=source==null?ModelSurfaceProviderR30.NONE:source;modelSourceVersion++;cache.keySet().removeIf(k->k.startsWith("model_surface|"));rawBytes=0;for(double[] value:cache.values())rawBytes+=arrayBytesR31(value);}}
    public long modelSourceVersion(){return modelSourceVersion;}
    public ModelSurfaceProviderR30.Surface modelSurface(int row,int position){return checkedSurface(modelSurfaces,row,position);}
    private ModelSurfaceProviderR30.Surface checkedSurface(ModelSurfaceProviderR30.Source source,int row,int position){
        if(row<0||row>=data.size()||position<1||position>10)return null;ModelSurfaceProviderR30.Surface s=source.at(row,position);if(s==null)return null;
        if(s.targetOrdinal!=data.ordinals[row]||s.position!=position||s.trainedThroughOrdinal>=s.targetOrdinal)return null;
        String expected;synchronized(modelHistoryHashes){expected=modelHistoryHashes.get(row);}if(expected==null){expected=ModelSurfaceProviderR30.historyHash(data,row);synchronized(modelHistoryHashes){modelHistoryHashes.put(row,expected);}}
        return expected.equals(s.historyHash)?s:null;
    }
    public String modelBlockReason(int row,int position){return modelSurfaces.reason(row,position);}
    public void maskMissingModelRows(double[] values,int positions){
        for(int t=0;t<data.size();t++)for(int p=1;p<=10;p++)if((positions&(1<<(p-1)))!=0&&modelSurface(t,p)==null){Arrays.fill(values,t*10,t*10+10,Double.NaN);break;}
    }
    private double[] modelSurfaceValues(int position){double[] out=nan();ModelSurfaceProviderR30.Source snapshot=modelSurfaces;for(int t=0;t<data.size();t++){ModelSurfaceProviderR30.Surface s=checkedSurface(snapshot,t,position);CooperativeContinuationR26.poll();if(s!=null)System.arraycopy(s.probabilities(),0,out,t*10,10);}return out;}


    public FeatureCacheR27(ReplayDatasetR27 data) { this(data, 48); }
    public FeatureCacheR27(ReplayDatasetR27 data, int capacity) {
        this(data,capacity,MemoryBudgetR31.processBudget(),SharedHolderR31.STORE);
    }
    public FeatureCacheR27(ReplayDatasetR27 data,int capacity,MemoryBudgetR31 memory,SharedFeatureStoreR31 shared) {
        this.data=data;this.capacity=Math.max(4,capacity);this.memory=java.util.Objects.requireNonNull(memory);this.sharedR31=java.util.Objects.requireNonNull(shared);
        this.rawByteCapacity=Math.max(128,Math.min(16L<<20,((Number)memory.snapshot().get("maxHeapBytes")).longValue()/32));
        memory.registerReclaimer(RAW_RECLAIMER_R31);REGISTERED_R31.put(this,Boolean.TRUE);
        cache=new LinkedHashMap<String,double[]>(64,.75f,true);
    }
    public synchronized long computedLeaves() { return computed; }
    public synchronized long cacheHits() { return hits; }

    /** Leaf kinds; the int parameters are documented per kind. Every kind is PREDRAW_HISTORY (rows &lt; t). */
    public static final String[] KINDS = {"freq", "fdecay", "freqnb", "gap", "gaprank", "meangap", "trans1", "trans2", "cross", "crosscond",
            "repeat", "neighbor", "oddeven", "bigsmall", "sumbucket", "regime", "knn", "family", "model_surface", "set_intersection", "set_intersection_when_repeat", "randu"};

    /** @param tp target position 1..10 the policy predicts. */
    public double[] leaf(String kind, int tp, int a, int b) {
        if (!targetDependent(kind)) tp = 0;
        String key = kind + '|' + tp + '|' + a + '|' + b+(kind.equals("model_surface")?"|"+AtomNetSessionR30.ABI+"|"+modelSourceVersion:"");
        synchronized (this) { double[] v = cache.get(key); if (v != null) { hits++; return v; } }
        if(closedR31)throw new IllegalStateException("R31_FEATURE_CACHE_CLOSED");
        MemoryBudgetR31.Admission admission=memory.tryAcquire("raw-leaf:"+data.datasetHash.substring(0,16),MemoryBudgetR31.Kind.FEATURE_CACHE,128L+80L*data.size(),()->Thread.currentThread().isInterrupted());
        if(!admission.acquired())throw new MemoryBudgetR31.CapacityWait(admission.reason);
        double[] v;
        try{v=compute(kind,tp,a,b);if(v.length!=data.size()*10)throw new IllegalStateException("R31_LEAF_LENGTH");trackRawR31(v,admission.lease);}
        catch(RuntimeException|Error failure){admission.lease.close();throw failure;}
        synchronized (this) { double[] raced = cache.get(key); if (raced != null) return raced; computed++;if(!closedR31){cache.put(key,v);rawBytes+=arrayBytesR31(v);trimRawR31();} }
        return v;
    }

    public SharedFeatureStoreR31.Reader acquireLeafR31(String kind,int tp,int a,int b) {
        if(!targetDependent(kind))tp=0;String local=kind+'|'+tp+'|'+a+'|'+b;
        final SharedFeatureStoreR31 store;final SharedFeatureStoreR31.Key key;final BuildLockR31 build;
        synchronized(this){if(closedR31)throw new IllegalStateException("R31_FEATURE_CACHE_CLOSED");if(!overrides.isEmpty()||kind.equals("model_surface")||kind.equals("family")||data.size()==0)return null;store=sharedR31;key=new SharedFeatureStoreR31.Key(data.datasetHash,Math.max(0,data.ordinals[data.size()-1]-1),featureAbiR31,"RAW_CAUSAL_LEAF_V1",local);SharedFeatureStoreR31.Reader hit=store.acquire(key,"ast-leaf-reader");if(hit!=null){sharedReads++;return hit;}build=buildLocksR31.computeIfAbsent(local,k->new BuildLockR31());build.users++;}
        boolean locked=false;
        try{
            while(!(locked=build.lock.tryLock(20,TimeUnit.MILLISECONDS)))CooperativeContinuationR26.poll();
            CooperativeContinuationR26.poll();SharedFeatureStoreR31.Reader hit=store.acquire(key,"ast-leaf-reader");if(hit!=null){synchronized(this){sharedReads++;}return hit;}
            MemoryBudgetR31.Admission working=memory.tryAcquire("shared-leaf-producer:"+data.datasetHash.substring(0,16),MemoryBudgetR31.Kind.RESEARCH,128L+80L*data.size(),()->Thread.currentThread().isInterrupted());if(!working.acquired())throw new MemoryBudgetR31.CapacityWait(working.reason);
            try{double[] values=compute(kind,tp,a,b);SharedFeatureStoreR31.Publication publication=store.publishDoubles("immutable-leaf:"+data.datasetHash.substring(0,16),key,values,()->Thread.currentThread().isInterrupted());if(!publication.acquired())throw new MemoryBudgetR31.CapacityWait(publication.reason);synchronized(this){sharedBuilds++;}return publication.reader;}
            finally{working.lease.close();}
        }catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new java.util.concurrent.CancellationException("R31_FEATURE_READER_INTERRUPTED");}
        finally{if(locked)build.lock.unlock();synchronized(this){if(--build.users==0)buildLocksR31.remove(local,build);}}
    }

    static boolean targetDependent(String kind) {
        switch (kind) { case "trans1": case "trans2": case "crosscond": case "repeat": case "sumbucket": case "knn": case "family": case "set_intersection": case "set_intersection_when_repeat": return true; default: return false; }
    }

    /** Test/ABI hook: replaces one leaf kind's implementation (used by H11 to simulate a changed operator). */
    public interface LeafOverride { double[] compute(FeatureCacheR27 f, int tp, int a, int b); }
    private final Map<String, LeafOverride> overrides = new java.util.HashMap<>();
    public synchronized void override(String kind, LeafOverride o) { overrides.put(kind, o); clearRawR31(); }

    private double[] compute(String kind, int tp, int a, int b) {
        LeafOverride o;
        synchronized (this) { o = overrides.get(kind); }
        if (o != null) return o.compute(this, tp, a, b);
        switch (kind) {
            case "set_intersection": return exactIntersection(tp,a,b);
            case "set_intersection_when_repeat": {double[] out=exactIntersection(tp,a,b),state=leaf("regime",tp,0,b);for(int row=0;row<data.size();row++)if(row<b||!data.consecutive(row,b)||!Double.isFinite(state[row*10])||state[row*10]<=.1)java.util.Arrays.fill(out,row*10,row*10+10,Double.NaN);return out;}
            case "freq": return freq(a, b);
            case "fdecay": return decay(a, b);
            case "freqnb": return neighborFreq(a, b);
            case "gap": return gap(a, false);
            case "gaprank": return gap(a, true);
            case "meangap": return meanGap(a, b);
            case "trans1": return transition(tp, 1);
            case "trans2": return transition(tp, 2);
            case "cross": return cross(a, b);
            case "crosscond": return crossCond(a, tp);
            case "repeat": return cross(tp, 1);
            case "neighbor": return neighbor(a);
            case "oddeven": return parity(a, b, true);
            case "bigsmall": return parity(a, b, false);
            case "sumbucket": return sumBucket(tp);
            case "regime": return regime(b);
            case "knn": return knn(tp, a, b);
            case "family": return family(tp, a);
            case "model_surface": return modelSurfaceValues(a);
            case "randu": return randomUniform(a);
            default: throw new IllegalArgumentException("R27_UNKNOWN_LEAF:" + kind);
        }
    }

    private int t() { return data.size(); }
    private double[] nan() { double[] v = new double[t() * 10]; Arrays.fill(v, Double.NaN); return v; }

    double[] exactIntersection(int position,int shortWindow,int longWindow){
        double[] out=nan();if(!PolicyAstR27.pos(position)||!PolicyAstR27.window(shortWindow)||!PolicyAstR27.window(longWindow)||shortWindow>=longWindow)throw new IllegalArgumentException("SET_SOURCE_CONTRACT");
        double[] a=leaf("freq",position,position,shortWindow),b=leaf("freqnb",position,position,longWindow);
        for(int row=longWindow;row<data.size();row++){if((row&31)==0)CooperativeContinuationR26.poll();if(!data.consecutive(row,longWindow))continue;int mask=SetIntersectionPolicyR30.mask(a,row*10)&SetIntersectionPolicyR30.mask(b,row*10);if(Integer.bitCount(mask)!=3)continue;for(int k=0;k<10;k++)out[row*10+k]=(mask&(1<<k))!=0?1.0/3:0;}return out;
    }

    /** freq(src,W): share of rows in [t-W,t) where number n sat at position src. */
    double[] freq(int src, int w) {
        int n = t(); double[] v = nan(); int[] count = new int[10];
        for (int t = 0; t < n; t++) {
            if((t&31)==0)CooperativeContinuationR26.poll();
            int len = Math.min(w, t);
            if (len > 0) for (int k = 0; k < 10; k++) v[t * 10 + k] = count[k] / (double) len;
            count[data.draws[t][src - 1] - 1]++;
            if (t - w >= 0) count[data.draws[t - w][src - 1] - 1]--;
        }
        return v;
    }

    /** fdecay(src,halfLife): exponentially weighted occurrence rate. */
    double[] decay(int src, int halfLife) {
        int n = t(); double[] v = nan(); double[] s = new double[10]; double lambda = Math.pow(0.5, 1.0 / halfLife), mass = 0;
        for (int t = 0; t < n; t++) {
            if((t&31)==0)CooperativeContinuationR26.poll();
            if (mass > 0) for (int k = 0; k < 10; k++) v[t * 10 + k] = s[k] / mass;
            for (int k = 0; k < 10; k++) s[k] *= lambda;
            mass = mass * lambda + 1;
            s[data.draws[t][src - 1] - 1] += 1;
        }
        return v;
    }

    /** freqnb(src,W): mean occurrence share at the two neighbouring positions of src. */
    double[] neighborFreq(int src, int w) {
        int left = src == 1 ? 10 : src - 1, right = src == 10 ? 1 : src + 1;
        double[] l = leaf("freq", 0, left, w), r = leaf("freq", 0, right, w), v = new double[l.length];
        for (int i = 0; i < v.length; i++) v[i] = 0.5 * (l[i] + r[i]);
        return v;
    }

    /** gap(src): rows since n last appeared at src, scaled by 1/100 and capped at 1; gaprank: rank of that gap / 9. */
    double[] gap(int src, boolean rank) {
        int n = t(); double[] v = nan(); int[] last = new int[10]; Arrays.fill(last, -1);
        double[] g = new double[10];
        for (int t = 0; t < n; t++) {
            if((t&31)==0)CooperativeContinuationR26.poll();
            if (t > 0) {
                for (int k = 0; k < 10; k++) g[k] = last[k] < 0 ? 1.0 : Math.min(1.0, (t - last[k]) / 100.0);
                if (!rank) for (int k = 0; k < 10; k++) v[t * 10 + k] = g[k];
                else for (int k = 0; k < 10; k++) { int r = 0; for (int j = 0; j < 10; j++) if (g[j] < g[k] || (g[j] == g[k] && j < k)) r++; v[t * 10 + k] = r / 9.0; }
            }
            last[data.draws[t][src - 1] - 1] = t;
        }
        return v;
    }

    /** meangap(src,W): mean inter-arrival of n at src within the window, scaled 1/30 and capped at 1. */
    double[] meanGap(int src, int w) {
        double[] f = leaf("freq", 0, src, w), v = new double[f.length];
        for (int t = 0; t < t(); t++) {
            int len = Math.min(w, t);
            for (int k = 0; k < 10; k++) { double x = f[t * 10 + k]; v[t * 10 + k] = Double.isNaN(x) ? Double.NaN : Math.min(1.0, len / (x * len + 1.0) / 30.0); }
        }
        return v;
    }

    /** trans{lag}(tp): Laplace-smoothed P(n at tp at t | number at tp at t-lag), counted from consecutive pairs before t. */
    double[] transition(int tp, int lag) {
        int n = t(); double[] v = nan(); double[][] c = new double[10][10];
        for (double[] row : c) Arrays.fill(row, 1.0);
        double[] tot = new double[10]; Arrays.fill(tot, 10.0);
        for (int t = 0; t < n; t++) {
            if((t&31)==0)CooperativeContinuationR26.poll();
            if (data.consecutive(t, lag)) {
                int x = data.draws[t - lag][tp - 1] - 1;
                for (int k = 0; k < 10; k++) v[t * 10 + k] = c[x][k] / tot[x];
                int y = data.draws[t][tp - 1] - 1; c[x][y]++; tot[x]++;
            }
        }
        return v;
    }

    /** cross(src,lag): indicator n == number at src in row t-lag. NaN across a gap. lag must be ≥1. */
    double[] cross(int src, int lag) {
        if (lag < 1) throw new IllegalArgumentException("R27_CAUSAL_LAG");
        int n = t(); double[] v = nan();
        for (int t = 0; t < n; t++) if (data.consecutive(t, lag)) { int x = data.draws[t - lag][src - 1]; for (int k = 0; k < 10; k++) v[t * 10 + k] = k + 1 == x ? 1 : 0; }
        return v;
    }

    /** crosscond(src): Laplace P(n at tp at t | number at src at t-1). */
    double[] crossCond(int src, int tp) {
        int n = t(); double[] v = nan(); double[][] c = new double[10][10];
        for (double[] row : c) Arrays.fill(row, 1.0);
        double[] tot = new double[10]; Arrays.fill(tot, 10.0);
        for (int t = 0; t < n; t++) {
            if((t&31)==0)CooperativeContinuationR26.poll();
            if (data.consecutive(t, 1)) {
                int x = data.draws[t - 1][src - 1] - 1;
                for (int k = 0; k < 10; k++) v[t * 10 + k] = c[x][k] / tot[x];
                int y = data.draws[t][tp - 1] - 1; c[x][y]++; tot[x]++;
            }
        }
        return v;
    }

    /** neighbor(src): indicator that n is cyclically adjacent (±1) to the number at src in row t-1. */
    double[] neighbor(int src) {
        int n = t(); double[] v = nan();
        for (int t = 0; t < n; t++) if (data.consecutive(t, 1)) {
            int x = data.draws[t - 1][src - 1];
            for (int k = 1; k <= 10; k++) { int d = Math.abs(k - x); v[t * 10 + k - 1] = d == 1 || d == 9 ? 1 : 0; }
        }
        return v;
    }

    /** oddeven / bigsmall(src,W): share of window rows whose number at src shares n's parity / size class. */
    double[] parity(int src, int w, boolean odd) {
        int n = t(); double[] v = nan(); int cls1 = 0; // count of class-1 rows in window
        for (int t = 0; t < n; t++) {
            if((t&31)==0)CooperativeContinuationR26.poll();
            int len = Math.min(w, t);
            if (len > 0) for (int k = 1; k <= 10; k++) { boolean c = odd ? (k & 1) == 1 : k > 5; double share = cls1 / (double) len; v[t * 10 + k - 1] = c ? share : 1 - share; }
            int x = data.draws[t][src - 1]; if (odd ? (x & 1) == 1 : x > 5) cls1++;
            if (t - w >= 0) { int y = data.draws[t - w][src - 1]; if (odd ? (y & 1) == 1 : y > 5) cls1--; }
        }
        return v;
    }

    /** sumbucket(tp): P(n at tp | (sum of positions 1..3 at t-1) mod 10), Laplace-smoothed counts. */
    double[] sumBucket(int tp) {
        int n = t(); double[] v = nan(); double[][] c = new double[10][10];
        for (double[] row : c) Arrays.fill(row, 1.0);
        double[] tot = new double[10]; Arrays.fill(tot, 10.0);
        for (int t = 0; t < n; t++) if (data.consecutive(t, 1)) {
            int[] p = data.draws[t - 1]; int x = (p[0] + p[1] + p[2]) % 10;
            for (int k = 0; k < 10; k++) v[t * 10 + k] = c[x][k] / tot[x];
            c[x][data.draws[t][tp - 1] - 1]++; tot[x]++;
        }
        return v;
    }

    /** regime(W): share of position repeats (x_t,p == x_{t-1},p) over the last W consecutive pairs; same for every n. */
    double[] regime(int w) {
        int n = t(); double[] v = nan(); int[] rep = new int[n]; int sum = 0, len = 0;
        for (int t = 0; t < n; t++) {
            if((t&31)==0)CooperativeContinuationR26.poll();
            if (len > 0) for (int k = 0; k < 10; k++) v[t * 10 + k] = sum / (10.0 * len);
            if (data.consecutive(t, 1)) { int r = 0; for (int p = 0; p < 10; p++) if (data.draws[t][p] == data.draws[t - 1][p]) r++; rep[t] = r; sum += r; len++; }
            else rep[t] = -1;
            if (t - w >= 0 && rep[t - w] >= 0) { sum -= rep[t - w]; len--; }
        }
        return v;
    }

    /** knn(W,k): next-number distribution at tp after the k most similar past windows of length W (Hamming at tp),
     * searched over at most the 1000 previous rows. NaN when the current W-window crosses a gap. */
    double[] knn(int tp, int w, int k) {
        int n = t(); double[] v = nan(); int col = tp - 1;
        for (int t = w; t < n; t++) {
            CooperativeContinuationR26.poll();
            if (!data.consecutive(t, w)) continue;
            int from = Math.max(w, t - 1000);
            int[] dist = new int[Math.max(0, t - from)]; int[] at = new int[dist.length]; int m = 0;
            for (int s = from; s < t; s++) {
                if((s&31)==0)CooperativeContinuationR26.poll();
                if (!data.consecutive(s, w)) continue;
                int d = 0; for (int j = 1; j <= w; j++) if (data.draws[s - j][col] != data.draws[t - j][col]) d++;
                dist[m] = d; at[m] = s; m++;
            }
            if (m == 0) continue;
            double[] h = new double[10]; Arrays.fill(h, 0.5); double tot = 5;
            // partial selection by distance, then by recency (deterministic)
            for (int d = 0; d <= w && tot - 5 < k; d++)
                for (int i = m - 1; i >= 0 && tot - 5 < k; i--) if (dist[i] == d) { h[data.draws[at[i]][col] - 1]++; tot++; }
            for (int j = 0; j < 10; j++) v[t * 10 + j] = h[j] / tot;
        }
        return v;
    }

    /** family(tp, familyIndex): R26 family model run strictly online (predict before update), reset at gaps.
     * One stream per family serves all ten positions, and it is extended incrementally when rows are appended
     * (see {@link #extendFrom}); the last row of the dataset is never fed to the model, so a live placeholder row
     * cannot leak into any value. */
    double[] family(int tp, int index) {
        FamilyStream st;
        synchronized (this) { st = streams.get(index); if (st == null) { st = new FamilyStream(index); streams.put(index, st); } }
        double[] all;
        try { synchronized (st) { all = st.advance(data); } }
        catch (IllegalStateException prefixChanged) {
            // R30-B03-04: rows [0,fed) changed under a reused stream: rebuild the whole stream from this dataset and record it.
            if (!String.valueOf(prefixChanged.getMessage()).startsWith("R27_FAMILY_STREAM") && !String.valueOf(prefixChanged.getMessage()).startsWith("R30_FAMILY_PREFIX")) throw prefixChanged;
            FamilyStream fresh = new FamilyStream(index);
            synchronized (this) { streams.put(index, fresh); }
            REBUILDS_R30.incrementAndGet();
            if (REBUILD_EVENTS_R30.size() < 256) REBUILD_EVENTS_R30.add(new Object[] {index, data.size() == 0 ? -1L : data.ordinals[data.size() - 1], String.valueOf(prefixChanged.getMessage()), System.currentTimeMillis()});
            synchronized (fresh) { all = fresh.advance(data); }
        }
        int n = t(); double[] v = new double[n * 10];
        for (int t = 0; t < n; t++) System.arraycopy(all, t * 100 + (tp - 1) * 10, v, t * 10, 10);
        return v;
    }

    private final Map<Integer, FamilyStream> streams = new java.util.HashMap<>();
    /** R30-B03-04: stream rebuilds after a changed prefix (written to r30_cache_rebuild by RuntimeR27). */
    public static final java.util.concurrent.atomic.AtomicLong REBUILDS_R30 = new java.util.concurrent.atomic.AtomicLong();
    public static final java.util.concurrent.ConcurrentLinkedQueue<Object[]> REBUILD_EVENTS_R30 = new java.util.concurrent.ConcurrentLinkedQueue<>();
    static final int PREFIX_CHECK_EVERY_R30 = 100;
    /** Order-sensitive hash of rows [0,n) (ordinal + permutation). */
    static long prefixHashR30(ReplayDatasetR27 d, int n) {
        long h = 0x9E3779B97F4A7C15L;
        for (int t = 0; t < n; t++) { h = (h ^ d.ordinals[t]) * 0x100000001B3L; for (int v : d.draws[t]) h = (h ^ v) * 0x100000001B3L; }
        return h;
    }

    static final class FamilyStream {
        final int index; final OnlineModelsSU2.Model model; int fed; double[] values = new double[0]; long lastFedOrdinal = Long.MIN_VALUE;
        final float[][] p = new float[10][10]; final float[] st;
        FamilyStream(int index) { this.index = index; model = FamilyRegistryR26.build(String.format(java.util.Locale.ROOT, "F%02d", index)); st = new float[Math.max(1, model.stateWidth())]; }
        int[] lastFedDraw;
        int checkedFed; long checkedHash; ReplayDatasetR27 verifiedData; // R30-B03-04: prefix [0,checkedFed) hash at the last 100-row checkpoint
        /** Values for every row of d: prediction at row t uses only rows &lt; t. Rows [0,fed) must be unchanged in d. */
        double[] advance(ReplayDatasetR27 d) {
            int n = d.size();
            if (fed > 0 && (n < fed || d.ordinals[fed - 1] != lastFedOrdinal || !java.util.Arrays.equals(d.draws[fed - 1], lastFedDraw)))
                throw new IllegalStateException("R27_FAMILY_STREAM_PREFIX_CHANGED");
            // R30-B03-04: the whole prefix up to the last 100-row checkpoint is compared by hash (once per new dataset),
            // not only its last row.
            if (checkedFed > 0 && d != verifiedData) {
                if (n < checkedFed || prefixHashR30(d, checkedFed) != checkedHash) throw new IllegalStateException("R30_FAMILY_PREFIX_HASH_CHANGED");
                verifiedData = d;
            }
            int known = values.length / 100;
            if (known > n) throw new IllegalStateException("R27_FAMILY_STREAM_SHRUNK");
            double[] out = java.util.Arrays.copyOf(values, n * 100);
            for (int t = fed; t < n; t++) {
                if (t >= known) {
                    if (t == 0) java.util.Arrays.fill(out, 0, 100, Double.NaN);
                    else {
                        if (!d.consecutive(t, 1) && model instanceof TemporalModelR26) ((TemporalModelR26) model).resetTemporalContext();
                        model.predict(p, st);
                        for (int q = 0; q < 10; q++) for (int k = 0; k < 10; k++) { double x = p[q][k]; out[t * 100 + q * 10 + k] = Double.isFinite(x) ? x : Double.NaN; }
                    }
                }
                if (t < n - 1) { model.update(d.draws[t]); fed = t + 1; lastFedOrdinal = d.ordinals[t]; lastFedDraw = d.draws[t].clone(); }
            }
            values = out;
            if (fed >= checkedFed + PREFIX_CHECK_EVERY_R30) { checkedFed = fed; checkedHash = prefixHashR30(d, fed); verifiedData = d; }
            return out;
        }
    }

    /** Reuses family streams of an older cache whose dataset is a prefix of this one (live incremental path). */
    public void extendFrom(FeatureCacheR27 older) {
        synchronized (older) { for (Map.Entry<Integer, FamilyStream> e : older.streams.entrySet()) synchronized (this) { streams.put(e.getKey(), e.getValue()); } }
    }

    /** randu(seed): deterministic uniform noise per (t,n) — the I00 random-baseline input, trivially causal. */
    double[] randomUniform(int seed) {
        int n = t(); double[] v = new double[n * 10];
        for (int t = 0; t < n; t++) for (int k = 0; k < 10; k++) {
            long z = (data.ordinals[t] * 0x9E3779B97F4A7C15L) ^ ((long) seed * 0xBF58476D1CE4E5B9L) ^ (k * 0x94D049BB133111EBL);
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L; z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL; z ^= z >>> 31;
            v[t * 10 + k] = (z >>> 11) * 0x1.0p-53;
        }
        return v;
    }
}
