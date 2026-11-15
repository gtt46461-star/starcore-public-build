package com.ethk3.novaonepro;

import java.util.Locale;

/** Immutable-after-build point-in-time feature surface for one target issue. */
public final class V49FeatureSnapshot {
    public static final int P = 10;
    public static final int N = 10;
    public static final int W = 9;

    public String baseIssue = "";
    public String targetIssue = "";
    public long createdAt;
    public int historySize;
    public final int[] windows = new int[] {6, 12, 24, 48, 96, 180, 360, 720, 1440};
    public final double[][][] frequency = new double[W][P][N];
    public final double[][] gapNorm = new double[P][N];
    public final double[][] gapPercentile = new double[P][N];
    public final double[][] slope = new double[P][N];
    public final double[][] acceleration = new double[P][N];
    public final double[][] volatility = new double[P][N];
    public final double[][] lastDistance = new double[P][N];
    public final double[][] neighborLast = new double[P][N];
    public final double[][] sameParityLast = new double[P][N];
    public final double[][] sameBigLast = new double[P][N];
    public final double[] entropy = new double[P];
    public final double[] concentration = new double[P];
    public final int[] lastNumber = new int[P];
    public final String[] family = new String[P];
    public final String[] lifecycle = new String[P];
    public final double[] switchRisk = new double[P];
    public final double[] familyStrength = new double[P];
    public final int[] stateRunLength = new int[P];
    public String checksum = "";

    public V49FeatureSnapshot() {
        createdAt = System.currentTimeMillis();
        for (int p = 0; p < P; p++) {
            family[p] = "BALANCED_OSCILLATION";
            lifecycle[p] = "START";
        }
    }

    public boolean isValid() {
        if (targetIssue == null || targetIssue.length() == 0 || historySize < 12) return false;
        for (int p = 0; p < P; p++) {
            double sum = 0.0d;
            for (int n = 0; n < N; n++) sum += frequency[4][p][n];
            if (Math.abs(sum - 1.0d) > 1.0e-6d) return false;
        }
        return true;
    }

    public double feature(int p, int n, int index) {
        switch (index) {
            case 0:
                return p / 9.0d;
            case 1:
                return n / 9.0d;
            case 2:
                return frequency[0][p][n];
            case 3:
                return frequency[1][p][n];
            case 4:
                return frequency[2][p][n];
            case 5:
                return frequency[3][p][n];
            case 6:
                return frequency[4][p][n];
            case 7:
                return frequency[5][p][n];
            case 8:
                return frequency[6][p][n];
            case 9:
                return frequency[7][p][n];
            case 10:
                return frequency[8][p][n];
            case 11:
                return gapNorm[p][n];
            case 12:
                return gapPercentile[p][n];
            case 13:
                return slope[p][n];
            case 14:
                return acceleration[p][n];
            case 15:
                return volatility[p][n];
            case 16:
                return entropy[p];
            case 17:
                return concentration[p];
            case 18:
                return lastDistance[p][n];
            case 19:
                return neighborLast[p][n];
            case 20:
                return sameParityLast[p][n];
            case 21:
                return sameBigLast[p][n];
            case 22:
                return switchRisk[p];
            case 23:
                return familyStrength[p];
            default:
                return 0.0d;
        }
    }

    public String positionSummary(int p) {
        return "P"
                + (p + 1)
                + "|"
                + family[p]
                + "|"
                + lifecycle[p]
                + "|run="
                + stateRunLength[p]
                + "|switch="
                + String.format(Locale.US, "%.3f", switchRisk[p])
                + "|strength="
                + String.format(Locale.US, "%.3f", familyStrength[p]);
    }
}
