package com.ethk3.novaonepro;

/**
 * V40 controlled real-probe policy.
 *
 * <p>This policy deliberately does not change honestP. It only decides whether a structurally valid
 * fixed-K3 candidate may enter the budgeted REAL_PROBE lane. Micro/calibration maturity is reported
 * and used for pacing context, but is not a hard prerequisite; otherwise the system can never
 * collect the real/OOS evidence needed to mature.
 */
public final class ProbeLanePolicyEngine {
    public static final String MODULE_VERSION = AppConfig.PROBE_LANE_POLICY_VERSION;

    public static final class Input {
        public double honestProbability;
        public double modelProbability;
        public double rankScore;
        public double coverage;
        public double thirdQuality;
        public double counterRisk;
        public double actionUtility;
        public double uncertaintyPenalty;
        public double currentFrequency;
        public int issuesSinceLastRealBet = 999;
        public int microSample;
        public int calibrationSample;
        public String calibrationStatus = "";
    }

    public static final class Result {
        public boolean allowed;
        public String code = "PROBE_NOT_EVALUATED";
        public String maturityStage = "COLD";
        public double requiredHonestP;
        public double requiredModelP;
        public double requiredRank;
        public double requiredCoverage;
        public double requiredThird;
        public double maxCounterRisk;
        public double requiredUtility;
        public double maxUncertainty;
        public double maxFrequency;
        public int requiredGap;

        public String summary() {
            return "ProbePolicy="
                    + (allowed ? "ALLOW" : "REJECT")
                    + "|code="
                    + safe(code)
                    + "|stage="
                    + safe(maturityStage)
                    + "|P>="
                    + requiredHonestP
                    + "|ModelP>="
                    + requiredModelP
                    + "|Rank>="
                    + requiredRank
                    + "|Cover>="
                    + requiredCoverage
                    + "|Third>="
                    + requiredThird
                    + "|Counter<="
                    + maxCounterRisk
                    + "|Utility>="
                    + requiredUtility
                    + "|Uncertainty<="
                    + maxUncertainty
                    + "|Freq<"
                    + maxFrequency
                    + "|Gap>="
                    + requiredGap;
        }
    }

    private ProbeLanePolicyEngine() {}

    public static Result evaluate(Input in) {
        Result r = new Result();
        if (in == null) {
            r.code = "PROBE_INPUT_NULL";
            return r;
        }

        boolean microReady = in.microSample >= AppConfig.MICRO_WALK_FORWARD_READY_SAMPLE;
        boolean microWarm = in.microSample >= AppConfig.MICRO_WALK_FORWARD_WARM_SAMPLE;
        boolean calReady =
                in.calibrationSample >= AppConfig.CALIBRATION_READY_TOTAL_SAMPLE
                        || MonotonicCalibrationEngine.isReadyStatus(in.calibrationStatus);
        boolean calWarm =
                in.calibrationSample >= AppConfig.CALIBRATION_WARM_TOTAL_SAMPLE
                        || MonotonicCalibrationEngine.isWarmStatus(in.calibrationStatus);
        if (microReady && calReady) r.maturityStage = "READY";
        else if (microWarm || calWarm) r.maturityStage = "WARM";
        else r.maturityStage = "COLD";

        r.requiredHonestP = AppConfig.AUTH_REAL_PROBE_MIN_HONEST_P;
        r.requiredModelP = AppConfig.AUTH_REAL_PROBE_MIN_MODEL_P;
        r.requiredRank = AppConfig.AUTH_REAL_PROBE_MIN_RANK_SCORE;
        r.requiredCoverage = AppConfig.AUTH_REAL_PROBE_MIN_COVERAGE;
        r.requiredThird = AppConfig.AUTH_REAL_PROBE_MIN_THIRD;
        r.maxCounterRisk = AppConfig.AUTH_REAL_PROBE_MAX_COUNTER_RISK;
        r.requiredUtility = AppConfig.AUTH_REAL_PROBE_MIN_UTILITY;
        r.maxUncertainty = AppConfig.AUTH_REAL_PROBE_MAX_UNCERTAINTY;
        r.maxFrequency = AppConfig.AUTH_REAL_PROBE_MAX_FREQUENCY;
        r.requiredGap = AppConfig.AUTH_REAL_PROBE_MIN_GAP;

        /* Maturity may make pacing slightly stricter, but never closes the lane. */
        if ("COLD".equals(r.maturityStage)) r.requiredGap = Math.max(r.requiredGap, 2);

        if (in.currentFrequency >= r.maxFrequency) return reject(r, "PROBE_FREQUENCY_CAP");
        if (in.issuesSinceLastRealBet < r.requiredGap) return reject(r, "PROBE_PACING_WAIT");
        if (in.honestProbability < r.requiredHonestP) return reject(r, "PROBE_HONEST_P_LOW");
        if (in.modelProbability < r.requiredModelP) return reject(r, "PROBE_MODEL_P_LOW");
        if (in.rankScore < r.requiredRank) return reject(r, "PROBE_RANK_LOW");
        if (in.coverage < r.requiredCoverage) return reject(r, "PROBE_COVERAGE_LOW");
        if (in.thirdQuality < r.requiredThird) return reject(r, "PROBE_THIRD_LOW");
        if (in.counterRisk > r.maxCounterRisk) return reject(r, "PROBE_COUNTER_RISK");
        if (in.actionUtility < r.requiredUtility) return reject(r, "PROBE_UTILITY_LOW");
        if (in.uncertaintyPenalty > r.maxUncertainty) return reject(r, "PROBE_UNCERTAINTY_HIGH");

        r.allowed = true;
        r.code = "PROBE_ALLOWED_" + r.maturityStage;
        return r;
    }

    private static Result reject(Result r, String code) {
        r.allowed = false;
        r.code = code;
        return r;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
