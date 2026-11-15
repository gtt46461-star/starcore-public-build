package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Strict time-ordered single-action model comparison. Each target contributes exactly one chosen
 * position/K3 action. A challenger has zero live influence unless every promotion contract is
 * satisfied.
 */
public final class WalkForwardChampionEngine {
    public static final String MODULE_VERSION =
            "V48_WALK_FORWARD_SINGLE_ACTION_CHAMPION_20260721_48072148";
    public static final String MODEL_UNIFORM = "UNIFORM";
    public static final String MODEL_LINEAR = ModelArtifactManifest.MODEL_ID;
    public static final String STATE_STATUS = "v48_champion_status";
    public static final String STATE_MODEL = "v48_champion_model";
    public static final String STATE_SUMMARY = "v48_champion_summary";
    public static final String STATE_BASE = "v48_champion_base_issue";

    public static final class Metrics {
        public String model = "";
        public int sample, hit;
        public double rate, wilson, pnl, brier, logLoss, ece;
        public final int[] blockN = new int[4];
        public final int[] blockHit = new int[4];
        public int goodBlocks;
        public boolean promoted;

        public String summary() {
            return model
                    + "|n="
                    + sample
                    + "|hit="
                    + hit
                    + "|rate="
                    + fmt(rate)
                    + "|W="
                    + fmt(wilson)
                    + "|pnl="
                    + String.format(Locale.US, "%.1f", pnl)
                    + "|brier="
                    + fmt(brier)
                    + "|logloss="
                    + fmt(logLoss)
                    + "|goodBlocks="
                    + goodBlocks
                    + "|promoted="
                    + (promoted ? 1 : 0);
        }
    }

    public static final class Result {
        public boolean champion;
        public String modelId = MODEL_UNIFORM;
        public Metrics uniform;
        public Metrics linear;
        public String baseIssue = "";
        public long costMs;

        public String summary() {
            return "champion="
                    + (champion ? 1 : 0)
                    + "|model="
                    + modelId
                    + "|base="
                    + baseIssue
                    + "|uniform="
                    + (uniform == null ? "--" : uniform.summary())
                    + "|linear="
                    + (linear == null ? "--" : linear.summary())
                    + "|cost="
                    + costMs
                    + "ms";
        }
    }

    private WalkForwardChampionEngine() {}

    public static Result evaluate(DbStore store, List<Models.Draw> input) {
        long start = System.currentTimeMillis();
        ArrayList<Models.Draw> h = clean(input, AppConfig.V48_CHAMPION_MAX_HISTORY);
        Result out = new Result();
        if (!h.isEmpty()) out.baseIssue = safe(h.get(h.size() - 1).issue);
        Result cached = loadCached(store, out.baseIssue);
        if (cached != null) return cached;
        int first =
                Math.max(
                        AppConfig.V48_POINT_IN_TIME_MIN_PREFIX,
                        h.size() - AppConfig.V48_CHAMPION_VALIDATION_TARGETS);
        out.uniform = evaluateModel(h, first, MODEL_UNIFORM);
        out.linear = evaluateModel(h, first, MODEL_LINEAR);
        Metrics selected = out.linear != null && out.linear.promoted ? out.linear : null;
        out.champion = selected != null;
        out.modelId = out.champion ? selected.model : MODEL_UNIFORM;
        out.costMs = System.currentTimeMillis() - start;
        publish(store, out);
        return out;
    }

    private static Metrics evaluateModel(ArrayList<Models.Draw> h, int first, String model) {
        Metrics m = new Metrics();
        m.model = model;
        if (h.size() <= first) return m;
        double probError = 0, log = 0, calErr = 0;
        for (int t = first; t < h.size(); t++) {
            List<Models.Draw> prefix = h.subList(0, t);
            double[][] distribution;
            if (MODEL_LINEAR.equals(model)) {
                PointInTimeNumberFeatureEngine.Snapshot f =
                        new PointInTimeNumberFeatureEngine().build(prefix);
                distribution =
                        LightweightModelInferenceEngine.predict(f, ModelArtifactManifest.current())
                                .probability;
            } else distribution = uniform();
            Action a = bestAction(distribution);
            int actual = h.get(t).getNumberAtPosition(a.position);
            int hit = a.contains(actual) ? 1 : 0;
            m.sample++;
            m.hit += hit;
            m.pnl += hit == 1 ? AppConfig.WIN_NET_PROFIT : AppConfig.LOSE_NET_PROFIT;
            double p = clamp(a.probability, 0.001d, 0.999d);
            double e = p - hit;
            probError += e * e;
            log += -(hit * Math.log(p) + (1 - hit) * Math.log(1 - p));
            calErr += Math.abs(p - hit);
            int block =
                    Math.min(3, (int) (((long) (t - first) * 4L) / Math.max(1, h.size() - first)));
            m.blockN[block]++;
            m.blockHit[block] += hit;
        }
        m.rate = rate(m.hit, m.sample);
        m.wilson = wilson(m.hit, m.sample);
        m.brier = m.sample == 0 ? 0 : probError / m.sample;
        m.logLoss = m.sample == 0 ? 0 : log / m.sample;
        m.ece = m.sample == 0 ? 0 : calErr / m.sample;
        for (int b = 0; b < 4; b++)
            if (m.blockN[b] > 0
                    && rate(m.blockHit[b], m.blockN[b]) >= AppConfig.V48_CHAMPION_MIN_BLOCK_RATE)
                m.goodBlocks++;
        m.promoted =
                m.sample >= AppConfig.V48_CHAMPION_MIN_ACTION_SAMPLE
                        && m.rate >= AppConfig.V48_CHAMPION_MIN_ACTION_RATE
                        && m.wilson > AppConfig.BREAK_EVEN_PROBABILITY
                        && m.pnl > 0.0d
                        && m.goodBlocks >= 3;
        return m;
    }

