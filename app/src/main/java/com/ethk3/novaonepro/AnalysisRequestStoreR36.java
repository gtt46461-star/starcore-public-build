package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Durable analysis request/lease state. REQUESTED is never reported as success. */
public final class AnalysisRequestStoreR36 {
    public static final String REQUESTED = "REQUESTED";
    public static final String RUNNING = "RUNNING";
    public static final String RETRY_WAIT = "RETRY_WAIT";
    public static final String CANONICAL_COMMITTED = "CANONICAL_COMMITTED";
    public static final String EXPIRED = "EXPIRED";
    public static final String FAILED_TERMINAL = "FAILED_TERMINAL";
    private final DbStore store;

    public AnalysisRequestStoreR36(DbStore store) {
        if (store == null) throw new IllegalArgumentException("R36_REQUEST_STORE_NULL");
        this.store = store;
    }

    public AnalysisSingleFlightR36.Request ensurePending(
            final String baseIssue,
            final String targetIssue,
            final long suppliedDeadlineAt,
            final String reason) {
        final long deadlineAt=fixedDeadlineF01(baseIssue,targetIssue);
        if(suppliedDeadlineAt!=deadlineAt)throw new IllegalArgumentException("F01_REQUEST_DEADLINE_MISMATCH");
        if(deadlineAt<=System.currentTimeMillis())throw new IllegalStateException("F01_TARGET_DEADLINE_EXPIRED");
        TargetIssueResolver.Resolution exact = TargetIssueResolver.exact(baseIssue, targetIssue);
        if (!exact.valid) throw new IllegalArgumentException("R36_REQUEST_TARGET_MISMATCH");
        AnalysisSingleFlightR36.Request existing = read(targetIssue);
        if (existing != null) {
            AnalysisSingleFlightR36.Request refreshed =
                    rearmStalePredrawDeadlineR947(existing, deadlineAt, reason);
            if (refreshed != null) return refreshed;
            AnalysisSingleFlightR36.Request upgraded =
                    rearmUpgradeFailureR366(existing, deadlineAt, reason);
            if (upgraded != null) return upgraded;
            AnalysisSingleFlightR36.Request repaired =
                    rearmKnownR363PreCommitFailureR364(existing, deadlineAt, reason);
            return (repaired == null ? existing : repaired).withDeadlineAtF01(deadlineAt);
        }
        final long requestStartedMonoR927 = TimeSourceR811.monoNowMs();
        final long now = System.currentTimeMillis();
        final String attemptSeed =
                V51ChecksumEngine.sha256(
                        "R36_REQUEST|"
                                + baseIssue
                                + '|'
                                + targetIssue
                                + '|'
                                + now
                                + '|'
                                + safe(reason));
        DatabaseCoordinator.TransactionResult<AnalysisSingleFlightR36.Request> result =
                store.coordinatorR36()
                        .runTransaction(
                                "R36_ANALYSIS_REQUEST",
                                targetIssue,
                                attemptSeed,
                                new DatabaseCoordinator.TransactionCallable<
                                        AnalysisSingleFlightR36.Request>() {
                                    @Override
                                    public AnalysisSingleFlightR36.Request call(
                                            SQLiteDatabase db,
                                            DatabaseCoordinator.TransactionContext tx) {
                                        AnalysisSingleFlightR36.Request replay =
                                                read(db, targetIssue);
                                        if (replay != null) return replay;
                                        expireSupersededPendingWithinTransaction(
                                                db, targetIssue, now);
                                        long generation =
                                                scalarLong(
                                                        db,
                                                        "SELECT COALESCE(MAX(generation),0)+1 FROM "
                                                                + DbHelper
                                                                        .TABLE_R36_ANALYSIS_REQUEST,
                                                        null,
                                                        1L);
                                        String requestHash =
                                                V51ChecksumEngine.sha256(
                                                        "R36|"
                                                                + baseIssue
                                                                + '|'
                                                                + targetIssue
                                                                + '|'
                                                                + generation
                                                                + '|'
                                                                + now);
                                        ContentValues cv = new ContentValues();
                                        cv.put("target_issue", targetIssue);
                                        cv.put("base_issue", baseIssue);
                                        cv.put("status", REQUESTED);
                                        cv.put("generation", generation);
                                        cv.put("request_at", now);
                                        cv.put("deadline_at", Math.max(0L, deadlineAt));
                                        cv.put("last_stage", "ANALYSIS_REQUESTED");
                                        cv.put("request_hash", requestHash);
                                        cv.put("next_attempt_at", now);
                                        cv.put(
                                                "repair_version",
                                                R36AnalysisLeasePolicyR365.REPAIR_VERSION);
                                        cv.put("last_transition_at", now);
                                        db.insertOrThrow(
                                                DbHelper.TABLE_R36_ANALYSIS_REQUEST, null, cv);
                                        AnalysisSingleFlightR36.Request created =
                                                new AnalysisSingleFlightR36.Request(
                                                        baseIssue,
                                                        targetIssue,
                                                        generation,
                                                        now,
                                                        deadlineAt,
                                                        reason,
                                                        requestHash,
                                                        "");
                                        RuntimeEventLedgerR926.appendWithinTransaction(
                                                db,
                                                RuntimeEventLedgerR926.Event.of(
                                                        targetIssue,
                                                        String.valueOf(generation),
                                                        created.attemptId(),
                                                        "REQUESTED",
                                                        "REQUESTED",
                                                        RuntimeEventLedgerR926.RANK_REQUESTED,
                                                        "base=" + baseIssue + "|reason=" + safe(reason)
                                                                + "|deadline=" + deadlineAt,
                                                        "AnalysisRequestStoreR36",
                                                        targetIssue + "|" + created.attemptId() + "|REQUESTED"));
                                        RuntimeObservabilityR925.registerRequest(created, requestStartedMonoR927);
                                        // R30-B02-01: NEXT_REQUESTED + NEXT_DEADLINE on the target-issue timeline row.
                                        com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30.mark(db, targetIssue, com.ethk3.novaonepro.strategyv2.IssueTimelineR30.Stage.NEXT_REQUESTED);
                                        if (deadlineAt > 0L) com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30.deadline(db, targetIssue, deadlineAt);
                                        return created;
                                    }
                                },
                                null);
        if (!result.success || result.value == null) {
            AnalysisSingleFlightR36.Request replay = read(targetIssue);
            if (replay != null) return replay;
            throw new IllegalStateException("R36_REQUEST_PERSIST_FAILED:" + result.code);
        }
        return result.value;
    }

