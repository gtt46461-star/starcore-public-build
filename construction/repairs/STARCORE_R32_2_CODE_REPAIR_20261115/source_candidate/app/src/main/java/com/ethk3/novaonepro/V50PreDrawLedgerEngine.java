package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;

import java.util.List;

/**
 * Atomically freezes the issue snapshot, 100 number events and ten position actions before the
 * target draw. It never authorizes or creates a final action.
 */
public final class V50PreDrawLedgerEngine {
    public static final class FreezeResult {
        public final boolean success;
        public final boolean idempotent;
        public final String targetIssue;
        public final V50Enums.FailureCode failureCode;
        public final String detail;
        public final LedgerReconciliationEngine.Report reconciliation;

        FreezeResult(
                boolean success,
                boolean idempotent,
                String targetIssue,
                V50Enums.FailureCode failureCode,
                String detail,
                LedgerReconciliationEngine.Report reconciliation) {
            this.success = success;
            this.idempotent = idempotent;
            this.targetIssue = targetIssue == null ? "" : targetIssue;
            this.failureCode = failureCode == null ? V50Enums.FailureCode.UNKNOWN : failureCode;
            this.detail = detail == null ? "" : detail;
            this.reconciliation = reconciliation;
        }
    }

    public FreezeResult freeze(
            final DbStore store,
            final V50Models.PredictionContext context,
            final String featureHash,
            final String stateHash,
            final String expertHash,
            final double[][] numberProbability,
            final int[][] numberRanks,
            final String[][] expertContribution,
            final String[][] teacherContribution,
            final String[] stateDomain,
            final List<V50Models.PositionAction> actions,
            final V50Models.SelectedActionEnvelope selected,
            final String modelHash,
            final String hypothesisBlob) {
        if (store == null || context == null || selected == null) {
            return failed(
                    context == null ? "" : context.targetIssue,
                    V50Enums.FailureCode.PREDICTION_INSERT_FAILED,
                    "V50_PREDRAW_INPUT_NULL",
                    null);
        }
        TargetIssueResolver.Resolution issue =
                TargetIssueResolver.exact(context.baseIssue, context.targetIssue);
        if (!issue.valid) {
            return failed(context.targetIssue, issue.failureCode, issue.detail, null);
        }
        V50PredictionContract.Result contract = V50PredictionContract.validate(selected);
        if (!contract.valid) {
            return failed(
                    context.targetIssue,
                    V50Enums.FailureCode.AUTH_CHAIN_INCOMPLETE,
                    contract.detail,
                    null);
        }
        V50Repository.ReadyReport ready = V50Repository.ensure(store);
        if (!ready.ready) {
            return failed(
                    context.targetIssue,
                    V50Enums.FailureCode.MIGRATION_FAILED,
                    ready.integrity == null ? "V50_DATABASE_NOT_READY" : ready.integrity.detail,
                    null);
        }

        try {
            FreezeAccumulator result =
                    V50Repository.transaction(
                            store,
                            new V50Repository.TransactionBody<FreezeAccumulator>() {
                                @Override
                                public FreezeAccumulator run(SQLiteDatabase db) throws Exception {
                                    V50Repository.WriteResult issueResult =
                                            IssuePredictionLedgerStore.freeze(
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
                                    require(issueResult);
                                    V50Repository.WriteResult numberResult =
                                            NumberEventLedgerStore.freeze(
                                                    db,
                                                    context.targetIssue,
                                                    numberProbability,
                                                    numberRanks,
                                                    expertContribution,
                                                    teacherContribution,
                                                    stateDomain,
                                                    featureHash);
                                    require(numberResult);
                                    V50Repository.WriteResult actionResult =
                                            PositionActionLedgerStore.freeze(
                                                    db, context.targetIssue, actions);
                                    require(actionResult);
                                    return new FreezeAccumulator(
                                            issueResult.idempotent
                                                    && numberResult.idempotent
                                                    && actionResult.idempotent,
                                            issueResult.affectedRows
                                                    + numberResult.affectedRows
                                                    + actionResult.affectedRows);
                                }
                            });
            LedgerReconciliationEngine.Report reconciliation =
                    LedgerReconciliationEngine.reconcile(store.readable(), context.targetIssue);
            if (!reconciliation.valid) {
                return failed(
                        context.targetIssue,
                        reconciliation.failureCode,
                        reconciliation.detail,
                        reconciliation);
            }
            return new FreezeResult(
                    true,
                    result.idempotent,
                    context.targetIssue,
                    V50Enums.FailureCode.NONE,
                    "V50_PREDRAW_FROZEN|rows=" + result.rows,
                    reconciliation);
        } catch (LedgerWriteFailure failure) {
            V50Repository.recordLedgerError(
                    store.writable(),
                    context.targetIssue,
                    failure.code,
                    "V50PreDrawLedgerEngine",
                    failure.getMessage());
            return failed(context.targetIssue, failure.code, failure.getMessage(), null);
        } catch (Throwable error) {
            String detail = error.getClass().getSimpleName() + ':' + safe(error.getMessage());
            V50Repository.recordLedgerError(
                    store.writable(),
                    context.targetIssue,
                    V50Enums.FailureCode.SQLITE_ERROR,
                    "V50PreDrawLedgerEngine",
                    detail);
            return failed(context.targetIssue, V50Enums.FailureCode.SQLITE_ERROR, detail, null);
        }
    }

    private static void require(V50Repository.WriteResult result) throws LedgerWriteFailure {
        if (result == null || !result.success) {
            throw new LedgerWriteFailure(
                    result == null ? V50Enums.FailureCode.UNKNOWN : result.failureCode,
                    result == null ? "V50_LEDGER_WRITE_NULL" : result.detail);
        }
    }

    private static FreezeResult failed(
            String issue,
            V50Enums.FailureCode code,
            String detail,
            LedgerReconciliationEngine.Report reconciliation) {
        return new FreezeResult(false, false, issue, code, detail, reconciliation);
    }

    private static final class FreezeAccumulator {
        final boolean idempotent;
        final int rows;

        FreezeAccumulator(boolean idempotent, int rows) {
            this.idempotent = idempotent;
            this.rows = rows;
        }
    }

    private static final class LedgerWriteFailure extends Exception {
        final V50Enums.FailureCode code;

        LedgerWriteFailure(V50Enums.FailureCode code, String detail) {
            super(detail);
            this.code = code == null ? V50Enums.FailureCode.UNKNOWN : code;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
