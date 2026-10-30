package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import com.ethk3.novaonepro.*;
import java.util.*;

/** User-session research owner. Power, screen and network class are not eligibility inputs.
 * Explicit task scopes resolve legacy calls to the independent research writer after verified migration;
 * claims, leases, numerical state and receipts have the same research transaction owner. */
public final class RuntimeCoordinatorR26 implements AutoCloseable {
    private static volatile RuntimeCoordinatorR26 active;
    private static final ThreadLocal<Claim> WRITING_CLAIM=new ThreadLocal<>();
    public static void checkThreadFence(SQLiteDatabase db){requireCurrentThreadSession();Claim c=WRITING_CLAIM.get();if(c!=null&&!LegacyResearchScopeR29.realtimeTransaction())requireFence(db,c);}
    public static void requireCurrentThreadSession(){Claim c=WRITING_CLAIM.get();RuntimeCoordinatorR26 runtime=active;if(c!=null&&runtime!=null&&(runtime.cancelled||!runtime.owner.equals(c.owner)))throw new IllegalStateException("R26_CANCELLED_OR_REPLACED_PROCESS_OWNER");}
    public static java.util.function.BooleanSupplier sessionValidity(){final RuntimeCoordinatorR26 runtime=active;return ()->runtime==null||active==runtime&&!runtime.cancelled;}
    public static boolean readyForResearchMigration(){RuntimeCoordinatorR26 runtime=active;return runtime==null||runtime.strategyStorageReady;}
    /** The eager legacy overlay shares this Root writer, but cannot race its self-contained bootstrap. */
    public static boolean strategyStoragePending(DatabaseCoordinator owner){RuntimeCoordinatorR26 runtime=active;return runtime!=null&&runtime.store!=null&&runtime.store.coordinatorForResearchR952()==owner&&!runtime.strategyStorageReady;}
    private final DbStore store;private final String owner=UUID.randomUUID().toString();
    private final Object signal=new Object();private volatile boolean cancelled,closeStore;
    private final Thread worker;private int slot;private volatile RuntimeR27 r27;private long lastIncumbentSeq=-1;private long heartbeat;private String recordedSessionState;private volatile boolean strategyStorageReady;private volatile String resourceBlocked;private long resourceChecked;
    private final CapabilityServiceR26 service=new CapabilityServiceR26();
    private volatile RuntimePowerLeaseR26 powerLease;
    private volatile boolean poolDispatcher;
    private final java.util.concurrent.locks.ReentrantLock legacySlice=new java.util.concurrent.locks.ReentrantLock();
    private static final String[] SLOTS={"P1","P2","P2","P3"};
    public static final class Claim {
        public final String task,owner,lane;public final long fence,cursor;private long completedEventCursor=-1,workerCpuMillis;private String reason;
        public void advanceCursorTo(long value){if(value<cursor)throw new IllegalArgumentException("R26_TASK_CURSOR_BACKWARDS");completedEventCursor=value;}
        public void addWorkerCpuMillis(long value){workerCpuMillis+=Math.max(0,value);}
        public void reason(String reason){this.reason=reason;}
        Claim(String task,String owner,long fence,long cursor){this(task,owner,fence,cursor,null);}
        Claim(String task,String owner,long fence,long cursor,String lane){this.task=task;this.owner=owner;this.fence=fence;this.cursor=cursor;this.lane=lane;}
    }
    public RuntimeCoordinatorR26(DbStore store){this.store=Objects.requireNonNull(store);worker=new Thread(this::run,"starcore-r26-continuous-research");worker.setDaemon(true);synchronized(RuntimeCoordinatorR26.class){RuntimeCoordinatorR26 previous=active;if(previous!=null)previous.close();active=this;}worker.start();}
    static void bindBackgroundClaim(Claim claim){WRITING_CLAIM.set(claim);}
    /** Pause signal for numerical continuation workers. R27-005: the minutes-long realtime-analysis pressure gate is
     * no longer a pause reason; only stop, real resources and the millisecond settlement write window are. */
    static boolean hasCriticalPressure(){RuntimeCoordinatorR26 a=active;return a!=null&&(a.cancelled||a.resourceBlocked!=null)||SettlementExecutionGateR819.isActive();}
    public static void wake(){RuntimeCoordinatorR26 a=active;if(a!=null)synchronized(a.signal){a.signal.notifyAll();}}
    /** R27-005: research computation no longer yields to the realtime-pressure gate for whole minutes. Only user stop,
     * interruption and real resources (memory, thermal) pre-empt a slice; writes yield in millisecond windows. */
    private boolean preempt(){return cancelled||Thread.currentThread().isInterrupted()||resourceBlocked!=null;}
    private final ThermalPolicyR29 thermalR29 = new ThermalPolicyR29();
    private long thermalSampleAtR29; private double thermalHeadroomR29 = Double.NaN;
    private void resources(){
        long mono=SystemClock.elapsedRealtime();if(mono-resourceChecked<1000)return;resourceChecked=mono;String reason=null;
        android.app.ActivityManager manager=(android.app.ActivityManager)store.contextForR26().getSystemService(android.content.Context.ACTIVITY_SERVICE);
        if(manager!=null){android.app.ActivityManager.MemoryInfo memory=new android.app.ActivityManager.MemoryInfo();manager.getMemoryInfo(memory);if(memory.lowMemory)reason="MEMORY_PRESSURE";}
        int thermalStatus=-1;
        if(android.os.Build.VERSION.SDK_INT>=29){
            android.os.PowerManager power=(android.os.PowerManager)store.contextForR26().getSystemService(android.content.Context.POWER_SERVICE);
            if(power!=null){
                thermalStatus=power.getCurrentThermalStatus();
                if(android.os.Build.VERSION.SDK_INT>=30&&mono-thermalSampleAtR29>=10_000){
                    thermalSampleAtR29=mono;
                    try{thermalHeadroomR29=power.getThermalHeadroom(0);}catch(RuntimeException unsupported){thermalHeadroomR29=Double.NaN;}
                }
            }
        }
        thermalR29.update(thermalStatus,thermalHeadroomR29,mono);
        RuntimeR27 runtime=r27;if(runtime!=null)runtime.pool().setThermalLimit(thermalR29.workers,thermalR29.reason);
        if(thermalR29.paused())reason=thermalR29.reason;
        if(Runtime.getRuntime().maxMemory()<(64L<<20))reason="MEMORY_PRESSURE:JVM_HEAP_BELOW_64M_RESEARCH_MINIMUM";
        resourceBlocked=reason;
    }
    private void sessionBlocked(String reason){try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){long now=System.currentTimeMillis();String state=reason==null?"RUNNING":"BLOCKED:"+reason;if(state.equals(recordedSessionState)&&now-heartbeat<1000)return;store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_SESSION_STATUS",db->{db.execSQL("UPDATE r26_session SET state=?,heartbeat=? WHERE session_id=? AND user_enabled=1",new Object[]{state,now,owner});return 0;});heartbeat=now;recordedSessionState=state;}}

    @Override public void close(){cancelled=true;RuntimeR27 runtime=r27;if(runtime!=null)runtime.pool().setResource("USER_STOP");LegacyGenerationR26.cancel(owner);LegacyReplayContinuationR26.cancel(owner);TaskContinuationR26.cancel(owner);wake();worker.interrupt();}
    /** The service must not close a shared connection while a slice is draining. */
    public void closeAndReleaseStore(){closeStore=true;close();if(!worker.isAlive())store.close();}
    public boolean isRunning(){return !cancelled && worker.isAlive();}
    public boolean isCpuLeaseHeld(){RuntimePowerLeaseR26 lease=powerLease;return lease!=null&&lease.isHeld();}
    public long cpuLeaseRenewals(){RuntimePowerLeaseR26 lease=powerLease;return lease==null?0:lease.renewals();}
    public static void requireFence(SQLiteDatabase db,Claim claim){try(Cursor c=db.rawQuery("SELECT 1 FROM r26_task t JOIN r26_session s ON s.session_id=t.owner WHERE t.task_id=? AND t.owner=? AND t.fence=? AND t.status='RUNNING' AND t.lease_until>? AND s.user_enabled=1 AND s.state='RUNNING'",new String[]{claim.task,claim.owner,String.valueOf(claim.fence),String.valueOf(System.currentTimeMillis())})){if(!c.moveToFirst())throw new IllegalStateException("R26_STALE_FENCE_OR_LEASE_OR_SESSION");}}
    private void initialize(){
        ResearchCutoverR29.resumeBeforeRuntime(store);
        try{LegacyTeacherOwnerR26.register(store.contextForR26());}catch(RuntimeException blocked){android.util.Log.w("STARCORE_R26","original teacher blocked",blocked);store.setState("r26_teacher_blocked",blocked.toString());}
        LegacyResearchScopeR29.realtimeWrite(store,"R29_LEGACY_SESSION_OWNER",db->{LegacyCapabilityAdapterR29.initialize(db,owner);LegacyLiveProjectionR29.ensureRealtime(db);LegacyAuthorityProjectionR29.ensureRealtime(db);StrategySchemaDomainR29.ensureRealtime(db);return 0;});
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_RUNTIME_SCHEMA",db->{RuntimeSchemaR26.ensure(db);if(ResearchRouteR29.isResearchDatabase(db))SchemaR27.ensureResearch(db);else SchemaR27.ensure(db);
            // R27.1: tasks quarantined only by the fixed continuation-quantum bug get their normal retry back.
            db.execSQL("UPDATE r26_task SET status='READY',wait_kind='',error_count=0,error_window_start=0,next_at=0 WHERE status='QUARANTINED' AND task_id IN (SELECT task_id FROM r27_task_error WHERE message='R26_CONTINUATION_QUANTUM' OR (message LIKE 'F01_WRITE_FAILED%' AND stack LIKE '%CP3_DB_BUSY_DEADLINE%'))");RuntimeSchemaR26.bindCapabilitySource(db,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256);long now=System.currentTimeMillis();
            // A new process has a new monotonic epoch. Leases remain wall-clock bounded and fenced.
            db.execSQL("UPDATE r26_session SET state='INTERRUPTED',user_enabled=0 WHERE user_enabled=1");
            db.execSQL("UPDATE r26_task SET status='YIELDED',lease_until=0,fence=fence+1,owner=NULL WHERE status='RUNNING'");
            db.execSQL("INSERT OR REPLACE INTO r26_session VALUES(?,1,?,'RUNNING',?,coalesce((SELECT actual_progress FROM r26_session WHERE session_id=?),0))",new Object[]{owner,String.valueOf(now-SystemClock.elapsedRealtime()),now,owner});
            add(db,"schema","P1");add(db,"schema-base","P1");add(db,"kernel","P1");add(db,"shadow-recover","P1");add(db,"evidence-recover","P1");add(db,"evidence-quality","P1");add(db,"sealed-planner","P1");add(db,"calibration","P2");add(db,"su1-certificate","P1");add(db,"su2-mainline","P1");add(db,"su2-certificate","P1");add(db,"qualification","P1");add(db,"aggregate-evidence","P1");add(db,"feedback","P1");add(db,"seed-incumbent","P1");add(db,"incumbent-forecast","P1");
            add(db,"legacy-generation","P2");add(db,"legacy-replay","P2");add(db,"artifact-retention","P3");add(db,"warm-candidates","P2");add(db,"publish-models","P2");add(db,"live-model-training","P2");add(db,"live-forecast-preparation","P2");add(db,"foundry-su2","P2");add(db,"foundry-su1","P2");add(db,"ablation","P2");add(db,"campaign-audit","P2");add(db,"archive","P3");add(db,"retention","P3");add(db,"diagnosis","P3");add(db,"daily-close","P3");
            for(int i=1;i<=24;i++)registerCapability(db,String.format(Locale.ROOT,"F%02d",i),"FAMILY");for(int i=1;i<=16;i++){String id=String.format(Locale.ROOT,"E%02d",i);registerCapability(db,id,"EXPERT");add(db,"expert-"+id,"P2");}
            db.execSQL("UPDATE r26_task SET status='BLOCKED',blocked_reason='WAITING_SELF_CONTAINED_STRATEGY_STORAGE',next_at=0 WHERE task_id<>'schema' AND lane<>'R27'");
            for(String r27Task:new String[]{"r29-event-inbox","r29-research-migration","r27-replay-dataset","r27-evolution","r27-live","r27-atoms","r27-atomnet","r27-abi","r27-calibration","r27-chart-cache","r27-legacy-export","r27-health"})
                db.execSQL("INSERT OR IGNORE INTO r26_task(task_id,lane,mechanism,status,registered_at) VALUES(?,'R27',?,'READY',?)",new Object[]{r27Task,r27Task,now});
            return 0;});
        if(ResearchRouteR29.active(store.coordinatorForResearchR952().realtimeReadOnlyR29()))ResearchRouteR29.write(store,"R29_RESEARCH_SCHEMA_UPGRADE",db->{SchemaR27.ensureResearch(db);return 0;});
        ReplayBurstSchedulerSU1.cancel(store.contextForR26());
    }
    private static void add(SQLiteDatabase db,String id,String lane){db.execSQL("INSERT OR IGNORE INTO r26_task(task_id,lane,mechanism) VALUES(?,?,?)",new Object[]{id,lane,id});}
    private static void registerCapability(SQLiteDatabase db,String id,String kind){
        String implementation=kind.equals("EXPERT")?ExpertWorkshopR26.implementation(id):FamilyRegistryR26.implementation(id);String state=implementation.isEmpty()?"BLOCKED":"EXECUTABLE";
        String reason=kind.equals("EXPERT")&&!implementation.isEmpty()?"Concrete resumable paired experiment or evidence audit; full proposal and expert acceptance pending":implementation.isEmpty()?"Full R26 mechanism integration and decisive experiments pending; no fallback substituted":"Concrete numerical kernel registered; complete family experiments and natural quality pending";
        db.execSQL("INSERT OR REPLACE INTO r26_capability(mechanism,kind,implementation,state,reason,last_progress) VALUES(?,?,?,?,?,coalesce((SELECT last_progress FROM r26_capability WHERE mechanism=?),0))",new Object[]{id,kind,implementation,state,reason,id});
    }
    private Claim claim(){
        // Before the bootstrap commits, P2/P3 have no executable dependency. One original fenced
        // admission avoids three empty Root transactions while retaining error cooldown and leases.
        if(!strategyStorageReady)return claimStrategySchema();
        // R27-008: right after a new draw the incumbent forecast for the next target is prepared first, so the
        // freeze is a small write long before the open time instead of INCUMBENT_MISSING_AFTER_OPEN.
        if(strategyStorageReady){long seq=latestSequence();if(seq>0&&seq!=lastIncumbentSeq){Claim first=claimTask("incumbent-forecast","P1");if(first!=null){lastIncumbentSeq=seq;return first;}}}
        // Dependency-blocked tasks are not ready work. Polling forty of them between migration
        // units made a fresh installation spend minutes without reaching its first model sample.
        final String dependency=strategyStorageReady?"":" AND task_id='schema'";
        for(int attempt=0;attempt<SLOTS.length;attempt++){
            String lane=attempt==0&&service.needsP2()?"P2":SLOTS[slot++%SLOTS.length];
            Claim result=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_TASK_CLAIM",db->{
                LegacyCapabilityAdapterR29.requireOwner(LegacyResearchScopeR29.realtime(db),owner,poolDispatcher);
                long now=System.currentTimeMillis();String id=null;long fence=0,cursor=0;
                int liveModels=1;if(lane.equals("P2")&&strategyStorageReady)try(Cursor count=db.rawQuery("SELECT count(*) FROM r26_live_model WHERE status='ACTIVE'",null)){if(count.moveToFirst())liveModels=Math.max(1,count.getInt(0));}
                Set<String> unserved=new HashSet<>();if(lane.equals("P2")&&strategyStorageReady)try(Cursor capabilities=db.rawQuery("SELECT mechanism FROM r26_capability WHERE last_progress=0 AND implementation<>''",null)){while(capabilities.moveToNext())unserved.add(capabilities.getString(0));}
                int bestPriority=Integer.MAX_VALUE;double best=Double.POSITIVE_INFINITY;
                try(Cursor c=db.rawQuery("SELECT task_id,fence,cursor FROM r26_task WHERE lane=? AND status IN ('READY','YIELDED','BLOCKED','RUNNING') AND lease_until<=? AND next_at<=?"+dependency+" ORDER BY last_served,task_id"+(lane.equals("P2")?"":" LIMIT 1"),new String[]{lane,String.valueOf(now),String.valueOf(now)})){
                    while(c.moveToNext()){double score=lane.equals("P2")?service.score(c.getString(0),CapabilityServiceR26.weight(c.getString(0),FamilyRegistryR26.EXECUTABLE.size(),liveModels)):0;int priority=lane.equals("P2")&&CapabilityServiceR26.firstService(c.getString(0),unserved)?0:1;if(priority<bestPriority||priority==bestPriority&&score<best){id=c.getString(0);fence=c.getLong(1)+1;cursor=c.getLong(2);best=score;bestPriority=priority;}}
                }
                if(id==null)return null;
                db.execSQL("UPDATE r26_task SET status='RUNNING',owner=?,fence=?,lease_until=?,last_served=?,blocked_reason=NULL WHERE task_id=?",new Object[]{owner,fence,now+30000,now,id});return new Claim(id,owner,fence,cursor,lane);
            });if(result!=null)return result;
        }return null;
    }
    private Claim claimStrategySchema(){
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_TASK_CLAIM",db->{
            LegacyCapabilityAdapterR29.requireOwner(LegacyResearchScopeR29.realtime(db),owner,poolDispatcher);
            long now=System.currentTimeMillis();
            try(Cursor c=db.rawQuery("SELECT fence,cursor FROM r26_task WHERE task_id='schema' AND lane='P1' AND status IN ('READY','YIELDED','BLOCKED','RUNNING') AND lease_until<=? AND next_at<=?",new String[]{String.valueOf(now),String.valueOf(now)})){
                if(!c.moveToFirst())return null;long fence=c.getLong(0)+1,cursor=c.getLong(1);
                db.execSQL("UPDATE r26_task SET status='RUNNING',owner=?,fence=?,lease_until=?,last_served=?,blocked_reason=NULL WHERE task_id='schema'",new Object[]{owner,fence,now+30000,now});return new Claim("schema",owner,fence,cursor,"P1");
            }
        });
    }
    private long latestSequence(){try{Models.Draw latest=store.getLatestDraw();return latest==null||!TargetIssueResolver.isValid(latest.issue)?-1:TargetIssueResolver.sequenceOrdinalSU1(latest.issue);}catch(RuntimeException e){return -1;}}
    private Claim claimTask(String task,String lane){
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R27_PRIORITY_CLAIM",db->{LegacyCapabilityAdapterR29.requireOwner(LegacyResearchScopeR29.realtime(db),owner,poolDispatcher);long now=System.currentTimeMillis();
            try(Cursor c=db.rawQuery("SELECT fence,cursor FROM r26_task WHERE task_id=? AND status IN ('READY','YIELDED','BLOCKED') AND lease_until<=?",new String[]{task,String.valueOf(now)})){
                if(!c.moveToFirst())return null;long fence=c.getLong(0)+1,cursor=c.getLong(1);
                db.execSQL("UPDATE r26_task SET status='RUNNING',owner=?,fence=?,lease_until=?,last_served=?,blocked_reason=NULL WHERE task_id=?",new Object[]{owner,fence,now+30000,now,task});return new Claim(task,owner,fence,cursor,lane);}});
    }
    public RuntimeR27 r27(){return r27;}
    private void run(){
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        com.ethk3.novaonepro.StallStacksR27.start();
        try{
            ServiceHeartbeatStore user=new ServiceHeartbeatStore(store.contextForR26());if(!user.isUserEnabled()){cancelled=true;return;}
            powerLease=new RuntimePowerLeaseR26(store.contextForR26(),owner);powerLease.keepAlive();
            boolean initialized=false;
            while(!cancelled&&!initialized){if(!user.isUserEnabled()){cancelled=true;break;}powerLease.keepAlive();try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){initialize();initialized=true;}catch(RuntimeException retry){android.util.Log.w("STARCORE_R26","initialization retry",retry);await(500);}}
            // R27-003/009: DbStore → P0 (service) → research scheduler. The R27 pool owns the R27 tasks.
            if(!cancelled){RuntimeR27 runtime=new RuntimeR27(store,owner,this);r27=runtime;runtime.start();}
            while(!cancelled){
            if(!user.isUserEnabled()){cancelled=true;break;}powerLease.keepAlive();
            resources();
            RuntimeR27 pool=r27;if(pool!=null)pool.pool().setResource(resourceBlocked);
            if(preempt()){try{sessionBlocked(resourceBlocked==null?"STOPPING":resourceBlocked);}catch(RuntimeException busy){android.util.Log.w("STARCORE_R26","blocked receipt retry",busy);}await(250);continue;}
            try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){sessionBlocked(null);}catch(RuntimeException busy){android.util.Log.w("STARCORE_R26","session status retry",busy);}
            if(ResearchRouteR29.active(store.coordinatorForResearchR952().realtimeReadOnlyR29())){
                // This thread owns no in-flight claim here. Publish the owner before enabling the pool task.
                if(!poolDispatcher)try{LegacyCapabilityAdapterR29.publish(store,owner);poolDispatcher=true;pool.pool().wake("r29-legacy-capabilities");}
                catch(RuntimeException retry){android.util.Log.w("STARCORE_R26","pool dispatch handoff retry",retry);}
                await(250);continue;
            }
            try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){
            Claim claim;
            try{claim=claim();}catch(RuntimeException busy){android.util.Log.w("STARCORE_R26","dispatch retry",busy);await(250);continue;}
            if(claim==null){await(250);continue;}
            long delta=executeClaim(claim);
            // Empty maintenance work waits for its next data event, never spins to fabricate progress.
            if(delta==0)await(10);
            }
        }}catch(Exception failure){android.util.Log.e("STARCORE_R26","runtime blocked",failure);try{store.setState("r26_runtime_error",failure.toString());}catch(Exception ignored){}}
        finally{try{RuntimeR27 runtime=r27;r27=null;if(runtime!=null)runtime.close(); // cancel token → join ≤10 s → close read-only connections
            LegacyGenerationR26.cancel(owner);LegacyReplayContinuationR26.cancel(owner);TaskContinuationR26.cancel(owner);Thread.interrupted();try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_SESSION_STOP",db->{if(RuntimeSchemaR26.ready(db)){db.execSQL("UPDATE r26_session SET state=?,user_enabled=0,heartbeat=? WHERE session_id=?",new Object[]{cancelled?"STOPPED":"FAILED",System.currentTimeMillis(),owner});db.execSQL("UPDATE r26_task SET status='YIELDED',lease_until=0 WHERE owner=? AND status='RUNNING'",new Object[]{owner});}return 0;});}catch(Exception ignored){}while(!CooperativeContinuationR26.awaitOwnerStopped(owner,10000))android.util.Log.e("STARCORE_R26","waiting for cancelled numerical workers before releasing shared database");if(active==this)active=null;if(closeStore)store.close();}finally{RuntimePowerLeaseR26 lease=powerLease;if(lease!=null)lease.close();}}
    }
    boolean poolDispatchReady(){return poolDispatcher&&!preempt();}
    ResearchPoolR27.Outcome runPoolLegacySlice()throws Exception{
        try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){
        if(!poolDispatchReady())return ResearchPoolR27.Outcome.idle("WAITING_LEGACY_OWNER_OR_RESOURCE",0,-1);
        if(!legacySlice.tryLock())return ResearchPoolR27.Outcome.idle("COOPERATIVE_LEGACY_SLICE_ALREADY_RUNNING",0,-1);
        try{
            if(!new ServiceHeartbeatStore(store.contextForR26()).isUserEnabled()||!poolDispatchReady())return ResearchPoolR27.Outcome.idle("USER_STOP_OR_RESOURCE",0,-1);
            Claim claim=claim();if(claim==null)return ResearchPoolR27.Outcome.idle("NO_READY_LEGACY_CAPABILITY",0,latestSequence());
            long units=executeClaim(claim);
            return units>0?ResearchPoolR27.Outcome.progress(units,claim.completedEventCursor>=0?claim.completedEventCursor:claim.cursor+units):ResearchPoolR27.Outcome.idle(claim.reason==null?"LEGACY_WAIT_FOR_COMMITTED_INPUT":claim.reason,claim.cursor,latestSequence());
        }finally{legacySlice.unlock();}
        }
    }
    private long executeClaim(Claim claim){
        long start=SystemClock.elapsedRealtime(),cpu=SystemClock.currentThreadTimeMillis();long delta=0;String reason=null;Throwable failed=null;
            try{WRITING_CLAIM.set(claim);BudgetTokenSU1 budget=new BudgetTokenSU1((int)CooperativeContinuationR26.SLICE_MS,3000000,this::preempt);ExecutionBudgetR26.enter(budget);delta=execute(claim,budget);reason=claim.reason;}
            catch(BudgetTokenSU1.Yield yield){reason=resourceBlocked!=null?resourceBlocked:"SLICE_BUDGET";}
            catch(Exception failure){if(ResearchWriteQueueR27.writerBusy(failure))reason="COOPERATIVE_WRITER_BUSY"; // R27.2: a wait, not a task error
                else{failed=failure;android.util.Log.w("STARCORE_R26",claim.task,failure);}}
            finally{ExecutionBudgetR26.leave();WRITING_CLAIM.remove();}
            // R27-006: a real wait kind instead of the blanket DATA_NOT_READY.
            final WaitKindR27.Wait wait=WaitKindR27.classify(delta,reason,failed,claim.cursor,latestSequence(),resourceBlocked,false);
            final String waitStack=failed==null?null:ResearchPoolR27.stack(failed);final Throwable sliceFailure=failed;
            if(failed!=null)reason=failed.getClass().getSimpleName()+":"+String.valueOf(failed.getMessage());else if(reason==null&&delta==0)reason=wait.blockedReason();
            final long work=delta,nextCursor=claim.completedEventCursor>=0?claim.completedEventCursor:claim.cursor+delta,elapsed=SystemClock.elapsedRealtime()-start,cpuMs=Math.max(0,SystemClock.currentThreadTimeMillis()-cpu)+claim.workerCpuMillis;final String blocked=elapsed>2*CooperativeContinuationR26.SLICE_MS?"CAPACITY_FAIL:UNSLICED_KERNEL:"+elapsed+"ms|"+reason:reason;
            if(elapsed>2*CooperativeContinuationR26.SLICE_MS)android.util.Log.w("STARCORE_R26","task="+claim.task+"|wallMs="+elapsed+"|cpuMs="+cpuMs+"|"+blocked);
            service.served(claim.task,claim.lane,cpuMs);
            try{store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_TASK_RECEIPT",db->{requireFence(db,claim);long now=System.currentTimeMillis();db.execSQL("UPDATE r26_task SET cursor=?,status=?,lease_until=0,next_at=?,blocked_reason=?,last_progress=CASE WHEN ?>0 THEN ? ELSE last_progress END,steps=steps+CASE WHEN ?>0 THEN 1 ELSE 0 END,last_cpu_ms=? WHERE task_id=? AND fence=?",new Object[]{nextCursor,blocked==null||blocked.startsWith("COOPERATIVE")?"YIELDED":"BLOCKED",now+(work>0||"CRITICAL_DEADLINE".equals(blocked)||blocked!=null&&blocked.startsWith("COOPERATIVE")?0:1000),blocked,work,now,work,cpuMs,claim.task,claim.fence});
                db.execSQL("INSERT INTO r26_progress(task_id,fence,cursor_before,cursor_after,cpu_ms,blocked_reason,recorded_at) VALUES(?,?,?,?,?,?,?)",new Object[]{claim.task,claim.fence,claim.cursor,nextCursor,cpuMs,blocked,now});
                if(SchemaR27.ready(db)){
                    db.execSQL("UPDATE r26_task SET wait_kind=?,wait_detail=?,wait_since=CASE WHEN wait_kind=? THEN wait_since ELSE ? END,first_progress_at=CASE WHEN first_progress_at=0 AND ?>0 THEN ? ELSE first_progress_at END WHERE task_id=?",new Object[]{wait.kind,wait.detail,wait.kind,now,work,now,claim.task});
                    if(sliceFailure!=null){long ws=0,cnt=0;try(Cursor e=db.rawQuery("SELECT error_window_start,error_count FROM r26_task WHERE task_id=?",new String[]{claim.task})){if(e.moveToFirst()){ws=e.getLong(0);cnt=e.getLong(1);}}
                        long[] err=WaitKindR27.recordError(ws,cnt,now);
                        LegacyResearchTransferR29.taskError(db,owner+":"+claim.task+":"+claim.fence,claim.task,sliceFailure.getClass().getName(),String.valueOf(sliceFailure.getMessage()),waitStack,now);
                        db.execSQL("UPDATE r26_task SET error_window_start=?,error_count=?,status=CASE WHEN ? THEN 'QUARANTINED' ELSE status END,wait_kind=CASE WHEN ? THEN 'QUARANTINED' ELSE wait_kind END WHERE task_id=?",new Object[]{err[0],err[1],WaitKindR27.quarantine(err[1])?1:0,WaitKindR27.quarantine(err[1])?1:0,claim.task});}
                }db.execSQL("DELETE FROM r26_progress WHERE receipt_id<(SELECT max(receipt_id)-4096 FROM r26_progress)");
                ServiceTotalsR26.record(db,owner,cpuMs,elapsed,elapsed>2*CooperativeContinuationR26.SLICE_MS);
                db.execSQL("UPDATE r26_session SET heartbeat=?,actual_progress=actual_progress+? WHERE session_id=?",new Object[]{now,work,owner});return 0;});}catch(RuntimeException failure){android.util.Log.w("STARCORE_R26","receipt not committed",failure);}
        claim.reason(reason);return delta;
    }
    private void await(long ms){synchronized(signal){try{if(!cancelled)signal.wait(ms);}catch(InterruptedException e){if(!cancelled)Thread.currentThread().interrupt();}}}
    static final long SCHEMA_BATCH_MS=800;
    private static final long SCHEMA_RESERVE_MS=50,SCHEMA_WRITER_WAIT_MS=250;
    private long execute(Claim c,BudgetTokenSU1 budget)throws Exception{
        if(c.task.equals("schema")){
            if(strategyStorageReady)return 0;
            if(!StrategySchemaSU1.ready(ExpertWorkshopR26.snapshotReader(store))){
                // Keep the original back-to-back unit loop, but admit a new Root unit only while
                // a full fair writer attempt and a 50ms computation reserve fit this service budget.
                long until=SystemClock.elapsedRealtime()+SCHEMA_BATCH_MS;int units=0;
                ResearchSchemaCoordinatorR926.StepResult result=null;
                while(!preempt()){
                    long remaining=until-SystemClock.elapsedRealtime();
                    if(remaining<=SCHEMA_RESERVE_MS||!budget.tryConsume(SCHEMA_RESERVE_MS*1000))break;
                    long writerWait=Math.min(SCHEMA_WRITER_WAIT_MS,remaining-SCHEMA_RESERVE_MS);
                    try(DatabaseCoordinator.WriterDeadlineScopeCP3 deadline=DatabaseCoordinator.withWriterDeadlineCP3(TimeSourceR811.monoNowMs()+writerWait)){
                        result=ResearchSchemaCoordinatorR926.ensureStrategyStorageOneStep(store.coordinatorForResearchR952());
                    }
                    // Pending intentionally has no failure cause. Do not consume another writer window
                    // inside this slice; its real committed prefix survives the fair re-queue.
                    if(result.pending||"WRITER_BUSY".equals(result.status))break;
                    if(result.cause!=null)throw new IllegalStateException("R26_STRATEGY_SCHEMA",result.cause);
                    units+=result.committedUnits;
                    if(result.ready||result.committedUnits==0)break;
                }
                c.reason("COOPERATIVE_"+(result==null?"STRATEGY_BUDGET_YIELD":result.status)+":units="+units+(result==null?"":":"+result.detail));return units;
            }
            if(!StrategySchemaSU2.ready(ExpertWorkshopR26.snapshotReader(store))){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_SU2_SCHEMA",db->{requireFence(db,c);StrategySchemaSU2.ensure(db);IncumbentLoopSU2.seedWithin(db,1);return 0;});return 1;}
            ProgramModelSU2.register();store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_STRATEGY_DEPENDENCIES_READY",db->{requireFence(db,c);db.execSQL("UPDATE r26_task SET status='READY',next_at=0,blocked_reason=NULL WHERE blocked_reason='WAITING_SELF_CONTAINED_STRATEGY_STORAGE'");return 0;});strategyStorageReady=true;RuntimeR27 runtime=r27;if(runtime!=null)runtime.pool().wake("r29-research-migration");return 1;
        }
        if(c.task.equals("schema-base")){
            if(!strategyStorageReady){c.reason("WAITING_SELF_CONTAINED_STRATEGY_STORAGE");return 0;}
            if(store.coordinatorForResearchR952().isR811ResearchSchemaReady())return 0;
            ResearchSchemaCoordinatorR926.StepResult result=ResearchSchemaCoordinatorR926.ensureOneStepResult(store.coordinatorForResearchR952());c.reason("LEGACY_SCHEMA:"+result.status+":"+ResearchSchemaCoordinatorR926.summary()+":"+result.detail);return result.ready?1:result.committedUnits;
        }
        if(!strategyStorageReady){c.reason("WAITING_SELF_CONTAINED_STRATEGY_STORAGE");return 0;}
        ProgramModelSU2.register();Models.Draw latest=store.getLatestDraw();if(latest==null||!TargetIssueResolver.isValid(latest.issue))return 0;
        long seq=TargetIssueResolver.sequenceOrdinalSU1(latest.issue);String target=TargetIssueResolver.next(latest.issue).targetIssue;
        if(c.task.startsWith("expert-"))return TaskContinuationR26.step(store,c,budget,seq+1);
        switch(c.task){
            case "legacy-generation":return LegacyGenerationR26.step(store,c,budget);
            case "legacy-replay":return LegacyReplayContinuationR26.step(store,c,budget);
            case "artifact-retention":return ArtifactMaintenanceR26.step(store,c,budget);
            case "seed-incumbent":IncumbentLoopSU2.seed(store,seq+1);c.completedEventCursor=seq;return 1;
            case "qualification":return CertificateSU2.maintainOne(store,seq+1);
            case "aggregate-evidence":return EvidenceAccumulatorR26.stepOne(store,c,budget,seq+1);
            case "incumbent-forecast":{if(c.cursor>=seq)return 0;long done=TaskContinuationR26.step(store,c,budget,seq+1);if(done>0)c.advanceCursorTo(seq);return done;}
            case "warm-candidates":return TaskContinuationR26.step(store,c,budget,seq+1);
            case "publish-models":return TaskContinuationR26.step(store,c,budget,seq+1);
            case "live-forecast-preparation":return TaskContinuationR26.step(store,c,budget,seq+1);
            case "live-model-training":return TaskContinuationR26.step(store,c,budget,seq+1);
            case "foundry-su2":return TaskContinuationR26.step(store,c,budget,seq+1);
            case "feedback":return ResearchFeedbackSU1.drainOne(store)?1:0;
            case "ablation":return ModelAblationStoreSU1.advanceOne(store,budget)?1:0;
            case "campaign-audit":return CampaignAuditStoreSU1.advanceOne(store,budget)?1:0;
            case "foundry-su1":return TaskContinuationR26.step(store,c,budget,seq+1);
            default:break;
        }
        if(!c.task.equals("retention")&&!store.coordinatorForResearchR952().isR811ResearchSchemaReady()){c.reason("WAITING_LEGACY_RESEARCH_SCHEMA:"+ResearchSchemaCoordinatorR926.summary());return 0;}
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
            case "su2-mainline":MainlineBridgeSU2.chooseIfDue(store,seq+1);break;
            case "su2-certificate":CertificateSU2.maintain(store,seq+1);break;
            case "archive":ArchiveStoreSU1.maintain(store);break;
            case "diagnosis":MatrixStoreSU2.maintain(store,target);DiagnosisStoreSU2.maintain(store,target);break;
            case "retention":{
                long now=System.currentTimeMillis();long retired=LegacyDecisionRetentionR29.step(store,seq+1,now);
                long metadata=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_RETENTION",db->{requireFence(db,c);RetentionRulesSU2.pruneResearchGenomes(db);return RetentionR26.prune(db,seq+1);});
                return retired+metadata+LegacyDecisionRetentionR29.pruneAuthorities(store,seq+1,now);
            }
            case "daily-close":store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_DAILY_CLOSE",db->{requireFence(db,c);LiveShadowLoopSU1.closeDays(db,Integer.parseInt(latest.issue.substring(0,8)));return 0;});break;
            default:throw new IllegalStateException("R26_UNKNOWN_TASK:"+c.task);
        }
        c.completedEventCursor=seq;return 1;
    }
}
