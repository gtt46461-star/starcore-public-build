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
        "CREATE TABLE IF NOT EXISTS r26_shadow_admission(policy_hash TEXT PRIMARY KEY,family_id TEXT NOT NULL,role TEXT NOT NULL CHECK(role IN ('BASE','CHALLENGER')),birth_sequence INTEGER NOT NULL,expires_sequence INTEGER NOT NULL,parent_hash TEXT NOT NULL,CHECK((role='BASE' AND expires_sequence=0) OR (role='CHALLENGER' AND expires_sequence>birth_sequence)))",
        "CREATE TRIGGER IF NOT EXISTS r26_shadow_admission_immutable BEFORE UPDATE ON r26_shadow_admission BEGIN SELECT RAISE(ABORT,'R26_SHADOW_ADMISSION_IMMUTABLE'); END",
        "CREATE TABLE IF NOT EXISTS r26_prepared_forecast(policy_hash TEXT PRIMARY KEY,checkpoint_hash TEXT NOT NULL,state_hash TEXT NOT NULL,input_through INTEGER NOT NULL,target_ordinal INTEGER NOT NULL,payload BLOB NOT NULL CHECK(length(payload)=437),payload_hash TEXT NOT NULL,prepared_at INTEGER NOT NULL,CHECK(target_ordinal=input_through+1))",
        "CREATE TABLE IF NOT EXISTS r26_policy_identity(policy_hash TEXT PRIMARY KEY,canonical_policy TEXT NOT NULL CHECK(length(canonical_policy)<=65536),genotype_text TEXT NOT NULL,numerical_abi TEXT NOT NULL,created_at INTEGER NOT NULL)",
        "CREATE TRIGGER IF NOT EXISTS r26_policy_identity_immutable BEFORE UPDATE ON r26_policy_identity BEGIN SELECT RAISE(ABORT,'R26_POLICY_IDENTITY_IMMUTABLE'); END",
        "CREATE TABLE IF NOT EXISTS r26_capability_source(singleton INTEGER PRIMARY KEY CHECK(singleton=1),numerical_abi TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_legacy_proposal(attempt_id TEXT PRIMARY KEY,campaign_id TEXT NOT NULL,generation INTEGER NOT NULL,policy_hash TEXT NOT NULL,canonical_text TEXT NOT NULL,parent_hash TEXT NOT NULL,parent_canonical TEXT NOT NULL,operation TEXT NOT NULL,category TEXT NOT NULL,created_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_prepared_policy(policy_hash TEXT PRIMARY KEY,campaign_id TEXT NOT NULL,snapshot_hash TEXT NOT NULL,family_id TEXT NOT NULL,canonical_text TEXT NOT NULL,state BLOB NOT NULL,state_hash TEXT NOT NULL,checkpoint_hash TEXT NOT NULL,genesis_ordinal INTEGER NOT NULL,through_ordinal INTEGER NOT NULL,numerical_abi TEXT NOT NULL,status TEXT NOT NULL,prepared_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_evidence_cursor(policy_hash TEXT PRIMARY KEY,protocol TEXT NOT NULL,through_ordinal INTEGER NOT NULL,partial BLOB NOT NULL,source_chain TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_qualification_result(policy_hash TEXT PRIMARY KEY,window_start INTEGER NOT NULL,window_end INTEGER NOT NULL,look INTEGER NOT NULL,evidence_digest TEXT NOT NULL,acts INTEGER NOT NULL,hits INTEGER NOT NULL,rate REAL NOT NULL,lower_bound REAL NOT NULL,coverage REAL NOT NULL,worst_block REAL NOT NULL,paired_lower REAL,fit_acts INTEGER NOT NULL,validation_acts INTEGER NOT NULL,ece REAL,reason TEXT NOT NULL,checked_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_evidence_block(policy_hash TEXT NOT NULL,block_index INTEGER NOT NULL,from_ordinal INTEGER NOT NULL,to_ordinal INTEGER NOT NULL,statistics BLOB NOT NULL CHECK(length(statistics)=2084),block_hash TEXT NOT NULL,PRIMARY KEY(policy_hash,block_index))",
        "CREATE TRIGGER IF NOT EXISTS r26_evidence_block_immutable BEFORE UPDATE ON r26_evidence_block BEGIN SELECT RAISE(ABORT,'R26_EVIDENCE_BLOCK_IMMUTABLE'); END",
        "CREATE TABLE IF NOT EXISTS r26_work_total(task_id TEXT PRIMARY KEY,completed INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_legacy_campaign(campaign_id TEXT PRIMARY KEY,snapshot_hash TEXT NOT NULL,snapshot BLOB NOT NULL CHECK(length(snapshot)<=524288),source_through INTEGER NOT NULL,source_first INTEGER NOT NULL,numerical_abi TEXT NOT NULL,generation INTEGER NOT NULL DEFAULT 0,phase TEXT NOT NULL DEFAULT 'REGISTERED',committed_samples INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL DEFAULT 'RUNNING',updated_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_member_replay(campaign_id TEXT NOT NULL,cache_key TEXT NOT NULL,member_text TEXT NOT NULL,calibrated INTEGER NOT NULL,total_samples INTEGER NOT NULL,cursor INTEGER NOT NULL DEFAULT 0,state BLOB,state_hash TEXT NOT NULL DEFAULT '',checkpoint_hash TEXT NOT NULL DEFAULT '',status TEXT NOT NULL DEFAULT 'RUNNING',numerical_abi TEXT NOT NULL,PRIMARY KEY(campaign_id,cache_key))",
        "CREATE TABLE IF NOT EXISTS r26_replay_output_chunk(campaign_id TEXT NOT NULL,cache_key TEXT NOT NULL,chunk_index INTEGER NOT NULL,from_sample INTEGER NOT NULL,to_sample INTEGER NOT NULL,artifact_ref BLOB NOT NULL,PRIMARY KEY(campaign_id,cache_key,chunk_index))",
        "CREATE TABLE IF NOT EXISTS r26_checkpoint_artifact(checkpoint_hash TEXT PRIMARY KEY,artifact_hash TEXT NOT NULL,policy_hash TEXT NOT NULL,source_hash TEXT NOT NULL,parent_hash TEXT NOT NULL,trained_through INTEGER NOT NULL,created_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_storage_measurement(kind TEXT PRIMARY KEY,bytes INTEGER NOT NULL,cap_bytes INTEGER NOT NULL,last_removed_bytes INTEGER NOT NULL,measured_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_legacy_runtime(singleton INTEGER PRIMARY KEY CHECK(singleton=1),roster_hash TEXT NOT NULL,snapshot_hash TEXT NOT NULL,snapshot BLOB NOT NULL CHECK(length(snapshot)<=524288),source_through INTEGER NOT NULL,genesis_ordinal INTEGER NOT NULL,through_ordinal INTEGER NOT NULL,cursor INTEGER NOT NULL,state BLOB NOT NULL,state_hash TEXT NOT NULL,checkpoint_hash TEXT NOT NULL,parent_hash TEXT NOT NULL,numerical_abi TEXT NOT NULL,status TEXT NOT NULL,updated_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_expert_experiment(experiment_id TEXT PRIMARY KEY,expert_id TEXT NOT NULL,campaign_id TEXT NOT NULL,cursor INTEGER NOT NULL DEFAULT 0,state BLOB NOT NULL CHECK(length(state)<=8388608),state_hash TEXT NOT NULL,checkpoint_hash TEXT NOT NULL,parent_hash TEXT NOT NULL DEFAULT '',work_nanos INTEGER NOT NULL DEFAULT 0,numerical_abi TEXT NOT NULL,proposal_json TEXT NOT NULL,result_json TEXT NOT NULL DEFAULT '{}',status TEXT NOT NULL DEFAULT 'RUNNING',created_at INTEGER NOT NULL,UNIQUE(expert_id,campaign_id))",
        "CREATE TABLE IF NOT EXISTS r26_expert_audit(audit_id TEXT PRIMARY KEY,expert_id TEXT NOT NULL,sequence_ordinal INTEGER NOT NULL,receipt_json TEXT NOT NULL,recorded_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_retention_total(kind TEXT PRIMARY KEY,folded INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_session(session_id TEXT PRIMARY KEY,user_enabled INTEGER NOT NULL,boot_id TEXT NOT NULL,state TEXT NOT NULL,heartbeat INTEGER NOT NULL,actual_progress INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_task(task_id TEXT PRIMARY KEY,lane TEXT NOT NULL,mechanism TEXT NOT NULL,snapshot_hash TEXT NOT NULL DEFAULT '',cursor INTEGER NOT NULL DEFAULT 0,fence INTEGER NOT NULL DEFAULT 0,lease_until INTEGER NOT NULL DEFAULT 0,owner TEXT,status TEXT NOT NULL DEFAULT 'READY',blocked_reason TEXT,next_at INTEGER NOT NULL DEFAULT 0,last_progress INTEGER NOT NULL DEFAULT 0,steps INTEGER NOT NULL DEFAULT 0,last_cpu_ms INTEGER NOT NULL DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS r26_progress(receipt_id INTEGER PRIMARY KEY,task_id TEXT NOT NULL,fence INTEGER NOT NULL,cursor_before INTEGER NOT NULL,cursor_after INTEGER NOT NULL,cpu_ms INTEGER NOT NULL,blocked_reason TEXT,recorded_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_foundry(campaign_id TEXT PRIMARY KEY,snapshot_hash TEXT NOT NULL,source_through INTEGER NOT NULL,snapshot BLOB NOT NULL CHECK(length(snapshot)<=524288),status TEXT NOT NULL,created_at INTEGER NOT NULL,numerical_abi TEXT NOT NULL DEFAULT '',source_first INTEGER NOT NULL DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS r26_candidate(campaign_id TEXT NOT NULL,policy_hash TEXT NOT NULL,canonical_text TEXT NOT NULL,cursor INTEGER NOT NULL DEFAULT 0,state BLOB,state_hash TEXT NOT NULL DEFAULT '',checkpoint_hash TEXT NOT NULL DEFAULT '',dev_n INTEGER NOT NULL DEFAULT 0,dev_k INTEGER NOT NULL DEFAULT 0,val_n INTEGER NOT NULL DEFAULT 0,val_k INTEGER NOT NULL DEFAULT 0,loss_sum REAL NOT NULL DEFAULT 0,status TEXT NOT NULL DEFAULT 'READY',parent TEXT,operation TEXT NOT NULL,PRIMARY KEY(campaign_id,policy_hash))",
        "CREATE TABLE IF NOT EXISTS r26_look(genome_key TEXT NOT NULL,cohort TEXT NOT NULL,evidence_digest TEXT NOT NULL,look INTEGER NOT NULL,family_alpha REAL NOT NULL,PRIMARY KEY(genome_key,cohort,evidence_digest))",
        "CREATE TABLE IF NOT EXISTS r26_cohort(genome_key TEXT PRIMARY KEY,from_sequence INTEGER NOT NULL,to_sequence INTEGER NOT NULL,incumbent TEXT NOT NULL,protocol TEXT NOT NULL,state TEXT NOT NULL DEFAULT 'ACCUMULATING',family_alpha REAL NOT NULL DEFAULT 0,checked_sequence INTEGER NOT NULL DEFAULT -1)",
        "CREATE TABLE IF NOT EXISTS r26_certificate_protocol(certificate_id TEXT PRIMARY KEY,protocol TEXT NOT NULL,paired_lower REAL,evidence_digest TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_capability(mechanism TEXT PRIMARY KEY,kind TEXT NOT NULL,implementation TEXT NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL,last_progress INTEGER NOT NULL DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS r26_checkpoint_receipt(checkpoint_hash TEXT PRIMARY KEY,policy_hash TEXT NOT NULL,snapshot_hash TEXT NOT NULL,parent_hash TEXT NOT NULL,through_sample INTEGER NOT NULL,state_hash TEXT NOT NULL,scope TEXT NOT NULL,created_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_consumption(event_id TEXT NOT NULL,policy_hash TEXT NOT NULL,checkpoint_hash TEXT NOT NULL,scope TEXT NOT NULL,PRIMARY KEY(event_id,policy_hash))",
        "CREATE TABLE IF NOT EXISTS r26_evolution(attempt_id TEXT PRIMARY KEY,campaign_id TEXT NOT NULL,policy_hash TEXT NOT NULL,parent TEXT,operation TEXT NOT NULL,category TEXT NOT NULL,structure_hash TEXT NOT NULL,research_accepted INTEGER NOT NULL DEFAULT 0,behavior_novel INTEGER NOT NULL DEFAULT 0,prospective_qualified INTEGER NOT NULL DEFAULT 0,verified_improvement INTEGER NOT NULL DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS r26_live_model(policy_hash TEXT PRIMARY KEY,canonical_text TEXT NOT NULL,state BLOB NOT NULL CHECK(length(state)<=4194304),state_hash TEXT NOT NULL,checkpoint_hash TEXT NOT NULL,parent_hash TEXT NOT NULL,through_ordinal INTEGER NOT NULL,genesis_ordinal INTEGER NOT NULL,born_ordinal INTEGER NOT NULL,status TEXT NOT NULL DEFAULT 'ACTIVE',updated_at INTEGER NOT NULL,family_id TEXT NOT NULL DEFAULT '')",
        "CREATE TABLE IF NOT EXISTS r26_frozen(policy_hash TEXT NOT NULL,target_ordinal INTEGER NOT NULL,input_through INTEGER NOT NULL,checkpoint_hash TEXT NOT NULL,state_hash TEXT NOT NULL,source_hash TEXT NOT NULL,source_generation INTEGER NOT NULL,frozen_at INTEGER NOT NULL,deadline_at INTEGER NOT NULL,probability_table BLOB NOT NULL CHECK(length(probability_table)=400),PRIMARY KEY(policy_hash,target_ordinal),CHECK(input_through<target_ordinal AND frozen_at<deadline_at))",
        "CREATE TABLE IF NOT EXISTS r26_live_failure(policy_hash TEXT NOT NULL,target_ordinal INTEGER NOT NULL,reason TEXT NOT NULL,recorded_at INTEGER NOT NULL,PRIMARY KEY(policy_hash,target_ordinal))",
        "CREATE TABLE IF NOT EXISTS r26_gap_receipt(policy_hash TEXT NOT NULL,after_ordinal INTEGER NOT NULL,before_ordinal INTEGER NOT NULL,missing_count INTEGER NOT NULL,reason TEXT NOT NULL,recorded_at INTEGER NOT NULL,PRIMARY KEY(policy_hash,after_ordinal,before_ordinal))",
        "CREATE TABLE IF NOT EXISTS r26_certificate_revocation(certificate_id TEXT PRIMARY KEY,reason TEXT NOT NULL,revoked_at INTEGER NOT NULL)",
        "CREATE TRIGGER IF NOT EXISTS r26_frozen_immutable BEFORE UPDATE ON r26_frozen BEGIN SELECT RAISE(ABORT,'R26_FROZEN_IMMUTABLE'); END",
        "CREATE INDEX IF NOT EXISTS r26_live_order ON r26_live_model(status,updated_at)",
        "CREATE INDEX IF NOT EXISTS r26_frozen_order ON r26_frozen(target_ordinal)",
        "CREATE INDEX IF NOT EXISTS r26_task_dispatch ON r26_task(status,lane,next_at,lease_until)",
        "CREATE INDEX IF NOT EXISTS r26_feedback_order ON r26_feedback(issue_ordinal)"
    };
    public static boolean ready(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('r26_capability','r26_live_model','r26_certificate_revocation','r26_legacy_runtime','r26_checkpoint_artifact','r26_member_replay','r26_legacy_campaign','r26_work_total','r26_prepared_policy','r26_evidence_cursor','r26_evidence_block','r26_qualification_result','r26_prepared_forecast','r26_shadow_admission')",null)){return c.moveToFirst()&&c.getInt(0)==14;}}
    public static void ensure(SQLiteDatabase db){if(!db.inTransaction())throw new IllegalStateException("R26_WRITER_REQUIRED");for(String sql:DDL)db.execSQL(sql);
        column(db,"r26_task","last_served","INTEGER NOT NULL DEFAULT 0");
        column(db,"r26_cohort","family_alpha","REAL NOT NULL DEFAULT 0");column(db,"r26_cohort","checked_sequence","INTEGER NOT NULL DEFAULT -1");
        column(db,"r26_foundry","numerical_abi","TEXT NOT NULL DEFAULT ''");column(db,"r26_foundry","source_first","INTEGER NOT NULL DEFAULT 0");
        column(db,"r26_live_model","family_id","TEXT NOT NULL DEFAULT ''");
        column(db,"r26_candidate","state_hash","TEXT NOT NULL DEFAULT ''");column(db,"r26_candidate","checkpoint_hash","TEXT NOT NULL DEFAULT ''");
        // Old engineering checkpoints remain preserved. Their missing hash is explicitly blocked.
    }
    public static boolean bindCapabilitySource(SQLiteDatabase db,String abi){
        if(!db.inTransaction()||abi==null||!abi.matches("[0-9a-f]{64}"))throw new IllegalStateException("R26_CAPABILITY_SOURCE_OWNER");
        try(Cursor c=db.rawQuery("SELECT numerical_abi FROM r26_capability_source WHERE singleton=1",null)){if(c.moveToFirst()&&abi.equals(c.getString(0)))return false;}
        // Historical progress remains in receipts/checkpoints. A different numerical program
        // must earn its current capability service record; learner weights are not modified.
        db.execSQL("UPDATE r26_capability SET last_progress=0");db.execSQL("INSERT OR REPLACE INTO r26_capability_source VALUES(1,?)",new Object[]{abi});return true;
    }
    private static void column(SQLiteDatabase db,String table,String name,String type){try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext())if(name.equals(c.getString(1)))return;}db.execSQL("ALTER TABLE "+table+" ADD COLUMN "+name+" "+type);}

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
