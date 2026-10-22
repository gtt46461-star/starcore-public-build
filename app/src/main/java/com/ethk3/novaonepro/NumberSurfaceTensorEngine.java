package com.ethk3.novaonepro;

/**
 * B09 merged core: position x number x expert x horizon evidence tensor. It owns diagnostics/tensor
 * only and has no authority over SelectedHitP, Lane, BET or settlement.
 */
public final class NumberSurfaceTensorEngine {
    public static final String V57_B09_OWNER = "NUMBER_SURFACE_TENSOR_EVIDENCE_ONLY";
    public static final int POSITIONS = 10, NUMBERS = 10, HORIZONS = 4;
    public static final int[] HORIZON_DRAW = new int[] {12, 48, 192, 720};

    public static final class TensorResult {
        public final double[][][][] tensor;
        public final double[][] exclusive, assignmentMargin, conflict;
        public final double[] effectiveExperts, positionPredictability;
        public final double[][] expertJaccard, expertMutualInformation;
        public final String checksum, diagnostics;

        TensorResult(
                double[][][][] t,
                double[][] ex,
                double[][] margin,
                double[][] conflict,
                double[] neff,
                double[] pp,
                double[][] jac,
                double[][] mi) {
            tensor = copy4(t);
            exclusive = V51Math.copy(ex);
            assignmentMargin = V51Math.copy(margin);
            this.conflict = V51Math.copy(conflict);
            effectiveExperts = V51Math.copy(neff);
            positionPredictability = V51Math.copy(pp);
            expertJaccard = V51Math.copy(jac);
            expertMutualInformation = V51Math.copy(mi);
            checksum = V51ChecksumEngine.sha256(canonical());
            diagnostics =
                    "tensor=10x10x"
                            + (t.length == 0 ? 0 : t[0][0].length)
                            + "x4|NeffAvg="
                            + V51Math.mean(neff)
                            + "|predictabilityAvg="
                            + V51Math.mean(pp)
                            + "|hash="
                            + checksum;
        }

        public String canonical() {
            StringBuilder b = new StringBuilder();
            for (int p = 0; p < POSITIONS; p++) {
                b.append('|')
                        .append(effectiveExperts[p])
                        .append(':')
                        .append(positionPredictability[p]);
                for (int n = 0; n < NUMBERS; n++)
                    b.append(',')
                            .append(exclusive[p][n])
                            .append('/')
                            .append(assignmentMargin[p][n])
                            .append('/')
                            .append(conflict[p][n]);
            }
            return b.toString();
        }
    }

    public TensorResult build(
            V51Models.ExpertEvidence[] experts,
            double[][] gateWeight,
            V51Models.NumberProbabilityMatrix matrix,
            V51Models.StateTrajectorySnapshot[] states) {
        int ecount = experts == null ? 0 : experts.length;
        double[][][][] t = new double[POSITIONS][NUMBERS][ecount][HORIZONS];
        double[][] ex = new double[POSITIONS][NUMBERS],
                margin = new double[POSITIONS][NUMBERS],
                conflict = new double[POSITIONS][NUMBERS];
        double[] neff = new double[POSITIONS], predict = new double[POSITIONS];
        for (int p = 0; p < POSITIONS; p++) {
            double[] row =
                    matrix != null && matrix.probability != null && p < matrix.probability.length
                            ? matrix.probability[p]
                            : new double[NUMBERS];
            neff[p] =
                    effective(
                            gateWeight != null && p < gateWeight.length
                                    ? gateWeight[p]
                                    : new double[0]);
            double stateConfidence =
                    states != null && p < states.length && states[p] != null
                            ? states[p].confidence
                            : 0.0d;
            predict[p] = clamp01((1.0d - V51Math.entropy(row)) * (0.60d + 0.40d * stateConfidence));
            for (int n = 0; n < NUMBERS; n++) {
                double q = n < row.length ? clamp01(row[n]) : 0.0d,
                        maxOther = 0.0d,
                        avgOther = 0.0d;
                for (int pp = 0; pp < POSITIONS; pp++)
                    if (pp != p
                            && matrix != null
                            && matrix.probability != null
                            && pp < matrix.probability.length
                            && n < matrix.probability[pp].length) {
                        double v = clamp01(matrix.probability[pp][n]);
                        if (v > maxOther) maxOther = v;
                        avgOther += v;
                    }
                avgOther /= 9.0d;
                ex[p][n] = clamp(q - avgOther, -1.0d, 1.0d);
                margin[p][n] = clamp(q - maxOther, -1.0d, 1.0d);
                double mean = 0.0d, ws = 0.0d;
                for (int e = 0; e < ecount; e++) {
                    double gw = weight(gateWeight, p, e),
                            raw = raw(experts[e], p, n),
                            ev = evidenceQuality(experts[e], p);
                    double base = sigmoid(raw) * ev;
                    for (int h = 0; h < HORIZONS; h++)
                        t[p][n][e][h] =
                                clamp01(
                                        base * gw * horizonDecay(h)
                                                + q * (1.0d - horizonDecay(h)) * gw);
                    mean += gw * sigmoid(raw);
                    ws += gw;
                }
                if (ws > 0) mean /= ws;
                double var = 0.0d;
                if (ws > 0)
                    for (int e = 0; e < ecount; e++) {
                        double gw = weight(gateWeight, p, e),
                                d = sigmoid(raw(experts[e], p, n)) - mean;
                        var += gw * d * d;
                    }
                conflict[p][n] = clamp01(Math.sqrt(Math.max(0.0d, var)) * 2.0d);
            }
        }
        double[][] jac = new double[ecount][ecount], mi = new double[ecount][ecount];
        for (int a = 0; a < ecount; a++)
            for (int b = 0; b < ecount; b++) {
                boolean[] aa = new boolean[POSITIONS * NUMBERS],
                        bb = new boolean[POSITIONS * NUMBERS];
                int k = 0;
                for (int p = 0; p < POSITIONS; p++)
                    for (int n = 0; n < NUMBERS; n++) {
                        aa[k] = sigmoid(raw(experts[a], p, n)) >= 0.55d;
                        bb[k] = sigmoid(raw(experts[b], p, n)) >= 0.55d;
                        k++;
                    }
                jac[a][b] = jaccard(aa, bb);
                mi[a][b] = mutualInformation(aa, bb);
            }
        return new TensorResult(t, ex, margin, conflict, neff, predict, jac, mi);
    }

