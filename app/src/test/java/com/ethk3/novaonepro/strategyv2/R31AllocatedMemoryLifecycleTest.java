package com.ethk3.novaonepro.strategyv2;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Real allocated primitive arrays with controlled heap measurements; no forced GC or enlarged budget. */
public final class R31AllocatedMemoryLifecycleTest {
    private static final class Probe implements MemoryBudgetR31.Probe {
        final AtomicLong heap=new AtomicLong(100000);long max=1000000;
        volatile CountDownLatch sampled,continueSample;volatile boolean stale;
        public MemoryBudgetR31.Measurement measure(){
            long time=System.nanoTime(),used=heap.get();
            if(stale){sampled.countDown();try{if(!continueSample.await(3,TimeUnit.SECONDS))throw new AssertionError("stale probe deadline");}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new AssertionError(interrupted);}}
            return new MemoryBudgetR31.Measurement(7,"controlled",max,used,-1,-1,-1,null,time,0);
        }
    }
    private static long value(MemoryBudgetR31 memory,String key){return((Number)memory.snapshot().get(key)).longValue();}
    @Test public void allocatedPayloadIsCoveredOnlyByAFreshHeapSampleAndStillOwned(){
        Probe p=new Probe();MemoryBudgetR31 memory=new MemoryBudgetR31(p,new MemoryBudgetR31.Config());
        MemoryBudgetR31.Lease retained=memory.tryAcquire("retained",MemoryBudgetR31.Kind.FEATURE_CACHE,300128,()->false).lease;
        double[] array=new double[37500];array[0]=.125;retained.allocatedPrimitiveArrayR31(array);
        assertEquals(300128,value(memory,"pendingJavaAllocationBytes"));
        p.heap.set(400000);memory.sample();assertEquals(128,value(memory,"pendingJavaAllocationBytes"));assertEquals(300000,value(memory,"allocatedJavaPayloadCoveredByHeapSampleBytes"));assertEquals(300128,value(memory,"declaredJavaWorkingBytes"));
        MemoryBudgetR31.Admission next=memory.tryAcquire("next",MemoryBudgetR31.Kind.RESEARCH,199872,()->false);assertTrue(next.acquired());
        assertEquals(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY,memory.tryAcquire("over",MemoryBudgetR31.Kind.RESEARCH,1,()->false).reason);
        retained.close();assertEquals(.125,array[0],0);assertEquals(400000,value(memory,"heapUsedBytes"));assertEquals(199872,value(memory,"declaredJavaWorkingBytes"));next.lease.close();
    }
    @Test public void aStaleProbeCannotSpendAConcurrentCompletedAllocationTwice()throws Exception{
        Probe p=new Probe();MemoryBudgetR31 memory=new MemoryBudgetR31(p,new MemoryBudgetR31.Config());MemoryBudgetR31.Lease retained=memory.tryAcquire("retained",MemoryBudgetR31.Kind.FEATURE_CACHE,200000,()->false).lease;
        p.sampled=new CountDownLatch(1);p.continueSample=new CountDownLatch(1);p.stale=true;ExecutorService thread=Executors.newSingleThreadExecutor();
        try{Future<MemoryBudgetR31.Admission> racing=thread.submit(()->memory.tryAcquire("racing",MemoryBudgetR31.Kind.RESEARCH,450000,()->false));assertTrue(p.sampled.await(1,TimeUnit.SECONDS));
            double[] actual=new double[25000];actual[0]=7;p.heap.set(300000);retained.allocatedPrimitiveArrayR31(actual);p.stale=false;p.continueSample.countDown();assertEquals(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY,racing.get(1,TimeUnit.SECONDS).reason);
            memory.sample();MemoryBudgetR31.Admission later=memory.tryAcquire("fresh",MemoryBudgetR31.Kind.RESEARCH,300000,()->false);assertTrue(later.acquired());assertEquals(7,actual[0],0);later.lease.close();
        }finally{p.stale=false;p.continueSample.countDown();retained.close();thread.shutdownNow();assertTrue(thread.awaitTermination(1,TimeUnit.SECONDS));}
    }
    @Test public void unknownWorkingSetGrowthAndCancelledOwnerRemainReservations(){
        Probe p=new Probe();MemoryBudgetR31 memory=new MemoryBudgetR31(p,new MemoryBudgetR31.Config());AtomicBoolean cancelled=new AtomicBoolean();MemoryBudgetR31.Lease lease=memory.tryAcquire("owner",MemoryBudgetR31.Kind.RESEARCH,100000,cancelled::get).lease;
        byte[] block=new byte[80000];lease.allocatedPrimitiveArrayR31(block);p.heap.set(180000);memory.sample();assertEquals(20000,value(memory,"pendingJavaAllocationBytes"));assertTrue(lease.grow(10000).acquired());assertEquals(30000,value(memory,"pendingJavaAllocationBytes"));
        cancelled.set(true);assertEquals(MemoryBudgetR31.WaitReason.CANCELLED,lease.grow(1).reason);assertEquals(110000,value(memory,"declaredJavaWorkingBytes"));assertEquals(30000,value(memory,"pendingJavaAllocationBytes"));assertEquals(80000,block.length);lease.close();assertEquals(0,value(memory,"declaredJavaWorkingBytes"));
    }
    @Test public void unknownObjectsOrUnreservedPayloadCannotBeMaterialized(){
        Probe p=new Probe();MemoryBudgetR31 memory=new MemoryBudgetR31(p,new MemoryBudgetR31.Config());MemoryBudgetR31.Lease lease=memory.tryAcquire("unproven",MemoryBudgetR31.Kind.RESEARCH,100,()->false).lease;
        try{try{lease.allocatedPrimitiveArrayR31(new Object[1]);fail();}catch(IllegalArgumentException expected){assertEquals("R31_MEMORY_UNPROVEN_ALLOCATION",expected.getMessage());}
            try{lease.allocatedPrimitiveArrayR31(new double[100]);fail();}catch(IllegalArgumentException expected){assertEquals("R31_MEMORY_ALLOCATION_EXCEEDS_RESERVATION",expected.getMessage());}assertEquals(100,value(memory,"pendingJavaAllocationBytes"));
        }finally{lease.close();}
    }
    @Test public void concurrentRealAllocationsCannotExceedOriginalHeapAndHeadroomEnvelope()throws Exception{
        Probe p=new Probe();p.max=10000000;p.heap.set(1000000);MemoryBudgetR31 memory=new MemoryBudgetR31(p,new MemoryBudgetR31.Config());ExecutorService threads=Executors.newFixedThreadPool(24);CountDownLatch done=new CountDownLatch(24),release=new CountDownLatch(1);AtomicInteger accepted=new AtomicInteger();List<Future<?>> results=new ArrayList<>();
        try{for(int i=0;i<24;i++){final int owner=i;results.add(threads.submit(()->{MemoryBudgetR31.Admission a=memory.tryAcquire("parallel-"+owner,MemoryBudgetR31.Kind.FEATURE_CACHE,250000,()->false);byte[] actual=null;
            try{if(a.acquired()){actual=new byte[250000];actual[0]=(byte)owner;p.heap.addAndGet(actual.length);a.lease.allocatedPrimitiveArrayR31(actual);accepted.incrementAndGet();}done.countDown();release.await();if(actual!=null)assertEquals((byte)owner,actual[0]);}
            catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new AssertionError(interrupted);}finally{if(a.acquired())a.lease.close();}}));}
            assertTrue(done.await(3,TimeUnit.SECONDS));assertTrue(accepted.get()>0);assertTrue(accepted.get()<=20);assertTrue(p.heap.get()<=6000000);assertEquals(250000L*accepted.get(),value(memory,"declaredJavaWorkingBytes"));
        }finally{release.countDown();threads.shutdown();assertTrue(threads.awaitTermination(3,TimeUnit.SECONDS));}for(Future<?> result:results)result.get();assertEquals(0,value(memory,"activeLeaseCount"));
    }
    @Test public void privateFeatureProductionHasOneBackingAndNoCloneReservation(){
        Probe p=new Probe();MemoryBudgetR31 memory=new MemoryBudgetR31(p,new MemoryBudgetR31.Config());SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,400000);AtomicInteger calls=new AtomicInteger();SharedFeatureStoreR31.Key key=R31MemoryGovernanceTest.key("owned");
        SharedFeatureStoreR31.Publication made=store.computePrivateDoublesR31("owned",key,37500,()->{assertEquals(1,value(memory,"activeLeaseCount"));calls.incrementAndGet();double[] actual=new double[37500];Arrays.fill(actual,.125);p.heap.addAndGet(300000);return actual;},()->false);
        try{assertTrue(made.acquired());memory.sample();assertEquals(300272,value(memory,"declaredJavaWorkingBytes"));assertEquals(272,value(memory,"pendingJavaAllocationBytes"));assertEquals(.125,made.reader.doubleAt(37499),0);
            SharedFeatureStoreR31.Publication existing=store.computePrivateDoublesR31("duplicate",key,37500,()->{throw new AssertionError("duplicate rebuilt");},()->false);existing.reader.close();assertEquals(1,calls.get());assertEquals(1,value(memory,"activeLeaseCount"));
        }finally{made.reader.close();store.close();}assertEquals(0,value(memory,"activeLeaseCount"));assertEquals(400000,value(memory,"heapUsedBytes"));
    }
    @Test public void failedOrCancelledPrivateProducerReturnsOnlyAfterItActuallyExits(){
        Probe p=new Probe();MemoryBudgetR31 memory=new MemoryBudgetR31(p,new MemoryBudgetR31.Config());SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,400000);java.util.concurrent.CancellationException cancel=new java.util.concurrent.CancellationException("fixture");
        try{try{store.computePrivateDoublesR31("cancelled",R31MemoryGovernanceTest.key("cancelled"),100,()->{assertEquals(1,value(memory,"activeLeaseCount"));throw cancel;},()->false);fail();}catch(java.util.concurrent.CancellationException actual){assertSame(cancel,actual);}assertEquals(0,value(memory,"activeLeaseCount"));assertEquals(0,((Number)store.snapshot().get("entries")).longValue());}
        finally{store.close();}
    }
    @Test public void concurrentOverrideCannotDonateAnExternallyMutableBackingToSharedStore()throws Exception{
        Probe p=new Probe();p.max=100000000;MemoryBudgetR31 memory=new MemoryBudgetR31(p,new MemoryBudgetR31.Config());SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,1000000);FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(1000,3172,null),4,memory,store);double[] external=new double[10000];Arrays.fill(external,.77);
        p.sampled=new CountDownLatch(1);p.continueSample=new CountDownLatch(1);p.stale=true;ExecutorService thread=Executors.newSingleThreadExecutor();SharedFeatureStoreR31.Reader reader=null;
        try{Future<SharedFeatureStoreR31.Reader> pending=thread.submit(()->cache.acquireLeafR31("freq",1,1,32));assertTrue(p.sampled.await(1,TimeUnit.SECONDS));cache.override("freq",(f,tp,a,b)->external);p.stale=false;p.continueSample.countDown();reader=pending.get(2,TimeUnit.SECONDS);assertNotNull(reader);double before=reader.doubleAt(9990);external[9990]=.99;assertEquals(before,reader.doubleAt(9990),0);assertNotEquals(.77,before,0);assertNull(cache.acquireLeafR31("freq",1,1,32));}
        finally{p.stale=false;p.continueSample.countDown();thread.shutdownNow();assertTrue(thread.awaitTermination(2,TimeUnit.SECONDS));if(reader!=null)reader.close();cache.close();store.close();}
    }
    @Test public void scoreMemoHasAByteBoundAndEvictionCannotChangeEscapedScores(){
        Probe p=new Probe();p.max=16000000;MemoryBudgetR31 memory=new MemoryBudgetR31(p,new MemoryBudgetR31.Config());SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,1000000);FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(1000,3173,null),4,memory,store);EvalContextR27 context=new EvalContextR27(cache,SplitR27.of(cache.data,null),null);java.util.List<double[]> escaped=new ArrayList<>();java.util.List<double[]> exact=new ArrayList<>();
        try{for(int position=1;position<=10;position++){PolicySpecR27 spec=R30PolicyContractTest.policy(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",position,32)),PolicySpecR27.Rule.MARGIN,R30PolicyContractTest.META);double[] scores=context.scores(spec);escaped.add(scores);exact.add(PolicyAstR27.eval(spec.ast,cache,spec.position));Map<String,Object> snapshot=context.scoreMemorySnapshotR31();assertTrue(((Number)snapshot.get("estimatedRetainedScoreBytes")).longValue()<=((Number)snapshot.get("byteCapacity")).longValue());}
            Map<String,Object> snapshot=context.scoreMemorySnapshotR31();assertTrue(((Number)snapshot.get("memoEntries")).intValue()<=3);assertTrue(((Number)snapshot.get("estimatedReferenceBytesDropped")).longValue()>0);context.trimUnusedScoresR31();assertEquals(0,((Number)context.scoreMemorySnapshotR31().get("estimatedRetainedScoreBytes")).longValue());assertNull(context.scoreMemorySnapshotR31().get("actualHeapReleasedBytes"));for(int i=0;i<escaped.size();i++)assertArrayEquals(exact.get(i),escaped.get(i),0);
        }finally{context.close();cache.close();store.close();}
    }
    @Test public void capacityTrimDropsOnlyRebuildableReferencesAndRequiresObservedHeadroomToResume(){
        Probe p=new Probe();MemoryBudgetR31 memory=new MemoryBudgetR31(p,new MemoryBudgetR31.Config());SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,100000);FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(100,3174,null),4,memory,store);EvalContextR27 context=new EvalContextR27(cache,SplitR27.of(cache.data,null),null);
        try{PolicySpecR27 spec=R30PolicyContractTest.policy(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,32)),PolicySpecR27.Rule.MARGIN,R30PolicyContractTest.META);double[] escaped=context.scores(spec),copy=escaped.clone();assertTrue(((Number)context.scoreMemorySnapshotR31().get("memoEntries")).intValue()>0);p.heap.set(600000);
            assertEquals(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY,memory.tryAcquire("wait-for-actual-headroom",MemoryBudgetR31.Kind.RESEARCH,1,()->false).reason);assertEquals(600000,value(memory,"heapUsedBytes"));assertEquals(0,((Number)context.scoreMemorySnapshotR31().get("memoEntries")).intValue());assertArrayEquals(copy,escaped,0);assertTrue(value(memory,"pressureReclaimAttempts")>0);
            p.heap.set(100000);MemoryBudgetR31.Admission restored=memory.tryAcquire("observed-recovery",MemoryBudgetR31.Kind.RESEARCH,100,()->false);assertTrue(restored.acquired());restored.lease.close();assertArrayEquals(copy,context.scores(spec),0);
        }finally{context.close();cache.close();store.close();}
    }
    @Test public void optionalCapacityEpisodeIsSeparateFromTheHeapHighWaterAndRechecksActualRequest(){
        Probe p=new Probe();MemoryBudgetR31 memory=new MemoryBudgetR31(p,new MemoryBudgetR31.Config());p.heap.set(599900);assertEquals(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY,memory.tryAcquire("actual-failed-request",MemoryBudgetR31.Kind.RESEARCH,200,()->false).reason);Map<String,Object> denied=memory.sample();assertEquals("NORMAL",denied.get("pressure"));assertEquals("RESOURCE_WAIT:MEMORY_DECLARED_CAPACITY",denied.get("optionalAdmissionStatus"));assertEquals(Boolean.TRUE,denied.get("optionalDenialStillBlocking"));assertEquals(200,((Number)denied.get("optionalLastDeniedRequestedBytes")).longValue());long since=((Number)denied.get("optionalWaitSinceNanos")).longValue();assertTrue(since>0);assertEquals(since,((Number)memory.sample().get("optionalWaitSinceNanos")).longValue());p.heap.set(599000);Map<String,Object> resumed=memory.sample();assertEquals("AVAILABLE",resumed.get("optionalAdmissionStatus"));assertEquals(Boolean.FALSE,resumed.get("optionalDenialStillBlocking"));assertEquals(0,((Number)resumed.get("optionalWaitSinceNanos")).longValue());assertTrue(((Number)resumed.get("optionalLastDeniedAtNanos")).longValue()>0);
    }
    @Test public void coldLiveLeafAndNestedDependenciesUseReservedRealtimeCapacityOnlyInExplicitLiveScope(){
        Probe p=new Probe();MemoryBudgetR31 memory=new MemoryBudgetR31(p,new MemoryBudgetR31.Config());SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,100000);FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(100,3187,null),4,memory,store);PolicyAstR27.Node ast=PolicyAstR27.op("zscore",PolicyAstR27.leaf("freqnb",1,32));PolicySpecR27 spec=new PolicySpecR27(ast,1,PolicySpecR27.Rule.ALWAYS,0,.15,1,false,.01,.001,null);p.heap.set(650000);
        try{try(PolicyAstR27.ScopedScoresR31 ignored=PolicyAstR27.evalScopedR31(ast,cache,1)){fail();}catch(MemoryBudgetR31.CapacityWait wait){assertEquals(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY,wait.reason);}double[] prediction=ReplayEvaluatorR27.predictNext(spec,cache,1);assertEquals(14,prediction.length);for(int i=0;i<10;i++)assertTrue(Double.isFinite(prediction[i]));Map<String,Object> kinds=(Map<String,Object>)memory.snapshot().get("declaredByKind");assertTrue(((Number)kinds.get("REALTIME")).longValue()>=4*8000);cache.trimUnusedR31();try(PolicyAstR27.ScopedScoresR31 ignored=PolicyAstR27.evalScopedR31(ast,cache,1)){fail();}catch(MemoryBudgetR31.CapacityWait wait){assertEquals(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY,wait.reason);}assertEquals("FEATURE_CACHE",memory.sample().get("optionalLastDeniedKind"));assertEquals("NORMAL",memory.sample().get("pressure"));}
        finally{cache.close();store.close();}
    }
    private static FeatureCacheR27 throwingCache(RuntimeException failure){
        Probe p=new Probe();p.max=100000000;MemoryBudgetR31 memory=new MemoryBudgetR31(p,new MemoryBudgetR31.Config());FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(1000,3171,null),4,memory,new SharedFeatureStoreR31(memory,1000000));cache.override("freq",(f,tp,a,b)->{throw failure;});return cache;
    }
    private static PolicySpecR27 complete(){return new PolicySpecR27(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",3,32)),3,PolicySpecR27.Rule.MARGIN,.05,.15,1,true,.01,.001,new PolicySpecR27.Meta("I01","BIRTH",Collections.singletonList("p0"),1,7,"R31_RUNTIME_CLASSIFICATION"));}
    @Test public void resourceWaitAndCancellationDoNotPermanentlyRejectTheCandidate(){
        for(RuntimeException failure:new RuntimeException[]{new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY),new CancellationException("fixture")}){
            FeatureCacheR27 cache=throwingCache(failure);try{BirthGateR27 gate=new BirthGateR27(cache,800);try{gate.checkCandidate(complete());fail();}catch(RuntimeException actual){assertSame(failure,actual);}assertEquals(0,gate.registered());}finally{cache.close();}
        }
    }
    @Test public void birthGateKeepsGenuineInvalidEvaluationRejectionAndRethrowsWrappedFatal(){
        FeatureCacheR27 invalid=throwingCache(new IllegalArgumentException("genuine invalid formula"));try{BirthGateR27.Verdict v=new BirthGateR27(invalid,800).checkCandidate(complete());assertFalse(v.ok);assertEquals(BirthGateR27.Reason.RANGE,v.reason);assertTrue(v.detail.contains("genuine invalid formula"));}finally{invalid.close();}
        LinkageError fatal=new LinkageError("fixture fatal");FeatureCacheR27 broken=throwingCache(new IllegalStateException("wrapped",fatal));try{try{new BirthGateR27(broken,800).checkCandidate(complete());fail();}catch(LinkageError actual){assertSame(fatal,actual);}}finally{broken.close();}
    }
    @Test public void evolutionPreservesPendingCandidateAcrossResourceWaitThenRetriesRealGate(){
        MemoryBudgetR31.CapacityWait waiting=new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY);FeatureCacheR27 cache=throwingCache(waiting);R27BirthGateTest.Recorder sink=new R27BirthGateTest.Recorder();EvolutionLoopR27.Config config=new EvolutionLoopR27.Config();config.nullControl=false;
        try{EvolutionLoopR27 loop=new EvolutionLoopR27(config,cache,sink,()->0L);loop.stepDataset();PolicySpecR27 candidate=complete();loop.addPendingForTest(candidate);try{loop.stepGate();fail();}catch(MemoryBudgetR31.CapacityWait actual){assertSame(waiting,actual);}assertEquals("NEW",loop.pendingState(candidate.policyHash()));assertFalse(sink.events.stream().anyMatch(e->e.contains("|REJECTED")));
            cache.override("freq",(f,tp,a,b)->f.freq(a,b));loop.stepGate();assertEquals("BORN",loop.pendingState(candidate.policyHash()));
        }finally{cache.close();}
    }
}
