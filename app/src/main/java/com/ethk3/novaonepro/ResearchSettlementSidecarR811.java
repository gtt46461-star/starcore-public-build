package com.ethk3.novaonepro;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Low-priority, model-free research settlement backlog drainer.
 *
 * <p>Production settlement/OOS always runs first. This sidecar only consumes already-completed
 * frozen evidence, yields to live pressure, and processes at most one exact-1200 issue per tick.
 */
public final class ResearchSettlementSidecarR811 {
    private static final int MAX_SCAN_PER_TICK = 3;

    private ResearchSettlementSidecarR811() {}

    /** Bounded multi-issue closure used only after Production settlement has yielded. */
    public static Result drainBatch(DbStore store, int maxIssues, long budgetMs) {
        int bound = Math.max(1, Math.min(8, maxIssues));
        long deadline = System.currentTimeMillis() + Math.max(100L, Math.min(1500L, budgetMs));
        int completed = 0, rows = 0;
        String lastTarget = "", lastHash = "", lastStatus = "IDLE";
        for (int i = 0; i < bound && System.currentTimeMillis() < deadline; i++) {
            Result one = drain(store);
            lastStatus = one.status;
            lastTarget = one.targetIssue;
            lastHash = one.groupHash;
            rows += one.rows;
            if (!one.success || "IDLE".equals(one.status)) break;
            completed++;
        }
        if (store != null) {
            store.setState("r903_research_settlement_batch", String.valueOf(completed));
            store.setState("r903_research_settlement_batch_at", String.valueOf(System.currentTimeMillis()));
        }
        return new Result(
                completed > 0 || "IDLE".equals(lastStatus),
                "R903_BATCH=" + completed + "|last=" + lastStatus,
                lastTarget,
                lastHash,
                rows);
    }

    public static Result drain(DbStore store) {
        if (store == null) return Result.fail("STORE_NULL", "");
        if (RealtimePressureGateR811.hasActiveCriticalWork()
                && AnalysisSingleFlightR36.shared().activeCount() > 0)
            return Result.fail("YIELD_REALTIME", "");
        if (!store.coordinatorR36().ensureR811ResearchSchemaOnce())
            return Result.fail("SCHEMA_NOT_READY", "");

        RuntimeMemoryGuardR811.Snapshot memory =
                RuntimeMemoryGuardR811.snapshot("RESEARCH_SIDECAR_PREFLIGHT");
        if (memory.headroomBytes < RuntimeMemoryGuardR811.LOW_HEADROOM_BYTES) {
            RuntimeMemoryGuardR811.recoverAfterFailure();
            memory = RuntimeMemoryGuardR811.snapshot("RESEARCH_SIDECAR_AFTER_RECOVERY");
            if (memory.headroomBytes < RuntimeMemoryGuardR811.LOW_HEADROOM_BYTES)
                return Result.fail("YIELD_MEMORY_PRESSURE|" + memory.canonical(), "");
        }

        /* Labels and strict selected OOS are committed by independent durable owners.  If labels
         * win that race, the immediate learner call may legitimately see no OOS row.  Re-scan the
         * oldest fully labelled strict-OOS issue above the last artifact so that dynamic learning
         * closes later without replaying or forking the monotonic FTRL chain. */
        String learningTarget = pendingLearningTarget(store);
        if (learningTarget.length() > 0) {
            ResearchAdaptiveLearnerStoreR811.UpdateResult learner =
                    ResearchAdaptiveLearnerStoreR811.updateFromStrictOos(store, learningTarget);
            ResearchTinyDeepLearnerR909.Result tiny = ResearchTinyDeepLearnerR909.updateOne(store);
            publishLearnerPair(store, learningTarget, learner.status, tiny);
            if (learner.updated || tiny.success) {
                store.setState("r811_research_sidecar_settled_target", learningTarget);
                return new Result(true,
                        "LEARNER_BACKLOG_CLOSED|ftrl=" + learner.status + "|tiny=" + tiny.status,
                        learningTarget, learner.updated ? learner.artifactHash : tiny.artifactHash, 0);
            }
        } else {
            ResearchTinyDeepLearnerR909.Result tiny = ResearchTinyDeepLearnerR909.updateOne(store);
            if (tiny.success) {
                publishLearnerPair(store, tiny.trainedUntil, "NO_FTRL_BACKLOG", tiny);
                return new Result(true, "TINY_DEEP_BACKLOG_CLOSED|" + tiny.status,
                        tiny.trainedUntil, tiny.artifactHash, 0);
            }
        }

        List<String> targets = pendingTargets(store, MAX_SCAN_PER_TICK);
        if (targets.isEmpty()) return new Result(true, "IDLE", "", "", 0);
        String lastStatus = "NO_ELIGIBLE_TARGET";
        String lastTarget = "";
        for (int i = 0; i < targets.size(); i++) {
            if (RealtimePressureGateR811.hasActiveCriticalWork()
                    && AnalysisSingleFlightR36.shared().activeCount() > 0)
                return Result.fail("YIELD_REALTIME", lastTarget);
            String target = targets.get(i);
            lastTarget = target;
            try {
                Models.Draw draw = store.getDrawByIssue(target);
                if (draw == null || !draw.isValid()) {
                    lastStatus = "DRAW_MISSING";
                    continue;
                }
                ResearchExpertPredrawSnapshotR811.Snapshot predraw =
                        ResearchExpertPredrawSnapshotR811.read(store, target);
                if (predraw != null)
                    ResearchExpertOutcomeLedgerR811.settleFromSnapshot(store, predraw, draw);
                ResearchActionOutcomeMaterializerR811.Materialization labels =
                        ResearchActionOutcomeMaterializerR811.materialize(store, target, draw);
                lastStatus = labels.status;
                if (!labels.success) continue;
                ResearchAdaptiveLearnerStoreR811.UpdateResult learner =
                        ResearchAdaptiveLearnerStoreR811.updateFromStrictOos(store, target);
                ResearchTinyDeepLearnerR909.Result tiny = ResearchTinyDeepLearnerR909.updateOne(store);
                String status = labels.status + "|ftrl=" + learner.status + "|tiny=" + tiny.status;
                publishLearnerPair(store, target, learner.status, tiny);
                store.setState("r811_research_sidecar_settled_target", target);
                store.setState("r811_research_sidecar_group_hash", labels.groupHash);
                return new Result(true, status, target, labels.groupHash, labels.rows);
            } catch (OutOfMemoryError oom) {
                RuntimeMemoryGuardR811.recoverAfterFailure();
                return Result.fail("OOM_RECOVERED", target);
            } catch (Throwable error) {
                lastStatus =
                        error.getClass().getSimpleName()
                                + ':'
                                + safe(error.getMessage());
            }
        }
        return Result.fail(lastStatus, lastTarget);
    }

