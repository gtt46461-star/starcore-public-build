package com.ethk3.novaonepro.strategyv2;

import java.util.LinkedHashMap;
import java.util.Map;

/** R27-040 anytime-valid sequential qualification (docs/09 9.2) replacing the 69,120-period window of
 * QualificationWindowR26. Only FUTURE samples (frozen before the deadline, settled after the draw) may be fed.
 * u_t = ACT_t × (hit_t − 0.30). Two betting martingales:
 *   up:   H0 E[u|past] ≤ 0          E_t = Π(1 + λ_s u_s),        λ_s = clip(μ̂/σ̂², 0, 0.5/0.7)   (aGRAPA, predictable)
 *   down: H0' E[u|past] ≥ δ_min     D_t = Π(1 + λ'_s (δ − u_s)), λ'_s = clip((δ−μ̂)/σ̂², 0, 0.5/0.7)
 * Crossing 1/α_i on "up" → evidence better than random (enter qualification); on "down" → not better than the target
 * (leave the live pool). α_i = α_root·[1/((i+1)ln²(i+1))]/C with a permanent global index i that is never reset,
 * so Σα_i = α_root = 0.05 over every policy that will ever enter the live pool. Thresholds 34%/quality gates are
 * applied separately by CertificateGateR27 and are unchanged. */
public final class SequentialQualificationR27 {
    public static final double ALPHA_ROOT = 0.05, LAMBDA_MAX = 0.5 / 0.7, PRIOR_VAR = 0.04, TARGET_HR = 0.34;
    public static final double C = normalizer();
    public static final String ACCUMULATING = "ACCUMULATING", QUALIFIED = "EVIDENCE_ABOVE_RANDOM", NOT_BETTER = "NOT_BETTER_THAN_TARGET";

    static double normalizer() {
        double s = 0; int n = 2_000_000;
        for (int i = 1; i <= n; i++) { double l = Math.log(i + 1.0); s += 1.0 / ((i + 1.0) * l * l); }
        return s + 1.0 / Math.log(n + 1.0); // integral tail
    }
    public static double alpha(long globalIndex) {
        if (globalIndex < 1) throw new IllegalArgumentException("R27_ALPHA_INDEX");
        double l = Math.log(globalIndex + 1.0);
        return ALPHA_ROOT / ((globalIndex + 1.0) * l * l) / C;
    }
    /** δ_min for a pre-registered act rate: the per-period excess of a 34% hit rate at that rate (0.15 → 0.006). */
    public static double deltaMin(double actRate) { return StatsR27.clamp(actRate, 0.01, 1) * (TARGET_HR - StatsR27.BASE); }

    public static final class State {
        public final String policyHash; public final long globalIndex; public final double alpha, delta, targetActRate;
        public double logUp, logDown, sumU, sumU2; public long n, acts, hits, lastOrdinal = -1; public String state = ACCUMULATING;
        public State(String policyHash, long globalIndex, double targetActRate) {
            this.policyHash = policyHash; this.globalIndex = globalIndex; this.alpha = alpha(globalIndex); this.delta = deltaMin(targetActRate);
            this.targetActRate = targetActRate;
        }
        public double threshold() { return Math.log(1 / alpha); }
        double mean() { return sumU / (n + 1.0); }
        double var() { return Math.max(1e-4, (PRIOR_VAR + sumU2 - sumU * sumU / (n + 1.0)) / (n + 1.0)); }

        /** Feeds one settled FUTURE period. Duplicate or older ordinals are ignored (idempotent). Returns the state. */
        public String observe(long ordinal, boolean act, boolean hit) {
            if (ordinal <= lastOrdinal) return state;
            double u = act ? (hit ? 1 : 0) - StatsR27.BASE : 0;
            double m = mean(), v = var();
            double lu = StatsR27.clamp(m / v, 0, LAMBDA_MAX), ld = StatsR27.clamp((delta - m) / v, 0, LAMBDA_MAX);
            logUp += Math.log1p(lu * u); logDown += Math.log1p(ld * (delta - u));
            n++; sumU += u; sumU2 += u * u; if (act) { acts++; if (hit) hits++; } lastOrdinal = ordinal;
            if (ACCUMULATING.equals(state)) {
                if (logUp >= threshold()) state = QUALIFIED; else if (logDown >= threshold()) state = NOT_BETTER;
            }
            return state;
        }
        /** Multiple of the rejection threshold reached so far (e-value / (1/α)) on the log scale. */
        public double progress() { return logUp / threshold(); }
        /** Planning estimate of remaining periods at the current effect estimate (not a promise). */
        public double expectedRemaining() {
            double m = sumU / Math.max(1, n), v = n < 2 ? PRIOR_VAR : Math.max(1e-6, sumU2 / n - m * m);
            if (m <= 0) return Double.POSITIVE_INFINITY;
            return Math.max(0, (threshold() - logUp) / (m * m / (2 * v)));
        }
        public double hitRate() { return acts == 0 ? Double.NaN : hits / (double) acts; }
        public Map<String, Object> json() {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("policyHash", policyHash); o.put("globalIndex", globalIndex); o.put("alpha", alpha); o.put("deltaMin", delta);
            o.put("targetActRate",targetActRate);
            o.put("deltaMinSource","PRE_REGISTERED_TARGET_ACT_RATE: clamp(rate,0.01,1)*(0.34-0.30); legacy unknown MARGIN uses 0.15; no FUTURE refit");
            o.put("logEUp", logUp); o.put("logEDown", logDown); o.put("threshold", threshold()); o.put("n", n); o.put("acts", acts); o.put("hits", hits);
            o.put("state", state); o.put("progress", progress()); double r = expectedRemaining(); o.put("expectedRemaining", Double.isFinite(r) ? (Object) r : null);
            return o;
        }
    }
}
