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
    private final Thread worker;private int slot;private volatile RuntimeR27 r27;private long lastIncumbentSeq=-1;private long heartbeat;private String recordedSessionState;private boolean strategyStorageReady;private volatile boolean retentionProtectionReadyR31;private volatile String resourceBlocked;private volatile long resourceChecked;
    private final CapabilityServiceR26 service=new CapabilityServiceR26();
    private volatile Claim executingClaimR31;
    private volatile RuntimePowerLeaseR26 powerLease;
    private final MemoryBudgetR31 memoryBudgetR31=MemoryBudgetR31.processBudget();
    private volatile java.util.Map<String,Object> memoryObservationR31=java.util.Collections.emptyMap();
    private volatile long memoryPressureSinceR31;private long lastOwnerHealthRequestR31,nextMemoryTrimR31;
    private static final String[] SLOTS={"P1","P2","P2","P3"};
    public static final class Claim {
        public final String task,owner,lane;public final long fence,cursor;private long completedEventCursor=-1,workerCpuMillis;private String reason;
        private long commitChunkR31;
        String artifactAtClaimR31;
        public synchronized long nextCommitChunkR31(){return commitChunkR31++;}
        public void inheritCommitChunksR31(Claim previous){if(previous==null||!task.equals(previous.task)||!owner.equals(previous.owner))return;long value;synchronized(previous){value=previous.commitChunkR31;}synchronized(this){commitChunkR31=Math.max(commitChunkR31,value);}}
        public void advanceCursorTo(long value){if(value<cursor)throw new IllegalArgumentException("R26_TASK_CURSOR_BACKWARDS");completedEventCursor=value;}
        public void addWorkerCpuMillis(long value){workerCpuMillis+=Math.max(0,value);}
        public void reason(String reason){this.reason=reason;}
        Claim(String task,String owner,long fence,long cursor){this(task,owner,fence,cursor,null);}
        Claim(String task,String owner,long fence,long cursor,String lane){this.task=task;this.owner=owner;this.fence=fence;this.cursor=cursor;this.lane=lane;}
    }
    public RuntimeCoordinatorR26(DbStore store){this.store=Objects.requireNonNull(store);ResearchPoolR27.installComputeBudgetR31();worker=new Thread(this::run,"starcore-r26-continuous-research");worker.setDaemon(true);synchronized(RuntimeCoordinatorR26.class){RuntimeCoordinatorR26 previous=active;if(previous!=null&&!previous.resourcesTerminatedR31()){previous.close();throw new IllegalStateException("R31_PREVIOUS_OWNER_DRAINING");}active=this;}worker.start();}
    static void bindBackgroundClaim(Claim claim){WRITING_CLAIM.set(claim);}
    static void unbindBackgroundClaimR31(){WRITING_CLAIM.remove();}
    static Claim currentClaimR30(){return WRITING_CLAIM.get();}
    public static void recordOwnedArtifactR30(SQLiteDatabase db,String operation){Claim claim=WRITING_CLAIM.get();if(claim!=null&&!claim.task.startsWith("r27-"))ResearchStoreR30R1.afterOwnedArtifact(db,operation,claim);}
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
        Boolean systemLowMemory=null;
        if(manager!=null){android.app.ActivityManager.MemoryInfo memory=new android.app.ActivityManager.MemoryInfo();manager.getMemoryInfo(memory);systemLowMemory=memory.lowMemory;if(memory.lowMemory)reason="MEMORY_PRESSURE";}
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
        if(systemLowMemory!=null)memoryBudgetR31.observeSystemLowMemory(systemLowMemory);
        java.util.Map<String,Object> memory=memoryBudgetR31.sample();memoryObservationR31=memory;
        String pressure=String.valueOf(memory.get("pressure"));if(!"NORMAL".equals(pressure)){if(memoryPressureSinceR31==0)memoryPressureSinceR31=mono;}else memoryPressureSinceR31=0;
        if(!"NORMAL".equals(pressure)&&mono>=nextMemoryTrimR31){FeatureCacheR27.trimRegisteredCachesR31();nextMemoryTrimR31=mono+30_000;}
        int memoryWorkers=memory.get("recommendedParallelism") instanceof Number?((Number)memory.get("recommendedParallelism")).intValue():1;
        RuntimeR27 runtime=r27;if(runtime!=null){runtime.pool().setThermalLimit(Math.min(thermalR29.workers,memoryWorkers),thermalR29.reason+"|MEMORY:"+pressure);runtime.pool().setMemoryPressureR31(!"NORMAL".equals(pressure));}
        if("EMERGENCY".equals(pressure))reason="MEMORY_EMERGENCY:"+memory.get("reason")+";resume="+memory.get("resumeCondition");
        if(memory.get("sampleFailure") instanceof String&&!((String)memory.get("sampleFailure")).isEmpty())reason="MEMORY_MEASUREMENT_UNAVAILABLE";
        if(thermalR29.paused())reason=thermalR29.reason;
        if(Runtime.getRuntime().maxMemory()<(64L<<20))reason="MEMORY_PRESSURE:JVM_HEAP_BELOW_64M_RESEARCH_MINIMUM";
        resourceBlocked=reason;
    }
    private void sessionBlocked(String reason){long now=System.currentTimeMillis();String state=reason==null?"RUNNING":"BLOCKED:"+reason;if(state.equals(recordedSessionState)&&now-heartbeat<1000)return;store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(controlContextR31("R26_SESSION_STATUS"),db->{db.execSQL("UPDATE r26_session SET state=?,heartbeat=? WHERE session_id=? AND user_enabled=1",new Object[]{state,now,owner});return 0;});heartbeat=now;recordedSessionState=state;}

    @Override public void close(){synchronized(signal){cancelled=true;}if(healthControlExecutorR31().cancelQueued("health:"+owner))healthProbePendingR31.set(false);RuntimeR27 runtime=r27;if(runtime!=null)runtime.pool().setResource("USER_STOP");LegacyGenerationR26.cancel(owner);LegacyReplayContinuationR26.cancel(owner);TaskContinuationR26.cancel(owner);wake();worker.interrupt();}
    /** The service must not close a shared connection while a slice is draining. */
    public void closeAndReleaseStore(){closeStore=true;close();if(resourcesTerminatedR31())store.close();}
    public boolean isRunning(){return !cancelled && worker.isAlive();}
    public static java.util.Map<String,Object> activeHealthObservationR31(){RuntimeCoordinatorR26 runtime=active;if(runtime!=null){java.util.Map<String,Object> out=new java.util.LinkedHashMap<>(runtime.healthSnapshotR31().asMap());out.put("scope","PER_PID");return java.util.Collections.unmodifiableMap(out);}java.util.Map<String,Object> out=new java.util.LinkedHashMap<>();out.put("scope","PER_PID");out.put("status","UNKNOWN");out.put("reason","MAIN_RUNTIME_OWNER_NOT_IN_THIS_PID");out.put("processPid",android.os.Process.myPid());out.put("researchHealth","UNKNOWN");return java.util.Collections.unmodifiableMap(out);}
    /** Cached immutable observation; a service/UI caller never performs a database query here. */
    public static final class RuntimeHealthSnapshot {
        public final String snapshotId,ownerEpoch,poolGeneration,bootId,processLiveness,researchHealth,lastArtifactHash,reason;
        public final long sampleWallMs,sampleElapsedMs;
        /** pending counts tasks with known pending input; heterogeneous work units are never added together. */
        public final Long latestSourceOrdinal,lastArtifactOrdinal,pending;
        public final int quarantined,needsFix,stalled,unknownTaskCount,knownPendingTaskCount;
        public final boolean pendingKnown;
        public final java.util.List<java.util.Map<String,Object>> tasksImmutable;
        private final java.util.Map<String,Object> data;
        RuntimeHealthSnapshot(String owner,String boot,long wall,long elapsed,String process,String health,String why,
                              Long latest,Long artifactOrdinal,String artifactHash,int quarantine,int fixes,int stalled,int unknown,
                              int knownPending,java.util.List<java.util.Map<String,Object>> tasks){
            this(owner,boot,wall,elapsed,process,health,why,latest,artifactOrdinal,artifactHash,quarantine,fixes,stalled,unknown,knownPending,tasks,java.util.Collections.emptyMap());
        }
        private RuntimeHealthSnapshot(String owner,String boot,long wall,long elapsed,String process,String health,String why,Long latest,Long artifactOrdinal,String artifactHash,int quarantine,int fixes,int stalled,int unknown,int knownPending,java.util.List<java.util.Map<String,Object>> tasks,java.util.Map<String,Object> control){
            ownerEpoch=owner;poolGeneration=owner;bootId=boot;sampleWallMs=wall;sampleElapsedMs=elapsed;processLiveness=process;researchHealth=health;reason=why;
            latestSourceOrdinal=latest;lastArtifactOrdinal=artifactOrdinal;lastArtifactHash=artifactHash;quarantined=quarantine;needsFix=fixes;this.stalled=stalled;unknownTaskCount=unknown;knownPendingTaskCount=knownPending;
            pendingKnown=unknown==0;pending=pendingKnown?(long)knownPending:null;
            java.util.List<java.util.Map<String,Object>> immutable=new java.util.ArrayList<>();for(java.util.Map<String,Object> task:tasks)immutable.add(java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(task)));tasksImmutable=java.util.Collections.unmodifiableList(immutable);
            java.util.Map<String,Object> values=new java.util.LinkedHashMap<>();values.put("version","R31_RUNTIME_HEALTH_1");values.put("ownerEpoch",ownerEpoch);values.put("poolGeneration",poolGeneration);values.put("bootId",bootId);values.put("sampleWallMs",sampleWallMs);values.put("sampleElapsedMs",sampleElapsedMs);values.put("processLiveness",processLiveness);values.put("researchHealth",researchHealth);values.put("reason",reason);values.put("latestSourceOrdinal",latestSourceOrdinal);values.put("lastArtifactOrdinal",lastArtifactOrdinal);values.put("lastArtifactHash",lastArtifactHash);values.put("pending",pending);values.put("pendingUnit","TASKS_WITH_PENDING_INPUT_NOT_SUM_OF_WORK_UNITS");values.put("pendingKnown",pendingKnown);values.put("knownPendingTaskCount",knownPendingTaskCount);values.put("quarantined",quarantined);values.put("needsFix",needsFix);values.put("stalled",stalled);values.put("unknownTaskCount",unknownTaskCount);values.put("tasks",tasksImmutable);values.put("scope","One bounded read snapshot; owner heartbeat/attempts are not artifact progress or model quality.");
            values.put("control",freezeHealthValueR31(control));snapshotId=StatsR27.sha256(JsonR27.write(values));values.put("snapshotId",snapshotId);data=java.util.Collections.unmodifiableMap(values);
        }
        public java.util.Map<String,Object> asMap(){return data;}
        RuntimeHealthSnapshot withControl(java.util.Map<String,Object> control){return new RuntimeHealthSnapshot(ownerEpoch,bootId,sampleWallMs,sampleElapsedMs,processLiveness,researchHealth,reason,latestSourceOrdinal,lastArtifactOrdinal,lastArtifactHash,quarantined,needsFix,stalled,unknownTaskCount,knownPendingTaskCount,tasksImmutable,control);}
    }
    private static Object freezeHealthValueR31(Object value){
        if(value instanceof java.util.Map){java.util.Map<String,Object> copy=new java.util.LinkedHashMap<>();for(java.util.Map.Entry<?,?> entry:((java.util.Map<?,?>)value).entrySet())copy.put(String.valueOf(entry.getKey()),freezeHealthValueR31(entry.getValue()));return java.util.Collections.unmodifiableMap(copy);}
        if(value instanceof java.util.List){java.util.List<Object> copy=new java.util.ArrayList<>();for(Object item:(java.util.List<?>)value)copy.add(freezeHealthValueR31(item));return java.util.Collections.unmodifiableList(copy);}return value;
    }
    public static final String HEALTH_QUERY_R31="SELECT substr(t.task_id,1,128),substr(t.status,1,32),substr(t.owner,1,128),t.fence,substr(t.blocked_reason,1,256),substr(t.wait_kind,1,64),substr(p.owner_version,1,64),substr(p.work_unit,1,32),p.work_cursor,p.known_latest,p.pending,p.last_commit_clock_ordinal,p.completed_input_ordinal,substr(p.artifact_identity,1,128),p.updated_at,substr(p.session_id,1,128),p.fence,c.level,substr(c.fingerprint,1,128),substr(c.half_open_owner,1,128),substr(l.boot_id,1,64),l.deadline_elapsed_ms,(SELECT substr(issue,1,20) FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1),length(t.task_id)>128 FROM r26_task t LEFT JOIN r30r1_task_progress p ON p.task_id=t.task_id LEFT JOIN r31_task_control c ON c.task_id=t.task_id LEFT JOIN r31_task_lease l ON l.task_id=t.task_id AND l.owner_epoch IS t.owner AND l.fence=t.fence ORDER BY t.task_id LIMIT 129";
    public static final long HEALTH_WORKING_ESTIMATE_R31=128L*(4096L+1400L*4L);
    /** Exposed for the same production query to be exercised by native-SQLite host and Android fixtures. */
    public static RuntimeHealthSnapshot captureHealthSnapshotR31(SQLiteDatabase db,String owner,String boot,long wall,long elapsed,String process,String resource){
        java.util.List<java.util.Map<String,Object>> tasks=new java.util.ArrayList<>();java.util.Set<String> seen=new java.util.HashSet<>();
        Long latest=null,lastArtifactOrdinal=null;String lastArtifactHash=null,failure=null;long latestArtifactTime=-1;int quarantined=0,fixes=0,stalled=0,unknown=0,pendingTasks=0,active=0,dependent=0;
        try(Cursor rows=db.rawQuery(HEALTH_QUERY_R31,null)){
            while(rows.moveToNext()){
                if(tasks.size()==128){failure="TASK_CATALOG_EXCEEDS_128_ROW_BOUND";unknown++;break;}
                String id=rows.getString(0),status=rows.getString(1),blocked=rows.getString(4),waitKind=rows.getString(5);seen.add(id);
                if(latest==null&&!rows.isNull(22)&&TargetIssueResolver.isValid(rows.getString(22)))latest=TargetIssueResolver.sequenceOrdinalSU1(rows.getString(22));
                ResearchTaskRegistryR30R1.Entry entry=rows.getInt(23)==0?ResearchTaskRegistryR30R1.all().get(id):null;String sourceFailure=null;Long workPending=null,clock=null,committedInput=null;
                boolean current=entry!=null&&ResearchTaskRegistryR30R1.VERSION.equals(rows.getString(6))&&entry.unit.name().equals(rows.getString(7))&&!rows.isNull(13)&&!rows.getString(13).isEmpty();
                if(current){
                    if(!rows.isNull(10)&&rows.getLong(10)>=0)workPending=rows.getLong(10);
                    if(!rows.isNull(11)&&rows.getLong(11)>=0)clock=rows.getLong(11);
                    if(!rows.isNull(12)&&rows.getLong(12)>=0)committedInput=rows.getLong(12);
                    if(clock!=null&&latest!=null&&clock>latest){current=false;workPending=null;sourceFailure="COMMIT_CLOCK_AHEAD_OF_SOURCE";}
                    if(workPending!=null&&latest!=null){
                        if((id.equals("r27-live")||id.equals("legacy-replay")||id.equals("live-model-training"))&&committedInput!=null&&latest>=committedInput)workPending=latest-committedInput;
                        else if(id.equals("r27-replay-dataset")&&!rows.isNull(8)&&latest>=rows.getLong(8))workPending=Math.max(0,latest-rows.getLong(8)-RuntimeR27.REFRESH_NEW_ROWS+1);
                        else if(workPending==0&&entry.unit!=TaskProgressR30R1.Unit.TIME_TICK&&(clock==null||latest>clock)){workPending=null;sourceFailure="PENDING_ZERO_IS_FROM_OLDER_SOURCE_WATERMARK";}
                    }
                    if(workPending==null&&sourceFailure==null)sourceFailure="OWNER_PENDING_NOT_MATERIALIZED";
                }else sourceFailure=entry==null?"UNREGISTERED_TASK_ID":"NO_CURRENT_TYPED_PROGRESS_OR_WORK_UNIT_MISMATCH";
                boolean resourceWait=WaitKindR27.RESOURCE.equals(waitKind);
                boolean dependency=WaitKindR27.DEPENDENCY.equals(waitKind)||blocked!=null&&(blocked.startsWith("DEPENDENCY")||blocked.startsWith("WAITING_"));
                boolean validLease="RUNNING".equals(status)&&boot.equals(rows.getString(20))&&!rows.isNull(21)&&rows.getLong(21)>elapsed;
                String state;
                if("DISABLED".equals(status))state="DISABLED";
                else if("QUARANTINED".equals(status)){state="QUARANTINED";quarantined++;}
                else if("NEEDS_FIX".equals(status)||"ERROR".equals(status)||"FAILED".equals(status)){state="NEEDS_FIX";fixes++;}
                else if("CANCELLING".equals(status))state="DRAINING_NOT_TERMINATED";
                else if("COMMIT_UNCERTAIN".equals(status))state="COMMIT_RESOLUTION_REQUIRED";
                else if(resourceWait){state="RESOURCE_WAIT";dependent++;}
                else if(dependency){state="WAITING_DEPENDENCY";dependent++;}
                else if(workPending==null||latest==null){state="UNKNOWN";unknown++;}
                else if(workPending>0&&clock!=null&&latest>=clock&&latest-clock>=TaskLivenessR30.STALL_PERIODS){state="STALLED";stalled++;}
                else if(workPending>0&&clock==null){state="UNKNOWN";unknown++;sourceFailure="LAST_DURABLE_COMMIT_CLOCK_UNKNOWN";}
                else if(workPending==0)state="IDLE_WAITING_INPUT";
                else{state=validLease?"RUNNING":"BACKLOG";active++;}
                if(workPending==null&&!(state.equals("UNKNOWN")))unknown++;
                if(workPending!=null&&workPending>0&&!dependency&&!resourceWait&&!"DISABLED".equals(status))pendingTasks++;
                String artifact=current?rows.getString(13):null;
                if(entry!=null&&entry.unit!=TaskProgressR30R1.Unit.TIME_TICK&&artifact!=null&&!artifact.isEmpty()&&!rows.isNull(14)&&rows.getLong(14)>latestArtifactTime){latestArtifactTime=rows.getLong(14);lastArtifactOrdinal=clock;lastArtifactHash=artifact;}
                java.util.Map<String,Object> row=new java.util.LinkedHashMap<>();row.put("taskId",id);row.put("rawSchedulerState",status);row.put("state",state);row.put("ownerEpoch",rows.getString(2));row.put("fence",rows.getLong(3));row.put("workUnit",entry==null?null:entry.unit.name());row.put("pending",workPending);row.put("pendingKnown",workPending!=null);row.put("lastArtifactClockOrdinal",clock);row.put("lastArtifactHash",artifact);row.put("completedInputOrdinal",committedInput);row.put("sourceFailure",sourceFailure);row.put("blockedReason",RuntimeFaultEvidenceR31.clean(blocked,256));row.put("leaseValid",validLease);row.put("dependencyMissing",dependency);row.put("quarantineLevel",rows.isNull(17)?null:rows.getLong(17));row.put("faultFingerprint",rows.getString(18));row.put("halfOpenOwner",rows.getString(19));tasks.add(row);
            }
            for(String registered:ResearchTaskRegistryR30R1.all().keySet())if(!seen.contains(registered))unknown++;
        }catch(RuntimeException source){failure="HEALTH_SOURCE_UNAVAILABLE:"+source.getClass().getSimpleName();unknown=Math.max(1,ResearchTaskRegistryR30R1.all().size()-tasks.size());}
        boolean broken=quarantined>0||fixes>0;for(java.util.Map<String,Object> row:tasks)broken|=row.get("state").equals("DRAINING_NOT_TERMINATED")||row.get("state").equals("COMMIT_RESOLUTION_REQUIRED");
        String health=broken?"DEGRADED":resource!=null?"RESOURCE_PAUSED":stalled>0?"STALLED":unknown>0?"UNKNOWN":active>0?"PROGRESS_PENDING":dependent>0?"WAITING_DEPENDENCY_OR_RESOURCE":"IDLE_WAITING_INPUT";
        if(process.contains("DRAINING")||process.contains("STOPPED"))health="OWNER_DRAINING_OR_STOPPED";
        String why=failure!=null?failure:broken?"AUTHORITATIVE_TASK_FAULT_OR_DRAINING":stalled>0?"READY_PENDING_WITHOUT_DURABLE_PROGRESS_FOR_FIVE_PERIODS":resource!=null?resource:unknown>0?"CURRENT_PENDING_OR_SOURCE_EVIDENCE_MISSING":"PER_TASK_TYPED_COMMIT_AND_INPUT_FACTS";
        return new RuntimeHealthSnapshot(owner,boot,wall,elapsed,process,health,why,latest,lastArtifactOrdinal,lastArtifactHash,quarantined,fixes,stalled,unknown,pendingTasks,tasks);
    }
    private volatile RuntimeHealthSnapshot healthSnapshotR31;
    private final java.util.concurrent.atomic.AtomicBoolean healthProbePendingR31=new java.util.concurrent.atomic.AtomicBoolean();
    private final java.util.Map<String,Long> nextTaskRecoveryR31=new java.util.HashMap<>();
    private final java.util.Map<String,String> lastStalledArtifactR31=new java.util.HashMap<>();
    private final java.util.Map<String,String> lastRecoveryArtifactR31=new java.util.HashMap<>();
    private final java.util.Map<String,Claim> drainingTasksR31=new java.util.HashMap<>();
    private long healthControlRequestsR31,healthControlRunsR31,healthTaskRevokesR31;
    private volatile String lastHealthControlReasonR31="NOT_REQUESTED",lastHealthControlFailureR31;
    private static final class HealthControlHolderR31{static final BoundedResearchExecutorR31 INSTANCE=new BoundedResearchExecutorR31(1,1024,MemoryBudgetR31.processBudget(),r->{Thread thread=new Thread(r,"starcore-r31-health-controller");thread.setDaemon(true);return thread;});}
    private static BoundedResearchExecutorR31 healthControlExecutorR31(){return HealthControlHolderR31.INSTANCE;}
    public RuntimeHealthSnapshot healthSnapshotR31(){
        RuntimeHealthSnapshot current=healthSnapshotR31;
        if(current==null)return new RuntimeHealthSnapshot(owner,IssueTimelineWriterR30.bootId(),System.currentTimeMillis(),SystemClock.elapsedRealtime(),isRunning()?"ALIVE_OWNER_RUNNING":"ALIVE_OWNER_DRAINING_OR_STOPPED","UNKNOWN","HEALTH_PROBE_NOT_YET_SAMPLED",null,null,null,0,0,0,ResearchTaskRegistryR30R1.all().size(),0,java.util.Collections.emptyList());
        boolean stale=SystemClock.elapsedRealtime()-current.sampleElapsedMs>10_000;
        String pressure=String.valueOf(memoryObservationR31.get("pressure"));
        if(!cancelled&&!stale&&resourceBlocked==null&&!"SOFT".equals(pressure)&&lastHealthControlFailureR31==null)return current;
        String health=cancelled?"OWNER_DRAINING_OR_STOPPED":current.quarantined>0||current.needsFix>0?"DEGRADED":resourceBlocked!=null?"RESOURCE_PAUSED":"SOFT".equals(pressure)?"RESOURCE_LIMITED":stale?"STALE_HEALTH_OBSERVATION":current.researchHealth;
        String why=cancelled?"OWNER_STOP_REQUESTED":resourceBlocked!=null?resourceBlocked:stale?"CONTROL_SAMPLE_OLDER_THAN_TWO_TICKS":lastHealthControlFailureR31!=null?lastHealthControlFailureR31:"CURRENT_RESOURCE_OBSERVATION_WITH_CACHED_TASK_FACTS";
        RuntimeHealthSnapshot merged=new RuntimeHealthSnapshot(owner,current.bootId,current.sampleWallMs,current.sampleElapsedMs,isRunning()?"ALIVE_OWNER_RUNNING":"ALIVE_OWNER_DRAINING_OR_STOPPED",health,why,current.latestSourceOrdinal,current.lastArtifactOrdinal,current.lastArtifactHash,current.quarantined,current.needsFix,current.stalled,stale?Math.max(1,current.unknownTaskCount):current.unknownTaskCount,current.knownPendingTaskCount,current.tasksImmutable);
        java.util.Map<String,Object> control=new java.util.LinkedHashMap<>();Object previous=current.asMap().get("control");if(previous instanceof java.util.Map)for(java.util.Map.Entry<?,?> entry:((java.util.Map<?,?>)previous).entrySet())control.put(String.valueOf(entry.getKey()),entry.getValue());control.put("memory",memoryObservationR31);control.put("resourceReason",resourceBlocked);control.put("resourceObservedElapsedMs",resourceChecked);control.put("taskFactsAreCached",true);control.put("lastControlAdmissionFailure",lastHealthControlFailureR31);return merged.withControl(control);
    }
    public RuntimeHealthSnapshot reconcileResearchHealthR31(String reason){
        synchronized(signal){if(cancelled)return healthSnapshotR31();healthControlRequestsR31++;if(!healthProbePendingR31.compareAndSet(false,true))return healthSnapshotR31();}lastHealthControlReasonR31=RuntimeFaultEvidenceR31.clean(reason,128);
        try{healthControlExecutorR31().submit("health:"+owner,512,HEALTH_WORKING_ESTIMATE_R31,()->{try{
            RuntimeHealthSnapshot observed=captureHealthSnapshotR31(store.readable(),owner,IssueTimelineWriterR30.bootId(),System.currentTimeMillis(),SystemClock.elapsedRealtime(),isRunning()?"ALIVE_OWNER_RUNNING":"ALIVE_OWNER_DRAINING_OR_STOPPED",resourceBlocked);
            healthControlRunsR31++;lastHealthControlFailureR31=null;
            if(!cancelled){try{ResearchPoolR27.recoverOneR31(store,owner);synchronized(signal){signal.notifyAll();}reconcileStalledTasksR31(observed);}catch(RuntimeException controlFailure){lastHealthControlFailureR31=RuntimeFaultEvidenceR31.capture(controlFailure,"HEALTH_CONTROL").category;}}
            java.util.Map<String,Object> control=new java.util.LinkedHashMap<>();synchronized(signal){control.put("requestCount",healthControlRequestsR31);}control.put("observedControlRuns",healthControlRunsR31);control.put("taskRevocations",healthTaskRevokesR31);control.put("pendingActualDrains",drainingTasksR31.size());control.put("requestReason",lastHealthControlReasonR31);control.put("controlFailure",lastHealthControlFailureR31);control.put("memory",memoryBudgetR31.snapshot());control.put("memoryPressureAgeMs",memoryPressureSinceR31==0?0:Math.max(0,SystemClock.elapsedRealtime()-memoryPressureSinceR31));control.put("memoryPressureOverTwoMinutes",memoryPressureSinceR31!=0&&SystemClock.elapsedRealtime()-memoryPressureSinceR31>=120_000);control.put("executor",healthControlExecutorR31().snapshot());control.put("workingEstimateBasis","128_BOUNDED_ROWS_WITH_CAPPED_TEXT_AND_MAP_ALLOWANCE_NOT_MEASURED_PEAK_OR_SQLITE_NATIVE");control.put("result","OBSERVATION_AND_CONTROL_REQUESTS_ONLY_NOT_RECOVERED_UNTIL_NEW_DURABLE_ARTIFACT");healthSnapshotR31=observed.withControl(control);
        }catch(RuntimeException sourceFailure){healthSnapshotR31=new RuntimeHealthSnapshot(owner,IssueTimelineWriterR30.bootId(),System.currentTimeMillis(),SystemClock.elapsedRealtime(),isRunning()?"ALIVE_OWNER_RUNNING":"ALIVE_OWNER_DRAINING_OR_STOPPED","UNKNOWN","HEALTH_SOURCE_UNAVAILABLE:"+sourceFailure.getClass().getSimpleName(),null,null,null,0,0,0,ResearchTaskRegistryR30R1.all().size(),0,java.util.Collections.emptyList());
        }catch(Error fatal){lastHealthControlFailureR31="HEALTH_CONTROL_FATAL:"+fatal.getClass().getSimpleName();healthSnapshotR31=new RuntimeHealthSnapshot(owner,IssueTimelineWriterR30.bootId(),System.currentTimeMillis(),SystemClock.elapsedRealtime(),isRunning()?"ALIVE_OWNER_RUNNING":"ALIVE_OWNER_DRAINING_OR_STOPPED","UNKNOWN",lastHealthControlFailureR31,null,null,null,0,0,0,ResearchTaskRegistryR30R1.all().size(),0,java.util.Collections.emptyList());throw fatal;
        }finally{healthProbePendingR31.set(false);}return null;});}catch(java.util.concurrent.RejectedExecutionException capacity){lastHealthControlFailureR31=RuntimeFaultEvidenceR31.clean(capacity.getMessage(),128);healthProbePendingR31.set(false);}
        return healthSnapshotR31();
    }
    private void cancelTaskComputationR31(Claim claim){
        if(claim.task.equals("legacy-generation"))LegacyGenerationR26.cancel(owner);
        else if(claim.task.equals("legacy-replay"))LegacyReplayContinuationR26.cancel(owner);
        CooperativeContinuationR26.requestTaskCancellationR31(owner,"starcore-r26-"+claim.task);ResearchPoolR27.cancelTaskHelpersR31(claim);
    }
    private void reconcileStalledTasksR31(RuntimeHealthSnapshot snapshot){
        java.util.Iterator<java.util.Map.Entry<String,Claim>> pending=drainingTasksR31.entrySet().iterator();
        while(pending.hasNext()){
            Claim old=pending.next().getValue();cancelTaskComputationR31(old);Claim executing=executingClaimR31;
            if(executing!=null&&executing.task.equals(old.task))continue;
            if(!CooperativeContinuationR26.isTaskTerminatedR31(owner,"starcore-r26-"+old.task)||!ResearchPoolR27.helpersTerminatedR31(old))continue;
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(controlContextR31("R31_HEALTH_R26_DRAINED"),db->{db.execSQL("UPDATE r26_task SET status='READY',wait_kind='HALF_OPEN',blocked_reason='ACTUAL_TASK_COMPUTATION_TERMINATED' WHERE task_id=? AND owner=? AND fence=? AND status='CANCELLING'",new Object[]{old.task,owner,old.fence+1});return 0;});ResearchPoolR27.clearTaskHelperCancellationR31(old);pending.remove();
        }
        RuntimeR27 runtime=r27;
        if(resourceBlocked!=null)return;
        for(java.util.Map<String,Object> task:snapshot.tasksImmutable){
            String id=(String)task.get("taskId"),state=(String)task.get("state"),artifact=(String)task.get("lastArtifactHash");long fence=((Number)task.get("fence")).longValue();
            if(!owner.equals(task.get("ownerEpoch")))continue;
            if(id.startsWith("r27-")&&runtime==null)continue;
            if(id.startsWith("r27-")&&state.equals("DRAINING_NOT_TERMINATED")&&runtime!=null){runtime.pool().reconcileTaskRecoveryR31(id,fence);continue;}
            if(!state.equals("STALLED"))continue;
            if(runtime!=null&&id.startsWith("r27-"))runtime.pool().wake(id);
            String previous=lastStalledArtifactR31.put(id,artifact);if(!java.util.Objects.equals(previous,artifact)||previous==null)continue;
            // A resumed long chunk must be able to finish. Repeated observation of the same durable
            // checkpoint wakes it but does not restart it every control interval.
            if(java.util.Objects.equals(lastRecoveryArtifactR31.get(id),artifact))continue;
            if(nextTaskRecoveryR31.getOrDefault(id,0L)>snapshot.sampleElapsedMs||drainingTasksR31.containsKey(id))continue;
            boolean revoked;
            if(id.startsWith("r27-")&&runtime!=null)revoked=runtime.pool().requestTaskRecoveryR31(id,fence,artifact,"STALLED_FIVE_PERIODS_TWO_OBSERVATIONS");
            else{
                Integer changed=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(controlContextR31("R31_HEALTH_R26_REVOKE"),db->{db.execSQL("UPDATE r26_task SET status='CANCELLING',fence=fence+1,lease_until=0,wait_kind='CANCELLED',blocked_reason='CONTROLLED_STALLED_TASK_RECOVERY' WHERE task_id=? AND owner=? AND fence=? AND status IN ('RUNNING','READY','YIELDED','BLOCKED') AND NOT EXISTS(SELECT 1 FROM r30r1_task_progress p WHERE p.task_id=r26_task.task_id AND p.artifact_identity IS NOT ?)",new Object[]{id,owner,fence,artifact});int rows;try(Cursor count=db.rawQuery("SELECT changes()",null)){rows=count.moveToFirst()?count.getInt(0):0;}if(rows==1)db.execSQL("UPDATE r31_task_lease SET deadline_elapsed_ms=0,next_elapsed_ms=0 WHERE task_id=? AND owner_epoch=? AND fence=?",new Object[]{id,owner,fence});return rows;});
                revoked=changed!=null&&changed==1;if(revoked){Claim old=new Claim(id,owner,fence,0);drainingTasksR31.put(id,old);cancelTaskComputationR31(old);}
            }
            if(revoked){healthTaskRevokesR31++;lastRecoveryArtifactR31.put(id,artifact);nextTaskRecoveryR31.put(id,snapshot.sampleElapsedMs+30_000);break;}
        }
    }
    public boolean isCpuLeaseHeld(){RuntimePowerLeaseR26 lease=powerLease;return lease!=null&&lease.isHeld();}
    public long cpuLeaseRenewals(){RuntimePowerLeaseR26 lease=powerLease;return lease==null?0:lease.renewals();}
    public static void requireFence(SQLiteDatabase db,Claim claim){try(Cursor c=db.rawQuery("SELECT 1 FROM r26_task t JOIN r26_session s ON s.session_id=t.owner JOIN r31_task_lease l ON l.task_id=t.task_id AND l.owner_epoch=t.owner AND l.fence=t.fence WHERE t.task_id=? AND t.owner=? AND t.fence=? AND t.status='RUNNING' AND l.boot_id=? AND l.deadline_elapsed_ms>? AND s.user_enabled=1 AND s.state='RUNNING'",new String[]{claim.task,claim.owner,String.valueOf(claim.fence),IssueTimelineWriterR30.bootId(),String.valueOf(SystemClock.elapsedRealtime())})){if(!c.moveToFirst())throw new IllegalStateException("R26_STALE_FENCE_OR_LEASE_OR_SESSION");}}
    static DatabaseCoordinator.WriteContext controlContextR31(String operation){return DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,operation);}
    public static final String CLAIM_CAS_R31="UPDATE r26_task SET owner=?,fence=?,status='RUNNING',lease_until=?,last_served=? WHERE task_id=? AND owner IS ? AND fence=? AND status=?";
    static Claim claimWithinR31(SQLiteDatabase db,String task,String owner,String lane,Claim prepared){
        long wall=System.currentTimeMillis(),mono=SystemClock.elapsedRealtime();String boot=IssueTimelineWriterR30.bootId();
        try(Cursor s=db.rawQuery("SELECT 1 FROM r26_session WHERE session_id=? AND user_enabled=1 AND state='RUNNING'",new String[]{owner})){if(!s.moveToFirst())return null;}
        long fence,cursor;String beforeOwner,status;
        try(Cursor row=db.rawQuery("SELECT fence,cursor,owner,status FROM r26_task WHERE task_id=?",new String[]{task})){if(!row.moveToFirst())return null;fence=row.getLong(0);cursor=row.getLong(1);beforeOwner=row.getString(2);status=row.getString(3);}
        if(!java.util.Arrays.asList("READY","YIELDED","BLOCKED","RUNNING").contains(status))return null;
        long until=0,next=0;String leaseBoot="";
        try(Cursor lease=db.rawQuery("SELECT boot_id,deadline_elapsed_ms,next_elapsed_ms FROM r31_task_lease WHERE task_id=? AND owner_epoch IS ? AND fence=?",new String[]{task,beforeOwner,String.valueOf(fence)})){if(lease.moveToFirst()){leaseBoot=lease.getString(0);until=lease.getLong(1);next=lease.getLong(2);}}
        if(prepared!=null){
            if(!owner.equals(beforeOwner)||prepared.fence!=fence||!boot.equals(leaseBoot)||until<=mono)throw new IllegalStateException("R31_STALE_PREPARED_LEASE");
        }else if(boot.equals(leaseBoot)&&(until>mono||next>mono))return null;
        long nextFence=prepared==null?fence+1:fence;
        db.execSQL(CLAIM_CAS_R31,new Object[]{owner,nextFence,wall+30_000,wall,task,beforeOwner,fence,status});
        try(Cursor changed=db.rawQuery("SELECT changes()",null)){if(!changed.moveToFirst()||changed.getInt(0)!=1)return null;}
        db.execSQL("INSERT OR REPLACE INTO r31_task_lease(task_id,owner_epoch,fence,boot_id,deadline_elapsed_ms,next_elapsed_ms) VALUES(?,?,?,?,?,0)",new Object[]{task,owner,nextFence,boot,mono+30_000});
        db.execSQL("UPDATE r31_task_control SET half_open_fence=? WHERE task_id=? AND half_open_owner=?",new Object[]{nextFence,task,owner});
        if(prepared!=null)return prepared;
        Claim result=new Claim(task,owner,nextFence,cursor,lane);
        if(SchemaR27.tableExists(db,"r30r1_task_progress"))try(Cursor artifact=db.rawQuery("SELECT artifact_identity FROM r30r1_task_progress WHERE task_id=?",new String[]{task})){if(artifact.moveToFirst())result.artifactAtClaimR31=artifact.getString(0);}
        return result;
    }
    static void releaseLeaseR31(SQLiteDatabase db,Claim claim,long nextDelay,boolean keepPrepared){
        long mono=SystemClock.elapsedRealtime();db.execSQL("UPDATE r31_task_lease SET deadline_elapsed_ms=?,next_elapsed_ms=? WHERE task_id=? AND owner_epoch=? AND fence=? AND boot_id=?",new Object[]{keepPrepared?mono+30_000:0,keepPrepared?0:mono+nextDelay,claim.task,claim.owner,claim.fence,IssueTimelineWriterR30.bootId()});
    }
    private void initialize(){
        // R30-B00-03: retire R29 CP07-CP16 Root triggers / independent RP before any worker starts (idempotent, no-op on CP06 data).
        try{String foldR30=R29FoldBackR30.run(store);if(!"NONE".equals(foldR30))android.util.Log.i("STARCORE_R30","R29 fold-back "+foldR30);}
        catch(Exception foldFailedR30){android.util.Log.e("STARCORE_R30","R29 fold-back incomplete; route kept for next start",foldFailedR30);store.setState("r30_foldback_error",String.valueOf(foldFailedR30));}
        try{LegacyTeacherOwnerR26.register(store.contextForR26());}catch(RuntimeException blocked){android.util.Log.w("STARCORE_R26","original teacher blocked",blocked);store.setState("r26_teacher_blocked",blocked.toString());}
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R26_RUNTIME_SCHEMA"),db->{RuntimeSchemaR26.ensure(db);SchemaR27.ensure(db);ResearchRetentionStoreR30.ensureProtected(db);
            // R27.1: tasks quarantined only by the fixed continuation-quantum bug get their normal retry back.
            // R31 inherited quarantines are recovered one fenced half-open probe at a time.
            RuntimeSchemaR26.bindCapabilitySource(db,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256);long now=System.currentTimeMillis();
            // A new process has a new monotonic epoch. Leases remain wall-clock bounded and fenced.
            java.util.List<Object> interrupted=new java.util.ArrayList<>();
            try(Cursor previous=db.rawQuery("SELECT session_id,heartbeat,state FROM r26_session WHERE user_enabled=1 ORDER BY heartbeat DESC LIMIT 16",null)){while(previous.moveToNext()){java.util.Map<String,Object> gap=new java.util.LinkedHashMap<>();gap.put("sessionId",previous.getString(0));gap.put("lastPersistedHeartbeat",previous.getLong(1));gap.put("previousState",previous.getString(2));gap.put("recoveredAt",now);gap.put("unobservedIntervalMs",Math.max(0,now-previous.getLong(1)));gap.put("cause","UNKNOWN_PROCESS_OR_SERVICE_GAP_NOT_PROVEN_CONTINUOUS");interrupted.add(gap);}}
            java.util.Map<String,Object> recovery=new java.util.LinkedHashMap<>();recovery.put("version","R30_REPAIR_LIFECYCLE_1");recovery.put("newSession",owner);recovery.put("recoveredAt",now);recovery.put("interruptedSessions",interrupted);recovery.put("scope","Observed persisted gaps only. OEM force stop and killed-process time are not service uptime; resume uses durable artifacts and invalidates prior leases.");
            db.execSQL("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r30-lifecycle','last-recovery',?,?)",new Object[]{JsonR27.write(recovery),now});
            db.execSQL("UPDATE r26_session SET state='INTERRUPTED',user_enabled=0 WHERE user_enabled=1");
            db.execSQL("UPDATE r26_task SET status='YIELDED',lease_until=0,fence=fence+1,owner=NULL WHERE status IN ('RUNNING','CANCELLING')");
            db.execSQL("INSERT OR REPLACE INTO r26_session VALUES(?,1,?,'RUNNING',?,coalesce((SELECT actual_progress FROM r26_session WHERE session_id=?),0))",new Object[]{owner,String.valueOf(now-SystemClock.elapsedRealtime()),now,owner});
            add(db,"schema","P1");add(db,"schema-base","P1");add(db,"kernel","P1");add(db,"shadow-recover","P1");add(db,"evidence-recover","P1");add(db,"evidence-quality","P1");add(db,"sealed-planner","P1");add(db,"calibration","P2");add(db,"su1-certificate","P1");add(db,"su2-mainline","P1");add(db,"su2-certificate","P1");add(db,"qualification","P1");add(db,"aggregate-evidence","P1");add(db,"feedback","P1");add(db,"seed-incumbent","P1");add(db,"incumbent-forecast","P1");
            add(db,"legacy-generation","P2");add(db,"legacy-replay","P2");add(db,"artifact-retention","P3");add(db,"warm-candidates","P2");add(db,"publish-models","P2");add(db,"live-model-training","P2");add(db,"live-forecast-preparation","P2");add(db,"foundry-su2","P2");add(db,"foundry-su1","P2");add(db,"ablation","P2");add(db,"campaign-audit","P2");add(db,"archive","P3");add(db,"retention","P3");add(db,"diagnosis","P3");add(db,"daily-close","P3");
            for(int i=1;i<=24;i++)registerCapability(db,String.format(Locale.ROOT,"F%02d",i),"FAMILY");for(int i=1;i<=16;i++){String id=String.format(Locale.ROOT,"E%02d",i);registerCapability(db,id,"EXPERT");add(db,"expert-"+id,"P2");}
            db.execSQL("UPDATE r26_task SET status='BLOCKED',blocked_reason='WAITING_SELF_CONTAINED_STRATEGY_STORAGE',next_at=0 WHERE task_id<>'schema' AND lane<>'R27' AND status NOT IN ('QUARANTINED','NEEDS_FIX')");
            for(String r27Task:new String[]{"r27-replay-dataset","r27-evolution","r27-live","r27-atoms","r27-atomnet","r27-abi","r27-calibration","r27-chart-cache","r27-legacy-export","r27-health"})
                db.execSQL("INSERT OR IGNORE INTO r26_task(task_id,lane,mechanism,status,registered_at) VALUES(?,'R27',?,'READY',?)",new Object[]{r27Task,r27Task,now});
            return 0;});
        retentionProtectionReadyR31=true;
        ReplayBurstSchedulerSU1.cancel(store.contextForR26());
    }
    private static void add(SQLiteDatabase db,String id,String lane){db.execSQL("INSERT OR IGNORE INTO r26_task(task_id,lane,mechanism) VALUES(?,?,?)",new Object[]{id,lane,id});}
    private static void registerCapability(SQLiteDatabase db,String id,String kind){
        String implementation=kind.equals("EXPERT")?ExpertWorkshopR26.implementation(id):FamilyRegistryR26.implementation(id);String state=implementation.isEmpty()?"BLOCKED":"EXECUTABLE";
        String reason=kind.equals("EXPERT")&&!implementation.isEmpty()?"Concrete resumable paired experiment or evidence audit; full proposal and expert acceptance pending":implementation.isEmpty()?"Full R26 mechanism integration and decisive experiments pending; no fallback substituted":"Concrete numerical kernel registered; complete family experiments and natural quality pending";
        db.execSQL("INSERT OR REPLACE INTO r26_capability(mechanism,kind,implementation,state,reason,last_progress) VALUES(?,?,?,?,?,coalesce((SELECT last_progress FROM r26_capability WHERE mechanism=?),0))",new Object[]{id,kind,implementation,state,reason,id});
    }
    private Claim claim(){
        // R27-008: right after a new draw the incumbent forecast for the next target is prepared first, so the
        // freeze is a small write long before the open time instead of INCUMBENT_MISSING_AFTER_OPEN.
        if(strategyStorageReady){long seq=latestSequence();if(seq>0&&seq!=lastIncumbentSeq){Claim first=claimTask("incumbent-forecast","P1");if(first!=null){lastIncumbentSeq=seq;return first;}}}
        // R30-B01-06: once per scheduling round, a ready task that TaskLivenessR30 classifies STALLED is claimed first.
        if(strategyStorageReady&&stallRoundR30++%SLOTS.length==0){Claim boosted=claimStalledR30();if(boosted!=null)return boosted;}
        // Dependency-blocked tasks are not ready work. Polling forty of them between migration
        // units made a fresh installation spend minutes without reaching its first model sample.
        final String dependency=strategyStorageReady?"":" AND task_id='schema'";
        for(int attempt=0;attempt<SLOTS.length;attempt++){
            String lane=attempt==0&&service.needsP2()?"P2":SLOTS[slot++%SLOTS.length];
            Claim result=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(controlContextR31("R26_TASK_CLAIM"),db->{
                long now=System.currentTimeMillis();String id=null;long fence=0,cursor=0;
                int liveModels=1;if(lane.equals("P2")&&strategyStorageReady)try(Cursor count=db.rawQuery("SELECT count(*) FROM r26_live_model WHERE status='ACTIVE'",null)){if(count.moveToFirst())liveModels=Math.max(1,count.getInt(0));}
                Set<String> unserved=new HashSet<>();if(lane.equals("P2")&&strategyStorageReady)try(Cursor capabilities=db.rawQuery("SELECT mechanism FROM r26_capability WHERE last_progress=0 AND implementation<>''",null)){while(capabilities.moveToNext())unserved.add(capabilities.getString(0));}
                int bestPriority=Integer.MAX_VALUE;double best=Double.POSITIVE_INFINITY;
                try(Cursor c=db.rawQuery("SELECT task_id,fence,cursor FROM r26_task WHERE lane=? AND status IN ('READY','YIELDED','BLOCKED','RUNNING')"+dependency+" ORDER BY last_served,task_id"+(lane.equals("P2")?"":" LIMIT 1"),new String[]{lane})){
                    while(c.moveToNext()){double score=lane.equals("P2")?service.score(c.getString(0),CapabilityServiceR26.weight(c.getString(0),FamilyRegistryR26.EXECUTABLE.size(),liveModels)):0;int priority=lane.equals("P2")&&CapabilityServiceR26.firstService(c.getString(0),unserved)?0:1;if(priority<bestPriority||priority==bestPriority&&score<best){id=c.getString(0);fence=c.getLong(1)+1;cursor=c.getLong(2);best=score;bestPriority=priority;}}
                }
                if(id==null)return null;
                return claimWithinR31(db,id,owner,lane,null);
            });if(result!=null)return result;
        }return null;
    }
    private long stallRoundR30,stallFactsCheckedR30;private boolean stallFactsR30;
    /** R30-B01-06: STALLED = pending>0 and >=5 draws since the task's last commit (TaskLivenessR30). Without the
     * pending/last_committed_seq facts every task is UNKNOWN, so nothing is boosted (no guessed stall). */
    private Claim claimStalledR30(){
        final long latest=latestSequence();if(latest<0)return null;
        if(!ResearchStoreR30R1.ready(store.readable()))return null;
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(controlContextR31("R26_TASK_CLAIM"),db->{
            long now=System.currentTimeMillis();
            try(Cursor c=db.rawQuery("SELECT t.task_id,t.fence,t.cursor,t.lane,t.status,coalesce(t.blocked_reason,''),p.pending,p.last_commit_clock_ordinal FROM r26_task t JOIN r30r1_task_progress p ON p.task_id=t.task_id WHERE t.lane<>'R27' AND t.status IN ('READY','YIELDED','BLOCKED') AND p.pending IS NOT NULL AND p.last_commit_clock_ordinal IS NOT NULL AND p.owner_version=? ORDER BY t.last_served,t.task_id",new String[]{ResearchTaskRegistryR30R1.VERSION})){
                while(c.moveToNext()){
                    String reason=c.getString(5);long seq=c.getLong(7);
                    TaskLivenessR30.State st=TaskLivenessR30.classify(c.getLong(6),seq<0?-1:Math.max(0,latest-seq),reason.startsWith("DEPENDENCY")||reason.startsWith("WAITING_"),reason.contains("WAITING_NEW_DRAW"),false);
                    if(st!=TaskLivenessR30.State.STALLED)continue;
                    String id=c.getString(0);long fence=c.getLong(1)+1,cursor=c.getLong(2);String lane=c.getString(3);
                    Claim result=claimWithinR31(db,id,owner,lane,null);if(result!=null)return result;
                }
            }
            return null;
        });
    }
    private long latestSequence(){try{Models.Draw latest=store.getLatestDraw();return latest==null||!TargetIssueResolver.isValid(latest.issue)?-1:TargetIssueResolver.sequenceOrdinalSU1(latest.issue);}catch(RuntimeException e){return -1;}}
    private Claim claimTask(String task,String lane){
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(controlContextR31("R27_PRIORITY_CLAIM"),db->{long now=System.currentTimeMillis();
            try(Cursor c=db.rawQuery("SELECT fence,cursor FROM r26_task WHERE task_id=? AND status IN ('READY','YIELDED','BLOCKED')",new String[]{task})){
                if(!c.moveToFirst())return null;long fence=c.getLong(0)+1,cursor=c.getLong(1);
                return claimWithinR31(db,task,owner,lane,null);}});
    }
    public RuntimeR27 r27(){return r27;}
    private void run(){
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        com.ethk3.novaonepro.StallStacksR27.start();
        try{
            ServiceHeartbeatStore user=new ServiceHeartbeatStore(store.contextForR26());if(!user.isUserEnabled()){cancelled=true;return;}
            powerLease=new RuntimePowerLeaseR26(store.contextForR26(),owner);powerLease.keepAlive();
            boolean initialized=false;
            while(!cancelled&&!initialized){if(!user.isUserEnabled()){cancelled=true;break;}powerLease.keepAlive();try{initialize();initialized=true;}catch(RuntimeException retry){FailureDispositionR31.Decision disposition=FailureDispositionR31.classify(retry);if(disposition.disposition==FailureDispositionR31.Disposition.FAULT||disposition.disposition==FailureDispositionR31.Disposition.FATAL)throw retry;android.util.Log.w("STARCORE_R26","initialization dependency wait",retry);await(500);}}
            // R27-003/009: DbStore → P0 (service) → research scheduler. The R27 pool owns the R27 tasks.
            if(!cancelled){RuntimeR27 runtime=new RuntimeR27(store,owner);r27=runtime;runtime.start();}
            while(!cancelled){
            if(!user.isUserEnabled()){cancelled=true;break;}powerLease.keepAlive();
            resources();if(SystemClock.elapsedRealtime()-lastOwnerHealthRequestR31>=5000){lastOwnerHealthRequestR31=SystemClock.elapsedRealtime();reconcileResearchHealthR31("OWNER_FIVE_SECOND_CONTROL_TICK");}CooperativeContinuationR26.reapTerminated();ResearchPoolR27.recoverOneR31(store,owner);
            RuntimeR27 pool=r27;if(pool!=null)pool.pool().setResource(resourceBlocked);
            if(preempt()){try{sessionBlocked(resourceBlocked==null?"STOPPING":resourceBlocked);}catch(RuntimeException busy){android.util.Log.w("STARCORE_R26","blocked receipt retry",busy);}await(250);continue;}
            try{sessionBlocked(null);}catch(RuntimeException busy){android.util.Log.w("STARCORE_R26","session status retry",busy);}
            Claim claim;
            try{claim=claim();}catch(RuntimeException failure){FailureDispositionR31.Decision disposition=FailureDispositionR31.classify(failure);if(disposition.disposition==FailureDispositionR31.Disposition.FAULT||disposition.disposition==FailureDispositionR31.Disposition.FATAL)throw failure;android.util.Log.w("STARCORE_R26","dispatch waiting "+disposition.category);await(250);continue;}
            if(claim==null){await(250);continue;}long start=SystemClock.elapsedRealtime(),cpu=SystemClock.currentThreadTimeMillis();long delta=0;String reason=null;Throwable failed=null;
            try{executingClaimR31=claim;WRITING_CLAIM.set(claim);BudgetTokenSU1 budget=new BudgetTokenSU1((int)CooperativeContinuationR26.SLICE_MS,3000000,this::preempt);ExecutionBudgetR26.enter(budget);delta=execute(claim,budget);reason=claim.reason;}
            catch(BudgetTokenSU1.Yield yield){reason=resourceBlocked!=null?resourceBlocked:"SLICE_BUDGET";}
            catch(Exception failure){Error processFatal=FailureDispositionR31.processFatalCause(failure);if(processFatal!=null)throw processFatal;failed=failure;FailureDispositionR31.Decision disposition=FailureDispositionR31.classify(failure);reason=disposition.category;if(disposition.disposition==FailureDispositionR31.Disposition.CANCELLED&&Thread.currentThread().isInterrupted())cancelled=true;}
            finally{ExecutionBudgetR26.leave();WRITING_CLAIM.remove();executingClaimR31=null;}
            // R27-006: a real wait kind instead of the blanket DATA_NOT_READY.
            final WaitKindR27.Wait wait=WaitKindR27.classify(delta,reason,failed,claim.cursor,latestSequence(),resourceBlocked,false);
            final String waitStack=failed==null?null:ResearchPoolR27.stack(failed);final Throwable sliceFailure=failed;
            if(failed!=null)reason=failed.getClass().getSimpleName()+":"+String.valueOf(failed.getMessage());else if(reason==null&&delta==0)reason=wait.blockedReason();
            final long work=delta,nextCursor=claim.completedEventCursor>=0?claim.completedEventCursor:claim.cursor+delta,elapsed=SystemClock.elapsedRealtime()-start,cpuMs=Math.max(0,SystemClock.currentThreadTimeMillis()-cpu)+claim.workerCpuMillis;final String blocked=elapsed>2*CooperativeContinuationR26.SLICE_MS?"CAPACITY_FAIL:UNSLICED_KERNEL:"+elapsed+"ms|"+reason:reason;
            if(elapsed>2*CooperativeContinuationR26.SLICE_MS)android.util.Log.w("STARCORE_R26","task="+claim.task+"|wallMs="+elapsed+"|cpuMs="+cpuMs+"|"+blocked);
            service.served(claim.task,claim.lane,cpuMs);
            boolean taskReceiptCommittedR31=recordClaimR31(claim,work,nextCursor,cpuMs,elapsed,blocked,wait,sliceFailure);
            ResearchPoolR27.observeAttemptR31(store,claim.task,claim.owner,claim,"COMPUTE_OR_PUBLISH",sliceFailure,work,cpuMs,taskReceiptCommittedR31);
            // Empty maintenance work waits for its next data event, never spins to fabricate progress.
            if(delta==0)await(10);
        }}catch(Exception failure){android.util.Log.e("STARCORE_R26","runtime blocked",failure);try{store.setState("r26_runtime_error",failure.toString());}catch(Exception ignored){}}
        finally{beginStopR31();}
    }
    private boolean recordClaimR31(Claim claim,long work,long nextCursor,long cpuMs,long elapsed,String blocked,WaitKindR27.Wait wait,Throwable failure){
        try{store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(controlContextR31("R26_TASK_RECEIPT"),db->{
            requireFence(db,claim);long now=System.currentTimeMillis();FailureDispositionR31.Decision disposition=failure==null?null:FailureDispositionR31.classify(failure);
            String status=blocked==null||blocked.startsWith("COOPERATIVE")?"YIELDED":"BLOCKED";
            if(disposition!=null&&disposition.requiresCommitResolution)status="COMMIT_UNCERTAIN";
            if(disposition!=null&&disposition.disposition==FailureDispositionR31.Disposition.FATAL)status="NEEDS_FIX";
            db.execSQL("UPDATE r26_task SET cursor=CASE WHEN ?>0 THEN max(cursor,?) ELSE cursor END,status=?,lease_until=0,next_at=?,blocked_reason=?,last_progress=CASE WHEN ?>0 THEN ? ELSE last_progress END,steps=steps+CASE WHEN ?>0 THEN 1 ELSE 0 END,last_cpu_ms=?,wait_kind=?,wait_detail=?,wait_since=CASE WHEN wait_kind=? THEN wait_since ELSE ? END,first_progress_at=CASE WHEN first_progress_at=0 AND ?>0 THEN ? ELSE first_progress_at END WHERE task_id=? AND owner=? AND fence=? AND status='RUNNING'",new Object[]{work,nextCursor,status,now+(work>0?0:1000),blocked,work,now,work,cpuMs,wait.kind,wait.detail,wait.kind,now,work,now,claim.task,claim.owner,claim.fence});
            try(Cursor changed=db.rawQuery("SELECT changes()",null)){if(!changed.moveToFirst()||changed.getInt(0)!=1)throw new IllegalStateException("R31_STALE_RECEIPT_IGNORED");}
            releaseLeaseR31(db,claim,work>0?0:1000,false);
            if(disposition!=null&&disposition.consumesFaultBudget())ResearchPoolR27.recordFaultBudgetR31(db,claim,failure,now);
            if(failure==null)ResearchPoolR27.recordDurableRecoveryR31(db,claim);
            db.execSQL("INSERT INTO r26_progress(task_id,fence,cursor_before,cursor_after,cpu_ms,blocked_reason,recorded_at) VALUES(?,?,?,?,?,?,?)",new Object[]{claim.task,claim.fence,claim.cursor,nextCursor,cpuMs,blocked,now});
            db.execSQL("DELETE FROM r26_progress WHERE receipt_id<(SELECT max(receipt_id)-4096 FROM r26_progress)");
            ServiceTotalsR26.record(db,owner,cpuMs,elapsed,elapsed>2*CooperativeContinuationR26.SLICE_MS);
            db.execSQL("UPDATE r26_session SET heartbeat=?,actual_progress=actual_progress+? WHERE session_id=?",new Object[]{now,work,owner});return 0;
        });return true;}catch(RuntimeException receipt){if(FailureDispositionR31.classify(receipt).disposition!=FailureDispositionR31.Disposition.STALE_OWNER)android.util.Log.w("STARCORE_R31","R26 task receipt deferred: "+receipt.getClass().getSimpleName());return false;}
    }
    private volatile RuntimeR27 drainingRuntimeR31;private volatile String stopStateR31="RUNNING";
    private void beginStopR31(){
        synchronized(signal){cancelled=true;}RuntimeR27 runtime=r27;drainingRuntimeR31=runtime;r27=null;if(runtime!=null)runtime.close();
        LegacyGenerationR26.cancel(owner);LegacyReplayContinuationR26.cancel(owner);TaskContinuationR26.cancel(owner);
        // This is the dedicated owner cleanup boundary. Preserve cancellation everywhere else; clear only so
        // the fenced session revocation is allowed to acquire its own short writer transaction.
        boolean interrupted=Thread.interrupted();
        try{store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(controlContextR31("R26_SESSION_STOP"),db->{
            db.execSQL("UPDATE r26_session SET state='DRAINING',user_enabled=0,heartbeat=? WHERE session_id=?",new Object[]{System.currentTimeMillis(),owner});
            db.execSQL("UPDATE r26_task SET status='CANCELLING',lease_until=0,fence=fence+1 WHERE owner=? AND status='RUNNING'",new Object[]{owner});
            db.execSQL("UPDATE r31_task_lease SET deadline_elapsed_ms=0 WHERE owner_epoch=?",new Object[]{owner});return 0;
        });}catch(RuntimeException unavailable){android.util.Log.w("STARCORE_R31","session revocation unacknowledged; old owner still cannot be replaced");}
        long end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        boolean r26Stopped=CooperativeContinuationR26.awaitOwnerStopped(owner,Math.max(0,(end-System.nanoTime())/1_000_000L));
        boolean poolStopped=runtime==null||runtime.pool().awaitResourcesTerminatedR31(Math.max(0,(end-System.nanoTime())/1_000_000L));
        stopStateR31=r26Stopped&&poolStopped?"TERMINATED":"DRAINING_NOT_TERMINATED";
        RuntimePowerLeaseR26 lease=powerLease;if(lease!=null)lease.close();
        if(interrupted)Thread.currentThread().interrupt();
        STOP_REAPER_R31.schedule(()->finishStopR31(),100,java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    private static final java.util.concurrent.ScheduledExecutorService STOP_REAPER_R31=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread thread=new Thread(r,"starcore-r31-session-controller");thread.setDaemon(true);return thread;});
    private void finishStopR31(){
        if(worker.isAlive()||healthProbePendingR31.get()||!healthControlExecutorR31().ownerTerminated("health:"+owner)||!CooperativeContinuationR26.awaitOwnerStopped(owner,0)||drainingRuntimeR31!=null&&!drainingRuntimeR31.pool().resourcesTerminatedR31()){
            stopStateR31="DRAINING_NOT_TERMINATED";STOP_REAPER_R31.schedule(()->finishStopR31(),1,java.util.concurrent.TimeUnit.SECONDS);return;
        }
        stopStateR31="TERMINATED";synchronized(RuntimeCoordinatorR26.class){if(active==this)active=null;}if(closeStore)store.close();
    }
    private boolean resourcesTerminatedR31(){return !worker.isAlive()&&!healthProbePendingR31.get()&&healthControlExecutorR31().ownerTerminated("health:"+owner)&&"TERMINATED".equals(stopStateR31);}
    public String stopStateR31(){return stopStateR31;}
    public java.util.Map<String,Object> stopSnapshotR31(){java.util.Map<String,Object> out=new java.util.LinkedHashMap<>();out.put("owner",owner);out.put("state",stopStateR31);out.put("continuations",ResearchPoolR27.continuationSnapshotsR31(owner));RuntimeR27 runtime=drainingRuntimeR31;if(runtime!=null)out.put("pool",runtime.pool().shutdownSnapshotR31());return out;}
    private void await(long ms){synchronized(signal){try{if(!cancelled)signal.wait(ms);}catch(InterruptedException e){if(!cancelled)Thread.currentThread().interrupt();}}}
    static final long SCHEMA_BATCH_MS=800;
    private long execute(Claim c,BudgetTokenSU1 budget)throws Exception{
        if(c.task.equals("schema")){
            if(strategyStorageReady)return 0;
            if(!StrategySchemaSU1.ready(store.readable())){
                // R27.1: a fresh install has ~265 journaled units; one unit per scheduler pass kept every research task
                // waiting for hours on device. Apply units back-to-back for up to SCHEMA_BATCH_MS per slice (below the
                // 2×SLICE_MS capacity limit; ~54 ms per unit was measured on device); each unit is
                // still its own short transaction (background writer class, so P0 fetch/draw writes go first).
                long until=SystemClock.elapsedRealtime()+SCHEMA_BATCH_MS;int units=0;ResearchSchemaCoordinatorR926.StepResult result;
                do{result=ResearchSchemaCoordinatorR926.ensureStrategyStorageOneStep(store.coordinatorForResearchR952());
                    // R27.2 (device: 152/265 units in 12 min): a busy writer retries inside this slice instead of failing it.
                    if(result.cause!=null&&ResearchWriteQueueR27.writerBusy(result.cause)){SystemClock.sleep(20);continue;}
                    if(result.cause!=null)throw new IllegalStateException("R26_STRATEGY_SCHEMA",result.cause);
                    if(result.status.equals("STRATEGY_UNIT_COMMITTED")||result.ready)units++;else break;
                }while(!result.ready&&!preempt()&&SystemClock.elapsedRealtime()<until);
                c.reason((units>0?"COOPERATIVE_":"")+result.status+":units="+units+":"+result.detail);return units;
            }
            if(!StrategySchemaSU2.ready(store.readable())){store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R26_SU2_SCHEMA"),db->{requireFence(db,c);StrategySchemaSU2.ensure(db);CertificateRevocationR30.ensure(db);ResearchRetentionStoreR30.ensureProtected(db);IncumbentLoopSU2.seedWithin(db,1);return 0;});return 1;}
            ProgramModelSU2.register();store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(controlContextR31("R26_STRATEGY_DEPENDENCIES_READY"),db->{requireFence(db,c);db.execSQL("UPDATE r26_task SET status='READY',next_at=0,blocked_reason=NULL WHERE blocked_reason='WAITING_SELF_CONTAINED_STRATEGY_STORAGE'");return 0;});strategyStorageReady=true;return 1;
        }
        if(c.task.equals("schema-base")){
            if(!strategyStorageReady){c.reason("WAITING_SELF_CONTAINED_STRATEGY_STORAGE");return 0;}
            if(store.coordinatorForResearchR952().isR811ResearchSchemaReady())return 0;
            ResearchSchemaCoordinatorR926.StepResult result=ResearchSchemaCoordinatorR926.ensureOneStepResult(store.coordinatorForResearchR952());c.reason("LEGACY_SCHEMA:"+result.status+":"+ResearchSchemaCoordinatorR926.summary()+":"+result.detail);return result.ready?1:0;
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
            case "feedback":{long bridged=RuntimeSchemaR26.reconcileFeedbackR31(store,c,budget);boolean consumed=ResearchFeedbackSU1.drainOne(store);c.reason("FEEDBACK_DISPATCH_NOT_MODEL_TRAINING:bridge="+bridged+",legacyConsumption="+(consumed?1:0));return bridged+(consumed?1:0);}
            case "ablation":return ModelAblationStoreSU1.advanceOne(store,budget)?1:0;
            case "campaign-audit":return CampaignAuditStoreSU1.advanceOne(store,budget)?1:0;
            case "foundry-su1":return TaskContinuationR26.step(store,c,budget,seq+1);
            default:break;
        }
        if(!c.task.equals("retention")&&!store.coordinatorForResearchR952().isR811ResearchSchemaReady()){c.reason("WAITING_LEGACY_RESEARCH_SCHEMA:"+ResearchSchemaCoordinatorR926.summary());return 0;}
        // A persisted event cursor prevents repeated maintenance over unchanged evidence.
        if(c.cursor>=seq&&!c.task.equals("retention"))return 0;budget.checkpointOrYield();
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
                return retentionStepR30(c,budget,seq);
            }
            case "daily-close":store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R26_DAILY_CLOSE"),db->{requireFence(db,c);LiveShadowLoopSU1.closeDays(db,Integer.parseInt(latest.issue.substring(0,8)));return 0;});break;
            default:throw new IllegalStateException("R26_UNKNOWN_TASK:"+c.task);
        }
        c.completedEventCursor=seq;return 1;
    }
    /** One recoverable maintenance phase per service slice, never all old maintenance inside one writer. */
    private long retentionStepR30(Claim claim,BudgetTokenSU1 budget,long latest)throws Exception{
        SQLiteDatabase read=store.readable();try(Cursor c=read.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='r27_chart_cache'",null)){if(!c.moveToFirst()){claim.reason("WAITING_R27_RETENTION_CHECKPOINT_SCHEMA");return 0;}}
        RetentionR26.Checkpoint checkpoint=RetentionR26.checkpoint(read);if(checkpoint.phase==0&&checkpoint.lastCompletedSequence>=latest){claim.reason("WAITING_NEW_DRAW");return 0;}
        long cycle=checkpoint.phase==0?latest:checkpoint.cycleSequence;
        final RetentionRulesSU2.PreparedFold fold=checkpoint.phase==0?RetentionRulesSU2.prepareFold(read,cycle+1):null;
        budget.checkpointOrYield();
        if(checkpoint.phase==RetentionR26.PHASES_R30_REPAIR+2){ResearchRetentionR30.Result result=ResearchRetentionStoreR30.step(store,claim);claim.reason(result.state);if(result.state.equals("MORE_RECOMPUTABLE_WORK"))return Math.max(1,result.filesDeleted);}
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RETENTION,"R26_RETENTION_PHASE_"+checkpoint.phase),db->{
            requireFence(db,claim);RetentionR26.Checkpoint current=RetentionR26.checkpoint(db);if(!Objects.equals(checkpoint.original,current.original))throw new IllegalStateException("R30_RETENTION_PHASE_SOURCE_CHANGED");
            if(!retentionProtectionReadyR31||!SchemaR27.tableExists(db,"r30_retention_archive_index"))throw new IllegalStateException("SCHEMA_PENDING:RETENTION_PROTECTION");
            if(checkpoint.phase==0)RetentionRulesSU2.commitFold(db,fold,System.currentTimeMillis());
            else if(checkpoint.phase==1)RetentionRulesSU2.pruneResearchGenomes(db);
            else if(checkpoint.phase<RetentionR26.PHASES_R30_REPAIR+2)RetentionR26.pruneStep(db,cycle+1,checkpoint.phase-2);
            RetentionR26.commitCheckpoint(db,checkpoint,cycle,fold==null?null:fold.stepHash,fold==null?-1:fold.throughOrdinal);return 0;
        });
        // Legacy task cursor is a work counter here; typed progress in B03 separates it from the calendar.
        claim.completedEventCursor=claim.cursor+1;claim.reason("COOPERATIVE_RETENTION_PHASE:"+checkpoint.phase);return 1;
    }
}
