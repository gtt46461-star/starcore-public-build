package com.ethk3.novaonepro.r952;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.Field173NativeEvidence;
import com.ethk3.novaonepro.Models;
import com.ethk3.novaonepro.ResearchSchemaCoordinatorR926;
import com.ethk3.novaonepro.ResearchTaskQueueR967;
import com.ethk3.novaonepro.RuntimeObservationScopeR341;
import com.ethk3.novaonepro.TargetIssueResolver;
import com.ethk3.novaonepro.V51ChecksumEngine;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class R343TrainingNativeTest {
    static final String TARGET = "202611260301";

    @Test
    public void dependencyWaitKeepsOriginalTaskAndCursor() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = open(app);
            try {
                String candidate = "R343-fixture-dependency-wait";
                String inputHash = hash("dependency fixture");
                DatabaseCoordinator owner = store.coordinatorForResearchR952();
                String task = owner.runAtomicWriteOrThrowF01("R343_TEST_SETUP_WAIT", db -> {
                    addCandidate(db, candidate, "NURSERY");
                    String id = ResearchTaskQueueR967.enqueue(
                            db, "TRAIN_CANDIDATE", candidate, TARGET, "fixture-protocol", inputHash, "fixture-action");
                    db.execSQL("UPDATE " + ResearchTaskQueueR967.TABLE + " SET cursor=? WHERE task_id=?",
                            new Object[]{"feature:32", id});
                    return id;
                });
                ResearchTaskQueueR967.Lease lease = owner.runAtomicWriteOrThrowF01("R343_TEST_CLAIM_WAIT", db ->
                        ResearchTaskQueueR967.claimFor(db, "TRAIN_CANDIDATE", candidate, System.currentTimeMillis()));
                checks.require(lease != null, "Original task can be claimed");
                checks.equal(task, lease.id, "Original queue identity");
                checks.equal("feature:32", lease.cursor, "Original cursor");
                RuntimeException injected = rejection("DEPENDENCY", DatabaseCoordinator.CommitOutcome.ROLLED_BACK,
                        new IllegalStateException("R31_WRITER_HOLD_FAULT_BACKLOG_PENDING_DURABLE_DRAIN"));
                checks.require(ResearchTrainingHookRegistryR952.retryInfrastructureCP12(store, lease, injected),
                        "Original training owner handles typed writer backpressure");
                SQLiteDatabase db = store.readable();
                try (Cursor row = db.rawQuery("SELECT t.status,t.cursor,t.input_hash,t.last_error,t.generation,c.state"
                        + " FROM " + ResearchTaskQueueR967.TABLE + " t JOIN r952_candidate c ON c.candidate_id=t.candidate_id"
                        + " WHERE t.task_id=?", new String[]{task})) {
                    checks.require(row.moveToFirst(), "Original task retained");
                    checks.equal("WAIT_INPUT", row.getString(0), "Dependency consumes no training work");
                    checks.equal("feature:32", row.getString(1), "Durable cursor unchanged");
                    checks.equal(inputHash, row.getString(2), "Input identity unchanged");
                    checks.equal("INFRA_DEPENDENCY", row.getString(3), "Retry classification persisted");
                    checks.equal(lease.generation, row.getLong(4), "Wait stays in the original generation");
                    checks.equal("NURSERY", row.getString(5), "Candidate is not quarantined");
                    checks.record("waitState", row.getString(0));
                    checks.record("waitReason", row.getString(3));
                }
                ResearchTaskQueueR967.Lease reclaimed = owner.runAtomicWriteOrThrowF01("R343_TEST_RECLAIM_WAIT", write ->
                        ResearchTaskQueueR967.claimFor(write, "TRAIN_CANDIDATE", candidate, System.currentTimeMillis() + 2_000));
                checks.require(reclaimed != null, "Original task is claimable when backoff ends");
                checks.equal(task, reclaimed.id, "Retry claims the same task");
                checks.equal(lease.cursor, reclaimed.cursor, "Retry claims the same cursor");
                checks.equal(lease.generation + 1, reclaimed.generation, "Retry obtains a fresh fence");
                checks.record("taskId", task);
                checks.record("resumeCursor", reclaimed.cursor);
                checks.record("resumeGeneration", reclaimed.generation);
                database(checks, db, TARGET);
            } finally {
                store.close();
            }
        });
    }

    @Test
    public void typedIntegrityBugAndUnboundedCauseAreNotDependencyWaits() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DatabaseCoordinator.CommitOutcome rolledBack = DatabaseCoordinator.CommitOutcome.ROLLED_BACK;
            Throwable integrity = rejection("INTEGRITY", rolledBack, new IllegalArgumentException("TRAIN_INPUT_HASH"));
            Throwable bug = rejection("BUG", rolledBack, new android.database.sqlite.SQLiteDatabaseLockedException());
            Throwable corrupt = new android.database.sqlite.SQLiteDatabaseCorruptException();
            Throwable constrained = new SQLiteConstraintException();
            RuntimeException cycleOuter = new IllegalStateException("cyclic first");
            RuntimeException cycleInner = new IllegalStateException("cyclic second", cycleOuter);
            cycleOuter.initCause(cycleInner);
            Throwable tooDeep = new IllegalStateException("terminal cause");
            for (int depth = 0; depth < 33; depth++) {
                tooDeep = new IllegalStateException("wrapped " + depth, tooDeep);
            }
            String[] names = {"integrity", "bug", "corrupt", "constraint", "cycle", "depth"};
            Throwable[] faults = {integrity, bug, corrupt, constrained, cycleOuter, tooDeep};
            for (int index = 0; index < faults.length; index++) {
                String reason = ResearchTrainingHookRegistryR952.infrastructureWaitReasonCP12(
                        rejection("DEPENDENCY", rolledBack, faults[index]));
                checks.equal("", reason, names[index] + " must not enter infrastructure retry");
                checks.record(names[index], reason);
            }
            String unknown = ResearchTrainingHookRegistryR952.infrastructureWaitReasonCP12(
                    rejection("DEPENDENCY", DatabaseCoordinator.CommitOutcome.UNKNOWN, null));
            checks.equal("INFRA_COMMIT_RESOLUTION_REQUIRED", unknown, "Ambiguous commit requires reconciliation");
            checks.record("unknownCommit", unknown);
            checks.noDatabase("ENGINEERING_SYNTHETIC", "Typed dispatch policy has no database writes");
        });
    }

    @Test
    public void historyTailUsesActualConsecutiveEpochs() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            List<Models.Draw> rows = new ArrayList<>();
            String target = "202611240001";
            for (int index = 0; index < 2070; index++) {
                rows.add(draw(target, index));
                target = TargetIssueResolver.next(target).targetIssue;
            }
            Models.Draw oldGap = rows.remove(3);
            List<Models.Draw> selected = DevelopmentProductionCP12.selectHistoryCP12(rows, 445, false);
            checks.equal(2066, selected.size(), "Verified suffix is sufficient without crossing the older gap");
            checks.equal(rows.get(3).issue, selected.get(0).issue, "Suffix starts after the missing oldest draw");
            checks.equal(rows.get(rows.size() - 1).issue, selected.get(selected.size() - 1).issue,
                    "Latest usable draw remains the fixed cutoff");
            int gapIndex = rows.size() - 100;
            Models.Draw recentGap = rows.remove(gapIndex);
            String edge = rows.get(gapIndex - 1).issue + "/" + rows.get(gapIndex).issue;
            DevelopmentProductionCP12.Wait held = org.junit.Assert.assertThrows(DevelopmentProductionCP12.Wait.class,
                    () -> DevelopmentProductionCP12.selectHistoryCP12(rows, 445, false));
            checks.equal("WAIT_HISTORY", held.reason, "Gap inside the required suffix blocks training registration");
            checks.require(held.getCause().getMessage().contains("BREAK=" + edge), "Exact missing history boundary is reported");
            checks.require(held.getCause().getMessage().contains("HISTORY_ACTUAL=99"), "Eligible row count is bounded by the gap");
            rows.add(gapIndex, recentGap);
            checks.equal(2066, DevelopmentProductionCP12.selectHistoryCP12(rows, 445, false).size(),
                    "Exact restoration unlocks the original legal history");
            checks.record("originalGap", oldGap.issue);
            checks.record("repairedGap", recentGap.issue);
            checks.record("eligibleSize", selected.size());
            checks.record("cutoff", selected.get(selected.size() - 1).issue);
            checks.noDatabase("ENGINEERING_SYNTHETIC", "Calendar and draw fixtures exercise the production history selector");
        });
    }


    @Test
    public void createdFamilyMakesProgressThroughTheExistingTrainingOwner() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            R342DevelopmentTestCases.independentFamilyRegistration(app);
            DbStore store = new DbStore(app);
            try {
                String candidate;
                String task;
                try (Cursor row = store.readable().rawQuery(
                        "SELECT candidate_id,task_id FROM " + ResearchTaskQueueR967.TABLE
                                + " WHERE kind='TRAIN_CANDIDATE' AND candidate_id LIKE 'CP12-FAMILY-%'", null)) {
                    checks.require(row.moveToFirst(), "The original factory must have enqueued a candidate");
                    candidate = row.getString(0);
                    task = row.getString(1);
                }
                long end = SystemClock.elapsedRealtime() + 100_000L;
                String status = "", cursor = "", reason = "";
                while (SystemClock.elapsedRealtime() < end) {
                    com.ethk3.novaonepro.ResearchWorkBudgetR927.Scope scope =
                            com.ethk3.novaonepro.ResearchWorkBudgetR927.enter(850);
                    try {
                        ResearchTrainingHookRegistryR952.drainTrainingSlotCP11(store,
                                System.nanoTime() + 850_000_000L);
                    } finally {
                        com.ethk3.novaonepro.ResearchWorkBudgetR927.leave(scope);
                    }
                    try (Cursor row = store.readable().rawQuery(
                            "SELECT status,cursor,last_error FROM " + ResearchTaskQueueR967.TABLE + " WHERE task_id=?",
                            new String[]{task})) {
                        checks.require(row.moveToFirst(), "The original training task remains addressable");
                        status = row.getString(0);
                        cursor = row.getString(1);
                        reason = row.getString(2);
                    }
                    if ("COMPLETED".equals(status) || "FAILED_TERMINAL".equals(status)) break;
                    SystemClock.sleep(260);
                }
                checks.equal("COMPLETED", status, "Family training must commit: " + cursor + "|" + reason);
                try (Cursor receipt = store.readable().rawQuery(
                        "SELECT a.numeric_updates,a.validation_sample_n,d.receipt_hash,c.future_n,c.may_create_real,"
                                + " EXISTS(SELECT 1 FROM " + ResearchTaskQueueR967.TABLE
                                + " t WHERE t.candidate_id=c.candidate_id AND t.kind='FREEZE_CANDIDATE')"
                                + " FROM r952_artifact a JOIN research_development_receipt_v2 d ON d.artifact_hash=a.artifact_hash"
                                + " JOIN r952_candidate c ON c.candidate_id=a.candidate_id"
                                + " WHERE a.candidate_id=? AND a.training_kind='DEVELOPMENT_AST3'",
                        new String[]{candidate})) {
                    checks.require(receipt.moveToFirst(), "The same task must produce its own development receipt");
                    checks.require(receipt.getInt(0) > 0, "Numerical training updates must be durable");
                    checks.equal(120, receipt.getInt(1), "Keep the original validation split");
                    checks.require(receipt.getString(2).matches("[0-9a-f]{64}"), "Bind the actual training receipt");
                    checks.equal(0, receipt.getInt(3), "Development rows do not confer future evidence");
                    checks.equal(0, receipt.getInt(4), "Development rows do not confer REAL authority");
                    checks.equal(1, receipt.getInt(5), "Training completion hands off to the existing freeze queue");
                    checks.record("numericUpdates", receipt.getInt(0));
                    checks.record("developmentReceipt", receipt.getString(2));
                }
                database(checks, store.readable(), store.getLatestDrawIssue());
            } finally {
                store.close();
            }
        });
    }

    @Test
    public void originalTerminalMissingCandidateStaysOnHold() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            String original = "e3b213d36a7ed00285d3eb1d5b3c9cdb2ed7c27da335ecc0669430e6276230af";
            String candidate = "CP12-FAMILY-872002fb10d1f206e96eae54";
            String protocol = "68d2ab3bb4937fa695f7e25fbedd42b3153e7f8be7691167c405e234184128d5";
            String input = "2bac10f86871fbee7b0c7eae5111b48499f7b1fe6714fe0f122e8efb720cf004";
            String action = "1f60e3a7e4f9491351ae8b74ffc7cb55891c5c449e39646dcd20f474189ee5cd";
            String target = "202611260092";
            DbStore store = open(app);
            try {
                checks.require(TrainingRecoveryR343.installed(store.readable()), "Recovery migration is installed");
                store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R343_TEST_MISSING_CANDIDATE", db -> {
                    db.execSQL("INSERT INTO " + ResearchTaskQueueR967.TABLE + "(task_id,kind,candidate_id,target_issue,"
                                    + "stream_seq,input_hash,authority_ref,status,cursor,generation,last_error,created_at,updated_at)"
                                    + " VALUES(?,'TRAIN_CANDIDATE',?,?,?,?,?,'FAILED_TERMINAL','fixture:32',7,?,1,2)",
                            new Object[]{original, candidate, target, protocol, input, action, TrainingRecoveryR343.REASON});
                    return null;
                });
                long deadline = System.nanoTime() + 30_000_000_000L;
                checks.equal(false, TrainingRecoveryR343.drainOne(store, deadline), "Missing original cannot be resumed");
                String hold = store.getState("r343_recovery_hold_" + original, "");
                checks.require(hold.endsWith("|WAIT_ORIGINAL_CANDIDATE"), "Hold identifies the missing original candidate");
                checks.equal(false, TrainingRecoveryR343.drainOne(store, deadline), "Immediate retry keeps the same hold");
                checks.equal(hold, store.getState("r343_recovery_hold_" + original, ""), "Retry does not rewrite the hold");
                try (Cursor row = store.readable().rawQuery("SELECT status,cursor,generation,input_hash,last_error FROM "
                        + ResearchTaskQueueR967.TABLE + " WHERE candidate_id=?", new String[]{candidate})) {
                    checks.require(row.moveToFirst(), "Original terminal record retained");
                    checks.equal("FAILED_TERMINAL", row.getString(0), "Terminal state unchanged");
                    checks.equal("fixture:32", row.getString(1), "Prior cursor unchanged");
                    checks.equal(7, row.getInt(2), "Prior generation unchanged");
                    checks.equal(input, row.getString(3), "Prior input identity unchanged");
                    checks.equal(TrainingRecoveryR343.REASON, row.getString(4), "Prior error unchanged");
                    checks.require(!row.moveToNext(), "No second task was fabricated");
                }
                try (Cursor receipts = store.readable().rawQuery("SELECT count(*) FROM " + TrainingRecoveryR343.TABLE, null)) {
                    checks.require(receipts.moveToFirst(), "Recovery receipt count read");
                    checks.equal(0, receipts.getInt(0), "No recovery receipt exists without the original");
                }
                checks.record("taskId", original);
                checks.record("hold", hold);
                database(checks, store.readable(), target);
            } finally {
                store.close();
            }
        });
    }

    private static RuntimeException rejection(String type, DatabaseCoordinator.CommitOutcome outcome, Throwable cause) {
        return new DatabaseCoordinator.WriteRejectedR31(type, "CP12_TRAIN_FEATURE_PAGE", "ADMISSION", outcome, cause);
    }

    static DbStore open(Context app) {
        DbStore store = new DbStore(app);
        long deadline = System.nanoTime() + 150_000_000_000L;
        boolean ready = false;
        try {
            while (!ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorForResearchR952(), 8)) {
                if (System.nanoTime() >= deadline) throw new AssertionError(ResearchSchemaCoordinatorR926.summary());
                SystemClock.sleep(15);
            }
            ready = true;
            return store;
        } finally {
            if (!ready) store.close();
        }
    }

    static void addCandidate(SQLiteDatabase db, String candidate, String state) {
        db.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,state,created_issue,may_create_real,created_at)"
                        + " VALUES(?,?,?,?,?,?,0,?)",
                new Object[]{candidate, "fixture-intent", "fixture-ast", hash(candidate), state, TARGET, System.currentTimeMillis()});
    }

    static Models.Draw draw(String target, int seed) {
        Models.Draw draw = new Models.Draw();
        draw.issue = target;
        draw.openTime = 1_795_651_200_000L + seed * 60_000L;
        draw.fetchTime = draw.openTime + 250;
        for (int position = 0; position < draw.numbers.length; position++) {
            draw.numbers[position] = (seed + position) % draw.numbers.length + 1;
        }
        return draw;
    }

    static String hash(String value) {
        return V51ChecksumEngine.sha256(value);
    }

    static void database(Field173NativeEvidence.Checks checks, SQLiteDatabase db, String target)
            throws Exception {
        String identity = RuntimeObservationScopeR341.databaseIdentity(db);
        checks.require(identity.matches("[a-f0-9-]{36}"), "Production DB identity installed");
        checks.database("ENGINEERING_SYNTHETIC", target, 1_795_651_200_000L, identity);
    }
}
