package com.ethk3.novaonepro;

import android.database.Cursor;

/**
 * The sole executable settlement owner. Other components may enqueue a draw but cannot activate
 * SettlementEngine's internal execution scope.
 */
public final class SettlementCoordinatorR815 {
    public enum Reason {
        AUTHORITATIVE_LATEST,
        WATCHDOG,
        RECONCILER,
        HISTORICAL_BACKFILL,
        LEGACY_API
    }

    private static final ThreadLocal<Integer> ACTIVE_EXECUTION = new ThreadLocal<Integer>();

    private SettlementCoordinatorR815() {}

    public static boolean enqueueDraw(DbStore store, Models.Draw draw, Reason reason) {
        if (store == null || draw == null || !draw.isValid() || !TargetIssueResolver.isValid(draw.issue)) {
            record(store, "ENQUEUE_INVALID", draw == null ? "" : draw.issue, null, false);
            return false;
        }
        try {
            boolean queued = SettlementTaskStoreR36.enqueueDraw(store, draw);
            if (queued && reason != Reason.HISTORICAL_BACKFILL)
                SettlementTaskStoreR36.promoteCurrentDraw(store, draw);
            return queued;
        } catch (RuntimeException failure) {
            record(store, "ENQUEUE_FAILED", draw.issue, failure, true);
            return false;
        }
    }

    public static SettlementEngine.SettlementReport settleNow(
            DbStore store, Models.Draw draw, Reason reason) {
        SettlementEngine.SettlementReport invalid = new SettlementEngine.SettlementReport();
        invalid.issue = draw == null ? "" : draw.issue;
        invalid.createdAt = System.currentTimeMillis();
        // R900: if the single-flight gate is idle, no durable RUNNING lease can be legitimate.
        // Recover stranded same-process leases before attempting the authoritative current draw.
        if (store != null && !SettlementExecutionGateR819.isActive())
            SettlementTaskStoreR36.recoverImpossibleRunningLeasesWhenIdle(store, "SETTLE_NOW");
        if (!enqueueDraw(store, draw, reason)) {
            invalid.detail = "R815_SETTLEMENT_ENQUEUE_REJECTED";
            invalid.dataErrorCount = 1;
            return invalid;
        }
        return execute(store, draw);
    }

    /**
     * Compatibility adapter for legacy callers that still consume V51Repository's report type.
     * The adapter remains here so every executable settlement path first enters this coordinator.
     */
    public static V51Repository.SettlementReport settleV51Compatibility(
            DbStore store, Models.Draw draw, Reason reason) {
        SettlementEngine.SettlementReport report = settleNow(store, draw, reason);
        String target = draw == null ? "" : safe(draw.issue);
        String detail = report == null ? "R815_SETTLEMENT_REPORT_NULL" : safe(report.detail);
        String result = report == null ? "" : safe(report.result);
        String hash =
                V51ChecksumEngine.sha256(
                        "R815_SETTLEMENT_COMPAT|"
                                + target
                                + '|'
                                + result
                                + '|'
                                + detail);
        return new V51Repository.SettlementReport(
                report != null && report.ok == 1,
                detail.indexOf("REPLAY") >= 0,
                target,
                result,
                hash,
                hash,
                detail);
    }

    /** R906 P0 path: one exact draw only. No historical seeding or backlog scan is allowed here. */
    public static SettlementEngine.SettlementReport closeCurrentExactR906(
            DbStore store, Models.Draw draw, Reason reason) {
        if (store == null || draw == null || !draw.isValid()) {
            SettlementEngine.SettlementReport r = new SettlementEngine.SettlementReport();
            r.ok = 0; r.issue = draw == null ? "" : safe(draw.issue); r.detail = "R906_CURRENT_EXACT_INVALID";
            return r;
        }
        // R909: if an older build falsely terminalized NO_PREDRAW before the frozen row became
        // visible, repair THIS exact issue before enqueue/claim.  This path is bounded and does not
        // scan history.
        SettlementTaskStoreR36.reopenCurrentRecoverableFailedR911(store, draw.issue);
        SettlementTaskStoreR36.reopenCurrentFalseNoPredraw(store, draw.issue);
        // settleNow owns the single enqueue+promotion+claim sequence.  R908 called enqueue twice here,
        // adding avoidable SQLite writer contention on every minute.
        SettlementEngine.SettlementReport r = settleNow(store, draw, reason == null ? Reason.RECONCILER : reason);
        SettlementTaskStoreR36.Terminal terminal = SettlementTaskStoreR36.terminal(store, draw.issue);
        try {
            store.setState("r909_current_exact_issue", draw.issue);
            store.setState("r909_current_exact_closed", SettlementCoreTruthR909.businessClosed(store, draw.issue) ? "1" : "0");
            store.setState("r909_current_exact_terminal", terminal == null ? "OPEN" : terminal.status);
            store.setState("r909_current_exact_settled_prediction", SettlementCoreTruthR909.settledPrediction(store, draw.issue) ? "1" : "0");
        } catch (Throwable ignore) {}
        return r;
    }

