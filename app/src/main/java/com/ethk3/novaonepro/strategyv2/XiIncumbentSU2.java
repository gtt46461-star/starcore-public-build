package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * The user's original multi-number-set x multi-position cross-intersection method
 * (Top6/7/8/9 support, fixed Rank1 position and K3, rolling-z features, ridge logistic
 * C=0.20 on a 2,500-period window refreshed every 250, 25% base pool, 15% coverage)
 * as the protected INCUMBENT of the league (P03). Port of hre.ExpertBank +
 * hre.base_and_features + hre.evaluate; parity with the Python reference is checked by
 * XiIncumbentSU2Test. The upgrade must beat this on the same periods before it may
 * replace it in the mainline.
 */
public final class XiIncumbentSU2 {
    private XiIncumbentSU2() {}
    public static final double[] WEIGHTS = {0.0, 0.3, 0.3, 0.2, 0.2, -0.2, 0.3, 0.1};
    public static final double TEMPERATURE = 1.0, C = 0.20, POOL = 0.25, COVERAGE = 0.15;
    public static final int TRAIN_WINDOW = 2500, RETRAIN = 250, WARMUP = 1500;
    static final int[] ROLL = {180, 360, 720, 1200};
    /** Identity of the incumbent program: its league row, frozen decisions and any certificate are bound to it. */
    public static final String CANONICAL = "SU2|XI_INCUMBENT|W=" + Arrays.toString(WEIGHTS) + "|T=" + TEMPERATURE + "|C=" + C + "|POOL=" + POOL
        + "|COV=" + COVERAGE + "|WIN=" + TRAIN_WINDOW + "|RE=" + RETRAIN + "|WARM=" + WARMUP + "|ROLL=" + Arrays.toString(ROLL);
    public static final String GENOME_KEY = HashingSU1.text(CANONICAL);

    /** numpy float32 pairwise summation of 10 contiguous values (8 partial sums, then the tail). */
    static float pairwise10(float[] v) {
        float r = ((v[0] + v[1]) + (v[2] + v[3])) + ((v[4] + v[5]) + (v[6] + v[7]));
        r += v[8]; r += v[9]; return r;
    }

