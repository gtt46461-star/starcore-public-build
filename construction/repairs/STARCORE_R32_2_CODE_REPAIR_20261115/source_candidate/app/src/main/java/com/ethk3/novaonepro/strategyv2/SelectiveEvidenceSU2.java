package com.ethk3.novaonepro.strategyv2;

/**
 * Evidence for selective strategies (P27/P28): a strategy that acts on 15-30% of
 * periods is judged on the periods it chose, with the calendar kept honest.
 *
 *  - quality:   hits / ACT periods, Hoeffding-style lower bound at the alpha this
 *               look is allowed to spend (alpha_k <= alpha / (k (k+1)), lifetime
 *               root alpha 0.05, so repeated looks can never add up to more);
 *  - frequency: ACT / (ACT + ABSTAIN); MISSING periods (not computed in time)
 *               are counted against the strategy, never silently dropped;
 *  - stability: every one of 4 calendar blocks must keep its rate >= floor;
 *  - parity:    on the SAME periods the challenger must not be significantly
 *               worse than the incumbent (the user's original method): lower
 *               bound of the paired per-period excess-hit difference >= -0.01 (fixed before outcomes).
 * The null baseline is exactly 30% for any K3 on a uniform permutation.
 */
public final class SelectiveEvidenceSU2 {
    private SelectiveEvidenceSU2() {}

    public static final class Verdict {
        public final boolean pass; public final String reason; public final int acts, hits, missing; /** Lower drives qualification; upper remains descriptive for legacy reports. */
        public final double rate, lower, coverage, worstBlock, pairedUpper, pairedLower;
        Verdict(boolean pass, String reason, int acts, int hits, int missing, double rate, double lower, double coverage, double worstBlock, double pairedUpper, double pairedLower) {
            this.pass = pass; this.reason = reason; this.acts = acts; this.hits = hits; this.missing = missing; this.rate = rate; this.lower = lower; this.coverage = coverage; this.worstBlock = worstBlock; this.pairedUpper = pairedUpper; this.pairedLower = pairedLower;
        }
    }

    public static double alphaForLook(double root, int look) { if (!(root > 0 && root < 1) || look < 1) throw new IllegalArgumentException("SU2_LOOK"); return root / ((double) look * (look + 1)); }

    /** One-sided Hoeffding lower bound for a Bernoulli mean. */
    public static double hoeffdingLower(int k, int n, double alpha) { if (n == 0) return 0; return k / (double) n - Math.sqrt(Math.log(1 / alpha) / (2.0 * n)); }

    /**
     * act/hit/missing over the evaluation calendar [a,b); incumbentAct/Hit on the same periods (may be null).
     * minActs: minimum ACT periods (e.g. 600); floor: block floor (e.g. 0.28); minCoverage: e.g. 0.15.
     */
    public static Verdict evaluate(boolean[] act, boolean[] hit, boolean[] missing, boolean[] incAct, boolean[] incHit,
                                   int a, int b, int look, double rootAlpha, int minActs, double floor, double minCoverage) {
        if (act == null || hit == null || a < 0 || b <= a || b > act.length || b > hit.length
                || (missing != null && b > missing.length) || ((incAct == null) != (incHit == null))
                || (incAct != null && (b > incAct.length || b > incHit.length))) throw new IllegalArgumentException("R26_CALENDAR_SHAPE");
        int n = 0, k = 0, miss = 0;
        for (int t = a; t < b; t++) { if (missing != null && missing[t]) { miss++; continue; } if (act[t]) { n++; if (hit[t]) k++; } }
        double cov = (n) / (double) Math.max(1, b - a);
        double rate = n == 0 ? 0 : k / (double) n;
        double alpha = alphaForLook(rootAlpha, look);
        // missing periods are scored as acted-and-missed: the strategy cannot hide its absences
        double lower = hoeffdingLower(k, n + miss, alpha);
        double worst = 1; int step = (b - a) / 4;
        for (int i = 0; i < 4; i++) {
            int bn = 0, bk = 0; int x = a + i * step, y = i == 3 ? b : a + (i + 1) * step;
            for (int t = x; t < y; t++) if (missing != null && missing[t]) continue; else if (act[t]) { bn++; if (hit[t]) bk++; }
            worst = Math.min(worst, bn == 0 ? 0 : bk / (double) bn);
        }
        double paired = Double.NaN, upper = Double.NaN;
        if (incAct != null) {
            // per-period paired difference in hits (each side counts 1 for an acted hit), bounded in [-1,1]
            int m = b - a; double s = 0;
            for (int t = a; t < b; t++) s += (act[t] && hit[t] ? 1 : 0) - (incAct[t] && incHit[t] ? 1 : 0) - 0.3 * ((act[t] ? 1 : 0) - (incAct[t] ? 1 : 0));
            double mean = s / m, radius = Math.sqrt(2 * Math.log(1 / alpha) / m); paired = mean - radius; upper = mean + radius;   // LOWER bound of paired excess; range [-1,1].
        }
        String reason = "PASS";
        if (miss > 0) reason = "CALENDAR_INCOMPLETE";
        else if (n < minActs) reason = "ACTS_LT_" + minActs;
        else if (cov < minCoverage) reason = "COVERAGE_LT_" + minCoverage;
        else if (rate < 0.35) reason = "OBSERVED_HIT_RATE_LT_035";
        else if (!(lower > 0.30)) reason = "LOWER_BOUND_NOT_ABOVE_NULL_30";
        else if (worst < floor) reason = "BLOCK_BELOW_FLOOR";
        else if (incAct != null && !(paired >= -0.01)) reason = "PAIRED_LCB_BELOW_PREREGISTERED_MINUS_001";
        return new Verdict(reason.equals("PASS"), reason, n, k, miss, rate, lower, cov, worst, upper, paired);
    }
}
