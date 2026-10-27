-- Proposed metadata schema, NOT a second draw / REAL / settlement database.
-- Integrate through existing DbStore owner and versioned migrations.
-- TEXT issue keys remain opaque; numeric ordinals come from the verified resolver.
PRAGMA foreign_keys=ON;
CREATE TABLE IF NOT EXISTS r25_policy (
 policy_hash TEXT PRIMARY KEY, structure_hash TEXT NOT NULL,
 canonical_spec BLOB NOT NULL, birth_ordinal INTEGER NOT NULL CHECK(birth_ordinal>=0),
 protocol_hash TEXT NOT NULL, status TEXT NOT NULL,
 parent_policy_hash TEXT REFERENCES r25_policy(policy_hash),
 CHECK(status IN ('DRAFT','EXECUTABLE','TRAINED','RESEARCH_PASS','PROSPECTIVE_SHADOW','QUALIFIED','ACTIVE','SUSPENDED','RETIRED'))
);
CREATE TABLE IF NOT EXISTS r25_checkpoint (
 checkpoint_hash TEXT PRIMARY KEY,
 policy_hash TEXT NOT NULL REFERENCES r25_policy(policy_hash),
 parent_checkpoint TEXT REFERENCES r25_checkpoint(checkpoint_hash),
 trained_through INTEGER NOT NULL CHECK(trained_through>=0),
 artifact_path TEXT NOT NULL, artifact_bytes INTEGER NOT NULL CHECK(artifact_bytes>=0),
 manifest_hash TEXT NOT NULL, state_schema INTEGER NOT NULL,
 created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS r25_active_snapshot (
 owner TEXT PRIMARY KEY, checkpoint_hash TEXT NOT NULL REFERENCES r25_checkpoint(checkpoint_hash),
 version INTEGER NOT NULL CHECK(version>=0), committed_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS r25_feedback_ref (
 event_id TEXT PRIMARY KEY, canonical_settlement_id TEXT NOT NULL UNIQUE,
 issue TEXT NOT NULL UNIQUE, draw_hash TEXT NOT NULL, ordinal INTEGER NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS r25_feedback_consumed (
 event_id TEXT NOT NULL REFERENCES r25_feedback_ref(event_id),
 policy_hash TEXT NOT NULL REFERENCES r25_policy(policy_hash),
 checkpoint_hash TEXT NOT NULL REFERENCES r25_checkpoint(checkpoint_hash),
 consumed_at INTEGER NOT NULL,
 PRIMARY KEY(event_id,policy_hash)
);
CREATE TABLE IF NOT EXISTS r25_exposure (
 protocol_hash TEXT NOT NULL, cohort_id TEXT NOT NULL, dataset_hash TEXT NOT NULL,
 scope TEXT NOT NULL CHECK(scope IN ('DEV','TUNE','SEALED_RESERVED','SEALED_OPENED','EXPOSED_HISTORY','PROSPECTIVE')),
 first_ordinal INTEGER NOT NULL,last_ordinal INTEGER NOT NULL,
 opened_at INTEGER, PRIMARY KEY(protocol_hash,cohort_id),CHECK(last_ordinal>=first_ordinal)
);
CREATE TABLE IF NOT EXISTS r25_alpha_spend (
 reservation_id TEXT PRIMARY KEY, family_budget_id TEXT NOT NULL,
 policy_hash TEXT NOT NULL REFERENCES r25_policy(policy_hash),
 candidate_index INTEGER NOT NULL CHECK(candidate_index>=1),
 look_index INTEGER NOT NULL CHECK(look_index>=1), endpoint_id TEXT NOT NULL,
 alpha REAL NOT NULL CHECK(alpha>0 AND alpha<1),
 reserved_before_ordinal INTEGER NOT NULL, observed_through INTEGER,
 UNIQUE(family_budget_id,candidate_index,look_index,endpoint_id)
);
CREATE TABLE IF NOT EXISTS r25_qualification_event (
 event_id TEXT PRIMARY KEY, policy_hash TEXT NOT NULL REFERENCES r25_policy(policy_hash),
 evidence_hash TEXT NOT NULL, budget_reservation TEXT NOT NULL REFERENCES r25_alpha_spend(reservation_id),
 verdict TEXT NOT NULL, observed_through INTEGER NOT NULL, created_at INTEGER NOT NULL,
 CHECK(verdict IN ('INSUFFICIENT','INVALID','INFERIOR','NONINFERIOR','IMPROVED','QUALIFIED','REVOKED'))
);
CREATE TABLE IF NOT EXISTS r25_research_task (
 task_id TEXT PRIMARY KEY, policy_hash TEXT REFERENCES r25_policy(policy_hash),
 input_manifest_hash TEXT NOT NULL, phase TEXT NOT NULL, cursor_blob BLOB,
 version INTEGER NOT NULL CHECK(version>=0), lease_owner TEXT, lease_until INTEGER,
 progress_at INTEGER, error_code TEXT, cpu_ms INTEGER NOT NULL DEFAULT 0 CHECK(cpu_ms>=0)
);
CREATE TABLE IF NOT EXISTS r25_report_section (
 report_id TEXT NOT NULL, section_id TEXT NOT NULL, status TEXT NOT NULL,
 expected_rows INTEGER NOT NULL CHECK(expected_rows>=0), exported_rows INTEGER NOT NULL CHECK(exported_rows>=0),
 omitted_rows INTEGER NOT NULL CHECK(omitted_rows>=0), payload_hash TEXT, error_chain TEXT,
 PRIMARY KEY(report_id,section_id), CHECK(expected_rows=exported_rows+omitted_rows)
);
-- No INSERT into canonical REAL/settlement tables exists here.
-- Binding to actual existing tables, FKs, authorization uniqueness, and migration versions
-- requires the complete production schema and exact owner interfaces in B00/B01.
-- Alpha root totals must be reserved transactionally by the existing writer; SQL uniqueness
-- alone is NOT a proof of the full lifetime budget or pre-outcome registration.
