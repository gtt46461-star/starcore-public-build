package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Deterministic declared-capacity boundary, not a device heap/released-byte claim.
 * The real cache, reader, admission callback and original fractions execute. */
public final class R31FeatureAdmissionReclaimTest {
    private static long number(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    private static MemoryBudgetR31 memory(){return new MemoryBudgetR31(()->new MemoryBudgetR31.Measurement(7,"SYNTHETIC_RECLAIM_BOUNDARY",1_000_000,100_000,-1,-1,-1,null,System.nanoTime(),0),new MemoryBudgetR31.Config());}
    private static SharedFeatureStoreR31.Key key(){return new SharedFeatureStoreR31.Key(StatsR27.sha256("old-private-backing"),0,"test-abi","raw","old");}
    private static SharedFeatureStoreR31.Reader oldBacking(SharedFeatureStoreR31 store){SharedFeatureStoreR31.Publication p=store.fillPrivateDoublesR31("old-private",key(),8,a->Arrays.fill(a,71),()->false,false);assertTrue(p.acquired());return p.reader;}
    private static MemoryBudgetR31.Lease holdRemainingPromise(MemoryBudgetR31 memory,int rows){
        long request=272L+80L*rows,headroom=number(memory.sample(),"optionalUnallocatedHeadroomBytes");
        // Keep exactly 200 bytes less than the next private feature declaration.
        // Reclaiming the unused old declaration's 272-byte envelope is sufficient;
        // the synthetic measured heap never decreases and no GC is requested.
        MemoryBudgetR31.Admission held=memory.tryAcquire("finite-context-promise",MemoryBudgetR31.Kind.QUEUED,headroom-request+200,()->false);assertTrue(held.acquired());return held.lease;
    }
    @Test public void declaredDenialEvictsUnusedCacheEvenBelowHeapHighWater(){
        MemoryBudgetR31 memory=memory();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,1<<20);FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(64,3114,null),8,memory,store);
        try(SharedFeatureStoreR31.Reader old=oldBacking(store)){}
        store.recycleUnpinnedR31();assertTrue(number(store.snapshot(),"idlePoolBytes")>0);
        try(MemoryBudgetR31.Lease held=holdRemainingPromise(memory,64)){
            assertEquals("NORMAL",memory.snapshot().get("pressure"));long heap=number(memory.snapshot(),"heapUsedBytes");
            try(SharedFeatureStoreR31.Reader next=cache.acquireLeafR31("trans2",1,0,0)){
                assertNotNull(next);assertEquals(640,next.length());assertTrue(number(store.snapshot(),"declaredBytesReleased")>=336);assertEquals(heap,number(memory.sample(),"heapUsedBytes"));assertNull(store.snapshot().get("actualHeapReleasedBytes"));
            }
        }finally{cache.close();store.close();}
        assertEquals(0,number(memory.snapshot(),"activeLeaseCount"));
    }
    @Test public void admissionReclaimPreservesAnActiveReaderUntilItsRealClose(){
        MemoryBudgetR31 memory=memory();SharedFeatureStoreR31 store=new SharedFeatureStoreR31(memory,1<<20);FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(64,3115,null),8,memory,store);SharedFeatureStoreR31.Reader reader=oldBacking(store);
        try(MemoryBudgetR31.Lease held=holdRemainingPromise(memory,64)){
            try{cache.acquireLeafR31("trans2",1,0,0);fail("pinned ownership must remain a capacity wait");}catch(MemoryBudgetR31.CapacityWait expected){assertEquals(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY,expected.reason);}
            assertEquals(71,reader.doubleAt(7),0);assertEquals(1,number(store.snapshot(),"readerLeases"));assertEquals(0,number(store.snapshot(),"declaredBytesReleased"));
            reader.close();
            // A new bounded request can reclaim the now-unpinned declaration.
            try(SharedFeatureStoreR31.Reader next=cache.acquireLeafR31("trans2",1,0,0)){assertNotNull(next);assertEquals(640,next.length());}
        }finally{reader.close();cache.close();store.close();}
        assertEquals(0,number(memory.snapshot(),"activeLeaseCount"));
    }
}
