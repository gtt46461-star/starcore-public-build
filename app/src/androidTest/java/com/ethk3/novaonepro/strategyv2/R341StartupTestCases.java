package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.LifecycleSchemaR31;
import com.ethk3.novaonepro.SchemaDomainCoordinatorR926;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class R341StartupTestCases {
    private static final String SOURCE_SQL = "CREATE TABLE r29_model_checkpoint(owner TEXT PRIMARY KEY,"
            + "feature_abi TEXT,dataset_hash TEXT,trained_through INTEGER,state_hash TEXT,artifact BLOB,updated_at INTEGER)";
    private static final String TEST_TASK = "r27-replay-dataset";

    private R341StartupTestCases() {}

    private static SQLiteDatabase fixture(Context context, String name) {
        File file = context.getDatabasePath(name + ".sqlite");
        assertTrue(file.getAbsolutePath().contains("/r30-instrumentation/"));
        return SQLiteDatabase.openOrCreateDatabase(file, null);
    }

    private static void transaction(SQLiteDatabase db, SchemaDomainCoordinatorR926.Step update) throws Exception {
        db.beginTransaction();
        try {
            update.run(db);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static long count(SQLiteDatabase db, String query, String...args) {
        try (Cursor cursor = db.rawQuery(query, args.length == 0 ? null : args)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getLong(0);
        }
    }

    private static void source(SQLiteDatabase db) throws Exception {
        transaction(db, write -> {
            SchemaDomainCoordinatorR926.ensureInfrastructure(write);
            write.execSQL(SOURCE_SQL);
        });
    }

    private static void installReader(SQLiteDatabase db) throws Exception {
        for (int i = 0; i < 14; i++) {
            try { if (StartupCheckpointIdentityR31.ready(db)) return; }
            catch (DatabaseCoordinator.SchemaPendingR31 incomplete) { /* The next unit owns that definition. */ }
            transaction(db, LifecycleSchemaR31::advanceStartupCheckpointWithinMigration);
        }
        assertTrue(StartupCheckpointIdentityR31.ready(db));
    }

    public static void resumeRetentionUnit(Context context) throws Exception {
        DbStore store = new DbStore(context);
        DatabaseCoordinator owner = store.coordinatorForResearchR952();
        AtomicInteger units = new AtomicInteger();
        try {
            owner.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,
                    "R341_B03_INITIAL_RUNTIME"), db -> {
                RuntimeSchemaR26.ensure(db);
                SchemaR27.ensure(db);
                return null;
            });
            RetentionMigrationR34.Writer writer = (contract, operation) -> owner.runAtomicWriteOrThrowF01(contract, db -> {
                operation.run(db);
                if ("R34_RETENTION_UNIT".equals(contract.operation) && units.incrementAndGet() == 2)
                    throw new IllegalStateException("INJECTED_SECOND_UNIT_BEFORE_COMMIT");
                return null;
            });
            String path = store.readable().getPath();
            try (SQLiteDatabase read = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)) {
                RetentionMigrationR34.Session session = new RetentionMigrationR34.Session(read, writer, "fixture-first-owner");
                assertFalse(session.step());
                try {
                    session.step();
                    fail("Injected rollback did not reach the migration");
                } catch (DatabaseCoordinator.WriteRejectedR31 rollback) {
                    assertEquals(DatabaseCoordinator.CommitOutcome.ROLLED_BACK, rollback.commitOutcome);
                }
                assertEquals(1L, count(read, "SELECT count(*) FROM r34_schema_unit_receipt"));
            }
            try (SQLiteDatabase read = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)) {
                RetentionMigrationR34.Session session = new RetentionMigrationR34.Session(read, writer, "fixture-successor");
                assertFalse(session.step());
                assertEquals(2L, count(read, "SELECT count(*) FROM r34_schema_unit_receipt"));
                assertEquals(1L, count(read, "SELECT count(*) FROM r34_schema_unit_receipt WHERE unit_index=0"));
                assertEquals(1L, count(read, "SELECT count(*) FROM r34_schema_unit_receipt WHERE unit_index=1 AND owner_epoch='fixture-successor'"));
            }
        } finally { store.close(); }
    }

    public static void invalidatedSourceProof(Context context) throws Exception {
        try (SQLiteDatabase db = fixture(context, "checkpoint-stale-source")) {
            source(db);
            installReader(db);
            File missingRoot = new File(context.getFilesDir(), "missing-cas");
            StartupCheckpointIdentityR31.Prepared frozen = StartupCheckpointIdentityR31.prepare(db, missingRoot);
            assertEquals(0, frozen.sourceRows);
            transaction(db, frozen::requireWithin);
            transaction(db, write -> write.execSQL("INSERT INTO r29_model_checkpoint VALUES('ATOMNET_TEN_R30','abi','inputs',1,'state',X'AB',0)"));
            try {
                transaction(db, frozen::requireWithin);
                fail("A changed source generation was accepted");
            } catch (DatabaseCoordinator.SchemaPendingR31 stale) {
                assertTrue(stale.getMessage().contains("SOURCE_CHANGED"));
            }
        }
    }

    public static void exactLifecycleObjects(Context context) throws Exception {
        try (SQLiteDatabase db = fixture(context, "cold-lifecycle-objects")) {
            source(db);
            List<Map<String, String>> definitions = LifecycleSchemaR31.expectedObjects(db);
            assertTrue(definitions.stream().anyMatch(row -> row.get("name").equals("r34_cold_rebirth")));
            assertTrue(definitions.stream().anyMatch(row -> row.get("name").equals("r31_startup_checkpoint_source")));
            installReader(db);
            assertEquals(3L, count(db, "SELECT count(*) FROM sqlite_master WHERE name IN"
                    + " ('r31_startup_checkpoint_insert','r31_startup_checkpoint_update','r31_startup_checkpoint_delete')"));
            transaction(db, write -> {
                write.execSQL("DROP TRIGGER r31_startup_checkpoint_insert");
                write.execSQL("CREATE TRIGGER r31_startup_checkpoint_insert AFTER INSERT ON r29_model_checkpoint BEGIN SELECT 1; END");
            });
            try {
                transaction(db, LifecycleSchemaR31::advanceStartupCheckpointWithinMigration);
                fail("A different schema definition was accepted");
            } catch (LifecycleSchemaR31.MigrationConflict expected) {
                assertTrue(expected.getMessage().contains("r31_startup_checkpoint_insert"));
            }
            assertEquals(1L, count(db, "SELECT count(*) FROM sqlite_master WHERE name='r31_startup_checkpoint_insert' AND sql LIKE '%SELECT 1%'"));
        }
    }

    public static void migrationVisibleOnlyAfterCommit(Context context) throws Exception {
        final String name = "lifecycle-visibility";
        try (SQLiteDatabase db = fixture(context, name)) {
            db.enableWriteAheadLogging();
            source(db);
            try (SQLiteDatabase read = SQLiteDatabase.openDatabase(db.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                db.beginTransactionNonExclusive();
                try {
                    LifecycleSchemaR31.Result first = LifecycleSchemaR31.advanceStartupCheckpointWithinMigration(db);
                    assertTrue(first.committedUnit);
                    assertEquals(1L, count(db, "SELECT count(*) FROM sqlite_master WHERE name=?", first.object));
                    assertEquals(0L, count(read, "SELECT count(*) FROM sqlite_master WHERE name=?", first.object));
                } finally { db.endTransaction(); }
                assertFalse(StartupCheckpointIdentityR31.ready(read));
                installReader(db);
                assertTrue(StartupCheckpointIdentityR31.ready(read));
            }
        }
        try (SQLiteDatabase reopened = fixture(context, name)) {
            assertTrue(StartupCheckpointIdentityR31.ready(reopened));
            assertEquals(5L, count(reopened, "SELECT count(*) FROM v57_schema_migration_journal_r926"
                    + " WHERE domain=? AND status='COMPLETED' AND step_id LIKE 'OBJECT:r31_startup_checkpoint_%'",
                    LifecycleSchemaR31.DOMAIN));
        }
    }

    private static void registrationSchema(SQLiteDatabase db) throws Exception {
        transaction(db, owner -> {
            RuntimeSchemaR26.ensure(owner);
            SchemaR27.ensure(owner);
        });
    }

    public static void registrationReadback(Context context) throws Exception {
        try (SQLiteDatabase db = fixture(context, "current-task-manifest")) {
            registrationSchema(db);
            transaction(db, write -> RuntimeCoordinatorR26.registerSessionR34(write, "current"));
            RuntimeCoordinatorR26.requireRegistrationR341(db, "current");
            assertEquals(1L, count(db, "SELECT count(*) FROM r26_task WHERE task_id='r27-atomnet-archive' AND registered_at>0"));
            transaction(db, write -> write.execSQL("DELETE FROM r26_task WHERE task_id=?", new Object[]{TEST_TASK}));
            try {
                RuntimeCoordinatorR26.requireRegistrationR341(db, "current");
                fail("An absent required task was treated as ready");
            } catch (DatabaseCoordinator.SchemaPendingR31 incomplete) {
                assertTrue(incomplete.getMessage().contains(TEST_TASK));
            }
            transaction(db, write -> RuntimeCoordinatorR26.registerSessionR34(write, "current"));
            RuntimeCoordinatorR26.requireRegistrationR341(db, "current");
        }
    }

    public static void staleGenerationCannotClaim(Context context) throws Exception {
        try (SQLiteDatabase db = fixture(context, "claim-takeover")) {
            registrationSchema(db);
            transaction(db, write -> RuntimeCoordinatorR26.registerSessionR34(write, "old"));
            RuntimeCoordinatorR26.Claim[] prior = new RuntimeCoordinatorR26.Claim[1];
            transaction(db, write -> prior[0] = RuntimeCoordinatorR26.claimWithinR31(write, TEST_TASK, "old", "R27", null));
            assertNotNull(prior[0]);
            transaction(db, write -> RuntimeCoordinatorR26.registerSessionR34(write, "new"));
            RuntimeCoordinatorR26.requireRegistrationR341(db, "new");
            try {
                RuntimeCoordinatorR26.requireFence(db, prior[0]);
                fail("The prior owner retained a writable fence");
            } catch (IllegalStateException stale) {
                assertTrue(stale.getMessage().contains("STALE_FENCE"));
            }
            assertEquals(0L, count(db, "SELECT deadline_elapsed_ms FROM r31_task_lease WHERE owner_epoch='old' AND task_id=?",TEST_TASK));
            RuntimeCoordinatorR26.Claim[] active = new RuntimeCoordinatorR26.Claim[1];
            transaction(db, write -> active[0] = RuntimeCoordinatorR26.claimWithinR31(write, TEST_TASK, "new", "R27", null));
            assertNotNull(active[0]);
            assertTrue(active[0].fence > prior[0].fence);
            RuntimeCoordinatorR26.requireFence(db, active[0]);
        }
    }

    public static void unrelatedMissingRoot(Context context) throws Exception {
        try (SQLiteDatabase db = fixture(context, "empty-incoming-checkpoint")) {
            source(db);
            installReader(db);
            StartupCheckpointIdentityR31.Prepared protectedInput = StartupCheckpointIdentityR31.prepare(
                    db, new File(context.getFilesDir(), "unexported-old-cas"));
            assertEquals(0, protectedInput.sourceRows);
            assertEquals(0, protectedInput.examinedOriginalRows);
            transaction(db, protectedInput::requireWithin);
            assertTrue(CheckpointDependencyR341.affectedOwners("HISTORICAL_UNATTRIBUTED_CHECKPOINT").isEmpty());
            assertEquals(java.util.Collections.singletonList("r27-atomnet"),
                    CheckpointDependencyR341.affectedOwners("r27-atomnet:session:13"));
        }
    }

    public static void declaredMissingOriginal(Context context) throws Exception {
        try (SQLiteDatabase db = fixture(context, "original-checkpoint-required")) {
            source(db);
            installReader(db);
            String hash = StatsR27.sha256("required-checkpoint");
            transaction(db, write -> {
                write.execSQL("CREATE TABLE r31_keep_original(archive_table TEXT,row_id INTEGER,row_hash TEXT,bytes INTEGER,state TEXT)");
                write.execSQL("INSERT INTO r29_model_checkpoint VALUES('ATOMNET_TEN_R30','abi','inputs',1,'state',X'AB',0)");
                write.execSQL("INSERT INTO r31_keep_original VALUES(?,1,?,41,'MIGRATED_ORIGINAL_RETAINED')",
                        new Object[]{StartupCheckpointIdentityR31.ARCHIVE,hash});
            });
            File missing = new File(context.getFilesDir(), "required-cas-root");
            try {
                StartupCheckpointIdentityR31.prepare(db, missing);
                fail("An original required for typed equality went unverified");
            } catch (CheckpointDependencyR341.SourceWait wait) {
                assertEquals(CheckpointDependencyR341.State.WAIT_SOURCE, wait.state);
                assertTrue(wait.getMessage().contains(hash));
            }
            assertEquals(1L, count(db, "SELECT count(*) FROM r29_model_checkpoint WHERE hex(artifact)='AB'"));
            assertEquals(1L, count(db, "SELECT count(*) FROM r31_keep_original WHERE row_hash=?",hash));
        }
    }
}
