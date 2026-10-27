package com.ethk3.novaonepro.strategyv2;
import com.ethk3.novaonepro.*;
import java.util.*;
/** At most one private parked computation per ready expert/foundry; service still runs one quantum at a time. */
public final class TaskContinuationR26 {
    private TaskContinuationR26() {}
    private static final class Context {volatile RuntimeCoordinatorR26.Claim claim;CooperativeContinuationR26<Long> work;}
    private static final Map<String,Context> ACTIVE=new HashMap<>();
    public static synchronized void cancel(String owner){for(Context context:ACTIVE.values())if(context.claim.owner.equals(owner))context.work.close();}
    public static synchronized long step(DbStore store,RuntimeCoordinatorR26.Claim claim,BudgetTokenSU1 budget,long nextTarget)throws Exception {
        Context previous=ACTIVE.get(claim.task);if(previous!=null&&!previous.claim.owner.equals(claim.owner)){
            previous.work.close();if(!previous.work.isDone()){claim.reason("COOPERATIVE_DRAINING_PREVIOUS_TASK_OWNER");return 0;}ACTIVE.remove(claim.task);
        }
        if(!ACTIVE.containsKey(claim.task)){Context context=new Context();context.claim=claim;
            context.work=new CooperativeContinuationR26<>("starcore-r26-"+claim.task,()->{BudgetTokenSU1 token=ExecutionBudgetR26.current();if(claim.task.startsWith("expert-"))return ExpertWorkshopR26.step(store,()->context.claim,token,nextTarget);if(claim.task.equals("publish-models"))return LivePoolR26.publishOne(store,()->context.claim,token);if(claim.task.equals("live-model-training"))return LivePoolR26.trainOne(store,()->context.claim,token);return FoundryCampaignR26.step(store,()->context.claim,token);},RuntimeCoordinatorR26::hasCriticalPressure,()->RuntimeCoordinatorR26.bindBackgroundClaim(context.claim),()->android.os.SystemClock.currentThreadTimeMillis()*1000000L);ACTIVE.put(claim.task,context);
        }
        Context context=ACTIVE.get(claim.task);context.claim=claim;budget.checkpointOrYield();CooperativeContinuationR26.Slice<Long> slice=context.work.slice(150);claim.addWorkerCpuMillis(slice.cpuNanos/1000000L);
        if(!slice.done){claim.reason("COOPERATIVE_NUMERICAL_STATE_AND_CHECKPOINT");return 0;}context.work.close();ACTIVE.remove(claim.task);
        if(slice.error!=null){if(slice.error instanceof Exception)throw(Exception)slice.error;throw new IllegalStateException("R26_TASK_CONTINUATION_FAILED",slice.error);}return slice.result;
    }
}
