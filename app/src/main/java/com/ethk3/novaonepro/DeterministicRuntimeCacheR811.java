package com.ethk3.novaonepro;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/** Bounded immutable cache shell; a mismatch disables the cache and fails closed. */
public final class DeterministicRuntimeCacheR811 {
    private final int maxEntries;
    private final LinkedHashMap<CacheKey, Entry> entries;
    private boolean enabled = true;
    private long hits, misses, parityChecks, parityFailures;

    public DeterministicRuntimeCacheR811(int maxEntries) {
        this.maxEntries = Math.max(1, Math.min(256, maxEntries));
        entries = new LinkedHashMap<CacheKey, Entry>(16, 0.75f, true);
    }

    public synchronized byte[] get(CacheKey key) {
        if (!enabled || key == null || !key.valid()) { misses++; return null; }
        Entry entry = entries.get(key);
        if (entry == null) { misses++; return null; }
        if (!entry.hash.equals(hash(entry.bytes))) {
            invalidate("CACHE_PARITY_FAIL:STORED_BYTES");
            misses++;
            return null;
        }
        hits++;
        return entry.bytes.clone();
    }

    public synchronized void put(CacheKey key, byte[] value) {
        if (!enabled || key == null || !key.valid() || value == null) return;
        entries.put(key, new Entry(value));
        while (entries.size() > maxEntries) {
            CacheKey eldest = entries.keySet().iterator().next();
            entries.remove(eldest);
        }
    }

    /** First-hit/direct recomputation parity hook. */
    public synchronized boolean verify(CacheKey key, Callable<byte[]> directComputation) {
        if (!enabled || key == null || directComputation == null) return false;
        Entry cached = entries.get(key);
        if (cached == null) return false;
        parityChecks++;
        try {
            byte[] direct = directComputation.call();
            if (direct == null || !cached.hash.equals(hash(direct))) {
                parityFailures++;
                invalidate("CACHE_PARITY_FAIL:DIRECT_MISMATCH");
                return false;
            }
            return true;
        } catch (Throwable e) {
            parityFailures++;
            invalidate("CACHE_PARITY_FAIL:DIRECT_ERROR");
            return false;
        }
    }

    public synchronized void invalidate() { invalidate("MANUAL"); }

    public synchronized void invalidate(String reason) {
        entries.clear();
        enabled = false;
    }

    public synchronized Stats stats() {
        return new Stats(enabled, entries.size(), hits, misses, parityChecks, parityFailures);
    }

    public static final class CacheKey {
        public final String historyHash, targetIssue, artifactBundleHash, codeVersion, stageId;

        public CacheKey(String history, String target, String artifact, String code, String stage) {
            historyHash = safe(history);
            targetIssue = safe(target);
            artifactBundleHash = safe(artifact);
            codeVersion = safe(code);
            stageId = safe(stage);
        }

        boolean valid() {
            return historyHash.length() == 64
                    && artifactBundleHash.length() == 64
                    && TargetIssueResolver.isValid(targetIssue)
                    && codeVersion.length() > 0
                    && stageId.length() > 0;
        }

        public String canonical() {
            return historyHash + '|' + targetIssue + '|' + artifactBundleHash + '|' + codeVersion + '|' + stageId;
        }

        @Override public int hashCode() { return canonical().hashCode(); }
        @Override public boolean equals(Object other) {
            return other instanceof CacheKey && canonical().equals(((CacheKey) other).canonical());
        }
    }

    public static final class Stats {
        public final boolean enabled;
        public final int entries;
        public final long hits, misses, parityChecks, parityFailures;
        Stats(boolean e, int n, long h, long m, long c, long f) {
            enabled = e; entries = n; hits = h; misses = m; parityChecks = c; parityFailures = f;
        }
    }

    private static final class Entry {
        final byte[] bytes;
        final String hash;
        Entry(byte[] value) { bytes = value.clone(); hash = hash(bytes); }
    }

    private static String hash(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder out = new StringBuilder(64);
            for (int i = 0; i < digest.length; i++) {
                int value = digest[i] & 0xff;
                if (value < 16) out.append('0');
                out.append(Integer.toHexString(value));
            }
            return out.toString();
        } catch (Exception e) { throw new IllegalStateException("R811_CACHE_HASH", e); }
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
