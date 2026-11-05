package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import com.ethk3.novaonepro.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** R27-003/005/006/007/009 research thread pool (docs/02 2.2). max(1,min(4,cores−2)) worker threads at
 * THREAD_PRIORITY_BACKGROUND+1, each with its own read-only WAL connection (PRAGMA query_only=1). Workers never
 * look at the realtime-pressure gate while computing; only writes yield (ResearchWriteQueueR27). Session-level
 * blocking exists only for real resources (memory, thermal) and user stop. Inside the last 3 s before a freeze
 * deadline the pool runs with one active worker, never zero. Every slice records a wait kind (WaitKindR27) on its
 * r26_task row (lane 'R27'); errors keep their full stack in r27_task_error and quarantine after 5 per 10 minutes. */
public final class ResearchPoolR27 implements AutoCloseable {
    public interface Task {
        String id();
        /** One bounded slice on the calling worker thread. */
        Outcome step(Worker w) throws Exception;
    }
    public static final class Outcome {
        public final long units; public final String reason; public final long cursor, latest;
        public Outcome(long units, String reason, long cursor, long latest) { this.units = units; this.reason = reason; this.cursor = cursor; this.latest = latest; }
        public static Outcome progress(long units, long cursor) { return new Outcome(units, null, cursor, -1); }
        public static Outcome idle(String reason, long cursor, long latest) { return new Outcome(0, reason, cursor, latest); }
    }
    /** Per-thread context: read-only connection and cancellation. */
    public final class Worker {
        public final SQLiteDatabase ro; public final String name;
        Worker(SQLiteDatabase ro, String name) { this.ro = ro; this.name = name; }
        public boolean cancelled() { RuntimeCoordinatorR26.Claim claim=RuntimeCoordinatorR26.currentClaimR30();Long revoked=claim==null?null:revokedTaskFencesR31.get(claim.task);return closed || Thread.currentThread().isInterrupted()||revoked!=null&&claim.fence<=revoked; }
        public ResearchWriteQueueR27 writes() { return writes; }
        public DbStore store() { return store; }
        public ResearchPoolR27 pool() { return ResearchPoolR27.this; }
    }

    private final DbStore store; private final String session; private final ResearchWriteQueueR27 writes;
    private final List<Task> tasks = new ArrayList<>(); private final Map<String, Boolean> running = new HashMap<>();
    private final Map<String, Long> notBefore = new HashMap<>();
    private final Map<String,PreparedTaskR30R1> prepared = new HashMap<>();
    private final List<Thread> threads = new ArrayList<>(); private final List<SQLiteDatabase> connections = new ArrayList<>();
    private volatile boolean closed; private volatile String resource;
    private final MemoryBudgetR31 memoryBudgetR31=MemoryBudgetR31.processBudget();
    private volatile boolean memoryPressureR31;
    private volatile String shutdownStateR31="RUNNING";
    private final java.util.concurrent.atomic.AtomicBoolean closeControllerR31=new java.util.concurrent.atomic.AtomicBoolean();
    private final Map<String,Integer> retryStreakR31=new HashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String,Long> revokedTaskFencesR31=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<String,ResearchPoolR27> POOLS_R31=new java.util.concurrent.ConcurrentHashMap<>();
    private volatile int parallelismLimit = 2; private volatile String thermalReason = "NORMAL";
    private final AtomicInteger active = new AtomicInteger();
    public final int size;
    private final ExecutorService compute;

