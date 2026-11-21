package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;

/** R33 research action schema. Only LifecycleSchemaR31 owns DDL execution.
 * Methods used by business code only inspect the schema. No hot-path CREATE is allowed.
 */
public final class ResearchActionSchemaR33 {
    private ResearchActionSchemaR33() {}
    public static final String[] DDL = {
        "CREATE TABLE IF NOT EXISTS r33_research_applied (\n consume_key TEXT PRIMARY KEY NOT NULL,\n source_run TEXT NOT NULL REFERENCES r30r1_analysis_run(run_id),\n view_id TEXT NOT NULL CHECK(view_id IN ('R01','R02','R03','R04','R05','R06','R07','R08','R09','R10','R12','R13','R15','R16','R17','MATRIX:EVIDENCE_TEACHER','MATRIX:STUDENT_ERROR','MATRIX:FORWARD_REASONING','MATRIX:REVERSE_REASONING','MATRIX:COUNTERFACTUAL','MATRIX:FEEDBACK','MATRIX:CONTRADICTION','MATRIX:TEACHER_DIVERSITY','MATRIX:INVARIANCE','MATRIX:CAUSAL_SUPPORT','MATRIX:COUNTER_EVIDENCE','MATRIX:RESIDUAL_MECHANISM','MATRIX:NUMBER_POSITION_COMPATIBILITY','MATRIX:CROSS_POSITION_FLOW','MATRIX:ACTION_UTILITY','MATRIX:ALGORITHM_COMPLEMENTARITY','MATRIX:EVOLUTION_CREDIT','MATRIX:RESOURCE_EFFICIENCY')),\n action_kind TEXT NOT NULL CHECK(action_kind IN ('BUDGET_APPLIED','DUPLICATE_REJECTED','REPLAY_PLAN_CREATED','MECHANISM_SKIPPED')),\n role TEXT NOT NULL CHECK((view_id='R08' AND role='TRAIN') OR (view_id<>'R08' AND role='SELECT')),\n dataset_hash TEXT NOT NULL,\n source_revision INTEGER NOT NULL CHECK(source_revision >= 0),\n source_commit TEXT NOT NULL,\n source_abi TEXT NOT NULL,\n input_hash TEXT NOT NULL CHECK(length(input_hash)=64),\n product_hash TEXT NOT NULL CHECK(length(product_hash)=64),\n before_hash TEXT NOT NULL CHECK(length(before_hash)=64),\n after_hash TEXT NOT NULL CHECK(length(after_hash)=64 AND after_hash<>before_hash),\n object_key TEXT NOT NULL,\n action_budget INTEGER NOT NULL CHECK(action_budget>0),\n action_ordinal INTEGER NOT NULL,\n latest_label_ordinal INTEGER NOT NULL CHECK(latest_label_ordinal < action_ordinal),\n applied_json TEXT NOT NULL,\n applied_at INTEGER NOT NULL,\n UNIQUE(source_run,view_id,action_kind,object_key,after_hash)\n)",
        "CREATE INDEX IF NOT EXISTS r33_research_applied_view ON r33_research_applied(source_commit,view_id,applied_at)",
        "CREATE TABLE IF NOT EXISTS r33_research_input_head (\n view_id TEXT PRIMARY KEY NOT NULL,\n dependency_key TEXT NOT NULL CHECK(length(dependency_key)=64),\n state TEXT NOT NULL CHECK(state IN ('PRODUCED','WAITING_DATASET','WAITING_STUDENT','WAITING_SOURCE','INSUFFICIENT','INVALID','FORMAL_READ_ONLY')),\n reason TEXT NOT NULL,\n source_run TEXT,\n producer_method TEXT NOT NULL,\n input_hash TEXT,\n output_hash TEXT,\n observed_at INTEGER NOT NULL\n)"
    };
    private static final String[][] TABLES = {
        {"r33_research_applied", "consume_key,source_run,view_id,action_kind,role,dataset_hash,source_revision,source_commit,source_abi,input_hash,product_hash,before_hash,after_hash,object_key,action_budget,action_ordinal,latest_label_ordinal,applied_json,applied_at"},
        {"r33_research_input_head", "view_id,dependency_key,state,reason,source_run,producer_method,input_hash,output_hash,observed_at"}
    };
    public static boolean declares(String name) {
        return "r33_research_applied".equals(name)
            || "r33_research_applied_view".equals(name)
            || "r33_research_input_head".equals(name);
    }
    public static void requireReady(SQLiteDatabase db) {
        DatabaseCoordinator.requireSchemaObjectsR31(db, "R33_RESEARCH_ACTION", TABLES,
            new String[][]{{"index", "r33_research_applied_view"}});
    }
}
