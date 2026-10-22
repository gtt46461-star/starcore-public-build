package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * R926 append-only per-issue runtime event ledger and the single materialized issue projection.
 *
 * <p>Design invariants:
 * <ul>
 *   <li>The event table is immutable. No UPDATE/DELETE is permitted after insert.</li>
 *   <li>Business transactions call {@link #appendWithinTransaction}; that method performs DML only.</li>
 *   <li>Schema DDL is owned by the Critical schema migration before any business transaction runs.</li>
 *   <li>dedup_key is unique, so duplicate callbacks converge on one durable event.</li>
 *   <li>The projection state rank is monotonic. Late/old callbacks can add evidence but cannot regress state.</li>
 *   <li>Projection rebuild uses the original stored event_id; replay never fabricates a replacement id.</li>
 * </ul>
 */
public final class RuntimeEventLedgerR926 {
    public static final String TABLE_EVENT = "v57_runtime_event_r926";
    public static final String TABLE_PROJECTION = "v57_issue_projection_r926";
    public static final String TABLE_OUTBOX = "v57_runtime_event_outbox_r926";
    public static final String LEGACY_UNKNOWN = "LEGACY_UNKNOWN";

    public static final int RANK_REQUESTED = 10;
    public static final int RANK_ACCEPTED = 20;
    public static final int RANK_QUEUED = 30;
    public static final int RANK_STARTED = 40;
    public static final int RANK_FROZEN = 50;
    public static final int RANK_CANONICAL = 60;
    public static final int RANK_SETTLED = 70;
    public static final int RANK_FULL = 80;
    public static final int RANK_LEARNED = 90;
    public static final int RANK_TERMINAL = 100;

    private static final AtomicBoolean STARTUP_RECONCILED = new AtomicBoolean(false);

    public static final class Event {
        public final String gameCode;
        public final String targetIssue;
        public final String epochId;
        public final String attemptId;
        public final String type;
        public final String stage;
        public final String payloadText;
        public final String payloadHash;
        public final String source;
        public final String dedupKey;
        public final int rank;
        public final long createdAt;
        private final String storedEventId;

        private Event(
                String issue,
                String epoch,
                String attempt,
                String type,
                String stage,
                int rank,
                String payloadText,
                String source,
                String dedupKey,
                long createdAt,
                String storedEventId) {
            gameCode = ReleaseManifestR926.GAME_CODE;
            targetIssue = safe(issue);
            epochId = safe(epoch);
            attemptId = safe(attempt);
            this.type = safe(type);
            this.stage = safe(stage);
            this.rank = Math.max(0, rank);
            this.payloadText = normalizePayload(payloadText);
            this.payloadHash = V51ChecksumEngine.sha256(this.payloadText);
            this.source = safe(source);
            this.dedupKey = safe(dedupKey);
            this.createdAt = Math.max(1L, createdAt);
            this.storedEventId = safe(storedEventId);
        }

        public static Event of(
                String issue,
                String epoch,
                String attempt,
                String type,
                String stage,
                int rank,
                String payloadText,
                String source,
                String dedupKey) {
            return new Event(
                    issue,
                    epoch,
                    attempt,
                    type,
                    stage,
                    rank,
                    payloadText,
                    source,
                    dedupKey,
                    System.currentTimeMillis(),
                    "");
        }

        static Event replay(
                String storedEventId,
                String issue,
                String epoch,
                String attempt,
                String type,
                String stage,
                int rank,
                String payloadText,
                String source,
                String dedupKey,
                long createdAt) {
            return new Event(
                    issue,
                    epoch,
                    attempt,
                    type,
                    stage,
                    rank,
                    payloadText,
                    source,
                    dedupKey,
                    createdAt,
                    storedEventId);
        }

        public String eventId() {
            if (storedEventId.length() > 0) return storedEventId;
            return V51ChecksumEngine.sha256(
                    gameCode
                            + '|'
                            + targetIssue
                            + '|'
                            + epochId
                            + '|'
                            + attemptId
                            + '|'
                            + type
                            + '|'
                            + stage
                            + '|'
                            + rank
                            + '|'
                            + payloadHash
                            + '|'
                            + dedupKey);
        }

        public boolean valid() {
            return targetIssue.length() > 0 && type.length() > 0 && dedupKey.length() > 0;
        }
    }

    /** A stable read model consumed by UI/report/export. */
    public static final class Projection {
        public final String targetIssue;
        public final String currentState;
        public final int stateRank;
        public final String epochId;
        public final String attemptId;
        public final String lastEventId;
        public final long eventWatermark;
        public final long asOf;
        public final String lastErrorCode;
        public final int requestSeen;
        public final int acceptedSeen;
        public final int rejectedSeen;
        public final int queuedSeen;
        public final int startedSeen;
        public final int frozenSeen;
        public final int canonicalSeen;
        public final int settledSeen;
        public final int fullSeen;
        public final int learnedSeen;
        public final int recoverySeen;

        Projection(Cursor c) {
            targetIssue = safe(c.getString(0));
            currentState = safe(c.getString(1));
            stateRank = c.getInt(2);
            epochId = safe(c.getString(3));
            attemptId = safe(c.getString(4));
            lastEventId = safe(c.getString(5));
            eventWatermark = c.getLong(6);
            asOf = c.getLong(7);
            lastErrorCode = safe(c.getString(8));
            requestSeen = c.getInt(9);
            acceptedSeen = c.getInt(10);
            rejectedSeen = c.getInt(11);
            queuedSeen = c.getInt(12);
            startedSeen = c.getInt(13);
            frozenSeen = c.getInt(14);
            canonicalSeen = c.getInt(15);
            settledSeen = c.getInt(16);
            fullSeen = c.getInt(17);
            learnedSeen = c.getInt(18);
            recoverySeen = c.getInt(19);
        }

        public String canonical() {
            return "issue="
                    + targetIssue
                    + "|state="
                    + currentState
                    + "|rank="
                    + stateRank
                    + "|epoch="
                    + epochId
                    + "|attempt="
                    + attemptId
                    + "|watermark="
                    + eventWatermark
                    + "|as_of="
                    + asOf
                    + "|error="
                    + lastErrorCode
                    + "|flags="
                    + requestSeen
                    + acceptedSeen
                    + rejectedSeen
                    + queuedSeen
                    + startedSeen
                    + frozenSeen
                    + canonicalSeen
                    + settledSeen
                    + fullSeen
                    + learnedSeen
                    + recoverySeen;
        }
    }

    private RuntimeEventLedgerR926() {}

    /** Critical-schema owner. Never call this from a canonical/settlement/FULL transaction. */
    public static void ensureSchema(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R926_EVENT_DB_NULL");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_EVENT
                        + "(seq INTEGER PRIMARY KEY AUTOINCREMENT,event_id TEXT NOT NULL UNIQUE,"
                        + "dedup_key TEXT NOT NULL UNIQUE,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,"
                        + "epoch_id TEXT NOT NULL,attempt_id TEXT NOT NULL,event_type TEXT NOT NULL,stage TEXT NOT NULL,"
                        + "state_rank INTEGER NOT NULL,payload_text TEXT NOT NULL DEFAULT '',payload_hash TEXT NOT NULL,"
                        + "source TEXT NOT NULL,created_at INTEGER NOT NULL)");
        addColumn(db, TABLE_EVENT, "payload_text TEXT NOT NULL DEFAULT ''");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r926_event_issue ON "
                        + TABLE_EVENT
                        + "(game_code,target_issue,seq)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r926_event_attempt ON "
                        + TABLE_EVENT
                        + "(attempt_id,seq)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_PROJECTION
                        + "(game_code TEXT NOT NULL,target_issue TEXT NOT NULL,current_state TEXT NOT NULL,state_rank INTEGER NOT NULL,"
                        + "epoch_id TEXT NOT NULL,attempt_id TEXT NOT NULL,last_event_id TEXT NOT NULL,event_watermark INTEGER NOT NULL,"
                        + "request_seen INTEGER NOT NULL DEFAULT 0,accepted_seen INTEGER NOT NULL DEFAULT 0,rejected_seen INTEGER NOT NULL DEFAULT 0,"
                        + "queued_seen INTEGER NOT NULL DEFAULT 0,started_seen INTEGER NOT NULL DEFAULT 0,frozen_seen INTEGER NOT NULL DEFAULT 0,"
                        + "canonical_seen INTEGER NOT NULL DEFAULT 0,settled_seen INTEGER NOT NULL DEFAULT 0,full_seen INTEGER NOT NULL DEFAULT 0,"
                        + "learned_seen INTEGER NOT NULL DEFAULT 0,recovery_seen INTEGER NOT NULL DEFAULT 0,last_error_code TEXT NOT NULL DEFAULT '',"
                        + "as_of INTEGER NOT NULL,PRIMARY KEY(game_code,target_issue))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_OUTBOX
                        + "(outbox_id TEXT PRIMARY KEY,dedup_key TEXT NOT NULL UNIQUE,target_issue TEXT NOT NULL,"
                        + "epoch_id TEXT NOT NULL,attempt_id TEXT NOT NULL,event_type TEXT NOT NULL,stage TEXT NOT NULL,state_rank INTEGER NOT NULL,"
                        + "payload_text TEXT NOT NULL DEFAULT '',payload_hash TEXT NOT NULL,source TEXT NOT NULL,status TEXT NOT NULL,"
                        + "attempts INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,last_error TEXT NOT NULL DEFAULT '')");
        addColumn(db, TABLE_OUTBOX, "epoch_id TEXT NOT NULL DEFAULT ''");
        addColumn(db, TABLE_OUTBOX, "attempt_id TEXT NOT NULL DEFAULT ''");
        addColumn(db, TABLE_OUTBOX, "payload_text TEXT NOT NULL DEFAULT ''");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r926_event_outbox ON "
                        + TABLE_OUTBOX
                        + "(status,created_at)");
        db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS trg_r926_runtime_event_no_update BEFORE UPDATE ON "
                        + TABLE_EVENT
                        + " BEGIN SELECT RAISE(ABORT,'R926_RUNTIME_EVENT_APPEND_ONLY'); END");
        db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS trg_r926_runtime_event_no_delete BEFORE DELETE ON "
                        + TABLE_EVENT
                        + " BEGIN SELECT RAISE(ABORT,'R926_RUNTIME_EVENT_APPEND_ONLY'); END");
    }

    /**
     * DML only. The caller owns the transaction. Returning false means the event already existed by
     * event_id/dedup_key; that is a successful idempotent replay, not an error.
     */
    public static boolean appendWithinTransaction(SQLiteDatabase db, Event event) {
        if (db == null || event == null || !event.valid()) return false;
        ContentValues cv = new ContentValues();
        cv.put("event_id", event.eventId());
        cv.put("dedup_key", event.dedupKey);
        cv.put("game_code", event.gameCode);
        cv.put("target_issue", event.targetIssue);
        cv.put("epoch_id", event.epochId);
        cv.put("attempt_id", event.attemptId);
        cv.put("event_type", event.type);
        cv.put("stage", event.stage);
        cv.put("state_rank", event.rank);
        cv.put("payload_text", event.payloadText);
        cv.put("payload_hash", event.payloadHash);
        cv.put("source", event.source);
        cv.put("created_at", event.createdAt);
        long row = db.insertWithOnConflict(TABLE_EVENT, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (row < 0L) return false;
        applyProjection(db, event, row, event.eventId());
        return true;
    }

    public static boolean append(DbStore store, final Event event) {
        if (store == null || event == null || !event.valid()) return false;
        final boolean[] inserted = new boolean[] {false};
        boolean ok =
                store.coordinatorR36()
                        .runWrite(
                                "R926_EVENT_" + event.type,
                                new DatabaseCoordinator.WriteRunnable() {
                                    @Override
                                    public void run(SQLiteDatabase db) {
                                        inserted[0] = appendWithinTransaction(db, event);
                                    }
                                });
        return ok && inserted[0];
    }

    /** External/non-transactional producer durable handoff. */
    public static void enqueueExternal(SQLiteDatabase db, Event event) {
        if (db == null || event == null || !event.valid()) return;
        String outboxId = V51ChecksumEngine.sha256("OUTBOX|" + event.eventId());
        ContentValues cv = new ContentValues();
        cv.put("outbox_id", outboxId);
        cv.put("dedup_key", event.dedupKey);
        cv.put("target_issue", event.targetIssue);
        cv.put("epoch_id", event.epochId);
        cv.put("attempt_id", event.attemptId);
        cv.put("event_type", event.type);
        cv.put("stage", event.stage);
        cv.put("state_rank", event.rank);
        cv.put("payload_text", event.payloadText);
        cv.put("payload_hash", event.payloadHash);
        cv.put("source", event.source);
        cv.put("status", "PENDING");
        cv.put("created_at", event.createdAt);
        cv.put("updated_at", event.createdAt);
        db.insertWithOnConflict(TABLE_OUTBOX, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /** Flushes durable external events into the immutable ledger using one local transaction each. */
    public static int drainOutbox(SQLiteDatabase db, int maxRows) {
        return drainOutboxR81(db, maxRows, null);
    }

    /** Exact external producer drain: no historical backlog work on the live owner. */
    static int drainOutboxForEventR81(SQLiteDatabase db, String dedupKey) {
        if (safe(dedupKey).isEmpty()) return 0;
        return drainOutboxR81(db, 1, safe(dedupKey));
    }

    private static int drainOutboxR81(SQLiteDatabase db, int maxRows, String dedupKey) {
        return drainOutboxR81(db, maxRows, dedupKey, 0L);
    }

    /** Called only by the existing background recovery lane under the shared writer. */
    static int drainOutboxRecoveryR81(SQLiteDatabase db, int maxRows, long deadlineMonoMs) {
        if (deadlineMonoMs <= 0L) return 0;
        return drainOutboxR81(db, Math.min(4, maxRows), null, deadlineMonoMs);
    }

    private static boolean recoveryYieldR81(long deadlineMonoMs) {
        return deadlineMonoMs > 0L && (Thread.currentThread().isInterrupted()
                || TimeSourceR811.monoNowMs() >= deadlineMonoMs
                || RealtimePressureGateR811.hasActiveCriticalWork());
    }

    private static int drainOutboxR81(SQLiteDatabase db, int maxRows, String dedupKey,
                                     long deadlineMonoMs) {
        if (db == null) return 0;
        if (recoveryYieldR81(deadlineMonoMs)) return 0;
        int limit = Math.max(1, Math.min(500, maxRows));
        List<Event> batch = new ArrayList<Event>();
        List<String> ids = new ArrayList<String>();
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT outbox_id,target_issue,epoch_id,attempt_id,event_type,stage,state_rank,payload_text,source,dedup_key,created_at"
                                    + " FROM "
                                    + TABLE_OUTBOX
                                    + (dedupKey == null
                                            ? (deadlineMonoMs > 0L
                                                ? " WHERE status='PENDING' ORDER BY updated_at,created_at,outbox_id LIMIT ?"
                                                : " WHERE status='PENDING' ORDER BY created_at LIMIT ?")
                                            : " WHERE status='PENDING' AND dedup_key=? LIMIT ?"),
                            dedupKey == null
                                    ? new String[] {String.valueOf(limit)}
                                    : new String[] {dedupKey, String.valueOf(limit)});
            while (c.moveToNext()) {
                ids.add(safe(c.getString(0)));
                batch.add(
                        new Event(
                                c.getString(1),
                                c.getString(2),
                                c.getString(3),
                                c.getString(4),
                                c.getString(5),
                                c.getInt(6),
                                c.getString(7),
                                c.getString(8),
                                c.getString(9),
                                c.getLong(10),
                                ""));
            }
        } finally {
            if (c != null) c.close();
        }
        int done = 0;
        for (int i = 0; i < batch.size(); i++) {
            // Finish an entered transaction atomically; yield before starting the next one.
            if (recoveryYieldR81(deadlineMonoMs)) break;
            Event e = batch.get(i);
            String id = ids.get(i);
            Throwable failure = null;
            db.beginTransaction();
            try {
                appendWithinTransaction(db, e);
                ContentValues cv = new ContentValues();
                cv.put("status", "COMPLETED");
                cv.put("updated_at", System.currentTimeMillis());
                cv.put("last_error", "");
                cv.put("attempts", scalarInt(db, "SELECT attempts+1 FROM " + TABLE_OUTBOX + " WHERE outbox_id=?", new String[] {id}, 1));
                db.update(TABLE_OUTBOX, cv, "outbox_id=?", new String[] {id});
                db.setTransactionSuccessful();
                done++;
            } catch (Throwable t) {
                failure = t;
            } finally {
                db.endTransaction();
            }
            // A failed transaction rolls back every write made inside it. Persist retry evidence only after rollback.
            if (failure != null) {
                ContentValues cv = new ContentValues();
                cv.put("status", "PENDING");
                cv.put("attempts", scalarInt(db, "SELECT attempts+1 FROM " + TABLE_OUTBOX + " WHERE outbox_id=?", new String[] {id}, 1));
                cv.put("updated_at", System.currentTimeMillis());
                cv.put("last_error", trim(String.valueOf(failure), 500));
                db.update(TABLE_OUTBOX, cv, "outbox_id=?", new String[] {id});
            }
        }
        return done;
    }

    public static int reconcile(SQLiteDatabase db, int maxIssues) {
        if (db == null) return 0;
        int limit = Math.max(1, Math.min(1000, maxIssues));
        Cursor c = null;
        int count = 0;
        try {
            c =
                    db.rawQuery(
                            "SELECT e.target_issue FROM (SELECT target_issue,MAX(seq) AS latest_seq FROM "
                                    + TABLE_EVENT
                                    + " WHERE game_code=? GROUP BY target_issue ORDER BY latest_seq DESC LIMIT ?) e"
                                    + " LEFT JOIN " + TABLE_PROJECTION
                                    + " p ON p.game_code=? AND p.target_issue=e.target_issue"
                                    + " WHERE p.event_watermark IS NULL OR p.event_watermark<>e.latest_seq",
                            new String[] {ReleaseManifestR926.GAME_CODE, String.valueOf(limit), ReleaseManifestR926.GAME_CODE});
            while (c.moveToNext()) {
                rebuildIssue(db, c.getString(0));
                count++;
            }
        } finally {
            if (c != null) c.close();
        }
        return count;
    }

    /**
     * One-time process startup reconciliation. Pre-R926 rows cannot be reconstructed into fictional
     * stage events, therefore only their existence is represented as LEGACY_UNKNOWN.
     */
    public static boolean startupReconciledR971() { return STARTUP_RECONCILED.get(); }

    public static int reconcileStartupOnce(SQLiteDatabase db, int maxIssues) {
        if (db == null || !STARTUP_RECONCILED.compareAndSet(false, true)) return 0;
        int limit = Math.max(1, Math.min(500, maxIssues));
        int marked = 0;
        try {
            reconcile(db, limit);
            Cursor c = null;
            try {
                c =
                        db.rawQuery(
                                "SELECT target_issue FROM "
                                        + V51Repository.ISSUE
                                        + " WHERE target_issue<>'' ORDER BY created_at DESC LIMIT ?",
                                new String[] {String.valueOf(limit)});
                while (c.moveToNext()) {
                    String issue = safe(c.getString(0));
                    if (issue.length() == 0 || projectionExists(db, issue)) continue;
                    markLegacyUnknown(db, issue);
                    marked++;
                }
            } finally {
                if (c != null) c.close();
            }
            drainOutbox(db, 100);
        } catch (Throwable failure) {
            // Allow another caller to retry startup reconciliation rather than permanently hiding failure.
            STARTUP_RECONCILED.set(false);
            throw failure;
        }
        return marked;
    }

    public static void markLegacyUnknown(SQLiteDatabase db, String issue) {
        if (safe(issue).length() == 0) return;
        Event e =
                Event.of(
                        issue,
                        "legacy",
                        "legacy",
                        "RECOVERY",
                        LEGACY_UNKNOWN,
                        RANK_REQUESTED,
                        "reason=PRE_R926_UNRECONSTRUCTABLE",
                        "R926_UPGRADE",
                        "LEGACY_UNKNOWN|" + issue);
        appendWithinTransaction(db, e);
    }

    public static Projection readProjection(SQLiteDatabase db, String issue) {
        if (db == null || safe(issue).length() == 0) return null;
        Cursor c = null;
        try {
            c = db.rawQuery(projectionSelect() + " WHERE game_code=? AND target_issue=?",
                    new String[] {ReleaseManifestR926.GAME_CODE, safe(issue)});
            return c.moveToFirst() ? new Projection(c) : null;
        } finally {
            if (c != null) c.close();
        }
    }

    public static List<Projection> recentProjections(SQLiteDatabase db, int limit) {
        ArrayList<Projection> out = new ArrayList<Projection>();
        if (db == null) return out;
        int capped = Math.max(1, Math.min(500, limit));
        Cursor c = null;
        try {
            c = db.rawQuery(
                    projectionSelect()
                            + " WHERE game_code=? ORDER BY target_issue DESC,event_watermark DESC LIMIT ?",
                    new String[] {ReleaseManifestR926.GAME_CODE, String.valueOf(capped)});
            while (c.moveToNext()) out.add(new Projection(c));
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    public static String exportText(SQLiteDatabase db, int limit) {
        StringBuilder b = new StringBuilder(8192);
        b.append("target_issue|current_state|state_rank|epoch_id|attempt_id|event_watermark|as_of|last_error_code|request|accepted|rejected|queued|started|frozen|canonical|settled|full|learned|recovery\n");
        List<Projection> rows = recentProjections(db, limit);
        for (Projection p : rows) {
            text(b, p.targetIssue); b.append('|');
            text(b, p.currentState); b.append('|').append(p.stateRank).append('|');
            text(b, p.epochId); b.append('|');
            text(b, p.attemptId); b.append('|').append(p.eventWatermark).append('|').append(p.asOf).append('|');
            text(b, p.lastErrorCode); b.append('|').append(p.requestSeen).append('|').append(p.acceptedSeen).append('|')
                    .append(p.rejectedSeen).append('|').append(p.queuedSeen).append('|').append(p.startedSeen).append('|')
                    .append(p.frozenSeen).append('|').append(p.canonicalSeen).append('|').append(p.settledSeen).append('|')
                    .append(p.fullSeen).append('|').append(p.learnedSeen).append('|').append(p.recoverySeen).append('\n');
        }
        return b.toString();
    }

    public static String exportCsv(SQLiteDatabase db, int limit) {
        StringBuilder b = new StringBuilder(8192);
        b.append("target_issue,current_state,state_rank,epoch_id,attempt_id,event_watermark,as_of,last_error_code,request,accepted,rejected,queued,started,frozen,canonical,settled,full,learned,recovery\n");
        List<Projection> rows = recentProjections(db, limit);
        for (Projection p : rows) {
            csv(b, p.targetIssue); b.append(',');
            csv(b, p.currentState); b.append(',').append(p.stateRank).append(',');
            csv(b, p.epochId); b.append(',');
            csv(b, p.attemptId); b.append(',').append(p.eventWatermark).append(',').append(p.asOf).append(',');
            csv(b, p.lastErrorCode); b.append(',').append(p.requestSeen).append(',').append(p.acceptedSeen).append(',')
                    .append(p.rejectedSeen).append(',').append(p.queuedSeen).append(',').append(p.startedSeen).append(',')
                    .append(p.frozenSeen).append(',').append(p.canonicalSeen).append(',').append(p.settledSeen).append(',')
                    .append(p.fullSeen).append(',').append(p.learnedSeen).append(',').append(p.recoverySeen).append('\n');
        }
        return b.toString();
    }

    private static String projectionSelect() {
        return "SELECT target_issue,current_state,state_rank,epoch_id,attempt_id,last_event_id,event_watermark,as_of,last_error_code,"
                + "request_seen,accepted_seen,rejected_seen,queued_seen,started_seen,frozen_seen,canonical_seen,settled_seen,full_seen,learned_seen,recovery_seen"
                + " FROM " + TABLE_PROJECTION;
    }

    private static boolean projectionExists(SQLiteDatabase db, String issue) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT 1 FROM " + TABLE_PROJECTION + " WHERE game_code=? AND target_issue=? LIMIT 1",
                    new String[] {ReleaseManifestR926.GAME_CODE, safe(issue)});
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    private static void rebuildIssue(SQLiteDatabase db, String issue) {
        db.delete(
                TABLE_PROJECTION,
                "game_code=? AND target_issue=?",
                new String[] {ReleaseManifestR926.GAME_CODE, safe(issue)});
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT event_id,epoch_id,attempt_id,event_type,stage,state_rank,payload_text,source,dedup_key,created_at,seq"
                                    + " FROM "
                                    + TABLE_EVENT
                                    + " WHERE game_code=? AND target_issue=? ORDER BY seq",
                            new String[] {ReleaseManifestR926.GAME_CODE, safe(issue)});
            while (c.moveToNext()) {
                Event e =
                        Event.replay(
                                c.getString(0),
                                issue,
                                c.getString(1),
                                c.getString(2),
                                c.getString(3),
                                c.getString(4),
                                c.getInt(5),
                                c.getString(6),
                                c.getString(7),
                                c.getString(8),
                                c.getLong(9));
                applyProjection(db, e, c.getLong(10), c.getString(0));
            }
        } finally {
            if (c != null) c.close();
        }
    }

    private static void applyProjection(SQLiteDatabase db, Event e, long watermark, String originalEventId) {
        Cursor c = null;
        int existingRank = -1;
        long existingWatermark = 0L;
        long existingAsOf = 0L;
        String currentState = LEGACY_UNKNOWN;
        String currentEpoch = "";
        String currentAttempt = "";
        String lastError = "";
        int request = 0, accepted = 0, rejected = 0, queued = 0, started = 0, frozen = 0,
                canonical = 0, settled = 0, full = 0, learned = 0, recovery = 0;
        try {
            c =
                    db.rawQuery(
                            "SELECT current_state,state_rank,epoch_id,attempt_id,event_watermark,as_of,last_error_code,"
                                    + "request_seen,accepted_seen,rejected_seen,queued_seen,started_seen,frozen_seen,canonical_seen,settled_seen,full_seen,learned_seen,recovery_seen"
                                    + " FROM "
                                    + TABLE_PROJECTION
                                    + " WHERE game_code=? AND target_issue=?",
                            new String[] {e.gameCode, e.targetIssue});
            if (c.moveToFirst()) {
                currentState = safe(c.getString(0));
                existingRank = c.getInt(1);
                currentEpoch = safe(c.getString(2));
                currentAttempt = safe(c.getString(3));
                existingWatermark = c.getLong(4);
                existingAsOf = c.getLong(5);
                lastError = safe(c.getString(6));
                request = c.getInt(7); accepted = c.getInt(8); rejected = c.getInt(9);
                queued = c.getInt(10); started = c.getInt(11); frozen = c.getInt(12);
                canonical = c.getInt(13); settled = c.getInt(14); full = c.getInt(15);
                learned = c.getInt(16); recovery = c.getInt(17);
            }
        } finally {
            if (c != null) c.close();
        }
        boolean advances = e.rank >= existingRank;
        ContentValues cv = new ContentValues();
        cv.put("game_code", e.gameCode);
        cv.put("target_issue", e.targetIssue);
        cv.put("current_state", advances ? e.stage : currentState);
        cv.put("state_rank", Math.max(existingRank, e.rank));
        cv.put("epoch_id", advances && e.epochId.length() > 0 ? e.epochId : currentEpoch);
        cv.put("attempt_id", advances && e.attemptId.length() > 0 ? e.attemptId : currentAttempt);
        cv.put("last_event_id", safe(originalEventId));
        cv.put("event_watermark", Math.max(existingWatermark, watermark));
        cv.put("request_seen", request == 1 || "REQUESTED".equals(e.type) ? 1 : 0);
        cv.put("accepted_seen", accepted == 1 || "ACCEPTED".equals(e.type) ? 1 : 0);
        cv.put("rejected_seen", rejected == 1 || "REJECTED".equals(e.type) ? 1 : 0);
        cv.put("queued_seen", queued == 1 || "QUEUED".equals(e.type) ? 1 : 0);
        cv.put("started_seen", started == 1 || "STARTED".equals(e.type) ? 1 : 0);
        cv.put("frozen_seen", frozen == 1 || "FROZEN".equals(e.type) ? 1 : 0);
        cv.put("canonical_seen", canonical == 1 || "CANONICAL".equals(e.type) ? 1 : 0);
        cv.put("settled_seen", settled == 1 || "SETTLED".equals(e.type) ? 1 : 0);
        cv.put("full_seen", full == 1 || "FULL".equals(e.type) ? 1 : 0);
        cv.put("learned_seen", learned == 1 || "LEARNED".equals(e.type) ? 1 : 0);
        cv.put("recovery_seen", recovery == 1 || "RECOVERY".equals(e.type) ? 1 : 0);
        cv.put("last_error_code", "REJECTED".equals(e.type) ? e.stage : lastError);
        // Replay must be deterministic: projection timestamps come only from durable event time.
        cv.put("as_of", Math.max(existingAsOf, e.createdAt));
        db.insertWithOnConflict(TABLE_PROJECTION, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static int scalarInt(SQLiteDatabase db, String sql, String[] args, int fallback) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            return c.moveToFirst() ? c.getInt(0) : fallback;
        } catch (Throwable ignored) {
            return fallback;
        } finally {
            if (c != null) c.close();
        }
    }

    private static void addColumn(SQLiteDatabase db, String table, String definition) {
        String name = definition.substring(0, definition.indexOf(' ')).trim();
        Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            while (c.moveToNext()) if (name.equalsIgnoreCase(safe(c.getString(1)))) return;
        } finally {
            if (c != null) c.close();
        }
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + definition);
    }

    private static void text(StringBuilder b, String value) {
        String s = safe(value);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\n' || ch == '\r' || ch == '|') b.append('/'); else b.append(ch);
        }
    }

    private static void csv(StringBuilder b, String value) {
        String s = safe(value);
        boolean quote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        if (!quote) { b.append(s); return; }
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"') b.append("\"\""); else b.append(ch);
        }
        b.append('"');
    }

    private static String normalizePayload(String s) {
        String x = s == null ? "" : s.replace('\n', ' ').replace('\r', ' ').trim();
        return trim(x, 4000);
    }

    private static String trim(String s, int max) {
        String x = s == null ? "" : s;
        return x.length() <= max ? x : x.substring(0, max);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

public static String exportTextSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, int limit) {
        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report, 8192);
        b.append("target_issue|current_state|state_rank|epoch_id|attempt_id|event_watermark|as_of|last_error_code|request|accepted|rejected|queued|started|frozen|canonical|settled|full|learned|recovery\n");
        List<Projection> rows = recentProjectionsSnapshotCP4(report, db, limit);
        for (Projection p : rows) {
            textSnapshotCP4(report, b, p.targetIssue); b.append('|');
            textSnapshotCP4(report, b, p.currentState); b.append('|').append(p.stateRank).append('|');
            textSnapshotCP4(report, b, p.epochId); b.append('|');
            textSnapshotCP4(report, b, p.attemptId); b.append('|').append(p.eventWatermark).append('|').append(p.asOf).append('|');
            textSnapshotCP4(report, b, p.lastErrorCode); b.append('|').append(p.requestSeen).append('|').append(p.acceptedSeen).append('|')
                    .append(p.rejectedSeen).append('|').append(p.queuedSeen).append('|').append(p.startedSeen).append('|')
                    .append(p.frozenSeen).append('|').append(p.canonicalSeen).append('|').append(p.settledSeen).append('|')
                    .append(p.fullSeen).append('|').append(p.learnedSeen).append('|').append(p.recoverySeen).append('\n');
        }
        return b.toString();
    }

