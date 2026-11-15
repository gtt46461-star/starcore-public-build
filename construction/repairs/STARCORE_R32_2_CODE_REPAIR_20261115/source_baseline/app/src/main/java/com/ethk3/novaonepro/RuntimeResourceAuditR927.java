package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** B05/E02A attempt-scoped evidence and cooperative deadlines. Never owns a decision. */
public final class RuntimeResourceAuditR927 {
    public static final String TABLE = "v57_attempt_resource_r927";
    public static final long SAMPLE_MS = 25L;
    private static final int TRANSACTION_SAMPLE_LIMIT_R75=128;
    private static final int TRANSACTION_LABEL_LIMIT_R75=96;
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<Scope>();
    private static final java.util.concurrent.atomic.AtomicBoolean FIRST = new java.util.concurrent.atomic.AtomicBoolean(true);
    private static final ScheduledExecutorService SAMPLER = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) { Thread t=new Thread(r,"r927-resource-sampler");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t; }
    });
    private RuntimeResourceAuditR927() {}
    private static final com.ethk3.novaonepro.r952.ExclusiveStageClockCP12 EXCLUSIVE_CP12=new com.ethk3.novaonepro.r952.ExclusiveStageClockCP12(new com.ethk3.novaonepro.r952.ExclusiveStageClockCP12.Clock(){
        public long wallNanos(){return System.nanoTime();}
        public long cpuNanos(){try{return android.os.Debug.threadCpuTimeNanos();}catch(RuntimeException unavailable){return -1;}}
    });

    public static void ensure(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+" (execution_id TEXT PRIMARY KEY,attempt_id TEXT NOT NULL,target_issue TEXT NOT NULL,process_id TEXT NOT NULL,release_code INTEGER NOT NULL,started_at INTEGER NOT NULL,finished_at INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,elapsed_ms INTEGER NOT NULL DEFAULT -1,heap_peak_bytes INTEGER NOT NULL DEFAULT -1,heap_samples INTEGER NOT NULL DEFAULT 0,gc_count INTEGER NOT NULL DEFAULT -1,gc_time_ms INTEGER NOT NULL DEFAULT -1,gc_status TEXT NOT NULL DEFAULT 'UNMEASURED',lock_wait_ms INTEGER NOT NULL DEFAULT 0,lock_wait_max_ms INTEGER NOT NULL DEFAULT 0,db_body_max_ms INTEGER NOT NULL DEFAULT 0,db_slice_overruns INTEGER NOT NULL DEFAULT 0,budget_hash TEXT NOT NULL,stage_samples TEXT NOT NULL DEFAULT '',reason TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r927_resource_attempt ON "+TABLE+"(attempt_id,started_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r928_resource_status ON "+TABLE+"(status,process_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r928_resource_release_time ON "+TABLE+"(release_code,started_at DESC)");
    }

    public static Scope begin(final DbStore store, AnalysisSingleFlightR36.Lease lease) {
        if(CURRENT.get()!=null)throw new IllegalStateException("B05_RESOURCE_SCOPE_NESTED");
        final Scope scope=new Scope(lease, FIRST.getAndSet(false));
        CURRENT.set(scope);EXCLUSIVE_CP12.abandon();
        scope.sampleHeap();
        scope.sampling=SAMPLER.scheduleWithFixedDelay(new Runnable(){public void run(){scope.sampleHeap();}},SAMPLE_MS,SAMPLE_MS,TimeUnit.MILLISECONDS);
        try {
            boolean ok=store.coordinatorR36().runWrite("R927_RESOURCE_START",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
                /* A stale STARTED row may appear after startup reconciliation (restored DB,
                 * delayed journal replay or a fault-injection row). Reconcile on every accepted
                 * execution. Unknown prior-process timings/resources keep their schema defaults
                 * (-1 / UNMEASURED) instead of being fabricated as zero. */
                db.execSQL("UPDATE "+TABLE+" SET status='PROCESS_INTERRUPTED',reason=CASE WHEN reason='' THEN 'PROCESS_ENDED_WITHOUT_FINAL_RESOURCE_SAMPLE' ELSE reason||'|PROCESS_ENDED_WITHOUT_FINAL_RESOURCE_SAMPLE' END WHERE status='STARTED' AND process_id<>?",new Object[]{RuntimeObservabilityR925.PROCESS_ID});
                db.execSQL("INSERT OR IGNORE INTO "+TABLE+"(execution_id,attempt_id,target_issue,process_id,release_code,started_at,status,budget_hash) VALUES(?,?,?,?,?,?,?,?)",new Object[]{scope.executionId,scope.attemptId,scope.target,RuntimeObservabilityR925.PROCESS_ID,BuildConfig.VERSION_CODE,System.currentTimeMillis(),"STARTED",scope.budgetHash});
            }});
            if(!ok)scope.persistenceError="RESOURCE_START_WRITE_FAILED";
            return scope;
        } catch (RuntimeException failure) {
            if(scope.sampling!=null)scope.sampling.cancel(false);CURRENT.remove();throw failure;
        } catch (Error failure) {
            if(scope.sampling!=null)scope.sampling.cancel(false);CURRENT.remove();throw failure;
        }
    }

    public static void finish(final DbStore store, final Scope scope, boolean committed, String reason) {
        if(scope==null)return;
        CURRENT.remove();
        int incompleteExclusive=EXCLUSIVE_CP12.abandon();
        if(incompleteExclusive>0)synchronized(scope){scope.stages.append("CP12EX_UNAVAILABLE|INCOMPLETE_SPANS|").append(incompleteExclusive).append(';');}
        synchronized(scope){for(Map.Entry<String,long[]> e:scope.active.entrySet())scope.stages.append(e.getKey()).append(":INTERRUPTED:").append(Math.max(0L,TimeSourceR811.monoNowMs()-e.getValue()[0])).append(';');scope.active.clear();}
        if(scope.sampling!=null)scope.sampling.cancel(false);scope.sampleHeap();
        final long elapsed=Math.max(0L,TimeSourceR811.monoNowMs()-scope.startedMono);
        final long gcCount=delta(scope.gcCountStart,runtimeStat("art.gc.gc-count"));
        final long gcTime=delta(scope.gcTimeStart,runtimeStat("art.gc.gc-time"));
        final String status=committed?"CANONICAL_COMMITTED":"NOT_COMMITTED";
        final String detail=clean(reason)+"|"+scope.persistenceError;
        final String stageSamples=stageSamplesWithTransactionsR75(scope);
        boolean ok=store.coordinatorR36().runWrite("R927_RESOURCE_FINISH",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
            db.execSQL("UPDATE "+TABLE+" SET finished_at=?,status=?,elapsed_ms=?,heap_peak_bytes=?,heap_samples=?,gc_count=?,gc_time_ms=?,gc_status=?,lock_wait_ms=?,lock_wait_max_ms=?,db_body_max_ms=?,db_slice_overruns=?,stage_samples=?,reason=? WHERE execution_id=?",new Object[]{System.currentTimeMillis(),status,elapsed,scope.heapPeak,scope.heapSamples,gcCount,gcTime,gcCount<0||gcTime<0?"UNAVAILABLE_OR_COUNTER_RESET":"PROCESS_COUNTER_DELTA",scope.lockWait,scope.lockWaitMax,scope.dbBodyMax,scope.dbOverruns,stageSamples,detail,scope.executionId});
        }});
        if(!ok)android.util.Log.e("R927_RESOURCE","RESOURCE_FINISH_WRITE_FAILED:"+scope.executionId);
    }

    /** Confirmed draw/history ingestion belongs to the data lane, even if a prediction
     * deadline expires while a bootstrap response is arriving. This scope does not
     * change the request, token, lease or deadline; the next prediction checkpoint
     * must still reject an expired request. Package-private: only the runtime adapter. */
    static final class DataIngressScopeCP12 implements AutoCloseable {
        private final Scope saved; private boolean closed;
        private DataIngressScopeCP12() { saved=CURRENT.get(); CURRENT.remove(); }
        @Override public void close() {
            if(closed) return; closed=true;
            if(CURRENT.get()!=null) throw new IllegalStateException("DATA_INGRESS_SCOPE_NESTED_AUDIT");
            if(saved!=null) CURRENT.set(saved);
        }
    }
    static DataIngressScopeCP12 dataIngressCP12() { return new DataIngressScopeCP12(); }

    static long delta(long before,long after){return before<0L||after<before?-1L:after-before;}
    private static long runtimeStat(String key) {
        try { Object value=Class.forName("android.os.Debug").getMethod("getRuntimeStat",String.class).invoke(null,key);return value==null?-1L:Long.parseLong(value.toString()); }
        catch(Exception unavailable){return -1L;}
    }
    public static String frozenBudgetHash(){Scope s=CURRENT.get();return s==null?"":s.budgetHash;}
    public static void beginRecovery(){Scope s=CURRENT.get();if(s!=null)s.recovering=true;}
    public static void checkpoint(String stage) {
        Scope scope=CURRENT.get();
        if(scope!=null&&!scope.recovering&&!scope.lease.token.isCanonicalCommitted())scope.check(stage,TimeSourceR811.monoNowMs());
    }
    public static long remainingMs() {
        Scope scope=CURRENT.get();return scope==null||scope.recovering||scope.lease.token.isCanonicalCommitted()?Long.MAX_VALUE:Math.max(0L,scope.deadline()-TimeSourceR811.monoNowMs());
    }
    public static void stageBegin(String stage) {
        Scope s=CURRENT.get();if(s==null||s.lease.token.isCanonicalCommitted())return;
        long now=TimeSourceR811.monoNowMs();s.check(stage,now);
        long budget=stageBudget(stage);if(budget>0)s.beginStage(stage,now,budget);
        try{EXCLUSIVE_CP12.begin(stage);}catch(IllegalStateException invalid){synchronized(s){s.stages.append("CP12EX_UNAVAILABLE|").append(clean(stage)).append('|').append(invalid.getMessage()).append(';');}}
    }
    public static void stageEnd(String stage,long elapsed,boolean success) {
        Scope s=CURRENT.get();if(s==null)return;
        long budget=stageBudget(stage);
        synchronized(s){s.stages.append(stage).append(':').append(elapsed).append('/').append(budget).append(':').append(!success?"FAILED":budget>0L&&elapsed>budget?"OVER_BUDGET":"OK").append(';');s.active.remove(stage);}
        try{com.ethk3.novaonepro.r952.ExclusiveStageClockCP12.Span span=EXCLUSIVE_CP12.end(stage);synchronized(s){s.stages.append(span.canonical());}}
        catch(IllegalStateException missing){synchronized(s){s.stages.append("CP12EX_UNAVAILABLE|").append(clean(stage)).append('|').append(missing.getMessage()).append(';');}EXCLUSIVE_CP12.abandon();}
        s.sampleHeap();
        // Stage SLO overruns are evidence. Only the immutable request deadline cancels prediction.
        if(success&&!s.lease.token.isCanonicalCommitted())s.check(stage,TimeSourceR811.monoNowMs());
    }
    /** Observe a successful event completed before this audit scope existed. Its supplied
     * duration is inclusive wall time only: no CPU or exclusive interval was measured.
     * Keep the original stage SLO, heap sample and immutable deadline check, but never
     * remove an active stage or pop/abandon the synchronous exclusive-stage stack.
     */
    public static void completedBeforeScopeR79(String stage,long elapsed) {
        Scope s=CURRENT.get();if(s==null)return;
        long budget=stageBudget(stage);
        synchronized(s){
            s.stages.append(stage).append(':').append(elapsed).append('/').append(budget)
                    .append(':').append(budget>0L&&elapsed>budget?"OVER_BUDGET":"OK").append(';');
            s.stages.append("CP12INCLUSIVE_ONLY|").append(clean(stage)).append("|PRE_SCOPE_WALL_ONLY;");
        }
        s.sampleHeap();
        if(!s.lease.token.isCanonicalCommitted())s.check(stage,TimeSourceR811.monoNowMs());
    }
    /** Separate disjoint async dispatch wait. CPU is unavailable, not zero; this never enters the sync stack. */
    public static void measuredQueueSpanCP12(String stage,long startMonoMs,long endMonoMs){
        Scope scope=CURRENT.get();if(scope==null)return;
        if(stage==null||!stage.matches("[A-Z][A-Z0-9_]{0,63}")||startMonoMs<0||endMonoMs<startMonoMs){synchronized(scope){scope.stages.append("CP12EX_UNAVAILABLE|QUEUE_CLOCK_INVALID;");}return;}
        long nanos=(endMonoMs-startMonoMs)*1000000L;synchronized(scope){scope.stages.append("CP12EX1|").append(stage).append('|').append(nanos).append("|-1|").append(Thread.currentThread().getId()).append("|QUEUE;");}
    }
    static long stageBudget(String stage) {
        if("HISTORY".equals(stage))return 1000L;if("R36_COMPUTE".equals(stage))return 3000L;if("FEATURES_SUB".equals(stage))return 900L;if("STATE_SUB".equals(stage))return 700L;if("NUMBER_MODEL".equals(stage))return 600L;if("ACTION_BUILD_FUNNEL_META_RANK".equals(stage))return 600L;if("SELECTION_POLICY".equals(stage))return 300L;if("SNAPSHOT_STREAM".equals(stage))return 400L;if("DB_CANONICAL_WAIT".equals(stage))return 600L;return 0L;
    }
    public static void lockWait(long ms){Scope s=CURRENT.get();if(s!=null){s.lockWait+=Math.max(0L,ms);s.lockWaitMax=Math.max(s.lockWaitMax,ms);}}
    public static void dbBody(long ms,boolean background){Scope s=CURRENT.get();if(s!=null){s.dbBodyMax=Math.max(s.dbBodyMax,ms);if(background&&ms>50L)s.dbOverruns++;}}
    /** Independent passive transaction detail. dbBody keeps its original inclusive meaning.
     * BEGIN includes SQLite connection/native-lock acquisition. BODY is the callback and
     * success mark. END includes commit/rollback and committed-schema cache publication.
     * -1 means not executed (including caller-owned transactions), never a zero-duration claim.
     * This deliberately does not use stageBegin/stageEnd: observation cannot add a deadline,
     * change the exclusive-stage stack, or turn a committed transaction into a rejected one.
     */
    static void transactionTimingR75(String operation,boolean owns,long beginNanos,
            long bodyNanos,long endNanos,String outcome) {
        Scope scope=CURRENT.get();if(scope==null)return;
        try {
            synchronized(scope){
                if(scope.transactionSamplesR75>=TRANSACTION_SAMPLE_LIMIT_R75){
                    if(!scope.transactionTruncatedR75){
                        scope.transactionDetailsR75.append("DBTX75_TRUNCATED|limit=").append(TRANSACTION_SAMPLE_LIMIT_R75).append(';');
                        scope.transactionTruncatedR75=true;
                    }
                    return;
                }
                // Bound before copying/sanitizing so a long caller label is never duplicated.
                String label=operation==null?"":operation.substring(0,Math.min(operation.length(),TRANSACTION_LABEL_LIMIT_R75));
                label=clean(label).replace('|','_').replace(';','_');
                String result=outcome==null?"":outcome.substring(0,Math.min(outcome.length(),48));
                result=clean(result).replace('|','_').replace(';','_');
                scope.transactionDetailsR75.append("DBTX75|operation=").append(label)
                        .append("|owns=").append(owns?1:0).append("|begin_ns=").append(beginNanos)
                        .append("|body_ns=").append(bodyNanos).append("|end_ns=").append(endNanos)
                        .append("|outcome=").append(result).append(';');
                scope.transactionSamplesR75++;
            }
        } catch (RuntimeException unavailable) {
            // Passive diagnostics must not replace a committed result or the original failure.
            // Fatal VM errors and ThreadDeath are deliberately not swallowed.
        }
    }
    /** Append only after all original spans are complete, preserving every original index.
     * The latency matrix accepts at most 65536 chars and 512 split(";",-1) entries.
     * Diagnostics use remaining room only; a full or already-invalid original is unchanged.
     */
    static String stageSamplesWithTransactionsR75(Scope scope) {
        synchronized(scope){
            String original=scope.stages.toString();
            if(original.length()>65536)return original;
            try{
                int entries=1;for(int i=0;i<original.length();i++)if(original.charAt(i)==';')entries++;
                if(entries>=512||scope.transactionDetailsR75.length()==0)return original;
                StringBuilder combined=new StringBuilder(original);
                String details=scope.transactionDetailsR75.toString();int offset=0;
                while(offset<details.length()){
                    int end=details.indexOf(';',offset);if(end<0)break;
                    String item=details.substring(offset,end+1);
                    boolean separator=combined.length()>0&&combined.charAt(combined.length()-1)!=';';
                    int extraEntries=separator?2:1;
                    if(combined.length()+item.length()+(separator?1:0)>65536||entries+extraEntries>512)break;
                    if(separator)combined.append(';');combined.append(item);entries+=extraEntries;offset=end+1;
                }
                if(offset<details.length()){
                    String marker="DBTX75_TRUNCATED|reason=STAGE_CAPACITY;";
                    boolean separator=combined.length()>0&&combined.charAt(combined.length()-1)!=';';
                    if(combined.length()+marker.length()+(separator?1:0)<=65536&&entries+(separator?2:1)<=512){
                        if(separator)combined.append(';');combined.append(marker);
                    }
                }
                return combined.toString();
            }catch(RuntimeException unavailable){return original;}
        }
    }
    public static String report(DbStore store) {
        StringBuilder b=new StringBuilder("B05_RESOURCE_RAW_LAST_120\n堆峰值为25ms采样最大值；GC是进程累计计数差，非单线程归因；-1=系统不支持/未测量。全部执行原始行见全表导出。\n");
        Cursor c=store.readable().rawQuery("SELECT execution_id,attempt_id,target_issue,status,elapsed_ms,heap_peak_bytes,heap_samples,gc_count,gc_time_ms,gc_status,lock_wait_ms,lock_wait_max_ms,db_body_max_ms,db_slice_overruns,budget_hash,stage_samples,reason FROM "+TABLE+" WHERE release_code=? ORDER BY started_at DESC LIMIT 120",new String[]{String.valueOf(BuildConfig.VERSION_CODE)});
        try {String[] names=c.getColumnNames();while(c.moveToNext()){for(int i=0;i<names.length;i++)b.append(names[i]).append('=').append(c.isNull(i)?"NULL":c.getString(i)).append('|');b.append('\n');}}finally{c.close();}return b.toString();
    }
    public static final class Scope {
        final AnalysisSingleFlightR36.Lease lease;final String executionId,attemptId,target,budgetHash;final long startedMono,totalDeadline,gcCountStart,gcTimeStart;
        final LinkedHashMap<String,long[]> active=new LinkedHashMap<String,long[]>();final StringBuilder stages=new StringBuilder();volatile long heapPeak,heapSamples;long lockWait,lockWaitMax,dbBodyMax,dbOverruns;boolean recovering;String persistenceError="";ScheduledFuture<?> sampling;
        final StringBuilder transactionDetailsR75=new StringBuilder();int transactionSamplesR75;boolean transactionTruncatedR75;
        Scope(AnalysisSingleFlightR36.Lease lease,boolean cold) {
            this.lease=lease;executionId=lease.ownerId;attemptId=lease.request.attemptId();target=lease.targetIssue;startedMono=TimeSourceR811.monoNowMs();
            long deadline=startedMono+(cold?8000L:5000L);
            long requestRemaining=lease.request.deadlineMonoAt<=0L?Long.MAX_VALUE:Math.max(0L,lease.request.deadlineMonoAt-TimeSourceR811.monoNowMs());
            totalDeadline=requestRemaining==Long.MAX_VALUE?deadline:startedMono+requestRemaining;
            gcCountStart=runtimeStat("art.gc.gc-count");gcTimeStart=runtimeStat("art.gc.gc-time");
            budgetHash=V51ChecksumEngine.sha256("B05_R931_V5|hardDeadline=IMMUTABLE_PREDRAW_REQUEST|activeStageDeadline=AUDIT_ONLY|stageBudgets=AUDITED_SLO|cold="+cold+"|totalSlo="+(cold?8000:5000)+"|compute=3000|history=1000|feature=900|state=700|number=600|rank=600|gate=300|snapshot=400|commit=600|heap="+Runtime.getRuntime().maxMemory()+"|sample=25|sqlSlice=50|researchCancel=100");
        }
        synchronized void sampleHeap(){Runtime r=Runtime.getRuntime();heapPeak=Math.max(heapPeak,r.totalMemory()-r.freeMemory());heapSamples++;}
        synchronized void beginStage(String stage,long now,long budget){if(active.containsKey(stage))throw new IllegalStateException("B05_STAGE_ALREADY_ACTIVE:"+stage);active.put(stage,new long[]{now,budget});}
        long deadline(){return totalDeadline;}
        synchronized void check(String checkpoint,long now){
            if(now>=deadline())throw new BudgetExceeded("DEADLINE_EXCEEDED:"+checkpoint+"|over_ms="+(now-deadline()));
        }
    }
    public static final class BudgetExceeded extends RuntimeException {BudgetExceeded(String message){super("B05_TIMEOUT|"+message);}}
    private static String clean(String s){return s==null?"":s.replace('\n',' ').replace('\r',' ');}

public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        com.ethk3.novaonepro.ReportTextCP12 b=new com.ethk3.novaonepro.ReportTextCP12(report, "B05_RESOURCE_RAW_LAST_120\n堆峰值为25ms采样最大值；GC是进程累计计数差，非单线程归因；-1=系统不支持/未测量。全部执行原始行见全表导出。\n");
        Cursor c=report.select("SELECT execution_id,attempt_id,target_issue,status,elapsed_ms,heap_peak_bytes,heap_samples,gc_count,gc_time_ms,gc_status,lock_wait_ms,lock_wait_max_ms,db_body_max_ms,db_slice_overruns,budget_hash,stage_samples,reason FROM "+TABLE+" WHERE release_code=? ORDER BY started_at DESC LIMIT 120",new String[]{String.valueOf(BuildConfig.VERSION_CODE)});
        try {String[] names=c.getColumnNames();while(c.moveToNext()){for(int i=0;i<names.length;i++)b.append(names[i]).append('=').append(c.isNull(i)?"NULL":c.getString(i)).append('|');b.append('\n');}}finally{c.close();}return b.toString();
    }
}
