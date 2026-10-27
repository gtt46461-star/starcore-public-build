package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/**
 * R15 (P45) teacher matrix pool: which member ("teacher") is reliable in which observable state, per position
 * (reference/replay/teacher.py; fixture r15_atomnet).
 *
 *   W[m, p, sigma] = softmax_m( sum of member m's log-likelihood at position p over the trailing 2,500 settled periods
 *                               whose state indicator at p was sigma )             sigma in {-1, +1}; 0 = all periods
 *   P[t, p, .]     = sum_m W[m, p, s[t, p]] * P_m[t, p, .]
 *
 * s = registry state atom j (AtomStreamSU2.state). Weights are re-estimated every 250 periods from draws before the
 * block (first at 1,500; equal weights before). Genome pool text: TEACHER:<j>.
 */
public final class TeacherPoolSU2 {
    private TeacherPoolSU2() {}
    public static final int START = 1500, WINDOW = 2500, REFIT = 250;

    public static boolean isTeacher(String pool) { return pool != null && pool.startsWith("TEACHER:"); }
    public static int stateOf(String pool) {
        int j = Integer.parseInt(pool.substring(8));
        if (j < 0 || j >= AtomStreamSU2.states()) throw new IllegalArgumentException("SU2_TEACHER_STATE");
        return j;
    }

    /** State indicator series s[t][p] of registry state j over a history (replay). */
    public static float[][] states(int[][] draws, int j) {
        AtomStreamSU2 st = new AtomStreamSU2(); float[][] s = new float[draws.length][];
        for (int t = 0; t < draws.length; t++) { s[t] = st.state(j).clone(); st.update(draws[t]); }
        return s;
    }

    static double ll(float[][] P, int p, int y) { return Math.log(Math.min(1.0, Math.max(1e-9, (double) P[p][y]))); }

    static void weights(double[][] llWin, float[][] sWin, int n, int M, double[][][] W) {
        // llWin[i][m*10+p], sWin[i][p] for the n periods of the window in time order
        double[] sel = new double[n];
        for (int si = 0; si < 3; si++) {
            float sig = si - 1;
            for (int p = 0; p < 10; p++) {
                double[] sc = new double[M]; int cnt = 0;
                for (int m = 0; m < M; m++) {
                    int k = 0;
                    for (int i = 0; i < n; i++) if (sig == 0f || sWin[i][p] == sig) sel[k++] = llWin[i][m * 10 + p];
                    sc[m] = ModelOpsSU2.pairwise(sel, 0, k); cnt = k;
                }
                if (cnt == 0) Arrays.fill(sc, 0.0);
                double mx = Double.NEGATIVE_INFINITY; for (double v : sc) mx = Math.max(mx, v);
                double[] e = new double[M]; for (int m = 0; m < M; m++) e[m] = Math.exp(sc[m] - mx);
                double z = ModelOpsSU2.pairwise(e, 0, M);
                for (int m = 0; m < M; m++) W[si][p][m] = e[m] / z;
            }
        }
    }

    static float[][] combine(float[][][] tabs, float[] s, double[][][] W) {
        int M = tabs.length; float[][] out = new float[10][10];
        for (int p = 0; p < 10; p++) {
            int si = (int) s[p] + 1;
            double[] acc = new double[10];
            for (int n = 0; n < 10; n++) acc[n] = W[si][p][0] * (double) tabs[0][p][n];
            for (int m = 1; m < M; m++) for (int n = 0; n < 10; n++) acc[n] = acc[n] + W[si][p][m] * (double) tabs[m][p][n];
            for (int n = 0; n < 10; n++) out[p][n] = (float) acc[n];
        }
        return out;
    }

