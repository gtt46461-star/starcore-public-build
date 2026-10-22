package com.ethk3.novaonepro;

import java.util.List;

/**
 * Persists the immutable final OOS surface. This class is diagnostic-only and never creates BET.
 */
public final class CandidateDecisionFunnelLogEngine {
    public static final String MODULE_VERSION = "STARCORE_TIANSHU_B07_FINAL40_FUNNEL_LOG_20260806";

    private CandidateDecisionFunnelLogEngine() {}

    public static int persist(DbStore store, List<Models.ComboCandidate> formal) {
        return persist(store, formal, null, "");
    }

    public static int persist(
            DbStore store,
            List<Models.ComboCandidate> formal,
            List<Models.ComboCandidate> diagnosticSurface,
            String targetIssue) {
        int n = OosWriteAuditEngine.persist(store, formal, diagnosticSurface, targetIssue);
        try {
            if (store != null) {
                store.setState("v48_final40_log_version", MODULE_VERSION);
                store.setState(
                        "v48_final40_log_count",
                        String.valueOf(formal == null ? 0 : formal.size()));
                store.setState(
                        "v48_final40_diagnostic_surface_count",
                        String.valueOf(diagnosticSurface == null ? 0 : diagnosticSurface.size()));
                store.setState("v48_final40_inserted_or_duplicate", String.valueOf(n));
                store.setState(
                        "v48_final40_log_policy", "DIAGNOSTIC_ONLY_NOT_DECISION_GATE_NOT_BET");
            }
        } catch (Throwable ignore) {
        }
        return n;
    }
}
