package com.ethk3.novaonepro.strategyv2;

/**
 * Post-selection calibration (P29): the probability shown for an ACT period is
 * re-estimated only from earlier ACT periods of the same strategy (Platt scaling
 * on logit(phat) with a ridge toward "calibrated as is"), and the expected
 * calibration error is reported in 10 bins. Selection makes raw phat optimistic;
 * this is what the dashboard and the report must show instead.
 */
public final class SelectiveCalibrationSU2 {
    private SelectiveCalibrationSU2() {}

    public static final class Platt { public final double a, b; Platt(double a, double b) { this.a = a; this.b = b; }
        public double apply(double p) { double z = a * logit(p) + b; return 1 / (1 + Math.exp(-z)); } }

    static double logit(double p) { double q = Math.min(1 - 1e-6, Math.max(1e-6, p)); return Math.log(q / (1 - q)); }

    /** Fit on ACT rows < t only (caller guarantees causality). */
    public static Platt fit(double[] phat, boolean[] hit, int n, double ridge) {
        double a = 1, b = 0;
        for (int it = 0; it < 25; it++) {
            double ga = ridge * (a - 1), gb = ridge * b, haa = ridge, hab = 0, hbb = ridge + 1e-9;
            for (int i = 0; i < n; i++) {
                double x = logit(phat[i]), p = 1 / (1 + Math.exp(-(a * x + b))), e = p - (hit[i] ? 1 : 0), w = p * (1 - p);
                ga += e * x; gb += e; haa += w * x * x; hab += w * x; hbb += w;
            }
            double det = haa * hbb - hab * hab; if (Math.abs(det) < 1e-12) break;
            double da = (hbb * ga - hab * gb) / det, db = (haa * gb - hab * ga) / det;
            a -= da; b -= db; if (Math.abs(da) + Math.abs(db) < 1e-9) break;
        }
        return new Platt(a, b);
    }

    public static double ece(double[] p, boolean[] hit, int n) {
        double[] s = new double[10], k = new double[10]; int[] c = new int[10];
        for (int i = 0; i < n; i++) { int b = Math.min(9, (int) (p[i] * 10)); s[b] += p[i]; k[b] += hit[i] ? 1 : 0; c[b]++; }
        double e = 0; for (int b = 0; b < 10; b++) if (c[b] > 0) e += c[b] / (double) n * Math.abs(s[b] / c[b] - k[b] / c[b]);
        return e;
    }
}
