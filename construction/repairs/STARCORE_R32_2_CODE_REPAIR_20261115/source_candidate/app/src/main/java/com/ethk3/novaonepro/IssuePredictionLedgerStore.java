package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;

import java.util.List;

/** One immutable issue-level prediction row per target issue. */
public final class IssuePredictionLedgerStore {
    private IssuePredictionLedgerStore() {}

    public static V50Repository.WriteResult freeze(
            SQLiteDatabase db,
            V50Models.PredictionContext context,
            String featureHash,
            String stateHash,
            String expertHash,
            double[][] numberProbability,
            List<V50Models.PositionAction> actions,
            V50Models.SelectedActionEnvelope selected,
            String modelHash,
            String hypothesisBlob) {
        return V50Repository.insertIssuePrediction(
                db,
                context,
                featureHash,
                stateHash,
                expertHash,
                numberProbability,
                actions,
                selected,
                modelHash,
                hypothesisBlob);
    }
}
