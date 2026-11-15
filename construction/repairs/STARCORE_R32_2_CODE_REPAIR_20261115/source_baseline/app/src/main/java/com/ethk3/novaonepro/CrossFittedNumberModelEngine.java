package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Time-ordered challenger that learns component weights from earlier targets and validates them on
 * a later holdout block. It predicts a normalized ten-number distribution per position.
 */
public final class CrossFittedNumberModelEngine {
    public static final String MODULE_VERSION = "V47_CROSSFIT_NUMBER_MODEL_20260721_47072147";
    private static final int RECENT = 0, EWMA = 1, MARKOV = 2, GAP = 3, COMPONENTS = 4;

    public static final class Snapshot {
        public final double[][] probability = new double[10][10];
        public final double[][] weight = new double[10][COMPONENTS];
        public int historySize;
        public int validationTargets;
        public int validationRows;
        public int validationHits;
        public double validationTop3Rate;
        public double validationBrier;
        public double uniformBrier;
        public double brierGain;
        public boolean champion;
        public String baseIssue = "";
        public long costMs;
        public String status = "EMPTY";

        public String summary() {
            return "status="
                    + status
                    + "|history="
                    + historySize
                    + "|validationTargets="
                    + validationTargets
                    + "|top3="
                    + fmt(validationTop3Rate)
                    + "|brier="
                    + fmt(validationBrier)
                    + "|uniform="
                    + fmt(uniformBrier)
                    + "|gain="
                    + fmt(brierGain)
                    + "|champion="
                    + (champion ? 1 : 0)
                    + "|cost="
                    + costMs
                    + "ms|base="
                    + baseIssue;
        }
    }

    public Snapshot build(List<Models.Draw> input, SequenceDistributionEngine.Forecast base) {
        long start = System.currentTimeMillis();
        ArrayList<Models.Draw> h = clean(input, AppConfig.V47_CROSSFIT_MAX_HISTORY);
        Snapshot s = new Snapshot();
        s.historySize = h.size();
        if (!h.isEmpty()) s.baseIssue = safe(h.get(h.size() - 1).issue);
        initUniform(s);
        if (h.size() < AppConfig.V47_CROSSFIT_MIN_PREFIX + 5) {
            s.status = "WARM";
            s.costMs = System.currentTimeMillis() - start;
            return s;
        }
        int validation =
                Math.min(
                        AppConfig.V47_CROSSFIT_VALIDATION_TARGETS,
                        Math.max(5, h.size() - AppConfig.V47_CROSSFIT_MIN_PREFIX));
        int split = Math.max(AppConfig.V47_CROSSFIT_MIN_PREFIX, h.size() - validation);
        s.validationTargets = h.size() - split;
        for (int p = 1; p <= 10; p++) learnWeights(h, split, p, s.weight[p - 1]);
        double brier = 0.0, uniform = 0.0;
        int hits = 0, rows = 0;
        for (int t = split; t < h.size(); t++) {
            for (int p = 1; p <= 10; p++) {
                double[] d = ensembleAt(h, t, p, s.weight[p - 1]);
                int y = h.get(t).getNumberAtPosition(p);
                if (inTop3(d, y)) hits++;
                brier += brier(d, y);
                uniform += 0.90d;
                rows++;
            }
        }
        s.validationRows = rows;
        s.validationHits = hits;
        s.validationTop3Rate = rows == 0 ? 0.0d : (double) hits / rows;
        s.validationBrier = rows == 0 ? 0.0d : brier / rows;
        s.uniformBrier = rows == 0 ? 0.90d : uniform / rows;
        s.brierGain = s.uniformBrier - s.validationBrier;
        s.champion =
                s.validationTargets >= AppConfig.V47_CROSSFIT_MIN_VALIDATION_TARGETS
                        && s.validationTop3Rate >= AppConfig.V47_CHAMPION_MIN_TOP3_RATE
                        && s.brierGain >= AppConfig.V47_CHAMPION_MIN_BRIER_GAIN;
        for (int p = 1; p <= 10; p++) {
            double[] learned = ensembleAt(h, h.size(), p, s.weight[p - 1]);
            double blend = s.champion ? AppConfig.V47_CHAMPION_BLEND : 0.0d;
            for (int n = 0; n < 10; n++) {
                double fallback = base == null ? 0.10d : base.probability[p - 1][n];
                s.probability[p - 1][n] = (1.0d - blend) * fallback + blend * learned[n];
            }
            normalize(s.probability[p - 1]);
        }
        s.status = s.champion ? "CHAMPION" : "CHALLENGER_SHADOW";
        s.costMs = System.currentTimeMillis() - start;
        return s;
    }

