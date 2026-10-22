-- SU1 STORAGE V1 / DESIGN ADDITION. Baseline: 9ec590cafbd7cbcd1265a11b63a59df53ba78fcd.
-- Execute through the existing DatabaseCoordinator research writer, one complete
-- statement per journalled migration unit. This is NOT an Android DB replacement.
-- Caller sets PRAGMA foreign_keys=ON before transactions; no user_version change.
-- SQLite baseline features only: tables, indexes, checks, foreign keys, triggers.

CREATE TABLE IF NOT EXISTS su1_schema_revision (
 revision INTEGER PRIMARY KEY CHECK(revision=1),
 contract_name TEXT NOT NULL CHECK(contract_name='SU1_STORAGE_V1')
);
INSERT OR IGNORE INTO su1_schema_revision VALUES(1,'SU1_STORAGE_V1');

CREATE TABLE IF NOT EXISTS su1_runtime_control (
 singleton INTEGER PRIMARY KEY CHECK(singleton=1),
 mode TEXT NOT NULL DEFAULT 'DISABLED' CHECK(mode IN ('DISABLED','RESEARCH','SHADOW','MAINLINE_ADAPTER')),
 contract_hash TEXT NOT NULL DEFAULT '',
 updated_at INTEGER NOT NULL DEFAULT 0,
 reason TEXT NOT NULL DEFAULT 'DEFAULT_CLOSED'
);
INSERT OR IGNORE INTO su1_runtime_control(singleton) VALUES(1);

