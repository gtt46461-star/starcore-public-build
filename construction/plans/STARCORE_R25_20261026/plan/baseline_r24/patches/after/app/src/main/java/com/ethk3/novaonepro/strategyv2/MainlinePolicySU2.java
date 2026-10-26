package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * Incumbent-Protected Mainline, IPM (P03b + P30) - "never worse than the old version" as a rule.
 *
 * Defect fixed (R13 v2 verification): the earlier policy scored every member on the last W
 * settled periods and took the best. A tier champion is selected on draws before it joins the
 * league, so a window that reaches back before its birth is in-sample for it (winner's curse),
 * while the user's cross-intersection method (XI) is always out-of-sample. Challengers always
 * "won" and the mainline fell below XI on the 5.4.3 report data (26.5% vs 35.1%).
 *
 * Rule (bit-identical to reference/replay/mainline_ipm.py, fixture r13_ipm):
 *  1. out-of-sample only: a member is scored on periods >= birth (su2_league.joined_sequence);
 *  2. XI is the floor: the mainline starts as XI and returns to XI when the current challenger
 *     is significantly worse than XI - or significantly below the 30% baseline - on the last
 *     DEMOTE_WINDOW periods (one-sided z, alpha 0.02)
 *     or loses the frequency floor there (short window = fast reaction);
 *  3. a challenger C replaces the current member U only on the same periods
 *     [max(birth_C, birth_U, c-W), c): >= 1000 periods, >= 250 acts, coverage >= 0.15,
 *     one-sided two-proportion z with p <= alpha/k (Bonferroni over k eligible challengers),
 *     C better in >= 2 of 3 equal sub-blocks, not below XI's point estimate when U != XI, and
 *     not demotable itself right now (hysteresis);
 *     the largest z wins, ties by member id.
 * Indices are relative to the caller's window start; births are clamped to >= 0.
 */
public final class MainlinePolicySU2 {
    private MainlinePolicySU2() {}
    public static final int MIN_PERIODS = 1000, MIN_ACTS = 250, DEMOTE_WINDOW = 1667;
    /** From the repeated-look error budget, checked on known-truth leagues (reference/replay/ipm_oc.py). */
    public static final double MIN_COVERAGE = 0.15, ALPHA_SWITCH = 0.02, ALPHA_DEMOTE = 0.02, BASE = 0.30;

    public static final class Member {
        public final String id, name; public final boolean incumbent; public final boolean[] act, hit; public final int birth;
        public Member(String id, String name, boolean incumbent, boolean[] act, boolean[] hit, int birth) {
            if (id == null || name == null || act == null || hit == null || act.length != hit.length) throw new IllegalArgumentException("SU2_LEAGUE_MEMBER");
            this.id = id; this.name = name; this.incumbent = incumbent; this.act = act; this.hit = hit; this.birth = Math.max(0, birth);
        }
    }

    public static final class Choice {
        public final Member member; public final double score, incumbentScore; public final String reason;
        Choice(Member m, double s, double inc, String reason) { member = m; score = s; incumbentScore = inc; this.reason = reason; }
    }

    /** Numerical Recipes erfcc, |relative error| < 1.2e-7 (identical in the Python reference). */
    static double erfcc(double x) {
        double z = Math.abs(x), t = 1.0 / (1.0 + 0.5 * z);
        double r = t * Math.exp(-z * z - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 + t * (
            -0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 + t * (-0.82215223 + t * 0.17087277)))))))));
        return x >= 0 ? r : 2.0 - r;
    }
    static double normSf(double z) { return 0.5 * erfcc(z / Math.sqrt(2.0)); }

    static int[] nk(Member m, int a, int b) {
        int n = 0, k = 0;
        for (int t = Math.max(0, a); t < b; t++) if (m.act[t]) { n++; if (m.hit[t]) k++; }
        return new int[]{n, k};
    }
    static double rate(int n, int k) { return (k + 0.5) / (n + 1.0); }

    /** One-sided z for "C hits more often than U" (unpooled, continuity-shrunk). */
    static double zBetter(int nc, int kc, int nu, int ku) {
        if (nc == 0 || nu == 0) return 0.0;
        double pc = rate(nc, kc), pu = rate(nu, ku), se = Math.sqrt(pc * (1 - pc) / nc + pu * (1 - pu) / nu);
        return se > 0 ? (pc - pu) / se : 0.0;
    }

    static int blocksWon(Member c, Member u, int a, int b) {
        int won = 0;
        for (int f = 0; f < 3; f++) {
            int x = a + (b - a) * f / 3, y = a + (b - a) * (f + 1) / 3;
            int[] cc = nk(c, x, y), uu = nk(u, x, y);
            if (uu[0] == 0) uu = nk(u, a, b);
            if (cc[0] > 0 && uu[0] > 0 && cc[1] / (double) cc[0] > uu[1] / (double) uu[0]) won++;
        }
        return won;
    }

    static boolean eligible(Member m, int c, int window) {
        int a = Math.max(m.birth, c - window);
        if (c - a < MIN_PERIODS) return false;
        int[] x = nk(m, a, c);
        return x[0] >= MIN_ACTS && x[0] / (double) (c - a) >= MIN_COVERAGE;
    }

    /** Significantly worse than XI, or significantly below the 30% baseline, on the last DEMOTE_WINDOW periods,
     *  or below the frequency floor there.
     *  Used to demote the current member and to refuse promoting such a member (hysteresis). */
    static boolean demotable(Member m, Member xi, int c) {
        int a = Math.max(Math.max(m.birth, xi.birth), c - DEMOTE_WINDOW);
        int[] cc = nk(m, a, c), xx = nk(xi, a, c);
        // R24: insufficient evidence is not evidence of degradation.
        int periods = c - a;
        if (periods < MIN_PERIODS) return false;
        if (cc[0] / (double) periods < MIN_COVERAGE) return true;
        if (cc[0] < MIN_ACTS) return false;
        double belowBase = (BASE - cc[1] / (double) cc[0]) / Math.sqrt(BASE * (1 - BASE) / cc[0]);   // below the 30% baseline
        return normSf(zBetter(xx[0], xx[1], cc[0], cc[1])) <= ALPHA_DEMOTE || normSf(belowBase) <= ALPHA_DEMOTE;
    }

    /** Choose the member for the block starting at c (all evidence strictly before c). */
    public static Choice choose(List<Member> league, String currentId, int c, int window) {
        if (league.isEmpty()) throw new IllegalArgumentException("SU2_LEAGUE_EMPTY");
        Member xi = null, cur = null;
        for (Member m : league) {
            if (m.act.length < c) throw new IllegalArgumentException("SU2_LEAGUE_HISTORY_SHORT:" + m.id);
            if (m.incumbent && xi == null) xi = m;
            if (m.id.equals(currentId)) cur = m;
        }
        if (cur == null) cur = xi != null ? xi : league.get(0);
        String reason = "KEEP";
        if (xi != null && cur != xi && demotable(cur, xi, c)) { cur = xi; reason = "DEMOTE_TO_INCUMBENT"; }
        List<Member> cands = new ArrayList<>();
        for (Member m : league) if (m != cur && eligible(m, c, window)) cands.add(m);
        int k = cands.size(); Member best = null; double bz = Double.NEGATIVE_INFINITY;
        for (Member m : cands) {
            int a = Math.max(Math.max(m.birth, cur.birth), c - window);
            if (c - a < MIN_PERIODS) continue;
            int[] cc = nk(m, a, c), uu = nk(cur, a, c);
            if (cc[0] < MIN_ACTS || cc[0] / (double) (c - a) < MIN_COVERAGE) continue;
            double z = zBetter(cc[0], cc[1], uu[0], uu[1]);
            if (normSf(z) > ALPHA_SWITCH / k) continue;
            if (blocksWon(m, cur, a, c) < 2) continue;
            if (xi != null && m != xi && demotable(m, xi, c)) continue;
            if (xi != null && cur != xi && m != xi) {
                int ax = Math.max(Math.max(m.birth, xi.birth), c - window);
                int[] n1 = nk(m, ax, c), n2 = nk(xi, ax, c);
                if (n2[0] > 0 && n1[0] > 0 && n1[1] / (double) n1[0] < n2[1] / (double) n2[0]) continue;
            }
            if (best == null || z > bz || (z == bz && m.id.compareTo(best.id) < 0)) { best = m; bz = z; }
        }
        double incScore = Double.NaN;
        if (xi != null) { int a = Math.max(xi.birth, c - window); int[] x = nk(xi, a, c); incScore = x[0] == 0 ? Double.NaN : x[1] / (double) x[0]; }
        if (best != null) return new Choice(best, bz, incScore, best.incumbent ? "SWITCH_TO_INCUMBENT" : "SWITCH_CHALLENGER_BEATS_CURRENT");
        int a = Math.max(cur.birth, c - window); int[] x = nk(cur, a, c);
        return new Choice(cur, x[0] == 0 ? 0.0 : x[1] / (double) x[0], incScore, reason);
    }
}
