package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Durable draw delivery. Realtime append and research consume deliberately have different transaction boundaries.
 * An inbox acknowledgement means DELIVERED, never trained. Only an actual model/settlement commit creates a work
 * receipt. ResearchRouteR29 selects the independent writer after verified R29-004 publication.
 * No callback holds a realtime transaction while waiting for the research writer. */
public final class ResearchEventBridgeR29 {
    private ResearchEventBridgeR29() {}
    public static final int PAGE = 32;
    public static final String MODEL = "ATOMNET_ONLINE", SETTLEMENT = "SHADOW_SETTLEMENT";
    public static final String OUTBOX = "CREATE TABLE IF NOT EXISTS r29_event_outbox(seq INTEGER PRIMARY KEY AUTOINCREMENT,event_id TEXT NOT NULL UNIQUE,issue TEXT NOT NULL,kind TEXT NOT NULL,payload_json TEXT NOT NULL,payload_hash TEXT NOT NULL,origin TEXT NOT NULL,committed_at INTEGER NOT NULL,acked_at INTEGER NOT NULL DEFAULT 0)";
    public static final String[] RESEARCH_DDL = {
        "CREATE TABLE IF NOT EXISTS r29_event_inbox(event_id TEXT PRIMARY KEY,source_seq INTEGER NOT NULL,issue TEXT NOT NULL,payload_hash TEXT NOT NULL,origin TEXT NOT NULL,applied_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r29_event_cursor(source TEXT PRIMARY KEY,last_seq INTEGER NOT NULL,updated_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r29_learning_work(consumer TEXT NOT NULL,event_id TEXT NOT NULL,issue TEXT NOT NULL,target_ordinal INTEGER NOT NULL,status TEXT NOT NULL CHECK(status IN ('PENDING','COMMITTED')),enqueued_at INTEGER NOT NULL,completed_at INTEGER NOT NULL DEFAULT 0,state_hash TEXT,PRIMARY KEY(consumer,event_id))",
        "CREATE INDEX IF NOT EXISTS r29_learning_work_pending ON r29_learning_work(consumer,status,issue)",
        "CREATE TABLE IF NOT EXISTS r29_learning_receipt(consumer TEXT NOT NULL,issue TEXT NOT NULL,state_hash TEXT NOT NULL,units INTEGER NOT NULL CHECK(units>0),completed_at INTEGER NOT NULL,PRIMARY KEY(consumer,issue))",
        "CREATE TRIGGER IF NOT EXISTS r29_learning_receipt_no_update BEFORE UPDATE ON r29_learning_receipt BEGIN SELECT RAISE(ABORT,'R29_LEARNING_RECEIPT_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r29_learning_receipt_no_delete BEFORE DELETE ON r29_learning_receipt BEGIN SELECT RAISE(ABORT,'R29_LEARNING_RECEIPT_IMMUTABLE'); END"
    };

    public static void ensureRealtime(SQLiteDatabase db) {
        requireTransaction(db);
        db.execSQL(OUTBOX);
        db.execSQL(ExperimentRegistrationR29.AUTHORITY);
        for(String guard:ExperimentRegistrationR29.AUTHORITY_GUARDS)db.execSQL(guard);
        db.execSQL("CREATE INDEX IF NOT EXISTS r29_event_outbox_pending ON r29_event_outbox(acked_at,seq)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r29_event_payload_immutable BEFORE UPDATE OF event_id,issue,kind,payload_json,payload_hash,origin,committed_at ON r29_event_outbox BEGIN SELECT RAISE(ABORT,'R29_EVENT_IMMUTABLE'); END");
    }

    /** Called after canonical acceptance in the SAME draw transaction. Identity excludes repairable provider time.
     * Old rows observed after installation are labelled explicitly and are not future-confirmation evidence. */
    public static void appendCanonical(SQLiteDatabase db, String issue, boolean newlyInserted, long now) {
        requireTransaction(db);
        ensureRealtime(db);
        int[] numbers = new int[10];
        try (Cursor c = db.rawQuery("SELECT n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws WHERE issue=? AND dirty=0", new String[] {issue})) {
            if (!c.moveToFirst()) return; // dirty/conflicting rows never produce accepted learning events
            for (int i = 0; i < 10; i++) numbers[i] = c.getInt(i);
        }
        Event e = Event.draw(0, issue, numbers, newlyInserted ? "NEW_CANONICAL_COMMIT" : "EXISTING_CANONICAL_OBSERVED", now);
        append(db,e);
    }

