package com.ethk3.novaonepro;

/**
 * The only R3.5 business state machine: FETCH_DRAW -> RECONCILE -> exact settlement -> strict OOS
 * -> next target -> single-flight analysis if missing -> deferred evidence -> business beat.
 */
public final class BusinessReconcilerR35 {
    public interface AnalysisRequester {
        void request(String baseIssue, String reason, boolean force);
    }

    public ReconcileResult reconcile(
            DbStore store,
            Models.Draw latest,
            String reason,
            boolean force,
            AnalysisRequester requester) {
        long started = System.currentTimeMillis();
        if (store == null) return fail("STORE_NULL");
        if (latest == null || !latest.isValid()) return fail("LATEST_DRAW_INVALID");
        String base = safe(latest.issue);
        String stage = "RECONCILE_ENTER";
        try {
            store.setState("r35_reconcile_stage", stage);
            store.setState("r35_reconcile_base_issue", base);
            store.setState("r35_reconcile_reason", safe(reason));
            AutoFetchService.b13RecordBusinessBeat(store, "FETCH", base);

            stage = "SETTLE_EXACT_TARGET";
            store.setState("r35_reconcile_stage", stage);
            SettlementEngine.SettlementReport settlement =
                    SettlementCoordinatorR815.settleNow(
                            store, latest, SettlementCoordinatorR815.Reason.LEGACY_API);

            stage = "STRICT_OOS_AFTER_DRAW";
            store.setState("r35_reconcile_stage", stage);
            OosAfterDrawWriterR35.LabelResult oos =
                    OosAfterDrawWriterR35.settleFrozen(store, latest);

            stage = "RESOLVE_NEXT_TARGET";
            store.setState("r35_reconcile_stage", stage);
            TargetIssueResolver.Resolution resolution = TargetIssueResolver.next(base);
            if (resolution == null || !resolution.valid) {
                RuntimeHealthLedgerR35.record(
                        store,
                        "RECONCILE",
                        base,
                        "TARGET_RESOLUTION_FAILED",
                        System.currentTimeMillis() - started,
                        AnalysisSingleFlightR35.shared().activeCount(),
                        AnalysisSingleFlightR35.shared().maximumObserved(),
                        0,
                        resolution == null ? "NULL" : resolution.detail);
                return fail("TARGET_RESOLUTION_FAILED");
            }
            String target = resolution.targetIssue;
            store.setState("r35_reconcile_target_issue", target);
            boolean canonical = ShadowObservationWriterR35.hasCanonical(store, target);
            boolean requested = false;
            if (!canonical && requester != null) {
                stage = "ANALYSIS_SINGLE_FLIGHT_REQUEST";
                store.setState("r35_reconcile_stage", stage);
                requester.request(base, "R35_RECONCILE:" + safe(reason), force);
                requested = true;
            }

            stage = "DEFERRED_EVIDENCE_STATUS";
            int deferred = DeferredEvidenceWriterR35.pendingCount(store);
            store.setState("r35_deferred_pending_count", String.valueOf(deferred));
            store.setState("r35_reconcile_stage", "BUSINESS_BEAT");
            AutoFetchService.b13RecordBusinessBeat(store, "RECONCILE", target);
            long elapsed = System.currentTimeMillis() - started;
            RuntimeHealthLedgerR35.record(
                    store,
                    "RECONCILE",
                    target,
                    "SUCCESS",
                    elapsed,
                    AnalysisSingleFlightR35.shared().activeCount(),
                    AnalysisSingleFlightR35.shared().maximumObserved(),
                    0,
                    "canonical="
                            + canonical
                            + "|requested="
                            + requested
                            + "|settlement="
                            + (settlement == null ? "NULL" : settlement.ok)
                            + "|oos="
                            + oos.status
                            + "|deferred="
                            + deferred);
            store.setState("r35_reconcile_stage", "COMPLETE");
            store.setState("r35_reconcile_elapsed_ms", String.valueOf(elapsed));
            return new ReconcileResult(
                    true,
                    base,
                    target,
                    canonical,
                    requested,
                    settlement == null ? "NULL" : safe(settlement.detail),
                    oos.status,
                    deferred,
                    "COMPLETE");
        } catch (OutOfMemoryError oom) {
            RuntimeHealthLedgerR35.recordOom(store, stage, base, oom);
            return fail("OOM_CAUGHT_AT_" + stage);
        } catch (Throwable e) {
            RuntimeHealthLedgerR35.record(
                    store,
                    "RECONCILE",
                    base,
                    "ERROR",
                    System.currentTimeMillis() - started,
                    AnalysisSingleFlightR35.shared().activeCount(),
                    AnalysisSingleFlightR35.shared().maximumObserved(),
                    0,
                    stage + ":" + e.getClass().getSimpleName());
            return fail("ERROR_AT_" + stage + ":" + e.getClass().getSimpleName());
        }
    }

    private static ReconcileResult fail(String status) {
        return new ReconcileResult(false, "", "", false, false, "", "", 0, status);
    }

    private static String safe(String x) {
        return x == null ? "" : x;
    }

    public static final class ReconcileResult {
        public final boolean success, canonicalAlreadyPresent, analysisRequested;
        public final String baseIssue, targetIssue, settlementDetail, oosStatus, status;
        public final int deferredPending;

        ReconcileResult(
                boolean success,
                String base,
                String target,
                boolean canonical,
                boolean requested,
                String settlement,
                String oos,
                int deferred,
                String status) {
            this.success = success;
            baseIssue = safe(base);
            targetIssue = safe(target);
            canonicalAlreadyPresent = canonical;
            analysisRequested = requested;
            settlementDetail = safe(settlement);
            oosStatus = safe(oos);
            deferredPending = deferred;
            this.status = safe(status);
        }
    }
}
