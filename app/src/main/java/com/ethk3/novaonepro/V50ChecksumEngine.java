package com.ethk3.novaonepro;

import java.security.MessageDigest;
import java.util.Locale;

/** Stable deterministic SHA-256 helpers for every frozen V50 layer. */
public final class V50ChecksumEngine {
    private V50ChecksumEngine() {}

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(safe(value).getBytes("UTF-8"));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xff;
                if (v < 16) out.append('0');
                out.append(Integer.toHexString(v));
            }
            return out.toString();
        } catch (Throwable e) {
            throw new IllegalStateException("V50_SHA256_UNAVAILABLE", e);
        }
    }

    public static String hashStrings(String prefix, String[] values) {
        StringBuilder sb = new StringBuilder(safe(prefix));
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                sb.append('|').append(i).append('=').append(safe(values[i]));
            }
        }
        return sha256(sb.toString());
    }

    public static String hashDoubles(String prefix, double[] values) {
        StringBuilder sb = new StringBuilder(safe(prefix));
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                sb.append('|')
                        .append(i)
                        .append('=')
                        .append(String.format(Locale.US, "%.15f", finite(values[i])));
            }
        }
        return sha256(sb.toString());
    }

    public static String hashMatrix(String prefix, double[][] matrix) {
        StringBuilder sb = new StringBuilder(safe(prefix));
        if (matrix != null) {
            for (int r = 0; r < matrix.length; r++) {
                double[] row = matrix[r];
                sb.append("|r").append(r).append('[');
                if (row != null) {
                    for (int c = 0; c < row.length; c++) {
                        if (c > 0) sb.append(',');
                        sb.append(String.format(Locale.US, "%.15f", finite(row[c])));
                    }
                }
                sb.append(']');
            }
        }
        return sha256(sb.toString());
    }

    public static double finite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0d : value;
    }

    public static String safe(String value) {
        return value == null ? "" : value;
    }
}
