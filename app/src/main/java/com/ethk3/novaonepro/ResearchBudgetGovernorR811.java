package com.ethk3.novaonepro;

/** Research slice admission/yield policy. It owns no scheduler and never writes Production data. */
public final class ResearchBudgetGovernorR811 {
    private ResearchBudgetGovernorR811() {}

    public static boolean canRunSlice(Metrics metrics) {
        if (metrics == null || RealtimePressureGateR811.hasActiveCriticalWork()) return false;
        RealtimePressureGateR811.Snapshot pressure = RealtimePressureGateR811.snapshot();
        return metrics.settlementLag <= AppConfig.R811_RESEARCH_MAX_SETTLEMENT_LAG
                && metrics.deferredPending <= AppConfig.R811_RESEARCH_MAX_DEFERRED_PENDING
                && metrics.analysisIdle
                && metrics.liveP95Ms < AppConfig.R811_RESEARCH_PAUSE_P95_MS
                && !metrics.lowStorage;
    }

    public static Slice beginSlice(Metrics metrics) {
        return canRunSlice(metrics)
                ? new Slice(TimeSourceR811.monoNowMs(), Thread.currentThread().getId(), true)
                : new Slice(0L, Thread.currentThread().getId(), false);
    }

    public static boolean shouldYield(Slice slice, int candidatesProcessed) {
        return slice == null
                || !slice.admitted
                || slice.finished
                || RealtimePressureGateR811.hasActiveCriticalWork()
                || candidatesProcessed >= AppConfig.R811_RESEARCH_MAX_CANDIDATES_PER_SLICE
                || TimeSourceR811.elapsed(slice.startedMonoMs)
                        >= AppConfig.R811_RESEARCH_SLICE_BUDGET_MS;
    }

    public static Result finishSlice(Slice slice, int candidatesProcessed) {
        if (slice == null) return new Result(false, 0L, 0, "SLICE_NULL");
        if (slice.ownerThreadId != Thread.currentThread().getId())
            return new Result(false, TimeSourceR811.elapsed(slice.startedMonoMs), candidatesProcessed, "OWNER_MISMATCH");
        slice.finished = true;
        long elapsed = slice.admitted ? TimeSourceR811.elapsed(slice.startedMonoMs) : 0L;
        boolean within =
                slice.admitted
                        && elapsed <= AppConfig.R811_RESEARCH_SLICE_BUDGET_MS
                        && candidatesProcessed <= AppConfig.R811_RESEARCH_MAX_CANDIDATES_PER_SLICE;
        return new Result(within, elapsed, candidatesProcessed, within ? "YIELD_AFTER_SLICE" : "BUDGET_EXCEEDED");
    }

    public static final class Metrics {
        public final int settlementLag, deferredPending;
        public final boolean analysisIdle, lowStorage;
        public final long liveP95Ms;
        public Metrics(int lag, int deferred, boolean idle, long p95, boolean lowStorage) {
            settlementLag = Math.max(0, lag); deferredPending = Math.max(0, deferred);
            analysisIdle = idle; liveP95Ms = Math.max(0L, p95); this.lowStorage = lowStorage;
        }
    }

    public static final class Slice {
        final long startedMonoMs, ownerThreadId;
        final boolean admitted;
        boolean finished;
        Slice(long started, long owner, boolean admitted) { startedMonoMs = started; ownerThreadId = owner; this.admitted = admitted; }
        public boolean admitted() { return admitted; }
    }

    public static final class Result {
        public final boolean withinBudget;
        public final long elapsedMs;
        public final int candidatesProcessed;
        public final String status;
        Result(boolean within, long elapsed, int count, String status) {
            withinBudget = within; elapsedMs = elapsed; candidatesProcessed = Math.max(0, count); this.status = status;
        }
    }
}
