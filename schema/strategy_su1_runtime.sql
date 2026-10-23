-- Runtime bindings and durable burst cursor. All writes use the existing research owner.
CREATE TABLE IF NOT EXISTS su1_alpha_reservation (
 reservation_id TEXT PRIMARY KEY NOT NULL,
 origin TEXT NOT NULL CHECK(origin IN ('LEGACY_CP3','LEGACY_R932','SU1')),
 amount REAL NOT NULL CHECK(amount>0 AND amount<=0.05),
 created_at INTEGER NOT NULL
);
CREATE TRIGGER IF NOT EXISTS su1_alpha_reservation_cap BEFORE INSERT ON su1_alpha_reservation
BEGIN
 SELECT CASE WHEN NEW.amount+(SELECT coalesce(sum(amount),0) FROM su1_alpha_reservation)>
 (SELECT global_alpha FROM su1_alpha_root WHERE singleton=1)+0.000000000001
 THEN RAISE(ABORT,'SU1_LIFETIME_GLOBAL_ALPHA_EXHAUSTED') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_alpha_reservation_update BEFORE UPDATE ON su1_alpha_reservation
BEGIN SELECT RAISE(ABORT,'SU1_ALPHA_IRREVOCABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_alpha_reservation_delete BEFORE DELETE ON su1_alpha_reservation
BEGIN SELECT RAISE(ABORT,'SU1_ALPHA_IRREVOCABLE'); END;
CREATE TABLE IF NOT EXISTS su1_source_clock (
 singleton INTEGER PRIMARY KEY CHECK(singleton=1),
 generation INTEGER NOT NULL CHECK(generation>=0)
);
INSERT OR IGNORE INTO su1_source_clock VALUES(1,0);
CREATE TRIGGER IF NOT EXISTS su1_draw_insert_clock AFTER INSERT ON draws
BEGIN UPDATE su1_source_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_draw_update_clock AFTER UPDATE ON draws
BEGIN UPDATE su1_source_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_draw_delete_clock AFTER DELETE ON draws
BEGIN UPDATE su1_source_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TABLE IF NOT EXISTS su1_runtime_program (
 strategy_id TEXT PRIMARY KEY NOT NULL REFERENCES su1_strategy_version(strategy_id),
 canonical_text TEXT NOT NULL CHECK(length(canonical_text) BETWEEN 3 AND 4096),
 coverage REAL NOT NULL CHECK(coverage>0 AND coverage<=1),
 margin_gate INTEGER NOT NULL CHECK(margin_gate IN (0,1)),
 parent_id TEXT REFERENCES su1_strategy_version(strategy_id),
 created_at INTEGER NOT NULL,
 kernel_hash TEXT NOT NULL DEFAULT '0000000000000000000000000000000000000000000000000000000000000000' CHECK(length(kernel_hash)=64)
);
CREATE TABLE IF NOT EXISTS su1_kernel_state (
 singleton INTEGER PRIMARY KEY CHECK(singleton=1),
 kernel_hash TEXT NOT NULL CHECK(length(kernel_hash)=64),
 updated_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS su1_cohort_runtime (
 cohort_id TEXT PRIMARY KEY NOT NULL REFERENCES su1_eval_cohort(cohort_id),
 last_issue TEXT NOT NULL,
 policy_revision TEXT NOT NULL CHECK(policy_revision='SU1_ALL_CALENDAR_K3_CONFIRM_V1'),
 state TEXT NOT NULL DEFAULT 'OPEN' CHECK(state IN ('OPEN','STOPPED','CLOSED')),
 stop_reason TEXT NOT NULL DEFAULT ''
);
CREATE TRIGGER IF NOT EXISTS su1_cohort_runtime_immutable BEFORE UPDATE ON su1_cohort_runtime
BEGIN
 SELECT CASE WHEN NEW.cohort_id<>OLD.cohort_id OR NEW.last_issue<>OLD.last_issue OR NEW.policy_revision<>OLD.policy_revision
 OR OLD.state<>'OPEN' OR NEW.state NOT IN ('STOPPED','CLOSED')
 THEN RAISE(ABORT,'SU1_COHORT_RUNTIME_TRANSITION') END;
END;
CREATE TABLE IF NOT EXISTS su1_quality_receipt (
 strategy_id TEXT PRIMARY KEY NOT NULL REFERENCES su1_strategy_version(strategy_id),
 payload_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 recorded_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS su1_sealed_plan (
 campaign_id TEXT PRIMARY KEY NOT NULL REFERENCES su1_campaign(campaign_id),
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 dev_first INTEGER NOT NULL,
 dev_last INTEGER NOT NULL,
 val_first INTEGER NOT NULL,
 val_last INTEGER NOT NULL,
 first_sequence INTEGER NOT NULL,
 last_sequence INTEGER NOT NULL,
 selected_at INTEGER NOT NULL,
 CHECK(dev_first<=dev_last AND dev_last<val_first AND val_first<=val_last AND val_last<first_sequence AND first_sequence<=last_sequence)
);
CREATE TRIGGER IF NOT EXISTS su1_sealed_plan_immutable BEFORE UPDATE ON su1_sealed_plan
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TABLE IF NOT EXISTS su1_sealed_cancelled (
 campaign_id TEXT PRIMARY KEY NOT NULL REFERENCES su1_sealed_plan(campaign_id),
 reason TEXT NOT NULL,
 cancelled_at INTEGER NOT NULL
);
CREATE TRIGGER IF NOT EXISTS su1_sealed_cancelled_immutable BEFORE UPDATE ON su1_sealed_cancelled
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TABLE IF NOT EXISTS su1_sealed_result (
 plan_campaign TEXT PRIMARY KEY NOT NULL REFERENCES su1_sealed_plan(campaign_id),
 open_campaign TEXT UNIQUE NOT NULL REFERENCES su1_campaign(campaign_id),
 eligibility TEXT NOT NULL CHECK(eligibility IN ('PASS_SCREEN','FAIL_SCREEN')),
 recorded_at INTEGER NOT NULL
);
CREATE TRIGGER IF NOT EXISTS su1_sealed_result_immutable BEFORE UPDATE ON su1_sealed_result
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TABLE IF NOT EXISTS su1_calibration_attempt (
 source_strategy TEXT PRIMARY KEY NOT NULL REFERENCES su1_strategy_version(strategy_id),
 result_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 calibrated_strategy TEXT UNIQUE REFERENCES su1_strategy_version(strategy_id),
 first_sequence INTEGER NOT NULL,
 last_sequence INTEGER NOT NULL,
 recorded_at INTEGER NOT NULL,
 CHECK(first_sequence<last_sequence)
);
CREATE TRIGGER IF NOT EXISTS su1_calibration_attempt_immutable BEFORE UPDATE ON su1_calibration_attempt
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TABLE IF NOT EXISTS su1_portable_import (
 artifact_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 kernel_hash TEXT NOT NULL CHECK(length(kernel_hash)=64),
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 source_through_issue TEXT NOT NULL,
 recorded_at INTEGER NOT NULL,
 PRIMARY KEY(artifact_hash,kernel_hash)
);
CREATE TRIGGER IF NOT EXISTS su1_portable_import_immutable BEFORE UPDATE ON su1_portable_import
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_quality_receipt_immutable BEFORE UPDATE ON su1_quality_receipt
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TABLE IF NOT EXISTS su1_certificate_runtime (
 certificate_id TEXT PRIMARY KEY NOT NULL REFERENCES su1_certificate(certificate_id),
 kernel_hash TEXT NOT NULL CHECK(length(kernel_hash)=64),
 quality_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 family_root_hash TEXT NOT NULL REFERENCES su1_family_budget(family_root_hash),
 descriptor_hash TEXT NOT NULL CHECK(length(descriptor_hash)=64)
);
CREATE TRIGGER IF NOT EXISTS su1_certificate_runtime_immutable BEFORE UPDATE ON su1_certificate_runtime
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_runtime_program_immutable BEFORE UPDATE ON su1_runtime_program
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_runtime_program_no_delete BEFORE DELETE ON su1_runtime_program
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TABLE IF NOT EXISTS su1_live_target (
 sequence_ordinal INTEGER PRIMARY KEY NOT NULL CHECK(sequence_ordinal>0),
 target_issue TEXT UNIQUE NOT NULL,
 target_open_time INTEGER NOT NULL,
 snapshot_hash TEXT NOT NULL REFERENCES su1_snapshot(snapshot_hash),
 legacy_settlement_hash TEXT,
 completed_at INTEGER,
 CHECK((legacy_settlement_hash IS NULL AND completed_at IS NULL) OR (length(legacy_settlement_hash)=64 AND completed_at>=target_open_time))
);
CREATE TRIGGER IF NOT EXISTS su1_live_target_update BEFORE UPDATE ON su1_live_target
BEGIN
 SELECT CASE WHEN OLD.completed_at IS NOT NULL OR NEW.completed_at IS NULL OR NEW.sequence_ordinal<>OLD.sequence_ordinal
 OR NEW.target_issue<>OLD.target_issue OR NEW.target_open_time<>OLD.target_open_time OR NEW.snapshot_hash<>OLD.snapshot_hash
 THEN RAISE(ABORT,'SU1_TARGET_IMMUTABLE') END;
END;
CREATE TABLE IF NOT EXISTS su1_live_attempt (
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 sequence_ordinal INTEGER NOT NULL REFERENCES su1_live_target(sequence_ordinal),
 outcome TEXT NOT NULL CHECK(outcome IN ('FROZEN','GATE_SKIP','BUDGET_SKIP','COMPUTE_FAILED')),
 detail TEXT NOT NULL,
 created_at INTEGER NOT NULL,
 PRIMARY KEY(strategy_id,sequence_ordinal)
) WITHOUT ROWID;
CREATE TRIGGER IF NOT EXISTS su1_live_attempt_immutable BEFORE UPDATE ON su1_live_attempt
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TABLE IF NOT EXISTS su1_live_arm_stats (
 arm TEXT PRIMARY KEY NOT NULL,
 actions INTEGER NOT NULL CHECK(actions>=0),
 hits INTEGER NOT NULL CHECK(hits>=0 AND hits<=actions),
 through_sequence INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS su1_search_attempt (
 attempt_id TEXT PRIMARY KEY NOT NULL CHECK(length(attempt_id)=64),
 genome_key TEXT NOT NULL CHECK(length(genome_key)=64),
 campaign_id TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 program_text TEXT NOT NULL,
 arm TEXT NOT NULL,
 operator TEXT NOT NULL,
 parent_id TEXT,
 dev_fitness REAL,
 val_fitness REAL,
 result TEXT NOT NULL CHECK(result IN ('EVALUATED','REJECTED','YIELD')),
 detail TEXT NOT NULL,
 created_at INTEGER NOT NULL
);
CREATE TRIGGER IF NOT EXISTS su1_search_attempt_immutable BEFORE UPDATE ON su1_search_attempt
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TABLE IF NOT EXISTS su1_burst_checkpoint (
 singleton INTEGER PRIMARY KEY CHECK(singleton=1),
 generation INTEGER NOT NULL CHECK(generation>=0),
 dataset_hash TEXT NOT NULL CHECK(length(dataset_hash)=64),
 cursor_json TEXT NOT NULL CHECK(length(CAST(cursor_json AS BLOB))<=65536),
 state_hash TEXT NOT NULL CHECK(length(state_hash)=64),
 updated_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS su1_health_event (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 phase TEXT NOT NULL,
 detail TEXT NOT NULL,
 elapsed_micros INTEGER NOT NULL CHECK(elapsed_micros>=0),
 created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS su1_retention_clock(singleton INTEGER PRIMARY KEY CHECK(singleton=1),generation INTEGER NOT NULL);
INSERT OR IGNORE INTO su1_retention_clock VALUES(1,0);
CREATE TRIGGER IF NOT EXISTS su1_retention_research_pool_insert AFTER INSERT ON su1_research_pool
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_research_pool_update AFTER UPDATE ON su1_research_pool
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_cohort_runtime_insert AFTER INSERT ON su1_cohort_runtime
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_cohort_runtime_update AFTER UPDATE ON su1_cohort_runtime
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_certificate_insert AFTER INSERT ON su1_certificate
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_certificate_update AFTER UPDATE ON su1_certificate
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_burst_checkpoint_insert AFTER INSERT ON su1_burst_checkpoint
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_burst_checkpoint_update AFTER UPDATE ON su1_burst_checkpoint
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_live_target_insert AFTER INSERT ON su1_live_target
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_live_target_update AFTER UPDATE ON su1_live_target
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_prediction_insert AFTER INSERT ON su1_prediction
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_prediction_update AFTER UPDATE ON su1_prediction
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_settlement_insert AFTER INSERT ON su1_settlement
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_settlement_update AFTER UPDATE ON su1_settlement
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_research_task_insert AFTER INSERT ON su1_research_task
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_research_task_update AFTER UPDATE ON su1_research_task
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;

CREATE TABLE IF NOT EXISTS su1_live_day_archive (
 strategy_id TEXT NOT NULL REFERENCES su1_strategy_version(strategy_id),
 day_key INTEGER NOT NULL,
 payload_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 actions INTEGER NOT NULL,
 hits INTEGER NOT NULL,
 created_at INTEGER NOT NULL,
 compacted_at INTEGER,
 PRIMARY KEY(strategy_id,day_key)
);
CREATE TRIGGER IF NOT EXISTS su1_live_day_archive_update BEFORE UPDATE ON su1_live_day_archive
BEGIN
 SELECT CASE WHEN OLD.compacted_at IS NOT NULL OR NEW.compacted_at IS NULL OR NEW.strategy_id<>OLD.strategy_id OR NEW.day_key<>OLD.day_key OR NEW.payload_hash<>OLD.payload_hash OR NEW.actions<>OLD.actions OR NEW.hits<>OLD.hits OR NEW.created_at<>OLD.created_at
 THEN RAISE(ABORT,'SU1_DAILY_ARCHIVE_IMMUTABLE') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_live_shadow_verified_compaction BEFORE DELETE ON su1_live_shadow
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_live_day_archive a JOIN su1_blob b ON b.blob_hash=a.payload_hash JOIN su1_archive_receipt r ON r.receipt_id=b.archive_receipt_id WHERE a.strategy_id=OLD.strategy_id AND a.day_key=OLD.day_key AND b.storage_state='ARCHIVED' AND r.replay_verified=1)
 THEN RAISE(ABORT,'SU1_VERIFIED_ARCHIVE_REQUIRED') END;
END;

CREATE TABLE IF NOT EXISTS su1_campaign_curve (
 campaign_id TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 generation INTEGER NOT NULL,
 dev_fitness REAL NOT NULL,
 champion_val_fitness REAL NOT NULL,
 champion_family TEXT NOT NULL,
 recorded_at INTEGER NOT NULL,
 PRIMARY KEY(campaign_id,generation)
);
CREATE TRIGGER IF NOT EXISTS su1_curve_monotone BEFORE INSERT ON su1_campaign_curve
BEGIN
 SELECT CASE WHEN EXISTS(SELECT 1 FROM su1_campaign_curve c WHERE c.campaign_id=NEW.campaign_id AND (c.generation>=NEW.generation OR c.champion_val_fitness>NEW.champion_val_fitness))
 THEN RAISE(ABORT,'SU1_CHAMPION_CURVE_ORDER') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_curve_immutable BEFORE UPDATE ON su1_campaign_curve
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;

CREATE TABLE IF NOT EXISTS su1_mainline_head_event (
 generation INTEGER PRIMARY KEY CHECK(generation>=1),
 event_hash TEXT NOT NULL UNIQUE CHECK(length(event_hash)=64),
 previous_hash TEXT NOT NULL CHECK(length(previous_hash)=64),
 strategy_id TEXT REFERENCES su1_strategy_version(strategy_id),
 certificate_id TEXT REFERENCES su1_certificate(certificate_id),
 operation TEXT NOT NULL CHECK(operation IN ('BOOTSTRAP','PROMOTE','RENEW','FALLBACK','REVOKE','USER_ROLLBACK')),
 policy_hash TEXT NOT NULL CHECK(length(policy_hash)=64),
 recorded_at INTEGER NOT NULL,
 CHECK((strategy_id IS NULL AND certificate_id IS NULL AND operation='REVOKE') OR
       (strategy_id IS NOT NULL AND certificate_id IS NOT NULL))
);
CREATE TRIGGER IF NOT EXISTS su1_mainline_head_sequence BEFORE INSERT ON su1_mainline_head_event
BEGIN
 SELECT CASE WHEN NEW.generation<>coalesce((SELECT max(generation)+1 FROM su1_mainline_head_event),1)
   THEN RAISE(ABORT,'SU1_MAINLINE_HEAD_GENERATION') END;
 SELECT CASE WHEN NEW.previous_hash<>coalesce((SELECT event_hash FROM su1_mainline_head_event ORDER BY generation DESC LIMIT 1),'0000000000000000000000000000000000000000000000000000000000000000')
   THEN RAISE(ABORT,'SU1_MAINLINE_HEAD_PREVIOUS') END;
 SELECT CASE WHEN NEW.certificate_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM su1_certificate c WHERE c.certificate_id=NEW.certificate_id AND c.strategy_id=NEW.strategy_id AND c.issued_at<=NEW.recorded_at AND c.expires_at>NEW.recorded_at AND NOT EXISTS(SELECT 1 FROM su1_certificate_revocation r WHERE r.certificate_id=c.certificate_id))
   THEN RAISE(ABORT,'SU1_MAINLINE_HEAD_CERTIFICATE') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_mainline_head_no_update BEFORE UPDATE ON su1_mainline_head_event
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_mainline_head_no_delete BEFORE DELETE ON su1_mainline_head_event
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_retention_dependency_insert AFTER INSERT ON su1_dependency
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_lineage_insert AFTER INSERT ON su1_lineage
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_head_insert AFTER INSERT ON su1_mainline_head_event
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_revocation_insert AFTER INSERT ON su1_certificate_revocation
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TABLE IF NOT EXISTS su1_research_knowledge (
 attempt_id TEXT PRIMARY KEY REFERENCES su1_search_attempt(attempt_id),
 context_key TEXT NOT NULL,
 hypothesis TEXT NOT NULL,
 action TEXT NOT NULL,
 cost_units INTEGER NOT NULL CHECK(cost_units>=1),
 result TEXT NOT NULL,
 payload_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 recorded_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS su1_knowledge_lookup ON su1_research_knowledge(context_key,action,result,recorded_at);
CREATE TRIGGER IF NOT EXISTS su1_research_knowledge_immutable BEFORE UPDATE ON su1_research_knowledge
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_research_knowledge_no_delete BEFORE DELETE ON su1_research_knowledge
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TABLE IF NOT EXISTS su1_campaign_audit (
 source_campaign TEXT NOT NULL REFERENCES su1_campaign(campaign_id),
 variant TEXT NOT NULL CHECK(variant IN ('REAL','TIME_SHUFFLED','PLANTED_SIGNAL','NO_DIRECTOR','NO_MEMORY')),
 audit_campaign TEXT NOT NULL UNIQUE REFERENCES su1_campaign(campaign_id),
 dataset_blob TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 generation INTEGER NOT NULL CHECK(generation>=0),
 checkpoint_hash TEXT REFERENCES su1_blob(blob_hash),
 phase TEXT NOT NULL CHECK(phase IN ('SEARCH','SEALED','DONE','WAIT_HISTORY')),
 selected_hash TEXT CHECK(selected_hash IS NULL OR length(selected_hash)=64),
 selected_program TEXT,
 updated_at INTEGER NOT NULL,
 PRIMARY KEY(source_campaign,variant),
 CHECK(phase<>'SEALED' OR (selected_hash IS NOT NULL AND selected_program IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS su1_audit_work ON su1_campaign_audit(phase,updated_at);
CREATE TABLE IF NOT EXISTS su1_campaign_audit_open (
 source_campaign TEXT NOT NULL,
 variant TEXT NOT NULL,
 selected_hash TEXT,
 report_hash TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 opened_at INTEGER NOT NULL,
 PRIMARY KEY(source_campaign,variant),
 FOREIGN KEY(source_campaign,variant) REFERENCES su1_campaign_audit(source_campaign,variant)
);
CREATE TRIGGER IF NOT EXISTS su1_audit_open_once BEFORE INSERT ON su1_campaign_audit_open
BEGIN
 SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM su1_campaign_audit a WHERE a.source_campaign=NEW.source_campaign AND a.variant=NEW.variant AND (a.phase='SEALED' AND a.selected_hash=NEW.selected_hash OR a.phase='SEARCH' AND a.selected_hash IS NULL AND NEW.selected_hash IS NULL))
 THEN RAISE(ABORT,'SU1_AUDIT_CHAMPION_NOT_FROZEN') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_audit_checkpoint_cas BEFORE UPDATE ON su1_campaign_audit
BEGIN
 SELECT CASE WHEN OLD.phase IN ('DONE','WAIT_HISTORY') OR NEW.generation<>OLD.generation+1 OR NEW.source_campaign<>OLD.source_campaign OR NEW.variant<>OLD.variant OR NEW.audit_campaign<>OLD.audit_campaign OR NEW.dataset_blob<>OLD.dataset_blob OR NEW.updated_at<OLD.updated_at
 OR OLD.phase='SEALED' AND (NEW.selected_hash IS NOT OLD.selected_hash OR NEW.selected_program IS NOT OLD.selected_program OR NEW.phase NOT IN ('SEALED','DONE'))
 OR NEW.phase='DONE' AND NOT EXISTS(SELECT 1 FROM su1_campaign_audit_open o WHERE o.source_campaign=OLD.source_campaign AND o.variant=OLD.variant)
 THEN RAISE(ABORT,'SU1_AUDIT_CHECKPOINT_CAS') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_audit_open_no_update BEFORE UPDATE ON su1_campaign_audit_open
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_audit_open_no_delete BEFORE DELETE ON su1_campaign_audit_open
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_audit_no_delete BEFORE DELETE ON su1_campaign_audit
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_retention_audit_insert AFTER INSERT ON su1_campaign_audit
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_retention_audit_update AFTER UPDATE ON su1_campaign_audit
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
-- Paired model ablations are historical research and own no qualification or alpha allocation.
CREATE TABLE IF NOT EXISTS su1_model_ablation (
 source_campaign TEXT PRIMARY KEY NOT NULL REFERENCES su1_campaign(campaign_id),
 dataset_blob TEXT NOT NULL REFERENCES su1_blob(blob_hash),
 kernel_hash TEXT NOT NULL CHECK(length(kernel_hash)=64),
 generation INTEGER NOT NULL CHECK(generation>=0),
 phase TEXT NOT NULL CHECK(phase IN ('TRAIN','LOCKED','DONE','RETIRED')),
 checkpoint_hash TEXT REFERENCES su1_blob(blob_hash),
 frozen_hash TEXT CHECK(frozen_hash IS NULL OR length(frozen_hash)=64),
 report_hash TEXT REFERENCES su1_blob(blob_hash),
 updated_at INTEGER NOT NULL,
 CHECK(phase NOT IN ('LOCKED','DONE') OR checkpoint_hash IS NOT NULL AND frozen_hash IS NOT NULL),
 CHECK(phase<>'DONE' OR report_hash IS NOT NULL)
);
CREATE TRIGGER IF NOT EXISTS su1_model_ablation_start BEFORE INSERT ON su1_model_ablation
BEGIN
 SELECT CASE WHEN NEW.phase<>'TRAIN' OR NEW.generation<>0 OR NEW.checkpoint_hash IS NOT NULL OR NEW.frozen_hash IS NOT NULL OR NEW.report_hash IS NOT NULL
 THEN RAISE(ABORT,'SU1_MODEL_AUDIT_START') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_model_ablation_advance BEFORE UPDATE ON su1_model_ablation
BEGIN
 SELECT CASE WHEN OLD.phase IN ('DONE','RETIRED') OR NEW.generation<>OLD.generation+1 OR NEW.source_campaign<>OLD.source_campaign OR NEW.dataset_blob<>OLD.dataset_blob OR NEW.kernel_hash<>OLD.kernel_hash OR NEW.updated_at<OLD.updated_at
 OR OLD.frozen_hash IS NOT NULL AND NEW.frozen_hash IS NOT OLD.frozen_hash
 OR OLD.phase='TRAIN' AND NEW.phase NOT IN ('TRAIN','LOCKED','RETIRED')
 OR OLD.phase='LOCKED' AND NEW.phase NOT IN ('LOCKED','DONE','RETIRED')
 OR NEW.phase='DONE' AND (OLD.phase<>'LOCKED' OR NEW.report_hash IS NULL)
 OR NEW.report_hash IS NOT NULL AND NEW.phase<>'DONE'
 THEN RAISE(ABORT,'SU1_MODEL_AUDIT_FROZEN_OR_CAS') END;
END;
CREATE TRIGGER IF NOT EXISTS su1_model_ablation_no_delete BEFORE DELETE ON su1_model_ablation
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_model_ablation_retention_insert AFTER INSERT ON su1_model_ablation
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
CREATE TRIGGER IF NOT EXISTS su1_model_ablation_retention_update AFTER UPDATE ON su1_model_ablation
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
-- Stop obsolete research checkpoints without changing a frozen champion or manufacturing a report.
CREATE TABLE IF NOT EXISTS su1_campaign_audit_cancelled (
 source_campaign TEXT NOT NULL,
 variant TEXT NOT NULL,
 reason TEXT NOT NULL CHECK(reason='EXECUTION_KERNEL_CHANGED'),
 cancelled_at INTEGER NOT NULL,
 PRIMARY KEY(source_campaign,variant),
 FOREIGN KEY(source_campaign,variant) REFERENCES su1_campaign_audit(source_campaign,variant)
);
CREATE TRIGGER IF NOT EXISTS su1_audit_cancelled_no_update BEFORE UPDATE ON su1_campaign_audit_cancelled
BEGIN SELECT RAISE(ABORT,'SU1_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS su1_audit_cancelled_no_delete BEFORE DELETE ON su1_campaign_audit_cancelled
BEGIN SELECT RAISE(ABORT,'SU1_EVIDENCE_DELETE_DISABLED_V1'); END;
CREATE TRIGGER IF NOT EXISTS su1_audit_cancelled_retention AFTER INSERT ON su1_campaign_audit_cancelled
BEGIN UPDATE su1_retention_clock SET generation=generation+1 WHERE singleton=1; END;
