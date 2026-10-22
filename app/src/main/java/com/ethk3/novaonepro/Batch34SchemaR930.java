package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Arrays;

/**
 * Additive Batch-3/4 research/runtime schema for DB user_version 36.
 *
 * <p>The schema is isolated from the prediction hot path. It is created by the existing Research
 * domain migration before runtime callbacks execute. Append-only ledgers are protected by SQLite
 * triggers so process restarts, retries and dual callbacks cannot rewrite evidence.</p>
 */
public final class Batch34SchemaR930 {
    public static final String REVISION = "R930_BATCH34_SCHEMA_1";

    public static final String TABLE_META = "v57_batch34_schema_meta_r930";
    public static final String TABLE_CAPTURE = "v57_batch34_capture_r930";
    public static final String TABLE_CAPTURE_TRANSITION = "v57_batch34_capture_transition_r930";
    public static final String TABLE_EVIDENCE_LINK = "v57_batch34_evidence_link_r930";
    public static final String TABLE_PAIRED_WINDOW = "v57_batch34_paired_window_r930";
    public static final String TABLE_PAIRED_OBSERVATION = "v57_batch34_paired_observation_r930";
    public static final String TABLE_ALPHA_LEDGER = "v57_batch34_alpha_ledger_r930";
    public static final String TABLE_HYPOTHESIS = "v57_batch34_hypothesis_r930";
    public static final String TABLE_FAMILY_MEMBER = "v57_batch34_family_member_r930";
    public static final String TABLE_LIFECYCLE = "v57_batch34_lifecycle_r930";
    public static final String TABLE_LIFECYCLE_EVENT = "v57_batch34_lifecycle_event_r930";
    public static final String TABLE_FAILURE_CASE = "v57_batch34_failure_case_r930";
    public static final String TABLE_ABLATION = "v57_batch34_ablation_r930";
    public static final String TABLE_ATOM_VALUE = "v57_batch34_atom_value_r930";
    public static final String TABLE_ATOM_TIMING = "v57_batch34_atom_timing_r930";
    public static final String TABLE_GENOME = "v57_batch34_genome_r930";
    public static final String TABLE_GENOME_EDGE = "v57_batch34_genome_edge_r930";
    public static final String TABLE_BUNDLE = "v57_batch34_bundle_r930";
    public static final String TABLE_POLICY_DECISION = "v57_batch34_policy_decision_r930";
    public static final String TABLE_POLICY_OUTCOME = "v57_batch34_policy_outcome_r930";
    public static final String TABLE_POLICY_METRIC = "v57_batch34_policy_metric_r930";
    public static final String TABLE_MODULE_STATE = "v57_batch34_module_state_r930";
    public static final String TABLE_MODULE_EVENT = "v57_batch34_module_event_r930";

    private static final String[] REQUIRED_TABLES = new String[] {
        TABLE_META,
        TABLE_CAPTURE,
        TABLE_CAPTURE_TRANSITION,
        TABLE_EVIDENCE_LINK,
        TABLE_PAIRED_WINDOW,
        TABLE_PAIRED_OBSERVATION,
        TABLE_ALPHA_LEDGER,
        TABLE_HYPOTHESIS,
        TABLE_FAMILY_MEMBER,
        TABLE_LIFECYCLE,
        TABLE_LIFECYCLE_EVENT,
        TABLE_FAILURE_CASE,
        TABLE_ABLATION,
        TABLE_ATOM_VALUE,
        TABLE_ATOM_TIMING,
        TABLE_GENOME,
        TABLE_GENOME_EDGE,
        TABLE_BUNDLE,
        TABLE_POLICY_DECISION,
        TABLE_POLICY_OUTCOME,
        TABLE_POLICY_METRIC,
        TABLE_MODULE_STATE,
        TABLE_MODULE_EVENT
    };

    public static final String CONTRACT_HASH =
            V51ChecksumEngine.sha256(REVISION + "|" + Arrays.toString(REQUIRED_TABLES));

    private Batch34SchemaR930() {}

