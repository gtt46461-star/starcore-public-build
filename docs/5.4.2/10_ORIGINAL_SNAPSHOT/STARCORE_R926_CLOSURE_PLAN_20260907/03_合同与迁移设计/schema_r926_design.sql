-- R926 DESIGN ONLY. Additive user_version=36; mobile migration integration is not implemented here.
-- Compatible SQL subset; execute on a test database only.
PRAGMA foreign_keys=ON;
CREATE TABLE IF NOT EXISTS r926_schema_domain (
 domain TEXT PRIMARY KEY, revision TEXT NOT NULL, contract_hash TEXT NOT NULL,
 state TEXT NOT NULL CHECK(state IN ('PENDING','RUNNING','READY','FAILED')),
 step_cursor INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL, error_code TEXT NOT NULL DEFAULT '');
CREATE TABLE IF NOT EXISTS r926_migration_journal (
 domain TEXT NOT NULL, revision TEXT NOT NULL, step_id TEXT NOT NULL,
 input_hash TEXT NOT NULL, output_hash TEXT, status TEXT NOT NULL,
 backup_id TEXT, started_at INTEGER NOT NULL, completed_at INTEGER, error_code TEXT,
 PRIMARY KEY(domain,revision,step_id));
CREATE TABLE IF NOT EXISTS r926_runtime_event (
 event_id TEXT PRIMARY KEY, game TEXT NOT NULL, target_issue TEXT NOT NULL,
 release_code INTEGER NOT NULL, process_epoch TEXT NOT NULL, cadence_epoch TEXT NOT NULL,
 attempt_id TEXT NOT NULL, owner TEXT NOT NULL, stage TEXT NOT NULL, result TEXT NOT NULL,
 wall_ms INTEGER NOT NULL, elapsed_ns INTEGER NOT NULL, payload_hash TEXT NOT NULL,
 payload_json TEXT NOT NULL, error_code TEXT, idempotency_key TEXT NOT NULL UNIQUE);
CREATE INDEX IF NOT EXISTS r926_event_issue ON r926_runtime_event(game,target_issue,wall_ms,event_id);
CREATE INDEX IF NOT EXISTS r926_event_attempt ON r926_runtime_event(attempt_id,stage);
CREATE TABLE IF NOT EXISTS r926_issue_projection (
 game TEXT NOT NULL, target_issue TEXT NOT NULL, event_watermark TEXT NOT NULL,
 stage TEXT NOT NULL, version INTEGER NOT NULL, projection_json TEXT NOT NULL,
 projected_at INTEGER NOT NULL, PRIMARY KEY(game,target_issue));
CREATE TABLE IF NOT EXISTS r926_outbox (
 id TEXT PRIMARY KEY, event_id TEXT NOT NULL, consumer TEXT NOT NULL,
 status TEXT NOT NULL CHECK(status IN ('PENDING','LEASED','DONE','FAILED')),
 lease_owner TEXT, lease_until INTEGER, attempts INTEGER NOT NULL DEFAULT 0,
 available_at INTEGER NOT NULL, completed_at INTEGER, error_code TEXT,
 UNIQUE(event_id,consumer), FOREIGN KEY(event_id) REFERENCES r926_runtime_event(event_id));
