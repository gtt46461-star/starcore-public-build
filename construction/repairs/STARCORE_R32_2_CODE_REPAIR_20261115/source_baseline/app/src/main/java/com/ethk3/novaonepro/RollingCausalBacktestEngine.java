package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * V23 rolling micro walk-forward trainer.
 *
 * <p>It evaluates a finite set of causal-window profiles using only data before every simulated
 * target. One slice processes a small deterministic-random batch and is designed to finish inside
 * the idle gap between draws. Results are stored as compact state values; no SHADOW result is
 * counted as real profit and this engine never creates a Decision or Trade.
 */
public final class RollingCausalBacktestEngine {
    public static final String MODULE_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    public static final String STATE_PROFILE = "v23_causal_profile";
    public static final String STATE_SUMMARY = "v23_causal_micro_summary";
    public static final String STATE_SAMPLE = "v23_causal_micro_sample";
    public static final String STATE_HIT = "v23_causal_micro_hit";
    public static final String STATE_RATE = "v23_causal_micro_rate";
    public static final String STATE_CURSOR = "v23_causal_micro_cursor";
    public static final String STATE_COST = "v23_causal_micro_cost_ms";
    public static final String STATE_LAST_AT = "v23_causal_micro_last_at";

    private static final int PROFILE_COUNT = 8;
    private static final String[] PROFILE_NAME =
            new String[] {
                "BALANCED", "RECENT", "MID", "MARKOV", "OMISSION", "STABLE", "REVERSAL", "HYBRID"
            };
    /* w12,w24,w48,w96,w360,markov,omission,momentum,stability */
    private static final double[][] W =
            new double[][] {
                {0.18, 0.18, 0.16, 0.12, 0.08, 0.14, 0.06, 0.05, 0.03},
                {0.34, 0.23, 0.12, 0.06, 0.03, 0.10, 0.03, 0.07, 0.02},
                {0.12, 0.22, 0.24, 0.16, 0.07, 0.10, 0.04, 0.02, 0.03},
                {0.10, 0.12, 0.12, 0.09, 0.05, 0.38, 0.05, 0.05, 0.04},
                {0.11, 0.13, 0.13, 0.10, 0.06, 0.12, 0.26, 0.05, 0.04},
                {0.12, 0.17, 0.21, 0.19, 0.12, 0.08, 0.03, 0.02, 0.06},
                {0.27, 0.16, 0.10, 0.07, 0.04, 0.12, 0.08, 0.13, 0.03},
                {0.22, 0.18, 0.16, 0.10, 0.05, 0.17, 0.05, 0.04, 0.03}
            };

    public static final class NumberFeature {
        public double r12, r24, r48, r96, r360;
        public double markov, omission, momentum, stability;
        public int gap;
    }

    public static final class RealtimeContext {
        public final NumberFeature[][] feature = new NumberFeature[11][11];
        public int profileIndex;
        public int historySize;

        public int lastNumber(int position) {
            return 0;
        }
    }

    public static final class WindowEvidence {
        public double probability;
        public double confidence;
        public double score;
        public int effectiveSample;
        public String coarseFamily = "";
        public String midFamily = "";
        public String summary = "";
    }

    private static final class Prediction {
        int position, n1, n2, n3;
        double probability, margin, confidence;
        String family;
    }

