package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Durable, model-free materializer for the exact 100/1200 evidence bundle. */
public final class DeferredEvidenceWriterR36 {
    public static final String PENDING = "PENDING",
            RUNNING = "RUNNING",
            RETRY_WAIT = "RETRY_WAIT",
            COMPLETED = "COMPLETED",
            FAILED_TERMINAL = "FAILED_TERMINAL";
    public static final int MODEL_INVOCATIONS = 0;

    private DeferredEvidenceWriterR36() {}

    static boolean shouldContinueDrain(boolean running, int pending) {
        return running && pending != 0;
    }

    static long continuationDelayMs(int pending, boolean realtimePressure) {
        return pending < 0 || realtimePressure ? 5000L : 800L;
    }

    static boolean transientResourceFailure(String error) {
        String e = error == null ? "" : error;
        if (e.contains("SNAPSHOT_HASH_MISMATCH") || e.contains("SNAPSHOT_MISSING")
                || e.contains("CARDINALITY") || e.contains("CURSOR_RANGE") || e.contains("INTEGRITY")) return false;
        return e.contains("CP3_DB_BUSY")
                || e.contains("SQLiteDatabaseLockedException") || e.equals("OOM_CAUGHT")
                || e.contains("E02A_RESEARCH_YIELD:") || e.contains("B05_TIMEOUT")
                || e.contains("InterruptedException");
    }

    /** Reopen only resource failures; preserve snapshot, hash, cursor and original failure evidence. */
    public static int recoverResourceFailures(final DbStore store) {
        if (store == null || AnalysisSingleFlightR36.shared().activeCount() > 0) return 0;
        Integer recovered = store.coordinatorR36().runAtomicWrite("R941_DEFERRED_RESOURCE_RECOVERY",
                new DatabaseCoordinator.WriteCallable<Integer>() {
                    @Override public Integer call(SQLiteDatabase db) {
                        ArrayList<String> targets = new ArrayList<String>();
                        try (Cursor rows = db.rawQuery("SELECT target_issue,last_error FROM " + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                + " WHERE status='FAILED_TERMINAL' AND snapshot_blob IS NOT NULL AND "
                                + "(last_error LIKE '%CP3_DB_BUSY%' OR last_error LIKE '%WRITE_COORDINATION_FAILED%'"
                                + " OR last_error LIKE '%SQLiteDatabaseLockedException%' OR last_error='OOM_CAUGHT'"
                                + " OR last_error LIKE '%E02A_RESEARCH_YIELD:%' OR last_error LIKE '%B05_TIMEOUT%'"
                                + " OR last_error LIKE '%InterruptedException%') ORDER BY target_issue LIMIT 16", null)) {
                            while (rows.moveToNext() && targets.size() < 4)
                                if (transientResourceFailure(rows.getString(1))) targets.add(rows.getString(0));
                        }
                        for (String target : targets) db.execSQL("UPDATE " + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                + " SET status='RETRY_WAIT',lease_owner='',lease_until=0,next_attempt_at=?,updated_at=?,last_stage='R941_RESOURCE_RETRY'"
                                + " WHERE target_issue=? AND status='FAILED_TERMINAL'",
                                new Object[]{System.currentTimeMillis()+5000L,System.currentTimeMillis(),target});
                        return targets.size();
                    }
                }, Integer.valueOf(0));
        return recovered == null ? 0 : recovered.intValue();
    }

    public static DrainReport drainAvailable(DbStore store, int maxJobs, String owner) {
        if (store == null) return new DrainReport(0, 0, 0, "STORE_NULL");
        int attempted = 0, completed = 0, failed = 0;
        String last = "NO_PENDING";
        int bound = Math.max(1, Math.min(16, maxJobs));
        for (int i = 0; i < bound; i++) {
            if (RealtimePressureGateR811.hasActiveCriticalWork()) {
                last = "YIELD_REALTIME";
                break;
            }
            String target = nextRunnable(store, System.currentTimeMillis());
            if (target.length() == 0) break;
            attempted++;
            WriteResult one = writeOne(store, target, safeOwner(owner));
            last = one.status;
            if (COMPLETED.equals(one.status)) completed++;
            else if (!one.success && !"YIELD_REALTIME".equals(one.status)) failed++;
            Thread.yield();
        }
        return new DrainReport(attempted, completed, failed, last);
    }

    public static WriteResult writeOne(
            final DbStore store, final String target, final String owner) {
        return writeOneInternal(store, target, owner, false);
    }

    /**
     * Advances one already-drawn frozen issue by one bounded materialization slice.  This lane is
     * allowed only while the Production analysis owner is actually idle; it prevents a permanent
     * 100/1200 evidence backlog when the coarse realtime-pressure pulse stays asserted between
     * issues.  It still performs no model call and never touches BET authorization.
     */
    public static WriteResult writeOneSettledCatchup(
            final DbStore store, final String owner) {
        if (store == null) return new WriteResult(false, "", "STORE_NULL", 0, 0);
        if (AnalysisSingleFlightR36.shared().activeCount() > 0)
            return new WriteResult(false, "", "YIELD_ACTIVE_ANALYSIS", 0, 0);
        String target = nextRunnableSettled(store, System.currentTimeMillis());
        if (target.length() == 0)
            return new WriteResult(false, "", "NO_SETTLED_PENDING", 0, 0);
        return writeOneInternal(store, target, owner, true);
    }

    /**
     * Bounded settled catch-up that keeps working on the deepest job until it completes.  Progress
     * must outrank issue recency: otherwise every newly arriving issue preempts yesterday's
     * partially materialized job and no job ever reaches 1200 rows.
     */
    public static DrainReport drainSettledCatchup(
            DbStore store, int maxSlices, String owner) {
        if (store == null) return new DrainReport(0, 0, 0, "STORE_NULL");
        int attempted = 0, completed = 0, failed = 0;
        String last = "NO_SETTLED_PENDING";
        int bound = Math.max(1, Math.min(4, maxSlices));
        for (int i = 0; i < bound; i++) {
            if (AnalysisSingleFlightR36.shared().activeCount() > 0) {
                last = "YIELD_ACTIVE_ANALYSIS";
                break;
            }
            WriteResult one = writeOneSettledCatchup(store, safeOwner(owner));
            last = one.status;
            if ("NO_SETTLED_PENDING".equals(one.status)
                    || "YIELD_ACTIVE_ANALYSIS".equals(one.status)) break;
            attempted++;
            if (COMPLETED.equals(one.status)) completed++;
            else if (!one.success) failed++;
            Thread.yield();
        }
        return new DrainReport(attempted, completed, failed, last);
    }

