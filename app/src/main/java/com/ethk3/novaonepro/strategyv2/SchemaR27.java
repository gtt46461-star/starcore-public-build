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
        "CREATE TABLE IF NOT EXISTS r27_certificate(policy_hash TEXT PRIMARY KEY,status TEXT NOT NULL,gates_json TEXT NOT NULL,evidence_digest TEXT NOT NULL,issued_at INTEGER NOT NULL,expires_at INTEGER NOT NULL)",
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
        "CREATE TABLE IF NOT EXISTS r29_legacy_program_snapshot(source_id TEXT NOT NULL,program_hash TEXT NOT NULL,state_hash TEXT NOT NULL,program_spec TEXT NOT NULL,state_blob TEXT NOT NULL,state_updates INTEGER NOT NULL,source_abi TEXT NOT NULL,exported_at INTEGER NOT NULL,PRIMARY KEY(source_id,program_hash))",
        "CREATE TRIGGER IF NOT EXISTS r29_legacy_program_no_update BEFORE UPDATE ON r29_legacy_program_snapshot BEGIN SELECT RAISE(ABORT,'R29_LEGACY_PROGRAM_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r29_legacy_program_no_delete BEFORE DELETE ON r29_legacy_program_snapshot BEGIN SELECT RAISE(ABORT,'R29_LEGACY_PROGRAM_IMMUTABLE'); END",
        "CREATE TABLE IF NOT EXISTS r27_legacy_export(source_id TEXT PRIMARY KEY,source_table TEXT NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,policy_hash TEXT,reproduced INTEGER NOT NULL DEFAULT 0,compared INTEGER NOT NULL DEFAULT 0,exported_at INTEGER NOT NULL)",
        HistoryImportR27.DDL,
        ResearchEventBridgeR29.OUTBOX,
        com.ethk3.novaonepro.FetchObservationR29.DDL,
        ResearchEventBridgeR29.RESEARCH_DDL[0], ResearchEventBridgeR29.RESEARCH_DDL[1],
        ResearchEventBridgeR29.RESEARCH_DDL[2], ResearchEventBridgeR29.RESEARCH_DDL[3],
        ResearchEventBridgeR29.RESEARCH_DDL[4], ResearchEventBridgeR29.RESEARCH_DDL[5],
        ResearchEventBridgeR29.RESEARCH_DDL[6],
        ExperimentRegistrationR29.AUTHORITY,
        ResearchRouteR29.ROUTE_DDL,
        LegacyCapabilityAdapterR29.DDL,
        LegacyCapabilitySnapshotR29.PIN,LegacyCapabilitySnapshotR29.SNAPSHOT,
        LegacyCapabilitySnapshotR29.GUARDS[0],LegacyCapabilitySnapshotR29.GUARDS[1],LegacyCapabilitySnapshotR29.GUARDS[2],LegacyCapabilitySnapshotR29.GUARDS[3],
        PreparedForecastR29.MANIFEST,PreparedForecastR29.MEMBERS,PreparedForecastR29.SEALED,PreparedForecastR29.REJECTS,
        PreparedForecastR29.MEMBERS_NO_UPDATE,PreparedForecastR29.MEMBERS_NO_DELETE,
        ResearchCutoverR29.PUBLICATION,
        RealtimeResearchProjectionR29.REALTIME_DDL[0],RealtimeResearchProjectionR29.REALTIME_DDL[1],
        ExperimentRegistrationR29.AUTHORITY_GUARDS[0],ExperimentRegistrationR29.AUTHORITY_GUARDS[1],
        ExperimentRegistrationR29.RESEARCH_DDL[0],ExperimentRegistrationR29.RESEARCH_DDL[1],ExperimentRegistrationR29.RESEARCH_DDL[2],ExperimentRegistrationR29.RESEARCH_DDL[3],
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
        {"r27_live_frozen","manifest_hash","TEXT NOT NULL DEFAULT ''"},
        {"r27_live_frozen","input_through","INTEGER NOT NULL DEFAULT -1"},
        {"r27_live_frozen","commit_verified_at","INTEGER NOT NULL DEFAULT 0"},
        {"r27_live_frozen","commit_verified_mono","INTEGER NOT NULL DEFAULT 0"},
        {"r27_live_frozen","commit_session","TEXT NOT NULL DEFAULT ''"},
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
        ResearchEventBridgeR29.ensureRealtime(db);
        for (String[] c : COLUMNS) if (tableExists(db, c[0])) column(db, c[0], c[1], c[2]);
        db.execSQL(PreparedForecastR29.FROZEN_IMMUTABLE);
        if (tableExists(db, "r26_task")) {
            long now = System.currentTimeMillis();
            db.execSQL("UPDATE r26_task SET registered_at=? WHERE registered_at=0", new Object[] {now});
            db.execSQL("UPDATE r26_task SET first_progress_at=last_progress WHERE first_progress_at=0 AND last_progress>0");
        }
        db.execSQL("INSERT OR IGNORE INTO r27_migration VALUES(?,?,?)", new Object[] {REVISION, System.currentTimeMillis(), BuildConfig.SOURCE_COMMIT_R26});
    }

    public static boolean ready(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM r27_migration WHERE revision=?", new String[] {String.valueOf(REVISION)})) { return c.moveToFirst(); }
        catch (RuntimeException missing) { return false; }
    }

    /** Additive upgrades after cutover. Keep realtime control/authority tables on their original owner. */
    static void ensureResearch(SQLiteDatabase research){
        if(!research.inTransaction())throw new IllegalStateException("R29_RESEARCH_SCHEMA_TRANSACTION");
        for(String sql:DDL){boolean realtime=false;for(String table:MigrationCopyR29.REALTIME_ONLY)if(sql.contains(table)){realtime=true;break;}if(!realtime)research.execSQL(sql);}
        for(String[] c:COLUMNS)if(tableExists(research,c[0]))column(research,c[0],c[1],c[2]);
        research.execSQL(PreparedForecastR29.FROZEN_IMMUTABLE);
        RealtimeResearchProjectionR29.installResearch(research);
        LegacyLearningCommitR29.installResearch(research);
    }

    static boolean tableExists(SQLiteDatabase db, String table) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", new String[] {table})) { return c.moveToFirst(); }
    }
    static void column(SQLiteDatabase db, String table, String name, String type) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) { while (c.moveToNext()) if (name.equals(c.getString(1))) return; }
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + name + " " + type);
    }
}