    private static final class Action {
        int position, n1, n2, n3;
        double probability;

        boolean contains(int n) {
            return n == n1 || n == n2 || n == n3;
        }
    }

    private static Action bestAction(double[][] d) {
        Action best = null;
        for (int p = 0; p < 10; p++) {
            int[] idx = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
            for (int i = 0; i < 3; i++) {
                int b = i;
                for (int j = i + 1; j < 10; j++) if (d[p][idx[j]] > d[p][idx[b]]) b = j;
                int z = idx[i];
                idx[i] = idx[b];
                idx[b] = z;
            }
            Action a = new Action();
            a.position = p + 1;
            a.n1 = idx[0] + 1;
            a.n2 = idx[1] + 1;
            a.n3 = idx[2] + 1;
            a.probability = d[p][idx[0]] + d[p][idx[1]] + d[p][idx[2]];
            if (best == null || a.probability > best.probability) best = a;
        }
        return best;
    }

    private static double[][] uniform() {
        double[][] d = new double[10][10];
        for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) d[p][n] = 0.10d;
        return d;
    }

    private static void publish(DbStore store, Result r) {
        try {
            if (store != null) {
                store.setState(STATE_STATUS, r.champion ? "CHAMPION" : "MODEL_NOT_PROVEN");
                store.setState(STATE_MODEL, r.modelId);
                store.setState(STATE_SUMMARY, r.summary());
                store.setState(STATE_BASE, r.baseIssue);
                store.setState("v48_challenger_live_weight", r.champion ? "1.0" : "0.0");
                store.setState("v48_champion_version", MODULE_VERSION);
            }
        } catch (Throwable ignore) {
        }
    }

    private static Result loadCached(DbStore store, String base) {
        if (store == null || base.length() == 0) return null;
        try {
            if (!base.equals(store.getState(STATE_BASE, ""))) return null;
            String status = store.getState(STATE_STATUS, "");
            String summary = store.getState(STATE_SUMMARY, "");
            if (summary.length() == 0) return null;
            Result r = new Result();
            r.baseIssue = base;
            r.champion = "CHAMPION".equals(status);
            r.modelId = store.getState(STATE_MODEL, MODEL_UNIFORM);
            return r;
        } catch (Throwable e) {
            return null;
        }
    }

    private static ArrayList<Models.Draw> clean(List<Models.Draw> input, int max) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
        if (input != null) for (Models.Draw d : input) if (d != null && d.isValid()) out.add(d);
        Collections.sort(
                out,
                new Comparator<Models.Draw>() {
                    public int compare(Models.Draw left, Models.Draw right) {
                        return safe(left == null ? null : left.issue)
                                .compareTo(safe(right == null ? null : right.issue));
                    }
                });
        if (out.size() > max)
            return new ArrayList<Models.Draw>(out.subList(out.size() - max, out.size()));
        return out;
    }

    private static double rate(int h, int n) {
        return n <= 0 ? 0.0d : (double) h / n;
    }

    private static double wilson(int hit, int n) {
        if (n <= 0) return 0;
        double z = 1.6448536269514722d,
                p = (double) hit / n,
                den = 1 + z * z / n,
                center = p + z * z / (2 * n),
                margin = z * Math.sqrt((p * (1 - p) + z * z / (4 * n)) / n);
        return Math.max(0, (center - margin) / den);
    }

    private static double clamp(double v, double lo, double hi) {
        return !Double.isNaN(v) && !Double.isInfinite(v) ? Math.max(lo, Math.min(hi, v)) : lo;
    }

    private static String fmt(double v) {
        return String.format(Locale.US, "%.4f", v);
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
