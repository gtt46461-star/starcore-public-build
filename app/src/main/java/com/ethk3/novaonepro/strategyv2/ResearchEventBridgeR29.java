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
    public static final String TERMINAL_FEEDBACK = "R26_FEEDBACK_COMMITTED";
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

    private static final class RealtimeObject {
        final String type,name,table,sql,canonical;
        RealtimeObject(String type,String name,String table,String sql){this.type=type;this.name=name;this.table=table;this.sql=sql;canonical=canonicalDdl(sql);}
    }
    private static final RealtimeObject FETCH_OBSERVATION=new RealtimeObject("table","r29_fetch_observation","r29_fetch_observation",com.ethk3.novaonepro.FetchObservationR29.DDL);
    private static final RealtimeObject[] REALTIME_OBJECTS={
        new RealtimeObject("table","r29_event_outbox","r29_event_outbox",OUTBOX),FETCH_OBSERVATION,
        new RealtimeObject("table","r29_registration_authority","r29_registration_authority",ExperimentRegistrationR29.AUTHORITY),
        new RealtimeObject("trigger","r29_registration_authority_no_update","r29_registration_authority",ExperimentRegistrationR29.AUTHORITY_GUARDS[0]),
        new RealtimeObject("trigger","r29_registration_authority_no_delete","r29_registration_authority",ExperimentRegistrationR29.AUTHORITY_GUARDS[1]),
        new RealtimeObject("index","r29_event_outbox_pending","r29_event_outbox","CREATE INDEX IF NOT EXISTS r29_event_outbox_pending ON r29_event_outbox(acked_at,seq)"),
        new RealtimeObject("trigger","r29_event_payload_immutable","r29_event_outbox","CREATE TRIGGER IF NOT EXISTS r29_event_payload_immutable BEFORE UPDATE OF event_id,issue,kind,payload_json,payload_hash,origin,committed_at ON r29_event_outbox BEGIN SELECT RAISE(ABORT,'R29_EVENT_IMMUTABLE'); END")
    };
    /** A current-transaction, seven-name catalog read replaces no-op CREATE statements on every draw.
     * Android invalidates reader connections even for CREATE IF NOT EXISTS. No READY/version cache survives a
     * rollback, DROP, same-name replacement or reopened connection; every call examines the actual definitions. */
    public static void ensureRealtime(SQLiteDatabase db){ensureObjects(db,REALTIME_OBJECTS);}
    public static void ensureFetchObservationSchema(SQLiteDatabase db){ensureObjects(db,new RealtimeObject[]{FETCH_OBSERVATION});}
    private static void ensureObjects(SQLiteDatabase db,RealtimeObject[] expected){
        requireTransaction(db);
        List<RealtimeObject> missing=missingObjects(db,expected);
        if(missing.isEmpty())return;
        for(RealtimeObject object:missing)db.execSQL(object.sql);
        if(!missingObjects(db,expected).isEmpty())throw new IllegalStateException("R29_REALTIME_SCHEMA_INSTALL_INCOMPLETE");
    }
    private static List<RealtimeObject> missingObjects(SQLiteDatabase db,RealtimeObject[] expected){
        StringBuilder query=new StringBuilder("SELECT name,type,tbl_name,sql FROM main.sqlite_master WHERE name IN (");
        String[] names=new String[expected.length];Map<String,RealtimeObject> wanted=new LinkedHashMap<>();
        for(int i=0;i<expected.length;i++){if(i>0)query.append(',');query.append('?');names[i]=expected[i].name;wanted.put(names[i],expected[i]);}
        query.append(')');
        try(Cursor c=db.rawQuery(query.toString(),names)){
            while(c.moveToNext()){
                String name=c.getString(0);RealtimeObject object=wanted.remove(name);
                if(object==null||!object.type.equals(c.getString(1))||!object.table.equals(c.getString(2))||!object.canonical.equals(canonicalDdl(c.getString(3))))
                    throw new IllegalStateException("R29_REALTIME_SCHEMA_CONFLICT:"+name);
            }
        }
        return new ArrayList<>(wanted.values());
    }
    private static String canonicalDdl(String sql){
        if(sql==null)return "";
        String text=sql.trim();
        for(String kind:new String[]{"TABLE","INDEX","TRIGGER"}){String prefix="CREATE "+kind+" IF NOT EXISTS ";if(text.startsWith(prefix)){text="CREATE "+kind+" "+text.substring(prefix.length());break;}}
        StringBuilder result=new StringBuilder();boolean quoted=false,space=false;
        for(int i=0;i<text.length();i++){
            char ch=text.charAt(i);
            if(quoted){result.append(ch);if(ch=='\''){if(i+1<text.length()&&text.charAt(i+1)=='\'')result.append(text.charAt(++i));else quoted=false;}continue;}
            if(Character.isWhitespace(ch)){space=true;continue;}
            if(space&&result.length()>0)result.append(' ');space=false;result.append(ch);if(ch=='\'')quoted=true;
        }
        while(result.length()>0&&result.charAt(result.length()-1)==';')result.setLength(result.length()-1);
        return result.toString();
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
        com.ethk3.novaonepro.FetchObservationR29.acceptedWithin(db,issue,e.hash);
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

    /** The terminal owner calls this in its original realtime settlement transaction. Research receives an
     * immutable copy through the inbox; it never opens a second writer in that transaction. */
    public static void appendTerminalFeedback(SQLiteDatabase realtime,String eventId){
        requireTransaction(realtime);ensureRealtime(realtime);
        try(Cursor c=realtime.rawQuery("SELECT event_id,issue_ordinal,source_hash,payload,ready_at FROM r26_feedback WHERE event_id=?",new String[]{eventId})){
            if(!c.moveToFirst())throw new IllegalStateException("R29_FEEDBACK_SOURCE_RECEIPT_REQUIRED");
            append(realtime,terminalFeedback(c.getString(0),c.getLong(1),c.getString(2),c.getString(3),c.getLong(4)));
        }
    }
    static Event terminalFeedback(String original,long ordinal,String source,String draw,long ready){
        if(draw==null||draw.length()<12)throw new IllegalArgumentException("R29_FEEDBACK_DRAW");String issue=draw.substring(0,12);
        Map<String,Object> p=new LinkedHashMap<>();p.put("abi","R29_R26_TERMINAL_FEEDBACK_1");p.put("eventId",original);p.put("ordinal",ordinal);p.put("sourceHash",source);p.put("draw",draw);p.put("readyAt",ready);
        String payload=JsonR27.write(p),hash=StatsR27.sha256(payload);
        return new Event(0,StatsR27.sha256(TERMINAL_FEEDBACK+"|"+issue+"|"+hash),issue,payload,hash,"REALTIME_TERMINAL_SETTLEMENT",ready,TERMINAL_FEEDBACK);
    }
    static void validateFeedback(String issue,Map<String,Object> p){
        if(!"R29_R26_TERMINAL_FEEDBACK_1".equals(p.get("abi"))||!TargetIssueResolver.isValid(issue)
                ||!(p.get("ordinal") instanceof Number)||((Number)p.get("ordinal")).longValue()!=TargetIssueResolver.sequenceOrdinalSU1(issue)
                ||!(p.get("readyAt") instanceof Number)||((Number)p.get("readyAt")).longValue()<0
                ||!PolicyIdentityR26.sha256("R26_SETTLED|"+issue).equals(p.get("eventId"))
                ||!(p.get("sourceHash") instanceof String)||!((String)p.get("sourceHash")).matches("[0-9a-f]{64}")
                ||!(p.get("draw") instanceof String))throw new IllegalArgumentException("R29_FEEDBACK_PAYLOAD");
        String[] fields=((String)p.get("draw")).split(",",-1);if(fields.length!=11||!issue.equals(fields[0]))throw new IllegalArgumentException("R29_FEEDBACK_DRAW");
        int[] numbers=new int[10];for(int i=0;i<10;i++)try{numbers[i]=Integer.parseInt(fields[i+1]);}catch(NumberFormatException bad){throw new IllegalArgumentException("R29_FEEDBACK_DRAW",bad);}
        if(!ReplayDatasetR27.legal(numbers))throw new IllegalArgumentException("R29_FEEDBACK_PERMUTATION");
    }
    @SuppressWarnings("unchecked")
    static void consumeFeedback(SQLiteDatabase research,Event event){
        research.execSQL(RuntimeSchemaR26.FEEDBACK);
        Map<String,Object> p=(Map<String,Object>)JsonR27.parse(event.payload);validateFeedback(event.issue,p);
        String id=(String)p.get("eventId"),source=(String)p.get("sourceHash"),draw=(String)p.get("draw");long ordinal=((Number)p.get("ordinal")).longValue(),ready=((Number)p.get("readyAt")).longValue();
        research.execSQL("INSERT OR IGNORE INTO r26_feedback VALUES(?,?,?,?,?)",new Object[]{id,ordinal,source,draw,ready});
        try(Cursor c=research.rawQuery("SELECT issue_ordinal,source_hash,payload,ready_at FROM r26_feedback WHERE event_id=?",new String[]{id})){
            if(!c.moveToFirst()||c.getLong(0)!=ordinal||!source.equals(c.getString(1))||!draw.equals(c.getString(2))||c.getLong(3)!=ready)throw new IllegalStateException("R29_TERMINAL_FEEDBACK_CONFLICT");
        }
    }

    /** Existing terminal rows predate the outbox hook. Materialize them before acquiring only the realtime
     * writer; publishing their immutable receipts is delivery, never a new settlement or learning step. */
    private static void publishRetainedFeedback(ResearchPoolR27.Worker w){
        List<Event> retained=new ArrayList<>();
        try(Cursor exists=w.realtimeRo().rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='r26_feedback'",null)){if(!exists.moveToFirst())return;}
        try(Cursor c=w.realtimeRo().rawQuery("SELECT f.event_id,f.issue_ordinal,f.source_hash,f.payload,f.ready_at FROM r26_feedback f WHERE NOT EXISTS(SELECT 1 FROM r29_event_outbox o WHERE o.kind='R26_FEEDBACK_COMMITTED' AND o.issue=substr(f.payload,1,12)) ORDER BY f.issue_ordinal LIMIT "+PAGE,null)){
            while(c.moveToNext())retained.add(terminalFeedback(c.getString(0),c.getLong(1),c.getString(2),c.getString(3),c.getLong(4)));
        }
        if(!retained.isEmpty())w.store().coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_RETAINED_TERMINAL_FEEDBACK",db->{for(Event event:retained)append(db,event);return 0;});
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
            if(TERMINAL_FEEDBACK.equals(kind)){validateFeedback(issue,p);return;}
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
                try (Cursor c = db.rawQuery("SELECT source_seq,issue,payload_hash,origin FROM r29_event_inbox WHERE event_id=?", new String[] {e.id})) {
                    exists=c.moveToFirst();if (exists && (e.seq!=c.getLong(0)||!e.issue.equals(c.getString(1))||!e.hash.equals(c.getString(2))||!e.origin.equals(c.getString(3)))) throw new IllegalStateException("R29_INBOX_IDENTITY_CONFLICT");
                }
                if(!exists&&ExperimentRegistrationR29.KIND.equals(e.kind))ExperimentRegistrationR29.consume(db,e,now);
                if(!exists&&LegacyResearchTransferR29.supports(e.kind))LegacyResearchTransferR29.consume(db,e);
                if(TERMINAL_FEEDBACK.equals(e.kind))consumeFeedback(db,e);
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
            ModelArtifactR29.Stored stored=ModelArtifactR29.restore(db,"ATOMNET_FTRL");
            if(stored==null||!stateHash.equals(stored.stateHash)||!issue.equals(stored.artifact.trainedThrough))throw new IllegalStateException("R29_LEARNING_RECEIPT_WITHOUT_MATCHING_MODEL_COMMIT");
            return 0;
        };
    }

    static void receipt(ResearchWriteQueueR27.Batch b, String consumer, String issue, String hash, long units, long now) {
        b.add("INSERT OR IGNORE INTO r29_learning_receipt(consumer,issue,state_hash,units,completed_at) VALUES(?,?,?,?,?)", consumer, issue, hash, units, now);
        // Preserve the first successful receipt rather than changing evidence on a retry.
        b.add("UPDATE r29_learning_work SET status='COMMITTED',completed_at=(SELECT completed_at FROM r29_learning_receipt WHERE consumer=? AND issue=?),state_hash=(SELECT state_hash FROM r29_learning_receipt WHERE consumer=? AND issue=?) WHERE consumer=? AND issue=? AND status='PENDING'",
                consumer, issue, consumer, issue, consumer, issue);
    }

    static final String ACK_RECONCILE_SOURCE="ROOT_ACK_RECONCILE";
    static final String ACK_RECONCILE_BOUNDARY="ROOT_ACK_RECONCILE_BOUNDARY";
    static final class AcknowledgedRepair {
        final int scanned,repaired;final long last;
        AcknowledgedRepair(int scanned,int repaired,long last){this.scanned=scanned;this.repaired=repaired;this.last=last;}
    }
    private static long acknowledgedCursor(SQLiteDatabase research){
        try(Cursor c=research.rawQuery("SELECT last_seq FROM r29_event_cursor WHERE source=?",new String[]{ACK_RECONCILE_SOURCE})){return c.moveToFirst()?c.getLong(0):0;}
    }
    private static long acknowledgedBoundary(SQLiteDatabase research){
        try(Cursor c=research.rawQuery("SELECT last_seq FROM r29_event_cursor WHERE source=?",new String[]{ACK_RECONCILE_BOUNDARY})){return c.moveToFirst()?c.getLong(0):-1;}
    }
    /** A former Root-owned inbox can have committed an ACK after an immutable cutover snapshot.
     * Recover at most one page from the immutable outbox, without changing its ACK timestamp or
     * inventing model/settlement completion. The verified prefix and its real deliveries commit together. */
    static AcknowledgedRepair reconcileAcknowledged(ResearchPoolR27.Worker w)throws Exception{
        SQLiteDatabase root=w.realtimeRo(),research=w.ro;
        if(!ResearchRouteR29.active(root))return new AcknowledgedRepair(0,0,0);
        ResearchRouteR29.verifyActiveOwner(root);
        java.io.File expected=new java.io.File(new java.io.File(root.getPath()).getParentFile(),ResearchRouteR29.FILE_NAME);
        if(!expected.getAbsolutePath().equals(new java.io.File(research.getPath()).getAbsolutePath())||!ResearchRouteR29.isResearchDatabase(research))throw new IllegalStateException("R29_ACK_RECONCILIATION_REQUIRES_ACTIVE_RESEARCH_OWNER");
        String schema,source;long epoch;
        try(Cursor c=root.rawQuery("SELECT schema_hash,source_root,epoch FROM r29_database_route WHERE singleton=1",null)){if(!c.moveToFirst())throw new IllegalStateException("R29_ACK_RECONCILIATION_ROUTE_MISSING");schema=c.getString(0);source=c.getString(1);epoch=c.getLong(2);}
        try(Cursor c=research.rawQuery("SELECT phase,schema_hash,source_root,epoch FROM r29_research_owner WHERE singleton=1",null)){if(!c.moveToFirst()||!"READY".equals(c.getString(0))||!schema.equals(c.getString(1))||!source.equals(c.getString(2))||epoch!=c.getLong(3))throw new IllegalStateException("R29_ACK_RECONCILIATION_OWNER_MISMATCH");}
        final long before=acknowledgedCursor(research),recordedBoundary=acknowledgedBoundary(research);long capturedBoundary=recordedBoundary;
        if(recordedBoundary<0)try(Cursor c=root.rawQuery("SELECT coalesce(max(seq),0) FROM r29_event_outbox WHERE acked_at>0",null)){if(!c.moveToFirst())throw new IllegalStateException("R29_ACK_RECONCILIATION_HEAD_REQUIRED");capturedBoundary=c.getLong(0);}
        final long boundary=capturedBoundary;
        if(before<0||boundary<0||before>boundary)throw new IllegalStateException("R29_ACK_RECONCILIATION_PREFIX_INVALID");
        // New delivery uses this RP before ACKing Root. Repair only the history that preceded this
        // verified owner; do not repeatedly scan every normal ACK produced by the current runtime.
        if(recordedBoundary>=0&&before==boundary)return new AcknowledgedRepair(0,0,before);
        final List<Event> page=new ArrayList<>();
        // Close every Root cursor before submitting the independent RP transaction.
        try(Cursor c=root.rawQuery("SELECT seq,event_id,issue,payload_json,payload_hash,origin,committed_at,kind FROM r29_event_outbox WHERE acked_at>0 AND seq>? AND seq<=? ORDER BY seq LIMIT "+PAGE,new String[]{String.valueOf(before),String.valueOf(boundary)})){
            while(c.moveToNext())page.add(new Event(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getLong(6),c.getString(7)));
        }
        if(page.isEmpty()&&before<boundary)throw new IllegalStateException("R29_ACK_RECONCILIATION_CAPTURED_PREFIX_MISSING");
        final long last=page.isEmpty()?before:page.get(page.size()-1).seq,now=System.currentTimeMillis();final int[] repaired={0};
        ResearchWriteQueueR27.Batch batch=prepare(page,now);final DatabaseCoordinator.WriteCallable<Integer> original=batch.before;
        batch.before=db->{
            if(acknowledgedCursor(db)!=before)throw new IllegalStateException("CP3_DB_BUSY_ACK_RECONCILIATION_CURSOR_CHANGED");
            long priorBoundary=acknowledgedBoundary(db);
            if(priorBoundary>=0&&priorBoundary!=boundary)throw new IllegalStateException("CP3_DB_BUSY_ACK_RECONCILIATION_BOUNDARY_CHANGED");
            repaired[0]=0;
            for(Event event:page)try(Cursor c=db.rawQuery("SELECT 1 FROM r29_event_inbox WHERE event_id=?",new String[]{event.id})){if(!c.moveToFirst())repaired[0]++;}
            return original==null?0:original.call(db);
        };
        batch.add("INSERT OR IGNORE INTO r29_event_cursor(source,last_seq,updated_at) VALUES(?,?,?)",ACK_RECONCILE_BOUNDARY,boundary,now);
        batch.add("INSERT OR IGNORE INTO r29_event_cursor(source,last_seq,updated_at) VALUES(?,0,?)",ACK_RECONCILE_SOURCE,now);
        batch.add("UPDATE r29_event_cursor SET last_seq=?,updated_at=? WHERE source=? AND last_seq=?",last,now,ACK_RECONCILE_SOURCE,before);
        batch.after=db->{
            if(acknowledgedCursor(db)!=last||acknowledgedBoundary(db)!=boundary)throw new IllegalStateException("R29_ACK_RECONCILIATION_CURSOR_COMMIT_REQUIRED");
            for(Event event:page)try(Cursor c=db.rawQuery("SELECT source_seq,issue,payload_hash,origin FROM r29_event_inbox WHERE event_id=?",new String[]{event.id})){
                if(!c.moveToFirst()||event.seq!=c.getLong(0)||!event.issue.equals(c.getString(1))||!event.hash.equals(c.getString(2))||!event.origin.equals(c.getString(3)))throw new IllegalStateException("R29_ACK_RECONCILIATION_INBOX_COMMIT_REQUIRED");
            }
            return 0;
        };
        RuntimeR27.submit(w,"EVENT_ACK_RECONCILIATION",batch);
        return new AcknowledgedRepair(page.size(),repaired[0],last);
    }

    public static ResearchPoolR27.Outcome deliver(ResearchPoolR27.Worker w) throws Exception {
        AcknowledgedRepair repair=reconcileAcknowledged(w);
        ExperimentRegistrationR29.publishRequests(w);
        publishRetainedFeedback(w);
        List<Event> events = pending(w.realtimeRo());
        if (events.isEmpty()) {
            ResearchWriteQueueR27.Batch completed = reconcileSettlements(w.ro, System.currentTimeMillis());
            if (!completed.isEmpty()) { RuntimeR27.submit(w, "EVENT_SETTLEMENT_RECEIPTS", completed); return ResearchPoolR27.Outcome.progress(1, 0); }
            if(repair.repaired>0)return ResearchPoolR27.Outcome.progress(repair.repaired,repair.last);
            if(repair.scanned>0)return ResearchPoolR27.Outcome.idle("COOPERATIVE_ACK_RECONCILIATION_PAGE_VERIFIED",repair.last,-1);
            return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT:outbox drained; pending work requires actual learner/settlement commit", 0, -1);
        }
        ResearchWriteQueueR27.Batch batch = prepare(events, System.currentTimeMillis());
        RuntimeR27.submit(w, "EVENT_INBOX", batch); // returns with research transaction finished
        w.store().coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_EVENT_ACK", db -> {
            acknowledge(db, events, System.currentTimeMillis()); return 0;
        });
        return ResearchPoolR27.Outcome.progress(events.size()+repair.repaired,Math.max(repair.last,events.get(events.size() - 1).seq));
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
        m.put("databaseIsolation",ResearchRouteR29.isResearchDatabase(research)?"ACTIVE_INDEPENDENT_RESEARCH_FILE":"ORIGINAL_OWNER_MIGRATION_PENDING");
        m.put("terminalRowsVisibleToResearch",QualificationInboxR29.count(research,"SELECT count(*) FROM r26_feedback"));
        m.put("terminalMirrorMeaning","Read-only delivery of original realtime settlement receipts; visible rows are not newly trained samples");
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
