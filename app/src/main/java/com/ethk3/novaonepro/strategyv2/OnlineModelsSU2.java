package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.Arrays;

/**
 * R13 online mathematical models (P13-P17), stepwise.
 *
 * Every model is a state machine with two operations:
 *   predict(p, st)  - probability table for the NEXT draw and a causal state
 *                     description, from draws already settled;
 *   update(draw)    - training/settlement/feedback with the settled draw.
 * The phone calls predict() before the draw and update() inside the settlement
 * transaction - every period, for every model, whether or not anything acts.
 * Historical replay uses exactly the same code (batch = predict/update loop), so
 * history training and live training cannot diverge.
 *
 * Bit-level contract with reference/replay/models.py (double intermediates,
 * float storage). Parity: OnlineModelsSU2Test; live==replay: ModelLiveSU2Test.
 */
public final class OnlineModelsSU2 {
    private OnlineModelsSU2() {}

    public interface Model {
        int stateWidth();
        void predict(float[][] p, float[] st);
        void update(int[] draw);
        void write(DataOutput out) throws IOException;
        void read(DataInput in) throws IOException;
    }

    public static final class Output {
        public final float[][][] p; public final float[][] state;
        Output(int t, int s) { p = new float[t][10][10]; state = new float[t][s]; }
        public long bytes() { return (long) p.length * 100L * 4L + (long) state.length * state[0].length * 4L; }
    }

    static void requireDraws(int[][] d) {
        if (d == null || d.length < 1 || d.length > 20000) throw new IllegalArgumentException("SU2_MODEL_DRAWS");
        for (int[] row : d) requireDraw(row);
    }
    static void requireDraw(int[] row) {
        if (row == null || row.length != 10) throw new IllegalArgumentException("SU2_MODEL_DRAW_WIDTH");
        int seen = 0;
        for (int v : row) { if (v < 1 || v > 10 || (seen & (1 << v)) != 0) throw new IllegalArgumentException("SU2_MODEL_DRAW_PERMUTATION"); seen |= 1 << v; }
    }

    public static Output run(Model m, int[][] d) {
        requireDraws(d);
        Output out = new Output(d.length, m.stateWidth());
        for (int t = 0; t < d.length; t++) { m.predict(out.p[t], out.state[t]); m.update(d[t]); }
        return out;
    }

    static void writeArr(DataOutput o, double[] a) throws IOException { o.writeInt(a.length); for (double v : a) o.writeDouble(v); }
    static void readArr(DataInput in, double[] a) throws IOException { if (in.readInt() != a.length) throw new IOException("SU2_MODEL_STATE_SHAPE"); for (int i = 0; i < a.length; i++) a[i] = in.readDouble(); }

