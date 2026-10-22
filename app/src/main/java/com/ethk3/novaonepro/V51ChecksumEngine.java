package com.ethk3.novaonepro;

import java.security.MessageDigest;

/** Deterministic SHA-256 utilities used by all V51 ledgers and artifacts. */
public final class V51ChecksumEngine {
    private V51ChecksumEngine() {}

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes("UTF-8"));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xff;
                if (v < 16) out.append('0');
                out.append(Integer.toHexString(v));
            }
            return out.toString();
        } catch (Throwable error) {
            throw new IllegalStateException("V51_SHA256_UNAVAILABLE", error);
        }
    }

    public static String hashDoubles(double[][] matrix) {
        StringBuilder payload = new StringBuilder();
        if (matrix != null) {
            for (int i = 0; i < matrix.length; i++) {
                double[] row = matrix[i];
                if (row == null) {
                    payload.append("null;");
                    continue;
                }
                for (int j = 0; j < row.length; j++) {
                    payload.append(Long.toHexString(Double.doubleToLongBits(row[j]))).append(',');
                }
                payload.append(';');
            }
        }
        return sha256(payload.toString());
    }

    public static String hashInts(int[] values) {
        StringBuilder payload = new StringBuilder();
        if (values != null)
            for (int i = 0; i < values.length; i++) payload.append(values[i]).append(',');
        return sha256(payload.toString());
    }
}
