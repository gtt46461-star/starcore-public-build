package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Controlled clock/measurement fixtures test reporting semantics, never model quality or elapsed device time. */
public final class R31RuntimeMemoryControlTest {
    private static Map<String,Object> sample(String pressure,long headroom,boolean blocked,long since,long last){
        Map<String,Object> out=new LinkedHashMap<>();out.put("pressure",pressure);out.put("heapUsedBytes",339_000_000L);out.put("maxHeapBytes",536_870_912L);out.put("optionalUnallocatedHeadroomBytes",headroom);out.put("optionalAdmissionStatus",blocked?"RESOURCE_WAIT:MEMORY_DECLARED_CAPACITY":"AVAILABLE");out.put("optionalDenialStillBlocking",blocked);out.put("optionalWaitSinceNanos",since);out.put("optionalLastDeniedAtNanos",last);out.put("optionalLastDeniedRequestedBytes",400_000L);return out;
    }
    private static RuntimeCoordinatorR26.MemoryControlStateR31 observe(Map<String,Object> m,long elapsed,long nanos,RuntimeCoordinatorR26.MemoryControlStateR31 old){return RuntimeCoordinatorR26.MemoryControlStateR31.observe(m,elapsed,nanos,old);}
    @Test public void normalWatermarkDoesNotHideActuallyDeniedLargerRequest(){
        Map<String,Object> m=sample("NORMAL",235_307,true,1_000_000_000L,1_000_000_000L);
        RuntimeCoordinatorR26.MemoryControlStateR31 first=observe(m,100,2_000_000_000L,null);
        assertFalse(first.heapLimited);assertEquals(0,first.heapAgeMs);assertTrue(first.optionalLimited);assertTrue(first.limitParallelism);assertFalse(first.optionalCapacityExhausted);assertTrue(first.recentOptionalDenial);
        RuntimeCoordinatorR26.MemoryControlStateR31 aged=observe(m,120_101,122_001_000_000L,first);
        assertTrue(aged.optionalOverTwoMinutes);assertTrue(aged.constraintOverTwoMinutes);assertFalse(aged.heapOverTwoMinutes);assertEquals(0,aged.heapAgeMs);assertTrue(aged.reason.startsWith("OPTIONAL_MEMORY_ADMISSION_OVER_TWO_MINUTES:"));assertFalse(aged.recentOptionalDenial);assertTrue(aged.optionalWaitEvidence.contains("NOT_THREAD_WAIT_PROOF"));
    }
    @Test public void denialEpisodeCanAlreadyBeOldWhenControllerFirstObserves(){
        RuntimeCoordinatorR26.MemoryControlStateR31 s=observe(sample("NORMAL",2,true,1_000_000L,100_000_000_000L),7,121_001_000_000L,null);
        assertEquals(121000,s.actualDenialEpisodeAgeMs.longValue());assertTrue(s.optionalOverTwoMinutes);assertEquals(0,s.heapAgeMs);
    }
    @Test public void zeroHeadroomIsCapacityConstraintWithoutInventedThreadWait(){
        Map<String,Object> m=sample("NORMAL",0,false,0,0);RuntimeCoordinatorR26.MemoryControlStateR31 first=observe(m,1,5_000_000_000L,null),aged=observe(m,120_001,125_000_000_000L,first);
        assertTrue(aged.optionalCapacityExhausted);assertTrue(aged.optionalOverTwoMinutes);assertFalse(aged.recentOptionalDenial);assertNull(aged.actualDenialEpisodeAgeMs);assertTrue(aged.optionalWaitEvidence.contains("WITHOUT_ACTIVE_DENIAL_PROOF"));assertFalse(aged.heapLimited);
    }
    @Test public void actualAdmissionRecoveryDoesNotClaimHeapReleaseOrProgress(){
        RuntimeCoordinatorR26.MemoryControlStateR31 old=observe(sample("NORMAL",8,true,1_000_000L,1_000_000L),10,130_000_000_000L,null);
        Map<String,Object> fit=sample("NORMAL",800_000,false,0,1_000_000L);RuntimeCoordinatorR26.MemoryControlStateR31 fresh=observe(fit,20,130_010_000_000L,old);
        assertTrue(fresh.optionalAdmissionRecovered);assertEquals(20,fresh.optionalRecoveryElapsedMs);assertFalse(fresh.limitParallelism);assertEquals(0,fresh.optionalCapacityAgeMs);assertEquals("NOT_ESTABLISHED_BY_ADMISSION_CONTROL",fresh.asMap().get("heapReleaseEvidence"));assertTrue(fresh.asMap().get("recoveryEvidence").toString().contains("NOT_HEAP_RELEASE_OR_MODEL_PROGRESS"));
        RuntimeCoordinatorR26.MemoryControlStateR31 next=observe(fit,30,130_020_000_000L,fresh);assertFalse(next.optionalAdmissionRecovered);assertEquals(20,next.optionalRecoveryElapsedMs);
    }
    @Test public void watermarkAndAdmissionAgesRemainSeparateAcrossChanges(){
        RuntimeCoordinatorR26.MemoryControlStateR31 heap=observe(sample("SOFT",0,false,0,0),10,1_000_000L,null);
        RuntimeCoordinatorR26.MemoryControlStateR31 optional=observe(sample("NORMAL",0,false,0,0),120_011,120_002_000_000L,heap);
        assertFalse(optional.heapLimited);assertEquals(0,optional.heapAgeMs);assertTrue(optional.constraintOverTwoMinutes);assertTrue(optional.optionalOverTwoMinutes);
        RuntimeCoordinatorR26.MemoryControlStateR31 emergency=observe(sample("EMERGENCY",0,false,0,0),120_012,120_003_000_000L,optional);assertTrue(emergency.heapLimited);assertEquals(0,emergency.heapAgeMs);assertEquals("MEMORY_EMERGENCY_WATERMARK",emergency.reason);
    }
    @Test public void missingOrCrossProcessTelemetryIsNotRecoveredOrOldDenialProof(){
        RuntimeCoordinatorR26.MemoryControlStateR31 old=observe(sample("NORMAL",2,true,1_000_000L,1_000_000L),10,100_000_000_000L,null);
        RuntimeCoordinatorR26.MemoryControlStateR31 missing=observe(Collections.singletonMap("pressure","NORMAL"),20,100_010_000_000L,old);assertFalse(missing.optionalAdmissionRecovered);assertFalse(missing.optionalObservationKnown);assertEquals("MEMORY_OPTIONAL_ADMISSION_OBSERVATION_UNKNOWN",missing.reason);
        RuntimeCoordinatorR26.MemoryControlStateR31 other=observe(sample("NORMAL",2,true,900_000_000_000L,900_000_000_000L),30,100_020_000_000L,null);assertNull(other.actualDenialEpisodeAgeMs);assertNull(other.lastDenialAgeMs);assertTrue(other.optionalLimited);assertTrue(other.optionalWaitEvidence.contains("CLOCK_MISMATCH"));assertFalse(other.optionalOverTwoMinutes);
    }
    @Test public void measurementFailureAndPublishedFactsAreExplicitAndImmutable(){
        Map<String,Object> m=sample("NORMAL",2,true,1_000_000L,1_000_000L);m.put("sampleFailure","ProbeFailure");RuntimeCoordinatorR26.MemoryControlStateR31 s=observe(m,1,2_000_000L,null);assertEquals("MEMORY_MEASUREMENT_UNAVAILABLE",s.reason);
        try{s.asMap().put("heapReleaseEvidence","PASS");fail("mutable observation");}catch(UnsupportedOperationException expected){}assertTrue(s.asMap().get("actionPolicy").toString().contains("NO_TASK_REVOKE_NO_REALTIME_STOP"));
    }
    @Test public void productionBudgetRealDenialAndRecheckDriveController(){
        final long[] heap={100_000};MemoryBudgetR31 budget=new MemoryBudgetR31(()->new MemoryBudgetR31.Measurement(11,"CONTROLLED_MEASUREMENT_FIXTURE",1_000_000,heap[0],-1,-1,-1,null,System.nanoTime(),0),new MemoryBudgetR31.Config());
        MemoryBudgetR31.Admission held=budget.tryAcquire("held-unallocated-work",MemoryBudgetR31.Kind.RESEARCH,499_000,()->false);assertTrue(held.acquired());assertEquals(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY,budget.tryAcquire("request",MemoryBudgetR31.Kind.RESEARCH,4000,()->false).reason);
        Map<String,Object> denied=budget.sample();RuntimeCoordinatorR26.MemoryControlStateR31 old=observe(denied,10,System.nanoTime(),null);assertEquals("NORMAL",old.heapPressure);assertTrue(old.optionalLimited);
        MemoryBudgetR31.Admission small=budget.tryAcquire("small-request",MemoryBudgetR31.Kind.RESEARCH,100,()->false);assertTrue(small.acquired());assertTrue((Boolean)budget.sample().get("optionalDenialStillBlocking"));small.lease.close();held.lease.close();Map<String,Object> recheck=budget.sample();RuntimeCoordinatorR26.MemoryControlStateR31 available=observe(recheck,20,System.nanoTime(),old);assertTrue(available.optionalAdmissionRecovered);assertEquals(100_000,((Number)recheck.get("heapUsedBytes")).longValue());assertEquals(0,((Number)budget.snapshot().get("activeLeaseCount")).longValue());
    }
}
