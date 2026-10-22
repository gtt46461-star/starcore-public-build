package com.ethk3.novaonepro;

import java.util.Arrays;

/** Benjamini-Hochberg FDR and chronological block-stability guard for relation edges. */
public final class V51RelationGraphVerifier {
    public boolean[] benjaminiHochberg(double[] pValue, double q) {
        int n = pValue == null ? 0 : pValue.length;
        IntegerIndex[] x = new IntegerIndex[n];
        for (int i = 0; i < n; i++) x[i] = new IntegerIndex(i, V51Math.clamp(pValue[i], 0, 1));
        Arrays.sort(x);
        int max = -1;
        for (int i = 0; i < n; i++) if (x[i].value <= q * (i + 1) / (double) n) max = i;
        boolean[] pass = new boolean[n];
        for (int i = 0; i <= max; i++) pass[x[i].index] = true;
        return pass;
    }

    public boolean stable(double[] blockLift, int minimumPositiveBlocks, double minimumMeanLift) {
        if (blockLift == null || blockLift.length < 4) return false;
        int positive = 0;
        double mean = 0;
        for (int i = 0; i < blockLift.length; i++) {
            double v = V51Math.finite(blockLift[i]);
            if (v > 0) positive++;
            mean += v;
        }
        return positive >= minimumPositiveBlocks && mean / blockLift.length >= minimumMeanLift;
    }

    private static final class IntegerIndex implements Comparable<IntegerIndex> {
        final int index;
        final double value;

        IntegerIndex(int i, double v) {
            index = i;
            value = v;
        }

        public int compareTo(IntegerIndex o) {
            int c = Double.compare(value, o.value);
            return c != 0 ? c : index - o.index;
        }
    }
}
