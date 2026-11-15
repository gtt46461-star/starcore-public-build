package com.ethk3.novaonepro;

/** Natural-coverage three-lane compiler. It never uses frequency debt rescue. */
public final class V49LanePolicyEngine {
    public static final String CORE = "CORE_GOLDEN";
    public static final String DYNAMIC = "DYNAMIC_STATE";
    public static final String PROBE = "CONTROLLED_PROBE";
    public static final String SHADOW = "SHADOW_ONLY";

    public Result compile(
            DbStore store,
            double actionP,
            double score,
            double agreement,
            double uncertainty,
            double switchRisk,
            double familyStrength,
            String family,
            String lifecycle) {
        double threshold =
                V49Repository.opportunityThreshold(
                        store,
                        AppConfig.V49_TARGET_COVERAGE,
                        AppConfig.V49_COVERAGE_WINDOW,
                        0.585d);
        boolean hardRisk =
                switchRisk > AppConfig.V49_MAX_SWITCH_RISK
                        || uncertainty > 0.88d
                        || actionP < AppConfig.BREAK_EVEN_PROBABILITY;
        if (hardRisk) return new Result(SHADOW, threshold, "V49_HARD_STRUCTURE_RISK", false);

        boolean goldenFamily =
                family != null
                        && !family.startsWith("UNSTABLE")
                        && !family.equals("REGIME_TRANSITION")
                        && familyStrength >= 0.58d;
        if (goldenFamily
                && actionP >= AppConfig.V49_CORE_MIN_ACTION_P
                && agreement >= AppConfig.V49_CORE_MIN_AGREEMENT
                && switchRisk <= 0.48d
                && score >= threshold * 0.94d) {
            return new Result(CORE, AppConfig.V49_CORE_MIN_ACTION_P, "V49_CORE_GOLDEN_PASS", true);
        }
        if (actionP >= AppConfig.V49_DYNAMIC_MIN_ACTION_P
                && agreement >= AppConfig.V49_DYNAMIC_MIN_AGREEMENT
                && switchRisk <= 0.62d
                && score >= threshold) {
            return new Result(
                    DYNAMIC, AppConfig.V49_DYNAMIC_MIN_ACTION_P, "V49_DYNAMIC_STATE_PASS", true);
        }
        int sinceProbe = V49Repository.issuesSinceProbe(store);
        if (actionP >= AppConfig.V49_PROBE_MIN_ACTION_P
                && agreement >= 0.40d
                && uncertainty <= 0.78d
                && score >= threshold - 0.045d
                && sinceProbe >= AppConfig.V49_PROBE_MIN_GAP) {
            return new Result(
                    PROBE, AppConfig.V49_PROBE_MIN_ACTION_P, "V49_CONTROLLED_PROBE_PASS", true);
        }
        return new Result(SHADOW, threshold, "V49_NATURAL_COVERAGE_NOT_SELECTED", false);
    }

    public static final class Result {
        public final String lane;
        public final double gate;
        public final String code;
        public final boolean eligible;

        Result(String l, double g, String c, boolean e) {
            lane = l;
            gate = g;
            code = c;
            eligible = e;
        }
    }
}