    static void append(SQLiteDatabase db,Event e) {
        requireTransaction(db);e.verify();
        db.execSQL("INSERT OR IGNORE INTO r29_event_outbox(event_id,issue,kind,payload_json,payload_hash,origin,committed_at) VALUES(?,?,?,?,?,?,?)",
                new Object[] {e.id, e.issue, e.kind, e.payload, e.hash, e.origin, e.committedAt});
        // An existing identity must describe exactly the same immutable payload; an OR IGNORE alone hides conflicts.
        try (Cursor c = db.rawQuery("SELECT payload_hash,payload_json FROM r29_event_outbox WHERE event_id=?", new String[] {e.id})) {
            if (!c.moveToFirst() || !e.hash.equals(c.getString(0)) || !e.payload.equals(c.getString(1)))
                throw new IllegalStateException("R29_OUTBOX_IDENTITY_CONFLICT");
        }
    }

    public static final class Event {
        public final long seq, committedAt;
        public final String id, issue, payload, hash, origin,kind;
        Event(long seq, String id, String issue, String payload, String hash, String origin, long committedAt) {
            this(seq,id,issue,payload,hash,origin,committedAt,"DRAW_COMMITTED");
        }
        Event(long seq, String id, String issue, String payload, String hash, String origin, long committedAt,String kind) {
            this.seq = seq; this.id = id; this.issue = issue; this.payload = payload; this.hash = hash;
            this.origin = origin; this.committedAt = committedAt;this.kind=kind;
            verify();
        }
        public static Event draw(long seq, String issue, int[] numbers, String origin, long now) {
            if (!TargetIssueResolver.isValid(issue) || numbers == null || !ReplayDatasetR27.legal(numbers))
                throw new IllegalArgumentException("R29_EVENT_DRAW_INVALID");
            Map<String,Object> p = new LinkedHashMap<>();
            p.put("abi", "R29_CANONICAL_DRAW_V1"); p.put("issue", issue);
            List<Object> values = new ArrayList<>(); for (int n : numbers) values.add(n);
            p.put("numbers", values);
            String payload = JsonR27.write(p), hash = StatsR27.sha256(payload);
            return new Event(seq, StatsR27.sha256("DRAW_COMMITTED|" + issue + "|" + hash), issue, payload, hash, origin, now);
        }
        @SuppressWarnings("unchecked")
        void verify() {
            if (seq < 0 || committedAt < 0 || !LegacyResearchTransferR29.supports(kind)&&!TargetIssueResolver.isValid(issue) || !StatsR27.sha256(payload).equals(hash)
                    || !StatsR27.sha256(kind+"|" + issue + "|" + hash).equals(id))
                throw new IllegalArgumentException("R29_EVENT_CHECKSUM_OR_IDENTITY");
            Object parsed = JsonR27.parse(payload);
            if (!(parsed instanceof Map)) throw new IllegalArgumentException("R29_EVENT_PAYLOAD");
            Map<String,Object> p = (Map<String,Object>) parsed;
            if(LegacyResearchTransferR29.supports(kind)){LegacyResearchTransferR29.validate(kind,p);return;}
            if(ExperimentRegistrationR29.KIND.equals(kind)){ExperimentRegistrationR29.validate(issue,p);return;}
            if(!"DRAW_COMMITTED".equals(kind))throw new IllegalArgumentException("R29_EVENT_KIND");
            if (!"R29_CANONICAL_DRAW_V1".equals(p.get("abi")) || !issue.equals(p.get("issue")) || !(p.get("numbers") instanceof List))
                throw new IllegalArgumentException("R29_EVENT_PAYLOAD");
            List<?> values = (List<?>) p.get("numbers"); int[] numbers = new int[values.size()];
            for (int i = 0; i < numbers.length; i++) {
                Object v = values.get(i);
                if (!(v instanceof Number) || ((Number)v).doubleValue() != ((Number)v).intValue())
                    throw new IllegalArgumentException("R29_EVENT_NUMBER");
                numbers[i] = ((Number)v).intValue();
            }
            if (!ReplayDatasetR27.legal(numbers)) throw new IllegalArgumentException("R29_EVENT_PERMUTATION");
        }
    }

    /** Read-only realtime connection; materialize and CLOSE every cursor before acquiring the research writer. */
    public static List<Event> pending(SQLiteDatabase realtime) {
        List<Event> out = new ArrayList<>();
        try (Cursor c = realtime.rawQuery("SELECT seq,event_id,issue,payload_json,payload_hash,origin,committed_at,kind FROM r29_event_outbox WHERE acked_at=0 ORDER BY seq LIMIT " + PAGE, null)) {
            while (c.moveToNext()) out.add(new Event(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getLong(6),c.getString(7)));
        }
        return out;
    }

