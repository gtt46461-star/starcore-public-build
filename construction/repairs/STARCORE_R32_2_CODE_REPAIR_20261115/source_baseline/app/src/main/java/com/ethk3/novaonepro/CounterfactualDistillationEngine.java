package com.ethk3.novaonepro;

/**
 * V42 false-strong / counterfactual distillation.
 *
 * <p>Settled shadow, promotion, ablation and why-not evidence are compressed into one explicit
 * false-strong risk. It cannot create BET and does not change finalP.
 */
public final class CounterfactualDistillationEngine {
    public static final String MODULE_VERSION = AppConfig.V42_COUNTERFACTUAL_DISTILL_VERSION;

    public static final class Result {
        public double support;
        public double falseStrongRisk;
        public boolean hardRisk;
        public String status = "WATCH";

        public String summary() {
            return "status="
                    + status
                    + "|support="
                    + round(support)
                    + "|falseStrongRisk="
                    + round(falseStrongRisk)
                    + "|hard="
                    + (hardRisk ? 1 : 0);
        }
    }

    private CounterfactualDistillationEngine() {}

    public static Result evaluate(Models.ComboCandidate c) {
        Result r = new Result();
        if (c == null) return r;

        double promotion =
                maxProbability(
                        c.promotionWilsonLowerBound,
                        c.promotionClusterHitRate,
                        c.similarWindowHitRate);
        double posterior =
                maxProbability(
                        c.posteriorWilsonLower,
                        c.posteriorRecentRate,
                        c.dynamicClusterRecentHitRate);
        double teacher =
                maxProbability(
                        c.teacherNumberHitRate,
                        c.teacherMatrixRecentHitRate,
                        c.forwardReasonHitRate);
        r.support =
                clamp01(
                        0.38d * probabilityLift(promotion)
                                + 0.34d * probabilityLift(posterior)
                                + 0.28d * probabilityLift(teacher));

        double whyNot = clamp01(c.counterfactualWhyNotRisk);
        double counter = max01(c.counterRisk, c.counterEvidenceScore, c.evidenceRisk);
        double falseStrong = max01(c.falseStrongRisk, c.probabilityFalseHighRisk, c.k3CoverageRisk);
        double contradiction = max01(c.contradictionScore, c.comboConflict, c.conflictLevel);
        double ablation = clamp01(c.causeAblationFalseCauseCount / 5.0d);

        r.falseStrongRisk =
                clamp01(
                        0.24d * whyNot
                                + 0.24d * counter
                                + 0.22d * falseStrong
                                + 0.18d * contradiction
                                + 0.12d * ablation
                                - 0.16d * r.support);
        r.hardRisk =
                r.falseStrongRisk >= AppConfig.V42_DISTILL_HARD_RISK
                        || c.falseHighPFlag == 1
                        || c.probabilityFalseHighPFlag == 1;
        if (r.hardRisk) r.status = "HARD_RISK";
        else if (r.falseStrongRisk >= AppConfig.V42_DISTILL_SOFT_RISK) r.status = "SOFT_RISK";
        else if (r.support >= AppConfig.V42_DISTILL_READY_SUPPORT) r.status = "READY";
        return r;
    }

    private static double maxProbability(double a, double b, double c) {
        return clamp01(Math.max(a, Math.max(b, c)));
    }

    private static double probabilityLift(double p) {
        if (p <= 0.0d) return 0.0d;
        return clamp01((p - 0.25d) / 0.20d);
    }

    private static double max01(double a, double b, double c) {
        return clamp01(Math.max(a, Math.max(b, c)));
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        if (v > 1.5d) v /= 100.0d;
        return v < 0.0d ? 0.0d : (v > 1.0d ? 1.0d : v);
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }
}
