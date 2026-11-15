package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import java.util.concurrent.*;

/** Actual 120s pending-promise debt cannot be solved by collecting the measured heap. */
public final class ResourceRecoveryDebtR31Host {
    @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
        MemoryBudgetR31 b=MemoryBudgetR31.processBudget();long available=((Number)b.snapshot().get("optionalUnallocatedHeadroomBytes")).longValue();MemoryBudgetR31.Admission held=b.tryAcquire("engineering-live-unallocated-promise",MemoryBudgetR31.Kind.RESEARCH,available-(4L<<20),()->false);if(!held.acquired())throw new AssertionError("promise admission");
        long maximumOptional=(long)(Runtime.getRuntime().maxMemory()*MemoryBudgetR31.HIGH_FRACTION)-(long)Math.ceil(Runtime.getRuntime().maxMemory()*MemoryBudgetR31.REALTIME_FRACTION);long request=maximumOptional-held.lease.bytes()+(8L<<20);MemoryBudgetR31.Admission denied=b.tryAcquire("engineering-oversized-with-live-promise",MemoryBudgetR31.Kind.RESEARCH,request,()->false);if(denied.reason!=MemoryBudgetR31.WaitReason.DECLARED_CAPACITY)throw new AssertionError("real refusal");Map<String,Object> before=b.snapshot();long start=System.nanoTime();new CountDownLatch(1).await(120200,TimeUnit.MILLISECONDS);long elapsed=(System.nanoTime()-start)/1000000L;Map<String,Object> decision=b.recoverAgedOptionalHeapR31("engineering-debt-controller",()->true);if(elapsed<120000||Boolean.TRUE.equals(decision.get("attempted"))||!"CAPACITY_DEBT_PENDING_PROMISES_OR_OVERSIZED_REQUEST".equals(decision.get("reason")))throw new AssertionError("debt must not request collection: "+decision);if(held.lease.isClosed())throw new AssertionError("live declaration released");Map<String,Object> recovery=(Map<String,Object>)b.snapshot().get("optionalHeapRecovery");if(((Number)recovery.get("attemptCount")).longValue()!=0)throw new AssertionError("collector requested on promise debt");Map<String,Object> out=new LinkedHashMap<>();out.put("scope","ACTUAL_DEFAULT_PROBE_REAL_PENDING_PROMISE_120S_DEBT_NOT_DEVICE_QUALITY");out.put("elapsedMs",elapsed);out.put("before",before);out.put("decision",decision);out.put("after",b.snapshot());out.put("explicitCollectorRequests",0);out.put("probeSubstitution",false);out.put("clockSubstitution",false);System.out.println(JsonR27.write(out));held.lease.close();
    }
}