CREATE TABLE IF NOT EXISTS su1_archive_receipt (
 receipt_id TEXT NOT NULL PRIMARY KEY CHECK(length(receipt_id)=64),
 archive_locator TEXT NOT NULL,
 manifest_hash TEXT NOT NULL CHECK(length(manifest_hash)=64),
 dependency_graph_hash TEXT NOT NULL CHECK(length(dependency_graph_hash)=64),
 verified_at INTEGER NOT NULL CHECK(verified_at>0),
 replay_verified INTEGER NOT NULL CHECK(replay_verified=1),
 manifest_json TEXT NOT NULL CHECK(length(CAST(manifest_json AS BLOB))<=65536)
);
CREATE TABLE IF NOT EXISTS su1_blob (
 blob_hash TEXT NOT NULL PRIMARY KEY CHECK(length(blob_hash)=64),
 byte_count INTEGER NOT NULL CHECK(byte_count>=0),
 codec TEXT NOT NULL,
 storage_state TEXT NOT NULL CHECK(storage_state IN ('LOCAL','ARCHIVED')),
 local_locator TEXT,
 archive_receipt_id TEXT REFERENCES su1_archive_receipt(receipt_id),
 created_at INTEGER NOT NULL,
 CHECK((storage_state='LOCAL' AND local_locator IS NOT NULL) OR
       (storage_state='ARCHIVED' AND local_locator IS NULL AND archive_receipt_id IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS su1_snapshot (
 snapshot_hash TEXT NOT NULL PRIMARY KEY CHECK(length(snapshot_hash)=64),
 game TEXT NOT NULL,
 target_issue INTEGER NOT NULL,
 target_open_time INTEGER NOT NULL,
 source_max_issue INTEGER NOT NULL,
 source_available_at INTEGER NOT NULL,
 captured_at INTEGER NOT NULL,
 source_epoch TEXT NOT NULL,
 source_version_hash TEXT NOT NULL CHECK(length(source_version_hash)=64),
 payload_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 CHECK(source_max_issue<target_issue),
 CHECK(source_available_at<=captured_at AND captured_at<target_open_time)
);
CREATE TABLE IF NOT EXISTS su1_relation_asset (
 asset_hash TEXT NOT NULL PRIMARY KEY CHECK(length(asset_hash)=64),
 kind TEXT NOT NULL CHECK(kind IN ('PRIMITIVE','MACRO','STATE_TRANSITION')),
 typed_contract_hash TEXT NOT NULL CHECK(length(typed_contract_hash)=64),
 definition_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 material_status TEXT NOT NULL CHECK(material_status='MATERIAL'),
 available_through_sequence INTEGER NOT NULL CHECK(available_through_sequence>=0),
 created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS su1_program (
 program_hash TEXT NOT NULL PRIMARY KEY CHECK(length(program_hash)=64),
 grammar_hash TEXT NOT NULL CHECK(length(grammar_hash)=64),
 input_schema_hash TEXT NOT NULL CHECK(length(input_schema_hash)=64),
 typed_ir_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 state_schema_hash TEXT NOT NULL CHECK(length(state_schema_hash)=64),
 state_byte_limit INTEGER NOT NULL CHECK(state_byte_limit BETWEEN 0 AND 1048576),
 operation_limit INTEGER NOT NULL CHECK(operation_limit BETWEEN 1 AND 1000000),
 created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS su1_train_protocol (
 protocol_hash TEXT NOT NULL PRIMARY KEY CHECK(length(protocol_hash)=64),
 program_hash TEXT NOT NULL REFERENCES su1_program(program_hash),
 dataset_hash TEXT NOT NULL CHECK(length(dataset_hash)=64),
 split_hash TEXT NOT NULL CHECK(length(split_hash)=64),
 protocol_json TEXT NOT NULL CHECK(length(CAST(protocol_json AS BLOB))<=65536),
 train_first_sequence INTEGER NOT NULL,
 train_last_sequence INTEGER NOT NULL,
 validation_first_sequence INTEGER NOT NULL,
 validation_last_sequence INTEGER NOT NULL,
 target_contract_hash TEXT NOT NULL CHECK(length(target_contract_hash)=64),
 created_at INTEGER NOT NULL,
 CHECK(train_first_sequence<=train_last_sequence AND train_last_sequence<validation_first_sequence
       AND validation_first_sequence<=validation_last_sequence)
);
CREATE TABLE IF NOT EXISTS su1_model_artifact (
 artifact_hash TEXT NOT NULL PRIMARY KEY CHECK(length(artifact_hash)=64),
 protocol_hash TEXT NOT NULL REFERENCES su1_train_protocol(protocol_hash),
 program_hash TEXT NOT NULL REFERENCES su1_program(program_hash),
 model_kind TEXT NOT NULL,
 encoder_hash TEXT NOT NULL CHECK(length(encoder_hash)=64),
 weights_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 trained_through_sequence INTEGER NOT NULL CHECK(trained_through_sequence>=0),
 training_report_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS su1_strategy_version (
 strategy_id TEXT NOT NULL PRIMARY KEY CHECK(length(strategy_id)=64),
 codec TEXT NOT NULL CHECK(codec='SU1'),
 program_hash TEXT NOT NULL REFERENCES su1_program(program_hash),
 artifact_hash TEXT NOT NULL REFERENCES su1_model_artifact(artifact_hash),
 selector_hash TEXT NOT NULL CHECK(length(selector_hash)=64),
 calibration_hash TEXT REFERENCES su1_blob(blob_hash),
 calibration_state TEXT NOT NULL CHECK(calibration_state IN ('MISSING','FROZEN')),
 action_contract_hash TEXT NOT NULL CHECK(length(action_contract_hash)=64),
 state_initial_hash TEXT NOT NULL CHECK(length(state_initial_hash)=64),
 created_sequence INTEGER NOT NULL CHECK(created_sequence>=0),
 created_at INTEGER NOT NULL,
 -- R10: NULL_BASELINE = history-independent uniform sham (exact 30% K3 hit rate);
 -- NULL_CONTROL = pipeline negative control (sham pair or shuffled-label model).
 -- Only CANDIDATE strategies can ever hold a certificate.
 strategy_role TEXT NOT NULL DEFAULT 'CANDIDATE' CHECK(strategy_role IN ('CANDIDATE','NULL_BASELINE','NULL_CONTROL')),
 CHECK((calibration_state='MISSING' AND calibration_hash IS NULL) OR
       (calibration_state='FROZEN' AND calibration_hash IS NOT NULL))
);
-- R10: one lifetime error budget for the whole installation. Families draw
-- alpha_k <= global_alpha/(k(k+1)) by a monotone family_ordinal, so creating a
-- new family root, version, APK or research run can never mint fresh alpha.
CREATE TABLE IF NOT EXISTS su1_alpha_root (
 singleton INTEGER PRIMARY KEY CHECK(singleton=1),
 global_alpha REAL NOT NULL CHECK(global_alpha>0 AND global_alpha<=0.05),
 registration_hash TEXT NOT NULL CHECK(length(registration_hash)=64),
 created_at INTEGER NOT NULL
);
INSERT OR IGNORE INTO su1_alpha_root VALUES(1,0.05,'a289aa2cb13dc6f01651c7a2dbb715a1303e3e0518979dafae524da2d8c3c946',0);
CREATE TABLE IF NOT EXISTS su1_family_budget (
 family_root_hash TEXT NOT NULL PRIMARY KEY CHECK(length(family_root_hash)=64),
 family_alpha REAL NOT NULL CHECK(family_alpha>0 AND family_alpha<1),
 family_ordinal INTEGER NOT NULL UNIQUE CHECK(family_ordinal>0),
 registration_hash TEXT NOT NULL CHECK(length(registration_hash)=64),
 created_at INTEGER NOT NULL,
 UNIQUE(family_root_hash,family_alpha)
);
CREATE TABLE IF NOT EXISTS su1_eval_cohort (
 cohort_id TEXT NOT NULL PRIMARY KEY CHECK(length(cohort_id)=64),
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 parent_strategy_id TEXT REFERENCES su1_strategy_version(strategy_id),
 claim_id TEXT NOT NULL CHECK(length(claim_id)=64),
 evidence_role TEXT NOT NULL CHECK(evidence_role IN ('RESEARCH_FUTURE','CONFIRM','CALIBRATION_FIT','CALIBRATION_VALIDATION')),
 claim_kind TEXT NOT NULL CHECK(claim_kind IN ('PARENT_CHILD','VS_NULL_BASELINE','NULL_CONTROL')),
 registered_sequence INTEGER NOT NULL,
 first_issue INTEGER NOT NULL,
 planned_opportunities INTEGER NOT NULL CHECK(planned_opportunities BETWEEN 1 AND 1000000),
 protocol_hash TEXT NOT NULL CHECK(length(protocol_hash)=64),
 protocol_json TEXT NOT NULL CHECK(length(CAST(protocol_json AS BLOB))<=65536),
 family_root_hash TEXT NOT NULL CHECK(length(family_root_hash)=64),
 family_alpha REAL NOT NULL CHECK(family_alpha>0 AND family_alpha<1),
 global_claim_ordinal INTEGER NOT NULL CHECK(global_claim_ordinal>0),
 look_schedule_json TEXT NOT NULL CHECK(length(CAST(look_schedule_json AS BLOB))<=16384),
 baseline_hash TEXT NOT NULL CHECK(length(baseline_hash)=64),
 created_at INTEGER NOT NULL,
 FOREIGN KEY(family_root_hash,family_alpha) REFERENCES su1_family_budget(family_root_hash,family_alpha),
 UNIQUE(family_root_hash,global_claim_ordinal),
 UNIQUE(claim_id),
 CHECK(parent_strategy_id IS NULL OR parent_strategy_id<>strategy_id),
 CHECK(evidence_role<>'CONFIRM' OR parent_strategy_id IS NOT NULL)
);
CREATE TABLE IF NOT EXISTS su1_opportunity (
 opportunity_id TEXT NOT NULL PRIMARY KEY CHECK(length(opportunity_id)=64),
 cohort_id TEXT NOT NULL REFERENCES su1_eval_cohort(cohort_id),
 target_issue INTEGER NOT NULL,
 sequence_ordinal INTEGER NOT NULL CHECK(sequence_ordinal>0),
 opportunity_ordinal INTEGER NOT NULL CHECK(opportunity_ordinal>0),
 declared_at INTEGER NOT NULL,
 deadline_at INTEGER NOT NULL,
 source_epoch TEXT NOT NULL,
 eligibility_hash TEXT NOT NULL CHECK(length(eligibility_hash)=64),
 role TEXT NOT NULL CHECK(role IN ('RESEARCH_FUTURE','CONFIRM','CALIBRATION_FIT','CALIBRATION_VALIDATION')),
 UNIQUE(cohort_id,target_issue), UNIQUE(cohort_id,opportunity_ordinal),
 CHECK(declared_at<deadline_at)
);
CREATE TABLE IF NOT EXISTS su1_look_plan (
 cohort_id TEXT NOT NULL REFERENCES su1_eval_cohort(cohort_id),
 look_ordinal INTEGER NOT NULL CHECK(look_ordinal>0),
 planned_opportunities INTEGER NOT NULL CHECK(planned_opportunities>0),
 schedule_hash TEXT NOT NULL CHECK(length(schedule_hash)=64),
 PRIMARY KEY(cohort_id,look_ordinal),
 UNIQUE(cohort_id,look_ordinal,planned_opportunities)
);
-- One canonical outcome even when BOTH strategies missed the freeze deadline.
CREATE TABLE IF NOT EXISTS su1_opportunity_outcome (
 opportunity_id TEXT NOT NULL PRIMARY KEY REFERENCES su1_opportunity(opportunity_id),
 target_issue INTEGER NOT NULL,
 canonical_draw_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 legacy_settlement_hash TEXT NOT NULL CHECK(length(legacy_settlement_hash)=64),
 source_epoch TEXT NOT NULL,
 opened_at INTEGER NOT NULL,
 completed_at INTEGER NOT NULL CHECK(completed_at>=opened_at)
);
CREATE TABLE IF NOT EXISTS su1_prediction (
 prediction_id TEXT NOT NULL PRIMARY KEY CHECK(length(prediction_id)=64),
 opportunity_id TEXT NOT NULL REFERENCES su1_opportunity(opportunity_id),
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 snapshot_hash TEXT NOT NULL REFERENCES su1_snapshot(snapshot_hash),
 target_issue INTEGER NOT NULL,
 captured_at INTEGER NOT NULL,
 position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),
 n1 INTEGER NOT NULL CHECK(n1 BETWEEN 1 AND 10),
 n2 INTEGER NOT NULL CHECK(n2 BETWEEN 1 AND 10),
 n3 INTEGER NOT NULL CHECK(n3 BETWEEN 1 AND 10),
 prediction_mask INTEGER NOT NULL CHECK(prediction_mask BETWEEN 1 AND 1023),
 raw_k3_score REAL NOT NULL CHECK(raw_k3_score>=0 AND raw_k3_score<=1),
 selected_hit_p REAL CHECK(selected_hit_p>=0 AND selected_hit_p<=1),
 calibration_hash TEXT REFERENCES su1_blob(blob_hash),
 probability_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 period_state_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash) CHECK(length(period_state_hash)=64),
 trained_through_sequence INTEGER NOT NULL,
 input_through_sequence INTEGER NOT NULL,
 record_hash TEXT NOT NULL UNIQUE CHECK(length(record_hash)=64),
 UNIQUE(opportunity_id,strategy_id),
 CHECK(n1<n2 AND n2<n3),
 CHECK(prediction_mask=((1 << (n1-1)) | (1 << (n2-1)) | (1 << (n3-1)))),
 CHECK((selected_hit_p IS NULL AND calibration_hash IS NULL) OR
       (selected_hit_p IS NOT NULL AND calibration_hash IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS su1_settlement (
 settlement_id TEXT NOT NULL PRIMARY KEY CHECK(length(settlement_id)=64),
 prediction_id TEXT NOT NULL UNIQUE REFERENCES su1_prediction(prediction_id),
 target_issue INTEGER NOT NULL,
 canonical_draw_hash TEXT NOT NULL CHECK(length(canonical_draw_hash)=64),
 legacy_settlement_hash TEXT NOT NULL CHECK(length(legacy_settlement_hash)=64),
 actual INTEGER NOT NULL CHECK(actual BETWEEN 1 AND 10),
 hit INTEGER NOT NULL CHECK(hit IN (0,1)),
 label_hash TEXT NOT NULL UNIQUE CHECK(length(label_hash)=64),
 committed_at INTEGER NOT NULL
);
-- Rebuildable projection, not evidence. It makes late-terminal catch-up bounded.
CREATE TABLE IF NOT EXISTS su1_pending_settlement (
 prediction_id TEXT NOT NULL PRIMARY KEY REFERENCES su1_prediction(prediction_id),
 target_issue INTEGER NOT NULL,
 enqueued_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS su1_outbox (
 event_id TEXT NOT NULL PRIMARY KEY,
 event_type TEXT NOT NULL CHECK(event_type IN ('FROZEN','SETTLED')),
 prediction_id TEXT NOT NULL REFERENCES su1_prediction(prediction_id),
 settlement_id TEXT REFERENCES su1_settlement(settlement_id),
 source_record_hash TEXT NOT NULL CHECK(length(source_record_hash)=64),
 state TEXT NOT NULL DEFAULT 'PENDING' CHECK(state IN ('PENDING','LEASED','DONE','DEAD')),
 attempt_count INTEGER NOT NULL DEFAULT 0 CHECK(attempt_count>=0),
 lease_generation INTEGER NOT NULL DEFAULT 0 CHECK(lease_generation>=0),
 lease_owner TEXT,
 lease_until INTEGER,
 retry_at INTEGER NOT NULL DEFAULT 0,
 created_at INTEGER NOT NULL,
 updated_at INTEGER NOT NULL,
 last_error TEXT NOT NULL DEFAULT '',
 CHECK((event_type='FROZEN' AND settlement_id IS NULL) OR
       (event_type='SETTLED' AND settlement_id IS NOT NULL)),
 CHECK((state='LEASED' AND lease_owner IS NOT NULL AND lease_until IS NOT NULL) OR
       (state<>'LEASED' AND lease_owner IS NULL AND lease_until IS NULL))
);
CREATE TABLE IF NOT EXISTS su1_feedback (
 feedback_hash TEXT NOT NULL PRIMARY KEY CHECK(length(feedback_hash)=64),
 settlement_id TEXT NOT NULL REFERENCES su1_settlement(settlement_id),
 kind TEXT NOT NULL CHECK(kind IN ('RESIDUAL','COVERAGE','DRIFT','CALIBRATION','SYNERGY','DATA_QUALITY')),
 diagnostic_contract_hash TEXT NOT NULL CHECK(length(diagnostic_contract_hash)=64),
 payload_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 created_at INTEGER NOT NULL,
 UNIQUE(settlement_id,kind,diagnostic_contract_hash)
);
CREATE TABLE IF NOT EXISTS su1_research_task (
 task_id TEXT NOT NULL PRIMARY KEY CHECK(length(task_id)=64),
 intent_hash TEXT NOT NULL UNIQUE CHECK(length(intent_hash)=64),
 feedback_hash TEXT REFERENCES su1_feedback(feedback_hash),
 kind TEXT NOT NULL CHECK(kind IN ('DISCOVER','TRAIN','CALIBRATE','COMPARE','FUSE','REPLAY')),
 input_hash TEXT NOT NULL CHECK(length(input_hash)=64),
 boundary_sequence INTEGER NOT NULL CHECK(boundary_sequence>=0),
 state TEXT NOT NULL CHECK(state IN ('PENDING','LEASED','WAITING','COMPLETED','FAILED','CANCELLED')),
 checkpoint_hash TEXT REFERENCES su1_blob(blob_hash),
 output_hash TEXT REFERENCES su1_blob(blob_hash),
 lease_generation INTEGER NOT NULL DEFAULT 0 CHECK(lease_generation>=0),
 lease_owner TEXT,
 lease_until INTEGER,
 attempt_count INTEGER NOT NULL DEFAULT 0 CHECK(attempt_count>=0),
 cpu_budget_ms INTEGER NOT NULL CHECK(cpu_budget_ms BETWEEN 1 AND 60000),
 byte_budget INTEGER NOT NULL CHECK(byte_budget BETWEEN 1 AND 33554432),
 created_at INTEGER NOT NULL,
 updated_at INTEGER NOT NULL,
 last_error TEXT NOT NULL DEFAULT '',
 CHECK((state='LEASED' AND lease_owner IS NOT NULL AND lease_until IS NOT NULL) OR
       (state<>'LEASED' AND lease_owner IS NULL AND lease_until IS NULL)),
 CHECK(state<>'COMPLETED' OR output_hash IS NOT NULL)
);
CREATE TABLE IF NOT EXISTS su1_lineage (
 parent_strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 child_strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 task_id TEXT NOT NULL REFERENCES su1_research_task(task_id),
 operation_hash TEXT NOT NULL CHECK(length(operation_hash)=64),
 created_at INTEGER NOT NULL,
 PRIMARY KEY(parent_strategy_id,child_strategy_id,task_id),
 CHECK(parent_strategy_id<>child_strategy_id)
);
CREATE TABLE IF NOT EXISTS su1_evidence_look (
 evidence_hash TEXT NOT NULL PRIMARY KEY CHECK(length(evidence_hash)=64),
 cohort_id TEXT NOT NULL REFERENCES su1_eval_cohort(cohort_id),
 claim_id TEXT NOT NULL,
 global_claim_ordinal INTEGER NOT NULL CHECK(global_claim_ordinal>0),
 look_ordinal INTEGER NOT NULL CHECK(look_ordinal>0),
 protocol_hash TEXT NOT NULL CHECK(length(protocol_hash)=64),
 planned_opportunities INTEGER NOT NULL CHECK(planned_opportunities>0),
 paired_n INTEGER NOT NULL CHECK(paired_n>=0),
 parent_missing_n INTEGER NOT NULL CHECK(parent_missing_n>=0),
 child_missing_n INTEGER NOT NULL CHECK(child_missing_n>=0),
 unpaired_n INTEGER NOT NULL CHECK(unpaired_n>=0),
 parent_hits INTEGER NOT NULL CHECK(parent_hits>=0),
 child_hits INTEGER NOT NULL CHECK(child_hits>=0),
 child_only_wins INTEGER NOT NULL CHECK(child_only_wins>=0),
 parent_only_wins INTEGER NOT NULL CHECK(parent_only_wins>=0),
 paired_delta REAL,
 conservative_mean REAL NOT NULL CHECK(conservative_mean BETWEEN -1 AND 1),
 -- R10: radius = sqrt(2*ln(1/allocated_alpha)/planned_opportunities). SQLite has no
 -- portable ln/sqrt, so SQL only binds lower_bound to mean-radius; the reader and the
 -- certificate issuer MUST recompute radius from alpha and n (IEEE-754, 1e-12).
 bound_radius REAL NOT NULL CHECK(bound_radius>0),
 lower_bound REAL NOT NULL CHECK(lower_bound BETWEEN -1 AND 1),
 allocated_alpha REAL NOT NULL CHECK(allocated_alpha>0 AND allocated_alpha<1),
 cumulative_alpha_spent REAL NOT NULL CHECK(cumulative_alpha_spent>0 AND cumulative_alpha_spent<1),
 coverage REAL NOT NULL CHECK(coverage>=0 AND coverage<=1),
 result_state TEXT NOT NULL CHECK(result_state IN ('INSUFFICIENT','FAIL','PASS')),
 source_chain_hash TEXT NOT NULL CHECK(length(source_chain_hash)=64),
 metrics_json TEXT NOT NULL CHECK(length(CAST(metrics_json AS BLOB))<=65536),
 recorded_at INTEGER NOT NULL,
 FOREIGN KEY(cohort_id,look_ordinal,planned_opportunities)
  REFERENCES su1_look_plan(cohort_id,look_ordinal,planned_opportunities),
 UNIQUE(cohort_id,claim_id,look_ordinal),
 CHECK(paired_n+unpaired_n=planned_opportunities),
 CHECK(parent_missing_n<=unpaired_n AND child_missing_n<=unpaired_n
       AND parent_missing_n+child_missing_n>=unpaired_n),
 CHECK(parent_hits<=paired_n AND child_hits<=paired_n),
 CHECK(child_only_wins+parent_only_wins<=paired_n),
 CHECK(child_hits-parent_hits=child_only_wins-parent_only_wins),
 CHECK(result_state<>'PASS' OR lower_bound>0)
);
CREATE TABLE IF NOT EXISTS su1_certificate (
 certificate_id TEXT NOT NULL PRIMARY KEY CHECK(length(certificate_id)=64),
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 evidence_hash TEXT NOT NULL UNIQUE REFERENCES su1_evidence_look(evidence_hash),
 -- R10: mandatory second claim, child vs history-independent 30% sham. May equal
 -- evidence_hash only for a root strategy whose primary claim is VS_NULL_BASELINE.
 baseline_evidence_hash TEXT NOT NULL REFERENCES su1_evidence_look(evidence_hash),
 policy_hash TEXT NOT NULL CHECK(length(policy_hash)=64),
 source_chain_hash TEXT NOT NULL CHECK(length(source_chain_hash)=64),
 issued_at INTEGER NOT NULL,
 -- R10: hard expiry; renewal is a NEW prospective claim, never an extension.
 expires_at INTEGER NOT NULL,
 CHECK(expires_at>issued_at)
);
CREATE TABLE IF NOT EXISTS su1_certificate_revocation (
 certificate_id TEXT NOT NULL PRIMARY KEY REFERENCES su1_certificate(certificate_id),
 reason_code TEXT NOT NULL,
 evidence_hash TEXT NOT NULL CHECK(length(evidence_hash)=64),
 revoked_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS su1_mainline_binding (
 target_issue INTEGER PRIMARY KEY,
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 certificate_id TEXT NOT NULL REFERENCES su1_certificate(certificate_id),
 prediction_id TEXT NOT NULL UNIQUE REFERENCES su1_prediction(prediction_id),
 adapter_contract_hash TEXT NOT NULL CHECK(length(adapter_contract_hash)=64),
 original_owner_receipt_hash TEXT NOT NULL CHECK(length(original_owner_receipt_hash)=64),
 bound_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS su1_dependency (
 source_kind TEXT NOT NULL,
 source_id TEXT NOT NULL,
 target_kind TEXT NOT NULL,
 target_id TEXT NOT NULL,
 reason TEXT NOT NULL,
 created_at INTEGER NOT NULL,
 PRIMARY KEY(source_kind,source_id,target_kind,target_id)
);
CREATE TABLE IF NOT EXISTS su1_retention_pin (
 pin_id TEXT NOT NULL PRIMARY KEY CHECK(length(pin_id)=64),
 object_kind TEXT NOT NULL,
 object_id TEXT NOT NULL,
 reason TEXT NOT NULL CHECK(reason IN ('ACTIVE_STRATEGY','OPEN_COHORT','PENDING_EVENT','LEASED_TASK','REPLAY','MAINLINE')),
 created_at INTEGER NOT NULL,
 released_at INTEGER,
 CHECK(released_at IS NULL OR released_at>=created_at)
);
CREATE TABLE IF NOT EXISTS su1_runtime_checkpoint (
 checkpoint_key TEXT NOT NULL PRIMARY KEY,
 cursor_issue INTEGER NOT NULL DEFAULT 0,
 updated_at INTEGER NOT NULL,
 deferred_count INTEGER NOT NULL DEFAULT 0 CHECK(deferred_count>=0),
 last_error TEXT NOT NULL DEFAULT ''
);

CREATE INDEX IF NOT EXISTS su1_snapshot_issue ON su1_snapshot(target_issue,captured_at);
CREATE INDEX IF NOT EXISTS su1_opportunity_deadline ON su1_opportunity(deadline_at,cohort_id);
CREATE INDEX IF NOT EXISTS su1_opportunity_outcome_issue ON su1_opportunity_outcome(target_issue,opportunity_id);
CREATE INDEX IF NOT EXISTS su1_prediction_issue ON su1_prediction(target_issue,strategy_id);
CREATE INDEX IF NOT EXISTS su1_settlement_issue ON su1_settlement(target_issue,prediction_id);
CREATE INDEX IF NOT EXISTS su1_pending_settlement_issue ON su1_pending_settlement(target_issue,prediction_id);
CREATE INDEX IF NOT EXISTS su1_outbox_pending ON su1_outbox(state,retry_at,created_at);
CREATE INDEX IF NOT EXISTS su1_task_pending ON su1_research_task(state,updated_at);
CREATE INDEX IF NOT EXISTS su1_dependency_target ON su1_dependency(target_kind,target_id);
CREATE INDEX IF NOT EXISTS su1_active_pin ON su1_retention_pin(object_kind,object_id,released_at);

CREATE TRIGGER IF NOT EXISTS su1_model_binding BEFORE INSERT ON su1_model_artifact
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_train_protocol p WHERE p.protocol_hash=NEW.protocol_hash
  AND p.program_hash=NEW.program_hash AND p.validation_last_sequence<=NEW.trained_through_sequence)
 THEN RAISE(ABORT,'SU1_MODEL_PROTOCOL_MISMATCH') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_strategy_binding BEFORE INSERT ON su1_strategy_version
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_model_artifact m WHERE m.artifact_hash=NEW.artifact_hash
  AND m.program_hash=NEW.program_hash AND m.trained_through_sequence<=NEW.created_sequence)
 THEN RAISE(ABORT,'SU1_STRATEGY_ARTIFACT_MISMATCH') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_opportunity_binding BEFORE INSERT ON su1_opportunity
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_eval_cohort c WHERE c.cohort_id=NEW.cohort_id
  AND c.evidence_role=NEW.role AND NEW.sequence_ordinal>c.registered_sequence
  AND NEW.opportunity_ordinal<=c.planned_opportunities AND NEW.target_issue>=c.first_issue)
 THEN RAISE(ABORT,'SU1_OPPORTUNITY_PROTOCOL_MISMATCH') END;
 SELECT CASE WHEN NEW.role='CONFIRM' AND NOT EXISTS(SELECT 1 FROM su1_look_plan WHERE cohort_id=NEW.cohort_id)
 THEN RAISE(ABORT,'SU1_LOOK_PLAN_NOT_REGISTERED') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_cohort_binding BEFORE INSERT ON su1_eval_cohort
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_strategy_version WHERE strategy_id=NEW.strategy_id
  AND created_sequence<=NEW.registered_sequence)
 OR (NEW.parent_strategy_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM su1_strategy_version
  WHERE strategy_id=NEW.parent_strategy_id AND created_sequence<=NEW.registered_sequence))
 THEN RAISE(ABORT,'SU1_COHORT_STRATEGY_NOT_FROZEN') END;
 -- R12: only CONFIRM claims spend alpha, so only they take ordinals 1,2,3,...; research/
 -- calibration cohorts live in a separate namespace from 1000000 and never dilute CONFIRM power.
 SELECT CASE WHEN NEW.evidence_role='CONFIRM' AND NEW.global_claim_ordinal<>(SELECT coalesce(max(global_claim_ordinal),0)+1
  FROM su1_eval_cohort WHERE family_root_hash=NEW.family_root_hash AND evidence_role='CONFIRM')
 THEN RAISE(ABORT,'SU1_GLOBAL_CLAIM_ORDINAL') END;
 SELECT CASE WHEN NEW.evidence_role<>'CONFIRM' AND NEW.global_claim_ordinal<>(SELECT coalesce(max(global_claim_ordinal),999999)+1
  FROM su1_eval_cohort WHERE family_root_hash=NEW.family_root_hash AND evidence_role<>'CONFIRM')
 THEN RAISE(ABORT,'SU1_RESEARCH_CLAIM_ORDINAL') END;
 -- R10 claim kinds bind roles: candidates are compared with candidates (PARENT_CHILD)
 -- or with the uniform sham (VS_NULL_BASELINE); a NULL_CONTROL child is never a candidate.
 SELECT CASE WHEN NEW.parent_strategy_id IS NOT NULL AND NOT (
   (NEW.claim_kind='PARENT_CHILD'
    AND (SELECT strategy_role FROM su1_strategy_version WHERE strategy_id=NEW.strategy_id)='CANDIDATE'
    AND (SELECT strategy_role FROM su1_strategy_version WHERE strategy_id=NEW.parent_strategy_id)='CANDIDATE')
  OR (NEW.claim_kind='VS_NULL_BASELINE'
    AND (SELECT strategy_role FROM su1_strategy_version WHERE strategy_id=NEW.strategy_id)='CANDIDATE'
    AND (SELECT strategy_role FROM su1_strategy_version WHERE strategy_id=NEW.parent_strategy_id)='NULL_BASELINE')
  OR (NEW.claim_kind='NULL_CONTROL'
    AND (SELECT strategy_role FROM su1_strategy_version WHERE strategy_id=NEW.strategy_id) IN ('NULL_BASELINE','NULL_CONTROL')))
 THEN RAISE(ABORT,'SU1_CLAIM_KIND_ROLE_MISMATCH') END;
 SELECT CASE WHEN NEW.parent_strategy_id IS NULL AND NEW.claim_kind<>'PARENT_CHILD'
 THEN RAISE(ABORT,'SU1_CLAIM_KIND_REQUIRES_COMPARATOR') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_family_budget_allocation BEFORE INSERT ON su1_family_budget
BEGIN
 SELECT CASE WHEN NEW.family_ordinal<>(SELECT coalesce(max(family_ordinal),0)+1 FROM su1_family_budget)
 THEN RAISE(ABORT,'SU1_FAMILY_ORDINAL') END;
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_alpha_root WHERE singleton=1
  AND NEW.family_alpha<=global_alpha/(1.0*NEW.family_ordinal*(NEW.family_ordinal+1))+0.000000000001)
 THEN RAISE(ABORT,'SU1_FAMILY_ALPHA_EXCEEDS_GLOBAL_ROOT') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_look_plan_binding BEFORE INSERT ON su1_look_plan
BEGIN
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_opportunity WHERE cohort_id=NEW.cohort_id)
 OR NOT EXISTS(SELECT 1 FROM su1_eval_cohort WHERE cohort_id=NEW.cohort_id
  AND NEW.planned_opportunities<=planned_opportunities)
 THEN RAISE(ABORT,'SU1_LOOK_PLAN_MUST_PRECEDE_OPPORTUNITY') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_prediction_binding BEFORE INSERT ON su1_prediction
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_runtime_control WHERE singleton=1 AND mode<>'DISABLED')
 THEN RAISE(ABORT,'SU1_DISABLED') END;
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_opportunity o JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  JOIN su1_snapshot s ON s.snapshot_hash=NEW.snapshot_hash
  JOIN su1_strategy_version v ON v.strategy_id=NEW.strategy_id
  JOIN su1_model_artifact m ON m.artifact_hash=v.artifact_hash
  WHERE o.opportunity_id=NEW.opportunity_id AND o.target_issue=NEW.target_issue
  AND NOT EXISTS(SELECT 1 FROM su1_opportunity_outcome a WHERE a.opportunity_id=o.opportunity_id)
  AND s.target_issue=NEW.target_issue AND s.source_epoch=o.source_epoch
  AND NEW.captured_at>=s.captured_at AND NEW.captured_at>=o.declared_at AND NEW.captured_at<o.deadline_at
  AND NEW.captured_at<s.target_open_time AND v.created_sequence<o.sequence_ordinal
  AND NEW.trained_through_sequence=m.trained_through_sequence
  AND NEW.trained_through_sequence<o.sequence_ordinal AND NEW.input_through_sequence<o.sequence_ordinal
  AND NEW.input_through_sequence>=NEW.trained_through_sequence
  AND (NEW.strategy_id=c.strategy_id OR NEW.strategy_id=c.parent_strategy_id)
  AND ((NEW.calibration_hash IS NULL AND NEW.selected_hit_p IS NULL) OR
       (v.calibration_state='FROZEN' AND NEW.calibration_hash=v.calibration_hash)))
 THEN RAISE(ABORT,'SU1_FREEZE_BINDING_OR_TIME') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_freeze_event AFTER INSERT ON su1_prediction
BEGIN
 INSERT INTO su1_pending_settlement(prediction_id,target_issue,enqueued_at)
 VALUES(NEW.prediction_id,NEW.target_issue,NEW.captured_at);
 INSERT INTO su1_outbox(event_id,event_type,prediction_id,source_record_hash,created_at,updated_at)
 VALUES('FROZEN:'||NEW.prediction_id,'FROZEN',NEW.prediction_id,NEW.record_hash,NEW.captured_at,NEW.captured_at);
END;
CREATE TRIGGER IF NOT EXISTS su1_settlement_binding BEFORE INSERT ON su1_settlement
BEGIN
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_settlement WHERE target_issue=NEW.target_issue
  AND (canonical_draw_hash<>NEW.canonical_draw_hash OR legacy_settlement_hash<>NEW.legacy_settlement_hash))
 THEN RAISE(ABORT,'SU1_TARGET_TERMINAL_CONFLICT') END;
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_prediction p JOIN su1_snapshot s ON s.snapshot_hash=p.snapshot_hash
  JOIN su1_opportunity_outcome o ON o.opportunity_id=p.opportunity_id
  WHERE p.prediction_id=NEW.prediction_id AND p.target_issue=NEW.target_issue
  AND NEW.canonical_draw_hash=o.canonical_draw_hash AND NEW.legacy_settlement_hash=o.legacy_settlement_hash
  AND NEW.committed_at>=o.completed_at
  AND NEW.committed_at>=s.target_open_time
  AND NEW.hit=CASE WHEN NEW.actual IN (p.n1,p.n2,p.n3) THEN 1 ELSE 0 END)
 THEN RAISE(ABORT,'SU1_SETTLEMENT_BINDING') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_opportunity_outcome_binding BEFORE INSERT ON su1_opportunity_outcome
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_opportunity o WHERE o.opportunity_id=NEW.opportunity_id
  AND o.target_issue=NEW.target_issue AND o.source_epoch=NEW.source_epoch AND NEW.opened_at>=o.deadline_at)
 OR EXISTS(SELECT 1 FROM su1_opportunity_outcome WHERE target_issue=NEW.target_issue
  AND (canonical_draw_hash<>NEW.canonical_draw_hash OR legacy_settlement_hash<>NEW.legacy_settlement_hash))
 THEN RAISE(ABORT,'SU1_OPPORTUNITY_OUTCOME_BINDING') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_settlement_event AFTER INSERT ON su1_settlement
