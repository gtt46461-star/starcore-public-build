package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public final class LearningCursorR952 {
    private LearningCursorR952() {}
    public static String artifact(SQLiteDatabase db, String consumer) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT artifact_hash FROM r952_learning_cursor WHERE consumer_id=?", new String[] {consumer});
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        } finally { if (c != null) c.close(); }
    }
    public static void advance(SQLiteDatabase db, String consumer, String issue,
            String artifactHash, String deliveryHash) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT trained_until_issue,last_delivery_hash FROM r952_learning_cursor WHERE consumer_id=?", new String[] {consumer});
            if (c.moveToFirst()) {
                String oldIssue = safe(c.getString(0));
                String oldDelivery = safe(c.getString(1));
                if (deliveryHash.equals(oldDelivery)) return;
                if (oldIssue.length() > 0 && issue.compareTo(oldIssue) <= 0)
                    throw new IllegalStateException("R952_TRAIN_CURSOR_NON_MONOTONIC");
            }
        } finally { if (c != null) c.close(); }
        db.execSQL("INSERT OR REPLACE INTO r952_learning_cursor(consumer_id,trained_until_issue,artifact_hash,last_delivery_hash,updated_at) VALUES(?,?,?,?,?)",
                new Object[] {consumer, issue, artifactHash, deliveryHash, Long.valueOf(System.currentTimeMillis())});
    }
    private static String safe(String v) { return v == null ? "" : v; }
}