    public void apply(Snapshot s, Models.ComboCandidate c) {
        if (c == null || !c.isValidK3()) return;
        double p =
                (s == null
                        ? 0.30d
                        : s.probability[c.position - 1][c.n1 - 1]
                                + s.probability[c.position - 1][c.n2 - 1]
                                + s.probability[c.position - 1][c.n3 - 1]);
        p = clamp(p, 0.03d, 0.80d);
        c.basePBeforeFusion = p;
        c.modelP = p;
        c.rawP = p;
        c.v46EventProbability = p;
        c.v46Version = AppConfig.V48_VERSION;
        c.probabilityMode = "V47_CROSSFIT_TEN_NUMBER";
        c.probabilityFieldVersion = MODULE_VERSION;
        c.v46NumberDistributionSummary =
                "V48LegacyChallenger|P"
                        + c.position
                        + "|K3="
                        + fmt(p)
                        + "|"
                        + (s == null ? "EMPTY" : s.summary());
        c.v46ProbabilityTrace = append(c.v46ProbabilityTrace, "V48LegacyChallenger=" + fmt(p));
        if (s != null) {
            c.evidenceForestSample = Math.max(c.evidenceForestSample, s.historySize);
            c.historicalPriorSample = Math.max(c.historicalPriorSample, s.historySize);
        }
    }

    private void learnWeights(List<Models.Draw> h, int split, int p, double[] out) {
        double[] loss = new double[COMPONENTS];
        int rows = 0;
        int start = Math.max(AppConfig.V47_CROSSFIT_MIN_PREFIX, split - 120);
        for (int t = start; t < split; t++) {
            int y = h.get(t).getNumberAtPosition(p);
            double[][] d = componentsAt(h, t, p);
            for (int k = 0; k < COMPONENTS; k++) loss[k] += -Math.log(Math.max(1e-6, d[k][y - 1]));
            rows++;
        }
        if (rows == 0) {
            for (int k = 0; k < COMPONENTS; k++) out[k] = 0.25d;
            return;
        }
        double best = Double.POSITIVE_INFINITY;
        for (int k = 0; k < COMPONENTS; k++) {
            loss[k] /= rows;
            best = Math.min(best, loss[k]);
        }
        double sum = 0.0;
        for (int k = 0; k < COMPONENTS; k++) {
            out[k] = Math.exp(-3.0d * (loss[k] - best));
            sum += out[k];
        }
        for (int k = 0; k < COMPONENTS; k++) out[k] = 0.20d * 0.25d + 0.80d * (out[k] / sum);
        normalize(out);
    }

    private double[] ensembleAt(List<Models.Draw> h, int end, int p, double[] w) {
        double[][] d = componentsAt(h, end, p);
        double[] out = new double[10];
        for (int k = 0; k < COMPONENTS; k++) for (int n = 0; n < 10; n++) out[n] += w[k] * d[k][n];
        normalize(out);
        return out;
    }

    private double[][] componentsAt(List<Models.Draw> h, int end, int p) {
        return new double[][] {
            recent(h, end, p), ewma(h, end, p), markov(h, end, p), gap(h, end, p)
        };
    }

    private double[] recent(List<Models.Draw> h, int end, int p) {
        double[] x = new double[10];
        int start = Math.max(0, end - 48), n = end - start;
        for (int i = 0; i < 10; i++) x[i] = 1.5d;
        for (int i = start; i < end; i++) {
            int v = h.get(i).getNumberAtPosition(p);
            if (v >= 1 && v <= 10) x[v - 1]++;
        }
        normalize(x);
        return x;
    }

