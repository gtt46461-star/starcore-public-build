package com.ethk3.novaonepro;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public final class AnalysisSingleFlightR36Test {
    private AnalysisSingleFlightR36 flight;

    @Before public void reset(){flight=AnalysisSingleFlightR36.shared();flight.resetForTests();}

    @Test public void hundredRequestsKeepOneOwnerAndLatestPending(){
        AnalysisSingleFlightR36.SubmitResult first=flight.submit(request(1));
        assertTrue(first.startNow);assertEquals(1,flight.activeCount());
        for(int i=2;i<=100;i++)assertTrue(flight.submit(request(i)).queued);
        assertEquals("20260810"+four(100),flight.pendingTarget());
        assertEquals(1,flight.maximumObserved());
        AnalysisSingleFlightR36.Request latest=flight.releaseAndTakeNext(first.lease);
        assertNotNull(latest);assertEquals("202608100100",latest.targetIssue);
        AnalysisSingleFlightR36.SubmitResult successor=flight.submit(latest);
        assertTrue(successor.startNow);assertEquals(1,flight.activeCount());
        assertNull(flight.releaseAndTakeNext(successor.lease));
        assertEquals(0,flight.activeCount());assertEquals(1,flight.maximumObserved());
    }

    @Test public void latestWinsAndNewerTargetRequestsCooperativeCancellation(){
        AnalysisSingleFlightR36.SubmitResult owner=flight.submit(request(1));
        flight.submit(request(2));flight.submit(request(3));
        assertEquals("202608100003",flight.pendingTarget());
        assertTrue(owner.lease.token.supersededByNewerTarget());
        assertEquals("NEWER_TARGET:202608100002",owner.lease.token.cancelReason());
        assertFalse(flight.cancelIfNoProgress(owner.lease.token.lastProgressAt()+29999L));
        assertFalse(flight.cancelIfNoProgress(owner.lease.token.lastProgressAt()+60000L));
        AnalysisSingleFlightR36.Request next=flight.releaseAndTakeNext(owner.lease);
        assertEquals("202608100003",next.targetIssue);
    }

    @Test public void newerTargetAfterFinalizationQueuesUntilDurableCommit(){
        AnalysisSingleFlightR36.SubmitResult owner=flight.submit(request(1));
        owner.lease.token.enterFinalization();
        flight.submit(request(2));flight.submit(request(3));
        assertTrue(owner.lease.token.isFinalizing());
        assertFalse(owner.lease.token.supersededByNewerTarget());
        assertEquals("",owner.lease.token.cancelReason());
        assertEquals(1,flight.activeCount());
        assertEquals("202608100003",flight.pendingTarget());
        owner.lease.token.markCanonicalCommitted();
        assertFalse(flight.cancelIfNoProgress(owner.lease.token.lastProgressAt()+60000L));
        assertEquals("202608100003",flight.releaseAndTakeNext(owner.lease).targetIssue);
        assertEquals(0,flight.activeCount());
    }

    @Test public void supersededBeforeFinalizationCannotEnterCommitPhase(){
        AnalysisSingleFlightR36.SubmitResult owner=flight.submit(request(1));
        flight.submit(request(2));
        try { owner.lease.token.enterFinalization(); fail("superseded owner entered finalization"); }
        catch(AnalysisSingleFlightR36.CancelledBeforeCommitException expected) {
            assertTrue(expected.getMessage().contains("NEWER_TARGET"));
        }
        assertFalse(owner.lease.token.isFinalizing());
        assertEquals(1,flight.activeCount());
        assertEquals("202608100002",flight.releaseAndTakeNext(owner.lease).targetIssue);
    }

    @Test public void noProgressCancelsExactlyOnce(){
        AnalysisSingleFlightR36.SubmitResult owner=flight.submit(request(1));
        long at=owner.lease.token.lastProgressAt();
        assertFalse(flight.cancelIfNoProgress(at+29999L));
        assertTrue(flight.cancelIfNoProgress(at+30000L));
        assertFalse(flight.cancelIfNoProgress(at+60000L));
        flight.releaseAndTakeNext(owner.lease);
    }

    @Test public void progressPulsePreventsTwelveSecondBudgetFromCancelling(){
        AnalysisSingleFlightR36.SubmitResult owner=flight.submit(request(1));
        owner.lease.token.pulse("COMPUTE_RUNNING");
        long at=owner.lease.token.lastProgressAt();
        assertFalse(flight.cancelIfNoProgress(at+12000L));
        owner.lease.token.markCanonicalCommitted();
        assertFalse(flight.cancelIfNoProgress(at+120000L));
        flight.releaseAndTakeNext(owner.lease);
    }

    private static AnalysisSingleFlightR36.Request request(int sequence){
        String base="20260810"+four(sequence-1==0?1440:sequence-1);
        if(sequence==1)base="202608090001";
        String target=sequence==1?"202608090002":"20260810"+four(sequence);
        long now=1700000000000L+sequence;
        return new AnalysisSingleFlightR36.Request(base,target,sequence,now,now+60000L,
                "TEST",V51ChecksumEngine.sha256("REQ|"+sequence),"");
    }
    private static String four(int value){String s=String.valueOf(value);while(s.length()<4)s="0"+s;return s;}
}
