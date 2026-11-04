package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;

/** R27 additive schema migration. Every statement is CREATE ... IF NOT EXISTS or a guarded ADD COLUMN, so a database
 * written by the 1713d11 baseline (versionCode 577271029) is upgraded in place without touching existing rows, and
 * running the migration twice is a no-op (tools/r27/test_r27_migration.py replays this DDL on a baseline-shaped DB).
 * No table or row is ever dropped: historical evidence stays queryable. */
public final class SchemaR27 {
    private SchemaR27() {}
    public static final int REVISION = 1;
    public static final int R31_OBSERVATION_REVISION = 1;
    public static final int R31_WRITE_REVISION = 1;
    public static final String[] R31_WRITE_DDL = {
        "CREATE TABLE IF NOT EXISTS r31_commit_receipt(commit_key TEXT PRIMARY KEY,payload_hash TEXT NOT NULL,operation TEXT NOT NULL,task_id TEXT NOT NULL,owner_epoch TEXT NOT NULL,fence INTEGER NOT NULL,input_snapshot TEXT NOT NULL,chunk_seq INTEGER NOT NULL,operator_version TEXT NOT NULL,committed_at INTEGER NOT NULL)",
        "CREATE INDEX IF NOT EXISTS r31_commit_receipt_task ON r31_commit_receipt(task_id,committed_at)",
        "CREATE TABLE IF NOT EXISTS r31_task_lease(task_id TEXT PRIMARY KEY,owner_epoch TEXT NOT NULL,fence INTEGER NOT NULL,boot_id TEXT NOT NULL,deadline_elapsed_ms INTEGER NOT NULL,next_elapsed_ms INTEGER NOT NULL DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS r31_recovery_clock(singleton INTEGER PRIMARY KEY CHECK(singleton=1),boot_id TEXT NOT NULL,last_recovery_elapsed_ms INTEGER NOT NULL,owner_epoch TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r30_cache_rebuild(id INTEGER PRIMARY KEY AUTOINCREMENT,family_index INTEGER NOT NULL,through_ordinal INTEGER NOT NULL,reason TEXT NOT NULL,recorded_at INTEGER NOT NULL)"
    };
    /** Additive, executed only by the existing startup/migration owner. Never from a fault callback. */
    public static final String[] R31_DDL = {
        "CREATE TABLE IF NOT EXISTS r31_runtime_fault(event_id TEXT PRIMARY KEY,task_id TEXT NOT NULL,owner_epoch TEXT NOT NULL,fence INTEGER,operation TEXT NOT NULL,phase TEXT NOT NULL,category TEXT NOT NULL,root_fingerprint TEXT NOT NULL,minute_bucket INTEGER NOT NULL,cause_chain_json TEXT NOT NULL,stack TEXT NOT NULL,stack_hash TEXT NOT NULL,last_cause_chain_json TEXT NOT NULL,last_stack TEXT NOT NULL,last_stack_hash TEXT NOT NULL,commit_outcome TEXT NOT NULL,boot_id TEXT NOT NULL,first_wall_ms INTEGER NOT NULL,last_wall_ms INTEGER NOT NULL,first_elapsed_ms INTEGER NOT NULL,last_elapsed_ms INTEGER NOT NULL,occurrences INTEGER NOT NULL,byte_count INTEGER NOT NULL CHECK(byte_count>=0),UNIQUE(task_id,owner_epoch,root_fingerprint,minute_bucket))",
        "CREATE INDEX IF NOT EXISTS r31_runtime_fault_order ON r31_runtime_fault(last_wall_ms,task_id)",
        "CREATE TABLE IF NOT EXISTS r31_telemetry_counter(owner_epoch TEXT NOT NULL,name TEXT NOT NULL,value INTEGER NOT NULL CHECK(value>=0),updated_at INTEGER NOT NULL,PRIMARY KEY(owner_epoch,name))",
        "CREATE TABLE IF NOT EXISTS r31_task_observation(task_id TEXT PRIMARY KEY,owner_epoch TEXT NOT NULL,fence INTEGER,attempts INTEGER NOT NULL DEFAULT 0,work_cpu_ms INTEGER NOT NULL DEFAULT 0,observed_work_units INTEGER NOT NULL DEFAULT 0,task_receipts_committed INTEGER NOT NULL DEFAULT 0,task_receipts_failed INTEGER NOT NULL DEFAULT 0,last_attempt_elapsed_ms INTEGER NOT NULL,boot_id TEXT NOT NULL,last_artifact_hash TEXT,progress_evidence_status TEXT NOT NULL DEFAULT 'NOT_ARTIFACT_BOUND')",
        "CREATE TABLE IF NOT EXISTS r31_task_control(task_id TEXT PRIMARY KEY,quarantine_until INTEGER NOT NULL DEFAULT 0,level INTEGER NOT NULL DEFAULT 0,fingerprint TEXT NOT NULL DEFAULT '',half_open_owner TEXT,last_recovery_at INTEGER NOT NULL DEFAULT 0,generation INTEGER NOT NULL DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS r31_migration(revision INTEGER PRIMARY KEY,applied_at INTEGER NOT NULL,source_commit TEXT NOT NULL)"
    };
    public static final String[] DDL = {
        "CREATE TABLE IF NOT EXISTS r27_migration(revision INTEGER PRIMARY KEY,applied_at INTEGER NOT NULL,source_commit TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r27_task_error(error_id INTEGER PRIMARY KEY,task_id TEXT NOT NULL,error_class TEXT NOT NULL,message TEXT NOT NULL,stack TEXT NOT NULL,recorded_at INTEGER NOT NULL)",
        "CREATE INDEX IF NOT EXISTS r27_task_error_order ON r27_task_error(task_id,recorded_at)",
        "CREATE TABLE IF NOT EXISTS r27_thread_cpu(session_id TEXT NOT NULL,thread_name TEXT NOT NULL,cpu_ms INTEGER NOT NULL DEFAULT 0,wall_ms INTEGER NOT NULL DEFAULT 0,slices INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL,PRIMARY KEY(session_id,thread_name))",
        "CREATE TABLE IF NOT EXISTS r27_write_wait(hour_start INTEGER PRIMARY KEY,submits INTEGER NOT NULL DEFAULT 0,waits_over_200ms INTEGER NOT NULL DEFAULT 0,max_wait_ms INTEGER NOT NULL DEFAULT 0,requeues INTEGER NOT NULL DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS r27_replay_dataset(dataset_hash TEXT PRIMARY KEY,from_issue TEXT NOT NULL,to_issue TEXT NOT NULL,rows INTEGER NOT NULL,segments INTEGER NOT NULL,split TEXT NOT NULL,built_at INTEGER NOT NULL,build_ms INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r27_replay_score(policy_hash TEXT NOT NULL,dataset_hash TEXT NOT NULL,split TEXT NOT NULL CHECK(split IN ('H_TRAIN','H_SELECT','H_HOLDOUT')),n_sched INTEGER NOT NULL,n_act INTEGER NOT NULL,hits INTEGER NOT NULL,brier REAL,logloss REAL,ece REAL,worst_block_hr REAL,max_losing_streak INTEGER NOT NULL,paired_u_mean REAL NOT NULL,paired_u_lcb REAL,skipped_gap INTEGER NOT NULL DEFAULT 0,computed_at INTEGER NOT NULL,PRIMARY KEY(policy_hash,dataset_hash,split))",
        "CREATE TABLE IF NOT EXISTS r27_holdout_exposure(block_id INTEGER NOT NULL,policy_hash TEXT NOT NULL,looked_at INTEGER NOT NULL,PRIMARY KEY(block_id,policy_hash))",
        "CREATE TABLE IF NOT EXISTS r27_policy(policy_hash TEXT PRIMARY KEY,spec_json TEXT NOT NULL,idea_family TEXT NOT NULL,birth_generation INTEGER NOT NULL,birth_issue TEXT NOT NULL,dataset_hash TEXT NOT NULL,state TEXT NOT NULL,state_reason TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL DEFAULT 0)",
        "CREATE INDEX IF NOT EXISTS r27_policy_family ON r27_policy(idea_family,state)",
        "CREATE TABLE IF NOT EXISTS r27_lineage(child_hash TEXT NOT NULL,parent_hash TEXT NOT NULL,role TEXT NOT NULL,operator TEXT NOT NULL,generation INTEGER NOT NULL,created_at INTEGER NOT NULL,origin TEXT NOT NULL DEFAULT 'R27',created_issue TEXT NOT NULL DEFAULT '',PRIMARY KEY(child_hash,parent_hash,role))",
        "CREATE INDEX IF NOT EXISTS r27_lineage_order ON r27_lineage(created_at)",
        "CREATE TABLE IF NOT EXISTS r27_generation(generation INTEGER NOT NULL,loop TEXT NOT NULL DEFAULT 'MAIN',dataset_hash TEXT NOT NULL,births INTEGER NOT NULL,rejected INTEGER NOT NULL,no_op INTEGER NOT NULL,duplicates INTEGER NOT NULL,survivors INTEGER NOT NULL,retired INTEGER NOT NULL,champion_hash TEXT,champion_select_u REAL,champion_holdout_hr REAL,prev_champion_holdout_hr REAL,null_champion_holdout_hr REAL,i00_champion_holdout_hr REAL,paired_lcb REAL,verified INTEGER NOT NULL,detail_json TEXT NOT NULL,started_at INTEGER NOT NULL,finished_at INTEGER NOT NULL,PRIMARY KEY(loop,generation))",
        "CREATE TABLE IF NOT EXISTS r27_evolution_cursor(loop TEXT PRIMARY KEY,generation INTEGER NOT NULL,step INTEGER NOT NULL,dataset_hash TEXT NOT NULL,rng_state INTEGER NOT NULL,snapshot TEXT NOT NULL,updated_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r27_expert_proposal(generation INTEGER NOT NULL,expert TEXT NOT NULL,proposal_json TEXT NOT NULL,policy_hash TEXT,recorded_at INTEGER NOT NULL,PRIMARY KEY(generation,expert,proposal_json))",
        "CREATE TABLE IF NOT EXISTS r27_ablation(generation INTEGER NOT NULL,champion_hash TEXT NOT NULL,node_index INTEGER NOT NULL,removed_child INTEGER NOT NULL,branch TEXT NOT NULL,delta_u REAL,delta_logloss REAL,PRIMARY KEY(generation,node_index,removed_child))",
        "CREATE TABLE IF NOT EXISTS r27_live_shadow(policy_hash TEXT PRIMARY KEY,spec_json TEXT NOT NULL,temperature REAL NOT NULL,bias BLOB NOT NULL,global_index INTEGER NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,promoted_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,through_ordinal INTEGER NOT NULL DEFAULT -1,base_policy TEXT NOT NULL DEFAULT '',semantic_abi TEXT NOT NULL DEFAULT '')",
        "CREATE TABLE IF NOT EXISTS r27_live_frozen(policy_hash TEXT NOT NULL,target_ordinal INTEGER NOT NULL,target_issue TEXT NOT NULL,position INTEGER NOT NULL,probabilities BLOB NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,act INTEGER NOT NULL,status TEXT NOT NULL,frozen_at INTEGER NOT NULL,deadline_at INTEGER NOT NULL,hit INTEGER NOT NULL DEFAULT -1,settled_at INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(policy_hash,target_ordinal))",
        "CREATE TRIGGER IF NOT EXISTS r27_live_frozen_immutable BEFORE UPDATE OF probabilities,n1,n2,n3,act,frozen_at,deadline_at,position ON r27_live_frozen BEGIN SELECT RAISE(ABORT,'R27_FROZEN_IMMUTABLE'); END",
        "CREATE INDEX IF NOT EXISTS r27_live_frozen_order ON r27_live_frozen(target_ordinal)",
        "CREATE TABLE IF NOT EXISTS r27_eprocess(policy_hash TEXT PRIMARY KEY,global_index INTEGER NOT NULL UNIQUE,alpha REAL NOT NULL,log_e_up REAL NOT NULL,log_e_down REAL NOT NULL,n INTEGER NOT NULL,acts INTEGER NOT NULL DEFAULT 0,hits INTEGER NOT NULL DEFAULT 0,sum_u REAL NOT NULL,sum_u2 REAL NOT NULL,last_ordinal INTEGER NOT NULL,state TEXT NOT NULL,target_rate REAL NOT NULL DEFAULT 0.15,updated_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r27_certificate(policy_hash TEXT PRIMARY KEY,status TEXT NOT NULL,gates_json TEXT NOT NULL,evidence_digest TEXT NOT NULL,issued_at INTEGER NOT NULL,expires_at INTEGER NOT NULL,gate_version TEXT NOT NULL DEFAULT 'LEGACY_UNVERSIONED' CHECK(length(trim(gate_version))>0))",
        "CREATE TABLE IF NOT EXISTS r27_abi_continuity(policy_hash TEXT NOT NULL,old_build TEXT NOT NULL,new_build TEXT NOT NULL,result TEXT NOT NULL CHECK(result IN ('EQUIVALENT','CHANGED','ERROR')),first_diff_issue TEXT,first_diff_field TEXT,checked_at INTEGER NOT NULL,PRIMARY KEY(policy_hash,new_build))",
        "CREATE TABLE IF NOT EXISTS r27_legacy_roster_alias(checkpoint_hash TEXT PRIMARY KEY,minted_roster TEXT NOT NULL,recorded_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r27_semantic_fingerprint(policy_hash TEXT NOT NULL,build TEXT NOT NULL,fingerprint TEXT NOT NULL,operator_abi TEXT NOT NULL,rows_blob BLOB,computed_at INTEGER NOT NULL,PRIMARY KEY(policy_hash,build))",
        "CREATE TABLE IF NOT EXISTS r27_atomnet_epoch(run_id TEXT NOT NULL,epoch INTEGER NOT NULL,split TEXT NOT NULL,logloss REAL NOT NULL,brier REAL NOT NULL,recorded_at INTEGER NOT NULL,PRIMARY KEY(run_id,epoch,split))",
        "CREATE TABLE IF NOT EXISTS r27_brier_pair(target_issue TEXT NOT NULL,source TEXT NOT NULL CHECK(source IN ('FUTURE','REPLAY')),model TEXT NOT NULL DEFAULT 'ATOMNET',p_model REAL NOT NULL,p_base REAL NOT NULL,hit INTEGER NOT NULL,d REAL NOT NULL,recorded_at INTEGER NOT NULL,PRIMARY KEY(target_issue,source,model))",
        "CREATE TABLE IF NOT EXISTS r27_atomnet_state(owner TEXT PRIMARY KEY,artifact BLOB NOT NULL,artifact_hash TEXT NOT NULL,trained_through TEXT NOT NULL,online_steps INTEGER NOT NULL,learning_rate REAL NOT NULL,last_check_logloss REAL,updated_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r27_atomnet_pending(target_issue TEXT NOT NULL,model TEXT NOT NULL DEFAULT 'ATOMNET',p_model REAL NOT NULL,frozen_at INTEGER NOT NULL,deadline_at INTEGER NOT NULL,PRIMARY KEY(target_issue,model))",
        "CREATE TABLE IF NOT EXISTS r27_atom_score(atom_id TEXT PRIMARY KEY,atom_index INTEGER NOT NULL,dataset_hash TEXT NOT NULL,n_select INTEGER NOT NULL,gain_select REAL,lcb_select REAL,ucb_select REAL,n_holdout INTEGER NOT NULL,gain_holdout REAL,lcb_holdout REAL,missing_rate REAL NOT NULL,state TEXT NOT NULL,derived INTEGER NOT NULL DEFAULT 0,definition TEXT NOT NULL DEFAULT '',computed_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r27_molecule(molecule_hash TEXT PRIMARY KEY,definition TEXT NOT NULL,generation INTEGER NOT NULL,state TEXT NOT NULL,select_gain REAL,reason TEXT NOT NULL,created_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r27_calibration(policy_hash TEXT PRIMARY KEY,method TEXT NOT NULL,fit_source TEXT NOT NULL,fit_rows INTEGER NOT NULL,validation_rows INTEGER NOT NULL,excluded_late INTEGER NOT NULL,excluded_gap INTEGER NOT NULL,a REAL,b REAL,ece REAL,ece_by_stratum TEXT NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL,updated_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r27_chart_cache(panel_id TEXT NOT NULL,key TEXT NOT NULL,payload_json TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(panel_id,key))",
        "CREATE TABLE IF NOT EXISTS r27_fetch_reject(reason TEXT PRIMARY KEY,count INTEGER NOT NULL,last_issue TEXT,last_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r27_legacy_export(source_id TEXT PRIMARY KEY,source_table TEXT NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,policy_hash TEXT,reproduced INTEGER NOT NULL DEFAULT 0,compared INTEGER NOT NULL DEFAULT 0,exported_at INTEGER NOT NULL)",
        HistoryImportR27.DDL,
        // R29-010/035: contiguous qualification receipts, conflicts, rebuild log, cursor, pending draws (additive).
        QualificationInboxR29.DDL[0], QualificationInboxR29.DDL[1], QualificationInboxR29.DDL[2], QualificationInboxR29.DDL[3], QualificationInboxR29.DDL[4],
        QualificationInboxR29.DDL[5],
        GateV2R29.DDL,
        ModelArtifactR29.DDL,
        ModelArtifactR29.LEGACY_DDL,
        ModelArtifactR29.LEGACY_NO_UPDATE, ModelArtifactR29.LEGACY_NO_DELETE,
        ModelArtifactR29.PRESERVE_LEGACY,
        "CREATE TABLE IF NOT EXISTS r29_atomnet_forecast(target_issue TEXT NOT NULL,model TEXT NOT NULL,model_hash TEXT NOT NULL,trained_through TEXT NOT NULL,feature_abi TEXT NOT NULL,frozen_at INTEGER NOT NULL,deadline_at INTEGER NOT NULL,PRIMARY KEY(target_issue,model))",
        "CREATE TABLE IF NOT EXISTS r27_opportunity(target_ordinal INTEGER PRIMARY KEY,target_issue TEXT NOT NULL,scheduled INTEGER NOT NULL,frozen INTEGER NOT NULL,acted INTEGER NOT NULL,settled INTEGER NOT NULL,deadline_missed INTEGER NOT NULL,recorded_at INTEGER NOT NULL)"
    };
    /** Guarded column additions on existing baseline tables. */
    static final String[][] COLUMNS = {
        {"r30_frozen_confirm","registration_experiment_id","TEXT"},
        {"r26_task", "wait_kind", "TEXT NOT NULL DEFAULT ''"},
        {"r26_task", "wait_detail", "TEXT NOT NULL DEFAULT ''"},
        {"r26_task", "wait_since", "INTEGER NOT NULL DEFAULT 0"},
        {"r26_task", "registered_at", "INTEGER NOT NULL DEFAULT 0"},
        {"r26_task", "first_progress_at", "INTEGER NOT NULL DEFAULT 0"},
        {"r26_task", "error_window_start", "INTEGER NOT NULL DEFAULT 0"},
        {"r26_task", "error_count", "INTEGER NOT NULL DEFAULT 0"},
    };

