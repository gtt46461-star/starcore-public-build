package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Typed R3.6 health ledger. Read failures remain explicit and can never look like zero/PASS. */
public final class RuntimeHealthLedgerR36 {
    public static final String DURABLE_RETURN_EVENT_R923 = "ANALYSIS_DURABLE_RETURNED_R923";
    private RuntimeHealthLedgerR36() {}

    public static void record(
            final DbStore store,
            final String eventType,
            final String attemptId,
            final String target,
            final String status,
            final long requestAt,
            final long commitAt,
            final long elapsed,
            final int active,
            final int maximum,
            final boolean terminal,
            final String detail) {
        if (store == null) return;
        store.coordinatorR36()
                .runWrite(
                        "R36_HEALTH",
                        new DatabaseCoordinator.WriteRunnable() {
                            @Override
                            public void run(SQLiteDatabase db) {
                                recordWithinTransaction(
                                        db, eventType, attemptId, target, status, requestAt,
                                        commitAt, elapsed, active, maximum, terminal, detail);
                            }
                        });
    }

    static void recordWithinTransaction(
            SQLiteDatabase db,
            String eventType,
            String attemptId,
            String target,
            String status,
            long requestAt,
            long commitAt,
            long elapsed,
            int active,
            int maximum,
            boolean terminal,
            String detail) {
        long now = System.currentTimeMillis();
        final String storedStatusR80 = DURABLE_RETURN_EVENT_R923.equals(eventType) && "SUCCESS".equals(status) && elapsed < 0L ? "UNMEASURED" : safe(status);
        String hash =
                V51ChecksumEngine.sha256(
                        "R36_HEALTH|"
                                + safe(eventType)
                                + '|'
                                + safe(attemptId)
                                + '|'
                                + safe(target)
                                + '|'
                                + storedStatusR80
                                + '|'
                                + now
                                + '|'
                                + elapsed
                                + '|'
                                + safe(detail));
        ContentValues cv = new ContentValues();
        cv.put("event_at", now);
        cv.put("stage", safe(eventType));
        cv.put("target_issue", safe(target));
        cv.put("status", storedStatusR80);
        cv.put("elapsed_ms", Math.max(0L, elapsed));
        cv.put("active_concurrency", Math.max(0, active));
        cv.put("max_concurrency", Math.max(0, maximum));
        cv.put("oom_count", "OOM".equals(status) ? 1 : 0);
        cv.put("detail", trim(detail, 800));
        cv.put("record_hash", hash);
        cv.put("attempt_id", safe(attemptId));
        cv.put("event_type", safe(eventType));
        cv.put("request_at", Math.max(0L, requestAt));
        cv.put("commit_at", Math.max(0L, commitAt));
        cv.put("terminal", terminal ? 1 : 0);
        db.insertOrThrow(DbHelper.TABLE_R35_RUNTIME_HEALTH, null, cv);
    }

