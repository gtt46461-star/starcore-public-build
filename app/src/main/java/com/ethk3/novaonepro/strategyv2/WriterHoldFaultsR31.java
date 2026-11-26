package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.AtomicFile;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.ReportJobJournalCP12;
import com.ethk3.novaonepro.RuntimeFaultOriginalStoreR335;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Actual >200ms observations, persisted separately from the completed business transaction. */
public final class WriterHoldFaultsR31 {
    private WriterHoldFaultsR31() {}
    public static final int MAX_PENDING = 32;
    public static final int RESEARCH_BACKPRESSURE_AT = 24;
    private static final int DRAIN_ROWS = 4;
    private static final String OUTBOX_FORMAT = "R342_WRITER_FAULT_INFLIGHT_1";
    private static final long ENTRY_RESERVATION = 16 * 1024;
    private static final ArrayDeque<Event> PENDING = new ArrayDeque<>();
    private static final AtomicBoolean DRAINING = new AtomicBoolean();
    private static long observed, persisted, capacityLoss, drainFailures;
    private static long firstDroppedSequence, lastDroppedSequence, nextDrainElapsed;
    private static int consecutiveDrainFailures;
    private static String drainStatus = "IDLE";
    private static int durableInFlightRows;
    private static boolean recoveredDifferentProcess;
    private static String durableInFlightHash, lastBatchReceipt, lastBatchPayload;
    private static int lastBatchRows;
    private static int admitting;
    private static long lastDrainWallR33,lastDrainMsR33;
    private static String lastFailure = "", lastOutcome = "NOT_RUN";
    private static long drainAttemptsR343, lastControlTickWallR343, phaseStartedNsR343;
    private static int attemptedRowsR343, readBackRowsR343;
    private static String drainPhaseR343 = "NOT_RUN", failurePhaseR343 = "", failureCategoryR343 = "";
    private static final LinkedHashMap<String,Long> phaseMsR343 = new LinkedHashMap<>();

    private static synchronized void beginDrainAttemptR343() {
        drainAttemptsR343++;
        attemptedRowsR343 = 0;
        readBackRowsR343 = 0;
        failurePhaseR343 = "";
        failureCategoryR343 = "";
        phaseMsR343.clear();
        drainPhaseR343 = "READ_OUTBOX";
        phaseStartedNsR343 = System.nanoTime();
    }

    private static synchronized void phaseR343(String next) {
        long now = System.nanoTime();
        if (phaseStartedNsR343 != 0L) {
            long elapsed = Math.max(0L, (now - phaseStartedNsR343) / 1_000_000L);
            Long previous = phaseMsR343.get(drainPhaseR343);
            phaseMsR343.put(drainPhaseR343, (previous == null ? 0L : previous) + elapsed);
        }
        drainPhaseR343 = next;
        phaseStartedNsR343 = now;
    }
    private static final String COLUMNS = "event_id,task_id,owner_epoch,fence,operation,phase,category,root_fingerprint,minute_bucket,cause_chain_json,stack,stack_hash,last_cause_chain_json,last_stack,last_stack_hash,commit_outcome,boot_id,first_wall_ms,last_wall_ms,first_elapsed_ms,last_elapsed_ms,occurrences,byte_count";