    public static final int BACKGROUND_PRIORITY = android.os.Process.THREAD_PRIORITY_BACKGROUND + 1;
    public static int defaultThreads() { int cores = Runtime.getRuntime().availableProcessors(); return Math.max(1, Math.min(2, cores - 1)); }
    private static final int CONTINUATION_LIMIT_R31=4,HELPER_LIMIT_R31=2;
    private static final java.util.Map<String,Boolean> COMPUTE_OWNERS_R31=new java.util.LinkedHashMap<>();
    public static synchronized void installComputeBudgetR31(){
        if(!CooperativeContinuationR26.hasPermitProvider())CooperativeContinuationR26.installPermitProvider((owner,name)->acquireComputeR31(owner+":"+name,false));
    }
    private static CooperativeContinuationR26.ComputePermit acquireComputeR31(String key,boolean helper){
        synchronized(COMPUTE_OWNERS_R31){
            if(COMPUTE_OWNERS_R31.containsKey(key))return null;
            int count=0;for(boolean value:COMPUTE_OWNERS_R31.values())if(value==helper)count++;
            if(count>=(helper?HELPER_LIMIT_R31:CONTINUATION_LIMIT_R31))return null;
            COMPUTE_OWNERS_R31.put(key,helper);
        }
        java.util.concurrent.atomic.AtomicBoolean released=new java.util.concurrent.atomic.AtomicBoolean();
        return ()->{if(released.compareAndSet(false,true))synchronized(COMPUTE_OWNERS_R31){COMPUTE_OWNERS_R31.remove(key);}};
    }
    private static final java.util.concurrent.ConcurrentHashMap<String,HelperWorkR31> HELPER_WORK_R31=new java.util.concurrent.ConcurrentHashMap<>();
    public static final long HELPER_DESCRIPTOR_BYTES_R31=512;
    // Primitive payload widths are fixed; BYTES fields require Android API24, while this app supports21.
    public static final long HELPER_REPLAY_WORKING_ESTIMATE_R31=RuntimeR27.DATASET_ROWS*10L*(3*8L+4*4L);
    private static final java.util.Set<String> CANCELLED_HELPER_GROUPS_R31=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static String helperGroupR31(RuntimeCoordinatorR26.Claim claim){return claim.owner+"|"+claim.task+"|"+claim.fence;}
    static boolean helpersTerminatedR31(RuntimeCoordinatorR26.Claim claim){String group=helperGroupR31(claim);for(HelperWorkR31 work:HELPER_WORK_R31.values())if(group.equals(work.group))return false;return true;}
    static void cancelTaskHelpersR31(RuntimeCoordinatorR26.Claim claim){
        String group=helperGroupR31(claim);CANCELLED_HELPER_GROUPS_R31.add(group);
        for(HelperWorkR31 work:HELPER_WORK_R31.values())if(group.equals(work.group)){
            if(work.body instanceof java.util.concurrent.Future<?>)((java.util.concurrent.Future<?>)work.body).cancel(true);
            if(work.executor.remove(work))work.releaseUnstarted();
        }
    }
    static void clearTaskHelperCancellationR31(RuntimeCoordinatorR26.Claim claim){if(helpersTerminatedR31(claim))CANCELLED_HELPER_GROUPS_R31.remove(helperGroupR31(claim));}
    public static java.util.Map<String,Object> computeBudgetSnapshotR31(){
        java.util.Map<String,Object> out=new java.util.LinkedHashMap<>();
        synchronized(COMPUTE_OWNERS_R31){out.put("activeAndParkedOwners",new java.util.ArrayList<>(COMPUTE_OWNERS_R31.keySet()));out.put("count",COMPUTE_OWNERS_R31.size());}
        java.util.List<Object> helpers=new java.util.ArrayList<>();int queued=0,active=0;long queuedBytes=0,workingBytes=0;
        for(HelperWorkR31 work:HELPER_WORK_R31.values()){
            java.util.Map<String,Object> item=new java.util.LinkedHashMap<>();item.put("task",work.claim.task);item.put("owner",work.claim.owner);item.put("fence",work.claim.fence);item.put("phase",work.phase);item.put("descriptorDeclaredBytes",work.descriptor.bytes());item.put("workingDeclaredBytes",work.working.bytes());helpers.add(item);workingBytes+=work.working.bytes();if("QUEUED".equals(work.phase)){queued++;queuedBytes+=work.descriptor.bytes();}else active++;
        }
        out.put("helpers",helpers);out.put("queuedHelpers",queued);out.put("activeHelpers",active);out.put("queuedDeclaredBytes",queuedBytes);out.put("helperWorkingDeclaredBytes",workingBytes);out.put("workingEstimateBasis","5000_ROWS_X_10_OUTCOMES_X_THREE_DOUBLES_AND_FOUR_INTS; MODEL_PARAMETERS_AND_THREAD_NATIVE_STACKS_NOT_PROFILED");out.put("continuationLimit",CONTINUATION_LIMIT_R31);out.put("helperLimit",HELPER_LIMIT_R31);return out;
    }
    private static final class HelperWorkR31 implements Runnable {
        final Runnable body;final CooperativeContinuationR26.ComputePermit permit;final RuntimeCoordinatorR26.Claim claim;
        final MemoryBudgetR31.Lease descriptor,working;
        final String key,group;final BudgetedHelperExecutorR31 executor;volatile String phase="QUEUED";
        HelperWorkR31(Runnable body,CooperativeContinuationR26.ComputePermit permit,RuntimeCoordinatorR26.Claim claim,String key,BudgetedHelperExecutorR31 executor,MemoryBudgetR31.Lease descriptor,MemoryBudgetR31.Lease working){this.body=body;this.permit=permit;this.claim=claim;this.key=key;this.group=helperGroupR31(claim);this.executor=executor;this.descriptor=descriptor;this.working=working;}
        public void run(){
            phase="ACTIVE";RuntimeCoordinatorR26.bindBackgroundClaim(claim);
            try{if(CANCELLED_HELPER_GROUPS_R31.contains(group)){if(body instanceof java.util.concurrent.Future<?>)((java.util.concurrent.Future<?>)body).cancel(false);return;}body.run();}
            finally{RuntimeCoordinatorR26.unbindBackgroundClaimR31();working.close();descriptor.close();phase="TERMINATED";HELPER_WORK_R31.remove(key,this);permit.close();}
        }
        void releaseUnstarted(){working.close();descriptor.close();phase="TERMINATED";HELPER_WORK_R31.remove(key,this);permit.close();}
    }
    private static final class BudgetedHelperExecutorR31 extends ThreadPoolExecutor{
        final String owner;
        BudgetedHelperExecutorR31(int size,String owner){super(size,size,30,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(2*size),r->{Thread thread=new Thread(()->{try{android.os.Process.setThreadPriority(BACKGROUND_PRIORITY);}catch(RuntimeException unsupported){android.util.Log.w("STARCORE_R31","helper background priority unavailable");}r.run();},"starcore-r31-compute");thread.setDaemon(true);return thread;},new AbortPolicy());this.owner=owner;}
        @Override public void execute(Runnable command){
            RuntimeCoordinatorR26.Claim claim=RuntimeCoordinatorR26.currentClaimR30();
            if(claim==null)throw new RejectedExecutionException("R31_STALE_COMPUTE_HELPER_REQUIRES_CLAIM");
            String group=helperGroupR31(claim);if(CANCELLED_HELPER_GROUPS_R31.contains(group))throw new RejectedExecutionException("R31_CONTINUATION_CANCELLING");
            String key=group+":helper:"+java.util.UUID.randomUUID();CooperativeContinuationR26.ComputePermit permit=acquireComputeR31(key,true);
            if(permit==null)throw new RejectedExecutionException("R31_COMPUTE_CAPACITY_WAIT");
            MemoryBudgetR31 budget=MemoryBudgetR31.processBudget();MemoryBudgetR31.Admission queued;try{queued=budget.tryAcquire(key,claim.fence,MemoryBudgetR31.Kind.QUEUED,HELPER_DESCRIPTOR_BYTES_R31,()->CANCELLED_HELPER_GROUPS_R31.contains(group));}catch(RuntimeException|Error failure){permit.close();throw failure;}
            if(!queued.acquired()){permit.close();throw new RejectedExecutionException("R31_MEMORY_CAPACITY_WAIT:"+queued.reason);}
            MemoryBudgetR31.Admission admitted;try{admitted=budget.tryAcquire(key,claim.fence,MemoryBudgetR31.Kind.RESEARCH,HELPER_REPLAY_WORKING_ESTIMATE_R31,()->CANCELLED_HELPER_GROUPS_R31.contains(group));}catch(RuntimeException|Error failure){queued.lease.close();permit.close();throw failure;}
            if(!admitted.acquired()){queued.lease.close();permit.close();throw new RejectedExecutionException("R31_MEMORY_CAPACITY_WAIT:"+admitted.reason);}
            HelperWorkR31 work=new HelperWorkR31(command,permit,claim,key,this,queued.lease,admitted.lease);HELPER_WORK_R31.put(key,work);
            try{super.execute(work);}catch(RuntimeException|Error failure){work.releaseUnstarted();throw failure;}
        }
        @Override public java.util.List<Runnable> shutdownNow(){java.util.List<Runnable> pending=super.shutdownNow();for(Runnable item:pending)if(item instanceof HelperWorkR31){HelperWorkR31 work=(HelperWorkR31)item;if(work.body instanceof java.util.concurrent.Future<?>)((java.util.concurrent.Future<?>)work.body).cancel(true);work.releaseUnstarted();}return pending;}
    }

    public ResearchPoolR27(DbStore store, String session, int threads) {
        this.store = store; this.session = session; this.size = Math.max(1, threads); this.writes = new ResearchWriteQueueR27(store);
        installComputeBudgetR31();POOLS_R31.put(session,this);
        // Shared helper executor for data-parallel replay inside one task (evolution REPLAY step).
        // R27.2: the helpers run at the workers' background priority. They used to inherit the default priority and,
        // with up to four of them replaying at once, took the CPU from the realtime analysis thread on device.
        compute = new BudgetedHelperExecutorR31(size,session);
    }
    public ResearchWriteQueueR27 writes() { return writes; }
    MemoryBudgetR31 memoryBudgetR31(){return memoryBudgetR31;}
    public void setMemoryPressureR31(boolean pressured){memoryPressureR31=pressured;}
    public ExecutorService compute() { return compute; }
    public synchronized void register(Task t) { for(Task existing:tasks)if(existing.id().equals(t.id()))throw new IllegalArgumentException("DUPLICATE_RESEARCH_OWNER:"+t.id());tasks.add(t); running.put(t.id(), false); }
    public void setResource(String r) { resource = r; }
    public void setThermalLimit(int workers, String reason) { parallelismLimit = Math.max(1, Math.min(size, workers)); thermalReason = reason; }
    public int workerLimit() { return parallelismLimit; }
    public String thermalReason() { return thermalReason; }

