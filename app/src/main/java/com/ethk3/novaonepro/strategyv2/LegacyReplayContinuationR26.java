package com.ethk3.novaonepro.strategyv2;
import com.ethk3.novaonepro.*;
/** Large legacy snapshots may span service quanta; only their complete root can commit. */
public final class LegacyReplayContinuationR26 {
    private LegacyReplayContinuationR26() {}
    private static final class Context {volatile RuntimeCoordinatorR26.Claim claim;CooperativeContinuationR26<Long> work;}
    private static Context active;
    public static synchronized void cancel(String owner){if(active!=null&&active.claim.owner.equals(owner))active.work.close();}
    public static synchronized long step(DbStore store,RuntimeCoordinatorR26.Claim claim,BudgetTokenSU1 budget)throws Exception {
        if(active!=null&&!active.claim.owner.equals(claim.owner)){
            active.work.close();if(!active.work.isDone()){claim.reason("COOPERATIVE_DRAINING_PREVIOUS_LEGACY_OWNER");return 0;}active=null;LegacyReplayR26.clearWorking();
        }
        if(active==null){Context context=new Context();context.claim=claim;
            context.work=new CooperativeContinuationR26<>("starcore-r26-legacy-replay",()->LegacyReplayR26.step(store,()->context.claim,ExecutionBudgetR26.current()),RuntimeCoordinatorR26::hasCriticalPressure,()->RuntimeCoordinatorR26.bindBackgroundClaim(context.claim),()->android.os.SystemClock.currentThreadTimeMillis()*1000000L);active=context;
        }
        Context context=active;context.claim=claim;budget.checkpointOrYield();CooperativeContinuationR26.Slice<Long> slice=context.work.slice(150);claim.addWorkerCpuMillis(slice.cpuNanos/1000000L);
        if(!slice.done){claim.reason("COOPERATIVE_LEGACY_STATE_AND_CHECKPOINT");return 0;}
        context.work.close();active=null;if(slice.error!=null){LegacyReplayR26.clearWorking();if(slice.error instanceof Exception)throw(Exception)slice.error;throw new IllegalStateException("R26_LEGACY_CONTINUATION_FAILED",slice.error);}return slice.result;
    }
}
