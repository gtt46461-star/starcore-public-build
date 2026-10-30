package com.ethk3.novaonepro.strategyv2;

/** One bounded continuation for an existing task. Its own writer transactions remain indivisible; compute polls
 * park only outside those critical sections. A suspended call is not a completed numerical sample. These tasks
 * start after database cutover, so a parked cursor never crosses replacement of its worker's read connection. */
public final class CooperativeResearchTaskR29 implements ResearchPoolR27.Task, AutoCloseable {
    private final ResearchPoolR27.Task task;private final String owner;
    private volatile boolean closed;
    private volatile CooperativeContinuationR26<ResearchPoolR27.Outcome> work;
    CooperativeResearchTaskR29(ResearchPoolR27.Task task,String session){this.task=task;owner=session+":R29_TASK:"+task.id();}
    public String id(){return task.id();}
    public boolean enabled(){return !closed&&task.enabled();}
    public boolean boundedSlice(){return true;}
    // Once admitted, long urgent work receives fair resumptions instead of monopolizing every service turn.
    public boolean urgent(ResearchPoolR27.Worker worker,long now){return work==null&&task.urgent(worker,now);}
    public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker worker)throws Exception{
        if(closed||worker.cancelled())return ResearchPoolR27.Outcome.idle("USER_STOP",0,-1);
        if(!ResearchRouteR29.active(worker.realtimeRo()))return ResearchPoolR27.Outcome.idle("WAITING_VERIFIED_DATABASE_CUTOVER",0,-1);
        synchronized(this){if(closed)return ResearchPoolR27.Outcome.idle("USER_STOP",0,-1);if(work==null)work=new CooperativeContinuationR26<>("starcore-r29-"+id(),()->task.step(worker),()->closed||worker.cancelled(),null,()->android.os.SystemClock.currentThreadTimeMillis()*1000000L,owner);}
        CooperativeContinuationR26<ResearchPoolR27.Outcome> current=work;
        CooperativeContinuationR26.Slice<ResearchPoolR27.Outcome> slice=current.slice(40);
        if(!slice.done)return ResearchPoolR27.Outcome.idle("COOPERATIVE_PRIVATE_RESEARCH_TASK:"+id(),0,-1);
        synchronized(this){if(work==current)work=null;}
        if(closed||worker.cancelled())return ResearchPoolR27.Outcome.idle("USER_STOP",0,-1);
        if(slice.error!=null){if(slice.error instanceof Exception)throw(Exception)slice.error;throw new IllegalStateException("R29_RESEARCH_CONTINUATION:"+id(),slice.error);}
        return slice.result;
    }
    public synchronized void cancel(){closed=true;if(work!=null)work.close();}
    public boolean awaitStopped(long millis){return CooperativeContinuationR26.awaitOwnerStopped(owner,millis);}
    public void close(){cancel();while(!awaitStopped(1000))android.util.Log.w("STARCORE_R29","draining cancelled "+id());}
}
