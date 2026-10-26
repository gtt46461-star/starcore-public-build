package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * P49 stage layer (stagelayer_core.py in the Python reference): recognise the current stage of the draws from its
 * atomic fingerprint, recall how every deployed strategy performs in that stage, and let the best-matched strategy act.
 *
 *   fingerprint   per period from draws < t: at scales 30 and 100, per position the normalised entropy of the number
 *                 counts, the strongest number's frequency z, odd and big shares; repeat / neighbour rates over 30 draws;
 *                 drift |freq20 - freq120|; the regime-memory "no structure" posterior; the per-period means of the eight
 *                 per-position blocks; every candidate's hit rate over its last 50 periods
 *   stage library diagonal Gaussian mixture on standardised 25-period block fingerprints, K = 1..6 by BIC; PC1-quantile
 *                 initialisation (50 power iterations), 60 EM iterations, variance floor 1e-3
 *   sticky filter stay 0.95 over the mixture likelihood of every period
 *   fusion        per candidate a ridge logistic regression (IRLS, 8 Newton steps, lambda 1) of its hit on
 *                 [1, logit confidence, stage posterior, posterior x logit confidence]
 * fit() uses settled periods [a, b) only; apply()/Live run forward one period at a time. The deployment rule (best
 * candidate, causal top-`cov` threshold, validation safeguard against the tier committee) is in the caller.
 */
public final class StageLayerSU2 {
    static final int START = 1500, BLOCK = 25, KMAX = 6, EM_ITERS = 60, PI_ITERS = 50, NEWTON = 8;
    static final double STAY = 0.95, LAMBDA = 1.0;
    static final int BASE_COLS = 92;

    private StageLayerSU2() {}

