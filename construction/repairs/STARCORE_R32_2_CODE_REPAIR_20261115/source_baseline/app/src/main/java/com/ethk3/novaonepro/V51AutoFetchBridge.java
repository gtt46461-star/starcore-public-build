package com.ethk3.novaonepro;

/** Sole typed bridge. Ownership is acquired by AutoFetchService and is never duplicated here. */
public final class V51AutoFetchBridge {
    public static final String VERSION = AppConfig.R36_RUNTIME_REPAIR_VERSION + "-DISPATCHER-BRIDGE-R908";
    public static final String PRODUCTION_RUNTIME = "ProductionPredictionDispatcherR815#dispatch";
    public static final String LEGACY_RUNTIME = "DISABLED_ON_ACTIVE_R36_PATH";

    private V51AutoFetchBridge() {}

    public static V51RuntimeOrchestrator.OutcomeR36 runR36(
            DbStore store,
            V51RuntimeInputR36 input,
            AnalysisSingleFlightR36.Request request,
            AnalysisSingleFlightR36.Lease lease) {
        return runR36(store, input, request, lease, null);
    }

    static V51RuntimeOrchestrator.OutcomeR36 runR36(
            DbStore store,
            V51RuntimeInputR36 input,
            AnalysisSingleFlightR36.Request request,
            AnalysisSingleFlightR36.Lease lease, PredrawWakeLeaseR81 predrawWakeR81) {
        if (lease == null
                || request == null
                || !lease.targetIssue.equals(request.targetIssue)
                || !lease.baseIssue.equals(request.baseIssue)
                || !lease.request.requestHash.equals(request.requestHash))
            throw new IllegalArgumentException("R36_BRIDGE_LEASE_INVALID");
        return ProductionPredictionDispatcherR815.dispatch(store, input, request, lease, predrawWakeR81);
    }

    public static boolean invokesLegacyRuntime() {
        return false;
    }

    public static boolean canCreateBet() {
        return false;
    }

    public static boolean canSettle() {
        return false;
    }

    public static String classifyDrawSignal(
            String drawIssue, String authoritativeLatestIssue, boolean historicalBackfill) {
        if (historicalBackfill) return "BACKLOG_DATA_ONLY";
        String draw = drawIssue == null ? "" : drawIssue.trim();
        String latest = authoritativeLatestIssue == null ? "" : authoritativeLatestIssue.trim();
        return draw.length() > 0 && draw.equals(latest) ? "LIVE_CURRENT" : "BACKLOG_DATA_ONLY";
    }
}