    /**
     * R947 pre-draw deadline refresh. A durable request may sit behind a prior owner long enough
     * that its immutable execution deadline is nearly exhausted before it ever acquires the
     * analysis thread. Reuse of that stale deadline makes the first expensive checkpoint fail
     * immediately even though the target is still legally pre-draw.
     *
     * <p>This is not a post-draw retry and it never changes a canonical row. It creates a new
     * generation/hash only when there is no live in-process owner, the target is undrawn, no
     * canonical observation exists, and the durable request has less than the minimum useful
     * execution window remaining. A stale RUNNING row is eligible only after its lease expires.
     */
    private AnalysisSingleFlightR36.Request rearmStalePredrawDeadlineR947(
            final AnalysisSingleFlightR36.Request existing,
            final long requestedDeadlineAt,
            final String reason) {
        // F01: queue delay does not create a new period. Legacy now+55s rearm is retired.
        return null;
    }

    /**
     * Refreshes only the execution deadline of an already-created pre-draw request. The PIT
     * identity (base/target/generation/requestAt/requestHash/historyHash) is immutable. This closes
     * the queue-aging defect where a valid latest request could inherit only a few milliseconds of
     * its original 55s scheduling window after a prior single-flight owner released.
     */
    public AnalysisSingleFlightR36.Request refreshExecutionWindow(
            final AnalysisSingleFlightR36.Request request,
            final long minimumRemainingMs,
            final long now,
            final String reason) {
        if(request==null||!request.valid())return request;
        // F01: minimumRemainingMs is an observation budget, never permission to extend a period.
        return request.withDeadlineAtF01(fixedDeadlineF01(request.baseIssue,request.targetIssue));
    }

    static boolean executionWindowTooShort(AnalysisSingleFlightR36.Request request, long now, long minimum) {
        if (request == null || request.deadlineAt <= 0L) return false;
        return request.deadlineAt - now < Math.max(1L, minimum);
    }

    /**
     * Overwrite-install bridge for pre-commit rows terminalized by an older repair, including the
     * R3.6.5 action-snapshot contract fault. It is intentionally one-shot, requires an undrawn
     * target and cannot touch a canonical observation.
     */
    private AnalysisSingleFlightR36.Request rearmUpgradeFailureR366(
            final AnalysisSingleFlightR36.Request existing,
            final long deadlineAt,
            final String reason) {
        if (existing == null || !existing.valid() || AnalysisSingleFlightR36.shared().activeCount() != 0)
            return null;
        final long requestStartedMonoR927 = TimeSourceR811.monoNowMs();
        final long now = System.currentTimeMillis();
        if(deadlineAt!=fixedDeadlineF01(existing.baseIssue,existing.targetIssue)||deadlineAt<=now)return null;
        final String key =
                V51ChecksumEngine.sha256(
                        "R366_UPGRADE_REARM|" + existing.targetIssue + '|' + existing.requestHash + '|' + now);
        DatabaseCoordinator.TransactionResult<AnalysisSingleFlightR36.Request> result =
                store.coordinatorR36()
                        .runTransaction(
                                "R366_ANALYSIS_UPGRADE_REARM",
                                existing.targetIssue,
                                key,
                                new DatabaseCoordinator.TransactionCallable<
                                        AnalysisSingleFlightR36.Request>() {
                                    @Override
                                    public AnalysisSingleFlightR36.Request call(
                                            SQLiteDatabase db,
                                            DatabaseCoordinator.TransactionContext tx) {
                                        DurableRow row = readDurableRow(db, existing.targetIssue);
                                        if (row == null) return null;
                                        boolean drawn =
                                                scalarLong(
                                                                db,
                                                                "SELECT COUNT(1) FROM "
                                                                        + DbHelper.TABLE_DRAWS
                                                                        + " WHERE issue=?",
                                                                new String[] {existing.targetIssue},
                                                                0L)
                                                        > 0L;
                                        boolean canonical = canonicalExists(db, existing.targetIssue);
                                        if (!R36AnalysisLeasePolicyR365.canUpgradeRearm(
                                                row.status,
                                                row.repairVersion,
                                                drawn,
                                                canonical,
                                                false)) return null;

                                        long generation = Math.max(existing.generation + 1L, row.generation + 1L);
                                        String requestHash =
                                                V51ChecksumEngine.sha256(
                                                        "R366_REARM|"
                                                                + row.baseIssue
                                                                + '|'
                                                                + row.targetIssue
                                                                + '|'
                                                                + generation
                                                                + '|'
                                                                + now
                                                                + '|'
                                                                + safe(reason));
                                        ContentValues cv = new ContentValues();
                                        cv.put("status", REQUESTED);
                                        cv.put("generation", generation);
                                        cv.put("request_at", now);
                                        cv.put("deadline_at", deadlineAt);
                                        cv.put("started_at", 0L);
                                        cv.put("finished_at", 0L);
                                        cv.put("lease_owner", "");
                                        cv.put("lease_until", 0L);
                                        cv.put("attempts", 0L);
                                        cv.put("next_attempt_at", now);
                                        cv.put("last_stage", "R366_UPGRADE_REARM");
                                        cv.put("first_error_code", row.firstError.length() == 0 ? row.error : row.firstError);
                                        cv.put("error_code", "");
                                        cv.put("history_hash", "");
                                        cv.put("canonical_hash", "");
                                        cv.put("request_hash", requestHash);
                                        cv.put(
                                                "repair_version",
                                                R36AnalysisLeasePolicyR365.REPAIR_VERSION);
                                        cv.put("last_transition_at", now);
                                        int n =
                                                db.update(
                                                        DbHelper.TABLE_R36_ANALYSIS_REQUEST,
                                                        cv,
                                                        "target_issue=? AND generation=? AND request_hash=?"
                                                                + " AND status IN (?,?)",
                                                        new String[] {
                                                            row.targetIssue,
                                                            String.valueOf(row.generation),
                                                            row.requestHash,
                                                            EXPIRED,
                                                            FAILED_TERMINAL
                                                        });
                                        if (n != 1)
                                            throw new IllegalStateException(
                                                    "R366_UPGRADE_REARM_FENCE_" + n);
                                        java.util.HashMap<String, String> states =
                                                new java.util.HashMap<String, String>();
                                        states.put("r366_rearmed_analysis_target", row.targetIssue);
                                        states.put("r366_rearmed_analysis_old_status", row.status);
                                        states.put("r366_rearmed_analysis_old_error", row.error);
                                        states.put("r366_rearmed_analysis_at", String.valueOf(now));
                                        DbStore.putStatesR36(db, states);
                                        AnalysisSingleFlightR36.Request rearmed =
                                                new AnalysisSingleFlightR36.Request(
                                                        row.baseIssue,
                                                        row.targetIssue,
                                                        generation,
                                                        now,
                                                        deadlineAt,
                                                        "R366_UPGRADE_REARM|" + safe(reason),
                                                        requestHash,
                                                        "");
                                        RuntimeEventLedgerR926.appendWithinTransaction(
                                                db,
                                                RuntimeEventLedgerR926.Event.of(
                                                        rearmed.targetIssue,
                                                        String.valueOf(rearmed.generation),
                                                        rearmed.attemptId(),
                                                        "REQUESTED",
                                                        "R366_UPGRADE_REARM",
                                                        RuntimeEventLedgerR926.RANK_REQUESTED,
                                                        "previous_status=" + row.status + "|previous_error=" + safe(row.error),
                                                        "AnalysisRequestStoreR36",
                                                        rearmed.targetIssue + "|" + rearmed.attemptId() + "|REQUESTED"));
                                        RuntimeObservabilityR925.registerRequest(rearmed, requestStartedMonoR927);
                                        return rearmed;
                                    }
                                },
                                null);
        return result.success ? result.value : null;
    }

