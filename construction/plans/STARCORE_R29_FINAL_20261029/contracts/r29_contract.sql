-- R29 proposed SQL contract, standalone validation only; not an application migration.
PRAGMA foreign_keys=ON;
CREATE TABLE event_outbox(event_id TEXT PRIMARY KEY, issue TEXT NOT NULL, kind TEXT NOT NULL,
 payload_hash TEXT NOT NULL, committed_at INTEGER NOT NULL, acked INTEGER NOT NULL DEFAULT 0 CHECK(acked IN(0,1)));
CREATE TABLE research_inbox(event_id TEXT PRIMARY KEY, payload_hash TEXT NOT NULL, applied_at INTEGER NOT NULL);
CREATE TABLE model_artifact(model_hash TEXT PRIMARY KEY, feature_abi TEXT NOT NULL, trained_through TEXT NOT NULL,
 optimizer_hash TEXT NOT NULL, rng_hash TEXT NOT NULL, manifest_hash TEXT NOT NULL, status TEXT NOT NULL);
CREATE TABLE experiment(experiment_id TEXT PRIMARY KEY, policy_hash TEXT NOT NULL, global_index INTEGER NOT NULL UNIQUE CHECK(global_index>0),
 policy_version TEXT NOT NULL, registered_at INTEGER NOT NULL, first_future_issue TEXT NOT NULL,
 alpha REAL NOT NULL CHECK(alpha>0 AND alpha<1), confirm_manifest_hash TEXT NOT NULL);
CREATE TABLE frozen_prediction(freeze_id TEXT PRIMARY KEY, experiment_id TEXT NOT NULL REFERENCES experiment(experiment_id),
 issue TEXT NOT NULL, position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),
 n1 INTEGER NOT NULL CHECK(n1 BETWEEN 1 AND 10), n2 INTEGER NOT NULL CHECK(n2 BETWEEN 1 AND 10), n3 INTEGER NOT NULL CHECK(n3 BETWEEN 1 AND 10),
 act INTEGER NOT NULL CHECK(act IN(0,1)), feature_hash TEXT NOT NULL, model_hash TEXT NOT NULL,
 prepared_at INTEGER NOT NULL, frozen_at INTEGER NOT NULL, deadline_at INTEGER NOT NULL,
 CHECK(n1<n2 AND n2<n3), CHECK(prepared_at<=frozen_at), CHECK(frozen_at<=deadline_at-5000), UNIQUE(experiment_id,issue));
CREATE TABLE future_settlement(freeze_id TEXT PRIMARY KEY REFERENCES frozen_prediction(freeze_id),
 draw_hash TEXT NOT NULL, hit INTEGER NOT NULL CHECK(hit IN(0,1)), settled_at INTEGER NOT NULL);
CREATE TABLE real_decision(issue TEXT PRIMARY KEY, freeze_id TEXT NOT NULL, certificate_hash TEXT NOT NULL,
 selector_hash TEXT NOT NULL, model_hash TEXT NOT NULL, committed_at INTEGER NOT NULL);
CREATE TABLE capability_finding(finding_id TEXT PRIMARY KEY, capability_id TEXT NOT NULL, snapshot_hash TEXT NOT NULL,
 evidence_strength TEXT NOT NULL CHECK(evidence_strength IN('OBSERVED','SUPPORTED_CAUSE','HYPOTHESIS','UNKNOWN')),
 evidence_ref TEXT NOT NULL, repair_card TEXT NOT NULL, next_test TEXT NOT NULL);