    public String runSlice(
            DbStore store, List<Models.Draw> source, int requestedTargets, long budgetMs) {
        long start = System.currentTimeMillis();
        try {
            if (store != null)
                store.registerModuleBuildV20(
                        "RollingCausalBacktestEngine", MODULE_VERSION, "MICRO_WALK_FORWARD");
        } catch (Throwable ignore) {
        }
        ArrayList<Models.Draw> history = normalize(source);
        if (store == null || history.size() < 96) {
            return "微型走前跳过：历史少于96期";
        }
        int targets = requestedTargets <= 0 ? 10 : Math.min(16, requestedTargets);
        if (budgetMs <= 0L) budgetMs = 8500L;
        int minTrain = Math.min(360, Math.max(72, history.size() / 3));
        int range = history.size() - minTrain;
        if (range <= 1) return "微型走前跳过：可测试区间不足";

        int cursor = parseInt(store.getState(STATE_CURSOR, "0"), 0);
        int seed = Math.abs(safeIssue(history.get(history.size() - 1)).hashCode());
        int startOffset = Math.abs(cursor * 37 + seed * 13) % range;
        int[] batchSample = new int[PROFILE_COUNT];
        int[] batchHit = new int[PROFILE_COUNT];
        ArrayList<Integer> usedIndex = new ArrayList<Integer>();

        for (int t = 0; t < targets; t++) {
            if (System.currentTimeMillis() - start >= budgetMs) break;
            int idx = minTrain + ((startOffset + t * 17) % range);
            if (idx <= 0 || idx >= history.size()) continue;
            usedIndex.add(Integer.valueOf(idx));
            RealtimeContext ctx = buildContext(history, idx, 0);
            Models.Draw actual = history.get(idx);
            for (int p = 0; p < PROFILE_COUNT; p++) {
                ctx.profileIndex = p;
                Prediction pred = bestPrediction(ctx);
                if (pred == null) continue;
                batchSample[p]++;
                int actualNumber = actual.getNumberAtPosition(pred.position);
                if (contains(pred, actualNumber)) batchHit[p]++;
            }
        }

        int bestProfile = 0;
        double bestScore = -999.0d;
        for (int p = 0; p < PROFILE_COUNT; p++) {
            int oldN = parseInt(store.getState(profileKey(p, "sample"), "0"), 0);
            int oldH = parseInt(store.getState(profileKey(p, "hit"), "0"), 0);
            int n = oldN + batchSample[p];
            int h = oldH + batchHit[p];
            /* keep a rolling bounded memory instead of an ever-growing stale counter */
            if (n > 720) {
                double k = 720.0d / n;
                n = 720;
                h = (int) Math.round(h * k);
            }
            store.setState(profileKey(p, "sample"), String.valueOf(n));
            store.setState(profileKey(p, "hit"), String.valueOf(h));
            double rate = (h + 9.0d) / (n + 30.0d); /* Beta prior centered at 30% */
            double lower = wilsonLower(h, n);
            double score = rate * 0.62d + lower * 0.38d + Math.min(0.015d, n / 48000.0d);
            if (score > bestScore) {
                bestScore = score;
                bestProfile = p;
            }
        }

        /* Update finite family statistics only with the selected profile. */
        int familyAdded = 0;
        boolean kernelReplayRecordedB07R3 = false;
        for (int u = 0; u < usedIndex.size(); u++) {
            if (System.currentTimeMillis() - start >= budgetMs) break;
            int idx = usedIndex.get(u).intValue();
            RealtimeContext ctx = buildContext(history, idx, bestProfile);
            Prediction pred = bestPrediction(ctx);
            if (pred == null) continue;
            int actualNumber = history.get(idx).getNumberAtPosition(pred.position);
            int y = contains(pred, actualNumber) ? 1 : 0;
            if (AppConfig.B07R3_KERNEL_REPLAY_ENABLED && !kernelReplayRecordedB07R3) {
                Models.ComboCandidate replay = new Models.ComboCandidate();
                replay.position = pred.position;
                replay.n1 = pred.n1;
                replay.n2 = pred.n2;
                replay.n3 = pred.n3;
                replay.baseIssue = idx > 0 ? safeIssue(history.get(idx - 1)) : "";
                replay.targetIssue = safeIssue(history.get(idx));
                StarcoreEvidenceKernel.observeReplay(
                        store, replay, actualNumber, "RollingCausalBacktestEngine");
                kernelReplayRecordedB07R3 = true;
            }
            updateFamily(store, pred.family, y);
            familyAdded++;
        }

        int totalN = parseInt(store.getState(profileKey(bestProfile, "sample"), "0"), 0);
        int totalH = parseInt(store.getState(profileKey(bestProfile, "hit"), "0"), 0);
        double totalRate = totalN <= 0 ? 0.0d : ((double) totalH) / totalN;
        long cost = System.currentTimeMillis() - start;
        int nextCursor = cursor + Math.max(1, usedIndex.size());
        String summary =
                "V23微型走前：profile="
                        + PROFILE_NAME[bestProfile]
                        + "｜本片="
                        + usedIndex.size()
                        + "目标"
                        + "｜累计="
                        + totalN
                        + "/中"
                        + totalH
                        + "/率"
                        + pct(totalRate)
                        + "｜family更新="
                        + familyAdded
                        + "｜预算="
                        + budgetMs
                        + "ms/耗时="
                        + cost
                        + "ms"
                        + "｜每个目标只使用其之前历史；随机滚动游标="
                        + nextCursor;
        store.setState(STATE_PROFILE, PROFILE_NAME[bestProfile]);
        store.setState(STATE_SAMPLE, String.valueOf(totalN));
        store.setState(STATE_HIT, String.valueOf(totalH));
        store.setState(STATE_RATE, String.valueOf(totalRate));
        store.setState(STATE_CURSOR, String.valueOf(nextCursor));
        store.setState(STATE_COST, String.valueOf(cost));
        store.setState(STATE_LAST_AT, String.valueOf(System.currentTimeMillis()));
        store.setState(STATE_SUMMARY, summary);
        return summary;
    }