    private double[] ewma(List<Models.Draw> h, int end, int p) {
        double[] x = new double[10];
        for (int i = 0; i < 10; i++) x[i] = 0.8d;
        double w = 1.0d;
        for (int i = end - 1; i >= 0 && end - i <= 96; i--) {
            int v = h.get(i).getNumberAtPosition(p);
            if (v >= 1 && v <= 10) x[v - 1] += w;
            w *= 0.94d;
        }
        normalize(x);
        return x;
    }

    private double[] markov(List<Models.Draw> h, int end, int p) {
        double[] x = new double[10];
        for (int i = 0; i < 10; i++) x[i] = 1.0d;
        if (end <= 0) {
            normalize(x);
            return x;
        }
        int prev = h.get(end - 1).getNumberAtPosition(p);
        for (int i = 1; i < end; i++) {
            if (h.get(i - 1).getNumberAtPosition(p) == prev) {
                int v = h.get(i).getNumberAtPosition(p);
                if (v >= 1 && v <= 10) x[v - 1]++;
            }
        }
        normalize(x);
        return x;
    }

    private double[] gap(List<Models.Draw> h, int end, int p) {
        double[] x = new double[10];
        for (int n = 1; n <= 10; n++) {
            int g = 0;
            for (int i = end - 1; i >= 0; i--) {
                if (h.get(i).getNumberAtPosition(p) == n) break;
                g++;
                if (g >= 30) break;
            }
            x[n - 1] = 1.0d + 0.025d * Math.min(g, 20);
        }
        normalize(x);
        return x;
    }

    private static boolean inTop3(double[] d, int y) {
        int[] idx = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int i = 0; i < 3; i++) {
            int best = i;
            for (int k = i + 1; k < 10; k++) if (d[idx[k]] > d[idx[best]]) best = k;
            int t = idx[i];
            idx[i] = idx[best];
            idx[best] = t;
        }
        return y == idx[0] + 1 || y == idx[1] + 1 || y == idx[2] + 1;
    }

    private static double brier(double[] d, int y) {
        double z = 0;
        for (int n = 0; n < 10; n++) {
            double a = d[n] - ((n + 1) == y ? 1.0d : 0.0d);
            z += a * a;
        }
        return z;
    }

    private static ArrayList<Models.Draw> clean(List<Models.Draw> input, int max) {
        ArrayList<Models.Draw> x = new ArrayList<Models.Draw>();
        if (input != null) for (Models.Draw d : input) if (d != null && d.isValid()) x.add(d);
        Collections.sort(
                x,
                new Comparator<Models.Draw>() {
                    public int compare(Models.Draw left, Models.Draw right) {
                        return safe(left == null ? null : left.issue)
                                .compareTo(safe(right == null ? null : right.issue));
                    }
                });
        if (x.size() > max) return new ArrayList<Models.Draw>(x.subList(x.size() - max, x.size()));
        return x;
    }

    private static void initUniform(Snapshot s) {
        for (int p = 0; p < 10; p++) {
            for (int n = 0; n < 10; n++) s.probability[p][n] = 0.10d;
            for (int k = 0; k < COMPONENTS; k++) s.weight[p][k] = 0.25d;
        }
    }

    private static void normalize(double[] x) {
        double sum = 0;
        for (int i = 0; i < x.length; i++) {
            if (!finite(x[i]) || x[i] < 0) x[i] = 0;
            sum += x[i];
        }
        if (sum <= 0) {
            for (int i = 0; i < x.length; i++) x[i] = 1.0d / x.length;
        } else for (int i = 0; i < x.length; i++) x[i] /= sum;
    }

    private static double clamp(double v, double lo, double hi) {
        return !finite(v) ? lo : Math.max(lo, Math.min(hi, v));
    }

    private static boolean finite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private static String append(String a, String b) {
        if (a == null || a.isEmpty()) return b;
        if (a.contains(b)) return a;
        return a + "|" + b;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String fmt(double v) {
        return String.format(Locale.US, "%.4f", v);
    }
}
