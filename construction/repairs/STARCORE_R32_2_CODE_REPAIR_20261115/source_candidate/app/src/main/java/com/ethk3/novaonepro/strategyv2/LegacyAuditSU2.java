package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * Legacy high-score records (P02): three evidence tiers and the effective-mechanism
 * audit that explains where an old 40%+ figure came from.
 *
 *   LEGACY_REPORTED  a figure printed by an old report (kept, shown, never deleted)
 *   ROW_VERIFIED     the per-period frozen predictions/labels were recovered and recount
 *   REPLAY_VERIFIED  the original VM + features were replayed and reproduce the rows
 *
 * The audit replays a genome's score program over a probe of periods and records
 * whether its score, its action and its state actually change. A genome whose score
 * is constant contributes nothing; its displayed figure belongs to the shared outer
 * selection rule and is attributed to that rule (not to the genome).
 */
public final class LegacyAuditSU2 {
    private LegacyAuditSU2() {}
    public enum Tier { LEGACY_REPORTED, ROW_VERIFIED, REPLAY_VERIFIED }

    public static final class Probe { public final boolean scoreChanges, actionChanges, stateChanges; public final double scoreRange;
        Probe(boolean s, boolean a, boolean st, double r) { scoreChanges = s; actionChanges = a; stateChanges = st; scoreRange = r; } }

    public interface Program { double score(int t); int action(int t); long stateHash(int t); }

    public static Probe probe(Program p, int from, int to) {
        double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY; Set<Integer> actions = new HashSet<>(); Set<Long> states = new HashSet<>();
        for (int t = from; t < to; t++) { double s = p.score(t); lo = Math.min(lo, s); hi = Math.max(hi, s); actions.add(p.action(t)); states.add(p.stateHash(t)); }
        double range = hi - lo;
        return new Probe(range > 1e-12, actions.size() > 1, states.size() > 1, range);
    }

    /** Chance that the best of `count` exactly-30% strategies shows >= k hits in n acting periods (display-caliber check). */
    public static double bestOfChance(int count, int n, int k) {
        double tail = 0; double p = 0.3;
        // exact binomial upper tail via log-pmf
        for (int i = k; i <= n; i++) tail += Math.exp(logChoose(n, i) + i * Math.log(p) + (n - i) * Math.log(1 - p));
        return 1 - Math.pow(1 - Math.min(1, tail), count);
    }
    static double logChoose(int n, int k) { double s = 0; for (int i = 1; i <= k; i++) s += Math.log(n - k + i) - Math.log(i); return s; }
}
