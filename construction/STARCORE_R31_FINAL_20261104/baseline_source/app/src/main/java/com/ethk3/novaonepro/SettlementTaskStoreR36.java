package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

/** Durable exact-draw settlement backlog; never scans open trades or recent-500. */
public final class SettlementTaskStoreR36 {
    public static final String LIVE_CURRENT = "LIVE_CURRENT",
            LIVE_RETRY = "LIVE_RETRY",
            BACKLOG = "BACKLOG";
    public static final int PRIORITY_LIVE_CURRENT = 100,
            PRIORITY_LIVE_RETRY = 50,
            PRIORITY_BACKLOG = 0;
    public static final String PENDING = "PENDING",
            RUNNING = "RUNNING",
            RETRY_WAIT = "RETRY_WAIT",
            COMPLETED = "COMPLETED",
            FAILED_TERMINAL = "FAILED_TERMINAL",
            NO_PREDRAW_CANONICAL_TERMINAL = "NO_PREDRAW_CANONICAL_TERMINAL",
            INVALID_DRAW_TERMINAL = "INVALID_DRAW_TERMINAL";

    private SettlementTaskStoreR36() {}

    public static boolean enqueueDraw(final DbStore store, final Models.Draw draw) {
        if (store == null || draw == null || !draw.isValid()) return false;
        Boolean out =
                store.coordinatorR36()
                        .runWrite(
                                "R36_SETTLEMENT_ENQUEUE",
                                new DatabaseCoordinator.WriteCallable<Boolean>() {
                                    @Override
                                    public Boolean call(SQLiteDatabase db) {
                                        enqueueWithinWrite(
                                                db,
                                                draw,
                                                System.currentTimeMillis(),
                                                LIVE_CURRENT,
                                                PRIORITY_LIVE_CURRENT,
                                                "AUTHORITATIVE_LATEST");
                                        return Boolean.TRUE;
                                    }
                                },
                                Boolean.FALSE);
        return out.booleanValue();
    }

    public static int enqueueDraws(final DbStore store, final List<Models.Draw> draws) {
        return enqueueHistoricalDraws(store, draws);
    }

    public static int enqueueHistoricalDraws(final DbStore store, final List<Models.Draw> draws) {
        if (store == null || draws == null || draws.size() == 0) return 0;
        Integer out =
                store.coordinatorR36()
                        .runWrite(
                                "R36_SETTLEMENT_ENQUEUE_BATCH",
                                new DatabaseCoordinator.WriteCallable<Integer>() {
                                    @Override
                                    public Integer call(SQLiteDatabase db) {
                                        boolean owns = !db.inTransaction();
                                        int count = 0;
                                        if (owns) db.beginTransaction();
                                        try {
                                            long now = System.currentTimeMillis();
                                            for (int i = 0; i < draws.size(); i++) {
                                                Models.Draw d = draws.get(i);
                                                if (d != null && d.isValid()) {
                                                    enqueueWithinWrite(
                                                            db,
                                                            d,
                                                            now,
                                                            BACKLOG,
                                                            PRIORITY_BACKLOG,
                                                            "HISTORICAL_BACKFILL");
                                                    count++;
                                                }
                                            }
                                            if (owns) db.setTransactionSuccessful();
                                            return Integer.valueOf(count);
                                        } finally {
                                            if (owns && db.inTransaction()) db.endTransaction();
                                        }
                                    }
                                },
                                Integer.valueOf(0));
        return out.intValue();
    }

    static void enqueueWithinWrite(
            SQLiteDatabase db,
            Models.Draw draw,
            long now,
            String taskClass,
            int priority,
            String source) {
        String target = safe(draw.issue);
        DrawCanonicalizerR811.PersistenceDecision canonical =
                DrawCanonicalizerR811.observeWithinWrite(db, draw);
        if (!canonical.confirmed)
            throw new IllegalStateException("R811_DRAW_CONFLICT_HARD_BLOCK:" + target);
        enqueueConfirmedWithinWrite(db, draw, now, taskClass, priority, source);
    }

