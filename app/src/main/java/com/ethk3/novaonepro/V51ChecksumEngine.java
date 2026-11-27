package com.ethk3.novaonepro;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/** Deterministic SHA-256 utilities used by all V51 ledgers and artifacts. */
public final class V51ChecksumEngine {
    private V51ChecksumEngine() {}

    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final ThreadLocal<MessageDigest> SHA_256 = new ThreadLocal<>();

    public static String sha256(String value) {
        try {
            MessageDigest digest = SHA_256.get();
            if (digest == null) {
                digest = MessageDigest.getInstance("SHA-256");
                SHA_256.set(digest);
            }
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            char[] hex = new char[bytes.length * 2];
            for (int i = 0; i < bytes.length; i++) {
                int valueByte = bytes[i] & 0xff;
                hex[i * 2] = HEX[valueByte >>> 4];
                hex[i * 2 + 1] = HEX[valueByte & 0x0f];
            }
            return new String(hex);
        } catch (Throwable error) {
            SHA_256.remove();
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
