package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * V23 fast finite-family point-in-time prior.
 *
 * <p>The previous implementation rebuilt expensive taxonomy keys for every historical row and
 * candidate. This version builds a bounded set of coarse/mid families once per target and then
 * applies the same evidence to all candidates. It uses only draws before targetIssue and never
 * labels historical evidence as REAL/SHADOW domain evidence.
 */
public final class HistoricalStatePriorEngine {
    public static final String MODULE_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final double NUMBER_BASE = 0.10d, K3_BASE = 0.30d;
    private static final int HISTORY_CAP = 720;

    private static final class Stat {
        int sample;
        final int[] number = new int[11];

        void add(int n) {
            if (n >= 1 && n <= 10) {
                sample++;
                number[n]++;
            }
        }

        double p(int n, double prior) {
            return (number[n] + NUMBER_BASE * prior) / (sample + prior);
        }
    }

    private static final class Evidence {
        final Stat[] global = new Stat[11];
        final Stat[][] transition = new Stat[11][11];
        final Map<String, Stat> coarse = new HashMap<String, Stat>();
        final Map<String, Stat> mid = new HashMap<String, Stat>();
        final String[] currentCoarse = new String[11];
        final String[] currentMid = new String[11];

        Evidence() {
            for (int p = 1; p <= 10; p++) {
                global[p] = new Stat();
                for (int n = 0; n <= 10; n++) transition[p][n] = new Stat();
            }
        }
    }

