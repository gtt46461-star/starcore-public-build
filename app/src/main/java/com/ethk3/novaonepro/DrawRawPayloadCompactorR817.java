package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Bounded repair for legacy rows that contain a copy of an entire provider response.  Business
 * identity, numbers, timestamps, draw hashes and the R811 provenance ledger remain untouched.
 */
public final class DrawRawPayloadCompactorR817 {
    public static final int MAX_PER_OBJECT_BYTES = 8192;

    private DrawRawPayloadCompactorR817() {}

    public static int compactBatch(final DbStore store, int requested) {
        if (store == null || RealtimePressureGateR811.isUnderPressure()) return 0;
        final int limit = Math.max(1, Math.min(32, requested));
        Integer changed =
                store.coordinatorR36()
                        .runWrite(
                                "R817_DRAW_RAW_PAYLOAD_COMPACT",
                                new DatabaseCoordinator.WriteCallable<Integer>() {
                                    @Override
                                    public Integer call(SQLiteDatabase db) {
                                        db.execSQL(
                                                "UPDATE "
                                                        + DbHelper.TABLE_DRAWS
                                                        + " SET raw_json='' WHERE _id IN (SELECT _id FROM "
                                                        + DbHelper.TABLE_DRAWS
                                                        + " WHERE length(COALESCE(raw_json,''))>CAST(? AS INTEGER)"
                                                        + " ORDER BY _id ASC LIMIT ?)",
                                                new Object[] {
                                                    Integer.valueOf(MAX_PER_OBJECT_BYTES),
                                                    Integer.valueOf(limit)
                                                });
                                        Cursor cursor = null;
                                        try {
                                            cursor = db.rawQuery("SELECT changes()", null);
                                            return Integer.valueOf(
                                                    cursor.moveToFirst() ? cursor.getInt(0) : 0);
                                        } finally {
                                            if (cursor != null) cursor.close();
                                        }
                                    }
                                },
                                Integer.valueOf(0));
        int result = changed == null ? 0 : changed.intValue();
        if (result > 0) {
            store.setState("r817_raw_payload_compacted_last", String.valueOf(result));
            store.setState(
                    "r817_raw_payload_compacted_at", String.valueOf(System.currentTimeMillis()));
        }
        return result;
    }

    public static int pending(DbStore store) {
        if (store == null) return -1;
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT COUNT(1) FROM "
                                            + DbHelper.TABLE_DRAWS
                                            + " WHERE length(COALESCE(raw_json,''))>CAST(? AS INTEGER)",
                                    new String[] {String.valueOf(MAX_PER_OBJECT_BYTES)});
            return cursor.moveToFirst() ? cursor.getInt(0) : -1;
        } catch (Throwable ignored) {
            return -1;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

public static int pendingSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return -1;
        Cursor cursor = null;
        try {
            cursor =
                    report.select(
                                    "SELECT COUNT(1) FROM "
                                            + DbHelper.TABLE_DRAWS
                                            + " WHERE length(COALESCE(raw_json,''))>CAST(? AS INTEGER)",
                                    new String[] {String.valueOf(MAX_PER_OBJECT_BYTES)});
            return cursor.moveToFirst() ? cursor.getInt(0) : -1;
        } catch (Throwable ignored) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",ignored);} finally {
            if (cursor != null) cursor.close();
        }
    }
}
