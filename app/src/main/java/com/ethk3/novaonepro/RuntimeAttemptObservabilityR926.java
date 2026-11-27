package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Locale;

/**
 * B05 attempt/outcome observability backed only by durable production ledgers.
 *
 * <p>This class is deliberately read-only. It does not create requests, retry work, select a model,
 * or authorize a decision. Successful durable latency samples remain owned by
 * {@link RuntimeObservabilityR925}; this report makes rejected, retried, expired and terminal
 * attempts visible so percentile reports cannot silently describe only the successful population.
 */
public final class RuntimeAttemptObservabilityR926 {
    private RuntimeAttemptObservabilityR926() {}

    public static String report(DbStore store) {
        if (store == null) return "B05_ATTEMPT_OUTCOMES_STORE_NULL\n";
        StringBuilder b = new StringBuilder("B05_ATTEMPT_OUTCOMES\n");
        SQLiteDatabase db = store.readable();
        try {
            long releaseStartedAt = releaseStartedAt(db);
            b.append("release_code=").append(BuildConfig.VERSION_CODE)
                    .append("|release_started_at=").append(releaseStartedAt).append('\n');
            appendEventCounts(db, b, releaseStartedAt);
            appendDurableCounts(db, b, releaseStartedAt);
            appendRecentFailures(db, b, releaseStartedAt);
        } catch (Throwable t) {
            b.append("B05_ATTEMPT_OBSERVABILITY_ERROR=")
                    .append(t.getClass().getSimpleName())
                    .append(':')
                    .append(trim(t.getMessage(), 240))
                    .append('\n');
        }
        return b.toString();
    }

    private static final String EVENTS = RuntimeEventLedgerR926.TABLE_EVENT;
    // Take the latest distinct requested attempts in sequence order, then probe each
    // attempt's indexed evidence. A historical request from another game or before
    // the release boundary cannot hide a current request for the same attempt.
    private static final String ACCEPTANCE_WINDOW = "SELECT r.attempt_id,t.sample_class,"
            + "EXISTS(SELECT 1 FROM " + EVENTS + " x WHERE x.attempt_id=r.attempt_id AND x.event_type='REJECTED'),"
            + "EXISTS(SELECT 1 FROM " + EVENTS + " x WHERE x.attempt_id=r.attempt_id AND x.event_type='CANONICAL')"
            + ",EXISTS(SELECT 1 FROM v57_attempt_resource_r927 q INDEXED BY idx_r927_resource_attempt WHERE q.attempt_id=r.attempt_id AND q.status<>'CANONICAL_COMMITTED')"
            + ",EXISTS(SELECT 1 FROM v57_attempt_resource_r927 q INDEXED BY idx_r927_resource_attempt WHERE q.attempt_id=r.attempt_id AND q.status='CANONICAL_COMMITTED')"
            + " FROM (SELECT e.attempt_id FROM " + EVENTS + " e NOT INDEXED"
            + " WHERE e.event_type='REQUESTED' AND e.game_code=? AND e.created_at>=? AND e.attempt_id<>''"
            + " AND NOT EXISTS(SELECT 1 FROM " + EVENTS + " n INDEXED BY idx_r926_event_attempt"
            + " WHERE n.attempt_id=e.attempt_id AND n.seq>e.seq AND n.game_code=e.game_code"
            + " AND n.event_type='REQUESTED' AND n.created_at>=?2)"
            + " ORDER BY e.seq DESC LIMIT 120) r"
            + " LEFT JOIN v57_prediction_timing_r925 t ON t.attempt_id=r.attempt_id AND t.release_code=?";

    /** Latest requested attempts are the denominator; failed/missing/rehydrated samples block PASS. */
    static String acceptanceWindow(DbStore store) {
        SQLiteDatabase db = store.readable();
        long since = releaseStartedAt(db);
        if (since <= 0L) return "RELEASE_EPOCH_MISSING";
        Cursor c = db.rawQuery(ACCEPTANCE_WINDOW,
                new String[]{ReleaseManifestR926.GAME_CODE, String.valueOf(since), String.valueOf(BuildConfig.VERSION_CODE)});
        int n=0, failed=0;
        try { while(c.moveToNext()) { n++; if(!"WARM_MONOTONIC".equals(c.getString(1)) || c.getInt(2)>0 || c.getInt(3)==0 || c.getInt(4)>0 || c.getInt(5)==0) failed++; } }
        finally { c.close(); }
        return n==120 && failed==0 ? "COMPLETE_120" : "REQUESTED="+n+"|FAILED_OR_UNMEASURED="+failed;
    }