    private static WriteResult writeOneInternal(
            final DbStore store,
            final String target,
            final String owner,
            final boolean settledCatchup) {
        if (store == null || !TargetIssueResolver.isValid(target))
            return new WriteResult(false, target, "INVALID_INPUT", 0, 0);
        if (!settledCatchup && RealtimePressureGateR811.hasActiveCriticalWork())
            return new WriteResult(false, target, "YIELD_REALTIME", 0, 0);
        if (settledCatchup && AnalysisSingleFlightR36.shared().activeCount() > 0)
            return new WriteResult(false, target, "YIELD_ACTIVE_ANALYSIS", 0, 0);
        final long started = System.currentTimeMillis();
        // The service label is shared by drain callers. A lease must identify one claim,
        // so a timed-out claimant cannot publish under a later claim from the same service.
        final String leaseOwner = safeOwner(owner) + ':' + java.util.UUID.randomUUID().toString();
        Claim claim = claim(store, target, leaseOwner, started);
        if (claim == null) return new WriteResult(false, target, "NOT_CLAIMED", 0, 0);
        try {
            if (claim.snapshot == null || claim.snapshot.length == 0)
                throw new IllegalStateException("SNAPSHOT_MISSING");
            if (!claim.snapshotHash.equals(DeferredEvidenceSnapshotR36.sha256(claim.snapshot)))
                throw new IllegalStateException("SNAPSHOT_HASH_MISMATCH");
            DeferredEvidenceSnapshotR36.Snapshot snapshot =
                    DeferredEvidenceSnapshotR36.decodeCached(claim.snapshot,claim.snapshotHash);
            if (!target.equals(snapshot.targetIssue))
                throw new IllegalStateException("SNAPSHOT_TARGET_MISMATCH");
            if (snapshot.numbers.size() != 100 || snapshot.actions.size() != 1200)
                throw new IllegalStateException(
                        "SNAPSHOT_CARDINALITY_"
                                + snapshot.numbers.size()
                                + '_'
                                + snapshot.actions.size());
            if (claim.cursorVersion != 1)
                throw new IllegalStateException("CURSOR_VERSION_" + claim.cursorVersion);
            if (!claim.coreMaterialized) {
                int core = materializeCore(store, snapshot, claim.snapshotHash, leaseOwner, claim.coreCursor);
                return new WriteResult(true, target, "CORE_SLICE_" + claim.coreCursor + "_" + core, Math.min(100,core), 0);
            }
            if (claim.nextActionOrdinal < 0 || claim.nextActionOrdinal > 1200)
                throw new IllegalStateException("CURSOR_RANGE_" + claim.nextActionOrdinal);
            if (claim.nextActionOrdinal < 1200) {
                int from = claim.nextActionOrdinal;
                int to =
                        Math.min(1200, from + AppConfig.R36_DEFERRED_ACTION_CHUNK);
                int advanced = materializeActionChunk(
                        store,
                        snapshot,
                        claim.snapshotHash,
                        leaseOwner,
                        from,
                        to,
                        from / AppConfig.R36_DEFERRED_ACTION_CHUNK);
                return new WriteResult(true, target, "ACTION_SLICE_" + from + '_' + advanced, 100, advanced);
            }
            complete(store, snapshot, claim.snapshotHash, leaseOwner, started);
            return new WriteResult(true, target, COMPLETED, 100, 1200);
        } catch (OutOfMemoryError oom) {
            failOrRetry(store, target, leaseOwner, claim.attempts, "OOM_CAUGHT", started);
            return new WriteResult(false, target, "OOM_CAUGHT", 0, 0);
        } catch (Throwable e) {
            String code=DatabaseCoordinator.describeCauseChain(e,700);
            // Contention is a durable retry outcome, not a process crash. Keep its cause
            // in the job's last_error; corruption and unexpected failures remain crashes.
            if(!transientResourceFailure(code))
                CrashRecorder.recordCaught(store.coordinatorR36().appContextR925(),e,"F01_DEFERRED_MATERIALIZE");
            failOrRetry(store, target, leaseOwner, claim.attempts, code, started);
            return new WriteResult(false, target, trim(code, 180), 0, 0);
        }
    }

