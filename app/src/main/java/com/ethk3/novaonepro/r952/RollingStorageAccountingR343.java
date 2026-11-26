package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

/** Derived rolling working-set accounting. The original rows and receipts remain authoritative. */
final class RollingStorageAccountingR343 {
    static final String CAPACITY = "research_rolling_capacity_r343";
    static final String RESIDENT = "research_rolling_resident_r343";
    private static final int VERSION_PAGE = 8;
    private static final int FEATURE_PAGE = 32;
    private static final String VERSION = RollingLearnerR343.VERSION;
    private static final String FEATURE = RollingLearnerR343.FEATURE;
    private static final String VERSION_BYTES = "COALESCE(length(%1$sinput_blob),0)"
            + "+COALESCE(length(%1$scheckpoint),0)+length(CAST(%1$smodel_payload AS BLOB))";
    private static final String FEATURE_BYTES = "length(%1$spayload)+length(CAST(%1$ssource_json AS BLOB))";

    private RollingStorageAccountingR343() {}

    static void schema(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("ROLLING_ACCOUNTING_SCHEMA_TX");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + CAPACITY
                + "(singleton INTEGER PRIMARY KEY CHECK(singleton=1),"
                + "live_bytes INTEGER NOT NULL DEFAULT 0 CHECK(live_bytes>=0),"
                + "phase INTEGER NOT NULL CHECK(phase BETWEEN 0 AND 2),"
                + "version_after TEXT NOT NULL DEFAULT '',"
                + "feature_after_version TEXT NOT NULL DEFAULT '',"
                + "feature_after_index INTEGER NOT NULL DEFAULT -1)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + RESIDENT
                + "(version_id TEXT PRIMARY KEY REFERENCES " + VERSION + "(version_id),"
                + "version_bytes INTEGER NOT NULL DEFAULT 0 CHECK(version_bytes>=0),"
                + "feature_bytes INTEGER NOT NULL DEFAULT 0 CHECK(feature_bytes>=0))");
        db.execSQL("INSERT OR IGNORE INTO " + CAPACITY + "(singleton,phase)"
                + " SELECT 1,CASE WHEN EXISTS(SELECT 1 FROM " + VERSION + ") THEN 0 ELSE 2 END");

        String versionCovered = "(SELECT phase>0 OR NEW.version_id<=version_after FROM " + CAPACITY + ")";
        String newVersionBytes = qualified(VERSION_BYTES, "NEW.");
        String previousVersionBytes = qualified(VERSION_BYTES, "OLD.");
        track(db, "version_insert", "INSERT", VERSION, versionCovered, "NEW.version_id",
                "version_bytes", newVersionBytes);
        track(db, "version_update", "UPDATE OF input_blob,checkpoint,model_payload", VERSION,
                versionCovered, "NEW.version_id", "version_bytes", newVersionBytes + "-(" + previousVersionBytes + ")");

