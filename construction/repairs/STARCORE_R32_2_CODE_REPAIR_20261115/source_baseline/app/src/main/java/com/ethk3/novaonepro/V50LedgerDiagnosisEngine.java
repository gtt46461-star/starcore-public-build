package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Read-only 1/10/100 and target-issue reconciliation diagnosis. */
public final class V50LedgerDiagnosisEngine {
    public static final class Report {
        public final boolean valid;
        public final int issueCount;
        public final int invalidIssueCount;
        public final int unresolvedErrors;
        public final String detail;

        Report(
                boolean valid,
                int issueCount,
                int invalidIssueCount,
                int unresolvedErrors,
                String detail) {
            this.valid = valid;
            this.issueCount = issueCount;
            this.invalidIssueCount = invalidIssueCount;
            this.unresolvedErrors = unresolvedErrors;
            this.detail = detail == null ? "" : detail;
        }
    }

    public Report diagnose(DbStore store, int limit) {
        if (store == null) {
            return new Report(false, 0, 0, 0, "V50_STORE_NULL");
        }
        SQLiteDatabase db = store.readable();
        Cursor cursor = null;
        int issues = 0;
        int invalid = 0;
        StringBuilder detail = new StringBuilder();
        try {
            cursor =
                    db.query(
                            SchemaContractRegistry.TABLE_ISSUE,
                            new String[] {"target_issue"},
                            "cohort_id=?",
                            new String[] {V50BuildManifest.COHORT_ID},
                            null,
                            null,
                            "target_issue DESC",
                            String.valueOf(Math.max(1, limit)));
            while (cursor.moveToNext()) {
                String issue = cursor.getString(0);
                issues++;
                LedgerReconciliationEngine.Report report =
                        LedgerReconciliationEngine.reconcile(db, issue);
                if (!report.valid) {
                    invalid++;
                    if (detail.length() > 0) detail.append('\n');
                    detail.append(issue)
                            .append('|')
                            .append(report.failureCode)
                            .append('|')
                            .append(report.detail);
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        int errors =
                scalar(
                        db,
                        "SELECT COUNT(*) FROM "
                                + SchemaContractRegistry.TABLE_LEDGER_ERROR
                                + " WHERE resolved=0");
        if (detail.length() == 0) detail.append("V50_LEDGER_OK");
        detail.append("|checked=")
                .append(issues)
                .append("|invalid=")
                .append(invalid)
                .append("|unresolvedErrors=")
                .append(errors);
        return new Report(invalid == 0 && errors == 0, issues, invalid, errors, detail.toString());
    }

    private static int scalar(SQLiteDatabase db, String sql) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql, null);
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }
}
