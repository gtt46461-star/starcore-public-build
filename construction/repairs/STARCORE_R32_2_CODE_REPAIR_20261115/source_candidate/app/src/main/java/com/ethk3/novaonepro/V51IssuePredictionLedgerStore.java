package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;

/** Issue-level immutable ledger facade. */
public final class V51IssuePredictionLedgerStore {
    public void ensure(DbStore store) {
        V51Repository.ensure(store);
    }

    public boolean exists(DbStore store, String targetIssue) {
        SQLiteDatabase db = store.readable();
        V51Repository.ensure(db);
        android.database.Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT 1 FROM " + V51Repository.ISSUE + " WHERE target_issue=?",
                            new String[] {targetIssue});
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }
}
