package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * C07 host-pressure fixture over the production scheduler and lifecycle classes.
 *
 * This proves deterministic 8/24/64 caps, chronological fairness, no starvation inside the
 * designed rotation bound, proxy/full1200 separation, duplicate-structure budget suppression,
 * and that lack of scheduling resources is not a quality-prune signal. Latency is a host-SQLite
 * engineering guard only; it is explicitly NOT a phone/device 5-second performance claim.
 */
public final class GlobalSchedulePressureHarnessR932 {
    private static int checks;

    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        checks++;
        System.out.println(name + "=PASS");
    }

    private static String scalar(SQLiteDatabase db, String sql, String... args) {
        try (Cursor c = db.rawQuery(sql, args == null || args.length == 0 ? null : args)) {
            return c.moveToFirst() ? c.getString(0) : "";
        }
    }

    private static String lastSelected(SQLiteDatabase db, String key) {
        return scalar(db,
                "SELECT target_issue FROM " + GlobalResearchScheduleR932.TASK
                        + " WHERE candidate_key=? AND selected=1 ORDER BY target_issue DESC LIMIT 1",
                key);
    }

    private static List<GlobalResearchScheduleR932.Candidate> roster(SQLiteDatabase db) {
        ArrayList<GlobalResearchScheduleR932.Candidate> rows =
                new ArrayList<GlobalResearchScheduleR932.Candidate>();
        for (String tier : Arrays.asList("A", "B", "C")) {
            for (int family = 0; family < 8; family++) {
                for (int i = 0; i < 20; i++) {
                    String key = tier + ":" + family + ":" + i;
                    rows.add(new GlobalResearchScheduleR932.Candidate(
                            key,
                            "C07_FAMILY_" + family,
                            V51ChecksumEngine.sha256("C07_STRUCTURE|" + key),
                            tier,
                            lastSelected(db, key)));
                }
            }
        }
        return rows;
    }

    private static void freeze(SQLiteDatabase db, String target,
            List<GlobalResearchScheduleR932.Candidate> rows) {
        String input = V51ChecksumEngine.sha256("C07_SURFACE|" + target);
        db.beginTransaction();
        try {
            GlobalResearchScheduleR932.freeze(db, target, input, rows);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static int selected(SQLiteDatabase db, String target, String tier) {
        return Integer.parseInt(scalar(db,
                "SELECT COUNT(*) FROM " + GlobalResearchScheduleR932.TASK
                        + " WHERE target_issue=? AND tier=? AND selected=1",
                target, tier));
    }

    private static int distinctEverSelected(SQLiteDatabase db, String tier) {
        return Integer.parseInt(scalar(db,
                "SELECT COUNT(DISTINCT candidate_key) FROM " + GlobalResearchScheduleR932.TASK
                        + " WHERE tier=? AND selected=1",
                tier));
    }

    private static String firstUnselected(SQLiteDatabase db, String target, String tier) {
        return scalar(db,
                "SELECT candidate_key FROM " + GlobalResearchScheduleR932.TASK
                        + " WHERE target_issue=? AND tier=? AND selected=0 ORDER BY candidate_key LIMIT 1",
                target, tier);
    }

    private static String lifecycle(SQLiteDatabase db, String key) {
        return scalar(db,
                "SELECT state FROM " + ResearchLifecycleR932.TABLE + " WHERE candidate_key=?", key);
    }

    private static void reviewNoResource(SQLiteDatabase db, String key, String issue) {
        db.beginTransaction();
        try {
            ResearchLifecycleR932.review(db, key, issue);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        String state = lifecycle(db, key);
        check(!"QUALITY_PRUNED".equals(state) && !"QUARANTINED".equals(state),
                "RESOURCE_WITHDRAWAL_NOT_QUALITY_PRUNE_" + issue);
    }

    public static void main(String[] args) throws Exception {
        SQLiteDatabase db = new SQLiteDatabase(args[0]);
        DbStore store = new DbStore(db);
        try {
            db.execSQL("CREATE TABLE " + DbHelper.TABLE_DRAWS + "(issue TEXT PRIMARY KEY)");
            // Lifecycle pressure review traverses fixed-window evidence tables even when this
            // particular resource-withdrawal fixture has no mature window rows yet. Initialize the
            // complete production schema instead of relying on a partial scheduler-only schema.
            FixedResearchWindowR932.schema(db);
            GlobalResearchScheduleR932.schema(db);
            check(GlobalResearchScheduleR932.SLICE_MS == 250L
                            && GlobalResearchScheduleR932.TOTAL_MS == 700L,
                    "DECLARED_RESEARCH_SLICE_250MS_TOTAL_700MS");

            String target = "202609100001";
            String pressureCandidate = "";
            ArrayList<Long> elapsed = new ArrayList<Long>();
            for (int round = 1; round <= 20; round++) {
                List<GlobalResearchScheduleR932.Candidate> rows = roster(db);
                long started = System.nanoTime();
                freeze(db, target, rows);
                long ms = (System.nanoTime() - started) / 1000000L;
                elapsed.add(Long.valueOf(ms));

                check(selected(db, target, "A") == 8
                                && selected(db, target, "B") == 24
                                && selected(db, target, "C") == 64,
                        "EXACT_8_24_64_CAPS_ROUND_" + round);

                if (round == 1) {
                    pressureCandidate = firstUnselected(db, target, "C");
                    check(pressureCandidate.length() > 0,
                            "PRESSURE_FIXTURE_HAS_UNSCHEDULED_ELIGIBLE_CANDIDATE");
                }
                if (pressureCandidate.length() > 0 && round <= 4)
                    reviewNoResource(db, pressureCandidate, target);

                if (round == 3)
                    check(distinctEverSelected(db, "C") == 160,
                            "C_TIER_ALL_160_ROTATED_WITHIN_3_ISSUES");
                if (round == 7)
                    check(distinctEverSelected(db, "B") == 160,
                            "B_TIER_ALL_160_ROTATED_WITHIN_7_ISSUES");
                if (round == 20)
                    check(distinctEverSelected(db, "A") == 160,
                            "A_TIER_ALL_160_ROTATED_WITHIN_20_ISSUES");

                target = TargetIssueResolver.next(target).targetIssue;
            }

            check(distinctEverSelected(db, "A") == 160
                            && distinctEverSelected(db, "B") == 160
                            && distinctEverSelected(db, "C") == 160,
                    "NO_CANDIDATE_STARVES_WITHIN_DESIGNED_ROTATION_BOUND");

            Collections.sort(elapsed);
            long p95 = elapsed.get((int) Math.ceil(elapsed.size() * .95d) - 1).longValue();
            long max = elapsed.get(elapsed.size() - 1).longValue();
            check(max < 5000L,
                    "HOST_SQLITE_480_CANDIDATE_SCHEDULER_FREEZE_UNDER_5S_NOT_DEVICE_CLAIM");
            System.out.println("C07_HOST_SCHEDULER_LATENCY_MS|p95=" + p95 + "|max=" + max
                    + "|device=false|mainline_full_prediction=false");

            String proxyTarget = "202609100021";
            String proxy = scalar(db,
                    "SELECT candidate_key FROM " + GlobalResearchScheduleR932.TASK
                            + " WHERE target_issue='202609100001' AND tier='C' AND selected=1 LIMIT 1");
            check(proxy.length() > 0
                            && GlobalResearchScheduleR932.proxy(store, "202609100001", proxy)
                            && GlobalResearchScheduleR932.saveProxy(store, "202609100001", proxy,
                                    V51ChecksumEngine.sha256("C07_SURFACE|202609100001"), 17, .51d),
                    "C_PROXY_OUTPUT_IS_DURABLE");
            check("0".equals(scalar(db,
                            "SELECT full1200 FROM " + ResearchCaptureLedgerR932.TABLE
                                    + " WHERE target_issue='202609100001' AND candidate_key=?", proxy)),
                    "C_PROXY_CAN_NEVER_CLAIM_FULL1200");
            db.beginTransaction();
            try {
                ResearchLifecycleR932.review(db, proxy, "202609100020");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            check(!Arrays.asList("EVIDENCE_READY", "SHADOW", "INFLUENCE", "CHAMPION")
                            .contains(lifecycle(db, proxy)),
                    "PROXY_ONLY_SUPPORT_CANNOT_PROMOTE_TO_FULL_EVIDENCE_OR_INFLUENCE");

            String dupStructure = V51ChecksumEngine.sha256("C07_DUPLICATE_STRUCTURE");
            List<GlobalResearchScheduleR932.Candidate> dup =
                    new ArrayList<GlobalResearchScheduleR932.Candidate>();
            dup.add(new GlobalResearchScheduleR932.Candidate(
                    "C:dup:one", "C07_DUP_FAMILY", dupStructure, "C", ""));
            dup.add(new GlobalResearchScheduleR932.Candidate(
                    "C:dup:two", "C07_DUP_FAMILY", dupStructure, "C", ""));
            freeze(db, proxyTarget, dup);
            check("1".equals(scalar(db,
                            "SELECT COUNT(*) FROM " + GlobalResearchScheduleR932.TASK
                                    + " WHERE target_issue=? AND selected=1", proxyTarget)),
                    "DUPLICATE_STRUCTURE_DOES_NOT_CONSUME_DOUBLE_ACTIVE_BUDGET");
            check("2".equals(scalar(db,
                            "SELECT COUNT(*) FROM " + ResearchCaptureLedgerR932.TABLE
                                    + " WHERE target_issue=? AND eligible=1", proxyTarget)),
                    "DUPLICATE_ALIAS_REMAINS_ACCOUNTED_IN_ELIGIBLE_DENOMINATOR");

            System.out.println("R932_C07_PRESSURE_PASS|checks=" + checks
                    + "|scope=PRODUCTION_SCHEDULER_REAL_SQLITE_SYNTHETIC_20_ISSUES"
                    + "|device=false|future_effect=false");
        } finally {
            db.shutdown();
        }
    }
}
