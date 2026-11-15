package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.LinkedHashMap;
import java.util.Locale;

/** Draw-denominator prediction coverage; settlement count is never used as the denominator. */
public final class PredictionGenerationCoverageR923 {
    public static final String VERSION = "R923_DRAW_DENOMINATOR_COVERAGE_V1";
    public static final int[] WINDOWS = new int[] {30, 120, 4320};

    private PredictionGenerationCoverageR923() {}

    public static Report capture(DbStore store) {
        if (store == null) return Report.error("STORE_NULL");
        try {
            SQLiteDatabase db = store.readable();
            Window[] windows = new Window[WINDOWS.length];
            for (int i = 0; i < WINDOWS.length; i++) windows[i] = window(db, WINDOWS[i]);
            return new Report("OK", windows, missingRecent30(db), "");
        } catch (Throwable failure) {
            return Report.error(failure.getClass().getSimpleName() + ':' + safe(failure.getMessage()));
        }
    }

    private static Window window(SQLiteDatabase db, int requested) {
        String recent = "(SELECT issue FROM " + DbHelper.TABLE_DRAWS
                + " WHERE dirty=0 ORDER BY issue DESC LIMIT " + requested + ")";
        int draws = scalar(db, "SELECT COUNT(1) FROM " + recent, null);
        int canonical = scalar(db,
                "SELECT COUNT(DISTINCT o.target_issue) FROM "
                        + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " o JOIN " + recent
                        + " d ON d.issue=o.target_issue WHERE o.pre_draw_frozen=1", null);
        int opportunities = scalar(db,
                "SELECT COUNT(DISTINCT p.target_issue) FROM "
                        + ControlledCandidateOpportunityAuditR922.TABLE + " p JOIN " + recent
                        + " d ON d.issue=p.target_issue", null);
        int requestedCount = scalar(db,
                "SELECT COUNT(DISTINCT r.target_issue) FROM "
                        + DbHelper.TABLE_R36_ANALYSIS_REQUEST + " r JOIN " + recent
                        + " d ON d.issue=r.target_issue", null);
        int committedRequests = scalar(db,
                "SELECT COUNT(DISTINCT r.target_issue) FROM "
                        + DbHelper.TABLE_R36_ANALYSIS_REQUEST + " r JOIN " + recent
                        + " d ON d.issue=r.target_issue WHERE r.status=?",
                new String[] {AnalysisRequestStoreR36.CANONICAL_COMMITTED});
        int real = scalar(db,
                "SELECT COUNT(DISTINCT o.target_issue) FROM "
                        + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " o JOIN " + recent
                        + " d ON d.issue=o.target_issue JOIN " + DbHelper.TABLE_TRADES
                        + " t ON t.target_issue=o.target_issue WHERE o.pre_draw_frozen=1"
                        + " AND o.decision_action='BET' AND t.action='BET'"
                        + " AND t.source_type='REALTIME' AND t.is_realtime_real=1"
                        + " AND t.stat_scope='REAL_TRADE' AND t.authorization_valid=1"
                        + " AND t.authorized_mode='BET' AND t.contract_valid=1"
                        + " AND t.authorization_id=o.authorization_hash"
                        + " AND t.position=o.position AND t.n1=o.n1 AND t.n2=o.n2 AND t.n3=o.n3",
                null);
        int shadow = scalar(db,
                "SELECT COUNT(DISTINCT o.target_issue) FROM "
                        + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " o JOIN " + recent
                        + " d ON d.issue=o.target_issue WHERE o.pre_draw_frozen=1"
                        + " AND COALESCE(o.decision_action,'')<>'BET'", null);
        return new Window(requested, draws, canonical, opportunities, requestedCount,
                committedRequests, real, shadow);
    }

    private static String missingRecent30(SQLiteDatabase db) {
        String recent = "(SELECT issue FROM " + DbHelper.TABLE_DRAWS
                + " WHERE dirty=0 ORDER BY issue DESC LIMIT 30)";
        Cursor c = null;
        LinkedHashMap<String, Integer> reasons = new LinkedHashMap<String, Integer>();
        StringBuilder issues = new StringBuilder();
        try {
            c = db.rawQuery(
                    "SELECT d.issue,COALESCE(r.status,'NO_REQUEST'),COALESCE(r.error_code,''),"
                            + "COALESCE(r.last_stage,'') FROM " + recent + " d LEFT JOIN "
                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                            + " o ON o.target_issue=d.issue LEFT JOIN "
                            + DbHelper.TABLE_R36_ANALYSIS_REQUEST
                            + " r ON r.target_issue=d.issue WHERE (o.target_issue IS NULL OR COALESCE(o.pre_draw_frozen,0)<>1)"
                            + " ORDER BY d.issue DESC",
                    null);
            while (c.moveToNext()) {
                String issue = safe(c.getString(0));
                String status = safe(c.getString(1));
                String error = safe(c.getString(2));
                String stage = safe(c.getString(3));
                String reason = "NO_REQUEST".equals(status)
                        ? "NO_ANALYSIS_REQUEST"
                        : status + (error.length() == 0 ? "" : ':' + error)
                                + (stage.length() == 0 ? "" : "@" + stage);
                Integer old = reasons.get(reason);
                reasons.put(reason, Integer.valueOf(old == null ? 1 : old.intValue() + 1));
                if (issues.length() > 0) issues.append(',');
                issues.append(issue).append('(').append(reason).append(')');
            }
        } finally {
            if (c != null) c.close();
        }
        return "distribution=" + reasons + "|issues="
                + (issues.length() == 0 ? "NONE" : issues.toString());
    }