BEGIN
 DELETE FROM su1_pending_settlement WHERE prediction_id=NEW.prediction_id;
 INSERT INTO su1_outbox(event_id,event_type,prediction_id,settlement_id,source_record_hash,created_at,updated_at)
 VALUES('SETTLED:'||NEW.settlement_id,'SETTLED',NEW.prediction_id,NEW.settlement_id,NEW.label_hash,NEW.committed_at,NEW.committed_at);
END;
CREATE TRIGGER IF NOT EXISTS su1_pending_settlement_delete_guard BEFORE DELETE ON su1_pending_settlement
WHEN NOT EXISTS(SELECT 1 FROM su1_settlement WHERE prediction_id=OLD.prediction_id)
BEGIN SELECT RAISE(ABORT,'SU1_PENDING_REQUIRES_SETTLEMENT'); END;
CREATE TRIGGER IF NOT EXISTS su1_pending_settlement_update_guard BEFORE UPDATE ON su1_pending_settlement
BEGIN SELECT RAISE(ABORT,'SU1_PENDING_IDENTITY_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_look_binding BEFORE INSERT ON su1_evidence_look
BEGIN
 SELECT CASE WHEN NEW.planned_opportunities<>(SELECT count(*) FROM su1_opportunity o
  JOIN su1_opportunity_outcome a ON a.opportunity_id=o.opportunity_id
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities
   AND a.completed_at<=NEW.recorded_at)
 THEN RAISE(ABORT,'SU1_LOOK_CANONICAL_PREFIX_NOT_COMPLETE') END;
 SELECT CASE WHEN abs(NEW.cumulative_alpha_spent-NEW.allocated_alpha-
  (SELECT coalesce(sum(allocated_alpha),0) FROM su1_evidence_look WHERE cohort_id=NEW.cohort_id))>0.000000000001
 THEN RAISE(ABORT,'SU1_LOOK_ALPHA_LEDGER') END;
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_opportunity o JOIN su1_prediction p ON p.opportunity_id=o.opportunity_id
  LEFT JOIN su1_settlement s ON s.prediction_id=p.prediction_id AND s.committed_at<=NEW.recorded_at
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities AND s.settlement_id IS NULL)
 THEN RAISE(ABORT,'SU1_LOOK_PENDING_SETTLEMENT') END;
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_eval_cohort c WHERE c.cohort_id=NEW.cohort_id
  AND c.claim_id=NEW.claim_id AND c.protocol_hash=NEW.protocol_hash
  AND c.global_claim_ordinal=NEW.global_claim_ordinal
  AND NEW.planned_opportunities<=c.planned_opportunities
  AND NEW.allocated_alpha<=c.family_alpha /
      (1.0*c.global_claim_ordinal*(c.global_claim_ordinal+1)*NEW.look_ordinal*(NEW.look_ordinal+1))
  AND NEW.cumulative_alpha_spent<=c.family_alpha)
 THEN RAISE(ABORT,'SU1_LOOK_PROTOCOL_OR_ALPHA') END;
 SELECT CASE WHEN NEW.planned_opportunities<>(SELECT count(*) FROM su1_opportunity
  WHERE cohort_id=NEW.cohort_id AND opportunity_ordinal<=NEW.planned_opportunities AND deadline_at<=NEW.recorded_at)
 THEN RAISE(ABORT,'SU1_LOOK_OPPORTUNITY_DENOMINATOR') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_look_derived_counts BEFORE INSERT ON su1_evidence_look
