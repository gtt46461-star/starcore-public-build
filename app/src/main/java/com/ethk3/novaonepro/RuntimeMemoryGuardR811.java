package com.ethk3.novaonepro;

/**
 * Allocation-bounded runtime memory telemetry and preflight recovery for the 256 MiB Android heap.
 * It never changes a feature, score, action, lane, authorization, or settlement result.
 */
public final class RuntimeMemoryGuardR811 {
    public static final long MIB = 1024L * 1024L;
    public static final long LOW_HEADROOM_BYTES = 24L * MIB;
    public static final long MIN_START_HEADROOM_BYTES = 16L * MIB;

    private RuntimeMemoryGuardR811() {}

    /** One process-wide partition: callers must not create a new full budget per prediction. */
    private static final class AlgorithmBudgets {
        static final long TOTAL=com.ethk3.novaonepro.r952.MemoryBudgetCP4.algorithmTotal(Runtime.getRuntime().maxMemory());
        static final com.ethk3.novaonepro.r952.MemoryBudgetCP4 MODEL=new com.ethk3.novaonepro.r952.MemoryBudgetCP4(TOTAL/2);
        static final com.ethk3.novaonepro.r952.MemoryBudgetCP4 ATOM_MATRIX=new com.ethk3.novaonepro.r952.MemoryBudgetCP4(TOTAL-TOTAL/2);
    }
    public static com.ethk3.novaonepro.r952.MemoryBudgetCP4 algorithmModelBudget(){return AlgorithmBudgets.MODEL;}
    public static com.ethk3.novaonepro.r952.MemoryBudgetCP4 algorithmAtomMatrixBudget(){return AlgorithmBudgets.ATOM_MATRIX;}


    /** Clear deterministic recomputable caches only under measured pressure; never force GC. */
    public static Snapshot prepareForAnalysis() {
        Snapshot before = snapshot("PREFLIGHT_BEFORE");
        if (before.headroomBytes < LOW_HEADROOM_BYTES) clearRecomputableCaches();
        Snapshot after = snapshot("PREFLIGHT_AFTER");
        if (after.headroomBytes < MIN_START_HEADROOM_BYTES)
            throw new MemoryPressureException(after.canonical());
        return after;
    }

    public static Snapshot snapshot(String stage) {
        Runtime runtime = Runtime.getRuntime();
        long max = Math.max(0L, runtime.maxMemory());
        long allocated = Math.max(0L, runtime.totalMemory());
        long freeInAllocated = Math.max(0L, runtime.freeMemory());
        long used = Math.max(0L, allocated - freeInAllocated);
        long headroom = Math.max(0L, max - used);
        return new Snapshot(stage, max, allocated, used, headroom);
    }

    public static void recoverAfterFailure() {
        clearRecomputableCaches(); // no forced GC/finalization on the critical path
    }

    /** Warm caches survive normal targets; bounded caches own admission/eviction. */
    public static void releaseAfterAnalysis() {
        if (snapshot("POST_COMMIT").headroomBytes < LOW_HEADROOM_BYTES) clearRecomputableCaches();
    }

    private static void clearRecomputableCaches() {
        try {
            RollingWindowCache.shared().clear();
        } catch (Throwable ignored) {
        }
        try {
            FullInformationFeatureGraphEngine.shared().clear();
        } catch (Throwable ignored) {
        }
    }

    public static final class Snapshot {
        public final String stage;
        public final long maxBytes, allocatedBytes, usedBytes, headroomBytes;

        Snapshot(String stage, long max, long allocated, long used, long headroom) {
            this.stage = stage == null ? "" : stage;
            maxBytes = max;
            allocatedBytes = allocated;
            usedBytes = used;
            headroomBytes = headroom;
        }

        public String canonical() {
            return "stage="
                    + stage
                    + "|max="
                    + maxBytes
                    + "|allocated="
                    + allocatedBytes
                    + "|used="
                    + usedBytes
                    + "|headroom="
                    + headroomBytes;
        }
    }

    /** Cooperative retry signal; unlike an Error it leaves enough heap to persist the truth. */
    public static final class MemoryPressureException extends IllegalStateException {
        MemoryPressureException(String detail) {
            super("R368_MEMORY_PREFLIGHT_RETRY|" + detail);
        }
    }
}