    private static void appendEventCounts(SQLiteDatabase db, StringBuilder b, long since) {
        String table = RuntimeEventLedgerR926.TABLE_EVENT;
        String where = since > 0L ? " WHERE created_at>=?" : "";
        String[] args = since > 0L ? new String[] {String.valueOf(since)} : null;
        long requested = scalar(db,
                "SELECT COUNT(DISTINCT attempt_id) FROM " + table + where
                        + (where.length() == 0 ? " WHERE" : " AND")
                        + " event_type='REQUESTED' AND attempt_id<>''", args);
        long accepted = eventAttempts(db, table, since, "ACCEPTED");
        long started = eventAttempts(db, table, since, "STARTED");
        long rejected = eventAttempts(db, table, since, "REJECTED");
        long canonical = eventAttempts(db, table, since, "CANONICAL");
        long explicitTimeout = eventTextAttempts(db, table, since, "TIMEOUT");
        long explicitCancel = eventTextAttempts(db, table, since, "CANCEL");
        b.append("event_attempts requested=").append(requested)
                .append(" accepted=").append(accepted)
                .append(" started=").append(started)
                .append(" rejected=").append(rejected)
                .append(" canonical=").append(canonical)
                .append(" explicit_timeout=").append(explicitTimeout)
                .append(" explicit_cancel=").append(explicitCancel)
                .append(" unresolved_requested=").append(scalar(db, "SELECT COUNT(DISTINCT r.attempt_id) FROM " + table
                        + " r WHERE r.event_type='REQUESTED' AND r.attempt_id<>'' AND r.created_at>=?"
                        + " AND NOT EXISTS(SELECT 1 FROM " + table + " x WHERE x.attempt_id=r.attempt_id"
                        + " AND x.event_type IN ('CANONICAL','REJECTED'))", new String[]{String.valueOf(since)}))
                .append('\n');
    }

    private static long eventAttempts(
            SQLiteDatabase db, String table, long since, String eventType) {
        String where = since > 0L ? "created_at>=? AND " : "";
        String[] args = since > 0L
                ? new String[] {String.valueOf(since), eventType}
                : new String[] {eventType};
        return scalar(db,
                "SELECT COUNT(DISTINCT attempt_id) FROM " + table + " WHERE " + where
                        + "event_type=? AND attempt_id<>''", args);
    }

    private static long eventTextAttempts(
            SQLiteDatabase db, String table, long since, String token) {
        String where = since > 0L ? "created_at>=? AND " : "";
        String[] args = since > 0L
                ? new String[] {String.valueOf(since), "%" + token + "%", "%" + token + "%"}
                : new String[] {"%" + token + "%", "%" + token + "%"};
        return scalar(db,
                "SELECT COUNT(DISTINCT attempt_id) FROM " + table + " WHERE " + where
                        + "attempt_id<>'' AND (stage LIKE ? OR payload_text LIKE ?)", args);
    }

    private static void appendDurableCounts(SQLiteDatabase db, StringBuilder b, long since) {
        String table = DbHelper.TABLE_R36_ANALYSIS_REQUEST;
        String where = since > 0L ? " WHERE request_at>=?" : "";
        String[] args = since > 0L ? new String[] {String.valueOf(since)} : null;
        Cursor c = db.rawQuery(
                "SELECT status,COUNT(*),COALESCE(SUM(attempts),0),"
                        + "COALESCE(SUM(CASE WHEN error_code<>'' THEN 1 ELSE 0 END),0),"
                        + "COALESCE(SUM(CASE WHEN first_error_code<>'' THEN 1 ELSE 0 END),0)"
                        + " FROM " + table + where + " GROUP BY status ORDER BY status",
                args);
        try {
            long rows = 0L, claimAttempts = 0L, errors = 0L, firstErrors = 0L;
            while (c.moveToNext()) {
                String status = safe(c.getString(0));
                long n = c.getLong(1), attempts = c.getLong(2), e = c.getLong(3), first = c.getLong(4);
                rows += n;
                claimAttempts += attempts;
                errors += e;
                firstErrors += first;
                b.append("durable_status ").append(status)
                        .append(" rows=").append(n)
                        .append(" claim_attempts=").append(attempts)
                        .append(" active_errors=").append(e)
                        .append(" first_error_preserved=").append(first)
                        .append('\n');
            }
            b.append("durable_totals rows=").append(rows)
                    .append(" claim_attempts=").append(claimAttempts)
                    .append(" active_errors=").append(errors)
                    .append(" first_error_preserved=").append(firstErrors)
                    .append('\n');
        } finally {
            c.close();
        }
    }

