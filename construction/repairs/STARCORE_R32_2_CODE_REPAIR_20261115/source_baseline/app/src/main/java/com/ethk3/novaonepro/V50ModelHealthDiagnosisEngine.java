package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Pure-V50 rank, selected-probability calibration and model-gate report. */
public final class V50ModelHealthDiagnosisEngine {
    public static final class Report {
        public final int selectedSample;
        public final double selectedHitRate;
        public final double ece;
        public final double rank1;
        public final double rank2;
        public final double rank3;
        public final boolean rankOrder;
        public final String releaseState;
        public final String detail;

        Report(
                int selectedSample,
                double selectedHitRate,
                double ece,
                double rank1,
                double rank2,
                double rank3,
                boolean rankOrder,
                String releaseState,
                String detail) {
            this.selectedSample = selectedSample;
            this.selectedHitRate = selectedHitRate;
            this.ece = ece;
            this.rank1 = rank1;
            this.rank2 = rank2;
            this.rank3 = rank3;
            this.rankOrder = rankOrder;
            this.releaseState = releaseState;
            this.detail = detail == null ? "" : detail;
        }
    }

    public Report diagnose(DbStore store) {
        if (store == null) {
            return new Report(
                    0,
                    0.0d,
                    0.0d,
                    0.0d,
                    0.0d,
                    0.0d,
                    false,
                    V50BuildManifest.RELEASE_STATE,
                    "V50_STORE_NULL");
        }
        SQLiteDatabase db = store.readable();
        double[] selected = selectedMetrics(db);
        double[] ranks = rankMetrics(db);
        boolean order = ranks[0] > ranks[1] && ranks[0] > ranks[2];
        String detail =
                "sample="
                        + (int) selected[0]
                        + "|hitRate="
                        + selected[1]
                        + "|ece="
                        + selected[2]
                        + "|rank1="
                        + ranks[0]
                        + "|rank2="
                        + ranks[1]
                        + "|rank3="
                        + ranks[2]
                        + "|rankOrder="
                        + order
                        + "|strictOosGate="
                        + V50SelectivePolicyArtifact.MODEL_GATE_PASSED;
        return new Report(
                (int) selected[0],
                selected[1],
                selected[2],
                ranks[0],
                ranks[1],
                ranks[2],
                order,
                V50BuildManifest.RELEASE_STATE,
                detail);
    }

    private static double[] selectedMetrics(SQLiteDatabase db) {
        Cursor cursor = null;
        int sample = 0;
        int hits = 0;
        double calibrationSum = 0.0d;
        try {
            cursor =
                    db.query(
                            SchemaContractRegistry.TABLE_SELECTED,
                            new String[] {"selected_hit_p", "result"},
                            "strategy_cohort=? AND settled=1",
                            new String[] {V50BuildManifest.COHORT_ID},
                            null,
                            null,
                            null);
            while (cursor.moveToNext()) {
                int hit = AppConfig.isWinResult(cursor.getString(1)) ? 1 : 0;
                sample++;
                hits += hit;
                calibrationSum += Math.abs(cursor.getDouble(0) - hit);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return new double[] {sample, sample == 0 ? 0.0d : ((double) hits) / sample, binnedEce(db)};
    }

    private static double binnedEce(SQLiteDatabase db) {
        double value = 0.0d;
        int total =
                scalar(
                        db,
                        "SELECT COUNT(*) FROM "
                                + SchemaContractRegistry.TABLE_SELECTED
                                + " WHERE strategy_cohort=? AND settled=1",
                        new String[] {V50BuildManifest.COHORT_ID});
        if (total == 0) return 0.0d;
        for (int bin = 0; bin < 10; bin++) {
            double low = bin / 10.0d;
            double high = (bin + 1) / 10.0d;
            Cursor cursor = null;
            try {
                cursor =
                        db.rawQuery(
                                "SELECT COUNT(*),"
                                        + "AVG(selected_hit_p),"
                                        + "AVG(CASE WHEN result=? THEN 1.0 ELSE 0.0 END)"
                                        + " FROM "
                                        + SchemaContractRegistry.TABLE_SELECTED
                                        + " WHERE strategy_cohort=? AND settled=1"
                                        + " AND selected_hit_p>=?"
                                        + (bin == 9
                                                ? " AND selected_hit_p<=?"
                                                : " AND selected_hit_p<?"),
                                new String[] {
                                    AppConfig.RESULT_WIN,
                                    V50BuildManifest.COHORT_ID,
                                    String.valueOf(low),
                                    String.valueOf(high)
                                });
                if (cursor.moveToFirst() && cursor.getInt(0) > 0) {
                    int count = cursor.getInt(0);
                    value +=
                            ((double) count)
                                    / total
                                    * Math.abs(cursor.getDouble(1) - cursor.getDouble(2));
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        return value;
    }

    private static double[] rankMetrics(SQLiteDatabase db) {
        double[] out = new double[3];
        for (int rank = 1; rank <= 3; rank++) {
            Cursor cursor = null;
            try {
                cursor =
                        db.rawQuery(
                                "SELECT AVG(actual_hit) FROM "
                                        + SchemaContractRegistry.TABLE_ACTION
                                        + " WHERE settled=1 AND action_rank=?",
                                new String[] {String.valueOf(rank)});
                if (cursor.moveToFirst() && !cursor.isNull(0)) {
                    out[rank - 1] = cursor.getDouble(0);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        return out;
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
}
