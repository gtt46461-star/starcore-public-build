package com.ethk3.novaonepro;

/** Future-only promotion and irreversible evidence-pruning policy for invented algorithms. */
public final class AlgorithmEvidencePolicyR912 {
    public static final String EVIDENCE_VERSION = "R925_CAPTURE_QUALITY_SEPARATED_V2";
    public static final int MIN_CERTIFICATE_N = 240;
    public static final int MIN_PAIRED_N = 120;
    public static final int FAMILY_HYPOTHESES = 48;
    public static final double RANDOM_K3_RATE = 0.30d;
    public static final double MAX_FDR_Q = 0.20d;

    private AlgorithmEvidencePolicyR912() {}

    /** Compatibility entry point used by earlier host contracts. */
    public static Decision evaluate(int n, int hit, double blockMin, int maxMiss) {
        return evaluate(
                n,
                hit,
                blockMin,
                maxMiss,
                0.21d,
                0.08d,
                1.0d,
                Math.max(0, n),
                0.03d,
                blockMin);
    }

    /**
     * Promotion cannot be obtained from hit rate alone. Every value must come from chronological
     * predictions frozen after the genome's creation issue.
     */
    public static Decision evaluate(
            int n,
            int hit,
            double blockMin,
            int maxMiss,
            double brier,
            double ece,
            double coverage,
            int pairedN,
            double pairedDelta,
            double regimeMin) {
        return evaluate(
                n,
                hit,
                blockMin,
                maxMiss,
                brier,
                ece,
                coverage,
                pairedN,
                pairedDelta,
                regimeMin,
                fdrQ(hit, n, FAMILY_HYPOTHESES));
    }

    /**
     * Complete evidence policy. {@code fdrQ} is the multiplicity-adjusted one-sided probability
     * that the observed result can be explained by the 30% K3 null. It is supplied explicitly by
     * the lifecycle so the exact value written into the contract is the value used for promotion.
     */
    public static Decision evaluate(
            int n,
            int hit,
            double blockMin,
            int maxMiss,
            double brier,
            double ece,
            double coverage,
            int pairedN,
            double pairedDelta,
            double regimeMin,
            double fdrQ) {
        return evaluateWithRegimeSupport(
                n, hit, blockMin, maxMiss, brier, ece, coverage, pairedN, pairedDelta,
                regimeMin, fdrQ, AlgorithmRegimeEvidenceR923.MIN_GROUPS,
                AlgorithmRegimeEvidenceR923.MIN_GROUP_N);
    }

    /**
     * Complete policy with explicit subgroup support.  An unavailable regime metric is not the
     * same fact as an observed zero-rate regime: it can block certification but can never justify
     * an irreversible evidence prune.
     */
    public static Decision evaluateWithRegimeSupport(
            int n,
            int hit,
            double blockMin,
            int maxMiss,
            double brier,
            double ece,
            double coverage,
            int pairedN,
            double pairedDelta,
            double regimeMin,
            double fdrQ,
            int regimeGroups,
            int regimeMinN) {
        int sample = Math.max(0, n);
        int success = Math.max(0, Math.min(sample, hit));
        int paired = Math.max(0, Math.min(sample, pairedN));
        double rate = sample == 0 ? 0.0d : (double) success / sample;
        double lower = wilson(success, sample, false);
        double upper = wilson(success, sample, true);
        double block = clamp(blockMin, 0.0d, 1.0d);
        double regime = clamp(regimeMin, 0.0d, 1.0d);
        double bs = clamp(brier, 0.0d, 1.0d);
        double calibration = clamp(ece, 0.0d, 1.0d);
        double cov = clamp(coverage, 0.0d, 1.0d);
        double delta = clamp(pairedDelta, -1.0d, 1.0d);
        double adjustedQ = clamp(fdrQ, 0.0d, 1.0d);
        int groups = Math.max(0, regimeGroups);
        int minimumRegimeN = Math.max(0, regimeMinN);
        boolean regimeSupported = groups >= AlgorithmRegimeEvidenceR923.MIN_GROUPS
                && minimumRegimeN >= AlgorithmRegimeEvidenceR923.MIN_GROUP_N;

        String status;
        String reason;
        if (!finite(brier) || !finite(ece) || brier<0 || ece<0 || !finite(coverage) || !finite(fdrQ)
                || !finite(pairedDelta) || !finite(blockMin) || !finite(regimeMin)) {
            status = "OBSERVING_FUTURE_OOS";
            reason = "METRIC_UNAVAILABLE_DO_NOT_PROMOTE_OR_PRUNE";
        } else if (sample >= 64 && upper < RANDOM_K3_RATE) {
            status = "PRUNED_EVIDENCE_R912";
            reason = "WILSON_UPPER_BELOW_RANDOM_K3";
        } else if (sample >= 120 && rate < RANDOM_K3_RATE) {
            status = "PRUNED_EVIDENCE_R912";
            reason = "FUTURE_RATE_BELOW_RANDOM_K3";
        } else if (sample >= 120 && block < 0.20d) {
            status = "PRUNED_EVIDENCE_R912";
            reason = "CHRONOLOGICAL_BLOCK_COLLAPSE";
        } else if (sample >= 120 && regimeSupported && regime < 0.18d) {
            status = "PRUNED_EVIDENCE_R912";
            reason = "REGIME_COLLAPSE";
        } else if (sample >= 120 && maxMiss > 24) {
            status = "PRUNED_EVIDENCE_R912";
            reason = "MAX_MISS_UNSTABLE";
        } else if (sample >= 120 && bs > 0.30d) {
            status = "PRUNED_EVIDENCE_R912";
            reason = "BRIER_UNUSABLE";
        } else if (sample >= 120 && calibration > 0.20d) {
            status = "PRUNED_EVIDENCE_R912";
            reason = "ECE_UNCALIBRATED";
        } else if (paired >= MIN_PAIRED_N && delta < -0.03d) {
            status = "PRUNED_EVIDENCE_R912";
            reason = "PAIRED_MAINLINE_HARM";
        } else if (sample >= 120 && cov < 0.60d) {
            status = "OBSERVING_FUTURE_OOS";
            reason = "SYSTEM_CAPTURE_GAP_DO_NOT_PRUNE";
        } else if (sample >= 120 && !regimeSupported) {
            status = "OBSERVING_FUTURE_OOS";
            reason = "REGIME_SUPPORT_INSUFFICIENT_DO_NOT_PRUNE";
        } else if (sample >= MIN_CERTIFICATE_N
                && rate >= CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE
                && lower > 0.30d
                && block >= 0.25d
                && regime >= 0.22d
                && maxMiss <= 16
                && bs <= CertificateBundleServiceR929.MAX_ENTRY_BRIER
                && calibration <= 0.12d
                && cov >= 0.80d
                && paired >= MIN_PAIRED_N
                && delta >= 0.02d
                && adjustedQ <= MAX_FDR_Q) {
            status = "CHALLENGER_FUTURE_OOS";
            reason = "MULTI_METRIC_FUTURE_OOS_PASS";
        } else if (sample >= 24) {
            status = "OBSERVING_FUTURE_OOS";
            reason = "MORE_FUTURE_EVIDENCE_REQUIRED";
        } else {
            status = "IDEA_WAIT_FUTURE_OOS";
            reason = "FUTURE_SAMPLE_LT_24";
        }
        return new Decision(
                status,
                reason,
                rate,
                lower,
                upper,
                block,
                maxMiss,
                bs,
                calibration,
                cov,
                paired,
                delta,
                regime,
                adjustedQ,
                regimeSupported,
                groups,
                minimumRegimeN);
    }

