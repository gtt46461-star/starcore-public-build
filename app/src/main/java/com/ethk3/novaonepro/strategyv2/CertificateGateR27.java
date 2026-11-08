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
    public static final String GATE_VERSION = "R27_GATE_V1", LEGACY_UNVERSIONED = "LEGACY_UNVERSIONED";
    public static final long VALID_MS = 24L * 3600_000L;

    public static final class Evidence {
        public String gateVersion = GATE_VERSION;
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
        gate(g, fail, "gateVersion", GATE_VERSION.equals(e.gateVersion), e.gateVersion);
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
            if (!GATE_VERSION.equals(e.gateVersion)) continue; // different statistical contracts are not ranked together
            double q = Double.isFinite(e.calibrationQuality) ? e.calibrationQuality : 0;
            double s = e.logE * q;
            if (s > score || (s == score && best != null && e.policyHash.compareTo(best) < 0)) { score = s; best = e.policyHash; }
        }
        return best;
    }
    /** R31-B10-C02: exact P(longest losing streak <= maxStreak) for n iid Bernoulli(p) acts. DP over the current
     * losing streak 0..maxStreak: a hit returns to 0, a miss adds one, exceeding maxStreak is absorbed as rejection.
     * This is the sensitivity of the single streak gate under an iid assumption, never a live estimate. */
    public static double streakGatePassProbabilityR31(int n, double p, int maxStreak) {
        if (n < 0 || maxStreak < 0 || !(p >= 0 && p <= 1)) throw new IllegalArgumentException("R31_STREAK_DP_ARGUMENT");
        double[] state = new double[maxStreak + 1]; state[0] = 1;
        for (int t = 0; t < n; t++) {
            double[] next = new double[maxStreak + 1]; double alive = 0; for (double v : state) alive += v;
            next[0] = alive * p;
            for (int s = 0; s < maxStreak; s++) next[s + 1] += state[s] * (1 - p);
            state = next;
        }
        double pass = 0; for (double v : state) pass += v; return pass;
    }
    /** Single-gate pass probability over a grid of true hit rates, explicitly labelled as an iid sensitivity. */
    public static Map<String, Object> streakGateSensitivityR31(int acts, double[] hitRates) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("scope", "IID_SINGLE_STREAK_GATE_SENSITIVITY_NOT_A_LIVE_ESTIMATE"); m.put("acts", (long) acts); m.put("maxLosingStreak", (long) MAX_LOSS_STREAK);
        Map<String, Object> grid = new LinkedHashMap<>(); for (double p : hitRates) grid.put(String.valueOf(p), streakGatePassProbabilityR31(acts, p, MAX_LOSS_STREAK)); m.put("passProbabilityByHitRate", grid);
        m.put("note", "Other gates only lower the joint pass probability below this single-gate value; V1 thresholds are unchanged.");
        return m;
    }
    /** Which gates rejected: an evaluated-and-rejected certificate is a result, not an unfinished computation. */
    public static Map<String, Object> rejectionReportR31(Verdict v) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("gateVersion", GATE_VERSION); m.put("computation", "COMPLETE");
        m.put("result", v.active ? "ISSUED" : "REJECTED_BY_GATES"); m.put("rejectingGates", new ArrayList<Object>(v.failing)); return m;
    }
    /** Calibration quality in [0,1]: 1 at ECE 0, 0 at the 0.04 gate. */
    public static Map<String,Object> contract(){Map<String,Object> c=new LinkedHashMap<>();c.put("activeGate",GATE_VERSION);c.put("MIN_HR",MIN_HR);c.put("MIN_ACTS",MIN_ACTS);c.put("MAX_ECE",MAX_ECE);c.put("MIN_WORST_720_BLOCK",MIN_WORST_BLOCK);c.put("MAX_LOSS_STREAK",MAX_LOSS_STREAK);c.put("scope","Original immutable V1 thresholds; no V2 report can issue a certificate");c.put("contractHash",StatsR27.sha256(JsonR27.write(c)));return java.util.Collections.unmodifiableMap(c);}
    public static double calibrationQuality(double ece) { return Double.isFinite(ece) ? StatsR27.clamp(1 - ece / MAX_ECE, 0, 1) : 0; }
}
