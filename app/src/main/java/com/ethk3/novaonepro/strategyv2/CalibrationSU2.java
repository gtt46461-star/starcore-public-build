package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R14 online temperature calibration of a member's probability table (P36).
 *
 * Diagnosis on known-truth worlds: every model family is 2.5-3x over-confident (its predictive
 * log-likelihood is worse than uniform), which spoils confidence gating and fusion. Calibration:
 *
 *   P_cal(t)[p][n] = softmax_n( beta * log P(t)[p][n] ),   beta in [0, 2],
 *
 * with beta re-estimated every REFIT periods (first at START) by Newton steps on the mean
 * log-likelihood of the realised numbers over the trailing WINDOW periods (all 10 positions);
 * the objective is concave in beta, beta is warm-started from the previous block. Before START the
 * table is uniform. Strictly causal: the beta used for target t is fitted on draws < t.
 * Mirrors foundry14.calibrate (fixture r14_parity; tolerance 1e-6 on the table).
 */
public final class CalibrationSU2 {
    private CalibrationSU2() {}
    public static final int START = 1500, WINDOW = 2500, REFIT = 250, ITERS = 3;

    static double[] logRow(float[] row) { double[] l = new double[10]; for (int n = 0; n < 10; n++) l[n] = Math.log(Math.min(1.0, Math.max(1e-9, (double) row[n]))); return l; }

    /** Newton update of beta on rows L[i][p][n] with realised numbers y[i][p] (0-based); returns the new beta. */
    static double fitBeta(double[][][] L, int[][] y, int n, double beta) {
        for (int it = 0; it < ITERS; it++) {ExecutionBudgetR26.poll();
            double gs = 0, hs = 0; long cnt = 0;
            for (int i = 0; i < n; i++) for (int p = 0; p < 10; p++) {
                double[] l = L[i][p]; double mx = Double.NEGATIVE_INFINITY;
                for (int k = 0; k < 10; k++) mx = Math.max(mx, beta * l[k]);
                double z = 0; double[] e = new double[10];
                for (int k = 0; k < 10; k++) { e[k] = Math.exp(beta * l[k] - mx); z += e[k]; }
                double m1 = 0, m2 = 0; for (int k = 0; k < 10; k++) { double pr = e[k] / z; m1 += pr * l[k]; m2 += pr * l[k] * l[k]; }
                gs += l[y[i][p]] - m1; hs += m2 - m1 * m1; cnt++;
            }
            double g = gs / cnt, h = -hs / cnt;
            if (h > -1e-12) break;
            double nb = Math.min(2.0, Math.max(0.0, beta - g / h));
            if (Math.abs(nb - beta) < 1e-6) { beta = nb; break; }
            beta = nb;
        }
        return beta;
    }

    static float[][] apply(double beta, float[][] row) {
        float[][] out = new float[10][10];
        for (int p = 0; p < 10; p++) {
            double[] l = logRow(row[p]); double mx = Double.NEGATIVE_INFINITY;
            for (int k = 0; k < 10; k++) mx = Math.max(mx, beta * l[k]);
            double z = 0; double[] e = new double[10];
            for (int k = 0; k < 10; k++) { e[k] = Math.exp(beta * l[k] - mx); z += e[k]; }
            for (int k = 0; k < 10; k++) out[p][k] = (float) (e[k] / z);
        }
        return out;
    }

    static float[][] uniform() { float[][] u = new float[10][10]; for (float[] r : u) Arrays.fill(r, 0.1f); return u; }

    /** Batch (replay) form over a whole table. */
    public static float[][][] calibrate(float[][][] P, int[][] draws) {
        int T = P.length; float[][][] out = new float[T][][];
        for (int t = 0; t < Math.min(START, T); t++) out[t] = uniform();
        double beta = 1.0;
        for (int t0 = START; t0 < T; t0 += REFIT) {ExecutionBudgetR26.poll();
            int lo = Math.max(0, t0 - WINDOW), n = t0 - lo;
            double[][][] L = new double[n][10][]; int[][] y = new int[n][10];
            for (int i = 0; i < n; i++) for (int p = 0; p < 10; p++) { L[i][p] = logRow(P[lo + i][p]); y[i][p] = draws[lo + i][p] - 1; }
            beta = fitBeta(L, y, n, beta);
            for (int t = t0; t < Math.min(t0 + REFIT, T); t++) out[t] = apply(beta, P[t]);
        }
        return out;
    }

    /** Incremental (live) form: identical outputs to calibrate(), one period at a time. */
    public static final class Live {
        // raw float tables (logs are recomputed at refit time, bit-identical): 1 MB per member instead of 2 MB
        final float[][][] ring = new float[WINDOW][][]; final byte[][] ringY = new byte[WINDOW][]; int filled; long t; double beta = 1.0;
        /** Calibrated table for the current target (index t), from draws < t only. */
        public float[][] predict(float[][] raw) {
            if (t < START) return uniform();
            if ((t - START) % REFIT == 0) refitIfDue();
            return apply(beta, raw);
        }
        private long fittedAt = -1;
        private void refitIfDue() {
            if (fittedAt == t) return;
            if (filled == 0) { fittedAt = t; return; }
            int n = filled; double[][][] L = new double[n][10][]; int[][] y = new int[n][10];
            for (int i = 0; i < n; i++) { int j = (int) ((t - n + i) % WINDOW); for (int p = 0; p < 10; p++) { L[i][p] = logRow(ring[j][p]); y[i][p] = ringY[j][p]; } }
            beta = fitBeta(L, y, n, beta); fittedAt = t;
        }
        /** After the draw of target t is settled (whether or not anything acted). */
        public void settle(float[][] raw, int[] draw) {
            if (t >= START && (t - START) % REFIT == 0) refitIfDue();       // a period skipped by predict still refits
            int j = (int) (t % WINDOW); float[][] l = new float[10][]; byte[] y = new byte[10];
            for (int p = 0; p < 10; p++) { l[p] = raw[p].clone(); y[p] = (byte) (draw[p] - 1); }
            ring[j] = l; ringY[j] = y; if (filled < WINDOW) filled++; t++;
        }
        public double beta() { return beta; }
        /** Preserve fitted temperature and lifetime clock; unknown periods cannot remain in raw rings. */
        public void resetTemporalContextR26() {
            Arrays.fill(ring,null);Arrays.fill(ringY,null);filled=0;fittedAt=-1;
        }
        public void write(java.io.DataOutput o) throws java.io.IOException {
            o.writeLong(t); o.writeDouble(beta); o.writeLong(fittedAt); o.writeInt(filled);
            for (int i = 0; i < filled; i++) { int j = (int) ((t - filled + i) % WINDOW); for (int p = 0; p < 10; p++) { o.writeByte(ringY[j][p]); for (int k = 0; k < 10; k++) o.writeFloat(ring[j][p][k]); } }
        }
        public void read(java.io.DataInput in) throws java.io.IOException {
            t = in.readLong(); beta = in.readDouble(); fittedAt = in.readLong(); filled = in.readInt();
            for (int i = 0; i < filled; i++) { int j = (int) ((t - filled + i) % WINDOW); ringY[j] = new byte[10]; ring[j] = new float[10][10];
                for (int p = 0; p < 10; p++) { ringY[j][p] = in.readByte(); for (int k = 0; k < 10; k++) ring[j][p][k] = in.readFloat(); } }
        }
    }
}