    /**
     * One-time upgrade bridge for the deterministic R3.6.3 hash/warm-up failures. Only a known
     * pre-commit error may be rearmed, the target must still be undrawn, and no in-process analysis
     * owner may exist. Canonical rows are never touched.
     */
    private AnalysisSingleFlightR36.Request rearmKnownR363PreCommitFailureR364(
            final AnalysisSingleFlightR36.Request existing,
            final long deadlineAt,
            final String reason) {
        if (existing == null
                || !existing.valid()
                || AnalysisSingleFlightR36.shared().activeCount() != 0
                || existing.targetIssue.equals(
                        store.getState("r364_rearmed_analysis_target", ""))) return null;
        final long requestStartedMonoR927 = TimeSourceR811.monoNowMs();
        final long now = System.currentTimeMillis();
        if(deadlineAt!=fixedDeadlineF01(existing.baseIssue,existing.targetIssue)||deadlineAt<=now)return null;
        final String transactionKey =
                V51ChecksumEngine.sha256(
                        "R364_REQUEST_REARM|" + existing.targetIssue + '|' + now);
        DatabaseCoordinator.TransactionResult<AnalysisSingleFlightR36.Request> result =
                store.coordinatorR36()
                        .runTransaction(
                                "R364_ANALYSIS_REQUEST_REARM",
                                existing.targetIssue,
                                transactionKey,
                                new DatabaseCoordinator.TransactionCallable<
                                        AnalysisSingleFlightR36.Request>() {
                                    @Override
                                    public AnalysisSingleFlightR36.Request call(
                                            SQLiteDatabase db,
                                            DatabaseCoordinator.TransactionContext tx) {
                                        Cursor c = null;
                                        String status = "", error = "", repair = "";
                                        try {
                                            c =
                                                    db.rawQuery(
                                                            "SELECT status,error_code,repair_version FROM "
                                                                    + DbHelper
                                                                            .TABLE_R36_ANALYSIS_REQUEST
                                                                    + " WHERE target_issue=? LIMIT 1",
                                                            new String[] {existing.targetIssue});
                                            if (!c.moveToFirst()) return null;
                                            status = safe(c.getString(0));
                                            error = safe(c.getString(1));
                                            repair = safe(c.getString(2));
                                        } finally {
                                            if (c != null) c.close();
                                        }
                                        if (!FAILED_TERMINAL.equals(status)
                                                || R36AnalysisLeasePolicyR365.REPAIR_VERSION.equals(repair)
                                                || !isKnownR363RepairableError(error)) return null;
                                        if (scalarLong(
                                                                db,
                                                                "SELECT COUNT(1) FROM "
                                                                        + DbHelper.TABLE_DRAWS
                                                                        + " WHERE issue=?",
                                                                new String[] {existing.targetIssue},
                                                                0L)
                                                        > 0L) return null;
                                        long generation =
                                                scalarLong(
                                                        db,
                                                        "SELECT COALESCE(MAX(generation),0)+1 FROM "
                                                                + DbHelper
                                                                        .TABLE_R36_ANALYSIS_REQUEST,
                                                        null,
                                                        existing.generation + 1L);
                                        String requestHash =
                                                V51ChecksumEngine.sha256(
                                                        "R364_REARM|"
                                                                + existing.baseIssue
                                                                + '|'
                                                                + existing.targetIssue
                                                                + '|'
                                                                + generation
                                                                + '|'
                                                                + now
                                                                + '|'
                                                                + safe(reason));
                                        ContentValues cv = new ContentValues();
                                        cv.put("status", REQUESTED);
                                        cv.put("generation", generation);
                                        cv.put("request_at", now);
                                        cv.put("deadline_at", deadlineAt);
                                        cv.put("started_at", 0L);
                                        cv.put("finished_at", 0L);
                                        cv.put("lease_owner", "");
                                        cv.put("lease_until", 0L);
                                        cv.put("attempts", 0);
                                        cv.put("last_stage", "R364_KNOWN_ABI_REARM");
                                        cv.put("error_code", "");
                                        cv.put("history_hash", "");
                                        cv.put("canonical_hash", "");
                                        cv.put("request_hash", requestHash);
                                        cv.put("next_attempt_at", now);
                                        cv.put(
                                                "repair_version",
                                                R36AnalysisLeasePolicyR365.REPAIR_VERSION);
                                        cv.put("last_transition_at", now);
                                        int updated =
                                                db.update(
                                                        DbHelper.TABLE_R36_ANALYSIS_REQUEST,
                                                        cv,
                                                        "target_issue=? AND generation=? AND request_hash=?"
                                                                + " AND status=? AND repair_version<>?",
                                                        new String[] {
                                                            existing.targetIssue,
                                                            String.valueOf(existing.generation),
                                                            existing.requestHash,
                                                            FAILED_TERMINAL,
                                                            R36AnalysisLeasePolicyR365.REPAIR_VERSION
                                                        });
                                        if (updated != 1)
                                            throw new IllegalStateException(
                                                    "R364_REQUEST_REARM_CARDINALITY_" + updated);
                                        java.util.HashMap<String, String> states =
                                                new java.util.HashMap<String, String>();
                                        states.put(
                                                "r364_rearmed_analysis_target",
                                                existing.targetIssue);
                                        states.put("r364_rearmed_analysis_old_error", error);
                                        states.put("r364_rearmed_analysis_at", String.valueOf(now));
                                        DbStore.putStatesR36(db, states);
                                        AnalysisSingleFlightR36.Request rearmed =
                                                new AnalysisSingleFlightR36.Request(
                                                        existing.baseIssue,
                                                        existing.targetIssue,
                                                        generation,
                                                        now,
                                                        deadlineAt,
                                                        "R364_KNOWN_ABI_REARM|" + safe(reason),
                                                        requestHash,
                                                        "");
                                        RuntimeEventLedgerR926.appendWithinTransaction(
                                                db,
                                                RuntimeEventLedgerR926.Event.of(
                                                        rearmed.targetIssue,
                                                        String.valueOf(rearmed.generation),
                                                        rearmed.attemptId(),
                                                        "REQUESTED",
                                                        "R364_KNOWN_ABI_REARM",
                                                        RuntimeEventLedgerR926.RANK_REQUESTED,
                                                        "previous_error=" + safe(error),
                                                        "AnalysisRequestStoreR36",
                                                        rearmed.targetIssue + "|" + rearmed.attemptId() + "|REQUESTED"));
                                        RuntimeObservabilityR925.registerRequest(rearmed, requestStartedMonoR927);
                                        return rearmed;
                                    }
                                },
                                null);
        return result.success ? result.value : null;
    }