BEGIN
 SELECT CASE WHEN NEW.paired_n<>(SELECT count(*) FROM su1_opportunity o
  JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  JOIN su1_prediction a ON a.opportunity_id=o.opportunity_id AND a.strategy_id=c.parent_strategy_id
  JOIN su1_settlement sa ON sa.prediction_id=a.prediction_id
  JOIN su1_prediction b ON b.opportunity_id=o.opportunity_id AND b.strategy_id=c.strategy_id
  JOIN su1_settlement sb ON sb.prediction_id=b.prediction_id
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities
   AND sa.committed_at<=NEW.recorded_at AND sb.committed_at<=NEW.recorded_at)
 THEN RAISE(ABORT,'SU1_LOOK_PAIRED_COUNT') END;
 SELECT CASE WHEN NEW.parent_missing_n<>(SELECT count(*) FROM su1_opportunity o
  JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  LEFT JOIN su1_prediction p ON p.opportunity_id=o.opportunity_id AND p.strategy_id=c.parent_strategy_id
  LEFT JOIN su1_settlement s ON s.prediction_id=p.prediction_id AND s.committed_at<=NEW.recorded_at
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities AND s.settlement_id IS NULL)
 THEN RAISE(ABORT,'SU1_LOOK_PARENT_MISSING') END;
 SELECT CASE WHEN NEW.child_missing_n<>(SELECT count(*) FROM su1_opportunity o
  JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  LEFT JOIN su1_prediction p ON p.opportunity_id=o.opportunity_id AND p.strategy_id=c.strategy_id
  LEFT JOIN su1_settlement s ON s.prediction_id=p.prediction_id AND s.committed_at<=NEW.recorded_at
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities AND s.settlement_id IS NULL)
 THEN RAISE(ABORT,'SU1_LOOK_CHILD_MISSING') END;
 SELECT CASE WHEN NEW.parent_hits<>(SELECT coalesce(sum(sa.hit),0) FROM su1_opportunity o
  JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  JOIN su1_prediction a ON a.opportunity_id=o.opportunity_id AND a.strategy_id=c.parent_strategy_id
  JOIN su1_settlement sa ON sa.prediction_id=a.prediction_id
  JOIN su1_prediction b ON b.opportunity_id=o.opportunity_id AND b.strategy_id=c.strategy_id
  JOIN su1_settlement sb ON sb.prediction_id=b.prediction_id
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities
   AND sa.committed_at<=NEW.recorded_at AND sb.committed_at<=NEW.recorded_at)
 OR NEW.child_hits<>(SELECT coalesce(sum(sb.hit),0) FROM su1_opportunity o
  JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  JOIN su1_prediction a ON a.opportunity_id=o.opportunity_id AND a.strategy_id=c.parent_strategy_id
  JOIN su1_settlement sa ON sa.prediction_id=a.prediction_id
  JOIN su1_prediction b ON b.opportunity_id=o.opportunity_id AND b.strategy_id=c.strategy_id
  JOIN su1_settlement sb ON sb.prediction_id=b.prediction_id
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities
   AND sa.committed_at<=NEW.recorded_at AND sb.committed_at<=NEW.recorded_at)
 THEN RAISE(ABORT,'SU1_LOOK_HIT_COUNTS') END;
 SELECT CASE WHEN NEW.child_only_wins<>(SELECT count(*) FROM su1_opportunity o
  JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  JOIN su1_prediction a ON a.opportunity_id=o.opportunity_id AND a.strategy_id=c.parent_strategy_id
  JOIN su1_settlement sa ON sa.prediction_id=a.prediction_id
  JOIN su1_prediction b ON b.opportunity_id=o.opportunity_id AND b.strategy_id=c.strategy_id
  JOIN su1_settlement sb ON sb.prediction_id=b.prediction_id
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities
   AND sa.committed_at<=NEW.recorded_at AND sb.committed_at<=NEW.recorded_at AND sa.hit=0 AND sb.hit=1)
 OR NEW.parent_only_wins<>(SELECT count(*) FROM su1_opportunity o
  JOIN su1_eval_cohort c ON c.cohort_id=o.cohort_id
  JOIN su1_prediction a ON a.opportunity_id=o.opportunity_id AND a.strategy_id=c.parent_strategy_id
  JOIN su1_settlement sa ON sa.prediction_id=a.prediction_id
  JOIN su1_prediction b ON b.opportunity_id=o.opportunity_id AND b.strategy_id=c.strategy_id
  JOIN su1_settlement sb ON sb.prediction_id=b.prediction_id
  WHERE o.cohort_id=NEW.cohort_id AND o.opportunity_ordinal<=NEW.planned_opportunities
   AND sa.committed_at<=NEW.recorded_at AND sb.committed_at<=NEW.recorded_at AND sa.hit=1 AND sb.hit=0)
 THEN RAISE(ABORT,'SU1_LOOK_DISCORDANT_PAIRS') END;
 SELECT CASE WHEN abs(NEW.conservative_mean-
  (1.0*(NEW.child_only_wins-NEW.parent_only_wins-NEW.unpaired_n)/NEW.planned_opportunities))>0.000000001
  OR abs(NEW.coverage-(1.0*NEW.paired_n/NEW.planned_opportunities))>0.000000001
  OR (NEW.paired_n=0 AND NEW.paired_delta IS NOT NULL)
  OR (NEW.paired_n>0 AND (NEW.paired_delta IS NULL OR abs(NEW.paired_delta-
     (1.0*(NEW.child_hits-NEW.parent_hits)/NEW.paired_n))>0.000000001))
  OR abs(NEW.lower_bound-max(-1.0,NEW.conservative_mean-NEW.bound_radius))>0.000000001
 THEN RAISE(ABORT,'SU1_LOOK_DERIVED_METRICS') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_certificate_binding BEFORE INSERT ON su1_certificate
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_evidence_look e JOIN su1_eval_cohort c ON c.cohort_id=e.cohort_id
  JOIN su1_strategy_version v ON v.strategy_id=c.strategy_id
  WHERE e.evidence_hash=NEW.evidence_hash AND c.strategy_id=NEW.strategy_id
  AND v.strategy_role='CANDIDATE' AND c.claim_kind IN ('PARENT_CHILD','VS_NULL_BASELINE')
  AND c.evidence_role='CONFIRM' AND e.result_state='PASS' AND e.lower_bound>0
  AND e.source_chain_hash=NEW.source_chain_hash AND NEW.issued_at>=e.recorded_at)
 THEN RAISE(ABORT,'SU1_CERTIFICATE_EVIDENCE') END;
 -- R10: beating a parent is never enough; the same strategy must also beat the
 -- uniform 30% sham inside the same lifetime-budgeted family.
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_evidence_look b JOIN su1_eval_cohort bc ON bc.cohort_id=b.cohort_id
  JOIN su1_evidence_look e ON e.evidence_hash=NEW.evidence_hash
  JOIN su1_eval_cohort ec ON ec.cohort_id=e.cohort_id
  WHERE b.evidence_hash=NEW.baseline_evidence_hash AND bc.strategy_id=NEW.strategy_id
  AND bc.claim_kind='VS_NULL_BASELINE' AND bc.evidence_role='CONFIRM'
  AND b.result_state='PASS' AND b.lower_bound>0 AND NEW.issued_at>=b.recorded_at
  AND bc.family_root_hash=ec.family_root_hash)
 THEN RAISE(ABORT,'SU1_CERTIFICATE_NULL_BASELINE_REQUIRED') END;
 -- A strategy with a candidate lineage parent must also confirm against that parent.
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_lineage l JOIN su1_strategy_version pv ON pv.strategy_id=l.parent_strategy_id
   WHERE l.child_strategy_id=NEW.strategy_id AND pv.strategy_role='CANDIDATE')
  AND NOT EXISTS(SELECT 1 FROM su1_evidence_look e JOIN su1_eval_cohort c ON c.cohort_id=e.cohort_id
   JOIN su1_lineage l ON l.child_strategy_id=c.strategy_id AND l.parent_strategy_id=c.parent_strategy_id
   WHERE e.evidence_hash=NEW.evidence_hash AND c.claim_kind='PARENT_CHILD')
 THEN RAISE(ABORT,'SU1_CERTIFICATE_PARENT_CLAIM_REQUIRED') END;
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_evidence_look x JOIN su1_eval_cohort xc ON xc.cohort_id=x.cohort_id
  JOIN su1_evidence_look e ON e.evidence_hash=NEW.evidence_hash
  JOIN su1_eval_cohort ec ON ec.cohort_id=e.cohort_id
  WHERE xc.claim_kind='NULL_CONTROL' AND x.result_state='PASS' AND xc.family_root_hash=ec.family_root_hash)
 THEN RAISE(ABORT,'SU1_NULL_CONTROL_ALARM') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_mainline_guard BEFORE INSERT ON su1_mainline_binding
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_runtime_control WHERE singleton=1 AND mode='MAINLINE_ADAPTER')
 THEN RAISE(ABORT,'SU1_MAINLINE_DISABLED') END;
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_certificate c JOIN su1_prediction p ON p.prediction_id=NEW.prediction_id
  JOIN su1_snapshot s ON s.snapshot_hash=p.snapshot_hash
  WHERE c.certificate_id=NEW.certificate_id AND c.strategy_id=NEW.strategy_id AND p.strategy_id=NEW.strategy_id
  AND p.target_issue=NEW.target_issue AND NEW.bound_at>=p.captured_at
  AND NEW.bound_at>=c.issued_at AND NEW.bound_at<s.target_open_time AND NEW.bound_at<c.expires_at
  AND NOT EXISTS(SELECT 1 FROM su1_certificate_revocation r WHERE r.certificate_id=c.certificate_id))
 THEN RAISE(ABORT,'SU1_MAINLINE_CERTIFICATE_MISMATCH') END;
 -- A null-control PASS discovered after issuance still blocks every binding in that family.
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_certificate c JOIN su1_evidence_look e ON e.evidence_hash=c.evidence_hash
  JOIN su1_eval_cohort ec ON ec.cohort_id=e.cohort_id
  JOIN su1_eval_cohort xc ON xc.family_root_hash=ec.family_root_hash AND xc.claim_kind='NULL_CONTROL'
  JOIN su1_evidence_look x ON x.cohort_id=xc.cohort_id AND x.result_state='PASS'
  WHERE c.certificate_id=NEW.certificate_id)
 THEN RAISE(ABORT,'SU1_NULL_CONTROL_ALARM') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_outbox_identity BEFORE UPDATE ON su1_outbox