    /** Replay form (Python teacher.teacher_pool). */
    public static float[][][] pool(float[][][][] Ps, int[][] draws, float[][] s) {
        int M = Ps.length, T = draws.length; float[][][] out = new float[T][][];
        double[][][] W = new double[3][10][M]; for (double[][] a : W) for (double[] b : a) Arrays.fill(b, 1.0 / M);
        float[][][] at = new float[M][][];
        for (int t0 = 0; t0 < T; t0 += REFIT) {
            if (t0 >= START) {
                int lo = Math.max(0, t0 - WINDOW), n = t0 - lo;
                double[][] L = new double[n][M * 10]; float[][] S = new float[n][];
                for (int i = 0; i < n; i++) { int u = lo + i; S[i] = s[u]; for (int m = 0; m < M; m++) for (int p = 0; p < 10; p++) L[i][m * 10 + p] = ll(Ps[m][u], p, draws[u][p] - 1); }
                weights(L, S, n, M, W);
            }
            for (int t = t0; t < Math.min(t0 + REFIT, T); t++) { for (int m = 0; m < M; m++) at[m] = Ps[m][t]; out[t] = combine(at, s[t], W); }
        }
        return out;
    }

    /** Live form: identical tables to pool(), one period at a time (its own atom stream supplies the state). */
    public static final class Live {
        final int M, j; final AtomStreamSU2 stream = new AtomStreamSU2();
        final double[][] llRing; final float[][] sRing; long t, fittedAt = -1; float[] sNow;
        final double[][][] W;
        public Live(int M, int j) {
            this.M = M; this.j = j; llRing = new double[WINDOW][M * 10]; sRing = new float[WINDOW][];
            W = new double[3][10][M]; for (double[][] a : W) for (double[] b : a) Arrays.fill(b, 1.0 / M);
        }
        public float[][] predict(float[][][] tabs) {
            if (t >= START && t % REFIT == 0 && fittedAt != t) {
                long lo = Math.max(0, t - WINDOW); int n = (int) (t - lo);
                double[][] L = new double[n][]; float[][] S = new float[n][];
                for (int i = 0; i < n; i++) { int k = (int) ((lo + i) % WINDOW); L[i] = llRing[k]; S[i] = sRing[k]; }
                weights(L, S, n, M, W); fittedAt = t;
            }
            sNow = stream.state(j);
            return combine(tabs, sNow, W);
        }
        public void settle(float[][][] tabs, int[] draw) {
            if (sNow == null || stream.time() != t) sNow = stream.state(j);
            int k = (int) (t % WINDOW); double[] row = new double[M * 10];
            for (int m = 0; m < M; m++) for (int p = 0; p < 10; p++) row[m * 10 + p] = ll(tabs[m], p, draw[p] - 1);
            llRing[k] = row; sRing[k] = sNow.clone(); stream.update(draw); t++; sNow = null;
        }
        public void write(DataOutput o) throws IOException {
            o.writeLong(t); o.writeLong(fittedAt);
            for (double[][] a : W) for (double[] b : a) for (double v : b) o.writeDouble(v);
            int keep = (int) Math.min(t, WINDOW); o.writeInt(keep);
            for (long u = t - keep; u < t; u++) { int k = (int) (u % WINDOW); for (double v : llRing[k]) o.writeDouble(v); for (float v : sRing[k]) o.writeFloat(v); }
            stream.write(o);
        }
        public void read(DataInput in) throws IOException {
            t = in.readLong(); fittedAt = in.readLong();
            for (double[][] a : W) for (double[] b : a) for (int m = 0; m < M; m++) b[m] = in.readDouble();
            int keep = in.readInt(); if (keep < 0 || keep > WINDOW) throw new IOException("SU2_TEACHER_STATE");
            for (long u = t - keep; u < t; u++) { int k = (int) (u % WINDOW); llRing[k] = new double[M * 10]; for (int i = 0; i < M * 10; i++) llRing[k][i] = in.readDouble(); sRing[k] = new float[10]; for (int p = 0; p < 10; p++) sRing[k][p] = in.readFloat(); }
            stream.read(in); sNow = null;
        }
    }
}
