package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;

/** Additive metadata migration through the existing database writer. */
public final class RuntimeSchemaR26 {
    private RuntimeSchemaR26() {}
    public static final int R31_FEEDBACK_REVISION=1;
    public static final String R31_MODEL_CONSUMER_PROTOCOL="R26_CHECKPOINT_RECEIPT_V1";
    public static final String[] R31_FEEDBACK_DDL={
        "CREATE TABLE IF NOT EXISTS r31_feedback_event(event_id TEXT PRIMARY KEY,kind TEXT NOT NULL CHECK(kind IN ('DRAW_LABEL_REVEALED','FROZEN_RESULT')),issue TEXT NOT NULL,issue_ordinal INTEGER NOT NULL,source_hash TEXT NOT NULL,canonical_hash TEXT NOT NULL,legacy_event_id TEXT NOT NULL,payload TEXT NOT NULL,payload_hash TEXT NOT NULL,producer TEXT NOT NULL,created_at INTEGER NOT NULL,UNIQUE(kind,issue,source_hash,canonical_hash))",
        "CREATE INDEX IF NOT EXISTS r31_feedback_event_order ON r31_feedback_event(kind,issue_ordinal,event_id)",
        "CREATE TABLE IF NOT EXISTS r31_feedback_receipt(consumer_version TEXT NOT NULL,event_id TEXT NOT NULL,source_hash TEXT NOT NULL,checkpoint_hash TEXT NOT NULL,artifact_hash TEXT NOT NULL,policy_hash TEXT NOT NULL,scope TEXT NOT NULL,committed_at INTEGER NOT NULL,PRIMARY KEY(consumer_version,event_id))",
        "CREATE INDEX IF NOT EXISTS r31_feedback_receipt_event ON r31_feedback_receipt(event_id,consumer_version)",
        "CREATE INDEX IF NOT EXISTS r31_feedback_source ON r26_feedback(source_hash)",
        "CREATE TABLE IF NOT EXISTS r31_frozen_feedback_effect(consumer_version TEXT PRIMARY KEY,events INTEGER NOT NULL,calibrated_events INTEGER NOT NULL,brier_sum REAL NOT NULL,residual_sum REAL NOT NULL,last_ordinal INTEGER NOT NULL,state_hash TEXT NOT NULL,updated_at INTEGER NOT NULL)",
        "CREATE TRIGGER IF NOT EXISTS r31_feedback_event_immutable BEFORE UPDATE ON r31_feedback_event BEGIN SELECT RAISE(ABORT,'R31_FEEDBACK_EVENT_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_feedback_event_no_replace BEFORE INSERT ON r31_feedback_event WHEN EXISTS(SELECT 1 FROM r31_feedback_event WHERE event_id=NEW.event_id OR (kind=NEW.kind AND issue=NEW.issue AND source_hash=NEW.source_hash AND canonical_hash=NEW.canonical_hash)) BEGIN SELECT RAISE(ABORT,'R31_FEEDBACK_EVENT_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_feedback_event_no_delete BEFORE DELETE ON r31_feedback_event BEGIN SELECT RAISE(ABORT,'R31_FEEDBACK_EVENT_DELETE_REQUIRES_VERSIONED_RETENTION_PROTOCOL'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_feedback_model_receipt AFTER INSERT ON r26_consumption BEGIN INSERT OR IGNORE INTO r31_feedback_receipt(consumer_version,event_id,source_hash,checkpoint_hash,artifact_hash,policy_hash,scope,committed_at) SELECT NEW.policy_hash||'|R26_CHECKPOINT_RECEIPT_V1',e.event_id,e.source_hash,NEW.checkpoint_hash,a.artifact_hash,NEW.policy_hash,NEW.scope,a.created_at FROM r31_feedback_event e JOIN r26_feedback f ON f.event_id=NEW.event_id AND f.source_hash=e.source_hash JOIN r26_checkpoint_artifact a ON a.checkpoint_hash=NEW.checkpoint_hash AND a.trained_through>=e.issue_ordinal WHERE e.kind='DRAW_LABEL_REVEALED' AND e.legacy_event_id=NEW.event_id; END"
    };
    /** Called only from the versioned SchemaDomain journal after the R26 base tables exist. */
    public static void ensureR31Feedback(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R31_FEEDBACK_MIGRATION_OWNER_REQUIRED");
        for(String sql:R31_FEEDBACK_DDL)db.execSQL(sql);
        try(Cursor columns=db.rawQuery("SELECT consumer_version,event_id,source_hash,checkpoint_hash,artifact_hash,policy_hash,scope,committed_at FROM r31_feedback_receipt LIMIT 0",null)){
            if(columns.getColumnCount()!=8)throw new IllegalStateException("R31_FEEDBACK_SCHEMA_CONFLICT");
        }
    }
    public static final String FEEDBACK = "CREATE TABLE IF NOT EXISTS r26_feedback(event_id TEXT PRIMARY KEY,issue_ordinal INTEGER NOT NULL UNIQUE CHECK(issue_ordinal>=0),source_hash TEXT NOT NULL,payload TEXT NOT NULL,ready_at INTEGER NOT NULL)";
    public static final String[] DDL = {
        FEEDBACK,
        "CREATE TABLE IF NOT EXISTS r26_training_step(policy_hash TEXT NOT NULL,campaign_id TEXT NOT NULL,sample_ordinal INTEGER NOT NULL,sample_issue TEXT NOT NULL,member_key TEXT NOT NULL,source_hash TEXT NOT NULL,source_kind TEXT NOT NULL,kernel_update INTEGER NOT NULL CHECK(kernel_update>0),loss_before REAL NOT NULL,loss_after REAL NOT NULL,gradient_norm REAL NOT NULL CHECK(gradient_norm>=0),parameter_delta_norm REAL NOT NULL CHECK(parameter_delta_norm>=0),input_hash TEXT NOT NULL,objective TEXT NOT NULL,before_state_hash TEXT NOT NULL,after_state_hash TEXT NOT NULL,gap_reset INTEGER NOT NULL CHECK(gap_reset IN (0,1)),optimizer TEXT NOT NULL,scope TEXT NOT NULL,batch_checkpoint TEXT NOT NULL,receipt_hash TEXT NOT NULL CHECK(length(receipt_hash)=64),recorded_at INTEGER NOT NULL,PRIMARY KEY(policy_hash,campaign_id,sample_ordinal,member_key))",
        "CREATE TRIGGER IF NOT EXISTS r26_training_step_no_update BEFORE UPDATE ON r26_training_step BEGIN SELECT RAISE(ABORT,'R26_TRAINING_STEP_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r26_training_step_no_replace BEFORE INSERT ON r26_training_step WHEN EXISTS(SELECT 1 FROM r26_training_step WHERE policy_hash=NEW.policy_hash AND campaign_id=NEW.campaign_id AND sample_ordinal=NEW.sample_ordinal AND member_key=NEW.member_key) BEGIN SELECT RAISE(ABORT,'R26_TRAINING_STEP_IMMUTABLE'); END",
        "CREATE TABLE IF NOT EXISTS r26_statistic_training_step(policy_hash TEXT NOT NULL,campaign_id TEXT NOT NULL,sample_ordinal INTEGER NOT NULL,sample_issue TEXT NOT NULL,member_key TEXT NOT NULL,source_hash TEXT NOT NULL,source_kind TEXT NOT NULL,kernel_update INTEGER NOT NULL CHECK(kernel_update>0),loss_before REAL NOT NULL,loss_after REAL NOT NULL,statistic_delta_norm REAL NOT NULL CHECK(statistic_delta_norm>=0),parameter_delta_norm REAL NOT NULL CHECK(parameter_delta_norm>=0),input_hash TEXT NOT NULL,objective TEXT NOT NULL,statistic_name TEXT NOT NULL,before_state_hash TEXT NOT NULL,after_state_hash TEXT NOT NULL,gap_reset INTEGER NOT NULL CHECK(gap_reset IN (0,1)),optimizer TEXT NOT NULL,scope TEXT NOT NULL,batch_checkpoint TEXT NOT NULL,receipt_hash TEXT NOT NULL CHECK(length(receipt_hash)=64),recorded_at INTEGER NOT NULL,PRIMARY KEY(policy_hash,campaign_id,sample_ordinal,member_key))",
        "CREATE TRIGGER IF NOT EXISTS r26_statistic_training_step_no_update BEFORE UPDATE ON r26_statistic_training_step BEGIN SELECT RAISE(ABORT,'R26_STATISTICAL_TRAINING_STEP_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r26_statistic_training_step_no_replace BEFORE INSERT ON r26_statistic_training_step WHEN EXISTS(SELECT 1 FROM r26_statistic_training_step WHERE policy_hash=NEW.policy_hash AND campaign_id=NEW.campaign_id AND sample_ordinal=NEW.sample_ordinal AND member_key=NEW.member_key) BEGIN SELECT RAISE(ABORT,'R26_STATISTICAL_TRAINING_STEP_IMMUTABLE'); END",
        "CREATE TABLE IF NOT EXISTS r26_warm_initialization(policy_hash TEXT PRIMARY KEY,parent_policy TEXT NOT NULL,parent_checkpoint TEXT NOT NULL,parent_state_hash TEXT NOT NULL,parent_trained_through INTEGER NOT NULL,parent_exposed_through INTEGER NOT NULL,initializer_hash TEXT NOT NULL,state BLOB NOT NULL CHECK(length(state)<=4194304),child_campaign TEXT NOT NULL,seed_checkpoint TEXT NOT NULL,numerical_abi TEXT NOT NULL,created_at INTEGER NOT NULL)",
        "CREATE TRIGGER IF NOT EXISTS r26_warm_initialization_no_update BEFORE UPDATE ON r26_warm_initialization BEGIN SELECT RAISE(ABORT,'R26_WARM_INITIALIZER_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r26_warm_initialization_no_replace BEFORE INSERT ON r26_warm_initialization WHEN EXISTS(SELECT 1 FROM r26_warm_initialization WHERE policy_hash=NEW.policy_hash) BEGIN SELECT RAISE(ABORT,'R26_WARM_INITIALIZER_IMMUTABLE'); END",
        "CREATE TABLE IF NOT EXISTS r26_service_total(session_id TEXT PRIMARY KEY,source_commit TEXT NOT NULL,numerical_abi TEXT NOT NULL,api_level INTEGER NOT NULL,device_model TEXT NOT NULL,slices INTEGER NOT NULL DEFAULT 0 CHECK(slices>=0),cpu_ms INTEGER NOT NULL DEFAULT 0 CHECK(cpu_ms>=0),wall_ms INTEGER NOT NULL DEFAULT 0 CHECK(wall_ms>=0),capacity_failures INTEGER NOT NULL DEFAULT 0 CHECK(capacity_failures>=0))",
        "CREATE TABLE IF NOT EXISTS r26_shadow_admission(policy_hash TEXT PRIMARY KEY,family_id TEXT NOT NULL,role TEXT NOT NULL CHECK(role IN ('BASE','CHALLENGER')),birth_sequence INTEGER NOT NULL,expires_sequence INTEGER NOT NULL,parent_hash TEXT NOT NULL,CHECK((role='BASE' AND expires_sequence=0) OR (role='CHALLENGER' AND expires_sequence>birth_sequence)))",
        "CREATE TRIGGER IF NOT EXISTS r26_shadow_admission_immutable BEFORE UPDATE ON r26_shadow_admission BEGIN SELECT RAISE(ABORT,'R26_SHADOW_ADMISSION_IMMUTABLE'); END",
        "CREATE TABLE IF NOT EXISTS r26_incumbent_frozen(target_ordinal INTEGER PRIMARY KEY,input_through INTEGER NOT NULL,source_hash TEXT NOT NULL,source_generation INTEGER NOT NULL,numerical_abi TEXT NOT NULL,frozen_at INTEGER NOT NULL,deadline_at INTEGER NOT NULL,preference_table BLOB NOT NULL CHECK(length(preference_table)=400),payload_hash TEXT NOT NULL,CHECK(input_through=target_ordinal-1 AND frozen_at<deadline_at))",
        "CREATE TRIGGER IF NOT EXISTS r26_incumbent_frozen_no_replace BEFORE INSERT ON r26_incumbent_frozen WHEN EXISTS(SELECT 1 FROM r26_incumbent_frozen WHERE target_ordinal=NEW.target_ordinal) BEGIN SELECT RAISE(ABORT,'R26_INCUMBENT_FROZEN_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r26_incumbent_frozen_immutable BEFORE UPDATE ON r26_incumbent_frozen BEGIN SELECT RAISE(ABORT,'R26_INCUMBENT_FROZEN_IMMUTABLE'); END",
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
        "CREATE TRIGGER IF NOT EXISTS r26_cohort_birth_immutable BEFORE UPDATE OF genome_key,from_sequence,to_sequence,incumbent,protocol,family_alpha ON r26_cohort BEGIN SELECT RAISE(ABORT,'R26_COHORT_BIRTH_IMMUTABLE'); END",
        "CREATE TABLE IF NOT EXISTS r26_certificate_protocol(certificate_id TEXT PRIMARY KEY,protocol TEXT NOT NULL,paired_lower REAL,evidence_digest TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_capability(mechanism TEXT PRIMARY KEY,kind TEXT NOT NULL,implementation TEXT NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL,last_progress INTEGER NOT NULL DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS r26_checkpoint_receipt(checkpoint_hash TEXT PRIMARY KEY,policy_hash TEXT NOT NULL,snapshot_hash TEXT NOT NULL,parent_hash TEXT NOT NULL,through_sample INTEGER NOT NULL,state_hash TEXT NOT NULL,scope TEXT NOT NULL,created_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r26_consumption(event_id TEXT NOT NULL,policy_hash TEXT NOT NULL,checkpoint_hash TEXT NOT NULL,scope TEXT NOT NULL,PRIMARY KEY(event_id,policy_hash))",
        "CREATE TABLE IF NOT EXISTS r26_failure_research_cursor(parent_policy TEXT PRIMARY KEY,evidence_digest TEXT NOT NULL,window_end INTEGER NOT NULL,campaign_id TEXT NOT NULL,proposal_json TEXT NOT NULL)",
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
        column(db,"r26_candidate","service_cpu_ms","INTEGER NOT NULL DEFAULT 0");column(db,"r26_candidate","last_served","INTEGER NOT NULL DEFAULT 0");
        column(db,"r26_candidate","state_hash","TEXT NOT NULL DEFAULT ''");column(db,"r26_candidate","checkpoint_hash","TEXT NOT NULL DEFAULT ''");
        SchemaDomainCoordinatorR926.migrateR31Feedback(db);
        // Old engineering checkpoints remain preserved. Their missing hash is explicitly blocked.
    }
    public static boolean bindCapabilitySource(SQLiteDatabase db,String abi){
        if(!db.inTransaction()||abi==null||!abi.matches("[0-9a-f]{64}"))throw new IllegalStateException("R26_CAPABILITY_SOURCE_OWNER");
        try(Cursor c=db.rawQuery("SELECT numerical_abi FROM r26_capability_source WHERE singleton=1",null)){if(c.moveToFirst()&&abi.equals(c.getString(0)))return false;}
        // Historical progress remains in receipts/checkpoints. A different numerical program
        // must earn its current capability service record; learner weights are not modified.
        java.util.List<String> incompatible=new java.util.ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT policy_hash,canonical_text FROM r26_live_model WHERE status='ACTIVE'",null)){while(c.moveToNext())if(!CurrentPolicyR26.matches(c.getString(1),c.getString(0)))incompatible.add(c.getString(0));}
        boolean league=table(db,"su2_league"),genomes=table(db,"su2_model_genome"),certificates=table(db,"su2_certificate");long now=System.currentTimeMillis();
        for(String policy:incompatible){
            // An obsolete executable identity cannot occupy a current shadow slot indefinitely.
            // This is an ABI boundary, never a quality comparison or an alpha refund.
            db.execSQL("UPDATE r26_live_model SET status='BLOCKED_NUMERICAL_ABI_PRESERVED' WHERE policy_hash=? AND status='ACTIVE'",new Object[]{policy});
            db.execSQL("UPDATE r26_cohort SET state='NUMERICAL_ABI_ARCHIVED_NO_EVIDENCE_TRANSFER' WHERE genome_key=?",new Object[]{policy});
            if(league)db.execSQL("UPDATE su2_league SET state='RETIRED' WHERE genome_key=?",new Object[]{policy});
            if(genomes)db.execSQL("UPDATE su2_model_genome SET state='RETIRED' WHERE genome_key=?",new Object[]{policy});
            if(certificates)db.execSQL("INSERT OR IGNORE INTO r26_certificate_revocation SELECT certificate_id,'NUMERICAL_ABI_CHANGED_WEIGHTS_PRESERVED',? FROM su2_certificate WHERE genome_key=?",new Object[]{now,policy});
        }
        db.execSQL("UPDATE r26_capability SET last_progress=0");db.execSQL("INSERT OR REPLACE INTO r26_capability_source VALUES(1,?)",new Object[]{abi});return true;
    }
    private static boolean table(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}}
    private static void column(SQLiteDatabase db,String table,String name,String type){try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext())if(name.equals(c.getString(1)))return;}db.execSQL("ALTER TABLE "+table+" ADD COLUMN "+name+" "+type);}

    /** Confirmed draw outbox producer. It has no dependency on a prediction or settlement terminal. */
    public static void feedbackWithinTransaction(SQLiteDatabase db,Models.Draw draw){
        if(!db.inTransaction()||draw==null||!draw.isValid())throw new IllegalArgumentException("R26_FEEDBACK_OWNER");
        long ordinal=TargetIssueResolver.sequenceOrdinalSU1(draw.issue);String source=DrawHashR36.sha256V2(draw);
        StringBuilder payload=new StringBuilder(draw.issue);for(int p=1;p<=10;p++)payload.append(',').append(draw.getNumberAtPosition(p));
        String event=PolicyIdentityR26.sha256("R26_SETTLED|"+draw.issue);
        try(Cursor c=db.rawQuery("SELECT source_hash,payload FROM r26_feedback WHERE event_id=?",new String[]{event})){
            if(c.moveToFirst()){if(!source.equals(c.getString(0))||!payload.toString().equals(c.getString(1)))throw new IllegalStateException("R26_FEEDBACK_CONFLICT");return;}}
        db.execSQL("INSERT INTO r26_feedback VALUES(?,?,?,?,?)",new Object[]{event,ordinal,source,payload.toString(),System.currentTimeMillis()});
    }

    /** Bounded repair/typed delivery on the existing fenced research owner. Bridge work is not model training. */
    public static long reconcileFeedbackR31(DbStore store,RuntimeCoordinatorR26.Claim claim,BudgetTokenSU1 budget){
        budget.checkpointOrYield();
        final java.util.List<Models.Draw> missing=new java.util.ArrayList<>();
        try(Cursor rows=store.readable().rawQuery("SELECT d.issue FROM draws d JOIN "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t ON t.target_issue=d.issue LEFT JOIN r26_feedback f ON f.source_hash=d.draw_hash_v2 WHERE d.dirty=0 AND f.event_id IS NULL ORDER BY d.issue LIMIT 8",null)){
            while(rows.moveToNext()){Models.Draw draw=store.getDrawByIssue(rows.getString(0));if(draw!=null&&draw.isValid())missing.add(draw);}
        }
        budget.checkpointOrYield();
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_FEEDBACK_RECONCILE"),db->{
                    RuntimeCoordinatorR26.requireFence(db,claim);long work=0;
                    for(Models.Draw draw:missing){if(!confirmedDrawR31(db,draw))continue;feedbackWithinTransaction(db,draw);work++;}
                    work+=typedDrawEventsR31(db,8);work+=typedFrozenEventsR31(db,8);work+=bridgeModelReceiptsR31(db,16);work+=consumeFrozenEffectsR31(db,8);
                    return work;
                });
    }
    private static long typedDrawEventsR31(SQLiteDatabase db,int limit){
        java.util.List<String[]> prepared=new java.util.ArrayList<>();
        try(Cursor rows=db.rawQuery("SELECT f.event_id,f.issue_ordinal,f.source_hash,f.payload,f.ready_at FROM r26_feedback f WHERE NOT EXISTS(SELECT 1 FROM r31_feedback_event e WHERE e.kind='DRAW_LABEL_REVEALED' AND e.legacy_event_id=f.event_id) ORDER BY f.issue_ordinal LIMIT ?",new String[]{String.valueOf(limit)})){
            while(rows.moveToNext())prepared.add(new String[]{rows.getString(0),rows.getString(1),rows.getString(2),rows.getString(3),rows.getString(4)});
        }
        for(String[] row:prepared){String issue=row[3].split(",",2)[0];appendEventR31(db,"DRAW_LABEL_REVEALED",issue,Long.parseLong(row[1]),row[2],"",row[0],row[3],"CONFIRMED_DRAW_R26_COMPAT_OUTBOX",Long.parseLong(row[4]));}
        return prepared.size();
    }
    private static long typedFrozenEventsR31(SQLiteDatabase db,int limit){
        java.util.List<Object[]> prepared=new java.util.ArrayList<>();
        try(Cursor rows=db.rawQuery("SELECT o.target_issue,d.draw_hash_v2,s.record_hash,o.record_hash,o.selected_hit_p,o.hit_label,o.settled_at FROM "+DbHelper.TABLE_R35_OOS_LABEL+" o JOIN "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" s ON s.target_issue=o.target_issue JOIN draws d ON d.issue=o.target_issue WHERE o.strict_oos=1 AND o.pre_draw_frozen=1 AND s.pre_draw_frozen=1 AND s.settled=1 AND s.commit_at>0 AND s.commit_at<d.open_time AND s.commit_at<d.first_seen_at AND d.dirty=0 AND NOT EXISTS(SELECT 1 FROM r31_feedback_event e WHERE e.kind='FROZEN_RESULT' AND e.issue=o.target_issue AND e.canonical_hash=s.record_hash) ORDER BY o.target_issue LIMIT ?",new String[]{String.valueOf(limit)})){
            while(rows.moveToNext()){
                java.util.Map<String,Object> payload=new java.util.LinkedHashMap<>();payload.put("labelHash",rows.getString(3));payload.put("probability",rows.isNull(4)?null:rows.getDouble(4));payload.put("hit",rows.getInt(5));payload.put("role","FUTURE_FROZEN_RESULT");
                prepared.add(new Object[]{rows.getString(0),rows.getString(1),rows.getString(2),JsonR27.write(payload),rows.getLong(6)});
            }
        }
        for(Object[] row:prepared)appendEventR31(db,"FROZEN_RESULT",(String)row[0],TargetIssueResolver.sequenceOrdinalSU1((String)row[0]),(String)row[1],(String)row[2],"",(String)row[3],"STRICT_OOS_ORIGINAL_CANONICAL",(Long)row[4]);
        return prepared.size();
    }
    private static void appendEventR31(SQLiteDatabase db,String kind,String issue,long ordinal,String source,String canonical,String legacy,String payload,String producer,long at){
        String event=PolicyIdentityR26.sha256("R31_FEEDBACK|"+kind+'|'+issue+'|'+source+'|'+canonical),hash=PolicyIdentityR26.sha256(payload);
        try(Cursor existing=db.rawQuery("SELECT source_hash,canonical_hash,payload_hash FROM r31_feedback_event WHERE event_id=?",new String[]{event})){
            if(existing.moveToFirst()){if(!source.equals(existing.getString(0))||!canonical.equals(existing.getString(1))||!hash.equals(existing.getString(2)))throw new IllegalStateException("R31_FEEDBACK_INTEGRITY_CONFLICT");return;}
        }
        db.execSQL("INSERT INTO r31_feedback_event VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{event,kind,issue,ordinal,source,canonical,legacy,payload,hash,producer,at});
    }
    private static long bridgeModelReceiptsR31(SQLiteDatabase db,int limit){
        // These are pre-existing real model/checkpoint receipts, not newly claimed training work.
        db.execSQL("INSERT OR IGNORE INTO r31_feedback_receipt(consumer_version,event_id,source_hash,checkpoint_hash,artifact_hash,policy_hash,scope,committed_at) SELECT c.policy_hash||'|R26_CHECKPOINT_RECEIPT_V1',e.event_id,e.source_hash,c.checkpoint_hash,a.artifact_hash,c.policy_hash,c.scope,a.created_at FROM r26_consumption c JOIN r31_feedback_event e ON e.legacy_event_id=c.event_id AND e.kind='DRAW_LABEL_REVEALED' JOIN r26_feedback f ON f.event_id=c.event_id AND f.source_hash=e.source_hash JOIN r26_checkpoint_artifact a ON a.checkpoint_hash=c.checkpoint_hash AND a.trained_through>=e.issue_ordinal WHERE NOT EXISTS(SELECT 1 FROM r31_feedback_receipt r WHERE r.consumer_version=c.policy_hash||'|R26_CHECKPOINT_RECEIPT_V1' AND r.event_id=e.event_id) ORDER BY e.issue_ordinal,c.policy_hash LIMIT ?",new Object[]{limit});
        return changedR31(db);
    }
    private static long consumeFrozenEffectsR31(SQLiteDatabase db,int limit){
        final String consumer="R31_FROZEN_RESIDUAL_V1";
        java.util.List<String[]> events=new java.util.ArrayList<>();
        try(Cursor rows=db.rawQuery("SELECT e.event_id,e.issue_ordinal,e.source_hash,e.payload FROM r31_feedback_event e WHERE e.kind='FROZEN_RESULT' AND NOT EXISTS(SELECT 1 FROM r31_feedback_receipt r WHERE r.consumer_version=? AND r.event_id=e.event_id) ORDER BY e.issue_ordinal,e.event_id LIMIT ?",new String[]{consumer,String.valueOf(limit)})){
            while(rows.moveToNext())events.add(new String[]{rows.getString(0),rows.getString(1),rows.getString(2),rows.getString(3)});
        }
        long count=0,calibrated=0;double brier=0,residual=0;
        try(Cursor prior=db.rawQuery("SELECT events,calibrated_events,brier_sum,residual_sum FROM r31_frozen_feedback_effect WHERE consumer_version=?",new String[]{consumer})){if(prior.moveToFirst()){count=prior.getLong(0);calibrated=prior.getLong(1);brier=prior.getDouble(2);residual=prior.getDouble(3);}}
        for(String[] row:events){
            Object decoded=JsonR27.parse(row[3]);if(!(decoded instanceof java.util.Map))throw new IllegalStateException("R31_FROZEN_FEEDBACK_PAYLOAD");
            java.util.Map<?,?> payload=(java.util.Map<?,?>)decoded;Object probability=payload.get("probability"),label=payload.get("hit");
            if(!(label instanceof Number)||(((Number)label).intValue()!=0&&((Number)label).intValue()!=1))throw new IllegalStateException("R31_FROZEN_FEEDBACK_LABEL");
            count++;if(probability instanceof Number){double p=((Number)probability).doubleValue();if(Double.isFinite(p)&&p>=0&&p<=1){double delta=((Number)label).doubleValue()-p;calibrated++;brier+=delta*delta;residual+=delta;}}
            String state=PolicyIdentityR26.sha256(consumer+'|'+count+'|'+calibrated+'|'+Double.toHexString(brier)+'|'+Double.toHexString(residual)+'|'+row[0]);long now=System.currentTimeMillis();
            db.execSQL("INSERT OR REPLACE INTO r31_frozen_feedback_effect VALUES(?,?,?,?,?,?,?,?)",new Object[]{consumer,count,calibrated,brier,residual,Long.parseLong(row[1]),state,now});
            db.execSQL("INSERT INTO r31_feedback_receipt VALUES(?,?,?,?,?,?,?,?)",new Object[]{consumer,row[0],row[2],"",state,"","NUMERIC_RESEARCH_FROZEN_RESIDUAL_NOT_MODEL_TRAINING",now});
        }
        return events.size();
    }
    private static long changedR31(SQLiteDatabase db){try(Cursor row=db.rawQuery("SELECT changes()",null)){return row.moveToFirst()?row.getLong(0):0;}}
    private static boolean confirmedDrawR31(SQLiteDatabase db,Models.Draw draw){
        try(Cursor row=db.rawQuery("SELECT status,canonical_draw_hash FROM "+DbHelper.TABLE_R811_DRAW_CANONICAL+" WHERE target_issue=?",new String[]{draw.issue})){
            return row.moveToFirst()&&"CONFIRMED_CANONICAL_DRAW".equals(row.getString(0))&&(DrawHashR36.sha256V2(draw).equals(row.getString(1))||DrawHashR36.sha256(draw).equals(row.getString(1)));
        }
    }
}
