package com.ethk3.novaonepro.strategyv2;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;

/** Scope ownership and arithmetic tests; controlled Probe values are explicitly numerical test inputs. */
public final class R31ScopedAstLifecycleTest {
    private static MemoryBudgetR31 budget(){return new MemoryBudgetR31(()->new MemoryBudgetR31.Measurement(7,"NUMERIC_ONLY",1000000000,100000,-1,-1,-1,null,System.nanoTime(),0),new MemoryBudgetR31.Config());}
    private static long number(Map<String,Object> map,String key){Object value=map.get(key);return value instanceof Number?((Number)value).longValue():0;}
    private static PolicyAstR27.Node ast(){return PolicyAstR27.op("mix",.3,PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",3,32)),PolicyAstR27.op("ema",.2,PolicyAstR27.op("rank",PolicyAstR27.leaf("gap",2))));}
    @Test public void privateIntermediateBuffersAreReusedButPublicOutputsNeverAlias(){
        MemoryBudgetR31 memory=budget();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,8000000);FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(1000,3181,null),8,memory,store);
        try{double[] first=PolicyAstR27.eval(ast(),cache,3),saved=first.clone();long warm=number(cache.scratchMemorySnapshotR31(),"allocations");for(int i=0;i<40;i++){double[] next=PolicyAstR27.eval(ast(),cache,3);assertNotSame(first,next);assertArrayEquals(saved,next,0);next[9999]=999;assertArrayEquals(saved,first,0);}assertEquals(warm,number(cache.scratchMemorySnapshotR31(),"allocations"));assertTrue(number(cache.scratchMemorySnapshotR31(),"reuses")>=40);assertEquals(0,number(cache.scratchMemorySnapshotR31(),"busyBlocks"));}
        finally{cache.close();store.close();}
    }
    @Test public void closeAndPressureCannotReuseOrReleaseAStillOpenScoreReader(){
        MemoryBudgetR31 memory=budget();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,8000000);FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(1000,3182,null),8,memory,store);PolicyAstR27.ScopedScoresR31 scope=PolicyAstR27.evalScopedR31(ast(),cache,3);double last=scope.at(9999);
        cache.trimUnusedR31();assertEquals(1,number(cache.scratchMemorySnapshotR31(),"busyBlocks"));cache.close();store.close();assertEquals(1,number(memory.snapshot(),"activeLeaseCount"));assertEquals(last,scope.at(9999),0);scope.close();scope.close();assertEquals(0,number(memory.snapshot(),"activeLeaseCount"));try{scope.at(0);fail();}catch(IllegalStateException expected){assertEquals("R31_SCORE_READER_CLOSED",expected.getMessage());}
    }
    @Test public void actualCancelledWorkerUnwindsEveryNestedScratchScopeBeforeReuse()throws Exception{
        MemoryBudgetR31 memory=budget();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,8000000);FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(1000,3183,null),8,memory,store);
        CooperativeContinuationR26<double[]> work=new CooperativeContinuationR26<>("r31-scoped-ast-cancel",()->PolicyAstR27.eval(ast(),cache,3),()->number(cache.scratchMemorySnapshotR31(),"busyBlocks")>0,null,System::nanoTime,"r31-scoped-ast-cancel");
        try{assertFalse(work.slice(200).done);assertTrue(number(cache.scratchMemorySnapshotR31(),"busyBlocks")>0);work.close();assertTrue(work.awaitTermination(2000));assertEquals(0,number(cache.scratchMemorySnapshotR31(),"busyBlocks"));assertEquals(0,number(store.snapshot(),"readerLeases"));cache.trimUnusedR31();assertEquals(0,number(cache.scratchMemorySnapshotR31(),"blocks"));}
        finally{work.close();assertTrue(work.awaitTermination(2000));cache.close();store.close();}
    }
    @Test public void concurrentReadersHaveExclusiveMutableScratchAndExactlyEqualResults()throws Exception{
        MemoryBudgetR31 memory=budget();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,8000000);FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(1000,3184,null),8,memory,store);ExecutorService threads=Executors.newFixedThreadPool(8);List<Future<double[]>> jobs=new ArrayList<>();
        try{double[] exact=PolicyAstR27.eval(ast(),cache,3);IdentityHashMap<double[],Boolean> identities=new IdentityHashMap<>();for(int i=0;i<32;i++)jobs.add(threads.submit(()->PolicyAstR27.eval(ast(),cache,3)));for(Future<double[]> job:jobs){double[] result=job.get();assertArrayEquals(exact,result,0);identities.put(result,Boolean.TRUE);}assertEquals(32,identities.size());assertEquals(0,number(cache.scratchMemorySnapshotR31(),"busyBlocks"));assertTrue(number(cache.scratchMemorySnapshotR31(),"estimatedRetainedBytes")<=number(cache.scratchMemorySnapshotR31(),"byteCapacity"));}
        finally{threads.shutdownNow();assertTrue(threads.awaitTermination(2,TimeUnit.SECONDS));cache.close();store.close();}
    }
    private static final class Orphan {final java.lang.ref.WeakReference<FeatureCacheR27> cache;final PolicyAstR27.ScopedScoresR31 reader;Orphan(FeatureCacheR27 cache,PolicyAstR27.ScopedScoresR31 reader){this.cache=new java.lang.ref.WeakReference<>(cache);this.reader=reader;}}
    private static Orphan orphan(MemoryBudgetR31 memory){SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,8000000);FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(1000,3186,null),8,memory,store);PolicyAstR27.ScopedScoresR31 reader=PolicyAstR27.evalScopedR31(ast(),cache,3);store.close();return new Orphan(cache,reader);}
    @Test public void orphanScratchDeclarationEndsOnlyAfterActualBackingCollection()throws Exception{
        MemoryBudgetR31 memory=budget();Orphan orphan=orphan(memory);double last=orphan.reader.at(9999);
        // Isolated ownership/GC probe permitted by ST-M03; production never requests collection.
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(orphan.cache.get()!=null&&System.nanoTime()<deadline){System.gc();Thread.sleep(10);}assertNull(orphan.cache.get());memory.sample();assertTrue(number(memory.snapshot(),"activeLeaseCount")>0);assertEquals(last,orphan.reader.at(9999),0);orphan.reader.close();
        deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(number(memory.snapshot(),"activeLeaseCount")>0&&System.nanoTime()<deadline){System.gc();Thread.sleep(10);memory.sample();}assertEquals("unreachable private backing must not leave a permanent owner/header reservation",0,number(memory.snapshot(),"activeLeaseCount"));
    }
}