public static void textSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 b, String value) {
        String s = safeSnapshotCP4(report, value);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\n' || ch == '\r' || ch == '|') b.append('/'); else b.append(ch);
        }
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String s) {
        return s == null ? "" : s.trim();
    }

public static List<Projection> recentProjectionsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, int limit) {
        ArrayList<Projection> out = new ArrayList<Projection>();
        if (db == null) return out;
        int capped = Math.max(1, Math.min(500, limit));
        Cursor c = null;
        try {
            c = report.select(
                    projectionSelectSnapshotCP4(report)
                            + " WHERE game_code=? ORDER BY target_issue DESC,event_watermark DESC LIMIT ?",
                    new String[] {ReleaseManifestR926.GAME_CODE, String.valueOf(capped)});
            while (c.moveToNext()) out.add(new Projection(c));
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

public static String projectionSelectSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report) {
        return "SELECT target_issue,current_state,state_rank,epoch_id,attempt_id,last_event_id,event_watermark,as_of,last_error_code,"
                + "request_seen,accepted_seen,rejected_seen,queued_seen,started_seen,frozen_seen,canonical_seen,settled_seen,full_seen,learned_seen,recovery_seen"
                + " FROM " + TABLE_PROJECTION;
    }

public static String exportCsvSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, int limit) {
        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report, 8192);
        b.append("target_issue,current_state,state_rank,epoch_id,attempt_id,event_watermark,as_of,last_error_code,request,accepted,rejected,queued,started,frozen,canonical,settled,full,learned,recovery\n");
        List<Projection> rows = recentProjectionsSnapshotCP4(report, db, limit);
        for (Projection p : rows) {
            csvSnapshotCP4(report, b, p.targetIssue); b.append(',');
            csvSnapshotCP4(report, b, p.currentState); b.append(',').append(p.stateRank).append(',');
            csvSnapshotCP4(report, b, p.epochId); b.append(',');
            csvSnapshotCP4(report, b, p.attemptId); b.append(',').append(p.eventWatermark).append(',').append(p.asOf).append(',');
            csvSnapshotCP4(report, b, p.lastErrorCode); b.append(',').append(p.requestSeen).append(',').append(p.acceptedSeen).append(',')
                    .append(p.rejectedSeen).append(',').append(p.queuedSeen).append(',').append(p.startedSeen).append(',')
                    .append(p.frozenSeen).append(',').append(p.canonicalSeen).append(',').append(p.settledSeen).append(',')
                    .append(p.fullSeen).append(',').append(p.learnedSeen).append(',').append(p.recoverySeen).append('\n');
        }
        return b.toString();
    }

public static void csvSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 b, String value) {
        String s = safeSnapshotCP4(report, value);
        boolean quote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        if (!quote) { b.append(s); return; }
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"') b.append("\"\""); else b.append(ch);
        }
        b.append('"');
    }
}