    public void start() {
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(RuntimeCoordinatorR26.controlContextR31("R31_REGISTER_RESEARCH_TASKS"),db->{
            long now=System.currentTimeMillis();for(Task task:tasks)db.execSQL("INSERT OR IGNORE INTO r26_task(task_id,lane,mechanism,status,registered_at) VALUES(?,'R27',?,'READY',?)",new Object[]{task.id(),task.id(),now});return 0;
        });
        String path = store.contextForR26().getDatabasePath(AppConfig.DB_NAME).getPath();
        for (int i = 0; i < size; i++) {
            SQLiteDatabase ro = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
            try (Cursor c = ro.rawQuery("PRAGMA query_only=1", null)) { c.moveToFirst(); } catch (RuntimeException ignored) { }
            connections.add(ro);
            final Worker w = new Worker(ro, "starcore-r27-research-" + i);
            Thread t = new Thread(() -> loop(w), w.name); t.setDaemon(true); threads.add(t); t.start();
        }
    }

    private void loop(Worker w) {
        android.os.Process.setThreadPriority(BACKGROUND_PRIORITY);
        long cpuStart=android.os.Debug.threadCpuTimeNanos(),wallStart=SystemClock.elapsedRealtime(),flushed=wallStart,slices=0;
        while(!closed&&!Thread.currentThread().isInterrupted()){
            try{
                CooperativeContinuationR26.reapTerminated();
                recoverOneR31(store,session);
                if(resource!=null){sleep(500);continue;}
                int limit=narrow(System.currentTimeMillis())?1:Math.min(size,parallelismLimit);
                if(!ResearchSlotsR29.tryEnter(active,limit)){sleep(100);continue;}
                Task task;
                try{task=next();}catch(Throwable failure){active.decrementAndGet();throw failure;}
                if(task==null){active.decrementAndGet();sleep(250);continue;}
                Outcome outcome=null;Throwable failure=null;ResourceBudget applied=null;RuntimeCoordinatorR26.Claim claim=null;String phase="CLAIM";
                long cpu=android.os.Debug.threadCpuTimeNanos();PreparedTaskR30R1 current;
                synchronized(this){current=prepared.get(task.id());}
                try{
                    if(current!=null&&current.state()==CooperativeContinuationR26.State.CANCELLING){
                        if(!current.isTerminated()){outcome=Outcome.idle("DEPENDENCY(DRAINING_NOT_TERMINATED)",current.claim.cursor,-1);synchronized(this){notBefore.put(task.id(),SystemClock.elapsedRealtime()+250);}continue;}
                        clearTaskHelperCancellationR31(current.claim);synchronized(this){prepared.remove(task.id(),current);}invalidateDraftR31(task.id());finishCancelledR31(current.claim);current=null;
                    }
                    if(current!=null&&current.done()){
                        current.awaitTermination(50);
                        if(!current.isTerminated()){outcome=Outcome.idle("DEPENDENCY(RESULT_CLEANUP_PENDING)",current.claim.cursor,-1);synchronized(this){notBefore.put(task.id(),SystemClock.elapsedRealtime()+50);}continue;}
                        clearTaskHelperCancellationR31(current.claim);synchronized(this){prepared.remove(task.id(),current);}if(current.failedR31){invalidateDraftR31(task.id());finishCancelledR31(current.claim);}current=null;
                    }
                    ClaimAttempt attempt=claimPrepared(task.id(),current);claim=attempt.claim;phase=attempt.phase;
                    if(claim==null){outcome=Outcome.idle("DEPENDENCY(ACTIVE_SCHEMA_SESSION_TASK_LEASE_OR_RECOVERY)",0,-1);}
                    else{
                        if(current==null){current=new PreparedTaskR30R1(task,w,claim);synchronized(this){prepared.put(task.id(),current);}}
                        current.claim=claim;phase="COMPUTE_OR_PUBLISH";
                        refreshResourceBudget(w.ro);applied=resourceBudget;int quantum=Math.min(applied.quantums.getOrDefault(task.id(),200),memoryPressureR31?100:200);
                        try{outcome=current.slice(quantum);}finally{
                            recordResearchCost(task.id(),current.lastCpuNanos);
                            if(current.done()&&!current.failedR31){current.awaitTermination(50);if(current.isTerminated()){clearTaskHelperCancellationR31(current.claim);synchronized(this){prepared.remove(task.id(),current);}}}
                        }
                    }
                }catch(Throwable error){
                    failure=error;FailureDispositionR31.Decision d=FailureDispositionR31.classify(error);
                    if(current!=null&&(d.disposition==FailureDispositionR31.Disposition.STALE_OWNER||d.disposition==FailureDispositionR31.Disposition.CANCELLED||d.disposition==FailureDispositionR31.Disposition.FATAL||current.state()==CooperativeContinuationR26.State.CANCELLING)){current.failedR31=true;current.close();}
                }finally{active.decrementAndGet();synchronized(this){running.put(task.id(),false);}}
                slices++;
                record(task.id(),outcome,failure,applied,claim,phase,Math.max(0,android.os.Debug.threadCpuTimeNanos()-cpu)/1_000_000L);
                FailureDispositionR31.Decision disposition=failure==null?null:FailureDispositionR31.classify(failure);
                long delay=outcome!=null&&"COOPERATIVE_PREPARED_STEP".equals(outcome.reason)?25:1000;
                synchronized(this){
                    if(disposition!=null&&disposition.disposition==FailureDispositionR31.Disposition.RETRY_LATER){int streak=retryStreakR31.getOrDefault(task.id(),0)+1;retryStreakR31.put(task.id(),streak);delay=FailureDispositionR31.backoffMillis(streak,task.id());}
                    else if(failure==null&&outcome!=null&&outcome.units>0)retryStreakR31.remove(task.id());
                    notBefore.put(task.id(),SystemClock.elapsedRealtime()+delay);
                }
                Error processFatal=FailureDispositionR31.processFatalCause(failure);if(processFatal!=null)throw processFatal;
            }catch(VirtualMachineError|ThreadDeath|LinkageError fatal){closed=true;throw fatal;}
            catch(RuntimeException unexpected){android.util.Log.w("STARCORE_R31","worker control deferred "+w.name+":"+unexpected.getClass().getSimpleName());sleep(500);}
            long now=SystemClock.elapsedRealtime();if(now-flushed>15_000){flushCpu(w.name,android.os.Debug.threadCpuTimeNanos()-cpuStart,now-wallStart,slices);flushed=now;}
        }
    }
    static final class ClaimAttempt {
        final RuntimeCoordinatorR26.Claim claim;final String phase;
        ClaimAttempt(RuntimeCoordinatorR26.Claim claim,String phase){this.claim=claim;this.phase=phase;}
    }
    private ClaimAttempt claimPrepared(String id,PreparedTaskR30R1 current){
        RuntimeCoordinatorR26.Claim claimed=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(RuntimeCoordinatorR26.controlContextR31("R30_PREPARED_TASK_CLAIM"),db->{
            if(!ResearchStoreR30R1.ready(db))return null;
            return RuntimeCoordinatorR26.claimWithinR31(db,id,session,"R27",current==null?null:current.claim);
        });
        return new ClaimAttempt(claimed,claimed==null?"CLAIM_WAIT":"CLAIMED");
    }

    /** Freeze window or active P0 work: the pool runs with one worker (R27.2). */
    boolean narrow(long now) { return parallelismLimit == 1 || writes.freezeWindow(now) || ResearchWriteQueueR27.p0Writing(); }

