package com.ethk3.novaonepro;

import java.security.MessageDigest;

/** Immutable V46 EventP + uncertainty + ActionLCB envelope. */
public final class PredictionEnvelope {
    public static final String MODULE_VERSION = "V46_PREDICTION_ENVELOPE_20260721_46072130";

    public final String candidateKey;
    public final String targetIssue;
    public final int position;
    public final int k3Mask;
    public final double modelProbability;
    public final double calibratedProbability;
    public final double safeProbability;
    public final double canonicalProbability;
    public final double uncertainty;
    public final double actionLcb;
    public final double gate;
    public final double edge;
    public final String probabilityVersion;
    public final String featureHash;
    public final String envelopeHash;

    private PredictionEnvelope(
            String candidateKey,
            String targetIssue,
            int position,
            int k3Mask,
            double modelProbability,
            double calibratedProbability,
            double canonicalProbability,
            double uncertainty,
            double actionLcb,
            double gate,
            String probabilityVersion,
            String featureHash) {
        this.candidateKey = safe(candidateKey);
        this.targetIssue = safe(targetIssue);
        this.position = position;
        this.k3Mask = k3Mask;
        this.modelProbability = clamp01(modelProbability);
        this.calibratedProbability = clamp01(calibratedProbability);
        this.safeProbability = clamp01(canonicalProbability); // legacy alias
        this.canonicalProbability = clamp01(canonicalProbability);
        this.uncertainty = clamp01(uncertainty);
        this.actionLcb = clamp01(actionLcb);
        this.gate = clamp01(gate);
        this.edge = this.canonicalProbability - this.gate;
        this.probabilityVersion = safe(probabilityVersion);
        this.featureHash = safe(featureHash);
        this.envelopeHash = hash(serialized());
    }

    /** Finalize after every probability/risk-affecting module and immediately before Lane. */
    public static PredictionEnvelope finalizeAndApply(Models.ComboCandidate c) {
        if (c == null) return null;
        double model = firstProbability(c.modelP, c.rawP, AppConfig.RANDOM_K3_BASELINE);
        double calibrated =
                firstProbability(c.calibratedHitProbability, c.calibratedP, c.finalP, model);
        double eventP = firstProbability(c.v46EventProbability, calibrated, model);
        double uncertainty =
                valid01(c.v46ProbabilityUncertainty)
                        ? c.v46ProbabilityUncertainty
                        : Math.max(0.0d, c.actionUncertaintyPenalty);
        double actionLcb =
                firstProbability(
                        c.v46ActionLcb,
                        Math.max(0.001d, eventP - AppConfig.V46_MAX_ACTION_BOUND_PENALTY),
                        eventP);
        double gate =
                firstProbability(
                        c.dynamicGate, c.riskAdjustedGate, c.gate, AppConfig.DEFAULT_DYNAMIC_GATE);
        String key = candidateKey(c);
        String version = safe(c.probabilityPipelineVersion);
        if (version.length() == 0) version = safe(c.calibrationVersion);
        if (version.length() == 0) version = MODULE_VERSION;
        String featureHash =
                hash(
                        safe(c.candidateHash)
                                + "|"
                                + safe(c.snapshotMaxIssue)
                                + "|"
                                + safe(c.featureSchemaVersion)
                                + "|"
                                + key);
        PredictionEnvelope out =
                new PredictionEnvelope(
                        key,
                        c.targetIssue,
                        c.position,
                        c.k3Mask,
                        model,
                        calibrated,
                        eventP,
                        uncertainty,
                        actionLcb,
                        gate,
                        version,
                        featureHash);

        c.v46EventProbability = out.canonicalProbability;
        c.v46ProbabilityUncertainty = out.uncertainty;
        c.v46ActionLcb = out.actionLcb;
        c.v46ExpectedValue = AppConfig.ODDS * out.canonicalProbability - AppConfig.BET_COST;
        c.calibratedHitProbability = out.canonicalProbability;
        c.calibratedP = out.canonicalProbability;
        c.finalP = out.canonicalProbability;
        c.honestProbability = out.canonicalProbability;
        c.v43SafeProbability = out.actionLcb;
        c.edge = out.edge;
        c.v43PipelineStage = "V46_EVENT_ACTION_BOUND_READY";
        c.v43StrategyVersion = MODULE_VERSION;
        c.v46ContractHash = out.envelopeHash;
        c.v46Version = AppConfig.V46_VERSION;
        c.reasonSummary =
                append(
                        c.reasonSummary,
                        "V46合同 EventP="
                                + round(out.canonicalProbability)
                                + "，ActionLCB="
                                + round(out.actionLcb)
                                + "，Uncertainty="
                                + round(out.uncertainty)
                                + "，Model="
                                + round(out.modelProbability));
        return out;
    }

