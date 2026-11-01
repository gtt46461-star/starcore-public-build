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
    static final class Committed {final String event,clock;final IssueTimelineR30.Stage stage;final long wall,mono;Committed(String event,IssueTimelineR30.Stage stage,long wall,long mono,String clock){this.event=event;this.stage=stage;this.wall=wall;this.mono=mono;this.clock=clock;}}
    /** Called only after the caller observes a successful transaction return, after COMMIT and writer release. */
    public static void committed(String event,IssueTimelineR30.Stage stage){try{committedAt(event,stage,System.currentTimeMillis(),SystemClock.elapsedRealtime(),BOOT_ID);}catch(RuntimeException unavailable){diagnosticFailure(unavailable);}}
    static void committedAt(String event,IssueTimelineR30.Stage stage,long wall,long mono,String clock){
        if(event==null||stage!=IssueTimelineR30.Stage.SETTLED&&stage!=IssueTimelineR30.Stage.NEXT_FROZEN)return;
        try{String key=event+"|"+stage;if(COMMITTED.containsKey(key))return;if(COMMITTED_SIZE.incrementAndGet()>256){COMMITTED_SIZE.decrementAndGet();OMITTED_COMMITTED.incrementAndGet();return;}if(COMMITTED.putIfAbsent(key,new Committed(event,stage,wall,mono,clock))!=null)COMMITTED_SIZE.decrementAndGet();}catch(RuntimeException ignored){}
    }
    static void flushCommittedWithin(android.database.sqlite.SQLiteDatabase db,java.util.List<Committed> receipts){
        for(Committed receipt:receipts)db.execSQL("UPDATE "+IssueTimelineR30.EVENT_TABLE+" SET durable_wall=?,durable_mono=?,durable_boot=?,durable_stage=?,timing_boundary='CALLER_OBSERVED_SUCCESS_AFTER_SQLITE_COMMIT',updated_at=? WHERE event_id=? AND durable_mono IS NULL",new Object[]{receipt.wall,receipt.mono,receipt.clock,receipt.stage.name(),System.currentTimeMillis(),receipt.event});
    }
    public static void flushCommitted(com.ethk3.novaonepro.DbStore store){
        if(COMMITTED.isEmpty()||!FLUSHING_COMMITTED.compareAndSet(false,true))return;
        try{java.util.List<Committed> receipts=new java.util.ArrayList<>();for(Committed receipt:COMMITTED.values()){receipts.add(receipt);if(receipts.size()==64)break;}
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_TIMELINE_COMMIT_OBSERVATIONS",db->{flushCommittedWithin(db,receipts);return 0;});
            for(Committed receipt:receipts)if(COMMITTED.remove(receipt.event+"|"+receipt.stage,receipt))COMMITTED_SIZE.decrementAndGet();
        }catch(RuntimeException unavailable){diagnosticFailure(unavailable);}finally{FLUSHING_COMMITTED.set(false);}
    }
    public static java.util.Map<String,Object> commitObservationState(){java.util.Map<String,Object> out=new java.util.LinkedHashMap<>();out.put("scope","CURRENT_PROCESS_PENDING_RECEIPTS; unflushed commit instants UNKNOWN after process death");out.put("pending",COMMITTED_SIZE.get());out.put("omittedDueToBound",OMITTED_COMMITTED.get());out.put("clockScope",BOOT_ID.startsWith("KERNEL_BOOT:")?"KERNEL_BOOT":"PROCESS_ONLY_CONSERVATIVE");return out;}
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
    static void diagnosticFailure(RuntimeException failure){android.util.Log.w("STARCORE_R30","event timeline unavailable: "+failure.getClass().getSimpleName());}
    /** First-writer stage timestamps belong to this exact event, never to an arbitrary row sharing an issue. */
    public static void markEventAt(SQLiteDatabase db,String issue,IssueTimelineR30.EventRole role,IssueTimelineR30.EventKind kind,String trace,String sourceIssue,String sourceDrawHash,String drawHash,IssueTimelineR30.Stage stage,long wall,long mono,String clockScope){
        if(db==null||!db.inTransaction()||issue==null||trace==null||trace.isEmpty()||clockScope==null||clockScope.isEmpty())return;
        try{
            String id=eventId(issue,kind,trace,drawHash);long now=System.currentTimeMillis();
            db.execSQL("INSERT OR IGNORE INTO "+IssueTimelineR30.EVENT_TABLE+"(event_id,issue,event_role,event_kind,source_trace,source_issue,source_draw_hash,draw_hash,boot_id,event_started_wall,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{id,issue,role.name(),kind.name(),trace,sourceIssue,sourceDrawHash,drawHash,clockScope,wall,now});
            try(android.database.Cursor c=db.rawQuery("SELECT issue,event_role,event_kind,source_trace,draw_hash,source_issue,source_draw_hash FROM "+IssueTimelineR30.EVENT_TABLE+" WHERE event_id=?",new String[]{id})){
                if(!c.moveToFirst()||!issue.equals(c.getString(0))||!role.name().equals(c.getString(1))||!kind.name().equals(c.getString(2))||!trace.equals(c.getString(3))||!java.util.Objects.equals(drawHash,c.getString(4))||sourceIssue!=null&&c.getString(5)!=null&&!sourceIssue.equals(c.getString(5))||sourceDrawHash!=null&&c.getString(6)!=null&&!sourceDrawHash.equals(c.getString(6))){db.execSQL("UPDATE "+IssueTimelineR30.EVENT_TABLE+" SET outcome='EVENT_IDENTITY_CONFLICT',updated_at=? WHERE event_id=?",new Object[]{now,id});return;}
            }
            db.execSQL("UPDATE "+IssueTimelineR30.EVENT_TABLE+" SET source_issue=coalesce(source_issue,?),source_draw_hash=coalesce(source_draw_hash,?) WHERE event_id=?",new Object[]{sourceIssue,sourceDrawHash,id});
            String column=stage.name().toLowerCase(java.util.Locale.ROOT);
            db.execSQL("UPDATE "+IssueTimelineR30.EVENT_TABLE+" SET duplicate_marks=duplicate_marks+1 WHERE event_id=? AND wall_"+column+" IS NOT NULL",new Object[]{id});
            if(IssueTimelineR30.hasMono(stage))db.execSQL("UPDATE "+IssueTimelineR30.EVENT_TABLE+" SET wall_"+column+"=?,mono_"+column+"=?,boot_"+column+"=?,updated_at=? WHERE event_id=? AND wall_"+column+" IS NULL",new Object[]{wall,mono,clockScope,now,id});
            else db.execSQL("UPDATE "+IssueTimelineR30.EVENT_TABLE+" SET wall_"+column+"=?,updated_at=? WHERE event_id=? AND wall_"+column+" IS NULL",new Object[]{wall,now,id});
        }catch(RuntimeException unavailable){diagnosticFailure(unavailable);}
    }
    public static void markPredictionAt(SQLiteDatabase db,String target,String attempt,String baseIssue,IssueTimelineR30.Stage stage,long wall,long mono){
        try{
            String base=baseIssue,hash=null;String id=eventId(target,IssueTimelineR30.EventKind.PREDICTION,attempt,null);
            if(base==null)try(android.database.Cursor c=db.rawQuery("SELECT source_issue,source_draw_hash FROM "+IssueTimelineR30.EVENT_TABLE+" WHERE event_id=?",new String[]{id})){if(c.moveToFirst()){base=c.getString(0);hash=c.getString(1);}}
            if(base!=null&&hash==null)try(android.database.Cursor c=db.rawQuery("SELECT draw_hash_v2 FROM draws WHERE issue=?",new String[]{base})){if(c.moveToFirst())hash=c.getString(0);}
            markEventAt(db,target,IssueTimelineR30.EventRole.LIVE,IssueTimelineR30.EventKind.PREDICTION,attempt,base,hash,null,stage,wall,mono,BOOT_ID);
        }catch(RuntimeException unavailable){diagnosticFailure(unavailable);}
    }
    public static void markPrediction(SQLiteDatabase db,String target,String attempt,String base,IssueTimelineR30.Stage stage){markPredictionAt(db,target,attempt,base,stage,System.currentTimeMillis(),SystemClock.elapsedRealtime());}
    public static String markSettled(SQLiteDatabase db,String issue){
        try(android.database.Cursor c=db.rawQuery("SELECT event_id,event_role,source_trace,draw_hash FROM "+IssueTimelineR30.EVENT_TABLE+" WHERE issue=? AND event_kind='DRAW' AND mono_draw_committed IS NOT NULL AND draw_hash=(SELECT draw_hash_v2 FROM draws WHERE issue=?) ORDER BY wall_draw_committed,event_id LIMIT 1",new String[]{issue,issue})){
            if(c.moveToFirst()){markEventAt(db,issue,IssueTimelineR30.EventRole.valueOf(c.getString(1)),IssueTimelineR30.EventKind.DRAW,c.getString(2),null,null,c.getString(3),IssueTimelineR30.Stage.SETTLED,System.currentTimeMillis(),SystemClock.elapsedRealtime(),BOOT_ID);return c.getString(0);}
        }catch(RuntimeException unavailable){diagnosticFailure(unavailable);}
        return null;
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