    // ------------------------------------------------------------------ MSDIR
    /** Multi-scale forgetting Dirichlet-categorical, Bayesian averaging + fixed share over memory scales. */
    public static final class MsDir implements Model {
        final double[] scales, mem; final double alpha, share; final int S;
        final double[] c, logw;            // c[s][p][n] flattened, logw[s][p]
        final double[] pred, w;
        public MsDir(double[] scales, double alpha, double share) {
            this.scales = scales.clone(); this.alpha = alpha; this.share = share; S = scales.length;
            if (S < 1 || S > 8) throw new IllegalArgumentException("SU2_MSDIR_SCALES");
            c = new double[S * 100]; logw = new double[S * 10]; pred = new double[S * 100]; w = new double[S * 10];
            Arrays.fill(logw, -Math.log(S));
            mem = new double[S];
            for (int s = 0; s < S; s++) mem[s] = scales[s] < 1 ? 1.0 / Math.max(1e-9, 1.0 - scales[s]) : 5000.0;
        }
        public int stateWidth() { return 3; }
        private void computePred() {
            for (int s = 0; s < S; s++) for (int p = 0; p < 10; p++) {
                double sum = 0; for (int n = 0; n < 10; n++) sum += c[s * 100 + p * 10 + n];
                double den = sum + 10 * alpha;
                for (int n = 0; n < 10; n++) pred[s * 100 + p * 10 + n] = (c[s * 100 + p * 10 + n] + alpha) / den;
            }
        }
        private void normalise() {
            for (int p = 0; p < 10; p++) {
                double m = Double.NEGATIVE_INFINITY; for (int s = 0; s < S; s++) m = Math.max(m, logw[s * 10 + p]);
                double z = 0; for (int s = 0; s < S; s++) { w[s * 10 + p] = Math.exp(logw[s * 10 + p] - m); z += w[s * 10 + p]; }
                for (int s = 0; s < S; s++) w[s * 10 + p] /= z;
            }
        }
        public void predict(float[][] out, float[] st) {
            computePred(); normalise();
            double ent = 0, lm = 0, pmax = 0;
            for (int p = 0; p < 10; p++) {
                double e = 0, m = 0;
                for (int s = 0; s < S; s++) { double ws = w[s * 10 + p]; e -= ws * Math.log(ws + 1e-12); m += ws * mem[s]; }
                ent += e; lm += Math.log(m);
                double best = -1;
                for (int n = 0; n < 10; n++) { double v = 0; for (int s = 0; s < S; s++) v += w[s * 10 + p] * pred[s * 100 + p * 10 + n]; out[p][n] = (float) v; best = Math.max(best, (float) v); }
                pmax += best;
            }
            st[0] = (float) (ent / 10.0 / Math.log(S)); st[1] = (float) (lm / 10.0); st[2] = (float) (pmax / 10.0);
        }
        public void update(int[] d) {
            requireDraw(d);
            computePred(); normalise();
            for (int s = 0; s < S; s++) for (int p = 0; p < 10; p++) logw[s * 10 + p] += Math.log(pred[s * 100 + p * 10 + d[p] - 1]);
            normalise();
            for (int i = 0; i < S * 10; i++) { w[i] = (1 - share) * w[i] + share / S; logw[i] = Math.log(w[i]); }
            for (int s = 0; s < S; s++) {
                for (int i = 0; i < 100; i++) c[s * 100 + i] *= scales[s];
                for (int p = 0; p < 10; p++) c[s * 100 + p * 10 + d[p] - 1] += 1.0;
            }
        }
        public void write(DataOutput o) throws IOException { writeArr(o, c); writeArr(o, logw); }
        public void read(DataInput in) throws IOException { readArr(in, c); readArr(in, logw); }
    }

    // ------------------------------------------------------------------ BOCPD
    /** Adams-MacKay change-point detection, Dirichlet-categorical per position, run length truncated at R. */
    public static final class Bocpd implements Model {
        final double hazard, alpha; final int R;
        double[] r, C, nC; final double[] l;
        public Bocpd(double hazard, double alpha, int R) {
            if (R < 8 || R > 512 || !(hazard > 0 && hazard < 1)) throw new IllegalArgumentException("SU2_BOCPD_PARAMS");
            this.hazard = hazard; this.alpha = alpha; this.R = R;
            r = new double[10 * R]; C = new double[10 * R * 10]; nC = new double[10 * R * 10]; l = new double[10 * R];
            for (int p = 0; p < 10; p++) r[p * R] = 1.0;
        }
        public int stateWidth() { return 3; }
        public void predict(float[][] out, float[] st) {
            double recent = 0, elr = 0, pmax = 0;
            for (int p = 0; p < 10; p++) {
                double[] acc = new double[10];
                for (int k = 0; k < R; k++) {
                    int base = (p * R + k) * 10; double n = 0; for (int v = 0; v < 10; v++) n += C[base + v];
                    double den = n + 10 * alpha, rk = r[p * R + k];
                    for (int v = 0; v < 10; v++) acc[v] += rk * ((C[base + v] + alpha) / den);
                }
                double best = -1; for (int v = 0; v < 10; v++) { out[p][v] = (float) acc[v]; best = Math.max(best, (float) acc[v]); }
                pmax += best;
                double s10 = 0, e = 0; for (int k = 0; k < R; k++) { if (k < 10) s10 += r[p * R + k]; e += r[p * R + k] * k; }
                recent += s10; elr += Math.log1p(e);
            }
            st[0] = (float) (recent / 10.0); st[1] = (float) (elr / 10.0); st[2] = (float) (pmax / 10.0);
        }
        public void update(int[] d) {
            requireDraw(d);
            for (int p = 0; p < 10; p++) {
                int x = d[p] - 1;
                for (int k = 0; k < R; k++) { int base = (p * R + k) * 10; double n = 0; for (int v = 0; v < 10; v++) n += C[base + v]; l[p * R + k] = (C[base + x] + alpha) / (n + 10 * alpha); }
                double cp = 0; double[] grow = new double[R];
                for (int k = 0; k < R; k++) { double g = r[p * R + k] * l[p * R + k]; cp += g; grow[k] = g * (1 - hazard); }
                cp *= hazard;
                double[] nr = new double[R]; nr[0] = cp; for (int k = 1; k < R; k++) nr[k] = grow[k - 1]; nr[R - 1] += grow[R - 1];
                double z = 0; for (int k = 0; k < R; k++) z += nr[k];
                for (int k = 0; k < R; k++) r[p * R + k] = nr[k] / z;
                for (int v = 0; v < 10; v++) nC[(p * R) * 10 + v] = 0;
                for (int k = 1; k < R; k++) for (int v = 0; v < 10; v++) nC[(p * R + k) * 10 + v] = C[(p * R + k - 1) * 10 + v];
                for (int v = 0; v < 10; v++) nC[(p * R + R - 1) * 10 + v] += C[(p * R + R - 1) * 10 + v];
                for (int k = 1; k < R; k++) nC[(p * R + k) * 10 + x] += 1.0;
            }
            double[] tmp = C; C = nC; nC = tmp;
        }
        public void write(DataOutput o) throws IOException { writeArr(o, r); writeArr(o, C); }
        public void read(DataInput in) throws IOException { readArr(in, r); readArr(in, C); }
    }