    /** Conservative family-wise Benjamini-Hochberg upper bound for an unranked candidate. */
    public static double fdrQ(int hit, int n, int hypotheses) {
        if (n <= 0) return 1.0d;
        int success = Math.max(0, Math.min(n, hit));
        double variance = n * RANDOM_K3_RATE * (1.0d - RANDOM_K3_RATE);
        if (variance <= 0.0d) return 1.0d;
        // Continuity-corrected one-sided normal tail. Multiplication by the number of concurrently
        // searched families is a conservative BH bound when the candidate's rank is unknown.
        double z = (success - 0.5d - n * RANDOM_K3_RATE) / Math.sqrt(variance);
        double p = z <= 0.0d ? 1.0d : normalUpperTail(z);
        return clamp(p * Math.max(1, hypotheses), 0.0d, 1.0d);
    }

    private static double normalUpperTail(double z) {
        // Abramowitz-Stegun 7.1.26, deterministic and Android/host-Java compatible.
        double x = Math.abs(z) / Math.sqrt(2.0d);
        double t = 1.0d / (1.0d + 0.3275911d * x);
        double erf = 1.0d
                - (((((1.061405429d * t - 1.453152027d) * t) + 1.421413741d) * t
                                - 0.284496736d) * t + 0.254829592d) * t * Math.exp(-x * x);
        double cdf = 0.5d * (1.0d + (z >= 0.0d ? erf : -erf));
        return clamp(1.0d - cdf, 0.0d, 1.0d);
    }

    private static double wilson(int hit, int n, boolean upper) {
        if (n <= 0) return upper ? 1.0d : 0.0d;
        double z = 1.959963984540054d;
        double p = (double) hit / n;
        double z2 = z * z;
        double denominator = 1.0d + z2 / n;
        double center = p + z2 / (2.0d * n);
        double radius = z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * n)) / n);
        return clamp((center + (upper ? radius : -radius)) / denominator, 0.0d, 1.0d);
    }

    private static boolean finite(double x) { return !Double.isNaN(x) && !Double.isInfinite(x); }

    private static double clamp(double value, double lo, double hi) {
        double x = Double.isNaN(value) || Double.isInfinite(value) ? lo : value;
        return Math.max(lo, Math.min(hi, x));
    }

    public static final class Decision {
        public final String status, reason;
        public final double hitRate, wilsonLower, wilsonUpper;
        public final double blockMin, brier, ece, coverage, pairedDelta, regimeMin, fdrQ;
        public final int maxMiss, pairedN;
        public final boolean regimeSupported;
        public final int regimeGroups, regimeMinN;

        Decision(
                String status,
                String reason,
                double hitRate,
                double wilsonLower,
                double wilsonUpper,
                double blockMin,
                int maxMiss,
                double brier,
                double ece,
                double coverage,
                int pairedN,
                double pairedDelta,
                double regimeMin,
                double fdrQ,
                boolean regimeSupported,
                int regimeGroups,
                int regimeMinN) {
            this.status = status;
            this.reason = reason;
            this.hitRate = hitRate;
            this.wilsonLower = wilsonLower;
            this.wilsonUpper = wilsonUpper;
            this.blockMin = blockMin;
            this.maxMiss = maxMiss;
            this.brier = brier;
            this.ece = ece;
            this.coverage = coverage;
            this.pairedN = pairedN;
            this.pairedDelta = pairedDelta;
            this.regimeMin = regimeMin;
            this.fdrQ = fdrQ;
            this.regimeSupported = regimeSupported;
            this.regimeGroups = Math.max(0, regimeGroups);
            this.regimeMinN = Math.max(0, regimeMinN);
        }
    }
}