    static boolean isKnownR363RepairableError(String error) {
        String value = safe(error);
        return value.indexOf("HISTORY_HASH_MISMATCH") >= 0
                || value.indexOf("V51_HISTORY_LT_12") >= 0
                || value.indexOf("R36_HISTORY_HASH_CONFLICT") >= 0;
    }

    private long fixedDeadlineF01(String base,String target) {
        TargetIssueResolver.Resolution exact=TargetIssueResolver.exact(base,target);
        if(!exact.valid)throw new IllegalArgumentException("F01_REQUEST_TARGET_MISMATCH");
        Models.Draw draw=store.getDrawByIssue(base);
        if(draw==null||!base.equals(draw.issue))throw new IllegalStateException("F01_REQUEST_BASE_MISSING");
        Models.Draw latest=store.getLatestDraw();
        if(latest==null||!base.equals(latest.issue))throw new IllegalStateException("F01_REQUEST_BASE_SUPERSEDED");
        return TargetIssueResolver.predrawDeadlineWallR20261008(draw);
    }

    public ClaimResult claimDetailed(
            final AnalysisSingleFlightR36.Request request, final String owner, final long now) {
        if (request == null || !request.valid() || safe(owner).length() == 0)
            return ClaimResult.denied(
                    R36AnalysisLeasePolicyR365.Decision.STALE_REQUEST,
                    "CLAIM_INPUT_INVALID",
                    0L);
        final long fixedDeadline=fixedDeadlineF01(request.baseIssue,request.targetIssue);
        final String key =
                V51ChecksumEngine.sha256(
                        "R366_CLAIM|" + request.targetIssue + '|' + owner + '|' + now);
        DatabaseCoordinator.TransactionResult<ClaimResult> result =
                store.coordinatorR36()
                        .runTransaction(
                                "R366_ANALYSIS_CLAIM",
                                request.targetIssue,
                                key,
                                new DatabaseCoordinator.TransactionCallable<ClaimResult>() {
                                    @Override
                                    public ClaimResult call(
                                            SQLiteDatabase db,
                                            DatabaseCoordinator.TransactionContext tx) {
                                        DurableRow row = readDurableRow(db, request.targetIssue);
                                        if (row == null)
                                            return ClaimResult.denied(
                                                    R36AnalysisLeasePolicyR365.Decision.STALE_REQUEST,
                                                    "REQUEST_ROW_MISSING",
                                                    0L);
                                        boolean drawn =
                                                scalarLong(
                                                                db,
                                                                "SELECT COUNT(1) FROM "
                                                                        + DbHelper.TABLE_DRAWS
                                                                        + " WHERE issue=?",
                                                                new String[] {request.targetIssue},
                                                                0L)
                                                        > 0L;
                                        boolean canonical = canonicalExists(db, request.targetIssue);
                                        R36AnalysisLeasePolicyR365.Decision decision =
                                                R36AnalysisLeasePolicyR365.decide(
                                                        row.status,
                                                        row.generation,
                                                        row.requestHash,
                                                        request.generation,
                                                        request.requestHash,
                                                        row.leaseOwner,
                                                        owner,
                                                        row.leaseUntil,
                                                        row.nextAttemptAt,
                                                        now,
                                                        drawn,
                                                        canonical);
                                        if (decision
                                                == R36AnalysisLeasePolicyR365.Decision
                                                        .CANONICAL_ALREADY) {
                                            reconcileCanonicalRow(db, row, now);
                                            return ClaimResult.denied(
                                                    decision, "CANONICAL_ALREADY", 0L);
                                        }
                                        long currentBaseTime=scalarLong(db,"SELECT open_time FROM "+DbHelper.TABLE_DRAWS
                                                +" WHERE issue=? AND dirty=0",new String[]{request.baseIssue},0L);
                                        if(fixedDeadline<=System.currentTimeMillis()
                                                || request.deadlineMonoAt<=TimeSourceR811.monoNowMs()
                                                || request.deadlineAt!=fixedDeadline
                                                || currentBaseTime<=0L || currentBaseTime!=fixedDeadline-60000L) {
                                            ContentValues expired=new ContentValues();
                                            expired.put("status",EXPIRED);expired.put("finished_at",now);
                                            expired.put("lease_owner","");expired.put("lease_until",0L);
                                            expired.put("next_attempt_at",0L);expired.put("last_stage","FIXED_PERIOD_DEADLINE_EXPIRED");
                                            expired.put("error_code","FIXED_PERIOD_DEADLINE_EXPIRED");
                                            if(row.firstError.length()==0)expired.put("first_error_code","FIXED_PERIOD_DEADLINE_EXPIRED");
                                            expired.put("last_transition_at",now);
                                            db.update(DbHelper.TABLE_R36_ANALYSIS_REQUEST,expired,
                                                    "target_issue=? AND generation=? AND request_hash=? AND status<>?",
                                                    new String[]{row.targetIssue,String.valueOf(row.generation),row.requestHash,CANONICAL_COMMITTED});
                                            return ClaimResult.denied(R36AnalysisLeasePolicyR365.Decision.STALE_REQUEST,
                                                    "FIXED_PERIOD_DEADLINE_EXPIRED",0L);
                                        }
                                        if (decision
                                                == R36AnalysisLeasePolicyR365.Decision
                                                        .TARGET_ALREADY_DRAWN) {
                                            terminalizeDrawnRow(db, row, now);
                                            return ClaimResult.denied(
                                                    decision, "TARGET_ALREADY_DRAWN", 0L);
                                        }
                                        if (decision
                                                == R36AnalysisLeasePolicyR365.Decision
                                                        .DEFER_ACTIVE_OWNER)
                                            return ClaimResult.denied(
                                                    decision,
                                                    "ACTIVE_OWNER:" + row.leaseOwner,
                                                    row.leaseUntil);
                                        if (decision
                                                == R36AnalysisLeasePolicyR365.Decision.NOT_DUE)
                                            return ClaimResult.denied(
                                                    decision, "RETRY_BACKOFF", row.nextAttemptAt);
                                        if (decision
                                                        != R36AnalysisLeasePolicyR365.Decision
                                                                .CLAIM_AVAILABLE
                                                && decision
                                                        != R36AnalysisLeasePolicyR365.Decision
                                                                .CLAIM_STALE_TAKEOVER
                                                && decision
                                                        != R36AnalysisLeasePolicyR365.Decision
                                                                .RESUME_SAME_OWNER)
                                            return ClaimResult.denied(
                                                    decision,
                                                    "CLAIM_DENIED:" + row.status,
                                                    0L);

                                        boolean resume =
                                                decision
                                                        == R36AnalysisLeasePolicyR365.Decision
                                                                .RESUME_SAME_OWNER;
                                        ContentValues cv = new ContentValues();
                                        cv.put("status", RUNNING);
                                        cv.put("started_at", resume ? row.startedAt : now);
                                        cv.put("lease_owner", owner);
                                        cv.put("lease_until", now + AppConfig.R36_ANALYSIS_LEASE_MS);
                                        cv.put("attempts", row.attempts + (resume ? 0L : 1L));
                                        cv.put("next_attempt_at", 0L);
                                        cv.put("last_stage", resume ? "ANALYSIS_RESUMED" : "ANALYSIS_RUNNING");
                                        cv.put("error_code", "");
                                        cv.put(
                                                "repair_version",
                                                R36AnalysisLeasePolicyR365.REPAIR_VERSION);
                                        cv.put("last_transition_at", now);
                                        String where;
                                        String[] args;
                                        if (resume) {
                                            where =
                                                    "target_issue=? AND generation=? AND request_hash=?"
                                                            + " AND status=? AND lease_owner=?";
                                            args =
                                                    new String[] {
                                                        row.targetIssue,
                                                        String.valueOf(row.generation),
                                                        row.requestHash,
                                                        RUNNING,
                                                        owner
                                                    };
                                        } else if (decision
                                                == R36AnalysisLeasePolicyR365.Decision
                                                        .CLAIM_STALE_TAKEOVER) {
                                            where =
                                                    "target_issue=? AND generation=? AND request_hash=?"
                                                            + " AND status=? AND lease_until<=?";
                                            args =
                                                    new String[] {
                                                        row.targetIssue,
                                                        String.valueOf(row.generation),
                                                        row.requestHash,
                                                        RUNNING,
                                                        String.valueOf(now)
                                                    };
                                        } else {
                                            where =
                                                    "target_issue=? AND generation=? AND request_hash=?"
                                                            + " AND status IN (?,?) AND next_attempt_at<=?";
                                            args =
                                                    new String[] {
                                                        row.targetIssue,
                                                        String.valueOf(row.generation),
                                                        row.requestHash,
                                                        REQUESTED,
                                                        RETRY_WAIT,
                                                        String.valueOf(now)
                                                    };
                                        }
                                        int n =
                                                db.update(
                                                        DbHelper.TABLE_R36_ANALYSIS_REQUEST,
                                                        cv,
                                                        where,
                                                        args);
                                        if (n != 1)
                                            return ClaimResult.denied(
                                                    R36AnalysisLeasePolicyR365.Decision.STALE_REQUEST,
                                                    "CLAIM_FENCE_LOST:" + n,
                                                    now + 250L);
                                        return ClaimResult.claimed(
                                                decision,
                                                request,
                                                now + AppConfig.R36_ANALYSIS_LEASE_MS,
                                                row.attempts + (resume ? 0L : 1L));
                                    }
                                },
                                null);
        if (!result.success || result.value == null) {
            DatabaseCoordinator.B03CoordinatorSnapshot snapshot =
                    store.coordinatorR36().snapshotB03();
            return ClaimResult.databaseError(
                    safe(result.code) + "|" + safe(snapshot.lastError));
        }
        return result.value;
    }

