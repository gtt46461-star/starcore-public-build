package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class R30WriteWaitTest {
    @Test public void rejectionRetainsExactBatchAndNormalRetryWritesOnce(){
        AtomicBoolean blocked=new AtomicBoolean(true);AtomicInteger writes=new AtomicInteger();ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch().add("SYNTHETIC_PENDING");
        ResearchWriteQueueR27 queue=new ResearchWriteQueueR27((op,actual)->{assertSame(batch,actual);writes.incrementAndGet();},blocked::get);
        long start=System.nanoTime();assertFalse(queue.submit("TEST",batch,()->false));long elapsed=(System.nanoTime()-start)/1_000_000;
        assertTrue("unchanged 200ms allowance",elapsed>=ResearchWriteQueueR27.MAX_WAIT_MS);assertEquals(200,ResearchWriteQueueR27.MAX_WAIT_MS);assertEquals(0,writes.get());assertEquals(1,batch.ops.size());
        blocked.set(false);assertTrue(queue.submit("TEST",batch,()->false));assertEquals(1,writes.get());Map<String,Object> measured=queue.waitSnapshotR30();assertEquals(2L,measured.get("submits"));assertEquals(1L,measured.get("rejectedOrRequeued"));assertEquals(1,batch.ops.size());System.out.println("R30_WAIT_MEASUREMENT|scenario=P0_REJECTION_THEN_RETRY|hostElapsedMs="+elapsed+"|submits="+measured.get("submits")+"|rejected="+measured.get("rejectedOrRequeued")+"|writes=1|HOST_SYNTHETIC_NOT_DEVICE");
    }
    @Test public void cancellationAndInterruptNeverSubmitAndKeepPendingWork(){
        AtomicInteger writes=new AtomicInteger();ResearchWriteQueueR27 queue=new ResearchWriteQueueR27((op,batch)->writes.incrementAndGet(),()->true);ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch().add("SYNTHETIC_PENDING");
        assertFalse(queue.submit("CANCEL",batch,()->true));Thread.currentThread().interrupt();try{assertFalse(queue.submit("INTERRUPT",batch,()->false));assertTrue(Thread.currentThread().isInterrupted());}finally{Thread.interrupted();}
        assertEquals(0,writes.get());assertEquals(1,batch.ops.size());assertEquals(2L,queue.waitSnapshotR30().get("rejectedOrRequeued"));
    }
    @Test public void releasedCriticalWorkResumesAndGenuineBusinessFailureIsNotSilenced()throws Exception{
        AtomicBoolean blocked=new AtomicBoolean(true);AtomicInteger writes=new AtomicInteger();ScheduledExecutorService release=Executors.newSingleThreadScheduledExecutor();
        try{release.schedule(()->blocked.set(false),40,TimeUnit.MILLISECONDS);ResearchWriteQueueR27 queue=new ResearchWriteQueueR27((op,batch)->writes.incrementAndGet(),blocked::get);assertTrue(queue.submit("RELEASE",new ResearchWriteQueueR27.Batch().add("SYNTHETIC"),()->false));assertEquals(1,writes.get());assertEquals(0L,queue.waitSnapshotR30().get("rejectedOrRequeued"));}finally{release.shutdownNow();}
        ResearchWriteQueueR27 bad=new ResearchWriteQueueR27((op,batch)->{throw new IllegalStateException("REAL_BUSINESS_FAILURE");},()->false);try{bad.submit("FAIL",new ResearchWriteQueueR27.Batch().add("SYNTHETIC"),()->false);fail("business failure swallowed");}catch(IllegalStateException expected){assertEquals("REAL_BUSINESS_FAILURE",expected.getMessage());}
    }
}
