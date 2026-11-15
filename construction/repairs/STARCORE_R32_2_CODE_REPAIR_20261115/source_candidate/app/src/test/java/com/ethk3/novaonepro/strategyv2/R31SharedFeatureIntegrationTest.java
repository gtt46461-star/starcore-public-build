package com.ethk3.novaonepro.strategyv2;
import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Real AST/scoped-reader consumer tests. Synthetic rows never prove natural predictive quality. */
public final class R31SharedFeatureIntegrationTest {
    private static MemoryBudgetR31 budget(){R31MemoryGovernanceTest.Probe p=new R31MemoryGovernanceTest.Probe();p.max=1_000_000_000;return new MemoryBudgetR31(p,new MemoryBudgetR31.Config());}
    private static long number(Map<String,Object> value,String key){return ((Number)value.get(key)).longValue();}
    @Test public void sixtyFourCandidatesConsumeOneImmutableLeafAndMatchOriginalRowArithmetic() throws Exception {
        MemoryBudgetR31 memory=budget();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,8L<<20);ReplayDatasetR27 data=ReplayDatasetR27.synthetic(1200,3114,new int[]{600});
        FeatureCacheR27 shared=new FeatureCacheR27(data,8,memory,store),reference=new FeatureCacheR27(data,8,memory,store);shared.bindSharedFeaturesR31("actual-golden-abi-test");reference.override("unused-test-disable-shared",(f,tp,a,b)->{throw new AssertionError();});
        ExecutorService threads=Executors.newFixedThreadPool(16);
        try{
            for(String op:new String[]{"rank","zscore","softmax"}){
                PolicyAstR27.Node ast=PolicyAstR27.op(op,2,PolicyAstR27.leaf("freq",3,32));double[] expected=PolicyAstR27.eval(ast,reference,3);List<Future<double[]>> results=new ArrayList<>();
                for(int i=0;i<64;i++)results.add(threads.submit(()->PolicyAstR27.eval(ast,shared,3)));for(Future<double[]> result:results)assertArrayEquals(expected,result.get(),0);
            }
            assertEquals(1,number(shared.memorySnapshotR31(),"sharedBuilds"));assertEquals(1,number(store.snapshot(),"entries"));assertEquals(0,number(store.snapshot(),"readerLeases"));assertTrue(number(shared.memorySnapshotR31(),"sharedReads")>=191);
        }finally{threads.shutdownNow();assertTrue(threads.awaitTermination(3,TimeUnit.SECONDS));shared.close();reference.close();store.close();}
    }
    @Test public void duplicatePublicationRaceReleasesEveryUnadoptedCloneDeclaration() throws Exception {
        CyclicBarrier barrier=new CyclicBarrier(6);AtomicBoolean armed=new AtomicBoolean();MemoryBudgetR31.Probe probe=()->{if(armed.get())try{barrier.await(3,TimeUnit.SECONDS);}catch(Exception failed){throw new AssertionError(failed);}return new MemoryBudgetR31.Measurement(7,"synthetic",100_000_000,100000,-1,-1,-1,null,System.nanoTime(),0);};
        MemoryBudgetR31 memory=new MemoryBudgetR31(probe,new MemoryBudgetR31.Config());SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,1_000_000);SharedFeatureStoreR31.Key key=R31MemoryGovernanceTest.key("racing");double[] source=new double[20000];Arrays.fill(source,.125);armed.set(true);ExecutorService threads=Executors.newFixedThreadPool(32);List<Future<SharedFeatureStoreR31.Publication>> futures=new ArrayList<>();List<SharedFeatureStoreR31.Reader> readers=new ArrayList<>();
        try{for(int i=0;i<32;i++){final int owner=i;futures.add(threads.submit(()->store.publishDoubles("producer"+owner,key,source)));}int retry=0;for(Future<SharedFeatureStoreR31.Publication> future:futures){SharedFeatureStoreR31.Publication p=future.get();if(p.acquired())readers.add(p.reader);else{assertEquals(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY,p.reason);retry++;}}armed.set(false);for(int i=0;i<retry;i++){SharedFeatureStoreR31.Publication p=store.publishDoubles("retry"+i,key,source);assertTrue(p.acquired());readers.add(p.reader);}assertEquals(32,readers.size());assertTrue(number(store.snapshot(),"declaredBytes")<=1000000);assertEquals(1,number(memory.snapshot(),"activeLeaseCount"));assertEquals(1,number(store.snapshot(),"entries"));assertEquals(32,number(store.snapshot(),"readerLeases"));long id=readers.get(0).storageIdentity();for(SharedFeatureStoreR31.Reader reader:readers){assertEquals(id,reader.storageIdentity());assertEquals(.125,reader.doubleAt(19999),0);}}
        finally{armed.set(false);threads.shutdownNow();assertTrue(threads.awaitTermination(3,TimeUnit.SECONDS));for(SharedFeatureStoreR31.Reader reader:readers)reader.close();store.close();}assertEquals(0,number(memory.snapshot(),"activeLeaseCount"));
    }
    @Test public void sharedAstRemainsPointInTimeAndOverridesCannotReuseOldBlocks() {
        ReplayDatasetR27 data=ReplayDatasetR27.synthetic(800,3115,new int[]{500});int[][] replacement=new int[data.size()][];StatsR27.SplitMix rng=new StatsR27.SplitMix(8);for(int i=0;i<replacement.length;i++)replacement[i]=i<300?data.draws[i]:ReplayDatasetR27.randomPermutation(rng);ReplayDatasetR27 altered=data.withDraws(replacement);
        FeatureCacheR27 original=new FeatureCacheR27(data),changed=new FeatureCacheR27(altered);PolicyAstR27.Node ast=PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,32));
        try{double[] before=PolicyAstR27.eval(ast,original,1),after=PolicyAstR27.eval(ast,changed,1);for(int i=0;i<3010;i++)assertEquals(Double.doubleToLongBits(before[i]),Double.doubleToLongBits(after[i]));original.override("freq",(f,tp,a,b)->new double[f.data.size()*10]);double[] overridden=PolicyAstR27.eval(ast,original,1);assertEquals(0,overridden[6000],0);assertTrue((Boolean)original.memorySnapshotR31().get("overrideSharedDisabled"));}
        finally{original.close();changed.close();FeatureCacheR27.trimRegisteredCachesR31();}
    }
    @Test public void cancellationClosesActualHotPathReaderBeforeItCanBeEvicted() throws Exception {
        MemoryBudgetR31 memory=budget();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,1_000_000);FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(800,3116,null),8,memory,store);
        CooperativeContinuationR26<double[]> work=new CooperativeContinuationR26<>("r31-shared-cancel",()->PolicyAstR27.eval(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,32)),cache,1),()->number(store.snapshot(),"readerLeases")>0,null,System::nanoTime,"r31-shared-cancel");
        try{CooperativeContinuationR26.Slice<double[]> slice=work.slice(200);assertFalse(slice.done);assertEquals(1,number(store.snapshot(),"readerLeases"));assertFalse((Boolean)store.evictTo(0).get("targetReached"));work.close();assertTrue(work.awaitTermination(2000));assertEquals(0,number(store.snapshot(),"readerLeases"));assertTrue((Boolean)store.evictTo(0).get("targetReached"));}
        finally{work.close();assertTrue(work.awaitTermination(2000));cache.close();store.close();}
    }
    private static WeakReference<double[]> retiredRaw(FeatureCacheR27 cache,MemoryBudgetR31 memory){double[] retained=cache.leaf("freq",0,1,32);cache.trimUnusedR31();System.gc();FeatureCacheR27.reapReleasedArraysR31();assertTrue(number(memory.snapshot(),"activeLeaseCount")>0);assertTrue(Double.isNaN(retained[0]));return new WeakReference<>(retained);}
    @Test public void rawAliasBudgetOnlyReturnsAfterIsolatedObservedGc() throws Exception {
        MemoryBudgetR31 memory=budget();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,1_000_000);FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(800,3117,null),8,memory,store);WeakReference<double[]> weak=retiredRaw(cache,memory);cache.close();
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(weak.get()!=null&&System.nanoTime()<deadline){System.gc();Thread.sleep(10);}assertNull("isolated GC must actually clear the alias",weak.get());FeatureCacheR27.reapReleasedArraysR31();assertEquals(0,number(memory.snapshot(),"activeLeaseCount"));store.close();
    }
    @Test public void directModelLeafMaskCannotModifyCachedInput() {
        FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(50,3118,null));try{double[] cached=cache.leaf("model_surface",1,1,0);cached[0]=.7;double[] result=PolicyAstR27.eval(PolicyAstR27.leaf("model_surface",1),cache,1);assertTrue(Double.isNaN(result[0]));assertEquals(.7,cached[0],0);result[0]=.9;assertEquals(.7,cached[0],0);}finally{cache.close();}
    }
    @Test public void withDrawsOwnsReplacementRowsButPublicDatasetApiStillHasMutableArrays() {
        ReplayDatasetR27 data=ReplayDatasetR27.synthetic(20,3119,null);int[][] input=data.draws.clone();ReplayDatasetR27 owned=data.withDraws(input);int before=owned.draws[0][0],other=input[0][1];input[0][0]=other;assertEquals(before,owned.draws[0][0]);assertNotSame(input[0],owned.draws[0]);
    }
    @Test public void actualOsLowMemoryObservationSurvivesUnknownProbeUntilExplicitRecovery() {
        MemoryBudgetR31 memory=budget();memory.observeSystemLowMemory(true);assertEquals("EMERGENCY",memory.sample().get("pressure"));assertEquals(Boolean.TRUE,memory.sample().get("osLowMemory"));memory.observeSystemLowMemory(false);assertEquals("NORMAL",memory.sample().get("pressure"));
    }
    @Test public void nativeReflectionDoesNotTurnWrappedProcessFatalIntoUnknown() throws Exception {
        Method method=MemoryBudgetR31.RuntimeProbe.class.getDeclaredMethod("rethrowProcessFatal",Throwable.class);method.setAccessible(true);OutOfMemoryError fatal=new OutOfMemoryError("synthetic wrapped fatal");try{method.invoke(null,new InvocationTargetException(fatal));fail();}catch(InvocationTargetException expected){assertSame(fatal,expected.getCause());}
    }
}
