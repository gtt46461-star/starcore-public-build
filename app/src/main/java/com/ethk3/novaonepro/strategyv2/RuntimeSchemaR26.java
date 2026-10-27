package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;

/** Additive metadata migration through the existing database writer. */
public final class RuntimeSchemaR26 {
    private RuntimeSchemaR26() {}
    public static final String FEEDBACK = "CREATE TABLE IF NOT EXISTS r26_feedback(event_id TEXT PRIMARY KEY,issue_ordinal INTEGER NOT NULL UNIQUE CHECK(issue_ordinal>=0),source_hash TEXT NOT NULL,payload TEXT NOT NULL,ready_at INTEGER NOT NULL)";
    public static final String[] DDL = {
        FEEDBACK,
        "CREATE TABLE IF NOT EXISTS r26_session(session_id TEXT PRIMARY KEY,user_enabled INTEGER NOT NULL,boot_id TEXT NOT NULL,state TEXT NOT NULL,heartbeat INTEGER NOT NULL,actual_progress INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_task(task_id TEXT PRIMARY KEY,lane TEXT NOT NULL,mechanism TEXT NOT NULL,snapshot_hash TEXT NOT NULL DEFAULT '',cursor INTEGER NOT NULL DEFAULT 0,fence INTEGER NOT NULL DEFAULT 0,lease_until INTEGER NOT NULL DEFAULT 0,owner TEXT,status TEXT NOT NULL DEFAULT 'READY',blocked_reason TEXT,next_at INTEGER NOT NULL DEFAULT 0,last_progress INTEGER NOT NULL DEFAULT 0,steps INTEGER NOT NULL DEFAULT 0,last_cpu_ms INTEGER NOT NULL DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS r26_progress(receipt_id INTEGER PRIMARY KEY,task_id TEXT NOT NULL,fence INTEGER NOT NULL,cursor_before INTEGER NOT NULL,cursor_after INTEGER NOT NULL,cpu_ms INTEGER NOT NULL,blocked_reason TEXT,recorded_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_foundry(campaign_id TEXT PRIMARY KEY,snapshot_hash TEXT NOT NULL,source_through INTEGER NOT NULL,snapshot BLOB NOT NULL CHECK(length(snapshot)<=524288),status TEXT NOT NULL,created_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_candidate(campaign_id TEXT NOT NULL,policy_hash TEXT NOT NULL,canonical_text TEXT NOT NULL,cursor INTEGER NOT NULL DEFAULT 0,state BLOB,state_hash TEXT NOT NULL DEFAULT '',checkpoint_hash TEXT NOT NULL DEFAULT '',dev_n INTEGER NOT NULL DEFAULT 0,dev_k INTEGER NOT NULL DEFAULT 0,val_n INTEGER NOT NULL DEFAULT 0,val_k INTEGER NOT NULL DEFAULT 0,loss_sum REAL NOT NULL DEFAULT 0,status TEXT NOT NULL DEFAULT 'READY',parent TEXT,operation TEXT NOT NULL,PRIMARY KEY(campaign_id,policy_hash))",
        "CREATE TABLE IF NOT EXISTS r26_look(genome_key TEXT NOT NULL,cohort TEXT NOT NULL,evidence_digest TEXT NOT NULL,look INTEGER NOT NULL,family_alpha REAL NOT NULL,PRIMARY KEY(genome_key,cohort,evidence_digest))",
        "CREATE TABLE IF NOT EXISTS r26_cohort(genome_key TEXT PRIMARY KEY,from_sequence INTEGER NOT NULL,to_sequence INTEGER NOT NULL,incumbent TEXT NOT NULL,protocol TEXT NOT NULL,state TEXT NOT NULL DEFAULT 'ACCUMULATING')",
        "CREATE TABLE IF NOT EXISTS r26_certificate_protocol(certificate_id TEXT PRIMARY KEY,protocol TEXT NOT NULL,paired_lower REAL,evidence_digest TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_capability(mechanism TEXT PRIMARY KEY,kind TEXT NOT NULL,implementation TEXT NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL,last_progress INTEGER NOT NULL DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS r26_checkpoint_receipt(checkpoint_hash TEXT PRIMARY KEY,policy_hash TEXT NOT NULL,snapshot_hash TEXT NOT NULL,parent_hash TEXT NOT NULL,through_sample INTEGER NOT NULL,state_hash TEXT NOT NULL,scope TEXT NOT NULL,created_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_consumption(event_id TEXT NOT NULL,policy_hash TEXT NOT NULL,checkpoint_hash TEXT NOT NULL,scope TEXT NOT NULL,PRIMARY KEY(event_id,policy_hash))",
        "CREATE TABLE IF NOT EXISTS r26_evolution(attempt_id TEXT PRIMARY KEY,campaign_id TEXT NOT NULL,policy_hash TEXT NOT NULL,parent TEXT,operation TEXT NOT NULL,category TEXT NOT NULL,structure_hash TEXT NOT NULL,research_accepted INTEGER NOT NULL DEFAULT 0,behavior_novel INTEGER NOT NULL DEFAULT 0,prospective_qualified INTEGER NOT NULL DEFAULT 0,verified_improvement INTEGER NOT NULL DEFAULT 0)",
        "CREATE INDEX IF NOT EXISTS r26_task_dispatch ON r26_task(status,lane,next_at,lease_until)",
        "CREATE INDEX IF NOT EXISTS r26_feedback_order ON r26_feedback(issue_ordinal)"
    };
    public static boolean ready(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='r26_capability'",null)){return c.moveToFirst();}}
    public static void ensure(SQLiteDatabase db){if(!db.inTransaction())throw new IllegalStateException("R26_WRITER_REQUIRED");for(String sql:DDL)db.execSQL(sql);}
    /** Called only by SettlementEngine, inside its sole-owner terminal transaction. */
    public static void feedbackWithinTransaction(SQLiteDatabase db,Models.Draw draw){
        if(!db.inTransaction()||draw==null||!draw.isValid())throw new IllegalArgumentException("R26_FEEDBACK_OWNER");
        long ordinal=TargetIssueResolver.sequenceOrdinalSU1(draw.issue);String source=DrawHashR36.sha256V2(draw);
        StringBuilder payload=new StringBuilder(draw.issue);for(int p=1;p<=10;p++)payload.append(',').append(draw.getNumberAtPosition(p));
        String event=PolicyIdentityR26.sha256("R26_SETTLED|"+draw.issue);
        try(Cursor c=db.rawQuery("SELECT source_hash,payload FROM r26_feedback WHERE event_id=?",new String[]{event})){
            if(c.moveToFirst()){if(!source.equals(c.getString(0))||!payload.toString().equals(c.getString(1)))throw new IllegalStateException("R26_FEEDBACK_CONFLICT");return;}}
        db.execSQL("INSERT INTO r26_feedback VALUES(?,?,?,?,?)",new Object[]{event,ordinal,source,payload.toString(),System.currentTimeMillis()});
    }
}
