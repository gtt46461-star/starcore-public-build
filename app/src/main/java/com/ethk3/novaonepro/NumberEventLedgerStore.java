package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;

/** Exactly 100 position-number prediction rows per target issue. */
public final class NumberEventLedgerStore {
    private NumberEventLedgerStore() {}

    public static V50Repository.WriteResult freeze(
            SQLiteDatabase db,
            String targetIssue,
            double[][] numberProbability,
            int[][] numberRanks,
            String[][] expertContribution,
            String[][] teacherContribution,
            String[] stateDomain,
            String featureHash) {
        return V50Repository.insertNumberEvents(
                db,
                targetIssue,
                numberProbability,
                numberRanks,
                expertContribution,
                teacherContribution,
                stateDomain,
                featureHash);
    }
}
