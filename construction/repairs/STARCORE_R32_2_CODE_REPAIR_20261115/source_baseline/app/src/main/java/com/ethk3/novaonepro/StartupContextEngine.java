package com.ethk3.novaonepro;

/** Resolves startup maturity from V48 immutable issue context. */
public final class StartupContextEngine {
    public static final String MODULE_VERSION = "V48_STARTUP_CONTEXT_20260721_48072148";

    private StartupContextEngine() {}

    public static int resolve(DbStore store, Models.ComboCandidate c) {
        int candidate = 0;
        if (c != null) {
            candidate = Math.max(c.evidenceForestSample, c.historicalPriorSample);
            candidate = Math.max(candidate, c.calibrationTotalSample);
            candidate = Math.max(candidate, c.posteriorSample);
            candidate = Math.max(candidate, c.microWalkForwardSample);
        }
        int issue = V48IssueContext.visibleHistory(store);
        int resolved = Math.max(candidate, issue);
        if (c != null) {
            c.evidenceForestSample = Math.max(c.evidenceForestSample, resolved);
            c.historicalPriorSample = Math.max(c.historicalPriorSample, resolved);
            c.v46ProbabilityTrace = append(c.v46ProbabilityTrace, "V48Startup=" + resolved);
        }
        try {
            if (store != null) {
                store.setState("v48_startup_resolved", String.valueOf(resolved));
                store.setState("v48_startup_issue_sample", String.valueOf(issue));
                store.setState(
                        "v48_startup_status",
                        issue >= AppConfig.V48_CONTROLLED_START_MIN_ISSUES && resolved == 0
                                ? "STARTUP_CONTEXT_LOST"
                                : "OK");
            }
        } catch (Throwable ignore) {
        }
        return resolved;
    }

    private static String append(String a, String b) {
        return a == null || a.isEmpty() ? b : (a.contains(b) ? a : a + '|' + b);
    }
}
