package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Read-only verifier for the V50 1/10/100 prediction ledgers. */
public final class LedgerReconciliationEngine {
    public static final class Report {
        public final String targetIssue;
        public final boolean valid;
        public final int issueCount;
        public final int numberEventCount;
        public final int positionActionCount;
        public final int selectedDecisionCount;
        public final int authorizationCount;
        public final V50Enums.FailureCode failureCode;
        public final String detail;

        Report(
                String targetIssue,
                boolean valid,
                int issueCount,
                int numberEventCount,
                int positionActionCount,
                int selectedDecisionCount,
                int authorizationCount,
                V50Enums.FailureCode failureCode,
                String detail) {
            this.targetIssue = targetIssue == null ? "" : targetIssue;
            this.valid = valid;
            this.issueCount = issueCount;
            this.numberEventCount = numberEventCount;
            this.positionActionCount = positionActionCount;
            this.selectedDecisionCount = selectedDecisionCount;
            this.authorizationCount = authorizationCount;
            this.failureCode = failureCode == null ? V50Enums.FailureCode.UNKNOWN : failureCode;
            this.detail = detail == null ? "" : detail;
        }
    }

    private LedgerReconciliationEngine() {}

    public static Report reconcile(SQLiteDatabase db, String targetIssue) {
        if (db == null || targetIssue == null || targetIssue.length() == 0) {
            return report(
                    targetIssue,
                    false,
                    0,
                    0,
                    0,
                    0,
                    0,
                    V50Enums.FailureCode.TARGET_ISSUE_MISMATCH,
                    "V50_RECONCILIATION_INPUT_INVALID");
        }
        int issue = count(db, SchemaContractRegistry.TABLE_ISSUE, targetIssue);
        int number = count(db, SchemaContractRegistry.TABLE_NUMBER, targetIssue);
        int action = count(db, SchemaContractRegistry.TABLE_ACTION, targetIssue);
        int selected = count(db, SchemaContractRegistry.TABLE_SELECTED, targetIssue);
        int auth = count(db, SchemaContractRegistry.TABLE_AUTH, targetIssue);

        if (issue != 1)
            return report(
                    targetIssue,
                    false,
                    issue,
                    number,
                    action,
                    selected,
                    auth,
                    V50Enums.FailureCode.PREDICTION_INSERT_FAILED,
                    "ISSUE_COUNT=" + issue);
        if (number != 100)
            return report(
                    targetIssue,
                    false,
                    issue,
                    number,
                    action,
                    selected,
                    auth,
                    V50Enums.FailureCode.NUMBER_EVENT_COUNT_NOT_100,
                    "NUMBER_EVENT_COUNT=" + number);
        if (action != 10)
            return report(
                    targetIssue,
                    false,
                    issue,
                    number,
                    action,
                    selected,
                    auth,
                    V50Enums.FailureCode.POSITION_ACTION_COUNT_NOT_10,
                    "POSITION_ACTION_COUNT=" + action);
        if (selected > 1)
            return report(
                    targetIssue,
                    false,
                    issue,
                    number,
                    action,
                    selected,
                    auth,
                    V50Enums.FailureCode.SELECTED_DECISION_COUNT_GT_1,
                    "SELECTED_COUNT=" + selected);
        if (selected != auth)
            return report(
                    targetIssue,
                    false,
                    issue,
                    number,
                    action,
                    selected,
                    auth,
                    V50Enums.FailureCode.AUTH_CHAIN_INCOMPLETE,
                    "AUTH_COUNT=" + auth);
        return report(
                targetIssue,
                true,
                issue,
                number,
                action,
                selected,
                auth,
                V50Enums.FailureCode.NONE,
                "V50_LEDGER_OK");
    }

    private static Report report(
            String issue,
            boolean valid,
            int issueCount,
            int numberCount,
            int actionCount,
            int selectedCount,
            int authCount,
            V50Enums.FailureCode code,
            String detail) {
        return new Report(
                issue,
                valid,
                issueCount,
                numberCount,
                actionCount,
                selectedCount,
                authCount,
                code,
                detail);
    }

    private static int count(SQLiteDatabase db, String table, String targetIssue) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT COUNT(*) FROM " + table + " WHERE target_issue=?",
                            new String[] {targetIssue});
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }
}
