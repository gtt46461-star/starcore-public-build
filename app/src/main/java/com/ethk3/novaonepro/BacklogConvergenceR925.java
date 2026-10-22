package com.ethk3.novaonepro;

import android.database.Cursor;
import java.util.Locale;

/**
 * B10 convergence truth. Latest FULL alone is insufficient: a continuous watermark is calculated
 * over canonical issues so a newer completion can never hide an older hole.
 */
public final class BacklogConvergenceR925 {
    private BacklogConvergenceR925() {}

    public static String report(DbStore store) {
        if (store == null) return "B10_STORE_NULL";
        DeferredEvidenceWriterR36.BacklogStats b = DeferredEvidenceWriterR36.backlogStats(store);
        long now = System.currentTimeMillis(), cutoff = now - 3600000L;
        String table = DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB;
        Cursor c = BudgetedQueryR927.query(store.readable(),
                "SELECT COALESCE(SUM(CASE WHEN completed_at>=? THEN 1 ELSE 0 END),0),"
                        + "COALESCE(SUM(CASE WHEN created_at>=? THEN 1 ELSE 0 END),0),"
                        + "COALESCE(MIN(created_at),0),"
                        + "COALESCE(SUM(CASE WHEN status='FAILED_TERMINAL' THEN 1 ELSE 0 END),0) FROM "
                        + table,
                new String[] {String.valueOf(cutoff), String.valueOf(cutoff)});
        int done, arrived, terminal;
        long first;
        try {
            c.moveToFirst();
            done = c.getInt(0);
            arrived = c.getInt(1);
            first = c.getLong(2);
            terminal = c.getInt(3);
        } finally {
            c.close();
        }

        double minutes = first <= 0L ? 0.0d : Math.min(60.0d, Math.max(0.0d, (now - first) / 60000.0d));
        double service = minutes > 0.0d ? done / minutes : 0.0d;
        double incoming = minutes > 0.0d ? arrived / minutes : 0.0d;
        double net = service - incoming;
        String eta = b.pendingJobs == 0
                ? "0"
                : minutes < 5.0d
                        ? "OBSERVING_LT_5_MIN"
                        : net <= 0.0d
                                ? "NOT_CONVERGING"
                                : String.format(Locale.US, "%.1f min", b.pendingJobs / net);

        Batch2ContractCoreR926.Continuity continuity = continuity(store, 240, terminal);
        String drawLatest = latestCleanDrawIssue(store);
        long continuousLag = issueLag(store, continuity.continuousWatermark, drawLatest);
        boolean p0 = "OK".equals(b.readStatus)
                && b.pendingJobs == 0
                && continuity.closure
                && continuousLag >= 0L
                && continuousLag <= 2L;

        return b.summary()
                + "\nB10_CONTINUITY|scope=ALL_CANONICAL|expected=" + continuity.expected
                + "|full=" + continuity.full
                + "|holes=" + continuity.holes
                + "|first_hole=" + empty(continuity.firstHole)
                + "|continuous_full_watermark=" + empty(continuity.continuousWatermark)
                + "|latest_full=" + empty(continuity.latestFull)
                + "|failed_terminal=" + terminal
                + "|latest_draw=" + empty(drawLatest)
                + "|continuous_draw_lag=" + (continuousLag < 0L ? "N/A" : String.valueOf(continuousLag))
                + "|closure=" + (continuity.closure ? "PASS" : "FAIL")
                + "|P0=" + (p0 ? "PASS" : "FAIL")
                + "\n窗口分钟=" + String.format(Locale.US, "%.1f", minutes)
                + "｜FULL完成=" + done
                + "｜新增=" + arrived
                + "｜完成/分钟=" + String.format(Locale.US, "%.3f", service)
                + "｜新增/分钟=" + String.format(Locale.US, "%.3f", incoming)
                + "｜净清理/分钟=" + String.format(Locale.US, "%.3f", net)
                + "｜预计清空=" + eta
                + "\n规则：连续水位之前不得有隐藏洞；FAILED_TERMINAL显式阻断闭环；实时预测始终优先。";
    }

