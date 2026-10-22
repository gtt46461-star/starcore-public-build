package com.ethk3.novaonepro;

/**
 * Continuously reconciles the exact settlement denominator.
 *
 * <p>Correctness work is deliberately independent of Research and deferred 100/1200-row evidence.
 * Every clean draw that already has an immutable pre-draw canonical must acquire a durable task and
 * reach one terminal settlement. Rows without a pre-draw canonical are excluded, never fabricated.</p>
 */
public final class SettlementRecoverySupervisorR906 {
    private static final SettlementRecoverySupervisorR906 INSTANCE =
            new SettlementRecoverySupervisorR906();
    private static final long MIN_PULSE_GAP_MS = 250L;

    private long lastPulseAt;

    private SettlementRecoverySupervisorR906() {}

    public static SettlementRecoverySupervisorR906 shared() { return INSTANCE; }

    public synchronized int pulse(DbStore store, long requestedBudgetMs, String reason) {
        if (store == null || SettlementExecutionGateR819.isActive()) return 0;
        long started = TimeSourceR811.monoNowMs();
        if (started - lastPulseAt < MIN_PULSE_GAP_MS) return 0;
        lastPulseAt = started;
        long budget = Math.max(120L, Math.min(2200L, requestedBudgetMs));
        long deadline = started + budget;

        int recoveredLeases = 0;
        int seededEligible = 0;
        int reopenedFalseNoPredraw = 0;
        int reopenedRetryable = 0;
        int settled = 0;
        int evidenceRepaired = 0;
        int excludedLegacy = 0;
        String stage = "START";
        String error = "";

        try {
            stage = "RECOVER_ORPHAN_LEASES";
            recoveredLeases = SettlementTaskStoreR36.recoverImpossibleRunningLeasesWhenIdle(
                    store, "R914_SUPERVISOR_" + safe(reason));

            // Already runnable settlements get a reserved slice before migration/repair scans.
            // A busy database must not consume every pulse seeding rows while existing tasks wait.
            if (before(deadline)) {
                stage = "DRAIN_EXISTING_ELIGIBLE_PREDRAW";
                settled = SettlementCoordinatorR815.drainReadyPredraw(
                        store, 16, Math.max(1L, Math.min(budget / 2,
                                deadline - TimeSourceR811.monoNowMs())));
            }

            // Seeding eligible rows is P0 correctness, not quiet-window maintenance. Without this
            // step an API jump can save several exact draws while only the newest one is enqueued.
            if (before(deadline)) {
                stage = "SEED_ELIGIBLE_PREDRAW";
                seededEligible = SettlementTaskStoreR36.seedHistorical(store, 48);
            }

            if (before(deadline)) {
                stage = "REOPEN_FALSE_NO_PREDRAW";
                reopenedFalseNoPredraw =
                        SettlementTaskStoreR36.reopenRecoverableNoPredrawTerminals(store, 32);
            }

            if (before(deadline)) {
                stage = "REOPEN_RETRYABLE_FAILURE";
                reopenedRetryable =
                        SettlementTaskStoreR36.reopenRecoverableFailedTerminals(store, 16);
            }

            if (before(deadline)) {
                stage = "DRAIN_ELIGIBLE_PREDRAW";
                settled += SettlementCoordinatorR815.drainReadyPredraw(
                        store, 32, Math.max(1L, deadline - TimeSourceR811.monoNowMs()));
            }

            // Legacy display evidence is repairable from immutable selected-core columns. It is not
            // part of settlement eligibility and therefore runs only after the exact drain.
            if (before(deadline)) {
                stage = "REPAIR_LEGACY_SELECTED_EVIDENCE";
                evidenceRepaired = SelectedEvidenceContinuityR912.repairBatch(store, 16);
            }

            // Rows that truly never had a pre-draw canonical cannot be labelled.  R915 required a
            // completely quiet window here, so one-minute realtime pressure could leave dozens of
            // already-drawn tasks open forever and make the UI look as if settlement had stopped.
            // Terminalization is model-free and bounded: close a tiny slice under pressure and a
            // larger slice when quiet, retaining the exclusion row instead of fabricating a label.
            if (before(deadline)) {
                boolean pressure = RealtimePressureGateR811.hasActiveCriticalWork();
                stage = pressure ? "CLASSIFY_NO_PREDRAW_PRESSURE_SLICE" : "CLASSIFY_NO_PREDRAW";
                excludedLegacy = SettlementTaskStoreR36.terminalizeAuditedNoPredrawBacklog(
                        store, pressure ? 4 : 32);
            }
            stage = "DONE";
        } catch (Throwable failure) {
            error = failure.getClass().getSimpleName() + ':' + safe(failure.getMessage());
        }

        SettlementTaskStoreR36.EligibilitySnapshot truth =
                SettlementTaskStoreR36.eligibilitySnapshot(store);
        long finished = System.currentTimeMillis();
        try {
            store.setState("r906_supervisor_last_at", String.valueOf(finished));
            store.setState("r906_supervisor_last_stage", stage);
            store.setState("r906_supervisor_last_error", error);
            store.setState(
                    "r906_supervisor_last_cost_ms",
                    String.valueOf(Math.max(0L, TimeSourceR811.monoNowMs() - started)));
            store.setState(
                    "r906_supervisor_last_result",
                    "recovered=" + recoveredLeases
                            + "|seededEligible=" + seededEligible
                            + "|reopenedFalseNoPredraw=" + reopenedFalseNoPredraw
                            + "|reopenedRetryable=" + reopenedRetryable
                            + "|settled=" + settled
                            + "|evidenceRepaired=" + evidenceRepaired
                            + "|excludedLegacy=" + excludedLegacy);
            store.setState("r914_settlement_eligibility_truth", truth.canonical());
            store.setState(
                    "r906_settlement_outbox_pending",
                    String.valueOf(SettlementOutboxR906.pendingCount(store)));
        } catch (Throwable ignoredTelemetry) {
        }
        return recoveredLeases + seededEligible + reopenedFalseNoPredraw + reopenedRetryable
                + settled + evidenceRepaired + excludedLegacy;
    }

    private static boolean before(long deadline) {
        return TimeSourceR811.monoNowMs() < deadline;
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '_').trim();
    }
}
