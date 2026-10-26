package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * SU1_NULL_BASELINE_V1 reference (R10). A history-independent uniform sham: the
 * choice depends only on a pre-committed seed and the target issue ordinal, so its
 * single-position K3 hit probability is exactly 3/10 for any draw distribution.
 * Byte-for-byte identical to reference/python/null_baseline.py.
 *
 * It is a comparator for evidence only; it never reads history, labels or models,
 * and it never authorizes production influence.
 */
public final class NullBaseline {
    public static final String PROTOCOL = "SU1_NULL_BASELINE_V1";

    public static final class Choice {
        public final int position;
        private final int[] numbers;
        Choice(int position, int[] numbers) { this.position = position; this.numbers = numbers.clone(); }
        public int[] numbers() { return numbers.clone(); }
        public boolean hits(int[] drawPermutation) {
            int actual = drawPermutation[position - 1];
            for (int n : numbers) if (n == actual) return true;
            return false;
        }
    }

    private static final class Stream {
        private byte[] block; private int index; private int counter;
        Stream(byte[] first) { block = first; }
        int next() {
            if (index == block.length) {
                counter++;
                byte[] c = new byte[]{(byte)(counter >>> 24), (byte)(counter >>> 16), (byte)(counter >>> 8), (byte)counter};
                block = sha(concat(block, c)); index = 0;
            }
            return block[index++] & 0xff;
        }
    }

    private NullBaseline() {}

    public static Choice choose(String seedHex, long targetIssue) {
        if (seedHex == null || !seedHex.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("SEED_COMMITMENT_MUST_BE_64_LOWER_HEX");
        if (targetIssue <= 0) throw new IllegalArgumentException("TARGET_ISSUE_ORDINAL");
        byte[] head = (PROTOCOL + "|" + seedHex + "|" + Long.toString(targetIssue)).getBytes(StandardCharsets.UTF_8);
        Stream s = new Stream(sha(head));
        int position = uniform(s, 10) + 1;
        int[] pool = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int size = 10;
        int[] picked = new int[3];
        for (int k = 0; k < 3; k++) {
            int j = uniform(s, 10 - k);
            picked[k] = pool[j];
            System.arraycopy(pool, j + 1, pool, j, size - j - 1);
            size--;
        }
        Arrays.sort(picked);
        return new Choice(position, picked);
    }

    private static int uniform(Stream s, int m) {
        int limit = (256 / m) * m;
        while (true) { int b = s.next(); if (b < limit) return b % m; }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static byte[] sha(byte[] input) {
        try { return MessageDigest.getInstance("SHA-256").digest(input); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    /** Evidence comparator only. */
    public static boolean mayAuthorizeReal() { return false; }
}