    private synchronized Task next() {
        long now = SystemClock.elapsedRealtime();
        for (Task t : tasks) {
            if (Boolean.TRUE.equals(running.get(t.id()))) continue;
            Long nb = notBefore.get(t.id()); if (nb != null && nb > now) continue;
            running.put(t.id(), true); notBefore.remove(t.id());
            // Round-robin fairness: move the claimed task to the end.
            tasks.remove(t); tasks.add(t);
            return t;
        }
        return null;
    }
    public synchronized void wake(String id) { notBefore.remove(id); }

    public static final String TASK_RECEIPT_CAS_R31="UPDATE r26_task SET status=?,lease_until=?,wait_kind=?,wait_detail=?,blocked_reason=?,wait_since=CASE WHEN wait_kind=? THEN wait_since ELSE ? END,last_served=? WHERE task_id=? AND owner=? AND fence=? AND status='RUNNING'";
    private void record(String id,Outcome outcome,Throwable failure,ResourceBudget applied,RuntimeCoordinatorR26.Claim claim,String phase,long cpuMs){
        WaitKindR27.Wait wait=WaitKindR27.classify(outcome==null?0:outcome.units,outcome==null?null:outcome.reason,failure,outcome==null?0:outcome.cursor,outcome==null?-1:outcome.latest,resource,false);
        long now=System.currentTimeMillis();boolean receiptCommitted=false;
        // An unclaimed attempt has no authority to change a task's status or error budget.
        if(claim!=null)try{
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(RuntimeCoordinatorR26.controlContextR31("R27_TASK_RECEIPT"),db->{
                RuntimeCoordinatorR26.requireFence(db,claim);
                boolean parked=outcome!=null&&"COOPERATIVE_PREPARED_STEP".equals(outcome.reason);
                FailureDispositionR31.Decision disposition=failure==null?null:FailureDispositionR31.classify(failure);
                String status=parked?"RUNNING":WaitKindR27.PROGRESS.equals(wait.kind)?"YIELDED":"BLOCKED";
                if(disposition!=null&&disposition.requiresCommitResolution)status="COMMIT_UNCERTAIN";
                if(disposition!=null&&disposition.disposition==FailureDispositionR31.Disposition.FATAL)status="NEEDS_FIX";
                db.execSQL(TASK_RECEIPT_CAS_R31,new Object[]{status,parked?now+30_000:0,wait.kind,wait.detail,wait.blockedReason(),wait.kind,now,now,id,claim.owner,claim.fence});
                try(Cursor changed=db.rawQuery("SELECT changes()",null)){if(!changed.moveToFirst()||changed.getInt(0)!=1)throw new IllegalStateException("R31_STALE_RECEIPT_IGNORED");}
                RuntimeCoordinatorR26.releaseLeaseR31(db,claim,outcome!=null&&outcome.units>0?0:1000,parked);
                if(disposition!=null&&disposition.consumesFaultBudget())recordFaultBudgetR31(db,claim,failure,now);
                if(failure==null){recordResourceUse(db,id,now,applied);recordDurableRecoveryR31(db,claim);}
                return 0;
            });receiptCommitted=true;
        }catch(RuntimeException receipt){if(FailureDispositionR31.classify(receipt).disposition!=FailureDispositionR31.Disposition.STALE_OWNER)android.util.Log.w("STARCORE_R31","task receipt deferred "+id+":"+receipt.getClass().getSimpleName());}
        observeAttemptR31(store,id,session,claim,phase,failure,outcome==null?0:outcome.units,cpuMs,receiptCommitted);
    }
    static void recordFaultBudgetR31(SQLiteDatabase db,RuntimeCoordinatorR26.Claim claim,Throwable failure,long now){
        long window=0,count=0;try(Cursor row=db.rawQuery("SELECT error_window_start,error_count FROM r26_task WHERE task_id=? AND owner=? AND fence=?",new String[]{claim.task,claim.owner,String.valueOf(claim.fence)})){if(!row.moveToFirst())throw new IllegalStateException("R31_STALE_ERROR_RECEIPT");window=row.getLong(0);count=row.getLong(1);}
        db.execSQL("INSERT OR IGNORE INTO r31_task_control(task_id) VALUES(?)",new Object[]{claim.task});
        long windowMono=0;String windowBoot="";try(Cursor row=db.rawQuery("SELECT fault_window_boot_id,fault_window_elapsed_ms FROM r31_task_control WHERE task_id=?",new String[]{claim.task})){if(row.moveToFirst()){windowBoot=row.getString(0);windowMono=row.getLong(1);}}
        long mono=SystemClock.elapsedRealtime();boolean newWindow=!IssueTimelineWriterR30.bootId().equals(windowBoot)||windowMono==0||mono-windowMono>WaitKindR27.ERROR_WINDOW_MS;
        long[] errors={newWindow?now:window,newWindow?1:count+1};
        db.execSQL("UPDATE r31_task_control SET fault_window_boot_id=?,fault_window_elapsed_ms=? WHERE task_id=?",new Object[]{IssueTimelineWriterR30.bootId(),newWindow?mono:windowMono,claim.task});
        RuntimeFaultEvidenceR31 evidence=RuntimeFaultEvidenceR31.capture(failure,"TASK");
        long level=0,rounds=0;String fingerprint="",probeOwner=null;long probeFence=0;
        try(Cursor row=db.rawQuery("SELECT level,fault_rounds,fingerprint,half_open_owner,half_open_fence FROM r31_task_control WHERE task_id=?",new String[]{claim.task})){if(row.moveToFirst()){level=row.getLong(0);rounds=row.getLong(1);fingerprint=row.getString(2);probeOwner=row.getString(3);probeFence=row.getLong(4);}}
        boolean probe=claim.owner.equals(probeOwner)&&claim.fence==probeFence,quarantine=WaitKindR27.quarantine(errors[1])||probe;
        if(quarantine){boolean same=evidence.fingerprint.equals(fingerprint);level=same?level+1:1;rounds=same?rounds+1:1;}
        String status=quarantine?(rounds>=6?"NEEDS_FIX":"QUARANTINED"):"BLOCKED";
        db.execSQL("UPDATE r26_task SET error_window_start=?,error_count=?,status=?,wait_kind=CASE WHEN ? THEN ? ELSE wait_kind END,blocked_reason=? WHERE task_id=? AND owner=? AND fence=?",
                new Object[]{errors[0],errors[1],status,quarantine?1:0,status,"FAULT:"+evidence.category+":"+evidence.fingerprint,claim.task,claim.owner,claim.fence});
        if(quarantine){long delay=FailureDispositionR31.quarantineMillis((int)level);db.execSQL("UPDATE r31_task_control SET quarantine_until=?,level=?,fingerprint=?,half_open_owner=NULL,half_open_fence=0,quarantine_boot_id=?,quarantine_deadline_elapsed_ms=?,fault_rounds=?,generation=generation+1 WHERE task_id=?",new Object[]{now+delay,level,evidence.fingerprint,IssueTimelineWriterR30.bootId(),SystemClock.elapsedRealtime()+delay,rounds,claim.task});}
        db.execSQL("INSERT INTO r27_task_error(task_id,error_class,message,stack,recorded_at) VALUES(?,?,?,?,?)",new Object[]{claim.task,failure.getClass().getName(),RuntimeFaultEvidenceR31.clean(failure.getMessage(),1024),RuntimeFaultEvidenceR31.clean(evidence.stack,2048),now});
        db.execSQL("DELETE FROM r27_task_error WHERE error_id<(SELECT max(error_id)-2000 FROM r27_task_error)");
    }
    static void recordDurableRecoveryR31(SQLiteDatabase db,RuntimeCoordinatorR26.Claim claim){
        String artifact=null;try(Cursor row=db.rawQuery("SELECT artifact_identity FROM r30r1_task_progress WHERE task_id=? AND session_id=? AND fence=?",new String[]{claim.task,claim.owner,String.valueOf(claim.fence)})){if(row.moveToFirst())artifact=row.getString(0);}
        if(artifact==null||artifact.isEmpty()||artifact.equals(claim.artifactAtClaimR31))return;
        db.execSQL("UPDATE r31_task_control SET level=0,fault_rounds=0,quarantine_until=0,quarantine_deadline_elapsed_ms=0,half_open_owner=NULL,half_open_fence=0,last_success_artifact_hash=? WHERE task_id=?",new Object[]{artifact,claim.task});
        db.execSQL("UPDATE r26_task SET error_count=0,error_window_start=0 WHERE task_id=? AND owner=? AND fence=?",new Object[]{claim.task,claim.owner,claim.fence});
    }
    /** Manual recovery first revokes the old fence. A still-live calculation remains CANCELLING. */
    public static void retry(DbStore store,String taskId){
        boolean live=false;for(ResearchPoolR27 pool:POOLS_R31.values()){synchronized(pool){PreparedTaskR30R1 task=pool.prepared.get(taskId);if(task!=null){task.failedR31=true;if(!task.isTerminated()){task.close();live=true;}}}}
        final boolean draining=live;
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(RuntimeCoordinatorR26.controlContextR31("R27_TASK_RETRY"),db->{
            db.execSQL("UPDATE r26_task SET fence=fence+1,status=?,lease_until=0,wait_kind=?,blocked_reason=? WHERE task_id=? AND lane='R27'",new Object[]{draining?"CANCELLING":"READY",draining?"CANCELLED":"HALF_OPEN",draining?"DRAINING_NOT_TERMINATED":"MANUAL_FENCED_RETRY",taskId});
            db.execSQL("UPDATE r31_task_lease SET deadline_elapsed_ms=0,next_elapsed_ms=0 WHERE task_id=?",new Object[]{taskId});
            db.execSQL("UPDATE r31_task_control SET half_open_owner=NULL,half_open_fence=0,quarantine_until=0,quarantine_deadline_elapsed_ms=0,generation=generation+1 WHERE task_id=?",new Object[]{taskId});return 0;
        });
        for(ResearchPoolR27 pool:POOLS_R31.values())pool.wake(taskId);
    }
    private void finishCancelledR31(RuntimeCoordinatorR26.Claim retired){
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(RuntimeCoordinatorR26.controlContextR31("R31_TASK_DRAINED"),db->{db.execSQL("UPDATE r26_task SET status='READY',wait_kind='HALF_OPEN',blocked_reason='PREVIOUS_COMPUTE_TERMINATED' WHERE task_id=? AND owner=? AND fence=? AND status='CANCELLING' AND EXISTS(SELECT 1 FROM r26_session WHERE session_id=? AND user_enabled=1 AND state='RUNNING')",new Object[]{retired.task,session,retired.fence+1,session});return 0;});
    }
    /** Health control revokes only the observed owner/fence, never a newer task generation. */
    public boolean requestTaskRecoveryR31(String task,long expectedFence,String expectedArtifact,String reason){
        final boolean live;synchronized(this){PreparedTaskR30R1 current=prepared.get(task);live=Boolean.TRUE.equals(running.get(task))||current!=null&&!current.isTerminated();}
        Integer changed=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(RuntimeCoordinatorR26.controlContextR31("R31_HEALTH_TASK_REVOKE"),db->{
            db.execSQL("UPDATE r26_task SET fence=fence+1,status=?,lease_until=0,wait_kind='CANCELLED',blocked_reason=? WHERE task_id=? AND owner=? AND fence=? AND status IN ('RUNNING','READY','YIELDED','BLOCKED') AND NOT EXISTS(SELECT 1 FROM r30r1_task_progress p WHERE p.task_id=r26_task.task_id AND p.artifact_identity IS NOT ?)",new Object[]{live?"CANCELLING":"READY","HEALTH_RECOVERY:"+RuntimeFaultEvidenceR31.clean(reason,128),task,session,expectedFence,expectedArtifact});
            int rows;try(Cursor count=db.rawQuery("SELECT changes()",null)){rows=count.moveToFirst()?count.getInt(0):0;}if(rows==1)db.execSQL("UPDATE r31_task_lease SET deadline_elapsed_ms=0,next_elapsed_ms=0 WHERE task_id=? AND owner_epoch=? AND fence=?",new Object[]{task,session,expectedFence});return rows;
        });
        if(changed==null||changed!=1)return false;
        revokedTaskFencesR31.put(task,expectedFence);
        synchronized(this){PreparedTaskR30R1 current=prepared.get(task);if(current!=null&&current.claim.fence==expectedFence){current.failedR31=true;current.close();}}
        wake(task);return true;
    }
    /** Fallback for a revoke between dispatch and PreparedTask registration. No replacement while either is live. */
    public boolean reconcileTaskRecoveryR31(String task,long expectedFence){
        PreparedTaskR30R1 previous;synchronized(this){if(Boolean.TRUE.equals(running.get(task)))return false;previous=prepared.get(task);if(previous!=null&&!previous.isTerminated())return false;running.put(task,true);if(previous!=null)prepared.remove(task,previous);}
        try{
            if(previous!=null){clearTaskHelperCancellationR31(previous.claim);invalidateDraftR31(task);}
            Integer result=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(RuntimeCoordinatorR26.controlContextR31("R31_HEALTH_TASK_DRAINED"),db->{db.execSQL("UPDATE r26_task SET status='READY',wait_kind='HALF_OPEN',blocked_reason='ACTUAL_PREVIOUS_TASK_TERMINATED' WHERE task_id=? AND owner=? AND fence=? AND status='CANCELLING'",new Object[]{task,session,expectedFence});try(Cursor count=db.rawQuery("SELECT changes()",null)){return count.moveToFirst()?count.getInt(0):0;}});
            if(result!=null&&result==1){revokedTaskFencesR31.remove(task);wake(task);return true;}return false;
        }finally{synchronized(this){running.put(task,false);}}
    }
    private void invalidateDraftR31(String task){RuntimeR27 runtime=RuntimeR27.active();if(runtime!=null&&runtime.pool()==this)runtime.invalidateTaskDraftR31(task);}
    private static final java.util.concurrent.atomic.AtomicLong RECOVERY_POLL_R31=new java.util.concurrent.atomic.AtomicLong();
    /** One fenced half-open probe per second, shared by R26 and R27, driven by a boot-scoped clock. */
    static void recoverOneR31(DbStore store,String owner){
        long mono=SystemClock.elapsedRealtime(),last=RECOVERY_POLL_R31.get();if(mono-last<1000||!RECOVERY_POLL_R31.compareAndSet(last,mono))return;
        try{store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(RuntimeCoordinatorR26.controlContextR31("R31_TASK_RECOVERY_CONTROL"),db->{
            String boot=IssueTimelineWriterR30.bootId();
            try(Cursor rate=db.rawQuery("SELECT boot_id,last_recovery_elapsed_ms FROM r31_recovery_clock WHERE singleton=1",null)){if(rate.moveToFirst()&&boot.equals(rate.getString(0))&&mono-rate.getLong(1)<1000)return 0;}
            try(Cursor task=db.rawQuery("SELECT t.task_id,t.owner,t.fence,c.quarantine_boot_id,c.quarantine_deadline_elapsed_ms,c.level FROM r26_task t JOIN r31_task_control c ON c.task_id=t.task_id WHERE t.status='QUARANTINED' ORDER BY c.last_recovery_at,t.task_id LIMIT 64",null)){
                while(task.moveToNext()){
                    String id=task.getString(0);boolean oldLive=false;
                    for(ResearchPoolR27 pool:POOLS_R31.values())synchronized(pool){PreparedTaskR30R1 current=pool.prepared.get(id);if(current!=null&&!current.isTerminated())oldLive=true;}
                    if(oldLive)continue;
                    if(!boot.equals(task.getString(3))){long delay=FailureDispositionR31.quarantineMillis(Math.max(1,task.getInt(5)));db.execSQL("UPDATE r31_task_control SET quarantine_boot_id=?,quarantine_deadline_elapsed_ms=?,quarantine_until=? WHERE task_id=?",new Object[]{boot,mono+delay,System.currentTimeMillis()+delay,id});continue;}
                    if(task.getLong(4)>mono)continue;
                    db.execSQL("UPDATE r26_task SET status='READY',owner=NULL,fence=fence+1,lease_until=0,wait_kind='HALF_OPEN',wait_detail='ONE_FENCED_RECOVERY_PROBE',blocked_reason='HALF_OPEN_PENDING_DURABLE_PROGRESS' WHERE task_id=? AND owner IS ? AND fence=? AND status='QUARANTINED'",new Object[]{id,task.getString(1),task.getLong(2)});
                    try(Cursor changed=db.rawQuery("SELECT changes()",null)){if(!changed.moveToFirst()||changed.getInt(0)!=1)continue;}
                    db.execSQL("UPDATE r31_task_control SET half_open_owner=?,half_open_fence=0,last_recovery_at=?,generation=generation+1 WHERE task_id=?",new Object[]{owner,System.currentTimeMillis(),id});
                    db.execSQL("UPDATE r31_task_lease SET deadline_elapsed_ms=0,next_elapsed_ms=0 WHERE task_id=?",new Object[]{id});
                    db.execSQL("INSERT OR REPLACE INTO r31_recovery_clock VALUES(1,?,?,?)",new Object[]{boot,mono,owner});
                    return 1;
                }
            }return 0;
        });}catch(RuntimeException unavailable){/* Pending schema/busy remains observable in task rows; no recursive fault receipt. */}
    }
    public boolean quarantined(String id) {
        try (Cursor c = store.readable().rawQuery("SELECT 1 FROM r26_task WHERE task_id=? AND status=?", new String[] {id, WaitKindR27.QUARANTINED})) { return c.moveToFirst(); }
        catch (RuntimeException e) { return false; }
    }