    private static Claim claim(
            final DbStore store, final String target, final String owner, final long now) {
        // Large immutable bytes are read outside the sole writer. Claim + compact identity
        // read are one transaction: failed reads/yields cannot leave an orphan RUNNING lease.
        final byte[] frozenBytes;
        final String frozenHash;
        try(Cursor frozen=store.readable().rawQuery("SELECT snapshot_blob,snapshot_hash FROM "
                +DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" WHERE target_issue=?",new String[]{target})) {
            if(!frozen.moveToFirst())return null;
            frozenBytes=frozen.getBlob(0);frozenHash=safe(frozen.getString(1));
        }
        return store.coordinatorR36()
                .runAtomicWrite(
                        "R36_DEFERRED_CLAIM",
                        new DatabaseCoordinator.WriteCallable<Claim>() {
                            @Override
                            public Claim call(SQLiteDatabase db) {
                                db.execSQL(
                                        "UPDATE "
                                                + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                                + " SET status='RUNNING',lease_owner=?,lease_until=?,attempts=attempts+1,updated_at=?,last_stage='CLAIMED',last_error=''"
                                                + " WHERE target_issue=? AND status IN"
                                                + " ('PENDING','RETRY_WAIT','RUNNING') AND"
                                                + " next_attempt_at<=? AND (lease_until=0 OR"
                                                + " lease_until<?)",
                                        new Object[] {
                                            owner,
                                            Long.valueOf(now + AppConfig.R36_DEFERRED_LEASE_MS),
                                            Long.valueOf(now),
                                            target,
                                            Long.valueOf(now),
                                            Long.valueOf(now)
                                        });
                                if (changes(db) != 1) return null;
                                Cursor c = null;
                                try {
                                    c =
                                            db.rawQuery(
                                                    "SELECT snapshot_hash,attempts,"
                                                            + "core_materialized,next_action_ordinal,cursor_version,core_next_ordinal"
                                                        + " FROM "
                                                            + DbHelper
                                                                    .TABLE_R35_DEFERRED_EVIDENCE_JOB
                                                            + " WHERE target_issue=? AND"
                                                            + " status='RUNNING' AND lease_owner=?"
                                                            + " LIMIT 1",
                                                    new String[] {target, owner});
                                    if (!c.moveToFirst() || !frozenHash.equals(safe(c.getString(0))))
                                        throw new IllegalStateException("DEFERRED_CLAIM_SNAPSHOT_IDENTITY_CHANGED");
                                    return new Claim(
                                            frozenBytes,
                                            frozenHash,
                                            c.getInt(1),
                                            c.getInt(2) == 1,
                                            c.getInt(3),
                                            c.getInt(4), c.getInt(5));
                                } finally {
                                    if (c != null) c.close();
                                }
                            }
                        },
                        null);
    }

    private static int materializeCore(
            final DbStore store,
            final DeferredEvidenceSnapshotR36.Snapshot s,
            final String snapshotHash,
            final String owner, final int from) {
        final int total = 121 + s.spans.size();
        if(from<0 || from>=total)throw new IllegalStateException("CORE_CURSOR_RANGE_"+from);
        final int[] advanced = new int[]{from};
        DatabaseCoordinator.TransactionResult<Boolean> out =
                store.coordinatorR36()
                        .runDeferredTransactionR36(
                                s.targetIssue,
                                snapshotHash + "|R927|CORE|FROM|" + from,
                                new DatabaseCoordinator.TransactionCallable<Boolean>() {
                                    @Override
                                    public Boolean call(
                                            SQLiteDatabase db,
                                            DatabaseCoordinator.TransactionContext tx) {
                                        long sliceStarted=TimeSourceR811.monoNowMs();
                                        int cursor=from;
                                        while(cursor<total) {
                                            if(cursor<100)insertNumber(db,s,s.numbers.get(cursor));
                                            else if(cursor==100)insertPermutation(db,s);
                                            else if(cursor<121) {
                                                int p=1+(cursor-101)/2;
                                                if((cursor-101)%2==0)insertPositionSummary(db,s,p);
                                                else insertFunnel(db,s,p);
                                            } else insertSpan(db,s,snapshotHash,s.spans.get(cursor-121),cursor-121);
                                            cursor++;
                                            if(shouldYieldSlice(cursor-from,TimeSourceR811.monoNowMs()-sliceStarted,
                                                    RealtimePressureGateR811.hasActiveCriticalWork()))break;
                                        }
                                        advanced[0]=cursor;
                                        ContentValues pulse = new ContentValues();
                                        pulse.put("status", PENDING);
                                        pulse.put("lease_owner", "");
                                        pulse.put("lease_until", 0L);
                                        pulse.put("attempts", 0);
                                        pulse.put("next_attempt_at", 0L);
                                        pulse.put("updated_at", System.currentTimeMillis());
                                        pulse.put("core_materialized", advanced[0]==total ? 1 : 0);
                                        pulse.put("core_next_ordinal", advanced[0]);
                                        pulse.put("next_action_ordinal", 0);
                                        pulse.put("cursor_version", 1);
                                        pulse.put("number_rows", Math.min(100,advanced[0]));
                                        pulse.put("last_stage", advanced[0]==total ? "CORE_MATERIALIZED" : "CORE_CURSOR_"+advanced[0]);
                                        int n =
                                                db.update(
                                                        DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB,
                                                        pulse,
                                                        "target_issue=? AND status=? AND"
                                                            + " lease_owner=?",
                                                        new String[] {
                                                            s.targetIssue, RUNNING, owner
                                                        });
                                        if (n != 1)
                                            throw new IllegalStateException(
                                                    "DEFERRED_CORE_LEASE_LOST");
                                        return Boolean.TRUE;
                                    }
                                },
                                Boolean.FALSE);
        if (!out.success) throw new IllegalStateException("DEFERRED_CORE_TX:" + out.code,out.failureCause);
        return advanced[0];
    }

    private static int materializeActionChunk(
            final DbStore store,
            final DeferredEvidenceSnapshotR36.Snapshot s,
            final String snapshotHash,
            final String owner,
            final int from,
            final int to,
            final int chunk) {
        final int[] advanced = new int[]{from};
        DatabaseCoordinator.TransactionResult<Boolean> out =
                store.coordinatorR36()
                        .runDeferredTransactionR36(
                                s.targetIssue,
                                snapshotHash + "|R927|ACTION|FROM|" + from,
                                new DatabaseCoordinator.TransactionCallable<Boolean>() {
                                    @Override
                                    public Boolean call(
                                            SQLiteDatabase db,
                                            DatabaseCoordinator.TransactionContext tx) {
                                        long sliceStarted = TimeSourceR811.monoNowMs();
                                        int cursor = from;
                                        while (cursor < to) {
                                            insertAction(db, s, s.actions.get(cursor));
                                            cursor++;
                                            if (shouldYieldSlice(cursor-from, TimeSourceR811.monoNowMs()-sliceStarted,
                                                    RealtimePressureGateR811.hasActiveCriticalWork())) break;
                                        }
                                        advanced[0] = cursor;
                                        long now = System.currentTimeMillis();
                                        ContentValues cv = new ContentValues();
                                        cv.put("status", PENDING);
                                        cv.put("lease_owner", "");
                                        cv.put("lease_until", 0L);
                                        cv.put("attempts", 0);
                                        cv.put("next_attempt_at", 0L);
                                        cv.put("updated_at", now);
                                        cv.put("core_materialized", 1);
                                        cv.put("next_action_ordinal", advanced[0]);
                                        cv.put("cursor_version", 1);
                                        cv.put(
                                                "action_rows",
                                                count(
                                                        db,
                                                        V51Repository.ACTION_SPACE,
                                                        s.targetIssue));
                                        cv.put("last_stage", "ACTION_CURSOR_" + from + "_" + advanced[0]);
                                        int n =
                                                db.update(
                                                        DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB,
                                                        cv,
                                                        "target_issue=? AND status=? AND"
                                                            + " lease_owner=?",
                                                        new String[] {
                                                            s.targetIssue, RUNNING, owner
                                                        });
                                        if (n != 1)
                                            throw new IllegalStateException(
                                                    "DEFERRED_ACTION_LEASE_LOST");
                                        return Boolean.TRUE;
                                    }
                                },
                                Boolean.FALSE);
        if (!out.success)
            throw new IllegalStateException("DEFERRED_ACTION_TX_" + chunk + ':' + out.code,out.failureCause);
        return advanced[0];
    }

