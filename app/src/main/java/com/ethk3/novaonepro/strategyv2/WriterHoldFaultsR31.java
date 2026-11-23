package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.BuildConfig;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Actual >200ms observations, persisted separately from the completed business transaction. */
public final class WriterHoldFaultsR31 {
    private WriterHoldFaultsR31() {}
    public static final int MAX_PENDING = 32;
    public static final int RESEARCH_BACKPRESSURE_AT = 24;
    private static final long ENTRY_RESERVATION = 16 * 1024;
    private static final ArrayDeque<Event> PENDING = new ArrayDeque<>();
    private static final AtomicBoolean DRAINING = new AtomicBoolean();
    private static long observed, persisted, capacityLoss, drainFailures;
    private static int admitting;
    private static long lastDrainWallR33,lastDrainMsR33;
    private static String lastFailure = "", lastOutcome = "NOT_RUN";
    private static final String COLUMNS = "event_id,task_id,owner_epoch,fence,operation,phase,category,root_fingerprint,minute_bucket,cause_chain_json,stack,stack_hash,last_cause_chain_json,last_stack,last_stack_hash,commit_outcome,boot_id,first_wall_ms,last_wall_ms,first_elapsed_ms,last_elapsed_ms,occurrences,byte_count";

    private static final class Event {
        final String id, payload, task, owner, operation, outcome, boot, details;
        final Long fence;
        final long wall, elapsed, bytes;
        final MemoryBudgetR31.Lease memory;
        Event(String operation, boolean critical, long wait, long hold, boolean failed,
              DatabaseCoordinator.CommitOutcome outcome, long sequence, MemoryBudgetR31.Lease memory) {
            RuntimeCoordinatorR26.Claim claim = RuntimeCoordinatorR26.currentClaimR30();
            this.memory = memory;
            this.operation = RuntimeFaultEvidenceR31.clean(operation, 128);
            task = claim == null ? "database-writer" : RuntimeFaultEvidenceR31.clean(claim.task, 128);
            owner = claim == null ? WriterHoldLedgerR30.PROCESS_SESSION : RuntimeFaultEvidenceR31.clean(claim.owner, 128);
            fence = claim == null ? null : claim.fence;
            this.outcome = outcome == null ? "UNKNOWN" : outcome.name();
            boot = RuntimeFaultEvidenceR31.clean(IssueTimelineWriterR30.bootId(), 128);
            wall = System.currentTimeMillis(); elapsed = android.os.SystemClock.elapsedRealtime();
            id = HashingSU1.fields("R31_WRITER_HOLD_OBSERVATION_V1", WriterHoldLedgerR30.PROCESS_SESSION,
                    Long.toString(sequence), task, owner, String.valueOf(fence), this.operation, Long.toString(elapsed));
            Map<String,Object> facts = new LinkedHashMap<>();
            facts.put("observation", "ACTUAL_WRITER_UNLOCKED_HOLD_OVER_200MS");
            facts.put("operation", this.operation); facts.put("critical", critical);
            facts.put("writerWaitMs", Math.max(0, wait)); facts.put("writerHoldMs", hold);
            facts.put("bodyReportedFailure", failed); facts.put("commitOutcome", this.outcome);
            facts.put("commitOutcomeSource", "DATABASE_COORDINATOR_ATTEMPT_STATE");
            facts.put("outcomeCapturedAt", "AFTER_UNLOCK_BEFORE_DIAGNOSTIC_CALLBACK");
            facts.put("sourceCommit", BuildConfig.SOURCE_COMMIT_R26);
            facts.put("rootCause", "UNKNOWN_NOT_INFERRED_FROM_DURATION");
            facts.put("beginSqlEndBreakdown", "NOT_MEASURED");
            facts.put("gcOrSchedulingAttribution", "NOT_MEASURED");
            facts.put("stackAvailability", "NOT_CAPTURED_NO_BUSINESS_THROWABLE");
            facts.put("captureSite", "DatabaseCoordinator.recordUnlockedWriterR30");
            facts.put("card", "R31-B02-C02");
            facts.put("next", "Inspect the operation SQL, writer timing and device resource evidence; do not repeat a committed business body.");
            details = JsonR27.write(facts);
            payload = HashingSU1.fields(id, details, this.outcome, boot, Long.toString(wall), Long.toString(elapsed));
            bytes = 2048L + 2L * details.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (bytes > ENTRY_RESERVATION / 2) throw new IllegalStateException("R31_WRITER_HOLD_FAULT_ENVELOPE");
        }
        Object[] row() {
            String stack = "NOT_CAPTURED_NO_BUSINESS_THROWABLE", stackHash = StatsR27.sha256(stack);
            return new Object[] {id, task, owner, fence, operation, "WRITER_UNLOCKED", "WRITER_HOLD", id,
                    Math.floorDiv(wall, 60000L), details, stack, stackHash, details, stack, stackHash,
                    outcome, boot, wall, wall, elapsed, elapsed, 1, bytes};
        }
    }