    private void flushCpu(String thread, long cpuNs, long wallMs, long slices) {
        final long cpu = cpuNs / 1_000_000L, now = System.currentTimeMillis();
        try {
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.TELEMETRY,"R27_THREAD_CPU"), db -> {
                db.execSQL("INSERT OR IGNORE INTO r27_thread_cpu(session_id,thread_name,updated_at) VALUES(?,?,?)", new Object[] {session, thread, now});
                db.execSQL("UPDATE r27_thread_cpu SET cpu_ms=?,wall_ms=?,slices=?,updated_at=? WHERE session_id=? AND thread_name=?", new Object[] {cpu, wallMs, slices, now, session, thread});
                return 0;
            });
        } catch (RuntimeException busy) { android.util.Log.w("STARCORE_R27", "cpu receipt deferred", busy); }
        writes.flush();
        HoldLedgerFlushR30.maybeFlush(store); // R30-B01-02
    }

    static String stack(Throwable t) { return RuntimeFaultEvidenceR31.capture(t, "UNKNOWN").stack; }
    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    /** Cancellation requests return immediately; a single control reaper owns resources until actual exit. */
    @Override public void close(){
        if(!closeControllerR31.compareAndSet(false,true))return;
        closed=true;shutdownStateR31="CANCELLING";
        synchronized(this){for(PreparedTaskR30R1 task:prepared.values())task.close();}
        for(Thread thread:threads)thread.interrupt();
        compute.shutdownNow();
        DRAINER_R31.execute(()->{
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
            while(System.nanoTime()<end&&!tryFinishResourcesR31()){try{Thread.sleep(50);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();break;}}
            if(!resourcesTerminatedR31())shutdownStateR31="DRAINING_NOT_TERMINATED";
        });
    }
    private static final java.util.concurrent.ScheduledExecutorService DRAINER_R31;
    static{
        DRAINER_R31=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread thread=new Thread(r,"starcore-r31-resource-controller");thread.setDaemon(true);return thread;});
        DRAINER_R31.scheduleWithFixedDelay(()->{for(ResearchPoolR27 pool:POOLS_R31.values())if(pool.closed)try{pool.tryFinishResourcesR31();}catch(RuntimeException ignored){pool.shutdownStateR31="DRAINING_NOT_TERMINATED";}},1,1,TimeUnit.SECONDS);
    }
    private boolean tryFinishResourcesR31(){
        CooperativeContinuationR26.reapTerminated();
        for(Thread thread:threads)if(thread.isAlive())return false;
        if(!compute.isTerminated())return false;
        if(!CooperativeContinuationR26.awaitOwnerStopped("r30-r27:"+session,0))return false;
        synchronized(this){for(PreparedTaskR30R1 task:prepared.values())if(!task.isTerminated())return false;for(PreparedTaskR30R1 task:prepared.values())clearTaskHelperCancellationR31(task.claim);prepared.clear();}
        synchronized(connections){for(SQLiteDatabase connection:connections)try{connection.close();}catch(RuntimeException ignored){}connections.clear();}
        shutdownStateR31="TERMINATED";POOLS_R31.remove(session,this);return true;
    }
    public boolean awaitResourcesTerminatedR31(long millis){long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(Math.max(0,millis));do{if(resourcesTerminatedR31())return true;try{Thread.sleep(10);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();return false;}}while(System.nanoTime()<end);return resourcesTerminatedR31();}
    public boolean resourcesTerminatedR31(){return "TERMINATED".equals(shutdownStateR31);}
    public String shutdownStateR31(){return shutdownStateR31;}
    public java.util.Map<String,Object> shutdownSnapshotR31(){java.util.Map<String,Object> out=new java.util.LinkedHashMap<>();out.put("state",shutdownStateR31);out.put("owner",session);out.put("compute",computeBudgetSnapshotR31());out.put("continuations",continuationSnapshotsR31("r30-r27:"+session));out.put("readConnectionsReleased",resourcesTerminatedR31());return out;}
    static java.util.List<Object> continuationSnapshotsR31(String owner){
        java.util.List<Object> out=new java.util.ArrayList<>();for(CooperativeContinuationR26.DiagnosticSnapshot snapshot:CooperativeContinuationR26.snapshots(owner)){
            java.util.Map<String,Object> row=new java.util.LinkedHashMap<>(snapshot.asMap());row.put("phase",RuntimeFaultEvidenceR31.clean(snapshot.phase,128));row.put("checkpoint",RuntimeFaultEvidenceR31.clean(snapshot.checkpoint,512));row.put("cleanupFailure",RuntimeFaultEvidenceR31.clean(snapshot.cleanupFailure,512));java.util.List<String> stack=new java.util.ArrayList<>();for(String frame:snapshot.stack)stack.add(RuntimeFaultEvidenceR31.clean(frame,512));row.put("stack",stack);out.add(row);
        }return out;
    }
    public boolean isClosed() { return closed; }
    static final class ResourceBudget {final String run;final long from,through;final java.util.Map<String,Integer> quantums;ResourceBudget(String run,long from,long through,java.util.Map<String,Integer> q){this.run=run;this.from=from;this.through=through;quantums=java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(q));}}
    private volatile ResourceBudget resourceBudget=new ResourceBudget(null,0,0,java.util.Collections.emptyMap());private final java.util.concurrent.atomic.AtomicLong lastResourceRead=new java.util.concurrent.atomic.AtomicLong();
    @SuppressWarnings("unchecked") static java.util.Map<String,Integer> resourceQuantums(java.util.List<?> costs,String session){java.util.Map<String,Long> observed=new java.util.LinkedHashMap<>();long total=0;for(Object o:costs)if(o instanceof java.util.Map){java.util.Map<?,?> row=(java.util.Map<?,?>)o;if(session.equals(row.get("session"))&&row.get("actualContinuationCpuNs") instanceof Number&&row.get("task") instanceof String){long cpu=Math.max(0,((Number)row.get("actualContinuationCpuNs")).longValue());observed.put((String)row.get("task"),cpu);total+=cpu;}}java.util.Map<String,Integer> out=new java.util.LinkedHashMap<>();for(java.util.Map.Entry<String,Long> e:observed.entrySet())out.put(e.getKey(),observed.size()>1&&total>=100000000L&&e.getValue()/(double)total>Math.min(.5,2.0/observed.size())?100:200);return out;}
    @SuppressWarnings("unchecked") private void refreshResourceBudget(SQLiteDatabase db){long now=SystemClock.elapsedRealtime(),before=lastResourceRead.get();if(now-before<60000||!lastResourceRead.compareAndSet(before,now))return;try(Cursor q=db.rawQuery("SELECT r.run_id,r.summary_json,r.contract_hash,r.from_ordinal,r.through_ordinal FROM r30r1_research_measure m JOIN r30r1_analysis_run r ON r.run_id=m.run_id WHERE m.kind='MATRIX_RESOURCE_EFFICIENCY' AND r.role='SELECT' AND r.status='COMMITTED' AND r.source_commit=? ORDER BY r.recorded_at DESC LIMIT 1",new String[]{com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26})){if(!q.moveToFirst()||!ResearchStoreR30R1.currentSource(com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26,q.getString(1)))return;java.util.Map<String,Object> source=(java.util.Map<String,Object>)JsonR27.parse(q.getString(1));if(!(source.get("frame") instanceof java.util.Map)||!StatsR27.sha256(JsonR27.write(source.get("frame"))).equals(q.getString(2)))throw new IllegalStateException("RESOURCE_SOURCE_HASH");java.util.Map<?,?> frame=(java.util.Map<?,?>)source.get("frame");Object detail=frame.get("details");Object raw=detail instanceof java.util.Map?((java.util.Map<?,?>)detail).get("actualTaskCosts"):null;if(!(raw instanceof java.util.List))return;java.util.Map<String,Integer> selected=resourceQuantums((java.util.List<?>)raw,session);if(!selected.isEmpty())resourceBudget=new ResourceBudget(q.getString(0),q.getLong(3),q.getLong(4),selected);}catch(RuntimeException sourceUnavailable){android.util.Log.w("STARCORE_R30","resource analysis unavailable; retain last legal bounded compute quantum",sourceUnavailable);}}
    private void recordResourceUse(SQLiteDatabase db,String task,long now,ResourceBudget budget){if(budget==null)return;if(budget.run==null||!budget.quantums.containsKey(task))return;java.util.Map<String,Object> receipt=new java.util.LinkedHashMap<>();receipt.put("task",task);receipt.put("session",session);receipt.put("actualComputeQuantumMs",budget.quantums.get(task));receipt.put("maximumQuantumMs",200);receipt.put("minimumQuantumMs",100);receipt.put("writerRetryMaximumMs",200);receipt.put("originalDeadlineAndThermalGuards",true);receipt.put("scope","ACTUAL_SCHEDULER_COMPUTE_QUANTUM_APPLICATION_NOT_MODEL_QUALITY_OR_TASK_COMPLETION");db.execSQL("INSERT OR IGNORE INTO r30r1_research_measure SELECT ?,?,?,?,?,?,?,?,?,?,?,? WHERE EXISTS(SELECT 1 FROM r30r1_analysis_run WHERE run_id=? AND role='SELECT' AND status='COMMITTED')",new Object[]{budget.run,"QUANTUM_APPLIED:"+session+":"+task,"RESOURCE_QUANTUM_APPLICATION",StatsR27.sha256(task),"CURRENT_ORIGINAL_POOL_SESSION",budget.from,budget.through,0,null,null,null,JsonR27.write(receipt),budget.run});}
    private final java.util.Map<String,long[]> researchCpu=new java.util.LinkedHashMap<>();
    private void recordResearchCost(String task,long cpu){synchronized(researchCpu){long[] v=researchCpu.computeIfAbsent(task,k->new long[2]);v[0]+=Math.max(0,cpu);v[1]++;}}
    public java.util.List<java.util.Map<String,Object>> researchCosts(){java.util.List<java.util.Map<String,Object>> out=new java.util.ArrayList<>();synchronized(researchCpu){for(java.util.Map.Entry<String,long[]> e:researchCpu.entrySet()){java.util.Map<String,Object> r=new java.util.LinkedHashMap<>();r.put("task",e.getKey());r.put("session",session);r.put("actualContinuationCpuNs",e.getValue()[0]);r.put("quanta",e.getValue()[1]);r.put("cpuClockResolutionNs",1000000L);r.put("scope","PROCESS_OBSERVED_TASK_CPU_EXCLUDES_WRITER_AND_UNOBSERVED_PROCESSES_NOT_PER_POLICY_CPU");out.add(r);}}return out;}
    private static final java.util.concurrent.atomic.AtomicLong TELEMETRY_DROPPED_R31 = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong RECEIPTS_FAILED_R31 = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong FAULT_SAMPLES_DROPPED_R31 = new java.util.concurrent.atomic.AtomicLong();
    private static final String OBSERVATION_EPOCH_R31 = java.util.UUID.randomUUID().toString();
    public static final int MAX_FAULT_SAMPLES_R31 = 2000;
    public static final long MAX_FAULT_BYTES_R31 = 8L * 1024 * 1024;

    /** Kept in memory if even the telemetry write fails; never interpreted as zero missing observations. */
    public static java.util.Map<String,Object> telemetrySnapshotR31() {
        java.util.Map<String,Object> out = new java.util.LinkedHashMap<>();
        out.put("scope", "CURRENT_PROCESS_ONLY_UNFLUSHED_COUNTS_LOST_ON_PROCESS_DEATH");
        out.put("observationEpoch", OBSERVATION_EPOCH_R31);
        out.put("telemetry_incomplete", TELEMETRY_DROPPED_R31.get() > 0 || FAULT_SAMPLES_DROPPED_R31.get() > 0 || RECEIPTS_FAILED_R31.get() > 0);
        out.put("observationWritesFailed", TELEMETRY_DROPPED_R31.get());
        out.put("taskReceiptsFailed", RECEIPTS_FAILED_R31.get());
        out.put("faultSamplesDropped", FAULT_SAMPLES_DROPPED_R31.get());
        return out;
    }

    /** A separate diagnostic transaction: its failure cannot roll back or relabel a business/artifact commit. */
    static void observeAttemptR31(DbStore store, String task, String owner,
                                  RuntimeCoordinatorR26.Claim claim, String phase, Throwable failure,
                                  long units, long cpuMs, boolean receiptCommitted) {
        if (!receiptCommitted) RECEIPTS_FAILED_R31.incrementAndGet();
        final long wall = System.currentTimeMillis(), elapsed = SystemClock.elapsedRealtime();
        final String safeTask = RuntimeFaultEvidenceR31.clean(task, 128);
        final String safeOwner = RuntimeFaultEvidenceR31.clean(owner, 128);
        try {
            final RuntimeFaultEvidenceR31 fault = failure == null ? null : RuntimeFaultEvidenceR31.capture(failure, "TASK_" + phase);
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.TELEMETRY,"R31_RUNTIME_OBSERVATION"), db -> {
                // At most the finite registered task set, not arbitrary caller/event IDs.
                try (Cursor registered = db.rawQuery("SELECT 1 FROM r26_task WHERE task_id=?", new String[] {safeTask})) {
                    if (!registered.moveToFirst()) throw new IllegalStateException("R31_OBSERVATION_UNREGISTERED_TASK");
                }
                db.execSQL("INSERT OR IGNORE INTO r31_task_observation(task_id,owner_epoch,last_attempt_elapsed_ms,boot_id) VALUES(?,?,?,?)",
                        new Object[] {safeTask, safeOwner, elapsed, IssueTimelineWriterR30.bootId()});
                db.execSQL("UPDATE r31_task_observation SET owner_epoch=?,fence=?,attempts=attempts+1,work_cpu_ms=work_cpu_ms+?,observed_work_units=observed_work_units+?,task_receipts_committed=task_receipts_committed+?,task_receipts_failed=task_receipts_failed+?,last_attempt_elapsed_ms=?,boot_id=? WHERE task_id=?",
                        new Object[] {safeOwner, claim == null ? null : claim.fence, Math.max(0, cpuMs), Math.max(0, units), receiptCommitted ? 1 : 0, receiptCommitted ? 0 : 1, elapsed, IssueTimelineWriterR30.bootId(), safeTask});
                if (fault != null) persistFaultR31(db, safeTask, safeOwner, claim, phase, fault, wall, elapsed);
                persistCounterR31(db, "observationWritesFailed", TELEMETRY_DROPPED_R31.get(), wall);
                persistCounterR31(db, "taskReceiptsFailed", RECEIPTS_FAILED_R31.get(), wall);
                persistCounterR31(db, "faultSamplesDropped", FAULT_SAMPLES_DROPPED_R31.get(), wall);
                return 0;
            });
        } catch (RuntimeException observationUnavailable) {
            TELEMETRY_DROPPED_R31.incrementAndGet();
            // No recursive diagnostic write, and no raw exception text or credential-bearing URL.
            android.util.Log.w("STARCORE_R31", "runtime observation incomplete task=" + safeTask + " class=" + observationUnavailable.getClass().getSimpleName());
        }
    }

    private static void persistCounterR31(SQLiteDatabase db, String name, long value, long wall) {
        db.execSQL("INSERT OR REPLACE INTO r31_telemetry_counter(owner_epoch,name,value,updated_at) VALUES(?,?,?,?)",
                new Object[] {OBSERVATION_EPOCH_R31, name, value, wall});
        // Old process counters are summaries, not prediction/settlement/qualification evidence.
        db.execSQL("DELETE FROM r31_telemetry_counter WHERE owner_epoch NOT IN (SELECT owner_epoch FROM r31_telemetry_counter GROUP BY owner_epoch ORDER BY max(updated_at) DESC LIMIT 32)");
    }

    private static void persistFaultR31(SQLiteDatabase db, String task, String owner, RuntimeCoordinatorR26.Claim claim,
                                       String phase, RuntimeFaultEvidenceR31 fault, long wall, long elapsed) {
        long minute = Math.floorDiv(wall, 60_000L), total = 0, count = 0, oldBytes = 0, oldLastBytes = 0;
        String eventId = null;
        try (Cursor c = db.rawQuery("SELECT count(*),coalesce(sum(byte_count),0) FROM r31_runtime_fault", null)) {
            if (c.moveToFirst()) { count = c.getLong(0); total = c.getLong(1); }
        }
        try (Cursor c = db.rawQuery("SELECT event_id,byte_count,length(CAST(last_cause_chain_json AS BLOB))+length(CAST(last_stack AS BLOB)) FROM r31_runtime_fault WHERE task_id=? AND owner_epoch=? AND root_fingerprint=? AND minute_bucket=?",
                new String[] {task, owner, fault.fingerprint, String.valueOf(minute)})) {
            if (c.moveToFirst()) { eventId = c.getString(0); oldBytes = c.getLong(1); oldLastBytes = c.getLong(2); }
        }
        long nextBytes = eventId == null ? 1024L + 2L * fault.bytes() : oldBytes - oldLastBytes + fault.bytes();
        if ((eventId == null && count >= MAX_FAULT_SAMPLES_R31) || total - oldBytes + nextBytes > MAX_FAULT_BYTES_R31) {
            FAULT_SAMPLES_DROPPED_R31.incrementAndGet();
            return;
        }
        if (eventId != null) {
            db.execSQL("UPDATE r31_runtime_fault SET occurrences=occurrences+1,last_wall_ms=?,last_elapsed_ms=?,last_cause_chain_json=?,last_stack=?,last_stack_hash=?,byte_count=? WHERE event_id=?",
                    new Object[] {wall, elapsed, fault.causeChainJson, fault.stack, fault.stackHash, nextBytes, eventId});
        } else {
            eventId = StatsR27.sha256(task + "|" + owner + "|" + fault.fingerprint + "|" + minute);
            db.execSQL("INSERT INTO r31_runtime_fault(event_id,task_id,owner_epoch,fence,operation,phase,category,root_fingerprint,minute_bucket,cause_chain_json,stack,stack_hash,last_cause_chain_json,last_stack,last_stack_hash,commit_outcome,boot_id,first_wall_ms,last_wall_ms,first_elapsed_ms,last_elapsed_ms,occurrences,byte_count) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    new Object[] {eventId, task, owner, claim == null ? null : claim.fence, fault.operation, RuntimeFaultEvidenceR31.clean(phase, 64), fault.category, fault.fingerprint, minute, fault.causeChainJson, fault.stack, fault.stackHash, fault.causeChainJson, fault.stack, fault.stackHash, fault.commitOutcome, IssueTimelineWriterR30.bootId(), wall, wall, elapsed, elapsed, 1, nextBytes});
        }
    }
    public String session() { return session; }
}