    private static int scalar(SQLiteDatabase db, String sql, String[] args) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            return c.moveToFirst() ? Math.max(0, c.getInt(0)) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    private static String pct(int numerator, int denominator) {
        return denominator <= 0 ? "N/A"
                : String.format(Locale.US, "%.2f%%", 100.0d * numerator / denominator);
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('|', '/').trim();
    }

    public static final class Window {
        public final int requestedWindow, draws, canonicalPredictions, opportunities,
                analysisRequests, committedRequests;
        public final int validReal, shadowObservations;
        Window(int requested, int drawCount, int predictions, int opportunityCount,
                int requests, int committed) {
            this(requested,drawCount,predictions,opportunityCount,requests,committed,-1,-1);
        }
        Window(int requested, int drawCount, int predictions, int opportunityCount,
                int requests, int committed, int real, int shadow) {
            requestedWindow=requested;draws=drawCount;canonicalPredictions=predictions;
            opportunities=opportunityCount;analysisRequests=requests;committedRequests=committed;
            validReal=real;shadowObservations=shadow;
        }
        public String canonical() {
            return "last"+requestedWindow+"Draws="+draws+"|predictions="+canonicalPredictions
                    +"|coverage="+pct(canonicalPredictions,draws)+"|opportunities="+opportunities
                    +"|opportunityCoverage="+pct(opportunities,draws)+"|requests="+analysisRequests
                    +"|committedRequests="+committedRequests
                    +"|validREAL="+validReal+"|SHADOW="+shadowObservations
                    +"|missingPrediction="+Math.max(0,draws-canonicalPredictions)
                    +"|invalidREALProvenance="+(validReal<0||shadowObservations<0?-1:
                            Math.max(0,canonicalPredictions-validReal-shadowObservations))
                    +"|realPerDraw="+(validReal<0?"N/A":pct(validReal,draws))
                    +"|minimum15="+(draws<=0||validReal<0?"NO_SAMPLE":
                            validReal*100>=draws*15?"OBSERVED":"NOT_MET")
                    +"|nonREALShadowCoverage="+(validReal<0||shadowObservations<0?"N/A":
                            pct(shadowObservations,draws-validReal));
        }
    }

    public static final class Report {
        public final String readStatus, recent30Missing, error;
        public final Window[] windows;
        Report(String status, Window[] values, String missing, String errorValue) {
            readStatus=safe(status);windows=values==null?new Window[0]:values.clone();
            recent30Missing=safe(missing);error=safe(errorValue);
        }
        static Report error(String error) {
            return new Report("READ_ERROR", new Window[0], "UNAVAILABLE", error);
        }
        public String summary() {
            StringBuilder out = new StringBuilder(VERSION).append("|read=").append(readStatus);
            for (Window window : windows) out.append('\n').append(window.canonical());
            out.append("\nrecent30Missing=").append(recent30Missing)
                    .append("\ndenominator=ACTUAL_CLEAN_DRAWS|settlementAsDenominator=FORBIDDEN");
            if (error.length() > 0) out.append("|error=").append(error);
            return out.toString();
        }
    }

public static Report captureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return Report.error("STORE_NULL");
        try {
            SQLiteDatabase db = store.readable();
            Window[] windows = new Window[WINDOWS.length];
            for (int i = 0; i < WINDOWS.length; i++) windows[i] = windowSnapshotCP4(report, db, WINDOWS[i]);
            return new Report("OK", windows, missingRecent30SnapshotCP4(report, db), "");
        } catch (Throwable failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);}
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('|', '/').trim();
    }

