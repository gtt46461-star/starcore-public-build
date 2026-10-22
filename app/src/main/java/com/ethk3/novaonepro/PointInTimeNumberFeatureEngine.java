package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Builds a 10-position x 10-number point-in-time feature cube using only draws strictly earlier
 * than the target. No target label or future state is accepted.
 */
public final class PointInTimeNumberFeatureEngine {
    public static final String MODULE_VERSION =
            "V48_POINT_IN_TIME_NUMBER_FEATURES_20260721_48072148";
    public static final int FEATURE_COUNT = 24;
    private static final int[] WINDOWS = new int[] {12, 24, 48, 96, 240, 360};

    public static final class Snapshot {
        public final double[][][] feature = new double[10][10][FEATURE_COUNT];
        public int historySize;
        public String baseIssue = "";
        public long costMs;
        public boolean pointInTime;

        public String summary() {
            return "PIT=1|history="
                    + historySize
                    + "|features="
                    + FEATURE_COUNT
                    + "|base="
                    + baseIssue
                    + "|cost="
                    + costMs
                    + "ms";
        }
    }

    public Snapshot build(List<Models.Draw> input) {
        long start = System.currentTimeMillis();
        ArrayList<Models.Draw> history = clean(input, AppConfig.V48_FEATURE_MAX_HISTORY);
        Snapshot snapshot = new Snapshot();
        snapshot.historySize = history.size();
        snapshot.pointInTime = true;
        if (!history.isEmpty()) snapshot.baseIssue = safe(history.get(history.size() - 1).issue);
        for (int position = 1; position <= 10; position++) {
            int last =
                    history.isEmpty()
                            ? 0
                            : history.get(history.size() - 1).getNumberAtPosition(position);
            int previous =
                    history.size() < 2
                            ? 0
                            : history.get(history.size() - 2).getNumberAtPosition(position);
            for (int number = 1; number <= 10; number++) {
                double[] f = snapshot.feature[position - 1][number - 1];
                f[0] = 1.0d;
                for (int w = 0; w < WINDOWS.length; w++)
                    f[1 + w] = frequency(history, position, number, WINDOWS[w]);
                f[7] = ewma(history, position, number, 0.94d, 96);
                f[8] = markov1(history, position, last, number, 240);
                f[9] = markov2(history, position, previous, last, number, 360);
                int gap = gap(history, position, number, 30);
                f[10] = gap / 30.0d;
                f[11] = gapDelta(history, position, number) / 30.0d;
                f[12] = slope(f[1], f[3]);
                f[13] = rebound(f[1], f[4]);
                f[14] = neighbor(last, number);
                f[15] = sameTail(last, number);
                f[16] = last <= 0 ? 0.5d : (((last & 1) == (number & 1)) ? 1.0d : 0.0d);
                f[17] = last <= 0 ? 0.5d : (((last >= 6) == (number >= 6)) ? 1.0d : 0.0d);
                f[18] = positionDensity(history, position, number, 48);
                f[19] = crossPositionPrevious(history, number);
                f[20] = teacherProxy(f);
                f[21] = dynamicProxy(f);
                f[22] = counterfactualProxy(f);
                f[23] = history.size() < 30 ? 1.0d : 0.0d;
            }
        }
        snapshot.costMs = System.currentTimeMillis() - start;
        return snapshot;
    }

    private static double frequency(List<Models.Draw> h, int p, int n, int window) {
        int end = h.size(), start = Math.max(0, end - window), count = 0;
        for (int i = start; i < end; i++) if (h.get(i).getNumberAtPosition(p) == n) count++;
        return end == start ? 0.10d : ((double) count) / (end - start);
    }

    private static double ewma(List<Models.Draw> h, int p, int n, double decay, int cap) {
        double hit = 0, total = 0, w = 1.0d;
        int start = Math.max(0, h.size() - cap);
        for (int i = h.size() - 1; i >= start; i--) {
            total += w;
            if (h.get(i).getNumberAtPosition(p) == n) hit += w;
            w *= decay;
        }
        return total <= 0 ? 0.10d : (hit + 0.20d) / (total + 2.0d);
    }

    private static double markov1(List<Models.Draw> h, int p, int last, int n, int cap) {
        if (last <= 0) return 0.10d;
        double hit = 1, total = 10;
        int start = Math.max(1, h.size() - cap);
        for (int i = start; i < h.size(); i++)
            if (h.get(i - 1).getNumberAtPosition(p) == last) {
                total++;
                if (h.get(i).getNumberAtPosition(p) == n) hit++;
            }
        return hit / total;
    }

    private static double markov2(List<Models.Draw> h, int p, int prev, int last, int n, int cap) {
        if (prev <= 0 || last <= 0) return 0.10d;
        double hit = 1, total = 10;
        int start = Math.max(2, h.size() - cap);
        for (int i = start; i < h.size(); i++)
            if (h.get(i - 2).getNumberAtPosition(p) == prev
                    && h.get(i - 1).getNumberAtPosition(p) == last) {
                total++;
                if (h.get(i).getNumberAtPosition(p) == n) hit++;
            }
        return hit / total;
    }

    private static int gap(List<Models.Draw> h, int p, int n, int cap) {
        int g = 0;
        for (int i = h.size() - 1; i >= 0 && g < cap; i--) {
            if (h.get(i).getNumberAtPosition(p) == n) break;
            g++;
        }
        return g;
    }

    private static double gapDelta(List<Models.Draw> h, int p, int n) {
        int last = -1, second = -1;
        for (int i = h.size() - 1; i >= 0; i--) {
            if (h.get(i).getNumberAtPosition(p) == n) {
                if (last < 0) last = h.size() - 1 - i;
                else {
                    second = h.size() - 1 - i;
                    break;
                }
            }
        }
        if (last < 0) return 0;
        if (second < 0) return last;
        return last - (second - last);
    }

    private static double slope(double shortRate, double longRate) {
        return clamp(0.5d + 4.0d * (shortRate - longRate));
    }

    private static double rebound(double shortRate, double longRate) {
        return clamp(0.5d + 3.0d * (longRate - shortRate));
    }

    private static double neighbor(int last, int n) {
        if (last <= 0) return 0.5d;
        int d = Math.abs(last - n);
        return d == 1 || d == 9 ? 1.0d : 0.0d;
    }

    private static double sameTail(int last, int n) {
        return last <= 0 ? 0.5d : (last % 10 == n % 10 ? 1.0d : 0.0d);
    }

    private static double positionDensity(List<Models.Draw> h, int p, int n, int window) {
        double f = frequency(h, p, n, window);
        return clamp(f / 0.20d);
    }

    private static double crossPositionPrevious(List<Models.Draw> h, int n) {
        if (h.isEmpty()) return 0.5d;
        Models.Draw d = h.get(h.size() - 1);
        for (int p = 1; p <= 10; p++) if (d.getNumberAtPosition(p) == n) return (p - 1) / 9.0d;
        return 0.5d;
    }

    private static double teacherProxy(double[] f) {
        return clamp(0.45d + 0.35d * (f[7] - 0.10d) + 0.20d * (f[8] - 0.10d));
    }

    private static double dynamicProxy(double[] f) {
        return clamp(0.50d + 0.35d * (f[12] - 0.50d) - 0.20d * Math.abs(f[1] - f[4]));
    }

    private static double counterfactualProxy(double[] f) {
        return clamp(0.50d + 0.25d * (f[8] - f[7]) + 0.20d * (f[10] - 0.50d));
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

    private static double clamp(double v) {
        return finite(v) ? Math.max(0.0d, Math.min(1.0d, v)) : 0.0d;
    }

    private static boolean finite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
