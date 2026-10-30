package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Derivative post-commit bridge.  It mirrors an already committed Production settlement into
 * R906 closure/outbox evidence.  Failure is recorded but can NEVER roll back OOS/task terminal.
 */
public final class SettlementPostCommitSidecarR909 {
    private SettlementPostCommitSidecarR909() {}

    public static String reconcileIssue(final DbStore store, final String targetIssue) {
        if (store == null || !TargetIssueResolver.isValid(targetIssue)) return "INVALID";
        if(com.ethk3.novaonepro.strategyv2.LegacyResearchScopeR29.rawRealtime(store).inTransaction())return "CORE_OWNER_TRANSACTION_NOT_COMMITTED";
        try(com.ethk3.novaonepro.strategyv2.LegacyResearchScopeR29.Scope scope=com.ethk3.novaonepro.strategyv2.LegacyResearchScopeR29.enter(store)){
            return reconcileResearchIssue(store,targetIssue);
        }
    }
    private static String reconcileResearchIssue(final DbStore store,final String targetIssue){
        final SQLiteDatabase realtime=com.ethk3.novaonepro.strategyv2.LegacyResearchScopeR29.realtime(store.readable());
        final TerminalProjectionR29.Snapshot captured=TerminalProjectionR29.capture(realtime,targetIssue);
        if(captured==null)return "CORE_TERMINAL_EVIDENCE_PENDING";
        final Models.Draw draw=captured.draw;
        com.ethk3.novaonepro.strategyv2.LegacyResearchScopeR29.realtimeWrite(store,"R29_SELECTED_TERMINAL_PROJECTION",db->TerminalProjectionR29.projectSelected(db,captured));
        String out = store.coordinatorR36().runWrite(
                "R909_SETTLEMENT_POST_COMMIT_SIDECAR",
                new DatabaseCoordinator.WriteCallable<String>() {
                    @Override public String call(SQLiteDatabase db) {
                        boolean owns = !db.inTransaction();
                        if (owns) db.beginTransaction();
                        try {
                            // Captured from the original realtime authority before either writer was held.
                            // Formal shadow/OOS/task rows are inputs, never manufactured in the research mirror.
                            IssueClosureContractR906.Receipt receipt=captured.receipt;
                            String drawHash=receipt.drawHash,predrawHash=receipt.predrawHash,oosHash=receipt.oosHash;
                            int actual=receipt.actualNumber,hit=receipt.hitLabel;long completedAt=receipt.completedAt;
                            IssueClosureContractR906.Eligibility eligibility=receipt.eligibility;
                            IssueClosureContractR906.CoreStatus status=receipt.status;

                            ContentValues cv = new ContentValues();
                            cv.put("target_issue", targetIssue);
                            cv.put("draw_hash", drawHash);
                            cv.put("predraw_record_hash", predrawHash);
                            cv.put("eligibility", eligibility.name());
                            cv.put("core_status", status.name());
                            cv.put("actual_number", actual);
                            cv.put("hit_label", hit);
                            cv.put("oos_record_hash", oosHash);
                            cv.put("real_record_hash", "");
                            cv.put("settlement_hash", receipt.settlementHash);
                            cv.put("previous_task_status", captured.originalStatus);
                            cv.put("exclusion_or_error_code", captured.errorCode);
                            cv.put("classified_at", System.currentTimeMillis());
                            cv.put("completed_at", completedAt);
                            cv.put("record_hash", receipt.recordHash);
                            db.insertWithOnConflict(
                                    DbHelper.TABLE_R906_ISSUE_CLOSURE, null, cv,
                                    SQLiteDatabase.CONFLICT_REPLACE);

                            if (receipt.countsAsSettledPrediction()) {
                                SettlementOutboxR906.enqueueWithinTransaction(
                                        db, "RESEARCH_COMPACT_LABEL", targetIssue, "PRODUCTION",
                                        oosHash, 80, System.currentTimeMillis());
                                SettlementOutboxR906.enqueueWithinTransaction(
                                        db, "DEFERRED_ACTION_EVIDENCE", targetIssue, "1200_ACTION_SPACE",
                                        receipt.settlementHash, 10, System.currentTimeMillis());
                            }
                            if (owns) db.setTransactionSuccessful();
                            return "OK:" + status.name();
                        } finally {
                            if (owns && db.inTransaction()) db.endTransaction();
                        }
                    }
                }, "WRITE_FAILED");
        try {
            store.setState("r909_settlement_sidecar_issue", targetIssue);
            store.setState("r909_settlement_sidecar_status", safe(out));
            store.setState("r909_settlement_sidecar_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable ignore) {}
        // Derivative Research fanout runs only after Production has committed. Failure remains
        // fail-open and can be recovered by ResearchPredrawSettlementPumpR908.
        EssentialSettlementFeedbackR923.Result essential =
                EssentialSettlementFeedbackR923.settleNow(store, draw);
        MissStreakEscalationR923.Result missEscalation =
                MissStreakEscalationR923.analyze(store);
        try {
            store.setState("r923_feedback_before_heavy_fanout", essential.canonical());
            store.setState("r923_miss_escalation_before_heavy_fanout",
                    missEscalation.summary());
        } catch (Throwable ignored) {}
        try {
            int algorithms = AlgorithmInventionLabR908.settleDraw(store, draw);
            store.setState("r919_algorithm_post_commit_rows", String.valueOf(algorithms));
            store.setState("r919_algorithm_post_commit_issue", targetIssue);
            store.setState("r919_algorithm_post_commit_status", "OK");
        } catch (Throwable algorithmFailure) {
            try {
                store.setState("r919_algorithm_post_commit_status",
                        "NON_FATAL_" + algorithmFailure.getClass().getSimpleName());
                store.setState("r919_algorithm_post_commit_issue", targetIssue);
            } catch (Throwable ignored) {}
        }
        try {
            int assets = AssetGenesisEngineR915.settleDraw(store, draw);
            store.setState("r915_genesis_post_commit_rows", String.valueOf(assets));
            store.setState("r915_genesis_post_commit_issue", targetIssue);
            store.setState("r915_genesis_post_commit_status", "OK");
        } catch (Throwable assetFailure) {
            try { store.setState("r915_genesis_post_commit_status",
                    "NON_FATAL_" + assetFailure.getClass().getSimpleName()); }
            catch (Throwable ignored) {}
        }
        try {
            int formulas = ResearchShadowChallengerRuntimeR817.settle(store, draw);
            store.setState("r919_formula_post_commit_rows", String.valueOf(formulas));
            store.setState("r919_formula_post_commit_issue", targetIssue);
            store.setState("r919_formula_post_commit_status", "OK");
        } catch (Throwable formulaFailure) {
            try {
                store.setState("r919_formula_post_commit_status",
                        "NON_FATAL_" + formulaFailure.getClass().getSimpleName());
                store.setState("r919_formula_post_commit_issue", targetIssue);
            } catch (Throwable ignored) {}
        }
        try {
            store.setState("r919_teacher_post_commit_rows", String.valueOf(essential.rows));
            store.setState("r919_teacher_post_commit_issue", targetIssue);
            store.setState("r919_teacher_post_commit_status", essential.status);
        } catch (Throwable ignored) {}
        return safe(out);
    }

    public static int drain(DbStore store, int limit, long budgetMs) {
        if (store == null || RealtimePressureGateR811.hasActiveCriticalWork()) return 0;
        int bound = Math.max(1, Math.min(32, limit));
        long deadline = System.currentTimeMillis() + Math.max(100L, Math.min(2500L, budgetMs));
        java.util.ArrayList<String> issues = new java.util.ArrayList<String>();
        Cursor c = null;
        try {
            c = store.readable().rawQuery(pendingIssuesSql(store.readable()),
                    new String[] {SettlementTaskStoreR36.COMPLETED,
                            SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL,
                            SettlementTaskStoreR36.FAILED_TERMINAL,
                            SettlementTaskStoreR36.INVALID_DRAW_TERMINAL,
                            SettlementTaskStoreR36.COMPLETED,
                            String.valueOf(bound)});
            while (c.moveToNext()) issues.add(safe(c.getString(0)));
        } catch (Throwable error) {
            try { store.setState("r909_settlement_sidecar_scan_error", error.getClass().getSimpleName()); }
            catch (Throwable ignore) {}
            return 0;
        } finally { if (c != null) c.close(); }

        int done = 0;
        for (int i = 0; i < issues.size() && System.currentTimeMillis() < deadline; i++) {
            String status = reconcileIssue(store, issues.get(i));
            if (status.startsWith("OK:")) done++;
            if (RealtimePressureGateR811.hasActiveCriticalWork()) break;
        }
        try {
            store.setState("r909_settlement_sidecar_drained", String.valueOf(done));
            store.setState("r909_settlement_sidecar_pending_scan", String.valueOf(issues.size()));
        } catch (Throwable ignore) {}
        return done;
    }

    static String pendingIssuesSql(SQLiteDatabase db) {
        return "SELECT t.target_issue FROM " + DbHelper.TABLE_R36_SETTLEMENT_TASK
                            + " t LEFT JOIN " + DbHelper.TABLE_R906_ISSUE_CLOSURE
                            + " c ON c.target_issue=t.target_issue"
                            + " WHERE t.status IN (?,?,?,?)"
                            + " AND ((c.target_issue IS NULL OR c.settlement_hash<>t.settlement_hash)"
                            + " OR (t.status=? AND "
                            + LegacyPredrawContextBridgeR915.missingPostRowPredicate(db) + "))"
                            + " ORDER BY t.priority DESC,t.completed_at DESC LIMIT ?";
    }

    private static String safe(String x) { return x == null ? "" : x.trim(); }
}