    private static final class Event {
        final String id, payload, task, owner, operation, outcome, boot, details, processSession;
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
            processSession = WriterHoldLedgerR30.PROCESS_SESSION;
            wall = System.currentTimeMillis(); elapsed = android.os.SystemClock.elapsedRealtime();
            id = HashingSU1.fields("R31_WRITER_HOLD_OBSERVATION_V1", processSession,
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

        Event(Map<?, ?> row, String session) throws IOException {
            memory = null;
            processSession = session;
            id = (String) row.get("id");
            task = (String) row.get("task");
            owner = (String) row.get("owner");
            operation = (String) row.get("operation");
            outcome = (String) row.get("outcome");
            boot = (String) row.get("boot");
            details = (String) row.get("details");
            fence = row.get("fence") == null ? null : ((Number) row.get("fence")).longValue();
            wall = ((Number) row.get("wall")).longValue();
            elapsed = ((Number) row.get("elapsed")).longValue();
            bytes = 2048L + 2L * details.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            payload = HashingSU1.fields(id, details, outcome, boot, Long.toString(wall), Long.toString(elapsed));
            if (!id.matches("[a-f0-9]{64}") || bytes > ENTRY_RESERVATION / 2 || wall <= 0 || elapsed < 0
                    || !payload.equals(row.get("payload"))) throw new IOException("R342_FAULT_OUTBOX_EVENT_IDENTITY");
        }

        Map<String, Object> outboxRow() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", id); row.put("task", task); row.put("owner", owner);
            row.put("operation", operation); row.put("outcome", outcome); row.put("boot", boot);
            row.put("details", details); row.put("fence", fence); row.put("wall", wall);
            row.put("elapsed", elapsed); row.put("payload", payload);
            return row;
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
        if (hold <= 200 || "R31_WRITER_HOLD_FAULT".equals(operation)) return;
        final long sequence;
        synchronized (WriterHoldFaultsR31.class) {
            sequence = ++observed;
            if (PENDING.size() + admitting >= MAX_PENDING) { recordDrop(sequence); lastFailure = "QUEUE_CAPACITY_LOSS"; return; }
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
            synchronized (WriterHoldFaultsR31.class) { admitting--; if (!retained) recordDrop(sequence); }
            if (!retained && memory != null) memory.close();
        }
    }