    /** Called after unlocking. No SQL, executor, nested transaction or task-fault-budget update. */
    public static void observe(String operation, boolean critical, long wait, long hold,
                                            boolean failed, DatabaseCoordinator.CommitOutcome outcome) {
        if (hold <= 200) return;
        final long sequence;
        synchronized (WriterHoldFaultsR31.class) {
            sequence = ++observed;
            if (PENDING.size() + admitting >= MAX_PENDING) { capacityLoss++; lastFailure = "QUEUE_CAPACITY_LOSS"; return; }
            admitting++;
        }
        MemoryBudgetR31.Lease memory = null;
        boolean retained = false;
        try {
            MemoryBudgetR31.Admission admission = MemoryBudgetR31.processBudget().tryAcquire(
                    "writer-hold-fault-pending", MemoryBudgetR31.Kind.QUEUED, ENTRY_RESERVATION, () -> false);
            if (!admission.acquired()) {
                synchronized (WriterHoldFaultsR31.class) { lastFailure = "MEMORY_ADMISSION_LOSS:" + admission.reason; }
                return;
            }
            memory = admission.lease;
            Event event = new Event(operation, critical, wait, hold, failed, outcome, sequence, memory);
            synchronized (WriterHoldFaultsR31.class) { PENDING.addLast(event); }
            retained = true;
        } finally {
            synchronized (WriterHoldFaultsR31.class) { admitting--; if (!retained) capacityLoss++; }
            if (!retained && memory != null) memory.close();
        }
    }

    private static void requireReady(SQLiteDatabase db) {
        DatabaseCoordinator.requireSchemaObjectsR31(db, "R31_WRITER_HOLD_FAULT",
                new String[][] {{"r31_runtime_fault", COLUMNS},
                        {"r31_commit_receipt", "commit_key,payload_hash,operation,task_id,owner_epoch,fence,input_snapshot,chunk_seq,operator_version,committed_at"}},
                new String[0][]);
    }

    private static boolean present(SQLiteDatabase db, Event event) {
        try (Cursor c = db.rawQuery("SELECT " + COLUMNS + " FROM r31_runtime_fault WHERE event_id=?", new String[] {event.id})) {
            if (!c.moveToFirst()) return false;
            Object[] expected = event.row();
            for (int column = 0; column < expected.length; column++) {
                Object wanted = expected[column];
                boolean equal = wanted == null ? c.isNull(column) : wanted instanceof Number
                        ? c.getType(column) == Cursor.FIELD_TYPE_INTEGER && c.getLong(column) == ((Number) wanted).longValue()
                        : c.getType(column) == Cursor.FIELD_TYPE_STRING && wanted.equals(c.getString(column));
                if (!equal) throw new IllegalStateException("R31_INTEGRITY_WRITER_HOLD_FAULT_IDENTITY:" + column);
            }
            return true;
        }
    }

    private static void requireQuota(SQLiteDatabase db, Event event) {
        if (present(db, event)) return;
        try (Cursor c = db.rawQuery("SELECT count(*),coalesce(sum(byte_count),0) FROM r31_runtime_fault", null)) {
            if (!c.moveToFirst() || c.getLong(0) >= ResearchPoolR27.MAX_FAULT_SAMPLES_R31
                    || c.getLong(1) + event.bytes > ResearchPoolR27.MAX_FAULT_BYTES_R31)
                throw new IllegalStateException("DEPENDENCY(WRITER_HOLD_FAULT_PROTECTED_CAPACITY_DEBT)");
        }
    }