    static boolean shouldYieldSlice(int written, long elapsedMs, boolean realtime) {
        return written > 0 && (realtime || elapsedMs >= Batch2ContractCoreR926.DB_SLICE_TARGET_MS);
    }

    private static void complete(
            final DbStore store,
            final DeferredEvidenceSnapshotR36.Snapshot s,
            final String snapshotHash,
            final String owner,
            final long started) {
        DatabaseCoordinator.TransactionResult<Boolean> out =
                store.coordinatorR36()
                        .runDeferredTransactionR36(
                                s.targetIssue,
                                snapshotHash + "|R811|COMPLETE",
                                new DatabaseCoordinator.TransactionCallable<Boolean>() {
                                    @Override
                                    public Boolean call(
                                            SQLiteDatabase db,
                                            DatabaseCoordinator.TransactionContext tx) {
                                        int numbers =
                                                count(db, V51Repository.NUMBER, s.targetIssue);
                                        int actions =
                                                count(
                                                        db,
                                                        V51Repository.ACTION_SPACE,
                                                        s.targetIssue);
                                        int summaries =
                                                count(db, V51Repository.ACTION, s.targetIssue);
                                        int funnels =
                                                count(db, V51Repository.FUNNEL, s.targetIssue);
                                        int permutation =
                                                count(db, V51Repository.PERMUTATION, s.targetIssue);
                                        if (numbers != 100
                                                || actions != 1200
                                                || summaries != 10
                                                || funnels != 10
                                                || permutation != 1)
                                            throw new IllegalStateException(
                                                    "DEFERRED_CARDINALITY_"
                                                            + numbers
                                                            + '_'
                                                            + actions
                                                            + '_'
                                                            + summaries
                                                            + '_'
                                                            + funnels
                                                            + '_'
                                                            + permutation);
                                        if (!exactNumberGrid(db, s.targetIssue))
                                            throw new IllegalStateException(
                                                    "DEFERRED_NUMBER_GRID_NOT_10X10");
                                        if (!exactActionOrdinals(db, s.targetIssue))
                                            throw new IllegalStateException(
                                                    "DEFERRED_ACTION_ORDINAL_OR_HASH_GAP");
                                        for (int p = 1; p <= 10; p++)
                                            if (countPosition(
                                                            db,
                                                            V51Repository.ACTION_SPACE,
                                                            s.targetIssue,
                                                            p)
                                                    != 120)
                                                throw new IllegalStateException(
                                                        "DEFERRED_POSITION_CARDINALITY_" + p);
                                        long now = System.currentTimeMillis();
                                        ContentValues cv = new ContentValues();
                                        cv.put("status", COMPLETED);
                                        cv.put("lease_owner", "");
                                        cv.put("lease_until", 0L);
                                        cv.put("completed_at", now);
                                        cv.put("updated_at", now);
                                        cv.put("number_rows", numbers);
                                        cv.put("action_rows", actions);
                                        cv.put("core_materialized", 1);
                                        cv.put("next_action_ordinal", 1200);
                                        cv.put("cursor_version", 1);
                                        cv.put("last_error", "");
                                        cv.put("last_stage", "COMPLETED");
                                        int n =
                                                db.update(
                                                        DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB,
                                                        cv,
                                                        "target_issue=? AND status=? AND"
                                                            + " lease_owner=?",
                                                        new String[] {
                                                            s.targetIssue, RUNNING, owner
                                                        });
                                        if (n != 1)
                                            throw new IllegalStateException(
                                                    "DEFERRED_COMPLETE_LEASE_LOST");
                                        RuntimeEventHooksR926.fullWithinTransaction(
                                                db, s.targetIssue, snapshotHash, s.actionSpaceHash);
                                        RuntimeHealthLedgerR36.recordWithinTransaction(
                                                db,
                                                "DEFERRED_EVIDENCE_COMPLETED",
                                                "DEF-" + snapshotHash.substring(0, 16),
                                                s.targetIssue,
                                                "SUCCESS",
                                                started,
                                                now,
                                                now - started,
                                                0,
                                                1,
                                                true,
                                                "NUMBER=100|ACTION=1200|MODEL_CALLS=0");
                                        Map<String, String> states = new HashMap<String, String>();
                                        states.put("r36_deferred_status", COMPLETED);
                                        states.put("r36_deferred_target", s.targetIssue);
                                        states.put("r36_deferred_number_rows", "100");
                                        states.put("r36_deferred_action_rows", "1200");
                                        states.put("r36_deferred_model_calls", "0");
                                        DbStore.putStatesR36(db, states);
                                        return Boolean.TRUE;
                                    }
                                },
                                Boolean.FALSE);
        if (!out.success) throw new IllegalStateException("DEFERRED_COMPLETE_TX:" + out.code,out.failureCause);
    }