    private static void recordDrop(long sequence) {
        if (capacityLoss++ == 0) firstDroppedSequence = sequence;
        firstDroppedSequence = Math.min(firstDroppedSequence, sequence);
        lastDroppedSequence = Math.max(lastDroppedSequence, sequence);
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

    private static void requireQuota(SQLiteDatabase db, List<Event> newEvents) {
        if (newEvents.isEmpty()) return;
        long bytes = 0L;
        for (Event event : newEvents) bytes += event.bytes;
        try (Cursor c = db.rawQuery("SELECT count(*),coalesce(sum(byte_count),0) FROM r31_runtime_fault", null)) {
            if (!c.moveToFirst() || c.getLong(0) > ResearchPoolR27.MAX_FAULT_SAMPLES_R31 - newEvents.size()
                    || c.getLong(1) > ResearchPoolR27.MAX_FAULT_BYTES_R31 - bytes)
                throw new IllegalStateException("DEPENDENCY(WRITER_HOLD_FAULT_PROTECTED_CAPACITY_DEBT)");
        }
    }

    /** One bounded receipt per actual Health dispatch. Failed/ambiguous delivery retains its event and lease. */
    /** Use the existing controller and per-event fenced writer, never hold the queue across events. */
    public static void drainControlSliceR32(DbStore store) {
        drainBatchR33(store,DRAIN_ROWS);
    }

    public static boolean drainOne(DbStore store) { return drainBatchR33(store,1)>0; }

    private static int drainBatchR33(DbStore store,int maximum) {
        if(store==null||!DRAINING.compareAndSet(false,true))return 0;
        final long started=android.os.SystemClock.elapsedRealtime();
        boolean attempted = false;
        synchronized (WriterHoldFaultsR31.class) { lastControlTickWallR343 = System.currentTimeMillis(); }
        try {
            if (!RuntimeFaultOriginalStoreR335.mainProcess(store.contextForR26())) return 0;
            final File outbox = new File(store.contextForR26().getNoBackupFilesDir(), "r342-writer-fault-inflight.json");
            synchronized(WriterHoldFaultsR31.class){
                if(started<nextDrainElapsed)return 0;
            }
            attempted = true;
            beginDrainAttemptR343();
            final List<Event> batch = readInFlight(outbox);
            final boolean retry = !batch.isEmpty();
            phaseR343("SELECT_QUEUE");
            if (!retry) synchronized (WriterHoldFaultsR31.class) {
                for (Event event : PENDING) {
                    batch.add(event);
                    if (batch.size() == maximum) break;
                }
                if (batch.isEmpty()) {
                    if (!"COMMITTED_READ_BACK".equals(drainStatus)) drainStatus = "IDLE";
                    phaseR343("NO_PENDING");
                    return 0;
                }
            }
            phaseR343("BOUND_BIND_BYTES");
            long bindBytes=4096;
            for(Event event:batch)bindBytes+=HoldLedgerFlushR30.bindBytesR31(event.row());
            long bindLimit=Math.min(HoldLedgerFlushR30.bindCapR31(),com.ethk3.novaonepro.WriteBudgetR341.MAX_BIND_BYTES);
            while(!retry&&bindBytes>bindLimit&&batch.size()>1){
                Event removed=batch.remove(batch.size()-1);bindBytes-=HoldLedgerFlushR30.bindBytesR31(removed.row());
            }
            if(bindBytes>bindLimit)throw new IllegalStateException("DEPENDENCY(WRITER_HOLD_FAULT_BIND_CAPACITY)");
            synchronized (WriterHoldFaultsR31.class) { attemptedRowsR343 = batch.size(); }
            if (!retry) {
                phaseR343("WRITE_OUTBOX");
                writeInFlight(outbox, batch);
            }
            phaseR343("VERIFY_SCHEMA");
            synchronized (WriterHoldFaultsR31.class) { drainStatus = "VERIFY_SCHEMA"; }
            SQLiteDatabase read=store.readable();
            if(read.inTransaction())throw new IllegalStateException("R31_WRITER_HOLD_FAULT_DRAIN_REQUIRES_SEPARATE_TRANSACTION");
            requireReady(read);

            StringBuilder identities = new StringBuilder(), payloads = new StringBuilder();
            for(Event event:batch){identities.append(event.id);payloads.append(event.payload);}
            String batchId=StatsR27.sha256(identities.toString()),batchPayload=StatsR27.sha256(payloads.toString());
            DatabaseCoordinator.CommitIdentityR31 identity=new DatabaseCoordinator.CommitIdentityR31(
                "R33_WRITER_HOLD_BATCH:"+batchId,batchPayload,"writer-hold-fault-evidence",batch.get(0).processSession,
                0,batchId,0,"R33_WRITER_HOLD_BATCH_V1");
            DatabaseCoordinator.WriteContext context=DatabaseCoordinator.WriteContext.of(
                DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_WRITER_HOLD_FAULT")
                .withWorkload(batch.size(),bindBytes,3*batch.size()+2,0,true);
            synchronized (WriterHoldFaultsR31.class) {
                drainStatus = retry ? "RESOLVE_RECEIPT" : "WRITE_RECEIPT";
            }
            DatabaseCoordinator coordinator = store.coordinatorForResearchR952();
            phaseR343("RESOLVE_RECEIPT");
            DatabaseCoordinator.CommitOutcome outcome = retry ? coordinator.resolveCommitReceiptR31(identity)
                    : DatabaseCoordinator.CommitOutcome.ROLLED_BACK;
            if (outcome == DatabaseCoordinator.CommitOutcome.UNKNOWN)
                throw new IllegalStateException("DEPENDENCY(WRITER_HOLD_FAULT_COMMIT_UNKNOWN)");
            if (outcome == DatabaseCoordinator.CommitOutcome.ROLLED_BACK) {
                phaseR343("WRITE_RECEIPT");
                outcome=coordinator.runIdempotentAtomicWriteR31(
                context,identity,db->{
                    List<Event> newEvents = new ArrayList<>(batch.size());
                    for (Event event : batch) if (!present(db, event)) newEvents.add(event);
                    requireQuota(db, newEvents);
                    for(Event event:newEvents){
                        db.execSQL("INSERT OR IGNORE INTO r31_runtime_fault("+COLUMNS+") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",event.row());
                        if(!present(db,event))throw new IllegalStateException("R31_INTEGRITY_WRITER_HOLD_FAULT_MISSING");
                    }
                    return batch.size();
                },batch.size()).outcome;
            }
            if(outcome!=DatabaseCoordinator.CommitOutcome.COMMITTED&&outcome!=DatabaseCoordinator.CommitOutcome.COMMITTED_DIAGNOSTIC_FAILED)
                throw new IllegalStateException("DEPENDENCY(WRITER_HOLD_FAULT_COMMIT_UNKNOWN)");
            phaseR343("READ_BACK_EVENTS");
            for (Event event : batch) {
                if (!present(read, event) && !matchesColdOriginal(store, read, event)) {
                    throw new IllegalStateException("R31_INTEGRITY_WRITER_HOLD_FAULT_RECEIPT_WITHOUT_EVENT");
                }
                synchronized (WriterHoldFaultsR31.class) { readBackRowsR343++; }
            }
            phaseR343("VALIDATE_QUEUE");
            if (WriterHoldLedgerR30.PROCESS_SESSION.equals(batch.get(0).processSession)) {
                synchronized (WriterHoldFaultsR31.class) {
                    java.util.Iterator<Event> head = PENDING.iterator();
                    for (Event event : batch) {
                        if (!head.hasNext() || !head.next().id.equals(event.id)) {
                            throw new IllegalStateException("R31_WRITER_HOLD_FAULT_QUEUE_IDENTITY");
                        }
                    }
                }
            }
            phaseR343("RETIRE_OUTBOX");
            new AtomicFile(outbox).delete();
            if (outbox.exists() || new File(outbox.getPath()+".bak").exists()) {
                throw new IOException("R342_FAULT_OUTBOX_RETIRE_FAILED");
            }
            List<Event> released = new ArrayList<>();
            synchronized(WriterHoldFaultsR31.class){
                for (int i = 0; i < batch.size(); i++)
                    if (WriterHoldLedgerR30.PROCESS_SESSION.equals(batch.get(i).processSession))
                        released.add(PENDING.removeFirst());
                persisted += batch.size();
                lastOutcome=outcome.name();lastFailure="";
                drainStatus="COMMITTED_READ_BACK";nextDrainElapsed=0;consecutiveDrainFailures=0;
                durableInFlightRows = 0; recoveredDifferentProcess = false; durableInFlightHash = null;
                lastBatchReceipt = identity.commitKey; lastBatchPayload = identity.payloadHash; lastBatchRows = batch.size();
            }
            phaseR343("RELEASE_MEMORY");
            for(Event event:released)event.memory.close();
            phaseR343("COMPLETED");
            return batch.size();
        }catch(RuntimeException|IOException unavailable){
            Error fatal=FailureDispositionR31.processFatalCause(unavailable);if(fatal!=null)throw fatal;
            String detail=RuntimeFaultEvidenceR31.clean(unavailable.getClass().getName()+":"+unavailable.getMessage(),512);
            boolean protectedDebt = com.ethk3.novaonepro.RuntimeFaultArchiveB01.isProtectedDebt(unavailable);
            synchronized(WriterHoldFaultsR31.class){
                drainFailures++;lastFailure=detail;
                failurePhaseR343 = drainPhaseR343;
                failureCategoryR343 = FailureDispositionR31.classify(unavailable).category;
                drainStatus = retryStatus(unavailable, protectedDebt);
                long delay = 250L << Math.min(3, consecutiveDrainFailures++);
                long jitter = java.util.concurrent.ThreadLocalRandom.current().nextLong(101L);
                nextDrainElapsed = android.os.SystemClock.elapsedRealtime() + Math.min(5000L, delay + jitter);
            }
            if (protectedDebt)
                com.ethk3.novaonepro.RuntimeFaultArchiveB01.request(store,"r31_runtime_fault",detail);
            return 0;
        }finally{
            synchronized(WriterHoldFaultsR31.class){
                if (attempted) {
                    phaseR343(drainPhaseR343);
                    phaseStartedNsR343 = 0L;
                    lastDrainWallR33=System.currentTimeMillis();
                    lastDrainMsR33=android.os.SystemClock.elapsedRealtime()-started;
                }
            }
            DRAINING.set(false);
        }
    }

    private static String retryStatus(Throwable error, boolean protectedDebt) {
        if (protectedDebt) return "BLOCKED_STORAGE";
        if (error instanceof IOException) return "WAIT_OUTBOX_IO";
        if (error instanceof DatabaseCoordinator.WriteRejectedR31 &&
                ((DatabaseCoordinator.WriteRejectedR31) error).commitOutcome == DatabaseCoordinator.CommitOutcome.UNKNOWN)
            return "COMMIT_UNKNOWN_REQUIRES_RECEIPT";
        String category = FailureDispositionR31.classify(error).category;
        if ("DISK_FULL".equals(category)) return "BLOCKED_STORAGE";
        if ("LOCK_BUSY".equals(category) || "TEMP_TIMEOUT".equals(category)) return "WAIT_LOCK";
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof DatabaseCoordinator.SchemaPendingR31) return "MIGRATION_REQUIRED";
            String message = String.valueOf(cause.getMessage());
            if (message.contains("BIND_CAPACITY")) return "BLOCKED_STORAGE";
            if (message.contains("COMMIT_UNKNOWN")) return "COMMIT_UNKNOWN_REQUIRES_RECEIPT";
        }
        return category;
    }

