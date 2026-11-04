package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.ArrayList;
import java.util.List;

/** Research batch boundary. A batch's CAS, outputs and cursor are ONE transaction, including batches over 500
 * statements. Callers must prepare bounded slices before submitting; splitting a logical batch inside this queue
 * exposed partial model state and let a retry apply non-idempotent increments twice. R29-005's independent database
 * cutover is still pending: this correctness fix deliberately retains the existing writer and its pressure guard. */
public final class ResearchWriteQueueR27 {
    public static final int MAX_ROWS = 500; public static final long MAX_CHUNK_MS = 20, MAX_WAIT_MS = 200, DEADLINE_GUARD_MS = 5000;
    private final DbStore store; private volatile long nextDeadline;
    interface AtomicWriter { void write(String operation, Batch batch); }
    private final AtomicWriter writer;
    private final java.util.function.BooleanSupplier critical;
    private static final ThreadLocal<java.util.function.BooleanSupplier> SUBMIT_CANCELLED_R31=new ThreadLocal<>();
    private long hourStart, submits, longWaits, maxWait, requeues;

    public ResearchWriteQueueR27(DbStore store) {
        this.store = store; this.critical = ResearchWriteQueueR27::p0Writing;
        this.writer = (operation, batch) -> store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R27_"+operation), db -> { execute(db, batch, operation); return 0; });
    }
    // Same production submit path with a transactional test sink, without an Android process/DbStore.
    ResearchWriteQueueR27(AtomicWriter writer, java.util.function.BooleanSupplier critical) {
        this.store = null; this.writer = writer; this.critical = critical;
    }

    /** One prepared statement and its bind arguments. */
    public static final class Op {
        final String sql; final Object[] args;
        public Op(String sql,Object... args) {
            if(sql==null||sql.trim().isEmpty())throw new IllegalArgumentException("R31_SQL_REQUIRED");
            this.sql=sql;this.args=copyArgsR31(args);
        }
    }
    /** A batch built in memory; optional callback runs first inside the same transaction (fence checks, CAS). */
    public static final class Batch {
        public final List<Op> ops = new ArrayList<>(); public DatabaseCoordinator.WriteCallable<Integer> before, after;
        public Batch add(String sql, Object... args) { ops.add(new Op(sql, args)); return this; }
        public boolean isEmpty() { return ops.isEmpty() && before == null && after == null; }
        /** Compose preconditions; a later helper must not overwrite the original fence. */
        public Batch guardBefore(DatabaseCoordinator.WriteCallable<Integer> guard) {
            if(guard==null)throw new IllegalArgumentException("R30_BATCH_GUARD_REQUIRED");
            final DatabaseCoordinator.WriteCallable<Integer> prior=before;
            before=db->{if(prior!=null){Integer r=prior.call(db);if(r==null||r<0)return -1;}Integer result=guard.call(db);return result==null?-1:result;};return this;
        }
        public Batch guardAfter(DatabaseCoordinator.WriteCallable<Integer> guard) {
            if(guard==null)throw new IllegalArgumentException("R30_BATCH_GUARD_REQUIRED");
            final DatabaseCoordinator.WriteCallable<Integer> prior=after;
            after=db->{if(prior!=null){Integer r=prior.call(db);if(r==null||r<0)return -1;}Integer result=guard.call(db);return result==null?-1:result;};return this;
        }
        /** Snapshot once, before yielding. Guards must capture immutable owner-prepared inputs. */
        public ImmutableBatch prepareR31(String operation,String commitKey,String taskId,String ownerEpoch,
                long fence,String inputSnapshot,long chunkSeq,String operatorVersion) {
            return new ImmutableBatch(operation,commitKey,taskId,ownerEpoch,fence,inputSnapshot,chunkSeq,operatorVersion,this);
        }
    }

