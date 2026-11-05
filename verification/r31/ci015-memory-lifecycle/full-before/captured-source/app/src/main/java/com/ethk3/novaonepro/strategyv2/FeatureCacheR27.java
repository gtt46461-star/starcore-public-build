package com.ethk3.novaonepro.strategyv2;

import java.util.Arrays;
import java.util.Collection;
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
    private PolicyAstR27.ScratchPoolR31 scratchPoolR31;
    private String featureAbiR31="UNBOUND_INSTANCE:"+CACHE_IDS_R31.incrementAndGet();
    private static final java.util.concurrent.atomic.AtomicLong CACHE_IDS_R31=new java.util.concurrent.atomic.AtomicLong();
    private static final ReferenceQueue<double[]> RAW_QUEUE_R31=new ReferenceQueue<>();
    private static final Map<WeakReference<double[]>,MemoryBudgetR31.Lease> RAW_LEASES_R31=new LinkedHashMap<>();
    private static final ReferenceQueue<FamilyStream> FAMILY_FORK_QUEUE_R31=new ReferenceQueue<>();
    private static final Map<WeakReference<FamilyStream>,FamilyOwnerR31> FAMILY_FORK_LEASES_R31=new LinkedHashMap<>();
    private static final Runnable RAW_RECLAIMER_R31=FeatureCacheR27::reapReleasedArraysR31;
    private static final ThreadLocal<Boolean> CRITICAL_ALLOCATION_R31=new ThreadLocal<>();
    public static boolean realtimeAllocationScopeR31(){return Boolean.TRUE.equals(CRITICAL_ALLOCATION_R31.get());}
    private static final class BuiltinTargetR31 {final FeatureCacheR27 owner;final double[] values;boolean used;BuiltinTargetR31(FeatureCacheR27 owner,double[] values){this.owner=owner;this.values=values;}}
    private static final ThreadLocal<BuiltinTargetR31> BUILTIN_TARGET_R31=new ThreadLocal<>();
    private static final class SharedHolderR31 {static final SharedFeatureStoreR31 STORE=new SharedFeatureStoreR31(MemoryBudgetR31.processBudget(),Math.max(272,Math.min(32L<<20,Runtime.getRuntime().maxMemory()/16)));}
    private static final class BuildLockR31 {final ReentrantLock lock=new ReentrantLock();int users;}
    private final Map<String,BuildLockR31> buildLocksR31=new LinkedHashMap<>();
    public static void reapReleasedArraysR31(){synchronized(RAW_LEASES_R31){Reference<? extends double[]> ref;while((ref=RAW_QUEUE_R31.poll())!=null){MemoryBudgetR31.Lease lease=RAW_LEASES_R31.remove(ref);if(lease!=null)lease.close();}}synchronized(FAMILY_FORK_LEASES_R31){Reference<? extends FamilyStream> ref;while((ref=FAMILY_FORK_QUEUE_R31.poll())!=null){FamilyOwnerR31 owner=FAMILY_FORK_LEASES_R31.remove(ref);if(owner!=null)owner.close();}}}
    private static void trackRawR31(double[] array,MemoryBudgetR31.Lease lease){synchronized(RAW_LEASES_R31){RAW_LEASES_R31.put(new WeakReference<>(array,RAW_QUEUE_R31),lease);}}
    private static long arrayBytesR31(double[] array){return 128L+8L*array.length;}
    private void clearRawR31(){rawMapBytesDropped+=rawBytes;cache.clear();rawBytes=0;}
    private void trimRawR31(){java.util.Iterator<Map.Entry<String,double[]>> it=cache.entrySet().iterator();while((cache.size()>capacity||rawBytes>rawByteCapacity)&&it.hasNext()){double[] value=it.next().getValue();it.remove();long bytes=arrayBytesR31(value);rawBytes-=bytes;rawMapBytesDropped+=bytes;}}
    public void bindSharedFeaturesR31(String operatorAbiHash){bindSharedFeaturesR31(sharedR31,operatorAbiHash);}
    public static SharedFeatureStoreR31 processSharedFeatureStoreR31(){return SharedHolderR31.STORE;}
    MemoryBudgetR31 memoryBudgetR31(){return memory;}
    synchronized PolicyAstR27.ScratchPoolR31 scratchPoolR31(){if(closedR31)throw new java.util.concurrent.CancellationException("R31_FEATURE_CACHE_CLOSED");if(scratchPoolR31==null)scratchPoolR31=new PolicyAstR27.ScratchPoolR31(memory,data.size()*10,sharedR31);return scratchPoolR31;}
    public synchronized Map<String,Object> scratchMemorySnapshotR31(){return scratchPoolR31==null?java.util.Collections.emptyMap():scratchPoolR31.snapshot();}
    public synchronized void bindSharedFeaturesR31(SharedFeatureStoreR31 shared,String operatorAbiHash){if(operatorAbiHash==null||operatorAbiHash.isEmpty())throw new IllegalArgumentException("R31_FEATURE_ABI");if(!operatorAbiHash.equals(featureAbiR31))clearRawR31();this.sharedR31=java.util.Objects.requireNonNull(shared);featureAbiR31=operatorAbiHash;}
    public synchronized Map<String,Object> memorySnapshotR31(){reapReleasedArraysR31();Map<String,Object> out=new LinkedHashMap<>();out.put("rawCacheBytes",rawBytes);out.put("rawCacheCapacityBytes",rawByteCapacity);out.put("rawMapBytesDropped",rawMapBytesDropped);out.put("rawDropEvidence","MAP_REFERENCE_REMOVED_NOT_ACTUAL_HEAP_FREE");out.put("sharedReads",sharedReads);out.put("sharedBuilds",sharedBuilds);out.put("featureAbi",featureAbiR31);out.put("overrideSharedDisabled",!overrides.isEmpty());synchronized(RAW_LEASES_R31){out.put("processRawArrayLeasesAwaitingGc",RAW_LEASES_R31.size());}out.put("sharedStore",sharedR31.snapshot());out.put("coveredOwners",java.util.Arrays.asList("IMMUTABLE_SHARED_FEATURE_BLOCKS_WITH_READER_REFS","PRIVATE_AST_SCRATCH_UNTIL_SCOPE_CLOSE","PUBLIC_SCORE_MEMO_BYTE_LRU_DROP_REFERENCES_ONLY","FAMILY_CONSTRUCTOR_AND_PRIVATE_MATRIX_WRITE_BEFORE_ALLOCATE","FAMILY_ROW_CODEC_AND_RECOVERY_WORKSPACE","FAMILY_PENDING_OUTPUT_AND_CURSOR_SURVIVE_CANCEL"));out.put("uncoveredOwners",java.util.Arrays.asList("ESCAPED_PUBLIC_SCORE_ARRAYS_NOT_REUSED_OR_DECLARED_FREED","PUBLIC_SCORE_RESULT_ALLOCATION_ESTIMATES_NOT_FULLY_REGISTERED","MODEL_KERNEL_PER_STEP_TEMPORARIES_AND_DYNAMIC_PRIVATE_STATE_NOT_FULLY_REGISTERED","LEAF_INTERNAL_TEMPORARY_SCRATCH"));return out;}
    public Map<String,Object> trimUnusedR31(){synchronized(this){clearRawR31();if(scratchPoolR31!=null)scratchPoolR31.trimUnused();}Map<String,Object> result=sharedR31.evictTo(0);reapReleasedArraysR31();return result;}
    public static Map<String,Object> trimRegisteredCachesR31(){return trimRegisteredCachesR31(MemoryBudgetR31.processBudget());}
    private static Map<String,Object> trimRegisteredCachesR31(MemoryBudgetR31 memory){java.util.List<FeatureCacheR27> caches;synchronized(REGISTERED_R31){caches=new java.util.ArrayList<>(REGISTERED_R31.keySet());}boolean reuse="NORMAL".equals(memory.snapshot().get("pressure"));int trimmed=0;for(FeatureCacheR27 cache:caches)if(cache.memory==memory){if(reuse)cache.sharedR31.recycleUnpinnedR31();else cache.trimUnusedR31();trimmed++;}if(memory==MemoryBudgetR31.processBudget()){if(reuse)SharedHolderR31.STORE.recycleUnpinnedR31();else SharedHolderR31.STORE.evictTo(0);}int scoreContexts=EvalContextR27.trimRegisteredScoresR31(memory);boolean teacherReleased=memory==MemoryBudgetR31.processBudget()&&com.ethk3.novaonepro.LegacyTeacherOwnerR26.releaseUnusedOwnerR31();reapReleasedArraysR31();Map<String,Object> out=new LinkedHashMap<>();out.put("registeredCachesTrimmed",trimmed);out.put("scoreContextsTrimmed",scoreContexts);out.put("unusedOriginalTeacherOwnerAbsentAfterTrim",teacherReleased);out.put("actualHeapReleasedBytes",null);out.put("evidence",reuse?"PRIVATE_BACKING_RETAINED_FOR_BOUNDED_REUSE; NOT_HEAP_FREE":"UNPINNED_CACHE_REFERENCES_REMOVED; RAW_ARRAY_LEASES_WAIT_FOR_OBSERVED_GC");return out;}
    @Override public synchronized void close(){closedR31=true;clearRawR31();if(scratchPoolR31!=null)scratchPoolR31.close();for(FamilyStream stream:streams.values())stream.closeForkOwnerR31();streams.clear();modelHistoryHashes.clear();overrides.clear();reapReleasedArraysR31();}
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
        memory.registerReclaimer(RAW_RECLAIMER_R31);
        memory.registerPressureReclaimerR31("FEATURE_AND_SCORE_CACHE",()->trimRegisteredCachesR31(memory));
        cache=new LinkedHashMap<String,double[]>(64,.75f,true);REGISTERED_R31.put(this,Boolean.TRUE);
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
        MemoryBudgetR31.Admission admission=memory.tryAcquire("raw-leaf:"+data.datasetHash.substring(0,16),Boolean.TRUE.equals(CRITICAL_ALLOCATION_R31.get())?MemoryBudgetR31.Kind.REALTIME:MemoryBudgetR31.Kind.FEATURE_CACHE,128L+80L*data.size(),()->Thread.currentThread().isInterrupted());
        if(!admission.acquired())throw leafCapacityWaitR31(admission.reason,kind,tp,a,b);
        double[] v;
        try{v=compute(kind,tp,a,b);if(v.length!=data.size()*10)throw new IllegalStateException("R31_LEAF_LENGTH");admission.lease.allocatedPrimitiveArrayR31(v);trackRawR31(v,admission.lease);}
        catch(RuntimeException|Error failure){admission.lease.close();throw failure;}
        synchronized (this) { double[] raced = cache.get(key); if (raced != null) return raced; computed++;if(!closedR31){cache.put(key,v);rawBytes+=arrayBytesR31(v);trimRawR31();} }
        return v;
    }

    double[] leafWithPriorityR31(String kind,int tp,int a,int b,boolean critical){Boolean old=CRITICAL_ALLOCATION_R31.get();if(critical)CRITICAL_ALLOCATION_R31.set(true);try{return leaf(kind,tp,a,b);}finally{if(critical){if(old==null)CRITICAL_ALLOCATION_R31.remove();else CRITICAL_ALLOCATION_R31.set(old);}}}
    public SharedFeatureStoreR31.Reader acquireLeafR31(String kind,int tp,int a,int b){return acquireLeafR31(kind,tp,a,b,false);}
    SharedFeatureStoreR31.Reader acquireLeafR31(String kind,int tp,int a,int b,boolean critical){Boolean old=CRITICAL_ALLOCATION_R31.get();if(critical)CRITICAL_ALLOCATION_R31.set(true);try{return acquireLeafImplR31(kind,tp,a,b);}finally{if(critical){if(old==null)CRITICAL_ALLOCATION_R31.remove();else CRITICAL_ALLOCATION_R31.set(old);}}}
    private SharedFeatureStoreR31.Reader acquireLeafImplR31(String kind,int tp,int a,int b) {
        if(!targetDependent(kind))tp=0;final int target=tp;String local=kind+'|'+tp+'|'+a+'|'+b;
        final SharedFeatureStoreR31 store;final SharedFeatureStoreR31.Key key;final BuildLockR31 build;
        synchronized(this){if(closedR31)throw new IllegalStateException("R31_FEATURE_CACHE_CLOSED");if(!overrides.isEmpty()||kind.equals("model_surface")||kind.equals("family")||data.size()==0)return null;store=sharedR31;key=new SharedFeatureStoreR31.Key(data.datasetHash,Math.max(0,data.ordinals[data.size()-1]-1),featureAbiR31,"RAW_CAUSAL_LEAF_V1",local);SharedFeatureStoreR31.Reader hit=store.acquire(key,"ast-leaf-reader");if(hit!=null){sharedReads++;return hit;}if(buildLocksR31.size()>=128&&!buildLocksR31.containsKey(local))throw new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.OWNER_LIMIT);build=buildLocksR31.computeIfAbsent(local,k->new BuildLockR31());build.users++;}
        boolean locked=false;
        try{
            while(!(locked=build.lock.tryLock(20,TimeUnit.MILLISECONDS)))CooperativeContinuationR26.poll();
            CooperativeContinuationR26.poll();SharedFeatureStoreR31.Reader hit=store.acquire(key,"ast-leaf-reader");if(hit!=null){synchronized(this){sharedReads++;}return hit;}
            SharedFeatureStoreR31.Publication publication=store.fillPrivateDoublesR31("immutable-leaf:"+data.datasetHash.substring(0,16),key,data.size()*10,values->computeBuiltinIntoR31(kind,target,a,b,values),()->Thread.currentThread().isInterrupted(),Boolean.TRUE.equals(CRITICAL_ALLOCATION_R31.get()));
            if(!publication.acquired())throw leafCapacityWaitR31(publication.reason,kind,tp,a,b);synchronized(this){sharedBuilds++;}return publication.reader;
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
        BuiltinTargetR31 outer=BUILTIN_TARGET_R31.get();if(outer!=null)BUILTIN_TARGET_R31.remove();
        try{
        LeafOverride o;
        synchronized (this) { o = overrides.get(kind); }
        if (o != null) return o.compute(this, tp, a, b);
        return computeBuiltinR31(kind,tp,a,b);
        }finally{if(outer!=null)BUILTIN_TARGET_R31.set(outer);}
    }

    private void computeBuiltinIntoR31(String kind,int tp,int a,int b,double[] target){
        if(kind.equals("family")||kind.equals("model_surface"))throw new IllegalArgumentException("R31_MUTABLE_MODEL_OUTPUT_NOT_POOLABLE");
        BuiltinTargetR31 previous=BUILTIN_TARGET_R31.get(),current=new BuiltinTargetR31(this,target);BUILTIN_TARGET_R31.set(current);
        try{double[] result=computeBuiltinR31(kind,tp,a,b);if(!current.used||result!=target)throw new IllegalStateException("R31_BUILTIN_TARGET_NOT_EXCLUSIVE");}
        finally{if(previous==null)BUILTIN_TARGET_R31.remove();else BUILTIN_TARGET_R31.set(previous);}
    }
    private boolean privateFillR31(){BuiltinTargetR31 target=BUILTIN_TARGET_R31.get();return target!=null&&target.owner==this;}
    private SharedFeatureStoreR31.Reader privateDependencyR31(String kind,int tp,int a,int b){SharedFeatureStoreR31.Reader reader=acquireLeafR31(kind,tp,a,b);if(reader==null)throw new java.util.concurrent.CancellationException("R31_PRIVATE_INPUT_SOURCE_CHANGED");return reader;}
    private MemoryBudgetR31.CapacityWait leafCapacityWaitR31(MemoryBudgetR31.WaitReason reason,String kind,int tp,int a,int b){MemoryBudgetR31.CapacityWait wait=new MemoryBudgetR31.CapacityWait(reason);wait.addSuppressed(new IllegalStateException("R31_LEAF_ADMISSION kind="+kind.substring(0,Math.min(48,kind.length()))+" tp="+tp+" a="+a+" b="+b+" rows="+data.size()));return wait;}

    // Shared ownership transfer is restricted to new private built-in outputs. A concurrent
    // test/operator override may return an externally aliased array and must never be adopted.
    private double[] computeBuiltinR31(String kind,int tp,int a,int b){
        switch (kind) {
            case "set_intersection": return exactIntersection(tp,a,b);
            case "set_intersection_when_repeat": {double[] out=exactIntersection(tp,a,b);if(privateFillR31()){try(SharedFeatureStoreR31.Reader state=privateDependencyR31("regime",tp,0,b)){for(int row=0;row<data.size();row++)if(row<b||!data.consecutive(row,b)||!Double.isFinite(state.doubleAt(row*10))||state.doubleAt(row*10)<=.1)java.util.Arrays.fill(out,row*10,row*10+10,Double.NaN);}}else{double[] state=leaf("regime",tp,0,b);for(int row=0;row<data.size();row++)if(row<b||!data.consecutive(row,b)||!Double.isFinite(state[row*10])||state[row*10]<=.1)java.util.Arrays.fill(out,row*10,row*10+10,Double.NaN);}return out;}
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
    private double[] leafOutputR31(int length,boolean unavailable){BuiltinTargetR31 target=BUILTIN_TARGET_R31.get();double[] values;if(target!=null&&target.owner==this&&!target.used){if(target.values.length!=length)throw new IllegalStateException("R31_BUILTIN_TARGET_LENGTH");target.used=true;values=target.values;}else values=new double[length];Arrays.fill(values,unavailable?Double.NaN:0.0);return values;}
    private double[] nan() {return leafOutputR31(t()*10,true);}

    double[] exactIntersection(int position,int shortWindow,int longWindow){
        double[] out=nan();if(!PolicyAstR27.pos(position)||!PolicyAstR27.window(shortWindow)||!PolicyAstR27.window(longWindow)||shortWindow>=longWindow)throw new IllegalArgumentException("SET_SOURCE_CONTRACT");
        if(privateFillR31()){try(SharedFeatureStoreR31.Reader a=privateDependencyR31("freq",position,position,shortWindow);SharedFeatureStoreR31.Reader b=privateDependencyR31("freqnb",position,position,longWindow)){double[] ar=new double[10],br=new double[10];for(int row=longWindow;row<data.size();row++){if((row&31)==0)CooperativeContinuationR26.poll();if(!data.consecutive(row,longWindow))continue;a.copyRangeTo(row*10,ar,0,10);b.copyRangeTo(row*10,br,0,10);int mask=SetIntersectionPolicyR30.mask(ar,0)&SetIntersectionPolicyR30.mask(br,0);if(Integer.bitCount(mask)!=3)continue;for(int k=0;k<10;k++)out[row*10+k]=(mask&(1<<k))!=0?1.0/3:0;}return out;}}
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
        if(privateFillR31()){try(SharedFeatureStoreR31.Reader l=privateDependencyR31("freq",0,left,w);SharedFeatureStoreR31.Reader r=privateDependencyR31("freq",0,right,w)){double[] v=leafOutputR31(l.length(),false);for(int i=0;i<v.length;i++)v[i]=0.5*(l.doubleAt(i)+r.doubleAt(i));return v;}}
        double[] l = leaf("freq", 0, left, w), r = leaf("freq", 0, right, w), v = leafOutputR31(l.length,false);
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
        if(privateFillR31()){try(SharedFeatureStoreR31.Reader f=privateDependencyR31("freq",0,src,w)){double[] v=leafOutputR31(f.length(),false);for(int t=0;t<t();t++){int len=Math.min(w,t);for(int k=0;k<10;k++){double x=f.doubleAt(t*10+k);v[t*10+k]=Double.isNaN(x)?Double.NaN:Math.min(1.0,len/(x*len+1.0)/30.0);}}return v;}}
        double[] f = leaf("freq", 0, src, w), v = leafOutputR31(f.length,false);
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
        return familyCopyR31(tp,index,null);
    }
    void familyIntoR31(int tp,int index,double[] target){if(target.length!=data.size()*10)throw new IllegalArgumentException("R31_FAMILY_OUTPUT_LENGTH");familyCopyR31(tp,index,target);}
    void familyIntoR31(int tp,int index,double[] target,boolean critical){Boolean prior=CRITICAL_ALLOCATION_R31.get();if(critical)CRITICAL_ALLOCATION_R31.set(true);try{familyIntoR31(tp,index,target);}finally{if(critical){if(prior==null)CRITICAL_ALLOCATION_R31.remove();else CRITICAL_ALLOCATION_R31.set(prior);}}}
    private double[] familyCopyR31(int tp,int index,double[] target){
        FamilyStream st;
        synchronized(this){if(closedR31)throw new java.util.concurrent.CancellationException("R31_FEATURE_CACHE_CLOSED");st=streams.get(index);}
        if(st==null){FamilyStream created=new FamilyStream(index,memory);synchronized(this){if(closedR31){created.closeForkOwnerR31();throw new java.util.concurrent.CancellationException("R31_FEATURE_CACHE_CLOSED");}st=streams.get(index);if(st==null){st=created;streams.put(index,st);}else created.closeForkOwnerR31();}}
        st.beginUseR31();
        try{
        double[] all;
        try { synchronized (st) { all = st.advance(data); } }
        catch (IllegalStateException prefixChanged) {
            // R30-B03-04: rows [0,fed) changed under a reused stream: rebuild the whole stream from this dataset and record it.
            if (!String.valueOf(prefixChanged.getMessage()).startsWith("R27_FAMILY_STREAM") && !String.valueOf(prefixChanged.getMessage()).startsWith("R30_FAMILY_PREFIX")) throw prefixChanged;
            FamilyStream fresh=new FamilyStream(index,memory),replaced=st;boolean installed=false;
            synchronized(this){if(closedR31){fresh.closeForkOwnerR31();throw new java.util.concurrent.CancellationException("R31_FEATURE_CACHE_CLOSED");}FamilyStream current=streams.get(index);if(current==replaced){streams.put(index,fresh);installed=true;}else{fresh.closeForkOwnerR31();fresh=current;}}
            if(installed){replaced.closeForkOwnerR31();REBUILDS_R30.incrementAndGet();if(REBUILD_EVENTS_R30.size()<256)REBUILD_EVENTS_R30.add(new Object[]{index,data.size()==0?-1L:data.ordinals[data.size()-1],String.valueOf(prefixChanged.getMessage()),System.currentTimeMillis()});}
            fresh.beginUseR31();st.endUseR31();st=fresh;
            synchronized(fresh){all=fresh.advance(data);}
        }
        int n = t(); double[] v = target==null?new double[n * 10]:target;
        for (int t = 0; t < n; t++) System.arraycopy(all, t * 100 + (tp - 1) * 10, v, t * 10, 10);
        return v;
        }finally{st.endUseR31();}
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

    private static final class FamilyOwnerR31 {
        final MemoryBudgetR31 memory;final MemoryBudgetR31.Lease model;
        MemoryBudgetR31.Lease currentMatrix,pendingMatrix,checkpoint;DistillationFamilyR26 teacherStudent;
        FamilyOwnerR31(MemoryBudgetR31 memory,MemoryBudgetR31.Lease model){this.memory=memory;this.model=model;}
        synchronized void close(){if(teacherStudent!=null){teacherStudent.close();teacherStudent=null;}model.close();if(currentMatrix!=null)currentMatrix.close();if(pendingMatrix!=null)pendingMatrix.close();if(checkpoint!=null)checkpoint.close();}
    }
    /** Dynamic teacher adapters must declare their own constructor/read workspace before construction. */
    public static synchronized void registerFamilyConstructorBudgetR31(int family,long bytes,String basis){
        if(family!=23||bytes<1024||bytes>(64L<<20)||basis==null||basis.isEmpty()||basis.length()>192)throw new IllegalArgumentException("R31_FAMILY_CONSTRUCTOR_CONTRACT");
        Long old=FAMILY_CONSTRUCTOR_BUDGETS_R31.get(family);if(old!=null&&old!=bytes)throw new IllegalStateException("R31_FAMILY_CONSTRUCTOR_BUDGET_LOCKED");FAMILY_CONSTRUCTOR_BUDGETS_R31.put(family,bytes);
    }
    private static final Map<Integer,Long> FAMILY_CONSTRUCTOR_BUDGETS_R31=new LinkedHashMap<>();
    static final class FamilyStream {
        final int index;final OnlineModelsSU2.Model model;int fed;double[] values=new double[0];long lastFedOrdinal=Long.MIN_VALUE;
        final float[][] p=new float[10][10];final float[] st;int[] lastFedDraw;
        private final java.util.concurrent.atomic.AtomicInteger activeUsesR31=new java.util.concurrent.atomic.AtomicInteger();
        private volatile boolean closingR31;
        private final FamilyOwnerR31 ownerR31;private final long constructorBoundR31;
        private final WeakReference<FamilyStream> ownerReferenceR31;
        // The pending matrix is private and survives cancellation. Only complete matrices are published as values.
        private double[] pendingValuesR31;private int pendingNextRowR31,pendingKnownRowsR31;
        private ReplayDatasetR27 pendingDataR31;private long pendingPrefixHashR31;
        private byte[] rowCheckpointR31;private int rowCheckpointLengthR31;
        private final float[][] rollbackP_R31=new float[10][10];private final float[] rollbackStateR31;
        FamilyStream(int index){this(index,MemoryBudgetR31.processBudget());}
        FamilyStream(int index,MemoryBudgetR31 memory){this(index,memory,admitConstructorR31(index,memory));}
        private FamilyStream(int index,MemoryBudgetR31 memory,MemoryBudgetR31.Lease reservation){
            boolean adopted=false;this.index=index;constructorBoundR31=constructorBoundR31(index);ownerR31=new FamilyOwnerR31(memory,reservation);
            try{OnlineModelsSU2.Model prepared;if(index==23){try(DistillationFamilyR26.ConstructorScopeR31 scope=DistillationFamilyR26.useConstructorReservationR31(reservation)){prepared=FamilyRegistryR26.build("F23");}}else prepared=FamilyRegistryR26.build(String.format(java.util.Locale.ROOT,"F%02d",index));model=prepared;if(model instanceof DistillationFamilyR26)ownerR31.teacherStudent=(DistillationFamilyR26)model;st=new float[Math.max(1,model.stateWidth())];rollbackStateR31=new float[st.length];if(modelShapeR31(model)+2048>constructorBoundR31)throw new IllegalStateException("R31_FAMILY_CONSTRUCTOR_SHAPE_DRIFT:F"+index);
                ownerReferenceR31=new WeakReference<>(this,FAMILY_FORK_QUEUE_R31);synchronized(FAMILY_FORK_LEASES_R31){if(FAMILY_FORK_LEASES_R31.size()>=4096)throw new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.OWNER_LIMIT);FAMILY_FORK_LEASES_R31.put(ownerReferenceR31,ownerR31);}adopted=true;
            }finally{if(!adopted)ownerR31.close();}
        }
        /** Source-derived constructor envelopes, checked against all 23 actual built-in object graphs.
         * Includes NeuralFamily's nonserialized 64000-node tape. Kernel per-step temporary allocations are separate debt. */
        private static long constructorBoundR31(int index){
            switch(index){case 1:return 315392;case 2:return 40960;case 3:case 4:case 5:case 6:case 7:return 2674688;
                case 8:case 9:return 16384;case 10:return 364544;case 11:return 65536;case 12:return 184320;case 13:case 14:return 32768;
                case 15:case 16:case 24:return 372736;case 17:return 167936;case 18:return 28672;case 19:return 20480;case 20:return 98304;case 21:return 28672;case 22:return 323584;
                case 23:synchronized(FeatureCacheR27.class){Long bound=FAMILY_CONSTRUCTOR_BUDGETS_R31.get(23);if(bound!=null)return bound;}throw new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.UNKNOWN_CAPACITY);
                default:throw new IllegalArgumentException("R26_FAMILY_IMPLEMENTATION_BLOCKED:F"+index);}
        }
        private static MemoryBudgetR31.Kind allocationKindR31(){return Boolean.TRUE.equals(CRITICAL_ALLOCATION_R31.get())?MemoryBudgetR31.Kind.REALTIME:MemoryBudgetR31.Kind.MODEL;}
        private static MemoryBudgetR31.Lease admitConstructorR31(int index,MemoryBudgetR31 memory){return acquireR31(memory,"family-model:F"+index,constructorBoundR31(index));}
        private static MemoryBudgetR31.Lease acquireR31(MemoryBudgetR31 memory,String name,long bytes){MemoryBudgetR31.Admission admission=memory.tryAcquire(name,allocationKindR31(),bytes,()->Thread.currentThread().isInterrupted());if(!admission.acquired())throw new MemoryBudgetR31.CapacityWait(admission.reason);return admission.lease;}
        void beginUseR31(){if(closingR31)throw new java.util.concurrent.CancellationException("R31_FAMILY_OWNER_CLOSED");activeUsesR31.incrementAndGet();if(closingR31){endUseR31();throw new java.util.concurrent.CancellationException("R31_FAMILY_OWNER_CLOSED");}}
        void endUseR31(){int remaining=activeUsesR31.decrementAndGet();if(remaining<0)throw new IllegalStateException("R31_FAMILY_OWNER_REFERENCE_COUNT");if(remaining==0&&closingR31)releaseOwnerR31();}
        void closeForkOwnerR31(){closingR31=true;if(activeUsesR31.get()==0)releaseOwnerR31();}
        private void releaseOwnerR31(){synchronized(FAMILY_FORK_LEASES_R31){FAMILY_FORK_LEASES_R31.remove(ownerReferenceR31);}synchronized(this){values=new double[0];pendingValuesR31=null;pendingDataR31=null;rowCheckpointR31=null;ownerR31.close();}}
        private static final class CountOutputR31 extends java.io.OutputStream {long bytes;@Override public void write(int value){bytes++;}@Override public void write(byte[] values,int offset,int length){bytes+=length;}}
        private static long modelShapeR31(Object root){
            java.util.IdentityHashMap<Object,Boolean> seen=new java.util.IdentityHashMap<>();java.util.ArrayDeque<Object> pending=new java.util.ArrayDeque<>();pending.add(root);Object borrowedTeacherIdentity=root instanceof DistillationFamilyR26?((DistillationFamilyR26)root).sharedTeacherIdentityR31():null;long bytes=0;
            try{while(!pending.isEmpty()){Object object=pending.removeFirst();if(object==borrowedTeacherIdentity||seen.put(object,Boolean.TRUE)!=null)continue;if(object instanceof MemoryBudgetR31.Lease){bytes+=128;continue;}if(object instanceof DistillationFamilyR26.MemoryBoundTeacherR31){DistillationFamilyR26.MemoryBoundTeacherR31 reference=(DistillationFamilyR26.MemoryBoundTeacherR31)object;MemoryBudgetR31.Lease shared=reference.sharedOwnerLeaseR31();if(shared==null||shared.isClosed())throw new java.util.concurrent.CancellationException("F23_SHARED_OWNER_CLOSED");long local=reference.ownedReferenceBytesR31();if(local<64||local>1024)throw new IllegalStateException("F23_REFERENCE_ENVELOPE");bytes+=local;continue;}if(seen.size()>200000)throw new IllegalStateException("R31_FAMILY_MODEL_SHAPE_BOUND");Class<?> type=object.getClass();if(type.isArray()){int length=java.lang.reflect.Array.getLength(object);Class<?> component=type.getComponentType();if(component.isPrimitive()){int width=component==double.class||component==long.class?8:component==int.class||component==float.class?4:component==short.class||component==char.class?2:1;bytes+=32L+(long)width*length;}else{bytes+=32L+8L*length;for(Object value:(Object[])object)if(value!=null)pending.add(value);}continue;}
                if(object instanceof String){bytes+=64L+2L*((String)object).length();continue;}if(object instanceof Number||object instanceof Boolean||object instanceof Character||object instanceof Enum<?>){bytes+=64;continue;}if(object instanceof Map<?,?>){bytes+=128L+96L*((Map<?,?>)object).size();for(Map.Entry<?,?> e:((Map<?,?>)object).entrySet()){if(e.getKey()!=null)pending.add(e.getKey());if(e.getValue()!=null)pending.add(e.getValue());}continue;}if(object instanceof Collection<?>){bytes+=128L+32L*((Collection<?>)object).size();for(Object value:(Collection<?>)object)if(value!=null)pending.add(value);continue;}
                if(type.getName().startsWith("java.")||type.getName().startsWith("android."))throw new IllegalStateException("R31_FAMILY_MODEL_SHAPE_UNSUPPORTED:"+type.getName());bytes+=64;for(Class<?> owner=type;owner!=null&&owner!=Object.class;owner=owner.getSuperclass())for(java.lang.reflect.Field field:owner.getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())){bytes+=8;if(!field.getType().isPrimitive()){field.setAccessible(true);Object value=field.get(object);if(value!=null)pending.add(value);}}
            }return bytes;}catch(IllegalAccessException inaccessible){throw new IllegalStateException("R31_FAMILY_MODEL_SHAPE_INACCESSIBLE",inaccessible);}
        }
        private static final class MatrixR31 {final double[] values;final MemoryBudgetR31.Lease lease;MatrixR31(double[] values,MemoryBudgetR31.Lease lease){this.values=values;this.lease=lease;}}
        private MatrixR31 copyMatrixR31(double[] source,int length,String phase){
            MemoryBudgetR31.Lease lease=acquireR31(ownerR31.memory,"family-matrix-"+phase+":F"+index,128L+8L*length);boolean adopted=false;
            try{double[] copy=Arrays.copyOf(source,length);lease.allocatedPrimitiveArrayR31(copy);lease.closeWhenPrivateBackingCollectedR31(copy);adopted=true;return new MatrixR31(copy,lease);}finally{if(!adopted)lease.close();}
        }
        FamilyStream forkR31(MemoryBudgetR31 memory){
            beginUseR31();FamilyStream copy=null;boolean adopted=false;
            try{synchronized(this){CountOutputR31 count=new CountOutputR31();model.write(new java.io.DataOutputStream(count));if(count.bytes>Integer.MAX_VALUE-8)throw new IllegalStateException("R31_FAMILY_FORK_CODEC_TOO_LARGE:F"+index);
                try(MemoryBudgetR31.Lease transientCodec=acquireR31(memory,"family-fork-codec:F"+index,2L*constructorBoundR31+2*count.bytes+8192)){
                    java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream((int)count.bytes);model.write(new java.io.DataOutputStream(bytes));if(bytes.size()!=count.bytes)throw new java.io.IOException("R31_FAMILY_FORK_CODEC_LENGTH");copy=new FamilyStream(index,memory);
                    java.io.DataInputStream restored=new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()));if(copy.model instanceof DistillationFamilyR26){try(DistillationFamilyR26.ReadScopeR31 scope=DistillationFamilyR26.useReadReservationR31(transientCodec)){copy.model.read(restored);}}else copy.model.read(restored);if(restored.available()!=0)throw new java.io.IOException("R31_FAMILY_FORK_CODEC_TRAILING");System.arraycopy(st,0,copy.st,0,st.length);for(int i=0;i<p.length;i++)System.arraycopy(p[i],0,copy.p[i],0,p[i].length);copy.fed=fed;copy.lastFedOrdinal=lastFedOrdinal;copy.lastFedDraw=lastFedDraw==null?null:lastFedDraw.clone();
                    MatrixR31 matrix=copy.copyMatrixR31(values,values.length,"fork");copy.values=matrix.values;copy.ownerR31.currentMatrix=matrix.lease;copy.checkedFed=checkedFed;copy.checkedHash=checkedHash;copy.verifiedData=null;
                    if(pendingValuesR31!=null){MatrixR31 pending=copy.copyMatrixR31(pendingValuesR31,pendingValuesR31.length,"fork-pending");copy.pendingValuesR31=pending.values;copy.ownerR31.pendingMatrix=pending.lease;copy.pendingNextRowR31=pendingNextRowR31;copy.pendingKnownRowsR31=pendingKnownRowsR31;copy.pendingPrefixHashR31=pendingPrefixHashR31;copy.pendingDataR31=pendingDataR31;}
                    adopted=true;return copy;
                }
            }}catch(java.io.IOException failure){throw new IllegalStateException("R31_FAMILY_FORK_CODEC:F"+index,failure);}finally{if(!adopted&&copy!=null)copy.closeForkOwnerR31();endUseR31();}
        }
        private static final class CodecFullR31 extends java.io.IOException {private static final long serialVersionUID=1L;}
        private static final class RowOutputR31 extends java.io.OutputStream implements java.io.DataOutput {
            byte[] bytes;java.nio.ByteBuffer buffer;
            void reset(byte[] target){if(bytes!=target){bytes=target;buffer=java.nio.ByteBuffer.wrap(target);}else buffer.clear();}
            private void room(int count)throws java.io.IOException{if(count>buffer.remaining())throw new CodecFullR31();}
            @Override public void write(int value)throws java.io.IOException{room(1);buffer.put((byte)value);}
            @Override public void write(byte[] source)throws java.io.IOException{write(source,0,source.length);}
            @Override public void write(byte[] source,int offset,int length)throws java.io.IOException{room(length);buffer.put(source,offset,length);}
            public void writeBoolean(boolean value)throws java.io.IOException{write(value?1:0);}
            public void writeByte(int value)throws java.io.IOException{write(value);}
            public void writeShort(int value)throws java.io.IOException{room(2);buffer.putShort((short)value);}
            public void writeChar(int value)throws java.io.IOException{room(2);buffer.putChar((char)value);}
            public void writeInt(int value)throws java.io.IOException{room(4);buffer.putInt(value);}
            public void writeLong(long value)throws java.io.IOException{room(8);buffer.putLong(value);}
            public void writeFloat(float value)throws java.io.IOException{writeInt(Float.floatToIntBits(value));}
            public void writeDouble(double value)throws java.io.IOException{writeLong(Double.doubleToLongBits(value));}
            public void writeBytes(String text)throws java.io.IOException{for(int i=0;i<text.length();i++)write(text.charAt(i));}
            public void writeChars(String text)throws java.io.IOException{for(int i=0;i<text.length();i++)writeChar(text.charAt(i));}
            public void writeUTF(String text)throws java.io.IOException{int length=0;for(int i=0;i<text.length();i++){int c=text.charAt(i);length+=c>=1&&c<=127?1:c<=2047?2:3;}if(length>65535)throw new java.io.UTFDataFormatException("R31_FAMILY_ROW_UTF");writeShort(length);for(int i=0;i<text.length();i++){int c=text.charAt(i);if(c>=1&&c<=127)write(c);else if(c<=2047){write(192|c>>6);write(128|c&63);}else{write(224|c>>12);write(128|c>>6&63);write(128|c&63);}}}
        }
        private void saveRowR31(RowOutputR31 target)throws java.io.IOException{
            for(;;){if(rowCheckpointR31!=null){target.reset(rowCheckpointR31);try{model.write(target);rowCheckpointLengthR31=target.buffer.position();break;}catch(CodecFullR31 full){/* model was not mutated; grow the private codec before retrying serialization */}}
                CountOutputR31 count=new CountOutputR31();model.write(new java.io.DataOutputStream(count));long bound=(count.bytes+4095)/4096*4096;if(bound<1||bound>Integer.MAX_VALUE-8||bound>4*constructorBoundR31)throw new IllegalStateException("R31_FAMILY_CHECKPOINT_SHAPE:F"+index);
                MemoryBudgetR31.Lease lease=acquireR31(ownerR31.memory,"family-row-checkpoint:F"+index,128+bound);boolean adopted=false;
                try{byte[] bytes=new byte[(int)bound];lease.allocatedPrimitiveArrayR31(bytes);lease.closeWhenPrivateBackingCollectedR31(bytes);rowCheckpointR31=bytes;ownerR31.checkpoint=lease;adopted=true;}finally{if(!adopted)lease.close();}
            }
            System.arraycopy(st,0,rollbackStateR31,0,st.length);for(int i=0;i<p.length;i++)System.arraycopy(p[i],0,rollbackP_R31[i],0,p[i].length);
        }
        private void restoreRowR31(RuntimeException failure,MemoryBudgetR31.Lease reserved){
            try{java.io.DataInputStream input=new java.io.DataInputStream(new java.io.ByteArrayInputStream(rowCheckpointR31,0,rowCheckpointLengthR31));if(model instanceof DistillationFamilyR26){try(DistillationFamilyR26.ReadScopeR31 scope=DistillationFamilyR26.useReadReservationR31(reserved)){model.read(input);}}else model.read(input);if(input.available()!=0)throw new java.io.IOException("R31_FAMILY_ROW_CODEC_TRAILING");System.arraycopy(rollbackStateR31,0,st,0,st.length);for(int i=0;i<p.length;i++)System.arraycopy(rollbackP_R31[i],0,p[i],0,p[i].length);}
            catch(java.io.IOException|RuntimeException corrupt){failure.addSuppressed(corrupt);throw new IllegalStateException("R31_FAMILY_ROW_RECOVERY_FAILED:F"+index,failure);}
        }
        int checkedFed;long checkedHash;ReplayDatasetR27 verifiedData;
        /** Completed row progress and its private output survive cancellation. A model call either commits one row or restores its pre-row codec. */
        double[] advance(ReplayDatasetR27 d){
            int n=d.size();
            if(fed>0&&(n<fed||d.ordinals[fed-1]!=lastFedOrdinal||!Arrays.equals(d.draws[fed-1],lastFedDraw)))throw new IllegalStateException("R27_FAMILY_STREAM_PREFIX_CHANGED");
            if(checkedFed>0&&d!=verifiedData){if(n<checkedFed||prefixHashR30(d,checkedFed)!=checkedHash)throw new IllegalStateException("R30_FAMILY_PREFIX_HASH_CHANGED");verifiedData=d;}
            int known=values.length/100;if(known>n)throw new IllegalStateException("R27_FAMILY_STREAM_SHRUNK");
            if(pendingValuesR31==null&&known==n)return values;
            if(pendingValuesR31!=null&&pendingDataR31!=d){if(n<pendingNextRowR31||prefixHashR30(d,fed)!=pendingPrefixHashR31)throw new IllegalStateException("R30_FAMILY_PREFIX_PENDING_CHANGED");pendingDataR31=d;}
            if(pendingValuesR31==null){MatrixR31 matrix=copyMatrixR31(values,n*100,"initial-or-grow");pendingValuesR31=matrix.values;ownerR31.pendingMatrix=matrix.lease;pendingNextRowR31=fed;pendingKnownRowsR31=known;pendingDataR31=d;pendingPrefixHashR31=prefixHashR30(d,fed);}
            else if(pendingValuesR31.length!=n*100){MatrixR31 matrix=copyMatrixR31(pendingValuesR31,n*100,"pending-grow");pendingValuesR31=matrix.values;ownerR31.pendingMatrix=matrix.lease;}
            // Recovery allocations must be admitted while there is still a valid pre-row model,
            // rather than attempting an unbudgeted read after cancellation or memory pressure.
            try(MemoryBudgetR31.Lease restoreWorkspace=acquireR31(ownerR31.memory,"family-row-restore:F"+index,2*constructorBoundR31)){
                RowOutputR31 target=new RowOutputR31();
                for(int t=pendingNextRowR31;t<n;t++){
                    if(closingR31)throw new java.util.concurrent.CancellationException("R31_FAMILY_OWNER_CLOSED");
                    CooperativeContinuationR26.poll();saveRowR31(target);
                    try{
                        if(t>=pendingKnownRowsR31){if(t==0)Arrays.fill(pendingValuesR31,0,100,Double.NaN);else{if(!d.consecutive(t,1)&&model instanceof TemporalModelR26)((TemporalModelR26)model).resetTemporalContext();model.predict(p,st);for(int q=0;q<10;q++)for(int k=0;k<10;k++){double x=p[q][k];pendingValuesR31[t*100+q*10+k]=Double.isFinite(x)?x:Double.NaN;}}}
                        if(t<n-1){if(index==23){try(com.ethk3.novaonepro.LegacyTeacherOwnerR26.RowFrameScopeR31 scope=com.ethk3.novaonepro.LegacyTeacherOwnerR26.rowFrameR31(d.issues[t],d.draws[t])){model.update(d.draws[t]);}}else model.update(d.draws[t]);fed=t+1;lastFedOrdinal=d.ordinals[t];lastFedDraw=d.draws[t].clone();pendingPrefixHashR31=(pendingPrefixHashR31^d.ordinals[t])*0x100000001B3L;for(int v:d.draws[t])pendingPrefixHashR31=(pendingPrefixHashR31^v)*0x100000001B3L;}
                        pendingNextRowR31=t+1;
                    }catch(RuntimeException cancelledOrFailed){restoreRowR31(cancelledOrFailed,restoreWorkspace);throw cancelledOrFailed;}
                }
                values=pendingValuesR31;ownerR31.currentMatrix=ownerR31.pendingMatrix;ownerR31.pendingMatrix=null;pendingValuesR31=null;pendingDataR31=null;
                if(fed>=checkedFed+PREFIX_CHECK_EVERY_R30){checkedFed=fed;checkedHash=prefixHashR30(d,fed);verifiedData=d;}return values;
            }catch(java.io.IOException failure){throw new IllegalStateException("R31_FAMILY_ROW_CODEC:F"+index,failure);}
        }
    }

    /** Explicit live preparation owns the realtime reserve through the complete deep fork,
     * including codec, model construction/read workspace and private matrix copies. History
     * keeps extendFrom's optional admissions; no caller/thread retains this priority afterward. */
    public void extendFromForLiveR31(FeatureCacheR27 older) {
        Boolean prior=CRITICAL_ALLOCATION_R31.get();CRITICAL_ALLOCATION_R31.set(true);
        try{extendFrom(older);}finally{if(prior==null)CRITICAL_ALLOCATION_R31.remove();else CRITICAL_ALLOCATION_R31.set(prior);}
    }

    /** Reuses family streams of an older cache whose dataset is a prefix of this one (live incremental path). */
    public void extendFrom(FeatureCacheR27 older) {
        Map<Integer,FamilyStream> parents;synchronized(older){parents=new LinkedHashMap<>(older.streams);}synchronized(this){if(closedR31)throw new java.util.concurrent.CancellationException("R31_FEATURE_CACHE_CLOSED");if(!streams.isEmpty())throw new IllegalStateException("R31_FAMILY_CHILD_ALREADY_INITIALIZED");}
        Map<Integer,FamilyStream> copies=new LinkedHashMap<>();boolean adopted=false;
        try{for(Map.Entry<Integer,FamilyStream> e:parents.entrySet())copies.put(e.getKey(),e.getValue().forkR31(memory));synchronized(this){if(closedR31||!streams.isEmpty())throw new java.util.concurrent.CancellationException("R31_FAMILY_CHILD_CHANGED_DURING_COPY");streams.putAll(copies);adopted=true;}}
        finally{if(!adopted)for(FamilyStream copy:copies.values())copy.closeForkOwnerR31();}
    }

    /** randu(seed): deterministic uniform noise per (t,n) — the I00 random-baseline input, trivially causal. */
    double[] randomUniform(int seed) {
        int n = t(); double[] v = leafOutputR31(n*10,false);
        for (int t = 0; t < n; t++) for (int k = 0; k < 10; k++) {
            long z = (data.ordinals[t] * 0x9E3779B97F4A7C15L) ^ ((long) seed * 0xBF58476D1CE4E5B9L) ^ (k * 0x94D049BB133111EBL);
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L; z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL; z ^= z >>> 31;
            v[t * 10 + k] = (z >>> 11) * 0x1.0p-53;
        }
        return v;
    }
}
