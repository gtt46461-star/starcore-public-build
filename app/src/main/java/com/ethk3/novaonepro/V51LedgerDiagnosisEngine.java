package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Read-only 1/10/100/permutation ledger diagnosis. */
public final class V51LedgerDiagnosisEngine {
    public static final class Report {
        public final int issue, number, action, permutation, selected;
        public final boolean complete;

        Report(int i, int n, int a, int p, int s) {
            issue = i;
            number = n;
            action = a;
            permutation = p;
            selected = s;
            complete = i == 1 && n == 100 && a == 10 && p == 1 && (s == 0 || s == 1);
        }
    }

    public Report inspect(DbStore store, String targetIssue) {
        if (store == null) return new Report(0, 0, 0, 0, 0);
        SQLiteDatabase db = store.readable();
        V51Repository.ensure(db);
        return new Report(
                count(db, V51Repository.ISSUE, targetIssue),
                count(db, V51Repository.NUMBER, targetIssue),
                count(db, V51Repository.ACTION, targetIssue),
                count(db, V51Repository.PERMUTATION, targetIssue),
                count(db, V51Repository.SELECTED, targetIssue));
    }

    private static int count(SQLiteDatabase db, String table, String issue) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT COUNT(1) FROM " + table + " WHERE target_issue=?",
                            new String[] {issue == null ? "" : issue});
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }
}
