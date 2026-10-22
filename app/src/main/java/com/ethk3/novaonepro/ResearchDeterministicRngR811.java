package com.ethk3.novaonepro;

/** Deterministic xorshift64* RNG for Research initialization and reproducible fixtures only. */
public final class ResearchDeterministicRngR811 {
    private long state;

    public ResearchDeterministicRngR811(long seed) {
        state = seed == 0L ? 0x9e3779b97f4a7c15L : seed;
    }

    public static long seed(String value) {
        String hash = V51ChecksumEngine.sha256(value == null ? "" : value);
        long out = 0L;
        for (int i = 0; i < 16; i++) out = (out << 4) | Character.digit(hash.charAt(i), 16);
        return out == 0L ? 0x9e3779b97f4a7c15L : out;
    }

    public long xorshift() {
        long x = state;
        x ^= x >>> 12;
        x ^= x << 25;
        x ^= x >>> 27;
        state = x;
        return x * 2685821657736338717L;
    }

    public double nextDouble() {
        return (xorshift() >>> 11) * 0x1.0p-53;
    }

    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("R811_RNG_BOUND");
        return (int) Math.floor(nextDouble() * bound);
    }
}
