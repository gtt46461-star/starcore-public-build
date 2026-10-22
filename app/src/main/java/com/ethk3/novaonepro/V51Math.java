package com.ethk3.novaonepro;

/** Small deterministic numeric kernel shared by every V51 inference component. */
public final class V51Math {
    private V51Math() {}

    public static double finite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0d : value;
    }

    public static double clamp(double value, double low, double high) {
        value = finite(value);
        return value < low ? low : (value > high ? high : value);
    }

    public static double sigmoid(double value) {
        value = clamp(value, -35.0d, 35.0d);
        return 1.0d / (1.0d + Math.exp(-value));
    }

    public static double safeLog(double value) {
        return Math.log(Math.max(1.0e-15d, finite(value)));
    }

    public static double entropy(double[] probability) {
        if (probability == null || probability.length == 0) return 1.0d;
        double total = 0.0d;
        for (int i = 0; i < probability.length; i++) {
            double p = Math.max(1.0e-15d, finite(probability[i]));
            total -= p * Math.log(p);
        }
        return clamp(total / Math.log((double) probability.length), 0.0d, 1.0d);
    }

    public static double mean(double[] values) {
        if (values == null || values.length == 0) return 0.0d;
        double sum = 0.0d;
        for (int i = 0; i < values.length; i++) sum += finite(values[i]);
        return sum / values.length;
    }

    public static double variance(double[] values) {
        if (values == null || values.length == 0) return 0.0d;
        double mean = mean(values), sum = 0.0d;
        for (int i = 0; i < values.length; i++) {
            double d = finite(values[i]) - mean;
            sum += d * d;
        }
        return sum / values.length;
    }

    public static double[] copy(double[] values) {
        if (values == null) return new double[0];
        double[] out = new double[values.length];
        System.arraycopy(values, 0, out, 0, values.length);
        return out;
    }

    public static int[] copy(int[] values) {
        if (values == null) return new int[0];
        int[] out = new int[values.length];
        System.arraycopy(values, 0, out, 0, values.length);
        return out;
    }

    public static double[][] copy(double[][] values) {
        if (values == null) return new double[0][0];
        double[][] out = new double[values.length][];
        for (int i = 0; i < values.length; i++) out[i] = copy(values[i]);
        return out;
    }

    public static boolean validPermutation(int[] values) {
        if (values == null || values.length != 10) return false;
        boolean[] seen = new boolean[11];
        for (int i = 0; i < values.length; i++) {
            int n = values[i];
            if (n < 1 || n > 10 || seen[n]) return false;
            seen[n] = true;
        }
        return true;
    }
}
