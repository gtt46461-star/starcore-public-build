package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.UUID;

/**
 * A bounded, reconstructible scheduling index; never a prediction/settlement/training ledger.
 * All mutators require the caller's existing coordinator transaction. The authorityRef names
 * the immutable source receipt. Completion must share the transaction with the owner's writes.
 */
public final class ResearchTaskQueueR967 {
    public static final String TABLE="v57_research_task_r967";
    public static final String PIN="v57_research_input_pin_r967";
    public static final String POST_TRAIN="POST_TRAIN", MAINTENANCE="MAINTENANCE", LANE="v57_research_lane_cursor_r967";
    /** Payload and control capacity are separate: a full payload queue must still run its drain owner. */
    public static final int MAX_ACTIVE=4096;
    public static final int MAX_MAINTENANCE=22;
    public static final long LEASE_MS=45000L;
    private static final String PROCESS=UUID.randomUUID().toString();
    private static final java.util.concurrent.ConcurrentHashMap<String,Lease> ACTIVE_CP4=
            new java.util.concurrent.ConcurrentHashMap<String,Lease>();
    private ResearchTaskQueueR967(){}

    public static void schema(SQLiteDatabase db) {
        transaction(db);
        if(TransactionSchemaCacheR941.readyCommitted(db,"R980_DURABLE_TASK_CONTROL_CAPACITY"))return;
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(task_id TEXT PRIMARY KEY,kind TEXT NOT NULL,candidate_id TEXT NOT NULL,target_issue TEXT NOT NULL,stream_seq TEXT NOT NULL,input_hash TEXT NOT NULL,authority_ref TEXT NOT NULL,status TEXT NOT NULL CHECK(status IN ('PENDING','RUNNING','YIELDED','WAIT_INPUT','COMPLETED','EXPIRED','FAILED_TERMINAL')),cursor TEXT NOT NULL DEFAULT '',generation INTEGER NOT NULL DEFAULT 0,lease_owner TEXT NOT NULL DEFAULT '',lease_until INTEGER NOT NULL DEFAULT 0,available_at INTEGER NOT NULL DEFAULT 0,attempt_count INTEGER NOT NULL DEFAULT 0,last_error TEXT NOT NULL DEFAULT '',result_hash TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,UNIQUE(kind,candidate_id,target_issue,stream_seq))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r967_task_ready ON "+TABLE+"(kind,status,available_at,updated_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r967_task_order ON "+TABLE+"(kind,candidate_id,target_issue,status)");
        // Match the readyPredicate's unresolved-predecessor clause verbatim. Completed
        // history is deliberately absent from this index; no scheduling/evidence rows
        // are deleted to improve performance.
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r975_task_unresolved ON "+TABLE+"(kind,candidate_id,target_issue) WHERE status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL')");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r975_task_terminal_block ON "+TABLE+"(kind,candidate_id,target_issue) WHERE status IN ('EXPIRED','FAILED_TERMINAL')");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r975_post_authority ON "+TABLE+"(kind,authority_ref)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+PIN+"(task_id TEXT NOT NULL,source_table TEXT NOT NULL,source_issue TEXT NOT NULL,PRIMARY KEY(task_id,source_table,source_issue))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r967_input_pin ON "+PIN+"(source_table,source_issue,task_id)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r967_task_identity BEFORE UPDATE OF task_id,kind,candidate_id,target_issue,stream_seq,input_hash,authority_ref,created_at ON "+TABLE+" BEGIN SELECT RAISE(ABORT,'R967_TASK_IDENTITY_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r967_task_terminal BEFORE UPDATE ON "+TABLE+" WHEN OLD.status IN ('COMPLETED','EXPIRED','FAILED_TERMINAL') BEGIN SELECT RAISE(ABORT,'R967_TASK_TERMINAL_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r967_task_active_delete BEFORE DELETE ON "+TABLE+" WHEN OLD.status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL') BEGIN SELECT RAISE(ABORT,'R967_PENDING_TASK_PROTECTED'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r967_input_pin_identity BEFORE UPDATE ON "+PIN+" BEGIN SELECT RAISE(ABORT,'R967_PIN_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r967_input_pin_active_delete BEFORE DELETE ON "+PIN+" WHEN EXISTS(SELECT 1 FROM "+TABLE+" t WHERE t.task_id=OLD.task_id AND t.status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL')) BEGIN SELECT RAISE(ABORT,'R967_PENDING_INPUT_PROTECTED'); END");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+LANE+"(lane INTEGER PRIMARY KEY CHECK(lane>=0 AND lane<22),desired_issue TEXT NOT NULL,served_issue TEXT NOT NULL DEFAULT '',next_sequence INTEGER NOT NULL DEFAULT 0,active_task_id TEXT NOT NULL DEFAULT '',last_outcome TEXT NOT NULL DEFAULT '',updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_r967_one_active_lane ON "+TABLE+"(kind,candidate_id) WHERE kind='MAINTENANCE' AND status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL')");
        TransactionSchemaCacheR941.markCommitted(db,"R980_DURABLE_TASK_CONTROL_CAPACITY");
    }
    public static String naturalKey(String kind,String candidate,String issue,String sequence) {
        required(kind);required(candidate);required(sequence);
        if(!TargetIssueResolver.isValid(issue))throw new IllegalArgumentException("R967_TASK_ISSUE");
        return V51ChecksumEngine.sha256("R967_TASK_INDEX_V1"+part(ReleaseManifestR926.GAME_CODE)+part(kind)+part(candidate)+part(issue)+part(sequence));
    }
    /** Equal natural keys with unequal payloads are corruption, not a second task. */
    public static String enqueue(SQLiteDatabase db,String kind,String candidate,String issue,String sequence,String input,String authority) {
        transaction(db);required(input);required(authority);
        String id=naturalKey(kind,candidate,issue,sequence);
        try(Cursor c=db.rawQuery("SELECT input_hash,authority_ref FROM "+TABLE+" WHERE task_id=?",new String[]{id})) {
            if(c.moveToFirst()) {
                if(!input.equals(c.getString(0))||!authority.equals(c.getString(1)))throw new IllegalStateException("R967_TASK_INPUT_DRIFT");
                return id;
            }
        }
        // R979 could fill all 4096 slots with POST_TRAIN and then reject lane:13,
        // the only owner that can drain them. Do not delete/ACK inputs to escape it.
        // Preserve the old 4096-payload bound and reserve exactly 22 control slots;
        // an imported full queue can therefore recover with at most 4118 active rows.
        final boolean control=MAINTENANCE.equals(kind);
        if(control && !candidate.matches("lane:(?:[0-9]|1[0-9]|2[01])"))
            throw new IllegalArgumentException("R980_MAINTENANCE_LANE_ID");
        String category=control?"kind='MAINTENANCE'":"kind<>'MAINTENANCE'";
        try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+TABLE+" WHERE "+category
                +" AND status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL')",null)) {
            if(c.moveToFirst()&&c.getLong(0)>=(control?MAX_MAINTENANCE:MAX_ACTIVE))
                throw new Capacity(control?"R980_CONTROL_CAPACITY":"R967_TASK_BACKPRESSURE");
        }
        long now=System.currentTimeMillis();
        db.execSQL("INSERT INTO "+TABLE+"(task_id,kind,candidate_id,target_issue,stream_seq,input_hash,authority_ref,status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,'PENDING',?,?)",new Object[]{id,kind,candidate,issue,sequence,input,authority,now,now});
        return id;
    }
    public static void pin(SQLiteDatabase db,String task,String table,String issue) {
        transaction(db);
        if(table==null||!table.matches("[A-Za-z_][A-Za-z0-9_]*")||!TargetIssueResolver.isValid(issue))throw new IllegalArgumentException("R967_PIN_SOURCE");
        try(Cursor c=db.rawQuery("SELECT status FROM "+TABLE+" WHERE task_id=?",new String[]{task})) {
            if(!c.moveToFirst()||terminal(c.getString(0)))throw new IllegalStateException("R967_PIN_REQUIRES_ACTIVE_TASK");
        }
        db.execSQL("INSERT OR IGNORE INTO "+PIN+" VALUES(?,?,?)",new Object[]{task,table,issue});
    }
    public static final class Lease {
        public final String id,kind,candidate,issue,sequence,inputHash,authorityRef,cursor,owner;
        public final long generation,leaseUntil,monotonicUntil;
        Lease(Cursor c,long now) {
            id=c.getString(0);kind=c.getString(1);candidate=c.getString(2);issue=c.getString(3);
            sequence=c.getString(4);inputHash=c.getString(5);authorityRef=c.getString(6);cursor=c.getString(7);
            if(c.getLong(8)<0||c.getLong(8)==Long.MAX_VALUE)throw new IllegalStateException("R967_TASK_GENERATION_EXHAUSTED");
            generation=c.getLong(8)+1;owner=PROCESS;leaseUntil=now+LEASE_MS;
            monotonicUntil=TimeSourceR811.monoNowMs()+LEASE_MS;
        }
    }
    public static Lease claim(SQLiteDatabase db,String kind,long now) {return claimFor(db,kind,null,now);}
    public static Lease claimFor(SQLiteDatabase db,String kind,String candidate,long now) {
        transaction(db);required(kind);
        reapExpiredOwnedCP4(db);
        String sql="SELECT t.task_id,t.kind,t.candidate_id,t.target_issue,t.stream_seq,t.input_hash,t.authority_ref,t.cursor,t.generation FROM "+TABLE+" t WHERE "+readyPredicate()+ (candidate==null?"":" AND t.candidate_id=?")+" ORDER BY t.updated_at,t.target_issue,t.task_id LIMIT 1";
        String[] args=candidate==null?readyArgsCP4(kind,now):new String[]{kind,Long.toString(now),PROCESS,candidate};
        try(Cursor c=db.rawQuery(sql,args)) {
            if(!c.moveToFirst())return null;
            Lease lease=new Lease(c,now);
            db.execSQL("UPDATE "+TABLE+" SET status='RUNNING',generation=?,lease_owner=?,lease_until=?,attempt_count=attempt_count+1,updated_at=? WHERE task_id=? AND generation=?",new Object[]{lease.generation,lease.owner,lease.leaseUntil,now,lease.id,lease.generation-1});
            if(changed(db)!=1)throw new IllegalStateException("R967_CLAIM_CONFLICT");
            ACTIVE_CP4.put(lease.id,lease);
            return lease;
        }
    }
    public static boolean owns(SQLiteDatabase db,Lease lease) {
        transaction(db);
        if(lease==null||!PROCESS.equals(lease.owner)||TimeSourceR811.monoNowMs()>=lease.monotonicUntil)return false;
        try(Cursor c=db.rawQuery("SELECT status,generation,lease_owner,lease_until,cursor FROM "+TABLE+" WHERE task_id=?",new String[]{lease.id})) {
            return c.moveToFirst()&&"RUNNING".equals(c.getString(0))&&c.getLong(1)==lease.generation
                    &&lease.owner.equals(c.getString(2))&&c.getLong(3)==lease.leaseUntil&&lease.cursor.equals(c.getString(4));
        }
    }
    /** Call after the actual owner's writes, in the SAME rollback-capable transaction. */
    public static void complete(SQLiteDatabase db,Lease lease,String result) {
        transaction(db);required(result);
        if(!owns(db,lease))throw new IllegalStateException("R967_STALE_TASK_LEASE");
        db.execSQL("UPDATE "+TABLE+" SET status='COMPLETED',lease_until=0,lease_owner='',result_hash=?,last_error='',updated_at=? WHERE task_id=? AND generation=? AND cursor=? AND status='RUNNING'",new Object[]{result,System.currentTimeMillis(),lease.id,lease.generation,lease.cursor});
        if(changed(db)!=1)throw new IllegalStateException("R967_COMPLETE_CAS");
        db.execSQL("DELETE FROM "+PIN+" WHERE task_id=?",new Object[]{lease.id});
        ACTIVE_CP4.remove(lease.id,lease);
    }
    /** Irrecoverable identity failure only. Caller owns candidate quarantine/finding in this same transaction. */
    public static boolean failTerminalCP12(SQLiteDatabase db,Lease lease,String reason) {
        transaction(db);required(reason);if(!owns(db,lease))return false;
        db.execSQL("UPDATE "+TABLE+" SET status='FAILED_TERMINAL',lease_owner='',lease_until=0,last_error=?,updated_at=? WHERE task_id=? AND generation=? AND cursor=? AND status='RUNNING'",new Object[]{reason,System.currentTimeMillis(),lease.id,lease.generation,lease.cursor});
        if(changed(db)!=1)throw new IllegalStateException("TRAIN_TERMINAL_CAS");
        db.execSQL("DELETE FROM "+PIN+" WHERE task_id=?",new Object[]{lease.id});ACTIVE_CP4.remove(lease.id,lease);return true;
    }
    /** A scheduler yield never becomes evidence of an algorithm's statistical failure. */
    public static boolean yield(SQLiteDatabase db,Lease lease,String cursor,String reason,long delayMs) {
        transaction(db);if(lease==null||cursor==null||reason==null)throw new IllegalArgumentException("R967_YIELD_INPUT");
        // Expired work may release its own generation, but cannot overwrite a successor lease.
        long now=System.currentTimeMillis();
        db.execSQL("UPDATE "+TABLE+" SET status='YIELDED',cursor=?,lease_until=0,lease_owner='',available_at=?,last_error=?,updated_at=? WHERE task_id=? AND generation=? AND status='RUNNING' AND lease_owner=? AND cursor=?",new Object[]{cursor,now+Math.max(0,Math.min(60000,delayMs)),reason,now,lease.id,lease.generation,lease.owner,lease.cursor});
        boolean updated=changed(db)==1;
        if(updated)ACTIVE_CP4.remove(lease.id,lease);
        return updated;
    }
    /** Resource HOLD keeps the exact durable cursor/pins without creating evidence. */
    public static boolean waitInputCP12(SQLiteDatabase db,Lease lease,String reason,long delayMs){
        transaction(db);if(lease==null||reason==null)throw new IllegalArgumentException("R967_WAIT_INPUT");
        if(!ResearchTaskQueueR967.yield(db,lease,lease.cursor,reason,delayMs))return false;
        db.execSQL("UPDATE "+TABLE+" SET status='WAIT_INPUT' WHERE task_id=? AND generation=? AND status='YIELDED' AND lease_owner='' AND cursor=? AND last_error=?",new Object[]{lease.id,lease.generation,lease.cursor,reason});
        if(changed(db)!=1)throw new IllegalStateException("R967_WAIT_INPUT_CAS");return true;
    }
    /** Schedule-only failure receipt after a projection transaction rolled back.
     * The failed claim's predecessor generation must still be present. A successor
     * claim, valid lease, changed cursor or terminal row can never be overwritten.
     * Pins/inputs remain intact; this is not training, settlement or statistical failure.
     */
    public static boolean deferRolledBack(SQLiteDatabase db,Lease attempted,String reason,long delayMs) {
        transaction(db);
        if(attempted==null||reason==null||reason.isEmpty())throw new IllegalArgumentException("R976_RETRY_INPUT");
        long now=System.currentTimeMillis();
        db.execSQL("UPDATE "+TABLE+" SET status='YIELDED',generation=?,lease_until=0,lease_owner='',available_at=?,last_error=?,attempt_count=attempt_count+1,updated_at=?"
                +" WHERE task_id=? AND generation=? AND cursor=? AND (status IN ('PENDING','YIELDED','WAIT_INPUT')"
                +" OR (status='RUNNING' AND lease_owner<>?))",
                new Object[]{attempted.generation,now+Math.max(250L,Math.min(60000L,delayMs)),reason,now,
                    attempted.id,attempted.generation-1,attempted.cursor,PROCESS});
        return changed(db)==1;
    }

