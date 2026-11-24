package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.LinkedHashMap;
import java.util.Map;

/** Recovery observations are tied to the attempt fence and grant no model or certificate authority. */
final class RecoveryLedgerR341 {
    static final String TABLE = "r341_checkpoint_recovery_observation";
    static final String DDL = "CREATE TABLE IF NOT EXISTS " + TABLE + "(expected_hash TEXT NOT NULL,"
            + "attempt_id INTEGER NOT NULL,owner_identity TEXT NOT NULL,status TEXT NOT NULL,"
            + "source_location TEXT NOT NULL,source_evidence TEXT NOT NULL,observed_hash TEXT,observed_bytes INTEGER,"
            + "reason TEXT NOT NULL,observed_at INTEGER NOT NULL,retry_not_before INTEGER,"
            + "PRIMARY KEY(expected_hash,attempt_id,status))";

    static void record(SQLiteDatabase db, String hash, long attempt, String owner, String state,
            String location, String source, String observedHash, Long bytes, String reason, long at) {
        if (!db.inTransaction()) throw new IllegalStateException("R341_RECOVERY_OBSERVATION_OWNER");
        String status;
        switch (state) {
            case "BYTES_RESTORED_NOT_MODEL_ACCEPTED": status = "VERIFIED"; break;
            case "HASH_OR_SOURCE_INVALID_PRESERVED": status = "MISMATCH"; break;
            case "WAIT_SOURCE": status = "NO_EXACT_BYTES_IN_HOT_COLD_OR_RECOVERY_INTAKE; OLD_IDENTITY_PRESERVED".equals(reason)
                    ? "MISSING" : "SOURCE_UNREADABLE"; break;
            default: status = "AVAILABLE_UNVERIFIED";
        }
        try (Cursor latest = db.rawQuery("SELECT status,source_location,source_evidence,observed_hash,observed_bytes,reason FROM "
                + TABLE + " WHERE expected_hash=? ORDER BY attempt_id DESC LIMIT 1", new String[]{hash})) {
            if (latest.moveToFirst() && status.equals(latest.getString(0)) && location.equals(latest.getString(1))
                    && source.equals(latest.getString(2)) && java.util.Objects.equals(observedHash, latest.getString(3))
                    && java.util.Objects.equals(bytes, latest.isNull(4) ? null : latest.getLong(4))
                    && reason.equals(latest.getString(5))) return;
        }
        Long retry = "WAIT_SOURCE".equals(state) ? at + 15L * 60_000L : null;
        db.execSQL("INSERT OR IGNORE INTO " + TABLE + " VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                new Object[]{hash, attempt, owner, status, location, source, observedHash, bytes, reason, at, retry});
    }

    static Map<String,Object> counts(SQLiteDatabase db) {
        Map<String,Object> counts = new LinkedHashMap<>();
        try (Cursor rows = db.rawQuery("SELECT status,count(*) FROM " + TABLE + " GROUP BY status ORDER BY status", null)) {
            while (rows.moveToNext()) counts.put(rows.getString(0), rows.getLong(1));
        }
        return counts;
    }

    private RecoveryLedgerR341() {}
}
