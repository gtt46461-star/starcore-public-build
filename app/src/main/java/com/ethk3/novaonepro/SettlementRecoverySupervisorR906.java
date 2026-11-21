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
    private int nextPhase;

    private SettlementRecoverySupervisorR906() {}

    public static SettlementRecoverySupervisorR906 shared() { return INSTANCE; }

    public synchronized int pulse(DbStore store, long requestedBudgetMs, String reason) {
        if (store == null || SettlementExecutionGateR819.isActive()) return 0;
        long started = TimeSourceR811.monoNowMs();
        if (started - lastPulseAt < MIN_PULSE_GAP_MS) return 0;
        lastPulseAt = started;
        long budget = Math.max(120L, Math.min(2200L, requestedBudgetMs));
        long deadline = started + budget;

        int seededEligible = 0;
        int reopenedFalseNoPredraw = 0;
        int reopenedRetryable = 0;
        int settled = 0;
        int evidenceRepaired = 0;
        int excludedLegacy = 0;
        String stage = "START";
        String error = "";

        try {
            // Rotate the first bounded operation. A slow eligibility scan must not consume
            // every deadline before a runnable settlement or a genuine exclusion is reached.
            for (int count=0; count<7 && (count==0 || before(deadline)); count++) {
                int phase=nextPhase;
                nextPhase=(phase+1)%7;
                switch(phase) {
                    case 0:
                    case 3:
                        stage="DRAIN_ELIGIBLE_PREDRAW";
                        settled+=SettlementCoordinatorR815.drainReadyPredraw(store,8,
                                Math.max(1L, Math.min(budget/2,deadline-TimeSourceR811.monoNowMs())));
                        break;
                    case 1:
                        stage="SEED_ELIGIBLE_PREDRAW";
                        seededEligible+=SettlementTaskStoreR36.seedHistorical(store,16);
                        break;
                    case 2:
                        stage="REOPEN_FALSE_NO_PREDRAW";
                        reopenedFalseNoPredraw+=SettlementTaskStoreR36.reopenRecoverableNoPredrawTerminals(store,12);
                        break;
                    case 4:
                        stage="REOPEN_RETRYABLE_FAILURE";
                        reopenedRetryable+=SettlementTaskStoreR36.reopenRecoverableFailedTerminals(store,8);
                        break;
                    case 5:
                        stage="CLASSIFY_NO_PREDRAW";
                        excludedLegacy+=SettlementTaskStoreR36.terminalizeAuditedNoPredrawBacklog(store,
                                RealtimePressureGateR811.hasActiveCriticalWork()?4:16);
                        break;
                    default:
                        stage="REPAIR_LEGACY_SELECTED_EVIDENCE";
                        evidenceRepaired+=SelectedEvidenceContinuityR912.repairBatch(store,8);
                        break;
                }
            }
            stage = "DONE";
        } catch (Throwable failure) {
            error = failure.getClass().getSimpleName() + ':' + safe(failure.getMessage());
        }

        SettlementTaskStoreR36.EligibilitySnapshot truth =
                SettlementTaskStoreR36.eligibilitySnapshot(store);
        long finished = System.currentTimeMillis();
        try {
            java.util.Map<String,String> state=new java.util.LinkedHashMap<>();
            state.put("r906_supervisor_last_at", String.valueOf(finished));
            state.put("r906_supervisor_last_stage", stage);
            state.put("r906_supervisor_last_error", error);
            state.put(
                    "r906_supervisor_last_cost_ms",
                    String.valueOf(Math.max(0L, TimeSourceR811.monoNowMs() - started)));
            state.put(
                    "r906_supervisor_last_result",
                    "recovery=OWNED_BY_DRAIN_READY_PREDRAW"
                            + "|seededEligible=" + seededEligible
                            + "|reopenedFalseNoPredraw=" + reopenedFalseNoPredraw
                            + "|reopenedRetryable=" + reopenedRetryable
                            + "|settled=" + settled
                            + "|evidenceRepaired=" + evidenceRepaired
                            + "|excludedLegacy=" + excludedLegacy);
            state.put("r914_settlement_eligibility_truth", truth.canonical());
            state.put(
                    "r906_settlement_outbox_pending",
                    String.valueOf(SettlementOutboxR906.pendingCount(store)));
            store.setStates(state);
        } catch (Throwable ignoredTelemetry) {
        }
        return seededEligible + reopenedFalseNoPredraw + reopenedRetryable
                + settled + evidenceRepaired + excludedLegacy;
    }

    private static boolean before(long deadline) {
        return TimeSourceR811.monoNowMs() < deadline;
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '_').trim();
    }
}