    private static void appendRecentFailures(SQLiteDatabase db, StringBuilder b, long since) {
        String table = DbHelper.TABLE_R36_ANALYSIS_REQUEST;
        String where = " WHERE (status IN ('RETRY_WAIT','EXPIRED','FAILED_TERMINAL')"
                + " OR error_code<>'' OR first_error_code<>'')";
        java.util.ArrayList<String> argsList = new java.util.ArrayList<String>();
        if (since > 0L) {
            where += " AND request_at>=?";
            argsList.add(String.valueOf(since));
        }
        Cursor c = db.rawQuery(
                "SELECT target_issue,status,attempts,request_at,deadline_at,last_transition_at,"
                        + "error_code,first_error_code,last_stage FROM " + table + where
                        + " ORDER BY last_transition_at DESC LIMIT 30",
                argsList.isEmpty() ? null : argsList.toArray(new String[argsList.size()]));
        try {
            b.append("recent_failure_or_retry_rows\n");
            while (c.moveToNext()) {
                long deadline = c.getLong(4);
                long transition = c.getLong(5);
                boolean deadlineExceeded = deadline > 0L && transition > deadline;
                b.append("issue=").append(safe(c.getString(0)))
                        .append("|status=").append(safe(c.getString(1)))
                        .append("|attempts=").append(c.getLong(2))
                        .append("|request_at=").append(c.getLong(3))
                        .append("|deadline_at=").append(deadline)
                        .append("|last_transition_at=").append(transition)
                        .append("|deadline_exceeded=").append(deadlineExceeded ? 1 : 0)
                        .append("|error=").append(safe(c.getString(6)))
                        .append("|first_error=").append(safe(c.getString(7)))
                        .append("|stage=").append(safe(c.getString(8)))
                        .append('\n');
            }
        } finally {
            c.close();
        }
    }

    private static long releaseStartedAt(SQLiteDatabase db) {
        Cursor c = db.rawQuery(
                "SELECT started_at FROM v57_runtime_epoch_r925 WHERE epoch_id=? LIMIT 1",
                new String[] {"release-" + BuildConfig.VERSION_CODE});
        try {
            return c.moveToFirst() ? Math.max(0L, c.getLong(0)) : 0L;
        } finally {
            c.close();
        }
    }

    private static long scalar(SQLiteDatabase db, String sql, String[] args) {
        Cursor c = db.rawQuery(sql, args);
        try {
            return c.moveToFirst() ? Math.max(0L, c.getLong(0)) : 0L;
        } finally {
            c.close();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('|', '/').trim();
    }

    private static String trim(String value, int max) {
        String x = safe(value);
        return x.length() <= max ? x : x.substring(0, max);
    }

public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return "B05_ATTEMPT_OUTCOMES_STORE_NULL\n";
        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report, "B05_ATTEMPT_OUTCOMES\n");
        SQLiteDatabase db = store.readable();
        try {
            long releaseStartedAt = releaseStartedAtSnapshotCP4(report, db);
            b.append("release_code=").append(BuildConfig.VERSION_CODE)
                    .append("|release_started_at=").append(releaseStartedAt).append('\n');
            appendEventCountsSnapshotCP4(report, db, b, releaseStartedAt);
            appendDurableCountsSnapshotCP4(report, db, b, releaseStartedAt);
            appendRecentFailuresSnapshotCP4(report, db, b, releaseStartedAt);
        } catch (Throwable t) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",t);}
        return b.toString();
    }

