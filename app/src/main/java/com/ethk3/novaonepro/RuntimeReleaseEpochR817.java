package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.HashMap;

/** Durable, non-destructive start boundary for R817 runtime-health acceptance metrics. */
public final class RuntimeReleaseEpochR817 {
    public static final String EPOCH_KEY = "r817_runtime_epoch_at";
    public static final String VERSION_KEY = "r817_runtime_epoch_version";

    private RuntimeReleaseEpochR817() {}

    public static void ensure(final DbStore store) {
        if (store == null) throw new IllegalArgumentException("R817_EPOCH_STORE_NULL");
        // R971: repeated readers must not acquire the global writer merely to discover no work.
        // Recheck inside the transaction below, so concurrent first-open remains idempotent.
        SQLiteDatabase readable = store.readable();
        if (readAt(readable) > 0L && AppConfig.R36_RUNTIME_REPAIR_VERSION.equals(readVersion(readable))) return;
        store.coordinatorR36()
                .runWrite(
                        "R817_RUNTIME_EPOCH",
                        new DatabaseCoordinator.WriteRunnable() {
                            @Override
                            public void run(SQLiteDatabase db) {
                                long existingAt = readAt(db);
                                String existingVersion = readVersion(db);
                                // R820: acceptance latency/OOM/concurrency must be scoped to the
                                // currently installed release. Keep historical ledger rows intact,
                                // but never carry an older release's P95 into a new release verdict.
                                if (existingAt > 0L
                                        && AppConfig.R36_RUNTIME_REPAIR_VERSION.equals(existingVersion))
                                    return;
                                long now = System.currentTimeMillis();
                                HashMap<String, String> values = new HashMap<String, String>();
                                values.put(EPOCH_KEY, String.valueOf(now));
                                values.put(VERSION_KEY, AppConfig.R36_RUNTIME_REPAIR_VERSION);
                                DbStore.putStatesR36(db, values);
                            }
                        });
    }

    public static long readAt(DbStore store) {
        if (store == null) return -1L;
        try {
            long value = Long.parseLong(store.getState(EPOCH_KEY, "-1"));
            return value > 0L ? value : -1L;
        } catch (Throwable ignored) {
            return -1L;
        }
    }

    private static String readVersion(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT state_value FROM "
                                    + DbHelper.TABLE_APP_STATE
                                    + " WHERE state_key=? LIMIT 1",
                            new String[] {VERSION_KEY});
            return cursor.moveToFirst() ? safe(cursor.getString(0)) : "";
        } catch (Throwable ignored) {
            return "";
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static long readAt(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT state_value FROM "
                                    + DbHelper.TABLE_APP_STATE
                                    + " WHERE state_key=? LIMIT 1",
                            new String[] {EPOCH_KEY});
            if (!cursor.moveToFirst()) return -1L;
            long value = Long.parseLong(cursor.getString(0));
            return value > 0L ? value : -1L;
        } catch (Throwable ignored) {
            return -1L;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

public static long readAtSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return -1L;
        try {
            long value = Long.parseLong(store.getState(EPOCH_KEY, "-1"));
            return value > 0L ? value : -1L;
        } catch (Throwable ignored) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",ignored);}
    }

public static long readAtSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor =
                    report.select(
                            "SELECT state_value FROM "
                                    + DbHelper.TABLE_APP_STATE
                                    + " WHERE state_key=? LIMIT 1",
                            new String[] {EPOCH_KEY});
            if (!cursor.moveToFirst()) return -1L;
            long value = Long.parseLong(cursor.getString(0));
            return value > 0L ? value : -1L;
        } catch (Throwable ignored) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",ignored);} finally {
            if (cursor != null) cursor.close();
        }
    }
}