    public static int resolveProfile(DbStore store) {
        String name = store == null ? "" : store.getState(STATE_PROFILE, "");
        for (int i = 0; i < PROFILE_NAME.length; i++) if (PROFILE_NAME[i].equals(name)) return i;
        return 0;
    }

    public static RealtimeContext buildRealtimeContext(List<Models.Draw> history, int profile) {
        ArrayList<Models.Draw> clean = normalize(history);
        return buildContext(clean, clean.size(), profile);
    }

    public static WindowEvidence scoreCandidate(
            DbStore store, RealtimeContext ctx, Models.ComboCandidate c) {
        WindowEvidence e = new WindowEvidence();
        if (ctx == null || c == null || !c.isValidK3() || c.position < 1 || c.position > 10)
            return e;
        double s1 = numberScore(ctx.feature[c.position][c.n1], ctx.profileIndex);
        double s2 = numberScore(ctx.feature[c.position][c.n2], ctx.profileIndex);
        double s3 = numberScore(ctx.feature[c.position][c.n3], ctx.profileIndex);
        double raw = clamp(s1 + s2 + s3, 0.16d, 0.55d);
        NumberFeature f1 = ctx.feature[c.position][c.n1];
        NumberFeature f2 = ctx.feature[c.position][c.n2];
        NumberFeature f3 = ctx.feature[c.position][c.n3];
        String family = familyKey(c.position, f1, f2, f3);
        int n =
                store == null
                        ? 0
                        : parseInt(store.getState(familyStateKey(family, "sample"), "0"), 0);
        int h = store == null ? 0 : parseInt(store.getState(familyStateKey(family, "hit"), "0"), 0);
        double familyP = (h + 6.0d) / (n + 20.0d);
        double familyWeight = Math.min(0.60d, Math.sqrt(n / 60.0d) * 0.60d);
        double p = raw * (1.0d - familyWeight) + familyP * familyWeight;
        double dispersion = std3(s1, s2, s3);
        double conf =
                clamp01(
                        0.28d
                                + Math.min(0.42d, ctx.historySize / 900.0d)
                                + Math.min(0.20d, n / 120.0d)
                                + Math.max(0.0d, 0.10d - dispersion));
        e.probability = clamp(p, 0.20d, 0.50d);
        e.confidence = conf;
        e.score = (s1 + s2 + s3) / 3.0d;
        e.effectiveSample = Math.max(n, Math.min(ctx.historySize, 360));
        e.coarseFamily = family;
        e.midFamily = family + "|P" + c.position + "|SP" + spreadBucket(c.n1, c.n2, c.n3);
        e.summary =
                "因果窗口：P="
                        + pct(e.probability)
                        + "｜conf="
                        + pct(conf)
                        + "｜profile="
                        + PROFILE_NAME[ctx.profileIndex]
                        + "｜family="
                        + family
                        + "｜family="
                        + n
                        + "/"
                        + h
                        + "｜只用目标期前历史";
        return e;
    }

    private static RealtimeContext buildContext(
            List<Models.Draw> history, int endExclusive, int profile) {
        RealtimeContext ctx = new RealtimeContext();
        ctx.profileIndex = profile < 0 || profile >= PROFILE_COUNT ? 0 : profile;
        ctx.historySize = Math.max(0, Math.min(endExclusive, history == null ? 0 : history.size()));
        for (int p = 1; p <= 10; p++)
            for (int n = 1; n <= 10; n++) {
                ctx.feature[p][n] = buildNumberFeature(history, ctx.historySize, p, n);
            }
        return ctx;
    }