        track(db, "feature_insert", "INSERT", FEATURE, featureCovered("NEW"), "NEW.version_id",
                "feature_bytes", qualified(FEATURE_BYTES, "NEW."));
        track(db, "feature_delete", "DELETE", FEATURE, featureCovered("OLD"), "OLD.version_id",
                "feature_bytes", "-(" + qualified(FEATURE_BYTES, "OLD.") + ")");
    }

    private static String featureCovered(String row) {
        return "(SELECT phase=2 OR (phase=1 AND (" + row + ".version_id," + row
                + ".row_index)<=(feature_after_version,feature_after_index)) FROM " + CAPACITY + ")";
    }

    private static String qualified(String expression, String qualifier) {
        return expression.replace("%1$s", qualifier);
    }

    private static void track(SQLiteDatabase db, String name, String event, String table, String covered,
                              String versionId, String column, String delta) {
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r343_resident_" + name + " AFTER " + event + " ON " + table
                + " WHEN " + covered + " AND (" + delta + ")<>0 BEGIN "
                + "INSERT OR IGNORE INTO " + RESIDENT + "(version_id) VALUES(" + versionId + ");"
                + "UPDATE " + RESIDENT + " SET " + column + "=" + column + "+(" + delta + ") WHERE version_id=" + versionId + ";"
                + "UPDATE " + CAPACITY + " SET live_bytes=live_bytes+(" + delta + ") WHERE singleton=1;"
                + "DELETE FROM " + RESIDENT + " WHERE version_id=" + versionId
                + " AND version_bytes=0 AND feature_bytes=0; END");
    }

    static boolean installed(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN(?,?)",
                new String[]{CAPACITY, RESIDENT})) {
            return cursor.moveToFirst() && cursor.getInt(0) == 2;
        }
    }

    /** -1 while bounded legacy reconciliation still owns the accounting cursor. */
    static long liveBytes(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery("SELECT phase,live_bytes FROM " + CAPACITY + " WHERE singleton=1", null)) {
            if (!cursor.moveToFirst()) throw new IllegalStateException("ROLLING_ACCOUNTING_MISSING");
            return cursor.getInt(0) == 2 ? cursor.getLong(1) : -1;
        }
    }

    /** Uses ordered primary-key seeks. Changes to the covered prefix are counted by the same
     * transaction's triggers. Inserts or deletes ahead of it are read in their committed state.
     * An interrupted page rolls back the cursor and all derived bytes together. */
    static boolean reconcileOne(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("ROLLING_ACCOUNTING_RECONCILE_TX");
        try (Cursor cursor = db.rawQuery("SELECT phase,version_after,feature_after_version,feature_after_index FROM "
                + CAPACITY + " WHERE singleton=1", null)) {
            if (!cursor.moveToFirst()) throw new IllegalStateException("ROLLING_ACCOUNTING_MISSING");
            int phase = cursor.getInt(0);
            if (phase == 0) return reconcileVersions(db, cursor.getString(1));
            if (phase == 1) return reconcileFeatures(db, cursor.getString(2), cursor.getInt(3));
            return false;
        }
    }

    private static boolean reconcileVersions(SQLiteDatabase db, String after) {
        List<ResidentRow> rows = new ArrayList<>(VERSION_PAGE);
        try (Cursor cursor = db.rawQuery("SELECT version_id," + qualified(VERSION_BYTES, "") + " FROM " + VERSION
                + " WHERE version_id>? ORDER BY version_id LIMIT " + VERSION_PAGE, new String[]{after})) {
            while (cursor.moveToNext()) rows.add(new ResidentRow(cursor.getString(0), -1, cursor.getLong(1)));
        }
        long bytes = addRows(db, rows, "version_bytes");
        String next = rows.isEmpty() ? after : rows.get(rows.size() - 1).version;
        db.execSQL("UPDATE " + CAPACITY + " SET live_bytes=live_bytes+?, phase=?,version_after=? WHERE singleton=1",
                new Object[]{bytes, rows.size() < VERSION_PAGE ? 1 : 0, next});
        return true;
    }

    private static boolean reconcileFeatures(SQLiteDatabase db, String after, int index) {
        List<ResidentRow> rows = new ArrayList<>(FEATURE_PAGE);
        try (Cursor cursor = db.rawQuery("SELECT version_id,row_index," + qualified(FEATURE_BYTES, "") + " FROM " + FEATURE
                        + " WHERE (version_id,row_index)>(?,?) ORDER BY version_id,row_index LIMIT " + FEATURE_PAGE,
                new String[]{after, Integer.toString(index)})) {
            while (cursor.moveToNext()) rows.add(new ResidentRow(cursor.getString(0), cursor.getInt(1), cursor.getLong(2)));
        }
        long bytes = addRows(db, rows, "feature_bytes");
        ResidentRow next = rows.isEmpty() ? new ResidentRow(after, index, 0) : rows.get(rows.size() - 1);
        db.execSQL("UPDATE " + CAPACITY + " SET live_bytes=live_bytes+?,phase=?,feature_after_version=?,"
                        + "feature_after_index=? WHERE singleton=1",
                new Object[]{bytes, rows.size() < FEATURE_PAGE ? 2 : 1, next.version, next.index});
        return true;
    }

    private static long addRows(SQLiteDatabase db, List<ResidentRow> rows, String column) {
        long bytes = 0;
        for (ResidentRow row : rows) {
            if (row.bytes == 0) continue;
            db.execSQL("INSERT OR IGNORE INTO " + RESIDENT + "(version_id) VALUES(?)", new Object[]{row.version});
            db.execSQL("UPDATE " + RESIDENT + " SET " + column + "=" + column + "+? WHERE version_id=?",
                    new Object[]{row.bytes, row.version});
            bytes += row.bytes;
        }
        return bytes;
    }

    private static final class ResidentRow {
        final String version;
        final int index;
        final long bytes;

        ResidentRow(String version, int index, long bytes) {
            this.version = version;
            this.index = index;
            this.bytes = bytes;
        }
    }
}
