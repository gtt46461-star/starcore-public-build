package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbHelper;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.DrawHashR36;
import com.ethk3.novaonepro.Field173NativeEvidence;
import com.ethk3.novaonepro.Models;
import com.ethk3.novaonepro.ResearchTaskQueueR967;
import com.ethk3.novaonepro.TargetIssueResolver;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import static com.ethk3.novaonepro.r952.R343TrainingNativeTest.TARGET;
import static com.ethk3.novaonepro.r952.R343TrainingNativeTest.addCandidate;
import static com.ethk3.novaonepro.r952.R343TrainingNativeTest.database;
import static com.ethk3.novaonepro.r952.R343TrainingNativeTest.draw;
import static com.ethk3.novaonepro.r952.R343TrainingNativeTest.hash;
import static com.ethk3.novaonepro.r952.R343TrainingNativeTest.open;
import static org.junit.Assert.assertThrows;

@RunWith(AndroidJUnit4.class)
public final class R343RollingNativeTest {
    @Test
    public void settlementEventIsSingleAndImmutable() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = open(app);
            try {
                SQLiteDatabase db = store.readable();
                checks.require(RollingLearnerR343.installed(db), "Rolling schema installed by startup migration");
                DatabaseCoordinator owner = store.coordinatorForResearchR952();
                String latest = owner.runAtomicWriteOrThrowF01("R343_TEST_SETTLED_EVENTS", write -> {
                    String target = TARGET;
                    String last = "";
                    for (int index = 0; index < 10; index++) {
                        Models.Draw row = draw(target, index);
                        String drawHash = DrawHashR36.sha256V2(row);
                        String settlement = hash("R343 settlement fixture|" + row.issue);
                        long completed = row.openTime + 1_000;
                        write.execSQL("INSERT INTO " + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                        + "(target_issue,draw_issue,status,created_at,updated_at,completed_at,draw_hash,settlement_hash)"
                                        + " VALUES(?,?,'COMPLETED',?,?,?,?,?)",
                                new Object[]{target, target, completed, completed, completed, drawHash, settlement});
                        RollingLearnerR343.settled(write, row, settlement, completed);
                        RollingLearnerR343.settled(write, row, settlement, completed);
                        last = target;
                        target = TargetIssueResolver.next(target).targetIssue;
                    }
                    return last;
                });
                checks.equal(10L, scalar(db, "SELECT count(*) FROM " + RollingLearnerR343.EVENT, null),
                        "Duplicate delivery does not create a second rolling event");
                try (Cursor latestEvent = db.rawQuery("SELECT draw_hash,settlement_hash,completed_at,receipt_hash FROM "
                        + RollingLearnerR343.EVENT + " WHERE target_issue=?", new String[]{latest})) {
                    checks.require(latestEvent.moveToFirst(), "Latest event retained");
                    checks.equal(DrawHashR36.sha256V2(draw(latest, 9)), latestEvent.getString(0), "Draw identity retained");
                    checks.equal(hash("R343 settlement fixture|" + latest), latestEvent.getString(1), "Settlement identity retained");
                    checks.equal(draw(latest, 9).openTime + 1_000, latestEvent.getLong(2), "Settlement completion retained");
                    checks.record("latestReceipt", latestEvent.getString(3));
                }
                assertThrows(IllegalStateException.class, () ->
                        owner.runAtomicWriteOrThrowF01("R343_TEST_WRONG_SETTLEMENT", write -> {
                            RollingLearnerR343.settled(write, draw(latest, 9), hash("wrong"), draw(latest, 9).openTime + 1_000);
                            return null;
                        }));
                checks.equal(10L, scalar(db, "SELECT count(*) FROM " + RollingLearnerR343.EVENT, null),
                        "Wrong settlement authority creates no event");
                assertThrows(SQLiteConstraintException.class, () -> db.execSQL("UPDATE " + RollingLearnerR343.EVENT
                        + " SET completed_at=completed_at+1 WHERE target_issue=?", new Object[]{latest}));
                checks.equal(draw(latest, 9).openTime + 1_000, scalar(db, "SELECT completed_at FROM "
                        + RollingLearnerR343.EVENT + " WHERE target_issue=?", new String[]{latest}), "Event bytes remain immutable");
                checks.record("latestTarget", latest);
                checks.record("recordedEvents", 10);
                database(checks, db, latest);
            } finally {
                store.close();
            }
        });
    }

    @Test
    public void staleSourceAndFullWorkingBudgetYieldWithinOriginalWriter() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = open(app);
            try {
                DatabaseCoordinator owner = store.coordinatorForResearchR952();
                for (boolean checkSource : new boolean[]{true, false}) {
                    String label = checkSource ? "source" : "storage";
                    String candidate = "fixture-budget-" + label;
                    String selected = owner.runAtomicWriteOrThrowF01("R343_TEST_ROLLING_YIELD", db -> {
                        String task = ResearchTaskQueueR967.enqueue(db, RollingLearnerR343.KIND, candidate, TARGET,
                                "fixture-protocol-" + label, hash(label), candidate);
                        db.execSQL("UPDATE " + ResearchTaskQueueR967.TABLE + " SET cursor=? WHERE task_id=?",
                                new Object[]{"FEATURES|32|fixture-prefix", task});
                        ResearchTaskQueueR967.Lease lease = ResearchTaskQueueR967.claimFor(
                                db, RollingLearnerR343.KIND, candidate, System.currentTimeMillis());
                        checks.require(lease != null, "Original rolling task authority obtained");
                        long epoch = RollingLearnerR343.sourceEpochR343(db);
                        String reason = RollingLearnerR343.writeWaitReasonR343(
                                db, checkSource ? 18 : (64L << 20) + 1, checkSource, epoch + 1);
                        checks.equal(checkSource ? "ROLLING_SOURCE_CHANGED_DURING_COMMIT" : "ROLLING_STORAGE_CAPACITY",
                                reason, "Transient wait diagnosed inside the original writer");
                        checks.require(RollingLearnerR343.waitWithinR343(db, lease, reason), "Original writer persists yield");
                        return task;
                    });
                    try (Cursor row = store.readable().rawQuery("SELECT status,cursor,last_error,lease_owner FROM "
                            + ResearchTaskQueueR967.TABLE + " WHERE task_id=?", new String[]{selected})) {
                        checks.require(row.moveToFirst(), "Original lease read back");
                        checks.equal("YIELDED", row.getString(0), "Expected wait is not a writer fault");
                        checks.equal("FEATURES|32|fixture-prefix", row.getString(1), "Wait does not advance the feature cursor");
                        checks.equal("", row.getString(3), "Owner released by the same transaction");
                        checks.record(label + "Wait", row.getString(2));
                    }
                }
                checks.equal(0L, scalar(store.readable(), "SELECT COUNT(*) FROM " + RollingLearnerR343.OFFER, null),
                        "Waiting tasks create no independent publication offer");
                database(checks, store.readable(), TARGET);
            } finally {
                store.close();
            }
        });
    }

    @Test
    public void cleanupContinuesPastPinnedPageWithoutErasingHeadOrActiveInput() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            String cutoff = TARGET;
            String reclaimable = "v17";
            DbStore store = open(app);
            try {
                DatabaseCoordinator owner = store.coordinatorForResearchR952();
                owner.runAtomicWriteOrThrowF01("R343_TEST_ROLLING_PIN_SEED", db -> {
                    for (int index = 1; index <= 19; index++) {
                        String version = String.format(Locale.ROOT, "v%02d", index);
                        String candidate = "r343-cleanup-fixture-" + version;
                        addCandidate(db, candidate, "NURSERY");
                        addVersion(db, version, candidate, cutoff,
                                index == 19 ? "FEATURES" : index == 17 ? "FAILED" : "COMPLETE");
                        if (index <= 16) {
                            db.execSQL("INSERT INTO " + RollingLearnerR343.OFFER
                                            + " VALUES(?,?,?,?,?,'NEEDS_INDEPENDENT_PUBLICATION_ADAPTER',0)",
                                    new Object[]{version, candidate, cutoff, hash("model|" + version), hash("receipt|" + version)});
                            checks.require(RollingFrozenPublisherR343.pinsRawVersion(db, version),
                                    "Independent unsuperseded offer pins " + version);
                        } else if (index == 18) {
                            db.execSQL("INSERT INTO " + RollingLearnerR343.HEAD + "(candidate_id,version_id,updated_at) VALUES(?,?,0)",
                                    new Object[]{candidate, version});
                        }
                    }
                    for (int index = 0; index < 65; index++) {
                        db.execSQL("INSERT INTO " + RollingLearnerR343.FEATURE + " VALUES(?,?,?,?,?,?,?,?,?)",
                                new Object[]{reclaimable, index, cutoff, 1, hash("raw|" + index), new byte[18],
                                        hash("feature|" + index), "{}", hash("original")});
                    }
                    return null;
                });
                checks.equal(false, owner.runAtomicWriteOrThrowF01("R343_TEST_ROLLING_PIN_PAGE", RollingLearnerR343::cleanup),
                        "First bounded pass skips all pending publications");
                checks.equal("v16", store.getState("r343_rolling_cleanup_after", ""), "Cursor advances through pinned page");
                SQLiteDatabase db = store.readable();
                try (SQLiteDatabase reopened = SQLiteDatabase.openDatabase(db.getPath(), null,
                        SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                     Cursor cursor = reopened.rawQuery("SELECT state_value FROM app_state WHERE state_key=?",
                             new String[]{"r343_rolling_cleanup_after"})) {
                    checks.require(cursor.moveToFirst(), "A new SQLite reader finds the committed cleanup cursor");
                    checks.equal("v16", cursor.getString(0), "Committed cleanup cursor is visible to new readers");
                }
                checks.equal(true, owner.runAtomicWriteOrThrowF01(
                        "R343_TEST_ROLLING_PIN_RESUME", RollingLearnerR343::cleanup), "Reopened cleanup reaches version 17");
                checks.equal(1L, scalar(db, "SELECT COUNT(*) FROM " + RollingLearnerR343.FEATURE + " WHERE version_id=?",
                        new String[]{reclaimable}), "One committed cleanup removes at most 64 feature rows");
                checks.equal(16L, scalar(db, "SELECT count(*) FROM " + RollingLearnerR343.VERSION
                        + " WHERE version_id<='v16' AND input_blob IS NOT NULL", null), "All pending originals remain available");
                checks.equal(4L, scalar(db, "SELECT length(model_payload) FROM " + RollingLearnerR343.VERSION
                        + " WHERE version_id='v18'", null), "HEAD model remains available");
                checks.equal(4L, scalar(db, "SELECT length(input_blob) FROM " + RollingLearnerR343.VERSION
                        + " WHERE version_id='v19'", null), "Active input remains available");
                checks.record("lastScannedVersion", store.getState("r343_rolling_cleanup_after", ""));
                checks.record("remainingFeatures", scalar(db, "SELECT COUNT(*) FROM " + RollingLearnerR343.FEATURE
                        + " WHERE version_id=?", new String[]{reclaimable}));
                database(checks, db, cutoff);
            } finally {
                store.close();
            }
        });
    }

    @Test
    public void legacyAccountingResumesWithExactPrefixAfterRollback() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = open(app);
            try {
                DatabaseCoordinator owner = store.coordinatorForResearchR952();
                SQLiteDatabase db = store.readable();
                owner.runAtomicWriteOrThrowF01("R343_TEST_LEGACY_ROLLING_STORAGE", write -> {
                    // Reproduce a candidate upgraded from the sealed rolling schema.
                    for (String suffix : new String[]{"version_insert", "version_update", "feature_insert", "feature_delete"}) {
                        write.execSQL("DROP TRIGGER r343_resident_" + suffix);
                    }
                    write.execSQL("DROP TABLE " + RollingStorageAccountingR343.RESIDENT);
                    write.execSQL("DROP TABLE " + RollingStorageAccountingR343.CAPACITY);
                    for (int index = 0; index < 12; index++) {
                        String version = String.format(Locale.ROOT, "v%02d", index);
                        String candidate = "fixture-legacy-accounting-" + version;
                        addCandidate(write, candidate, "NURSERY");
                        addVersion(write, version, candidate, TARGET, "FAILED");
                        for (int row = 0; row < 8; row++) addFeature(write, version, row);
                    }
                    RollingStorageAccountingR343.schema(write);
                    return null;
                });
                checks.equal(-1L, RollingStorageAccountingR343.liveBytes(db), "Legacy bytes are withheld until exact");
                assertThrows(IllegalStateException.class, () -> owner.runAtomicWriteOrThrowF01("R343_TEST_RECONCILE_ROLLBACK", write -> {
                    RollingStorageAccountingR343.reconcileOne(write);
                    throw new IllegalStateException("ROLLBACK_RECONCILIATION_PAGE");
                }));
                checks.equal(0L, scalar(db, "SELECT live_bytes FROM " + RollingStorageAccountingR343.CAPACITY, null),
                        "Page and cursor do not survive rollback");
                checks.equal(true, owner.runAtomicWriteOrThrowF01("R343_TEST_RECONCILE_FIRST", RollingStorageAccountingR343::reconcileOne),
                        "First page scans only eight version payloads");
                checks.equal(64L, scalar(db, "SELECT live_bytes FROM " + RollingStorageAccountingR343.CAPACITY, null),
                        "First page counted exactly eight original eight-byte payloads");
                owner.runAtomicWriteOrThrowF01("R343_TEST_RECONCILE_MUTATION", write -> {
                    write.execSQL("UPDATE " + RollingLearnerR343.VERSION + " SET input_blob=NULL WHERE version_id='v00'");
                    addFeature(write, "v00", 8);
                    return null;
                });
                for (int page = 0; page < 8 && RollingStorageAccountingR343.liveBytes(db) < 0; page++) {
                    owner.runAtomicWriteOrThrowF01("R343_TEST_RECONCILE_RESUME", RollingStorageAccountingR343::reconcileOne);
                }
                long expected = 12 * 8 - 4 + 97 * 22;
                checks.equal(expected, RollingStorageAccountingR343.liveBytes(db),
                        "Reconciliation reads actual legacy bytes and counts covered mutations once");
                owner.runAtomicWriteOrThrowF01("R343_TEST_RECONCILE_DELETE", write -> {
                    write.execSQL("DELETE FROM " + RollingLearnerR343.FEATURE + " WHERE version_id='v00' AND row_index=8");
                    return null;
                });
                checks.equal(expected - 22, RollingStorageAccountingR343.liveBytes(db),
                        "A committed raw deletion releases only its exact stored UTF-8 byte count");
                checks.equal(12L, scalar(db, "SELECT count(*) FROM " + RollingLearnerR343.VERSION, null),
                        "All compact immutable version rows remain");
                checks.record("bytesBeforeDeletion", expected);
                checks.record("bytesAfterDeletion", RollingStorageAccountingR343.liveBytes(db));
                database(checks, db, TARGET);
            } finally {
                store.close();
            }
        });
    }

    private static void addFeature(SQLiteDatabase db, String version, int row) {
        db.execSQL("INSERT INTO " + RollingLearnerR343.FEATURE + " VALUES(?,?,?,?,?,?,?,?,?)",
                new Object[]{version, row, TARGET, 1, hash(version + "|raw|" + row), new byte[18],
                        hash(version + "|feature|" + row), "\"é\"", hash("original")});
    }

    private static void addVersion(SQLiteDatabase db, String version, String candidate, String cutoff, String state) {
        db.execSQL("INSERT INTO " + RollingLearnerR343.VERSION
                        + "(version_id,candidate_id,predecessor,cutoff_issue,input_first_issue,ast_hash,protocol_hash,"
                        + "protocol_json,input_hash,input_bytes,input_blob,train_n,train_first,train_last,"
                        + "validation_first,validation_last,seed,state,prefix_hash,model_payload,created_at,updated_at)"
                        + " VALUES(?,?,'',?,?,?,?,?,?,?,?,300,?,?,?,?,0,?,?,?,0,0)",
                new Object[]{version, candidate, cutoff, cutoff, hash(candidate), hash("protocol|" + version),
                        "{}", hash("input|" + version), 4, new byte[4], cutoff, cutoff, cutoff, cutoff, state,
                        hash("prefix|" + version), "test"});
    }

    static long scalar(SQLiteDatabase db, String sql, String[] args) {
        try (Cursor c = db.rawQuery(sql, args)) {
            if (!c.moveToFirst()) throw new AssertionError("Expected scalar row");
            return c.getLong(0);
        }
    }
}