    public static final class ImmutableBatch {
        public final String operation,commitKey,payloadHash;
        public final DatabaseCoordinator.CommitIdentityR31 identityR31;
        public final int opCount;
        public final long bindBytes;
        public final boolean overInitialBudget,containsDdl;
        private final List<Op> ops;
        private final DatabaseCoordinator.WriteCallable<Integer> before,after;
        private ImmutableBatch(String operation,String key,String task,String owner,long fence,String input,long seq,String version,Batch batch) {
            if(operation==null||operation.isEmpty()||batch==null)throw new IllegalArgumentException("R31_PREPARED_BATCH_REQUIRED");
            this.operation=operation;commitKey=key;
            List<Op> copy=new ArrayList<>();long bytes=0;boolean ddl=false;
            for(Op op:batch.ops){Op frozen=new Op(op.sql,op.args);copy.add(frozen);bytes+=bindBytesR31(frozen.args);ddl|=ddlR31(frozen.sql);}
            ops=java.util.Collections.unmodifiableList(copy);before=batch.before;after=batch.after;opCount=copy.size();bindBytes=bytes;containsDdl=ddl;
            long cap=Math.min(262144L,Runtime.getRuntime().maxMemory()/512L);overInitialBudget=opCount>64||bindBytes>cap;
            payloadHash=hashR31(operation,task,input,seq,version,ops,before!=null,after!=null);
            identityR31=new DatabaseCoordinator.CommitIdentityR31(key,payloadHash,task,owner,fence,input,seq,version);
        }
        public boolean isEmpty(){return ops.isEmpty()&&before==null&&after==null;}
        /** Copies on export as well: callers cannot mutate the published bind arrays. */
        public List<Op> operationsR31(){List<Op> copy=new ArrayList<>();for(Op op:ops)copy.add(new Op(op.sql,op.args));return java.util.Collections.unmodifiableList(copy);}
    }
    private static Object[] copyArgsR31(Object[] source) {
        if(source==null)return null;Object[] copy=source.clone();
        for(int i=0;i<copy.length;i++) {
            Object value=copy[i];if(value instanceof byte[])copy[i]=((byte[])value).clone();
            else if(value!=null&&!(value instanceof String||value instanceof Long||value instanceof Integer||value instanceof Short||value instanceof Byte||value instanceof Double||value instanceof Float||value instanceof Boolean))
                throw new IllegalArgumentException("R31_MUTABLE_BIND_ARGUMENT:"+value.getClass().getName());
        }
        return copy;
    }
    private static long bindBytesR31(Object[] args) {
        long size=0;if(args!=null)for(Object arg:args){if(arg instanceof byte[])size+=((byte[])arg).length;else if(arg instanceof String)size+=((String)arg).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;else size+=8;}
        return size;
    }
    private static boolean ddlR31(String sql) {
        String canonical=sql.trim().toUpperCase(java.util.Locale.ROOT);
        return canonical.matches("(?s)^(CREATE|ALTER|DROP|VACUUM|REINDEX|ATTACH|DETACH)\\b.*")||canonical.startsWith("PRAGMA ");
    }
    private static String hashR31(String operation,String task,String input,long sequence,String version,List<Op> ops,boolean before,boolean after) {
        try {
            java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
            java.io.DataOutputStream out=new java.io.DataOutputStream(new java.security.DigestOutputStream(new java.io.OutputStream(){@Override public void write(int value){}},digest));
            writeTextR31(out,"R31_IMMUTABLE_BATCH_V1");writeTextR31(out,operation);writeTextR31(out,task);writeTextR31(out,input);out.writeLong(sequence);writeTextR31(out,version);out.writeBoolean(before);out.writeBoolean(after);out.writeInt(ops.size());
            for(Op op:ops){writeTextR31(out,op.sql);out.writeInt(op.args==null?-1:op.args.length);if(op.args!=null)for(Object arg:op.args){
                if(arg==null)out.writeByte(0);else if(arg instanceof byte[]){out.writeByte(1);byte[] data=(byte[])arg;out.writeInt(data.length);out.write(data);}
                else {out.writeByte(2);writeTextR31(out,arg.getClass().getName());writeTextR31(out,String.valueOf(arg));}
            }}
            out.close();StringBuilder hex=new StringBuilder(64);for(byte value:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",value&255));return hex.toString();
        }catch(java.io.IOException|java.security.NoSuchAlgorithmException unavailable){throw new IllegalStateException("R31_BATCH_HASH_FAILED",unavailable);}
    }
    private static void writeTextR31(java.io.DataOutputStream out,String value)throws java.io.IOException {
        byte[] bytes=value.getBytes(java.nio.charset.StandardCharsets.UTF_8);out.writeInt(bytes.length);out.write(bytes);
    }

    private static final java.util.Map<String,long[]> BATCH_INVENTORY_R31=new java.util.LinkedHashMap<>();
    private static synchronized void inventoryR31(String operation,int count,long bytes,boolean over,boolean legacy,boolean ddl) {
        String key=operation.length()>100?operation.substring(0,100):operation;
        if(!BATCH_INVENTORY_R31.containsKey(key)&&BATCH_INVENTORY_R31.size()>=256)key="__OTHER__";
        long[] row=BATCH_INVENTORY_R31.get(key);if(row==null){row=new long[6];BATCH_INVENTORY_R31.put(key,row);}
        row[0]++;row[1]=Math.max(row[1],count);row[2]=Math.max(row[2],bytes);if(over)row[3]++;if(legacy)row[4]++;if(ddl)row[5]++;
    }
    public static synchronized java.util.Map<String,Object> batchInventoryR31() {
        java.util.Map<String,Object> out=new java.util.LinkedHashMap<>();
        for(java.util.Map.Entry<String,long[]> entry:BATCH_INVENTORY_R31.entrySet()){long[] row=entry.getValue();java.util.Map<String,Object> item=new java.util.LinkedHashMap<>();item.put("attempts",row[0]);item.put("maxOps",row[1]);item.put("maxBindBytes",row[2]);item.put("overInitialBudget",row[3]);item.put("legacyWithoutDurableIdentity",row[4]);item.put("runtimeDdlAttempts",row[5]);out.put(entry.getKey(),java.util.Collections.unmodifiableMap(item));}
        return java.util.Collections.unmodifiableMap(out);
    }

    public void setNextDeadline(long wallMs) { nextDeadline = wallMs; }
    public long nextDeadline() { return nextDeadline; }
    public boolean freezeWindow(long now) { long d = nextDeadline; return d > 0 && now < d && d - now < DEADLINE_GUARD_MS; }
    public static boolean p0Writing() { return SettlementExecutionGateR819.isActive() || RealtimePressureGateR811.hasActiveCriticalWork(); }

    /** Blocks the calling research thread for at most 200 ms per attempt; returns false if it had to re-queue
     * (the caller keeps the batch and retries on its next slice). */
    public boolean submit(String operation, Batch batch, java.util.function.BooleanSupplier cancelled) { return submit(operation, batch, cancelled, false); }

    /** @param freezeWrite the R27 live freeze itself: exempt from the deadline guard (it is the work the guard protects),
     *                     still yields to P0 settlement for at most 200 ms. */
    public boolean submit(String operation, Batch batch, java.util.function.BooleanSupplier cancelled, boolean freezeWrite) {
        if(batch==null||cancelled==null)throw new IllegalArgumentException("R31_BATCH_REQUIRED");
        if(batch.isEmpty())return true;
        // Compatibility sinks keep their original object contract. Production snapshots before the first wait.
        Batch frozen=batch;
        if(store!=null){frozen=new Batch();for(Op op:batch.ops)frozen.ops.add(new Op(op.sql,op.args));frozen.before=batch.before;frozen.after=batch.after;}
        final Batch prepared=frozen;long bytes=0;boolean ddl=false;for(Op op:prepared.ops){bytes+=bindBytesR31(op.args);ddl|=ddlR31(op.sql);}
        inventoryR31(operation,prepared.ops.size(),bytes,prepared.ops.size()>64||bytes>Math.min(262144L,Runtime.getRuntime().maxMemory()/512L),true,ddl);
        return submitAttemptR31(cancelled,freezeWrite,()->writer.write(operation,prepared));
    }
    public boolean submit(String operation,ImmutableBatch batch,java.util.function.BooleanSupplier cancelled) {
        return submit(operation,batch,cancelled,false);
    }
    public boolean submit(String operation,ImmutableBatch batch,java.util.function.BooleanSupplier cancelled,boolean freezeWrite) {
        if(batch==null||cancelled==null||!batch.operation.equals(operation))throw new IllegalArgumentException("R31_BATCH_OPERATION_MISMATCH");
        if(batch.isEmpty())return true;
        inventoryR31(operation,batch.opCount,batch.bindBytes,batch.overInitialBudget,false,batch.containsDdl);
        if(batch.containsDdl)throw new DatabaseCoordinator.WriteRejectedR31("DEPENDENCY","R27_"+operation,"SCHEMA_PENDING",DatabaseCoordinator.CommitOutcome.ROLLED_BACK,new IllegalStateException("R31_RUNTIME_DDL_REQUIRES_MIGRATION_OWNER"));
        return submitAttemptR31(cancelled,freezeWrite,()->{
            if(store==null)throw new IllegalStateException("R31_DURABLE_BATCH_REQUIRES_DATABASE");
            DatabaseCoordinator.WriteClass kind=freezeWrite?DatabaseCoordinator.WriteClass.LIVE_FREEZE:DatabaseCoordinator.WriteClass.RESEARCH_DATA;
            store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(DatabaseCoordinator.WriteContext.of(kind,"R27_"+operation),batch.identityR31,
                    db->{execute(db,batch,operation);return 0;},0);
        });
    }
    private boolean submitAttemptR31(java.util.function.BooleanSupplier cancelled,boolean freezeWrite,Runnable write) {
        long start=System.nanoTime();
        while(critical.getAsBoolean()||(!freezeWrite&&freezeWindow(System.currentTimeMillis()))) {
            long waited=(System.nanoTime()-start)/1_000_000L;
            if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted()){stat(waited,true);return false;}
            long remaining=MAX_WAIT_MS-waited;if(remaining<=0){stat(waited,true);return false;}
            try{Thread.sleep(Math.min(4L,remaining));}catch(InterruptedException interrupted){Thread.currentThread().interrupt();stat((System.nanoTime()-start)/1_000_000L,true);return false;}
        }
        long waited=(System.nanoTime()-start)/1_000_000L,remaining=MAX_WAIT_MS-waited;
        if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted()||remaining<=0){stat(waited,true);return false;}
        try(DatabaseCoordinator.WriterDeadlineScopeCP3 deadline=store==null?null:DatabaseCoordinator.withWriterDeadlineCP3(TimeSourceR811.monoNowMs()+remaining)) {
            java.util.function.BooleanSupplier previous=SUBMIT_CANCELLED_R31.get();SUBMIT_CANCELLED_R31.set(cancelled);
            try{write.run();}finally{if(previous==null)SUBMIT_CANCELLED_R31.remove();else SUBMIT_CANCELLED_R31.set(previous);}
        }catch(IllegalStateException failure){if(!writerBusy(failure))throw failure;stat((System.nanoTime()-start)/1_000_000L,true);return false;}
        // Successful transaction/hold time belongs to the coordinator ledger, not the gate-wait counter.
        stat(waited,false);return true;
    }

    /** In-memory counters from actual submit attempts; health flush keeps the existing durable hourly format. */
    public synchronized java.util.Map<String,Object> waitSnapshotR30(){
        java.util.Map<String,Object> out=new java.util.LinkedHashMap<>();out.put("hourStart",hourStart);out.put("submits",submits);out.put("rejectedOrRequeued",requeues);out.put("waitsAtLeast200ms",longWaits);out.put("maxObservedGateOrRejectedAttemptMs",maxWait);out.put("maxWaitAllowanceMs",MAX_WAIT_MS);return java.util.Collections.unmodifiableMap(out);
    }

    static void execute(SQLiteDatabase db, Batch batch, String operation) throws Exception {
        if (!db.inTransaction()) throw new IllegalStateException("R29_BATCH_TRANSACTION_REQUIRED");
        checkCancelledR31();
        if (batch.before != null) require(batch.before.call(db), operation);
        exec(db, batch);
        // Any automatic task facts precede the original final deadline/source guard.
        ResearchStoreR30R1.afterOwnedArtifact(db,"R27_"+operation,RuntimeCoordinatorR26.currentClaimR30());
        if (batch.after != null) require(batch.after.call(db), operation);
        checkCancelledR31();
    }
    static void execute(SQLiteDatabase db,ImmutableBatch batch,String operation)throws Exception {
        if(!db.inTransaction())throw new IllegalStateException("R29_BATCH_TRANSACTION_REQUIRED");
        checkCancelledR31();
        if(batch.before!=null)require(batch.before.call(db),operation);
        for(Op op:batch.ops)db.execSQL(op.sql,copyArgsR31(op.args));
        ResearchStoreR30R1.afterOwnedArtifact(db,"R27_"+operation,RuntimeCoordinatorR26.currentClaimR30());
        if(batch.after!=null)require(batch.after.call(db),operation);
        checkCancelledR31();
    }
    private static void checkCancelledR31()throws InterruptedException {
        java.util.function.BooleanSupplier cancelled=SUBMIT_CANCELLED_R31.get();
        if(Thread.currentThread().isInterrupted()||(cancelled!=null&&cancelled.getAsBoolean()))throw new InterruptedException("R27_CANCELLED");
    }
    private static void require(Integer ok, String operation) {
        if (ok != null && ok < 0) throw new IllegalStateException("R27_WRITE_PRECONDITION:" + operation);
    }

    /** True when the failure is the single writer being busy (deadline/interrupt), as opposed to a real write error. */
    static boolean writerBusy(Throwable failure) {
        if(failure instanceof DatabaseCoordinator.WriteRejectedR31&&((DatabaseCoordinator.WriteRejectedR31)failure).commitOutcome==DatabaseCoordinator.CommitOutcome.UNKNOWN)return false;
        String category=DatabaseCoordinator.failureCategoryR31(failure);return "LOCK_BUSY".equals(category)||"TEMP_TIMEOUT".equals(category);
    }

    private void stat(long waited,boolean requeued) {
        long[] prior=null;long hour=System.currentTimeMillis()/3_600_000L*3_600_000L;
        synchronized(this){
            if(hour!=hourStart){if(hourStart!=0)prior=new long[]{hourStart,submits,longWaits,maxWait,requeues};hourStart=hour;submits=longWaits=maxWait=requeues=0;}
            submits++;if(waited>=MAX_WAIT_MS)longWaits++;maxWait=Math.max(maxWait,waited);if(requeued)requeues++;
        }
        // No queue monitor is held across the sole writer or SQLite I/O.
        if(prior!=null)flushSnapshotR31(prior);
    }
    public void flush() {
        long[] snapshot;synchronized(this){snapshot=new long[]{hourStart,submits,longWaits,maxWait,requeues};}
        flushSnapshotR31(snapshot);
    }
    private void flushSnapshotR31(long[] snapshot) {
        if(snapshot[0]==0||store==null)return;
        try {
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.TELEMETRY,"R27_WRITE_WAIT"),db->{
                db.execSQL("INSERT OR IGNORE INTO r27_write_wait(hour_start) VALUES(?)",new Object[]{snapshot[0]});
                db.execSQL("UPDATE r27_write_wait SET submits=max(submits,?),waits_over_200ms=max(waits_over_200ms,?),max_wait_ms=max(max_wait_ms,?),requeues=max(requeues,?) WHERE hour_start=?",new Object[]{snapshot[1],snapshot[2],snapshot[3],snapshot[4],snapshot[0]});return 0;
            });
        }catch(RuntimeException failure){try{android.util.Log.w("STARCORE_R27","write-wait receipt deferred",failure);}catch(RuntimeException ignored){/* diagnostics do not change business commit */}}
    }

    static void exec(SQLiteDatabase db, Batch b) { for (Op op : b.ops) db.execSQL(op.sql, op.args); }
}
