CREATE TABLE IF NOT EXISTS v57_issue_closure_r906 (
 target_issue TEXT PRIMARY KEY, draw_hash TEXT NOT NULL,
 predraw_record_hash TEXT NOT NULL DEFAULT '', eligibility TEXT NOT NULL,
 core_status TEXT NOT NULL, actual_number INTEGER NOT NULL DEFAULT 0,
 hit_label INTEGER NOT NULL DEFAULT -1, oos_record_hash TEXT NOT NULL DEFAULT '',
 real_record_hash TEXT NOT NULL DEFAULT '', settlement_hash TEXT NOT NULL DEFAULT '',
 previous_task_status TEXT NOT NULL DEFAULT '', exclusion_or_error_code TEXT NOT NULL DEFAULT '',
 classified_at INTEGER NOT NULL, completed_at INTEGER NOT NULL DEFAULT 0,
 record_hash TEXT NOT NULL UNIQUE
);
CREATE INDEX IF NOT EXISTS idx_r906_closure_status ON v57_issue_closure_r906(core_status,target_issue);

CREATE TABLE IF NOT EXISTS v57_settlement_outbox_r906 (
 event_id TEXT PRIMARY KEY, event_type TEXT NOT NULL, target_issue TEXT NOT NULL,
 subject_id TEXT NOT NULL DEFAULT '', payload_hash TEXT NOT NULL, status TEXT NOT NULL,
 priority INTEGER NOT NULL, attempts INTEGER NOT NULL DEFAULT 0,
 next_attempt_at INTEGER NOT NULL DEFAULT 0, lease_owner TEXT NOT NULL DEFAULT '',
 lease_generation INTEGER NOT NULL DEFAULT 0, process_boot_id TEXT NOT NULL DEFAULT '',
 lease_until INTEGER NOT NULL DEFAULT 0, last_progress_at INTEGER NOT NULL DEFAULT 0,
 last_error_code TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL,
 updated_at INTEGER NOT NULL, completed_at INTEGER NOT NULL DEFAULT 0,
 UNIQUE(event_type,target_issue,subject_id)
);
CREATE INDEX IF NOT EXISTS idx_r906_outbox_runnable ON v57_settlement_outbox_r906(status,priority DESC,next_attempt_at,created_at);

