package com.ethk3.novaonepro.strategyv2;

/** Ridge logistic regression by Newton iterations (hre._fit_logistic); intercept unpenalised. */
public final class LogisticRidgeSU2 {
    private LogisticRidgeSU2() {}

    public static double[] fit(double[][] X, double[] y, int rows, double C, int iters) {
        int d = X[0].length, k = d + 1;
        double[] w = new double[k];
        double lam = 1.0 / Math.max(C, 1e-6);
        double[] g = new double[k];
        double[][] H = new double[k][k];
        double[] xb = new double[k];
        for (int it = 0; it < iters; it++) { ExecutionBudgetR26.poll();
            java.util.Arrays.fill(g, 0);
            for (double[] r : H) java.util.Arrays.fill(r, 0);
            for (int i = 0; i < rows; i++) { if((i&7)==0)ExecutionBudgetR26.poll();
                xb[0] = 1; System.arraycopy(X[i], 0, xb, 1, d);
                double z = 0; for (int j = 0; j < k; j++) z += xb[j] * w[j];
                z = Math.max(-30, Math.min(30, z));
                double p = 1.0 / (1.0 + Math.exp(-z)), q = p * (1 - p), e = p - y[i];
                for (int a = 0; a < k; a++) { g[a] += xb[a] * e; double qa = q * xb[a]; for (int b = 0; b < k; b++) H[a][b] += qa * xb[b]; }
            }
            for (int a = 1; a < k; a++) g[a] += lam * w[a];
            for (int a = 0; a < k; a++) H[a][a] += (a == 0 ? 0 : lam) + 1e-9;
            double[] step = solve(H, g);
            double mx = 0;
            for (int a = 0; a < k; a++) { w[a] -= step[a]; mx = Math.max(mx, Math.abs(step[a])); }
            if (mx < 1e-8) break;
        }
        return w;
    }

    static double[] solve(double[][] A0, double[] b0) {
        int n = b0.length;
        double[][] A = new double[n][];
        for (int i = 0; i < n; i++) A[i] = A0[i].clone();
        double[] b = b0.clone();
        for (int c = 0; c < n; c++) { ExecutionBudgetR26.poll();
            int piv = c; for (int r = c + 1; r < n; r++) if (Math.abs(A[r][c]) > Math.abs(A[piv][c])) piv = r;
            double[] tr = A[c]; A[c] = A[piv]; A[piv] = tr; double tb = b[c]; b[c] = b[piv]; b[piv] = tb;
            if (Math.abs(A[c][c]) < 1e-300) throw new IllegalStateException("SU2_LOGISTIC_SINGULAR");
            for (int r = c + 1; r < n; r++) { double f = A[r][c] / A[c][c]; if (f == 0) continue; for (int j = c; j < n; j++) A[r][j] -= f * A[c][j]; b[r] -= f * b[c]; }
        }
        double[] x = new double[n];
        for (int r = n - 1; r >= 0; r--) { double s = b[r]; for (int j = r + 1; j < n; j++) s -= A[r][j] * x[j]; x[r] = s / A[r][r]; }
        return x;
    }
}
