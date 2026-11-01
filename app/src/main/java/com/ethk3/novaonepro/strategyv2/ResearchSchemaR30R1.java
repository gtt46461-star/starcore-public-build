package com.ethk3.novaonepro.strategyv2;

/** Additive DDL only. Invoke from SchemaR27.ensure's existing writer, never from a report read. */
public final class ResearchSchemaR30R1 {
    private ResearchSchemaR30R1(){}
    public static final int REVISION=1;
    public static final String[] DDL={
      "CREATE TABLE IF NOT EXISTS r30r1_analysis_run(run_id TEXT PRIMARY KEY,dataset_hash TEXT NOT NULL,policy_hash TEXT NOT NULL,role TEXT NOT NULL CHECK(role IN ('TRAIN','SELECT','ONLINE_RESEARCH','CONFIRM','FUTURE','SYNTHETIC')),source_commit TEXT NOT NULL,source_revision INTEGER NOT NULL,from_ordinal INTEGER NOT NULL,through_ordinal INTEGER NOT NULL,contract_hash TEXT NOT NULL,status TEXT NOT NULL,summary_json TEXT NOT NULL,recorded_at INTEGER NOT NULL,CHECK(from_ordinal<=through_ordinal))",
      "CREATE TABLE IF NOT EXISTS r30r1_research_measure(run_id TEXT NOT NULL,measure_key TEXT NOT NULL,kind TEXT NOT NULL,parent_hash TEXT NOT NULL,block_id TEXT NOT NULL,from_ordinal INTEGER NOT NULL,to_ordinal INTEGER NOT NULL,evaluated_calendar_issues INTEGER NOT NULL CHECK(evaluated_calendar_issues>=0),mean_gain REAL,lower95 REAL,upper95 REAL,payload_json TEXT NOT NULL,PRIMARY KEY(run_id,measure_key),FOREIGN KEY(run_id) REFERENCES r30r1_analysis_run(run_id),CHECK(from_ordinal<=to_ordinal),CHECK(lower95 IS NULL OR upper95 IS NULL OR lower95<=upper95))",
      "CREATE TABLE IF NOT EXISTS r30r1_proposal(proposal_id TEXT PRIMARY KEY,run_id TEXT NOT NULL,parent_hash TEXT NOT NULL,kind TEXT NOT NULL,payload_json TEXT NOT NULL,payload_hash TEXT NOT NULL,state TEXT NOT NULL CHECK(state IN ('READY','CONSUMED','REJECTED','STALE')),reason TEXT NOT NULL,created_at INTEGER NOT NULL,FOREIGN KEY(run_id) REFERENCES r30r1_analysis_run(run_id))",
      "CREATE TABLE IF NOT EXISTS r30r1_consume(proposal_id TEXT PRIMARY KEY,child_policy_hash TEXT NOT NULL,source_generation INTEGER NOT NULL,consumer_version TEXT NOT NULL,receipt_hash TEXT NOT NULL,consumed_at INTEGER NOT NULL,FOREIGN KEY(proposal_id) REFERENCES r30r1_proposal(proposal_id))",
      "CREATE TABLE IF NOT EXISTS r30r1_chart_cache(view_id TEXT NOT NULL,frame_id TEXT NOT NULL,run_id TEXT NOT NULL,payload_json TEXT NOT NULL,payload_hash TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(view_id,frame_id),FOREIGN KEY(run_id) REFERENCES r30r1_analysis_run(run_id))",
      "CREATE TABLE IF NOT EXISTS r30r1_task_progress(task_id TEXT PRIMARY KEY,owner_version TEXT NOT NULL,work_unit TEXT NOT NULL,work_cursor INTEGER NOT NULL,known_latest INTEGER NOT NULL,pending INTEGER,oldest_pending_ordinal INTEGER,last_commit_clock_ordinal INTEGER,completed_input_ordinal INTEGER,fence INTEGER NOT NULL,session_id TEXT NOT NULL,updated_at INTEGER NOT NULL,artifact_identity TEXT NOT NULL,CHECK(work_cursor>=0 AND known_latest>=work_cursor),CHECK(pending IS NULL OR pending>=0))",
      "CREATE INDEX IF NOT EXISTS r30r1_proposal_ready ON r30r1_proposal(state,created_at,proposal_id)",
      "CREATE INDEX IF NOT EXISTS r30r1_measure_parent ON r30r1_research_measure(parent_hash,kind,from_ordinal)",
      "CREATE INDEX IF NOT EXISTS r30r1_chart_latest ON r30r1_chart_cache(view_id,updated_at DESC)",
      "CREATE TRIGGER IF NOT EXISTS r30r1_collection_no_update BEFORE UPDATE ON r30r1_analysis_run WHEN OLD.status='ONLINE_MODEL_ROLE_REGISTERED' BEGIN SELECT RAISE(ABORT,'RESEARCH_COLLECTION_CONTRACT_IMMUTABLE'); END",
      "CREATE TRIGGER IF NOT EXISTS r30r1_collection_no_delete BEFORE DELETE ON r30r1_analysis_run WHEN OLD.status='ONLINE_MODEL_ROLE_REGISTERED' BEGIN SELECT RAISE(ABORT,'RESEARCH_COLLECTION_CONTRACT_IMMUTABLE'); END",
      "CREATE TRIGGER IF NOT EXISTS r30r1_consume_immutable BEFORE UPDATE ON r30r1_consume BEGIN SELECT RAISE(ABORT,'RESEARCH_CONSUMPTION_IMMUTABLE'); END"
    };
}