WHEN NEW.event_id<>OLD.event_id OR NEW.event_type<>OLD.event_type OR NEW.prediction_id<>OLD.prediction_id
 OR NEW.settlement_id IS NOT OLD.settlement_id OR NEW.source_record_hash<>OLD.source_record_hash OR NEW.created_at<>OLD.created_at
BEGIN SELECT RAISE(ABORT,'SU1_OUTBOX_IDENTITY_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_outbox_transition BEFORE UPDATE ON su1_outbox
BEGIN
 SELECT CASE WHEN NOT ((OLD.state IN ('PENDING','LEASED') AND NEW.state='LEASED'
   AND NEW.lease_generation=OLD.lease_generation+1 AND NEW.attempt_count=OLD.attempt_count+1)
  OR (OLD.state='LEASED' AND NEW.state IN ('PENDING','DONE','DEAD')
   AND NEW.lease_generation=OLD.lease_generation AND NEW.attempt_count=OLD.attempt_count)
  OR (OLD.state='DEAD' AND NEW.state='PENDING' AND NEW.lease_generation=OLD.lease_generation
   AND NEW.attempt_count=OLD.attempt_count))
 THEN RAISE(ABORT,'SU1_OUTBOX_STATE_TRANSITION') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_task_identity BEFORE UPDATE ON su1_research_task
WHEN NEW.task_id<>OLD.task_id OR NEW.intent_hash<>OLD.intent_hash OR NEW.feedback_hash IS NOT OLD.feedback_hash
 OR NEW.kind<>OLD.kind OR NEW.input_hash<>OLD.input_hash OR NEW.boundary_sequence<>OLD.boundary_sequence
 OR NEW.cpu_budget_ms<>OLD.cpu_budget_ms OR NEW.byte_budget<>OLD.byte_budget OR NEW.created_at<>OLD.created_at
BEGIN SELECT RAISE(ABORT,'SU1_TASK_IDENTITY_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_task_transition BEFORE UPDATE ON su1_research_task
BEGIN
 SELECT CASE WHEN NOT ((OLD.state IN ('PENDING','WAITING','LEASED') AND NEW.state='LEASED'
   AND NEW.lease_generation=OLD.lease_generation+1 AND NEW.attempt_count=OLD.attempt_count+1)
  OR (OLD.state='LEASED' AND NEW.state IN ('WAITING','COMPLETED','FAILED','CANCELLED')
   AND NEW.lease_generation=OLD.lease_generation AND NEW.attempt_count=OLD.attempt_count)
  OR (OLD.state IN ('PENDING','WAITING') AND NEW.state='CANCELLED'
   AND NEW.lease_generation=OLD.lease_generation AND NEW.attempt_count=OLD.attempt_count))
 THEN RAISE(ABORT,'SU1_TASK_STATE_TRANSITION') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_blob_archive_guard BEFORE UPDATE ON su1_blob
BEGIN
 SELECT CASE WHEN NOT (NEW.blob_hash=OLD.blob_hash AND NEW.byte_count=OLD.byte_count AND NEW.codec=OLD.codec
  AND NEW.created_at=OLD.created_at AND OLD.storage_state='LOCAL' AND NEW.storage_state='ARCHIVED'
  AND NEW.archive_receipt_id IS NOT NULL AND NEW.local_locator IS NULL)
 THEN RAISE(ABORT,'SU1_BLOB_ARCHIVE_ONLY') END;
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_retention_pin WHERE object_kind='BLOB'
  AND object_id=OLD.blob_hash AND released_at IS NULL)
 THEN RAISE(ABORT,'SU1_BLOB_PINNED') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_pin_release_only BEFORE UPDATE ON su1_retention_pin
BEGIN
 SELECT CASE WHEN NOT(NEW.pin_id=OLD.pin_id AND NEW.object_kind=OLD.object_kind AND NEW.object_id=OLD.object_id
  AND NEW.reason=OLD.reason AND NEW.created_at=OLD.created_at AND OLD.released_at IS NULL AND NEW.released_at IS NOT NULL)
 THEN RAISE(ABORT,'SU1_PIN_RELEASE_ONLY') END;
END;

-- Immutable-table guards are generated below as explicit portable SQL statements.

CREATE TRIGGER IF NOT EXISTS su1_archive_receipt_no_replace BEFORE INSERT ON su1_archive_receipt
WHEN EXISTS(SELECT 1 FROM su1_archive_receipt WHERE receipt_id=NEW.receipt_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_archive_receipt_no_delete BEFORE DELETE ON su1_archive_receipt
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_archive_receipt_immutable BEFORE UPDATE ON su1_archive_receipt
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_snapshot_no_replace BEFORE INSERT ON su1_snapshot
WHEN EXISTS(SELECT 1 FROM su1_snapshot WHERE snapshot_hash=NEW.snapshot_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_snapshot_no_delete BEFORE DELETE ON su1_snapshot
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_snapshot_immutable BEFORE UPDATE ON su1_snapshot
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_relation_asset_no_replace BEFORE INSERT ON su1_relation_asset
WHEN EXISTS(SELECT 1 FROM su1_relation_asset WHERE asset_hash=NEW.asset_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_relation_asset_no_delete BEFORE DELETE ON su1_relation_asset
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_relation_asset_immutable BEFORE UPDATE ON su1_relation_asset
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_program_no_replace BEFORE INSERT ON su1_program
WHEN EXISTS(SELECT 1 FROM su1_program WHERE program_hash=NEW.program_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_program_no_delete BEFORE DELETE ON su1_program
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_program_immutable BEFORE UPDATE ON su1_program
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_train_protocol_no_replace BEFORE INSERT ON su1_train_protocol
WHEN EXISTS(SELECT 1 FROM su1_train_protocol WHERE protocol_hash=NEW.protocol_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_train_protocol_no_delete BEFORE DELETE ON su1_train_protocol
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_train_protocol_immutable BEFORE UPDATE ON su1_train_protocol
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_model_artifact_no_replace BEFORE INSERT ON su1_model_artifact
WHEN EXISTS(SELECT 1 FROM su1_model_artifact WHERE artifact_hash=NEW.artifact_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_model_artifact_no_delete BEFORE DELETE ON su1_model_artifact
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_model_artifact_immutable BEFORE UPDATE ON su1_model_artifact
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_strategy_version_no_replace BEFORE INSERT ON su1_strategy_version
WHEN EXISTS(SELECT 1 FROM su1_strategy_version WHERE strategy_id=NEW.strategy_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_strategy_version_no_delete BEFORE DELETE ON su1_strategy_version
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_strategy_version_immutable BEFORE UPDATE ON su1_strategy_version
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_eval_cohort_no_replace BEFORE INSERT ON su1_eval_cohort
WHEN EXISTS(SELECT 1 FROM su1_eval_cohort WHERE cohort_id=NEW.cohort_id) OR EXISTS(SELECT 1 FROM su1_eval_cohort WHERE claim_id=NEW.claim_id) OR EXISTS(SELECT 1 FROM su1_eval_cohort WHERE family_root_hash=NEW.family_root_hash AND global_claim_ordinal=NEW.global_claim_ordinal)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_eval_cohort_no_delete BEFORE DELETE ON su1_eval_cohort
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_eval_cohort_immutable BEFORE UPDATE ON su1_eval_cohort
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_opportunity_no_replace BEFORE INSERT ON su1_opportunity
WHEN EXISTS(SELECT 1 FROM su1_opportunity WHERE opportunity_id=NEW.opportunity_id) OR EXISTS(SELECT 1 FROM su1_opportunity WHERE cohort_id=NEW.cohort_id AND target_issue=NEW.target_issue) OR EXISTS(SELECT 1 FROM su1_opportunity WHERE cohort_id=NEW.cohort_id AND opportunity_ordinal=NEW.opportunity_ordinal)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_opportunity_no_delete BEFORE DELETE ON su1_opportunity
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_opportunity_immutable BEFORE UPDATE ON su1_opportunity
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_prediction_no_replace BEFORE INSERT ON su1_prediction
WHEN EXISTS(SELECT 1 FROM su1_prediction WHERE prediction_id=NEW.prediction_id) OR EXISTS(SELECT 1 FROM su1_prediction WHERE record_hash=NEW.record_hash) OR EXISTS(SELECT 1 FROM su1_prediction WHERE opportunity_id=NEW.opportunity_id AND strategy_id=NEW.strategy_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_prediction_no_delete BEFORE DELETE ON su1_prediction
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_prediction_immutable BEFORE UPDATE ON su1_prediction
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_settlement_no_replace BEFORE INSERT ON su1_settlement
WHEN EXISTS(SELECT 1 FROM su1_settlement WHERE settlement_id=NEW.settlement_id) OR EXISTS(SELECT 1 FROM su1_settlement WHERE prediction_id=NEW.prediction_id) OR EXISTS(SELECT 1 FROM su1_settlement WHERE label_hash=NEW.label_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_settlement_no_delete BEFORE DELETE ON su1_settlement
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_settlement_immutable BEFORE UPDATE ON su1_settlement
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_feedback_no_replace BEFORE INSERT ON su1_feedback
WHEN EXISTS(SELECT 1 FROM su1_feedback WHERE feedback_hash=NEW.feedback_hash) OR EXISTS(SELECT 1 FROM su1_feedback WHERE settlement_id=NEW.settlement_id AND kind=NEW.kind AND diagnostic_contract_hash=NEW.diagnostic_contract_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_feedback_no_delete BEFORE DELETE ON su1_feedback
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_feedback_immutable BEFORE UPDATE ON su1_feedback
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_lineage_no_replace BEFORE INSERT ON su1_lineage
WHEN EXISTS(SELECT 1 FROM su1_lineage WHERE parent_strategy_id=NEW.parent_strategy_id AND child_strategy_id=NEW.child_strategy_id AND task_id=NEW.task_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_lineage_no_delete BEFORE DELETE ON su1_lineage
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_lineage_immutable BEFORE UPDATE ON su1_lineage
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_evidence_look_no_replace BEFORE INSERT ON su1_evidence_look
WHEN EXISTS(SELECT 1 FROM su1_evidence_look WHERE evidence_hash=NEW.evidence_hash) OR EXISTS(SELECT 1 FROM su1_evidence_look WHERE cohort_id=NEW.cohort_id AND claim_id=NEW.claim_id AND look_ordinal=NEW.look_ordinal)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_evidence_look_no_delete BEFORE DELETE ON su1_evidence_look
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_evidence_look_immutable BEFORE UPDATE ON su1_evidence_look
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_certificate_no_replace BEFORE INSERT ON su1_certificate
WHEN EXISTS(SELECT 1 FROM su1_certificate WHERE certificate_id=NEW.certificate_id) OR EXISTS(SELECT 1 FROM su1_certificate WHERE evidence_hash=NEW.evidence_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_certificate_no_delete BEFORE DELETE ON su1_certificate
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_certificate_immutable BEFORE UPDATE ON su1_certificate
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_certificate_revocation_no_replace BEFORE INSERT ON su1_certificate_revocation
WHEN EXISTS(SELECT 1 FROM su1_certificate_revocation WHERE certificate_id=NEW.certificate_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_certificate_revocation_no_delete BEFORE DELETE ON su1_certificate_revocation
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_certificate_revocation_immutable BEFORE UPDATE ON su1_certificate_revocation
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_mainline_binding_no_replace BEFORE INSERT ON su1_mainline_binding
WHEN EXISTS(SELECT 1 FROM su1_mainline_binding WHERE target_issue=NEW.target_issue) OR EXISTS(SELECT 1 FROM su1_mainline_binding WHERE prediction_id=NEW.prediction_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_mainline_binding_no_delete BEFORE DELETE ON su1_mainline_binding
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_mainline_binding_immutable BEFORE UPDATE ON su1_mainline_binding
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_dependency_no_replace BEFORE INSERT ON su1_dependency
WHEN EXISTS(SELECT 1 FROM su1_dependency WHERE source_kind=NEW.source_kind AND source_id=NEW.source_id AND target_kind=NEW.target_kind AND target_id=NEW.target_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_dependency_no_delete BEFORE DELETE ON su1_dependency
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_dependency_immutable BEFORE UPDATE ON su1_dependency
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_blob_no_replace BEFORE INSERT ON su1_blob
WHEN EXISTS(SELECT 1 FROM su1_blob WHERE blob_hash=NEW.blob_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_blob_no_delete BEFORE DELETE ON su1_blob
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

CREATE TRIGGER IF NOT EXISTS su1_outbox_no_replace BEFORE INSERT ON su1_outbox
WHEN EXISTS(SELECT 1 FROM su1_outbox WHERE event_id=NEW.event_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_outbox_no_delete BEFORE DELETE ON su1_outbox
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

CREATE TRIGGER IF NOT EXISTS su1_research_task_no_replace BEFORE INSERT ON su1_research_task
WHEN EXISTS(SELECT 1 FROM su1_research_task WHERE task_id=NEW.task_id) OR EXISTS(SELECT 1 FROM su1_research_task WHERE intent_hash=NEW.intent_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_research_task_no_delete BEFORE DELETE ON su1_research_task
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

CREATE TRIGGER IF NOT EXISTS su1_retention_pin_no_replace BEFORE INSERT ON su1_retention_pin
WHEN EXISTS(SELECT 1 FROM su1_retention_pin WHERE pin_id=NEW.pin_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_retention_pin_no_delete BEFORE DELETE ON su1_retention_pin
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;

CREATE TRIGGER IF NOT EXISTS su1_look_plan_no_replace BEFORE INSERT ON su1_look_plan
WHEN EXISTS(SELECT 1 FROM su1_look_plan WHERE cohort_id=NEW.cohort_id AND look_ordinal=NEW.look_ordinal)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_look_plan_no_delete BEFORE DELETE ON su1_look_plan
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_look_plan_immutable BEFORE UPDATE ON su1_look_plan
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS su1_family_budget_no_replace BEFORE INSERT ON su1_family_budget
WHEN EXISTS(SELECT 1 FROM su1_family_budget WHERE family_root_hash=NEW.family_root_hash)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_family_budget_no_delete BEFORE DELETE ON su1_family_budget
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_family_budget_immutable BEFORE UPDATE ON su1_family_budget
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_alpha_root_no_delete BEFORE DELETE ON su1_alpha_root
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_alpha_root_immutable BEFORE UPDATE ON su1_alpha_root
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_pending_settlement_insert_guard BEFORE INSERT ON su1_pending_settlement
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_prediction WHERE prediction_id=NEW.prediction_id
  AND target_issue=NEW.target_issue AND captured_at=NEW.enqueued_at)
 OR EXISTS(SELECT 1 FROM su1_pending_settlement WHERE prediction_id=NEW.prediction_id)
 THEN RAISE(ABORT,'SU1_PENDING_BINDING_OR_REPLAY') END;
END;

CREATE TRIGGER IF NOT EXISTS su1_opportunity_outcome_no_replace BEFORE INSERT ON su1_opportunity_outcome
WHEN EXISTS(SELECT 1 FROM su1_opportunity_outcome WHERE opportunity_id=NEW.opportunity_id)
BEGIN SELECT RAISE(ABORT,'SU1_COMPARE_REPLAY_BEFORE_INSERT'); END;
CREATE TRIGGER IF NOT EXISTS su1_opportunity_outcome_no_delete BEFORE DELETE ON su1_opportunity_outcome
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_opportunity_outcome_immutable BEFORE UPDATE ON su1_opportunity_outcome
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