    public static void ensure(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R27_WRITER_REQUIRED");
        for (String sql : DDL) db.execSQL(sql);
        SchemaDomainCoordinatorR926.migrateR31Observations(db);
        SchemaDomainCoordinatorR926.migrateR31WriteProtocol(db);
        SchemaDomainCoordinatorR926.migrateR31QualificationReceipts(db);
        SchemaDomainCoordinatorR926.migrateR31Feedback(db);
        for (String sql : ResearchSchemaR30R1.DDL) db.execSQL(sql);
        if(tableExists(db,"su2_matrix_snapshot"))MatrixStoreSU2.ensureMetadata(db); // existing SU2 ready markers do not imply new additive columns
        ResearchEvolutionStoreR930.createSchema(db); // restore the 13 baseline-declared legacy evidence tables, same owner/writer
        ensureCertificateVersionR30(db);
        for(String sql:DecisionExplanationStoreR30.DDL)db.execSQL(sql);
        CertificateRevocationR30.ensure(db);
        db.execSQL(HoldoutLedgerR27.CONFIRM_DDL); // R30-B03-03: durable reservation before confirmation reads
        db.execSQL(AtomScoreR27.CONTRIBUTION_DDL_R30);
        SchemaDomainCoordinatorR926.migrateR31AtomPublication(db);
        SchemaDomainCoordinatorR926.migrateR31HistoryImport(db);
        SchemaDomainCoordinatorR926.migrateR31EvolutionPublication(db);
        db.execSQL(BehaviorSignatureR30.DDL); // R30-B04-02: idempotent duplicate rejection receipts
        for (String sql : ModelSurfaceStoreR30.DDL) db.execSQL(sql);
        for (String sql : AtomNetStoreR30.DDL) db.execSQL(sql);
        for (String sql : FrozenConfirmationStoreR30.DDL) db.execSQL(sql);
        column(db,"r30_frozen_confirm","registration_state","TEXT NOT NULL DEFAULT 'PENDING'");
        column(db, "r30_confirm_read", "control", "INTEGER NOT NULL DEFAULT 0");
        ensureLiveSourceRevision(db);
        SchemaDomainCoordinatorR926.migrateR31SourceJournal(db);
        db.execSQL(LiveShadowsR27.PIT_DDL_R30);
        db.execSQL(HoldLedgerFlushR30.DDL); // R30-B01-02 (additive)
        column(db,"r30_writer_hold","histogram_json","TEXT");column(db,"r30_writer_hold","process_session","TEXT");
        IssueTimelineR30.ensureEventSchema(db); // old physical timeline retained; event receipts use the existing cache namespace
        for (String[] c : COLUMNS) if (tableExists(db, c[0])) column(db, c[0], c[1], c[2]);
        if (tableExists(db, "r26_task")) {
            long now = System.currentTimeMillis();
            db.execSQL("UPDATE r26_task SET registered_at=? WHERE registered_at=0", new Object[] {now});
            db.execSQL("UPDATE r26_task SET first_progress_at=last_progress WHERE first_progress_at=0 AND last_progress>0");
        }
        db.execSQL("INSERT OR IGNORE INTO r27_migration VALUES(?,?,?)", new Object[] {REVISION, System.currentTimeMillis(), BuildConfig.SOURCE_COMMIT_R26});
    }

