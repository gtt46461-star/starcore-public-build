package com.ethk3.novaonepro;

import java.security.MessageDigest;
import java.util.HashMap;

/**
 * V45 one-candidate/one-compile action contract.
 *
 * <p>LanePolicyCompiler publishes exactly one immutable contract for a locked candidate.
 * DecisionGate may select it; DecisionAuthorizationEngine may only verify and sign it.
 */
public final class CompiledActionContract {
    public static final String MODULE_VERSION = "V46_COMPILED_EVENT_LCB_CONTRACT_20260721_46072130";

    private static final HashMap<String, CompiledActionContract> CACHE =
            new HashMap<String, CompiledActionContract>();
    private static final HashMap<String, Integer> COMPILE_COUNT = new HashMap<String, Integer>();

    public final String candidateKey;
    public final String targetIssue;
    public final String lane;
    public final String code;
    public final boolean eligible;
    public final double probability;
    public final double actionLcb;
    public final double uncertainty;
    public final double gate;
    public final double edge;
    public final double familyLcb;
    public final double acceptScore;
    public final double coverage;
    public final double thirdQuality;
    public final int supportVotes;
    public final int qualityVotes;
    public final String primaryReject;
    public final String secondaryReject;
    public final String tertiaryReject;
    public final String probabilityLockHash;
    public final String contractHash;
    public final long compiledAt;
    public final int compileCount;
    private boolean authorizationAttempted;

    public CompiledActionContract(
            Models.ComboCandidate c,
            String lane,
            String code,
            boolean eligible,
            double probability,
            double gate,
            double familyLcb,
            double acceptScore,
            double coverage,
            double thirdQuality,
            int supportVotes,
            int qualityVotes,
            String primaryReject,
            String secondaryReject,
            String tertiaryReject,
            int compileCount) {
        this.candidateKey = PredictionEnvelope.candidateKey(c);
        this.targetIssue = c == null ? "" : safe(c.targetIssue);
        this.lane = safe(lane);
        this.code = safe(code);
        this.eligible = eligible;
        this.probability = clamp01(probability);
        this.actionLcb = c == null ? 0.0d : clamp01(c.v46ActionLcb);
        this.uncertainty = c == null ? 1.0d : clamp01(c.v46ProbabilityUncertainty);
        this.gate = clamp01(gate);
        this.edge = this.probability - this.gate;
        this.familyLcb = clamp01(familyLcb);
        this.acceptScore = clamp01(acceptScore);
        this.coverage = clamp01(coverage);
        this.thirdQuality = clamp01(thirdQuality);
        this.supportVotes = Math.max(0, supportVotes);
        this.qualityVotes = Math.max(0, qualityVotes);
        this.primaryReject = safe(primaryReject);
        this.secondaryReject = safe(secondaryReject);
        this.tertiaryReject = safe(tertiaryReject);
        this.probabilityLockHash = c == null ? "" : safe(c.v43ProbabilityLockHash);
        this.compiledAt = System.currentTimeMillis();
        this.compileCount = Math.max(1, compileCount);
        this.contractHash = hash(serialized());
        this.authorizationAttempted = false;
    }

    public static synchronized CompiledActionContract publish(CompiledActionContract c) {
        if (c == null || c.candidateKey.length() == 0) return c;
        CompiledActionContract old = CACHE.get(c.candidateKey);
        if (old != null) return old;
        CACHE.put(c.candidateKey, c);
        COMPILE_COUNT.put(c.candidateKey, Integer.valueOf(c.compileCount));
        trim();
        return c;
    }

    public static synchronized CompiledActionContract get(Models.ComboCandidate c) {
        return c == null ? null : CACHE.get(PredictionEnvelope.candidateKey(c));
    }

    public static synchronized CompiledActionContract getEligible(
            Models.ComboCandidate c, String requiredLane) {
        CompiledActionContract x = get(c);
        if (x == null || !x.eligible) return null;
        String required = LanePolicyCompiler.normalizeLane(requiredLane);
        if (required.length() > 0 && !required.equals(x.lane)) return null;
        return x;
    }