    private static boolean matchesColdOriginal(DbStore store, SQLiteDatabase db, Event event) throws IOException {
        Map<String, Object> original = RuntimeFaultOriginalStoreR335.readHotCold(
                db, store.contextForR26().getFilesDir(), event.id);
        if (original == null) return false;
        Object[] expected = event.row();
        String[] columns = COLUMNS.split(",");
        for (int i = 0; i < columns.length; i++) {
            Object actual = original.get(columns[i]), value = expected[i];
            if (value instanceof Number) {
                if (!(actual instanceof Number) || ((Number) value).longValue() != ((Number) actual).longValue()) return false;
            } else if (!java.util.Objects.equals(value, actual)) return false;
        }
        return true;
    }

    private static List<Event> readInFlight(File outbox) throws IOException {
        List<Event> batch = new ArrayList<>();
        String raw;
        try {
            raw = ReportJobJournalCP12.readAtomic(outbox, (int) com.ethk3.novaonepro.WriteBudgetR341.MAX_BIND_BYTES);
        } catch (FileNotFoundException missing) {
            if (outbox.exists() || new File(outbox.getPath()+".bak").exists()) throw missing;
            return batch;
        }
        try {
            Map<?, ?> envelope = (Map<?, ?>) JsonR27.parse(raw);
            String wire = (String) envelope.get("body");
            String digest = StatsR27.sha256(wire);
            if (!OUTBOX_FORMAT.equals(envelope.get("format")) || !digest.equals(envelope.get("sha256"))) {
                throw new IOException("R342_FAULT_OUTBOX_HASH");
            }
            Map<?, ?> body = (Map<?, ?>) JsonR27.parse(wire);
            String session = (String) body.get("session");
            List<?> rows = (List<?>) body.get("rows");
            if (session == null || !session.matches("[a-f0-9-]{36}") || rows.isEmpty() || rows.size() > DRAIN_ROWS) {
                throw new IOException("R342_FAULT_OUTBOX_BATCH_BOUND");
            }
            java.util.Set<String> distinct = new java.util.HashSet<>();
            for (Object row : rows) {
                Event event = new Event((Map<?, ?>) row, session);
                if (!distinct.add(event.id)) throw new IOException("R342_FAULT_OUTBOX_EVENT_REPEATED");
                batch.add(event);
            }
            synchronized (WriterHoldFaultsR31.class) {
                durableInFlightHash = digest;
                durableInFlightRows = batch.size();
                recoveredDifferentProcess = !WriterHoldLedgerR30.PROCESS_SESSION.equals(session);
            }
            return batch;
        } catch (RuntimeException malformed) {
            throw new IOException("R342_FAULT_OUTBOX_FORMAT", malformed);
        }
    }