    /** Compatibility wrapper; new production code consumes the structured decision. */
    public AnalysisSingleFlightR36.Request claim(
            AnalysisSingleFlightR36.Request request, String owner, long now) {
        ClaimResult result = claimDetailed(request, owner, now);
        return result.claimed ? result.request : null;
    }

    public boolean pulse(
            final AnalysisSingleFlightR36.Request request,
            final String stage,
            final String owner,
            final long now) {
        if (request == null || !request.valid()) return false;
        Boolean updated =
                store.coordinatorR36()
                        .runWriteOrThrowF01(
                                "R366_ANALYSIS_PULSE",
                                new DatabaseCoordinator.WriteCallable<Boolean>() {
                                    @Override
                                    public Boolean call(SQLiteDatabase db) {
                                        ContentValues cv = new ContentValues();
                                        cv.put("last_stage", safe(stage));
                                        cv.put("lease_until", now + AppConfig.R36_ANALYSIS_LEASE_MS);
                                        cv.put("last_transition_at", now);
                                        return Boolean.valueOf(
                                                db.update(
                                                                DbHelper.TABLE_R36_ANALYSIS_REQUEST,
                                                                cv,
                                                                "target_issue=? AND generation=?"
                                                                    + " AND request_hash=? AND status=?"
                                                                    + " AND lease_owner=?",
                                                                new String[] {
                                                                    request.targetIssue,
                                                                    String.valueOf(request.generation),
                                                                    request.requestHash,
                                                                    RUNNING,
                                                                    safe(owner)
                                                                })
                                                        == 1);
                                    }
                                });
        return updated.booleanValue();
    }

