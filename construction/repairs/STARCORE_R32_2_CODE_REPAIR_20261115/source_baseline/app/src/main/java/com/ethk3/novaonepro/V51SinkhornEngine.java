package com.ethk3.novaonepro;

/**
 * B09 log-domain Sinkhorn projection for the 10x10 assignment surface. Any invalid/non-converged
 * projection is explicit fallback and therefore invalid for production decision.
 */
public final class V51SinkhornEngine {
    public static final String V57_B09_OWNER = "NUMBER_Q_NORMALIZATION_ONLY";
    public static final int SIZE = 10;
    private static final int DEFAULT_MAX_ITERATIONS = 240;
    private static final double DEFAULT_TOLERANCE = 1.0e-10d;

    public enum FallbackReason {
        NONE,
        INVALID_SHAPE,
        NON_FINITE_SCORE,
        INVALID_TEMPERATURE,
        LOG_DOMAIN_FAILURE,
        NON_CONVERGENCE,
        INVALID_ASSIGNMENT
    }

    public static final class ProjectionResult {
        public final V51Models.NumberProbabilityMatrix matrix;
        public final FallbackReason fallbackReason;
        public final double maxLogKernel, minLogKernel;
        public final int iterations;
        public final double rowError, columnError;
        public final String diagnostics;

        ProjectionResult(
                V51Models.NumberProbabilityMatrix m,
                FallbackReason r,
                double maxLog,
                double minLog,
                int it,
                double re,
                double ce) {
            matrix = m;
            fallbackReason = r;
            maxLogKernel = maxLog;
            minLogKernel = minLog;
            iterations = it;
            rowError = re;
            columnError = ce;
            diagnostics =
                    "reason="
                            + r
                            + "|iter="
                            + it
                            + "|row="
                            + re
                            + "|col="
                            + ce
                            + "|logMin="
                            + minLog
                            + "|logMax="
                            + maxLog;
        }

        public boolean productionValid() {
            return fallbackReason == FallbackReason.NONE
                    && matrix != null
                    && matrix.validForDecision();
        }
    }

    public V51Models.NumberProbabilityMatrix project(double[][] score, double temperature) {
        return projectDetailed(score, temperature, DEFAULT_MAX_ITERATIONS, DEFAULT_TOLERANCE)
                .matrix;
    }

    public V51Models.NumberProbabilityMatrix project(
            double[][] score, double temperature, int maxIterations, double tolerance) {
        return projectDetailed(score, temperature, maxIterations, tolerance).matrix;
    }