    private static void writeInFlight(File outbox, List<Event> batch) throws IOException {
        List<Object> rows = new ArrayList<>();
        for (Event event : batch) rows.add(event.outboxRow());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("session", batch.get(0).processSession);
        body.put("rows", rows);
        String data = JsonR27.write(body);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("format", OUTBOX_FORMAT);
        envelope.put("body", data);
        envelope.put("sha256", StatsR27.sha256(data));
        ReportJobJournalCP12.writeAtomic(outbox, JsonR27.write(envelope));
        List<Event> saved = readInFlight(outbox);
        if (saved.size() != batch.size()) throw new IOException("R342_FAULT_OUTBOX_READBACK");
        for (int i = 0; i < saved.size(); i++) {
            if (!saved.get(i).payload.equals(batch.get(i).payload)) throw new IOException("R342_FAULT_OUTBOX_PAYLOAD");
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
        out.put("lastControlTickWallMs", lastControlTickWallR343);
        out.put("drainAttempts", drainAttemptsR343);
        out.put("drainActive", DRAINING.get());
        out.put("drainPhase", drainPhaseR343);
        out.put("drainPhaseDurationsMs", new LinkedHashMap<>(phaseMsR343));
        out.put("activePhaseElapsedMs", phaseStartedNsR343 == 0L ? null
                : Math.max(0L, (System.nanoTime()-phaseStartedNsR343)/1_000_000L));
        out.put("lastDrainFailurePhase", failurePhaseR343);
        out.put("lastDrainFailureCategory", failureCategoryR343);
        out.put("attemptedRows", attemptedRowsR343);
        out.put("readBackRows", readBackRowsR343);
        out.put("maximumDrainRows", DRAIN_ROWS);
        out.put("drainTimingContract", "ATTEMPT_ONLY; COOLDOWN_TICKS_DO_NOT_REFRESH_LAST_DRAIN; NO_HARD_REALTIME_CLAIM");
        out.put("oldestPendingWallMs",PENDING.isEmpty()?null:PENDING.peekFirst().wall);
        out.put("oldestPendingAgeMs",PENDING.isEmpty()?0L:android.os.SystemClock.elapsedRealtime()-PENDING.peekFirst().elapsed);
        out.put("drainStatus",drainStatus); out.put("nextDrainElapsedMs",nextDrainElapsed);
        out.put("droppedFirstSequence",firstDroppedSequence); out.put("droppedLastSequence",lastDroppedSequence);
        out.put("observedOver200ms", observed); out.put("pending", PENDING.size()); out.put("persistedFaultReceipts", persisted);
        out.put("capacityLoss", capacityLoss); out.put("drainFailures", drainFailures); out.put("lastDrainFailure", lastFailure);
        out.put("lastDrainCommitOutcome", lastOutcome); out.put("maxPending", MAX_PENDING);
        out.put("durableInFlightRows", durableInFlightRows);
        out.put("durableInFlightHash", durableInFlightHash);
        out.put("recoveredDifferentProcess", recoveredDifferentProcess);
        out.put("lastBatchReceiptKey", lastBatchReceipt);
        out.put("lastBatchPayloadSha256", lastBatchPayload);
        out.put("lastBatchRows", lastBatchRows);
        out.put("admitting", admitting);
        out.put("researchBackpressureAt", RESEARCH_BACKPRESSURE_AT);
        out.put("researchDeferred", deferResearch());
        out.put("researchResumeCondition", "pending+admitting < 24 after actual durable drain; Critical and telemetry remain admitted");
        out.put("declaredQueueBytes", PENDING.size() * ENTRY_RESERVATION);
        out.put("state", capacityLoss > 0 ? "INCOMPLETE_CAPACITY_LOSS" : PENDING.isEmpty() ? "NO_PENDING_OBSERVATION" : "PENDING_DURABLE_FAULT_RECEIPTS");
        out.put("heapReleaseClaim", false); out.put("processDeathBeforeDrain", "OUTBOX_ROWS_RECOVERABLE; UNSTAGED_MEMORY_UNKNOWN");
        out.put("timingAttribution", "UNKNOWN_UNLESS_SEPARATELY_MEASURED");
        return Collections.unmodifiableMap(out);
    }

    /** Reserve queue room for Critical/telemetry while an independent Health drain catches up. */
    public static synchronized boolean deferResearch() {
        return PENDING.size() + admitting >= RESEARCH_BACKPRESSURE_AT;
    }
}
