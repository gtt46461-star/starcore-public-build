package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy Bayesian bridge retained for compatibility. V22 deliberately starts from the latest
 * final/raw probability instead of blindly preferring a default/low-sample calibrated value. The
 * active final probability is produced once by AdaptiveExpertConsensusEngine. It never changes a
 * probability merely to satisfy a frequency target.
 */
public final class BayesianEvidenceFusionEngine {
    public static final String MODULE_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final double BASELINE = 0.30d;

    public ArrayList<Models.ComboCandidate> apply(List<Models.ComboCandidate> input) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();
        if (input == null) return out;
        for (int i = 0; i < input.size(); i++) {
            Models.ComboCandidate c = input.get(i);
            if (c == null) continue;
            fuse(c);
            out.add(c);
        }
        return out;
    }

    public void fuse(Models.ComboCandidate c) {
        double base = firstPositive(c.finalP, c.rawP, c.calibratedP, c.modelP, BASELINE);
        base = clamp(base, 0.15d, 0.55d);
        double prior = c.historicalPriorP > 0.0d ? c.historicalPriorP : BASELINE;
        double priorConfidence = clamp01(c.historicalPriorConfidence);
        double priorWeight = Math.min(0.32d, priorConfidence * 0.28d);
        double logit = logit(base);
        logit += priorWeight * (logit(prior) - logit(BASELINE));
        double forestP = c.evidenceForestP > 0.0d ? c.evidenceForestP : BASELINE;
        double forestWeight =
                c.evidenceForestSample >= 6
                        ? Math.min(0.12d, clamp01(c.evidenceForestConfidence) * 0.11d)
                        : 0.0d;
        logit += forestWeight * (logit(forestP) - logit(BASELINE));

        double teacherWeight = 0.0d;
        double teacherP = BASELINE;
        int teacherSample =
                Math.max(
                        c.teacherDomainRealSample,
                        Math.max(c.teacherDomainShadowSample, c.teacherMatrixSample));
        if (teacherSample >= 30 && c.teacherMatrixHitRate > 0.0d) {
            teacherP = clamp(c.teacherMatrixHitRate, 0.12d, 0.55d);
            teacherWeight = Math.min(0.18d, clamp01(c.teacherMatrixConfidence) * 0.16d);
        }

        logit += teacherWeight * (logit(teacherP) - logit(BASELINE));

        /* Settled dynamic evidence only; WATCH/UNKNOWN/no sample is neutral. */
        int dynamicSample = Math.max(c.dynamicRealSample, c.dynamicShadowSample);
        double dynamicWeight = 0.0d;
        double dynamicP = BASELINE;
        if (dynamicSample >= 30 && c.dynamicClusterHitRate > 0.0d) {
            dynamicP = clamp(c.dynamicClusterHitRate, 0.12d, 0.55d);
            dynamicWeight = Math.min(0.14d, Math.log(1.0d + dynamicSample) / 40.0d);
            logit += dynamicWeight * (logit(dynamicP) - logit(BASELINE));
        }

        double fused = logistic(logit);
        /* Honest bounded adjustment.  This prevents a tiny prior cell from dominating. */
        fused = clamp(fused, base - 0.018d, base + 0.032d);
        fused = clamp(fused, 0.15d, 0.55d);

        c.basePBeforeFusion = base;
        c.finalPAfterFusion = fused;
        c.fusionWeightBase = 1.0d;
        c.fusionWeightPrior = priorWeight;
        c.fusionWeightTeacher = teacherWeight;
        c.fusionWeightDynamic = dynamicWeight;
        c.fusionWeightForest = forestWeight;
        c.fusionSummary =
                "贝叶斯证据融合：Base="
                        + pct(base)
                        + "｜历史先验="
                        + pct(prior)
                        + "×"
                        + pct(priorWeight)
                        + "｜证据森林="
                        + pct(forestP)
                        + "×"
                        + pct(forestWeight)
                        + "｜教师="
                        + pct(teacherP)
                        + "×"
                        + pct(teacherWeight)
                        + "｜动态="
                        + pct(dynamicP)
                        + "×"
                        + pct(dynamicWeight)
                        + "｜Final="
                        + pct(fused)
                        + "｜无样本严格中性，不为补频虚抬概率";
        c.finalP = fused;
        c.calibratedP = fused;
        c.edge = c.finalP - c.dynamicGate;
        c.finalPAfterDiagnosis = fused;
        c.probabilityActionAdvice = append(c.probabilityActionAdvice, c.fusionSummary);
        c.reasonSummary = append(c.reasonSummary, c.fusionSummary);
    }

    private static double firstPositive(double a, double b, double c, double d, double fallback) {
        if (a > 0.0d) return a;
        if (b > 0.0d) return b;
        if (c > 0.0d) return c;
        if (d > 0.0d) return d;
        return fallback;
    }

    private static double logit(double p) {
        p = clamp(p, 0.0001d, 0.9999d);
        return Math.log(p / (1.0d - p));
    }

    private static double logistic(double x) {
        return 1.0d / (1.0d + Math.exp(-x));
    }

    private static double clamp01(double v) {
        return clamp(v, 0.0d, 1.0d);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static String pct(double p) {
        return String.format(java.util.Locale.US, "%.1f%%", p * 100.0d);
    }

    private static String append(String a, String b) {
        if (a == null || a.length() == 0) return b == null ? "" : b;
        if (b == null || b.length() == 0) return a;
        return a + "；" + b;
    }
}