    public static synchronized int nextCompileCount(Models.ComboCandidate c) {
        String key = PredictionEnvelope.candidateKey(c);
        Integer old = COMPILE_COUNT.get(key);
        int n = old == null ? 1 : old.intValue() + 1;
        COMPILE_COUNT.put(key, Integer.valueOf(n));
        return n;
    }

    public static synchronized boolean markAuthorizationAttempt(CompiledActionContract c) {
        if (c == null || c.authorizationAttempted) return false;
        c.authorizationAttempted = true;
        return true;
    }

    public static synchronized void clearIssue(String targetIssue) {
        String issue = safe(targetIssue);
        java.util.ArrayList<String> remove = new java.util.ArrayList<String>();
        for (String key : CACHE.keySet()) {
            CompiledActionContract c = CACHE.get(key);
            if (c != null && (issue.equals(c.targetIssue)||(TargetIssueResolver.isValid(issue)
                    &&TargetIssueResolver.isValid(c.targetIssue)&&c.targetIssue.compareTo(issue)<0))) remove.add(key);
        }
        for (int i = 0; i < remove.size(); i++) {
            CACHE.remove(remove.get(i));
            COMPILE_COUNT.remove(remove.get(i));
        }
    }

    public boolean matches(Models.ComboCandidate c) {
        if (c == null) return false;
        if (!candidateKey.equals(PredictionEnvelope.candidateKey(c))) return false;
        if (!probabilityLockHash.equals(safe(c.v43ProbabilityLockHash))) return false;
        double p = PredictionEnvelope.readOnly(c).canonicalProbability;
        PredictionEnvelope e = PredictionEnvelope.readOnly(c);
        return Math.abs(p - probability) <= 0.000000001d
                && e != null
                && Math.abs(e.actionLcb - actionLcb) <= 0.000000001d
                && Math.abs(e.uncertainty - uncertainty) <= 0.000000001d
                && contractHash.equals(hash(serialized()));
    }

    public String serialized() {
        return candidateKey
                + "|"
                + targetIssue
                + "|"
                + lane
                + "|"
                + code
                + "|"
                + (eligible ? 1 : 0)
                + "|"
                + number(probability)
                + "|"
                + number(actionLcb)
                + "|"
                + number(uncertainty)
                + "|"
                + number(gate)
                + "|"
                + number(familyLcb)
                + "|"
                + number(acceptScore)
                + "|"
                + number(coverage)
                + "|"
                + number(thirdQuality)
                + "|"
                + supportVotes
                + "|"
                + qualityVotes
                + "|"
                + primaryReject
                + "|"
                + secondaryReject
                + "|"
                + tertiaryReject
                + "|"
                + probabilityLockHash
                + "|"
                + compileCount;
    }

    public String summary() {
        return "candidate="
                + candidateKey
                + "|lane="
                + lane
                + "|eligible="
                + (eligible ? 1 : 0)
                + "|code="
                + code
                + "|EventP="
                + round(probability)
                + "|ActionLCB="
                + round(actionLcb)
                + "|U="
                + round(uncertainty)
                + "|gate="
                + round(gate)
                + "|edge="
                + round(edge)
                + "|accept="
                + round(acceptScore)
                + "|votes="
                + supportVotes
                + "/"
                + qualityVotes
                + "|compileCount="
                + compileCount
                + "|hash="
                + shortHash(contractHash);
    }

    private static void trim() {
        if (CACHE.size() <= 256) return;
        String oldestKey = null;
        long oldest = Long.MAX_VALUE;
        for (String key : CACHE.keySet()) {
            CompiledActionContract c = CACHE.get(key);
            if (c != null && c.compiledAt < oldest) {
                oldest = c.compiledAt;
                oldestKey = key;
            }
        }
        if (oldestKey != null) {
            CACHE.remove(oldestKey);
            COMPILE_COUNT.remove(oldestKey);
        }
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        if (v > 1.5d) v /= 100.0d;
        return v < 0.0d ? 0.0d : (v > 1.0d ? 1.0d : v);
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