    /**
     * Hot-path handoff: promote the already durable exact-draw task without executing
     * SettlementEngine on the fetch/analysis caller thread.
     */
    public static boolean queueCurrentExactR914(DbStore store, Models.Draw draw, Reason reason) {
        if (store == null || draw == null || !draw.isValid()) return false;
        // R947 P0 request-first boundary: the just-drawn issue has a durable settlement queue,
        // while the next issue has a finite pre-draw analysis window. Persist its request before
        // settlement-table work can delay the caller. Execution remains owned by AutoFetchService.
        primeNextAnalysisRequestR947(store, draw);
        boolean queued = SettlementTaskStoreR36.handoffCurrentExactR914(store, draw);
        try {
            java.util.LinkedHashMap<String, String> state =
                    new java.util.LinkedHashMap<String, String>();
            state.put("r914_current_exact_handoff_issue", draw.issue);
            state.put("r914_current_exact_handoff_status", queued ? "DURABLE_QUEUED" : "REJECTED");
            state.put("r914_current_exact_handoff_at", String.valueOf(System.currentTimeMillis()));
            store.setStates(state);
        } catch (Throwable ignoredTelemetry) {}
        return queued;
    }

    private static void primeNextAnalysisRequestR947(DbStore store, Models.Draw draw) {
        try {
            TargetIssueResolver.Resolution next = TargetIssueResolver.next(draw.issue);
            if (!next.valid || CanonicalObservationWriterR36.hasCanonical(store, next.targetIssue)) return;
            long now = System.currentTimeMillis();
            long deadline=TargetIssueResolver.predrawDeadlineWallR20261008(draw);
            // The durable request store requires the exact draw-derived deadline.
            // now+55s was always a different value and made this handoff's
            // request-first path fail with F01_REQUEST_DEADLINE_MISMATCH.
            if(deadline<=now)return;
            AnalysisSingleFlightR36.Request request = new AnalysisRequestStoreR36(store).ensurePending(
                    draw.issue, next.targetIssue, deadline,
                    "R947_SETTLEMENT_HANDOFF_REQUEST_FIRST");
            java.util.LinkedHashMap<String, String> state = new java.util.LinkedHashMap<String, String>();
            state.put("r947_request_first_base", draw.issue);
            state.put("r947_request_first_target", request == null ? next.targetIssue : request.targetIssue);
            state.put("r947_request_first_status", request == null ? "NO_REQUEST" : "DURABLE_REQUESTED");
            state.put("r947_request_first_at", String.valueOf(now));
            store.trySetTelemetryStatesR77(state);
        } catch (Throwable failure) {
            try {
                java.util.LinkedHashMap<String,String> telemetry=new java.util.LinkedHashMap<>();
                telemetry.put("r947_request_first_status","ERROR:" + failure.getClass().getSimpleName() + ':'
                        + (failure.getMessage() == null ? "" : failure.getMessage()));
                store.trySetTelemetryStatesR77(telemetry);
            } catch (Throwable ignored) {}
        }
    }

    /** R906 P2 maintenance adapter. Call only outside the current exact-draw -> next-analysis critical path. */
    public static int pulseCatchupR906(DbStore store, long budgetMs) {
        return SettlementRecoverySupervisorR906.shared().pulse(store, budgetMs, "COORDINATOR_PULSE");
    }

