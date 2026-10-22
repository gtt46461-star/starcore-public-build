package com.ethk3.novaonepro;

/** Host regression contract for support-aware, label-blind regime evidence. */
public final class R923RegimeEvidenceHarness {
    private R923RegimeEvidenceHarness() {}

    public static void main(String[] args) {
        narrowButSupportedMustNotLookMissing();
        constantFeatureMustNotBecomeZeroRateFailure();
        observedSupportedCollapseMustStillPrune();
        globalFailureMustStillPruneWithoutRegimeSupport();
        System.out.println("R923_REGIME_EVIDENCE=PASS");
        System.out.println("CONTRACT=UNSUPPORTED_BLOCKS_CERTIFICATION_BUT_NEVER_CAUSES_PRUNE");
    }

    private static void narrowButSupportedMustNotLookMissing() {
        double[] score = new double[120];
        int[] hit = new int[120];
        for (int i = 0; i < score.length; i++) {
            score[i] = 0.100000d + i * 0.000001d;
            hit[i] = i % 20 < 9 ? 1 : 0;
        }
        AlgorithmRegimeEvidenceR923.Result evidence =
                AlgorithmRegimeEvidenceR923.evaluate(score, hit);
        require(evidence.supported, "NARROW_FEATURE_SUPPORT");
        require(evidence.representedGroups == 3, "NARROW_FEATURE_GROUPS");
        require(evidence.minimumGroupN >= 39, "NARROW_FEATURE_MIN_N");
        require(evidence.minimumHitRate >= 0.40d, "NARROW_FEATURE_RATE");

        AlgorithmEvidencePolicyR912.Decision decision = decision(120, 54, evidence, 0.01d);
        require(!decision.status.startsWith("PRUNED"), "NARROW_FEATURE_FALSE_PRUNE");
    }

    private static void constantFeatureMustNotBecomeZeroRateFailure() {
        double[] score = new double[120];
        int[] hit = new int[120];
        for (int i = 0; i < score.length; i++) {
            score[i] = 0.125d;
            hit[i] = i % 20 < 9 ? 1 : 0;
        }
        AlgorithmRegimeEvidenceR923.Result evidence =
                AlgorithmRegimeEvidenceR923.evaluate(score, hit);
        require(!evidence.supported, "CONSTANT_FEATURE_UNSUPPORTED");
        AlgorithmEvidencePolicyR912.Decision decision = decision(120, 54, evidence, 0.01d);
        require("OBSERVING_FUTURE_OOS".equals(decision.status), "UNSUPPORTED_STATUS");
        require("REGIME_SUPPORT_INSUFFICIENT_DO_NOT_PRUNE".equals(decision.reason),
                "UNSUPPORTED_REASON");
        require(!decision.regimeSupported, "UNSUPPORTED_FLAG");
    }

    private static void observedSupportedCollapseMustStillPrune() {
        double[] score = new double[120];
        int[] hit = new int[120];
        for (int i = 0; i < score.length; i++) {
            score[i] = i;
            if (i < 40) hit[i] = i < 4 ? 1 : 0;
            else hit[i] = i % 2;
        }
        AlgorithmRegimeEvidenceR923.Result evidence =
                AlgorithmRegimeEvidenceR923.evaluate(score, hit);
        require(evidence.supported, "COLLAPSE_SUPPORT");
        require(evidence.minimumHitRate < 0.18d, "COLLAPSE_RATE");
        AlgorithmEvidencePolicyR912.Decision decision = decision(120, 44, evidence, 0.01d);
        require("PRUNED_EVIDENCE_R912".equals(decision.status), "COLLAPSE_STATUS");
        require("REGIME_COLLAPSE".equals(decision.reason), "COLLAPSE_REASON");
    }

    private static void globalFailureMustStillPruneWithoutRegimeSupport() {
        double[] score = new double[120];
        int[] hit = new int[120];
        for (int i = 0; i < score.length; i++) {
            score[i] = 0.5d;
            hit[i] = i < 20 ? 1 : 0;
        }
        AlgorithmRegimeEvidenceR923.Result evidence =
                AlgorithmRegimeEvidenceR923.evaluate(score, hit);
        AlgorithmEvidencePolicyR912.Decision decision = decision(120, 20, evidence, 1.0d);
        require("PRUNED_EVIDENCE_R912".equals(decision.status), "GLOBAL_FAILURE_STATUS");
        require(!"REGIME_COLLAPSE".equals(decision.reason), "GLOBAL_FAILURE_REASON");
    }

    private static AlgorithmEvidencePolicyR912.Decision decision(
            int n, int hit, AlgorithmRegimeEvidenceR923.Result evidence, double fdrQ) {
        return AlgorithmEvidencePolicyR912.evaluateWithRegimeSupport(
                n, hit, 0.30d, 10, 0.22d, 0.08d, 1.0d, n, 0.03d,
                evidence.minimumHitRate, fdrQ, evidence.representedGroups,
                evidence.minimumGroupN);
    }

    private static void require(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