    public static boolean hasDecisionAuthority() {
        return false;
    }

    private static double raw(V51Models.ExpertEvidence e, int p, int n) {
        return e != null
                        && e.rawScore != null
                        && p < e.rawScore.length
                        && e.rawScore[p] != null
                        && n < e.rawScore[p].length
                ? finite(e.rawScore[p][n])
                : 0.0d;
    }

    private static double evidenceQuality(V51Models.ExpertEvidence e, int p) {
        if (e == null) return 0.0d;
        double trust = e.trust != null && p < e.trust.length ? clamp01(e.trust[p]) : 0.0d,
                u =
                        e.uncertainty != null && p < e.uncertainty.length
                                ? clamp01(e.uncertainty[p])
                                : 1.0d,
                s =
                        e.signalStrength != null && p < e.signalStrength.length
                                ? clamp01(e.signalStrength[p])
                                : 0.0d;
        return clamp01(trust * (1.0d - u) * (0.35d + 0.65d * s));
    }

    private static double weight(double[][] w, int p, int e) {
        return w != null && p < w.length && w[p] != null && e < w[p].length
                ? Math.max(0.0d, finite(w[p][e]))
                : 0.0d;
    }

    private static double horizonDecay(int h) {
        return h == 0 ? 1.0d : (h == 1 ? 0.86d : (h == 2 ? 0.70d : 0.55d));
    }

    private static double effective(double[] w) {
        double s = 0, s2 = 0;
        for (int i = 0; i < w.length; i++) {
            double x = Math.max(0.0d, finite(w[i]));
            s += x;
            s2 += x * x;
        }
        return s2 <= 1e-15 ? 0.0d : s * s / s2;
    }

    private static double jaccard(boolean[] a, boolean[] b) {
        int in = 0, un = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            if (a[i] || b[i]) un++;
            if (a[i] && b[i]) in++;
        }
        return un == 0 ? 0.0d : (double) in / un;
    }

    private static double mutualInformation(boolean[] a, boolean[] b) {
        int n = Math.min(a.length, b.length);
        if (n == 0) return 0.0d;
        double[][] c = new double[2][2];
        for (int i = 0; i < n; i++) c[a[i] ? 1 : 0][b[i] ? 1 : 0]++;
        double out = 0;
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++) {
                if (c[x][y] <= 0) continue;
                double pxy = c[x][y] / n,
                        px = (c[x][0] + c[x][1]) / n,
                        py = (c[0][y] + c[1][y]) / n;
                out += pxy * Math.log(pxy / Math.max(1e-15, px * py));
            }
        return Math.max(0.0d, out);
    }

    private static double sigmoid(double x) {
        x = clamp(finite(x), -35, 35);
        return 1.0d / (1.0d + Math.exp(-x));
    }

    private static double finite(double x) {
        return Double.isNaN(x) || Double.isInfinite(x) ? 0.0d : x;
    }

    private static double clamp01(double x) {
        return clamp(x, 0, 1);
    }

    private static double clamp(double x, double lo, double hi) {
        x = finite(x);
        return x < lo ? lo : (x > hi ? hi : x);
    }

    private static double[][][][] copy4(double[][][][] x) {
        double[][][][] y = new double[x.length][][][];
        for (int a = 0; a < x.length; a++) {
            y[a] = new double[x[a].length][][];
            for (int b = 0; b < x[a].length; b++) {
                y[a][b] = new double[x[a][b].length][];
                for (int c = 0; c < x[a][b].length; c++) y[a][b][c] = V51Math.copy(x[a][b][c]);
            }
        }
        return y;
    }
}