    public ProjectionResult projectDetailed(
            double[][] score, double temperature, int maxIterations, double tolerance) {
        if (!validShape(score)) return fallback(FallbackReason.INVALID_SHAPE, 0, 0, 0);
        if (Double.isNaN(temperature) || Double.isInfinite(temperature) || temperature <= 0.0d)
            return fallback(FallbackReason.INVALID_TEMPERATURE, 0, 0, 0);
        double t = Math.max(0.05d, Math.min(5.0d, temperature)),
                tol = Math.max(1e-14, Math.min(1e-4, tolerance));
        int maxIt = Math.max(8, Math.min(2000, maxIterations));
        double[][] logK = new double[SIZE][SIZE];
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++) {
                double s = score[i][j];
                if (Double.isNaN(s) || Double.isInfinite(s))
                    return fallback(FallbackReason.NON_FINITE_SCORE, 0, 0, 0);
                double z = Math.max(-80.0d, Math.min(80.0d, s / t));
                logK[i][j] = z;
                if (z < min) min = z;
                if (z > max) max = z;
            }
        double[] u = new double[SIZE], v = new double[SIZE];
        int iter = 0;
        double rowError = Double.POSITIVE_INFINITY, columnError = Double.POSITIVE_INFINITY;
        double[][] q = new double[SIZE][SIZE];
        try {
            for (iter = 1; iter <= maxIt; iter++) {
                for (int i = 0; i < SIZE; i++) {
                    double[] tmp = new double[SIZE];
                    for (int j = 0; j < SIZE; j++) tmp[j] = logK[i][j] + v[j];
                    u[i] = -logSumExp(tmp);
                }
                for (int j = 0; j < SIZE; j++) {
                    double[] tmp = new double[SIZE];
                    for (int i = 0; i < SIZE; i++) tmp[i] = logK[i][j] + u[i];
                    v[j] = -logSumExp(tmp);
                }
                if (iter == 1 || iter % 4 == 0 || iter == maxIt) {
                    materialize(logK, u, v, q);
                    rowError = maxRowError(q);
                    columnError = maxColumnError(q);
                    if (Math.max(rowError, columnError) <= tol) break;
                }
            }
        } catch (Throwable x) {
            return fallback(FallbackReason.LOG_DOMAIN_FAILURE, min, max, iter);
        }
        materialize(logK, u, v, q);
        rowError = maxRowError(q);
        columnError = maxColumnError(q);
        if (Double.isNaN(rowError)
                || Double.isInfinite(rowError)
                || Double.isNaN(columnError)
                || Double.isInfinite(columnError))
            return fallback(FallbackReason.LOG_DOMAIN_FAILURE, min, max, iter);
        if (Math.max(rowError, columnError) > Math.max(1e-8, tol * 10.0d))
            return fallback(FallbackReason.NON_CONVERGENCE, min, max, iter);
        int[] map = V51AssignmentDecoder.decode(q);
        if (!V51Math.validPermutation(map))
            return fallback(FallbackReason.INVALID_ASSIGNMENT, min, max, iter);
        String checksum =
                V51ChecksumEngine.sha256(
                        V51ChecksumEngine.hashDoubles(q)
                                + '|'
                                + iter
                                + '|'
                                + rowError
                                + '|'
                                + columnError
                                + "|LOG_DOMAIN");
        V51Models.NumberProbabilityMatrix m =
                new V51Models.NumberProbabilityMatrix(
                        q, rowError, columnError, iter, true, false, map, checksum);
        return new ProjectionResult(m, FallbackReason.NONE, min, max, iter, rowError, columnError);
    }

    private static ProjectionResult fallback(
            FallbackReason reason, double min, double max, int iter) {
        double[][] q = new double[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) for (int j = 0; j < SIZE; j++) q[i][j] = 0.1d;
        int[] map = V51AssignmentDecoder.decode(q);
        String checksum = V51ChecksumEngine.sha256("B09_SINKHORN_FALLBACK|" + reason + '|' + iter);
        V51Models.NumberProbabilityMatrix m =
                new V51Models.NumberProbabilityMatrix(
                        q, 0.0d, 0.0d, iter, false, true, map, checksum);
        return new ProjectionResult(m, reason, max, min, iter, 0.0d, 0.0d);
    }

    private static void materialize(double[][] logK, double[] u, double[] v, double[][] q) {
        for (int i = 0; i < SIZE; i++)
            for (int j = 0; j < SIZE; j++)
                q[i][j] = Math.exp(Math.max(-745.0d, Math.min(50.0d, logK[i][j] + u[i] + v[j])));
    }

    private static double logSumExp(double[] x) {
        double m = -Double.MAX_VALUE;
        for (int i = 0; i < x.length; i++) if (x[i] > m) m = x[i];
        if (m == -Double.MAX_VALUE) throw new IllegalStateException("B09_LOGSUMEXP_EMPTY");
        double s = 0;
        for (int i = 0; i < x.length; i++)
            s += Math.exp(Math.max(-745.0d, Math.min(50.0d, x[i] - m)));
        if (!(s > 0.0d)) throw new IllegalStateException("B09_LOGSUMEXP_ZERO");
        return m + Math.log(s);
    }

    private static boolean validShape(double[][] x) {
        if (x == null || x.length != SIZE) return false;
        for (int i = 0; i < SIZE; i++) if (x[i] == null || x[i].length != SIZE) return false;
        return true;
    }

    private static double maxRowError(double[][] x) {
        double e = 0;
        for (int i = 0; i < SIZE; i++) {
            double s = 0;
            for (int j = 0; j < SIZE; j++) s += x[i][j];
            e = Math.max(e, Math.abs(1.0d - s));
        }
        return e;
    }

    private static double maxColumnError(double[][] x) {
        double e = 0;
        for (int j = 0; j < SIZE; j++) {
            double s = 0;
            for (int i = 0; i < SIZE; i++) s += x[i][j];
            e = Math.max(e, Math.abs(1.0d - s));
        }
        return e;
    }
}
