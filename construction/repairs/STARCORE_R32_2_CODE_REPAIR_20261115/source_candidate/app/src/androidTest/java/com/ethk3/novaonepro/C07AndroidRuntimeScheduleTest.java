package com.ethk3.novaonepro;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Debug;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * C07 Android-runtime acceptance over the exact production scheduler and the app's real SQLite
 * coordinator. This is emulator/device runtime evidence, not a Future-OOS effect claim and not a
 * physical-phone performance claim.
 */
@RunWith(AndroidJUnit4.class)
public final class C07AndroidRuntimeScheduleTest {
    private static String scalar(SQLiteDatabase db, String sql, String... args) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args == null || args.length == 0 ? null : args);
            return c.moveToFirst() ? c.getString(0) : "";
        } finally {
            if (c != null) c.close();
        }
    }

    private static String lastSelected(SQLiteDatabase db, String key) {
        return scalar(
                db,
                "SELECT target_issue FROM "
                        + GlobalResearchScheduleR932.TASK
                        + " WHERE candidate_key=? AND selected=1 ORDER BY target_issue DESC LIMIT 1",
                key);
    }

    private static List<GlobalResearchScheduleR932.Candidate> roster(SQLiteDatabase db) {
        ArrayList<GlobalResearchScheduleR932.Candidate> rows =
                new ArrayList<GlobalResearchScheduleR932.Candidate>();
        for (String tier : Arrays.asList("A", "B", "C")) {
            for (int family = 0; family < 8; family++) {
                for (int i = 0; i < 20; i++) {
                    String key = tier + ":android:" + family + ":" + i;
                    rows.add(
                            new GlobalResearchScheduleR932.Candidate(
                                    key,
                                    "C07_ANDROID_FAMILY_" + family,
                                    V51ChecksumEngine.sha256("C07_ANDROID_STRUCTURE|" + key),
                                    tier,
                                    lastSelected(db, key)));
                }
            }
        }
        return rows;
    }

    private static void freeze(
            DbStore store,
            String target,
            List<GlobalResearchScheduleR932.Candidate> rows) {
        // Match the actual writer and its transaction-local schema memoization.
        // A raw beginTransaction bypasses both and repeats DDL for every candidate.
        assertTrue(store.coordinatorR36().runAtomicWrite("C07_ANDROID_FREEZE", db -> {
            GlobalResearchScheduleR932.freeze(
                    db,
                    target,
                    V51ChecksumEngine.sha256("C07_ANDROID_SURFACE|" + target),
                    rows);
        }));
    }

    private static int selected(SQLiteDatabase db, String target, String tier) {
        return Integer.parseInt(
                scalar(
                        db,
                        "SELECT COUNT(*) FROM "
                                + GlobalResearchScheduleR932.TASK
                                + " WHERE target_issue=? AND tier=? AND selected=1",
                        target,
                        tier));
    }

    private static int distinctEverSelected(SQLiteDatabase db, String tier) {
        return Integer.parseInt(
                scalar(
                        db,
                        "SELECT COUNT(DISTINCT candidate_key) FROM "
                                + GlobalResearchScheduleR932.TASK
                                + " WHERE tier=? AND selected=1",
                        tier));
    }

    @Test
    public void exactProductionSchedulerRunsOnAndroidSQLiteWithCurrentFairCaps() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Instrumentation owns this isolated debug install. Start from an empty production DB so no
        // prior app state can accidentally make the fairness assertion pass.
        context.deleteDatabase(AppConfig.DB_NAME);
        DbStore store = new DbStore(context);
        SQLiteDatabase db = store.writable();
        try {
            assertTrue(store.coordinatorR36().runAtomicWrite(
                    "C07_ANDROID_SCHEMA", tx -> GlobalResearchScheduleR932.schema(tx)));
            // Fixed baseline and ALL_SOURCE_ANSWER in the supplied optimized299 plan
            // both specify 650/1200 ms and A8/B128/C64. Do not regress production to
            // the obsolete R932 A8/B24/C64 fixture to satisfy an old assertion.
            assertEquals(650L, GlobalResearchScheduleR932.SLICE_MS);
            assertEquals(1200L, GlobalResearchScheduleR932.TOTAL_MS);
            assertEquals(128, GlobalResearchScheduleR932.EVALUATION_CAP_R963);

            long heapBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long pssBeforeKb = Debug.getPss();
            ArrayList<Long> elapsed = new ArrayList<Long>();
            String target = "202609100001";

            for (int round = 1; round <= 20; round++) {
                List<GlobalResearchScheduleR932.Candidate> rows = roster(db);
                long started = SystemClock.elapsedRealtime();
                freeze(store, target, rows);
                elapsed.add(Long.valueOf(SystemClock.elapsedRealtime() - started));

                assertEquals("tier A cap round " + round, 8, selected(db, target, "A"));
                assertEquals("tier B cap round " + round, 128, selected(db, target, "B"));
                assertEquals("tier C nursery cap round " + round, 64, selected(db, target, "C"));

                if (round == 3)
                    assertEquals("C tier rotation", 160, distinctEverSelected(db, "C"));
                if (round == 2)
                    assertEquals("B tier rotation", 160, distinctEverSelected(db, "B"));
                if (round == 20)
                    assertEquals("A tier rotation", 160, distinctEverSelected(db, "A"));

                TargetIssueResolver.Resolution next = TargetIssueResolver.next(target);
                assertTrue("next target must remain valid", next.valid);
                target = next.targetIssue;
            }

            assertEquals(160, distinctEverSelected(db, "A"));
            assertEquals(160, distinctEverSelected(db, "B"));
            assertEquals(160, distinctEverSelected(db, "C"));

            Collections.sort(elapsed);
            long p95 = elapsed.get((int) Math.ceil(elapsed.size() * .95d) - 1).longValue();
            long max = elapsed.get(elapsed.size() - 1).longValue();
            long heapAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long pssAfterKb = Debug.getPss();
            long heapGrowth = Math.max(0L, heapAfter - heapBefore);
            long pssGrowthKb = Math.max(0L, pssAfterKb - pssBeforeKb);

            // Five seconds is only an emulator/runtime engineering redline for the complete 480
            // candidate freeze. Production keeps its current bounded slices and P0 preemption;
            // this assertion must never be reported as a physical-phone SLO proof.
            assertTrue("Android runtime scheduler max must stay below 5s, max=" + max, max < 5000L);
            assertTrue(
                    "Android runtime scheduler heap growth must stay bounded, bytes=" + heapGrowth,
                    heapGrowth < 96L * 1024L * 1024L);

            System.out.println(
                    "R932_C07_ANDROID_RUNTIME_PASS"
                            + "|rounds=20|candidate_pool=480|tierA=8|tierB=128|tierC=64"
                            + "|p95_ms=" + p95 + "|max_ms=" + max
                            + "|heap_growth_bytes=" + heapGrowth
                            + "|pss_growth_kb=" + pssGrowthKb
                            + "|runtime=ANDROID|physical_device_claim=false|future_effect=false");
        } finally {
            store.close();
        }
    }
}