    private static void failOrRetry(
            final DbStore store,
            final String target,
            final String owner,
            final int attempts,
            final String error,
            final long started) {
        store.coordinatorR36()
                .runAtomicWrite(
                        "R36_DEFERRED_FAILURE",
                        new DatabaseCoordinator.WriteRunnable() {
                            @Override
                            public void run(SQLiteDatabase db) {
                                long now = System.currentTimeMillis();
                                boolean terminal = !transientResourceFailure(error)
                                        && (error.contains("SNAPSHOT_HASH_MISMATCH")
                                                || error.contains("SNAPSHOT_MISSING")
                                                || error.contains("CARDINALITY")
                                                || error.contains("CURSOR_RANGE")
                                                || error.contains("INTEGRITY")
                                                || attempts >= AppConfig.R36_DEFERRED_MAX_ATTEMPTS);
                                ContentValues cv = new ContentValues();
                                cv.put("status", terminal ? FAILED_TERMINAL : RETRY_WAIT);
                                cv.put("lease_owner", "");
                                cv.put("lease_until", 0L);
                                cv.put("updated_at", now);
                                cv.put(
                                        "next_attempt_at",
                                        terminal ? 0L : now + retryDelay(attempts));
                                cv.put("last_error", trim(error, 700));
                                cv.put("last_stage", terminal ? FAILED_TERMINAL : RETRY_WAIT);
                                db.update(
                                        DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB,
                                        cv,
                                        "target_issue=? AND status=? AND lease_owner=?",
                                        new String[] {target, RUNNING, owner});
                                RuntimeHealthLedgerR36.recordWithinTransaction(
                                        db,
                                        "DEFERRED_EVIDENCE_"
                                                + (terminal ? "FAILED_TERMINAL" : "RETRY_WAIT"),
                                        "DEF-" + target,
                                        target,
                                        terminal ? "FAILED_TERMINAL" : "RETRY_WAIT",
                                        started,
                                        now,
                                        now - started,
                                        0,
                                        1,
                                        terminal,
                                        error);
                            }
                        });
    }

    private static void insertNumber(
            SQLiteDatabase db,
            DeferredEvidenceSnapshotR36.Snapshot s,
            DeferredEvidenceSnapshotR36.NumberRow x) {
        ContentValues cv = new ContentValues();
        common(cv, s);
        cv.put("position", x.position);
        cv.put("number", x.number);
        cv.put("raw_score", x.rawScore);
        cv.put("number_p", x.probability);
        cv.put("number_rank", x.rank);
        cv.put("state_id", x.stateId);
        cv.put("domain_id", x.domainId);
        cv.put("expert_hash", x.expertHash);
        cv.put(
                "record_hash",
                V51ChecksumEngine.sha256(s.recordHash + "|N|" + x.position + '|' + x.number));
        long id =
                db.insertWithOnConflict(
                        V51Repository.NUMBER, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id < 0 && !numberMatches(db, s, x))
            throw new IllegalStateException(
                    "DEFERRED_NUMBER_CONFLICT_" + x.position + '_' + x.number);
    }

    private static void insertAction(
            SQLiteDatabase db,
            DeferredEvidenceSnapshotR36.Snapshot s,
            DeferredEvidenceSnapshotR36.ActionRow x) {
        ContentValues cv = new ContentValues();
        common(cv, s);
        cv.put("position", x.position);
        cv.put("n1", x.n1);
        cv.put("n2", x.n2);
        cv.put("n3", x.n3);
        cv.put("n4", x.n4);
        cv.put("raw_action_p", x.rawActionP);
        cv.put("rank_score", x.rankScore);
        cv.put("selected_hit_p", x.selectedHitP);
        cv.put("rank_no", x.rank);
        cv.put("fingerprint", x.fingerprint);
        cv.put("meta_blob", x.metaBlob);
        cv.put("action_hash", x.actionHash);
        cv.put("record_hash", x.recordHash);
        long id =
                db.insertWithOnConflict(
                        V51Repository.ACTION_SPACE, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id < 0
                && !hashMatches(
                        db,
                        V51Repository.ACTION_SPACE,
                        s.targetIssue,
                        x.position,
                        x.n1,
                        x.n2,
                        x.n3,
                        x.recordHash))
            throw new IllegalStateException(
                    "DEFERRED_ACTION_CONFLICT_"
                            + x.position
                            + '_'
                            + x.n1
                            + '_'
                            + x.n2
                            + '_'
                            + x.n3);
    }

    private static void insertPermutation(
            SQLiteDatabase db, DeferredEvidenceSnapshotR36.Snapshot s) {
        ContentValues cv = new ContentValues();
        common(cv, s);
        cv.put("matrix_blob", s.permutationBlob);
        cv.put("row_error", s.permutationRowError);
        cv.put("column_error", s.permutationColumnError);
        cv.put("iterations", s.permutationIterations);
        cv.put("converged", s.permutationConverged ? 1 : 0);
        cv.put("used_fallback", s.permutationUsedFallback ? 1 : 0);
        cv.put("map_blob", s.permutationMapBlob);
        cv.put("matrix_hash", s.permutationHash);
        cv.put("record_hash", s.recordHash);
        long id =
                db.insertWithOnConflict(
                        V51Repository.PERMUTATION, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id < 0
                && !simpleHashMatches(db, V51Repository.PERMUTATION, s.targetIssue, s.recordHash))
            throw new IllegalStateException("DEFERRED_PERMUTATION_CONFLICT");
    }

