package com.ethk3.novaonepro.r952;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.V51ChecksumEngine;

public final class RuntimeRecoveryJournalR952 {
    private RuntimeRecoveryJournalR952() {}
    public static void record(SQLiteDatabase db, String delivery, int attempt, String state, String error) {
        String event = V51ChecksumEngine.sha256("RECOVERY|" + delivery + '|' + attempt + '|' + state + '|' + safe(error));
        db.execSQL("INSERT OR IGNORE INTO r952_recovery_journal(delivery_hash,attempt,state,error_code,event_hash,created_at) VALUES(?,?,?,?,?,?)",
                new Object[] {delivery, Integer.valueOf(attempt), state, safe(error), event, Long.valueOf(System.currentTimeMillis())});
    }
    private static String safe(String v) { return v == null ? "" : (v.length() > 500 ? v.substring(0, 500) : v); }
}
