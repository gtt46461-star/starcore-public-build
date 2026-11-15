package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Lightweight Matrix-Profile-style nearest motif expert for categorical position sequences. */
public final class MotifMatrixProfileEngine implements NumberExpert {
    public static final String MODULE_VERSION = V49BuildManifest.EXPERT_SCHEMA + ":MOTIF";

    public String name() {
        return "GOLDEN_MOTIF";
    }

    public String version() {
        return MODULE_VERSION;
    }

    public Result evaluate(Context c) {
        Result r = new Result(name(), version());
        if (c == null || c.features == null || c.history == null || c.history.size() < 36) {
            r.valid = false;
            return r;
        }
        List<Models.Draw> h = c.history;
        for (int p = 0; p < 10; p++) {
            double[] combined = new double[10];
            double total = 0.0d;
            int neighbors = 0;
            double meanDistance = 0.0d;
            int[] lengths = new int[] {6, 12, 24};
            double[] lengthWeight = new double[] {0.22d, 0.48d, 0.30d};
            for (int li = 0; li < lengths.length; li++) {
                int len = lengths[li];
                if (h.size() < len * 2 + 2) continue;
                ArrayList<Neighbor> list = new ArrayList<Neighbor>();
                int currentStart = h.size() - len;
                int earliest = Math.max(len, h.size() - 720);
                for (int end = earliest; end < currentStart - 2; end++) {
                    int start = end - len;
                    if (start < 0 || end >= h.size()) continue;
                    double d = distance(h, p + 1, start, currentStart, len);
                    int next = h.get(end).getNumberAtPosition(p + 1);
                    if (next >= 1 && next <= 10) list.add(new Neighbor(d, next));
                }
                Collections.sort(
                        list,
                        new Comparator<Neighbor>() {
                            public int compare(Neighbor a, Neighbor b) {
                                return Double.compare(a.distance, b.distance);
                            }
                        });
                int cap = Math.min(AppConfig.V49_MOTIF_NEIGHBORS, list.size());
                if (cap <= 0) continue;
                double localWeight = 0.0d;
                for (int i = 0; i < cap; i++) {
                    Neighbor x = list.get(i);
                    double w =
                            lengthWeight[li] * Math.exp(-3.0d * x.distance) / Math.sqrt(i + 1.0d);
                    combined[x.next - 1] += w;
                    localWeight += w;
                    meanDistance += x.distance;
                    neighbors++;
                }
                total += localWeight;
            }
            if (total <= 1.0e-12d) {
                for (int n = 0; n < 10; n++) r.probability[p][n] = c.features.frequency[4][p][n];
            } else {
                for (int n = 0; n < 10; n++)
                    r.probability[p][n] = combined[n] + 0.08d * c.features.frequency[6][p][n];
            }
            FeatureContractRegistry.normalize10(r.probability[p]);
            meanDistance = neighbors == 0 ? 1.0d : meanDistance / neighbors;
            r.trust[p] =
                    FeatureContractRegistry.clamp01(
                            0.20d
                                    + Math.min(0.55d, neighbors / 100.0d)
                                    + 0.25d * (1.0d - meanDistance));
            r.uncertainty[p] =
                    FeatureContractRegistry.clamp01(
                            0.25d
                                    + meanDistance * 0.55d
                                    + FeatureContractRegistry.entropy10(r.probability[p]) * 0.20d);
            r.summary[p] = "黄金motif近邻=" + neighbors + " meanDistance=" + format(meanDistance);
        }
        r.normalize();
        return r;
    }

    private static double distance(
            List<Models.Draw> h, int position, int aStart, int bStart, int len) {
        double sum = 0.0d;
        for (int k = 0; k < len; k++) {
            int a = h.get(aStart + k).getNumberAtPosition(position);
            int b = h.get(bStart + k).getNumberAtPosition(position);
            double exact = a == b ? 0.0d : 0.55d;
            double numeric = Math.abs(a - b) / 9.0d;
            double parity = ((a & 1) == (b & 1)) ? 0.0d : 0.12d;
            double big = ((a >= 6) == (b >= 6)) ? 0.0d : 0.12d;
            sum += exact + numeric * 0.21d + parity + big;
        }
        return Math.min(1.0d, sum / len);
    }

    private static String format(double v) {
        return String.format(java.util.Locale.US, "%.3f", v);
    }

    private static final class Neighbor {
        final double distance;
        final int next;

        Neighbor(double d, int n) {
            distance = d;
            next = n;
        }
    }
}
