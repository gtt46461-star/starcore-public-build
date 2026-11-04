package com.ethk3.novaonepro.strategyv2;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Private physical ownership, not natural prediction quality or device memory evidence. */
public final class R31PrivateBackingReuseTest {
    private static MemoryBudgetR31 memory(){return new MemoryBudgetR31(()->new MemoryBudgetR31.Measurement(7,"synthetic-unit",100000000,100000,-1,-1,-1,null,System.nanoTime(),0),new MemoryBudgetR31.Config());}
    private static long n(Map<String,Object> m,String key){return((Number)m.get(key)).longValue();}
    private static SharedFeatureStoreR31.Key key(String name){return R31MemoryGovernanceTest.key(name);}
    private static SharedFeatureStoreR31.Publication fill(SharedFeatureStoreR31 store,String name,int length,double value){return store.fillPrivateDoublesR31("builtin",key(name),length,a->Arrays.fill(a,value),()->false,false);}
    private static void bounded(SharedFeatureStoreR31 store){Map<String,Object> s=store.snapshot();assertEquals(n(s,"declaredBytes"),n(s,"entryBytes")+n(s,"idlePoolBytes")+n(s,"inflightBytes"));assertTrue(n(s,"declaredBytes")<=n(s,"capacityBytes"));}
    @Test public void physicalBackingRebindsOnlyAfterOldReaderClosesAndPublicCopiesStayIndependent(){
        MemoryBudgetR31 memory=memory();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,336);SharedFeatureStoreR31.Reader first=fill(store,"old",8,1).reader;double[] escaped=new double[8];first.copyTo(escaped,0);long backing=first.privateBackingIdentityR31();
        try{assertFalse(fill(store,"new",8,2).acquired());assertEquals(1,first.doubleAt(0),0);assertEquals(1,n(store.snapshot(),"backingAllocations"));first.close();SharedFeatureStoreR31.Publication next=fill(store,"new",8,2);assertTrue(next.acquired());try{assertEquals(backing,next.reader.privateBackingIdentityR31());assertEquals(2,next.reader.doubleAt(7),0);assertNull(store.acquire(key("old"),"stale"));assertEquals(1,escaped[0],0);assertEquals(1,n(memory.snapshot(),"activeLeaseCount"));bounded(store);}finally{next.reader.close();}}
        finally{first.close();store.close();}assertEquals(0,n(memory.snapshot(),"activeLeaseCount"));assertNull(store.snapshot().get("actualHeapReleasedBytes"));
    }
    @Test public void cancelledPartialFillIsUnpublishedThenCompletelyReinitialized(){
        MemoryBudgetR31 memory=memory();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,336);CancellationException stop=new CancellationException("fixture");
        try{try{store.fillPrivateDoublesR31("partial",key("partial"),8,a->{a[0]=99;throw stop;},()->false,false);fail();}catch(CancellationException actual){assertSame(stop,actual);}assertNull(store.acquire(key("partial"),"reader"));assertEquals(1,n(store.snapshot(),"idleBackings"));assertEquals(1,n(memory.snapshot(),"activeLeaseCount"));SharedFeatureStoreR31.Publication next=store.fillPrivateDoublesR31("next",key("next"),8,a->{for(double v:a)assertTrue(Double.isNaN(v));Arrays.fill(a,3);},()->false,false);assertTrue(next.acquired());try{assertEquals(3,next.reader.doubleAt(0),0);assertEquals(1,n(store.snapshot(),"backingAllocations"));bounded(store);}finally{next.reader.close();}}
        finally{store.close();}assertEquals(0,n(memory.snapshot(),"activeLeaseCount"));
    }
    @Test public void storeCloseAndFutureCancelKeepInflightBytesUntilActualProducerExit()throws Exception{
        MemoryBudgetR31 memory=memory();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,336);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),exited=new CountDownLatch(1);ExecutorService thread=Executors.newSingleThreadExecutor();Future<?> future=thread.submit(()->{try{return store.fillPrivateDoublesR31("blocked",key("blocked"),8,a->{entered.countDown();for(;;)try{release.await();break;}catch(InterruptedException ignored){}Arrays.fill(a,4);},()->false,false);}finally{exited.countDown();}});
        try{assertTrue(entered.await(1,TimeUnit.SECONDS));assertEquals(336,n(store.snapshot(),"inflightBytes"));future.cancel(true);store.close();assertEquals(1,exited.getCount());assertEquals(336,n(store.snapshot(),"declaredBytes"));assertEquals(1,n(memory.snapshot(),"activeLeaseCount"));assertFalse(fill(store,"replacement",8,5).acquired());bounded(store);}
        finally{release.countDown();thread.shutdown();assertTrue(thread.awaitTermination(2,TimeUnit.SECONDS));}assertEquals(0,exited.getCount());assertEquals(0,n(store.snapshot(),"declaredBytes"));assertEquals(0,n(memory.snapshot(),"activeLeaseCount"));
    }
    @Test public void duplicatePrivateProducersHaveOneTruthAndNeverRecyclePinnedBacking()throws Exception{
        MemoryBudgetR31 memory=memory();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,672);CyclicBarrier started=new CyclicBarrier(2);ExecutorService threads=Executors.newFixedThreadPool(2);List<Future<SharedFeatureStoreR31.Publication>> futures=new ArrayList<>();List<SharedFeatureStoreR31.Reader> readers=new ArrayList<>();
        try{for(int i=0;i<2;i++)futures.add(threads.submit(()->store.fillPrivateDoublesR31("race",key("same"),8,a->{try{started.await(2,TimeUnit.SECONDS);}catch(Exception e){throw new AssertionError(e);}Arrays.fill(a,.25);},()->false,false)));for(Future<SharedFeatureStoreR31.Publication> f:futures){SharedFeatureStoreR31.Publication p=f.get();assertTrue(p.acquired());readers.add(p.reader);}assertEquals(readers.get(0).storageIdentity(),readers.get(1).storageIdentity());long pinned=readers.get(0).privateBackingIdentityR31();assertEquals(2,n(store.snapshot(),"backingAllocations"));assertEquals(1,n(store.snapshot(),"idleBackings"));SharedFeatureStoreR31.Publication different=fill(store,"different",8,7);assertTrue(different.acquired());try{assertNotEquals(pinned,different.reader.privateBackingIdentityR31());for(SharedFeatureStoreR31.Reader reader:readers)assertEquals(.25,reader.doubleAt(7),0);assertEquals(7,different.reader.doubleAt(7),0);bounded(store);}finally{different.reader.close();}}
        finally{threads.shutdownNow();assertTrue(threads.awaitTermination(2,TimeUnit.SECONDS));for(SharedFeatureStoreR31.Reader reader:readers)reader.close();store.close();}assertEquals(0,n(memory.snapshot(),"activeLeaseCount"));
    }
    @Test public void concurrentDifferentKeysRetainEveryResultWithinEntryPoolInflightBound()throws Exception{
        MemoryBudgetR31 memory=memory();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,4*(272+512));ExecutorService threads=Executors.newFixedThreadPool(8);List<Future<?>> futures=new ArrayList<>();AtomicInteger completed=new AtomicInteger();
        try{for(int i=0;i<64;i++){final int id=i;futures.add(threads.submit(()->{for(int attempt=0;attempt<1000;attempt++){SharedFeatureStoreR31.Publication p=fill(store,"key-"+id,64,id);bounded(store);if(p.acquired()){try{for(int j=0;j<64;j++)assertEquals(id,p.reader.doubleAt(j),0);completed.incrementAndGet();return;}finally{p.reader.close();}}Thread.yield();}throw new AssertionError("bounded retry did not finish");}));}for(Future<?> f:futures)f.get();assertEquals(64,completed.get());assertTrue(n(store.snapshot(),"backingReuses")>0);bounded(store);}
        finally{threads.shutdownNow();assertTrue(threads.awaitTermination(2,TimeUnit.SECONDS));store.close();}assertEquals(0,n(memory.snapshot(),"activeLeaseCount"));
    }
    @Test public void realBuiltinNestedDependenciesCannotStealThePrivateOutputTarget(){
        MemoryBudgetR31 memory=memory();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,3*(272+80*300));FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(300,3191,null),8,memory,store);SharedFeatureStoreR31.Reader first=cache.acquireLeafR31("freqnb",1,1,32);assertNotNull(first);double[] left=cache.leaf("freq",0,10,32),right=cache.leaf("freq",0,2,32),savedLeft=left.clone();long backing=first.privateBackingIdentityR31();try{for(int i=0;i<left.length;i++)assertEquals(Double.doubleToLongBits(.5*(left[i]+right[i])),Double.doubleToLongBits(first.doubleAt(i)));}finally{first.close();}
        try(SharedFeatureStoreR31.Reader second=cache.acquireLeafR31("meangap",1,1,32)){assertNotNull(second);assertArrayEquals(savedLeft,left,0);assertEquals(3,n(store.snapshot(),"backingAllocations"));assertTrue(n(store.snapshot(),"backingReuses")>0);bounded(store);}finally{cache.close();store.close();}
    }
    @Test public void cancelledWorkingBodyKeepsExclusiveBackingUntilItsActualFinally()throws Exception{
        MemoryBudgetR31 memory=memory();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,336);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),exited=new CountDownLatch(1);ExecutorService worker=Executors.newSingleThreadExecutor();
        Future<?> future=worker.submit(()->{try(SharedFeatureStoreR31.PrivateWorkingR31 loan=store.borrowPrivateWorkingR31("busy-scope",8,()->false,false)){double[] values=loan.exclusiveValuesR31();Arrays.fill(values,71);entered.countDown();for(;;)try{release.await();break;}catch(InterruptedException ignored){}assertEquals(71,values[7],0);}finally{exited.countDown();}});
        try{assertTrue(entered.await(1,TimeUnit.SECONDS));assertEquals(1,n(store.snapshot(),"workingLoans"));future.cancel(true);store.close();assertEquals(1,exited.getCount());assertEquals(336,n(store.snapshot(),"declaredBytes"));assertEquals(1,n(memory.snapshot(),"activeLeaseCount"));bounded(store);}
        finally{release.countDown();worker.shutdown();assertTrue(worker.awaitTermination(2,TimeUnit.SECONDS));store.close();}assertEquals(0,exited.getCount());assertEquals(0,n(store.snapshot(),"declaredBytes"));assertEquals(0,n(memory.snapshot(),"activeLeaseCount"));
    }
    private static java.lang.ref.WeakReference<double[]> orphanWorking(SharedFeatureStoreR31 store){SharedFeatureStoreR31.PrivateWorkingR31 loan=store.borrowPrivateWorkingR31("orphan-scope",8,()->false,false);return new java.lang.ref.WeakReference<>(loan.exclusiveValuesR31());}
    @Test public void rootedStoreDoesNotStronglyRetainAnAbandonedExclusiveWorkingArray()throws Exception{
        MemoryBudgetR31 memory=memory();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,336);java.lang.ref.WeakReference<double[]> array=orphanWorking(store);assertEquals(1,n(store.snapshot(),"workingLoans"));assertEquals(1,n(memory.snapshot(),"activeLeaseCount"));
        // Isolated ST-M03 ownership inspection only; ordinary admission never invokes this collector.
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(array.get()!=null&&System.nanoTime()<deadline){System.gc();Thread.sleep(10);}assertNull(array.get());
        while(n(store.snapshot(),"workingLoans")!=0&&System.nanoTime()<deadline)Thread.yield();assertEquals(0,n(store.snapshot(),"workingLoans"));assertEquals(1,n(store.snapshot(),"workingOwnersCollected"));assertEquals(0,n(store.snapshot(),"declaredBytes"));assertEquals(0,n(memory.snapshot(),"activeLeaseCount"));assertNull(store.snapshot().get("actualHeapReleasedBytes"));store.close();
    }
    @Test public void lastScoreScopeReturnsBackingToGlobalIdleBeforeItsCacheCloses(){
        MemoryBudgetR31 memory=memory();int rows=64;long bytes=272+80*rows;SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,bytes);FeatureCacheR27 first=new FeatureCacheR27(ReplayDatasetR27.synthetic(rows,3192,null),8,memory,store),second=new FeatureCacheR27(ReplayDatasetR27.synthetic(rows,3193,null),8,memory,store);
        try{try(PolicyAstR27.ScopedScoresR31 scope=PolicyAstR27.evalScopedR31(PolicyAstR27.op("c",.5),first,1)){assertEquals(.5,scope.at(rows*10-1),0);assertEquals(bytes,n(store.snapshot(),"workingBytesIncludedInInflight"));assertEquals(1,n(store.snapshot(),"workingLoans"));bounded(store);}assertEquals(0,n(store.snapshot(),"workingLoans"));assertEquals(bytes,n(store.snapshot(),"idlePoolBytes"));assertEquals(0,n(first.scratchMemorySnapshotR31(),"blocks"));try(SharedFeatureStoreR31.Reader reader=second.acquireLeafR31("freq",1,1,8)){assertNotNull(reader);assertEquals(1,n(store.snapshot(),"backingAllocations"));assertTrue(n(store.snapshot(),"backingReuses")>0);bounded(store);}}
        finally{first.close();second.close();store.close();}assertEquals(0,n(memory.snapshot(),"activeLeaseCount"));
    }
}