    /** One bounded receipt per actual Health dispatch. Failed/ambiguous delivery retains its event and lease. */
    /** Use the existing controller and per-event fenced writer, never hold the queue across events. */
    public static void drainControlSliceR32(DbStore store) {
        drainBatchR33(store,4);
    }

    public static boolean drainOne(DbStore store) { return drainBatchR33(store,1)>0; }

    private static int drainBatchR33(DbStore store,int maximum) {
        if(store==null||!DRAINING.compareAndSet(false,true))return 0;
        final long started=android.os.SystemClock.elapsedRealtime();
        try {
            final java.util.ArrayList<Event> batch=new java.util.ArrayList<>();
            synchronized(WriterHoldFaultsR31.class){for(Event e:PENDING){batch.add(e);if(batch.size()>=maximum)break;}}
            if(batch.isEmpty())return 0;
            SQLiteDatabase read=store.readable();
            if(read.inTransaction())throw new IllegalStateException("R31_WRITER_HOLD_FAULT_DRAIN_REQUIRES_SEPARATE_TRANSACTION");
            requireReady(read);
            StringBuilder identities=new StringBuilder(),payloads=new StringBuilder();
            long bindBytes=4096;
            for(Event event:batch){identities.append(event.id);payloads.append(event.payload);bindBytes+=HoldLedgerFlushR30.bindBytesR31(event.row());}
            while(bindBytes>HoldLedgerFlushR30.bindCapR31()&&batch.size()>1){
                Event removed=batch.remove(batch.size()-1);bindBytes-=HoldLedgerFlushR30.bindBytesR31(removed.row());
            }
            if(bindBytes>HoldLedgerFlushR30.bindCapR31())throw new IllegalStateException("DEPENDENCY(WRITER_HOLD_FAULT_BIND_CAPACITY)");
            identities.setLength(0);payloads.setLength(0);
            for(Event event:batch){identities.append(event.id);payloads.append(event.payload);}
            String batchId=StatsR27.sha256(identities.toString()),batchPayload=StatsR27.sha256(payloads.toString());
            DatabaseCoordinator.CommitIdentityR31 identity=new DatabaseCoordinator.CommitIdentityR31(
                "R33_WRITER_HOLD_BATCH:"+batchId,batchPayload,"writer-hold-fault-evidence",WriterHoldLedgerR30.PROCESS_SESSION,
                0,batchId,0,"R33_WRITER_HOLD_BATCH_V1");
            DatabaseCoordinator.WriteContext context=DatabaseCoordinator.WriteContext.of(
                DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_WRITER_HOLD_FAULT");
            DatabaseCoordinator.CommitResultR31<Integer> result=store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(
                context,identity,db->{
                    for(Event event:batch){
                        requireQuota(db,event);
                        db.execSQL("INSERT OR IGNORE INTO r31_runtime_fault("+COLUMNS+") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",event.row());
                        if(!present(db,event))throw new IllegalStateException("R31_INTEGRITY_WRITER_HOLD_FAULT_MISSING");
                    }
                    return batch.size();
                },batch.size());
            if(result.outcome!=DatabaseCoordinator.CommitOutcome.COMMITTED&&result.outcome!=DatabaseCoordinator.CommitOutcome.COMMITTED_DIAGNOSTIC_FAILED)
                throw new IllegalStateException("DEPENDENCY(WRITER_HOLD_FAULT_COMMIT_UNKNOWN)");
            for(Event event:batch)if(!present(read,event))throw new IllegalStateException("R31_INTEGRITY_WRITER_HOLD_FAULT_RECEIPT_WITHOUT_EVENT");
            synchronized(WriterHoldFaultsR31.class){
                for(Event event:batch){if(PENDING.peekFirst()!=event)throw new IllegalStateException("R31_WRITER_HOLD_FAULT_QUEUE_IDENTITY");PENDING.removeFirst();persisted++;}
                lastOutcome=result.outcome.name();lastFailure="";
            }
            for(Event event:batch)event.memory.close();
            return batch.size();
        }catch(RuntimeException unavailable){
            Error fatal=FailureDispositionR31.processFatalCause(unavailable);if(fatal!=null)throw fatal;
            String detail=RuntimeFaultEvidenceR31.clean(unavailable.getClass().getName()+":"+unavailable.getMessage(),512);
            synchronized(WriterHoldFaultsR31.class){drainFailures++;lastFailure=detail;}
            if(com.ethk3.novaonepro.RuntimeFaultArchiveB01.isProtectedDebt(unavailable))
                com.ethk3.novaonepro.RuntimeFaultArchiveB01.request(store,"r31_runtime_fault",detail);
            return 0;
        }finally{
            synchronized(WriterHoldFaultsR31.class){lastDrainWallR33=System.currentTimeMillis();lastDrainMsR33=android.os.SystemClock.elapsedRealtime()-started;}
            DRAINING.set(false);
        }
    }

    // Single primary process. A committed event stays pinned through the live read-back.
    public static synchronized boolean retainsHotOriginalR33(String eventId) {
        if(eventId==null)return true;
        for(Event event:PENDING)if(eventId.equals(event.id))return true;
        return false;
    }

    public static synchronized Map<String,Object> snapshot() {
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("faultArchiveRequestGeneration",com.ethk3.novaonepro.RuntimeFaultArchiveB01.requestedGeneration());
        com.ethk3.novaonepro.RuntimeFaultArchiveB01.View archive=com.ethk3.novaonepro.RuntimeFaultArchiveB01.observation();
        out.put("faultArchiveLastDetail",com.ethk3.novaonepro.RuntimeFaultOriginalStoreR335.processDetail());
        out.put("checkpointRecoveryR335",CheckpointRecoveryR335.processObservation());
        out.put("faultArchiveState",archive.state.name());out.put("faultArchiveObservedElapsedMs",archive.observedAtElapsedMs);
        out.put("faultArchiveNextElapsedMs",archive.nextAttemptElapsedMs);out.put("faultArchiveRetiredThisPage",archive.retiredThisPage);
        out.put("faultArchiveSignalAck",archive.acknowledged);
        out.put("observerPid",android.os.Process.myPid());
        out.put("observerProcessSession",WriterHoldLedgerR30.PROCESS_SESSION);
        out.put("observationWallMs",System.currentTimeMillis());
        out.put("lastDrainWallMs",lastDrainWallR33);out.put("lastDrainMs",lastDrainMsR33);
        out.put("oldestPendingWallMs",PENDING.isEmpty()?null:PENDING.peekFirst().wall);
        out.put("observedOver200ms", observed); out.put("pending", PENDING.size()); out.put("persistedFaultReceipts", persisted);
        out.put("capacityLoss", capacityLoss); out.put("drainFailures", drainFailures); out.put("lastDrainFailure", lastFailure);
        out.put("lastDrainCommitOutcome", lastOutcome); out.put("maxPending", MAX_PENDING);
        out.put("admitting", admitting);
        out.put("researchBackpressureAt", RESEARCH_BACKPRESSURE_AT);
        out.put("researchDeferred", deferResearch());
        out.put("researchResumeCondition", "pending+admitting < 24 after actual durable drain; Critical and telemetry remain admitted");
        out.put("declaredQueueBytes", PENDING.size() * ENTRY_RESERVATION);
        out.put("state", capacityLoss > 0 ? "INCOMPLETE_CAPACITY_LOSS" : PENDING.isEmpty() ? "NO_PENDING_OBSERVATION" : "PENDING_DURABLE_FAULT_RECEIPTS");
        out.put("heapReleaseClaim", false); out.put("processDeathBeforeDrain", "PENDING_MEMORY_OBSERVATION_NOT_DURABLE_UNKNOWN");
        out.put("timingAttribution", "UNKNOWN_UNLESS_SEPARATELY_MEASURED");
        return Collections.unmodifiableMap(out);
    }

    /** Reserve queue room for Critical/telemetry while an independent Health drain catches up. */
    public static synchronized boolean deferResearch() {
        return PENDING.size() + admitting >= RESEARCH_BACKPRESSURE_AT;
    }
}
