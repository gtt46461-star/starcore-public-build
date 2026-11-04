package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.*;
import java.util.*;

/** One private computation per task. Map ownership never covers a numerical slice or shutdown wait. */
public final class TaskContinuationR26 {
    private TaskContinuationR26() { }
    private static final class Context {
        volatile RuntimeCoordinatorR26.Claim claim;
        long target;
        CooperativeContinuationR26<Long> work;
        boolean inFlight;
    }
    private static final Map<String, Context> ACTIVE = new HashMap<>();

    public static void cancel(String owner) {
        List<CooperativeContinuationR26<Long>> cancel = new ArrayList<>();
        synchronized (ACTIVE) {
            for (Context context : ACTIVE.values()) if (context.claim.owner.equals(owner)) cancel.add(context.work);
        }
        for (CooperativeContinuationR26<Long> work : cancel) work.close();
    }
    public static long step(DbStore store, RuntimeCoordinatorR26.Claim claim, BudgetTokenSU1 budget, long nextTarget) throws Exception {
        CooperativeContinuationR26.reapTerminated();
        Context context;
        synchronized (ACTIVE) {
            Context previous = ACTIVE.get(claim.task);
            if (previous != null) {
                boolean ownerChanged = !previous.claim.owner.equals(claim.owner);
                boolean targetChanged = claim.task.equals("incumbent-forecast") && previous.target != nextTarget;
                if (ownerChanged || targetChanged || previous.work.state() == CooperativeContinuationR26.State.CANCELLING) {
                    previous.work.close();
                    if (previous.inFlight || !previous.work.isTerminated()) {
                        claim.reason(ownerChanged ? "COOPERATIVE_DRAINING_PREVIOUS_TASK_OWNER" : targetChanged ? "COOPERATIVE_DRAINING_SUPERSEDED_INCUMBENT" : "R31_CONTINUATION_CANCELLING");
                        return 0;
                    }
                    ACTIVE.remove(claim.task); previous = null;
                }
                if (previous != null && previous.inFlight) { claim.reason("R31_CONTINUATION_SLICE_IN_FLIGHT"); return 0; }
            }
            if (previous == null) {
                context = new Context(); context.claim = claim; context.target = nextTarget;
                context.work = new CooperativeContinuationR26<>("starcore-r26-" + claim.task,
                    () -> compute(store, context, nextTarget), RuntimeCoordinatorR26::hasCriticalPressure,
                    () -> RuntimeCoordinatorR26.bindBackgroundClaim(context.claim),
                    () -> android.os.SystemClock.currentThreadTimeMillis() * 1000000L, claim.owner);
                ACTIVE.put(claim.task, context);
            } else {
                context = previous;
                claim.inheritCommitChunksR31(context.claim);
                context.claim = claim;
            }
            context.inFlight = true;
        }
        try {
            budget.checkpointOrYield();
            CooperativeContinuationR26.Slice<Long> slice = context.work.slice(CooperativeContinuationR26.CONTINUATION_SLICE_MS);
            claim.addWorkerCpuMillis(slice.cpuNanos / 1000000L);
            if (!slice.done) { claim.reason(slice.state == CooperativeContinuationR26.State.CANCELLING ? "R31_CONTINUATION_CANCELLING" : "COOPERATIVE_NUMERICAL_STATE_AND_CHECKPOINT"); return 0; }
            if (!context.work.awaitTermination(25)) { claim.reason("COOPERATIVE_DRAINING_COMPLETED_TASK"); return 0; }
            synchronized (ACTIVE) { if (ACTIVE.get(claim.task) == context) ACTIVE.remove(claim.task); }
            if (slice.error != null) {
                if (slice.error instanceof Exception) throw (Exception) slice.error;
                throw new IllegalStateException("R26_TASK_CONTINUATION_FAILED", slice.error);
            }
            return slice.result;
        } finally { synchronized (ACTIVE) { context.inFlight = false; } }
    }
    private static long compute(DbStore store, Context context, long target) throws Exception {
        String task = context.claim.task; BudgetTokenSU1 token = ExecutionBudgetR26.current();
        ExecutionBudgetR26.phase("R26_TASK:" + task);
        if (task.equals("foundry-su1")) {
            try (CampaignStateSU1 state = CampaignStateSU1.open(store)) {
                if (state == null || state.foundry.done()) return 0;
                try {
                    StrategyFoundrySU1.Attempt attempt = state.foundry.next(token); state.checkpoint(attempt);
                    CooperativeContinuationR26.recordCheckpoint("SU1_ATTEMPT"); return 1;
                } catch (BudgetTokenSU1.Yield yielded) {
                    state.checkpoint(null); CooperativeContinuationR26.recordCheckpoint("SU1_YIELD"); throw yielded;
                }
            }
        }
        if (task.startsWith("expert-")) return ExpertWorkshopR26.step(store, () -> context.claim, token, target);
        if (task.equals("incumbent-forecast")) return IncumbentLoopSU2.prepareNext(store, () -> context.claim, token, target);
        if (task.equals("warm-candidates")) return WarmCandidateR26.step(store, () -> context.claim, token);
        if (task.equals("publish-models")) return LivePoolR26.publishOne(store, () -> context.claim, token);
        if (task.equals("live-forecast-preparation")) return PreparedForecastStoreR26.step(store, () -> context.claim, token);
        if (task.equals("live-model-training")) return LivePoolR26.trainOne(store, () -> context.claim, token);
        return FoundryCampaignR26.step(store, () -> context.claim, token);
    }
}