    /** Same-transaction fast path after DbStore has persisted and verified the exact draw. */
    static void enqueueConfirmedWithinWrite(
            SQLiteDatabase db,
            Models.Draw draw,
            long now,
            String taskClass,
            int priority,
            String source) {
        if (!DrawCanonicalizerR811.isConfirmedWithinRead(db, draw))
            throw new IllegalStateException("R914_DRAW_NOT_CONFIRMED_BEFORE_TASK");
        String target = safe(draw.issue), hash = DrawHashR36.sha256V2(draw);
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT draw_hash,status FROM "
                                    + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                    + " WHERE target_issue=? LIMIT 1",
                            new String[] {target});
            if (c.moveToFirst()) {
                String stored = safe(c.getString(0));
                if (!matchesTaskHash(stored, draw))
                    throw new IllegalStateException("R36_DRAW_HASH_CONFLICT");
                if (!hash.equals(stored) && !isTerminal(c.getString(1))) {
                    ContentValues upgrade = new ContentValues();
                    upgrade.put("draw_hash", hash);
                    upgrade.put("updated_at", now);
                    db.update(
                            DbHelper.TABLE_R36_SETTLEMENT_TASK,
                            upgrade,
                            "target_issue=? AND draw_hash=?",
                            new String[] {target, stored});
                }
                return;
            }
        } finally {
            if (c != null) c.close();
        }
        ContentValues cv = new ContentValues();
        cv.put("target_issue", target);
        cv.put("draw_issue", target);
        cv.put("status", PENDING);
        cv.put("created_at", now);
        cv.put("updated_at", now);
        cv.put("completed_at", 0L);
        cv.put("lease_owner", "");
        cv.put("lease_until", 0L);
        cv.put("attempts", 0);
        cv.put("next_attempt_at", 0L);
        cv.put("draw_hash", hash);
        cv.put("settlement_hash", "");
        cv.put("last_error", "");
        cv.put("task_class", taskClass);
        cv.put("priority", priority);
        cv.put("terminal_code", "");
        cv.put("source", safe(source));
        db.insertOrThrow(DbHelper.TABLE_R36_SETTLEMENT_TASK, null, cv);
    }

    /** One-transaction current-draw promotion after atomic draw+task persistence. */
    public static boolean handoffCurrentExactR914(final DbStore store, final Models.Draw draw) {
        if (store == null || draw == null || !draw.isValid()
                || !TargetIssueResolver.isValid(draw.issue)) return false;
        Boolean result = store.coordinatorR36().runWrite(
                "R914_SETTLEMENT_HANDOFF_CURRENT",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        boolean owns = !db.inTransaction();
                        if (owns) db.beginTransaction();
                        try {
                            if (!DrawCanonicalizerR811.isConfirmedWithinRead(db, draw))
                                return Boolean.FALSE;
                            Cursor present = null;
                            try {
                                present = db.rawQuery(
                                        "SELECT 1 FROM " + DrawDependencyInputCP12.SOURCE
                                                + " WHERE issue=? AND dirty=0 LIMIT 1",
                                        new String[] {draw.issue});
                                if (!present.moveToFirst()) return Boolean.FALSE;
                            } finally {
                                if (present != null) present.close();
                            }
                            long now = System.currentTimeMillis();
                            enqueueConfirmedWithinWrite(
                                    db, draw, now, BACKLOG, PRIORITY_BACKLOG,
                                    "R914_CURRENT_HANDOFF_ENSURE");

                            ContentValues demote = new ContentValues();
                            demote.put("task_class", LIVE_RETRY);
                            demote.put("priority", PRIORITY_LIVE_RETRY);
                            demote.put("updated_at", now);
                            db.update(
                                    DbHelper.TABLE_R36_SETTLEMENT_TASK,
                                    demote,
                                    "target_issue<>? AND task_class=? AND status NOT IN (?,?,?,?)",
                                    new String[] {
                                        draw.issue, LIVE_CURRENT, COMPLETED, FAILED_TERMINAL,
                                        NO_PREDRAW_CANONICAL_TERMINAL, INVALID_DRAW_TERMINAL
                                    });

                            ContentValues promote = new ContentValues();
                            promote.put("draw_hash", DrawHashR36.sha256V2(draw));
                            promote.put("task_class", LIVE_CURRENT);
                            promote.put("priority", PRIORITY_LIVE_CURRENT);
                            promote.put("next_attempt_at", 0L);
                            promote.put("updated_at", now);
                            promote.put("source", "R914_AUTHORITATIVE_CURRENT_HANDOFF");
                            int changed = db.update(
                                    DbHelper.TABLE_R36_SETTLEMENT_TASK,
                                    promote,
                                    "target_issue=? AND status NOT IN (?,?,?,?)",
                                    new String[] {
                                        draw.issue, COMPLETED, FAILED_TERMINAL,
                                        NO_PREDRAW_CANONICAL_TERMINAL, INVALID_DRAW_TERMINAL
                                    });
                            boolean durable = changed == 1;
                            if (!durable) {
                                Cursor existing = null;
                                try {
                                    existing = db.rawQuery(
                                            "SELECT 1 FROM " + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                                    + " WHERE target_issue=? LIMIT 1",
                                            new String[] {draw.issue});
                                    durable = existing.moveToFirst();
                                } finally {
                                    if (existing != null) existing.close();
                                }
                            }
                            if (owns) db.setTransactionSuccessful();
                            return Boolean.valueOf(durable);
                        } finally {
                            if (owns && db.inTransaction()) db.endTransaction();
                        }
                    }
                },
                Boolean.FALSE);
        return result != null && result.booleanValue();
    }

    /** Explicit authoritative-current promotion; historical enqueue/drain can never call this. */
    public static boolean promoteCurrentDraw(final DbStore store, final Models.Draw draw) {
        if (store == null || draw == null || !draw.isValid()) return false;
        final String target = safe(draw.issue);
        final String hash = DrawHashR36.sha256V2(draw);
        Boolean out =
                store.coordinatorR36()
                        .runWrite(
                                "R811_SETTLEMENT_PROMOTE_CURRENT",
                                new DatabaseCoordinator.WriteCallable<Boolean>() {
                                    @Override
                                    public Boolean call(SQLiteDatabase db) {
                                        // R900 invariant: there can be only one authoritative LIVE_CURRENT issue.
                                        // Older live rows stay durable but are demoted to LIVE_RETRY so they can
                                        // never starve the newest exact draw.  Status/lease/result are untouched.
                                        ContentValues demote = new ContentValues();
                                        demote.put("task_class", LIVE_RETRY);
                                        demote.put("priority", PRIORITY_LIVE_RETRY);
                                        demote.put("updated_at", System.currentTimeMillis());
                                        db.update(
                                                DbHelper.TABLE_R36_SETTLEMENT_TASK,
                                                demote,
                                                "target_issue<>? AND task_class=? AND status NOT IN (?,?,?,?)",
                                                new String[] {
                                                    target, LIVE_CURRENT, COMPLETED, FAILED_TERMINAL,
                                                    NO_PREDRAW_CANONICAL_TERMINAL, INVALID_DRAW_TERMINAL
                                                });
                                        Cursor c = null;
                                        try {
                                            c =
                                                    db.rawQuery(
                                                            "SELECT draw_hash,status FROM "
                                                                    + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                                                    + " WHERE target_issue=? LIMIT 1",
                                                            new String[] {target});
                                            if (!c.moveToFirst()) return Boolean.FALSE;
                                            String stored = safe(c.getString(0));
                                            if (!matchesTaskHash(stored, draw))
                                                throw new IllegalStateException(
                                                        "R811_DRAW_HASH_CONFLICT");
                                            if (isTerminal(c.getString(1))) return Boolean.FALSE;
                                        } finally {
                                            if (c != null) c.close();
                                        }
                                        ContentValues cv = new ContentValues();
                                        cv.put("draw_hash", hash);
                                        cv.put("task_class", LIVE_CURRENT);
                                        cv.put("priority", PRIORITY_LIVE_CURRENT);
                                        cv.put("next_attempt_at", 0L);
                                        cv.put("updated_at", System.currentTimeMillis());
                                        cv.put("source", "AUTHORITATIVE_LATEST");
                                        return Boolean.valueOf(
                                                db.update(
                                                                DbHelper.TABLE_R36_SETTLEMENT_TASK,
                                                                cv,
                                                                "target_issue=? AND draw_hash=? AND status NOT IN (?,?,?,?)",
                                                                new String[] {
                                                                    target,
                                                                    hash,
                                                                    COMPLETED,
                                                                    FAILED_TERMINAL,
                                                                    NO_PREDRAW_CANONICAL_TERMINAL,
                                                                    INVALID_DRAW_TERMINAL
                                                                })
                                                        == 1);
                                    }
                                },
                                Boolean.FALSE);
        return out.booleanValue();
    }

    public static TaskClaim claim(
            final DbStore store, final String target, final String owner, final long now) {
        if (store == null || !TargetIssueResolver.isValid(target)) return null;
        return store.coordinatorR36()
                .runWrite(
                        "R36_SETTLEMENT_CLAIM",
                        new DatabaseCoordinator.WriteCallable<TaskClaim>() {
                            @Override
                            public TaskClaim call(SQLiteDatabase db) {
                                db.execSQL(
                                        "UPDATE "
                                                + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                                + " SET status='RUNNING',lease_owner=?,lease_until=?,attempts=COALESCE(attempts,0)+1,lease_generation=COALESCE(lease_generation,0)+1,process_boot_id=?,last_progress_at=?,updated_at=?,last_error='',failure_class=''"
                                                + " WHERE target_issue=? AND status IN"
                                                + " ('PENDING','RETRY_WAIT','RUNNING') AND"
                                                + " COALESCE(next_attempt_at,0)<=CAST(? AS INTEGER) AND"
                                                + " COALESCE(lease_until,0)<CAST(? AS INTEGER)",
                                        new Object[] {
                                            safeOwner(owner),
                                            Long.valueOf(now + AppConfig.R36_SETTLEMENT_LEASE_MS),
                                            ProcessBootIdentityR906.id(),
                                            Long.valueOf(now),
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
                                                    "SELECT draw_hash,attempts,task_class,priority,source,lease_generation,process_boot_id FROM "
                                                            + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                                            + " WHERE target_issue=? AND"
                                                            + " status='RUNNING' AND lease_owner=?"
                                                            + " LIMIT 1",
                                                    new String[] {target, safeOwner(owner)});
                                    return c.moveToFirst()
                                            ? new TaskClaim(
                                                    target,
                                                    safe(c.getString(0)),
                                                    safeOwner(owner),
                                                    c.getInt(1),
                                                    safe(c.getString(2)),
                                                    c.getInt(3),
                                                    safe(c.getString(4)),
                                                    c.getInt(5),
                                                    safe(c.getString(6)))
                                            : null;
                                } finally {
                                    if (c != null) c.close();
                                }
                            }
                        },
                        null);
    }

    /**
     * R915 exact-current normalize-and-claim.
     *
     * <p>The R911 two-transaction path first cleared an orphan lease and then attempted a second
     * claim.  Migrated rows with NULL {@code lease_until}/{@code next_attempt_at} did not match the
     * old predicates, and a competing maintenance pulse could also observe the intermediate row.
     * This method is legal only while the sole process-wide settlement gate is held; it normalizes
     * every nullable lease field and acquires the exact current row in one database transaction.</p>
     */
    public static TaskClaim normalizeAndClaimCurrentR915(
            final DbStore store,
            final String target,
            final String owner,
            final long now,
            final String reason) {
        if (store == null || !TargetIssueResolver.isValid(target)) return null;
        if (!SettlementCoordinatorR815.isCoordinatorExecutionActive()
                || !SettlementExecutionGateR819.isActive()) return null;
        return store.coordinatorR36()
                .runWrite(
                        "R915_SETTLEMENT_CURRENT_NORMALIZE_AND_CLAIM",
                        new DatabaseCoordinator.WriteCallable<TaskClaim>() {
                            @Override
                            public TaskClaim call(SQLiteDatabase db) {
                                boolean owns = !db.inTransaction();
                                if (owns) db.beginTransaction();
                                Cursor before = null;
                                try {
                                    before =
                                            db.rawQuery(
                                                    "SELECT status FROM "
                                                            + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                                            + " WHERE target_issue=? LIMIT 1",
                                                    new String[] {target});
                                    if (!before.moveToFirst()) return null;
                                    String prior = safe(before.getString(0));
                                    if (isTerminal(prior)) return null;
                                    before.close();
                                    before = null;

                                    db.execSQL(
                                            "UPDATE "
                                                    + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                                    + " SET status='RUNNING',task_class=?,priority=?,"
                                                    + " lease_owner=?,lease_until=?,"
                                                    + " attempts=COALESCE(attempts,0)+1,"
                                                    + " lease_generation=COALESCE(lease_generation,0)+1,"
                                                    + " process_boot_id=?,last_progress_at=?,updated_at=?,"
                                                    + " next_attempt_at=0,last_error='',failure_class=?,"
                                                    + " source='R915_CURRENT_ATOMIC_CLAIM',"
                                                    + " recovery_count=COALESCE(recovery_count,0)+?"
                                                    + " WHERE target_issue=? AND status NOT IN (?,?,?,?)",
                                            new Object[] {
                                                LIVE_CURRENT,
                                                Integer.valueOf(PRIORITY_LIVE_CURRENT),
                                                safeOwner(owner),
                                                Long.valueOf(now + AppConfig.R36_SETTLEMENT_LEASE_MS),
                                                ProcessBootIdentityR906.id(),
                                                Long.valueOf(now),
                                                Long.valueOf(now),
                                                RUNNING.equals(prior)
                                                        ? "ORPHAN_LEASE_ATOMICALLY_RECOVERED:"
                                                                + trim(reason, 120)
                                                        : "",
                                                Integer.valueOf(RUNNING.equals(prior) ? 1 : 0),
                                                target,
                                                COMPLETED,
                                                FAILED_TERMINAL,
                                                NO_PREDRAW_CANONICAL_TERMINAL,
                                                INVALID_DRAW_TERMINAL
                                            });
                                    if (changes(db) != 1) return null;
                                    Cursor claimed = null;
                                    try {
                                        claimed =
                                                db.rawQuery(
                                                        "SELECT draw_hash,attempts,task_class,priority,source,lease_generation,process_boot_id FROM "
                                                                + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                                                + " WHERE target_issue=? AND status='RUNNING'"
                                                                + " AND lease_owner=? LIMIT 1",
                                                        new String[] {target, safeOwner(owner)});
                                        if (!claimed.moveToFirst()) return null;
                                        TaskClaim out =
                                                new TaskClaim(
                                                        target,
                                                        safe(claimed.getString(0)),
                                                        safeOwner(owner),
                                                        claimed.getInt(1),
                                                        safe(claimed.getString(2)),
                                                        claimed.getInt(3),
                                                        safe(claimed.getString(4)),
                                                        claimed.getInt(5),
                                                        safe(claimed.getString(6)));
                                        if (owns) db.setTransactionSuccessful();
                                        return out;
                                    } finally {
                                        if (claimed != null) claimed.close();
                                    }
                                } finally {
                                    if (before != null) before.close();
                                    if (owns && db.inTransaction()) db.endTransaction();
                                }
                            }
                        },
                        null);
    }

    /**
     * R911 exact-current orphan-lease recovery.
     *
     * <p>This method is legal only while SettlementCoordinator owns the process-wide settlement
     * gate.  Under that sole-owner proof, a durable RUNNING lease for the same target cannot have
     * another valid in-process executor, so it is safe to reclaim immediately instead of waiting
     * for the wall-clock lease to expire.  RETRY_WAIT is also promoted to runnable-now for the
     * authoritative current draw.
     */
    public static boolean prepareCurrentForSoleCoordinatorR911(
            final DbStore store, final String target, final String reason) {
        if (store == null || !TargetIssueResolver.isValid(target)) return false;
        if (!SettlementCoordinatorR815.isCoordinatorExecutionActive()
                || !SettlementExecutionGateR819.isActive()) return false;
        Boolean out = store.coordinatorR36().runWrite(
                "R911_SETTLEMENT_CURRENT_SOLE_OWNER_RECOVER",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        Cursor c = null;
                        String status = "";
                        try {
                            c = db.rawQuery(
                                    "SELECT status FROM " + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " WHERE target_issue=? LIMIT 1",
                                    new String[] {target});
                            if (!c.moveToFirst()) return Boolean.FALSE;
                            status = safe(c.getString(0));
                        } finally { if (c != null) c.close(); }
                        if (isTerminal(status)) return Boolean.FALSE;
                        long now = System.currentTimeMillis();
                        ContentValues cv = new ContentValues();
                        cv.put("task_class", LIVE_CURRENT);
                        cv.put("priority", PRIORITY_LIVE_CURRENT);
                        cv.put("next_attempt_at", 0L);
                        cv.put("updated_at", now);
                        cv.put("last_progress_at", now);
                        cv.put("source", "R911_CURRENT_EXACT_SOLE_OWNER");
                        if (RUNNING.equals(status)) {
                            cv.put("status", RETRY_WAIT);
                            cv.put("lease_owner", "");
                            cv.put("lease_until", 0L);
                            cv.put("process_boot_id", "");
                            cv.put("failure_class", "ORPHAN_LEASE_RECOVERED");
                            cv.put("last_error", "R911_SOLE_GATE_ORPHAN_RECOVERED:" + trim(reason, 120));
                            db.execSQL(
                                    "UPDATE " + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " SET recovery_count=recovery_count+1 WHERE target_issue=?",
                                    new Object[] {target});
                        }
                        int n = db.update(
                                DbHelper.TABLE_R36_SETTLEMENT_TASK, cv,
                                "target_issue=? AND status NOT IN (?,?,?,?)",
                                new String[] {target, COMPLETED, FAILED_TERMINAL,
                                        NO_PREDRAW_CANONICAL_TERMINAL, INVALID_DRAW_TERMINAL});
                        return Boolean.valueOf(n == 1);
                    }
                }, Boolean.FALSE);
        return out != null && out.booleanValue();
    }

    static void markTerminalWithinTransaction(
            SQLiteDatabase db,
            TaskClaim claim,
            String status,
            String settlementHash,
            String error,
            long now) {
        markTerminalWithinTransaction(
                db, claim, status, settlementHash, status, error, now);
    }

    static void markTerminalWithinTransaction(
            SQLiteDatabase db,
            TaskClaim claim,
            String status,
            String settlementHash,
            String terminalCode,
            String error,
            long now) {
        if (db == null || !db.inTransaction() || claim == null)
            throw new IllegalStateException("R36_SETTLEMENT_TASK_TX_REQUIRED");
        if (!isTerminal(status))
            throw new IllegalArgumentException("R36_SETTLEMENT_STATUS_NOT_TERMINAL");
        ContentValues cv = new ContentValues();
        cv.put("status", status);
        cv.put("updated_at", now);
        cv.put("completed_at", now);
        cv.put("lease_owner", "");
        cv.put("lease_until", 0L);
        cv.put("next_attempt_at", 0L);
        cv.put("settlement_hash", safe(settlementHash));
        cv.put("last_error", trim(error, 700));
        cv.put("terminal_code", safe(terminalCode));
        cv.put("last_progress_at", now);
        cv.put("process_boot_id", ProcessBootIdentityR906.id());
        String eligibility = COMPLETED.equals(status) ? "ELIGIBLE_PREDRAW"
                : (NO_PREDRAW_CANONICAL_TERMINAL.equals(status) ? "EXCLUDED_NO_PREDRAW" : "BLOCKED_CONFLICT");
        cv.put("eligibility", eligibility);
        cv.put("failure_class", SettlementFailureClassR906.classify(error.length() == 0 ? terminalCode : error).name());
        int n =
                db.update(
                        DbHelper.TABLE_R36_SETTLEMENT_TASK,
                        cv,
                        "target_issue=? AND status=? AND lease_owner=? AND draw_hash=?",
                        new String[] {claim.targetIssue, RUNNING, claim.owner, claim.drawHash});
        if (n != 1) throw new IllegalStateException("R36_SETTLEMENT_TASK_LEASE_LOST");
    }

    public static void markRetry(final DbStore store, final TaskClaim claim, final String error) {
        if (store == null || claim == null) return;
        store.coordinatorR36()
                .runWrite(
                        "R36_SETTLEMENT_RETRY",
                        new DatabaseCoordinator.WriteRunnable() {
                            @Override
                            public void run(SQLiteDatabase db) {
                                long now = System.currentTimeMillis();
                                boolean permanentFailure = isPermanentSettlementFailureR911(error);
                                boolean transientFailure = !permanentFailure && isTransientSettlementFailure(error);
                                int attemptLimit =
                                        transientFailure
                                                ? Math.max(50, AppConfig.R36_SETTLEMENT_MAX_ATTEMPTS)
                                                : AppConfig.R36_SETTLEMENT_MAX_ATTEMPTS;
                                boolean terminal = claim.attempts >= attemptLimit;
                                ContentValues cv = new ContentValues();
                                cv.put("status", terminal ? FAILED_TERMINAL : RETRY_WAIT);
                                cv.put("updated_at", now);
                                cv.put("completed_at", terminal ? now : 0L);
                                cv.put("lease_owner", "");
                                cv.put("lease_until", 0L);
                                cv.put(
                                        "next_attempt_at",
                                        terminal
                                                ? 0L
                                                : now + retryDelayMsR911(claim, transientFailure));
                                cv.put("last_error", trim(error, 700));
                                cv.put("terminal_code", terminal ? FAILED_TERMINAL : "");
                                cv.put("last_progress_at", now);
                                cv.put("process_boot_id", ProcessBootIdentityR906.id());
                                cv.put("failure_class", SettlementFailureClassR906.classify(error).name());
                                if (!terminal) {
                                    cv.put("task_class", LIVE_RETRY);
                                    cv.put("priority", PRIORITY_LIVE_RETRY);
                                }
                                db.update(
                                        DbHelper.TABLE_R36_SETTLEMENT_TASK,
                                        cv,
                                        "target_issue=? AND status=? AND lease_owner=?",
                                        new String[] {claim.targetIssue, RUNNING, claim.owner});
                            }
                        });
    }

    private static long retryDelayMsR911(TaskClaim claim, boolean transientFailure) {
        boolean live = claim != null
                && (LIVE_CURRENT.equals(claim.taskClass) || LIVE_RETRY.equals(claim.taskClass));
        int attempts = claim == null ? 1 : Math.max(1, claim.attempts);
        if (live) {
            long base = transientFailure ? 120L : 350L;
            return Math.min(2000L, base * (1L << Math.min(4, attempts - 1)));
        }
        long base = transientFailure ? 1000L : 5000L;
        return Math.min(transientFailure ? 15000L : 60000L,
                base * (1L << Math.min(5, attempts - 1)));
    }

    private static boolean isPermanentSettlementFailureR911(String error) {
        String value = safe(error).toUpperCase(java.util.Locale.US);
        return value.indexOf("NO SUCH COLUMN") >= 0
                || value.indexOf("NO SUCH TABLE") >= 0
                || value.indexOf("SQLITE_ERROR") >= 0 && value.indexOf("LOCKED") < 0
                || value.indexOf("SYNTAX ERROR") >= 0
                || value.indexOf("DRAW_HASH_MISMATCH") >= 0
                || value.indexOf("FIRST_WRITE_CONFLICT") >= 0
                || value.indexOf("FROZEN_CONTRACT_INVALID") >= 0
                || value.indexOf("FROZEN_K3_INVALID") >= 0
                || value.indexOf("ACTUAL_POSITION_NUMBER_INVALID") >= 0;
    }

    private static boolean isTransientSettlementFailure(String error) {
        String value = safe(error).toUpperCase(java.util.Locale.US);
        return value.indexOf("WRITE_COORDINATION") >= 0
                || value.indexOf("TX_FAILED") >= 0
                || value.indexOf("LOCKED") >= 0
                || value.indexOf("BUSY") >= 0
                || value.indexOf("TIMEOUT") >= 0
                || value.indexOf("INTERRUPT") >= 0
                || value.indexOf("OOM") >= 0;
    }

    public static String nextRunnable(DbStore store, long now) {
        if (store == null) return "";
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT target_issue FROM "
                                            + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " WHERE ((status IN ('PENDING','RETRY_WAIT') AND"
                                            + " COALESCE(next_attempt_at,0)<=CAST(? AS INTEGER)) OR (status='RUNNING' AND"
                                            + " COALESCE(lease_until,0)<CAST(? AS INTEGER))) ORDER BY priority DESC,"
                                            + " COALESCE(next_attempt_at,0) ASC,created_at ASC LIMIT 1",
                                    new String[] {String.valueOf(now), String.valueOf(now)});
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        } finally {
            if (c != null) c.close();
        }
    }

    public static Terminal terminal(DbStore store, String target) {
        if (store == null) return null;
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT status,settlement_hash,last_error,completed_at,"
                                            + "terminal_code,task_class,attempts,source FROM "
                                            + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " WHERE target_issue=? LIMIT 1",
                                    new String[] {target});
            if (!c.moveToFirst() || !isTerminal(c.getString(0))) return null;
            return new Terminal(
                    target,
                    c.getString(0),
                    c.getString(1),
                    c.getString(2),
                    c.getLong(3),
                    c.getString(4),
                    c.getString(5),
                    c.getInt(6),
                    c.getString(7));
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * A durable task must never block all later targets when its referenced draw row is missing or
     * corrupt. Terminalization is still lease-guarded and atomic; no model or post-draw
     * reconstruction is attempted.
     */
    public static boolean markInvalidDrawTerminal(
            final DbStore store, final String target, final String reason) {
        if (store == null || !TargetIssueResolver.isValid(target)) return false;
        if (terminal(store, target) != null) return true;
        final long now = System.currentTimeMillis();
        final String owner = "R36-INVALID-DRAW-" + Thread.currentThread().getId();
        final TaskClaim claim = claim(store, target, owner, now);
        if (claim == null) return terminal(store, target) != null;
        final String error = trim(reason, 700);
        final String settlementHash =
                V51ChecksumEngine.sha256(
                        "R36_INVALID_DRAW|" + target + '|' + claim.drawHash + '|' + error);
        DatabaseCoordinator.TransactionResult<Boolean> out =
                store.coordinatorR36()
                        .runSettlementTransactionR36(
                                target,
                                claim.drawHash + "|INVALID",
                                new DatabaseCoordinator.TransactionCallable<Boolean>() {
                                    @Override
                                    public Boolean call(
                                            SQLiteDatabase db,
                                            DatabaseCoordinator.TransactionContext tx) {
                                        markTerminalWithinTransaction(
                                                db,
                                                claim,
                                                INVALID_DRAW_TERMINAL,
                                                settlementHash,
                                                error,
                                                now);
                                        RuntimeHealthLedgerR36.recordWithinTransaction(
                                                db,
                                                "SETTLEMENT_TERMINAL",
                                                "SET-INVALID-" + prefix(claim.drawHash, 12),
                                                target,
                                                INVALID_DRAW_TERMINAL,
                                                now,
                                                now,
                                                0L,
                                                0,
                                                1,
                                                true,
                                                "MODEL_CALLS=0|" + error);
                                        return Boolean.TRUE;
                                    }
                                },
                                Boolean.FALSE);
        if (out.success) return true;
        markRetry(store, claim, out.code);
        return false;
    }

    /**
     * Bounded migration seeding. Eligibility is deliberately not duplicated here: the sole OOS
     * owner validates immutable {@code first_seen_at} inside the settlement transaction. R815 used
     * provider {@code open_time > commit_at}, which stranded valid rows whenever a timezone parser
     * shifted open_time by eight hours.
     */
    public static int seedHistorical(DbStore store, int limit) {
        if (store == null) return 0;
        Cursor c = null;
        java.util.ArrayList<Models.Draw> draws = new java.util.ArrayList<Models.Draw>();
        final java.util.ArrayList<InvalidSeed> invalid = new java.util.ArrayList<InvalidSeed>();
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT"
                                        + " d.issue,d.open_time,d.n1,d.n2,d.n3,d.n4,d.n5,d.n6,d.n7,d.n8,d.n9,d.n10"
                                        + " FROM "
                                            + DrawDependencyInputCP12.SOURCE
                                            + " d JOIN "
                                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                            + " o ON o.target_issue=d.issue"
                                            + " LEFT JOIN "
                                            + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " t ON t.target_issue=d.issue WHERE o.settled=0 AND"
                                            + " o.pre_draw_frozen=1 AND o.commit_at>0 AND"
                                            + " t.target_issue IS NULL"
                                            + " ORDER BY d.issue ASC LIMIT"
                                            + " ?",
                                    new String[] {
                                        String.valueOf(Math.max(1, Math.min(500, limit)))
                                    });
            while (c.moveToNext()) {
                Models.Draw d = new Models.Draw();
                d.issue = c.getString(0);
                d.openTime = c.getLong(1);
                d.numbers = new int[10];
                StringBuilder raw = new StringBuilder(safe(d.issue)).append('|').append(d.openTime);
                for (int i = 0; i < 10; i++) {
                    d.numbers[i] = c.getInt(i + 2);
                    raw.append('|').append(d.numbers[i]);
                }
                if (d.isValid() && TargetIssueResolver.isValid(d.issue))
                    draws.add(d);
                else if (safe(d.issue).length() > 0)
                    invalid.add(
                            new InvalidSeed(
                                    d.issue,
                                    V51ChecksumEngine.sha256(
                                            "R36_INVALID_HISTORICAL_DRAW|" + raw.toString())));
            }
        } finally {
            if (c != null) c.close();
        }
        int valid = enqueueHistoricalDraws(store, draws);
        if (invalid.size() == 0) return valid;
        Integer bad =
                store.coordinatorR36()
                        .runWrite(
                                "R36_SETTLEMENT_SEED_INVALID",
                                new DatabaseCoordinator.WriteCallable<Integer>() {
                                    @Override
                                    public Integer call(SQLiteDatabase db) {
                                        long now = System.currentTimeMillis();
                                        int count = 0;
                                        for (int i = 0; i < invalid.size(); i++) {
                                            InvalidSeed x = invalid.get(i);
                                            ContentValues cv = new ContentValues();
                                            cv.put("target_issue", x.target);
                                            cv.put("draw_issue", x.target);
                                            cv.put("status", INVALID_DRAW_TERMINAL);
                                            cv.put("created_at", now);
                                            cv.put("updated_at", now);
                                            cv.put("completed_at", now);
                                            cv.put("lease_owner", "");
                                            cv.put("lease_until", 0L);
                                            cv.put("attempts", 0);
                                            cv.put("next_attempt_at", 0L);
                                            cv.put("draw_hash", x.hash);
                                            cv.put(
                                                    "settlement_hash",
                                                    V51ChecksumEngine.sha256(
                                                            "R36_INVALID_TERMINAL|" + x.hash));
                                            cv.put(
                                                    "last_error",
                                                    "HISTORICAL_DRAW_INVALID_OR_TIME_MISSING");
                                            cv.put("task_class", BACKLOG);
                                            cv.put("priority", PRIORITY_BACKLOG);
                                            cv.put("terminal_code", INVALID_DRAW_TERMINAL);
                                            cv.put("source", "HISTORICAL_SEED_INVALID");
                                            if (db.insertWithOnConflict(
                                                            DbHelper.TABLE_R36_SETTLEMENT_TASK,
                                                            null,
                                                            cv,
                                                            SQLiteDatabase.CONFLICT_IGNORE)
                                                    >= 0L) count++;
                                        }
                                        return Integer.valueOf(count);
                                    }
                                },
                                Integer.valueOf(0));
        return valid + bad.intValue();
    }

    /**
     * R819 bounded upgrade repair for rows created by pre-R815 broad draw-history seeding.
     *
     * <p>These rows have no frozen pre-draw observation and therefore can never produce a strict
     * OOS label.  They are retained as audited terminal rows rather than deleted.  Active or
     * unexpired leases are never touched.
     */
    /**
     * R820 process-start fence recovery. A fresh Android process cannot have a surviving in-memory
     * settlement worker from the previous process, therefore every durable RUNNING row is an orphan.
     * Reset only the lease/status; never change draw identity, attempts, canonical prediction or result.
     */
    public static int recoverOrphanedRunningLeasesOnProcessStart(final DbStore store) {
        return recoverImpossibleRunningLeasesWhenIdle(store, "PROCESS_START");
    }

    /**
     * R900 sole-owner invariant repair.  A durable RUNNING row is valid only while the
     * process-wide SettlementExecutionGate is active.  When that gate is idle there is no
     * in-memory SettlementEngine capable of owning ANY RUNNING lease, so every such row is an
     * orphan even if its wall-clock lease has not expired yet.
     *
     * <p>This repairs only status/lease scheduling fields.  It never changes the frozen
     * prediction, draw, hit label, settlement hash, attempts or any REAL/SHADOW result.
     */
    public static int recoverImpossibleRunningLeasesWhenIdle(
            final DbStore store, final String reason) {
        if (store == null || !SettlementExecutionGateR819.tryEnterRecovery()) return 0;
        try {
            Integer out = store.coordinatorR36().runWrite(
                                "R900_SETTLEMENT_IDLE_GATE_RECOVERY",
                                new DatabaseCoordinator.WriteCallable<Integer>() {
                                    @Override
                                    public Integer call(SQLiteDatabase db) {
                                        long now = System.currentTimeMillis();
                                        ContentValues cv = new ContentValues();
                                        cv.put("status", RETRY_WAIT);
                                        cv.put("lease_owner", "");
                                        cv.put("lease_until", 0L);
                                        cv.put("next_attempt_at", 0L);
                                        cv.put("updated_at", now);
                                        cv.put("last_progress_at", now);
                                        cv.put("process_boot_id", "");
                                        cv.put("failure_class", "TRANSIENT_DB_BUSY");
                                        db.execSQL("UPDATE " + DbHelper.TABLE_R36_SETTLEMENT_TASK + " SET recovery_count=recovery_count+1 WHERE status='RUNNING'");
                                        cv.put(
                                                "last_error",
                                                "R914_FENCED_ORPHAN_RECOVERED:" + trim(reason, 120));
                                        int n =
                                                db.update(
                                                        DbHelper.TABLE_R36_SETTLEMENT_TASK,
                                                        cv,
                                                        "status=?",
                                                        new String[] {RUNNING});
                                        return Integer.valueOf(Math.max(0, n));
                                    }
                                },
                                Integer.valueOf(0));
            return out == null ? 0 : out.intValue();
        } finally {
            SettlementExecutionGateR819.exitRecovery();
        }
    }

    /**
     * R911 targeted current-issue recovery for retryable FAILED_TERMINAL rows left by older
     * versions. Permanent draw/PIT/integrity conflicts remain terminal.
     */
    public static boolean reopenCurrentRecoverableFailedR911(
            final DbStore store, final String target) {
        if(store==null||!TargetIssueResolver.isValid(target))return false;
        final RuntimeException[] cause=new RuntimeException[1];
        Boolean result=store.coordinatorR36().runAtomicWrite("R911_SETTLEMENT_REOPEN_CURRENT_FAILED",db->{
            try {return reopenRecoverableFailedWithinTransactionF02(db,1,System.currentTimeMillis(),target)==1;}
            catch(RuntimeException bad){cause[0]=bad;throw bad;}
        },null);
        if(result==null)throw new IllegalStateException("F02_CURRENT_RECOVERY_TRANSACTION_FAILED|target="+target,cause[0]);
        return result;
    }

    /** R909 targeted repair for the current exact issue; never scans history. */
    public static boolean reopenCurrentFalseNoPredraw(final DbStore store, final String target) {
        if (store == null || !TargetIssueResolver.isValid(target)) return false;
        Boolean out = store.coordinatorR36().runWrite(
                "R909_SETTLEMENT_REOPEN_CURRENT_FALSE_NO_PREDRAW",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        Cursor c = null;
                        boolean eligible = false;
                        try {
                            c = db.rawQuery(
                                    "SELECT 1 FROM " + DbHelper.TABLE_R36_SETTLEMENT_TASK + " t"
                                            + " JOIN " + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                            + " o ON o.target_issue=t.target_issue"
                                            + " JOIN " + DrawDependencyInputCP12.SOURCE
                                            + " d ON d.issue=t.target_issue AND d.dirty=0"
                                            + " WHERE t.target_issue=? AND t.status=?"
                                            + " AND o.pre_draw_frozen=1 AND o.settled=0 LIMIT 1",
                                    new String[] {target, NO_PREDRAW_CANONICAL_TERMINAL});
                            eligible = c.moveToFirst();
                        } finally { if (c != null) c.close(); }
                        if (!eligible) return Boolean.FALSE;
                        long now = System.currentTimeMillis();
                        ContentValues cv = new ContentValues();
                        cv.put("status", RETRY_WAIT);
                        cv.put("task_class", LIVE_CURRENT);
                        cv.put("priority", PRIORITY_LIVE_CURRENT);
                        cv.put("updated_at", now);
                        cv.put("completed_at", 0L);
                        cv.put("lease_owner", "");
                        cv.put("lease_until", 0L);
                        cv.put("next_attempt_at", 0L);
                        cv.put("settlement_hash", "");
                        cv.put("terminal_code", "");
                        cv.put("last_error", "R909_RECOVERED_CURRENT_FALSE_NO_PREDRAW");
                        cv.put("source", "R909_CURRENT_EXACT_RECOVERY");
                        return Boolean.valueOf(db.update(
                                DbHelper.TABLE_R36_SETTLEMENT_TASK, cv,
                                "target_issue=? AND status=?",
                                new String[] {target, NO_PREDRAW_CANONICAL_TERMINAL}) == 1);
                    }
                }, Boolean.FALSE);
        return out != null && out.booleanValue();
    }

    /**
     * R905 repairs only a provably false NO_PREDRAW terminal. A row is eligible iff an immutable
     * pre-draw SHADOW observation already exists, it is still unsettled, and the exact draw is
     * present and clean. This never creates/recomputes a prediction after draw.
     */
    public static int reopenRecoverableNoPredrawTerminals(final DbStore store, final int limit) {
        if (store == null) return 0;
        final int bounded = Math.max(1, Math.min(500, limit));
        Integer out = store.coordinatorR36().runWrite(
                "R905_REOPEN_FALSE_NO_PREDRAW_TERMINALS",
                new DatabaseCoordinator.WriteCallable<Integer>() {
                    @Override public Integer call(SQLiteDatabase db) {
                        Cursor c = null;
                        java.util.ArrayList<String> targets = new java.util.ArrayList<String>();
                        try {
                            c = db.rawQuery(
                                    "SELECT t.target_issue FROM " + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " t JOIN " + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                            + " o ON o.target_issue=t.target_issue"
                                            + " JOIN " + DrawDependencyInputCP12.SOURCE
                                            + " d ON d.issue=t.target_issue AND d.dirty=0"
                                            + " WHERE t.status=? AND o.pre_draw_frozen=1 AND o.settled=0"
                                            + " ORDER BY t.target_issue ASC LIMIT ?",
                                    new String[]{NO_PREDRAW_CANONICAL_TERMINAL, String.valueOf(bounded)});
                            while (c.moveToNext()) targets.add(safe(c.getString(0)));
                        } finally { if (c != null) c.close(); }
                        long now = System.currentTimeMillis();
                        int changed = 0;
                        for (int i = 0; i < targets.size(); i++) {
                            ContentValues cv = new ContentValues();
                            cv.put("status", RETRY_WAIT);
                            cv.put("task_class", LIVE_RETRY);
                            cv.put("priority", PRIORITY_LIVE_RETRY);
                            cv.put("updated_at", now);
                            cv.put("completed_at", 0L);
                            cv.put("lease_owner", "");
                            cv.put("lease_until", 0L);
                            cv.put("next_attempt_at", 0L);
                            cv.put("settlement_hash", "");
                            cv.put("terminal_code", "");
                            cv.put("last_error", "R905_RECOVERED_FALSE_NO_PREDRAW_WITH_FROZEN_OBSERVATION");
                            cv.put("source", "R905_RECOVERABLE_NO_PREDRAW");
                            changed += db.update(
                                    DbHelper.TABLE_R36_SETTLEMENT_TASK, cv,
                                    "target_issue=? AND status=?",
                                    new String[]{targets.get(i), NO_PREDRAW_CANONICAL_TERMINAL});
                        }
                        return Integer.valueOf(changed);
                    }
                }, Integer.valueOf(0));
        return out == null ? 0 : out.intValue();
    }

    /**
     * Reopens historical eligible rows that an older build exhausted only because of a retryable
     * SQLite/memory failure, or the corrected unknown-probability SHADOW label defect.
     * Permanent PIT, draw and integrity conflicts remain terminal; the settlement owner rechecks
     * every reopened row before committing a label.
     */
    public static int reopenRecoverableFailedTerminals(final DbStore store, final int limit) {
        if (store == null) return 0;
        final int bounded = Math.max(1, Math.min(64, limit));
        final RuntimeException[] cause = new RuntimeException[1];
        Integer out=store.coordinatorR36().runAtomicWrite("R914_REOPEN_RETRYABLE_FAILED_SETTLEMENTS",db->{
            try { return reopenRecoverableFailedWithinTransactionF02(db,bounded,System.currentTimeMillis()); }
            catch(RuntimeException bad) {cause[0]=bad;throw bad;}
        },null);
        if(out==null)throw new IllegalStateException("F02_RECOVERY_TRANSACTION_FAILED",cause[0]);
        return out;
    }
    static int reopenRecoverableFailedWithinTransactionF02(SQLiteDatabase db,int bound,long now) {
        return reopenRecoverableFailedWithinTransactionF02(db,bound,now,null);
    }
    private static int reopenRecoverableFailedWithinTransactionF02(SQLiteDatabase db,int bound,long now,String exactTarget) {
        if(db==null||!db.inTransaction())throw new IllegalStateException("F02_RECOVERY_TX_REQUIRED");
        int bounded=Math.max(1,Math.min(64,bound));
        java.util.ArrayList<String> targets = new java.util.ArrayList<String>();
        try(Cursor c=db.rawQuery("SELECT t.target_issue,t.terminal_code,t.failure_class FROM "
                +DbHelper.TABLE_R36_SETTLEMENT_TASK+" t JOIN "+DbHelper.TABLE_R35_SHADOW_OBSERVATION
                +" s ON s.target_issue=t.target_issue JOIN "+DrawDependencyInputCP12.SOURCE+" d ON d.issue=t.target_issue"
                +" WHERE t.status=? AND s.pre_draw_frozen=1 AND s.settled=0 AND d.dirty=0"
                +" AND t.recovery_count<12"+(exactTarget==null?"":" AND t.target_issue=?")+" ORDER BY t.priority DESC,t.target_issue ASC LIMIT ?",
                exactTarget==null?new String[]{FAILED_TERMINAL,Integer.toString(bounded)}:new String[]{FAILED_TERMINAL,exactTarget,Integer.toString(bounded)})) {
            while(c.moveToNext()) {
                String target=safe(c.getString(0)),code=safe(c.getString(1)),failure=safe(c.getString(2));
                boolean corrected="FROZEN_PROBABILITY_INVALID".equals(code)
                        &&OosAfterDrawWriterR36.recoverableUncalibratedShadow(db,target);
                boolean transientFailure=SettlementFailureClassR906.TRANSIENT_DB_BUSY.name().equals(failure)
                        ||SettlementFailureClassR906.TRANSIENT_MEMORY_PRESSURE.name().equals(failure);
                boolean restoredDrawR48="DRAW_FIRST_SEEN_MISSING".equals(code)
                        &&recoverableRestoredDrawReceiptR48(db,target);
                // UNKNOWN / empty class is not a license to overwrite permanent PIT failures.
                if(corrected||restoredDrawR48||(transientFailure&&!permanentFailureR81(code)&&OosAfterDrawWriterR36.recoverableTransientFrozenR81(db,target))) targets.add(target);
            }
        }
        int changed=0;
        for(String target:targets) {
            java.util.Map<String,Object> old=new java.util.TreeMap<>();
            long generation,recovery;String oldDraw,oldCode;
            try(Cursor c=db.rawQuery("SELECT * FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK
                    +" WHERE target_issue=? AND status=?",new String[]{target,FAILED_TERMINAL})) {
                if(!c.moveToFirst())continue;
                for(int i=0;i<c.getColumnCount();i++) {
                    String key=c.getColumnName(i);
                    if(c.isNull(i))old.put(key,null);
                    else if(c.getType(i)==Cursor.FIELD_TYPE_INTEGER)old.put(key,c.getLong(i));
                    else if(c.getType(i)==Cursor.FIELD_TYPE_FLOAT)old.put(key,c.getDouble(i));
                    else old.put(key,c.getString(i));
                }
                generation=c.getLong(c.getColumnIndexOrThrow("lease_generation"));
                recovery=c.getLong(c.getColumnIndexOrThrow("recovery_count"));
                oldDraw=c.getString(c.getColumnIndexOrThrow("draw_hash"));
                oldCode=c.getString(c.getColumnIndexOrThrow("terminal_code"));
            }
            if(generation==Long.MAX_VALUE||recovery<0||recovery>=12)
                throw new IllegalStateException("F02_RECOVERY_GENERATION_INVALID");
            String json=com.ethk3.novaonepro.r952.StrictCanonicalJson.writeContext(old);
            String snapshotHash=V51ChecksumEngine.sha256(json);
            String recoveryId=V51ChecksumEngine.sha256("F02_RECOVERY_V1|"+target+'|'+recovery+'|'+snapshotHash);
            // Startup schema owner installed this append-only ledger. No business-path DDL.
            db.execSQL("INSERT INTO "+REPAIR_AUDIT
                    +"(recovery_id,target_issue,prior_generation,prior_recovery_count,prior_status,prior_code,prior_draw_hash,prior_snapshot_json,prior_snapshot_hash,reason,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                    new Object[]{recoveryId,target,generation,recovery,FAILED_TERMINAL,oldCode,oldDraw,json,snapshotHash,
                        "FROZEN_PROBABILITY_INVALID".equals(oldCode)?"CORRECTED_SHADOW_ACTION_LABEL"
                        :"DRAW_FIRST_SEEN_MISSING".equals(oldCode)?"IMMUTABLE_DRAW_RECEIPT_RESTORED":"TRANSIENT_DB_OR_MEMORY",now});
            ContentValues cv=new ContentValues();cv.put("status",RETRY_WAIT);cv.put("task_class",LIVE_RETRY);
            cv.put("priority",PRIORITY_LIVE_RETRY);cv.put("updated_at",now);cv.put("completed_at",0L);
            cv.put("lease_owner","");cv.put("lease_until",0L);cv.put("next_attempt_at",0L);
            cv.put("settlement_hash","");cv.put("terminal_code","");cv.put("failure_class","");
            cv.put("last_error","F02_AUDITED_RECOVERY|"+recoveryId);cv.put("source","F02_AUDITED_RECOVERY");
            cv.put("recovery_count",recovery+1L);cv.put("lease_generation",generation+1L);
            int n=db.update(DbHelper.TABLE_R36_SETTLEMENT_TASK,cv,
                    "target_issue=? AND status=? AND lease_generation=? AND recovery_count=? AND draw_hash=?",
                    new String[]{target,FAILED_TERMINAL,Long.toString(generation),Long.toString(recovery),oldDraw});
            if(n!=1)throw new IllegalStateException("F02_RECOVERY_CAS_CARDINALITY_"+n);
            changed++;
        }
        return changed;
    }

    private static boolean permanentFailureR81(String code) {
        SettlementFailureClassR906 kind=SettlementFailureClassR906.classify(code);
        return kind==SettlementFailureClassR906.BLOCKED_PIT_CONFLICT
                ||kind==SettlementFailureClassR906.BLOCKED_DRAW_CONFLICT
                ||kind==SettlementFailureClassR906.PERMANENT_INTEGRITY
                ||kind==SettlementFailureClassR906.INVALID_DRAW
                ||kind==SettlementFailureClassR906.EXCLUDED_NO_PREDRAW;
    }

    private static boolean recoverableRestoredDrawReceiptR48(SQLiteDatabase db,String target) {
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK
                +" t JOIN "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" o ON o.target_issue=t.target_issue"
                +" JOIN "+DrawDependencyInputCP12.SOURCE+" d ON d.issue=t.target_issue AND d.dirty=0"
                +" WHERE t.target_issue=? AND t.status=? AND t.terminal_code='DRAW_FIRST_SEEN_MISSING'"
                +" AND o.pre_draw_frozen=1 AND o.settled=0 AND o.commit_at>0 AND d.first_seen_at>o.commit_at"
                +" AND d.draw_hash_version="+DrawHashR36.CURRENT_VERSION+" AND d.draw_hash_v2=t.draw_hash"
                +" AND EXISTS(SELECT 1 FROM "+DbHelper.TABLE_R811_DRAW_CANONICAL
                +" c WHERE c.target_issue=d.issue AND c.status='CONFIRMED_CANONICAL_DRAW'"
                +" AND c.canonical_draw_hash=d.draw_hash_v2) LIMIT 1",new String[]{target,FAILED_TERMINAL})) {
            return c.moveToFirst();
        }
    }

    public static final String REPAIR_AUDIT="v57_settlement_repair_audit_f02";
    /** Called ONLY by DbHelper.ensureR36Schema inside the startup schema-owner transaction. */
    static void ensureRepairAuditSchema(SQLiteDatabase db) {
        if(db==null||!db.inTransaction())throw new IllegalStateException("F02_AUDIT_SCHEMA_TX_REQUIRED");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+REPAIR_AUDIT+"(recovery_id TEXT PRIMARY KEY,target_issue TEXT NOT NULL,prior_generation INTEGER NOT NULL,prior_recovery_count INTEGER NOT NULL,prior_status TEXT NOT NULL,prior_code TEXT NOT NULL,prior_draw_hash TEXT NOT NULL,prior_snapshot_json TEXT NOT NULL,prior_snapshot_hash TEXT NOT NULL UNIQUE,reason TEXT NOT NULL,created_at INTEGER NOT NULL,UNIQUE(target_issue,prior_recovery_count))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_f02_recovery_target ON "+REPAIR_AUDIT+"(target_issue,prior_recovery_count)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS f02_recovery_no_update BEFORE UPDATE ON "+REPAIR_AUDIT+" BEGIN SELECT RAISE(ABORT,'F02_RECOVERY_RECEIPT_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS f02_recovery_no_delete BEFORE DELETE ON "+REPAIR_AUDIT+" BEGIN SELECT RAISE(ABORT,'F02_RECOVERY_RECEIPT_PERMANENT'); END");
    }

    /** One-denominator truth: only immutable pre-draw rows with an exact clean draw are eligible. */
    public static EligibilitySnapshot eligibilitySnapshot(DbStore store) {
        if (store == null) return new EligibilitySnapshot(0, 0, 0, 0, 0, 0);
        Cursor c = null;
        try {
            c = store.readable().rawQuery(
                    "SELECT"
                            + " SUM(CASE WHEN o.settled=1 THEN 1 ELSE 0 END),"
                            + " SUM(CASE WHEN o.settled=0 THEN 1 ELSE 0 END),"
                            + " SUM(CASE WHEN o.settled=0 AND t.target_issue IS NULL THEN 1 ELSE 0 END),"
                            + " SUM(CASE WHEN o.settled=0 AND t.status IN ('PENDING','RETRY_WAIT') THEN 1 ELSE 0 END),"
                            + " SUM(CASE WHEN o.settled=0 AND t.status='RUNNING' THEN 1 ELSE 0 END),"
                            + " SUM(CASE WHEN o.settled=0 AND t.status IN ('FAILED_TERMINAL','INVALID_DRAW_TERMINAL') THEN 1 ELSE 0 END)"
                            + " FROM " + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " o"
                            + " JOIN " + DrawDependencyInputCP12.SOURCE + " d ON d.issue=o.target_issue AND d.dirty=0"
                            + " LEFT JOIN " + DbHelper.TABLE_R36_SETTLEMENT_TASK + " t ON t.target_issue=o.target_issue"
                            + " WHERE o.pre_draw_frozen=1",
                    null);
            if (!c.moveToFirst()) return new EligibilitySnapshot(0, 0, 0, 0, 0, 0);
            return new EligibilitySnapshot(
                    c.isNull(0) ? 0 : c.getInt(0),
                    c.isNull(1) ? 0 : c.getInt(1),
                    c.isNull(2) ? 0 : c.getInt(2),
                    c.isNull(3) ? 0 : c.getInt(3),
                    c.isNull(4) ? 0 : c.getInt(4),
                    c.isNull(5) ? 0 : c.getInt(5));
        } finally {
            if (c != null) c.close();
        }
    }

    /** Diagnostic hard invariant: gate idle implies durable RUNNING count must be zero. */
    public static int runningCount(DbStore store) {
        if (store == null) return -1;
        Cursor c = null;
        try {
            c = store.readable().rawQuery(
                    "SELECT COUNT(1) FROM " + DbHelper.TABLE_R36_SETTLEMENT_TASK + " WHERE status=?",
                    new String[] {RUNNING});
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    /**
     * Closes an already-drawn task that has no immutable pre-draw canonical as an explicit
     * exclusion.  This is still a settlement terminal for the draw-task denominator, but it never
     * creates a hit/miss label and never enters the prediction denominator.
     */
    public static int terminalizeAuditedNoPredrawBacklog(final DbStore store, final int limit) {
        if (store == null) return 0;
        final int bounded = Math.max(1, Math.min(500, limit));
        Integer out =
                store.coordinatorR36()
                        .runWrite(
                                "R919_TERMINALIZE_AUDITED_NO_PREDRAW",
                                new DatabaseCoordinator.WriteCallable<Integer>() {
                                    @Override
                                    public Integer call(SQLiteDatabase db) {
                                        Cursor c = null;
                                        java.util.ArrayList<LegacyBacklog> rows =
                                                new java.util.ArrayList<LegacyBacklog>();
                                        long now = System.currentTimeMillis();
                                        try {
                                            c =
                                                    db.rawQuery(
                                                            "SELECT t.target_issue,t.draw_hash FROM "
                                                                    + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                                                    + " t LEFT JOIN "
                                                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                                                    + " o ON o.target_issue=t.target_issue"
                                                                    + " WHERE t.task_class='BACKLOG'"
                                                                    + " AND t.status IN ('PENDING','RETRY_WAIT','RUNNING')"
                                                                    + " AND (t.status<>'RUNNING' OR COALESCE(t.lease_until,0)<CAST(? AS INTEGER))"
                                                                    + " AND o.target_issue IS NULL"
                                                                    + " ORDER BY t.created_at ASC LIMIT ?",
                                                            new String[] {
                                                                String.valueOf(now),
                                                                String.valueOf(bounded)
                                                            });
                                            while (c.moveToNext())
                                                rows.add(
                                                        new LegacyBacklog(
                                                                c.getString(0), c.getString(1)));
                                        } finally {
                                            if (c != null) c.close();
                                        }
                                        int retired = 0;
                                        for (int i = 0; i < rows.size(); i++) {
                                            LegacyBacklog row = rows.get(i);
                                            ContentValues cv = new ContentValues();
                                            cv.put("status", NO_PREDRAW_CANONICAL_TERMINAL);
                                            cv.put("updated_at", now);
                                            cv.put("completed_at", now);
                                            cv.put("lease_owner", "");
                                            cv.put("lease_until", 0L);
                                            cv.put("next_attempt_at", 0L);
                                            cv.put("terminal_code", "R919_AUDITED_NO_IMMUTABLE_PREDRAW");
                                            cv.put(
                                                    "last_error",
                                                    "AUDITED_TERMINAL:NO_IMMUTABLE_PREDRAW_CANONICAL;EXCLUDED_FROM_PREDICTION_STATS");
                                            cv.put(
                                                    "settlement_hash",
                                                    V51ChecksumEngine.sha256(
                                                            "R919_AUDITED_NO_PREDRAW_TERMINAL|"
                                                                    + row.target
                                                                    + '|'
                                                                    + row.drawHash));
                                            retired +=
                                                    db.update(
                                                            DbHelper.TABLE_R36_SETTLEMENT_TASK,
                                                            cv,
                                                            "target_issue=? AND task_class='BACKLOG'"
                                                                    + " AND status IN ('PENDING','RETRY_WAIT','RUNNING')"
                                                                    + " AND (status<>'RUNNING' OR COALESCE(lease_until,0)<CAST(? AS INTEGER))",
                                                            new String[] {
                                                                row.target, String.valueOf(now)
                                                            });
                                        }
                                        return Integer.valueOf(retired);
                                    }
                                },
                                Integer.valueOf(0));
        return out.intValue();
    }

    /** Compatibility entry point retained for older callers and stored diagnostics. */
    public static int retireLegacyBacklogWithoutPredraw(final DbStore store, final int limit) {
        return terminalizeAuditedNoPredrawBacklog(store, limit);
    }

    private static boolean isTerminal(String s) {
        String x = safe(s);
        return COMPLETED.equals(x)
                || FAILED_TERMINAL.equals(x)
                || NO_PREDRAW_CANONICAL_TERMINAL.equals(x)
                || INVALID_DRAW_TERMINAL.equals(x);
    }

    private static boolean matchesTaskHash(String stored, Models.Draw draw) {
        return stored.equals(DrawHashR36.sha256V2(draw)) || stored.equals(DrawHashR36.sha256(draw));
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

    private static String safeOwner(String x) {
        String s = safe(x);
        return s.length() == 0 ? "R36-SETTLE-" + Thread.currentThread().getId() : s;
    }

    private static String safe(String x) {
        return x == null ? "" : x.trim();
    }

    private static String trim(String x, int n) {
        String s = safe(x);
        return s.length() > n ? s.substring(0, n) : s;
    }

    private static String prefix(String x, int n) {
        String s = safe(x);
        return s.length() <= n ? s : s.substring(0, n);
    }

    private static final class InvalidSeed {
        final String target, hash;

        InvalidSeed(String target, String hash) {
            this.target = safe(target);
            this.hash = safe(hash);
        }
    }

    private static final class LegacyBacklog {
        final String target, drawHash;

        LegacyBacklog(String target, String drawHash) {
            this.target = safe(target);
            this.drawHash = safe(drawHash);
        }
    }

    public static final class TaskClaim {
        public final String targetIssue, drawHash, owner, taskClass, source, processBootId;
        public final int attempts, priority, leaseGeneration;

        TaskClaim(String t, String h, String o, int a, String taskClass, int priority, String source,
                  int leaseGeneration, String processBootId) {
            targetIssue = safe(t);
            drawHash = safe(h);
            owner = safe(o);
            attempts = a;
            this.taskClass = safe(taskClass);
            this.priority = priority;
            this.source = safe(source);
            this.leaseGeneration = Math.max(0, leaseGeneration);
            this.processBootId = safe(processBootId);
        }
    }

    public static final class Terminal {
        public final String targetIssue, status, settlementHash, error, terminalCode, taskClass, source;
        public final int attempts;
        public final long completedAt;

        Terminal(
                String t,
                String s,
                String h,
                String e,
                long at,
                String code,
                String taskClass,
                int attempts,
                String source) {
            targetIssue = safe(t);
            status = safe(s);
            settlementHash = safe(h);
            error = safe(e);
            completedAt = at;
            terminalCode = safe(code);
            this.taskClass = safe(taskClass);
            this.attempts = attempts;
            this.source = safe(source);
        }
    }

    public static final class EligibilitySnapshot {
        public final int settled, open, missingTask, runnable, running, blocked;

        EligibilitySnapshot(int settled, int open, int missingTask, int runnable, int running, int blocked) {
            this.settled = Math.max(0, settled);
            this.open = Math.max(0, open);
            this.missingTask = Math.max(0, missingTask);
            this.runnable = Math.max(0, runnable);
            this.running = Math.max(0, running);
            this.blocked = Math.max(0, blocked);
        }

        public int eligible() { return settled + open; }

        public String canonical() {
            return "R914_ELIGIBLE_SETTLEMENT|eligible=" + eligible()
                    + "|settled=" + settled
                    + "|open=" + open
                    + "|missingTask=" + missingTask
                    + "|runnable=" + runnable
                    + "|running=" + running
                    + "|blocked=" + blocked;
        }
    }

public static Terminal terminalSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String target) {
        if (store == null) return null;
        Cursor c = null;
        try {
            c =
                    report.select(
                                    "SELECT status,settlement_hash,last_error,completed_at,"
                                            + "terminal_code,task_class,attempts,source FROM "
                                            + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " WHERE target_issue=? LIMIT 1",
                                    new String[] {target});
            if (!c.moveToFirst() || !isTerminalSnapshotCP4(report, c.getString(0))) return null;
            return new Terminal(
                    target,
                    c.getString(0),
                    c.getString(1),
                    c.getString(2),
                    c.getLong(3),
                    c.getString(4),
                    c.getString(5),
                    c.getInt(6),
                    c.getString(7));
        } finally {
            if (c != null) c.close();
        }
    }

public static boolean isTerminalSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String s) {
        String x = safeSnapshotCP4(report, s);
        return COMPLETED.equals(x)
                || FAILED_TERMINAL.equals(x)
                || NO_PREDRAW_CANONICAL_TERMINAL.equals(x)
                || INVALID_DRAW_TERMINAL.equals(x);
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String x) {
        return x == null ? "" : x.trim();
    }
}