    /**
     * Z[e][t][p][n] for the 8 causal experts r12,r24,r48,r96,r360,omission,markov720,momentum.
     * dtypes follow hre.ExpertBank exactly: window frequencies and momentum are float64
     * (float32 counts divided by int64 lengths), omission and markov are float32.
     */
    private static double[][][][] doubleBank(int banks,int samples){double[][][][] out=new double[banks][samples][][];for(int e=0;e<banks;e++)for(int t=0;t<samples;t++){ExecutionBudgetR26.poll();out[e][t]=new double[10][10];}return out;}
    private static float[][][][] floatBank(int banks,int samples){float[][][][] out=new float[banks][samples][][];for(int e=0;e<banks;e++)for(int t=0;t<samples;t++){ExecutionBudgetR26.poll();out[e][t]=new float[10][10];}return out;}
    private static byte[][][][] rankBank(int banks,int samples){byte[][][][] out=new byte[banks][samples][][];for(int e=0;e<banks;e++)for(int t=0;t<samples;t++){ExecutionBudgetR26.poll();out[e][t]=new byte[10][10];}return out;}
    static float[][][][] experts(int[][] d) {
        int T = d.length;
        double[][][][] r64 = doubleBank(6,T);     // 0..4 windows, 5 momentum
        float[][][][] r32 = floatBank(2,T);       // 0 omission, 1 markov
        float[][][] C = new float[T + 1][][];C[0]=new float[10][10];
        for (int t = 0; t < T; t++) { ExecutionBudgetR26.poll(); C[t+1]=new float[10][];for (int p = 0; p < 10; p++) C[t + 1][p] = C[t][p].clone(); for (int p = 0; p < 10; p++) C[t + 1][p][d[t][p] - 1] += 1f; }
        int[] wins = {12, 24, 48, 96, 360};
        for (int wi = 0; wi < 5; wi++) for (int t = 0; t < T; t++) { ExecutionBudgetR26.poll();
            int lo = Math.max(t - wins[wi], 0); long den = Math.max(t - lo, 1);
            for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) r64[wi][t][p][n] = (double) (C[t][p][n] - C[lo][p][n]) / (double) den;
        }
        for (int t = 0; t < T; t++) for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) { if(p==0&&n==0)ExecutionBudgetR26.poll();r64[5][t][p][n] = r64[1][t][p][n] - r64[4][t][p][n]; }
        long[][] last = new long[10][10]; for (long[] r : last) Arrays.fill(r, -1);
        for (int t = 0; t < T; t++) { ExecutionBudgetR26.poll();
            for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) r32[0][t][p][n] = (float) Math.log1p((float) (last[p][n] >= 0 ? t - last[p][n] : t + 1));
            for (int p = 0; p < 10; p++) last[p][d[t][p] - 1] = t;
        }
        float[][][] M = new float[10][10][10];
        for (int t = 0; t < T; t++) { ExecutionBudgetR26.poll();
            if (t >= 2) { for (int p = 0; p < 10; p++) M[p][d[t - 2][p] - 1][d[t - 1][p] - 1] += 1; if (t - 2 - 720 >= 0) for (int p = 0; p < 10; p++) M[p][d[t - 2 - 720][p] - 1][d[t - 1 - 720][p] - 1] -= 1; }
            if (t >= 1) for (int p = 0; p < 10; p++) { float[] row = M[p][d[t - 1][p] - 1]; float s = pairwise10(row); for (int n = 0; n < 10; n++) r32[1][t][p][n] = (row[n] + 1.0f) / (s + 10.0f); }
            else for (int p = 0; p < 10; p++) Arrays.fill(r32[1][t][p], 0.1f);
        }
        float[][][][] Z = floatBank(8,T);
        int[] map = {0, 1, 2, 3, 4, -1, -2, 5};           // expert -> r64 index, or -1 omission, -2 markov
        for (int e = 0; e < 8; e++) for (int t = 0; t < T; t++) for (int p = 0; p < 10; p++) {
            ExecutionBudgetR26.poll();if (map[e] >= 0) {
                double[] v = r64[map[e]][t][p]; double mu = 0; for (double x : v) mu += x;   // float64 pairwise == sequential to < 1 ulp here
                mu /= 10; double var = 0; for (double x : v) var += (x - mu) * (x - mu); double sd = Math.sqrt(var / 10) + 1e-6;
                for (int n = 0; n < 10; n++) Z[e][t][p][n] = (float) ((v[n] - mu) / sd);
            } else {
                float[] v = r32[map[e] == -1 ? 0 : 1][t][p]; float mu = pairwise10(v) / 10f;
                float[] q = new float[10]; for (int n = 0; n < 10; n++) { float x = v[n] - mu; q[n] = x * x; }
                float sd = (float) Math.sqrt(pairwise10(q) / 10f) + 1e-6f;
                for (int n = 0; n < 10; n++) Z[e][t][p][n] = (v[n] - mu) / sd;
            }
        }
        return Z;
    }

    public static final class Result {
        public final boolean[] act, hit; public final int[] position; public final int[][] k3; public final double[] prob; public float[][] table;
        Result(int T) { act = new boolean[T]; hit = new boolean[T]; position = new int[T]; k3 = new int[T][3]; prob = new double[T]; Arrays.fill(prob, Double.NaN); }
    }

    public static Result evaluate(int[][] d, int start, int end) {
        int T = d.length; float[][][][] Z = experts(d);
        // ranks with tie-break toward the lower number
        byte[][][][] rank = rankBank(8,T);
        Integer[] ord = new Integer[10];
        for (int e = 0; e < 8; e++) for (int t = 0; t < T; t++) for (int p = 0; p < 10; p++) {
            ExecutionBudgetR26.poll();final float[] z = Z[e][t][p]; for (int i = 0; i < 10; i++) ord[i] = i;
            Arrays.sort(ord, (a, b) -> Float.compare(z[b] - b * 1e-4f, z[a] - a * 1e-4f));
            for (int r = 0; r < 10; r++) rank[e][t][p][ord[r]] = (byte) r;
        }
        Result res = new Result(T);
        int nf = 19; double[][] F = new double[T][nf]; double[] base = new double[T];
        for (int t = 0; t < T; t++) { ExecutionBudgetR26.poll();
            float[][] S = new float[10][10];
            for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) { float s = 0; for (int e = 0; e < 8; e++) s += (float) WEIGHTS[e] * Z[e][t][p][n]; S[p][n] = s; }
            float[][] P = new float[10][10]; int[][] top = new int[10][4]; float[] mass = new float[10];
            for (int p = 0; p < 10; p++) {
                float[] zz = new float[10]; float mx = Float.NEGATIVE_INFINITY;
                for (int n = 0; n < 10; n++) { zz[n] = S[p][n] / (float) TEMPERATURE; mx = Math.max(mx, zz[n]); }
                for (int n = 0; n < 10; n++) P[p][n] = (float) Math.exp(zz[n] - mx);
                float z = pairwise10(P[p]); for (int n = 0; n < 10; n++) P[p][n] /= z;
                final float[] row = S[p]; for (int i = 0; i < 10; i++) ord[i] = i;
                Arrays.sort(ord, (a, b) -> Float.compare(row[b] - b * 1e-6f, row[a] - a * 1e-6f));
                for (int i = 0; i < 4; i++) top[p][i] = ord[i];
                mass[p] = P[p][top[p][0]] + P[p][top[p][1]] + P[p][top[p][2]];
            }
            for (int i = 0; i < 10; i++) ord[i] = i;
            final float[] ms = mass; Arrays.sort(ord, (a, b) -> Float.compare(ms[b] - b * 1e-9f, ms[a] - a * 1e-9f));
            int p1 = ord[0], p2 = ord[1]; int[] K3 = {top[p1][0], top[p1][1], top[p1][2]};
            if (t == end - 1) { res.table = new float[10][]; for (int p = 0; p < 10; p++) res.table[p] = P[p].clone(); }   // pooled table of the target
            res.position[t] = p1 + 1; res.k3[t] = new int[]{K3[0] + 1, K3[1] + 1, K3[2] + 1};
            int x = d[t][p1] - 1; res.hit[t] = K3[0] == x || K3[1] == x || K3[2] == x;
            base[t] = mass[p1];
            double[] f = F[t]; int j = 0; double[] avg = new double[4], mn = new double[4];
            for (int ki = 0; ki < 4; ki++) {
                int kk = 6 + ki; double s = 0, m = 1e9;
                for (int e = 0; e < 8; e++) { double sup = 0; for (int q = 0; q < 3; q++) sup += rank[e][t][p1][K3[q]] < kk ? 1 : 0; sup /= 3.0; s += sup; m = Math.min(m, sup); }
                avg[ki] = s / 8; mn[ki] = m;
            }
            for (int ki = 0; ki < 4; ki++) f[j++] = avg[ki];
            for (int ki = 0; ki < 4; ki++) f[j++] = mn[ki];
            f[j++] = avg[0] / Math.max(avg[3], 1e-3);
            double own = 0, other = -1;
            for (int p = 0; p < 10; p++) { double pr = P[p][K3[0]] + P[p][K3[1]] + P[p][K3[2]]; if (p == p1) own = pr; else other = Math.max(other, pr); }
            f[j++] = own - other;
            f[j++] = mass[p1] - mass[p2];
            int amb = 0; for (int p = 0; p < 10; p++) if (mass[p] >= mass[p1] - 0.01f) amb++; f[j++] = amb;
            double ts = 0; for (int e = 0; e < 8; e++) ts += rank[e][t][p1][K3[2]] < 6 ? 1 : 0; f[j++] = ts / 8;
            float s0 = S[p1][top[p1][0]], s1 = S[p1][top[p1][1]], s2 = S[p1][top[p1][2]], s3 = S[p1][top[p1][3]];
            f[j++] = (s1 - s2) - (s2 - s3);
            double[] su = new double[8]; double sm = 0; for (int e = 0; e < 8; e++) { double c = 0; for (int q = 0; q < 3; q++) c += rank[e][t][p1][K3[q]] < 6 ? 1 : 0; su[e] = c / 3.0; sm += su[e]; }
            sm /= 8; double sv = 0; for (double v : su) sv += (v - sm) * (v - sm); f[j++] = Math.sqrt(sv / 8);
            double ent = 0; for (int n = 0; n < 10; n++) ent += P[p1][n] * Math.log(P[p1][n] + 1e-12); f[j++] = 1.0 + ent / Math.log(10);
            f[j++] = s2 - s3;
            f[j++] = avg[0] * (ts / 8);
            f[j++] = avg[0] * (own - other);
        }
        double[][] X = new double[T][nf * 5];
        for (int t = 0; t < T; t++) System.arraycopy(F[t], 0, X[t], 0, nf);
        for (int wi = 0; wi < 4; wi++) { ExecutionBudgetR26.poll();
            int w = ROLL[wi]; double[] c1 = new double[nf], c2 = new double[nf];
            double[][] C1 = new double[T + 1][], C2 = new double[T + 1][]; C1[0] = new double[nf]; C2[0] = new double[nf];
            for (int t = 0; t < T; t++) { ExecutionBudgetR26.poll(); C1[t + 1] = C1[t].clone(); C2[t + 1] = C2[t].clone(); for (int k = 0; k < nf; k++) { C1[t + 1][k] += F[t][k]; C2[t + 1][k] += F[t][k] * F[t][k]; } }
            for (int t = 0; t < T; t++) { ExecutionBudgetR26.poll();
                int lo = Math.max(t - w, 0), n = Math.max(t - lo, 1);
                for (int k = 0; k < nf; k++) {
                    double mu = (C1[t][k] - C1[lo][k]) / n, var = Math.max((C2[t][k] - C2[lo][k]) / n - mu * mu, 1e-9);
                    double z = t < 30 ? 0 : Math.max(-6, Math.min(6, (F[t][k] - mu) / Math.sqrt(var)));
                    X[t][nf * (wi + 1) + k] = z;
                }
            }
        }
        boolean[] pool = new boolean[T];
        for (int t = 30; t < end; t++) { ExecutionBudgetR26.poll(); int lo = Math.max(0, t - 720); pool[t] = base[t] >= ModelReplaySU2.quantileLinear(Arrays.copyOfRange(base, lo, t), 1 - POOL); }
        double keep = Math.min(1.0, COVERAGE / POOL);
        double[] prob = res.prob;
        double[] wt = null, mu = null, sd = null; int lastFit = 0; int D = nf * 5;
        for (int t = Math.max(start, WARMUP); t < end; t++) { ExecutionBudgetR26.poll();
            if (!pool[t]) continue;
            if (wt == null || t - lastFit >= RETRAIN) {
                lastFit = t; int lo = Math.max(0, t - TRAIN_WINDOW);
                List<Integer> idx = new ArrayList<>(); for (int u = lo; u < t; u++) if (pool[u]) idx.add(u);
                if (idx.size() >= 60) {
                    int n = idx.size(); mu = new double[D]; sd = new double[D];
                    for (int k = 0; k < D; k++) { ExecutionBudgetR26.poll();double s = 0; for (int u : idx) s += X[u][k]; mu[k] = s / n; double v = 0; for (int u : idx) { double q = X[u][k] - mu[k]; v += q * q; } sd[k] = Math.sqrt(v / n) + 1e-9; }
                    double[][] Zs = new double[n][D]; double[] y = new double[n];
                    for (int i = 0; i < n; i++) { int u = idx.get(i); for (int k = 0; k < D; k++) Zs[i][k] = (X[u][k] - mu[k]) / sd[k]; y[i] = res.hit[u] ? 1 : 0; }
                    wt = LogisticRidgeSU2.fit(Zs, y, n, C, 12);
                }
            }
            if (wt == null) continue;
            double z = wt[0]; for (int k = 0; k < D; k++) z += (X[t][k] - mu[k]) / sd[k] * wt[k + 1];
            z = Math.max(-30, Math.min(30, z)); prob[t] = 1.0 / (1.0 + Math.exp(-z));
            int lo = Math.max(0, t - 600); double[] v = new double[t - lo]; int m = 0; for (int u = lo; u < t; u++) if (!Double.isNaN(prob[u])) v[m++] = prob[u];
            if (m < 30) continue;
            res.act[t] = prob[t] >= ModelReplaySU2.quantileLinear(Arrays.copyOf(v, m), 1 - keep);
        }
        return res;
    }

    /** Full decision for the next target: act, position, K3, the gate probability P(hit | state) and the pooled table. */
    public static final class Decision {
        public final boolean act; public final int position; public final int[] k3; public final double phat; public final float[][] table;
        Decision(boolean act, int position, int[] k3, double phat, float[][] table) { this.act = act; this.position = position; this.k3 = k3; this.phat = phat; this.table = table; }
        public int mask() { return (1 << (k3[0] - 1)) | (1 << (k3[1] - 1)) | (1 << (k3[2] - 1)); }
    }

    /** Decision for the next target (index = history.length), using draws < target only. */
    public static Decision decideNextFull(int[][] history) {
        int T = history.length; if (T < WARMUP + 60) return null;
        int[][] padded = Arrays.copyOf(history, T + 1);
        padded[T] = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};             // placeholder: features at T never read row T
        Result r = evaluate(padded, WARMUP, T + 1);                      // same refit schedule as the replay (anchored at warm-up)
        return new Decision(r.act[T], r.position[T], r.k3[T].clone(), r.prob[T], r.table);
    }

    public static int[] decideNext(int[][] history) {
        Decision x = decideNextFull(history); if (x == null) return null;
        return new int[]{x.act ? 1 : 0, x.position, x.k3[0], x.k3[1], x.k3[2]};
    }
}
