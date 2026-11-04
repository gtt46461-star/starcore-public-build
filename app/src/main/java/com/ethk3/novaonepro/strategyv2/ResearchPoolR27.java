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
import java.util.concurrent.LinkedBlockingQueue;
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
        public boolean cancelled() { return closed || Thread.currentThread().isInterrupted(); }
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
    private volatile int parallelismLimit = 2; private volatile String thermalReason = "NORMAL";
    private final AtomicInteger active = new AtomicInteger();
    public final int size;
    private final ExecutorService compute;

    public static final int BACKGROUND_PRIORITY = android.os.Process.THREAD_PRIORITY_BACKGROUND + 1;
    public static int defaultThreads() { int cores = Runtime.getRuntime().availableProcessors(); return Math.max(1, Math.min(2, cores - 1)); }

    public ResearchPoolR27(DbStore store, String session, int threads) {
        this.store = store; this.session = session; this.size = Math.max(1, threads); this.writes = new ResearchWriteQueueR27(store);
        // Shared helper executor for data-parallel replay inside one task (evolution REPLAY step).
        // R27.2: the helpers run at the workers' background priority. They used to inherit the default priority and,
        // with up to four of them replaying at once, took the CPU from the realtime analysis thread on device.
        compute = new ThreadPoolExecutor(size, size, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), r -> { Thread t = new Thread(() -> { try { android.os.Process.setThreadPriority(BACKGROUND_PRIORITY); } catch (Throwable ignored) { } r.run(); }, "starcore-r27-compute"); t.setDaemon(true); return t; });
    }
    public ResearchWriteQueueR27 writes() { return writes; }
    public ExecutorService compute() { return compute; }
    public synchronized void register(Task t) { for(Task existing:tasks)if(existing.id().equals(t.id()))throw new IllegalArgumentException("DUPLICATE_RESEARCH_OWNER:"+t.id());tasks.add(t); running.put(t.id(), false); }
    public void setResource(String r) { resource = r; }
    public void setThermalLimit(int workers, String reason) { parallelismLimit = Math.max(1, Math.min(size, workers)); thermalReason = reason; }
    public int workerLimit() { return parallelismLimit; }
    public String thermalReason() { return thermalReason; }

    public void start() {
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
        long cpuStart = android.os.Debug.threadCpuTimeNanos(), wallStart = SystemClock.elapsedRealtime(), flushed = wallStart;
        long slices = 0;
        while (!closed) {
            try {
                if (resource != null) { sleep(500); continue; }
                // Freeze guard: the last 3 s before a deadline run with one active research worker (not zero).
                // R27.2: the same one-worker floor while P0 analysis/settlement runs (device: request→commit P95 20.5 s with
                // MODEL_BUNDLE_OPEN 10.6 s wall / 0.13 s CPU while every worker ran). Research never drops to zero.
                int limit = narrow(System.currentTimeMillis()) ? 1 : Math.min(size, parallelismLimit);
                if (!ResearchSlotsR29.tryEnter(active, limit)) { sleep(100); continue; }
                Task t;
                try { t = next(); } catch (Throwable failure) { active.decrementAndGet(); throw failure; }
                if (t == null) { active.decrementAndGet(); sleep(250); continue; }
                Outcome o = null; Throwable failure = null;ResourceBudget appliedResourceBudget=null;
                RuntimeCoordinatorR26.Claim observedClaim = null; String observedPhase = "CLAIM";
                long attemptCpu = android.os.Debug.threadCpuTimeNanos();
                boolean quarantined = false;
                try {
                    try (Cursor c = w.ro.rawQuery("SELECT status FROM r26_task WHERE task_id=?", new String[] {t.id()})) {
                        quarantined = c.moveToFirst() && WaitKindR27.QUARANTINED.equals(c.getString(0));
                    }
                    if (!quarantined) {
                        PreparedTaskR30R1 current; synchronized(this){current=prepared.get(t.id());}
                        RuntimeCoordinatorR26.Claim claim=claimPrepared(t.id(),current);
                        observedClaim = claim;
                        if(claim==null){o=Outcome.idle("DEPENDENCY(ACTIVE_SCHEMA_SESSION_OR_TASK_LEASE)",0,-1);}
                        else {
                            if(current==null){current=new PreparedTaskR30R1(t,w,claim);synchronized(this){prepared.put(t.id(),current);}}
                            current.claim=claim;
                            observedPhase = "COMPUTE_OR_PUBLISH";
                            refreshResourceBudget(w.ro);appliedResourceBudget=resourceBudget;int quantum=appliedResourceBudget.quantums.getOrDefault(t.id(),200);
                            try{o=current.slice(quantum);}finally{recordResearchCost(t.id(),current.lastCpuNanos);if(current.done()){current.close();synchronized(this){prepared.remove(t.id());}}}
                        }
                    }
                } catch (Throwable e) { failure = e; }
                finally { active.decrementAndGet(); synchronized (this) { running.put(t.id(), false); } }
                if (quarantined) { synchronized (this) { notBefore.put(t.id(), System.currentTimeMillis() + 5000); } continue; }
                slices++;
                record(t.id(), o, failure,appliedResourceBudget, observedClaim, observedPhase,
                        Math.max(0, android.os.Debug.threadCpuTimeNanos() - attemptCpu) / 1_000_000L);
                if (o == null || o.units == 0) synchronized (this) { notBefore.put(t.id(), System.currentTimeMillis() + (failure != null ? 5000 : o!=null&&"COOPERATIVE_PREPARED_STEP".equals(o.reason)?25:1000)); }
            } catch (Throwable unexpected) { android.util.Log.w("STARCORE_R27", "worker " + w.name, unexpected); sleep(500); }
            long now = SystemClock.elapsedRealtime();
            if (now - flushed > 15_000) { flushCpu(w.name, android.os.Debug.threadCpuTimeNanos() - cpuStart, now - wallStart, slices); flushed = now; }
        }
    }

    private RuntimeCoordinatorR26.Claim claimPrepared(String id,PreparedTaskR30R1 current){
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_PREPARED_TASK_CLAIM",db->{
            if(!ResearchStoreR30R1.ready(db))return null;
            long now=System.currentTimeMillis();
            try(Cursor c=db.rawQuery("SELECT 1 FROM r26_session WHERE session_id=? AND user_enabled=1 AND state='RUNNING'",new String[]{session})){if(!c.moveToFirst())return null;}
            long fence,cursor;String owner,status;long until;
            try(Cursor c=db.rawQuery("SELECT fence,cursor,owner,status,lease_until FROM r26_task WHERE task_id=?",new String[]{id})){if(!c.moveToFirst())return null;fence=c.getLong(0);cursor=c.getLong(1);owner=c.getString(2);status=c.getString(3);until=c.getLong(4);}
            if(current!=null){if(!session.equals(owner)||fence!=current.claim.fence)throw new IllegalStateException("R30_PREPARED_OWNER_CHANGED");}
            else {if("QUARANTINED".equals(status)||until>now&&!session.equals(owner))return null;fence++;}
            db.execSQL("UPDATE r26_task SET owner=?,fence=?,status='RUNNING',lease_until=? WHERE task_id=?",new Object[]{session,fence,now+30000,id});
            return new RuntimeCoordinatorR26.Claim(id,session,fence,cursor,"R27");
        });
    }

    /** Freeze window or active P0 work: the pool runs with one worker (R27.2). */
    boolean narrow(long now) { return parallelismLimit == 1 || writes.freezeWindow(now) || ResearchWriteQueueR27.p0Writing(); }

    private synchronized Task next() {
        long now = System.currentTimeMillis();
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

    private void record(String id, Outcome o, Throwable failure,ResourceBudget appliedResourceBudget,
                        RuntimeCoordinatorR26.Claim claim, String phase, long cpuMs) {
        WaitKindR27.Wait wait = WaitKindR27.classify(o == null ? 0 : o.units, o == null ? null : o.reason, failure, o == null ? 0 : o.cursor, o == null ? -1 : o.latest, resource, false);
        final long now = System.currentTimeMillis(); final String stack = failure == null ? null : stack(failure);
        boolean receiptCommitted = false;
        try {
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R27_TASK_RECEIPT", db -> {
                db.execSQL("INSERT OR IGNORE INTO r26_task(task_id,lane,mechanism,status,registered_at) VALUES(?,'R27',?,'READY',?)", new Object[] {id, id, now});
                if (failure != null) {
                    long ws = 0, cnt = 0;
                    try (Cursor c = db.rawQuery("SELECT error_window_start,error_count FROM r26_task WHERE task_id=?", new String[] {id})) { if (c.moveToFirst()) { ws = c.getLong(0); cnt = c.getLong(1); } }
                    long[] e = WaitKindR27.recordError(ws, cnt, now);
                    boolean q = WaitKindR27.quarantine(e[1]);
                    db.execSQL("INSERT INTO r27_task_error(task_id,error_class,message,stack,recorded_at) VALUES(?,?,?,?,?)", new Object[] {id, failure.getClass().getName(), RuntimeFaultEvidenceR31.clean(failure.getMessage(), 1024), stack, now});
                    db.execSQL("DELETE FROM r27_task_error WHERE error_id<(SELECT max(error_id)-2000 FROM r27_task_error)");
                    db.execSQL("UPDATE r26_task SET error_window_start=?,error_count=?,status=?,wait_kind=?,wait_detail=?,blocked_reason=?,wait_since=CASE WHEN wait_kind=? THEN wait_since ELSE ? END,last_served=? WHERE task_id=?",
                            new Object[] {e[0], e[1], q ? WaitKindR27.QUARANTINED : "BLOCKED", q ? WaitKindR27.QUARANTINED : wait.kind, wait.detail, wait.blockedReason(), wait.kind, now, now, id});
                } else {
                    boolean progress = wait.kind.equals(WaitKindR27.PROGRESS);
                    boolean parked=o!=null&&"COOPERATIVE_PREPARED_STEP".equals(o.reason);
                    db.execSQL("UPDATE r26_task SET status=?,lease_until=?,wait_kind=?,wait_detail=?,blocked_reason=?,wait_since=CASE WHEN wait_kind=? THEN wait_since ELSE ? END,last_served=? WHERE task_id=? AND owner=? AND status<>?",
                            new Object[] {parked?"RUNNING":progress?"YIELDED":"BLOCKED",parked?now+30000:0,wait.kind,wait.detail,wait.blockedReason(),wait.kind,now,now,id,session,WaitKindR27.QUARANTINED});
                }
                if(failure==null)recordResourceUse(db,id,now,appliedResourceBudget);
                return 0;
            });
            receiptCommitted = true;
        } catch (RuntimeException busy) { android.util.Log.w("STARCORE_R27", "task receipt deferred " + id, busy); }
        observeAttemptR31(store, id, session, claim, phase, failure, o == null ? 0 : o.units, cpuMs, receiptCommitted);
        if (failure != null) android.util.Log.w("STARCORE_R27", id, failure);
    }

    /** Manual retry from the UI: clears a quarantine. */
    public static void retry(DbStore store, String taskId) {
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R27_TASK_RETRY", db -> { db.execSQL("UPDATE r26_task SET status='READY',wait_kind='',error_count=0,error_window_start=0 WHERE task_id=? AND lane='R27'", new Object[] {taskId}); return 0; });
    }
    public boolean quarantined(String id) {
        try (Cursor c = store.readable().rawQuery("SELECT 1 FROM r26_task WHERE task_id=? AND status=?", new String[] {id, WaitKindR27.QUARANTINED})) { return c.moveToFirst(); }
        catch (RuntimeException e) { return false; }
    }

    private void flushCpu(String thread, long cpuNs, long wallMs, long slices) {
        final long cpu = cpuNs / 1_000_000L, now = System.currentTimeMillis();
        try {
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R27_THREAD_CPU", db -> {
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

    /** Cancel token first, then wait up to 10 s for workers, then close read-only connections (R27-009). */
    @Override public void close() {
        closed = true;
        synchronized(this){for(PreparedTaskR30R1 task:prepared.values())task.close();}
        for (Thread t : threads) t.interrupt();
        long deadline = SystemClock.elapsedRealtime() + 10_000;
        for (Thread t : threads) { long left = deadline - SystemClock.elapsedRealtime(); if (left > 0) try { t.join(left); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
        compute.shutdownNow();
        while(!CooperativeContinuationR26.awaitOwnerStopped("r30-r27:"+session,1000))android.util.Log.w("STARCORE_R30","waiting for prepared workers before closing their read connections");
        synchronized(this){prepared.clear();}
        for (SQLiteDatabase c : connections) try { c.close(); } catch (RuntimeException ignored) { }
        connections.clear();
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
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R31_RUNTIME_OBSERVATION", db -> {
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
                    new Object[] {eventId, task, owner, claim == null ? null : claim.fence, fault.operation, RuntimeFaultEvidenceR31.clean(phase, 64), fault.category, fault.fingerprint, minute, fault.causeChainJson, fault.stack, fault.stackHash, fault.causeChainJson, fault.stack, fault.stackHash, "UNKNOWN_NOT_INFERRED_FROM_EXCEPTION", IssueTimelineWriterR30.bootId(), wall, wall, elapsed, elapsed, 1, nextBytes});
        }
    }
    public String session() { return session; }
}
