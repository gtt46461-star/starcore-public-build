package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.LifecycleSchemaR31;
import com.ethk3.novaonepro.ResearchTaskQueueR967;
import com.ethk3.novaonepro.SchemaDomainCoordinatorR926;
import com.ethk3.novaonepro.StartupStageJournalR341;
import com.ethk3.novaonepro.r952.ModernFamilyFactoryCP12;
import java.io.File;

public final class R342StartupTests {
    private R342StartupTests() {}

    private static SQLiteDatabase fixture(Context app, String file) {
        return SQLiteDatabase.openOrCreateDatabase(app.getDatabasePath(file + ".sqlite"), null);
    }

    private static void tx(SQLiteDatabase db, SchemaDomainCoordinatorR926.Step step) throws Exception {
        db.beginTransaction();
        try {
            step.run(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static long number(SQLiteDatabase db, String sql) {
        try (Cursor row = db.rawQuery(sql,null)) {
            assertTrue(row.moveToFirst());
            return row.getLong(0);
        }
    }

    public static void checkpoint(Context app) throws Exception {
        try (SQLiteDatabase db = fixture(app, "r342-checkpoint-007")) {
            String hash = StatsR27.sha256("unavailable-baseline-original");
            tx(db, write -> {
                SchemaDomainCoordinatorR926.ensureInfrastructure(write);
                write.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
                write.execSQL("CREATE TABLE r29_model_checkpoint(owner TEXT PRIMARY KEY,feature_abi TEXT,dataset_hash TEXT,"
                        + "trained_through INTEGER,state_hash TEXT,artifact BLOB,updated_at INTEGER)");
                ResearchRetentionStoreR30.initializeProtectionMetadataR34(write);
                ResearchRetentionStoreR30.installProtectionUnitR34(write, StartupCheckpointIdentityR31.SOURCE);
                write.execSQL("CREATE TABLE r31_keep_original(archive_table TEXT,row_id INTEGER,row_hash TEXT,bytes INTEGER,state TEXT)");
                write.execSQL("INSERT INTO r31_keep_original VALUES(?,1,?,40,'MIGRATED_ORIGINAL_RETAINED')",
                        new Object[]{StartupCheckpointIdentityR31.ARCHIVE,hash});
                write.execSQL("INSERT INTO r29_model_checkpoint VALUES('ATOMNET_TEN_R30','abi','set',8,'state',X'0080F201',8)");
            });
            for (int i = 0; i < 12; i++) {
                tx(db, LifecycleSchemaR31::advanceStartupCheckpointWithinMigration);
            }
            assertTrue(StartupCheckpointIdentityR31.ready(db));
            File missing = new File(app.getFilesDir(), "r342-missing-cas");
            StartupCheckpointIdentityR31.Prepared beforeMarker = StartupCheckpointIdentityR31.preparePreservingHotRowsR342(db,missing);
            assertEquals(1, beforeMarker.evidence().get("unavailableColdRows"));
            assertEquals(1, beforeMarker.sourceRows);
            try {
                tx(db, write -> ResearchRetentionStoreR30.preservePreparedStartupCheckpoint(write,beforeMarker));
                fail("The absent boot marker invalidates prepared source equality");
            } catch (DatabaseCoordinator.SchemaPendingR31 expected) {
                assertTrue(expected.getMessage().contains("BOOT_MARKER_DEFINITION_PENDING"));
            }
            assertFalse(ResearchRetentionStoreR30.startupBootMarkerReadyR342(db));
            tx(db, ResearchRetentionStoreR30::installStartupBootMarkerR342);
            StartupCheckpointIdentityR31.Prepared prepared = StartupCheckpointIdentityR31.preparePreservingHotRowsR342(db,missing);
            try {
                tx(db, write -> {
                    ResearchRetentionStoreR30.preservePreparedStartupCheckpoint(write, prepared);
                    throw new IllegalStateException("FORCED_ROLLBACK");
                });
                fail("Expected a rollback");
            } catch (IllegalStateException expected) {
                assertEquals("FORCED_ROLLBACK", expected.getMessage());
            }
            assertNull(ResearchRetentionStoreR30.startupEvidenceReadBackR342(db));
            tx(db, write -> ResearchRetentionStoreR30.preservePreparedStartupCheckpoint(write, prepared));
            String receipt = ResearchRetentionStoreR30.startupEvidenceReadBackR342(db);
            assertTrue(receipt.matches("[a-f0-9]{64}"));
            tx(db, write -> ResearchRetentionStoreR30.preservePreparedStartupCheckpoint(write, prepared));
            assertEquals(receipt, ResearchRetentionStoreR30.startupEvidenceReadBackR342(db));
            assertEquals(1L, number(db, "SELECT count(*) FROM r30_keep_r29_model_checkpoint WHERE hex(artifact)='0080F201'"));
            assertEquals(1L, number(db, "SELECT count(*) FROM r31_keep_original"));
            assertEquals(1L, number(db, "SELECT count(*) FROM r31_startup_checkpoint_receipt"));
        }
    }

    public static void circuit(Context app) {
        StartupStageJournalR341 journal = StartupStageJournalR341.open(app,"r342-research");
        String old = StatsR27.sha256("schema-epoch-1"), repaired = StatsR27.sha256("schema-epoch-2");
        for (int i = 0; i < 3; i++) {
            StartupStageJournalR341.Token begin = journal.begin("research","run-" + i,
                    StartupStageJournalR341.Stage.OWNER_BEGIN,old);
            assertTrue(journal.recordUncaught(begin,new IllegalStateException("DETERMINISTIC_FIXTURE_BUG")).durable);
        }
        assertTrue(journal.hasDeterministicFailureLimit(old));
        assertFalse(journal.hasDeterministicFailureLimit(repaired));
    }

    public static void isolatedSchema(Context app) throws Exception {
        try (SQLiteDatabase db = fixture(app, "r342-cold-domain-009")) {
            tx(db, write -> {
                SchemaDomainCoordinatorR926.ensureInfrastructure(write);
                write.execSQL("CREATE TABLE r26_candidate(status TEXT,campaign_id TEXT,policy_hash TEXT)");
                write.execSQL("CREATE TABLE r26_expert_experiment(status TEXT,created_at INTEGER,experiment_id TEXT)");
            });
            assertFalse(ColdRebirthSchemaR34.readyR342(db));
            assertFalse(LifecycleSchemaR31.coldRebirthMigrationCommittedR342(db));
            for (int i = 0; i < 16 && !LifecycleSchemaR31.coldRebirthMigrationCommittedR342(db); i++)
                tx(db, LifecycleSchemaR31::advanceColdRebirthWithinMigrationR342);
            assertTrue(LifecycleSchemaR31.coldRebirthMigrationCommittedR342(db));
            ColdRebirthSchemaR34.require(db);
            assertFalse(LifecycleSchemaR31.ready(db));
            try {
                AtomNetPublicationR31.requireConsumerSchemaR342(db);
                fail("The cold-rebirth migration is not AtomNet readiness");
            } catch (DatabaseCoordinator.SchemaPendingR31 expected) {
                assertEquals("DEPENDENCY", expected.failureCategory());
            }
        }
    }

    public static void taskAdmission(Context app) throws Exception {
        File path=app.getDatabasePath("r342-task-admission-010.sqlite");
        String[] task = new String[1];
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(path,null)) {
            tx(db, ResearchTaskQueueR967::schema);
            tx(db, ModernFamilyFactoryCP12::schema);
            tx(db, write -> write.execSQL("UPDATE " + ModernFamilyFactoryCP12.CURSOR
                    + " SET next_variant=7,last_reason='WAIT_HISTORY',generation=1"));
            assertTrue(ModernFamilyFactoryCP12.hasOpenVariantsR342(db));
            tx(db, write -> task[0] = ResearchTaskQueueR967.enqueue(write, "TRAIN_CANDIDATE", "candidate-fixture",
                    "202611250005","protocol-fixture","input-fixture","authoritative-action-fixture"));
        }
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(path.getPath(),null,SQLiteDatabase.OPEN_READWRITE)) {
            assertTrue(ResearchTaskQueueR967.hasReady(db,"TRAIN_CANDIDATE",System.currentTimeMillis()));
            assertFalse(ResearchTaskQueueR967.hasReady(db,"POST_TRAIN",System.currentTimeMillis()));
            tx(db, write -> {
                String same = ResearchTaskQueueR967.enqueue(write,"TRAIN_CANDIDATE","candidate-fixture",
                        "202611250005","protocol-fixture","input-fixture","authoritative-action-fixture");
                assertEquals(task[0],same);
                try {
                    ResearchTaskQueueR967.enqueue(write,"TRAIN_CANDIDATE","candidate-fixture","202611250005",
                            "protocol-fixture","foreign-input","authoritative-action-fixture");
                    fail("Expected immutable input identity");
                } catch (IllegalStateException mismatch) {
                    assertEquals("R967_TASK_INPUT_DRIFT",mismatch.getMessage());
                }
            });
            assertEquals(1L,number(db,"SELECT count(*) FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE'"));
            assertEquals(0L,number(db,"SELECT count(*) FROM v57_research_task_r967 WHERE status='COMPLETED'"));
        }
    }
}
