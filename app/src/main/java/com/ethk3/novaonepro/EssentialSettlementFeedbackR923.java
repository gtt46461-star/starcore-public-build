package com.ethk3.novaonepro;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Priority lane for the user-visible Teacher/reason/reverse/causal hit-or-miss explanation.
 *
 * <p>This is derivative of an already terminal Production settlement and cannot roll it back or
 * authorize a future BET.  It runs before heavier formula/genome/asset fan-out so the latest issue
 * no longer remains {@code NO_SAMPLE} merely because Research exhausted its time budget.</p>
 */
public final class EssentialSettlementFeedbackR923 {
    public static final String VERSION = "R923_ESSENTIAL_SETTLEMENT_FEEDBACK_V1";

    private EssentialSettlementFeedbackR923() {}

    public static Result settleNow(DbStore store, Models.Draw draw) {
        if (store == null || draw == null || !draw.isValid())
            return new Result(false, "INVALID_INPUT", "", 0, 0L);
        long started = System.currentTimeMillis();
        try {
            int rows = LegacyPredrawContextBridgeR915.settleDraw(store, draw);
            Result out = new Result(true, rows > 0 ? "CREATED" : "ALREADY_OR_NO_PREDRAW",
                    draw.issue, rows, System.currentTimeMillis() - started);
            publish(store, out);
            return out;
        } catch (Throwable failure) {
            Result out = new Result(false,
                    "NON_FATAL_" + failure.getClass().getSimpleName(), draw.issue, 0,
                    System.currentTimeMillis() - started);
            publish(store, out);
            return out;
        }
    }

    /** Newest-first repair of terminal issues whose explanation row is still absent. */
    public static Result drain(DbStore store, int requestedLimit, long budgetMs) {
        if (store == null) return new Result(false, "STORE_NULL", "", 0, 0L);
        if (RealtimePressureGateR811.hasActiveCriticalWork()
                || SettlementExecutionGateR819.isActive())
            return new Result(false, "YIELD_P0", "", 0, 0L);
        long started = System.currentTimeMillis();
        long deadline = started + Math.max(100L, Math.min(1200L, budgetMs));
        int limit = Math.max(1, Math.min(16, requestedLimit));
        ArrayList<String> issues = new ArrayList<String>();
        Cursor c = null;
        try {
            c = store.readable().rawQuery(pendingIssuesSql(store.readable()),
                    new String[] {SettlementTaskStoreR36.COMPLETED, String.valueOf(limit)});
            while (c.moveToNext()) issues.add(c.getString(0));
        } catch (Throwable unavailable) {
            Result out = new Result(false,
                    "SCAN_" + unavailable.getClass().getSimpleName(), "", 0,
                    System.currentTimeMillis() - started);
            publish(store, out);
            return out;
        } finally {
            if (c != null) c.close();
        }

        int completed = 0;
        String last = "NO_PENDING";
        for (String issue : issues) {
            if (System.currentTimeMillis() >= deadline
                    || RealtimePressureGateR811.hasActiveCriticalWork()) {
                last = "YIELD_BUDGET_OR_P0";
                break;
            }
            Models.Draw draw = store.getDrawByIssue(issue);
            Result one = settleNow(store, draw);
            last = one.status;
            if (one.success) completed++;
        }
        Result out = new Result(true, last, issues.isEmpty() ? "" : issues.get(0), completed,
                System.currentTimeMillis() - started);
        publish(store, out);
        return out;
    }

    static String pendingIssuesSql(android.database.sqlite.SQLiteDatabase db) {
        return "SELECT t.target_issue FROM " + DbHelper.TABLE_R36_SETTLEMENT_TASK
                            + " t JOIN " + DrawDependencyInputCP12.SOURCE
                            + " d ON d.issue=t.target_issue AND d.dirty=0"
                            + " WHERE t.status=? AND EXISTS(SELECT 1 FROM "
                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                            + " s WHERE s.target_issue=t.target_issue AND s.pre_draw_frozen=1"
                            + " AND s.settled=1 AND s.hit_label IN(0,1)) AND "
                            + LegacyPredrawContextBridgeR915.missingPostRowPredicate(db)
                            + " ORDER BY t.completed_at DESC,t.target_issue DESC LIMIT ?";
    }

    private static void publish(DbStore store, Result result) {
        if (store == null || result == null) return;
        try {
            LinkedHashMap<String, String> state = new LinkedHashMap<String, String>();
            state.put("r923_essential_feedback_version", VERSION);
            state.put("r923_essential_feedback_status", result.canonical());
            state.put("r923_essential_feedback_issue", result.issue);
            state.put("r923_essential_feedback_at", String.valueOf(System.currentTimeMillis()));
            store.setStates(state);
        } catch (Throwable ignored) {}
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '_').trim();
    }

    public static final class Result {
        public final boolean success;
        public final String status, issue;
        public final int rows;
        public final long costMs;

        Result(boolean ok, String state, String target, int count, long elapsed) {
            success = ok;
            status = safe(state);
            issue = safe(target);
            rows = Math.max(0, count);
            costMs = Math.max(0L, elapsed);
        }

        public String canonical() {
            return VERSION + "|ok=" + (success ? 1 : 0) + "|status=" + status
                    + "|issue=" + issue + "|rows=" + rows + "|costMs=" + costMs;
        }
    }
}
