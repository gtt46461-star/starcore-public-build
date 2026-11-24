package com.ethk3.novaonepro;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Map;

public final class R341ObservationTestCases {
    private R341ObservationTestCases() {}

    private static final String SNAPSHOT = V51ChecksumEngine.sha256("R341_B01_FIXTURE");

    private static RuntimeObservationScopeR341 fixtureScope(String process, String boot, String database,
            long wall, long mono) {
        return new RuntimeObservationScopeR341(ReleaseManifestR926.MANIFEST_SHA256, BuildConfig.SOURCE_COMMIT_R26,
                process, boot, database, wall, mono, null);
    }

    public static void verifyScopeBoundaries() {
        RuntimeObservationScopeR341 source = fixtureScope("main", "bootA", "DB_A", 1000, 1000);
        assertEquals("COMPATIBLE", source.validateJoin(fixtureScope("main", "bootA", "DB_A", 1001, 1001)));
        assertEquals("PROCESS_CHANGED", source.validateJoin(fixtureScope("report", "bootA", "DB_A", 1001, 1001)));
        assertEquals("DATABASE_CHANGED", source.validateJoin(fixtureScope("main", "bootA", "DB_B", 1001, 1001)));
        assertEquals("BOOT_CHANGED", source.validateJoin(fixtureScope("main", "bootB", "DB_A", 1001, 1001)));
        assertEquals("CLOCK_ORDER_UNVERIFIED", source.validateJoin(fixtureScope("main", "bootA", "DB_A", 999, 1001)));
        assertEquals("SNAPSHOT_AND_PROCESS_SAMPLE_DIFFER", source.validateJoin(
                new RuntimeObservationScopeR341(source.releaseId, source.sourceCommit, "main", "bootA",
                        "DB_A", 1001, 1001, SNAPSHOT)));
        RuntimeObservationScopeR341 unknown = new RuntimeObservationScopeR341(source.releaseId, "",
                "main", "bootA", "DB_A", 1001, 1001, null);
        assertEquals("IDENTITY_UNAVAILABLE", source.validateJoin(unknown));
    }