    public static int drainCurrent(DbStore store, int limit) {
        return drain(store, limit, false);
    }

    public static int drainBacklog(DbStore store, int limit) {
        return drain(store, limit, true);
    }

    /**
     * R901 urgent historical settlement lane. The API can advance by several issues between two
     * polls, so a frozen SHADOW target may already be present in the fetched history even when it
     * was never the authoritative latest row. Those rows are real settlement work, not optional
     * maintenance. Drain only BACKLOG rows that have an immutable pre-draw SHADOW observation.
     * This lane remains inside the single SettlementEngine owner and never recomputes a model.
     */
    public static int drainPredrawBacklog(DbStore store, int limit) {
        if (store == null) return 0;
        if (SettlementExecutionGateR819.isActive()) return 0;
        SettlementTaskStoreR36.recoverImpossibleRunningLeasesWhenIdle(
                store, "R901_DRAIN_PREDRAW_BACKLOG");
        int bound = Math.max(1, Math.min(32, limit));
        int terminalized = 0;
        for (int i = 0; i < bound; i++) {
            String target = nextRunnablePredrawBacklogR901(store);
            if (target.length() == 0) break;
            Models.Draw draw = store.getDrawByIssue(target);
            if (draw == null || !draw.isValid()) {
                SettlementTaskStoreR36.markInvalidDrawTerminal(
                        store,
                        target,
                        draw == null
                                ? "R901_PREDRAW_REFERENCED_DRAW_MISSING"
                                : "R901_PREDRAW_REFERENCED_DRAW_INVALID");
            } else {
                enqueueDraw(store, draw, Reason.HISTORICAL_BACKFILL);
                SettlementEngine.SettlementReport report = execute(store, draw);
                if (report != null
                        && "R819_SETTLEMENT_SINGLE_FLIGHT_BUSY".equals(report.detail)) break;
            }
            if (SettlementTaskStoreR36.terminal(store, target) != null) terminalized++;
        }
        return terminalized;
    }

    /**
     * R903 correctness drain for every runnable class that already has immutable pre-draw
     * evidence.  R901 only selected {@code BACKLOG}; after a transient failure tasks are promoted
     * to {@code LIVE_RETRY}, so that query could strand hundreds of otherwise settleable issues.
     * The wall-clock budget keeps this repair lane from monopolising the single DB writer.
     */
    public static int drainReadyPredraw(
            DbStore store, int limit, long budgetMs) {
        if (store == null || SettlementExecutionGateR819.isActive()) return 0;
        SettlementTaskStoreR36.recoverImpossibleRunningLeasesWhenIdle(
                store, "R903_DRAIN_ALL_READY_PREDRAW");
        int bound = Math.max(1, Math.min(64, limit));
        long deadline = TimeSourceR811.monoNowMs() + Math.max(1L, Math.min(2500L, budgetMs));
        int terminalized = 0;
        for (int i = 0; i < bound && TimeSourceR811.monoNowMs() < deadline; i++) {
            String target = nextRunnableReadyPredrawR903(store);
            if (target.length() == 0) break;
            Models.Draw draw = store.getDrawByIssue(target);
            if (draw == null || !draw.isValid()) {
                SettlementTaskStoreR36.markInvalidDrawTerminal(
                        store,
                        target,
                        draw == null
                                ? "R903_PREDRAW_REFERENCED_DRAW_MISSING"
                                : "R903_PREDRAW_REFERENCED_DRAW_INVALID");
            } else {
                enqueueDraw(store, draw, Reason.HISTORICAL_BACKFILL);
                SettlementEngine.SettlementReport report = execute(store, draw);
                if (report != null
                        && "R819_SETTLEMENT_SINGLE_FLIGHT_BUSY".equals(report.detail)) break;
            }
            if (SettlementTaskStoreR36.terminal(store, target) != null) terminalized++;
        }
        store.setState("r903_ready_predraw_last_drained", String.valueOf(terminalized));
        store.setState("r903_ready_predraw_last_at", String.valueOf(System.currentTimeMillis()));
        return terminalized;
    }

    static boolean isCoordinatorExecutionActive() {
        Integer depth = ACTIVE_EXECUTION.get();
        return depth != null && depth.intValue() > 0;
    }

