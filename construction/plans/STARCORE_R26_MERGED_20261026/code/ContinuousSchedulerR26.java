import java.util.*;

/** HOST reference for dispatch fairness only. Android uses durable queues and
 * cooperative kernels. No power/idle/screen eligibility parameter exists. */
public final class ContinuousSchedulerR26 {
    public enum Lane { P0, P1, P2, P3 }
    public static final class Task {
        public final String id; public final Lane lane;
        public long cursor, steps, lastProgress; public boolean ready=true;
        Task(String id,Lane lane){this.id=id;this.lane=lane;}
    }
    private final EnumMap<Lane,ArrayDeque<Task>> queues=new EnumMap<>(Lane.class);
    private final HashSet<String> ids=new HashSet<>();
    // Work-conserving slots: feedback, research, research, maintenance.
    // Available capacity is never withheld waiting for an idle device.
    private final Lane[] slots={Lane.P1,Lane.P2,Lane.P2,Lane.P3};
    private int slot;
    public ContinuousSchedulerR26(){for(Lane l:Lane.values())queues.put(l,new ArrayDeque<>());}
    public Task add(String id,Lane lane){
        if(id==null||id.isEmpty()||!ids.add(id))throw new IllegalArgumentException("DUPLICATE_TASK");
        Task t=new Task(id,lane);queues.get(lane).add(t);return t;
    }
    private Task rotate(Lane lane){
        ArrayDeque<Task> q=queues.get(lane);int n=q.size();
        while(n-->0){Task t=q.removeFirst();q.addLast(t);if(t.ready)return t;}return null;
    }
    public Task next(boolean userEnabled,boolean osAllowed,boolean criticalSection,
                     boolean severeThermal,boolean memoryAvailable){
        if(!userEnabled||!osAllowed)return null;
        Task critical=rotate(Lane.P0);if(critical!=null)return critical;
        if(criticalSection||severeThermal||!memoryAvailable)return null;
        for(int n=0;n<slots.length;n++){
            Lane l=slots[slot];slot=(slot+1)%slots.length;
            Task t=rotate(l);if(t!=null)return t;
        }
        return null;
    }
    public void committed(Task t,long newCursor,long now){
        if(newCursor<=t.cursor||now<t.lastProgress)throw new IllegalArgumentException("NO_DURABLE_PROGRESS");
        t.cursor=newCursor;t.steps++;t.lastProgress=now;
    }
    public List<String> starved(long now,long maxWait){
        List<String> out=new ArrayList<>();
        for(ArrayDeque<Task> q:queues.values())for(Task t:q)if(t.ready&&now-t.lastProgress>maxWait)out.add(t.id);
        return out;
    }
}