    private static NumberFeature buildNumberFeature(
            List<Models.Draw> h, int end, int position, int number) {
        NumberFeature f = new NumberFeature();
        f.r12 = rate(h, end, position, number, 12);
        f.r24 = rate(h, end, position, number, 24);
        f.r48 = rate(h, end, position, number, 48);
        f.r96 = rate(h, end, position, number, 96);
        f.r360 = rate(h, end, position, number, 360);
        f.markov = transitionRate(h, end, position, number, 360);
        f.gap = omissionGap(h, end, position, number, 96);
        double expectedGap = 9.0d;
        double distance = Math.abs(f.gap - expectedGap) / 18.0d;
        double overDue = clamp((f.gap - 5.0d) / 28.0d, 0.0d, 1.0d);
        /* moderate omission is useful; extreme omission is not treated as certainty */
        f.omission =
                clamp(
                        0.10d + 0.07d * overDue - 0.035d * Math.max(0.0d, distance - 0.65d),
                        0.035d,
                        0.19d);
        f.momentum = clamp(0.10d + (f.r12 - f.r48) * 0.70d, 0.035d, 0.19d);
        double mean = (f.r12 + f.r24 + f.r48 + f.r96) / 4.0d;
        double var =
                ((f.r12 - mean) * (f.r12 - mean)
                                + (f.r24 - mean) * (f.r24 - mean)
                                + (f.r48 - mean) * (f.r48 - mean)
                                + (f.r96 - mean) * (f.r96 - mean))
                        / 4.0d;
        f.stability = clamp(0.14d - Math.sqrt(var) * 0.50d, 0.035d, 0.17d);
        return f;
    }

    private static Prediction bestPrediction(RealtimeContext ctx) {
        Prediction best = null;
        for (int p = 1; p <= 10; p++) {
            int[] order = new int[10];
            double[] value = new double[10];
            for (int n = 1; n <= 10; n++) {
                order[n - 1] = n;
                value[n - 1] = numberScore(ctx.feature[p][n], ctx.profileIndex);
            }
            sortNumbers(order, value);
            Prediction x = new Prediction();
            x.position = p;
            x.n1 = order[0];
            x.n2 = order[1];
            x.n3 = order[2];
            double top = value[0] + value[1] + value[2];
            double next = value[3] + value[4] + value[5];
            x.margin = top - next;
            x.probability = clamp(top, 0.18d, 0.52d);
            x.confidence =
                    clamp01(
                            0.35d
                                    + Math.max(0.0d, x.margin) * 2.5d
                                    + Math.min(0.30d, ctx.historySize / 1200.0d));
            x.family =
                    familyKey(p, ctx.feature[p][x.n1], ctx.feature[p][x.n2], ctx.feature[p][x.n3]);
            double score = x.probability + x.margin * 0.70d + x.confidence * 0.025d;
            if (best == null
                    || score > (best.probability + best.margin * 0.70d + best.confidence * 0.025d))
                best = x;
        }
        return best;
    }

    private static double numberScore(NumberFeature f, int profile) {
        if (f == null) return 0.10d;
        int i = profile < 0 || profile >= PROFILE_COUNT ? 0 : profile;
        double[] w = W[i];
        double s =
                f.r12 * w[0]
                        + f.r24 * w[1]
                        + f.r48 * w[2]
                        + f.r96 * w[3]
                        + f.r360 * w[4]
                        + f.markov * w[5]
                        + f.omission * w[6]
                        + f.momentum * w[7]
                        + f.stability * w[8];
        return clamp(s, 0.025d, 0.24d);
    }

    private static void sortNumbers(int[] number, double[] value) {
        for (int i = 1; i < number.length; i++) {
            int nn = number[i];
            double vv = value[i];
            int j = i - 1;
            while (j >= 0 && value[j] < vv) {
                number[j + 1] = number[j];
                value[j + 1] = value[j];
                j--;
            }
            number[j + 1] = nn;
            value[j + 1] = vv;
        }
    }

    private static boolean contains(Prediction p, int n) {
        return p != null && (n == p.n1 || n == p.n2 || n == p.n3);
    }

    private static double rate(List<Models.Draw> h, int end, int pos, int num, int window) {
        if (h == null || end <= 0) return 0.10d;
        int start = Math.max(0, end - window), valid = 0, hit = 0;
        for (int i = start; i < end; i++) {
            Models.Draw d = h.get(i);
            if (d == null || !d.isValid()) continue;
            valid++;
            if (d.getNumberAtPosition(pos) == num) hit++;
        }
        return (hit + 1.8d) / (valid + 18.0d);
    }