    public static Metrics metrics(DbStore store, String eventType, int limit) {
        if (store == null) return Metrics.readError("STORE_NULL");
        long epochAt = RuntimeReleaseEpochR817.readAt(store);
        if (epochAt <= 0L) return Metrics.readError("R816_RELEASE_EPOCH_MISSING");
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT elapsed_ms,status,target_issue,event_at,detail FROM "
                                            + DbHelper.TABLE_R35_RUNTIME_HEALTH
                                            + " WHERE event_type=? AND event_at>=?"
                                            + " ORDER BY event_at DESC,_id DESC LIMIT ?",
                                    new String[] {
                                        safe(eventType),
                                        String.valueOf(epochAt),
                                        String.valueOf(Math.max(1, limit))
                                    });
            java.util.ArrayList<Long> values = new java.util.ArrayList<Long>();
            String status = "NEVER", target = "";
            long at = 0L;
            int windowSample = 0, unmeasured = 0;
            while (c.moveToNext()) {
                if (windowSample == 0) {
                    status = safe(c.getString(1));
                    target = safe(c.getString(2));
                    at = c.getLong(3);
                }
                windowSample++;
                if (DURABLE_RETURN_EVENT_R923.equals(eventType)
                        && !measuredDurableR80(c.getLong(0), c.getString(1), c.getString(4))) {
                    unmeasured++;
                    continue;
                }
                values.add(Long.valueOf(c.getLong(0)));
            }
            if (values.size() == 0) return new Metrics(windowSample == 0 ? "NO_DATA" : "UNMEASURED",
                    0, -1L, status, target, at, "", windowSample, unmeasured);
            java.util.Collections.sort(values);
            int index = (int) Math.ceil(values.size() * 0.95d) - 1;
            if (index < 0) index = 0;
            return new Metrics(
                    "OK", values.size(), values.get(index).longValue(), status, target, at, "", windowSample, unmeasured);
        } catch (Throwable e) {
            return Metrics.readError(e.getClass().getSimpleName());
        } finally {
            if (c != null) c.close();
        }
    }

    public static final class Metrics {
        public final String readStatus, lastStatus, lastTarget, error;
        public final int sample, windowSample, unmeasured;
        public final long p95Ms, lastAt;

        Metrics(
                String read,
                int sample,
                long p95,
                String last,
                String target,
                long at,
                String error) {
            this(read, sample, p95, last, target, at, error, sample, 0);
        }

        Metrics(String read, int sample, long p95, String last, String target, long at,
                String error, int windowSample, int unmeasured) {
            readStatus = read;
            this.windowSample = windowSample;
            this.unmeasured = unmeasured;
            this.sample = sample;
            p95Ms = p95;
            lastStatus = last;
            lastTarget = target;
            lastAt = at;
            this.error = error;
        }

        static Metrics readError(String e) {
            return new Metrics("READ_ERROR", -1, -1L, "READ_ERROR", "", 0L, safe(e));
        }
    }

    // Only a measured monotonic request-to-transaction-return receipt is a latency sample.
    // Legacy rehydrated SUCCESS/0 records remain intact but are not observations of 0ms.
    public static boolean measuredDurableR80(long elapsed, String status, String detail) {
        return elapsed >= 0L && "SUCCESS".equals(status)
                && "MONOTONIC_REQUEST_TO_TRANSACTION_RETURN|TELEMETRY_IO_EXCLUDED".equals(detail);
    }

    static void applyPerformanceR80(BusinessTruth out, Metrics metrics) {
        out.performanceMeasured = metrics.sample;
        out.performanceWindow = metrics.windowSample;
        out.performanceUnmeasured = metrics.unmeasured;
        out.performanceState = !"OK".equals(metrics.readStatus) || metrics.sample <= 0 || metrics.p95Ms < 0L
                ? "UNKNOWN" : metrics.p95Ms <= out.performanceThresholdMs ? "PASS" : "FAIL";
    }

    /** Single read-only health truth used by UI and diagnostic renderers. */
    public static BusinessTruth businessTruth(DbStore store) {
        if (store == null) return BusinessTruth.readError("STORE_NULL");
        Cursor c = null;
        try {
            BusinessTruth out = new BusinessTruth();
            AutoFetchService.B13RuntimeHealth runtimeHealth = AutoFetchService.b13Health(store);
            out.serviceAlive = runtimeHealth.serviceAlive;
            out.businessAlive = runtimeHealth.businessAlive;
            out.runtimeHealthRootCause = safe(runtimeHealth.rootCause);
            out.latestDrawIssue = safe(store.getLatestDrawIssue());
            SQLiteDatabase db = store.readable();
            out.latestCanonicalIssue =
                    scalarText(
                            db,
                            "SELECT target_issue FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " ORDER BY target_issue DESC LIMIT 1");
            out.latestSettledIssue =
                    scalarText(
                            db,
                            "SELECT target_issue FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " WHERE settled=1 ORDER BY target_issue DESC LIMIT 1");
            out.latestOosIssue =
                    scalarText(
                            db,
                            "SELECT target_issue FROM "
                                    + DbHelper.TABLE_R35_OOS_LABEL
                                    + " WHERE strict_oos=1 ORDER BY target_issue DESC LIMIT 1");
            c =
                    db.rawQuery(
                            "SELECT COUNT(*) FROM "
                                    + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                    + " WHERE status<>'COMPLETED'",
                            null);
            if (c.moveToFirst()) out.deferredPending = c.getInt(0);
            c.close();
            c = null;
            Metrics analysis = metrics(store, DURABLE_RETURN_EVENT_R923, 120);
            if ("READ_ERROR".equals(analysis.readStatus)) {
                return BusinessTruth.readError("ANALYSIS_METRICS:" + analysis.error);
            }
            c = db.rawQuery(
                    "SELECT target_issue FROM " + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                            + " WHERE pre_draw_frozen=1 AND target_issue<=? ORDER BY target_issue DESC LIMIT 1",
                    new String[] {out.latestDrawIssue});
            out.expectedSettledIssue = c.moveToFirst() ? safe(c.getString(0)) : "";
            c.close();
            c = null;
            out.analysisP95Ms = analysis.p95Ms;
            out.analysisEpochAt = RuntimeReleaseEpochR817.readAt(store);
            applyPerformanceR80(out, analysis);
            classifyBusinessR77(out, analysis.sample);
            return out;
        } catch (Throwable e) {
            return BusinessTruth.readError(e.getClass().getSimpleName());
        } finally {
            if (c != null) c.close();
        }
    }

    /** Compare settlement with obligations that actually have immutable predraw evidence. */
    static void classifyBusinessR77(BusinessTruth out, int analysisSamples) {
        String expectedCanonical = AppConfig.nextIssueText(out.latestDrawIssue);
        out.drawToCanonicalLag = out.latestDrawIssue.length() == 0 ? -1
                : (expectedCanonical.equals(out.latestCanonicalIssue) ? 0 : 1);
        out.latestDrawPredrawMissing = out.latestDrawIssue.length() > 0
                && !out.latestDrawIssue.equals(out.expectedSettledIssue);
        out.drawToSettledLag = out.expectedSettledIssue.length() == 0 ? -1
                : (out.expectedSettledIssue.equals(out.latestSettledIssue) ? 0 : 1);
        out.settledToOosLag = out.latestSettledIssue.length() == 0 ? -1
                : (out.latestSettledIssue.equals(out.latestOosIssue) ? 0 : 1);
        if (!out.businessAlive || out.drawToCanonicalLag > 0) {
            out.status = "FAIL";
            out.rootCause = "RUNTIME_CHAIN";
        } else if (out.drawToSettledLag > 0 || out.settledToOosLag > 0) {
            out.status = "DEGRADED";
            out.rootCause = "SETTLEMENT_OOS";
        } else if (out.latestDrawPredrawMissing) {
            out.status = "FAIL";
            out.rootCause = "PREDRAW_COVERAGE_MISSING";
        } else if (analysisSamples <= 0) {
            out.status = "DEGRADED";
            out.rootCause = "MODEL_SAMPLE";
        } else {
            out.status = "HEALTHY";
            out.rootCause = "NONE";
        }
    }

    private static String scalarText(SQLiteDatabase db, String sql) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, null);
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        } finally {
            if (c != null) c.close();
        }
    }

    public static final class BusinessTruth {
        public boolean serviceAlive, businessAlive;
        public String runtimeHealthRootCause = "READ_ERROR";
        public String status = "UNKNOWN";
        public String rootCause = "READ_ERROR";
        public String latestDrawIssue = "";
        public String latestCanonicalIssue = "";
        public String latestSettledIssue = "";
        public String expectedSettledIssue = "";
        public boolean latestDrawPredrawMissing;
        public String latestOosIssue = "";
        public int drawToCanonicalLag = -1;
        public int drawToSettledLag = -1;
        public int settledToOosLag = -1;
        public int deferredPending = -1;
        public long analysisP95Ms = -1L;
        public long analysisEpochAt = -1L;
        public String performanceState = "UNKNOWN";
        public String performanceScope = DURABLE_RETURN_EVENT_R923;
        public int performanceMeasured = 0, performanceWindow = 0, performanceUnmeasured = 0;
        public long performanceThresholdMs = 5000L;
        public String readError = "";

        static BusinessTruth readError(String error) {
            BusinessTruth out = new BusinessTruth();
            out.status = "FAIL";
            out.rootCause = "READ_ERROR";
            out.readError = safe(error);
            return out;
        }

        public String canonical() {
            return "status="
                    + status
                    + "|rootCause="
                    + rootCause
                    + "|serviceAlive="
                    + (serviceAlive ? 1 : 0)
                    + "|businessAlive="
                    + (businessAlive ? 1 : 0)
                    + "|runtimeHealthRootCause="
                    + runtimeHealthRootCause
                    + "|latestDraw="
                    + latestDrawIssue
                    + "|latestCanonical="
                    + latestCanonicalIssue
                    + "|latestSettled="
                    + latestSettledIssue
                    + "|latestOos="
                    + latestOosIssue
                    + "|expectedSettled="
                    + expectedSettledIssue
                    + "|latestDrawPredrawMissing="
                    + (latestDrawPredrawMissing ? 1 : 0)
                    + "|settlementLagScope=EXISTING_PREDRAW_OBLIGATIONS"
                    + "|lag="
                    + drawToCanonicalLag
                    + ','
                    + drawToSettledLag
                    + ','
                    + settledToOosLag
                    + "|deferred="
                    + deferredPending
                    + "|analysisP95="
                    + analysisP95Ms
                    + "|performanceState=" + performanceState
                    + "|performanceScope=" + performanceScope
                    + "|performanceMeasured=" + performanceMeasured
                    + "|performanceWindow=" + performanceWindow
                    + "|performanceUnmeasured=" + performanceUnmeasured
                    + "|performanceThresholdMs=" + performanceThresholdMs
                    + "|analysisScope=R900_CURRENT_RELEASE"
                    + "|analysisEpochAt="
                    + analysisEpochAt
                    + "|readError="
                    + readError;
        }
    }

    private static String trim(String x, int max) {
        String s = safe(x);
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String safe(String x) {
        return x == null ? "" : x.trim();
    }

public static BusinessTruth businessTruthSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return BusinessTruth.readError("STORE_NULL");
        Cursor c = null;
        try {
            BusinessTruth out = new BusinessTruth();
            AutoFetchService.B13RuntimeHealth runtimeHealth = AutoFetchService.b13HealthSnapshotCP4(report, store);
            out.serviceAlive = runtimeHealth.serviceAlive;
            out.businessAlive = runtimeHealth.businessAlive;
            out.runtimeHealthRootCause = safeSnapshotCP4(report, runtimeHealth.rootCause);
            out.latestDrawIssue = safeSnapshotCP4(report, store.getLatestDrawIssue());
            SQLiteDatabase db = store.readable();
            out.latestCanonicalIssue =
                    scalarTextSnapshotCP4(report, 
                            db,
                            "SELECT target_issue FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " ORDER BY target_issue DESC LIMIT 1");
            out.latestSettledIssue =
                    scalarTextSnapshotCP4(report, 
                            db,
                            "SELECT target_issue FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " WHERE settled=1 ORDER BY target_issue DESC LIMIT 1");
            out.latestOosIssue =
                    scalarTextSnapshotCP4(report, 
                            db,
                            "SELECT target_issue FROM "
                                    + DbHelper.TABLE_R35_OOS_LABEL
                                    + " WHERE strict_oos=1 ORDER BY target_issue DESC LIMIT 1");
            c =
                    report.select(
                            "SELECT COUNT(*) FROM "
                                    + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                    + " WHERE status<>'COMPLETED'",
                            null);
            if (c.moveToFirst()) out.deferredPending = c.getInt(0);
            c.close();
            c = null;
            Metrics analysis = metricsSnapshotCP4(report, store, DURABLE_RETURN_EVENT_R923, 120);
            if ("READ_ERROR".equals(analysis.readStatus)) {
                return BusinessTruth.readError("ANALYSIS_METRICS:" + analysis.error);
            }
            c = report.select(
                    "SELECT target_issue FROM " + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                            + " WHERE pre_draw_frozen=1 AND target_issue<=? ORDER BY target_issue DESC LIMIT 1",
                    new String[] {out.latestDrawIssue});
            out.expectedSettledIssue = c.moveToFirst() ? safe(c.getString(0)) : "";
            c.close();
            c = null;
            out.analysisP95Ms = analysis.p95Ms;
            out.analysisEpochAt = RuntimeReleaseEpochR817.readAtSnapshotCP4(report, store);
            applyPerformanceR80(out, analysis);
            classifyBusinessR77(out, analysis.sample);
            return out;
        } catch (Throwable e) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);} finally {
            if (c != null) c.close();
        }
    }

