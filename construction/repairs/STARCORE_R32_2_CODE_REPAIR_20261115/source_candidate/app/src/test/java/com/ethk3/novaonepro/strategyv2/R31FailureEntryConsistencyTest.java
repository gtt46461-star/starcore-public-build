package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import com.ethk3.novaonepro.DatabaseCoordinator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

/** R31-B01-C01#A2/#A3: one root cause gets the same category and disposition at the R26/R27 task entry,
 * the prepared-slice wrapper, the DatabaseCoordinator direct-write adapter, the R27 wait kind and the
 * research write queue. Storage-fatal and integrity failures are never transient retries. */
public final class R31FailureEntryConsistencyTest {
    private static Throwable locked(){return new android.database.sqlite.SQLiteDatabaseLockedException("database is locked (code 5 SQLITE_BUSY)");}
    private static Map<String,Throwable> roots(){
        Map<String,Throwable> roots=new LinkedHashMap<>();
        roots.put("androidLocked",locked());
        // Native SQLITE_BUSY text surfaced through an untyped wrapper (the Android stub jar does not keep exception messages).
        roots.put("androidGenericLockedMessage",new RuntimeException("database is locked (code 5 SQLITE_BUSY)"));
        roots.put("writerDeadline",new IllegalStateException("CP3_DB_BUSY_DEADLINE"));
        roots.put("timeout",new java.util.concurrent.TimeoutException("writer wait"));
        roots.put("userStop",new InterruptedException("user stop"));
        roots.put("cancelOverLock",new IllegalStateException("R27_CANCELLED",locked()));
        Throwable lockOverCancel=locked();lockOverCancel.initCause(new InterruptedException("user stop"));roots.put("lockOverCancel",lockOverCancel);
        roots.put("staleFence",new IllegalStateException("R26_STALE_FENCE:task=r27"));
        roots.put("ownerChanged",new IllegalStateException("R30_PREPARED_OWNER_CHANGED:task=r27"));
        roots.put("memoryWait",new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.HEAP_PRESSURE));
        roots.put("queueWait",new BoundedResearchExecutorR31.QueueCapacityWait("OWNER_DRAINING"));
        roots.put("schemaPending",new IllegalStateException("SCHEMA_PENDING:R27"));
        roots.put("diskFull",new IllegalStateException("commit",new android.database.sqlite.SQLiteFullException("database or disk is full (code 13 SQLITE_FULL)")));
        roots.put("diskFullUnderBusyText",new IllegalStateException("CP3_DB_BUSY_DEADLINE",new android.database.sqlite.SQLiteFullException("database or disk is full (code 13 SQLITE_FULL)")));
        roots.put("constraint",new android.database.sqlite.SQLiteConstraintException("UNIQUE constraint failed: r26_task.task_id"));
        roots.put("corrupt",new android.database.sqlite.SQLiteDatabaseCorruptException("database disk image is malformed (code 11 SQLITE_CORRUPT)"));
        roots.put("plainWriteFailure",new IllegalStateException("F01_WRITE_FAILED"));
        roots.put("bug",new IllegalStateException("boom"));
        return roots;
    }
    private static String waitKind(FailureDispositionR31.Decision d){
        switch(d.disposition){
            case RETRY_LATER:return WaitKindR27.RETRY_LATER;
            case CANCELLED:return WaitKindR27.CANCELLED;
            case STALE_OWNER:return WaitKindR27.STALE_OWNER;
            case DEPENDENCY:return d.category.startsWith("RESOURCE_")?WaitKindR27.RESOURCE:WaitKindR27.DEPENDENCY;
            case FATAL:return WaitKindR27.NEEDS_FIX;
            default:return WaitKindR27.ERROR;
        }
    }

    @Test public void fourEntriesClassifyTheSameRootIdentically(){
        for(Map.Entry<String,Throwable> e:roots().entrySet()){
            String name=e.getKey();Throwable root=e.getValue();
            FailureDispositionR31.Decision claim=FailureDispositionR31.classify(root);
            FailureDispositionR31.Decision slice=FailureDispositionR31.classify(new IllegalStateException("R30_PREPARED_TASK_FAILED",root));
            String adapted=DatabaseCoordinator.failureCategoryR31(root);
            FailureDispositionR31.Decision write=FailureDispositionR31.classify(new DatabaseCoordinator.WriteRejectedR31(adapted,"R31_ENTRY_TEST","BODY",DatabaseCoordinator.CommitOutcome.ROLLED_BACK,root));
            assertEquals(name+" adapter category",FailureDispositionR31.rootCategoryR31(root),adapted);
            assertEquals(name+" slice category",claim.category,slice.category);
            assertEquals(name+" write category",claim.category,write.category);
            assertEquals(name+" slice disposition",claim.disposition,slice.disposition);
            assertEquals(name+" write disposition",claim.disposition,write.disposition);
            assertEquals(name+" fault budget",claim.consumesFaultBudget(),write.consumesFaultBudget());
            assertEquals(name+" R27 wait kind",waitKind(claim),WaitKindR27.classify(0,null,root,0,1,null,false).kind);
            assertEquals(name+" R27 wait kind wrapped",waitKind(claim),WaitKindR27.classify(0,null,new IllegalStateException("R30_PREPARED_TASK_FAILED",root),0,1,null,false).kind);
            assertEquals(name+" research queue busy",claim.disposition==FailureDispositionR31.Disposition.RETRY_LATER,ResearchWriteQueueR27.writerBusy(root));
        }
    }

    @Test public void expectedDispositionsForEachRoot(){
        Map<String,FailureDispositionR31.Disposition> expected=new LinkedHashMap<>();
        for(String n:new String[]{"androidLocked","androidGenericLockedMessage","writerDeadline","timeout"})expected.put(n,FailureDispositionR31.Disposition.RETRY_LATER);
        for(String n:new String[]{"userStop","cancelOverLock","lockOverCancel"})expected.put(n,FailureDispositionR31.Disposition.CANCELLED);
        for(String n:new String[]{"staleFence","ownerChanged"})expected.put(n,FailureDispositionR31.Disposition.STALE_OWNER);
        for(String n:new String[]{"memoryWait","queueWait","schemaPending"})expected.put(n,FailureDispositionR31.Disposition.DEPENDENCY);
        for(String n:new String[]{"diskFull","diskFullUnderBusyText","corrupt"})expected.put(n,FailureDispositionR31.Disposition.FATAL);
        for(String n:new String[]{"constraint","plainWriteFailure","bug"})expected.put(n,FailureDispositionR31.Disposition.FAULT);
        Map<String,Throwable> roots=roots();assertEquals(expected.keySet(),roots.keySet());
        for(Map.Entry<String,FailureDispositionR31.Disposition> e:expected.entrySet())
            assertEquals(e.getKey(),e.getValue(),FailureDispositionR31.classify(roots.get(e.getKey())).disposition);
    }

    @Test public void fullConstraintAndCorruptAreNeverTransientRetry(){
        for(String name:new String[]{"diskFull","diskFullUnderBusyText","constraint","corrupt"}){
            Throwable root=roots().get(name);
            assertNotEquals(name,FailureDispositionR31.Disposition.RETRY_LATER,FailureDispositionR31.classify(root).disposition);
            assertFalse(name,ResearchWriteQueueR27.writerBusy(root));
            assertFalse(name,ResearchWriteQueueR27.writerBusy(new IllegalStateException("writer body",root)));
        }
        assertEquals("INTEGRITY",DatabaseCoordinator.failureCategoryR31(roots().get("constraint")));
        assertEquals("DISK_FULL",DatabaseCoordinator.failureCategoryR31(roots().get("diskFullUnderBusyText")));
        assertEquals("DB_CORRUPT",DatabaseCoordinator.failureCategoryR31(roots().get("corrupt")));
    }

    @Test public void busyBackoffIsBoundedExponentialWithDeterministicJitter(){
        long[] base={25,50,100,200,400,800,1000,1000,1000};
        for(int streak=1;streak<=base.length;streak++){
            long a=FailureDispositionR31.backoffMillis(streak,"r27-evolution"),b=FailureDispositionR31.backoffMillis(streak,"r27-evolution");
            assertEquals("deterministic",a,b);
            assertTrue("streak "+streak+" delay "+a,a>=base[streak-1]&&a<base[streak-1]+17);
        }
        assertTrue(FailureDispositionR31.backoffMillis(10_000,"x")<=1016);
    }
}