CREATE TABLE IF NOT EXISTS v57_selected_evidence_r906 (
 target_issue TEXT PRIMARY KEY, action_ordinal INTEGER NOT NULL, action_hash TEXT NOT NULL,
 raw_action_p_value REAL, raw_action_p_status TEXT NOT NULL, raw_action_p_source TEXT NOT NULL,
 raw_action_p_reason TEXT NOT NULL DEFAULT '', rank_no INTEGER, rank_score_value REAL,
 rank_status TEXT NOT NULL, third_margin_value REAL, third_margin_status TEXT NOT NULL,
 entropy_risk_value REAL, entropy_risk_status TEXT NOT NULL, entropy_risk_source TEXT NOT NULL,
 stability_value REAL, stability_status TEXT NOT NULL, stability_source TEXT NOT NULL,
 stability_reason TEXT NOT NULL DEFAULT '', window_consensus_value REAL,
 window_consensus_status TEXT NOT NULL, assignment_support_value REAL,
 assignment_support_status TEXT NOT NULL, metric_schema_hash TEXT NOT NULL,
 frozen_at INTEGER NOT NULL, record_hash TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS v57_runtime_truth_snapshot_r906 (
 snapshot_id TEXT PRIMARY KEY, generated_at INTEGER NOT NULL, db_data_version INTEGER NOT NULL,
 latest_draw_issue TEXT NOT NULL, latest_canonical_issue TEXT NOT NULL,
 latest_closure_issue TEXT NOT NULL, latest_oos_issue TEXT NOT NULL,
 payload_json TEXT NOT NULL, payload_hash TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS v57_research_asset_r906 (
 asset_id TEXT PRIMARY KEY, display_name_zh TEXT NOT NULL, normalized_name_zh TEXT NOT NULL UNIQUE,
 candidate_type TEXT NOT NULL, novelty_class TEXT NOT NULL, tier TEXT NOT NULL,
 method_signature TEXT NOT NULL, canonical_spec TEXT NOT NULL, representation_family TEXT NOT NULL,
 learner_family TEXT NOT NULL, update_rule TEXT NOT NULL, objective_family TEXT NOT NULL,
 calibration_family TEXT NOT NULL, risk_family TEXT NOT NULL, falsifier_family TEXT NOT NULL,
 parent_asset_ids TEXT NOT NULL DEFAULT '', generation INTEGER NOT NULL,
 genetic_operator TEXT NOT NULL, feature_schema_hash TEXT NOT NULL DEFAULT '',
 artifact_status TEXT NOT NULL, artifact_hash TEXT NOT NULL DEFAULT '',
 trained_until_issue TEXT NOT NULL DEFAULT '', earliest_use_issue TEXT NOT NULL DEFAULT '',
 runtime_executable INTEGER NOT NULL DEFAULT 0, may_authorize_real INTEGER NOT NULL DEFAULT 0 CHECK(may_authorize_real=0),
 last_transition_code TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
 registry_hash TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS v57_research_eval_coverage_r906 (
 asset_id TEXT PRIMARY KEY, eligible_n INTEGER NOT NULL DEFAULT 0,
 scheduled_n INTEGER NOT NULL DEFAULT 0, frozen_n INTEGER NOT NULL DEFAULT 0,
 settled_n INTEGER NOT NULL DEFAULT 0, hit_n INTEGER NOT NULL DEFAULT 0,
 first_eligible_issue TEXT NOT NULL DEFAULT '', last_eligible_issue TEXT NOT NULL DEFAULT '',
 last_frozen_issue TEXT NOT NULL DEFAULT '', last_settled_issue TEXT NOT NULL DEFAULT '',
 max_gap_issues INTEGER NOT NULL DEFAULT 0, hit_rate REAL, interval_lower REAL,
 interval_upper REAL, interval_method TEXT NOT NULL DEFAULT '', brier REAL, ece REAL,
 block_stability REAL, drift_score REAL, latency_p95_ms REAL, ram_p95_bytes INTEGER,
 evidence_hash TEXT NOT NULL, updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS v57_research_compact_label_r906 (
 asset_id TEXT NOT NULL, target_issue TEXT NOT NULL, selected_ordinal INTEGER NOT NULL,
 feature_schema_hash TEXT NOT NULL, feature_blob BLOB NOT NULL, feature_hash TEXT NOT NULL,
 predicted_probability REAL NOT NULL, actual_number INTEGER NOT NULL,
 hit_label INTEGER NOT NULL CHECK(hit_label IN (0,1)), predraw_hash TEXT NOT NULL,
 draw_hash TEXT NOT NULL, outcome_hash TEXT NOT NULL UNIQUE, frozen_at INTEGER NOT NULL,
 settled_at INTEGER NOT NULL, PRIMARY KEY(asset_id,target_issue)
);

CREATE TABLE IF NOT EXISTS v57_research_learner_update_r906 (
 learner_id TEXT NOT NULL, target_issue TEXT NOT NULL, prior_artifact_hash TEXT NOT NULL,
 feature_hash TEXT NOT NULL, outcome_hash TEXT NOT NULL, new_artifact_hash TEXT NOT NULL,
 trained_until_issue TEXT NOT NULL, earliest_use_issue TEXT NOT NULL,
 update_status TEXT NOT NULL, updated_at INTEGER NOT NULL, record_hash TEXT NOT NULL UNIQUE,
 PRIMARY KEY(learner_id,target_issue)
);

CREATE TABLE IF NOT EXISTS v57_research_artifact_job_r906 (
 job_id TEXT PRIMARY KEY, asset_id TEXT NOT NULL, job_status TEXT NOT NULL,
 dataset_hash TEXT NOT NULL, data_until_issue TEXT NOT NULL, feature_schema_hash TEXT NOT NULL,
 canonical_method_spec TEXT NOT NULL, seed INTEGER NOT NULL, protocol_hash TEXT NOT NULL,
 artifact_hash TEXT NOT NULL DEFAULT '', manifest_hash TEXT NOT NULL DEFAULT '',
 parity_status TEXT NOT NULL DEFAULT 'NOT_RUN', last_error_code TEXT NOT NULL DEFAULT '',
 created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
 UNIQUE(asset_id,dataset_hash,protocol_hash)
);

CREATE TABLE IF NOT EXISTS v57_research_certificate_r906 (
 certificate_id TEXT PRIMARY KEY, asset_id TEXT NOT NULL, certificate_type TEXT NOT NULL,
 evidence_start_issue TEXT NOT NULL, evidence_end_issue TEXT NOT NULL,
 future_n INTEGER NOT NULL, coverage REAL NOT NULL, paired_delta REAL NOT NULL,
 adjusted_lower REAL NOT NULL, brier REAL NOT NULL, ece REAL NOT NULL,
 block_stability REAL NOT NULL, drift_score REAL NOT NULL, latency_p95_ms REAL NOT NULL,
 ram_p95_bytes INTEGER NOT NULL, valid_from_issue TEXT NOT NULL, valid_until_issue TEXT NOT NULL,
 status TEXT NOT NULL, revocation_code TEXT NOT NULL DEFAULT '', evidence_hash TEXT NOT NULL,
 record_hash TEXT NOT NULL UNIQUE, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
);