    public static PredictionEnvelope readOnly(Models.ComboCandidate c) {
        if (c == null) return null;
        double model = firstProbability(c.modelP, c.rawP, AppConfig.RANDOM_K3_BASELINE);
        double eventP =
                firstProbability(
                        c.v46EventProbability,
                        c.calibratedHitProbability,
                        c.finalP,
                        c.modelP,
                        AppConfig.RANDOM_K3_BASELINE);
        double calibrated =
                firstProbability(c.calibratedHitProbability, c.calibratedP, eventP, model);
        double uncertainty =
                valid01(c.v46ProbabilityUncertainty)
                        ? c.v46ProbabilityUncertainty
                        : Math.max(0.0d, c.actionUncertaintyPenalty);
        double actionLcb =
                firstProbability(
                        c.v46ActionLcb,
                        Math.max(0.001d, eventP - AppConfig.V46_MAX_ACTION_BOUND_PENALTY),
                        eventP);
        double gate =
                firstProbability(
                        c.dynamicGate, c.riskAdjustedGate, c.gate, AppConfig.DEFAULT_DYNAMIC_GATE);
        return new PredictionEnvelope(
                candidateKey(c),
                c.targetIssue,
                c.position,
                c.k3Mask,
                model,
                calibrated,
                eventP,
                uncertainty,
                actionLcb,
                gate,
                safe(c.probabilityPipelineVersion),
                hash(
                        safe(c.candidateHash)
                                + "|"
                                + safe(c.snapshotMaxIssue)
                                + "|"
                                + safe(c.featureSchemaVersion)
                                + "|"
                                + candidateKey(c)));
    }

    public boolean matches(Models.ComboCandidate c) {
        if (c == null) return false;
        PredictionEnvelope now = readOnly(c);
        return now != null
                && candidateKey.equals(now.candidateKey)
                && close(canonicalProbability, now.canonicalProbability)
                && close(actionLcb, now.actionLcb)
                && close(uncertainty, now.uncertainty)
                && envelopeHash.equals(now.envelopeHash);
    }

    public String serialized() {
        return candidateKey
                + "|"
                + targetIssue
                + "|"
                + position
                + "|"
                + k3Mask
                + "|"
                + number(modelProbability)
                + "|"
                + number(calibratedProbability)
                + "|"
                + number(canonicalProbability)
                + "|"
                + number(uncertainty)
                + "|"
                + number(actionLcb)
                + "|"
                + number(gate)
                + "|"
                + probabilityVersion
                + "|"
                + featureHash;
    }

    public String summary() {
        return "candidate="
                + candidateKey
                + "|EventP="
                + round(canonicalProbability)
                + "|ActionLCB="
                + round(actionLcb)
                + "|U="
                + round(uncertainty)
                + "|model="
                + round(modelProbability)
                + "|gate="
                + round(gate)
                + "|edge="
                + round(edge)
                + "|hash="
                + shortHash(envelopeHash);
    }

    public static String candidateKey(Models.ComboCandidate c) {
        if (c == null) return "";
        return safe(c.targetIssue)
                + "|"
                + c.position
                + "|"
                + c.n1
                + "|"
                + c.n2
                + "|"
                + c.n3
                + "|"
                + c.k3Mask;
    }

    private static double firstProbability(
            double a, double b, double c, double d, double fallback) {
        if (valid(a)) return clamp01(a);
        if (valid(b)) return clamp01(b);
        if (valid(c)) return clamp01(c);
        if (valid(d)) return clamp01(d);
        return clamp01(fallback);
    }

    private static double firstProbability(double a, double b, double c, double fallback) {
        if (valid(a)) return clamp01(a);
        if (valid(b)) return clamp01(b);
        if (valid(c)) return clamp01(c);
        return clamp01(fallback);
    }

    private static double firstProbability(double a, double b, double fallback) {
        if (valid(a)) return clamp01(a);
        if (valid(b)) return clamp01(b);
        return clamp01(fallback);
    }

    private static boolean valid(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v) && v > 0.0d && v < 1.0d;
    }

    private static boolean valid01(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v) && v >= 0.0d && v <= 1.0d;
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) <= 0.000000001d;
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        if (v > 1.5d) v /= 100.0d;
        return v < 0.0d ? 0.0d : (v > 1.0d ? 1.0d : v);
    }

    private static String append(String a, String b) {
        if (safe(a).length() == 0) return safe(b);
        if (safe(b).length() == 0 || a.indexOf(b) >= 0) return safe(a);
        return a + "｜" + b;
    }

    private static String number(double v) {
        return String.valueOf(Math.round(v * 1000000000.0d));
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }

    private static String shortHash(String s) {
        return safe(s).length() <= 12 ? safe(s) : s.substring(0, 12);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] data = md.digest(safe(raw).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                String h = Integer.toHexString(data[i] & 0xff);
                if (h.length() == 1) sb.append('0');
                sb.append(h);
            }
            return sb.toString();
        } catch (Throwable e) {
            return Integer.toHexString(safe(raw).hashCode());
        }
    }
}
