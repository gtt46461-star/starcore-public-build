package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.strategyv2.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real Android SQLite in the routed test sandbox. Injected data is never natural evidence. */
@RunWith(AndroidJUnit4.class)
public final class R31RuntimeProtocolInstrumentedTest {
    private static DatabaseCoordinator.WriteContext context(DatabaseCoordinator.WriteClass kind,String operation) {
        return DatabaseCoordinator.WriteContext.of(kind,operation);
    }
    private static DbStore open() {
        DbStore store=new DbStore(ApplicationProvider.getApplicationContext());
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                context(DatabaseCoordinator.WriteClass.MIGRATION,"R31_TEST_SCHEMA"),db->{
                    RuntimeSchemaR26.ensure(db);SchemaR27.ensure(db);
                    db.execSQL("CREATE TABLE IF NOT EXISTS r31_test_protocol_probe(id TEXT PRIMARY KEY,value INTEGER NOT NULL,payload BLOB)");
                    return 0;
                });
        return store;
    }
    private static long number(SQLiteDatabase db,String sql,String... args) {
        try(Cursor c=db.rawQuery(sql,args)){assertTrue(sql,c.moveToFirst());return c.getLong(0);}
    }
    private static String probe(DbStore store) {
        String key=UUID.randomUUID().toString();
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                context(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_TEST_SEED"),db->{
                    db.execSQL("INSERT INTO r31_test_protocol_probe VALUES(?,0,?)",new Object[]{key,new byte[]{0,1,2,-1}});return 0;
                });
        return key;
    }
    private static DatabaseCoordinator.CommitIdentityR31 identity(String key,String payload) {
        return new DatabaseCoordinator.CommitIdentityR31(key,StatsR27.sha256(payload),"R31_TEST_ONLY", "TEST_SESSION",1,
                StatsR27.sha256("FIXED_INJECTED_TEST_INPUT"),0,"R31_INSTRUMENTED_PROTOCOL_TEST_V1");
    }
    @Test public void migrationIsAdditiveAndIdempotent() {
        DbStore store=open();try {
            String key=probe(store);long version=number(store.readable(),"PRAGMA user_version");
            for(int i=0;i<2;i++)store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                    context(DatabaseCoordinator.WriteClass.MIGRATION,"R31_TEST_MIGRATE_AGAIN"),db->{SchemaR27.ensure(db);return 0;});
            assertEquals(version,number(store.readable(),"PRAGMA user_version"));
            try(Cursor c=store.readable().rawQuery("SELECT payload FROM r31_test_protocol_probe WHERE id=?",new String[]{key})) {
                assertTrue(c.moveToFirst());assertArrayEquals(new byte[]{0,1,2,-1},c.getBlob(0));
            }
            assertTrue(SchemaDomainCoordinatorR926.matches(store.readable(),SchemaDomainCoordinatorR926.r31WriteContract()));
            System.out.println("R31_ANDROID|ADDITIVE_MIGRATION_IDEMPOTENT|ORIGINAL_BLOB_PRESERVED|INJECTED_NOT_NATURAL|PASS");
        } finally {store.close();}
    }
    @Test public void committedDiagnosticFailuresNeverRepeatBusiness() {
        DbStore store=open();try {
            for(String phase:new String[]{"AFTER_COMMIT","SCHEMA_LEAVE","TRANSACTION_TIMING","RECORD_SUCCESS"}) {
                String key=probe(store);DatabaseCoordinator.CommitIdentityR31 id=identity(key,"increment:"+key);
                AtomicInteger bodies=new AtomicInteger(),faults=new AtomicInteger();
                DatabaseCoordinator.diagnosticFaultR31.set(at->{if(phase.equals(at)){faults.incrementAndGet();throw new IllegalStateException("INJECTED_DIAGNOSTIC_"+phase);}});
                DatabaseCoordinator.CommitResultR31<Integer> first;
                try {
                    first=store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(
                            context(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_TEST_POSTCOMMIT"),id,db->{
                                bodies.incrementAndGet();db.execSQL("UPDATE r31_test_protocol_probe SET value=value+1 WHERE id=?",new Object[]{key});return 1;
                            },0);
                } finally {DatabaseCoordinator.diagnosticFaultR31.remove();}
                assertEquals(1,faults.get());assertFalse(first.replay);
                assertEquals(DatabaseCoordinator.CommitOutcome.COMMITTED_DIAGNOSTIC_FAILED,first.outcome);
                assertNotNull(first.diagnosticFailure);
                for(int repeat=0;repeat<100;repeat++)assertTrue(store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(
                        context(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_TEST_POSTCOMMIT"),id,db->{bodies.incrementAndGet();return -1;},0).replay);
                assertEquals(1,bodies.get());assertEquals(1,number(store.readable(),"SELECT value FROM r31_test_protocol_probe WHERE id=?",key));
                assertEquals(1,number(store.readable(),"SELECT count(*) FROM r31_commit_receipt WHERE commit_key=?",key));
                assertEquals(DatabaseCoordinator.CommitOutcome.COMMITTED,store.coordinatorForResearchR952().resolveCommitReceiptR31(id));
            }
            System.out.println("R31_ANDROID|POSTCOMMIT_DIAGNOSTICS_SEPARATE|100_REPLAYS_ONE_EFFECT|INDEPENDENT_READBACK|INJECTED_NOT_NATURAL|PASS");
        } finally {DatabaseCoordinator.diagnosticFaultR31.remove();store.close();}
    }
    @Test public void rollbackCanRetryAndPayloadConflictCannotMutate() {
        DbStore store=open();try {
            String key=probe(store);DatabaseCoordinator.CommitIdentityR31 id=identity(key,"allowed");
            try {
                store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(context(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_TEST_ROLLBACK"),id,db->{
                    db.execSQL("UPDATE r31_test_protocol_probe SET value=value+1 WHERE id=?",new Object[]{key});throw new IllegalArgumentException("INJECTED_BODY_FAILURE");
                },0);fail("body failure accepted");
            } catch(DatabaseCoordinator.WriteRejectedR31 expected) {assertEquals(DatabaseCoordinator.CommitOutcome.ROLLED_BACK,expected.commitOutcome);}
            assertEquals(0,number(store.readable(),"SELECT value FROM r31_test_protocol_probe WHERE id=?",key));
            assertEquals(0,number(store.readable(),"SELECT count(*) FROM r31_commit_receipt WHERE commit_key=?",key));
            store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(context(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_TEST_ROLLBACK"),id,db->{
                db.execSQL("UPDATE r31_test_protocol_probe SET value=value+1 WHERE id=?",new Object[]{key});return 1;
            },0);
            try {
                store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(context(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_TEST_ROLLBACK"),identity(key,"different"),db->{
                    db.execSQL("UPDATE r31_test_protocol_probe SET value=value+10 WHERE id=?",new Object[]{key});return 1;
                },0);fail("key accepted with a different payload");
            } catch(DatabaseCoordinator.WriteRejectedR31 expected) {assertEquals("INTEGRITY",expected.category);}
            assertEquals(1,number(store.readable(),"SELECT value FROM r31_test_protocol_probe WHERE id=?",key));
        } finally {store.close();}
    }
    @Test public void successfulNullIsNotAWriteFailure() {
        DbStore store=open();try {
            assertNull(store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                    context(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_TEST_NULL"),db->null));
        } finally {store.close();}
    }

    @Test public void feedbackReceiptRequiresTheActualCheckpointTransaction() {
        DbStore store=open();try {
            final String seed=UUID.randomUUID().toString(),event="R31_TEST_LABEL_"+seed;
            final String typed=StatsR27.sha256("typed:"+seed),source=StatsR27.sha256("source:"+seed);
            final String checkpoint=StatsR27.sha256("checkpoint:"+seed),artifact=StatsR27.sha256("artifact:"+seed);
            final String policy=StatsR27.sha256("policy:"+seed),consumer=policy+"|"+RuntimeSchemaR26.R31_MODEL_CONSUMER_PROTOCOL;
            final long ordinal=9_000_000_000L+Math.abs((long)seed.hashCode());
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                    context(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_TEST_FEEDBACK_FIXTURE"),db->{
                        db.execSQL("INSERT INTO r26_feedback VALUES(?,?,?,?,?)",new Object[]{event,ordinal,source,"INJECTED_ENGINEERING_FIXTURE",1});
                        db.execSQL("INSERT INTO r31_feedback_event VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{typed,"DRAW_LABEL_REVEALED","R31_TEST_"+seed,ordinal,source,"",event,"INJECTED_ENGINEERING_FIXTURE",StatsR27.sha256("INJECTED_ENGINEERING_FIXTURE"),"INSTRUMENTED_TEST_NOT_NATURAL",1});
                        db.execSQL("INSERT INTO r26_checkpoint_artifact VALUES(?,?,?,?,?,?,?)",new Object[]{checkpoint,artifact,policy,source,"",ordinal,1});return 0;
                    });
            try {
                store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                        context(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_TEST_FEEDBACK_ROLLBACK"),db->{
                            db.execSQL("INSERT INTO r26_consumption VALUES(?,?,?,?)",new Object[]{event,policy,checkpoint,"TRAIN"});
                            throw new IllegalStateException("INJECTED_AFTER_CONSUMPTION_BEFORE_COMMIT");
                        });
                fail("rollback accepted");
            } catch(DatabaseCoordinator.WriteRejectedR31 expected) {
                assertEquals(DatabaseCoordinator.CommitOutcome.ROLLED_BACK,expected.commitOutcome);
            }
            assertEquals(0,number(store.readable(),"SELECT count(*) FROM r31_feedback_receipt WHERE consumer_version=?",consumer));
            for(int i=0;i<2;i++)store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                    context(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_TEST_FEEDBACK_RECEIPT"),db->{
                        db.execSQL("INSERT OR IGNORE INTO r26_consumption VALUES(?,?,?,?)",new Object[]{event,policy,checkpoint,"TRAIN"});return 0;
                    });
            assertEquals(1,number(store.readable(),"SELECT count(*) FROM r31_feedback_receipt WHERE consumer_version=? AND event_id=? AND checkpoint_hash=? AND artifact_hash=?",consumer,typed,checkpoint,artifact));
            assertTrue(SchemaDomainCoordinatorR926.matches(store.readable(),SchemaDomainCoordinatorR926.r31FeedbackContract()));
            System.out.println("R31_ANDROID|CHECKPOINT_FEEDBACK_RECEIPT_ATOMIC_ROLLBACK_AND_DEDUP|INJECTED_NOT_LEARNING_OR_QUALITY_PROOF|PASS");
        } finally {store.close();}
    }

    @Test public void transportTimingMigrationPreservesUnknownTimesAndOriginalData() {
        DbStore store=open();try {
            String key=probe(store);long version=number(store.readable(),"PRAGMA user_version");
            for(int i=0;i<2;i++)store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                    context(DatabaseCoordinator.WriteClass.MIGRATION,"R31_TEST_TIMING_MIGRATION"),db->{
                        SchemaDomainCoordinatorR926.migrateR31TransportTiming(db);return 0;
                    });
            assertTrue(SchemaDomainCoordinatorR926.matches(store.readable(),SchemaDomainCoordinatorR926.r31TransportTimingContract()));
            assertEquals(version,number(store.readable(),"PRAGMA user_version"));
            assertEquals(0,number(store.readable(),"SELECT value FROM r31_test_protocol_probe WHERE id=?",key));
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                    context(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_TEST_TIMING_UNKNOWN"),db->{
                        db.execSQL("INSERT INTO r31_fetch_timing(request_id,boot_id,payload_hash,issue,accepted,body_truncated,observation_contract) VALUES(?,?,?,?,?,?,?)",
                                new Object[]{key,"TEST_BOOT",StatsR27.sha256(key),"",0,0,"INJECTED_MISSING_CLOCKS_REMAIN_UNKNOWN"});return 0;
                    });
            try(Cursor row=store.readable().rawQuery("SELECT first_byte_mono_ms,body_complete_mono_ms,validated_mono_ms FROM r31_fetch_timing WHERE request_id=?",new String[]{key})) {
                assertTrue(row.moveToFirst());for(int i=0;i<3;i++)assertTrue("missing clock must be NULL",row.isNull(i));
            }
        } finally {store.close();}
    }
}