    public static void ensureR31Observations(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_SCHEMA_WRITER_REQUIRED");
        for (String sql : R31_DDL) db.execSQL(sql);
        verifyR31ObservationColumns(db);
        db.execSQL("INSERT OR IGNORE INTO r31_migration VALUES(?,?,?)", new Object[] {
                R31_OBSERVATION_REVISION, System.currentTimeMillis(), BuildConfig.SOURCE_COMMIT_R26});
    }

    public static void ensureR31WriteProtocol(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R31_SCHEMA_WRITER_REQUIRED");
        for (String sql : R31_WRITE_DDL) db.execSQL(sql);
        column(db,"r31_task_control","quarantine_boot_id","TEXT NOT NULL DEFAULT ''");
        column(db,"r31_task_control","quarantine_deadline_elapsed_ms","INTEGER NOT NULL DEFAULT 0");
        column(db,"r31_task_control","fault_rounds","INTEGER NOT NULL DEFAULT 0");
        column(db,"r31_task_control","half_open_fence","INTEGER NOT NULL DEFAULT 0");
        column(db,"r31_task_control","last_success_artifact_hash","TEXT");
        column(db,"r31_task_control","fault_window_boot_id","TEXT NOT NULL DEFAULT ''");
        column(db,"r31_task_control","fault_window_elapsed_ms","INTEGER NOT NULL DEFAULT 0");
        String[][] required = {
            {"r31_commit_receipt","commit_key,payload_hash,operation,task_id,owner_epoch,fence,input_snapshot,chunk_seq,operator_version,committed_at"},
            {"r31_task_lease","task_id,owner_epoch,fence,boot_id,deadline_elapsed_ms,next_elapsed_ms"},
            {"r31_recovery_clock","singleton,boot_id,last_recovery_elapsed_ms,owner_epoch"},
            {"r30_cache_rebuild","id,family_index,through_ordinal,reason,recorded_at"},
            {"r31_task_control","quarantine_boot_id,quarantine_deadline_elapsed_ms,fault_rounds,half_open_fence,last_success_artifact_hash,fault_window_boot_id,fault_window_elapsed_ms"}
        };
        for (String[] table : required) {
            java.util.Set<String> names = new java.util.HashSet<>();
            try (Cursor c = db.rawQuery("PRAGMA table_info(" + table[0] + ")",null)) { while(c.moveToNext()) names.add(c.getString(1)); }
            for (String name : table[1].split(",")) if (!names.contains(name)) throw new IllegalStateException("R31_WRITE_SCHEMA_CONFLICT:"+table[0]+":"+name);
        }
        // Seed a finite recovery schedule for pre-R31 quarantines without clearing historical errors.
        // Existing R31 rows are not reset by process restart or schema re-entry.
        if (tableExists(db,"r26_task")) db.execSQL("INSERT OR IGNORE INTO r31_task_control(task_id,quarantine_until,level,fingerprint,quarantine_boot_id,quarantine_deadline_elapsed_ms) SELECT task_id,?,1,'INHERITED_PRE_R31_QUARANTINE',?,? FROM r26_task WHERE status='QUARANTINED'",
                new Object[] {System.currentTimeMillis()+30_000,IssueTimelineWriterR30.bootId(),android.os.SystemClock.elapsedRealtime()+30_000});
    }

