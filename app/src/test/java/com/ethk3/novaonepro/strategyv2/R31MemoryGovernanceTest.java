package com.ethk3.novaonepro.strategyv2;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Explicitly injected synthetic measurements test admission, not device memory/72-hour stability. */
public final class R31MemoryGovernanceTest {
    static final class Probe implements MemoryBudgetR31.Probe {
        volatile long heap=100000,max=1000000;volatile boolean fail;
        public MemoryBudgetR31.Measurement measure(){if(fail)throw new IllegalStateException("measurement unavailable");return new MemoryBudgetR31.Measurement(7,"synthetic",max,heap,31337,141000,123000,null,System.nanoTime(),System.nanoTime());}
    }
    private static MemoryBudgetR31 budget(Probe probe){return new MemoryBudgetR31(probe,new MemoryBudgetR31.Config());}
    private static long number(Map<String,Object> map,String key){return ((Number)map.get(key)).longValue();}
    static SharedFeatureStoreR31.Key key(String id){return new SharedFeatureStoreR31.Key("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",99,"feature-abi","normalizer-v1",id);}
    @Test public void concurrentAdmissionsAndDoubleCloseHaveHardDeclaredBound() throws Exception {
        MemoryBudgetR31 memory=budget(new Probe());ExecutorService threads=Executors.newFixedThreadPool(24);CountDownLatch attempted=new CountDownLatch(24),release=new CountDownLatch(1);AtomicInteger accepted=new AtomicInteger();List<Future<?>> jobs=new ArrayList<>();
        try{for(int i=0;i<24;i++){final int owner=i;jobs.add(threads.submit(()->{MemoryBudgetR31.Admission a=memory.tryAcquire("owner"+owner,MemoryBudgetR31.Kind.RESEARCH,100000,()->false);if(a.acquired())accepted.incrementAndGet();attempted.countDown();try{release.await();}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}finally{if(a.acquired()){a.lease.close();a.lease.close();}}}));}
            assertTrue(attempted.await(3,TimeUnit.SECONDS));assertEquals(5,accepted.get());assertEquals(500000,number(memory.snapshot(),"declaredJavaWorkingBytes"));
        }finally{release.countDown();threads.shutdown();assertTrue(threads.awaitTermination(3,TimeUnit.SECONDS));}for(Future<?> job:jobs)job.get();assertEquals(0,number(memory.snapshot(),"declaredJavaWorkingBytes"));
    }
    @Test public void realWatermarksLatchAndCriticalHeadroomAreSeparate() {
        Probe probe=new Probe();MemoryBudgetR31 memory=budget(probe);probe.heap=750000;assertEquals("SOFT",memory.sample().get("pressure"));probe.heap=650000;assertEquals("SOFT",memory.sample().get("pressure"));
        assertFalse(memory.tryAcquire("optional",MemoryBudgetR31.Kind.MODEL,1,()->false).acquired());MemoryBudgetR31.Admission rt=memory.tryAcquire("settlement",MemoryBudgetR31.Kind.REALTIME,50000,()->false);assertTrue(rt.acquired());rt.lease.close();
        probe.heap=600000;assertEquals("NORMAL",memory.sample().get("pressure"));probe.heap=900000;assertEquals("EMERGENCY",memory.sample().get("pressure"));assertFalse(memory.tryAcquire("large",MemoryBudgetR31.Kind.RESEARCH,1,()->false).acquired());assertEquals(150000,number(memory.snapshot(),"realtimeReservedBytes"));
    }
    @Test public void cancellationDoesNotReturnLeaseAndGrowthMustPrecedeAllocation() {
        MemoryBudgetR31 memory=budget(new Probe());AtomicBoolean cancel=new AtomicBoolean();MemoryBudgetR31.Admission a=memory.tryAcquire("training",4,MemoryBudgetR31.Kind.RESEARCH,100, cancel::get);assertTrue(a.acquired());assertTrue(a.lease.grow(50).acquired());assertEquals(150,a.lease.bytes());
        cancel.set(true);assertEquals(MemoryBudgetR31.WaitReason.CANCELLED,a.lease.grow(1).reason);assertEquals(150,number(memory.snapshot(),"declaredJavaWorkingBytes"));a.lease.close();assertEquals(0,number(memory.snapshot(),"declaredJavaWorkingBytes"));assertEquals(MemoryBudgetR31.WaitReason.CLOSED_LEASE,a.lease.grow(1).reason);
    }
    @Test public void nativeCapacityAndUnknownMeasurementsAreNotInventedFromHeap() {
        Probe probe=new Probe();MemoryBudgetR31 memory=budget(probe);assertEquals(MemoryBudgetR31.WaitReason.UNKNOWN_CAPACITY,memory.tryAcquire("decoder",MemoryBudgetR31.Kind.NATIVE_DECODER,1,()->false).reason);
        memory.registerNativeCapacity(MemoryBudgetR31.Kind.NATIVE_DECODER,80,"synthetic explicit cap");MemoryBudgetR31.Admission a=memory.tryAcquire("decoder",MemoryBudgetR31.Kind.NATIVE_DECODER,60,()->false);assertTrue(a.acquired());assertEquals(MemoryBudgetR31.WaitReason.NATIVE_DECLARED_CAPACITY,memory.tryAcquire("second",MemoryBudgetR31.Kind.NATIVE_DECODER,30,()->false).reason);
        Map<String,Object> snapshot=memory.snapshot();assertEquals(31337,number(snapshot,"nativeAllocatedBytes"));assertEquals(141000,number(snapshot,"pssBytes"));assertEquals(123000,number(snapshot,"rssBytes"));assertEquals(60,number(snapshot,"declaredNativeWorkingBytes"));assertEquals("NOT_COORDINATED",snapshot.get("crossProcessBudget"));a.lease.close();
        probe.fail=true;assertEquals(MemoryBudgetR31.WaitReason.UNKNOWN_CAPACITY,memory.tryAcquire("unknown",MemoryBudgetR31.Kind.RESEARCH,1,()->false).reason);assertEquals("MEMORY_MEASUREMENT_UNAVAILABLE",memory.sample().get("reason"));assertFalse((Boolean)memory.snapshot().get("allowOptionalWork"));
    }
    @Test public void nativeConfigurationCannotSilentlyExpand() {
        MemoryBudgetR31 memory=budget(new Probe());memory.registerNativeCapacity(MemoryBudgetR31.Kind.NATIVE_BACKUP,32,"test");memory.registerNativeCapacity(MemoryBudgetR31.Kind.NATIVE_BACKUP,32,"test");try{memory.registerNativeCapacity(MemoryBudgetR31.Kind.NATIVE_BACKUP,64,"changed");fail();}catch(IllegalStateException expected){assertEquals("R31_NATIVE_CAPACITY_CONFIG_LOCKED",expected.getMessage());}
    }
    @Test public void sixtyFourReadersShareOnePrivateImmutablePrimitiveBlock() throws Exception {
        MemoryBudgetR31 memory=budget(new Probe());SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,10000);double[] source={1,2,3};SharedFeatureStoreR31.Publication published=store.publishDoubles("producer",key("freq"),source);assertTrue(published.acquired());source[0]=999;
        List<SharedFeatureStoreR31.Reader> readers=new ArrayList<>();readers.add(published.reader);for(int i=1;i<64;i++)readers.add(store.acquire(key("freq"),"candidate"+i));long id=readers.get(0).storageIdentity();ExecutorService threads=Executors.newFixedThreadPool(8);List<Future<?>> checks=new ArrayList<>();
        try{for(SharedFeatureStoreR31.Reader reader:readers)checks.add(threads.submit(()->{assertEquals(id,reader.storageIdentity());assertEquals(1,reader.doubleAt(0),0);double[] own=new double[3];reader.copyTo(own,0);own[0]=7;assertEquals(1,reader.doubleAt(0),0);}));for(Future<?> check:checks)check.get();assertEquals(64,number(store.snapshot(),"readerLeases"));assertFalse((Boolean)store.evictTo(0).get("targetReached"));}
        finally{threads.shutdown();assertTrue(threads.awaitTermination(3,TimeUnit.SECONDS));for(SharedFeatureStoreR31.Reader reader:readers)reader.close();store.close();}
        assertEquals(0,number(memory.snapshot(),"declaredJavaWorkingBytes"));assertNull(store.snapshot().get("actualHeapReleasedBytes"));
    }
    @Test public void cacheCloseRetainsPinnedBlockUntilLastActualReaderCloses() {
        MemoryBudgetR31 memory=budget(new Probe());SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,1000);SharedFeatureStoreR31.Reader reader=store.publishDoubles("producer",key("pinned"),new double[]{3}).reader;store.close();assertTrue(number(memory.snapshot(),"declaredJavaWorkingBytes")>0);assertEquals(3,reader.doubleAt(0),0);reader.close();reader.close();assertEquals(0,number(memory.snapshot(),"declaredJavaWorkingBytes"));try{reader.doubleAt(0);fail();}catch(IllegalStateException expected){}
    }
    @Test public void pitAbiNormalizerAndFeatureKeysDoNotAlias() {
        MemoryBudgetR31 memory=budget(new Probe());SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,10000);SharedFeatureStoreR31.Reader reader=store.publishDoubles("p",key("freq"),new double[]{2}).reader;
        assertNull(store.acquire(new SharedFeatureStoreR31.Key(key("freq").datasetHash,100,"feature-abi","normalizer-v1","freq"),"future"));assertNull(store.acquire(new SharedFeatureStoreR31.Key(key("freq").datasetHash,99,"changed","normalizer-v1","freq"),"abi"));assertNull(store.acquire(key("gap"),"other"));reader.close();store.close();
    }
    @Test public void thousandRequestsCannotExceedCountOrQueuedBytesAndNeverRunOnCaller() throws Exception {
        Probe probe=new Probe();probe.max=100000000;MemoryBudgetR31 memory=budget(probe);BoundedResearchExecutorR31 executor=new BoundedResearchExecutorR31(2,400,memory);CountDownLatch entered=new CountDownLatch(2),release=new CountDownLatch(1);String caller=Thread.currentThread().getName();List<Future<Integer>> accepted=new ArrayList<>();int rejected=0;
        try{for(int i=0;i<1000;i++){final int n=i;try{accepted.add(executor.submit("request"+i,100,100,()->{assertNotEquals(caller,Thread.currentThread().getName());entered.countDown();release.await();return n;}));}catch(BoundedResearchExecutorR31.QueueCapacityWait waiting){rejected++;}assertTrue(number(executor.snapshot(),"queueSize")<=4);assertTrue(number(executor.snapshot(),"queuedBytes")<=400);assertTrue(number(executor.snapshot(),"activeCount")<=2);}
            assertTrue(entered.await(2,TimeUnit.SECONDS));assertEquals(1000,accepted.size()+rejected);assertTrue(rejected>0);
        }finally{release.countDown();executor.shutdown();assertTrue(executor.awaitTermination(3,TimeUnit.SECONDS));}for(Future<Integer> future:accepted)future.get();assertEquals(0,number(memory.snapshot(),"activeLeaseCount"));
    }
    @Test public void futureCancelKeepsRunningOwnerAndMemoryUntilBodyFinallyExits() throws Exception {
        MemoryBudgetR31 memory=budget(new Probe());BoundedResearchExecutorR31 executor=new BoundedResearchExecutorR31(1,1000,memory);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),exited=new CountDownLatch(1);
        Future<?> future=executor.submit("held-owner",100,100,()->{entered.countDown();try{for(;;)try{release.await();return null;}catch(InterruptedException ignored){}}finally{exited.countDown();}});
        try{assertTrue(entered.await(1,TimeUnit.SECONDS));future.cancel(true);assertFalse(executor.ownerTerminated("held-owner"));assertEquals(200,number(memory.snapshot(),"declaredJavaWorkingBytes"));try{executor.submit("held-owner",1,1,()->null);fail();}catch(BoundedResearchExecutorR31.QueueCapacityWait wait){assertEquals("OWNER_DRAINING",wait.reason);}executor.close();assertFalse(executor.awaitTermination(20,TimeUnit.MILLISECONDS));assertEquals(1,exited.getCount());}
        finally{release.countDown();executor.close();assertTrue(executor.awaitTermination(2,TimeUnit.SECONDS));}assertEquals(0,exited.getCount());assertTrue(executor.ownerTerminated("held-owner"));assertEquals(0,number(memory.snapshot(),"activeLeaseCount"));
    }
    @Test public void queuedFutureCancellationRemovesClosureAndReleasesOnlyThatLease() throws Exception {
        MemoryBudgetR31 memory=budget(new Probe());BoundedResearchExecutorR31 executor=new BoundedResearchExecutorR31(1,1000,memory);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger queuedCalls=new AtomicInteger();
        Future<?> active=executor.submit("active",100,100,()->{entered.countDown();release.await();return null;});assertTrue(entered.await(1,TimeUnit.SECONDS));Future<?> queued=executor.submit("queued",100,100,()->queuedCalls.incrementAndGet());
        try{assertEquals(1,number(executor.snapshot(),"queueSize"));assertTrue(queued.cancel(false));assertEquals(0,number(executor.snapshot(),"queueSize"));assertEquals(200,number(memory.snapshot(),"declaredJavaWorkingBytes"));assertTrue(executor.ownerTerminated("queued"));}
        finally{release.countDown();executor.shutdown();assertTrue(executor.awaitTermination(2,TimeUnit.SECONDS));}active.get();assertEquals(0,queuedCalls.get());assertEquals(0,number(memory.snapshot(),"activeLeaseCount"));
    }
    @Test public void failedWorkerStartReturnsDeclarationsAndLeavesNoOwner() {
        MemoryBudgetR31 memory=budget(new Probe());BoundedResearchExecutorR31 executor=new BoundedResearchExecutorR31(1,1000,memory,r->{throw new AssertionError("thread creation fixture");});
        try{try{executor.submit("cannot-start",100,100,()->null);fail();}catch(AssertionError expected){assertEquals("thread creation fixture",expected.getMessage());}assertTrue(executor.ownerTerminated("cannot-start"));assertEquals(0,number(memory.snapshot(),"activeLeaseCount"));}finally{executor.close();}
    }
    @Test public void workerFactoryDefectIsNotMislabelledAsResourceWait() {
        MemoryBudgetR31 memory=budget(new Probe());IllegalStateException defect=new IllegalStateException("factory defect");BoundedResearchExecutorR31 executor=new BoundedResearchExecutorR31(1,1000,memory,r->{throw defect;});
        try{try{executor.submit("factory-defect",100,100,()->null);fail();}catch(IllegalStateException actual){assertSame(defect,actual);}assertEquals(0,number(memory.snapshot(),"activeLeaseCount"));}finally{executor.close();}
    }
    @Test public void fatalWorkingAdmissionReturnsAlreadyHeldDescriptor() {
        AtomicInteger measurements=new AtomicInteger();OutOfMemoryError fatal=new OutOfMemoryError("synthetic admission fatal");MemoryBudgetR31.Probe probe=()->{if(measurements.incrementAndGet()==3)throw fatal;return new MemoryBudgetR31.Measurement(7,"synthetic",1000000,100000,-1,-1,-1,null,System.nanoTime(),0);};
        MemoryBudgetR31 memory=new MemoryBudgetR31(probe,new MemoryBudgetR31.Config());BoundedResearchExecutorR31 executor=new BoundedResearchExecutorR31(1,1000,memory);
        try{try{executor.submit("admission-fatal",100,100,()->null);fail();}catch(OutOfMemoryError actual){assertSame(fatal,actual);}assertEquals(0,number(memory.snapshot(),"activeLeaseCount"));assertTrue(executor.ownerTerminated("admission-fatal"));}finally{executor.close();}
    }

}