    /** Pure batch preparation; no realtime read/write and no claim of training completion. */
    public static ResearchWriteQueueR27.Batch prepare(List<Event> events, long now) {
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
        if (events.isEmpty()) return b;
        for (Event e : events) e.verify();
        b.before = db -> {
            java.util.Set<String> handled=new java.util.HashSet<>();
            for (Event e : events) {
                if(!handled.add(e.id))continue;
                boolean exists=false;
                try (Cursor c = db.rawQuery("SELECT payload_hash FROM r29_event_inbox WHERE event_id=?", new String[] {e.id})) {
                    exists=c.moveToFirst();if (exists && !e.hash.equals(c.getString(0))) throw new IllegalStateException("R29_INBOX_IDENTITY_CONFLICT");
                }
                if(!exists&&ExperimentRegistrationR29.KIND.equals(e.kind))ExperimentRegistrationR29.consume(db,e,now);
                if(!exists&&LegacyResearchTransferR29.supports(e.kind))LegacyResearchTransferR29.consume(db,e);
            }
            return 0;
        };
        long highest = 0;
        for (Event e : events) {
            highest = Math.max(highest, e.seq);
            b.add("INSERT OR IGNORE INTO r29_event_inbox(event_id,source_seq,issue,payload_hash,origin,applied_at) VALUES(?,?,?,?,?,?)", e.id, e.seq, e.issue, e.hash, e.origin, now);
            if(!"DRAW_COMMITTED".equals(e.kind))continue;
            for (String consumer : new String[] {MODEL, SETTLEMENT}) {
                b.add("INSERT OR IGNORE INTO r29_learning_work(consumer,event_id,issue,target_ordinal,status,enqueued_at) VALUES(?,?,?,?,'PENDING',?)", consumer, e.id, e.issue, TargetIssueResolver.sequenceOrdinalSU1(e.issue), now);
                // The learner can commit before this event is delivered. Its durable receipt is authoritative.
                b.add("UPDATE r29_learning_work SET status='COMMITTED',completed_at=(SELECT completed_at FROM r29_learning_receipt WHERE consumer=? AND issue=?),state_hash=(SELECT state_hash FROM r29_learning_receipt WHERE consumer=? AND issue=?) WHERE consumer=? AND event_id=? AND status='PENDING' AND EXISTS(SELECT 1 FROM r29_learning_receipt WHERE consumer=? AND issue=?)",
                        consumer, e.issue, consumer, e.issue, consumer, e.id, consumer, e.issue);
            }
        }
        b.add("INSERT OR IGNORE INTO r29_event_cursor(source,last_seq,updated_at) VALUES('REALTIME',0,?)", now);
        b.add("UPDATE r29_event_cursor SET last_seq=max(last_seq,?),updated_at=? WHERE source='REALTIME'", highest, now);
        return b;
    }

    /** Only call AFTER the research transaction returned successfully. Repeated/lost acknowledgement is harmless. */
    public static void acknowledge(SQLiteDatabase realtime, List<Event> events, long now) {
        requireTransaction(realtime);
        for (Event e : events) realtime.execSQL("UPDATE r29_event_outbox SET acked_at=CASE WHEN acked_at=0 THEN ? ELSE acked_at END WHERE event_id=? AND payload_hash=?",
                new Object[] {now, e.id, e.hash});
    }

    /** Append to the very batch that commits the actual model state/cursor. Replay/import is never an online step. */
    public static void modelCommitted(ResearchWriteQueueR27.Batch b, String issue, String stateHash, long units, long now) {
        if (!TargetIssueResolver.isValid(issue) || stateHash == null || !stateHash.matches("[0-9a-f]{64}") || units <= 0)
            throw new IllegalArgumentException("R29_LEARNING_RECEIPT_REQUIRED");
        receipt(b, MODEL, issue, stateHash, units, now);
        final DatabaseCoordinator.WriteCallable<Integer> previous = b.after;
        b.after = db -> {
            if (previous != null) { Integer ok = previous.call(db); if (ok != null && ok < 0) return ok; }
            try (Cursor c = db.rawQuery("SELECT state_hash,units FROM r29_learning_receipt WHERE consumer='ATOMNET_ONLINE' AND issue=?", new String[] {issue})) {
                if (!c.moveToFirst() || !stateHash.equals(c.getString(0)) || units != c.getLong(1))
                    throw new IllegalStateException("R29_LEARNING_RECEIPT_CONFLICT");
            }
            try (Cursor c = db.rawQuery("SELECT state_hash,trained_through,artifact FROM r29_model_checkpoint WHERE owner='ATOMNET_FTRL'", null)) {
                if (!c.moveToFirst() || !stateHash.equals(c.getString(0)) || !issue.equals(c.getString(1)) || !stateHash.equals(StatsR27.sha256(c.getBlob(2))))
                    throw new IllegalStateException("R29_LEARNING_RECEIPT_WITHOUT_MATCHING_MODEL_COMMIT");
            }
            return 0;
        };
    }

