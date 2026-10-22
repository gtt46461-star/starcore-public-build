package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Pure V51 cohort summary; legacy decisions are impossible to enter this query. */
public final class V51CohortDiagnosisEngine {
    public static final class Summary {
        public final int predictions, selected, realBet, settled, hits;

        Summary(int p, int s, int b, int z, int h) {
            predictions = p;
            selected = s;
            realBet = b;
            settled = z;
            hits = h;
        }
    }

    public Summary summarize(DbStore store) {
        if (store == null) return new Summary(0, 0, 0, 0, 0);
        SQLiteDatabase db = store.readable();
        V51Repository.ensure(db);
        int predictions =
                scalar(
                        db,
                        "SELECT COUNT(1) FROM " + V51Repository.ISSUE + " WHERE cohort_id=?",
                        new String[] {V51BuildManifest.COHORT_ID});
        int selected = scalar(db, "SELECT COUNT(1) FROM " + V51Repository.SELECTED, new String[0]);
        int real =
                scalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + V51Repository.SELECTED
                                + " WHERE decision_action='BET'",
                        new String[0]);
        int settled =
                scalar(
                        db,
                        "SELECT COUNT(1) FROM " + V51Repository.SELECTED + " WHERE settled=1",
                        new String[0]);
        int hits =
                scalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + V51Repository.SELECTED
                                + " WHERE settled=1 AND result='WIN'",
                        new String[0]);
        return new Summary(predictions, selected, real, settled, hits);
    }

    private static int scalar(SQLiteDatabase db, String sql, String[] args) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }
}
