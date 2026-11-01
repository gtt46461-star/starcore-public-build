package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;

/**
 * Event-instance telemetry on the existing writer. V1 methods retain the historical per-issue API.
 * V2 DRAW identities bind the original HTTP request and exact draw hash; PREDICTION identities bind
 * the original attempt plus its base issue/hash. LIVE, BACKFILL and REPLAY never share a duration.
 * R925 captures the actual response monotonic instant before waiting for its receipt write.
 * No wall-clock difference is used to reconstruct a monotonic instant. An unavailable kernel boot
 * identity falls back to a process-only scope, so cross-process continuity remains UNKNOWN.
 * Stage DML runs in its existing owner transaction. Successful root-COMMIT return observations are
 * queued after the sole writer releases and persisted by the original diagnostics task. A missing
 * or unflushed observation remains UNKNOWN after process death, never a fabricated zero or PASS.
 */
public final class IssueTimelineWriterR30 {
    private IssueTimelineWriterR30() {}
    private static final String BOOT_ID = realBootOrConservativeProcessScope();
    private static final ThreadLocal<IssueTimelineR30.EventRole> REQUEST_ROLE=new ThreadLocal<>();
    private static final java.util.concurrent.ConcurrentHashMap<String,Committed> COMMITTED=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicInteger COMMITTED_SIZE=new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicBoolean FLUSHING_COMMITTED=new java.util.concurrent.atomic.AtomicBoolean();
    private static final java.util.concurrent.atomic.AtomicLong OMITTED_COMMITTED=new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong DIAGNOSTIC_FAILURES=new java.util.concurrent.atomic.AtomicLong();
    static final class Committed {final String event,clock;final IssueTimelineR30.Stage stage;final long wall,mono;Committed(String event,IssueTimelineR30.Stage stage,long wall,long mono,String clock){this.event=event;this.stage=stage;this.wall=wall;this.mono=mono;this.clock=clock;}}
    /** Called only after the caller observes a successful transaction return, after COMMIT and writer release. */
    public static void committed(String event,IssueTimelineR30.Stage stage){try{committedAt(event,stage,System.currentTimeMillis(),SystemClock.elapsedRealtime(),BOOT_ID);}catch(RuntimeException unavailable){diagnosticFailure(unavailable);}}
    static void committedAt(String event,IssueTimelineR30.Stage stage,long wall,long mono,String clock){
        if(event==null||stage!=IssueTimelineR30.Stage.SETTLED&&stage!=IssueTimelineR30.Stage.NEXT_FROZEN)return;
        try{String key=event+"|"+stage;if(COMMITTED.containsKey(key))return;if(COMMITTED_SIZE.incrementAndGet()>256){COMMITTED_SIZE.decrementAndGet();OMITTED_COMMITTED.incrementAndGet();return;}if(COMMITTED.putIfAbsent(key,new Committed(event,stage,wall,mono,clock))!=null)COMMITTED_SIZE.decrementAndGet();}catch(RuntimeException ignored){}
    }
    static void flushCommittedWithin(SQLiteDatabase db,java.util.List<Committed> receipts){
        for(Committed receipt:receipts){java.util.Map<String,Object> row=readEvent(db,receipt.event);if(row==null||row.get("durable_mono")!=null)continue;row.put("durable_wall",receipt.wall);row.put("durable_mono",receipt.mono);row.put("durable_boot",receipt.clock);row.put("durable_stage",receipt.stage.name());row.put("timing_boundary","CALLER_OBSERVED_SUCCESS_AFTER_SQLITE_COMMIT");saveEvent(db,row);}
    }
    public static void flushCommitted(com.ethk3.novaonepro.DbStore store){
        if(COMMITTED.isEmpty()||!FLUSHING_COMMITTED.compareAndSet(false,true))return;
        try{java.util.List<Committed> receipts=new java.util.ArrayList<>();for(Committed receipt:COMMITTED.values()){receipts.add(receipt);if(receipts.size()==64)break;}
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_TIMELINE_COMMIT_OBSERVATIONS",db->{flushCommittedWithin(db,receipts);return 0;});
            for(Committed receipt:receipts)if(COMMITTED.remove(receipt.event+"|"+receipt.stage,receipt))COMMITTED_SIZE.decrementAndGet();
        }catch(RuntimeException unavailable){diagnosticFailure(unavailable);}finally{FLUSHING_COMMITTED.set(false);}
    }
    public static java.util.Map<String,Object> commitObservationState(){java.util.Map<String,Object> out=new java.util.LinkedHashMap<>();out.put("scope","CURRENT_PROCESS_PENDING_RECEIPTS; unflushed commit instants UNKNOWN after process death");out.put("pending",COMMITTED_SIZE.get());out.put("omittedDueToBound",OMITTED_COMMITTED.get());out.put("diagnosticFailures",DIAGNOSTIC_FAILURES.get());out.put("clockScope",BOOT_ID.startsWith("KERNEL_BOOT:")?"KERNEL_BOOT":"PROCESS_ONLY_CONSERVATIVE");return out;}
    private static String realBootOrConservativeProcessScope(){
        try(java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.FileReader("/proc/sys/kernel/random/boot_id"))){String value=reader.readLine();if(value!=null&&value.matches("[0-9a-fA-F-]{36}"))return "KERNEL_BOOT:"+value.toLowerCase(java.util.Locale.ROOT);}
        catch(java.io.IOException|SecurityException unavailable){}
        return "PROCESS_ONLY:"+java.util.UUID.randomUUID();
    }
    public static final class RequestScope implements AutoCloseable {
        private final IssueTimelineR30.EventRole previous;private boolean closed;
        private RequestScope(IssueTimelineR30.EventRole requested){previous=REQUEST_ROLE.get();REQUEST_ROLE.set(previous==IssueTimelineR30.EventRole.LIVE?previous:requested);}
        @Override public void close(){if(closed)return;closed=true;if(previous==null)REQUEST_ROLE.remove();else REQUEST_ROLE.set(previous);}
    }
    public static RequestScope requestScope(IssueTimelineR30.EventRole role){return new RequestScope(java.util.Objects.requireNonNull(role));}
    public static IssueTimelineR30.EventRole requestRole(){IssueTimelineR30.EventRole role=REQUEST_ROLE.get();return role==null?IssueTimelineR30.EventRole.UNKNOWN:role;}

    public static String bootId() { return BOOT_ID; }
    public static String eventId(String issue,IssueTimelineR30.EventKind kind,String trace,String drawHash){return HashingSU1.text(kind+"|"+issue+"|"+trace+"|"+(drawHash==null?"":drawHash));}
    static void diagnosticFailure(RuntimeException failure){DIAGNOSTIC_FAILURES.incrementAndGet();android.util.Log.w("STARCORE_R30","event timeline unavailable: "+failure.getClass().getSimpleName());}
    @SuppressWarnings("unchecked") static java.util.Map<String,Object> readMetadata(SQLiteDatabase db,String panel,String key){
        try(android.database.Cursor c=db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",new String[]{panel,key})){if(!c.moveToFirst())return null;Object parsed=JsonR27.parse(c.getString(0));if(!(parsed instanceof java.util.Map))throw new IllegalStateException("R30_TIMELINE_PAYLOAD_UNREADABLE");return new java.util.LinkedHashMap<>((java.util.Map<String,Object>)parsed);}
    }
    static java.util.Map<String,Object> readEvent(SQLiteDatabase db,String id){
        java.util.Map<String,Object> found=null;for(String panel:new String[]{IssueTimelineR30.EVENT_DRAW_PANEL,IssueTimelineR30.EVENT_PRED_PANEL,IssueTimelineR30.EVENT_RAW_PANEL}){java.util.Map<String,Object> row=readMetadata(db,panel,id);if(row!=null){if(found!=null)throw new IllegalStateException("R30_TIMELINE_DUPLICATE_STORAGE_IDENTITY");if(!id.equals(row.get("event_id"))||!"ISSUE_EVENT_R30_REPAIR_1".equals(row.get("version")))throw new IllegalStateException("R30_TIMELINE_EVENT_IDENTITY_UNREADABLE");found=row;}}return found;
    }
    static void saveEvent(SQLiteDatabase db,java.util.Map<String,Object> row){
        String id=(String)row.get("event_id"),panel="PREDICTION".equals(row.get("event_kind"))?IssueTimelineR30.EVENT_PRED_PANEL:row.get("wall_draw_committed")!=null?IssueTimelineR30.EVENT_DRAW_PANEL:IssueTimelineR30.EVENT_RAW_PANEL;
        db.execSQL("DELETE FROM r27_chart_cache WHERE panel_id IN (?,?,?) AND key=? AND panel_id<>?",new Object[]{IssueTimelineR30.EVENT_RAW_PANEL,IssueTimelineR30.EVENT_DRAW_PANEL,IssueTimelineR30.EVENT_PRED_PANEL,id,panel});
        db.execSQL("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES(?,?,?,?)",new Object[]{panel,id,JsonR27.write(row),row.get("event_started_wall")});
    }
    /** First-writer clocks are scoped to this immutable request/attempt; old physical timeline stays intact. */
    public static void markEventAt(SQLiteDatabase db,String issue,IssueTimelineR30.EventRole role,IssueTimelineR30.EventKind kind,String trace,String sourceIssue,String sourceDrawHash,String drawHash,IssueTimelineR30.Stage stage,long wall,long mono,String clockScope){
        if(db==null||!db.inTransaction()||issue==null||trace==null||trace.isEmpty()||clockScope==null||clockScope.isEmpty())return;
        boolean savepoint=false;try{
            db.execSQL("SAVEPOINT r30_event_observation");savepoint=true;
            String id=eventId(issue,kind,trace,drawHash);java.util.Map<String,Object> row=readEvent(db,id);
            if(row==null){row=new java.util.LinkedHashMap<>();row.put("version","ISSUE_EVENT_R30_REPAIR_1");row.put("event_id",id);row.put("issue",issue);row.put("event_role",role.name());row.put("event_kind",kind.name());row.put("source_trace",trace);row.put("draw_hash",drawHash);row.put("boot_id",clockScope);row.put("event_started_wall",wall);row.put("duplicate_marks",0L);row.put("outcome","OPEN");}
            if(!issue.equals(row.get("issue"))||!role.name().equals(row.get("event_role"))||!kind.name().equals(row.get("event_kind"))||!trace.equals(row.get("source_trace"))||!java.util.Objects.equals(drawHash,row.get("draw_hash"))||sourceIssue!=null&&row.get("source_issue")!=null&&!sourceIssue.equals(row.get("source_issue"))||sourceDrawHash!=null&&row.get("source_draw_hash")!=null&&!sourceDrawHash.equals(row.get("source_draw_hash"))){row.put("outcome","EVENT_IDENTITY_CONFLICT");saveEvent(db,row);db.execSQL("RELEASE r30_event_observation");savepoint=false;return;}
            if(row.get("source_issue")==null&&sourceIssue!=null)row.put("source_issue",sourceIssue);if(row.get("source_draw_hash")==null&&sourceDrawHash!=null)row.put("source_draw_hash",sourceDrawHash);
            String column=stage.name().toLowerCase(java.util.Locale.ROOT);Object prior=row.get("wall_"+column);
            if(prior==null){row.put("wall_"+column,wall);if(IssueTimelineR30.hasMono(stage)){row.put("mono_"+column,mono);row.put("boot_"+column,clockScope);}}
            else if(!java.util.Objects.equals(((Number)prior).longValue(),wall)||IssueTimelineR30.hasMono(stage)&&(!java.util.Objects.equals(row.get("mono_"+column),mono)||!java.util.Objects.equals(row.get("boot_"+column),clockScope)))row.put("duplicate_marks",((Number)row.get("duplicate_marks")).longValue()+1);
            if(kind==IssueTimelineR30.EventKind.DRAW&&stage==IssueTimelineR30.Stage.DRAW_COMMITTED){java.util.Map<String,Object> head=new java.util.LinkedHashMap<>();for(String field:new String[]{"event_id","issue","event_role","source_trace","draw_hash"})head.put(field,row.get(field));db.execSQL("INSERT OR IGNORE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES(?,?,?,?)",new Object[]{IssueTimelineR30.EVENT_HEAD_PANEL,issue+"|"+drawHash,JsonR27.write(head),wall});row.put("canonical_draw_head",id.equals(readMetadata(db,IssueTimelineR30.EVENT_HEAD_PANEL,issue+"|"+drawHash).get("event_id")));}
            saveEvent(db,row);
            db.execSQL("RELEASE r30_event_observation");savepoint=false;
        }catch(RuntimeException unavailable){if(savepoint)try{db.execSQL("ROLLBACK TO r30_event_observation");db.execSQL("RELEASE r30_event_observation");}catch(RuntimeException abortedOwner){/* The original owner will surface an aborted transaction. */}diagnosticFailure(unavailable);}
    }
    public static void markPredictionAt(SQLiteDatabase db,String target,String attempt,String baseIssue,IssueTimelineR30.Stage stage,long wall,long mono){
        try{String base=baseIssue,hash=null;java.util.Map<String,Object> existing=readEvent(db,eventId(target,IssueTimelineR30.EventKind.PREDICTION,attempt,null));if(existing!=null){if(base==null)base=(String)existing.get("source_issue");hash=(String)existing.get("source_draw_hash");}
            if(base!=null&&hash==null)try(android.database.Cursor c=db.rawQuery("SELECT draw_hash_v2 FROM draws WHERE issue=?",new String[]{base})){if(c.moveToFirst())hash=c.getString(0);}
            markEventAt(db,target,IssueTimelineR30.EventRole.LIVE,IssueTimelineR30.EventKind.PREDICTION,attempt,base,hash,null,stage,wall,mono,BOOT_ID);
        }catch(RuntimeException unavailable){diagnosticFailure(unavailable);}
    }
    public static void markPrediction(SQLiteDatabase db,String target,String attempt,String base,IssueTimelineR30.Stage stage){markPredictionAt(db,target,attempt,base,stage,System.currentTimeMillis(),SystemClock.elapsedRealtime());}
    public static String markSettled(SQLiteDatabase db,String issue){
        try{String hash=null;try(android.database.Cursor c=db.rawQuery("SELECT draw_hash_v2 FROM draws WHERE issue=?",new String[]{issue})){if(c.moveToFirst())hash=c.getString(0);}if(hash==null)return null;java.util.Map<String,Object> head=readMetadata(db,IssueTimelineR30.EVENT_HEAD_PANEL,issue+"|"+hash);if(head==null)return null;
            markEventAt(db,issue,IssueTimelineR30.EventRole.valueOf((String)head.get("event_role")),IssueTimelineR30.EventKind.DRAW,(String)head.get("source_trace"),null,null,hash,IssueTimelineR30.Stage.SETTLED,System.currentTimeMillis(),SystemClock.elapsedRealtime(),BOOT_ID);return(String)head.get("event_id");
        }catch(RuntimeException unavailable){diagnosticFailure(unavailable);return null;}
    }

    public static void mark(SQLiteDatabase db, String issue, IssueTimelineR30.Stage stage) {
        markAt(db, issue, stage, System.currentTimeMillis(), SystemClock.elapsedRealtime());
    }

    public static void markAt(SQLiteDatabase db, String issue, IssueTimelineR30.Stage stage, long wallMs, long monoMs) {
        if (db == null || issue == null || issue.isEmpty() || !db.inTransaction()) return;
        try {
            long now = System.currentTimeMillis();
            db.execSQL(IssueTimelineR30.SQL_ENSURE, new Object[] {issue, BOOT_ID, now});
            db.execSQL(IssueTimelineR30.sqlMark(stage), IssueTimelineR30.markArgs(stage, issue, wallMs, monoMs, BOOT_ID, now));
        } catch (android.database.sqlite.SQLiteException diagnosticOnly) {
            android.util.Log.w("STARCORE_R30", "timeline mark skipped " + stage, diagnosticOnly);
        }
    }

    public static void deadline(SQLiteDatabase db, String targetIssue, long deadlineWallMs) {
        markAt(db, targetIssue, IssueTimelineR30.Stage.NEXT_DEADLINE, deadlineWallMs, 0L);
    }
}
