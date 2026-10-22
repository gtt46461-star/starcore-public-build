package com.ethk3.novaonepro;

/**
 * V42 finite regime registry.
 *
 * <p>Trend, break, recovery and drift are converted into one of four reusable coarse regimes. The
 * engine is a feature producer only; it never creates BET, never changes probability and never
 * overrides DecisionGateEngine.
 */
public final class RegimeRegistryEngine {
    public static final String MODULE_VERSION = AppConfig.V42_REGIME_REGISTRY_VERSION;
    public static final String CONFIRMED_TREND = "CONFIRMED_TREND";
    public static final String BREAK_RECOVERY = "BREAK_RECOVERY";
    public static final String TRANSITION_PROBE = "TRANSITION_PROBE";
    public static final String CHAOS_REJECT = "CHAOS_REJECT";

    public static final class Result {
        public String family = TRANSITION_PROBE;
        public String key = "";
        public double trendLife;
        public double driftRisk;
        public double breakDepth;
        public double recovery;
        public double consistency;
        public double quality;
        public double risk;
        public boolean hardChaos;

        public String summary() {
            return "family="
                    + family
                    + "|quality="
                    + round(quality)
                    + "|risk="
                    + round(risk)
                    + "|TL="
                    + round(trendLife)
                    + "|drift="
                    + round(driftRisk)
                    + "|break="
                    + round(breakDepth)
                    + "|recovery="
                    + round(recovery)
                    + "|consistency="
                    + round(consistency)
                    + "|key="
                    + key;
        }
    }

    private RegimeRegistryEngine() {}

    public static Result evaluate(Models.ComboCandidate c) {
        Result r = new Result();
        if (c == null) return r;

        r.trendLife = score01(c.trendLife);
        r.driftRisk = score01(c.driftRisk);
        r.breakDepth = score01(c.breakDepth);
        r.recovery = score01(c.recoveryScore);

        double state =
                max01(
                        Math.max(c.stateConfidence, c.v20SupportScore),
                        c.evidenceForestConfidence,
                        c.historicalPriorConfidence);
        double recent =
                probabilitySupport(
                        c.similarWindowHitRate,
                        c.posteriorRecentRate,
                        c.dynamicClusterRecentHitRate);
        double teacher =
                max01(
                        c.teacherSampleReliability,
                        c.teacherRecentValidity,
                        c.teacherMatrixConfidence);
        r.consistency = clamp01(0.44d * state + 0.30d * recent + 0.26d * teacher);

        double counter = max01(c.counterRisk, c.counterEvidenceScore, c.evidenceRisk);
        double conflict = max01(c.comboConflict, c.conflictLevel, c.falseStrongRisk);
        double deep = score01(c.deepFeatureRisk);
        r.risk =
                clamp01(
                        0.34d * r.driftRisk
                                + 0.20d * r.breakDepth
                                + 0.18d * counter
                                + 0.16d * conflict
                                + 0.12d * deep);

        r.hardChaos =
                r.driftRisk >= AppConfig.V42_REGIME_CHAOS_DRIFT
                        || counter >= AppConfig.V42_REGIME_CHAOS_COUNTER
                        || conflict >= AppConfig.V42_REGIME_CHAOS_CONFLICT;

        if (r.hardChaos) {
            r.family = CHAOS_REJECT;
        } else if (r.trendLife >= AppConfig.V42_REGIME_TREND_MIN
                && r.driftRisk <= AppConfig.V42_REGIME_TREND_MAX_DRIFT
                && r.consistency >= AppConfig.V42_REGIME_TREND_MIN_CONSISTENCY) {
            r.family = CONFIRMED_TREND;
        } else if (r.breakDepth >= AppConfig.V42_REGIME_RECOVERY_MIN_BREAK
                && r.recovery >= AppConfig.V42_REGIME_RECOVERY_MIN_SCORE
                && r.driftRisk <= AppConfig.V42_REGIME_RECOVERY_MAX_DRIFT) {
            r.family = BREAK_RECOVERY;
        } else {
            r.family = TRANSITION_PROBE;
        }

        double trendTerm =
                CONFIRMED_TREND.equals(r.family)
                        ? r.trendLife
                        : (BREAK_RECOVERY.equals(r.family)
                                ? r.recovery
                                : 0.50d * r.trendLife + 0.50d * r.recovery);
        r.quality =
                clamp01(
                        0.35d * trendTerm
                                + 0.32d * r.consistency
                                + 0.18d * (1.0d - r.driftRisk)
                                + 0.15d * (1.0d - r.risk));
        if (CHAOS_REJECT.equals(r.family)) r.quality *= 0.35d;

        r.key =
                r.family
                        + "|P"
                        + c.position
                        + "|TL"
                        + bucket(r.trendLife)
                        + "|DR"
                        + bucket(r.driftRisk)
                        + "|RC"
                        + bucket(r.recovery)
                        + "|CS"
                        + bucket(r.consistency);
        return r;
    }

    private static double probabilitySupport(double a, double b, double c) {
        double v = Math.max(a, Math.max(b, c));
        if (v <= 0.0d) return 0.30d;
        return clamp01((v - 0.20d) / 0.25d);
    }

    private static double max01(double a, double b, double c) {
        return clamp01(Math.max(a, Math.max(b, c)));
    }

    private static double score01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0d) return 0.0d;
        return clamp01(v > 1.5d ? v / 100.0d : v);
    }

    private static String bucket(double v) {
        int b = (int) Math.floor(clamp01(v) * 5.0d);
        if (b > 4) b = 4;
        return String.valueOf(b);
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        return v < 0.0d ? 0.0d : (v > 1.0d ? 1.0d : v);
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }
}
