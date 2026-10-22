package com.ethk3.novaonepro;

/**
 * V42 compact association-graph projection.
 *
 * <p>The existing position, number, transition, role and teacher matrices are projected into one
 * stable structural score. It is deliberately bounded and cannot write probability or authorize
 * BET.
 */
public final class AssociationGraphEngine {
    public static final String MODULE_VERSION = AppConfig.V42_ASSOCIATION_GRAPH_VERSION;

    public static final class Result {
        public double positionSupport;
        public double numberSupport;
        public double transitionSupport;
        public double roleSupport;
        public double conflictRisk;
        public double score;
        public String status = "NEUTRAL";

        public String summary() {
            return "status="
                    + status
                    + "|score="
                    + round(score)
                    + "|position="
                    + round(positionSupport)
                    + "|number="
                    + round(numberSupport)
                    + "|transition="
                    + round(transitionSupport)
                    + "|role="
                    + round(roleSupport)
                    + "|conflict="
                    + round(conflictRisk);
        }
    }

    private AssociationGraphEngine() {}

    public static Result evaluate(Models.ComboCandidate c) {
        Result r = new Result();
        if (c == null) return r;

        r.positionSupport =
                max01(
                        c.positionNeedStrength,
                        c.positionNeedScore,
                        c.deepPositionScore,
                        c.teacherMatrixPositionScore);
        r.numberSupport =
                max01(
                        c.numberSupplyStrength,
                        c.teacherNumberScore,
                        c.teacherMatrixNumberScore,
                        c.stateNumberProfileScore);
        r.transitionSupport =
                max01(
                        c.deepTransitionScore,
                        c.adaptiveCauseScore,
                        c.similarWindowHitRate,
                        c.dynamicClusterRecentHitRate);
        r.roleSupport =
                max01(
                        c.comboRoleDiversity,
                        c.balanceScore,
                        c.teacherMatrixComboScore,
                        c.needSupplyRoleDiversity);
        r.conflictRisk =
                max01(c.comboConflict, c.conflictLevel, c.numberSupplyRisk, c.needSupplyRisk);

        r.score =
                clamp01(
                        0.27d * r.positionSupport
                                + 0.27d * r.numberSupport
                                + 0.20d * r.transitionSupport
                                + 0.18d * r.roleSupport
                                + 0.08d * (1.0d - r.conflictRisk));
        if (r.conflictRisk >= AppConfig.V42_ASSOCIATION_HARD_CONFLICT) {
            r.status = "RISK";
        } else if (r.score >= AppConfig.V42_ASSOCIATION_STRONG) {
            r.status = "STRONG";
        } else if (r.score >= AppConfig.V42_ASSOCIATION_READY) {
            r.status = "READY";
        }
        return r;
    }

    private static double max01(double a, double b, double c, double d) {
        return clamp01(Math.max(Math.max(a, b), Math.max(c, d)));
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