    private static void insertPositionSummary(
            SQLiteDatabase db, DeferredEvidenceSnapshotR36.Snapshot s, int position) {
        DeferredEvidenceSnapshotR36.ActionRow best = null;
        for (int i = 0; i < s.actions.size(); i++) {
            DeferredEvidenceSnapshotR36.ActionRow x = s.actions.get(i);
            if (x.position == position && (best == null || x.rank < best.rank)) best = x;
        }
        if (best == null) throw new IllegalStateException("DEFERRED_SUMMARY_MISSING_" + position);
        ContentValues cv = new ContentValues();
        common(cv, s);
        cv.put("position", best.position);
        cv.put("n1", best.n1);
        cv.put("n2", best.n2);
        cv.put("n3", best.n3);
        cv.put("n4", best.n4);
        cv.put("raw_action_p", best.rawActionP);
        cv.put("rank_score", best.rankScore);
        cv.put("selected_hit_p", best.selectedHitP);
        cv.put("rank_no", best.rank);
        cv.put("fingerprint", best.fingerprint);
        cv.put("meta_blob", best.metaBlob);
        cv.put("action_hash", best.actionHash);
        String record = V51ChecksumEngine.sha256(s.recordHash + "|SUMMARY|" + position);
        cv.put("record_hash", record);
        long id =
                db.insertWithOnConflict(
                        V51Repository.ACTION, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id < 0 && !summaryMatches(db, s.targetIssue, position, best.actionHash))
            throw new IllegalStateException("DEFERRED_SUMMARY_CONFLICT_" + position);
    }

