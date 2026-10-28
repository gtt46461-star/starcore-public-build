package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R27-041/042 certificate quality gates (docs/09 9.3) and the mainline choice rule. The numeric gates are the
 * current ones, not tightened and not relaxed: hit rate ≥ 34% on FUTURE acts, ≥ 250 settled FUTURE acts, stratified
 * ECE ≤ 0.04, worst 720-block hit rate ≥ 26%, longest losing streak ≤ 6 inside the registered window, semantic ABI
 * equivalent, sequential test passed, and paired-better than the same-period negative controls.
 * This class never authorizes REAL: DecisionGateEngine remains the only REAL owner, SettlementEngine the only
 * settlement owner; without an ACTIVE certificate the production path stays PRODUCTION_FALLBACK|NO_VALID_CERTIFICATE. */
public final class CertificateGateR27 {
    private CertificateGateR27() {}
    public static final double MIN_HR = 0.34, MAX_ECE = 0.04, MIN_WORST_BLOCK = 0.26; public static final int MIN_ACTS = 250, MAX_LOSS_STREAK = 6;
    public static final long VALID_MS = 24L * 3600_000L;

    public static final class Evidence {
        public String policyHash; public String sequentialState; public long acts, hits; public double ece = Double.NaN, worstBlock = Double.NaN;
        public int maxLosingStreak; public boolean abiEquivalent; public double pairedLcbVsI00 = Double.NaN, pairedLcbVsNull = Double.NaN;
        public double logE; public double calibrationQuality = Double.NaN;
    }
    public static final class Verdict {
        public final boolean active; public final Map<String, Object> gates; public final List<String> failing;
        Verdict(boolean a, Map<String, Object> g, List<String> f) { active = a; gates = g; failing = f; }
    }

    public static Verdict evaluate(Evidence e) {
        Map<String, Object> g = new LinkedHashMap<>(); List<String> fail = new ArrayList<>();
        double hr = e.acts == 0 ? Double.NaN : e.hits / (double) e.acts;
        gate(g, fail, "sequentialTest", SequentialQualificationR27.QUALIFIED.equals(e.sequentialState), e.sequentialState);
        gate(g, fail, "hitRate>=0.34", Double.isFinite(hr) && hr >= MIN_HR, hr);
        gate(g, fail, "futureActs>=250", e.acts >= MIN_ACTS, e.acts);
        gate(g, fail, "ece<=0.04", Double.isFinite(e.ece) && e.ece <= MAX_ECE, e.ece);
        gate(g, fail, "worst720Block>=0.26", Double.isFinite(e.worstBlock) && e.worstBlock >= MIN_WORST_BLOCK, e.worstBlock);
        gate(g, fail, "losingStreak<=6", e.maxLosingStreak <= MAX_LOSS_STREAK, e.maxLosingStreak);
        gate(g, fail, "semanticAbiEquivalent", e.abiEquivalent, e.abiEquivalent);
        gate(g, fail, "betterThanI00", Double.isFinite(e.pairedLcbVsI00) && e.pairedLcbVsI00 > 0, e.pairedLcbVsI00);
        gate(g, fail, "betterThanShuffledControl", Double.isFinite(e.pairedLcbVsNull) && e.pairedLcbVsNull > 0, e.pairedLcbVsNull);
        return new Verdict(fail.isEmpty(), g, fail);
    }
    static void gate(Map<String, Object> g, List<String> fail, String name, boolean ok, Object value) {
        Map<String, Object> x = new LinkedHashMap<>(); x.put("pass", ok); x.put("value", value instanceof Double && !Double.isFinite((Double) value) ? null : value); g.put(name, x);
        if (!ok) fail.add(name);
    }

    /** Mainline choice among ACTIVE certificates: maximum logE × calibration quality (docs/09 9.3-3); null = keep fallback. */
    public static String choose(List<Evidence> active) {
        String best = null; double score = Double.NEGATIVE_INFINITY;
        for (Evidence e : active) {
            double q = Double.isFinite(e.calibrationQuality) ? e.calibrationQuality : 0;
            double s = e.logE * q;
            if (s > score || (s == score && best != null && e.policyHash.compareTo(best) < 0)) { score = s; best = e.policyHash; }
        }
        return best;
    }
    /** Calibration quality in [0,1]: 1 at ECE 0, 0 at the 0.04 gate. */
    public static double calibrationQuality(double ece) { return Double.isFinite(ece) ? StatsR27.clamp(1 - ece / MAX_ECE, 0, 1) : 0; }
}