    public AnalysisSingleFlightR36.Request bindHistoryHash(
            final AnalysisSingleFlightR36.Request request,
            final String historyHash,
            final String owner) {
        if (request == null
                || !request.valid()
                || historyHash == null
                || historyHash.length() != 64)
            throw new IllegalArgumentException("R36_HISTORY_BIND_INVALID");
        Boolean ok =
                store.coordinatorR36()
                        .runWriteOrThrowF01(
                                "R36_ANALYSIS_HISTORY_BIND",
                                new DatabaseCoordinator.WriteCallable<Boolean>() {
                                    @Override
                                    public Boolean call(SQLiteDatabase db) {
                                        Cursor c = null;
                                        try {
                                            c =
                                                    db.rawQuery(
                                                            "SELECT history_hash FROM "
                                                                    + DbHelper
                                                                            .TABLE_R36_ANALYSIS_REQUEST
                                                                    + " WHERE target_issue=? AND generation=?"
                                                                    + " AND request_hash=? AND status=?"
                                                                    + " AND lease_owner=? LIMIT 1",
                                                            new String[] {
                                                                request.targetIssue,
                                                                String.valueOf(request.generation),
                                                                request.requestHash,
                                                                RUNNING,
                                                                safe(owner)
                                                            });
                                            if (!c.moveToFirst()) return Boolean.FALSE;
                                            String old = safe(c.getString(0));
                                            if (old.length() > 0 && !historyHash.equals(old))
                                                throw new IllegalStateException(
                                                        "R36_HISTORY_HASH_CONFLICT");
                                        } finally {
                                            if (c != null) c.close();
                                        }
                                        ContentValues cv = new ContentValues();
                                        cv.put("history_hash", historyHash);
                                        cv.put("last_stage", "HISTORY_FROZEN");
                                        return Boolean.valueOf(
                                                db.update(
                                                                DbHelper.TABLE_R36_ANALYSIS_REQUEST,
                                                                cv,
                                                                "target_issue=? AND generation=?"
                                                                        + " AND request_hash=? AND status=?"
                                                                        + " AND lease_owner=?",
                                                                new String[] {
                                                                    request.targetIssue,
                                                                    String.valueOf(request.generation),
                                                                    request.requestHash,
                                                                    RUNNING,
                                                                    safe(owner)
                                                                })
                                                        == 1);
                                    }
                                });
        if (!ok.booleanValue()) throw new IllegalStateException("R36_HISTORY_BIND_FAILED");
        return request.withHistoryHash(historyHash);
    }

    public TransitionResult markRetry(
            final AnalysisSingleFlightR36.Request request,
            final String owner,
            final String error,
            final long now) {
        return transitionOwned(request, owner, error, now, false, false);
    }

    public TransitionResult markExpired(
            final AnalysisSingleFlightR36.Request request,
            final String owner,
            final String error,
            final long now) {
        return transitionOwned(request, owner, error, now, true, false);
    }

    public TransitionResult markFailed(
            final AnalysisSingleFlightR36.Request request,
            final String owner,
            final String error,
            final long now) {
        return transitionOwned(request, owner, error, now, true, true);
    }

    private TransitionResult transitionOwned(
            final AnalysisSingleFlightR36.Request request,
            final String owner,
            final String error,
            final long now,
            final boolean terminal,
            final boolean failed) {
        if (request == null || !request.valid() || safe(owner).length() == 0)
            return TransitionResult.lost("TRANSITION_INPUT_INVALID");
        TransitionResult result =
                store.coordinatorR36()
                        .runWrite(
                                "R366_ANALYSIS_TRANSITION",
                                new DatabaseCoordinator.WriteCallable<TransitionResult>() {
                                    @Override
                                    public TransitionResult call(SQLiteDatabase db) {
                                        DurableRow row = readDurableRow(db, request.targetIssue);
                                        if (row == null
                                                || row.generation != request.generation
                                                || !row.requestHash.equals(request.requestHash)
                                                || !RUNNING.equals(row.status)
                                                || !safe(owner).equals(row.leaseOwner))
                                            return TransitionResult.lost("LEASE_FENCE_LOST");
                                        boolean maxed =
                                                row.attempts >= AppConfig.R36_ANALYSIS_MAX_ATTEMPTS;
                                        String nextStatus =
                                                terminal
                                                        ? (failed ? FAILED_TERMINAL : EXPIRED)
                                                        : (maxed ? FAILED_TERMINAL : RETRY_WAIT);
                                        long retryAt =
                                                RETRY_WAIT.equals(nextStatus)
                                                        ? now
                                                                + R36AnalysisLeasePolicyR365
                                                                        .retryDelayMs(row.attempts)
                                                        : 0L;
                                        ContentValues cv = new ContentValues();
                                        cv.put("status", nextStatus);
                                        cv.put(
                                                "finished_at",
                                                RETRY_WAIT.equals(nextStatus) ? 0L : now);
                                        cv.put("lease_owner", "");
                                        cv.put("lease_until", 0L);
                                        cv.put("next_attempt_at", retryAt);
                                        cv.put("last_stage", nextStatus);
                                        cv.put("error_code", safe(error));
                                        if (row.firstError.length() == 0)
                                            cv.put("first_error_code", safe(error));
                                        cv.put(
                                                "repair_version",
                                                R36AnalysisLeasePolicyR365.REPAIR_VERSION);
                                        cv.put("last_transition_at", now);
                                        int n =
                                                db.update(
                                                        DbHelper.TABLE_R36_ANALYSIS_REQUEST,
                                                        cv,
                                                        "target_issue=? AND generation=? AND request_hash=?"
                                                                + " AND status=? AND lease_owner=?",
                                                        new String[] {
                                                            request.targetIssue,
                                                            String.valueOf(request.generation),
                                                            request.requestHash,
                                                            RUNNING,
                                                            safe(owner)
                                                        });
                                        return n == 1
                                                ? TransitionResult.ok(nextStatus, retryAt)
                                                : TransitionResult.lost("TRANSITION_FENCE_" + n);
                                    }
                                },
                                TransitionResult.lost("WRITE_COORDINATION_FAILED"));
        return result == null ? TransitionResult.lost("TRANSITION_NULL") : result;
    }

    /**
     * Latest-target-wins for requests that never acquired a live owner. A running request is only
     * expired here after its durable lease is stale; an in-process owner exits cooperatively and is
     * terminalized by Service.
     */
    private static void expireSupersededPendingWithinTransaction(
            SQLiteDatabase db, String latestTarget, long now) {
        if (db == null || !db.inTransaction())
            throw new IllegalStateException("R36_REQUEST_TX_REQUIRED");
        ContentValues cv = new ContentValues();
        cv.put("status", EXPIRED);
        cv.put("finished_at", now);
        cv.put("lease_owner", "");
        cv.put("lease_until", 0L);
        cv.put("next_attempt_at", 0L);
        cv.put("last_stage", EXPIRED);
        cv.put("error_code", "SUPERSEDED_BY:" + latestTarget);
        cv.put("repair_version", R36AnalysisLeasePolicyR365.REPAIR_VERSION);
        cv.put("last_transition_at", now);
        db.update(
                DbHelper.TABLE_R36_ANALYSIS_REQUEST,
                cv,
                "target_issue<? AND (status IN (?,?) OR (status=? AND lease_until<?))",
                new String[] {latestTarget, REQUESTED, RETRY_WAIT, RUNNING, String.valueOf(now)});
    }