    private static void publishLearnerPair(DbStore store, String settlementTarget, String ftrlStatus, ResearchTinyDeepLearnerR909.Result tiny) {
        if (store == null) return;
        LinkedHashMap<String,String> state=new LinkedHashMap<String,String>();
        state.put("r811_adaptive_learner_status", safe(ftrlStatus));
        state.put("r947_learning_pair_target", safe(settlementTarget));
        state.put("r947_tiny_deep_status", tiny == null ? "NULL_RESULT" : safe(tiny.status));
        state.put("r947_tiny_deep_trained_until", tiny == null ? "" : safe(tiny.trainedUntil));
        state.put("r947_tiny_deep_updates", tiny == null ? "0" : String.valueOf(tiny.updates));
        state.put("r947_learning_pair_at", String.valueOf(System.currentTimeMillis()));
        // Both learner owners have returned their durable results before this summary.
        store.trySetTelemetryStatesR77(state);
    }

    private static String pendingLearningTarget(DbStore store) {
        Cursor cursor = null;
        try {
            cursor = store.readable().rawQuery(
                    "SELECT o.target_issue FROM " + DbHelper.TABLE_R35_OOS_LABEL + " o JOIN "
                            + DbHelper.TABLE_R906_SELECTED_EVIDENCE + " e ON e.target_issue=o.target_issue"
                            + " JOIN " + DbHelper.TABLE_R36_SETTLEMENT_TASK + " t ON t.target_issue=o.target_issue"
                            + " JOIN " + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " s ON s.target_issue=o.target_issue"
                            + " WHERE o.strict_oos=1 AND t.status='COMPLETED' AND s.pre_draw_frozen=1 AND s.settled=1"
                            + " AND e.raw_action_p_status='AVAILABLE' AND e.rank_status='AVAILABLE'"
                            + " AND e.third_margin_status='AVAILABLE' AND e.entropy_risk_status='AVAILABLE'"
                            + " AND e.stability_status='AVAILABLE' AND e.window_consensus_status='AVAILABLE'"
                            + " AND o.target_issue>COALESCE((SELECT MAX(trained_until_issue) FROM v57_research_artifact_r811 WHERE artifact_type='FTRL_SHADOW'),'')"
                            + " AND NOT EXISTS(SELECT 1 FROM v57_research_artifact_r811 a WHERE a.artifact_type='FTRL_SHADOW' AND a.trained_until_issue=o.target_issue)"
                            + " ORDER BY o.target_issue ASC LIMIT 1", null);
            return cursor.moveToFirst() ? safe(cursor.getString(0)) : "";
        } finally { if (cursor != null) cursor.close(); }
    }

    private static List<String> pendingTargets(DbStore store, int limit) {
        ArrayList<String> output = new ArrayList<String>();
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT j.target_issue FROM "
                                            + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                            + " j JOIN "
                                            + DrawDependencyInputCP12.SOURCE
                                            + " d ON d.issue=j.target_issue AND d.dirty=0"
                                            + " LEFT JOIN (SELECT target_issue,COUNT(*) c FROM"
                                            + " v57_research_action_label_r811 GROUP BY target_issue) l"
                                            + " ON l.target_issue=j.target_issue"
                                            + " WHERE j.status='COMPLETED' AND j.core_materialized=1"
                                            + " AND j.action_rows=1200 AND COALESCE(l.c,0)<>1200"
                                            + " ORDER BY j.target_issue ASC LIMIT ?",
                                    new String[] {String.valueOf(Math.max(1, limit))});
            while (cursor.moveToNext()) {
                String target = safe(cursor.getString(0));
                if (TargetIssueResolver.isValid(target)) output.add(target);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return output;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Result {
        public final boolean success;
        public final String status, targetIssue, groupHash;
        public final int rows;

        Result(boolean success, String status, String target, String hash, int rows) {
            this.success = success;
            this.status = safe(status);
            targetIssue = safe(target);
            groupHash = safe(hash);
            this.rows = rows;
        }

        static Result fail(String status, String target) {
            return new Result(false, status, target, "", 0);
        }
    }
}