    static Batch2ContractCoreR926.Continuity continuity(DbStore store, int window, int terminal) {
        String table = RuntimeEventLedgerR926.TABLE_PROJECTION;
        Cursor c=BudgetedQueryR927.query(store.readable(),"SELECT COUNT(*),COALESCE(SUM(full_seen),0),"
                + "COALESCE(MIN(CASE WHEN full_seen=0 THEN target_issue END),''),"
                + "COALESCE(MAX(CASE WHEN full_seen=1 THEN target_issue END),'') FROM " + table
                + " WHERE game_code=? AND canonical_seen=1", new String[]{ReleaseManifestR926.GAME_CODE});
        int expected, full; String hole, latest;
        try { c.moveToFirst(); expected=c.getInt(0); full=c.getInt(1); hole=safe(c.getString(2)); latest=safe(c.getString(3)); }
        finally { c.close(); }
        String watermark=latest;
        if(hole.length()>0) {
            c=BudgetedQueryR927.query(store.readable(),"SELECT COALESCE(MAX(target_issue),'') FROM " + table
                    + " WHERE game_code=? AND canonical_seen=1 AND full_seen=1 AND target_issue<?",
                    new String[]{ReleaseManifestR926.GAME_CODE,hole});
            try { watermark=c.moveToFirst()?safe(c.getString(0)):""; } finally { c.close(); }
        }
        return new Batch2ContractCoreR926.Continuity(expected,full,expected-full,terminal,
                hole,watermark,latest,expected>0 && expected==full && terminal==0);
    }

    /** Uses the canonical clean draw table directly; does not invent a DbStore API. */
    private static String latestCleanDrawIssue(DbStore store) {
        if (store == null) return "";
        Cursor c = null;
        try {
            c = BudgetedQueryR927.query(store.readable(),
                    "SELECT issue FROM " + DbHelper.TABLE_DRAWS
                            + " WHERE dirty=0 ORDER BY issue DESC LIMIT 1",
                    null);
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        } catch (Throwable ignored) {
            return "";
        } finally {
            if (c != null) c.close();
        }
    }

    private static long issueLag(DbStore store, String from, String to) {
        if (!TargetIssueResolver.isValid(from) || !TargetIssueResolver.isValid(to) || from.compareTo(to)>0) return -1L;
        Cursor c=BudgetedQueryR927.query(store.readable(),"SELECT COUNT(*) FROM " + DbHelper.TABLE_DRAWS
                + " WHERE dirty=0 AND issue>? AND issue<=?",new String[]{from,to});
        try {return c.moveToFirst()?c.getLong(0):-1L;} finally {c.close();}
    }

    private static String empty(String s) {
        String x = safe(s);
        return x.length() == 0 ? "N/A" : x;
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return "B10_STORE_NULL";
        DeferredEvidenceWriterR36.BacklogStats b = DeferredEvidenceWriterR36.backlogStatsSnapshotCP4(report, store);
        long now = report.nowMs(), cutoff = now - 3600000L;
        String table = DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB;
        Cursor c = BudgetedQueryR927.querySnapshotCP4(report, store.readable(),
                "SELECT COALESCE(SUM(CASE WHEN completed_at>=? THEN 1 ELSE 0 END),0),"
                        + "COALESCE(SUM(CASE WHEN created_at>=? THEN 1 ELSE 0 END),0),"
                        + "COALESCE(MIN(created_at),0),"
                        + "COALESCE(SUM(CASE WHEN status='FAILED_TERMINAL' THEN 1 ELSE 0 END),0) FROM "
                        + table,
                new String[] {String.valueOf(cutoff), String.valueOf(cutoff)});
        int done, arrived, terminal;
        long first;
        try {
            c.moveToFirst();
            done = c.getInt(0);
            arrived = c.getInt(1);
            first = c.getLong(2);
            terminal = c.getInt(3);
        } finally {
            c.close();
        }

        double minutes = first <= 0L ? 0.0d : Math.min(60.0d, Math.max(0.0d, (now - first) / 60000.0d));
        double service = minutes > 0.0d ? done / minutes : 0.0d;
        double incoming = minutes > 0.0d ? arrived / minutes : 0.0d;
        double net = service - incoming;
        String eta = b.pendingJobs == 0
                ? "0"
                : minutes < 5.0d
                        ? "OBSERVING_LT_5_MIN"
                        : net <= 0.0d
                                ? "NOT_CONVERGING"
                                : String.format(Locale.US, "%.1f min", b.pendingJobs / net);

        Batch2ContractCoreR926.Continuity continuity = continuitySnapshotCP4(report, store, 240, terminal);
        String drawLatest = latestCleanDrawIssueSnapshotCP4(report, store);
        long continuousLag = issueLagSnapshotCP4(report, store, continuity.continuousWatermark, drawLatest);
        boolean p0 = "OK".equals(b.readStatus)
                && b.pendingJobs == 0
                && continuity.closure
                && continuousLag >= 0L
                && continuousLag <= 2L;

        return b.summary()
                + "\nB10_CONTINUITY|scope=ALL_CANONICAL|expected=" + continuity.expected
                + "|full=" + continuity.full
                + "|holes=" + continuity.holes
                + "|first_hole=" + emptySnapshotCP4(report, continuity.firstHole)
                + "|continuous_full_watermark=" + emptySnapshotCP4(report, continuity.continuousWatermark)
                + "|latest_full=" + emptySnapshotCP4(report, continuity.latestFull)
                + "|failed_terminal=" + terminal
                + "|latest_draw=" + emptySnapshotCP4(report, drawLatest)
                + "|continuous_draw_lag=" + (continuousLag < 0L ? "N/A" : String.valueOf(continuousLag))
                + "|closure=" + (continuity.closure ? "PASS" : "FAIL")
                + "|P0=" + (p0 ? "PASS" : "FAIL")
                + "\n窗口分钟=" + String.format(Locale.US, "%.1f", minutes)
                + "｜FULL完成=" + done
                + "｜新增=" + arrived
                + "｜完成/分钟=" + String.format(Locale.US, "%.3f", service)
                + "｜新增/分钟=" + String.format(Locale.US, "%.3f", incoming)
                + "｜净清理/分钟=" + String.format(Locale.US, "%.3f", net)
                + "｜预计清空=" + eta
                + "\n规则：连续水位之前不得有隐藏洞；FAILED_TERMINAL显式阻断闭环；实时预测始终优先。";
    }

public static String emptySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String s) {
        String x = safeSnapshotCP4(report, s);
        return x.length() == 0 ? "N/A" : x;
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String s) { return s == null ? "" : s.trim(); }