    // ------------------------------------------------------------------ fingerprint
    /** Fingerprint row of target t (draws D[0..t-1]); st0 = regime-memory null posterior at t; perf = candidates' last-50 hit rates. */
    public static double[] row(int[][] D, int t, double st0, double[] perf) {
        double[] f = new double[BASE_COLS + perf.length];
        double[][] blockMeans = new double[8][];
        int col = 0, blk = 0;
        for (int w : new int[]{30, 100}) {
            double[][] cnt = new double[10][10];
            if (t >= w) { for (int u = t - w; u < t; u++) for (int p = 0; p < 10; p++) cnt[p][D[u][p] - 1] += 1; }
            else { double sc = w / (double) Math.max(1, t); for (int u = 0; u < t; u++) for (int p = 0; p < 10; p++) cnt[p][D[u][p] - 1] += 1; for (double[] r : cnt) for (int n = 0; n < 10; n++) r[n] *= sc; }
            double[][] blocks = new double[4][10];
            for (int p = 0; p < 10; p++) {
                double s = ModelOpsSU2.pairwise(cnt[p], 0, 10) + 5;
                double[] e = new double[10];
                for (int n = 0; n < 10; n++) { double q = (cnt[p][n] + 0.5) / s; e[n] = q * Math.log(q); }
                blocks[0][p] = -ModelOpsSU2.pairwise(e, 0, 10) / Math.log(10);
                double mx = Double.NEGATIVE_INFINITY; for (int n = 0; n < 10; n++) mx = Math.max(mx, cnt[p][n]);
                blocks[1][p] = (mx - w / 10.0) / Math.sqrt(w * 0.09);
                double odd = 0, big = 0; for (int n = 0; n < 10; n += 2) odd += cnt[p][n]; for (int n = 5; n < 10; n++) big += cnt[p][n];
                blocks[2][p] = odd / w - 0.5; blocks[3][p] = big / w - 0.5;
            }
            for (int b = 0; b < 4; b++) { System.arraycopy(blocks[b], 0, f, col, 10); col += 10; blockMeans[blk++] = blocks[b]; }
        }
        double rep30 = 0, nb30 = 0;
        if (t >= 30) for (int u = t - 30; u < t; u++) { if (u < 1) continue; int r = 0, nb = 0; for (int p = 0; p < 10; p++) { if (D[u][p] == D[u - 1][p]) r++; if (Math.abs(D[u][p] - D[u - 1][p]) == 1) nb++; } rep30 += r / 10.0; nb30 += nb / 10.0; }
        f[col++] = t >= 30 ? rep30 / 30 : 0; f[col++] = t >= 30 ? nb30 / 30 : 0;
        double[] dr = new double[100];
        double[][] c20 = new double[10][10], c120 = new double[10][10];
        if (t >= 20) for (int u = t - 20; u < t; u++) for (int p = 0; p < 10; p++) c20[p][D[u][p] - 1] += 1;
        if (t >= 120) for (int u = t - 120; u < t; u++) for (int p = 0; p < 10; p++) c120[p][D[u][p] - 1] += 1;
        for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) dr[p * 10 + n] = Math.abs(c20[p][n] / 20 - c120[p][n] / 120);
        f[col++] = ModelOpsSU2.pairwise(dr, 0, 100); f[col++] = st0;
        for (double[] b : blockMeans) f[col++] = ModelOpsSU2.pairwise(b, 0, 10) / 10;
        System.arraycopy(perf, 0, f, col, perf.length);
        return f;
    }

    /** Batch fingerprints for t = 0..T-1; hits[s][t] = candidate s hit at t; st0[t] from a RegMem model run over D. */
    public static double[][] fingerprints(int[][] D, boolean[][] hits, double[] st0) {
        int T = D.length, S = hits.length; double[][] F = new double[T][];
        for (int t = 0; t < T; t++) {
            double[] perf = new double[S];
            for (int s = 0; s < S; s++) { if (t < 50) { perf[s] = 0.3; continue; } int k = 0; for (int u = t - 50; u < t; u++) if (hits[s][u]) k++; perf[s] = k / 50.0; }
            F[t] = row(D, t, st0[t], perf);
        }
        return F;
    }

    // ------------------------------------------------------------------ mixture
    public static final class Gmm { final double[][] mu, var; final double[] w; final double bic; Gmm(double[][] mu, double[][] var, double[] w, double bic) { this.mu = mu; this.var = var; this.w = w; this.bic = bic; } public int K() { return w.length; } }

    static double[] loglik(double[] x, Gmm g) { return loglik(x, g.mu, g.var, g.w); }
    static double[] loglik(double[] x, double[][] mu, double[][] var, double[] w) {
        int K = w.length, d = x.length; double[] out = new double[K], tmp = new double[d];
        for (int k = 0; k < K; k++) { for (int j = 0; j < d; j++) { double z = x[j] - mu[k][j]; tmp[j] = z * z / var[k][j] + Math.log(2 * Math.PI * var[k][j]); } out[k] = -0.5 * ModelOpsSU2.pairwise(tmp, 0, d) + Math.log(w[k]); }
        return out;
    }

    static Gmm gmmFit(double[][] X, int K) {
        int n = X.length, d = X[0].length;
        double[] mean = new double[d]; for (double[] r : X) for (int j = 0; j < d; j++) mean[j] += r[j]; for (int j = 0; j < d; j++) mean[j] /= n;
        double[][] C = new double[d][d];
        if (n > 1) for (double[] r : X) for (int i = 0; i < d; i++) { double a = r[i] - mean[i]; for (int j = 0; j < d; j++) C[i][j] += a * (r[j] - mean[j]); }
        for (int i = 0; i < d; i++) for (int j = 0; j < d; j++) C[i][j] = n > 1 ? C[i][j] / (n - 1) : (i == j ? 1 : 0);
        double[] v = new double[d]; Arrays.fill(v, 1 / Math.sqrt(d));
        for (int it = 0; it < PI_ITERS; it++) { double[] u = new double[d]; for (int i = 0; i < d; i++) { double s = 0; for (int j = 0; j < d; j++) s += C[i][j] * v[j]; u[i] = s; } double nr = 0; for (double a : u) nr += a * a; nr = Math.sqrt(nr) + 1e-12; for (int i = 0; i < d; i++) v[i] = u[i] / nr; }
        double[] proj = new double[n]; for (int i = 0; i < n; i++) { double s = 0; for (int j = 0; j < d; j++) s += X[i][j] * v[j]; proj[i] = s; }
        Integer[] ord = new Integer[n]; for (int i = 0; i < n; i++) ord[i] = i;
        Arrays.sort(ord, (a, b) -> proj[a] != proj[b] ? Double.compare(proj[a], proj[b]) : Integer.compare(a, b));
        double[][] mu = new double[K][], var = new double[K][d]; double[] w = new double[K];
        double[] colVar = new double[d];
        for (int j = 0; j < d; j++) { double s = 0; for (double[] r : X) { double a = r[j] - mean[j]; s += a * a; } colVar[j] = s / n + 1e-3; }
        for (int k = 0; k < K; k++) { mu[k] = X[ord[(int) ((k + 0.5) / K * (n - 1))]].clone(); var[k] = colVar.clone(); w[k] = 1.0 / K; }
        double[][] R = new double[n][K];
        for (int it = 0; it < EM_ITERS; it++) {
            for (int i = 0; i < n; i++) { double[] ll = loglik(X[i], mu, var, w); double m = Double.NEGATIVE_INFINITY; for (double a : ll) m = Math.max(m, a); double s = 0; for (int k = 0; k < K; k++) { R[i][k] = Math.exp(ll[k] - m); s += R[i][k]; } for (int k = 0; k < K; k++) R[i][k] /= s; }
            double[] Nk = new double[K]; for (double[] r : R) for (int k = 0; k < K; k++) Nk[k] += r[k]; for (int k = 0; k < K; k++) Nk[k] += 1e-9;
            for (int k = 0; k < K; k++) {
                w[k] = Nk[k] / n;
                double[] m1 = new double[d], m2 = new double[d];
                for (int i = 0; i < n; i++) { double r = R[i][k]; for (int j = 0; j < d; j++) { m1[j] += r * X[i][j]; m2[j] += r * X[i][j] * X[i][j]; } }
                for (int j = 0; j < d; j++) { double mm = m1[j] / Nk[k]; mu[k][j] = mm; var[k][j] = Math.max(m2[j] / Nk[k] - mm * mm + 1e-3, 1e-3); }
            }
        }
        double[] rowTot = new double[n];
        for (int i = 0; i < n; i++) { double[] ll = loglik(X[i], mu, var, w); double m = Double.NEGATIVE_INFINITY; for (double a : ll) m = Math.max(m, a); double s = 0; for (double a : ll) s += Math.exp(a - m); rowTot[i] = m + Math.log(s); }
        double tot = ModelOpsSU2.pairwise(rowTot, 0, n);
        return new Gmm(mu, var, w, -2 * tot + (K * 2.0 * d + K - 1) * Math.log(n));
    }

    static double[] post(Gmm g, double[] x) {
        double[] ll = loglik(x, g); double m = Double.NEGATIVE_INFINITY; for (double a : ll) m = Math.max(m, a);
        double s = 0; double[] r = new double[ll.length]; for (int k = 0; k < ll.length; k++) { r[k] = Math.exp(ll[k] - m); s += r[k]; } for (int k = 0; k < r.length; k++) r[k] /= s; return r;
    }

    /** One sticky-filter step: a = previous posterior, lik = mixture posterior of this period. */
    static double[] stickyStep(double[] a, double[] lik) {
        int K = lik.length; double[] out = new double[K]; double off = (1 - STAY) / Math.max(1, K - 1), dia = K > 1 ? STAY : 1.0, s = 0;
        for (int j = 0; j < K; j++) { double pj = 0; for (int i = 0; i < K; i++) pj += a[i] * (i == j ? dia : off); out[j] = pj * Math.max(lik[j], 1e-12); s += out[j]; }
        for (int j = 0; j < K; j++) out[j] /= s;
        return out;
    }

    // ------------------------------------------------------------------ logistic
    static double logitConf(double c) { c = Math.min(Math.max(c, 1e-6), 1 - 1e-6); return Math.log(c / (1 - c)); }
    static double[] design(double[] post, double lc) { int K = post.length; double[] x = new double[2 + 2 * K]; x[0] = 1; x[1] = lc; for (int k = 0; k < K; k++) { x[2 + k] = post[k]; x[2 + K + k] = post[k] * lc; } return x; }
    static double sigm(double z) { return 1 / (1 + Math.exp(-Math.min(30, Math.max(-30, z)))); }

    static double[] logitFit(double[][] X, double[] y) {
        int n = X.length, d = X[0].length; double[] w = new double[d];
        for (int it = 0; it < NEWTON; it++) {
            double[] g = new double[d]; double[][] H = new double[d][d];
            for (int i = 0; i < n; i++) { double z = 0; for (int j = 0; j < d; j++) z += X[i][j] * w[j]; double p = sigm(z), r = p - y[i], q = p * (1 - p); for (int j = 0; j < d; j++) { g[j] += X[i][j] * r; for (int k = 0; k < d; k++) H[j][k] += X[i][j] * q * X[i][k]; } }
            for (int j = 0; j < d; j++) { g[j] += LAMBDA * w[j]; H[j][j] += LAMBDA; }
            double[] step = solve(H, g); for (int j = 0; j < d; j++) w[j] -= step[j];
        }
        return w;
    }

    static double[] solve(double[][] A0, double[] b0) {
        int n = b0.length; double[][] A = new double[n][]; for (int i = 0; i < n; i++) A[i] = A0[i].clone(); double[] b = b0.clone();
        for (int c = 0; c < n; c++) {
            int piv = c; for (int r = c + 1; r < n; r++) if (Math.abs(A[r][c]) > Math.abs(A[piv][c])) piv = r;
            double[] tr = A[c]; A[c] = A[piv]; A[piv] = tr; double tb = b[c]; b[c] = b[piv]; b[piv] = tb;
            for (int r = c + 1; r < n; r++) { double f = A[r][c] / A[c][c]; if (f == 0) continue; for (int k = c; k < n; k++) A[r][k] -= f * A[c][k]; b[r] -= f * b[c]; }
        }
        double[] x = new double[n];
        for (int r = n - 1; r >= 0; r--) { double s = b[r]; for (int k = r + 1; k < n; k++) s -= A[r][k] * x[k]; x[r] = s / A[r][r]; }
        return x;
    }

    // ------------------------------------------------------------------ fit / apply
    public static final class Layer {
        public final Gmm gmm; final double[] mu, sd; final double[][] W; final double[] last;
        Layer(Gmm g, double[] mu, double[] sd, double[][] W, double[] last) { this.gmm = g; this.mu = mu; this.sd = sd; this.W = W; this.last = last; }
        public int stages() { return gmm.K(); }
        double[] z(double[] f) { double[] o = new double[f.length]; for (int j = 0; j < f.length; j++) o[j] = (f[j] - mu[j]) / sd[j]; return o; }
    }

    public static Layer fit(double[][] F, double[][] confs, boolean[][] hits, int a, int b) {
        int d = F[0].length; List<double[]> blocks = new ArrayList<>();
        for (int i = a; i <= b - BLOCK; i += BLOCK) { double[] m = new double[d]; for (int u = i; u < i + BLOCK; u++) for (int j = 0; j < d; j++) m[j] += F[u][j]; for (int j = 0; j < d; j++) m[j] /= BLOCK; blocks.add(m); }
        int nb = blocks.size(); double[] mu = new double[d], sd = new double[d];
        for (double[] m : blocks) for (int j = 0; j < d; j++) mu[j] += m[j]; for (int j = 0; j < d; j++) mu[j] /= nb;
        for (int j = 0; j < d; j++) { double s = 0; for (double[] m : blocks) { double e = m[j] - mu[j]; s += e * e; } sd[j] = Math.sqrt(s / nb) + 1e-9; }
        double[][] Z = new double[nb][d]; for (int i = 0; i < nb; i++) for (int j = 0; j < d; j++) Z[i][j] = (blocks.get(i)[j] - mu[j]) / sd[j];
        Gmm best = null;
        for (int K = 1; K <= KMAX; K++) { Gmm g = gmmFit(Z, K); if (best == null || g.bic < best.bic) best = g; }
        Layer tmp = new Layer(best, mu, sd, null, null);
        int K = best.K(), n = b - a; double[][] post = new double[n][];
        double[] at = new double[K]; Arrays.fill(at, 1.0 / K);
        for (int u = 0; u < n; u++) { at = stickyStep(at, post(best, tmp.z(F[a + u]))); post[u] = at; }
        double[][] W = new double[confs.length][];
        for (int s = 0; s < confs.length; s++) {
            double[][] X = new double[n][]; double[] y = new double[n];
            for (int u = 0; u < n; u++) { X[u] = design(post[u], logitConf(confs[s][a + u])); y[u] = hits[s][a + u] ? 1 : 0; }
            W[s] = logitFit(X, y);
        }
        return new Layer(best, mu, sd, W, post[n - 1]);
    }

    /** P(hit) [b-a][S] for periods [a, b), filtering forward from the end of the fit. */
    public static double[][] apply(Layer L, double[][] F, double[][] confs, int a, int b) {
        Live live = new Live(L); double[][] out = new double[b - a][];
        for (int u = a; u < b; u++) { double[] c = new double[confs.length]; for (int s = 0; s < confs.length; s++) c[s] = confs[s][u]; out[u - a] = live.step(F[u], c); }
        return out;
    }

    /** One period at a time (the phone): fingerprint row of the target and the candidates' confidences -> P(hit) each. */
    public static final class Live {
        final Layer L; double[] a;
        public Live(Layer L) { this.L = L; this.a = L.last.clone(); }
        public double[] step(double[] fingerprintRow, double[] conf) {
            a = stickyStep(a, post(L.gmm, L.z(fingerprintRow)));
            double[] out = new double[conf.length];
            for (int s = 0; s < conf.length; s++) { double[] x = design(a, logitConf(conf[s])); double z = 0; for (int j = 0; j < x.length; j++) z += x[j] * L.W[s][j]; out[s] = sigm(z); }
            return out;
        }
    }
}