    static void markCanonicalCommittedWithinTransaction(
            SQLiteDatabase db,
            AnalysisSingleFlightR36.Request request,
            String owner,
            String canonicalHash,
            long commitAt) {
        if (db == null || !db.inTransaction())
            throw new IllegalStateException("R36_REQUEST_TX_REQUIRED");
        if (request == null || !request.valid() || safe(owner).length() == 0)
            throw new IllegalArgumentException("R366_REQUEST_COMMIT_FENCE_INVALID");
        ContentValues cv = new ContentValues();
        cv.put("status", CANONICAL_COMMITTED);
        cv.put("finished_at", commitAt);
        cv.put("lease_owner", "");
        cv.put("lease_until", 0L);
        cv.put("next_attempt_at", 0L);
        cv.put("last_stage", "CANONICAL_COMMITTED");
        cv.put("canonical_hash", safe(canonicalHash));
        cv.put("error_code", "");
        cv.put("repair_version", R36AnalysisLeasePolicyR365.REPAIR_VERSION);
        cv.put("last_transition_at", commitAt);
        int n =
                db.update(
                        DbHelper.TABLE_R36_ANALYSIS_REQUEST,
                        cv,
                        "target_issue=? AND generation=? AND request_hash=? AND status=?"
                                + " AND lease_owner=?",
                        new String[] {
                            request.targetIssue,
                            String.valueOf(request.generation),
                            request.requestHash,
                            RUNNING,
                            safe(owner)
                        });
        if (n != 1) throw new IllegalStateException("R36_REQUEST_COMMIT_CARDINALITY_" + n);
    }

