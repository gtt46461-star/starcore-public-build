package com.ethk3.novaonepro;

/**
 * Source-compatible R816 facade. R817 owns the sole current runtime-health boundary.
 */
public final class RuntimeReleaseEpochR816 {
    public static final String EPOCH_KEY = RuntimeReleaseEpochR817.EPOCH_KEY;
    public static final String VERSION_KEY = RuntimeReleaseEpochR817.VERSION_KEY;

    private RuntimeReleaseEpochR816() {}

    /** Creates the boundary once, in the same serialized SQLite writer used by all R3.6 owners. */
    public static void ensure(final DbStore store) {
        RuntimeReleaseEpochR817.ensure(store);
    }

    /** Returns -1 when the durable boundary cannot be read; callers must fail closed. */
    public static long readAt(DbStore store) {
        return RuntimeReleaseEpochR817.readAt(store);
    }
}
