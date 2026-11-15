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
        final Models.Draw draw = store.getDrawByIssue(targetIssue);
        if (draw == null || !draw.isValid()) return "DRAW_MISSING";
        String out = store.coordinatorR36().runWrite(
                "R909_SETTLEMENT_POST_COMMIT_SIDECAR",
                new DatabaseCoordinator.WriteCallable<String>() {
                    @Override public String call(SQLiteDatabase db) {
                        boolean owns = !db.inTransaction();
                        if (owns) db.beginTransaction();
                        try {
                            CoreRow core = readCore(db, targetIssue);
                            if (core == null || !core.terminal()) return "CORE_NOT_TERMINAL";

                            String drawHash = DrawHashR36.sha256V2(draw);
                            String predrawHash = "";
                            String oosHash = "";
                            int actual = 0;
                            int hit = -1;
                            IssueClosureContractR906.Eligibility eligibility;
                            IssueClosureContractR906.CoreStatus status;

                            if (SettlementTaskStoreR36.COMPLETED.equals(core.status)) {
                                EvidenceRow evidence = readEvidence(db, targetIssue);
                                if (evidence == null || !evidence.complete())
                                    return "CORE_COMPLETED_EVIDENCE_MISSING";
                                predrawHash = evidence.predrawHash;
                                oosHash = evidence.oosHash;
                                actual = evidence.actual;
                                hit = evidence.hit;
                                eligibility = IssueClosureContractR906.Eligibility.ELIGIBLE_PREDRAW;
                                status = IssueClosureContractR906.CoreStatus.COMPLETED;
                            } else if (SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL.equals(core.status)) {
                                eligibility = IssueClosureContractR906.Eligibility.EXCLUDED_NO_PREDRAW;
                                status = IssueClosureContractR906.CoreStatus.EXCLUDED;
                            } else {
                                eligibility = IssueClosureContractR906.Eligibility.BLOCKED_DATA_ERROR;
                                status = IssueClosureContractR906.CoreStatus.BLOCKED;
                            }

                            long completedAt = core.completedAt > 0L ? core.completedAt : System.currentTimeMillis();
                            IssueClosureContractR906.Receipt receipt = new IssueClosureContractR906.Receipt(
                                    targetIssue, drawHash, predrawHash, oosHash, core.settlementHash,
                                    eligibility, status, actual, hit, completedAt);

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
                            cv.put("settlement_hash", core.settlementHash);
                            cv.put("previous_task_status", core.status);
                            cv.put("exclusion_or_error_code", core.errorCode);
                            cv.put("classified_at", System.currentTimeMillis());
                            cv.put("completed_at", completedAt);
                            cv.put("record_hash", receipt.recordHash);
                            db.insertWithOnConflict(
                                    DbHelper.TABLE_R906_ISSUE_CLOSURE, null, cv,
                                    SQLiteDatabase.CONFLICT_REPLACE);

                            if (receipt.countsAsSettledPrediction()) {
                                ContentValues selected = new ContentValues();
                                selected.put("settled", 1);
                                selected.put("result", hit == 1 ? "HIT" : "MISS");
                                selected.put("settlement_hash", oosHash);
                                db.update(
                                        V51Repository.SELECTED,
                                        selected,
                                        "target_issue=? AND settled=0",
                                        new String[] {targetIssue});
                                SettlementOutboxR906.enqueueWithinTransaction(
                                        db, "RESEARCH_COMPACT_LABEL", targetIssue, "PRODUCTION",
                                        oosHash, 80, System.currentTimeMillis());
                                SettlementOutboxR906.enqueueWithinTransaction(
                                        db, "DEFERRED_ACTION_EVIDENCE", targetIssue, "1200_ACTION_SPACE",
                                        core.settlementHash, 10, System.currentTimeMillis());
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

    private static CoreRow readCore(SQLiteDatabase db, String target) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT status,settlement_hash,terminal_code,last_error,completed_at FROM "
                            + DbHelper.TABLE_R36_SETTLEMENT_TASK + " WHERE target_issue=? LIMIT 1",
                    new String[] {target});
            return c.moveToFirst() ? new CoreRow(safe(c.getString(0)), safe(c.getString(1)),
                    safe(c.getString(2)), safe(c.getString(3)), c.getLong(4)) : null;
        } finally { if (c != null) c.close(); }
    }

    private static EvidenceRow readEvidence(SQLiteDatabase db, String target) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT o.record_hash,o.settled,o.actual_number,o.hit_label,l.record_hash,l.strict_oos"
                            + " FROM " + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " o JOIN "
                            + DbHelper.TABLE_R35_OOS_LABEL + " l ON l.target_issue=o.target_issue"
                            + " WHERE o.target_issue=? LIMIT 1",
                    new String[] {target});
            return c.moveToFirst() ? new EvidenceRow(safe(c.getString(0)), c.getInt(1), c.getInt(2),
                    c.getInt(3), safe(c.getString(4)), c.getInt(5)) : null;
        } finally { if (c != null) c.close(); }
    }

    private static final class CoreRow {
        final String status, settlementHash, terminalCode, lastError, errorCode;
        final long completedAt;
        CoreRow(String s, String h, String tc, String e, long at) {
            status=s; settlementHash=h; terminalCode=tc; lastError=e; completedAt=at;
            errorCode = tc.length() > 0 ? tc : e;
        }
        boolean terminal() {
            return SettlementTaskStoreR36.COMPLETED.equals(status)
                    || SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL.equals(status)
                    || SettlementTaskStoreR36.FAILED_TERMINAL.equals(status)
                    || SettlementTaskStoreR36.INVALID_DRAW_TERMINAL.equals(status);
        }
    }

    private static final class EvidenceRow {
        final String predrawHash, oosHash; final int settled, actual, hit, strict;
        EvidenceRow(String p, int s, int a, int h, String o, int strict) {
            predrawHash=p; settled=s; actual=a; hit=h; oosHash=o; this.strict=strict;
        }
        boolean complete() {
            return predrawHash.length()==64 && oosHash.length()==64 && settled==1 && strict==1
                    && AppConfig.isValidNumber(actual) && (hit==0 || hit==1);
        }
    }

    private static String safe(String x) { return x == null ? "" : x.trim(); }
}