    /**
     * One predicate shared by ready probes, round-robin selection and the CAS claim.
     * POST_TRAIN reviews an already committed immutable training receipt; MAINTENANCE
     * reviews a new desired watermark. Failure/expiry of an older review does not invalidate
     * a later independent receipt. This NEVER advances an algorithm training cursor or
     * changes the older failure status. Unknown task kinds retain strict predecessor rules.
     * SQL alias must be t. Bind kind, wall now, process owner in that order.
     */
    static String readyPredicate() {
        // R76: spell out the active domain so SQLite can use the existing partial
        // index instead of reading all completed task history on every worker wake.
        // The predecessor probes must use their matching partial indexes as well:
        // the wider covering index otherwise wins the planner's cost estimate and
        // scans thousands of completed predecessors before finding no blocker.
        return "t.status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL') AND t.kind=? AND ((t.status IN ('PENDING','YIELDED','WAIT_INPUT') AND t.available_at<=?)"
            +" OR (t.status='RUNNING' AND t.lease_owner<>?))"
            +" AND NOT EXISTS(SELECT 1 FROM "+TABLE+" e INDEXED BY idx_r975_task_unresolved WHERE e.kind=t.kind AND e.candidate_id=t.candidate_id"
            +" AND e.target_issue<t.target_issue AND e.status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL'))"
            +" AND (t.kind IN ('POST_TRAIN','MAINTENANCE') OR NOT EXISTS(SELECT 1 FROM "+TABLE
            +" e INDEXED BY idx_r975_task_terminal_block WHERE e.kind=t.kind AND e.candidate_id=t.candidate_id AND e.target_issue<t.target_issue"
            +" AND e.status IN ('EXPIRED','FAILED_TERMINAL')))";
    }
    public static boolean hasReady(SQLiteDatabase db,String kind,long now) {
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+TABLE+" t WHERE "+readyPredicate()+" LIMIT 1",readyArgsCP4(kind,now))) {return c.moveToFirst()||hasExpiredOwnedCP4(db,kind); }
    }
    static String[] readyArgsCP4(String kind,long now){return new String[]{kind,Long.toString(now),PROCESS};}
    private static boolean hasExpiredOwnedCP4(SQLiteDatabase db,String kind){
        long mono=TimeSourceR811.monoNowMs();
        try(Cursor c=db.rawQuery("SELECT task_id FROM "+TABLE+" WHERE kind=? AND status='RUNNING' AND lease_owner=?",new String[]{kind,PROCESS})){
            while(c.moveToNext()){Lease lease=ACTIVE_CP4.get(c.getString(0));if(lease==null||mono>=lease.monotonicUntil)return true;}
        }
        return false;
    }
    /** Same-process orphan detection is confined to active tasks, not terminal history. */
    static String localOrphanQueryR76(){
        return "SELECT task_id,generation FROM "+TABLE+" INDEXED BY idx_r975_task_unresolved"
                +" WHERE status='RUNNING' AND lease_owner=?"
                +" AND status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL')";
    }
    static void reapExpiredOwnedCP4(SQLiteDatabase db){
        transaction(db);long mono=TimeSourceR811.monoNowMs(),wall=System.currentTimeMillis();
        for(Lease lease:ACTIVE_CP4.values())if(mono>=lease.monotonicUntil){
            db.execSQL("UPDATE "+TABLE+" SET status='YIELDED',lease_owner='',lease_until=0,available_at=?,last_error='CP4_MONOTONIC_LEASE_EXPIRED',updated_at=? WHERE task_id=? AND generation=? AND status='RUNNING' AND lease_owner=?",
                new Object[]{wall,wall,lease.id,lease.generation,PROCESS});
            // Removal only after a committed transaction is ideal; on rollback the next claim's
            // foreign/same-session orphan scan below recovers the row without reviving the old token.
            ACTIVE_CP4.remove(lease.id,lease);
        }
        // Same-process rows lacking a live registered token can arise after a rollback/restart of a worker.
        // Do not use wall-clock lease_until to judge a still registered token.
        try(Cursor c=db.rawQuery(localOrphanQueryR76(),new String[]{PROCESS})){
            while(c.moveToNext())if(!ACTIVE_CP4.containsKey(c.getString(0)))
                db.execSQL("UPDATE "+TABLE+" SET status='YIELDED',lease_owner='',lease_until=0,available_at=?,last_error='CP4_NO_LIVE_LOCAL_TOKEN',updated_at=? WHERE task_id=? AND generation=? AND status='RUNNING' AND lease_owner=?",
                    new Object[]{wall,wall,c.getString(0),c.getLong(1),PROCESS});
        }
    }
    public static boolean terminal(String state){return "COMPLETED".equals(state)||"EXPIRED".equals(state)||"FAILED_TERMINAL".equals(state);}
    static int changed(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT changes()",null)){return c.moveToFirst()?c.getInt(0):0;}}
    private static String part(String value){return value.length()+":"+value;}
    private static void required(String value){if(value==null||value.isEmpty())throw new IllegalArgumentException("R967_EMPTY_TASK_FIELD");}
    private static void transaction(SQLiteDatabase db){if(db==null||!db.inTransaction())throw new IllegalStateException("R967_TASK_TRANSACTION_REQUIRED");}
    public static final class Capacity extends RuntimeException {public Capacity(String reason){super(reason);}}

public static int expirePostTrainBeforeCP12(SQLiteDatabase db,String cutoff,long deadline){
        transaction(db);if(!TargetIssueResolver.isValid(cutoff))throw new IllegalArgumentException("RETENTION_TASK_CUTOFF");
        reapExpiredOwnedCP4(db);int count=0;
        try(Cursor c=db.rawQuery("SELECT task_id,target_issue,input_hash,authority_ref,cursor,generation,status,lease_owner FROM "+TABLE+" WHERE kind='POST_TRAIN' AND length(target_issue)=12 AND target_issue<? AND status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL') ORDER BY target_issue,task_id LIMIT 16",new String[]{cutoff})){
            while(c.moveToNext()&&TimeSourceR811.monoNowMs()<deadline&&!RealtimePressureGateR811.hasActiveCriticalWork()){
                String id=c.getString(0);Lease live=ACTIVE_CP4.get(id);
                if(live!=null&&TimeSourceR811.monoNowMs()<live.monotonicUntil)continue;
                if(c.getLong(5)==Long.MAX_VALUE)throw new IllegalStateException("RETENTION_TASK_GENERATION_EXHAUSTED");
                String terminal=RetentionTaskExpiryCP12.record(db,id,c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getLong(5),cutoff);
                db.execSQL("UPDATE "+TABLE+" SET status='EXPIRED',generation=generation+1,lease_owner='',lease_until=0,available_at=0,last_error='POST_TRAIN_INPUT_EXPIRED_WITHOUT_REVIEW',result_hash=?,updated_at=? WHERE task_id=? AND generation=? AND cursor=? AND status=? AND lease_owner=?",new Object[]{terminal,System.currentTimeMillis(),id,c.getLong(5),c.getString(4),c.getString(6),c.getString(7)});
                if(changed(db)!=1)throw new IllegalStateException("RETENTION_TASK_EXPIRY_CAS");
                db.execSQL("DELETE FROM "+PIN+" WHERE task_id=?",new Object[]{id});ACTIVE_CP4.remove(id);count++;
            }
        }
        return count;
    }
}