    private static void verifyR31ObservationColumns(SQLiteDatabase db) {
        String[][] required = {
            {"r31_runtime_fault", "event_id,task_id,owner_epoch,fence,operation,phase,category,root_fingerprint,minute_bucket,cause_chain_json,stack,stack_hash,last_cause_chain_json,last_stack,last_stack_hash,commit_outcome,boot_id,first_wall_ms,last_wall_ms,first_elapsed_ms,last_elapsed_ms,occurrences,byte_count"},
            {"r31_telemetry_counter", "owner_epoch,name,value,updated_at"},
            {"r31_task_observation", "task_id,owner_epoch,fence,attempts,work_cpu_ms,observed_work_units,task_receipts_committed,task_receipts_failed,last_attempt_elapsed_ms,boot_id,last_artifact_hash,progress_evidence_status"},
            {"r31_task_control", "task_id,quarantine_until,level,fingerprint,half_open_owner,last_recovery_at,generation"},
            {"r31_migration", "revision,applied_at,source_commit"}
        };
        for (String[] table : required) {
            java.util.Set<String> actual = new java.util.HashSet<>();
            try (Cursor columns = db.rawQuery("PRAGMA table_info(" + table[0] + ")", null)) {
                while (columns.moveToNext()) actual.add(columns.getString(1));
            }
            for (String column : table[1].split(",")) if (!actual.contains(column))
                throw new IllegalStateException("R31_OBSERVATION_SCHEMA_CONFLICT:" + table[0] + ":" + column);
        }
    }