public static Metrics metricsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String eventType, int limit) {
        if (store == null) return Metrics.readError("STORE_NULL");
        long epochAt = RuntimeReleaseEpochR817.readAtSnapshotCP4(report, store);
        if (epochAt <= 0L) return Metrics.readError("R816_RELEASE_EPOCH_MISSING");
        Cursor c = null;
        try {
            c =
                    report.select(
                                    "SELECT elapsed_ms,status,target_issue,event_at,detail FROM "
                                            + DbHelper.TABLE_R35_RUNTIME_HEALTH
                                            + " WHERE event_type=? AND event_at>=?"
                                            + " ORDER BY event_at DESC,_id DESC LIMIT ?",
                                    new String[] {
                                        safeSnapshotCP4(report, eventType),
                                        String.valueOf(epochAt),
                                        String.valueOf(Math.max(1, limit))
                                    });
            java.util.ArrayList<Long> values = new java.util.ArrayList<Long>();
            String status = "NEVER", target = "";
            long at = 0L;
            int windowSample = 0, unmeasured = 0;
            while (c.moveToNext()) {
                if (windowSample == 0) {
                    status = safeSnapshotCP4(report, c.getString(1));
                    target = safeSnapshotCP4(report, c.getString(2));
                    at = c.getLong(3);
                }
                windowSample++;
                if (DURABLE_RETURN_EVENT_R923.equals(eventType)
                        && !measuredDurableR80(c.getLong(0), c.getString(1), c.getString(4))) {
                    unmeasured++;
                    continue;
                }
                values.add(Long.valueOf(c.getLong(0)));
            }
            if (values.size() == 0) return new Metrics(windowSample == 0 ? "NO_DATA" : "UNMEASURED",
                    0, -1L, status, target, at, "", windowSample, unmeasured);
            java.util.Collections.sort(values);
            int index = (int) Math.ceil(values.size() * 0.95d) - 1;
            if (index < 0) index = 0;
            return new Metrics(
                    "OK", values.size(), values.get(index).longValue(), status, target, at, "", windowSample, unmeasured);
        } catch (Throwable e) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);} finally {
            if (c != null) c.close();
        }
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String x) {
        return x == null ? "" : x.trim();
    }

public static String scalarTextSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, String sql) {
        Cursor c = null;
        try {
            c = report.select(sql, null);
            return c.moveToFirst() ? safeSnapshotCP4(report, c.getString(0)) : "";
        } finally {
            if (c != null) c.close();
        }
    }
}