    // -------------------------------------------------------------------- HMM
    /** Whole-draw hidden regime model: K states with own 10x10 emissions, online EM with forgetting. */
    public static final class Hmm implements Model {
        final int K; final double stay, lr, temper; final double[] E, A, a, pi, ll;
        public Hmm(int K, double stay, double lr, long seed, double temper) {
            if (K < 2 || K > 6) throw new IllegalArgumentException("SU2_HMM_K");
            this.K = K; this.stay = stay; this.lr = lr; this.temper = temper;
            double[] z = DeterministicRandomSU2.normals(seed, K * 100);
            E = new double[K * 100];
            for (int k = 0; k < K; k++) for (int p = 0; p < 10; p++) {
                double sum = 0; for (int n = 0; n < 10; n++) { E[k * 100 + p * 10 + n] = 0.1 * Math.exp(0.05 * z[k * 100 + p * 10 + n]); sum += E[k * 100 + p * 10 + n]; }
                for (int n = 0; n < 10; n++) E[k * 100 + p * 10 + n] /= sum;
            }
            A = new double[K * K];
            for (int i = 0; i < K; i++) for (int j = 0; j < K; j++) A[i * K + j] = i == j ? stay : (1 - stay) / Math.max(1, K - 1);
            a = new double[K]; Arrays.fill(a, 1.0 / K); pi = new double[K]; ll = new double[K];
        }
        public int stateWidth() { return 3; }
        private void prior() { for (int j = 0; j < K; j++) { double s = 0; for (int i = 0; i < K; i++) s += a[i] * A[i * K + j]; pi[j] = s; } }
        public void predict(float[][] out, float[] st) {
            prior();
            for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) { double v = 0; for (int k = 0; k < K; k++) v += pi[k] * E[k * 100 + p * 10 + n]; out[p][n] = (float) v; }
            double mx = -1, ent = 0; int arg = 0;
            for (int k = 0; k < K; k++) { if (pi[k] > mx) { mx = pi[k]; arg = k; } ent -= pi[k] * Math.log(pi[k] + 1e-12); }
            st[0] = (float) mx; st[1] = (float) (ent / Math.log(K)); st[2] = (float) (arg / (double) Math.max(1, K - 1));
        }
        public void update(int[] d) {
            requireDraw(d);
            prior();
            double lmax = Double.NEGATIVE_INFINITY;
            for (int k = 0; k < K; k++) { double s = 0; for (int p = 0; p < 10; p++) s += Math.log(E[k * 100 + p * 10 + d[p] - 1]); ll[k] = temper * s; lmax = Math.max(lmax, ll[k]); }
            double z = 0; for (int k = 0; k < K; k++) { a[k] = pi[k] * Math.exp(ll[k] - lmax); z += a[k]; }
            for (int k = 0; k < K; k++) a[k] /= z;
            for (int k = 0; k < K; k++) { double g = lr * a[k]; for (int p = 0; p < 10; p++) { for (int n = 0; n < 10; n++) E[k * 100 + p * 10 + n] *= (1 - g); E[k * 100 + p * 10 + d[p] - 1] += g; } }
        }
        public void write(DataOutput o) throws IOException { writeArr(o, E); writeArr(o, a); }
        public void read(DataInput in) throws IOException { readArr(in, E); readArr(in, a); }
    }

    // ----------------------------------------------------------------- LOGLIN
    /** Cross-position causal log-linear model, online AdaGrad + L2. sources[i] = {offset, lag}. Keeps the last maxlag draws. */
    public static final class LogLin implements Model {
        final int[][] sources; final double lr, l2; final int S, maxlag;
        final double[] b, Gb, W, GW; final int[][] recent; int seen;
        final double[] pr = new double[100]; final int[] idx;
        public LogLin(int[][] sources, double lr, double l2) {
            this.sources = new int[sources.length][]; int ml = 0;
            for (int i = 0; i < sources.length; i++) { int[] s = sources[i]; if (s[0] < 0 || s[0] > 9 || s[1] < 1 || s[1] > 5) throw new IllegalArgumentException("SU2_LOGLIN_SOURCE"); this.sources[i] = s.clone(); ml = Math.max(ml, s[1]); }
            S = sources.length; maxlag = ml; this.lr = lr; this.l2 = l2;
            b = new double[100]; Gb = new double[100]; Arrays.fill(Gb, 1e-3);
            W = new double[S * 1000]; GW = new double[S * 1000]; Arrays.fill(GW, 1e-3);
            recent = new int[maxlag][10]; idx = new int[S * 10];
        }
        public int stateWidth() { return 3; }
        private int[] lagged(int lag) { return recent[(seen - lag) % maxlag]; }
        private void probs() {
            boolean have = seen >= maxlag;
            for (int p = 0; p < 10; p++) {
                double[] z = new double[10];
                for (int n = 0; n < 10; n++) z[n] = b[p * 10 + n];
                if (have) for (int s = 0; s < S; s++) { int a = lagged(sources[s][1])[(p + sources[s][0]) % 10] - 1; idx[s * 10 + p] = a; for (int n = 0; n < 10; n++) z[n] += W[s * 1000 + p * 100 + a * 10 + n]; }
                double m = Double.NEGATIVE_INFINITY; for (int n = 0; n < 10; n++) m = Math.max(m, z[n]);
                double sum = 0; for (int n = 0; n < 10; n++) { pr[p * 10 + n] = Math.exp(z[n] - m); sum += pr[p * 10 + n]; }
                for (int n = 0; n < 10; n++) pr[p * 10 + n] /= sum;
            }
        }
        public void predict(float[][] out, float[] st) {
            probs();
            double pm = 0, ent = 0, absW = 0;
            for (int p = 0; p < 10; p++) { double best = 0, e = 0; for (int n = 0; n < 10; n++) { double v = pr[p * 10 + n]; out[p][n] = (float) v; best = Math.max(best, v); e -= v * Math.log(v); } pm += best; ent += e; }
            for (double v : W) absW += Math.abs(v);
            st[0] = (float) (pm / 10.0); st[1] = (float) (absW / (S * 1000.0) * 100.0); st[2] = (float) (ent / 10.0 / Math.log(10));
        }
        public void update(int[] d) {
            requireDraw(d);
            probs();
            boolean have = seen >= maxlag;
            for (int p = 0; p < 10; p++) {
                int x = d[p] - 1; double[] g = new double[10];
                for (int n = 0; n < 10; n++) g[n] = pr[p * 10 + n] - (n == x ? 1.0 : 0.0);
                for (int n = 0; n < 10; n++) { int i = p * 10 + n; double gb = g[n] + l2 * b[i]; Gb[i] += gb * gb; b[i] -= lr * gb / Math.sqrt(Gb[i]); }
                if (have) for (int s = 0; s < S; s++) { int a = idx[s * 10 + p]; for (int n = 0; n < 10; n++) { int i = s * 1000 + p * 100 + a * 10 + n; double gw = g[n] + l2 * W[i]; GW[i] += gw * gw; W[i] -= lr * gw / Math.sqrt(GW[i]); } }
            }
            System.arraycopy(d, 0, recent[seen % maxlag], 0, 10); seen++;
        }
        public void write(DataOutput o) throws IOException { writeArr(o, b); writeArr(o, Gb); writeArr(o, W); writeArr(o, GW); o.writeInt(seen); for (int[] r : recent) for (int v : r) o.writeByte(v); }
        public void read(DataInput in) throws IOException { readArr(in, b); readArr(in, Gb); readArr(in, W); readArr(in, GW); seen = in.readInt(); for (int[] r : recent) for (int i = 0; i < 10; i++) r[i] = in.readByte(); }
    }

    // --------------------------------------------------------------- RELBAYES
    /** Every relation (target p, source q, lag, map SAME/PLUS1/MINUS1/MIRROR) keeps a forgetting Beta reliability. */
    public static final class RelBayes implements Model {
        final int[] lags; final double decay, kappa, temp; final boolean positiveOnly; final int L, maxlag;
        final double[] s, n; final int[][] recent; int seen; final int[] pred;
        public RelBayes(int[] lags, double decay, double kappa, boolean positiveOnly, double temp) {
            int ml = 0; for (int l : lags) { if (l < 1 || l > 5) throw new IllegalArgumentException("SU2_REL_LAG"); ml = Math.max(ml, l); }
            this.lags = lags.clone(); L = lags.length; maxlag = ml; this.decay = decay; this.kappa = kappa; this.positiveOnly = positiveOnly; this.temp = temp;
            s = new double[10 * 10 * L * 4]; n = new double[10 * 10 * L * 4]; recent = new int[maxlag][10]; pred = new int[10 * L * 4];
        }
        public int stateWidth() { return 3; }
        private boolean preds() {
            if (seen < maxlag) return false;
            for (int q = 0; q < 10; q++) for (int li = 0; li < L; li++) {
                int src = recent[(seen - lags[li]) % maxlag][q] - 1, b = (q * L + li) * 4;
                pred[b] = src; pred[b + 1] = (src + 1) % 10; pred[b + 2] = (src + 9) % 10; pred[b + 3] = 9 - src;
            }
            return true;
        }
        public void predict(float[][] out, float[] st) {
            double[][] score = new double[10][10];
            if (preds()) for (int p = 0; p < 10; p++) for (int q = 0; q < 10; q++) for (int li = 0; li < L; li++) for (int m = 0; m < 4; m++) {
                int i = ((p * 10 + q) * L + li) * 4 + m;
                double rho = (s[i] + 0.1 * kappa) / (n[i] + kappa), w = Math.log(rho / 0.1) / temp;
                if (positiveOnly) w = Math.max(w, 0.0);
                score[p][pred[(q * L + li) * 4 + m]] += w;
            }
            double smax = 0, pm = 0, ss = 0, nn = 0;
            for (int p = 0; p < 10; p++) {
                double mx = Double.NEGATIVE_INFINITY; for (int v = 0; v < 10; v++) mx = Math.max(mx, score[p][v]);
                smax += mx; double sum = 0; double[] e = new double[10];
                for (int v = 0; v < 10; v++) { e[v] = Math.exp(score[p][v] - mx); sum += e[v]; }
                double best = 0; for (int v = 0; v < 10; v++) { out[p][v] = (float) (e[v] / sum); best = Math.max(best, e[v] / sum); }
                pm += best;
            }
            for (double v : s) ss += v; for (double v : n) nn += v;
            st[0] = (float) (smax / 10.0); st[1] = (float) (nn > 0 ? ss / Math.max(nn, 1) : 0.0); st[2] = (float) (pm / 10.0);
        }
        public void update(int[] d) {
            requireDraw(d);
            if (preds()) for (int p = 0; p < 10; p++) {
                int x = d[p] - 1;
                for (int q = 0; q < 10; q++) for (int li = 0; li < L; li++) for (int m = 0; m < 4; m++) {
                    int i = ((p * 10 + q) * L + li) * 4 + m;
                    s[i] = decay * s[i] + (pred[(q * L + li) * 4 + m] == x ? 1.0 : 0.0); n[i] = decay * n[i] + 1.0;
                }
            }
            System.arraycopy(d, 0, recent[seen % maxlag], 0, 10); seen++;
        }
        public void write(DataOutput o) throws IOException { writeArr(o, s); writeArr(o, n); o.writeInt(seen); for (int[] r : recent) for (int v : r) o.writeByte(v); }
        public void read(DataInput in) throws IOException { readArr(in, s); readArr(in, n); seen = in.readInt(); for (int[] r : recent) for (int i = 0; i < 10; i++) r[i] = in.readByte(); }
    }

    // ----------------------------------------------------------------- KALMAN
    /** Local-linear-trend Kalman filter (level + slope with uncertainty) on every (position, number) indicator. */
    public static final class Kalman implements Model {
        final double qLevel, qSlope, r, horizon, temp; final double[] lvl = new double[100], slo = new double[100], P11 = new double[100], P12 = new double[100], P22 = new double[100];
        boolean predicted;
        public Kalman(double qLevel, double qSlope, double r, double horizon, double temp) {
            this.qLevel = qLevel; this.qSlope = qSlope; this.r = r; this.horizon = horizon; this.temp = temp;
            Arrays.fill(lvl, 0.1); Arrays.fill(P11, 1e-2); Arrays.fill(P22, 1e-6);
        }
        public int stateWidth() { return 3; }
        private void timeStep() {
            if (predicted) return;
            for (int i = 0; i < 100; i++) { lvl[i] += slo[i]; P11[i] = P11[i] + 2 * P12[i] + P22[i] + qLevel; P12[i] = P12[i] + P22[i]; P22[i] = P22[i] + qSlope; }
            predicted = true;
        }
        public void predict(float[][] out, float[] st) {
            timeStep();
            double pm = 0, as = 0, sd = 0;
            for (int p = 0; p < 10; p++) {
                double mx = Double.NEGATIVE_INFINITY; double[] f = new double[10];
                for (int v = 0; v < 10; v++) { f[v] = lvl[p * 10 + v] + horizon * slo[p * 10 + v]; mx = Math.max(mx, f[v]); }
                double sum = 0; double[] e = new double[10];
                for (int v = 0; v < 10; v++) { e[v] = Math.exp(temp * (f[v] - mx)); sum += e[v]; }
                double best = 0; for (int v = 0; v < 10; v++) { out[p][v] = (float) (e[v] / sum); best = Math.max(best, e[v] / sum); }
                pm += best;
            }
            for (int i = 0; i < 100; i++) { as += Math.abs(slo[i]); sd += Math.sqrt(P11[i]); }
            st[0] = (float) (as / 100.0 * 1e4); st[1] = (float) (sd / 100.0 * 100); st[2] = (float) (pm / 10.0);
        }
        public void update(int[] d) {
            requireDraw(d);
            timeStep();
            for (int p = 0; p < 10; p++) for (int v = 0; v < 10; v++) {
                int i = p * 10 + v; double y = (d[p] - 1 == v ? 1.0 : 0.0) - lvl[i];
                double S = P11[i] + r, k1 = P11[i] / S, k2 = P12[i] / S;
                lvl[i] += k1 * y; slo[i] += k2 * y; P22[i] = P22[i] - k2 * P12[i]; P12[i] = P12[i] - k2 * P11[i]; P11[i] = P11[i] - k1 * P11[i];
            }
            predicted = false;
        }
        public void write(DataOutput o) throws IOException { o.writeBoolean(predicted); writeArr(o, lvl); writeArr(o, slo); writeArr(o, P11); writeArr(o, P12); writeArr(o, P22); }
        public void read(DataInput in) throws IOException { predicted = in.readBoolean(); readArr(in, lvl); readArr(in, slo); readArr(in, P11); readArr(in, P12); readArr(in, P22); }
    }
}