public static long issueLagSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String from, String to) {
        if (!TargetIssueResolver.isValid(from) || !TargetIssueResolver.isValid(to) || from.compareTo(to)>0) return -1L;
        Cursor c=BudgetedQueryR927.querySnapshotCP4(report, store.readable(),"SELECT COUNT(*) FROM " + DbHelper.TABLE_DRAWS
                + " WHERE dirty=0 AND issue>? AND issue<=?",new String[]{from,to});
        try {return c.moveToFirst()?c.getLong(0):-1L;} finally {c.close();}
    }

public static String latestCleanDrawIssueSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return "";
        Cursor c = null;
        try {
            c = BudgetedQueryR927.querySnapshotCP4(report, store.readable(),
                    "SELECT issue FROM " + DbHelper.TABLE_DRAWS
                            + " WHERE dirty=0 ORDER BY issue DESC LIMIT 1",
                    null);
            return c.moveToFirst() ? safeSnapshotCP4(report, c.getString(0)) : "";
        } catch (Throwable ignored) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",ignored);} finally {
            if (c != null) c.close();
        }
    }

static Batch2ContractCoreR926.Continuity continuitySnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, int window, int terminal) {
        String table = RuntimeEventLedgerR926.TABLE_PROJECTION;
        Cursor c=BudgetedQueryR927.querySnapshotCP4(report, store.readable(),"SELECT COUNT(*),COALESCE(SUM(full_seen),0),"
                + "COALESCE(MIN(CASE WHEN full_seen=0 THEN target_issue END),''),"
                + "COALESCE(MAX(CASE WHEN full_seen=1 THEN target_issue END),'') FROM " + table
                + " WHERE game_code=? AND canonical_seen=1", new String[]{ReleaseManifestR926.GAME_CODE});
        int expected, full; String hole, latest;
        try { c.moveToFirst(); expected=c.getInt(0); full=c.getInt(1); hole=safeSnapshotCP4(report, c.getString(2)); latest=safeSnapshotCP4(report, c.getString(3)); }
        finally { c.close(); }
        String watermark=latest;
        if(hole.length()>0) {
            c=BudgetedQueryR927.querySnapshotCP4(report, store.readable(),"SELECT COALESCE(MAX(target_issue),'') FROM " + table
                    + " WHERE game_code=? AND canonical_seen=1 AND full_seen=1 AND target_issue<?",
                    new String[]{ReleaseManifestR926.GAME_CODE,hole});
            try { watermark=c.moveToFirst()?safeSnapshotCP4(report, c.getString(0)):""; } finally { c.close(); }
        }
        return new Batch2ContractCoreR926.Continuity(expected,full,expected-full,terminal,
                hole,watermark,latest,expected>0 && expected==full && terminal==0);
    }
}