    private static void insertFunnel(
            SQLiteDatabase db, DeferredEvidenceSnapshotR36.Snapshot s, int position) {
        List<DeferredEvidenceSnapshotR36.NumberRow> rows =
                new ArrayList<DeferredEvidenceSnapshotR36.NumberRow>();
        for (int i = 0; i < s.numbers.size(); i++)
            if (s.numbers.get(i).position == position) rows.add(s.numbers.get(i));
        Collections.sort(
                rows,
                new Comparator<DeferredEvidenceSnapshotR36.NumberRow>() {
                    @Override
                    public int compare(
                            DeferredEvidenceSnapshotR36.NumberRow a,
                            DeferredEvidenceSnapshotR36.NumberRow b) {
                        int p = Double.compare(b.probability, a.probability);
                        return p != 0 ? p : a.number - b.number;
                    }
                });
        if (rows.size() != 10)
            throw new IllegalStateException("DEFERRED_FUNNEL_NUMBERS_" + position);
        StringBuilder trace = new StringBuilder(), final3 = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) trace.append('>');
            trace.append(two(rows.get(i).number));
            if (i < 3) {
                if (i > 0) final3.append(',');
                final3.append(two(rows.get(i).number));
            }
        }
        ContentValues cv = new ContentValues();
        cv.put("target_issue", s.targetIssue);
        cv.put("position", position);
        cv.put("trace_blob", trace.toString());
        cv.put("final3", final3.toString());
        cv.put("matrix_hash", s.permutationHash);
        cv.put("game_code", V51Repository.GAME_CODE);
        cv.put("bundle_hash", s.bundleHash);
        cv.put("record_hash", V51ChecksumEngine.sha256(s.recordHash + "|FUNNEL|" + position));
        long id =
                db.insertWithOnConflict(
                        V51Repository.FUNNEL, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id < 0 && !funnelMatches(db, s.targetIssue, position, s.permutationHash))
            throw new IllegalStateException("DEFERRED_FUNNEL_CONFLICT_" + position);
    }

    private static void insertSpan(
            SQLiteDatabase db,
            DeferredEvidenceSnapshotR36.Snapshot s,
            String snapshotHash,
            RuntimeTraceR36.Span x,
            int index) {
        ContentValues cv = new ContentValues();
        cv.put("target_issue", s.targetIssue);
        cv.put("trace_id", snapshotHash);
        cv.put("span_name", x.stage);
        cv.put("parent_name", "R36_ANALYSIS");
        cv.put("elapsed_ms", x.elapsedMs);
        cv.put("exclusive_ms", x.elapsedMs);
        cv.put("success", x.success ? 1 : 0);
        cv.put("failure_owner", x.success ? "" : x.detail);
        cv.put("created_at", x.startedAt);
        cv.put("game_code", V51Repository.GAME_CODE);
        cv.put("bundle_hash", s.bundleHash);
        cv.put(
                "record_hash",
                V51ChecksumEngine.sha256(
                        s.recordHash + "|SPAN|" + index + '|' + x.stage + '|' + x.elapsedMs));
        db.insertOrThrow(V51Repository.SPAN, null, cv);
    }

    private static void common(ContentValues cv, DeferredEvidenceSnapshotR36.Snapshot s) {
        cv.put("game_code", V51Repository.GAME_CODE);
        cv.put("target_issue", s.targetIssue);
        cv.put("bundle_hash", s.bundleHash);
        cv.put("contract_hash", s.contractHash);
        cv.put("record_hash", s.recordHash);
    }

    private static boolean numberMatches(
            SQLiteDatabase db,
            DeferredEvidenceSnapshotR36.Snapshot s,
            DeferredEvidenceSnapshotR36.NumberRow x) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT number_rank,number_p,bundle_hash,contract_hash FROM "
                                    + V51Repository.NUMBER
                                    + " WHERE target_issue=? AND position=? AND number=? LIMIT 1",
                            new String[] {
                                s.targetIssue, String.valueOf(x.position), String.valueOf(x.number)
                            });
            return c.moveToFirst()
                    && c.getInt(0) == x.rank
                    && Double.compare(c.getDouble(1), x.probability) == 0
                    && s.bundleHash.equals(safe(c.getString(2)))
                    && s.contractHash.equals(safe(c.getString(3)));
        } finally {
            if (c != null) c.close();
        }
    }

    private static boolean hashMatches(
            SQLiteDatabase db,
            String table,
            String target,
            int p,
            int n1,
            int n2,
            int n3,
            String hash) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT record_hash FROM "
                                    + table
                                    + " WHERE target_issue=? AND position=? AND n1=? AND n2=? AND"
                                    + " n3=? LIMIT 1",
                            new String[] {
                                target,
                                String.valueOf(p),
                                String.valueOf(n1),
                                String.valueOf(n2),
                                String.valueOf(n3)
                            });
            return c.moveToFirst() && hash.equals(safe(c.getString(0)));
        } finally {
            if (c != null) c.close();
        }
    }

    private static boolean simpleHashMatches(
            SQLiteDatabase db, String table, String target, String hash) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT record_hash FROM " + table + " WHERE target_issue=? LIMIT 1",
                            new String[] {target});
            return c.moveToFirst() && hash.equals(safe(c.getString(0)));
        } finally {
            if (c != null) c.close();
        }
    }

    private static boolean summaryMatches(
            SQLiteDatabase db, String target, int position, String actionHash) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT action_hash FROM "
                                    + V51Repository.ACTION
                                    + " WHERE target_issue=? AND position=? LIMIT 1",
                            new String[] {target, String.valueOf(position)});
            return c.moveToFirst() && actionHash.equals(safe(c.getString(0)));
        } finally {
            if (c != null) c.close();
        }
    }

    private static boolean funnelMatches(
            SQLiteDatabase db, String target, int position, String matrixHash) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT matrix_hash FROM "
                                    + V51Repository.FUNNEL
                                    + " WHERE target_issue=? AND position=? LIMIT 1",
                            new String[] {target, String.valueOf(position)});
            return c.moveToFirst() && matrixHash.equals(safe(c.getString(0)));
        } finally {
            if (c != null) c.close();
        }
    }

    private static String nextRunnable(DbStore store, long now) {
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT target_issue FROM "
                                            + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                            + " WHERE ((status IN ('PENDING','RETRY_WAIT') AND"
                                            + " next_attempt_at<=?) OR (status='RUNNING' AND"
                                            + " lease_until<?)) ORDER BY core_materialized DESC,"
                                            + " next_action_ordinal DESC,target_issue DESC,"
                                            + " updated_at ASC LIMIT 1",
                                    new String[] {String.valueOf(now), String.valueOf(now)});
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        } finally {
            if (c != null) c.close();
        }
    }

    private static String nextRunnableSettled(DbStore store, long now) {
        Cursor c = null;
        try {
            c = store.readable().rawQuery(
                    "SELECT j.target_issue FROM "
                            + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                            + " j JOIN "
                            + DrawDependencyInputCP12.SOURCE
                            + " d ON d.issue=j.target_issue AND d.dirty=0"
                            + " WHERE ((j.status IN ('PENDING','RETRY_WAIT') AND j.next_attempt_at<=?)"
                            + " OR (j.status='RUNNING' AND j.lease_until<?))"
                            + " ORDER BY j.target_issue ASC,j.next_action_ordinal DESC,"
                            + "j.updated_at ASC LIMIT 1",
                    new String[] {String.valueOf(now), String.valueOf(now)});
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        } finally {
            if (c != null) c.close();
        }
    }

    public static int pendingCount(DbStore store) {
        if (store == null) return 0;
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT COUNT(1) FROM "
                                            + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                            + " WHERE status IN ('PENDING','RUNNING','RETRY_WAIT')",
                                    null);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    public static BacklogStats backlogStats(DbStore store) {
        if (store == null) return BacklogStats.readError("STORE_NULL");
        Cursor c = null;
        try {
            c = store.readable().rawQuery(
                    "SELECT COUNT(1),"
                            + "COALESCE(SUM(CASE WHEN core_materialized=1 THEN 1 ELSE 0 END),0),"
                            + "COALESCE(SUM(next_action_ordinal),0),"
                            + "COALESCE(MAX(next_action_ordinal),0),"
                            + "COALESCE(MIN(CASE WHEN core_materialized=1 THEN next_action_ordinal ELSE 0 END),0),"
                            + "COALESCE(MAX(target_issue),''),COALESCE(MIN(target_issue),'') FROM "
                            + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                            + " WHERE status IN ('PENDING','RUNNING','RETRY_WAIT')",
                    null);
            if (!c.moveToFirst()) return BacklogStats.readError("NO_ROW");
            return new BacklogStats("OK", c.getInt(0), c.getInt(1), c.getLong(2),
                    c.getInt(3), c.getInt(4), safe(c.getString(5)), safe(c.getString(6)), "");
        } catch (Throwable failure) {
            return BacklogStats.readError(
                    failure.getClass().getSimpleName() + ':' + safe(failure.getMessage()));
        } finally {
            if (c != null) c.close();
        }
    }

    /** Read-only per-target truth for reports; schema creation and repair remain startup-owned. */
    public static String statusForTarget(DbStore store, String target) {
        if (store == null) return "READ_ERROR:STORE_NULL";
        if (!TargetIssueResolver.isValid(target)) return "MISSING";
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT status FROM "
                                            + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                            + " WHERE target_issue=? LIMIT 1",
                                    new String[] {target});
            return c.moveToFirst() ? safe(c.getString(0)) : "MISSING";
        } catch (Throwable error) {
            return "READ_ERROR:" + error.getClass().getSimpleName();
        } finally {
            if (c != null) c.close();
        }
    }

    private static int count(SQLiteDatabase db, String table, String target) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT COUNT(1) FROM " + table + " WHERE target_issue=?",
                            new String[] {target});
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    private static int countPosition(SQLiteDatabase db, String table, String target, int p) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT COUNT(1) FROM "
                                    + table
                                    + " WHERE target_issue=? AND position=?",
                            new String[] {target, String.valueOf(p)});
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    private static boolean exactNumberGrid(SQLiteDatabase db, String target) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT COUNT(1),COUNT(DISTINCT position),COUNT(DISTINCT position||':'||number),"
                                    + "MIN(position),MAX(position),MIN(number),MAX(number) FROM "
                                    + V51Repository.NUMBER
                                    + " WHERE target_issue=?",
                            new String[] {target});
            return c.moveToFirst()
                    && c.getInt(0) == 100
                    && c.getInt(1) == 10
                    && c.getInt(2) == 100
                    && c.getInt(3) == 1
                    && c.getInt(4) == 10
                    && c.getInt(5) == 1
                    && c.getInt(6) == 10;
        } finally {
            if (c != null) c.close();
        }
    }

    private static boolean exactActionOrdinals(SQLiteDatabase db, String target) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT COUNT(1),COUNT(DISTINCT rank_no),MIN(rank_no),MAX(rank_no),"
                                    + "COUNT(DISTINCT action_hash),COUNT(DISTINCT record_hash) FROM "
                                    + V51Repository.ACTION_SPACE
                                    + " WHERE target_issue=?",
                            new String[] {target});
            return c.moveToFirst()
                    && c.getInt(0) == 1200
                    && c.getInt(1) == 1200
                    && c.getInt(2) == 1
                    && c.getInt(3) == 1200
                    && c.getInt(4) == 1200
                    && c.getInt(5) == 1200;
        } finally {
            if (c != null) c.close();
        }
    }

    private static int changes(SQLiteDatabase db) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT changes()", null);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    private static long retryDelay(int attempts) {
        long x = 1000L * (1L << Math.min(6, Math.max(0, attempts - 1)));
        return Math.min(60000L, x);
    }

    private static String safeOwner(String x) {
        String v = safe(x);
        return v.length() == 0 ? "R36-DEFERRED-" + Thread.currentThread().getId() : v;
    }

    private static String two(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    private static String safe(String x) {
        return x == null ? "" : x.trim();
    }

    private static String trim(String x, int max) {
        String v = safe(x);
        return v.length() > max ? v.substring(0, max) : v;
    }

    private static final class Claim {
        final byte[] snapshot;
        final String snapshotHash;
        final int attempts, nextActionOrdinal, cursorVersion, coreCursor;
        final boolean coreMaterialized;

        Claim(byte[] b, String h, int a, boolean core, int ordinal, int version, int coreOrdinal) {
            snapshot = b;
            snapshotHash = h;
            attempts = a;
            coreMaterialized = core;
            nextActionOrdinal = ordinal;
            cursorVersion = version;
            coreCursor = coreOrdinal;
        }
    }

    public static final class WriteResult {
        public final boolean success;
        public final String targetIssue, status;
        public final int numberRows, actionRows;

        WriteResult(boolean ok, String target, String status, int n, int a) {
            success = ok;
            targetIssue = safe(target);
            this.status = safe(status);
            numberRows = n;
            actionRows = a;
        }
    }

    public static final class DrainReport {
        public final int attempted, completed, failed;
        public final String status;

        DrainReport(int a, int c, int f, String s) {
            attempted = a;
            completed = c;
            failed = f;
            status = safe(s);
        }
    }

    public static final class BacklogStats {
        public final String readStatus, newestTarget, oldestTarget, error;
        public final int pendingJobs, coreMaterializedJobs, maxActionOrdinal, minActionOrdinal;
        public final long materializedActionRows;

        BacklogStats(String readStatus, int pendingJobs, int coreMaterializedJobs,
                long materializedActionRows, int maxActionOrdinal, int minActionOrdinal,
                String newestTarget, String oldestTarget, String error) {
            this.readStatus = safe(readStatus);
            this.pendingJobs = Math.max(0, pendingJobs);
            this.coreMaterializedJobs = Math.max(0, coreMaterializedJobs);
            this.materializedActionRows = Math.max(0L, materializedActionRows);
            this.maxActionOrdinal = Math.max(0, maxActionOrdinal);
            this.minActionOrdinal = Math.max(0, minActionOrdinal);
            this.newestTarget = safe(newestTarget);
            this.oldestTarget = safe(oldestTarget);
            this.error = safe(error);
        }

        static BacklogStats readError(String error) {
            return new BacklogStats("READ_ERROR", 0, 0, 0L, 0, 0, "", "", error);
        }

        public String summary() {
            return "read=" + readStatus + "|pending=" + pendingJobs + "|core="
                    + coreMaterializedJobs + "|actionRows=" + materializedActionRows
                    + "|ordinalMin/Max=" + minActionOrdinal + '/' + maxActionOrdinal
                    + "|oldest/newest=" + oldestTarget + '/' + newestTarget
                    + "|scheduler=LIVE_DEEPEST_PLUS_OLDEST_SETTLED_R925"
                    + (error.length() == 0 ? "" : "|error=" + error);
        }
    }

public static BacklogStats backlogStatsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return BacklogStats.readError("STORE_NULL");
        Cursor c = null;
        try {
            c = report.select(
                    "SELECT COUNT(1),"
                            + "COALESCE(SUM(CASE WHEN core_materialized=1 THEN 1 ELSE 0 END),0),"
                            + "COALESCE(SUM(next_action_ordinal),0),"
                            + "COALESCE(MAX(next_action_ordinal),0),"
                            + "COALESCE(MIN(CASE WHEN core_materialized=1 THEN next_action_ordinal ELSE 0 END),0),"
                            + "COALESCE(MAX(target_issue),''),COALESCE(MIN(target_issue),'') FROM "
                            + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                            + " WHERE status IN ('PENDING','RUNNING','RETRY_WAIT')",
                    null);
            if (!c.moveToFirst()) return BacklogStats.readError("NO_ROW");
            return new BacklogStats("OK", c.getInt(0), c.getInt(1), c.getLong(2),
                    c.getInt(3), c.getInt(4), safeSnapshotCP4(report, c.getString(5)), safeSnapshotCP4(report, c.getString(6)), "");
        } catch (Throwable failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);} finally {
            if (c != null) c.close();
        }
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String x) {
        return x == null ? "" : x.trim();
    }

public static int pendingCountSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return 0;
        Cursor c = null;
        try {
            c =
                    report.select(
                                    "SELECT COUNT(1) FROM "
                                            + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                            + " WHERE status IN ('PENDING','RUNNING','RETRY_WAIT')",
                                    null);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

public static String statusForTargetSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String target) {
        if (store == null) return "READ_ERROR:STORE_NULL";
        if (!TargetIssueResolver.isValid(target)) return "MISSING";
        Cursor c = null;
        try {
            c =
                    report.select(
                                    "SELECT status FROM "
                                            + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                            + " WHERE target_issue=? LIMIT 1",
                                    new String[] {target});
            return c.moveToFirst() ? safeSnapshotCP4(report, c.getString(0)) : "MISSING";
        } catch (Throwable error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);} finally {
            if (c != null) c.close();
        }
    }
}
