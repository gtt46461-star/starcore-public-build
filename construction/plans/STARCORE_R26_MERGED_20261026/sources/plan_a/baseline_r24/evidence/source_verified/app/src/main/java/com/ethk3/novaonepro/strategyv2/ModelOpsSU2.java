package com.ethk3.novaonepro.strategyv2;

/** Permutation consistency and pooling of member models (P17). Mirrors models.sink/pool_static/pool_bma. */
public final class ModelOpsSU2 {
    private ModelOpsSU2() {}
    static final double EPS = 1e-6;

    /** Doubly-stochastic projection: in one draw every number appears exactly once over the 10 positions. */
    public static float[][][] sink(float[][][] P, int iters) {
        int T = P.length;
        float[][][] out = new float[T][10][10];
        double[][] M = new double[10][10];
        for (int t = 0; t < T; t++) {
            for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) M[p][n] = (double) P[t][p][n] + 1e-9;
            for (int it = 0; it < iters; it++) {
                for (int p = 0; p < 10; p++) { double s = 0; for (int n = 0; n < 10; n++) s += M[p][n]; for (int n = 0; n < 10; n++) M[p][n] /= s; }
                for (int n = 0; n < 10; n++) { double s = 0; for (int p = 0; p < 10; p++) s += M[p][n]; for (int p = 0; p < 10; p++) M[p][n] /= s; }
            }
            for (int p = 0; p < 10; p++) { double s = 0; for (int n = 0; n < 10; n++) s += M[p][n]; for (int n = 0; n < 10; n++) out[t][p][n] = (float) (M[p][n] / s); }
        }
        return out;
    }

    /** Static log-linear pool: P ~ prod_m P_m^w_m. */
    public static float[][][] poolStatic(float[][][][] Ps, double[] w) {
        int T = Ps[0].length, M = Ps.length;
        float[][][] out = new float[T][10][10];
        double[] L = new double[10];
        for (int t = 0; t < T; t++) for (int p = 0; p < 10; p++) {
            double mx = Double.NEGATIVE_INFINITY;
            for (int n = 0; n < 10; n++) { double v = 0; for (int m = 0; m < M; m++) v += w[m] * Math.log(Math.max((double) Ps[m][t][p][n], EPS)); L[n] = v; mx = Math.max(mx, v); }
            double s = 0; for (int n = 0; n < 10; n++) { L[n] = Math.exp(L[n] - mx); s += L[n]; }
            for (int n = 0; n < 10; n++) out[t][p][n] = (float) (L[n] / s);
        }
        return out;
    }

    /** Online Bayesian model averaging with forgetting and fixed share; weights for t use settled draws < t. */
    public static float[][][] poolBma(float[][][][] Ps, int[][] d, double share, double decay) {
        int T = Ps[0].length, M = Ps.length;
        double[][] logw = new double[M][10], w = new double[M][10];
        float[][][] out = new float[T][10][10];
        for (int t = 0; t < T; t++) {
            for (int p = 0; p < 10; p++) {
                double mx = Double.NEGATIVE_INFINITY; for (int m = 0; m < M; m++) mx = Math.max(mx, logw[m][p]);
                double z = 0; for (int m = 0; m < M; m++) { w[m][p] = Math.exp(logw[m][p] - mx); z += w[m][p]; }
                for (int m = 0; m < M; m++) w[m][p] /= z;
                for (int n = 0; n < 10; n++) { double v = 0; for (int m = 0; m < M; m++) v += w[m][p] * (double) Ps[m][t][p][n]; out[t][p][n] = (float) v; }
            }
            for (int p = 0; p < 10; p++) {
                int x = d[t][p] - 1;
                for (int m = 0; m < M; m++) logw[m][p] = decay * logw[m][p] + Math.log(Math.max((double) Ps[m][t][p][x], EPS));
                double mx = Double.NEGATIVE_INFINITY; for (int m = 0; m < M; m++) mx = Math.max(mx, logw[m][p]);
                double z = 0; for (int m = 0; m < M; m++) { w[m][p] = Math.exp(logw[m][p] - mx); z += w[m][p]; }
                for (int m = 0; m < M; m++) { w[m][p] = (1 - share) * (w[m][p] / z) + share / M; logw[m][p] = Math.log(w[m][p]); }
            }
        }
        return out;
    }

    /** numpy-style pairwise summation (8 accumulators for blocks <= 128, recursive halves above). */
    static double pairwise(double[] a, int off, int n) {
        if (n < 8) { double s = 0; for (int i = 0; i < n; i++) s += a[off + i]; return s; }
        if (n <= 128) {
            double[] r = new double[8]; for (int j = 0; j < 8; j++) r[j] = a[off + j];
            int i = 8; for (; i < n - (n % 8); i += 8) for (int j = 0; j < 8; j++) r[j] += a[off + i + j];
            double res = ((r[0] + r[1]) + (r[2] + r[3])) + ((r[4] + r[5]) + (r[6] + r[7]));
            for (; i < n; i++) res += a[off + i];
            return res;
        }
        int n2 = n / 2; n2 -= n2 % 8;
        return pairwise(a, off, n2) + pairwise(a, off + n2, n - n2);
    }

    /**
     * State-gated mixture of experts (P17): log P(n) = sum_m (a_m + v_m . s_t) log P_m(n), weights learned
     * online by AdaGrad on the settled log-loss; s_t = the members' own causal states, standardised online.
     * Mirrors models.pool_gated (reduction order may differ from BLAS in the last bits; parity tolerance 1e-5).
     */
    public static float[][][] poolGated(float[][][][] Ps, float[][][] STs, int[][] d, double lr, double l2) {
        GatedState g = new GatedState(Ps.length, STs);
        int T = Ps[0].length; float[][][] out = new float[T][][];
        float[][][] cur = new float[Ps.length][][]; float[][] st = new float[Ps.length][];
        for (int t = 0; t < T; t++) {
            for (int m = 0; m < Ps.length; m++) { cur[m] = Ps[m][t]; st[m] = STs[m][t]; }
            out[t] = g.predict(cur, st);
            g.update(cur, st, d[t], lr, l2);
        }
        return out;
    }

    /** Incremental form used identically by replay and by the live loop. */
    public static final class GatedState {
        final int M, S, D; final double[] W, G, mu, var; long cnt; double[] f; double[][] lp; double[] q;
        public GatedState(int M, float[][][] STs) { this(M, width(STs)); }
        static int width(float[][][] STs) { int s = 0; for (float[][] x : STs) s += x[0].length; return s; }
        public GatedState(int M, int S) {
            this.M = M; this.S = S; D = 1 + S; W = new double[M * D]; G = new double[M * D]; mu = new double[S]; var = new double[S];
            java.util.Arrays.fill(G, 1e-3); java.util.Arrays.fill(var, 1.0); for (int m = 0; m < M; m++) W[m * D] = 1.0 / M;
        }
        double[] stateVector(float[][] st) { double[] s = new double[S]; int j = 0; for (float[] x : st) for (float v : x) s[j++] = v; return s; }
        public float[][] predict(float[][][] P, float[][] st) {
            double[] s = stateVector(st); f = new double[D]; f[0] = 1.0;
            for (int k = 0; k < S; k++) { double z = (s[k] - mu[k]) / Math.sqrt(var[k] + 1e-9); f[k + 1] = Math.max(-5, Math.min(5, z)); }
            double[] a = new double[M]; for (int m = 0; m < M; m++) { double v = 0; for (int k = 0; k < D; k++) v += W[m * D + k] * f[k]; a[m] = v; }
            lp = new double[M][100]; for (int m = 0; m < M; m++) for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) lp[m][p * 10 + n] = Math.log(Math.max((double) P[m][p][n], EPS));
            q = new double[100]; float[][] out = new float[10][10];
            for (int p = 0; p < 10; p++) {
                double[] L = new double[10]; double mx = Double.NEGATIVE_INFINITY;
                for (int n = 0; n < 10; n++) { double v = 0; for (int m = 0; m < M; m++) v += a[m] * lp[m][p * 10 + n]; L[n] = v; mx = Math.max(mx, v); }
                double z = 0; for (int n = 0; n < 10; n++) { L[n] = Math.exp(L[n] - mx); z += L[n]; }
                for (int n = 0; n < 10; n++) { q[p * 10 + n] = L[n] / z; out[p][n] = (float) q[p * 10 + n]; }
            }
            return out;
        }
        public void update(float[][][] P, float[][] st, int[] draw, double lr, double l2) {
            if (f == null) predict(P, st);
            double[] gm = new double[M], prod = new double[100];
            for (int m = 0; m < M; m++) { for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) prod[p * 10 + n] = (q[p * 10 + n] - (draw[p] - 1 == n ? 1.0 : 0.0)) * lp[m][p * 10 + n]; gm[m] = pairwise(prod, 0, 100); }
            for (int m = 0; m < M; m++) for (int k = 0; k < D; k++) { int i = m * D + k; double g = gm[m] * f[k] + l2 * W[i]; G[i] += g * g; W[i] -= lr * g / Math.sqrt(G[i]); }
            double[] s = stateVector(st); cnt++; double div = Math.min(cnt, 2000);
            for (int k = 0; k < S; k++) { double delta = s[k] - mu[k]; mu[k] += delta / div; var[k] += (delta * (s[k] - mu[k]) - var[k]) / div; }
            f = null;
        }
        public void write(java.io.DataOutput o) throws java.io.IOException { for (double v : W) o.writeDouble(v); for (double v : G) o.writeDouble(v); for (double v : mu) o.writeDouble(v); for (double v : var) o.writeDouble(v); o.writeLong(cnt); }
        public void read(java.io.DataInput in) throws java.io.IOException { for (int i = 0; i < W.length; i++) W[i] = in.readDouble(); for (int i = 0; i < G.length; i++) G[i] = in.readDouble(); for (int i = 0; i < mu.length; i++) mu[i] = in.readDouble(); for (int i = 0; i < var.length; i++) var[i] = in.readDouble(); cnt = in.readLong(); f = null; }
    }
}
