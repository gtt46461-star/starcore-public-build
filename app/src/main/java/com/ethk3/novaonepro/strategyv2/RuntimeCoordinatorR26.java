package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import com.ethk3.novaonepro.*;
import java.util.*;

/** User-session research owner. Power, screen and network class are not eligibility inputs.
 * All writes use DbStore's existing coordinator; claims are durable and fenced. */
public final class RuntimeCoordinatorR26 implements AutoCloseable {
    private static volatile RuntimeCoordinatorR26 active;
    private static final ThreadLocal<Claim> WRITING_CLAIM=new ThreadLocal<>();
    public static void checkThreadFence(SQLiteDatabase db){Claim c=WRITING_CLAIM.get();if(c!=null)requireFence(db,c);}
    private final DbStore store;private final String owner=UUID.randomUUID().toString();
    private final Object signal=new Object();private volatile boolean cancelled,closeStore;
    private final Thread worker;private int slot;private long heartbeat;private volatile String resourceBlocked;private long resourceChecked;
    private static final String[] SLOTS={"P1","P2","P2","P3"};
    public static final class Claim {
        public final String task,owner;public final long fence,cursor;private long completedEventCursor=-1;
        Claim(String task,String owner,long fence,long cursor){this.task=task;this.owner=owner;this.fence=fence;this.cursor=cursor;}
    }
    public RuntimeCoordinatorR26(DbStore store){this.store=Objects.requireNonNull(store);worker=new Thread(this::run,"starcore-r26-continuous-research");worker.setDaemon(true);active=this;worker.start();}
    public static void wake(){RuntimeCoordinatorR26 a=active;if(a!=null)synchronized(a.signal){a.signal.notifyAll();}}
    private boolean preempt(){return cancelled||Thread.currentThread().isInterrupted()||resourceBlocked!=null||SettlementExecutionGateR819.isActive()||RealtimePressureGateR811.hasActiveCriticalWork();}
    private void resources(){
        long mono=SystemClock.elapsedRealtime();if(mono-resourceChecked<1000)return;resourceChecked=mono;String reason=null;
        android.app.ActivityManager manager=(android.app.ActivityManager)store.contextForR26().getSystemService(android.content.Context.ACTIVITY_SERVICE);
        if(manager!=null){android.app.ActivityManager.MemoryInfo memory=new android.app.ActivityManager.MemoryInfo();manager.getMemoryInfo(memory);if(memory.lowMemory)reason="MEMORY_PRESSURE";}
        if(android.os.Build.VERSION.SDK_INT>=29){android.os.PowerManager power=(android.os.PowerManager)store.contextForR26().getSystemService(android.content.Context.POWER_SERVICE);if(power!=null&&power.getCurrentThermalStatus()>=android.os.PowerManager.THERMAL_STATUS_SEVERE)reason="THERMAL_SEVERE";}
        resourceBlocked=reason;
    }
    private void sessionBlocked(String reason){long now=System.currentTimeMillis();if(now-heartbeat<1000)return;heartbeat=now;store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_SESSION_STATUS",db->{db.execSQL("UPDATE r26_session SET state=?,heartbeat=? WHERE session_id=?",new Object[]{reason==null?"RUNNING":"BLOCKED:"+reason,now,owner});return 0;});}
    @Override public void close(){cancelled=true;wake();worker.interrupt();}
    /** The service must not close a shared connection while a slice is draining. */
    public void closeAndReleaseStore(){closeStore=true;close();if(!worker.isAlive())store.close();}
    public boolean isRunning(){return !cancelled && worker.isAlive();}
    public static void requireFence(SQLiteDatabase db,Claim claim){try(Cursor c=db.rawQuery("SELECT 1 FROM r26_task WHERE task_id=? AND owner=? AND fence=? AND status='RUNNING' AND lease_until>?",new String[]{claim.task,claim.owner,String.valueOf(claim.fence),String.valueOf(System.currentTimeMillis())})){if(!c.moveToFirst())throw new IllegalStateException("R26_STALE_FENCE_OR_LEASE");}}
    private void initialize(){
        try{LegacyTeacherOwnerR26.register(store.contextForR26());}catch(RuntimeException blocked){android.util.Log.w("STARCORE_R26","original teacher blocked",blocked);store.setState("r26_teacher_blocked",blocked.toString());}
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_RUNTIME_SCHEMA",db->{RuntimeSchemaR26.ensure(db);long now=System.currentTimeMillis();
            // A new process has a new monotonic epoch. Leases remain wall-clock bounded and fenced.
            db.execSQL("UPDATE r26_session SET state='INTERRUPTED',user_enabled=0 WHERE state='RUNNING'");
            db.execSQL("INSERT OR REPLACE INTO r26_session VALUES(?,1,?,'RUNNING',?,coalesce((SELECT actual_progress FROM r26_session WHERE session_id=?),0))",new Object[]{owner,String.valueOf(now-SystemClock.elapsedRealtime()),now,owner});
            add(db,"schema","P1");add(db,"kernel","P1");add(db,"shadow-recover","P1");add(db,"evidence-recover","P1");add(db,"evidence-quality","P1");add(db,"sealed-planner","P1");add(db,"calibration","P2");add(db,"su1-certificate","P1");add(db,"su2-mainline","P1");add(db,"su2-certificate","P1");add(db,"qualification","P1");add(db,"feedback","P1");add(db,"seed-incumbent","P1");
            add(db,"publish-models","P2");add(db,"live-model-training","P2");add(db,"foundry-su2","P2");add(db,"foundry-su1","P2");add(db,"ablation","P2");add(db,"campaign-audit","P2");add(db,"archive","P3");add(db,"retention","P3");add(db,"diagnosis","P3");add(db,"daily-close","P3");
            for(int i=1;i<=24;i++)registerCapability(db,String.format(Locale.ROOT,"F%02d",i),"FAMILY");for(int i=1;i<=16;i++){String id=String.format(Locale.ROOT,"E%02d",i);registerCapability(db,id,"EXPERT");add(db,"expert-"+id,"P2");}return 0;});
        ReplayBurstSchedulerSU1.cancel(store.contextForR26());
    }
    private static void add(SQLiteDatabase db,String id,String lane){db.execSQL("INSERT OR IGNORE INTO r26_task(task_id,lane,mechanism) VALUES(?,?,?)",new Object[]{id,lane,id});}
    private static void registerCapability(SQLiteDatabase db,String id,String kind){
        String implementation=kind.equals("EXPERT")?ExpertWorkshopR26.implementation(id):FamilyRegistryR26.implementation(id);String state=implementation.isEmpty()?"BLOCKED":"EXECUTABLE";
        String reason=kind.equals("EXPERT")&&!implementation.isEmpty()?"Concrete resumable paired experiment or evidence audit; full proposal and expert acceptance pending":implementation.isEmpty()?"Full R26 mechanism integration and decisive experiments pending; no fallback substituted":"Concrete numerical kernel registered; complete family experiments and natural quality pending";
        db.execSQL("INSERT OR REPLACE INTO r26_capability(mechanism,kind,implementation,state,reason,last_progress) VALUES(?,?,?,?,?,coalesce((SELECT last_progress FROM r26_capability WHERE mechanism=?),0))",new Object[]{id,kind,implementation,state,reason,id});
    }
    private Claim claim(){for(int attempt=0;attempt<SLOTS.length;attempt++){String lane=SLOTS[slot++%SLOTS.length];Claim result=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_TASK_CLAIM",db->{long now=System.currentTimeMillis();String id=null;long fence=0,cursor=0;try(Cursor c=db.rawQuery("SELECT task_id,fence,cursor FROM r26_task WHERE lane=? AND status IN ('READY','YIELDED','BLOCKED','RUNNING') AND lease_until<=? AND next_at<=? ORDER BY last_progress,task_id LIMIT 1",new String[]{lane,String.valueOf(now),String.valueOf(now)})){if(c.moveToFirst()){id=c.getString(0);fence=c.getLong(1)+1;cursor=c.getLong(2);}}if(id==null)return null;db.execSQL("UPDATE r26_task SET status='RUNNING',owner=?,fence=?,lease_until=?,blocked_reason=NULL WHERE task_id=?",new Object[]{owner,fence,now+30000,id});return new Claim(id,owner,fence,cursor);});if(result!=null)return result;}return null;}
    private void run(){
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        try{
            boolean initialized=false;
            while(!cancelled&&!initialized){try{initialize();initialized=true;}catch(RuntimeException retry){android.util.Log.w("STARCORE_R26","initialization retry",retry);await(500);}}
            while(!cancelled){
            if(!new ServiceHeartbeatStore(store.contextForR26()).isUserEnabled()){cancelled=true;break;}
            resources();
            if(preempt()){try{sessionBlocked(resourceBlocked==null?"CRITICAL_DEADLINE":resourceBlocked);}catch(RuntimeException busy){android.util.Log.w("STARCORE_R26","blocked receipt retry",busy);}await(50);continue;}
            try{sessionBlocked(null);}catch(RuntimeException busy){android.util.Log.w("STARCORE_R26","session status retry",busy);}
            Claim claim;
            try{claim=claim();}catch(RuntimeException busy){android.util.Log.w("STARCORE_R26","dispatch retry",busy);await(250);continue;}
            if(claim==null){await(250);continue;}long start=SystemClock.elapsedRealtime(),cpu=SystemClock.currentThreadTimeMillis();long delta=0;String reason="DATA_NOT_READY";
            try{WRITING_CLAIM.set(claim);BudgetTokenSU1 budget=new BudgetTokenSU1(200,3000000,this::preempt);ExecutionBudgetR26.enter(budget);delta=execute(claim,budget);reason=delta>0?null:"DATA_NOT_READY";}
            catch(BudgetTokenSU1.Yield yield){reason=resourceBlocked!=null?resourceBlocked:preempt()?"CRITICAL_DEADLINE":"SLICE_BUDGET";}
            catch(Exception failure){reason=failure.getClass().getSimpleName()+":"+String.valueOf(failure.getMessage());android.util.Log.w("STARCORE_R26",claim.task,failure);}
            finally{ExecutionBudgetR26.leave();WRITING_CLAIM.remove();}
            final long work=delta,nextCursor=claim.completedEventCursor>=0?claim.completedEventCursor:claim.cursor+delta,elapsed=SystemClock.elapsedRealtime()-start,cpuMs=Math.max(0,SystemClock.currentThreadTimeMillis()-cpu);final String blocked=elapsed>1000?"CAPACITY_FAIL:UNSLICED_KERNEL:"+elapsed+"ms|"+reason:reason;
            try{store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_TASK_RECEIPT",db->{requireFence(db,claim);long now=System.currentTimeMillis();db.execSQL("UPDATE r26_task SET cursor=?,status=?,lease_until=0,next_at=?,blocked_reason=?,last_progress=CASE WHEN ?>0 THEN ? ELSE last_progress END,steps=steps+CASE WHEN ?>0 THEN 1 ELSE 0 END,last_cpu_ms=? WHERE task_id=? AND fence=?",new Object[]{nextCursor,blocked==null?"YIELDED":"BLOCKED",now+(work>0||"CRITICAL_DEADLINE".equals(blocked)?0:1000),blocked,work,now,work,cpuMs,claim.task,claim.fence});
                db.execSQL("INSERT INTO r26_progress(task_id,fence,cursor_before,cursor_after,cpu_ms,blocked_reason,recorded_at) VALUES(?,?,?,?,?,?,?)",new Object[]{claim.task,claim.fence,claim.cursor,nextCursor,cpuMs,blocked,now});db.execSQL("DELETE FROM r26_progress WHERE receipt_id<(SELECT max(receipt_id)-4096 FROM r26_progress)");
                db.execSQL("UPDATE r26_session SET heartbeat=?,actual_progress=actual_progress+? WHERE session_id=?",new Object[]{now,work,owner});return 0;});}catch(RuntimeException failure){android.util.Log.w("STARCORE_R26","receipt not committed",failure);}
            // Empty maintenance work waits for its next data event, never spins to fabricate progress.
            if(delta==0)await(10);
        }}catch(Exception failure){android.util.Log.e("STARCORE_R26","runtime blocked",failure);try{store.setState("r26_runtime_error",failure.toString());}catch(Exception ignored){}}
        finally{Thread.interrupted();try{store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_SESSION_STOP",db->{if(RuntimeSchemaR26.ready(db)){db.execSQL("UPDATE r26_session SET state=?,user_enabled=0,heartbeat=? WHERE session_id=?",new Object[]{cancelled?"STOPPED":"FAILED",System.currentTimeMillis(),owner});db.execSQL("UPDATE r26_task SET status='YIELDED',lease_until=0 WHERE owner=? AND status='RUNNING'",new Object[]{owner});}return 0;});}catch(Exception ignored){}if(active==this)active=null;if(closeStore)store.close();}
    }
    private void await(long ms){synchronized(signal){try{if(!cancelled)signal.wait(ms);}catch(InterruptedException e){if(!cancelled)Thread.currentThread().interrupt();}}}
    private long execute(Claim c,BudgetTokenSU1 budget)throws Exception{
        if(c.task.equals("schema")){if(!StrategySchemaSU1.ready(store.readable())){ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorForResearchR952(),1);return 1;}if(!StrategySchemaSU2.ready(store.readable())){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_SU2_SCHEMA",db->{requireFence(db,c);StrategySchemaSU2.ensure(db);return 0;});return 1;}ProgramModelSU2.register();return 0;}
        if(!StrategySchemaSU1.ready(store.readable())||!StrategySchemaSU2.ready(store.readable()))return 0;
        ProgramModelSU2.register();Models.Draw latest=store.getLatestDraw();if(latest==null||!TargetIssueResolver.isValid(latest.issue))return 0;
        long seq=TargetIssueResolver.sequenceOrdinalSU1(latest.issue);String target=TargetIssueResolver.next(latest.issue).targetIssue;
        if(c.task.startsWith("expert-"))return ExpertWorkshopR26.step(store,c,budget,seq+1);
        switch(c.task){
            case "qualification":return CertificateSU2.maintainOne(store,seq+1);
            case "publish-models":return LivePoolR26.publishOne(store,c,budget);
            case "live-model-training":return LivePoolR26.trainOne(store,c,budget);
            case "foundry-su2":return FoundryCampaignR26.step(store,c,budget);
            case "feedback":return ResearchFeedbackSU1.drainOne(store)?1:0;
            case "ablation":return ModelAblationStoreSU1.advanceOne(store,budget)?1:0;
            case "campaign-audit":return CampaignAuditStoreSU1.advanceOne(store,budget)?1:0;
            case "foundry-su1":try(CampaignStateSU1 state=CampaignStateSU1.open(store)){if(state==null||state.foundry.done())return 0;try{StrategyFoundrySU1.Attempt a=state.foundry.next(budget);state.checkpoint(a);return 1;}catch(BudgetTokenSU1.Yield y){state.checkpoint(null);throw y;}}
            default:break;
        }
        // A persisted event cursor prevents repeated maintenance over unchanged evidence.
        if(c.cursor>=seq)return 0;budget.checkpointOrYield();
        switch(c.task){
            case "kernel":KernelTransitionSU1.ensure(store,seq);break;
            case "shadow-recover":LiveShadowLoopSU1.recover(store,8);break;
            case "evidence-recover":EvidenceLoopSU1.recover(store);break;
            case "evidence-quality":EvidenceQualitySU1.maintain(store);break;
            case "sealed-planner":SealedPlannerSU1.maintain(store);break;
            case "calibration":CalibrationPipelineSU1.maintain(store);break;
            case "su1-certificate":CertificateSU1.maintain(store);MainlineHeadSU1.maintain(store);break;
            case "seed-incumbent":IncumbentLoopSU2.seed(store,1);break;
            case "su2-mainline":MainlineBridgeSU2.chooseIfDue(store,seq+1);break;
            case "su2-certificate":CertificateSU2.maintain(store,seq+1);break;
            case "archive":ArchiveStoreSU1.maintain(store);break;
            case "diagnosis":MatrixStoreSU2.maintain(store,target);DiagnosisStoreSU2.maintain(store,target);break;
            case "retention":store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_RETENTION",db->{requireFence(db,c);RetentionRulesSU2.foldDecisions(db,seq+1,System.currentTimeMillis());RetentionRulesSU2.pruneResearchGenomes(db);RetentionR26.prune(db,seq+1);return 0;});break;
            case "daily-close":store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_DAILY_CLOSE",db->{requireFence(db,c);LiveShadowLoopSU1.closeDays(db,Integer.parseInt(latest.issue.substring(0,8)));return 0;});break;
            default:throw new IllegalStateException("R26_UNKNOWN_TASK:"+c.task);
        }
        claim.completedEventCursor=seq;return 1;
    }
}
