package com.ethk3.novaonepro.strategyv2;

import org.junit.Test;
import static org.junit.Assert.*;

public final class R31TaskFailureDispositionTest {
    static final class Rejected extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {
        final String category,outcome;
        Rejected(String category,String outcome,Throwable cause){super("operation=HOST_TEST",cause);this.category=category;this.outcome=outcome;}
        public String failureCategory(){return category;}public String operation(){return "HOST_TEST";}
        public String phase(){return "HOST_FAULT_INJECTION";}public String commitOutcomeName(){return outcome;}
    }
    @Test public void anUnknownCommitCannotBecomeBusyRetry(){
        FailureDispositionR31.Decision result=FailureDispositionR31.classify(new Rejected("LOCK_BUSY","UNKNOWN",new IllegalStateException("CP3_DB_BUSY_DEADLINE")));
        assertEquals(FailureDispositionR31.Disposition.DEPENDENCY,result.disposition);assertTrue(result.requiresCommitResolution);assertFalse(result.consumesFaultBudget());
    }
    @Test public void committedDiagnosticsCannotBecomeBusinessFailure(){
        FailureDispositionR31.Decision result=FailureDispositionR31.classify(new Rejected("BUG","COMMITTED_DIAGNOSTIC_FAILED",new IllegalStateException("afterCommit")));
        assertTrue(result.alreadyCommitted);assertFalse(result.consumesFaultBudget());assertFalse(result.requiresCommitResolution);
    }
    @Test public void retryCancelDependencyAndFaultHaveSeparateBudgets(){
        assertEquals(FailureDispositionR31.Disposition.RETRY_LATER,FailureDispositionR31.classify(new Rejected("LOCK_BUSY","ROLLED_BACK",null)).disposition);
        assertEquals(FailureDispositionR31.Disposition.CANCELLED,FailureDispositionR31.classify(new InterruptedException("user stop")).disposition);
        assertEquals(FailureDispositionR31.Disposition.DEPENDENCY,FailureDispositionR31.classify(new IllegalArgumentException("MATRIX_ORIGINAL_STUDENT_UNAVAILABLE")).disposition);
        assertEquals(FailureDispositionR31.Disposition.STALE_OWNER,FailureDispositionR31.classify(new IllegalStateException("R31_STALE_PREPARED_LEASE")).disposition);
        assertTrue(FailureDispositionR31.classify(new IllegalStateException("F01_WRITE_FAILED")).consumesFaultBudget());
        assertEquals(FailureDispositionR31.Disposition.FATAL,FailureDispositionR31.classify(new Rejected("DISK_FULL","ROLLED_BACK",null)).disposition);
        assertEquals(FailureDispositionR31.Disposition.FATAL,FailureDispositionR31.classify(new Rejected("DB_CORRUPT","ROLLED_BACK",null)).disposition);
    }
    @Test public void boundedCauseCycleAndCancellationPreserveInterrupt(){
        RuntimeException first=new RuntimeException("first"),second=new RuntimeException("second");first.initCause(second);second.initCause(first);
        assertEquals(FailureDispositionR31.Disposition.FAULT,FailureDispositionR31.classify(first).disposition);
        Thread.currentThread().interrupt();try{FailureDispositionR31.classify(new InterruptedException("normal shutdown"));assertTrue(Thread.currentThread().isInterrupted());}finally{Thread.interrupted();}
    }
    @Test public void backoffQuarantineAndWaitKindsAreExplicit(){
        for(int streak=1;streak<100;streak++){long delay=FailureDispositionR31.backoffMillis(streak,"task");assertTrue(delay>=25&&delay<=1016);}
        assertEquals(30_000,FailureDispositionR31.quarantineMillis(1));assertEquals(120_000,FailureDispositionR31.quarantineMillis(2));assertEquals(600_000,FailureDispositionR31.quarantineMillis(3));assertEquals(1_800_000,FailureDispositionR31.quarantineMillis(50));
        assertEquals(WaitKindR27.CANCELLED,WaitKindR27.classify(0,null,new InterruptedException(),0,1,null,false).kind);
        assertEquals(WaitKindR27.DEPENDENCY,WaitKindR27.classify(0,null,new IllegalStateException("R31_COMPUTE_CAPACITY_WAIT"),0,1,null,false).kind);
        assertEquals(WaitKindR27.NEEDS_FIX,WaitKindR27.classify(0,null,new Rejected("DISK_FULL","ROLLED_BACK",null),0,1,null,false).kind);
        assertEquals(WaitKindR27.ERROR,WaitKindR27.classify(0,null,new IllegalStateException("boom"),0,1,null,false).kind);
    }
    @Test public void processFatalCannotBeHiddenByTypedOrNestedWrappers(){
        LinkageError fatal=new LinkageError("HOST_FATAL_INJECTION");Rejected wrapper=new Rejected("BUG","ROLLED_BACK",new IllegalStateException("outer",fatal));
        assertSame(fatal,FailureDispositionR31.processFatalCause(wrapper));assertEquals(FailureDispositionR31.Disposition.FATAL,FailureDispositionR31.classify(wrapper).disposition);assertFalse(FailureDispositionR31.classify(wrapper).consumesFaultBudget());
    }
    @Test public void actualMemoryAndBoundedQueueExceptionsNeverConsumeFaultBudget(){
        Throwable memory=new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.HEAP_PRESSURE);
        assertEquals(FailureDispositionR31.Disposition.DEPENDENCY,FailureDispositionR31.classify(memory).disposition);assertFalse(FailureDispositionR31.classify(memory).consumesFaultBudget());assertEquals(WaitKindR27.RESOURCE,WaitKindR27.classify(0,null,memory,0,1,null,false).kind);
        assertEquals(FailureDispositionR31.Disposition.CANCELLED,FailureDispositionR31.classify(new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.CANCELLED)).disposition);
        Throwable queue=new BoundedResearchExecutorR31.QueueCapacityWait("OWNER_DRAINING");assertEquals(FailureDispositionR31.Disposition.DEPENDENCY,FailureDispositionR31.classify(queue).disposition);assertFalse(FailureDispositionR31.classify(queue).consumesFaultBudget());
    }
}
