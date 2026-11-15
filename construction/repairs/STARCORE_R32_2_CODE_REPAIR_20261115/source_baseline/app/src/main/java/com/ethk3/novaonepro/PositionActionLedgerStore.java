package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;

import java.util.List;

/** Exactly ten pre-draw position Top3 actions per target issue. */
public final class PositionActionLedgerStore {
    private PositionActionLedgerStore() {}

    public static V50Repository.WriteResult freeze(
            SQLiteDatabase db, String targetIssue, List<V50Models.PositionAction> actions) {
        return V50Repository.insertPositionActions(db, targetIssue, actions);
    }
}
