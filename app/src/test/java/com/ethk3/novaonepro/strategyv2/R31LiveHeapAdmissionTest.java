package com.ethk3.novaonepro.strategyv2;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Map;

/** R31-ENG-02: admission uses the observed post-collection live set without raising heap or thresholds.
 * Synthetic measurements only; no collector request, no device or 72-hour claim. */
public final class R31LiveHeapAdmissionTest {
    static final class LiveProbe implements MemoryBudgetR31.Probe {
        volatile long max=1_000_000,used=100_000,live=100_000;
        public MemoryBudgetR31.Measurement measure(){
            long now=System.nanoTime();
            return new MemoryBudgetR31.Measurement(7,"synthetic-live",max,used,-1,-1,-1,null,now,now,Math.min(live,used),now,"SYNTHETIC_POST_GC_LIVE");
        }
    }
    private static long number(Map<String,Object> map,String key){return ((Number)map.get(key)).longValue();}

    @Test public void uncollectedGarbageAboveHighWaterDoesNotDenyWhenLiveSetIsLow(){
        LiveProbe probe=new LiveProbe();MemoryBudgetR31 memory=new MemoryBudgetR31(probe,new MemoryBudgetR31.Config());
        probe.used=800_000;probe.live=100_000;
        MemoryBudgetR31.Admission admitted=memory.tryAcquire("research-after-garbage",MemoryBudgetR31.Kind.RESEARCH,100_000,()->false);
        assertTrue(String.valueOf(admitted.reason),admitted.acquired());
        assertEquals("NORMAL",memory.snapshot().get("pressure"));
        admitted.lease.close();
        assertEquals(0,number(memory.snapshot(),"declaredJavaWorkingBytes"));
    }

    @Test public void liveOccupancyAboveHighWaterStillDeniesOptionalWork(){
        LiveProbe probe=new LiveProbe();MemoryBudgetR31 memory=new MemoryBudgetR31(probe,new MemoryBudgetR31.Config());
        probe.used=800_000;probe.live=800_000;
        MemoryBudgetR31.Admission denied=memory.tryAcquire("research-under-pressure",MemoryBudgetR31.Kind.RESEARCH,1,()->false);
        assertFalse(denied.acquired());
        assertEquals(MemoryBudgetR31.WaitReason.HEAP_PRESSURE,denied.reason);
        assertEquals(0,number(memory.snapshot(),"declaredJavaWorkingBytes"));
    }

    @Test public void oversizedRequestIsRejectedUnderTheOriginalThresholds(){
        LiveProbe probe=new LiveProbe();MemoryBudgetR31 memory=new MemoryBudgetR31(probe,new MemoryBudgetR31.Config());
        probe.used=900_000;probe.live=100_000;
        // high=750000, realtime reserve=150000, live=100000 -> at most 500000 for optional research.
        MemoryBudgetR31.Admission tooLarge=memory.tryAcquire("oversized",MemoryBudgetR31.Kind.RESEARCH,500_001,()->false);
        assertFalse(tooLarge.acquired());
        assertEquals(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY,tooLarge.reason);
        MemoryBudgetR31.Admission fits=memory.tryAcquire("fits",MemoryBudgetR31.Kind.RESEARCH,500_000,()->false);
        assertTrue(String.valueOf(fits.reason),fits.acquired());
        assertEquals(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY,memory.tryAcquire("second",MemoryBudgetR31.Kind.RESEARCH,1,()->false).reason);
        fits.lease.close();
    }

    @Test public void exceptionPathReleasesAndRepeatedCloseIsIdempotent(){
        LiveProbe probe=new LiveProbe();MemoryBudgetR31 memory=new MemoryBudgetR31(probe,new MemoryBudgetR31.Config());
        int before=memory.explicitOwnerLeaseCountR31();
        try(MemoryBudgetR31.Lease lease=memory.tryAcquire("failing-owner",MemoryBudgetR31.Kind.RESEARCH,200_000,()->false).lease){
            assertNotNull(lease);assertEquals(200_000,number(memory.snapshot(),"declaredJavaWorkingBytes"));
            throw new IllegalStateException("R31_TEST_BODY_FAILURE");
        }catch(IllegalStateException expected){assertEquals("R31_TEST_BODY_FAILURE",expected.getMessage());}
        assertEquals(0,number(memory.snapshot(),"declaredJavaWorkingBytes"));
        assertEquals(before,memory.explicitOwnerLeaseCountR31());
        MemoryBudgetR31.Lease twice=memory.tryAcquire("twice",MemoryBudgetR31.Kind.RESEARCH,10,()->false).lease;
        twice.close();twice.close();twice.close();
        assertTrue(twice.isClosed());
        assertEquals(0,number(memory.snapshot(),"declaredJavaWorkingBytes"));
        assertEquals(MemoryBudgetR31.WaitReason.CLOSED_LEASE,twice.grow(1).reason);
    }

    @Test public void liveBasisCannotExceedRawSampleOrComeFromTheFuture(){
        long now=System.nanoTime();
        try{new MemoryBudgetR31.Measurement(1,"p",1000,100,-1,-1,-1,null,now,now,101,now,"X");fail("live above raw");}
        catch(IllegalArgumentException expected){assertEquals("R31_MEMORY_LIVE_BASIS",expected.getMessage());}
        try{new MemoryBudgetR31.Measurement(1,"p",1000,100,-1,-1,-1,null,now,now,50,now+1,"X");fail("future live sample");}
        catch(IllegalArgumentException expected){assertEquals("R31_MEMORY_LIVE_BASIS",expected.getMessage());}
        MemoryBudgetR31.Measurement raw=new MemoryBudgetR31.Measurement(1,"p",1000,100,-1,-1,-1,null,now,now);
        assertEquals(100,raw.liveHeapBytes);assertEquals("RAW_USED_CALLER_SAMPLE",raw.liveHeapBasis);
    }
}