    /** DDL owner. Do not invoke from a canonical prediction/settlement transaction. */
    public static void ensure(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R930_BATCH34_SCHEMA_DB_NULL");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_META
                + "(singleton INTEGER PRIMARY KEY CHECK(singleton=1),revision TEXT NOT NULL,"
                + "contract_hash TEXT NOT NULL,state TEXT NOT NULL,created_at INTEGER NOT NULL,"
                + "updated_at INTEGER NOT NULL,last_error TEXT NOT NULL DEFAULT '')");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CAPTURE
                + "(game_code TEXT NOT NULL,target_issue TEXT NOT NULL,opportunity_sha256 TEXT NOT NULL,"
                + "current_state TEXT NOT NULL,sequence INTEGER NOT NULL,system_available INTEGER NOT NULL,"
                + "eligible INTEGER NOT NULL,selected INTEGER NOT NULL,execution_started INTEGER NOT NULL,"
                + "frozen INTEGER NOT NULL,failed INTEGER NOT NULL,lane TEXT NOT NULL,decision_action TEXT NOT NULL,"
                + "reject_code TEXT NOT NULL,selected_hit_p REAL NOT NULL,predictability REAL NOT NULL,"
                + "canonical_hash TEXT NOT NULL,snapshot_hash TEXT NOT NULL,frozen_payload_hash TEXT NOT NULL,"
                + "last_failure TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,"
                + "PRIMARY KEY(game_code,target_issue))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CAPTURE_TRANSITION
                + "(seq INTEGER PRIMARY KEY AUTOINCREMENT,transition_id TEXT NOT NULL UNIQUE,"
                + "game_code TEXT NOT NULL,target_issue TEXT NOT NULL,from_state TEXT NOT NULL,"
                + "to_state TEXT NOT NULL,sequence_before INTEGER NOT NULL,sequence_after INTEGER NOT NULL,"
                + "reason TEXT NOT NULL,payload_hash TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_capture_transition_issue ON "
                + TABLE_CAPTURE_TRANSITION + "(game_code,target_issue,seq)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EVIDENCE_LINK
                + "(evidence_id TEXT PRIMARY KEY,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,"
                + "evidence_type TEXT NOT NULL,owner TEXT NOT NULL,payload_hash TEXT NOT NULL,"
                + "detail TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_evidence_issue ON "
                + TABLE_EVIDENCE_LINK + "(game_code,target_issue,evidence_type)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PAIRED_WINDOW
                + "(window_id TEXT PRIMARY KEY,candidate_sha256 TEXT NOT NULL,champion_sha256 TEXT NOT NULL,"
                + "policy_sha256 TEXT NOT NULL,first_issue TEXT NOT NULL,last_issue TEXT NOT NULL,"
                + "planned_issues INTEGER NOT NULL,observations INTEGER NOT NULL DEFAULT 0,"
                + "state TEXT NOT NULL,terminal_decision_sha256 TEXT NOT NULL DEFAULT '',"
                + "declared_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_window_state_range ON "
                + TABLE_PAIRED_WINDOW + "(state,first_issue,last_issue)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PAIRED_OBSERVATION
                + "(window_id TEXT NOT NULL,target_issue TEXT NOT NULL,candidate_hit INTEGER NOT NULL,"
                + "champion_hit INTEGER NOT NULL,candidate_probability REAL NOT NULL,"
                + "champion_probability REAL NOT NULL,payload_hash TEXT NOT NULL,created_at INTEGER NOT NULL,"
                + "PRIMARY KEY(window_id,target_issue))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ALPHA_LEDGER
                + "(family TEXT PRIMARY KEY,initial_alpha REAL NOT NULL,available_alpha REAL NOT NULL,"
                + "global_index INTEGER NOT NULL,updated_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_HYPOTHESIS
                + "(hypothesis_id TEXT PRIMARY KEY,family TEXT NOT NULL,global_index INTEGER NOT NULL,"
                + "alpha REAL NOT NULL,window_id TEXT NOT NULL,state TEXT NOT NULL,p_value REAL,"
                + "evidence_sha256 TEXT NOT NULL DEFAULT '',reserved_at INTEGER NOT NULL,"
                + "decided_at INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FAMILY_MEMBER
                + "(family_key TEXT NOT NULL,algorithm_id TEXT NOT NULL,structural_sha256 TEXT NOT NULL,"
                + "probe_behavior_sha256 TEXT NOT NULL,residual_signature_sha256 TEXT NOT NULL,"
                + "statistical_family TEXT NOT NULL,artifact_sha256 TEXT NOT NULL,role TEXT NOT NULL,"
                + "future_support INTEGER NOT NULL,registered_at INTEGER NOT NULL,"
                + "PRIMARY KEY(family_key,algorithm_id))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LIFECYCLE
                + "(algorithm_id TEXT PRIMARY KEY,parent_algorithm_id TEXT NOT NULL,genome_sha256 TEXT NOT NULL,"
                + "repair_target TEXT NOT NULL,falsification_rule TEXT NOT NULL,state TEXT NOT NULL,"
                + "future_support INTEGER NOT NULL,last_evidence_sha256 TEXT NOT NULL,"
                + "first_prune_reason TEXT NOT NULL,first_prune_metrics TEXT NOT NULL,"
                + "first_prune_issue TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LIFECYCLE_EVENT
                + "(seq INTEGER PRIMARY KEY AUTOINCREMENT,event_id TEXT NOT NULL UNIQUE,algorithm_id TEXT NOT NULL,"
                + "from_state TEXT NOT NULL,to_state TEXT NOT NULL,added_future_support INTEGER NOT NULL,"
                + "evidence_sha256 TEXT NOT NULL,reason TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_lifecycle_event_algorithm ON "
                + TABLE_LIFECYCLE_EVENT + "(algorithm_id,seq)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FAILURE_CASE
                + "(case_sha256 TEXT PRIMARY KEY,target_issue TEXT NOT NULL,selected_action_sha256 TEXT NOT NULL,"
                + "frozen_input_sha256 TEXT NOT NULL,actual_outcome_sha256 TEXT NOT NULL,"
                + "failure_class TEXT NOT NULL,created_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ABLATION
                + "(case_sha256 TEXT NOT NULL,disabled_module TEXT NOT NULL,replay_sha256 TEXT NOT NULL,"
                + "canonical_writes_suppressed INTEGER NOT NULL,selection_changed INTEGER NOT NULL,"
                + "probability_delta REAL NOT NULL,evidence_cases INTEGER NOT NULL,created_at INTEGER NOT NULL,"
                + "PRIMARY KEY(case_sha256,disabled_module))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ATOM_VALUE
                + "(atom_id TEXT NOT NULL,target_issue TEXT NOT NULL,category TEXT NOT NULL,module_state TEXT NOT NULL,"
                + "semantic_type TEXT NOT NULL,present INTEGER NOT NULL,missing_reason TEXT NOT NULL,"
                + "max_source_issue TEXT NOT NULL,cost_ns INTEGER NOT NULL,value_sha256 TEXT NOT NULL,"
                + "implementation_sha256 TEXT NOT NULL,contract_sha256 TEXT NOT NULL,legacy_fixed INTEGER NOT NULL,"
                + "created_at INTEGER NOT NULL,PRIMARY KEY(atom_id,target_issue))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_atom_value_issue ON "
                + TABLE_ATOM_VALUE + "(target_issue,category,module_state)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ATOM_TIMING
                + "(timing_id TEXT PRIMARY KEY,atom_id TEXT NOT NULL,target_issue TEXT NOT NULL,"
                + "cost_ns INTEGER NOT NULL,budget_ns INTEGER NOT NULL,within_budget INTEGER NOT NULL,"
                + "created_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GENOME
                + "(genome_id TEXT PRIMARY KEY,statistical_family TEXT NOT NULL,novelty_class TEXT NOT NULL,"
                + "expression_dsl TEXT NOT NULL,state_update_dsl TEXT NOT NULL,type_contract TEXT NOT NULL,"
                + "range_contract TEXT NOT NULL,worst_case_cost_ns INTEGER NOT NULL,pit_safe INTEGER NOT NULL,"
                + "deterministic INTEGER NOT NULL,hard_constraints_passed INTEGER NOT NULL,"
                + "genome_sha256 TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GENOME_EDGE
                + "(genome_id TEXT NOT NULL,edge_ordinal INTEGER NOT NULL,from_node TEXT NOT NULL,"
                + "to_node TEXT NOT NULL,edge_type TEXT NOT NULL,edge_sha256 TEXT NOT NULL,"
                + "PRIMARY KEY(genome_id,edge_ordinal))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BUNDLE
                + "(generation_id TEXT PRIMARY KEY,bundle_sha256 TEXT NOT NULL UNIQUE,"
                + "previous_bundle_sha256 TEXT NOT NULL,contract_sha256 TEXT NOT NULL,"
                + "state TEXT NOT NULL,activation_issue TEXT NOT NULL,active INTEGER NOT NULL,"
                + "rollback_group TEXT NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS uq_r930_one_active_bundle ON "
                + TABLE_BUNDLE + "(active) WHERE active=1");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_POLICY_DECISION
                + "(game_code TEXT NOT NULL,target_issue TEXT NOT NULL,policy_sha256 TEXT NOT NULL,"
                + "predictability REAL NOT NULL,selected_hit_p REAL NOT NULL,lane TEXT NOT NULL,"
                + "selected INTEGER NOT NULL,decision_action TEXT NOT NULL,reject_code TEXT NOT NULL,"
                + "canonical_hash TEXT NOT NULL,created_at INTEGER NOT NULL,"
                + "PRIMARY KEY(game_code,target_issue))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_POLICY_OUTCOME
                + "(game_code TEXT NOT NULL,target_issue TEXT NOT NULL,hit_label INTEGER NOT NULL,"
                + "settlement_sha256 TEXT NOT NULL,draw_sha256 TEXT NOT NULL,status TEXT NOT NULL,"
                + "created_at INTEGER NOT NULL,PRIMARY KEY(game_code,target_issue))");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_POLICY_METRIC
                + "(metric_id TEXT PRIMARY KEY,policy_sha256 TEXT NOT NULL,start_issue TEXT NOT NULL,"
                + "end_issue TEXT NOT NULL,available_n INTEGER NOT NULL,selected_n INTEGER NOT NULL,"
                + "settled_selected_n INTEGER NOT NULL,calibrated_n INTEGER NOT NULL,hits INTEGER NOT NULL,"
                + "coverage REAL NOT NULL,"
                + "hit_rate REAL NOT NULL,brier REAL NOT NULL,risk REAL NOT NULL,"
                + "denominator TEXT NOT NULL,frequency_debt INTEGER NOT NULL,created_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MODULE_STATE
                + "(module_id TEXT PRIMARY KEY,module_state TEXT NOT NULL,reason TEXT NOT NULL,"
                + "implementation_sha256 TEXT NOT NULL,contract_sha256 TEXT NOT NULL,"
                + "certificate_sha256 TEXT NOT NULL,updated_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MODULE_EVENT
                + "(event_id TEXT PRIMARY KEY,module_id TEXT NOT NULL,from_state TEXT NOT NULL,"
                + "to_state TEXT NOT NULL,reason TEXT NOT NULL,evidence_sha256 TEXT NOT NULL,"
                + "created_at INTEGER NOT NULL)");

        appendOnly(db, TABLE_CAPTURE_TRANSITION, "capture_transition");
        appendOnly(db, TABLE_EVIDENCE_LINK, "evidence_link");
        appendOnly(db, TABLE_PAIRED_OBSERVATION, "paired_observation");
        appendOnly(db, TABLE_FAMILY_MEMBER, "family_member");
        appendOnly(db, TABLE_LIFECYCLE_EVENT, "lifecycle_event");
        appendOnly(db, TABLE_FAILURE_CASE, "failure_case");
        appendOnly(db, TABLE_ABLATION, "ablation");
        appendOnly(db, TABLE_ATOM_VALUE, "atom_value");
        appendOnly(db, TABLE_ATOM_TIMING, "atom_timing");
        appendOnly(db, TABLE_GENOME, "genome");
        appendOnly(db, TABLE_GENOME_EDGE, "genome_edge");
        appendOnly(db, TABLE_POLICY_DECISION, "policy_decision");
        appendOnly(db, TABLE_POLICY_OUTCOME, "policy_outcome");
        appendOnly(db, TABLE_POLICY_METRIC, "policy_metric");
        appendOnly(db, TABLE_MODULE_EVENT, "module_event");

        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_r930_window_identity_immutable"
                + " BEFORE UPDATE OF candidate_sha256,champion_sha256,policy_sha256,first_issue,last_issue,"
                + "planned_issues,declared_at ON " + TABLE_PAIRED_WINDOW
                + " BEGIN SELECT RAISE(ABORT,'R930_WINDOW_IDENTITY_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_r930_hypothesis_identity_immutable"
                + " BEFORE UPDATE OF family,global_index,alpha,window_id,reserved_at ON " + TABLE_HYPOTHESIS
                + " BEGIN SELECT RAISE(ABORT,'R930_HYPOTHESIS_IDENTITY_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_r930_lifecycle_identity_immutable"
                + " BEFORE UPDATE OF parent_algorithm_id,genome_sha256,created_at ON " + TABLE_LIFECYCLE
                + " BEGIN SELECT RAISE(ABORT,'R930_LIFECYCLE_IDENTITY_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_r930_bundle_identity_immutable"
                + " BEFORE UPDATE OF bundle_sha256,previous_bundle_sha256,contract_sha256,rollback_group,created_at"
                + " ON " + TABLE_BUNDLE
                + " BEGIN SELECT RAISE(ABORT,'R930_BUNDLE_IDENTITY_IMMUTABLE'); END");

        long now = System.currentTimeMillis();
        db.execSQL("INSERT OR REPLACE INTO " + TABLE_META
                        + "(singleton,revision,contract_hash,state,created_at,updated_at,last_error)"
                        + " VALUES(1,?,?,?,COALESCE((SELECT created_at FROM " + TABLE_META
                        + " WHERE singleton=1),?),?,'')",
                new Object[] {REVISION, CONTRACT_HASH, "READY", Long.valueOf(now), Long.valueOf(now)});
        verify(db);
    }

    public static boolean ready(SQLiteDatabase db) {
        if (db == null) return false;
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT revision,contract_hash,state FROM " + TABLE_META
                    + " WHERE singleton=1", null);
            if (!c.moveToFirst()) return false;
            if (!REVISION.equals(c.getString(0))
                    || !CONTRACT_HASH.equals(c.getString(1))
                    || !"READY".equals(c.getString(2))) return false;
            c.close();
            c = null;
            for (String table : REQUIRED_TABLES) if (!tableExists(db, table)) return false;
            return true;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (c != null) c.close();
        }
    }

    public static void verify(SQLiteDatabase db) {
        for (String table : REQUIRED_TABLES) {
            if (!tableExists(db, table)) {
                throw new IllegalStateException("R930_BATCH34_SCHEMA_OBJECT_MISSING|" + table);
            }
        }
    }

    public static String[] requiredTables() {
        return REQUIRED_TABLES.clone();
    }

    private static void appendOnly(SQLiteDatabase db, String table, String suffix) {
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_r930_" + suffix + "_no_update BEFORE UPDATE ON "
                + table + " BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_UPDATE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_r930_" + suffix + "_no_delete BEFORE DELETE ON "
                + table + " BEGIN SELECT RAISE(ABORT,'R930_APPEND_ONLY_DELETE'); END");
    }

    private static boolean tableExists(SQLiteDatabase db, String table) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1",
                    new String[] {table});
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }
}
