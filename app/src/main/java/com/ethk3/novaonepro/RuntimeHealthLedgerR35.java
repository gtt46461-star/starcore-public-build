package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Short, bounded operational ledger. It never stores payloads or stack dumps. */
public final class RuntimeHealthLedgerR35 {
    private RuntimeHealthLedgerR35() {}

    public static void record(
            DbStore store,
            String stage,
            String targetIssue,
            String status,
            long elapsedMs,
            int activeConcurrency,
            int maxConcurrency,
            int oomCount,
            String detail) {
        if (store == null) return;
        long now = System.currentTimeMillis();
        try {
            SQLiteDatabase db = store.writable();
            DbHelper.ensureR35Schema(db);
            ContentValues cv = new ContentValues();
            cv.put("event_at", now);
            cv.put("stage", safe(stage));
            cv.put("target_issue", safe(targetIssue));
            cv.put("status", safe(status));
            cv.put("elapsed_ms", Math.max(0L, elapsedMs));
            cv.put("active_concurrency", Math.max(0, activeConcurrency));
            cv.put("max_concurrency", Math.max(0, maxConcurrency));
            cv.put("oom_count", Math.max(0, oomCount));
            cv.put("detail", trim(detail, 700));
            cv.put(
                    "record_hash",
                    V51ChecksumEngine.sha256(
                            now
                                    + "|"
                                    + System.nanoTime()
                                    + "|"
                                    + safe(stage)
                                    + "|"
                                    + safe(targetIssue)
                                    + "|"
                                    + safe(status)));
            db.insertWithOnConflict(
                    DbHelper.TABLE_R35_RUNTIME_HEALTH, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
            trim(db);
        } catch (Throwable ignore) {
        }
    }

    public static void recordOom(
            DbStore store, String stage, String targetIssue, OutOfMemoryError error) {
        record(
                store,
                stage,
                targetIssue,
                "OOM_CAUGHT",
                0L,
                AnalysisSingleFlightR35.shared().activeCount(),
                AnalysisSingleFlightR35.shared().maximumObserved(),
                1,
                error == null ? "OutOfMemoryError" : error.getClass().getSimpleName());
    }

    public static Snapshot snapshot(DbStore store) {
        if (store == null) return new Snapshot(0, 0, 0, 0L, "NO_DB", "");
        Cursor c = null;
        try {
            SQLiteDatabase db = store.readable();
            DbHelper.ensureR35Schema(db);
            int total = scalarInt(db, "SELECT COUNT(1) FROM " + DbHelper.TABLE_R35_RUNTIME_HEALTH);
            int oom =
                    scalarInt(
                            db,
                            "SELECT COALESCE(SUM(oom_count),0) FROM "
                                    + DbHelper.TABLE_R35_RUNTIME_HEALTH);
            int max =
                    scalarInt(
                            db,
                            "SELECT COALESCE(MAX(max_concurrency),0) FROM "
                                    + DbHelper.TABLE_R35_RUNTIME_HEALTH);
            int completed =
                    scalarInt(
                            db,
                            "SELECT COUNT(1) FROM "
                                    + DbHelper.TABLE_R35_RUNTIME_HEALTH
                                    + " WHERE stage='ANALYSIS' AND status='SUCCESS'");
            long p95 = 0L;
            if (completed > 0) {
                int offset = Math.max(0, (int) Math.ceil(completed * 0.95d) - 1);
                c =
                        db.rawQuery(
                                "SELECT elapsed_ms FROM "
                                        + DbHelper.TABLE_R35_RUNTIME_HEALTH
                                        + " WHERE stage='ANALYSIS' AND status='SUCCESS' ORDER BY"
                                        + " elapsed_ms ASC LIMIT 1 OFFSET "
                                        + offset,
                                null);
                if (c.moveToFirst()) p95 = c.getLong(0);
                c.close();
                c = null;
            }
            String lastStatus = "";
            String lastTarget = "";
            c =
                    db.rawQuery(
                            "SELECT status,target_issue FROM "
                                    + DbHelper.TABLE_R35_RUNTIME_HEALTH
                                    + " ORDER BY event_at DESC,_id DESC LIMIT 1",
                            null);
            if (c.moveToFirst()) {
                lastStatus = c.getString(0);
                lastTarget = c.getString(1);
            }
            return new Snapshot(total, oom, max, p95, lastStatus, lastTarget);
        } catch (Throwable e) {
            return new Snapshot(0, 0, 0, 0L, "READ_ERROR", e.getClass().getSimpleName());
        } finally {
            if (c != null) c.close();
        }
    }

    private static void trim(SQLiteDatabase db) {
        try {
            db.execSQL(
                    "DELETE FROM "
                            + DbHelper.TABLE_R35_RUNTIME_HEALTH
                            + " WHERE _id NOT IN (SELECT _id FROM "
                            + DbHelper.TABLE_R35_RUNTIME_HEALTH
                            + " ORDER BY event_at DESC,_id DESC LIMIT 2000)");
        } catch (Throwable ignore) {
        }
    }

    private static int scalarInt(SQLiteDatabase db, String sql) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, null);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    private static String safe(String x) {
        return x == null ? "" : x;
    }

    private static String trim(String x, int max) {
        x = safe(x);
        return x.length() <= max ? x : x.substring(0, max);
    }

    public static final class Snapshot {
        public final int eventCount, oomCount, maxConcurrency;
        public final long analysisP95Ms;
        public final String lastStatus, lastTargetIssue;

        Snapshot(int events, int oom, int max, long p95, String status, String target) {
            eventCount = events;
            oomCount = oom;
            maxConcurrency = max;
            analysisP95Ms = p95;
            lastStatus = safe(status);
            lastTargetIssue = safe(target);
        }

        public String summary() {
            return "events="
                    + eventCount
                    + "|analysisMaxConcurrency="
                    + maxConcurrency
                    + "|analysisP95Ms="
                    + analysisP95Ms
                    + "|oom="
                    + oomCount
                    + "|last="
                    + lastStatus
                    + "|target="
                    + lastTargetIssue;
        }
    }
}
