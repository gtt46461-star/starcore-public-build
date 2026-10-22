package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Read-only diagnosis restricted to the exact V50 strategy cohort. */
public final class V50CohortDiagnosisEngine {
    public static final class Report {
        public final int issueCount;
        public final int settledIssues;
        public final int realBets;
        public final int hits;
        public final int shadows;
        public final double hitRate;
        public final double coverage;
        public final double pnl;
        public final String detail;

        Report(
                int issueCount,
                int settledIssues,
                int realBets,
                int hits,
                int shadows,
                double hitRate,
                double coverage,
                double pnl,
                String detail) {
            this.issueCount = issueCount;
            this.settledIssues = settledIssues;
            this.realBets = realBets;
            this.hits = hits;
            this.shadows = shadows;
            this.hitRate = hitRate;
            this.coverage = coverage;
            this.pnl = pnl;
            this.detail = detail == null ? "" : detail;
        }
    }

    public Report diagnose(DbStore store) {
        if (store == null) return empty("V50_STORE_NULL");
        SQLiteDatabase db = store.readable();
        int issues =
                scalar(
                        db,
                        "SELECT COUNT(*) FROM "
                                + SchemaContractRegistry.TABLE_ISSUE
                                + " WHERE cohort_id=?",
                        new String[] {V50BuildManifest.COHORT_ID});
        int settled =
                scalar(
                        db,
                        "SELECT COUNT(*) FROM "
                                + SchemaContractRegistry.TABLE_ISSUE
                                + " WHERE cohort_id=? AND settled=1",
                        new String[] {V50BuildManifest.COHORT_ID});
        Cursor cursor = null;
        int bets = 0;
        int hits = 0;
        int shadows = 0;
        double pnl = 0.0d;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT "
                                    + "SUM(CASE WHEN real_trade=1 AND settled=1"
                                    + " THEN 1 ELSE 0 END),"
                                    + "SUM(CASE WHEN real_trade=1 AND settled=1"
                                    + " AND result=? THEN 1 ELSE 0 END),"
                                    + "SUM(CASE WHEN decision_action='SHADOW'"
                                    + " THEN 1 ELSE 0 END),"
                                    + "SUM(CASE WHEN real_trade=1 AND settled=1"
                                    + " THEN pnl ELSE 0 END)"
                                    + " FROM "
                                    + SchemaContractRegistry.TABLE_SELECTED
                                    + " WHERE strategy_cohort=?",
                            new String[] {AppConfig.RESULT_WIN, V50BuildManifest.COHORT_ID});
            if (cursor.moveToFirst()) {
                bets = cursor.isNull(0) ? 0 : cursor.getInt(0);
                hits = cursor.isNull(1) ? 0 : cursor.getInt(1);
                shadows = cursor.isNull(2) ? 0 : cursor.getInt(2);
                pnl = cursor.isNull(3) ? 0.0d : cursor.getDouble(3);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        double hitRate = bets <= 0 ? 0.0d : ((double) hits) / bets;
        double coverage = settled <= 0 ? 0.0d : ((double) bets) / settled;
        String detail =
                "cohort="
                        + V50BuildManifest.COHORT_ID
                        + "|issues="
                        + issues
                        + "|settled="
                        + settled
                        + "|bet="
                        + bets
                        + "|hit="
                        + hits
                        + "|shadow="
                        + shadows
                        + "|coverage="
                        + coverage
                        + "|hitRate="
                        + hitRate
                        + "|pnl="
                        + pnl;
        return new Report(issues, settled, bets, hits, shadows, hitRate, coverage, pnl, detail);
    }

    private static int scalar(SQLiteDatabase db, String sql, String[] args) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql, args);
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static Report empty(String detail) {
        return new Report(0, 0, 0, 0, 0, 0.0d, 0.0d, 0.0d, detail);
    }
}
