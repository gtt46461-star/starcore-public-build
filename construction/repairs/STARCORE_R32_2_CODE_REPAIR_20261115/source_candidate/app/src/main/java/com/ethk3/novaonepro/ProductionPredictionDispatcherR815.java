package com.ethk3.novaonepro;

/** The only production entrance permitted to invoke V51RuntimeOrchestrator#runR36. */
public final class ProductionPredictionDispatcherR815 {
    public static final String OWNER = "ProductionPredictionDispatcherR815";

    private ProductionPredictionDispatcherR815() {}

    public static V51RuntimeOrchestrator.OutcomeR36 dispatch(
            DbStore store,
            V51RuntimeInputR36 input,
            AnalysisSingleFlightR36.Request request,
            AnalysisSingleFlightR36.Lease lease) {
        return dispatch(store, input, request, lease, null);
    }

    static V51RuntimeOrchestrator.OutcomeR36 dispatch(
            DbStore store,
            V51RuntimeInputR36 input,
            AnalysisSingleFlightR36.Request request,
            AnalysisSingleFlightR36.Lease lease, PredrawWakeLeaseR81 predrawWakeR81) {
        if (store == null || input == null || request == null || lease == null)
            throw new IllegalArgumentException("R815_DISPATCH_ARGUMENT_NULL");
        ReleaseIdentityR815.assertBuildConfig();
        if (!request.valid()
                || !request.targetIssue.equals(input.targetIssue)
                || !request.baseIssue.equals(input.baseIssue)
                || !request.historyHash.equals(input.historyHash)
                || !TargetIssueResolver.exact(input.baseIssue, input.targetIssue).valid)
            throw new IllegalArgumentException("R815_DISPATCH_REQUEST_INVALID");
        HistoryIntegrityVerifierR815.Result history = HistoryIntegrityVerifierR815.verifyAscending(input.history);
        if (!history.minimalReady()
                || history.gapCount != 0
                || !input.baseIssue.equals(history.maxIssue)) {
            CriticalPathErrorLedgerR815.record(
                    store,
                    new CriticalPathErrorLedgerR815.Event(
                            "PREDICTION",
                            "HISTORY_NOT_READY",
                            input.targetIssue,
                            0L,
                            true,
                            "",
                            history.summary(),
                            System.currentTimeMillis()));
            throw new IllegalStateException("R815_DISPATCH_HISTORY_NOT_READY");
        }
        try {
            V51RuntimeOrchestrator.OutcomeR36 outcome =
                    new V51RuntimeOrchestrator().runR36(store, input, request, lease, predrawWakeR81);
            PredictionOutputContractR815.Validation validation =
                    PredictionOutputContractR815.validate(input.targetIssue, outcome.runtime, input.history);
            PredictionOutputContractR815.requireCanonicalReady(validation);
            return outcome;
        } catch (RuntimeException failure) {
            CriticalPathErrorLedgerR815.record(
                    store,
                    new CriticalPathErrorLedgerR815.Event(
                            "PREDICTION",
                            "DISPATCH_FAILED",
                            input.targetIssue,
                            0L,
                            true,
                            failure,
                            failure.getMessage()));
            throw failure;
        }
    }
}
