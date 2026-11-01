package com.ethk3.novaonepro.strategyv2;

/** Parked Java stack retains one task's immutable inputs, RNG draft and exact pending write batch.
 * A process restart uses the last database checkpoint; a parked thread is never durable progress. */
final class PreparedTaskR30R1 implements AutoCloseable {
    final ResearchPoolR27.Task task;final ResearchPoolR27.Worker worker;
    final CooperativeContinuationR26<ResearchPoolR27.Outcome> continuation;
    volatile RuntimeCoordinatorR26.Claim claim;
    PreparedTaskR30R1(ResearchPoolR27.Task task,ResearchPoolR27.Worker worker,RuntimeCoordinatorR26.Claim claim){
        this.task=task;this.worker=worker;this.claim=claim;
        continuation=new CooperativeContinuationR26<>("starcore-r30-prepared-"+task.id(),()->{android.os.Process.setThreadPriority(ResearchPoolR27.BACKGROUND_PRIORITY);return task.step(worker);},()->worker.cancelled(),()->RuntimeCoordinatorR26.bindBackgroundClaim(this.claim),()->android.os.SystemClock.currentThreadTimeMillis()*1000000L,"r30-r27:"+claim.owner);
    }
    ResearchPoolR27.Outcome slice()throws Exception{
        CooperativeContinuationR26.Slice<ResearchPoolR27.Outcome> slice=continuation.slice(200);
        if(!slice.done)return ResearchPoolR27.Outcome.idle("COOPERATIVE_PREPARED_STEP",claim.cursor,-1);
        if(slice.error instanceof Exception)throw(Exception)slice.error;
        if(slice.error!=null)throw new IllegalStateException("R30_PREPARED_TASK_FAILED",slice.error);
        return slice.result;
    }
    boolean done(){return continuation.isDone();}
    @Override public void close(){continuation.close();}
}