public static String missingRecent30SnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db) {
        String recent = "(SELECT issue FROM " + DbHelper.TABLE_DRAWS
                + " WHERE dirty=0 ORDER BY issue DESC LIMIT 30)";
        Cursor c = null;
        LinkedHashMap<String, Integer> reasons = new LinkedHashMap<String, Integer>();
        com.ethk3.novaonepro.ReportTextCP12 issues = new com.ethk3.novaonepro.ReportTextCP12(report);
        try {
            c = report.select(
                    "SELECT d.issue,COALESCE(r.status,'NO_REQUEST'),COALESCE(r.error_code,''),"
                            + "COALESCE(r.last_stage,'') FROM " + recent + " d LEFT JOIN "
                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                            + " o ON o.target_issue=d.issue LEFT JOIN "
                            + DbHelper.TABLE_R36_ANALYSIS_REQUEST
                            + " r ON r.target_issue=d.issue WHERE (o.target_issue IS NULL OR COALESCE(o.pre_draw_frozen,0)<>1)"
                            + " ORDER BY d.issue DESC",
                    null);
            while (c.moveToNext()) {
                String issue = safeSnapshotCP4(report, c.getString(0));
                String status = safeSnapshotCP4(report, c.getString(1));
                String error = safeSnapshotCP4(report, c.getString(2));
                String stage = safeSnapshotCP4(report, c.getString(3));
                String reason = "NO_REQUEST".equals(status)
                        ? "NO_ANALYSIS_REQUEST"
                        : status + (error.length() == 0 ? "" : ':' + error)
                                + (stage.length() == 0 ? "" : "@" + stage);
                Integer old = reasons.get(reason);
                reasons.put(reason, Integer.valueOf(old == null ? 1 : old.intValue() + 1));
                if (issues.length() > 0) issues.append(',');
                issues.append(issue).append('(').append(reason).append(')');
            }
        } finally {
            if (c != null) c.close();
        }
        return "distribution=" + reasons + "|issues="
                + (issues.length() == 0 ? "NONE" : issues.toString());
    }

public static Window windowSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, int requested) {
        String recent = "(SELECT issue FROM " + DbHelper.TABLE_DRAWS
                + " WHERE dirty=0 ORDER BY issue DESC LIMIT " + requested + ")";
        int draws = scalarSnapshotCP4(report, db, "SELECT COUNT(1) FROM " + recent, null);
        int canonical = scalarSnapshotCP4(report, db,
                "SELECT COUNT(DISTINCT o.target_issue) FROM "
                        + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " o JOIN " + recent
                        + " d ON d.issue=o.target_issue WHERE o.pre_draw_frozen=1", null);
        int opportunities = scalarSnapshotCP4(report, db,
                "SELECT COUNT(DISTINCT p.target_issue) FROM "
                        + ControlledCandidateOpportunityAuditR922.TABLE + " p JOIN " + recent
                        + " d ON d.issue=p.target_issue", null);
        int requestedCount = scalarSnapshotCP4(report, db,
                "SELECT COUNT(DISTINCT r.target_issue) FROM "
                        + DbHelper.TABLE_R36_ANALYSIS_REQUEST + " r JOIN " + recent
                        + " d ON d.issue=r.target_issue", null);
        int committedRequests = scalarSnapshotCP4(report, db,
                "SELECT COUNT(DISTINCT r.target_issue) FROM "
                        + DbHelper.TABLE_R36_ANALYSIS_REQUEST + " r JOIN " + recent
                        + " d ON d.issue=r.target_issue WHERE r.status=?",
                new String[] {AnalysisRequestStoreR36.CANONICAL_COMMITTED});
        int real = scalarSnapshotCP4(report, db,
                "SELECT COUNT(DISTINCT o.target_issue) FROM "
                        + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " o JOIN " + recent
                        + " d ON d.issue=o.target_issue JOIN " + DbHelper.TABLE_TRADES
                        + " t ON t.target_issue=o.target_issue WHERE o.pre_draw_frozen=1"
                        + " AND o.decision_action='BET' AND t.action='BET'"
                        + " AND t.source_type='REALTIME' AND t.is_realtime_real=1"
                        + " AND t.stat_scope='REAL_TRADE' AND t.authorization_valid=1"
                        + " AND t.authorized_mode='BET' AND t.contract_valid=1"
                        + " AND t.authorization_id=o.authorization_hash"
                        + " AND t.position=o.position AND t.n1=o.n1 AND t.n2=o.n2 AND t.n3=o.n3",
                null);
        int shadow = scalarSnapshotCP4(report, db,
                "SELECT COUNT(DISTINCT o.target_issue) FROM "
                        + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " o JOIN " + recent
                        + " d ON d.issue=o.target_issue WHERE o.pre_draw_frozen=1"
                        + " AND COALESCE(o.decision_action,'')<>'BET'", null);
        return new Window(requested, draws, canonical, opportunities, requestedCount,
                committedRequests, real, shadow);
    }

public static int scalarSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, String sql, String[] args) {
        Cursor c = null;
        try {
            c = report.select(sql, args);
            return c.moveToFirst() ? Math.max(0, c.getInt(0)) : 0;
        } finally {
            if (c != null) c.close();
        }
    }
}