static String acceptanceWindowSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        SQLiteDatabase db = store.readable();
        long since = releaseStartedAtSnapshotCP4(report, db);
        if (since <= 0L) return "RELEASE_EPOCH_MISSING";
        Cursor c = report.select(ACCEPTANCE_WINDOW,
                new String[]{ReleaseManifestR926.GAME_CODE, String.valueOf(since), String.valueOf(BuildConfig.VERSION_CODE)});
        int n=0, failed=0;
        try { while(c.moveToNext()) { n++; if(!"WARM_MONOTONIC".equals(c.getString(1)) || c.getInt(2)>0 || c.getInt(3)==0 || c.getInt(4)>0 || c.getInt(5)==0) failed++; } }
        finally { c.close(); }
        return n==120 && failed==0 ? "COMPLETE_120" : "REQUESTED="+n+"|FAILED_OR_UNMEASURED="+failed;
    }

public static void appendEventCountsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, com.ethk3.novaonepro.ReportTextCP12 b, long since) {
        String table = RuntimeEventLedgerR926.TABLE_EVENT;
        String where = since > 0L ? " WHERE created_at>=?" : "";
        String[] args = since > 0L ? new String[] {String.valueOf(since)} : null;
        long requested = scalarSnapshotCP4(report, db,
                "SELECT COUNT(DISTINCT attempt_id) FROM " + table + where
                        + (where.length() == 0 ? " WHERE" : " AND")
                        + " event_type='REQUESTED' AND attempt_id<>''", args);
        long accepted = eventAttemptsSnapshotCP4(report, db, table, since, "ACCEPTED");
        long started = eventAttemptsSnapshotCP4(report, db, table, since, "STARTED");
        long rejected = eventAttemptsSnapshotCP4(report, db, table, since, "REJECTED");
        long canonical = eventAttemptsSnapshotCP4(report, db, table, since, "CANONICAL");
        long explicitTimeout = eventTextAttemptsSnapshotCP4(report, db, table, since, "TIMEOUT");
        long explicitCancel = eventTextAttemptsSnapshotCP4(report, db, table, since, "CANCEL");
        b.append("event_attempts requested=").append(requested)
                .append(" accepted=").append(accepted)
                .append(" started=").append(started)
                .append(" rejected=").append(rejected)
                .append(" canonical=").append(canonical)
                .append(" explicit_timeout=").append(explicitTimeout)
                .append(" explicit_cancel=").append(explicitCancel)
                .append(" unresolved_requested=").append(scalarSnapshotCP4(report, db, "SELECT COUNT(DISTINCT r.attempt_id) FROM " + table
                        + " r WHERE r.event_type='REQUESTED' AND r.attempt_id<>'' AND r.created_at>=?"
                        + " AND NOT EXISTS(SELECT 1 FROM " + table + " x WHERE x.attempt_id=r.attempt_id"
                        + " AND x.event_type IN ('CANONICAL','REJECTED'))", new String[]{String.valueOf(since)}))
                .append('\n');
    }

public static long eventAttemptsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            SQLiteDatabase db, String table, long since, String eventType) {
        String where = since > 0L ? "created_at>=? AND " : "";
        String[] args = since > 0L
                ? new String[] {String.valueOf(since), eventType}
                : new String[] {eventType};
        return scalarSnapshotCP4(report, db,
                "SELECT COUNT(DISTINCT attempt_id) FROM " + table + " WHERE " + where
                        + "event_type=? AND attempt_id<>''", args);
    }

public static long eventTextAttemptsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            SQLiteDatabase db, String table, long since, String token) {
        String where = since > 0L ? "created_at>=? AND " : "";
        String[] args = since > 0L
                ? new String[] {String.valueOf(since), "%" + token + "%", "%" + token + "%"}
                : new String[] {"%" + token + "%", "%" + token + "%"};
        return scalarSnapshotCP4(report, db,
                "SELECT COUNT(DISTINCT attempt_id) FROM " + table + " WHERE " + where
                        + "attempt_id<>'' AND (stage LIKE ? OR payload_text LIKE ?)", args);
    }

