package com.ethk3.novaonepro;

import java.util.List;

/**
 * R3.6.4 live-analysis history policy.
 *
 * <p>The 240-row value is a frozen upper bound, never a warm-up threshold. A fresh install first
 * receives only five rows from the latest endpoint, so the sole analysis owner must bootstrap the
 * history endpoint before entering B04. All decisions remain pre-draw and SHADOW-only.
 */
public final class R36AnalysisHistoryPolicyR364 {
    public static final int MIN_LIVE_HISTORY = 12;
    public static final int PREFERRED_BOOTSTRAP_HISTORY = 100;
    public static final int MAX_FROZEN_HISTORY = 240;

    private R36AnalysisHistoryPolicyR364() {}

    public static int validCount(List<Models.Draw> history) {
        if (history == null) return 0;
        int count = 0;
        for (int i = 0; i < history.size(); i++) {
            Models.Draw draw = history.get(i);
            if (draw != null && draw.isValid()) count++;
        }
        return count;
    }

    public static boolean needsBootstrap(int validRows) {
        return validRows < MIN_LIVE_HISTORY;
    }

    public static boolean usable(int validRows) {
        return validRows >= MIN_LIVE_HISTORY;
    }

    /** Bootstrap is driven by the latest exact suffix, not by total rows scattered across gaps. */
    public static int contiguousSuffixCount(List<Models.Draw> history, String baseIssue) {
        if (!TargetIssueResolver.isValid(baseIssue)) return 0;
        java.util.ArrayList<Models.Draw> eligible = new java.util.ArrayList<Models.Draw>();
        if (history != null) {
            for (int i = 0; i < history.size(); i++) {
                Models.Draw draw = history.get(i);
                if (draw != null
                        && draw.isValid()
                        && TargetIssueResolver.isValid(draw.issue)
                        && !TargetIssueResolver.isAfter(draw.issue, baseIssue)) {
                    eligible.add(draw);
                }
            }
        }
        HistoryIntegrityVerifierR815.Result verified =
                HistoryIntegrityVerifierR815.verifyAscending(eligible);
        return baseIssue.equals(verified.maxIssue) && !verified.hasHardConflict()
                ? verified.contiguousSuffix
                : 0;
    }
}