    static void receipt(ResearchWriteQueueR27.Batch b, String consumer, String issue, String hash, long units, long now) {
        b.add("INSERT OR IGNORE INTO r29_learning_receipt(consumer,issue,state_hash,units,completed_at) VALUES(?,?,?,?,?)", consumer, issue, hash, units, now);
        // Preserve the first successful receipt rather than changing evidence on a retry.
        b.add("UPDATE r29_learning_work SET status='COMMITTED',completed_at=(SELECT completed_at FROM r29_learning_receipt WHERE consumer=? AND issue=?),state_hash=(SELECT state_hash FROM r29_learning_receipt WHERE consumer=? AND issue=?) WHERE consumer=? AND issue=? AND status='PENDING'",
                consumer, issue, consumer, issue, consumer, issue);
    }

    public static ResearchPoolR27.Outcome deliver(ResearchPoolR27.Worker w) throws Exception {
        ExperimentRegistrationR29.publishRequests(w);
        List<Event> events = pending(w.realtimeRo());
        if (events.isEmpty()) {
            ResearchWriteQueueR27.Batch completed = reconcileSettlements(w.ro, System.currentTimeMillis());
            if (!completed.isEmpty()) { RuntimeR27.submit(w, "EVENT_SETTLEMENT_RECEIPTS", completed); return ResearchPoolR27.Outcome.progress(1, 0); }
            return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT:outbox drained; pending work requires actual learner/settlement commit", 0, -1);
        }
        ResearchWriteQueueR27.Batch batch = prepare(events, System.currentTimeMillis());
        RuntimeR27.submit(w, "EVENT_INBOX", batch); // returns with research transaction finished
        w.store().coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_EVENT_ACK", db -> {
            acknowledge(db, events, System.currentTimeMillis()); return 0;
        });
        return ResearchPoolR27.Outcome.progress(events.size(), events.get(events.size() - 1).seq);
    }

    /** Reconcile an already committed settlement if it preceded delivery. Never infer it from an enqueue counter. */
    static ResearchWriteQueueR27.Batch reconcileSettlements(SQLiteDatabase research, long now) {
        List<String> issues = new ArrayList<>();
        try (Cursor c = research.rawQuery("SELECT DISTINCT w.issue FROM r29_learning_work w WHERE w.consumer='SHADOW_SETTLEMENT' AND w.status='PENDING'"
                + " AND EXISTS(SELECT 1 FROM r27_live_frozen f WHERE f.target_ordinal=w.target_ordinal AND f.target_issue=w.issue AND f.status='FROZEN' AND f.hit IN(0,1))"
                + " AND NOT EXISTS(SELECT 1 FROM r27_live_frozen f WHERE f.target_ordinal=w.target_ordinal AND f.target_issue=w.issue AND f.hit NOT IN(0,1)) ORDER BY w.issue LIMIT " + PAGE, null)) {
            while (c.moveToNext()) issues.add(c.getString(0));
        }
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
        if (!issues.isEmpty()) b.after = db -> { settlementsCommitted(db, issues, now); return 0; };
        return b;
    }

    /** In the settlement transaction, after all outcome rows. Every legal freeze must already have an outcome.
     * A period with no legal pre-draw freeze remains a visible gap and never earns a settlement receipt. */
    static void settlementsCommitted(SQLiteDatabase db, Iterable<String> issues, long now) {
        requireTransaction(db);
        for (String issue : issues) {
            if (!TargetIssueResolver.isValid(issue)) continue;
            StringBuilder state = new StringBuilder("R29_SHADOW_SETTLEMENT_V1|").append(issue);
            boolean draw = false;
            try (Cursor c = ResearchRouteR29.realtimeForWriter(db).rawQuery("SELECT n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws WHERE issue=? AND dirty=0", new String[] {issue})) {
                if (c.moveToFirst()) { int[] numbers=new int[10];for(int i=0;i<10;i++)numbers[i]=c.getInt(i);draw=ReplayDatasetR27.legal(numbers);for(int n:numbers)state.append('|').append(n); }
            }
            if (!draw) continue;
            boolean complete = true; long legal = 0;
            String[] keys = {String.valueOf(TargetIssueResolver.sequenceOrdinalSU1(issue)),issue};
            try (Cursor c = db.rawQuery("SELECT policy_hash,hit,status,frozen_at,deadline_at FROM r27_live_frozen WHERE target_ordinal=? AND target_issue=? ORDER BY policy_hash", keys)) {
                while (c.moveToNext()) {
                    int hit = c.getInt(1);
                    if (hit < 0 || hit > 1) { complete = false; break; }
                    if ("FROZEN".equals(c.getString(2)) && c.getLong(3) <= c.getLong(4) - LiveShadowsR27.FREEZE_MARGIN_MS) {
                        legal++; state.append('|').append(c.getString(0)).append(':').append(hit).append(':').append(c.getLong(3)).append(':').append(c.getLong(4));
                    }
                }
            }
            if (complete && legal > 0) {
                ResearchWriteQueueR27.Batch receipt = new ResearchWriteQueueR27.Batch();
                receipt(receipt, SETTLEMENT, issue, StatsR27.sha256(state.toString()), legal, now);
                ResearchWriteQueueR27.exec(db, receipt);
            }
        }
    }

    public static Map<String,Object> diagnostics(SQLiteDatabase research) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("meaning", "DELIVERED is not TRAINED; work remains PENDING without a real commit receipt");
        m.put("databaseIsolation",ResearchRouteR29.isResearchDatabase(research)?"ACTIVE_R27_RESEARCH_FILE_LEGACY_R26_ADAPTER_PENDING":"ORIGINAL_OWNER_MIGRATION_PENDING");
        m.put("pendingRealtimeRegistrations",QualificationInboxR29.count(research,"SELECT count(*) FROM r29_registration_request WHERE acked_at=0"));
        m.put("eventsDelivered", QualificationInboxR29.count(research, "SELECT count(*) FROM r29_event_inbox"));
        m.put("modelPending", QualificationInboxR29.count(research, "SELECT count(*) FROM r29_learning_work WHERE consumer='ATOMNET_ONLINE' AND status='PENDING'"));
        m.put("modelCommitted", QualificationInboxR29.count(research, "SELECT count(*) FROM r29_learning_receipt WHERE consumer='ATOMNET_ONLINE'"));
        m.put("settlementPending", QualificationInboxR29.count(research, "SELECT count(*) FROM r29_learning_work WHERE consumer='SHADOW_SETTLEMENT' AND status='PENDING'"));
        m.put("settlementCommitted", QualificationInboxR29.count(research, "SELECT count(*) FROM r29_learning_receipt WHERE consumer='SHADOW_SETTLEMENT'"));
        m.put("noPredrawFreeze", QualificationInboxR29.count(research, "SELECT count(*) FROM r29_learning_work w WHERE w.consumer='SHADOW_SETTLEMENT' AND w.status='PENDING' AND NOT EXISTS(SELECT 1 FROM r27_live_frozen f WHERE f.target_ordinal=w.target_ordinal AND f.target_issue=w.issue AND f.status='FROZEN' AND f.frozen_at<=f.deadline_at-5000)"));
        Map<String,Object> reasons = new LinkedHashMap<>();
        try (Cursor c = research.rawQuery("SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM r29_model_checkpoint WHERE owner='ATOMNET_FTRL') THEN 'MODEL_NOT_INITIALIZED'"
                + " WHEN w.issue<=(SELECT trained_through FROM r29_model_checkpoint WHERE owner='ATOMNET_FTRL') THEN 'CURSOR_AHEAD_WITHOUT_THIS_EVENT_RECEIPT'"
                + " ELSE 'WAITING_REAL_ONLINE_COMMIT' END,count(*) FROM r29_learning_work w WHERE w.consumer='ATOMNET_ONLINE' AND w.status='PENDING' GROUP BY 1", null)) {
            while (c.moveToNext()) reasons.put(c.getString(0),c.getLong(1));
            m.put("modelPendingReasons",reasons);
        } catch (RuntimeException missing) { m.put("modelPendingReasons",null); }
        return m;
    }

    private static void requireTransaction(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R29_EVENT_TRANSACTION_REQUIRED");
    }
}