    public AnalysisSingleFlightR36.Request latestRunnable(long now) {
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT base_issue,target_issue,generation,request_at,"
                                            + "deadline_at,request_hash,history_hash FROM "
                                            + DbHelper.TABLE_R36_ANALYSIS_REQUEST
                                            + " WHERE ((status IN (?,?) AND next_attempt_at<=?)"
                                            + " OR (status=? AND lease_until<=?))"
                                            + " ORDER BY target_issue DESC LIMIT 1",
                                    new String[] {
                                        REQUESTED,
                                        RETRY_WAIT,
                                        String.valueOf(now),
                                        RUNNING,
                                        String.valueOf(now)
                                    });
            return c.moveToFirst() ? from(c) : null;
        } finally {
            if (c != null) c.close();
        }
    }

    public AnalysisSingleFlightR36.Request latestPending() {
        return latestRunnable(System.currentTimeMillis());
    }

    public String diagnostic(String target) {
        DurableRow row = readDurableRow(store.readable(), target);
        if (row == null) return "REQUEST_ROW_MISSING";
        return "status="
                + row.status
                + "|generation="
                + row.generation
                + "|attempts="
                + row.attempts
                + "|stage="
                + row.lastStage
                + "|error="
                + row.error
                + "|firstError="
                + row.firstError
                + "|owner="
                + row.leaseOwner
                + "|leaseUntil="
                + row.leaseUntil
                + "|nextAttemptAt="
                + row.nextAttemptAt
                + "|deadlineAt="
                + row.deadlineAt
                + "|repair="
                + row.repairVersion;
    }

    public String status(String target) {
        DurableRow row = readDurableRow(store.readable(), target);
        return row == null ? "MISSING" : row.status;
    }

    public AnalysisSingleFlightR36.Request read(String target) {
        return read(store.readable(), target);
    }

    public boolean isTerminal(String target) {
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT status FROM "
                                            + DbHelper.TABLE_R36_ANALYSIS_REQUEST
                                            + " WHERE target_issue=? LIMIT 1",
                                    new String[] {target});
            if (!c.moveToFirst()) return false;
            String s = safe(c.getString(0));
            return CANONICAL_COMMITTED.equals(s) || EXPIRED.equals(s) || FAILED_TERMINAL.equals(s);
        } finally {
            if (c != null) c.close();
        }
    }

    private static AnalysisSingleFlightR36.Request read(SQLiteDatabase db, String target) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT base_issue,target_issue,generation,request_at,deadline_at,"
                                    + "request_hash,history_hash FROM "
                                    + DbHelper.TABLE_R36_ANALYSIS_REQUEST
                                    + " WHERE target_issue=? LIMIT 1",
                            new String[] {target});
            return c.moveToFirst() ? from(c) : null;
        } finally {
            if (c != null) c.close();
        }
    }

    private static AnalysisSingleFlightR36.Request from(Cursor c) {
        return new AnalysisSingleFlightR36.Request(
                c.getString(0),
                c.getString(1),
                c.getLong(2),
                c.getLong(3),
                c.getLong(4),
                "DURABLE_REPLAY",
                c.getString(5),
                c.getString(6));
    }

    private static boolean canonicalExists(SQLiteDatabase db, String target) {
        return scalarLong(
                        db,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                + " WHERE target_issue=?",
                        new String[] {target},
                        0L)
                > 0L;
    }

    private static void terminalizeDrawnRow(SQLiteDatabase db, DurableRow row, long now) {
        if (row == null || CANONICAL_COMMITTED.equals(row.status)) return;
        ContentValues cv = new ContentValues();
        cv.put("status", EXPIRED);
        cv.put("finished_at", now);
        cv.put("lease_owner", "");
        cv.put("lease_until", 0L);
        cv.put("next_attempt_at", 0L);
        cv.put("last_stage", EXPIRED);
        cv.put("error_code", "TARGET_ALREADY_DRAWN");
        if (row.firstError.length() == 0) cv.put("first_error_code", "TARGET_ALREADY_DRAWN");
        cv.put("repair_version", R36AnalysisLeasePolicyR365.REPAIR_VERSION);
        cv.put("last_transition_at", now);
        db.update(
                DbHelper.TABLE_R36_ANALYSIS_REQUEST,
                cv,
                "target_issue=? AND generation=? AND request_hash=? AND status<>?",
                new String[] {
                    row.targetIssue,
                    String.valueOf(row.generation),
                    row.requestHash,
                    CANONICAL_COMMITTED
                });
    }

    private static void reconcileCanonicalRow(SQLiteDatabase db, DurableRow row, long now) {
        if (row == null) return;
        Cursor c = null;
        String canonicalHash = "";
        try {
            c =
                    db.rawQuery(
                            "SELECT record_hash FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " WHERE target_issue=? LIMIT 1",
                            new String[] {row.targetIssue});
            if (c.moveToFirst()) canonicalHash = safe(c.getString(0));
        } finally {
            if (c != null) c.close();
        }
        if (canonicalHash.length() == 0) return;
        ContentValues cv = new ContentValues();
        cv.put("status", CANONICAL_COMMITTED);
        cv.put("finished_at", now);
        cv.put("lease_owner", "");
        cv.put("lease_until", 0L);
        cv.put("next_attempt_at", 0L);
        cv.put("last_stage", "CANONICAL_RECONCILED");
        cv.put("canonical_hash", canonicalHash);
        cv.put("error_code", "");
        cv.put("repair_version", R36AnalysisLeasePolicyR365.REPAIR_VERSION);
        cv.put("last_transition_at", now);
        db.update(
                DbHelper.TABLE_R36_ANALYSIS_REQUEST,
                cv,
                "target_issue=? AND generation=? AND request_hash=?",
                new String[] {
                    row.targetIssue, String.valueOf(row.generation), row.requestHash
                });
    }

    private static DurableRow readDurableRow(SQLiteDatabase db, String target) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT base_issue,target_issue,status,generation,request_at,deadline_at,"
                                    + "started_at,lease_owner,lease_until,attempts,last_stage,error_code,"
                                    + "request_hash,next_attempt_at,repair_version,first_error_code"
                                    + " FROM "
                                    + DbHelper.TABLE_R36_ANALYSIS_REQUEST
                                    + " WHERE target_issue=? LIMIT 1",
                            new String[] {target});
            if (!c.moveToFirst()) return null;
            return new DurableRow(
                    c.getString(0),
                    c.getString(1),
                    c.getString(2),
                    c.getLong(3),
                    c.getLong(4),
                    c.getLong(5),
                    c.getLong(6),
                    c.getString(7),
                    c.getLong(8),
                    c.getLong(9),
                    c.getString(10),
                    c.getString(11),
                    c.getString(12),
                    c.getLong(13),
                    c.getString(14),
                    c.getString(15));
        } finally {
            if (c != null) c.close();
        }
    }

    private static final class DurableRow {
        final String baseIssue, targetIssue, status, leaseOwner, lastStage, error;
        final String requestHash, repairVersion, firstError;
        final long generation, requestAt, deadlineAt, startedAt, leaseUntil, attempts, nextAttemptAt;

        DurableRow(
                String baseIssue,
                String targetIssue,
                String status,
                long generation,
                long requestAt,
                long deadlineAt,
                long startedAt,
                String leaseOwner,
                long leaseUntil,
                long attempts,
                String lastStage,
                String error,
                String requestHash,
                long nextAttemptAt,
                String repairVersion,
                String firstError) {
            this.baseIssue = safe(baseIssue);
            this.targetIssue = safe(targetIssue);
            this.status = safe(status);
            this.generation = generation;
            this.requestAt = requestAt;
            this.deadlineAt = deadlineAt;
            this.startedAt = startedAt;
            this.leaseOwner = safe(leaseOwner);
            this.leaseUntil = leaseUntil;
            this.attempts = attempts;
            this.lastStage = safe(lastStage);
            this.error = safe(error);
            this.requestHash = safe(requestHash);
            this.nextAttemptAt = nextAttemptAt;
            this.repairVersion = safe(repairVersion);
            this.firstError = safe(firstError);
        }
    }

    public static final class ClaimResult {
        public final boolean claimed, databaseError;
        public final R36AnalysisLeasePolicyR365.Decision decision;
        public final AnalysisSingleFlightR36.Request request;
        public final String code;
        public final long retryAt, leaseUntil, attempts;

        private ClaimResult(
                boolean claimed,
                boolean databaseError,
                R36AnalysisLeasePolicyR365.Decision decision,
                AnalysisSingleFlightR36.Request request,
                String code,
                long retryAt,
                long leaseUntil,
                long attempts) {
            this.claimed = claimed;
            this.databaseError = databaseError;
            this.decision = decision;
            this.request = request;
            this.code = safe(code);
            this.retryAt = retryAt;
            this.leaseUntil = leaseUntil;
            this.attempts = attempts;
        }

        static ClaimResult claimed(
                R36AnalysisLeasePolicyR365.Decision decision,
                AnalysisSingleFlightR36.Request request,
                long leaseUntil,
                long attempts) {
            return new ClaimResult(true, false, decision, request, "CLAIMED", 0L, leaseUntil, attempts);
        }

        static ClaimResult denied(
                R36AnalysisLeasePolicyR365.Decision decision, String code, long retryAt) {
            return new ClaimResult(false, false, decision, null, code, retryAt, 0L, 0L);
        }

        static ClaimResult databaseError(String code) {
            return new ClaimResult(
                    false,
                    true,
                    R36AnalysisLeasePolicyR365.Decision.INVALID_STATUS,
                    null,
                    "DATABASE_ERROR:" + safe(code),
                    0L,
                    0L,
                    0L);
        }

        public boolean deferred() {
            return decision == R36AnalysisLeasePolicyR365.Decision.DEFER_ACTIVE_OWNER
                    || decision == R36AnalysisLeasePolicyR365.Decision.NOT_DUE;
        }
    }

    public static final class TransitionResult {
        public final boolean updated;
        public final String status, code;
        public final long retryAt;

        private TransitionResult(boolean updated, String status, String code, long retryAt) {
            this.updated = updated;
            this.status = safe(status);
            this.code = safe(code);
            this.retryAt = retryAt;
        }

        static TransitionResult ok(String status, long retryAt) {
            return new TransitionResult(true, status, "OK", retryAt);
        }

        static TransitionResult lost(String code) {
            return new TransitionResult(false, "UNCHANGED", code, 0L);
        }
    }

    private static long scalarLong(SQLiteDatabase db, String sql, String[] args, long fallback) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            return c.moveToFirst() ? c.getLong(0) : fallback;
        } finally {
            if (c != null) c.close();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

public static String diagnosticSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String target) {
        DurableRow row = readDurableRow(report.readable(), target);
        if (row == null) return "REQUEST_ROW_MISSING";
        return "status="
                + row.status
                + "|generation="
                + row.generation
                + "|attempts="
                + row.attempts
                + "|stage="
                + row.lastStage
                + "|error="
                + row.error
                + "|firstError="
                + row.firstError
                + "|owner="
                + row.leaseOwner
                + "|leaseUntil="
                + row.leaseUntil
                + "|nextAttemptAt="
                + row.nextAttemptAt
                + "|deadlineAt="
                + row.deadlineAt
                + "|repair="
                + row.repairVersion;
    }

public static String statusSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String target) {
        DurableRow row = readDurableRow(report.readable(), target);
        return row == null ? "MISSING" : row.status;
    }
}
