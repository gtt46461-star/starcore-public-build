package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.*;
/** Large legacy snapshots resume the same stack; only complete, terminated roots may be released. */
public final class LegacyReplayContinuationR26 {
    private LegacyReplayContinuationR26() { }
    private static final Object LOCK = new Object();
    private static final class Context {
        volatile RuntimeCoordinatorR26.Claim claim;
        CooperativeContinuationR26<Long> work;
        boolean inFlight;
    }
    private static Context active;
    public static void cancel(String owner) {
        Context current; synchronized (LOCK) { current = active; }
        if (current != null && current.claim.owner.equals(owner)) current.work.close();
    }
    public static long step(DbStore store, RuntimeCoordinatorR26.Claim claim, BudgetTokenSU1 budget) throws Exception {
        CooperativeContinuationR26.reapTerminated();
        Context context;
        synchronized (LOCK) {
            if (active != null && !active.claim.owner.equals(claim.owner)) {
                active.work.close();
                if (active.inFlight || !active.work.isTerminated()) { claim.reason("COOPERATIVE_DRAINING_PREVIOUS_LEGACY_OWNER"); return 0; }
                active = null; LegacyReplayR26.clearWorking();
            }
            if (active != null && active.inFlight) { claim.reason("R31_CONTINUATION_SLICE_IN_FLIGHT"); return 0; }
            if (active == null) {
                context = new Context(); context.claim = claim;
                context.work = new CooperativeContinuationR26<>("starcore-r26-legacy-replay",
                    () -> LegacyReplayR26.step(store, () -> context.claim, ExecutionBudgetR26.current()),
                    RuntimeCoordinatorR26::hasCriticalPressure, () -> RuntimeCoordinatorR26.bindBackgroundClaim(context.claim),
                    () -> android.os.SystemClock.currentThreadTimeMillis() * 1000000L, claim.owner);
                active = context;
            } else { context = active; claim.inheritCommitChunksR31(context.claim); context.claim = claim; }
            context.inFlight = true;
        }
        try {
            budget.checkpointOrYield();
            CooperativeContinuationR26.Slice<Long> slice = context.work.slice(CooperativeContinuationR26.CONTINUATION_SLICE_MS);
            claim.addWorkerCpuMillis(slice.cpuNanos / 1000000L);
            if (!slice.done) { claim.reason(slice.state == CooperativeContinuationR26.State.CANCELLING ? "R31_CONTINUATION_CANCELLING" : "COOPERATIVE_LEGACY_STATE_AND_CHECKPOINT"); return 0; }
            if (!context.work.awaitTermination(25)) { claim.reason("COOPERATIVE_DRAINING_COMPLETED_LEGACY_REPLAY"); return 0; }
            synchronized (LOCK) { if (active == context) active = null; }
            if (slice.error != null) {
                LegacyReplayR26.clearWorking();
                if (slice.error instanceof Exception) throw (Exception) slice.error;
                throw new IllegalStateException("R26_LEGACY_CONTINUATION_FAILED", slice.error);
            }
            return slice.result;
        } finally { synchronized (LOCK) { context.inFlight = false; } }
    }
}
