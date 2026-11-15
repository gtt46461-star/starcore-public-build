package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Small point-in-time evidence forest for a phone runtime.
 *
 * <p>Nine randomized categorical trees use different feature subspaces and deterministic bootstrap
 * subsamples. Every leaf stores a smoothed 01..10 distribution. The ensemble is intentionally
 * bounded and only supplies evidence to BayesianEvidenceFusionEngine; it never creates a BET and
 * never raises probability to satisfy frequency debt.
 */
public final class OnlineEvidenceForestEngine {
    public static final String MODULE_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final int TREE_COUNT = 9;
    private static final double NUMBER_BASE = 0.10d;

    private static final class Leaf {
        int sample;
        final int[] count = new int[11];

        void add(int n) {
            if (n >= 1 && n <= 10) {
                sample++;
                count[n]++;
            }
        }

        double p(int n) {
            return (count[n] + 1.8d) / (sample + 18.0d);
        }
    }

    public ArrayList<Models.ComboCandidate> apply(
            List<Models.ComboCandidate> input, List<Models.Draw> source, String targetIssue) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();
        if (input == null) return out;
        ArrayList<Models.Draw> history = normalize(source, targetIssue);
        @SuppressWarnings("unchecked")
        Map<String, Leaf>[] forest = new Map[TREE_COUNT];
        for (int t = 0; t < TREE_COUNT; t++) forest[t] = new HashMap<String, Leaf>();
        train(forest, history);
        for (int i = 0; i < input.size(); i++) {
            Models.ComboCandidate c = input.get(i);
            if (c == null) continue;
            applyOne(c, history, forest);
            out.add(c);
        }
        return out;
    }

    private void train(Map<String, Leaf>[] forest, ArrayList<Models.Draw> h) {
        for (int i = 24; i < h.size(); i++) {
            Models.Draw d = h.get(i);
            if (d == null || !d.isValid()) continue;
            for (int p = 1; p <= 10; p++) {
                int actual = d.getNumberAtPosition(p);
                for (int tree = 0; tree < TREE_COUNT; tree++) {
                    int hash = Math.abs((safe(d.issue) + "|" + p + "|" + tree).hashCode());
                    if ((hash % 100) >= 72) continue; // deterministic bootstrap-like subsample
                    String key = leafKey(tree, h, i, p);
                    Leaf leaf = forest[tree].get(key);
                    if (leaf == null) {
                        leaf = new Leaf();
                        forest[tree].put(key, leaf);
                    }
                    leaf.add(actual);
                }
            }
        }
    }

    private void applyOne(
            Models.ComboCandidate c, ArrayList<Models.Draw> h, Map<String, Leaf>[] forest) {
        if (!c.isValidK3()) return;
        double sum = 0.0d;
        double wsum = 0.0d;
        int sampleSum = 0;
        int used = 0;
        for (int tree = 0; tree < TREE_COUNT; tree++) {
            Leaf leaf = forest[tree].get(leafKey(tree, h, h.size(), c.position));
            if (leaf == null || leaf.sample < 6) continue;
            double k3 = leaf.p(c.n1) + leaf.p(c.n2) + leaf.p(c.n3);
            double w = 0.45d + 0.55d * Math.min(1.0d, leaf.sample / 45.0d);
            sum += k3 * w;
            wsum += w;
            sampleSum += leaf.sample;
            used++;
        }
        double p = used <= 0 ? 0.30d : clamp(sum / wsum, 0.18d, 0.46d);
        int avgSample = used <= 0 ? 0 : sampleSum / used;
        double confidence =
                clamp01(
                        (used / (double) TREE_COUNT) * 0.55d
                                + Math.min(1.0d, avgSample / 45.0d) * 0.45d);
        c.evidenceForestP = p;
        c.evidenceForestConfidence = confidence;
        c.evidenceForestSample = avgSample;
        c.evidenceForestTreeCount = used;
        c.evidenceForestSummary =
                "在线证据森林：树="
                        + used
                        + "/"
                        + TREE_COUNT
                        + "｜叶均样本="
                        + avgSample
                        + "｜K3后验="
                        + pct(p)
                        + "｜置信="
                        + pct(confidence)
                        + "｜点时训练/确定性Bootstrap/不直接BET";
        c.reasonSummary = append(c.reasonSummary, c.evidenceForestSummary);
    }

    private static String leafKey(int tree, List<Models.Draw> h, int end, int p) {
        State s = state(h, end, p);
        switch (tree) {
            case 0:
                return "T0|PG" + group(p) + "|L" + s.last;
            case 1:
                return "T1|P" + p + "|SL" + s.slope;
            case 2:
                return "T2|PG" + group(p) + "|OD" + s.oddBand + "|LP" + (s.last & 1);
            case 3:
                return "T3|P" + p + "|BG" + s.bigBand + "|LB" + (s.last >= 6 ? 1 : 0);
            case 4:
                return "T4|PG" + group(p) + "|RP" + s.repeatBand;
            case 5:
                return "T5|P" + p + "|HOT" + s.hotBand;
            case 6:
                return "T6|PG" + group(p) + "|L" + s.last + "|SL" + s.slope;
            case 7:
                return "T7|P" + p + "|OD" + s.oddBand + "|BG" + s.bigBand;
            default:
                return "T8|PG" + group(p) + "|SL" + s.slope + "|RP" + s.repeatBand;
        }
    }

    private static final class State {
        int last;
        String slope = "F", oddBand = "M", bigBand = "M", repeatBand = "L", hotBand = "B";
    }

    private static State state(List<Models.Draw> h, int end, int p) {
        State s = new State();
        int e = Math.min(end, h == null ? 0 : h.size());
        int st = Math.max(0, e - 24);
        int valid = 0, odd = 0, big = 0, repeat = 0, prev = 0;
        double a = 0, b = 0;
        int ac = 0, bc = 0;
        int[] count = new int[11];
        for (int i = st; i < e; i++) {
            Models.Draw d = h.get(i);
            if (d == null || !d.isValid()) continue;
            int n = d.getNumberAtPosition(p);
            if (n < 1 || n > 10) continue;
            valid++;
            s.last = n;
            count[n]++;
            if ((n & 1) == 1) odd++;
            if (n >= 6) big++;
            if (prev == n) repeat++;
            prev = n;
            if (i < st + 12) {
                a += n;
                ac++;
            } else {
                b += n;
                bc++;
            }
        }
        if (valid <= 0) return s;
        double slope = (bc <= 0 ? 0 : b / bc) - (ac <= 0 ? 0 : a / ac);
        s.slope = slope > .9 ? "U" : (slope < -.9 ? "D" : "F");
        double or = odd / (double) valid,
                br = big / (double) valid,
                rr = repeat / (double) Math.max(1, valid - 1);
        s.oddBand = or > .62 ? "H" : (or < .38 ? "L" : "M");
        s.bigBand = br > .62 ? "H" : (br < .38 ? "L" : "M");
        s.repeatBand = rr > .18 ? "H" : (rr > .08 ? "M" : "L");
        int max = 0;
        for (int n = 1; n <= 10; n++) if (count[n] > max) max = count[n];
        s.hotBand = max >= 5 ? "H" : (max >= 3 ? "M" : "B");
        return s;
    }

    private static ArrayList<Models.Draw> normalize(List<Models.Draw> in, String target) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
        if (in != null)
            for (int i = 0; i < in.size(); i++) {
                Models.Draw d = in.get(i);
                if (d == null || !d.isValid()) continue;
                if (target != null && target.length() > 0 && safe(d.issue).compareTo(target) >= 0)
                    continue;
                out.add(d);
            }
        Collections.sort(
                out,
                new Comparator<Models.Draw>() {
                    public int compare(Models.Draw a, Models.Draw b) {
                        return safe(a.issue).compareTo(safe(b.issue));
                    }
                });
        return out;
    }

    private static int group(int p) {
        return p <= 3 ? 1 : (p <= 7 ? 2 : 3);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static double clamp01(double v) {
        return clamp(v, 0, 1);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static String pct(double p) {
        return String.format(java.util.Locale.US, "%.1f%%", p * 100.0d);
    }

    private static String append(String a, String b) {
        if (a == null || a.length() == 0) return b == null ? "" : b;
        if (b == null || b.length() == 0) return a;
        return a + "；" + b;
    }
}