    public static Map<String, Object> runCohort(Context context, boolean addUnrequested) {
        File file = new File(context.getDatabasePath("cohort.sqlite").getParentFile(),
                java.util.UUID.randomUUID() + ".sqlite");
        String database;
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            db.beginTransaction();
            try {
                db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,open_time INTEGER,dirty INTEGER)");
                RuntimeEventLedgerR926.ensureSchema(db);
                RuntimeObservationScopeR341.installWithinTransaction(db);
                database = RuntimeObservationScopeR341.databaseIdentity(db);
                for (int target = 132; target <= 138; target++) {
                    String issue = target(target);
                    append(db, issue, "REQUESTED", "REQUESTED", "request", RuntimeEventLedgerR926.RANK_REQUESTED);
                    if (target == 133 || target == 138) {
                        append(db, issue, "CANONICAL", "CANONICAL_COMMITTED", "canonical",
                                RuntimeEventLedgerR926.RANK_CANONICAL);
                    } else {
                        append(db, issue, "REJECTED", target == 132 ? "HISTORY_HASH_CONFLICT" : "EXPIRED",
                                "failure", RuntimeEventLedgerR926.RANK_ACCEPTED);
                    }
                    if (target < 138) {
                        long opened = target == 133 ? System.currentTimeMillis() + 1 : System.currentTimeMillis() - 1;
                        db.execSQL("INSERT INTO draws VALUES(?,?,0)", new Object[]{issue, opened});
                    }
                }
                for (int i = 0; i < 3; i++) {
                    append(db, target(133), "RECOVERY", "REQUESTED", "recovery:" + i,
                            RuntimeEventLedgerR926.RANK_REQUESTED);
                }
                append(db, target(133), "LEARNED", "LEARNED_EXACTLY_ONCE", "training",
                        RuntimeEventLedgerR926.RANK_LEARNED);
                if (addUnrequested) {
                    // Scoped evidence from the independent settlement lane brackets a missed request.
                    append(db, target(131), "SETTLED", "NO_PREDRAW_CANONICAL_TERMINAL", "no-predraw",
                            RuntimeEventLedgerR926.RANK_SETTLED);
                    db.execSQL("INSERT INTO draws VALUES(?,?,0)", new Object[]{target(131), System.currentTimeMillis() - 1});
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        try (SQLiteDatabase snapshot = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
            RuntimeObservationScopeR341 scope = RuntimeObservationScopeR341.capture(context, database, SNAPSHOT,
                    RuntimeObservationScopeR341.SYSTEM);
            Map<String, Object> result = RuntimeScenarioRecorderR341.exportFixture(snapshot, scope);
            assertEquals("OBSERVED_DECLARED_BOUNDED_RANGE", result.get("state"));
            assertEquals(7, result.get("requestedTargets"));
            assertEquals(addUnrequested ? 7 : 6, result.get("drawnEligibleTargets"));
            assertEquals(1, result.get("futureOrUnobservedLastTarget"));
            assertEquals(2, result.get("canonicalSuccesses"));
            assertEquals(1, result.get("drawnWithPredrawCanonical"));
            return result;
        }
    }

    private static String target(int number) {
        return "20261124" + String.format(java.util.Locale.ROOT, "%04d", number);
    }

    private static void append(SQLiteDatabase db, String issue, String type, String stage, String key, int rank) {
        RuntimeEventLedgerR926.Event event = RuntimeEventLedgerR926.Event.of(issue, "21", "fixture",
                type, stage, rank, "bounded fixture", "R341_TEST", issue + "/" + key);
        assertTrue(RuntimeEventLedgerR926.appendWithinTransaction(db, event));
        assertFalse(RuntimeEventLedgerR926.appendWithinTransaction(db, event));
    }

    public static Map<String, Object> startup(Context context, boolean corruptTail) throws Exception {
        File root = new File(context.getFilesDir(), "r341-journal-fixture");
        RuntimeObservationScopeR341.Clock clock = RuntimeObservationScopeR341.SYSTEM;
        StartupStageJournalR341 previous = StartupStageJournalR341.open(root, "test", clock,
                fixtureScope("old_process", "boot", "DB_A", 1, 1));
        StartupStageJournalR341.Token first = previous.begin("owner", "initial",
                StartupStageJournalR341.Stage.OWNER_BEGIN, null);
        assertTrue(first.beginReceipt.durable);
        StartupStageJournalR341.Token retained = previous.begin("owner", "initial",
                StartupStageJournalR341.Stage.CHECKPOINT_PREPARE, null);
        assertTrue(previous.recordUncaught(retained, new IllegalStateException("CHECKPOINT_MISSING")).durable);
        if (corruptTail) {
            try (RandomAccessFile file = new RandomAccessFile(new File(root, "test.frames"), "rw")) {
                file.seek(file.length());
                file.writeInt(24);
                file.writeInt(0);
                file.writeByte(1);
            }
            assertEquals(true, previous.snapshot().get("partialTail"));
        }
        StartupStageJournalR341 resumed = StartupStageJournalR341.open(root, "test", clock,
                fixtureScope("current_process", "boot", "DB_A", 2, 2));
        assertEquals(1, resumed.recoverOpenAttempts("boot", "current_process"));
        assertEquals(0, resumed.recoverOpenAttempts("boot", "current_process"));
        assertFalse(resumed.end(first, StartupStageJournalR341.Outcome.VERIFIED, "UNSUPPORTED", 0).durable);
        assertTrue(resumed.export().contains("MISSING_SOURCE"));
        assertTrue(resumed.export().contains("INTERRUPTED"));
        assertEquals(StartupStageJournalR341.Outcome.WAIT_RESOURCE,
                StartupStageJournalR341.classify(new IllegalStateException("SQLITE_BUSY")));
        return resumed.snapshot();
    }

    public static void rejectMutableOrForeignSnapshot(Context context) {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.beginTransaction();
            try {
                RuntimeObservationScopeR341.installWithinTransaction(db);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            RuntimeObservationScopeR341 scope = RuntimeObservationScopeR341.capture(context,
                    RuntimeObservationScopeR341.databaseIdentity(db), SNAPSHOT, RuntimeObservationScopeR341.SYSTEM);
            Map<String, Object> view = RuntimeScenarioRecorderR341.exportFixture(db, scope);
            assertEquals("IMMUTABLE_READ_ONLY_SNAPSHOT_REQUIRED", view.get("state"));
            assertEquals("DIAGNOSTIC_ONLY", view.get("datasetRole"));
            assertFalse(view.containsKey("canonicalSuccesses"));
        }
    }
}