    private static int drain(DbStore store, int limit, boolean backlogOnly) {
        if (store == null) return 0;
        if (SettlementExecutionGateR819.isActive()) return 0;
        SettlementTaskStoreR36.recoverImpossibleRunningLeasesWhenIdle(
                store, backlogOnly ? "DRAIN_BACKLOG" : "DRAIN_CURRENT");
        int bound = Math.max(1, Math.min(50, limit));
        int terminalized = 0;
        for (int i = 0; i < bound; i++) {
            String target = nextRunnable(store, backlogOnly);
            if (target.length() == 0) break;
            Models.Draw draw = store.getDrawByIssue(target);
            if (draw == null || !draw.isValid()) {
                SettlementTaskStoreR36.markInvalidDrawTerminal(
                        store,
                        target,
                        draw == null ? "R815_REFERENCED_DRAW_MISSING" : "R815_REFERENCED_DRAW_INVALID");
            } else {
                // Re-enter idempotent enqueue so legacy V1 task hashes are migrated to V2 before
                // the lease is claimed; this does not create a second task.
                enqueueDraw(store, draw, Reason.HISTORICAL_BACKFILL);
                SettlementEngine.SettlementReport report = execute(store, draw);
                if (report != null
                        && "R819_SETTLEMENT_SINGLE_FLIGHT_BUSY".equals(report.detail)) break;
            }
            if (SettlementTaskStoreR36.terminal(store, target) != null) terminalized++;
        }
        return terminalized;
    }

    private static SettlementEngine.SettlementReport execute(DbStore store, Models.Draw draw) {
        SettlementEngine.SettlementReport invalid = new SettlementEngine.SettlementReport();
        invalid.issue = draw == null ? "" : draw.issue;
        invalid.createdAt = System.currentTimeMillis();
        if (store == null || draw == null || !draw.isValid()) {
            invalid.dataErrorCount = 1;
            invalid.detail = "R815_EXECUTION_INVALID";
            return invalid;
        }
        if (!SettlementExecutionGateR819.tryEnter()) {
            invalid.ok = 1;
            invalid.waitingCount = 1;
            invalid.detail = "R819_SETTLEMENT_SINGLE_FLIGHT_BUSY";
            return invalid;
        }
        Integer depth = ACTIVE_EXECUTION.get();
        ACTIVE_EXECUTION.set(Integer.valueOf(depth == null ? 1 : depth.intValue() + 1));
        try {
            SettlementEngine.SettlementReport report =
                    new SettlementEngine().executeFromCoordinatorR815(store, draw);
            SettlementTaskStoreR36.Terminal terminal = SettlementTaskStoreR36.terminal(store, draw.issue);
            if (terminal != null && SettlementTaskStoreR36.COMPLETED.equals(terminal.status)) {
                // Essential why-hit/why-miss and Teacher/reverse/causal feedback must exist before
                // optional genome/formula/asset fan-out can consume the post-commit budget.
                EssentialSettlementFeedbackR923.settleNow(store, draw);
                MissStreakEscalationR923.analyze(store);
                CanonicalObservationWriterR36.CanonicalView canonical =
                        CanonicalObservationWriterR36.readView(store, draw.issue);
                if (canonical != null) {
                    int actual = draw.getNumberAtPosition(canonical.position);
                    int hit = actual == canonical.n1 || actual == canonical.n2 || actual == canonical.n3 ? 1 : 0;
                    ShadowObservationSidecarR815.onProductionSettled(
                            store,
                            new ShadowObservationSidecarR815.SettledEvent(
                                    draw.issue,
                                    terminal.settlementHash,
                                    canonical.recordHash,
                                    actual,
                                    hit,
                                    terminal.completedAt));
                }
                // Single post-COMPLETED extension point. R952 never calls SettlementEngine and
                // derives idempotence from the immutable terminal settlement hash.
                com.ethk3.novaonepro.r952.ResearchSettlementHookRegistryR952.dispatchCompleted(
                        store, draw, terminal.settlementHash, terminal.completedAt);
            }
            /* R908: Research sidecar settlement is intentionally excluded from the
             * Production SettlementExecutionGate.  ResearchPredrawSettlementPumpR908 later reads
             * the exact persisted draw and labels only already-frozen SHADOW rows. */
            return report;
        } catch (RuntimeException failure) {
            record(store, "EXECUTION_FAILED", draw.issue, failure, true);
            throw failure;
        } finally {
            Integer current = ACTIVE_EXECUTION.get();
            if (current == null || current.intValue() <= 1) ACTIVE_EXECUTION.remove();
            else ACTIVE_EXECUTION.set(Integer.valueOf(current.intValue() - 1));
            SettlementExecutionGateR819.exit();
        }
    }

