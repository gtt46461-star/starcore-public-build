package com.ethk3.novaonepro;

import java.util.Arrays;

final class V49ExpertSupport {
    private V49ExpertSupport() {}

    static void softmax(double[] score, double[] out) {
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < 10; i++) if (score[i] > max) max = score[i];
        double sum = 0.0d;
        for (int i = 0; i < 10; i++) {
            double x = Math.exp(Math.max(-40.0d, Math.min(40.0d, score[i] - max)));
            out[i] = x;
            sum += x;
        }
        if (sum <= 0.0d) {
            Arrays.fill(out, 0.10d);
        } else {
            for (int i = 0; i < 10; i++) out[i] /= sum;
        }
    }

    static double normalizedEntropy(double[] p) {
        return FeatureContractRegistry.entropy10(p);
    }

    static int[] top3(double[] p) {
        int[] idx = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int i = 0; i < 10; i++) {
            for (int j = i + 1; j < 10; j++) {
                if (p[idx[j]] > p[idx[i]] || (p[idx[j]] == p[idx[i]] && idx[j] < idx[i])) {
                    int t = idx[i];
                    idx[i] = idx[j];
                    idx[j] = t;
                }
            }
        }
        return new int[] {idx[0], idx[1], idx[2]};
    }

    static double top3Sum(double[] p) {
        int[] t = top3(p);
        return p[t[0]] + p[t[1]] + p[t[2]];
    }

    static double js(double[] a, double[] b) {
        double v = 0.0d;
        for (int i = 0; i < 10; i++) {
            double m = 0.5d * (a[i] + b[i]);
            if (a[i] > 0.0d) v += 0.5d * a[i] * Math.log(a[i] / Math.max(1e-12d, m));
            if (b[i] > 0.0d) v += 0.5d * b[i] * Math.log(b[i] / Math.max(1e-12d, m));
        }
        return Math.max(0.0d, Math.min(1.0d, v / Math.log(2.0d)));
    }
}
