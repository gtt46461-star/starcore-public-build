package com.ethk3.novaonepro;

import android.database.Cursor;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Owned synchronous production chain on Android; service lifecycle has a separate C07 contract. */
@RunWith(AndroidJUnit4.class)
public final class ShadowCycleAndroidTest {
    private static Models.Draw draw(int row, long openTime) {
        Models.Draw d = new Models.Draw();
        d.issue = String.format(Locale.US, "20260910%04d", row);
        d.openTime = openTime;
        for (int p = 0; p < 10; p++) d.numbers[p] = ((p + row - 1) % 10) + 1;
        assertTrue(d.isValid());
        return d;
    }

    private static long count(DbStore store, String table, String issue) {
        try (Cursor c = store.readable().rawQuery("SELECT COUNT(*) FROM " + table
                + " WHERE target_issue=?", new String[]{issue})) {
            assertTrue(c.moveToFirst());
            return c.getLong(0);
        }
    }

    private static Models.Decision predict(DbStore store, ArrayList<Models.Draw> history,
            String target) throws Exception {
        Models.Draw base = history.get(history.size() - 1);
        AnalysisRequestStoreR36 requests = new AnalysisRequestStoreR36(store);
        AnalysisSingleFlightR36.Request request = requests.ensurePending(base.issue, target,
                TargetIssueResolver.predrawDeadlineWallR20261008(store.getLatestDraw()),
                "SYNTHETIC_SHADOW_CYCLE_NOT_NATURAL");
        AnalysisSingleFlightR36.SubmitResult submitted = AnalysisSingleFlightR36.shared().submit(request);
        assertTrue("isolated owner: target=" + target + " owner=" + submitted.ownerTarget
                + " pending=" + submitted.pendingTarget, submitted.startNow);
        assertNotNull(submitted.lease);
        AnalysisSingleFlightR36.Lease lease = submitted.lease;
        lease.bindCurrentThread();
        try {
            request = requests.claim(request, lease.ownerId, System.currentTimeMillis());
            assertNotNull(request);
            V51RuntimeInputR36 input = V51RuntimeInputR36.freeze(history, request);
            request = requests.bindHistoryHash(request, input.historyHash, lease.ownerId);
            V51RuntimeOrchestrator.OutcomeR36 out = V51AutoFetchBridge.runR36(store, input, request, lease);
            assertNotNull(out);
            assertTrue(out.runtime.success);
            assertTrue(out.commit.success);
            assertEquals(1200, out.runtime.actions.length);
            assertEquals("CANONICAL_COMMITTED", requests.status(target));
            Models.Decision view = CanonicalDecisionViewAdapterR36.read(store, target);
            assertEquals(target, view.targetIssue);
            assertTrue(view.position >= 1 && view.position <= 10);
            assertTrue(DecisionGateEngine.b12K3(view.n1, view.n2, view.n3));
            return view;
        } finally {
            AnalysisSingleFlightR36.shared().releaseAndTakeNext(lease);
        }
    }