    private static String nextRunnablePredrawBacklogR901(DbStore store) {
        Cursor cursor = null;
        long now = System.currentTimeMillis();
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT t.target_issue FROM "
                                            + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " t JOIN "
                                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                            + " o ON o.target_issue=t.target_issue"
                                            + " WHERE t.task_class='BACKLOG'"
                                            + " AND o.pre_draw_frozen=1 AND o.settled=0"
                                            + " AND ((t.status IN ('PENDING','RETRY_WAIT') AND COALESCE(t.next_attempt_at,0)<=CAST(? AS INTEGER))"
                                            + " OR (t.status='RUNNING' AND COALESCE(t.lease_until,0)<CAST(? AS INTEGER)))"
                                            + " ORDER BY COALESCE(t.next_attempt_at,0) ASC,t.created_at ASC,t.target_issue ASC LIMIT 1",
                                    new String[] {String.valueOf(now), String.valueOf(now)});
            return cursor.moveToFirst() ? safe(cursor.getString(0)) : "";
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static String nextRunnableReadyPredrawR903(DbStore store) {
        Cursor cursor = null;
        long now = System.currentTimeMillis();
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT t.target_issue FROM "
                                            + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " t JOIN "
                                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                            + " o ON o.target_issue=t.target_issue"
                                            + " JOIN "
                                            + DrawDependencyInputCP12.SOURCE
                                            + " d ON d.issue=t.target_issue AND d.dirty=0"
                                            + " WHERE o.pre_draw_frozen=1 AND o.settled=0"
                                            + " AND ((t.status IN ('PENDING','RETRY_WAIT')"
                                            + " AND COALESCE(t.next_attempt_at,0)<=CAST(? AS INTEGER)) OR (t.status='RUNNING'"
                                            + " AND COALESCE(t.lease_until,0)<CAST(? AS INTEGER)))"
                                            + " ORDER BY t.priority DESC,COALESCE(t.next_attempt_at,0) ASC,"
                                            + "t.created_at ASC,t.target_issue ASC LIMIT 1",
                                    new String[] {String.valueOf(now), String.valueOf(now)});
            return cursor.moveToFirst() ? safe(cursor.getString(0)) : "";
        } catch (RuntimeException failure) {
            record(store, "R903_READY_PREDRAW_QUERY_FAILED", "", failure, true);
            return "";
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static String nextRunnable(DbStore store, boolean backlogOnly) {
        Cursor cursor = null;
        long now = System.currentTimeMillis();
        try {
            String classes = backlogOnly ? "task_class='BACKLOG'" : "task_class<>'BACKLOG'";
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT target_issue FROM "
                                            + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " WHERE "
                                            + classes
                                            + " AND ((status IN ('PENDING','RETRY_WAIT') AND COALESCE(next_attempt_at,0)<=CAST(? AS INTEGER))"
                                            + " OR (status='RUNNING' AND COALESCE(lease_until,0)<CAST(? AS INTEGER)))"
                                            + " ORDER BY priority DESC,COALESCE(next_attempt_at,0) ASC,created_at ASC LIMIT 1",
                                    new String[] {String.valueOf(now), String.valueOf(now)});
            return cursor.moveToFirst() ? safe(cursor.getString(0)) : "";
        } catch (RuntimeException failure) {
            record(store, "DRAIN_QUERY_FAILED", "", failure, true);
            return "";
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static void record(
            DbStore store, String code, String target, Throwable failure, boolean retryable) {
        CriticalPathErrorLedgerR815.record(
                store,
                new CriticalPathErrorLedgerR815.Event(
                        "SETTLEMENT",
                        code,
                        target,
                        0L,
                        retryable,
                        failure,
                        failure == null ? code : failure.getMessage()));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

