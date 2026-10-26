-- HOST reference DDL. Production: apply one statement per existing migration Owner.
-- No raw draw, REAL authority, or formal settlement table is created here.
PRAGMA foreign_keys=ON;
CREATE TABLE IF NOT EXISTS r25_policy(
 policy_hash TEXT PRIMARY KEY,
 spec_json TEXT NOT NULL,
 birth_ordinal INTEGER NOT NULL CHECK(birth_ordinal>0),
 mode TEXT NOT NULL CHECK(mode IN('STATIC','ADAPTIVE'))
);
CREATE TRIGGER IF NOT EXISTS r25_policy_no_update BEFORE UPDATE ON r25_policy
 BEGIN SELECT RAISE(ABORT,'POLICY_IMMUTABLE'); END;
CREATE TRIGGER IF NOT EXISTS r25_policy_no_delete BEFORE DELETE ON r25_policy
 BEGIN SELECT RAISE(ABORT,'POLICY_IMMUTABLE'); END;
CREATE TABLE IF NOT EXISTS r25_checkpoint(
 checkpoint_hash TEXT PRIMARY KEY,
 policy_hash TEXT NOT NULL REFERENCES r25_policy(policy_hash),
 parent_hash TEXT REFERENCES r25_checkpoint(checkpoint_hash),
 state_sequence INTEGER NOT NULL CHECK(state_sequence>=0),
 trained_through INTEGER NOT NULL,
 artifact_hash TEXT NOT NULL,
 optimizer_hash TEXT NOT NULL,
 rng_hash TEXT NOT NULL,
 UNIQUE(policy_hash,state_sequence)
);
CREATE TABLE IF NOT EXISTS r25_active(
 policy_hash TEXT PRIMARY KEY REFERENCES r25_policy(policy_hash),
 checkpoint_hash TEXT NOT NULL REFERENCES r25_checkpoint(checkpoint_hash),
 version INTEGER NOT NULL CHECK(version>=0)
);
CREATE TABLE IF NOT EXISTS r25_consumption(
 event_id TEXT NOT NULL,
 policy_hash TEXT NOT NULL REFERENCES r25_policy(policy_hash),
 source_hash TEXT NOT NULL,
 checkpoint_hash TEXT NOT NULL REFERENCES r25_checkpoint(checkpoint_hash),
 PRIMARY KEY(event_id,policy_hash)
);
CREATE TABLE IF NOT EXISTS r25_task(
 task_id TEXT PRIMARY KEY,
 snapshot_hash TEXT NOT NULL,
 cursor INTEGER NOT NULL DEFAULT 0 CHECK(cursor>=0),
 fence INTEGER NOT NULL DEFAULT 0 CHECK(fence>=0),
 owner TEXT,
 lease_until INTEGER NOT NULL DEFAULT 0,
 progress_at INTEGER NOT NULL DEFAULT 0,
 status TEXT NOT NULL DEFAULT 'QUEUED' CHECK(status IN('QUEUED','RUNNING','YIELDED','DONE','FAILED','CANCELLED'))
);
CREATE TABLE IF NOT EXISTS r25_cohort(
 cohort_id TEXT PRIMARY KEY,
 policy_hash TEXT NOT NULL REFERENCES r25_policy(policy_hash),
 start_ordinal INTEGER NOT NULL,
 end_ordinal INTEGER NOT NULL CHECK(end_ordinal>start_ordinal),
 mode TEXT NOT NULL CHECK(mode IN('FIXED_ENDPOINT','SEQUENTIAL')),
 alpha REAL NOT NULL CHECK(alpha>0 AND alpha<1),
 protocol_hash TEXT NOT NULL,
 status TEXT NOT NULL DEFAULT 'FROZEN' CHECK(status IN('FROZEN','OPENED','CLOSED'))
);
CREATE TABLE IF NOT EXISTS r25_look(
 cohort_id TEXT NOT NULL REFERENCES r25_cohort(cohort_id),
 evidence_digest TEXT NOT NULL,
 look_index INTEGER NOT NULL CHECK(look_index>0),
 alpha REAL NOT NULL CHECK(alpha>0 AND alpha<1),
 status TEXT NOT NULL DEFAULT 'RESERVED' CHECK(status IN('RESERVED','COMPLETE','ERROR')),
 PRIMARY KEY(cohort_id,evidence_digest),
 UNIQUE(cohort_id,look_index)
);
CREATE TABLE IF NOT EXISTS r25_experiment(
 experiment_id TEXT PRIMARY KEY,
 hypothesis_hash TEXT NOT NULL,
 parent_policy TEXT NOT NULL,
 child_policy TEXT NOT NULL,
 exposure_scope TEXT NOT NULL CHECK(exposure_scope IN('DEV','TUNE','SEALED','LIVE','SYNTHETIC')),
 seed INTEGER NOT NULL,
 cpu_seconds REAL CHECK(cpu_seconds>=0),
 status TEXT NOT NULL,
 result_hash TEXT,
 failure_code TEXT
);
CREATE TABLE IF NOT EXISTS r25_publication(
 manifest_hash TEXT PRIMARY KEY,
 policy_hash TEXT NOT NULL REFERENCES r25_policy(policy_hash),
 checkpoint_hash TEXT NOT NULL REFERENCES r25_checkpoint(checkpoint_hash),
 expected_version INTEGER NOT NULL,
 file_hash TEXT NOT NULL,
 status TEXT NOT NULL CHECK(status IN('PREPARED','COMMITTED','ACTIVE','RETIRED','REJECTED'))
);
CREATE TABLE IF NOT EXISTS r25_capability_receipt(
 capability_id TEXT NOT NULL,
 stage TEXT NOT NULL CHECK(stage IN('REGISTERED','EXECUTABLE','INVOKED','TRAINED','CHECKPOINTED','HISTORICAL','PROSPECTIVE','MAINLINE')),
 artifact_hash TEXT NOT NULL,
 evidence_ref TEXT NOT NULL,
 PRIMARY KEY(capability_id,stage,artifact_hash)
);
CREATE TABLE IF NOT EXISTS r25_report_section(
 report_id TEXT NOT NULL,
 section_id TEXT NOT NULL,
 status TEXT NOT NULL CHECK(status IN('PASS','FAIL','PARTIAL','NOT_RUN','INSUFFICIENT','NOT_APPLICABLE','ERROR')),
 expected_rows INTEGER NOT NULL CHECK(expected_rows>=0),
 exported_rows INTEGER NOT NULL CHECK(exported_rows>=0),
 omitted_rows INTEGER NOT NULL CHECK(omitted_rows>=0),
 evidence_hash TEXT,
 error_code TEXT,
 CHECK(expected_rows=exported_rows+omitted_rows),
 PRIMARY KEY(report_id,section_id)
);
CREATE INDEX IF NOT EXISTS r25_task_due ON r25_task(status,lease_until,progress_at);
CREATE INDEX IF NOT EXISTS r25_checkpoint_policy ON r25_checkpoint(policy_hash,trained_through);