    @Test public void canonicalCommitReopensCaptureOnTheSameDraw() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = new DbStore(app);
            try {
                Field173NativeEvidence.prepareResearchSchema(store);
                ArrayList<Models.Draw> history = new ArrayList<>();
                long wall = System.currentTimeMillis();
                for (int row = 1; row <= 240; row++) history.add(draw(row, wall - (240 - row) * 60_000L));
                Models.Draw base = history.get(history.size() - 1);
                String target = TargetIssueResolver.next(base.issue).targetIssue;
                checks.equal(240, store.saveDraws(history), "The production history writer committed the fixture");
                checks.equal("", ResearchCaptureQueueR946.recover(store), "No snapshot exists before normal analysis");

                String laneStatus = "";
                for (int step = 0; step < 4 && !laneStatus.contains("NO_PENDING_PREDRAW_SURFACE"); step++) {
                    laneStatus = ResearchLearningContinuityR912.pulse(store, 1000L).status;
                    SystemClock.sleep(70L);
                }
                checks.require(laneStatus.contains("NO_PENDING_PREDRAW_SURFACE"),
                        "Continuity reviewed lane 0 before the canonical write: " + laneStatus);
                try (Cursor cursor = store.readable().rawQuery("SELECT served_issue,active_task_id FROM "
                        + ResearchTaskQueueR967.LANE + " WHERE lane=0", null)) {
                    checks.require(cursor.moveToFirst(), "The closed lane has a durable cursor");
                    checks.equal(base.issue, cursor.getString(0), "The latest draw has already been served");
                    checks.equal("", cursor.getString(1), "The first review no longer owns a task");
                }

                predict(store, history, target);
                checks.equal(base.issue, store.getLatestDraw().issue, "Canonical commit does not advance the draw");
                checks.equal(target, ResearchCaptureQueueR946.recover(store), "The newly committed snapshot is pending");
                checks.equal(0L, count(store, FixedResearchWindowR932.CHAMPION, target), "Research capture has not yet run");

                String capture = "";
                for (int step = 0; step < 12 && count(store, FixedResearchWindowR932.CHAMPION, target) == 0; step++) {
                    capture = ResearchLearningContinuityR912.pulse(store, 1000L).status;
                    SystemClock.sleep(100L);
                }
                checks.equal(1L, count(store, FixedResearchWindowR932.CHAMPION, target),
                        "The resumed lane durably captured the exact target before the draw: " + capture);
                checks.equal(0L, count(store, DbHelper.TABLE_R35_OOS_LABEL, target), "No actual draw has been supplied");
                checks.equal(0L, count(store, DbHelper.TABLE_TRADES, target), "Synthetic capture cannot authorize REAL");
                String hash;
                long cutoff = TargetIssueResolver.predrawDeadlineWallR20261008(base);
                try (Cursor cursor = store.readable().rawQuery("SELECT prediction_hash,frozen_at FROM "
                        + FixedResearchWindowR932.CHAMPION + " WHERE target_issue=?", new String[]{target})) {
                    checks.require(cursor.moveToFirst(), "Frozen research result is readable");
                    hash = cursor.getString(0);
                    checks.require(hash.length() == 64 && cursor.getLong(1) < cutoff, "Frozen identity precedes the cutoff");
                }
                Models.Draw actual = draw(241, System.currentTimeMillis());
                checks.equal(1, store.saveDraws(Collections.singletonList(actual)), "The exact synthetic draw is durable");
                checks.equal("", ResearchCaptureQueueR946.recover(store), "The exact draw closes capture eligibility");
                checks.database("SYNTHETIC_ANDROID_OWNER", target, cutoff,
                        RuntimeObservationScopeR341.databaseIdentity(store.readable()));
                checks.record("baseIssue", base.issue);
                checks.record("predrawHash", hash);
                checks.record("durableLaneBeforeCanonical", laneStatus);
                checks.record("captureAfterCanonical", capture);
            } finally {
                store.close();
            }
        });
    }

    @Test public void eachPredrawSettlesOnceAndFeedsTheNextCycle() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        // Application startup schedules the real watchdog even for instrumentation. Use the
        // existing user-stop boundary in this disposable fixture before seeding its history.
        // Never reset or steal a live single-flight lease to make the assertion pass.
        BootReceiver.persistServiceUserEnabled(context, false);
        BootReceiver.cancelAllRecovery(context, "SYNTHETIC_OWNED_CHAIN_FIXTURE");
        context.stopService(new Intent(context, AutoFetchService.class));
        long stoppedBy = SystemClock.elapsedRealtime() + 10000L;
        while (AnalysisSingleFlightR36.shared().currentLease() != null
                && SystemClock.elapsedRealtime() < stoppedBy) SystemClock.sleep(20L);
        assertNull("watchdog owner must release normally", AnalysisSingleFlightR36.shared().currentLease());
        DbStore store = new DbStore(context);
        try {
            Field173NativeEvidence.prepareResearchSchema(store);
            ArrayList<Models.Draw> history = new ArrayList<>();
            long latest = System.currentTimeMillis();
            for (int row = 1; row <= 240; row++)
                history.add(draw(row, latest - (240 - row) * 60000L));
            assertEquals(240, store.saveDraws(history));
            for (int row = 241; row <= 243; row++) {
                String target = String.format(Locale.US, "20260910%04d", row);
                Models.Decision before = predict(store, history, target);
                assertEquals(1, count(store, DbHelper.TABLE_R35_SHADOW_OBSERVATION, target));
                assertEquals(0, count(store, DbHelper.TABLE_R35_OOS_LABEL, target));
                assertEquals("unqualified synthetic history must not authorize REAL", 0,
                        count(store, DbHelper.TABLE_TRADES, target));

                // Produce the next synthetic outcome only after the real pipeline froze its pick.
                SystemClock.sleep(2L);
                Models.Draw actual = draw(row, System.currentTimeMillis());
                assertEquals(1, store.saveDraws(Collections.singletonList(actual)));
                actual = store.getDrawByIssue(target);
                SettlementEngine.SettlementReport settled = SettlementCoordinatorR815.closeCurrentExactR906(
                        store, actual, SettlementCoordinatorR815.Reason.AUTHORITATIVE_LATEST);
                assertEquals(settled.detail, 1, settled.ok);
                assertEquals(settled.detail, 1, settled.shadowSettledCount);
                SettlementTaskStoreR36.Terminal terminal = SettlementTaskStoreR36.terminal(store, target);
                assertNotNull(terminal);
                assertEquals(SettlementTaskStoreR36.COMPLETED, terminal.status);
                assertEquals(1, count(store, DbHelper.TABLE_R35_OOS_LABEL, target));
                int value = actual.getNumberAtPosition(before.position);
                int hit = value == before.n1 || value == before.n2 || value == before.n3 ? 1 : 0;
                try (Cursor c = store.readable().rawQuery("SELECT settled,actual_number,hit_label,n1,n2,n3 FROM "
                        + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " WHERE target_issue=?", new String[]{target})) {
                    assertTrue(c.moveToFirst());
                    assertEquals(1, c.getInt(0)); assertEquals(value, c.getInt(1)); assertEquals(hit, c.getInt(2));
                    assertEquals(before.n1, c.getInt(3)); assertEquals(before.n2, c.getInt(4));
                    assertEquals(before.n3, c.getInt(5));
                }
                SettlementEngine.SettlementReport replay = SettlementCoordinatorR815.closeCurrentExactR906(
                        store, actual, SettlementCoordinatorR815.Reason.WATCHDOG);
                assertEquals(replay.detail, 1, replay.ok);
                assertEquals(0, replay.shadowSettledCount);
                assertEquals(terminal.settlementHash,
                        SettlementTaskStoreR36.terminal(store, target).settlementHash);
                assertEquals(1, count(store, DbHelper.TABLE_R35_OOS_LABEL, target));
                assertEquals(0, count(store, DbHelper.TABLE_TRADES, target));
                history.add(actual);
            }
            System.out.println("SHADOW_CYCLE_ANDROID_PASS|periods=3|canonical=3|settled=3"
                    + "|idempotent_replays=3|real=0|scope=SYNTHETIC_OWNED_CHAIN_NOT_NATURAL_NOT_PHONE");
        } finally {
            store.close();
        }
    }
}
