package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.DatabaseCoordinator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** Prepared production batches, without a phone. Durability is exercised by the JDBC host driver separately. */
public final class R31PreparedWriteContractTest {
    private static ResearchWriteQueueR27.ImmutableBatch prepare(ResearchWriteQueueR27.Batch batch) {
        return batch.prepareR31("TEST","stable-key","task","owner",3,"dataset:cursor",7,"operator-v1");
    }
    @Test public void callerAndExportMutationsCannotChangePreparedPayload() {
        byte[] bytes={1,2};Object[] binds={bytes,"x"};
        ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch().add("INSERT INTO t VALUES(?,?)",binds);
        bytes[0]=9;binds[1]="changed";
        ResearchWriteQueueR27.ImmutableBatch prepared=prepare(batch);
        String original=prepared.payloadHash;
        batch.ops.clear();batch.add("DELETE FROM t");
        ResearchWriteQueueR27.Op exported=prepared.operationsR31().get(0);
        ((byte[])exported.args[0])[0]=8;exported.args[1]="mutated export";
        assertArrayEquals(new byte[]{1,2},(byte[])prepared.operationsR31().get(0).args[0]);
        assertEquals("x",prepared.operationsR31().get(0).args[1]);assertEquals(original,prepared.payloadHash);assertEquals(1,prepared.opCount);
    }
    @Test public void typedLengthEncodingPreventsAmbiguousPayloadHashes() {
        String number=prepare(new ResearchWriteQueueR27.Batch().add("INSERT INTO t VALUES(?)",12)).payloadHash;
        String text=prepare(new ResearchWriteQueueR27.Batch().add("INSERT INTO t VALUES(?)","12")).payloadHash;
        String nullValue=prepare(new ResearchWriteQueueR27.Batch().add("INSERT INTO t VALUES(?)",new Object[]{null})).payloadHash;
        String nullText=prepare(new ResearchWriteQueueR27.Batch().add("INSERT INTO t VALUES(?)","null")).payloadHash;
        assertNotEquals(number,text);assertNotEquals(nullValue,nullText);
        assertNotEquals(prepare(new ResearchWriteQueueR27.Batch().add("x","a|b","c")).payloadHash,
                prepare(new ResearchWriteQueueR27.Batch().add("x","a","b|c")).payloadHash);
    }
    @Test public void largeAtomicBatchIsRetainedAndReportedForItsOwner() {
        ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();
        for(int i=0;i<65;i++)batch.add("INSERT INTO t VALUES(?)",i);
        ResearchWriteQueueR27.ImmutableBatch prepared=prepare(batch);
        assertTrue(prepared.overInitialBudget);assertEquals(65,prepared.opCount);assertEquals(65,prepared.operationsR31().size());
    }
    @Test public void persistentIdentitySurvivesOwnerChangeAndPayloadStillDetectsChanges() {
        ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch().add("UPDATE t SET count=count+?",1);
        ResearchWriteQueueR27.ImmutableBatch first=prepare(batch),replacement=batch.prepareR31("TEST","stable-key","task","new-owner",9,"dataset:cursor",7,"operator-v1");
        assertEquals(first.commitKey,replacement.commitKey);assertEquals(first.payloadHash,replacement.payloadHash);
        assertNotEquals(first.identityR31.ownerEpoch,replacement.identityR31.ownerEpoch);
        assertNotEquals(first.payloadHash,prepare(new ResearchWriteQueueR27.Batch().add("UPDATE t SET count=count+?",2)).payloadHash);
    }
    @Test public void preparedRuntimeDdlIsRejectedBeforeTheWriter() {
        AtomicInteger calls=new AtomicInteger();ResearchWriteQueueR27 queue=new ResearchWriteQueueR27((op,batch)->calls.incrementAndGet(),()->false);
        try{queue.submit("TEST",prepare(new ResearchWriteQueueR27.Batch().add("CREATE TABLE t(id INTEGER)")),()->false);fail("runtime DDL accepted");}
        catch(DatabaseCoordinator.WriteRejectedR31 expected){assertEquals("DEPENDENCY",expected.category);assertEquals("SCHEMA_PENDING",expected.phase);}
        assertEquals(0,calls.get());
    }
    @Test public void cancellationUnknownCommitAndRealFaultNeverMasqueradeAsBusy() {
        assertFalse(ResearchWriteQueueR27.writerBusy(new InterruptedException("stopped")));
        assertFalse(ResearchWriteQueueR27.writerBusy(new IllegalStateException("F01_WRITE_FAILED")));
        assertFalse(ResearchWriteQueueR27.writerBusy(new DatabaseCoordinator.WriteRejectedR31("LOCK_BUSY","x","END",DatabaseCoordinator.CommitOutcome.UNKNOWN,new IllegalStateException("database is locked"))));
        assertFalse(ResearchWriteQueueR27.writerBusy(new IllegalStateException("database or disk is full")));
        assertTrue(ResearchWriteQueueR27.writerBusy(new IllegalStateException("CP3_DB_BUSY_DEADLINE")));
    }
    @Test public void coordinatorAdaptationMatchesTheRuntimeFailureContract() {
        assertEquals("DEPENDENCY",DatabaseCoordinator.failureCategoryR31(new IllegalStateException("MATRIX_ORIGINAL_STUDENT_UNAVAILABLE:x")));
        assertEquals("DEPENDENCY",DatabaseCoordinator.failureCategoryR31(new IllegalStateException("R26_UNSLICED_COMPUTATION_CAPACITY")));
        assertEquals("STALE_OWNER",DatabaseCoordinator.failureCategoryR31(new IllegalStateException("R30_PREPARED_OWNER_CHANGED")));
        assertEquals("STALE_OWNER",DatabaseCoordinator.failureCategoryR31(new IllegalStateException("R26_STALE_FENCE")));
        DatabaseCoordinator.WriteRejectedR31 full=new DatabaseCoordinator.WriteRejectedR31("DISK_FULL","x","BODY",DatabaseCoordinator.CommitOutcome.ROLLED_BACK,new IllegalStateException("SQLITE_FULL"));
        assertEquals(FailureDispositionR31.Disposition.FATAL,FailureDispositionR31.classify(full).disposition);
    }
}