    private static double transitionRate(
            List<Models.Draw> h, int end, int pos, int num, int window) {
        if (h == null || end < 2) return 0.10d;
        int last = h.get(end - 1).getNumberAtPosition(pos);
        int start = Math.max(1, end - window), sample = 0, hit = 0;
        for (int i = start; i < end; i++) {
            Models.Draw prev = h.get(i - 1), cur = h.get(i);
            if (prev == null || cur == null || !prev.isValid() || !cur.isValid()) continue;
            if (prev.getNumberAtPosition(pos) == last) {
                sample++;
                if (cur.getNumberAtPosition(pos) == num) hit++;
            }
        }
        return (hit + 1.2d) / (sample + 12.0d);
    }

    private static int omissionGap(List<Models.Draw> h, int end, int pos, int num, int cap) {
        int gap = cap;
        if (h == null) return gap;
        for (int i = end - 1; i >= 0 && end - 1 - i < cap; i--) {
            Models.Draw d = h.get(i);
            if (d != null && d.isValid() && d.getNumberAtPosition(pos) == num) return end - 1 - i;
        }
        return gap;
    }

    private static String familyKey(
            int position, NumberFeature a, NumberFeature b, NumberFeature c) {
        String pg = position <= 3 ? "FRONT" : (position <= 7 ? "MID" : "BACK");
        double trend = ((a.r12 - a.r48) + (b.r12 - b.r48) + (c.r12 - c.r48)) / 3.0d;
        String tr = trend > 0.018d ? "UP" : (trend < -0.018d ? "DOWN" : "FLAT");
        double gap = (a.gap + b.gap + c.gap) / 3.0d;
        String om = gap < 5.0d ? "OLOW" : (gap < 14.0d ? "OMID" : "OHIGH");
        double mk = (a.markov + b.markov + c.markov) / 3.0d;
        String ma = mk >= 0.125d ? "MREADY" : "MWEAK";
        return "CW|" + pg + "|" + tr + "|" + om + "|" + ma;
    }

    private static String spreadBucket(int a, int b, int c) {
        int lo = Math.min(a, Math.min(b, c)), hi = Math.max(a, Math.max(b, c)), s = hi - lo;
        return s <= 3 ? "TIGHT" : (s <= 6 ? "MID" : "WIDE");
    }

    private static void updateFamily(DbStore store, String family, int hit) {
        if (store == null || family == null || family.length() == 0) return;
        String sk = familyStateKey(family, "sample"), hk = familyStateKey(family, "hit");
        int n = parseInt(store.getState(sk, "0"), 0), h = parseInt(store.getState(hk, "0"), 0);
        n++;
        h += hit;
        if (n > 360) {
            double k = 360.0d / n;
            n = 360;
            h = (int) Math.round(h * k);
        }
        store.setState(sk, String.valueOf(n));
        store.setState(hk, String.valueOf(h));
    }

    private static String familyStateKey(String family, String suffix) {
        return "v23_family_" + Integer.toHexString(family.hashCode()) + "_" + suffix;
    }

    private static String profileKey(int p, String suffix) {
        return "v23_profile_" + p + "_" + suffix;
    }

    private static double wilsonLower(int h, int n) {
        if (n <= 0) return 0.0d;
        double z = 1.281551565545d,
                p = ((double) h) / n,
                z2 = z * z,
                den = 1.0d + z2 / n,
                center = p + z2 / (2.0d * n),
                adj = z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * n)) / n);
        return clamp((center - adj) / den, 0.0d, 1.0d);
    }

    private static double std3(double a, double b, double c) {
        double m = (a + b + c) / 3.0d;
        return Math.sqrt(((a - m) * (a - m) + (b - m) * (b - m) + (c - m) * (c - m)) / 3.0d);
    }

    private static ArrayList<Models.Draw> normalize(List<Models.Draw> in) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
        if (in != null)
            for (int i = 0; i < in.size(); i++) {
                Models.Draw d = in.get(i);
                if (d != null && d.isValid()) out.add(d);
            }
        Collections.sort(
                out,
                new Comparator<Models.Draw>() {
                    public int compare(Models.Draw a, Models.Draw b) {
                        return safeIssue(a).compareTo(safeIssue(b));
                    }
                });
        return out;
    }

    private static String safeIssue(Models.Draw d) {
        return d == null || d.issue == null ? "" : d.issue;
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Throwable e) {
            return def;
        }
    }

    private static double clamp01(double v) {
        return clamp(v, 0.0d, 1.0d);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static String pct(double p) {
        return String.format(Locale.US, "%.1f%%", p * 100.0d);
    }
}