    public ArrayList<Models.ComboCandidate> apply(
            List<Models.ComboCandidate> input, List<Models.Draw> source, String targetIssue) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();
        if (input == null) return out;
        ArrayList<Models.Draw> history = normalize(source, targetIssue);
        Evidence e = build(history);
        for (int i = 0; i < input.size(); i++) {
            Models.ComboCandidate c = input.get(i);
            if (c == null) continue;
            applyOne(c, e);
            out.add(c);
        }
        return out;
    }

    private Evidence build(ArrayList<Models.Draw> history) {
        Evidence e = new Evidence();
        int size = history.size();
        for (int i = 0; i < size; i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) continue;
            for (int p = 1; p <= 10; p++) {
                int actual = d.getNumberAtPosition(p);
                e.global[p].add(actual);
                if (i > 0) {
                    int last = history.get(i - 1).getNumberAtPosition(p);
                    e.transition[p][last].add(actual);
                }
                if (i >= 24) {
                    String ck = coarseKey(history, i, p), mk = midKey(history, i, p);
                    stat(e.coarse, ck).add(actual);
                    stat(e.mid, mk).add(actual);
                }
            }
        }
        for (int p = 1; p <= 10; p++) {
            e.currentCoarse[p] = coarseKey(history, size, p);
            e.currentMid[p] = midKey(history, size, p);
        }
        return e;
    }

    private void applyOne(Models.ComboCandidate c, Evidence e) {
        if (!c.isValidK3()) return;
        int p = c.position;
        Stat gs = e.global[p];
        int last = 0;
        String mk = e.currentMid[p], ck = e.currentCoarse[p];
        try {
            String[] a = mk.split("\\|");
            for (int i = 0; i < a.length; i++)
                if (a[i].startsWith("L")) last = Integer.parseInt(a[i].substring(1));
        } catch (Throwable ignore) {
        }
        Stat ts = e.transition[p][last], cs = e.coarse.get(ck), ms = e.mid.get(mk);
        double p1 = posterior(c.n1, gs, ts, cs, ms),
                p2 = posterior(c.n2, gs, ts, cs, ms),
                p3 = posterior(c.n3, gs, ts, cs, ms);
        double k3 = clamp(p1 + p2 + p3, 0.18d, 0.48d);
        int midN = ms == null ? 0 : ms.sample,
                coarseN = cs == null ? 0 : cs.sample,
                transN = ts == null ? 0 : ts.sample;
        int effective = Math.max(gs.sample, Math.max(transN, Math.max(coarseN, midN)));
        double conf =
                clamp01(
                        0.15d
                                + Math.min(0.25d, gs.sample / 1440.0d)
                                + Math.min(0.18d, transN / 180.0d)
                                + Math.min(0.20d, coarseN / 150.0d)
                                + Math.min(0.22d, midN / 90.0d));
        double score = clamp01(0.50d + (k3 - K3_BASE) * 4.0d),
                risk = clamp01(0.50d + (K3_BASE - k3) * 4.0d);
        c.historicalPriorP = k3;
        c.historicalPriorConfidence = conf;
        c.historicalPriorSample = effective;
        c.historicalPriorCoarseKey = ck;
        c.historicalPriorMidKey = mk;
        c.historicalPriorSummary =
                "V23有限族历史先验：P="
                        + pct(k3)
                        + "｜conf="
                        + pct(conf)
                        + "｜global="
                        + gs.sample
                        + "｜transition="
                        + transN
                        + "｜coarse="
                        + coarseN
                        + "｜mid="
                        + midN
                        + "｜family="
                        + mk;
        c.teacherNumberHistorySample = gs.sample;
        c.teacherNumberSample = effective;
        c.teacherMatrixSample = effective;
        c.teacherMatrixStateSample = Math.max(midN, coarseN);
        c.teacherNumberHitRate = k3;
        c.teacherMatrixHitRate = k3;
        c.teacherMatrixRecentHitRate = k3;
        c.teacherNumberConfidence = conf;
        c.teacherMatrixConfidence = conf;
        c.teacherNumberScore = score;
        c.teacherMatrixScore = score;
        c.teacherNumberRisk = risk;
        c.teacherMatrixRiskPenalty = Math.max(0.0d, risk - 0.55d) * 0.10d;
        c.teacherMatrixApplied = 1;
        c.teacherMatrixStatus =
                effective >= 60 ? "HIERARCHICAL_HISTORY_READY" : "HIERARCHICAL_HISTORY_WARM";
        c.teacherNumberStatus = c.teacherMatrixStatus;
        c.teacherStatus = c.teacherMatrixStatus;
        c.teacherStatusClass = k3 >= K3_BASE ? "READY" : "WATCH";
        c.teacherOrdinaryBetPolicy = "FINITE_FAMILY_HISTORY_NEUTRAL";
        c.teacherMatrixOrdinaryBetPolicy = c.teacherOrdinaryBetPolicy;
        c.teacherFieldSource = "V26_FINITE_FAMILY_HISTORY";
        c.teacherFieldVersion = MODULE_VERSION;
        c.teacherRuntimeApplyVersion = MODULE_VERSION;
        c.teacherMatrixProfileKey = mk;
        c.teacherMatrixSummary = append(c.teacherMatrixSummary, c.historicalPriorSummary);
        c.teacherNumberSummary = append(c.teacherNumberSummary, c.historicalPriorSummary);
        if (c.dynamicRealSample + c.dynamicShadowSample <= 0) {
            c.dynamicClusterId = "HIST_PRIOR|" + mk;
            c.dynamicClusterPrototypeKey = ck;
            c.dynamicClusterFeatureKey = mk;
            c.dynamicClusterSample = effective;
            c.dynamicClusterHitRate = k3;
            c.dynamicClusterRecentHitRate = k3;
            c.dynamicClusterConfidence = conf;
            c.dynamicClusterStatus =
                    effective >= 60 ? "HISTORICAL_PRIOR_READY" : "HISTORICAL_PRIOR_WATCH";
            c.dynamicMatchedLevel = "HISTORICAL_PRIOR";
            c.dynamicClusterReason = "有限大类/中类历史回退，不冒充REAL或SHADOW同域样本";
        }
    }

    private double posterior(int n, Stat g, Stat t, Stat c, Stat m) {
        double sum = 0, w = 0;
        if (g != null) {
            sum += g.p(n, 30) * 0.22;
            w += 0.22;
        }
        if (t != null && t.sample >= 6) {
            double x = 0.20 + 0.10 * Math.min(1.0, t.sample / 40.0);
            sum += t.p(n, 16) * x;
            w += x;
        }
        if (c != null && c.sample >= 8) {
            double x = 0.22 + 0.10 * Math.min(1.0, c.sample / 80.0);
            sum += c.p(n, 18) * x;
            w += x;
        }
        if (m != null && m.sample >= 5) {
            double x = 0.25 + 0.13 * Math.min(1.0, m.sample / 45.0);
            sum += m.p(n, 12) * x;
            w += x;
        }
        return w <= 0 ? NUMBER_BASE : clamp(sum / w, 0.03, 0.22);
    }

    private static String coarseKey(List<Models.Draw> h, int end, int p) {
        String pg = p <= 3 ? "FRONT" : (p <= 7 ? "MID" : "BACK");
        double r12 = maxRate(h, end, p, 12), r48 = maxRate(h, end, p, 48);
        String density = r12 >= 0.22 ? "HOT" : (r12 <= 0.13 ? "COLD" : "BAL");
        String trend = r12 - r48 > 0.035 ? "RISING" : (r12 - r48 < -0.035 ? "FALLING" : "FLAT");
        return "HF|" + pg + "|" + density + "|" + trend;
    }

    private static String midKey(List<Models.Draw> h, int end, int p) {
        int last = end > 0 ? h.get(end - 1).getNumberAtPosition(p) : 0;
        double odd = oddRate(h, end, p, 24);
        String parity = odd > 0.58 ? "ODD" : (odd < 0.42 ? "EVEN" : "PBAL");
        return coarseKey(h, end, p) + "|P" + p + "|L" + last + "|" + parity;
    }

    private static double maxRate(List<Models.Draw> h, int end, int p, int w) {
        int start = Math.max(0, end - w), valid = 0;
        int[] count = new int[11];
        for (int i = start; i < end; i++) {
            Models.Draw d = h.get(i);
            if (d == null || !d.isValid()) continue;
            valid++;
            int n = d.getNumberAtPosition(p);
            if (n >= 1 && n <= 10) count[n]++;
        }
        int max = 0;
        for (int n = 1; n <= 10; n++) if (count[n] > max) max = count[n];
        return valid <= 0 ? 0.10 : ((double) max) / valid;
    }

    private static double oddRate(List<Models.Draw> h, int end, int p, int w) {
        int start = Math.max(0, end - w), v = 0, o = 0;
        for (int i = start; i < end; i++) {
            Models.Draw d = h.get(i);
            if (d == null || !d.isValid()) continue;
            v++;
            if ((d.getNumberAtPosition(p) & 1) == 1) o++;
        }
        return v <= 0 ? 0.5 : ((double) o) / v;
    }

    private static Stat stat(Map<String, Stat> m, String k) {
        Stat s = m.get(k);
        if (s == null) {
            s = new Stat();
            m.put(k, s);
        }
        return s;
    }

    private static ArrayList<Models.Draw> normalize(List<Models.Draw> in, String target) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
        if (in != null)
            for (int i = 0; i < in.size(); i++) {
                Models.Draw d = in.get(i);
                if (d == null || !d.isValid()) continue;
                if (target != null
                        && target.length() > 0
                        && d.issue != null
                        && d.issue.compareTo(target) >= 0) continue;
                out.add(d);
            }
        Collections.sort(
                out,
                new Comparator<Models.Draw>() {
                    public int compare(Models.Draw a, Models.Draw b) {
                        String x = a == null || a.issue == null ? "" : a.issue,
                                y = b == null || b.issue == null ? "" : b.issue;
                        return x.compareTo(y);
                    }
                });
        if (out.size() > HISTORY_CAP)
            return new ArrayList<Models.Draw>(out.subList(out.size() - HISTORY_CAP, out.size()));
        return out;
    }

    private static String append(String a, String b) {
        if (a == null || a.length() == 0) return b == null ? "" : b;
        if (b == null || b.length() == 0) return a;
        return a + "；" + b;
    }

    private static double clamp01(double v) {
        return clamp(v, 0, 1);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static String pct(double p) {
        return String.format(Locale.US, "%.1f%%", p * 100.0);
    }
}