    /** Add metadata without relabeling historical certificates as having passed the current contract. */
    static void ensureCertificateVersionR30(SQLiteDatabase db) {
        column(db,"r27_certificate","gate_version","TEXT NOT NULL DEFAULT 'LEGACY_UNVERSIONED' CHECK(length(trim(gate_version))>0)");
        db.execSQL("UPDATE r27_certificate SET gate_version='LEGACY_UNVERSIONED' WHERE gate_version IS NULL OR length(trim(gate_version))=0");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r30_certificate_version_insert BEFORE INSERT ON r27_certificate WHEN NEW.gate_version IS NULL OR length(trim(NEW.gate_version))=0 BEGIN SELECT RAISE(ABORT,'R30_CERTIFICATE_VERSION_REQUIRED'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r30_certificate_version_update BEFORE UPDATE OF gate_version ON r27_certificate WHEN NEW.gate_version IS NULL OR length(trim(NEW.gate_version))=0 BEGIN SELECT RAISE(ABORT,'R30_CERTIFICATE_VERSION_REQUIRED'); END");
    }

    /** Same-database, additive revision of the actual inputs used by the live feature cache. */
    static final String LIVE_SOURCE_REVISION = "r30_live_source_revision";
    static final String[] LIVE_SOURCE_TRIGGERS = liveSourceTriggers();
    private static String[] liveSourceTriggers() {
        String draws = "NEW.issue IS NOT OLD.issue OR NEW.open_time IS NOT OLD.open_time OR NEW.dirty IS NOT OLD.dirty";
        for (int i=1;i<=10;i++) draws += " OR NEW.n"+i+" IS NOT OLD.n"+i;
        String imports = "NEW.issue IS NOT OLD.issue OR NEW.ordinal IS NOT OLD.ordinal OR NEW.numbers IS NOT OLD.numbers OR NEW.status IS NOT OLD.status";
        java.util.ArrayList<String> sql=new java.util.ArrayList<>();
        for (String table:new String[]{"draws","r27_history_import"}) for (String op:new String[]{"INSERT","UPDATE","DELETE"}) {
            String name="r30_live_revision_"+table+"_"+op.toLowerCase(java.util.Locale.ROOT);
            String when="UPDATE".equals(op)?" WHEN ("+("draws".equals(table)?draws:imports)+")":"";
            sql.add("CREATE TRIGGER IF NOT EXISTS "+name+" AFTER "+op+" ON "+table+when
                    +" BEGIN UPDATE "+LIVE_SOURCE_REVISION+" SET generation=generation+1 WHERE singleton=1; END");
        }
        return sql.toArray(new String[0]);
    }
    static void ensureLiveSourceRevision(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("R30_SOURCE_REVISION_WRITER_REQUIRED");
        if (!tableExists(db,"draws") || !tableExists(db,"r27_history_import")) return;
        db.execSQL("CREATE TABLE IF NOT EXISTS "+LIVE_SOURCE_REVISION+"(singleton INTEGER PRIMARY KEY CHECK(singleton=1),generation INTEGER NOT NULL CHECK(generation>=0))");
        db.execSQL("INSERT OR IGNORE INTO "+LIVE_SOURCE_REVISION+" VALUES(1,0)");
        for (String sql:LIVE_SOURCE_TRIGGERS) db.execSQL(sql);
        if (!liveSourceTriggersValid(db)) throw new IllegalStateException("R30_SOURCE_REVISION_TRIGGER_CONFLICT");
    }
    static boolean liveSourceTriggersValid(SQLiteDatabase db) {
        for (String expected:LIVE_SOURCE_TRIGGERS) {
            String name=expected.substring("CREATE TRIGGER IF NOT EXISTS ".length(),expected.indexOf(" AFTER "));
            try (Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='trigger' AND name=?",new String[]{name})) {
                if (!c.moveToFirst() || !canonicalTrigger(expected).equals(canonicalTrigger(c.getString(0)))) return false;
            }
        }
        return true;
    }
    private static String canonicalTrigger(String sql) {
        return sql==null?"":sql.replace(" IF NOT EXISTS "," ").trim().replaceAll("\\s+"," ");
    }
    static long liveSourceRevision(SQLiteDatabase db) {
        try (Cursor c=db.rawQuery("SELECT generation FROM "+LIVE_SOURCE_REVISION+" WHERE singleton=1",null)) { return c.moveToFirst()?c.getLong(0):-1; }
        catch (RuntimeException unavailable) { return -1; }
    }
    static long liveSourceSchemaVersion(SQLiteDatabase db) {
        try (Cursor c=db.rawQuery("PRAGMA schema_version",null)) { return c.moveToFirst()?c.getLong(0):-1; }
        catch (RuntimeException unavailable) { return -1; }
    }

    public static boolean ready(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM r27_migration WHERE revision=?", new String[] {String.valueOf(REVISION)})) { return c.moveToFirst(); }
        catch (RuntimeException missing) { return false; }
    }

    static boolean tableExists(SQLiteDatabase db, String table) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", new String[] {table})) { return c.moveToFirst(); }
    }
    static void column(SQLiteDatabase db, String table, String name, String type) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) { while (c.moveToNext()) if (name.equals(c.getString(1))) return; }
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + name + " " + type);
    }
}
