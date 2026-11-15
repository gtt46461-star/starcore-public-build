package com.ethk3.novaonepro;

/**
 * Safe metadata repair for a provider-confirmed draw whose business identity is already frozen.
 *
 * <p>R816 compared a corrected open time only with the immutable first-receipt timestamp.  Once a
 * bad time had been stored, that rule could permanently prefer it even while the provider was
 * returning a current, validated timestamp.  R817 additionally accepts a current candidate when
 * the existing value is stale, while retaining the conservative R816 rule for historical rows.
 */
public final class ProviderTimeReconcilerR817 {
    private static final long CURRENT_CANDIDATE_MS = 180000L;
    private static final long EXISTING_STALE_MS = 300000L;
    private static final long MAX_FUTURE_SKEW_MS = 60000L;
    private static final long ZONE_TOLERANCE_MS = 120000L;

    private ProviderTimeReconcilerR817() {}

    public static long prefer(
            long existingOpenTime,
            long candidateOpenTime,
            long firstSeenAt,
            long observedAt) {
        long existing = ProviderTimeCodecR816.normalizeEpoch(existingOpenTime);
        long candidate = ProviderTimeCodecR816.normalizeEpoch(candidateOpenTime);
        long now = ProviderTimeCodecR816.normalizeEpoch(observedAt);
        if (candidate <= 0L) return existing;
        if (existing <= 0L || existing == candidate) return candidate;
        if (now > 0L && candidate <= now + MAX_FUTURE_SKEW_MS) {
            long candidateAge = Math.max(0L, now - candidate);
            long existingAge = existing > now ? Long.MAX_VALUE : now - existing;
            if (candidateAge <= CURRENT_CANDIDATE_MS && existingAge >= EXISTING_STALE_MS)
                return candidate;

            long delta = distance(existing, candidate);
            boolean zoneRepair =
                    near(delta, 8L * 60L * 60L * 1000L)
                            || near(delta, 9L * 60L * 60L * 1000L);
            if (zoneRepair && distance(now, candidate) < distance(now, existing)) return candidate;
        }
        return ProviderTimeCodecR816.preferCorrectedOpenTime(
                existing, candidate, firstSeenAt);
    }

    private static boolean near(long value, long expected) {
        return distance(value, expected) <= ZONE_TOLERANCE_MS;
    }

    private static long distance(long left, long right) {
        return left >= right ? left - right : right - left;
    }
}