CREATE INDEX IF NOT EXISTS r926_outbox_ready ON r926_outbox(status,available_at,lease_until);
CREATE TABLE IF NOT EXISTS r926_feature_manifest (
 schema_hash TEXT PRIMARY KEY, contract_name TEXT NOT NULL, version INTEGER NOT NULL,
 input_stage TEXT NOT NULL, fields_json TEXT NOT NULL, semantic_hash TEXT NOT NULL,
 signer_id TEXT NOT NULL, signature TEXT NOT NULL, created_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS r926_snapshot_registry (
 game TEXT NOT NULL, target_issue TEXT NOT NULL, snapshot_hash TEXT NOT NULL UNIQUE,
 schema_hash TEXT NOT NULL, max_source_issue TEXT NOT NULL, frozen_at INTEGER NOT NULL,
 received_at INTEGER NOT NULL, expected_draw_at INTEGER NOT NULL,
 bundle_hash TEXT NOT NULL, content_path TEXT NOT NULL, evidence_origin TEXT NOT NULL,
 PRIMARY KEY(game,target_issue,schema_hash), CHECK(max_source_issue<target_issue),
 CHECK(frozen_at<expected_draw_at), FOREIGN KEY(schema_hash) REFERENCES r926_feature_manifest(schema_hash));
CREATE TABLE IF NOT EXISTS r926_trial_registry (
 trial_id TEXT PRIMARY KEY, game TEXT NOT NULL, data_hash TEXT NOT NULL,
 schema_hash TEXT NOT NULL, split_hash TEXT NOT NULL, sealed_window_key TEXT NOT NULL,
 protocol_hash TEXT NOT NULL, source_commit TEXT NOT NULL, state TEXT NOT NULL,
 selected_artifact_hash TEXT, created_at INTEGER NOT NULL, sealed_consumed_at INTEGER);
CREATE TABLE IF NOT EXISTS r926_sealed_consumption (
 sealed_window_key TEXT PRIMARY KEY, trial_id TEXT NOT NULL UNIQUE,
 artifact_hash TEXT NOT NULL, look_id TEXT NOT NULL UNIQUE, consumed_at INTEGER NOT NULL,
 FOREIGN KEY(trial_id) REFERENCES r926_trial_registry(trial_id));
CREATE TABLE IF NOT EXISTS r926_bundle (
 bundle_hash TEXT PRIMARY KEY, manifest_json TEXT NOT NULL, manifest_hash TEXT NOT NULL,
 game TEXT NOT NULL, schema_hash TEXT NOT NULL, selection_hash TEXT NOT NULL,
 trained_until_issue TEXT NOT NULL, status TEXT NOT NULL CHECK(status IN ('STAGED','VERIFIED','ACTIVE','REVOKED','REJECTED')),
 signer_id TEXT NOT NULL, signature TEXT NOT NULL, verified_at INTEGER, activated_after_issue TEXT, error_code TEXT);
CREATE TABLE IF NOT EXISTS r926_bundle_part (
 bundle_hash TEXT NOT NULL, role TEXT NOT NULL, artifact_hash TEXT NOT NULL,
 input_schema_hash TEXT NOT NULL, parent_hashes_json TEXT NOT NULL, content_path TEXT NOT NULL,
 PRIMARY KEY(bundle_hash,role), FOREIGN KEY(bundle_hash) REFERENCES r926_bundle(bundle_hash));
CREATE TABLE IF NOT EXISTS r926_active_bundle (
 game TEXT PRIMARY KEY, bundle_hash TEXT NOT NULL, previous_bundle_hash TEXT,
 active_after_issue TEXT NOT NULL, pointer_version INTEGER NOT NULL,
 FOREIGN KEY(bundle_hash) REFERENCES r926_bundle(bundle_hash));
CREATE TABLE IF NOT EXISTS r926_hypothesis (
 hypothesis_id TEXT PRIMARY KEY, family_id TEXT NOT NULL, candidate_namespace TEXT NOT NULL,
 candidate_id TEXT NOT NULL, structure_hash TEXT NOT NULL, registration_issue TEXT NOT NULL,
 batch_id TEXT NOT NULL, alpha_budget REAL NOT NULL CHECK(alpha_budget>=0 AND alpha_budget<=0.05),
 protocol_hash TEXT NOT NULL, parent_hypothesis_id TEXT, UNIQUE(candidate_namespace,candidate_id,structure_hash));
CREATE TABLE IF NOT EXISTS r926_certificate (
 certificate_id TEXT PRIMARY KEY, hypothesis_id TEXT NOT NULL, candidate_hash TEXT NOT NULL,
 champion_bundle_hash TEXT NOT NULL, input_schema_hash TEXT NOT NULL,
 calibration_hash TEXT NOT NULL, selection_hash TEXT NOT NULL, domain_hash TEXT NOT NULL,
 protocol_hash TEXT NOT NULL, level TEXT NOT NULL, window_id TEXT NOT NULL,
 issued_after_issue TEXT NOT NULL, expires_after_issue TEXT NOT NULL,
 max_weight REAL NOT NULL CHECK(max_weight>=0 AND max_weight<=0.30), evidence_hash TEXT NOT NULL,
 signer_id TEXT NOT NULL, signature TEXT NOT NULL, revoked_at INTEGER, revoke_reason TEXT,
 FOREIGN KEY(hypothesis_id) REFERENCES r926_hypothesis(hypothesis_id));
CREATE TABLE IF NOT EXISTS r926_certificate_gate (
 certificate_id TEXT NOT NULL, gate_id TEXT NOT NULL, observed_json TEXT NOT NULL,
 required_json TEXT NOT NULL, result TEXT NOT NULL CHECK(result IN ('PASS','FAIL','UNKNOWN','NOT_APPLICABLE')),
 evidence_hash TEXT NOT NULL, PRIMARY KEY(certificate_id,gate_id),
 FOREIGN KEY(certificate_id) REFERENCES r926_certificate(certificate_id));
CREATE TABLE IF NOT EXISTS r926_window (
 window_id TEXT PRIMARY KEY, game TEXT NOT NULL, hypothesis_id TEXT NOT NULL,
 candidate_hash TEXT NOT NULL, champion_bundle_hash TEXT NOT NULL, calibration_hash TEXT NOT NULL,
 selection_hash TEXT NOT NULL, domain_hash TEXT NOT NULL, start_exclusive TEXT NOT NULL,
 end_inclusive TEXT NOT NULL, required_n INTEGER NOT NULL CHECK(required_n>0),
 block_size INTEGER NOT NULL CHECK(block_size>0), protocol_hash TEXT NOT NULL,
 look_id TEXT NOT NULL UNIQUE, state TEXT NOT NULL CHECK(state IN ('REGISTERED','COLLECTING','SEALED','PASSED','FAILED','ABORTED')),
 result_hash TEXT, CHECK(start_exclusive<end_inclusive), FOREIGN KEY(hypothesis_id) REFERENCES r926_hypothesis(hypothesis_id));
CREATE TABLE IF NOT EXISTS r926_pair (
 window_id TEXT NOT NULL, game TEXT NOT NULL, target_issue TEXT NOT NULL,
 candidate_predraw_hash TEXT NOT NULL, champion_predraw_hash TEXT NOT NULL,
 candidate_hit INTEGER NOT NULL CHECK(candidate_hit IN (0,1)), champion_hit INTEGER NOT NULL CHECK(champion_hit IN (0,1)),
 candidate_p REAL, champion_p REAL, calibration_hash TEXT, label_hash TEXT NOT NULL,
 pair_hash TEXT NOT NULL UNIQUE, PRIMARY KEY(window_id,target_issue),
 CHECK(candidate_p IS NULL OR (candidate_p>=0 AND candidate_p<=1)),
 CHECK(champion_p IS NULL OR (champion_p>=0 AND champion_p<=1)),
 FOREIGN KEY(window_id) REFERENCES r926_window(window_id));
CREATE INDEX IF NOT EXISTS r926_pair_issue ON r926_pair(game,target_issue,window_id);
CREATE TABLE IF NOT EXISTS r926_schedule (
 game TEXT NOT NULL, target_issue TEXT NOT NULL, candidate_namespace TEXT NOT NULL, candidate_id TEXT NOT NULL,
 tier TEXT NOT NULL CHECK(tier IN ('A','B','C')), plan_hash TEXT NOT NULL, status TEXT NOT NULL,
 eligible INTEGER NOT NULL CHECK(eligible IN (0,1)), selected INTEGER NOT NULL CHECK(selected IN (0,1)),
 budget_ms INTEGER NOT NULL CHECK(budget_ms>=0), cost_ns INTEGER, freeze_hash TEXT, error_code TEXT,
 PRIMARY KEY(game,target_issue,candidate_namespace,candidate_id));
CREATE TABLE IF NOT EXISTS r926_lifecycle_event (
 event_id TEXT PRIMARY KEY, candidate_namespace TEXT NOT NULL, candidate_id TEXT NOT NULL,
 from_state TEXT NOT NULL, to_state TEXT NOT NULL, reason_code TEXT NOT NULL,
 metrics_json TEXT NOT NULL, policy_hash TEXT NOT NULL, target_issue TEXT NOT NULL,
 created_at INTEGER NOT NULL, original_available INTEGER NOT NULL CHECK(original_available IN (0,1)));
CREATE INDEX IF NOT EXISTS r926_lifecycle_candidate ON r926_lifecycle_event(candidate_namespace,candidate_id,created_at);
CREATE TABLE IF NOT EXISTS r926_failure_case (
 failure_id TEXT PRIMARY KEY, game TEXT NOT NULL, target_issue TEXT NOT NULL,
 snapshot_hash TEXT NOT NULL, selection_hash TEXT NOT NULL, actual_label_hash TEXT NOT NULL,
 taxonomy_hash TEXT NOT NULL, features_json TEXT NOT NULL, flags_json TEXT NOT NULL,
 created_at INTEGER NOT NULL, UNIQUE(game,target_issue,selection_hash));
CREATE TABLE IF NOT EXISTS r926_repair_intent (
 repair_id TEXT PRIMARY KEY, parent_id TEXT NOT NULL, child_id TEXT NOT NULL UNIQUE,
 failure_cluster_id TEXT NOT NULL, repair_type TEXT NOT NULL,
 changed_modules_json TEXT NOT NULL, expected_effect TEXT NOT NULL, falsification_rule TEXT NOT NULL,
 registered_issue TEXT NOT NULL, future_n_at_birth INTEGER NOT NULL CHECK(future_n_at_birth=0),
 maturity_window_id TEXT, result TEXT NOT NULL CHECK(result IN ('PENDING','MATURED_PASS','MATURED_FAIL','REJECTED','ABORTED')));
CREATE TABLE IF NOT EXISTS r926_counterfactual (
 run_id TEXT PRIMARY KEY, failure_id TEXT NOT NULL, snapshot_hash TEXT NOT NULL,
 module_mask TEXT NOT NULL, runner_hash TEXT NOT NULL, output_hash TEXT NOT NULL,
 result_json TEXT NOT NULL, created_at INTEGER NOT NULL,
 UNIQUE(failure_id,module_mask,runner_hash), FOREIGN KEY(failure_id) REFERENCES r926_failure_case(failure_id));
CREATE TABLE IF NOT EXISTS r926_atom_measure (
 game TEXT NOT NULL, target_issue TEXT NOT NULL, atom_id TEXT NOT NULL, schema_hash TEXT NOT NULL,
 present INTEGER NOT NULL CHECK(present IN (0,1)), value REAL, missing_reason TEXT,
 max_source_issue TEXT NOT NULL, semantic_tier TEXT NOT NULL, cost_ns INTEGER NOT NULL CHECK(cost_ns>=0),
 input_hash TEXT NOT NULL, output_hash TEXT NOT NULL, PRIMARY KEY(game,target_issue,atom_id,schema_hash),
 CHECK((present=1 AND value IS NOT NULL) OR (present=0 AND missing_reason IS NOT NULL)));
CREATE INDEX IF NOT EXISTS r926_atom_recent ON r926_atom_measure(atom_id,game,target_issue);
CREATE TABLE IF NOT EXISTS r926_atom_state (
 atom_id TEXT NOT NULL, schema_hash TEXT NOT NULL, state TEXT NOT NULL CHECK(state IN ('ACTIVE','WATCH','SHADOW_ONLY','QUARANTINED')),
 fixed_legacy INTEGER NOT NULL CHECK(fixed_legacy IN (0,1)), reason TEXT NOT NULL,
 evidence_hash TEXT NOT NULL, policy_hash TEXT NOT NULL, changed_after_issue TEXT NOT NULL,
 PRIMARY KEY(atom_id,schema_hash));
CREATE TABLE IF NOT EXISTS r926_atom_edge (
 left_atom TEXT NOT NULL, right_atom TEXT NOT NULL, edge_type TEXT NOT NULL,
 schema_hash TEXT NOT NULL, window_id TEXT NOT NULL, support_issues INTEGER NOT NULL CHECK(support_issues>=0),
 value REAL NOT NULL, evidence_hash TEXT NOT NULL, PRIMARY KEY(left_atom,right_atom,edge_type,schema_hash,window_id));
CREATE TABLE IF NOT EXISTS r926_contribution (
 game TEXT NOT NULL, target_issue TEXT NOT NULL, baseline_ordinal INTEGER NOT NULL CHECK(baseline_ordinal BETWEEN 0 AND 1199),
 selected_ordinal INTEGER NOT NULL CHECK(selected_ordinal BETWEEN 0 AND 1199), consumer TEXT NOT NULL,
 candidate_id TEXT, certificate_id TEXT, bundle_hash TEXT NOT NULL, selection_hash TEXT NOT NULL,
 effective_weight REAL NOT NULL CHECK(effective_weight>=0 AND effective_weight<=0.30),
 calibration_hash TEXT, reason TEXT NOT NULL, record_hash TEXT NOT NULL UNIQUE,
 PRIMARY KEY(game,target_issue), FOREIGN KEY(certificate_id) REFERENCES r926_certificate(certificate_id));

CREATE TRIGGER IF NOT EXISTS r926_runtime_event_no_update BEFORE UPDATE ON r926_runtime_event BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;
CREATE TRIGGER IF NOT EXISTS r926_runtime_event_no_delete BEFORE DELETE ON r926_runtime_event BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;

CREATE TRIGGER IF NOT EXISTS r926_feature_manifest_no_update BEFORE UPDATE ON r926_feature_manifest BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;
CREATE TRIGGER IF NOT EXISTS r926_feature_manifest_no_delete BEFORE DELETE ON r926_feature_manifest BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;

CREATE TRIGGER IF NOT EXISTS r926_snapshot_registry_no_update BEFORE UPDATE ON r926_snapshot_registry BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;
CREATE TRIGGER IF NOT EXISTS r926_snapshot_registry_no_delete BEFORE DELETE ON r926_snapshot_registry BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;

CREATE TRIGGER IF NOT EXISTS r926_hypothesis_no_update BEFORE UPDATE ON r926_hypothesis BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;
CREATE TRIGGER IF NOT EXISTS r926_hypothesis_no_delete BEFORE DELETE ON r926_hypothesis BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;

CREATE TRIGGER IF NOT EXISTS r926_pair_no_update BEFORE UPDATE ON r926_pair BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;
CREATE TRIGGER IF NOT EXISTS r926_pair_no_delete BEFORE DELETE ON r926_pair BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;

CREATE TRIGGER IF NOT EXISTS r926_lifecycle_event_no_update BEFORE UPDATE ON r926_lifecycle_event BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;
CREATE TRIGGER IF NOT EXISTS r926_lifecycle_event_no_delete BEFORE DELETE ON r926_lifecycle_event BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;

CREATE TRIGGER IF NOT EXISTS r926_failure_case_no_update BEFORE UPDATE ON r926_failure_case BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;
CREATE TRIGGER IF NOT EXISTS r926_failure_case_no_delete BEFORE DELETE ON r926_failure_case BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;

CREATE TRIGGER IF NOT EXISTS r926_counterfactual_no_update BEFORE UPDATE ON r926_counterfactual BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;
CREATE TRIGGER IF NOT EXISTS r926_counterfactual_no_delete BEFORE DELETE ON r926_counterfactual BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;

CREATE TRIGGER IF NOT EXISTS r926_contribution_no_update BEFORE UPDATE ON r926_contribution BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;
CREATE TRIGGER IF NOT EXISTS r926_contribution_no_delete BEFORE DELETE ON r926_contribution BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;

CREATE TRIGGER IF NOT EXISTS r926_sealed_consumption_no_update BEFORE UPDATE ON r926_sealed_consumption BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;
CREATE TRIGGER IF NOT EXISTS r926_sealed_consumption_no_delete BEFORE DELETE ON r926_sealed_consumption BEGIN SELECT RAISE(ABORT,'IMMUTABLE_EVIDENCE'); END;

CREATE TRIGGER IF NOT EXISTS r926_window_contract_immutable BEFORE UPDATE ON r926_window WHEN OLD.window_id IS NOT NEW.window_id OR OLD.game IS NOT NEW.game OR OLD.hypothesis_id IS NOT NEW.hypothesis_id OR OLD.candidate_hash IS NOT NEW.candidate_hash OR OLD.champion_bundle_hash IS NOT NEW.champion_bundle_hash OR OLD.calibration_hash IS NOT NEW.calibration_hash OR OLD.selection_hash IS NOT NEW.selection_hash OR OLD.domain_hash IS NOT NEW.domain_hash OR OLD.start_exclusive IS NOT NEW.start_exclusive OR OLD.end_inclusive IS NOT NEW.end_inclusive OR OLD.required_n IS NOT NEW.required_n OR OLD.block_size IS NOT NEW.block_size OR OLD.protocol_hash IS NOT NEW.protocol_hash OR OLD.look_id IS NOT NEW.look_id BEGIN SELECT RAISE(ABORT,'CONTRACT_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS r926_certificate_contract_immutable BEFORE UPDATE ON r926_certificate WHEN OLD.certificate_id IS NOT NEW.certificate_id OR OLD.hypothesis_id IS NOT NEW.hypothesis_id OR OLD.candidate_hash IS NOT NEW.candidate_hash OR OLD.champion_bundle_hash IS NOT NEW.champion_bundle_hash OR OLD.input_schema_hash IS NOT NEW.input_schema_hash OR OLD.calibration_hash IS NOT NEW.calibration_hash OR OLD.selection_hash IS NOT NEW.selection_hash OR OLD.domain_hash IS NOT NEW.domain_hash OR OLD.protocol_hash IS NOT NEW.protocol_hash OR OLD.level IS NOT NEW.level OR OLD.window_id IS NOT NEW.window_id OR OLD.issued_after_issue IS NOT NEW.issued_after_issue OR OLD.expires_after_issue IS NOT NEW.expires_after_issue OR OLD.max_weight IS NOT NEW.max_weight OR OLD.evidence_hash IS NOT NEW.evidence_hash OR OLD.signer_id IS NOT NEW.signer_id OR OLD.signature IS NOT NEW.signature BEGIN SELECT RAISE(ABORT,'CONTRACT_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS r926_bundle_contract_immutable BEFORE UPDATE ON r926_bundle WHEN OLD.bundle_hash IS NOT NEW.bundle_hash OR OLD.manifest_json IS NOT NEW.manifest_json OR OLD.manifest_hash IS NOT NEW.manifest_hash OR OLD.game IS NOT NEW.game OR OLD.schema_hash IS NOT NEW.schema_hash OR OLD.selection_hash IS NOT NEW.selection_hash OR OLD.trained_until_issue IS NOT NEW.trained_until_issue OR OLD.signer_id IS NOT NEW.signer_id OR OLD.signature IS NOT NEW.signature BEGIN SELECT RAISE(ABORT,'CONTRACT_IMMUTABLE'); END;

CREATE TRIGGER IF NOT EXISTS r926_pair_window_match BEFORE INSERT ON r926_pair
WHEN NOT EXISTS (SELECT 1 FROM r926_window w WHERE w.window_id=NEW.window_id AND w.game=NEW.game
 AND NEW.target_issue>w.start_exclusive AND NEW.target_issue<=w.end_inclusive
 AND w.state IN ('REGISTERED','COLLECTING')
 AND (NEW.candidate_p IS NULL OR NEW.calibration_hash=w.calibration_hash))
BEGIN SELECT RAISE(ABORT,'PAIR_WINDOW_CONTRACT'); END;
CREATE TRIGGER IF NOT EXISTS r926_window_state_transition BEFORE UPDATE OF state ON r926_window
WHEN NOT (OLD.state=NEW.state OR (OLD.state='REGISTERED' AND NEW.state IN ('COLLECTING','ABORTED'))
 OR (OLD.state='COLLECTING' AND NEW.state IN ('SEALED','ABORTED'))
 OR (OLD.state='SEALED' AND NEW.state IN ('PASSED','FAILED','ABORTED')))
BEGIN SELECT RAISE(ABORT,'WINDOW_STATE_TRANSITION'); END;
CREATE TRIGGER IF NOT EXISTS r926_certificate_no_unrevoke BEFORE UPDATE ON r926_certificate
WHEN OLD.revoked_at IS NOT NULL AND (NEW.revoked_at IS NOT OLD.revoked_at OR NEW.revoke_reason IS NOT OLD.revoke_reason)
BEGIN SELECT RAISE(ABORT,'REVOCATION_IMMUTABLE'); END;