public static void appendDurableCountsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, com.ethk3.novaonepro.ReportTextCP12 b, long since) {
        String table = DbHelper.TABLE_R36_ANALYSIS_REQUEST;
        String where = since > 0L ? " WHERE request_at>=?" : "";
        String[] args = since > 0L ? new String[] {String.valueOf(since)} : null;
        Cursor c = report.select(
                "SELECT status,COUNT(*),COALESCE(SUM(attempts),0),"
                        + "COALESCE(SUM(CASE WHEN error_code<>'' THEN 1 ELSE 0 END),0),"
                        + "COALESCE(SUM(CASE WHEN first_error_code<>'' THEN 1 ELSE 0 END),0)"
                        + " FROM " + table + where + " GROUP BY status ORDER BY status",
                args);
        try {
            long rows = 0L, claimAttempts = 0L, errors = 0L, firstErrors = 0L;
            while (c.moveToNext()) {
                String status = safeSnapshotCP4(report, c.getString(0));
                long n = c.getLong(1), attempts = c.getLong(2), e = c.getLong(3), first = c.getLong(4);
                rows += n;
                claimAttempts += attempts;
                errors += e;
                firstErrors += first;
                b.append("durable_status ").append(status)
                        .append(" rows=").append(n)
                        .append(" claim_attempts=").append(attempts)
                        .append(" active_errors=").append(e)
                        .append(" first_error_preserved=").append(first)
                        .append('\n');
            }
            b.append("durable_totals rows=").append(rows)
                    .append(" claim_attempts=").append(claimAttempts)
                    .append(" active_errors=").append(errors)
                    .append(" first_error_preserved=").append(firstErrors)
                    .append('\n');
        } finally {
            c.close();
        }
    }

public static void appendRecentFailuresSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, com.ethk3.novaonepro.ReportTextCP12 b, long since) {
        String table = DbHelper.TABLE_R36_ANALYSIS_REQUEST;
        String where = " WHERE (status IN ('RETRY_WAIT','EXPIRED','FAILED_TERMINAL')"
                + " OR error_code<>'' OR first_error_code<>'')";
        java.util.ArrayList<String> argsList = new java.util.ArrayList<String>();
        if (since > 0L) {
            where += " AND request_at>=?";
            argsList.add(String.valueOf(since));
        }
        Cursor c = report.select(
                "SELECT target_issue,status,attempts,request_at,deadline_at,last_transition_at,"
                        + "error_code,first_error_code,last_stage FROM " + table + where
                        + " ORDER BY last_transition_at DESC LIMIT 30",
                argsList.isEmpty() ? null : argsList.toArray(new String[argsList.size()]));
        try {
            b.append("recent_failure_or_retry_rows\n");
            while (c.moveToNext()) {
                long deadline = c.getLong(4);
                long transition = c.getLong(5);
                boolean deadlineExceeded = deadline > 0L && transition > deadline;
                b.append("issue=").append(safeSnapshotCP4(report, c.getString(0)))
                        .append("|status=").append(safeSnapshotCP4(report, c.getString(1)))
                        .append("|attempts=").append(c.getLong(2))
                        .append("|request_at=").append(c.getLong(3))
                        .append("|deadline_at=").append(deadline)
                        .append("|last_transition_at=").append(transition)
                        .append("|deadline_exceeded=").append(deadlineExceeded ? 1 : 0)
                        .append("|error=").append(safeSnapshotCP4(report, c.getString(6)))
                        .append("|first_error=").append(safeSnapshotCP4(report, c.getString(7)))
                        .append("|stage=").append(safeSnapshotCP4(report, c.getString(8)))
                        .append('\n');
            }
        } finally {
            c.close();
        }
    }

public static long releaseStartedAtSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db) {
        Cursor c = report.select(
                "SELECT started_at FROM v57_runtime_epoch_r925 WHERE epoch_id=? LIMIT 1",
                new String[] {"release-" + BuildConfig.VERSION_CODE});
        try {
            return c.moveToFirst() ? Math.max(0L, c.getLong(0)) : 0L;
        } finally {
            c.close();
        }
    }

public static long scalarSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, String sql, String[] args) {
        Cursor c = report.select(sql, args);
        try {
            return c.moveToFirst() ? Math.max(0L, c.getLong(0)) : 0L;
        } finally {
            c.close();
        }
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('|', '/').trim();
    }

public static String trimSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String value, int max) {
        String x = safeSnapshotCP4(report, value);
        return x.length() <= max ? x : x.substring(0, max);
    }
}
